package com.ghlzm.iot.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.message.mqtt.MqttMessageConsumer;
import com.ghlxk.cloud.aes.core.AesEncryptor;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(classes = IotAdminApplication.class)
@ActiveProfiles("e2e")
class DeviceMqttReportE2EIntegrationTest {

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DevicePropertyMapper devicePropertyMapper;

    @Autowired
    private DeviceMessageLogMapper deviceMessageLogMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MqttMessageConsumer mqttMessageConsumer;

    @Autowired
    @Qualifier("aesEncryptors")
    private Map<String, AesEncryptor> aesEncryptors;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    void shouldPersistStandardTopicMqttReport() throws Exception {
        DeviceFixture fixture = createProductAndDevice();

        mqttMessageConsumer.messageArrived(
                fixture.standardTopic(),
                new MqttMessage("{\"messageType\":\"property\",\"properties\":{\"temperature\":26.5,\"humidity\":68}}"
                        .getBytes(StandardCharsets.UTF_8))
        );

        assertDeviceState(fixture.deviceCode(), fixture.standardTopic(), "property",
                null, "temperature", "26.5", "humidity", "68");
    }

    @Test
    void shouldPersistLegacyDpTopicReport() throws Exception {
        DeviceFixture fixture = createProductAndDevice();

        mqttMessageConsumer.messageArrived(
                "$dp",
                new MqttMessage(("{\"deviceCode\":\"" + fixture.deviceCode() + "\",\"temperature\":25.1,\"humidity\":61}")
                        .getBytes(StandardCharsets.UTF_8))
        );

        assertDeviceState(fixture.deviceCode(), "$dp", "property",
                null, "temperature", "25.1", "humidity", "61");
    }

    @Test
    void shouldPersistLegacyNestedTelemetryPayload() throws Exception {
        DeviceFixture fixture = createProductAndDevice();

        mqttMessageConsumer.messageArrived(
                "$dp",
                new MqttMessage(("""
                        {"%s":{"L1_QJ_1":{"2026-03-14T07:04:03.000Z":{"X":3.15,"Y":-5.14,"Z":83.97,"angle":-6.03,"trend":236.18,"AZI":236.18}},"L1_JS_1":{"2026-03-14T07:04:03.000Z":{"gX":-0.04,"gY":0.18,"gZ":-0.04}}}}
                        """.formatted(fixture.deviceCode())).getBytes(StandardCharsets.UTF_8))
        );

        assertDeviceState(
                fixture.deviceCode(),
                "$dp",
                "property",
                LocalDateTime.of(2026, 3, 14, 15, 4, 3),
                "L1_QJ_1.X",
                "3.15",
                "L1_JS_1.gY",
                "0.18"
        );
    }

    @Test
    void shouldPersistLegacyStatusPayloadWithControlPrefix() throws Exception {
        DeviceFixture fixture = createProductAndDevice();

        mqttMessageConsumer.messageArrived(
                "$dp",
                new MqttMessage(("""
                        \u0010{"%s":{"S1_ZT_1":{"ext_power_volt":3.540,"solar_volt":6.185,"battery_dump_energy":1,"temp":0.0,"humidity":0,"lon":103.482170,"lat":36.180176,"signal_4g":-51,"sw_version":"V1.0.3(Jul 19 2023 16:48:13)-15522832","sensor_state":{"L1_JS_1":0,"L1_QJ_1":0,"L1_LF_1":3}}}}
                        """.formatted(fixture.deviceCode())).getBytes(StandardCharsets.UTF_8))
        );

        assertDeviceState(fixture.deviceCode(), "$dp", "status",
                null, "S1_ZT_1.ext_power_volt", "3.54", "S1_ZT_1.sensor_state.L1_LF_1", "3");

        List<DeviceMessageLog> logs = deviceMessageLogMapper.selectList(new LambdaQueryWrapper<DeviceMessageLog>()
                .orderByDesc(DeviceMessageLog::getReportTime));
        assertTrue(logs.stream().anyMatch(item ->
                fixture.deviceCode().equals(resolveDeviceCodeFromPayload(item.getPayload()))
                        && "$dp".equals(item.getTopic())
                        && "status".equals(item.getMessageType())));
    }

    @Test
    void shouldPersistEncryptedLegacyDpTopicReport() throws Exception {
        DeviceFixture fixture = createProductAndDevice();
        AesEncryptor aesEncryptor = aesEncryptors.get("62000001");
        assertNotNull(aesEncryptor);

        String plaintextJson = """
                {"deviceCode":"%s","temperature":25.1,"humidity":61}
                """.formatted(fixture.deviceCode()).trim();
        byte[] innerPacket = buildPacket((byte) 1, plaintextJson);
        String encryptedBody = aesEncryptor.encrypt(new String(innerPacket, StandardCharsets.ISO_8859_1));
        String envelopeJson = """
                {"header":{"appId":"62000001"},"bodies":{"body":"%s"}}
                """.formatted(encryptedBody);
        byte[] outerPacket = buildPacket((byte) 1, envelopeJson);

        mqttMessageConsumer.messageArrived("$dp", new MqttMessage(outerPacket));

        assertDeviceState(fixture.deviceCode(), "$dp", "property",
                null, "temperature", "25.1", "humidity", "61");
    }

