package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.stereotype.Component;

/**
 * 3DES 解密器。
 * 对应 JCE 的 DESede 算法。
 */
@Component
public class TripleDesMqttPayloadDecryptor extends AbstractJceMqttPayloadDecryptor {

    public TripleDesMqttPayloadDecryptor(IotProperties iotProperties) {
        super(iotProperties);
    }

    @Override
    protected String algorithmCode() {
        return "DESede";
    }

    @Override
    protected String defaultTransformation() {
        return "DESede/CBC/PKCS5Padding";
    }
}
