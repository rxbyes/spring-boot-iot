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

    /**
     * 文件/固件类上报的统一协议模型。
     * 当前协议层先把 C.3/C.4 收口到这里，后续 OTA 或文件业务可直接复用，
     * 避免继续通过 events 中的临时字段做弱约定。
     */
    private DeviceFilePayload filePayload;

    private LocalDateTime timestamp;

    /**
     * 原始 JSON 字符串，便于消息日志原样落库。
     */
    private String rawPayload;
}
