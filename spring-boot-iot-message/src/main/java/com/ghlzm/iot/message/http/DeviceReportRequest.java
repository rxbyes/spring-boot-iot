package com.ghlzm.iot.message.http;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * HTTP 设备上报请求体。
 * 一期通过该模型承接模拟设备上报的原始参数。
 *
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
     * 原始 JSON 字符串，当前阶段直接透传给协议层解析。
     */
    @NotBlank
    private String payload;

    /**
     * payload 字节编码方式，默认 UTF-8。
     * 明文二进制帧模拟场景可传 ISO-8859-1（latin1）保留 0~255 原始字节。
     */
    private String payloadEncoding;

    /**
     * 上报 topic，HTTP 模拟场景下由调用方传入。
     */
    private String topic;

    /**
     * 设备接入客户端标识，可为空。
     */
    private String clientId;

    /**
     * 一期先默认使用字符串形式的租户 ID。
     */
    private String tenantId = "1";
}
