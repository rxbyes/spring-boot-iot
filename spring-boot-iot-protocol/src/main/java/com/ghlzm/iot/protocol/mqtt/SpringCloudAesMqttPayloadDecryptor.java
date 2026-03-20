package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlxk.cloud.aes.core.AesEncryptor;
import com.ghlxk.cloud.aes.properties.AesProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 基于 spring.cloud.aes 配置的 MQTT 解密器。
 * 负责按 header.appId 选择对应厂商密钥对象解密 body 中的密文。
 */
@Component
public class SpringCloudAesMqttPayloadDecryptor implements MqttPayloadDecryptor {

    private final AesProperties aesProperties;
    private final Map<String, AesEncryptor> aesEncryptors;

    public SpringCloudAesMqttPayloadDecryptor(ObjectProvider<AesProperties> aesPropertiesProvider,
                                              @Qualifier("aesEncryptors")
                                              ObjectProvider<Map<String, AesEncryptor>> aesEncryptorsProvider) {
        this.aesProperties = aesPropertiesProvider.getIfAvailable(AesProperties::new);
        this.aesEncryptors = aesEncryptorsProvider.getIfAvailable(Map::of);
    }

    @Override
    public boolean supports(String appId) {
        return appId != null
                && aesProperties.getMerchants() != null
                && aesProperties.getMerchants().containsKey(appId)
                && aesEncryptors.containsKey(appId);
    }

    @Override
    public byte[] decryptBytes(String appId, String encryptedBody) {
        AesEncryptor aesEncryptor = aesEncryptors.get(appId);
        if (aesEncryptor == null) {
            throw new BizException("未找到 appId 对应的 AES 解密器: " + appId);
        }
        return aesEncryptor.decryptByte(encryptedBody);
    }
}
