package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDpEnvelopeDecoderTest {

    @Test
    void shouldDecodePlaintextJsonPayload() {
        Object decoder = newDecoder(defaultResolver(), List.of());

        Object decoded = decode(decoder, """
                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2}}}}
                """.getBytes(StandardCharsets.UTF_8));

        Map<?, ?> payload = readPayload(decoded);
        assertEquals("DIRECT_JSON_COMPAT", readDataFormatType(decoded));
        assertEquals("DIRECT_JSON_COMPAT", payload.get("_dataFormatType"));
        assertTrue(payload.containsKey("17165802"));
        assertNull(readAppId(decoded));
        assertNull(readFilePayload(decoded));
    }

    @Test
    void shouldDecodePrefixedPlaintextJsonPayload() {
        Object decoder = newDecoder(defaultResolver(), List.of());

        byte[] payload = ("\u0010" + """
                {"100054920":{"S1_ZT_1":{"2026-03-14T06:00:00.000Z":{"ext_power_volt":12.3}}}}
                """).getBytes(StandardCharsets.UTF_8);

        Object decoded = decode(decoder, payload);

        Map<?, ?> decodedPayload = readPayload(decoded);
        assertEquals("DIRECT_JSON_COMPAT", readDataFormatType(decoded));
        assertEquals("DIRECT_JSON_COMPAT", decodedPayload.get("_dataFormatType"));
        assertTrue(decodedPayload.containsKey("100054920"));
        assertNull(readAppId(decoded));
    }

    @Test
    void shouldDecodeEncryptedEnvelopePayload() {
        DecoderHarness harness = newEncryptedDecoder(
                "62000001",
                buildPacket((byte) 2, """
                        {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2}}}}
                        """)
        );

        byte[] payload = """
                {"header":{"appId":"62000001"},"bodies":{"body":"cipher-text"}}
                """.getBytes(StandardCharsets.UTF_8);

        Object decoded = decode(harness.decoder(), payload);

        Map<?, ?> decodedPayload = readPayload(decoded);
        assertEquals("STANDARD_TYPE_2", readDataFormatType(decoded));
        assertEquals("STANDARD_TYPE_2", decodedPayload.get("_dataFormatType"));
        assertEquals("62000001", readAppId(decoded));
        assertNotNull(harness.registry().lastContext());
        assertEquals("62000001", harness.registry().lastContext().appId());
        assertEquals("mqtt-json", harness.registry().lastContext().protocolCode());
        assertEquals(List.of(), harness.registry().lastContext().familyCodes());
        assertTrue(String.valueOf(readRawPayload(decoded)).contains("\"appId\":\"62000001\""));
        assertNotNull(decodedPayload.get("17165802"));
    }

    @Test
    void shouldExposePlaintextPayloadForEncryptedEnvelope() {
        DecoderHarness harness = newEncryptedDecoder(
                "62000001",
                buildPacket((byte) 2, """
                        {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2}}}}
                        """)
        );

        Object decoded = decode(harness.decoder(), """
                {"header":{"appId":"62000001"},"bodies":{"body":"cipher-text"}}
                """.getBytes(StandardCharsets.UTF_8));

        assertTrue(String.valueOf(invoke(decoded, "plaintextPayload")).contains("\"17165802\""));
    }

    private Object newDecoder(ProtocolDecryptProfileResolver resolver, List<ProtocolDecryptExecutor> executors) {
        try {
            Class<?> decoderClass = Class.forName("com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpEnvelopeDecoder");
            Constructor<?> constructor = decoderClass.getConstructor(
                    MqttPayloadDecryptorRegistry.class,
                    MqttPayloadFrameParser.class,
                    MqttPayloadSecurityValidator.class,
                    MqttFirmwarePacketParser.class
            );
            IotProperties iotProperties = new IotProperties();
            return constructor.newInstance(
                    new MqttPayloadDecryptorRegistry(resolver, executors),
                    new MqttPayloadFrameParser(),
                    new MqttPayloadSecurityValidator(
                            iotProperties,
                            new MqttMessageSignerRegistry(List.of()),
                            new DefaultListableBeanFactory().getBeanProvider(org.springframework.data.redis.core.StringRedisTemplate.class)
                    ),
                    new MqttFirmwarePacketParser()
            );
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Expected LegacyDpEnvelopeDecoder class with the planned constructor", ex);
        }
    }

    private DecoderHarness newEncryptedDecoder(String appId, byte[] decryptedPayload) {
        ContextOnlyRegistry registry = new ContextOnlyRegistry(
                context -> profile("aes-" + appId, "AES", "SPRING_CLOUD_AES", appId),
                List.of(new StubExecutor("AES", decryptedPayload))
        );
        return new DecoderHarness(newDecoder(registry), registry);
    }

    private Object newDecoder(MqttPayloadDecryptorRegistry registry) {
        try {
            Class<?> decoderClass = Class.forName("com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpEnvelopeDecoder");
            Constructor<?> constructor = decoderClass.getConstructor(
                    MqttPayloadDecryptorRegistry.class,
                    MqttPayloadFrameParser.class,
                    MqttPayloadSecurityValidator.class,
                    MqttFirmwarePacketParser.class
            );
            IotProperties iotProperties = new IotProperties();
            return constructor.newInstance(
                    registry,
                    new MqttPayloadFrameParser(),
                    new MqttPayloadSecurityValidator(
                            iotProperties,
                            new MqttMessageSignerRegistry(List.of()),
                            new DefaultListableBeanFactory().getBeanProvider(org.springframework.data.redis.core.StringRedisTemplate.class)
                    ),
                    new MqttFirmwarePacketParser()
            );
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Expected LegacyDpEnvelopeDecoder class with the planned constructor", ex);
        }
    }

    private ProtocolDecryptProfileResolver defaultResolver() {
        return new IotPropertiesProtocolDecryptProfileResolver(new IotProperties());
    }

    private Object decode(Object decoder, byte[] payload) {
        try {
            Method method = decoder.getClass().getMethod("decode", byte[].class);
            return method.invoke(decoder, payload);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Expected LegacyDpEnvelopeDecoder.decode(byte[]) support", ex);
        }
    }

    private Map<?, ?> readPayload(Object decoded) {
        return (Map<?, ?>) invoke(decoded, "payload");
    }

    private Object readRawPayload(Object decoded) {
        return invoke(decoded, "rawPayload");
    }

    private String readDataFormatType(Object decoded) {
        Object dataFormatType = invoke(decoded, "dataFormatType");
        return dataFormatType == null ? null : String.valueOf(dataFormatType);
    }

    private Object readFilePayload(Object decoded) {
        return invoke(decoded, "filePayload");
    }

    private String readAppId(Object decoded) {
        Object appId = invoke(decoded, "appId");
        return appId == null ? null : String.valueOf(appId);
    }

    private Object invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Expected method " + methodName + " on " + target.getClass().getName(), ex);
        }
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

    private ProtocolDecryptProfile profile(String profileCode,
                                           String algorithm,
                                           String merchantSource,
                                           String merchantKey) {
        ProtocolDecryptProfile profile = new ProtocolDecryptProfile();
        profile.setProfileCode(profileCode);
        profile.setAlgorithm(algorithm);
        profile.setMerchantSource(merchantSource);
        profile.setMerchantKey(merchantKey);
        return profile;
    }

    private static final class StubExecutor implements ProtocolDecryptExecutor {

        private final String supportedAlgorithm;
        private final byte[] decryptedPayload;

        private StubExecutor(String supportedAlgorithm, byte[] decryptedPayload) {
            this.supportedAlgorithm = supportedAlgorithm;
            this.decryptedPayload = decryptedPayload;
        }

        @Override
        public boolean supports(String algorithm) {
            return supportedAlgorithm.equalsIgnoreCase(algorithm);
        }

        @Override
        public byte[] decryptBytes(ProtocolDecryptProfile profile, String encryptedBody) {
            return decryptedPayload;
        }
    }

    private record DecoderHarness(Object decoder, ContextOnlyRegistry registry) {
    }

    private static final class ContextOnlyRegistry extends MqttPayloadDecryptorRegistry {

        private ProtocolDecryptResolveContext lastContext;

        private ContextOnlyRegistry(ProtocolDecryptProfileResolver resolver,
                                    List<ProtocolDecryptExecutor> executors) {
            super(resolver, executors);
        }

        @Override
        public byte[] decryptBytesOrThrow(String appId, String encryptedBody) {
            throw new AssertionError("Expected LegacyDpEnvelopeDecoder to use ProtocolDecryptResolveContext overload");
        }

        @Override
        public byte[] decryptBytesOrThrow(ProtocolDecryptResolveContext context, String encryptedBody) {
            this.lastContext = context;
            return super.decryptBytesOrThrow(context, encryptedBody);
        }

        private ProtocolDecryptResolveContext lastContext() {
            return lastContext;
        }
    }
}
