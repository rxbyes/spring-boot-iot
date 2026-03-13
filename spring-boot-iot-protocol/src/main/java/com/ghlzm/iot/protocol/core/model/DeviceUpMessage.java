package com.ghlzm.iot.protocol.core.model;

import java.util.Map;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:06
 */
@Data
public class DeviceUpMessage {

    private String tenantId;
    private String productKey;
    private String deviceCode;
    private String messageType;
    private String topic;
    private Map<String, Object> properties;
    private Map<String, Object> events;
    private LocalDateTime timestamp;
    private String rawPayload;
}

