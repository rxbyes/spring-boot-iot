package com.ghlzm.iot.message.http;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:35
 */
@Data
public class DeviceReportRequest {

    @NotBlank
    private String protocolCode;

    @NotBlank
    private String productKey;

    @NotBlank
    private String deviceCode;

    /**
     * 原始JSON字符串
     */
    @NotBlank
    private String payload;

    /**
     * 可选
     */
    private String topic;

    /**
     * 可选
     */
    private String clientId;

    /**
     * 一期先默认 1
     */
    private String tenantId = "1";
}

