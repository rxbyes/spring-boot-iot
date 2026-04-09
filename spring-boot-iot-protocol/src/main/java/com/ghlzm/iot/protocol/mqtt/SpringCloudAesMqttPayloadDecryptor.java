package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlxk.cloud.aes.core.AesEncryptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 基于 spring.cloud.aes 配置的 MQTT 解密器。
 * 负责按 header.appId 选择对应厂商密钥对象解密 body 中的密文。
 */
@Component
public class SpringCloudAesMqttPayloadDecryptor implements ProtocolDecryptExecutor {

    private final Map<String, AesEncryptor> aesEncryptors;

    public SpringCloudAesMqttPayloadDecryptor(@Qualifier("aesEncryptors")
                                              ObjectProvider<Map<String, AesEncryptor>> aesEncryptorsProvider) {
        this.aesEncryptors = aesEncryptorsProvider.getIfAvailable(Map::of);
    }

    @Override
    public boolean supports(String algorithm) {
        return "AES".equalsIgnoreCase(algorithm) || "AES-ENCRYPT".equalsIgnoreCase(algorithm);
    }

    @Override
    public byte[] decryptBytes(ProtocolDecryptProfile profile, String encryptedBody) {
        String merchantKey = profile == null ? null : profile.getMerchantKey();
        AesEncryptor aesEncryptor = aesEncryptors.get(merchantKey);
        if (aesEncryptor == null) {
            throw new BizException("未找到 merchant 对应的 AES 解密器: " + merchantKey);
        }
        return aesEncryptor.decryptByte(encryptedBody);
    }
}
