package com.ghlzm.iot.message.http;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * MQTT 下行发布请求。
 */
@Data
public class DeviceDownPublishRequest {

    private String protocolCode;

    private String productKey;

    @NotBlank(message = "deviceCode 不能为空")
    private String deviceCode;

    private String topic;

    @Min(value = 0, message = "qos 最小为 0")
    @Max(value = 2, message = "qos 最大为 2")
    private Integer qos;

    private Boolean retained;

    private String messageId;

    private String commandType;

    private String serviceIdentifier;

    private Map<String, Object> params;
}
