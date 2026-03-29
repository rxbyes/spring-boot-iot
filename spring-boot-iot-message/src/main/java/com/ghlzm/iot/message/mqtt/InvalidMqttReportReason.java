package com.ghlzm.iot.message.mqtt;

/**
 * 首批纳入治理的无效 MQTT 上报原因。
 */
public enum InvalidMqttReportReason {

    EMPTY_DECRYPTED_PAYLOAD,
    DEVICE_NOT_FOUND
}
