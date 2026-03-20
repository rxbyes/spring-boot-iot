package com.ghlzm.iot.protocol.core.model;

import lombok.Data;

import java.util.Map;

/**
 * 统一设备下行消息骨架。
 * 一期主链路暂未真正使用，但保留最小定义以保持协议抽象完整。
 *
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:06
 */
@Data
public class DeviceDownMessage {

    private String messageId;
    private String commandType;
    private String serviceIdentifier;
    private Map<String, Object> params;
}
