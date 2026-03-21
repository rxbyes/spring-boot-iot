package com.ghlzm.iot.message.http;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * MQTT 原始上行模拟发布请求。
 */
@Data
public class DeviceMqttReportPublishRequest {

    @NotBlank(message = "protocolCode 不能为空")
    private String protocolCode;

    @NotBlank(message = "productKey 不能为空")
    private String productKey;

    @NotBlank(message = "deviceCode 不能为空")
    private String deviceCode;

    @NotBlank(message = "topic 不能为空")
    private String topic;

    @NotBlank(message = "payload 不能为空")
    private String payload;

    private String payloadEncoding;

    @Min(value = 0, message = "qos 最小为 0")
    @Max(value = 2, message = "qos 最大为 2")
    private Integer qos;

    private Boolean retained;
}
