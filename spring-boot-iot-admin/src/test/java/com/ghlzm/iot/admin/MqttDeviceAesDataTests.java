package com.ghlzm.iot.admin;

import com.ghlzm.iot.protocol.mqtt.Md5MqttMessageSigner;
import com.ghlzm.iot.protocol.mqtt.MqttMessageSignerRegistry;
import com.ghlzm.iot.protocol.mqtt.MqttPayloadDecryptorRegistry;
import com.ghlzm.iot.protocol.mqtt.MqttPayloadFrameParser;
import com.ghlzm.iot.protocol.mqtt.MqttPayloadSecurityValidator;
import com.ghlzm.iot.protocol.mqtt.SpringCloudAesMqttMessageSigner;
import com.ghlzm.iot.protocol.mqtt.SpringCloudAesMqttPayloadDecryptor;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlxk.cloud.aes.config.AesAutoConfiguration;
import com.ghlxk.cloud.aes.core.AesEncryptor;
import com.ghlxk.cloud.aes.properties.AesProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MQTT AES 加密报文流程测试。
 * 这里不依赖整个 admin 应用启动，而是只验证：
 * 1. dev 配置中的 AES 商户密钥能正确绑定
 * 2. 二进制帧头能正确解析
 * 3. header / bodies 能正确拆分
 * 4. appId 能正确找到对应解密器
 * 5. 解密后的 body 是完整 JSON
 */
@SpringBootTest(classes = MqttDeviceAesDataTests.TestApplication.class)
@ActiveProfiles("dev")
class MqttDeviceAesDataTests {

    @Autowired
    private AesProperties aesProperties;

    @Autowired
    @Qualifier("aesEncryptors")
    private Map<String, AesEncryptor> aesEncryptors;

    @Autowired
    private MqttPayloadFrameParser mqttPayloadFrameParser;

    @Autowired
    private MqttPayloadDecryptorRegistry mqttPayloadDecryptorRegistry;

    @Autowired
    private MqttMessageSignerRegistry mqttMessageSignerRegistry;

    @Autowired
    private MqttPayloadSecurityValidator mqttPayloadSecurityValidator;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void shouldLoadMerchantEncryptorsFromDevProfile() {
        assertNotNull(aesProperties.getMerchants());
        assertTrue(aesProperties.getMerchants().containsKey("62000001"));
        assertTrue(aesEncryptors.containsKey("62000001"));
    }

    @Test
    void shouldParseFrameAndDecryptEncryptedEnvelope() throws Exception {
        String appId = "62000001";
        String plaintextJson = """
                {"100054920":{"L1_QJ_1":{"2026-03-14T07:04:03.000Z":{"X":3.15,"Y":-5.14,"Z":83.97}}}}
                """;
        String encryptedBody = aesEncryptors.get(appId).encrypt(plaintextJson);
        String envelopeJson = """
                {"header":{"appId":"%s"},"bodies":{"body":"%s"}}
                """.formatted(appId, encryptedBody);
        byte[] packet = buildPacket((byte) 1, envelopeJson);

        MqttPayloadFrameParser.ParsedFrame parsedFrame = mqttPayloadFrameParser.parse("aes-test", packet);
        assertTrue(parsedFrame.framed());
        assertEquals(1, Byte.toUnsignedInt(parsedFrame.dataType()));
        assertEquals(envelopeJson.getBytes(StandardCharsets.UTF_8).length, parsedFrame.jsonLength());

        JsonNode root = objectMapper.readTree(parsedFrame.jsonMessage());
        appId = root.path("header").path("appId").asText();
        encryptedBody = root.path("bodies").path("body").asText();
        assertEquals("62000001", appId);
        assertTrue(aesEncryptors.containsKey(appId));

        MqttPayloadFrameParser.ParsedFrame decryptedFrame = mqttPayloadFrameParser.parse(
                "aes-test-decrypted",
                mqttPayloadDecryptorRegistry.decryptBytesOrThrow(appId, encryptedBody)
        );
        assertTrue(decryptedFrame.framed() || decryptedFrame.jsonMessage().startsWith("{"));
        JsonNode plaintextNode = objectMapper.readTree(decryptedFrame.jsonMessage());
        assertTrue(plaintextNode.isObject());
        assertTrue(plaintextNode.has("100054920"));
    }

    @Test
    void shouldValidateAesSignature() {
        String appId = "62000001";
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String nonce = "aes-sign-nonce";
        String body = "{\"temperature\":25.1}";
        String signContent = mqttPayloadSecurityValidator.buildSignContent(appId, timestamp, nonce, body);
        String signature = mqttMessageSignerRegistry.sign("AES", appId, signContent);

        Map<String, Object> payload = Map.of(
                "header", Map.of(
                        "appId", appId,
                        "timestamp", timestamp,
                        "nonce", nonce,
                        "signAlgorithm", "AES",
                        "signature", signature
                ),
                "bodies", Map.of("body", body)
        );

        assertDoesNotThrow(() -> mqttPayloadSecurityValidator.validateEnvelope(appId, payload, body));
    }

    private byte[] buildPacket(byte type, String json) {
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        int length = jsonBytes.length;
        byte highByte = (byte) ((length >> 8) & 0xFF);
        byte lowByte = (byte) (length & 0xFF);
        byte[] result = new byte[length + 3];
        result[0] = type;
        result[1] = highByte;
        result[2] = lowByte;
        System.arraycopy(jsonBytes, 0, result, 3, length);
        return result;
    }

    @SpringBootConfiguration
    @EnableConfigurationProperties(IotProperties.class)
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
            "org.redisson.spring.starter.RedissonAutoConfigurationV4",
            "com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration"
    })
    @Import({
            AesAutoConfiguration.class,
            MqttPayloadFrameParser.class,
            SpringCloudAesMqttMessageSigner.class,
            Md5MqttMessageSigner.class,
            MqttMessageSignerRegistry.class,
            SpringCloudAesMqttPayloadDecryptor.class,
            MqttPayloadDecryptorRegistry.class,
            MqttPayloadSecurityValidator.class
    })
    static class TestApplication {
    }
}
