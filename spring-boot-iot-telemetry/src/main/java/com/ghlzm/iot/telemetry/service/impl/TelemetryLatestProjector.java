package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryProjectionTask;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Telemetry v2 latest 投影器。
 */
@Service
public class TelemetryLatestProjector {

    private final TelemetryLatestProjectionRepository repository;

    public TelemetryLatestProjector(TelemetryLatestProjectionRepository repository) {
        this.repository = repository;
    }

    public void project(TelemetryProjectionTask task) {
        if (task == null || task.getPoints() == null || task.getPoints().isEmpty()) {
            return;
        }
        Map<String, TelemetryV2Point> freshestPoints = new LinkedHashMap<>();
        for (TelemetryV2Point point : task.getPoints()) {
            String key = buildKey(point);
            TelemetryV2Point existingPoint = freshestPoints.get(key);
            if (existingPoint == null || isNewer(point, existingPoint)) {
                freshestPoints.put(key, point);
            }
        }
        repository.bulkUpsert(new ArrayList<>(freshestPoints.values()));
    }

    private String buildKey(TelemetryV2Point point) {
        return safe(point.getTenantId()) + ":" + safe(point.getDeviceId()) + ":" + safeText(point.getMetricId());
    }

    private boolean isNewer(TelemetryV2Point current, TelemetryV2Point previous) {
        LocalDateTime currentReportedAt = current.getReportedAt();
        LocalDateTime previousReportedAt = previous.getReportedAt();
        if (currentReportedAt == null) {
            return false;
        }
        if (previousReportedAt == null || currentReportedAt.isAfter(previousReportedAt)) {
            return true;
        }
        if (currentReportedAt.isEqual(previousReportedAt)) {
            LocalDateTime currentIngestedAt = current.getIngestedAt();
            LocalDateTime previousIngestedAt = previous.getIngestedAt();
            return currentIngestedAt != null
                    && (previousIngestedAt == null || currentIngestedAt.isAfter(previousIngestedAt));
        }
        return false;
    }

    private String safe(Long value) {
        return value == null ? "0" : String.valueOf(value);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