    private void assertDeviceState(String deviceCode,
                                   String topic,
                                   String expectedMessageType,
                                   LocalDateTime expectedReportTime,
                                   String firstIdentifier,
                                   String firstValue,
                                   String secondIdentifier,
                                   String secondValue) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode)
                .orderByDesc(Device::getCreateTime)
                .last("limit 1"));
        assertNotNull(device);
        assertEquals(1, device.getOnlineStatus());
        assertNotNull(device.getLastReportTime());
        if (expectedReportTime == null) {
            assertTrue(device.getLastReportTime().isAfter(LocalDateTime.now().minusMinutes(2)));
        } else {
            assertEquals(expectedReportTime, device.getLastReportTime());
        }

        List<DeviceProperty> properties = devicePropertyMapper.selectList(new LambdaQueryWrapper<DeviceProperty>()
                .eq(DeviceProperty::getDeviceId, device.getId()));
        assertTrue(properties.stream().anyMatch(item ->
                firstIdentifier.equals(item.getIdentifier()) && firstValue.equals(item.getPropertyValue())));
        assertTrue(properties.stream().anyMatch(item ->
                secondIdentifier.equals(item.getIdentifier()) && secondValue.equals(item.getPropertyValue())));

        List<DeviceMessageLog> logs = deviceMessageLogMapper.selectList(new LambdaQueryWrapper<DeviceMessageLog>()
                .eq(DeviceMessageLog::getDeviceId, device.getId())
                .orderByDesc(DeviceMessageLog::getReportTime));
        assertTrue(logs.stream().anyMatch(item ->
                topic.equals(item.getTopic()) && expectedMessageType.equals(item.getMessageType())));
    }

    private DeviceFixture createProductAndDevice() throws Exception {
        return createProductAndDevice(null);
    }

    private DeviceFixture createProductAndDevice(String specifiedDeviceCode) throws Exception {
        DeviceFixture fixture = newFixture(specifiedDeviceCode);

        JsonNode productResponse = postJson("/device/product/add", mapOf(
                "productKey", fixture.productKey(),
                "productName", "MQTT Integration Product " + fixture.suffix(),
                "protocolCode", "mqtt-json",
                "nodeType", 1,
                "dataFormat", "JSON",
                "manufacturer", "Codex",
                "description", "MQTT E2E integration product"
        ));
        assertOk(productResponse);

        JsonNode deviceResponse = postJson("/device/add", mapOf(
                "productKey", fixture.productKey(),
                "deviceName", "MQTT Integration Device " + fixture.suffix(),
                "deviceCode", fixture.deviceCode(),
                "deviceSecret", "it-secret",
                "clientId", fixture.deviceCode(),
                "username", fixture.deviceCode(),
                "password", "it-secret",
                "firmwareVersion", "1.0.0",
                "ipAddress", "10.0.0.2",
                "address", "mqtt-lab",
                "metadataJson", "{\"source\":\"mqtt-e2e\"}"
        ));
        assertOk(deviceResponse);
        return fixture;
    }

    private DeviceFixture newFixture(String specifiedDeviceCode) {
        String suffix = System.currentTimeMillis() + "-" + Math.abs(System.nanoTime());
        String productKey = "mqtt-product-" + suffix;
        String deviceCode = specifiedDeviceCode == null || specifiedDeviceCode.isBlank()
                ? "mqtt-device-" + suffix
                : specifiedDeviceCode;
        String standardTopic = "/sys/" + productKey + "/" + deviceCode + "/thing/property/post";
        return new DeviceFixture(suffix, productKey, deviceCode, standardTopic);
    }

    private JsonNode postJson(String path, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>(body))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private void assertOk(JsonNode response) {
        assertEquals(200, response.path("code").asInt());
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return result;
    }

    private String resolveDeviceCodeFromPayload(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            return node.fieldNames().hasNext() ? node.fieldNames().next() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 构造“类型字节 + 长度字节 + JSON 正文”的历史数据帧，
     * 用于模拟真实 $dp 主题下的明文/密文载荷格式。
     */
    private byte[] buildPacket(byte type, String json) {
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        int length = jsonBytes.length;
        byte[] packet = new byte[length + 3];
        packet[0] = type;
        packet[1] = (byte) ((length >> 8) & 0xFF);
        packet[2] = (byte) (length & 0xFF);
        System.arraycopy(jsonBytes, 0, packet, 3, length);
        return packet;
    }

    private record DeviceFixture(String suffix, String productKey, String deviceCode, String standardTopic) {
    }
}
