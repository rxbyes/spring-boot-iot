package com.ghlzm.iot.telemetry.service.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Telemetry v2 投影任务。
 */
@Data
public class TelemetryProjectionTask {

    private ProjectionType projectionType;
    private Long tenantId;
    private Long deviceId;
    private Long productId;
    private String deviceCode;
    private String productKey;
    private String protocolCode;
    private String traceId;
    private String sessionId;
    private String messageType;
    private String topic;
    private LocalDateTime reportedAt;
    private Map<String, Object> properties = Map.of();
    private List<TelemetryV2Point> points = List.of();

    public enum ProjectionType {
        LATEST,
        LEGACY_MIRROR
    }
}
