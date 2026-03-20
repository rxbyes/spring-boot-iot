package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MqttPayloadSecurityValidatorTest {

    private final IotProperties iotProperties = buildIotProperties();

    private final MqttPayloadSecurityValidator validator = new MqttPayloadSecurityValidator(
            iotProperties,
            new MqttMessageSignerRegistry(List.of(
                    new MqttMessageSigner() {
                        @Override
                        public boolean supports(String algorithm) {
                            return "TEST".equalsIgnoreCase(algorithm);
                        }

                        @Override
                    public String sign(String appId, String content) {
                        return "SIGNED:" + appId + ":" + content;
                    }
                },
                    new Md5MqttMessageSigner(iotProperties)
            )),
            new DefaultListableBeanFactory().getBeanProvider(StringRedisTemplate.class)
    );

    @Test
    void shouldValidateSignedEnvelope() {
        String appId = "62000001";
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String nonce = "nonce-1";
        String body = "{\"temperature\":25.1}";
        String signContent = validator.buildSignContent(appId, timestamp, nonce, body);
        String signature = "SIGNED:" + appId + ":" + signContent;

        Map<String, Object> payload = Map.of(
                "header", Map.of(
                        "appId", appId,
                        "timestamp", timestamp,
                        "nonce", nonce,
                        "signAlgorithm", "TEST",
                        "signature", signature
                ),
                "bodies", Map.of("body", body)
        );

        assertDoesNotThrow(() -> validator.validateEnvelope(appId, payload, body));
    }

    @Test
    void shouldRejectReplayEnvelope() {
        String appId = "62000001";
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String nonce = "nonce-2";
        String body = "{\"temperature\":26.3}";
        String signContent = validator.buildSignContent(appId, timestamp, nonce, body);
        String signature = "SIGNED:" + appId + ":" + signContent;
        Map<String, Object> payload = Map.of(
                "header", Map.of(
                        "appId", appId,
                        "timestamp", timestamp,
                        "nonce", nonce,
                        "signAlgorithm", "TEST",
                        "signature", signature
                ),
                "bodies", Map.of("body", body)
        );

        validator.validateEnvelope(appId, payload, body);
        BizException ex = assertThrows(BizException.class, () -> validator.validateEnvelope(appId, payload, body));
        assertEquals("检测到重复报文", ex.getMessage());
    }

    @Test
    void shouldRejectExpiredTimestamp() {
        String appId = "62000001";
        String timestamp = String.valueOf(Instant.now().minusSeconds(601).toEpochMilli());
        String nonce = "nonce-3";
        String body = "{\"temperature\":27.8}";
        String signContent = validator.buildSignContent(appId, timestamp, nonce, body);
        String signature = "SIGNED:" + appId + ":" + signContent;
        Map<String, Object> payload = Map.of(
                "header", Map.of(
                        "appId", appId,
                        "timestamp", timestamp,
                        "nonce", nonce,
                        "signAlgorithm", "TEST",
                        "signature", signature
                ),
                "bodies", Map.of("body", body)
        );

        BizException ex = assertThrows(BizException.class, () -> validator.validateEnvelope(appId, payload, body));
        assertEquals("timestamp 超出允许时间窗", ex.getMessage());
    }

    @Test
    void shouldSupportMd5Signer() {
        String appId = "legacy-app";
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String nonce = "nonce-md5";
        String body = "{\"humidity\":61}";
        String signContent = validator.buildSignContent(appId, timestamp, nonce, body);
        String signature = new Md5MqttMessageSigner(iotProperties).sign(appId, signContent);
        Map<String, Object> payload = Map.of(
                "header", Map.of(
                        "appId", appId,
                        "timestamp", timestamp,
                        "nonce", nonce,
                        "signAlgorithm", "MD5",
                        "signature", signature
                ),
                "bodies", Map.of("body", body)
        );

        assertDoesNotThrow(() -> validator.validateEnvelope(appId, payload, body));
    }

    @Test
    void shouldSupportByteStreamSignature() {
        MqttMessageSignerRegistry signerRegistry = new MqttMessageSignerRegistry(List.of(
                new MqttMessageSigner() {
                    @Override
                    public boolean supports(String algorithm) {
                        return "TEST-BYTES".equalsIgnoreCase(algorithm);
                    }

                    @Override
                    public String sign(String appId, String content) {
                        return "B:" + appId + ":" + content;
                    }
                }
        ));

        String signature = signerRegistry.signBytes("TEST-BYTES", "62000001", new byte[]{0x01, 0x02, 0x03});
        assertEquals("B:62000001:AQID", signature);
    }

    private IotProperties buildIotProperties() {
        IotProperties properties = new IotProperties();
        IotProperties.Protocol.Crypto.Merchant merchant = new IotProperties.Protocol.Crypto.Merchant();
        merchant.setSignatureSecret("legacy-secret");
        merchant.setSignatureJoinMode("KEY_SUFFIX");
        properties.getProtocol().getCrypto().getMerchants().put("legacy-app", merchant);
        return properties;
    }
}
