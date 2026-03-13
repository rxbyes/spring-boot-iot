package com.ghlzm.iot.protocol.core.model;

import lombok.Data;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:29
 */
@Data
public class RawDeviceMessage {

    /**
     * 原始报文内容
     */
    private byte[] payload;

    /**
     * 接入协议编码，例如 mqtt-json / tcp-hex
     */
    private String protocolCode;

    /**
     * productKey
     */
    private String productKey;

    /**
     * deviceCode
     */
    private String deviceCode;

    /**
     * 原始topic，HTTP可为空
     */
    private String topic;

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 租户ID，当前一期先直接传字符串
     */
    private String tenantId;
}

