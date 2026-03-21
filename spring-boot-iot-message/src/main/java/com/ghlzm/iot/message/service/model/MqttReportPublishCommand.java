package com.ghlzm.iot.message.service.model;

import lombok.Data;

/**
 * MQTT 原始上行模拟发布命令。
 */
@Data
public class MqttReportPublishCommand {

    private String protocolCode;
    private String productKey;
    private String deviceCode;
    private String topic;
    private String payload;
    private String payloadEncoding;
    private Integer qos;
    private Boolean retained;
}
