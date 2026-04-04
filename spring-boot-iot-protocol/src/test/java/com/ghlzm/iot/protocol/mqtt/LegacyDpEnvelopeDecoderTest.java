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
        Object decoder = newDecoder(List.of());

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
        Object decoder = newDecoder(List.of());

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
        Object decoder = newDecoder(List.of(new StubDecryptor(
                "62000001",
                buildPacket((byte) 2, """
                        {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2}}}}
                        """)
        )));

        byte[] payload = """
                {"header":{"appId":"62000001"},"bodies":{"body":"cipher-text"}}
                """.getBytes(StandardCharsets.UTF_8);

        Object decoded = decode(decoder, payload);

        Map<?, ?> decodedPayload = readPayload(decoded);
        assertEquals("STANDARD_TYPE_2", readDataFormatType(decoded));
        assertEquals("STANDARD_TYPE_2", decodedPayload.get("_dataFormatType"));
        assertEquals("62000001", readAppId(decoded));
        assertTrue(String.valueOf(readRawPayload(decoded)).contains("\"appId\":\"62000001\""));
        assertNotNull(decodedPayload.get("17165802"));
    }

    @Test
    void shouldExposePlaintextPayloadForEncryptedEnvelope() {
        Object decoder = newDecoder(List.of(new StubDecryptor(
                "62000001",
                buildPacket((byte) 2, """
                        {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2}}}}
                        """)
        )));

        Object decoded = decode(decoder, """
                {"header":{"appId":"62000001"},"bodies":{"body":"cipher-text"}}
                """.getBytes(StandardCharsets.UTF_8));

        assertTrue(String.valueOf(invoke(decoded, "plaintextPayload")).contains("\"17165802\""));
    }

    private Object newDecoder(List<MqttPayloadDecryptor> decryptors) {
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
                    new MqttPayloadDecryptorRegistry(decryptors),
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
