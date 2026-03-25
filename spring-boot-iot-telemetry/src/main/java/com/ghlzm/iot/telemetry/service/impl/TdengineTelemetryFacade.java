package com.ghlzm.iot.telemetry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.DeviceTelemetryMappingService;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.telemetry.service.model.TelemetryLatestPoint;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 统一封装 TDengine legacy stable 与通用兼容表。
 */
@Service
public class TdengineTelemetryFacade {

    private static final long TRACE_COMPENSATION_WINDOW_SECONDS = 5L;

    private final IotProperties iotProperties;
    private final DevicePropertyMetadataService devicePropertyMetadataService;
    private final DeviceTelemetryMappingService deviceTelemetryMappingService;
    private final TdengineTelemetryStorageService tdengineTelemetryStorageService;
    private final LegacyTdengineTelemetryWriter legacyTdengineTelemetryWriter;
    private final LegacyTdengineTelemetryReader legacyTdengineTelemetryReader;
    private final DeviceMessageLogMapper deviceMessageLogMapper;

    public TdengineTelemetryFacade(IotProperties iotProperties,
                                   DevicePropertyMetadataService devicePropertyMetadataService,
                                   DeviceTelemetryMappingService deviceTelemetryMappingService,
                                   TdengineTelemetryStorageService tdengineTelemetryStorageService,
                                   LegacyTdengineTelemetryWriter legacyTdengineTelemetryWriter,
                                   LegacyTdengineTelemetryReader legacyTdengineTelemetryReader,
                                   DeviceMessageLogMapper deviceMessageLogMapper) {
        this.iotProperties = iotProperties;
        this.devicePropertyMetadataService = devicePropertyMetadataService;
        this.deviceTelemetryMappingService = deviceTelemetryMappingService;
        this.tdengineTelemetryStorageService = tdengineTelemetryStorageService;
        this.legacyTdengineTelemetryWriter = legacyTdengineTelemetryWriter;
        this.legacyTdengineTelemetryReader = legacyTdengineTelemetryReader;
        this.deviceMessageLogMapper = deviceMessageLogMapper;
    }

    public TelemetryPersistResult persist(DeviceProcessingTarget target) {
        if (!isLegacyCompatibleMode()) {
            return tdengineTelemetryStorageService.persist(target);
        }
        Map<String, Object> properties = target == null || target.getMessage() == null || target.getMessage().getProperties() == null
                ? Map.of()
                : target.getMessage().getProperties();
        Map<String, DevicePropertyMetadata> metadataMap = listPropertyMetadataMap(target);
        Map<String, TelemetryMetricMapping> mappingMap = listMetricMappingMap(target);
        LegacyTdengineTelemetryWriter.LegacyTdenginePersistOutcome legacyOutcome =
                legacyTdengineTelemetryWriter.persist(target, properties, mappingMap);

        Map<String, Object> fallbackProperties = collectFallbackProperties(properties, legacyOutcome);
        int fallbackMetricCount = fallbackProperties.size();
        int normalizedFallbackCount = 0;
        int skippedMetricCount = 0;
        if (!fallbackProperties.isEmpty()) {
            if (isLegacyNormalizedFallbackEnabled()) {
                TelemetryPersistResult fallbackResult =
                        tdengineTelemetryStorageService.persist(target, fallbackProperties, metadataMap);
                normalizedFallbackCount = fallbackResult.getPointCount() == null ? 0 : fallbackResult.getPointCount();
            } else {
                skippedMetricCount = fallbackProperties.size();
            }
        }

        int pointCount = legacyOutcome.getMetricCount() + normalizedFallbackCount;
        List<String> fallbackReasons = resolveFallbackReasons(fallbackProperties.keySet(), legacyOutcome, mappingMap);
        if (pointCount == 0) {
            TelemetryPersistResult result = TelemetryPersistResult.skipped(
                    skippedMetricCount > 0 ? "LEGACY_UNMAPPED_NO_FALLBACK" : "LEGACY_NO_MATCHED_PROPERTIES",
                    "legacy-compatible",
                    skippedMetricCount
            );
            applyGovernanceMetrics(result, legacyOutcome, fallbackMetricCount, fallbackReasons);
            return result;
        }

        String branch;
        if (legacyOutcome.getStableCount() > 0 && normalizedFallbackCount > 0) {
            branch = "LEGACY_WITH_NORMALIZED_FALLBACK";
        } else if (legacyOutcome.getStableCount() > 0) {
            branch = "LEGACY_COMPATIBLE";
        } else {
            branch = "NORMALIZED_FALLBACK_ONLY";
        }
        TelemetryPersistResult result = TelemetryPersistResult.persisted(
                branch,
                "legacy-compatible",
                pointCount,
                legacyOutcome.getStableCount(),
                legacyOutcome.getMetricCount(),
                normalizedFallbackCount,
                skippedMetricCount
        );
        applyGovernanceMetrics(result, legacyOutcome, fallbackMetricCount, fallbackReasons);
        return result;
    }

