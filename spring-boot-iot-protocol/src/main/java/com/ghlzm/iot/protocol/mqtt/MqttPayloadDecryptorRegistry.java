package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MQTT 负载解密器注册表。
 * 当前阶段先提供扩展点和清晰失败路径，后续再按厂商接入真实解密实现。
 */
@Component
public class MqttPayloadDecryptorRegistry {

    private final ProtocolDecryptProfileResolver protocolDecryptProfileResolver;
    private final List<ProtocolDecryptExecutor> executors;
    private final List<MqttPayloadDecryptor> legacyDecryptors;

    @Autowired
    public MqttPayloadDecryptorRegistry(ProtocolDecryptProfileResolver protocolDecryptProfileResolver,
                                        List<ProtocolDecryptExecutor> executors) {
        this.protocolDecryptProfileResolver = protocolDecryptProfileResolver;
        this.executors = List.copyOf(executors);
        this.legacyDecryptors = List.of();
    }

    public MqttPayloadDecryptorRegistry(List<MqttPayloadDecryptor> decryptors) {
        this.protocolDecryptProfileResolver = null;
        this.executors = List.of();
        this.legacyDecryptors = List.copyOf(decryptors);
    }

    public byte[] decryptBytesOrThrow(ProtocolDecryptResolveContext context, String encryptedBody) {
        ProtocolDecryptProfile profile = protocolDecryptProfileResolver.resolveOrThrow(context);
        return executorFor(profile.getAlgorithm()).decryptBytes(profile, encryptedBody);
    }

    public byte[] decryptBytesOrThrow(String appId, String encryptedBody) {
        if (!legacyDecryptors.isEmpty()) {
            for (MqttPayloadDecryptor decryptor : legacyDecryptors) {
                if (decryptor.supports(appId)) {
                    return decryptor.decryptBytes(appId, encryptedBody);
                }
            }
            throw new BizException("检测到加密 MQTT 报文，但未配置 appId 对应的解密器: " + appId);
        }
        return decryptBytesOrThrow(new ProtocolDecryptResolveContext(appId, "mqtt-json", List.of()), encryptedBody);
    }

    public String decryptOrThrow(String appId, String encryptedBody) {
        return new String(decryptBytesOrThrow(appId, encryptedBody), StandardCharsets.UTF_8);
    }

    private ProtocolDecryptExecutor executorFor(String algorithm) {
        for (ProtocolDecryptExecutor executor : executors) {
            if (executor.supports(algorithm)) {
                return executor;
            }
        }
        throw new BizException("未找到解密算法实现: " + algorithm);
    }
}
