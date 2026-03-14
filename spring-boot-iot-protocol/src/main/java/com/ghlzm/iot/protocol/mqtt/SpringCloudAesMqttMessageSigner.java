package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlxk.cloud.aes.core.AesEncryptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 基于 AesEncryptor 的签名器。
 * 兼容旧平台“用 AES 加密结果作为签名”的约定。
 */
@Component
public class SpringCloudAesMqttMessageSigner implements MqttMessageSigner {

    private final Map<String, AesEncryptor> aesEncryptors;

    public SpringCloudAesMqttMessageSigner(@Qualifier("aesEncryptors")
                                           ObjectProvider<Map<String, AesEncryptor>> aesEncryptorsProvider) {
        this.aesEncryptors = aesEncryptorsProvider.getIfAvailable(Map::of);
    }

    @Override
    public boolean supports(String algorithm) {
        if (algorithm == null || algorithm.isBlank()) {
            return false;
        }
        return "AES".equalsIgnoreCase(algorithm) || "AES-ENCRYPT".equalsIgnoreCase(algorithm);
    }

    @Override
    public String sign(String appId, String content) {
        AesEncryptor aesEncryptor = aesEncryptors.get(appId);
        if (aesEncryptor == null) {
            throw new BizException("未找到 appId 对应的 AES 签名器: " + appId);
        }
        return aesEncryptor.encrypt(content == null ? "" : content);
    }
}
