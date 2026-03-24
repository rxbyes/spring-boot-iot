package com.ghlzm.iot.telemetry.service.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 最新时序点快照。
 */
@Data
public class TelemetryLatestPoint {

    private LocalDateTime reportedAt;
    private String deviceCode;
    private String productKey;
    private String metricCode;
    private String metricName;
    private String valueType;
    private Object value;
    private String traceId;
}
