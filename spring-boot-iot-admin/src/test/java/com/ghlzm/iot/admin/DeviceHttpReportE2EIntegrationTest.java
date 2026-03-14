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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@ActiveProfiles("e2e")
class DeviceHttpReportE2EIntegrationTest {

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DevicePropertyMapper devicePropertyMapper;

    @Autowired
    private DeviceMessageLogMapper deviceMessageLogMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    void shouldPersistReportOnSuccessPath() throws Exception {
        DeviceFixture fixture = createProductAndDevice();

        JsonNode reportResponse = postJson("/message/http/report", mapOf(
                "protocolCode", "mqtt-json",
                "productKey", fixture.productKey(),
                "deviceCode", fixture.deviceCode(),
                "payload", "{\"messageType\":\"property\",\"properties\":{\"temperature\":26.5,\"humidity\":68}}",
                "topic", fixture.topic(),
                "clientId", fixture.deviceCode(),
                "tenantId", "1"
        ));
        assertOk(reportResponse);

        JsonNode deviceQueryResponse = getJson("/device/code/" + fixture.deviceCode());
        assertOk(deviceQueryResponse);
        assertEquals(fixture.deviceCode(), deviceQueryResponse.path("data").path("deviceCode").asText());

        JsonNode propertiesResponse = getJson("/device/" + fixture.deviceCode() + "/properties");
        assertOk(propertiesResponse);
        assertEquals(2, propertiesResponse.path("data").size());

        JsonNode logsResponse = getJson("/device/" + fixture.deviceCode() + "/message-logs");
        assertOk(logsResponse);
        assertTrue(logsResponse.path("data").size() >= 1);

        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, fixture.deviceCode())
                .orderByDesc(Device::getCreateTime)
                .last("limit 1"));
        assertNotNull(device);
        assertEquals(1, device.getOnlineStatus());
        assertEquals(1, device.getActivateStatus());
        assertNotNull(device.getLastReportTime());
        assertTrue(device.getLastReportTime().isAfter(LocalDateTime.now().minusMinutes(2)));

        List<DeviceProperty> properties = devicePropertyMapper.selectList(new LambdaQueryWrapper<DeviceProperty>()
                .eq(DeviceProperty::getDeviceId, device.getId()));
        assertEquals(2, properties.size());
        assertTrue(properties.stream().anyMatch(item ->
                "temperature".equals(item.getIdentifier()) &&
                        "26.5".equals(item.getPropertyValue()) &&
                        "double".equals(item.getValueType())));
        assertTrue(properties.stream().anyMatch(item ->
                "humidity".equals(item.getIdentifier()) &&
                        "68".equals(item.getPropertyValue())));

        List<DeviceMessageLog> logs = deviceMessageLogMapper.selectList(new LambdaQueryWrapper<DeviceMessageLog>()
                .eq(DeviceMessageLog::getDeviceId, device.getId())
                .orderByDesc(DeviceMessageLog::getReportTime));
        assertTrue(logs.stream().anyMatch(item ->
                fixture.topic().equals(item.getTopic()) && "property".equals(item.getMessageType())));
    }

    @Test
    void shouldReturnBizErrorWhenProtocolIsInvalid() throws Exception {
        DeviceFixture fixture = createProductAndDevice();

        JsonNode reportResponse = postJson("/message/http/report", mapOf(
                "protocolCode", "bad-protocol",
                "productKey", fixture.productKey(),
                "deviceCode", fixture.deviceCode(),
                "payload", "{\"messageType\":\"property\",\"properties\":{\"temperature\":26.5}}",
                "topic", fixture.topic(),
                "clientId", fixture.deviceCode(),
                "tenantId", "1"
        ));

        assertBizError(reportResponse, "未找到协议适配器: bad-protocol");

        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, fixture.deviceCode())
                .orderByDesc(Device::getCreateTime)
                .last("limit 1"));
        assertNotNull(device);
        assertEquals(0, device.getOnlineStatus());

        List<DeviceMessageLog> logs = deviceMessageLogMapper.selectList(new LambdaQueryWrapper<DeviceMessageLog>()
                .eq(DeviceMessageLog::getDeviceId, device.getId()));
        assertEquals(0, logs.size());
    }

    @Test
    void shouldReturnBizErrorWhenDeviceDoesNotExist() throws Exception {
        DeviceFixture fixture = createProductOnly();
        String missingDeviceCode = fixture.deviceCode();

        JsonNode reportResponse = postJson("/message/http/report", mapOf(
                "protocolCode", "mqtt-json",
                "productKey", fixture.productKey(),
                "deviceCode", missingDeviceCode,
                "payload", "{\"messageType\":\"property\",\"properties\":{\"temperature\":26.5}}",
                "topic", fixture.topic(),
                "clientId", missingDeviceCode,
                "tenantId", "1"
        ));

        assertBizError(reportResponse, "设备不存在: " + missingDeviceCode);

        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, missingDeviceCode)
                .orderByDesc(Device::getCreateTime)
                .last("limit 1"));
        assertNull(device);
    }

    private DeviceFixture createProductAndDevice() throws Exception {
        DeviceFixture fixture = newFixture();

        JsonNode productResponse = postJson("/device/product/add", mapOf(
                "productKey", fixture.productKey(),
                "productName", "Integration Product " + fixture.suffix(),
                "protocolCode", "mqtt-json",
                "nodeType", 1,
                "dataFormat", "JSON",
                "manufacturer", "Codex",
                "description", "E2E integration product"
        ));
        assertOk(productResponse);

        JsonNode deviceResponse = postJson("/device/add", mapOf(
                "productKey", fixture.productKey(),
                "deviceName", "Integration Device " + fixture.suffix(),
                "deviceCode", fixture.deviceCode(),
                "deviceSecret", "it-secret",
                "clientId", fixture.deviceCode(),
                "username", fixture.deviceCode(),
                "password", "it-secret",
                "firmwareVersion", "1.0.0",
                "ipAddress", "10.0.0.1",
                "address", "integration-lab",
                "metadataJson", "{\"source\":\"e2e\"}"
        ));
        assertOk(deviceResponse);
        return fixture;
    }

    private DeviceFixture createProductOnly() throws Exception {
        DeviceFixture fixture = newFixture();
        JsonNode productResponse = postJson("/device/product/add", mapOf(
                "productKey", fixture.productKey(),
                "productName", "Integration Product " + fixture.suffix(),
                "protocolCode", "mqtt-json",
                "nodeType", 1,
                "dataFormat", "JSON",
                "manufacturer", "Codex",
                "description", "E2E integration product"
        ));
        assertOk(productResponse);
        return fixture;
    }

    private DeviceFixture newFixture() {
        String suffix = System.currentTimeMillis() + "-" + Math.abs(System.nanoTime());
        String productKey = "it-product-" + suffix;
        String deviceCode = "it-device-" + suffix;
        String topic = "/sys/" + productKey + "/" + deviceCode + "/thing/property/post";
        return new DeviceFixture(suffix, productKey, deviceCode, topic);
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

    private JsonNode getJson(String path) throws Exception {
        String response = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private void assertOk(JsonNode response) {
        assertEquals(200, response.path("code").asInt());
    }

    private void assertBizError(JsonNode response, String message) {
        assertEquals(500, response.path("code").asInt());
        assertEquals(message, response.path("msg").asText());
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return result;
    }

    private record DeviceFixture(String suffix, String productKey, String deviceCode, String topic) {
    }
}
