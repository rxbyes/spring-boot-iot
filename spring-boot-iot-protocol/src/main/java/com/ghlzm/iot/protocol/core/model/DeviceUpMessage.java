package com.ghlzm.iot.protocol.core.model;

import java.util.Map;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一设备上行消息模型。
 * 协议层会把不同接入协议的报文统一转换为该对象，再交给 device 模块处理。
 *
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:06
 */
@Data
public class DeviceUpMessage {

    /**
     * 当前阶段沿用字符串形式租户标识。
     */
    private String tenantId;

    private String productKey;
    private String deviceCode;
    private String protocolCode;

    /**
     * 例如 property / event / reply。
     */
    private String messageType;

    private String topic;

    /**
     * 属性类上报统一放入 properties。
     */
    private Map<String, Object> properties;

    /**
     * 事件类上报统一放入 events。
     */
    private Map<String, Object> events;

    private LocalDateTime timestamp;

    /**
     * 原始 JSON 字符串，便于消息日志原样落库。
     */
    private String rawPayload;
}
