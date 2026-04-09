package com.ghlzm.iot.protocol.mqtt;

import java.util.Base64;

/**
 * MQTT 签名器扩展点。
 * 用于兼容不同厂商的签名算法，例如 AES、MD5 等。
 */
public interface MqttMessageSigner {

    /**
     * 是否支持当前签名算法编码。
     */
    boolean supports(String algorithm);

    /**
     * 基于字符串正文生成签名。
     */
    String sign(String appId, String content);

    /**
     * 基于字节流生成签名。
     * 默认通过 Base64 文本桥接，避免二进制内容在签名阶段丢失。
     */
    default String signBytes(String appId, byte[] content) {
        byte[] payload = content == null ? new byte[0] : content;
        return sign(appId, Base64.getEncoder().encodeToString(payload));
    }

    /**
     * 校验字符串签名。
     */
    default boolean verify(String appId, String content, String signature) {
        return sign(appId, content).equals(signature);
    }

    /**
     * 校验字节流签名。
     */
    default boolean verifyBytes(String appId, byte[] content, String signature) {
        return signBytes(appId, content).equals(signature);
    }
}