    public List<TelemetryLatestPoint> listLatestPoints(Device device, Product product) {
        if (!isLegacyCompatibleMode()) {
            return tdengineTelemetryStorageService.listLatestPoints(device.getId());
        }
        Map<String, DevicePropertyMetadata> metadataMap = listPropertyMetadataMap(device);
        Map<String, TelemetryMetricMapping> mappingMap = listMetricMappingMap(device);
        Map<String, TelemetryLatestPoint> mergedPoints = new LinkedHashMap<>();
        for (TelemetryLatestPoint point : legacyTdengineTelemetryReader.listLatestPoints(device, product, metadataMap, mappingMap)) {
            mergedPoints.put(point.getMetricCode(), point);
        }
        if (isLegacyNormalizedFallbackEnabled()) {
            for (TelemetryLatestPoint point : tdengineTelemetryStorageService.listLatestPoints(device.getId())) {
                mergedPoints.putIfAbsent(point.getMetricCode(), point);
            }
        }
        List<TelemetryLatestPoint> points = new ArrayList<>(mergedPoints.values());
        String compensatedTraceId = resolveCompensatedTraceId(device == null ? null : device.getId(), points);
        if (compensatedTraceId != null) {
            for (TelemetryLatestPoint point : points) {
                if (point.getTraceId() == null || point.getTraceId().isBlank()) {
                    point.setTraceId(compensatedTraceId);
                }
            }
        }
        return points;
    }

