package com.ghlzm.iot.device.dto;

import lombok.Data;

/**
 * 消息追踪查询条件。
 */
@Data
public class DeviceMessageTraceQuery {

    private String deviceCode;
    private String productKey;
    private String traceId;
    private String messageType;
    private String topic;
}
