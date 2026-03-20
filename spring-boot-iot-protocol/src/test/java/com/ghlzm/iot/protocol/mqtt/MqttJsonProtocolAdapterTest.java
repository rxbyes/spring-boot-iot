package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MqttJsonProtocolAdapterTest {

    private final MqttJsonProtocolAdapter adapter = newAdapter(new IotProperties());

    @Test
    void shouldDecodeLegacyNestedPlaintextPayload() {
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setMessageType("property");

        DeviceUpMessage message = adapter.decode("""
                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2,"gpsTotalX":9.9,"gpsTotalY":0.5}}}}
                """.getBytes(StandardCharsets.UTF_8), context);

        assertEquals("17165802", message.getDeviceCode());
        assertEquals("property", message.getMessageType());
        assertEquals(3.2, message.getProperties().get("L1_GP_1.gpsTotalZ"));
        assertEquals(9.9, message.getProperties().get("L1_GP_1.gpsTotalX"));
    }

    @Test
    void shouldRejectEncryptedPayloadWhenDecryptorIsMissing() {
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setMessageType("property");

        BizException ex = assertThrows(BizException.class, () -> adapter.decode("""
                {"header":{"appId":"62000001"},"bodies":{"body":"PTOLy04o/stDufUYFo5s3g=="}} 
                """.getBytes(StandardCharsets.UTF_8), context));

        assertEquals("检测到加密 MQTT 报文，但未配置 appId 对应的解密器: 62000001", ex.getMessage());
    }

    @Test
    void shouldDecodeType2TimestampScalarPayload() {
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setMessageType("property");

        byte[] packet = buildPacket((byte) 2, """
                {"484021":{"L1_LF_1":{"2018-08-02T08:52:32.449Z":11.2,"2018-08-02T10:52:32.449Z":10.9},"L4_NW_1":{"2018-08-02T09:02:32.449Z":36.5}}}
                """);

        DeviceUpMessage message = adapter.decode(packet, context);

        assertEquals("484021", message.getDeviceCode());
        assertEquals("10.9", String.valueOf(message.getProperties().get("L1_LF_1")));
        assertEquals("36.5", String.valueOf(message.getProperties().get("L4_NW_1")));
    }

    @Test
    void shouldSplitConfiguredSubDevicesFromLegacyDeepDisplacementPayload() {
        IotProperties properties = new IotProperties();
        IotProperties.Device deviceConfig = new IotProperties.Device();
        Map<String, String> baseStationMappings = new LinkedHashMap<>();
        baseStationMappings.put("L1_SW_1", "84330701");
        baseStationMappings.put("L1_SW_2", "84330695");
        deviceConfig.setSubDeviceMappings(Map.of("SK00FB0D1310195", baseStationMappings));
        properties.setDevice(deviceConfig);

        MqttJsonProtocolAdapter configuredAdapter = newAdapter(properties);
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setMessageType("property");

        DeviceUpMessage message = configuredAdapter.decode(buildPacket((byte) 2, """
                {"SK00FB0D1310195":{"L1_SW_1":{"2026-03-20T06:24:02.000Z":{"dispsX":-0.0445,"dispsY":0.0293}},"L1_SW_2":{"2026-03-20T06:24:02.000Z":{"dispsX":-0.0293,"dispsY":0.0330}}}}
                """), context);

        assertEquals("SK00FB0D1310195", message.getDeviceCode());
        assertEquals("property", message.getMessageType());
        assertNotNull(message.getChildMessages());
        assertEquals(2, message.getChildMessages().size());
        assertEquals("84330701", message.getChildMessages().get(0).getDeviceCode());
        assertEquals(-0.0445, message.getChildMessages().get(0).getProperties().get("dispsX"));
        assertEquals(0.0293, message.getChildMessages().get(0).getProperties().get("dispsY"));
        assertEquals("84330695", message.getChildMessages().get(1).getDeviceCode());
        assertEquals(-0.0293, message.getChildMessages().get(1).getProperties().get("dispsX"));
        assertEquals(0.0330, message.getChildMessages().get(1).getProperties().get("dispsY"));
        assertTrue(message.getProperties() == null || message.getProperties().isEmpty());
    }

    @Test
    void shouldDecodeType3FilePayloadIntoUnifiedModel() {
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");

        String descriptor = """
                {"did":"device-file-1","ds_id":"camera-image","file_type":"jpg","at":"2018-08-02T10:52:32.449Z","desc":"0-1-256"}
                """;
        byte[] binaryPayload = new byte[]{0x01, 0x02, 0x03, 0x04};

        DeviceUpMessage message = adapter.decode(buildType3Packet(descriptor, binaryPayload), context);

        assertEquals("device-file-1", message.getDeviceCode());
        assertEquals("file", message.getMessageType());
        assertNotNull(message.getFilePayload());
        assertEquals("device-file-1", message.getFilePayload().getDeviceId());
        assertEquals("camera-image", message.getFilePayload().getDataSetId());
        assertEquals("jpg", message.getFilePayload().getFileType());
        assertEquals(4, message.getFilePayload().getBinaryLength());
        assertArrayEquals(binaryPayload, message.getFilePayload().getBinaryPayload());
    }

    @Test
    void shouldDecodeType3FirmwarePayloadIntoUnifiedModel() {
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");

        String descriptor = """
                {"did":"device-ota-1","ds_id":"ota-firmware","file_type":"bin","at":"2018-08-02T10:52:32.449Z","desc":"0-2-1024"}
                """;
        byte[] firmwarePacket = buildFirmwarePacket(1, new byte[]{0x11, 0x22, 0x33}, 2, "abcd1234");

        DeviceUpMessage message = adapter.decode(buildType3Packet(descriptor, firmwarePacket), context);

        assertEquals("device-ota-1", message.getDeviceCode());
        assertEquals("firmware", message.getMessageType());
        assertNotNull(message.getFilePayload());
        assertNotNull(message.getFilePayload().getFirmwarePacket());
        assertEquals(1, message.getFilePayload().getFirmwarePacket().getPacketIndex());
        assertEquals(2, message.getFilePayload().getFirmwarePacket().getTotalPackets());
        assertEquals("abcd1234", message.getFilePayload().getFirmwarePacket().getFirmwareMd5());
        assertArrayEquals(new byte[]{0x11, 0x22, 0x33}, message.getFilePayload().getFirmwarePacket().getPacketData());
    }

    private byte[] buildPacket(byte type, String json) {
        byte[] jsonBytes = json.trim().getBytes(StandardCharsets.UTF_8);
        int length = jsonBytes.length;
        byte[] result = new byte[length + 3];
        result[0] = type;
        result[1] = (byte) ((length >> 8) & 0xFF);
        result[2] = (byte) (length & 0xFF);
        System.arraycopy(jsonBytes, 0, result, 3, length);
        return result;
    }

    private byte[] buildType3Packet(String descriptor, byte[] binaryPayload) {
        byte[] descriptorBytes = descriptor.trim().getBytes(StandardCharsets.UTF_8);
        int jsonLength = descriptorBytes.length;
        byte[] packet = new byte[3 + jsonLength + 2 + binaryPayload.length];
        packet[0] = 0x03;
        packet[1] = (byte) ((jsonLength >> 8) & 0xFF);
        packet[2] = (byte) (jsonLength & 0xFF);
        System.arraycopy(descriptorBytes, 0, packet, 3, jsonLength);
        int binaryLengthIndex = 3 + jsonLength;
        packet[binaryLengthIndex] = (byte) ((binaryPayload.length >> 8) & 0xFF);
        packet[binaryLengthIndex + 1] = (byte) (binaryPayload.length & 0xFF);
        System.arraycopy(binaryPayload, 0, packet, binaryLengthIndex + 2, binaryPayload.length);
        return packet;
    }

    private byte[] buildFirmwarePacket(int packetIndex, byte[] packetData, int totalPackets, String firmwareMd5) {
        byte[] md5Bytes = firmwareMd5 == null ? new byte[0] : firmwareMd5.getBytes(StandardCharsets.UTF_8);
        byte[] packet = new byte[6 + packetData.length + 2 + md5Bytes.length];
        packet[0] = (byte) ((packetIndex >> 8) & 0xFF);
        packet[1] = (byte) (packetIndex & 0xFF);
        packet[2] = (byte) ((packetData.length >> 8) & 0xFF);
        packet[3] = (byte) (packetData.length & 0xFF);
        packet[4] = (byte) ((totalPackets >> 8) & 0xFF);
        packet[5] = (byte) (totalPackets & 0xFF);
        System.arraycopy(packetData, 0, packet, 6, packetData.length);
        int md5LengthIndex = 6 + packetData.length;
        packet[md5LengthIndex] = (byte) ((md5Bytes.length >> 8) & 0xFF);
        packet[md5LengthIndex + 1] = (byte) (md5Bytes.length & 0xFF);
        System.arraycopy(md5Bytes, 0, packet, md5LengthIndex + 2, md5Bytes.length);
        return packet;
    }

    private MqttJsonProtocolAdapter newAdapter(IotProperties iotProperties) {
        return new MqttJsonProtocolAdapter(
                new MqttPayloadDecryptorRegistry(List.of()),
                new MqttPayloadFrameParser(),
                new MqttPayloadSecurityValidator(
                        iotProperties,
                        new MqttMessageSignerRegistry(List.of()),
                        new DefaultListableBeanFactory().getBeanProvider(org.springframework.data.redis.core.StringRedisTemplate.class)
                ),
                new MqttFirmwarePacketParser(),
                iotProperties
        );
    }
}
