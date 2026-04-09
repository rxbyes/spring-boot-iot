package com.ghlzm.iot.telemetry.service.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Telemetry v2 raw 单指标点模型。
 */
@Data
public class TelemetryV2Point {

    private TelemetryStreamKind streamKind;
    private Long tenantId;
    private Long deviceId;
    private Long productId;
    private String deviceCode;
    private String productKey;
    private String protocolCode;
    private String metricId;
    private String metricCode;
    private String metricName;
    private String valueType;
    private Double valueDouble;
    private Long valueLong;
    private Boolean valueBool;
    private String valueText;
    private String qualityCode;
    private Boolean alarmFlag = Boolean.FALSE;
    private String traceId;
    private String sessionId;
    private String sourceMessageType;
    private String sensorGroup;
    private String locationCode;
    private Long riskPointId;
    private LocalDateTime reportedAt;
    private LocalDateTime ingestedAt;
}
