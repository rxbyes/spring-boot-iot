package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.stereotype.Component;

/**
 * DES 解密器。
 * 面向历史厂商兼容场景，优先使用配置中的 appId -> 算法映射。
 */
@Component
public class DesMqttPayloadDecryptor extends AbstractJceMqttPayloadDecryptor {

    public DesMqttPayloadDecryptor(IotProperties iotProperties) {
        super(iotProperties);
    }

    @Override
    protected String algorithmCode() {
        return "DES";
    }

    @Override
    protected String defaultTransformation() {
        return "DES/CBC/PKCS5Padding";
    }
}
