package com.ghlzm.iot.protocol.mqtt;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceUpProtocolMetadata;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpChildMessageSplitter;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpFamilyResolver;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpNormalizeResult;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpPropertyNormalizer;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MqttJsonProtocolAdapterTest {

    private final MqttJsonProtocolAdapter adapter = newAdapter(new IotProperties());

    @Test
    void shouldBeCreatableAsSpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(IotProperties.class, IotProperties::new);
            context.registerBean(MqttPayloadDecryptorRegistry.class, () -> new MqttPayloadDecryptorRegistry(List.of()));
            context.registerBean(MqttPayloadFrameParser.class, MqttPayloadFrameParser::new);
            context.registerBean(MqttFirmwarePacketParser.class, MqttFirmwarePacketParser::new);
            context.registerBean(MqttMessageSignerRegistry.class, () -> new MqttMessageSignerRegistry(List.of()));
            context.registerBean(MqttPayloadSecurityValidator.class, () -> new MqttPayloadSecurityValidator(
                    context.getBean(IotProperties.class),
                    context.getBean(MqttMessageSignerRegistry.class),
                    new DefaultListableBeanFactory().getBeanProvider(org.springframework.data.redis.core.StringRedisTemplate.class)
            ));
            context.registerBean(MqttJsonProtocolAdapter.class);

            context.refresh();

            assertNotNull(context.getBean(MqttJsonProtocolAdapter.class));
        }
    }

    @Test
    void shouldNotRetainDuplicatedLegacyEnvelopeHelpersAfterExtraction() {
        Set<String> declaredMethodNames = Arrays.stream(MqttJsonProtocolAdapter.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertFalse(declaredMethodNames.contains("decodePayload"));
        assertFalse(declaredMethodNames.contains("enrichByDataFormat"));
        assertFalse(declaredMethodNames.contains("buildRawPayloadForLog"));
        assertFalse(declaredMethodNames.contains("buildFilePayload"));
        assertFalse(declaredMethodNames.contains("toFirmwarePacket"));
        assertFalse(declaredMethodNames.contains("sanitizePayload"));
        assertFalse(declaredMethodNames.contains("isEncryptedEnvelope"));
        assertFalse(declaredMethodNames.contains("extractAppId"));
        assertFalse(declaredMethodNames.contains("extractEncryptedBody"));
        assertTrue(Arrays.stream(MqttJsonProtocolAdapter.class.getDeclaredClasses())
                .noneMatch(type -> "DecodedPayload".equals(type.getSimpleName())));
    }

    @Test
    void shouldDecodeLegacyNestedPlaintextPayload() {
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setTopicRouteType("legacy");
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
    void shouldAttachLegacyDpProtocolMetadataForPlaintextPayload() {
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setTopicRouteType("legacy");
        context.setMessageType("property");

        DeviceUpMessage message = adapter.decode(buildPacket((byte) 2, """
                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2,"gpsTotalX":9.9,"gpsTotalY":0.5}}}}
                """), context);

        DeviceUpProtocolMetadata metadata = message.getProtocolMetadata();
        assertNotNull(metadata);
        assertEquals("legacy", metadata.getRouteType());
        assertEquals("LEGACY_DP", metadata.getNormalizationStrategy());
        assertEquals("PAYLOAD_LATEST_TIMESTAMP", metadata.getTimestampSource());
        assertEquals(List.of("L1_GP_1"), metadata.getFamilyCodes());
        assertFalse(Boolean.TRUE.equals(metadata.getChildSplitApplied()));
    }

    @Test
    void shouldCaptureAppIdInProtocolMetadataForEncryptedPayload() {
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setTopicRouteType("legacy");
        context.setMessageType("property");

        MqttJsonProtocolAdapter decryptingAdapter = newAdapter(
                new IotProperties(),
                List.of(new MqttPayloadDecryptor() {
                    @Override
                    public boolean supports(String appId) {
                        return "62000001".equals(appId);
                    }

                    @Override
                    public byte[] decryptBytes(String appId, String encryptedBody) {
                        return buildPacket((byte) 2, """
                                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2}}}}
                                """);
                    }
                })
        );

        DeviceUpMessage message = decryptingAdapter.decode("""
                {"header":{"appId":"62000001"},"bodies":{"body":"cipher-text"}}
                """.getBytes(StandardCharsets.UTF_8), context);

        assertNotNull(message.getProtocolMetadata());
        assertEquals("62000001", message.getProtocolMetadata().getAppId());
        assertEquals(List.of("L1_GP_1"), message.getProtocolMetadata().getFamilyCodes());
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
    void shouldDecodeLegacyStatusPayloadWithStableKeys() {
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setTopicRouteType("legacy");
        context.setMessageType("property");

        DeviceUpMessage message = adapter.decode(buildPacket((byte) 2, """
                {"17165802":{"S1_ZT_1":{"2026-03-14T06:00:00.000Z":{"temp":16.2,"humidity":81.5,"sensor_state":"OK"}}}}
                """), context);

        assertEquals("17165802", message.getDeviceCode());
        assertEquals("status", message.getMessageType());
        assertEquals(16.2, message.getProperties().get("S1_ZT_1.temp"));
        assertEquals(81.5, message.getProperties().get("S1_ZT_1.humidity"));
        assertEquals("OK", message.getProperties().get("S1_ZT_1.sensor_state"));
        assertEquals(List.of("S1_ZT_1"), message.getProtocolMetadata().getFamilyCodes());
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
    void shouldKeepLegacyV1PathWhenNormalizerV2IsDisabled() {
        IotProperties properties = new IotProperties();
        properties.getProtocol().getLegacyDp().setNormalizerV2Enabled(false);

        MqttJsonProtocolAdapter configuredAdapter = newAdapter(
                properties,
                List.of(),
                new StubLegacyDpFamilyResolver(List.of("V2_FAMILY"), "status"),
                new StubLegacyDpPropertyNormalizer(new LegacyDpNormalizeResult(
                        List.of("V2_FAMILY"),
                        Map.of("v2Metric", 99),
                        "status",
                        LocalDateTime.of(2026, 3, 20, 10, 0),
                        "V2_TIMESTAMP"
                )),
                new LegacyDpChildMessageSplitter()
        );
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setTopicRouteType("legacy");
        context.setMessageType("property");

        DeviceUpMessage message = configuredAdapter.decode(buildPacket((byte) 2, """
                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2,"gpsTotalX":9.9}}}}
                """), context);

        assertEquals("property", message.getMessageType());
        assertEquals(3.2, message.getProperties().get("L1_GP_1.gpsTotalZ"));
        assertEquals(List.of("L1_GP_1"), message.getProtocolMetadata().getFamilyCodes());
        assertFalse(message.getProperties().containsKey("v2Metric"));
    }

    @Test
    void shouldValidateOnlyAgainstV2ButStillReturnLegacyV1Result() {
        IotProperties properties = new IotProperties();
        properties.getProtocol().getLegacyDp().setNormalizerV2Enabled(true);
        properties.getTelemetry().setLegacyMappingValidateOnly(true);

        MqttJsonProtocolAdapter configuredAdapter = newAdapter(
                properties,
                List.of(),
                new StubLegacyDpFamilyResolver(List.of("V2_FAMILY"), "status"),
                new StubLegacyDpPropertyNormalizer(new LegacyDpNormalizeResult(
                        List.of("V2_FAMILY"),
                        Map.of("v2Metric", 99),
                        "status",
                        LocalDateTime.of(2026, 3, 20, 10, 0),
                        "V2_TIMESTAMP"
                )),
                new LegacyDpChildMessageSplitter()
        );
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setTopicRouteType("legacy");
        context.setMessageType("property");

        Logger logger = (Logger) LoggerFactory.getLogger(MqttJsonProtocolAdapter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        Level originalLevel = logger.getLevel();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
        try {
            DeviceUpMessage message = configuredAdapter.decode(buildPacket((byte) 2, """
                    {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2,"gpsTotalX":9.9}}}}
                    """), context);

            assertEquals("property", message.getMessageType());
            assertEquals(3.2, message.getProperties().get("L1_GP_1.gpsTotalZ"));
            assertEquals(List.of("L1_GP_1"), message.getProtocolMetadata().getFamilyCodes());
            assertTrue(appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(log -> log.contains("legacy_dp_normalizer_validation_diff")));
        } finally {
            logger.setLevel(originalLevel);
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void shouldSwitchToV2PathWhenNormalizerV2IsEnabled() {
        IotProperties properties = new IotProperties();
        properties.getProtocol().getLegacyDp().setNormalizerV2Enabled(true);

        MqttJsonProtocolAdapter configuredAdapter = newAdapter(
                properties,
                List.of(),
                new StubLegacyDpFamilyResolver(List.of("V2_FAMILY"), "status"),
                new StubLegacyDpPropertyNormalizer(new LegacyDpNormalizeResult(
                        List.of("V2_FAMILY"),
                        Map.of("v2Metric", 99),
                        "status",
                        LocalDateTime.of(2026, 3, 20, 10, 0),
                        "V2_TIMESTAMP"
                )),
                new LegacyDpChildMessageSplitter()
        );
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setTopicRouteType("legacy");
        context.setMessageType("property");

        DeviceUpMessage message = configuredAdapter.decode(buildPacket((byte) 2, """
                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2,"gpsTotalX":9.9}}}}
                """), context);

        assertEquals("status", message.getMessageType());
        assertEquals(99, message.getProperties().get("v2Metric"));
        assertEquals(List.of("V2_FAMILY"), message.getProtocolMetadata().getFamilyCodes());
        assertEquals("V2_TIMESTAMP", message.getProtocolMetadata().getTimestampSource());
    }

    @Test
    void shouldSuppressLegacyProtocolMetadataWhenFamilyObservabilityIsDisabled() {
        IotProperties properties = new IotProperties();
        properties.getProtocol().getLegacyDp().setFamilyObservabilityEnabled(false);
        MqttJsonProtocolAdapter configuredAdapter = newAdapter(properties);

        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setTopicRouteType("legacy");
        context.setMessageType("property");

        DeviceUpMessage message = configuredAdapter.decode(buildPacket((byte) 2, """
                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2,"gpsTotalX":9.9}}}}
                """), context);

        assertNull(message.getProtocolMetadata());
        assertEquals(3.2, message.getProperties().get("L1_GP_1.gpsTotalZ"));
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

    private MqttJsonProtocolAdapter newAdapter(IotProperties iotProperties,
                                               List<MqttPayloadDecryptor> decryptors) {
        return newAdapter(
                iotProperties,
                decryptors,
                new LegacyDpFamilyResolver(),
                new LegacyDpPropertyNormalizer(),
                new LegacyDpChildMessageSplitter()
        );
    }

    private MqttJsonProtocolAdapter newAdapter(IotProperties iotProperties,
                                               List<MqttPayloadDecryptor> decryptors,
                                               LegacyDpFamilyResolver familyResolver,
                                               LegacyDpPropertyNormalizer propertyNormalizer,
                                               LegacyDpChildMessageSplitter childMessageSplitter) {
        return new MqttJsonProtocolAdapter(
                new MqttPayloadDecryptorRegistry(decryptors),
                new MqttPayloadFrameParser(),
                new MqttPayloadSecurityValidator(
                        iotProperties,
                        new MqttMessageSignerRegistry(List.of()),
                        new DefaultListableBeanFactory().getBeanProvider(org.springframework.data.redis.core.StringRedisTemplate.class)
                ),
                new MqttFirmwarePacketParser(),
                iotProperties,
                familyResolver,
                propertyNormalizer,
                childMessageSplitter
        );
    }

    private static final class StubLegacyDpFamilyResolver extends LegacyDpFamilyResolver {

        private final List<String> familyCodes;
        private final String messageType;

        private StubLegacyDpFamilyResolver(List<String> familyCodes, String messageType) {
            this.familyCodes = familyCodes;
            this.messageType = messageType;
        }

        @Override
        public List<String> resolveFamilyCodes(Map<String, Object> payload, String resolvedDeviceCode) {
            return familyCodes;
        }

        @Override
        public String inferMessageType(Map<String, Object> payload,
                                       String resolvedDeviceCode,
                                       List<String> familyCodes) {
            return messageType;
        }
    }

    private static final class StubLegacyDpPropertyNormalizer extends LegacyDpPropertyNormalizer {

        private final LegacyDpNormalizeResult result;

        private StubLegacyDpPropertyNormalizer(LegacyDpNormalizeResult result) {
            this.result = result;
        }

        @Override
        public LegacyDpNormalizeResult normalize(Map<String, Object> payload,
                                                 String resolvedDeviceCode,
                                                 List<String> familyCodes) {
            return result;
        }
    }
}