    private Map<String, Object> collectFallbackProperties(Map<String, Object> properties,
                                                          LegacyTdengineTelemetryWriter.LegacyTdenginePersistOutcome legacyOutcome) {
        Map<String, Object> fallbackProperties = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (!legacyOutcome.getPersistedMetricCodes().contains(entry.getKey())) {
                fallbackProperties.put(entry.getKey(), entry.getValue());
            }
        }
        return fallbackProperties;
    }

    private void applyGovernanceMetrics(TelemetryPersistResult result,
                                        LegacyTdengineTelemetryWriter.LegacyTdenginePersistOutcome legacyOutcome,
                                        int fallbackMetricCount,
                                        List<String> fallbackReasons) {
        result.setLegacyMappedMetricCount(legacyOutcome.getLegacyMappedMetricCount());
        result.setLegacyUnmappedMetricCount(fallbackMetricCount);
        result.setFallbackMetricCount(fallbackMetricCount);
        result.setFallbackReasons(fallbackReasons);
    }

    private List<String> resolveFallbackReasons(Set<String> fallbackMetricCodes,
                                                LegacyTdengineTelemetryWriter.LegacyTdenginePersistOutcome legacyOutcome,
                                                Map<String, TelemetryMetricMapping> mappingMap) {
        if (fallbackMetricCodes == null || fallbackMetricCodes.isEmpty()) {
            return List.of();
        }
        Set<String> reasons = new LinkedHashSet<>();
        for (String metricCode : fallbackMetricCodes) {
            String reason = legacyOutcome.getUnmappedMetricReasons().get(metricCode);
            if (reason == null) {
                TelemetryMetricMapping mapping = mappingMap.get(metricCode);
                reason = mapping == null
                        ? TelemetryMetricMapping.REASON_PROPERTY_METADATA_MISSING
                        : mapping.primaryFallbackReason();
            }
            if (reason != null && !reason.isBlank()) {
                reasons.add(reason);
            }
        }
        return List.copyOf(reasons);
    }

    private Map<String, DevicePropertyMetadata> listPropertyMetadataMap(DeviceProcessingTarget target) {
        if (target == null || target.getDevice() == null) {
            return Map.of();
        }
        return devicePropertyMetadataService.listPropertyMetadataMap(target.getDevice().getProductId());
    }

    private Map<String, TelemetryMetricMapping> listMetricMappingMap(DeviceProcessingTarget target) {
        if (target == null || target.getDevice() == null) {
            return Map.of();
        }
        return deviceTelemetryMappingService.listMetricMappingMap(target.getDevice().getProductId());
    }

    private Map<String, DevicePropertyMetadata> listPropertyMetadataMap(Device device) {
        if (device == null) {
            return Map.of();
        }
        return devicePropertyMetadataService.listPropertyMetadataMap(device.getProductId());
    }

    private Map<String, TelemetryMetricMapping> listMetricMappingMap(Device device) {
        if (device == null) {
            return Map.of();
        }
        return deviceTelemetryMappingService.listMetricMappingMap(device.getProductId());
    }

    private String resolveCompensatedTraceId(Long deviceId, List<TelemetryLatestPoint> points) {
        if (deviceId == null || points == null || points.isEmpty()) {
            return null;
        }
        boolean needsCompensation = points.stream().anyMatch(point -> point.getTraceId() == null || point.getTraceId().isBlank());
        if (!needsCompensation) {
            return null;
        }
        LocalDateTime latestReportedAt = points.stream()
                .map(TelemetryLatestPoint::getReportedAt)
                .filter(time -> time != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        if (latestReportedAt == null) {
            return null;
        }
        LocalDateTime windowStart = latestReportedAt.minusSeconds(TRACE_COMPENSATION_WINDOW_SECONDS);
        LocalDateTime windowEnd = latestReportedAt.plusSeconds(TRACE_COMPENSATION_WINDOW_SECONDS);
        DeviceMessageLog earlierMessageLog = deviceMessageLogMapper.selectOne(
                new LambdaQueryWrapper<DeviceMessageLog>()
                        .eq(DeviceMessageLog::getDeviceId, deviceId)
                        .ge(DeviceMessageLog::getReportTime, windowStart)
                        .le(DeviceMessageLog::getReportTime, latestReportedAt)
                        .orderByDesc(DeviceMessageLog::getReportTime)
                        .orderByDesc(DeviceMessageLog::getId)
                        .last("limit 1")
        );
        DeviceMessageLog laterMessageLog = deviceMessageLogMapper.selectOne(
                new LambdaQueryWrapper<DeviceMessageLog>()
                        .eq(DeviceMessageLog::getDeviceId, deviceId)
                        .ge(DeviceMessageLog::getReportTime, latestReportedAt)
                        .le(DeviceMessageLog::getReportTime, windowEnd)
                        .orderByAsc(DeviceMessageLog::getReportTime)
                        .orderByAsc(DeviceMessageLog::getId)
                        .last("limit 1")
        );
        DeviceMessageLog nearestMessageLog = pickNearestMessageLog(latestReportedAt, earlierMessageLog, laterMessageLog);
        return nearestMessageLog == null ? null : nearestMessageLog.getTraceId();
    }

    private DeviceMessageLog pickNearestMessageLog(LocalDateTime latestReportedAt,
                                                   DeviceMessageLog earlierMessageLog,
                                                   DeviceMessageLog laterMessageLog) {
        if (earlierMessageLog == null) {
            return laterMessageLog;
        }
        if (laterMessageLog == null) {
            return earlierMessageLog;
        }
        long earlierDeltaMillis = Math.abs(Duration.between(earlierMessageLog.getReportTime(), latestReportedAt).toMillis());
        long laterDeltaMillis = Math.abs(Duration.between(laterMessageLog.getReportTime(), latestReportedAt).toMillis());
        return laterDeltaMillis <= earlierDeltaMillis ? laterMessageLog : earlierMessageLog;
    }

    private boolean isLegacyCompatibleMode() {
        return "legacy-compatible".equalsIgnoreCase(normalizeTdengineMode());
    }

    private boolean isLegacyNormalizedFallbackEnabled() {
        return iotProperties.getTelemetry() == null
                || iotProperties.getTelemetry().getLegacyNormalizedFallbackEnabled() == null
                || Boolean.TRUE.equals(iotProperties.getTelemetry().getLegacyNormalizedFallbackEnabled());
    }

    private String normalizeTdengineMode() {
        if (iotProperties.getTelemetry() == null || iotProperties.getTelemetry().getTdengineMode() == null) {
            return "legacy-compatible";
        }
        return iotProperties.getTelemetry().getTdengineMode().trim().toLowerCase(Locale.ROOT);
    }
}
