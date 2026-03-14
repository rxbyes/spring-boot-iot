package com.ghlzm.iot.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghlzm.iot.protocol.mqtt.MqttPayloadDecryptorRegistry;
import com.ghlzm.iot.protocol.mqtt.MqttPayloadFrameParser;
import com.ghlzm.iot.protocol.mqtt.SpringCloudAesMqttPayloadDecryptor;
import com.ghlxk.cloud.aes.config.AesAutoConfiguration;
import com.ghlxk.cloud.aes.core.AesEncryptor;
import com.ghlxk.cloud.aes.properties.AesProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldLoadMerchantEncryptorsFromDevProfile() {
        assertNotNull(aesProperties.getMerchants());
        assertTrue(aesProperties.getMerchants().containsKey("62000001"));
        assertTrue(aesEncryptors.containsKey("62000001"));
    }

    @Test
    void shouldParseFrameAndDecryptEncryptedEnvelope() throws Exception {
        String envelopeJson = """
                {"header":{"appId":"62000001"},"bodies":{"body":"8hBxs1xQYrHovuxfNvaZZOasvPiDJB8RgbozUIeVo3hVk70q0/Oaf3GmrhodAd6J0DloTmrcYcq+ieg9I95nBaU+Nr0Yjz/R63it6gePmfBNslGmii28Hgwp5pcj3R5I9Mh7JGsB8sJsxMbUqNShRAeLDtlKU+J1LE4S4rvne9Ab55DinF2u+f+ghUlkXuLUkMJzMx04GxgOo6zX85ADcEd/Et5LHZwLCWqtPh7sNJUwbJO4cCB66L33bCPjVCIynZfzczCb6qhhVZHh0q0uWdohmcCNFYrZkZhwgUDun5HvvVCL2Z7Plgqux6NiaMPW5MlNGBiVZ7sMlK3sUmKWdaerXsTghZ+8HIE3jP6DALCcTA9iphAmyxn7hW3/BAGwfyovt/y4iMMaT2Tf99atnRHqGeq61GELuaTlk0W3AVWIco7Z5XTHnXkYViPIw6/8qJ13EM/1CsWX0pDW6DPNq9wSLnZmXICtzl9VItZTdg4MDYxLoloB8PBdzjQiDgCaPBY/69FfRipmFvj6QvZW7xaudM2rjDfwpGhTg2i7jWA="}}
                """;
        byte[] packet = buildPacket((byte) 1, envelopeJson);

        MqttPayloadFrameParser.ParsedFrame parsedFrame = mqttPayloadFrameParser.parse("aes-test", packet);
        assertTrue(parsedFrame.framed());
        assertEquals(1, Byte.toUnsignedInt(parsedFrame.dataType()));
        assertEquals(envelopeJson.getBytes(StandardCharsets.UTF_8).length, parsedFrame.jsonLength());

        JsonNode root = objectMapper.readTree(parsedFrame.jsonMessage());
        String appId = root.path("header").path("appId").asText();
        String encryptedBody = root.path("bodies").path("body").asText();
        assertEquals("62000001", appId);
        assertTrue(aesEncryptors.containsKey(appId));

        MqttPayloadFrameParser.ParsedFrame decryptedFrame = mqttPayloadFrameParser.parse(
                "aes-test-decrypted",
                mqttPayloadDecryptorRegistry.decryptBytesOrThrow(appId, encryptedBody)
        );
        assertTrue(decryptedFrame.framed() || decryptedFrame.jsonMessage().startsWith("{"));
        JsonNode plaintextNode = objectMapper.readTree(decryptedFrame.jsonMessage());
        assertTrue(plaintextNode.isObject());
        assertTrue(plaintextNode.fieldNames().hasNext());
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
            SpringCloudAesMqttPayloadDecryptor.class,
            MqttPayloadDecryptorRegistry.class
    })
    static class TestApplication {
    }
}
