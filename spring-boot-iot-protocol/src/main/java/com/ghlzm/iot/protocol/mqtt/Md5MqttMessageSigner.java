package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * MD5 签名兼容实现。
 * 说明：MD5 只能做带密钥摘要兼容，不是可逆的加密算法。
 */
@Component
public class Md5MqttMessageSigner implements MqttMessageSigner {

    private final IotProperties iotProperties;

    public Md5MqttMessageSigner(IotProperties iotProperties) {
        this.iotProperties = iotProperties;
    }

    @Override
    public boolean supports(String algorithm) {
        return algorithm != null && "MD5".equalsIgnoreCase(algorithm);
    }

    @Override
    public String sign(String appId, String content) {
        IotProperties.Protocol.Crypto.Merchant merchant = iotProperties.getProtocol().getCrypto().getMerchants().get(appId);
        if (merchant == null) {
            throw new BizException("未找到 appId 对应的 MD5 配置: " + appId);
        }
        String secret = merchant.getSignatureSecret();
        if (secret == null || secret.isBlank()) {
            secret = merchant.getKey();
        }
        if (secret == null || secret.isBlank()) {
            throw new BizException("MD5 签名缺少共享密钥配置: " + appId);
        }

        String payload = content == null ? "" : content;
        String joinMode = merchant.getSignatureJoinMode();
        String signSource;
        if ("KEY_PREFIX".equalsIgnoreCase(joinMode)) {
            signSource = secret + payload;
        } else if ("CONTENT_ONLY".equalsIgnoreCase(joinMode)) {
            signSource = payload;
        } else {
            signSource = payload + secret;
        }

        byte[] bytes = signSource.getBytes(StandardCharsets.UTF_8);
        return DigestUtils.md5DigestAsHex(bytes);
    }
}
