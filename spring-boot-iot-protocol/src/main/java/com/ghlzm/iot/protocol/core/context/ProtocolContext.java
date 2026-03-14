package com.ghlzm.iot.protocol.core.context;

import lombok.Data;

import java.util.Map;

/**
 * 协议上下文。
 * 接入层负责填充上下文，协议层只消费上下文做解析。
 *
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:06
 */
@Data
public class ProtocolContext {

    /**
     * 当前阶段直接沿用上游传入的租户 ID 字符串。
     */
    private String tenantCode;

    private String productKey;
    private String deviceCode;
    private String topic;
    private String clientId;

    /**
     * 预留扩展元数据，后续接入不同协议时可补充更多环境信息。
     */
    private Map<String, Object> metadata;
}
