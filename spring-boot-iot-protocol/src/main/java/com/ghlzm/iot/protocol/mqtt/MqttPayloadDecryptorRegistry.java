package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MQTT 负载解密器注册表。
 * 当前阶段先提供扩展点和清晰失败路径，后续再按厂商接入真实解密实现。
 */
@Component
public class MqttPayloadDecryptorRegistry {

    private final List<MqttPayloadDecryptor> decryptors;

    public MqttPayloadDecryptorRegistry(List<MqttPayloadDecryptor> decryptors) {
        this.decryptors = decryptors;
    }

    public byte[] decryptBytesOrThrow(String appId, String encryptedBody) {
        for (MqttPayloadDecryptor decryptor : decryptors) {
            if (decryptor.supports(appId)) {
                return decryptor.decryptBytes(appId, encryptedBody);
            }
        }
        throw new BizException("检测到加密 MQTT 报文，但未配置 appId 对应的解密器: " + appId);
    }

    public String decryptOrThrow(String appId, String encryptedBody) {
        return new String(decryptBytesOrThrow(appId, encryptedBody), StandardCharsets.UTF_8);
    }
}
