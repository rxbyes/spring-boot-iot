package com.ghlzm.iot.protocol.mqtt.legacy;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.mqtt.MqttDataFormatType;
import com.ghlzm.iot.protocol.mqtt.MqttFirmwarePacketParser;
import com.ghlzm.iot.protocol.mqtt.MqttMessageSignerRegistry;
import com.ghlzm.iot.protocol.mqtt.MqttPayloadDecryptor;
import com.ghlzm.iot.protocol.mqtt.MqttPayloadDecryptorRegistry;
import com.ghlzm.iot.protocol.mqtt.MqttPayloadFrameParser;
import com.ghlzm.iot.protocol.mqtt.MqttPayloadSecurityValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDpEnvelopeDecoderTest {

    private final LegacyDpEnvelopeDecoder decoder = newDecoder(new IotProperties(), List.of());

    @Test
    void shouldDecodePlaintextJsonPayload() throws Exception {
        LegacyDpEnvelopeDecoder.DecodedPayload decodedPayload = decoder.decode("""
                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2}}}}
                """.getBytes(StandardCharsets.UTF_8));

        assertEquals(MqttDataFormatType.DIRECT_JSON_COMPAT, decodedPayload.dataFormatType());
        assertNull(decodedPayload.appId());
        assertNotNull(castMap(decodedPayload.payload().get("17165802")).get("L1_GP_1"));
    }

    @Test
    void shouldDecodeControlPrefixedPlaintextJsonPayload() throws Exception {
        LegacyDpEnvelopeDecoder.DecodedPayload decodedPayload = decoder.decode(prefixControlByte("""
                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalX":9.9}}}}
                """));

        assertEquals(MqttDataFormatType.DIRECT_JSON_COMPAT, decodedPayload.dataFormatType());
        assertNull(decodedPayload.appId());
        assertTrue(decodedPayload.rawPayload().contains("\"gpsTotalX\":9.9"));
    }

    @Test
    void shouldDecodeEncryptedEnvelopeAndPreserveAppId() throws Exception {
        LegacyDpEnvelopeDecoder decryptingDecoder = newDecoder(
                new IotProperties(),
                List.of(new MqttPayloadDecryptor() {
                    @Override
                    public boolean supports(String appId) {
                        return "62000001".equals(appId);
                    }

                    @Override
                    public byte[] decryptBytes(String appId, String encryptedBody) {
                        return buildPacket((byte) 2, """
                                {"17165802":{"S1_ZT_1":{"2026-03-14T06:00:00.000Z":{"temp":16.2}}}}
                                """);
                    }
                })
        );

        LegacyDpEnvelopeDecoder.DecodedPayload decodedPayload = decryptingDecoder.decode("""
                {"header":{"appId":"62000001"},"bodies":{"body":"cipher-text"}}
                """.getBytes(StandardCharsets.UTF_8));

        assertEquals(MqttDataFormatType.STANDARD_TYPE_2, decodedPayload.dataFormatType());
        assertEquals("62000001", decodedPayload.appId());
        assertTrue(decodedPayload.rawPayload().contains("\"appId\":\"62000001\""));
        assertTrue(castMap(decodedPayload.payload().get("17165802")).containsKey("S1_ZT_1"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private byte[] prefixControlByte(String json) {
        byte[] jsonBytes = json.trim().getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[jsonBytes.length + 1];
        payload[0] = 0x10;
        System.arraycopy(jsonBytes, 0, payload, 1, jsonBytes.length);
        return payload;
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

    private LegacyDpEnvelopeDecoder newDecoder(IotProperties iotProperties,
                                               List<MqttPayloadDecryptor> decryptors) {
        return new LegacyDpEnvelopeDecoder(
                new MqttPayloadDecryptorRegistry(decryptors),
                new MqttPayloadFrameParser(),
                new MqttPayloadSecurityValidator(
                        iotProperties,
                        new MqttMessageSignerRegistry(List.of()),
                        new DefaultListableBeanFactory().getBeanProvider(org.springframework.data.redis.core.StringRedisTemplate.class)
                ),
                new MqttFirmwarePacketParser()
        );
    }
}
