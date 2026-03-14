package com.ghlzm.iot.protocol.mqtt;

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
    String decrypt(String appId, String encryptedBody);
}
