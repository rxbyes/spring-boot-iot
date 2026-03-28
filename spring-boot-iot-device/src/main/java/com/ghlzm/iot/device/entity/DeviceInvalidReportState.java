package com.ghlzm.iot.device.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 无效 MQTT 上报最新态。
 */
@Data
public class DeviceInvalidReportState {

    private Long id;
    private Long tenantId;
    private String governanceKey;
    private String reasonCode;
    private String requestMethod;
    private String failureStage;
    private String deviceCode;
    private String productKey;
    private String protocolCode;
    private String topicRouteType;
    private String topic;
    private String clientId;
    private Integer payloadSize;
    private String payloadEncoding;
    private String lastPayload;
    private String lastTraceId;
    private String sampleErrorMessage;
    private String sampleExceptionClass;
    private LocalDateTime firstSeenTime;
    private LocalDateTime lastSeenTime;
    private Long hitCount;
    private Long sampledCount;
    private Long suppressedCount;
    private LocalDateTime suppressedUntil;
    private Integer resolved;
    private LocalDateTime resolvedTime;
    private Integer deleted;
}
