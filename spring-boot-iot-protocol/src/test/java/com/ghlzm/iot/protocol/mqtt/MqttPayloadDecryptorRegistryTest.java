package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class MqttPayloadDecryptorRegistryTest {

    @Test
    void shouldDecryptDesPayload() throws Exception {
        IotProperties properties = new IotProperties();
        IotProperties.Protocol.Crypto.Merchant merchant = new IotProperties.Protocol.Crypto.Merchant();
        merchant.setAlgorithm("DES");
        merchant.setTransformation("DES/CBC/PKCS5Padding");
        merchant.setKey(Base64.getEncoder().encodeToString("12345678".getBytes(StandardCharsets.UTF_8)));
        merchant.setIv(Base64.getEncoder().encodeToString("12345678".getBytes(StandardCharsets.UTF_8)));
        properties.getProtocol().getCrypto().getMerchants().put("des-app", merchant);

        byte[] plaintext = "des-payload".getBytes(StandardCharsets.UTF_8);
        String encryptedBody = encrypt("DES/CBC/PKCS5Padding", "DES", "12345678".getBytes(StandardCharsets.UTF_8),
                "12345678".getBytes(StandardCharsets.UTF_8), plaintext);

        MqttPayloadDecryptorRegistry registry = new MqttPayloadDecryptorRegistry(List.of(
                new DesMqttPayloadDecryptor(properties),
                new TripleDesMqttPayloadDecryptor(properties)
        ));

        assertArrayEquals(plaintext, registry.decryptBytesOrThrow("des-app", encryptedBody));
    }

    @Test
    void shouldDecryptTripleDesPayload() throws Exception {
        IotProperties properties = new IotProperties();
        IotProperties.Protocol.Crypto.Merchant merchant = new IotProperties.Protocol.Crypto.Merchant();
        merchant.setAlgorithm("DESede");
        merchant.setTransformation("DESede/CBC/PKCS5Padding");
        merchant.setKey(Base64.getEncoder().encodeToString("123456789012345678901234".getBytes(StandardCharsets.UTF_8)));
        merchant.setIv(Base64.getEncoder().encodeToString("12345678".getBytes(StandardCharsets.UTF_8)));
        properties.getProtocol().getCrypto().getMerchants().put("3des-app", merchant);

        byte[] plaintext = "triple-des-payload".getBytes(StandardCharsets.UTF_8);
        String encryptedBody = encrypt("DESede/CBC/PKCS5Padding", "DESede",
                "123456789012345678901234".getBytes(StandardCharsets.UTF_8),
                "12345678".getBytes(StandardCharsets.UTF_8),
                plaintext);

        MqttPayloadDecryptorRegistry registry = new MqttPayloadDecryptorRegistry(List.of(
                new DesMqttPayloadDecryptor(properties),
                new TripleDesMqttPayloadDecryptor(properties)
        ));

        assertArrayEquals(plaintext, registry.decryptBytesOrThrow("3des-app", encryptedBody));
    }

    private String encrypt(String transformation,
                           String algorithm,
                           byte[] key,
                           byte[] iv,
                           byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, algorithm), new IvParameterSpec(iv));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext));
    }
}
