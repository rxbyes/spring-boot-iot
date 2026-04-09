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
    void shouldDecryptDesPayloadWithLegacyDecryptorBridge() throws Exception {
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
                legacyDecryptor("des-app", "DES", new DesMqttPayloadDecryptor(properties))
        ));

        assertArrayEquals(plaintext, registry.decryptBytesOrThrow("des-app", encryptedBody));
    }

    @Test
    void shouldDecryptTripleDesPayloadWithLegacyDecryptorBridge() throws Exception {
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
                legacyDecryptor("3des-app", "DESede", new TripleDesMqttPayloadDecryptor(properties))
        ));

        assertArrayEquals(plaintext, registry.decryptBytesOrThrow("3des-app", encryptedBody));
    }

    @Test
    void shouldRouteByResolvedProfileAlgorithm() {
        ProtocolDecryptProfileResolver resolver =
                context -> profile("des-62000001", "DES", "IOT_PROTOCOL_CRYPTO", "62000001");
        ProtocolDecryptExecutor desExecutor =
                new StubExecutor("DES", "ok".getBytes(StandardCharsets.UTF_8));

        MqttPayloadDecryptorRegistry registry = new MqttPayloadDecryptorRegistry(resolver, List.of(desExecutor));

        byte[] decrypted = registry.decryptBytesOrThrow(new ProtocolDecryptResolveContext(
                "62000001",
                "mqtt-json",
                List.of("legacy-dp-gnss")
        ), "cipher-text");

        assertArrayEquals("ok".getBytes(StandardCharsets.UTF_8), decrypted);
    }

    @Test
    void shouldKeepLegacyAppIdOverloadForExistingDecoderPath() {
        ProtocolDecryptProfileResolver resolver =
                context -> profile("aes-62000000", "AES", "SPRING_CLOUD_AES", "62000000");
        ProtocolDecryptExecutor aesExecutor =
                new StubExecutor("AES", "legacy".getBytes(StandardCharsets.UTF_8));

        MqttPayloadDecryptorRegistry registry = new MqttPayloadDecryptorRegistry(resolver, List.of(aesExecutor));

        byte[] decrypted = registry.decryptBytesOrThrow("62000000", "cipher-text");

        assertArrayEquals("legacy".getBytes(StandardCharsets.UTF_8), decrypted);
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

    private MqttPayloadDecryptor legacyDecryptor(String appId,
                                                 String algorithm,
                                                 ProtocolDecryptExecutor executor) {
        ProtocolDecryptProfile profile = profile(algorithm + "-" + appId, algorithm, "TEST", appId);
        return new MqttPayloadDecryptor() {
            @Override
            public boolean supports(String candidateAppId) {
                return appId.equals(candidateAppId);
            }

            @Override
            public byte[] decryptBytes(String candidateAppId, String encryptedBody) {
                return executor.decryptBytes(profile, encryptedBody);
            }
        };
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
}
