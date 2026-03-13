package com.ghlzm.iot.protocol.core.context;

import lombok.Data;

import java.util.Map;

/**
 * 协议上下文
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:06
 */
@Data
public class ProtocolContext {

    private String tenantCode;
    private String productKey;
    private String deviceCode;
    private String topic;
    private String clientId;
    private Map<String, Object> metadata;
}

