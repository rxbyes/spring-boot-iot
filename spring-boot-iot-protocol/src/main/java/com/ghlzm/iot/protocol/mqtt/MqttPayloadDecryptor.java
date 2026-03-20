package com.ghlzm.iot.protocol.mqtt;

import java.nio.charset.StandardCharsets;

/**
 * MQTT 加密负载解密器扩展点。
 * 不同厂商可按 appId 提供各自的密钥获取和解密实现。
 */
public interface MqttPayloadDecryptor {

    /**
     * 是否支持当前厂商或应用编号。
     */
    boolean supports(String appId);

    /**
     * 解密 MQTT 负载中的密文正文。
     */
    byte[] decryptBytes(String appId, String encryptedBody);

    /**
     * 兼容只需要字符串结果的场景。
     * 真正的协议解码应优先走字节流，避免丢失二进制帧头信息。
     */
    default String decrypt(String appId, String encryptedBody) {
        return new String(decryptBytes(appId, encryptedBody), StandardCharsets.UTF_8);
    }
}
