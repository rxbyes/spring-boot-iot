package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MQTT 签名器注册表。
 * 当前按算法编码路由，便于兼容多厂商、多算法场景。
 */
@Component
public class MqttMessageSignerRegistry {

    private final List<MqttMessageSigner> signers;

    public MqttMessageSignerRegistry(List<MqttMessageSigner> signers) {
        this.signers = signers;
    }

    public String sign(String algorithm, String appId, String content) {
        return getSigner(algorithm).sign(appId, content);
    }

    public String signBytes(String algorithm, String appId, byte[] content) {
        return getSigner(algorithm).signBytes(appId, content);
    }

    public boolean verify(String algorithm, String appId, String content, String signature) {
        return getSigner(algorithm).verify(appId, content, signature);
    }

    private MqttMessageSigner getSigner(String algorithm) {
        for (MqttMessageSigner signer : signers) {
            if (signer.supports(algorithm)) {
                return signer;
            }
        }
        throw new BizException("未找到签名算法实现: " + algorithm);
    }
}
