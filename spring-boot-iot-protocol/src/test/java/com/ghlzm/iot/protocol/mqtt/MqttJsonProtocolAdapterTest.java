package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpEnvelopeDecoder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void shouldIgnoreTrailingJunkAfterEncryptedEnvelopeJson() {
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setMessageType("property");

        BizException ex = assertThrows(BizException.class, () -> adapter.decode("""
                {"header":{"appId":"62000001"},"bodies":{"body":"PTOLy04o/stDufUYFo5s3g=="}}}
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
    void shouldKeepStablePropertyNamesForCoreLegacyFamilies() {
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setTopicRouteType("legacy");
        context.setMessageType("property");

        DeviceUpMessage message = adapter.decode(buildPacket((byte) 2, """
                {"GW001":{"S1_ZT_1":{"2026-03-20T06:24:02.000Z":{"ext_power_volt":12.3,"sensor_state":1}},"L1_GP_1":{"2026-03-20T06:24:02.000Z":{"gpsTotalZ":3.2,"gpsTotalX":9.9}},"L1_QJ_1":{"2026-03-20T06:24:02.000Z":{"X":1.2,"Y":-0.4}},"L1_JS_1":{"2026-03-20T06:24:02.000Z":{"gX":0.11,"gY":0.22}},"L1_SW_1":{"2026-03-20T06:24:02.000Z":{"dispsX":-0.0445,"dispsY":0.0293}}}}
                """), context);

        assertEquals("status", message.getMessageType());
        assertEquals(12.3, message.getProperties().get("S1_ZT_1.ext_power_volt"));
        assertEquals(3.2, message.getProperties().get("L1_GP_1.gpsTotalZ"));
        assertEquals(1.2, message.getProperties().get("L1_QJ_1.X"));
        assertEquals(0.11, message.getProperties().get("L1_JS_1.gX"));
        assertEquals(-0.0445, message.getProperties().get("L1_SW_1.dispsX"));
    }

    @Test
    void shouldExposeProtocolMetadataForEncryptedLegacyDpPayload() {
        IotProperties properties = new IotProperties();
        properties.getProtocol().getLegacyDp().setNormalizerV2Enabled(true);
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setTopicRouteType("legacy");
        context.setMessageType("property");

        MqttJsonProtocolAdapter configuredAdapter = newAdapter(
                properties,
                List.of(new StubDecryptor(
                        "62000001",
                        buildPacket((byte) 2, """
                                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2,"gpsTotalX":9.9}},"L4_NW_1":{"2026-03-14T06:00:00.000Z":36.5}}}
                                """)
                ))
        );

        DeviceUpMessage message = configuredAdapter.decode("""
                {"header":{"appId":"62000001"},"bodies":{"body":"cipher-text"}}
                """.getBytes(StandardCharsets.UTF_8), context);

        Object protocolMetadata = getProtocolMetadata(message);
        assertNotNull(protocolMetadata);
        assertEquals("62000001", readMetadata(protocolMetadata, "getAppId"));
        assertEquals(List.of("L1_GP_1", "L4_NW_1"), readMetadata(protocolMetadata, "getFamilyCodes"));
        assertEquals("LEGACY_DP", readMetadata(protocolMetadata, "getNormalizationStrategy"));
        assertEquals(Boolean.FALSE, readMetadata(protocolMetadata, "getChildSplitApplied"));
        assertEquals("PAYLOAD_TIMESTAMP", readMetadata(protocolMetadata, "getTimestampSource"));
        assertEquals("legacy", readMetadata(protocolMetadata, "getRouteType"));
    }

    @Test
    void shouldExposePayloadComparisonMetadataForEncryptedLegacyDpPayload() {
        IotProperties properties = new IotProperties();
        properties.getProtocol().getLegacyDp().setNormalizerV2Enabled(true);
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setTopicRouteType("legacy");
        context.setMessageType("property");

        MqttJsonProtocolAdapter configuredAdapter = newAdapter(
                properties,
                List.of(new StubDecryptor(
                        "62000001",
                        buildPacket((byte) 2, """
                                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2,"gpsTotalX":9.9}},"L4_NW_1":{"2026-03-14T06:00:00.000Z":36.5}}}
                                """)
                ))
        );

        DeviceUpMessage message = configuredAdapter.decode("""
                {"header":{"appId":"62000001"},"bodies":{"body":"cipher-text"}}
                """.getBytes(StandardCharsets.UTF_8), context);

        Object protocolMetadata = getProtocolMetadata(message);
        assertTrue(String.valueOf(readMetadata(protocolMetadata, "getDecryptedPayloadPreview")).contains("\"17165802\""));

        @SuppressWarnings("unchecked")
        Map<String, Object> decodedPreview = (Map<String, Object>) readMetadata(protocolMetadata, "getDecodedPayloadPreview");
        assertEquals("property", decodedPreview.get("messageType"));
        assertEquals("17165802", decodedPreview.get("deviceCode"));
        assertTrue(decodedPreview.containsKey("properties"));
    }

    @Test
    void shouldExposePayloadComparisonMetadataForPlainLegacyDpPayload() {
        IotProperties properties = new IotProperties();
        properties.getProtocol().getLegacyDp().setNormalizerV2Enabled(true);
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setTopicRouteType("legacy");
        context.setMessageType("property");

        DeviceUpMessage message = newAdapter(properties).decode("""
                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2,"gpsTotalX":9.9}}}}
                """.getBytes(StandardCharsets.UTF_8), context);

        Object protocolMetadata = getProtocolMetadata(message);
        assertEquals(null, readMetadata(protocolMetadata, "getDecryptedPayloadPreview"));

        @SuppressWarnings("unchecked")
        Map<String, Object> decodedPreview = (Map<String, Object>) readMetadata(protocolMetadata, "getDecodedPayloadPreview");
        assertEquals("property", decodedPreview.get("messageType"));
        assertEquals("17165802", decodedPreview.get("deviceCode"));
        assertTrue(decodedPreview.containsKey("properties"));
    }

    @Test
    void shouldSuppressFamilyObservabilityMetadataWhenFlagDisabled() {
        IotProperties properties = new IotProperties();
        properties.getProtocol().getLegacyDp().setFamilyObservabilityEnabled(false);
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setTopicRouteType("legacy");
        context.setMessageType("property");

        DeviceUpMessage message = newAdapter(properties).decode(buildPacket((byte) 2, """
                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2,"gpsTotalX":9.9}},"L4_NW_1":{"2026-03-14T06:00:00.000Z":36.5}}}
                """), context);

        Object protocolMetadata = getProtocolMetadata(message);
        assertNotNull(protocolMetadata);
        assertEquals(null, readMetadata(protocolMetadata, "getFamilyCodes"));
        assertEquals(null, readMetadata(protocolMetadata, "getNormalizationStrategy"));
        assertEquals(null, readMetadata(protocolMetadata, "getTimestampSource"));
        assertEquals(null, readMetadata(protocolMetadata, "getChildSplitApplied"));
        assertEquals("legacy", readMetadata(protocolMetadata, "getRouteType"));
    }

    @Test
    void shouldExposeCompatStrategyWhenNormalizerV2Disabled() {
        IotProperties properties = new IotProperties();
        properties.getProtocol().getLegacyDp().setNormalizerV2Enabled(false);
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setTopicRouteType("legacy");
        context.setMessageType("property");

        DeviceUpMessage message = newAdapter(properties).decode(buildPacket((byte) 2, """
                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2,"gpsTotalX":9.9}}}}
                """), context);

        Object protocolMetadata = getProtocolMetadata(message);
        assertNotNull(protocolMetadata);
        assertEquals(List.of("L1_GP_1"), readMetadata(protocolMetadata, "getFamilyCodes"));
        assertEquals("LEGACY_DP_COMPAT", readMetadata(protocolMetadata, "getNormalizationStrategy"));
    }

    @Test
    void shouldSplitConfiguredSubDevicesFromLegacyDeepDisplacementPayload() {
        IotProperties properties = new IotProperties();
        properties.getProtocol().getLegacyDp().setNormalizerV2Enabled(true);
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
        Object protocolMetadata = getProtocolMetadata(message);
        assertNotNull(protocolMetadata);
        assertEquals(List.of("L1_SW_1", "L1_SW_2"), readMetadata(protocolMetadata, "getFamilyCodes"));
        assertEquals("LEGACY_DP", readMetadata(protocolMetadata, "getNormalizationStrategy"));
        assertEquals(Boolean.TRUE, readMetadata(protocolMetadata, "getChildSplitApplied"));
        assertEquals("PAYLOAD_TIMESTAMP", readMetadata(protocolMetadata, "getTimestampSource"));
        assertEquals("legacy", readMetadata(protocolMetadata, "getRouteType"));
    }

    @Test
    void shouldCollapseSingleDeepDisplacementLogicalPropertiesWithoutSubDeviceMappings() {
        IotProperties properties = new IotProperties();
        properties.getProtocol().getLegacyDp().setNormalizerV2Enabled(true);

        MqttJsonProtocolAdapter configuredAdapter = newAdapter(properties);
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setMessageType("property");

        DeviceUpMessage message = configuredAdapter.decode(buildPacket((byte) 2, """
                {"SK00EB0D1308310":{"L1_SW_1":{"2026-03-25T08:23:10.000Z":{"dispsX":-0.0445,"dispsY":0.0293}}}}
                """), context);

        assertEquals("SK00EB0D1308310", message.getDeviceCode());
        assertEquals("property", message.getMessageType());
        assertTrue(message.getChildMessages() == null || message.getChildMessages().isEmpty());
        assertEquals(-0.0445, message.getProperties().get("dispsX"));
        assertEquals(0.0293, message.getProperties().get("dispsY"));
        assertFalse(message.getProperties().containsKey("L1_SW_1.dispsX"));
        assertFalse(message.getProperties().containsKey("L1_SW_1.dispsY"));
        Object protocolMetadata = getProtocolMetadata(message);
        assertNotNull(protocolMetadata);
        assertEquals(List.of("L1_SW_1"), readMetadata(protocolMetadata, "getFamilyCodes"));
        assertEquals("LEGACY_DP", readMetadata(protocolMetadata, "getNormalizationStrategy"));
        assertEquals(Boolean.FALSE, readMetadata(protocolMetadata, "getChildSplitApplied"));
        assertEquals("PAYLOAD_TIMESTAMP", readMetadata(protocolMetadata, "getTimestampSource"));
        assertEquals("legacy", readMetadata(protocolMetadata, "getRouteType"));
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
        return newAdapter(iotProperties, List.of());
    }

    private MqttJsonProtocolAdapter newAdapter(IotProperties iotProperties, List<MqttPayloadDecryptor> decryptors) {
        LegacyDpEnvelopeDecoder envelopeDecoder = new LegacyDpEnvelopeDecoder(
                new MqttPayloadDecryptorRegistry(decryptors),
                new MqttPayloadFrameParser(),
                new MqttPayloadSecurityValidator(
                        iotProperties,
                        new MqttMessageSignerRegistry(List.of()),
                        new DefaultListableBeanFactory().getBeanProvider(org.springframework.data.redis.core.StringRedisTemplate.class)
                ),
                new MqttFirmwarePacketParser()
        );
        return new MqttJsonProtocolAdapter(
                envelopeDecoder,
                iotProperties
        );
    }

    private Object getProtocolMetadata(DeviceUpMessage message) {
        return invokeMethod(message, "getProtocolMetadata");
    }

    private Object readMetadata(Object metadata, String getterName) {
        return invokeMethod(metadata, getterName);
    }

    private Object invokeMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Expected method " + methodName + " on " + target.getClass().getName(), ex);
        }
    }

    private static final class StubDecryptor implements MqttPayloadDecryptor {

        private final String supportedAppId;
        private final byte[] decryptedPayload;

        private StubDecryptor(String supportedAppId, byte[] decryptedPayload) {
            this.supportedAppId = supportedAppId;
            this.decryptedPayload = decryptedPayload;
        }

        @Override
        public boolean supports(String appId) {
            return supportedAppId.equals(appId);
        }

        @Override
        public byte[] decryptBytes(String appId, String encryptedBody) {
            return decryptedPayload;
        }
    }
}
