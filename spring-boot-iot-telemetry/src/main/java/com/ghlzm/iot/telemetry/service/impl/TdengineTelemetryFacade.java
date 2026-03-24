package com.ghlzm.iot.telemetry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.telemetry.service.model.TelemetryLatestPoint;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 统一封装 TDengine legacy stable 与通用兼容表。
 */
@Service
public class TdengineTelemetryFacade {

    private static final long TRACE_COMPENSATION_WINDOW_SECONDS = 5L;
    private static final Logger log = LoggerFactory.getLogger(TdengineTelemetryFacade.class);

    private final IotProperties iotProperties;
    private final DevicePropertyMetadataService devicePropertyMetadataService;
    private final TdengineTelemetryStorageService tdengineTelemetryStorageService;
    private final LegacyTdengineTelemetryWriter legacyTdengineTelemetryWriter;
    private final LegacyTdengineTelemetryReader legacyTdengineTelemetryReader;
    private final DeviceMessageLogMapper deviceMessageLogMapper;

    public TdengineTelemetryFacade(IotProperties iotProperties,
                                   DevicePropertyMetadataService devicePropertyMetadataService,
                                   TdengineTelemetryStorageService tdengineTelemetryStorageService,
                                   LegacyTdengineTelemetryWriter legacyTdengineTelemetryWriter,
                                   LegacyTdengineTelemetryReader legacyTdengineTelemetryReader,
                                   DeviceMessageLogMapper deviceMessageLogMapper) {
        this.iotProperties = iotProperties;
        this.devicePropertyMetadataService = devicePropertyMetadataService;
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
        LegacyTdengineTelemetryWriter.LegacyTdenginePersistOutcome legacyOutcome =
                legacyTdengineTelemetryWriter.persist(target, properties, metadataMap);

        Map<String, Object> fallbackProperties = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (!legacyOutcome.getPersistedMetricCodes().contains(entry.getKey())) {
                fallbackProperties.put(entry.getKey(), entry.getValue());
            }
        }

        int normalizedFallbackCount = 0;
        int fallbackMetricCount = 0;
        int skippedMetricCount = 0;
        if (!fallbackProperties.isEmpty()) {
            if (isLegacyNormalizedFallbackEnabled()) {
                TelemetryPersistResult fallbackResult =
                        tdengineTelemetryStorageService.persist(target, fallbackProperties, metadataMap);
                normalizedFallbackCount = fallbackResult.getPointCount() == null ? 0 : fallbackResult.getPointCount();
                fallbackMetricCount = normalizedFallbackCount;
            } else {
                skippedMetricCount = fallbackProperties.size();
            }
        }

        int pointCount = legacyOutcome.getMetricCount() + normalizedFallbackCount;
        TelemetryPersistResult result;
        if (pointCount == 0) {
            result = TelemetryPersistResult.skipped(
                    skippedMetricCount > 0 ? "LEGACY_UNMAPPED_NO_FALLBACK" : "LEGACY_NO_MATCHED_PROPERTIES",
                    "legacy-compatible",
                    skippedMetricCount
            );
            enrichLegacyResult(result, legacyOutcome, fallbackMetricCount, fallbackProperties);
            logFallbackGovernanceSignal(target, result);
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
        result = TelemetryPersistResult.persisted(
                branch,
                "legacy-compatible",
                pointCount,
                legacyOutcome.getStableCount(),
                legacyOutcome.getMetricCount(),
                normalizedFallbackCount,
                skippedMetricCount
        );
        enrichLegacyResult(result, legacyOutcome, fallbackMetricCount, fallbackProperties);
        logLegacyMappingValidateOnly(target, properties, metadataMap, result);
        logFallbackGovernanceSignal(target, result);
        return result;
    }

    public List<TelemetryLatestPoint> listLatestPoints(Device device, Product product) {
        if (!isLegacyCompatibleMode()) {
            return tdengineTelemetryStorageService.listLatestPoints(device.getId());
        }
        Map<String, DevicePropertyMetadata> metadataMap = listPropertyMetadataMap(device);
        Map<String, TelemetryLatestPoint> mergedPoints = new LinkedHashMap<>();
        for (TelemetryLatestPoint point : legacyTdengineTelemetryReader.listLatestPoints(device, product, metadataMap)) {
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

    private Map<String, DevicePropertyMetadata> listPropertyMetadataMap(DeviceProcessingTarget target) {
        if (target == null || target.getDevice() == null) {
            return Map.of();
        }
        return devicePropertyMetadataService.listPropertyMetadataMap(target.getDevice().getProductId());
    }

    private Map<String, DevicePropertyMetadata> listPropertyMetadataMap(Device device) {
        if (device == null) {
            return Map.of();
        }
        return devicePropertyMetadataService.listPropertyMetadataMap(device.getProductId());
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
        long earlierDeltaMillis = Math.abs(java.time.Duration.between(earlierMessageLog.getReportTime(), latestReportedAt).toMillis());
        long laterDeltaMillis = Math.abs(java.time.Duration.between(laterMessageLog.getReportTime(), latestReportedAt).toMillis());
        return laterDeltaMillis <= earlierDeltaMillis ? laterMessageLog : earlierMessageLog;
    }

    private boolean isLegacyCompatibleMode() {
        return "legacy-compatible".equalsIgnoreCase(normalizeTdengineMode());
    }

    private void enrichLegacyResult(TelemetryPersistResult result,
                                    LegacyTdengineTelemetryWriter.LegacyTdenginePersistOutcome legacyOutcome,
                                    int fallbackMetricCount,
                                    Map<String, Object> fallbackProperties) {
        result.setLegacyMappedMetricCount(legacyOutcome.getMappedMetricCount());
        result.setLegacyUnmappedMetricCount(legacyOutcome.getUnmappedMetricCount());
        result.setFallbackMetricCount(fallbackMetricCount);
        if (fallbackProperties == null || fallbackProperties.isEmpty()) {
            result.setFallbackReason(null);
            return;
        }
        if (!legacyOutcome.getFallbackReasons().isEmpty()) {
            result.setFallbackReason(String.join(",", legacyOutcome.getFallbackReasons().stream().sorted().toList()));
            return;
        }
        result.setFallbackReason(isLegacyNormalizedFallbackEnabled() ? "LEGACY_UNMAPPED" : "FALLBACK_DISABLED");
    }

    private void logFallbackGovernanceSignal(DeviceProcessingTarget target, TelemetryPersistResult result) {
        if (result == null || result.getLegacyUnmappedMetricCount() == null || result.getLegacyUnmappedMetricCount() <= 0) {
            return;
        }
        log.info("TDengine legacy 映射回退, deviceCode={}, branch={}, legacyMappedMetricCount={}, legacyUnmappedMetricCount={}, fallbackMetricCount={}, reason={}",
                target == null || target.getDevice() == null ? null : target.getDevice().getDeviceCode(),
                result.getBranch(),
                result.getLegacyMappedMetricCount(),
                result.getLegacyUnmappedMetricCount(),
                result.getFallbackMetricCount(),
                result.getFallbackReason());
    }

    private void logLegacyMappingValidateOnly(DeviceProcessingTarget target,
                                              Map<String, Object> properties,
                                              Map<String, DevicePropertyMetadata> metadataMap,
                                              TelemetryPersistResult result) {
        if (!isLegacyMappingValidateOnlyEnabled()) {
            return;
        }
        MappingSnapshot oldSnapshot = buildLegacyMappingSnapshot(properties, metadataMap);
        log.info("legacy_mapping_validate_only, deviceCode={}, oldMappedMetricCount={}, oldUnmappedMetricCount={}, newMappedMetricCount={}, newUnmappedMetricCount={}, fallbackMetricCount={}, fallbackReason={}",
                target == null || target.getDevice() == null ? null : target.getDevice().getDeviceCode(),
                oldSnapshot.mappedMetricCount(),
                oldSnapshot.unmappedMetricCount(),
                result.getLegacyMappedMetricCount(),
                result.getLegacyUnmappedMetricCount(),
                result.getFallbackMetricCount(),
                result.getFallbackReason());
    }

    private MappingSnapshot buildLegacyMappingSnapshot(Map<String, Object> properties,
                                                       Map<String, DevicePropertyMetadata> metadataMap) {
        int mappedMetricCount = 0;
        int unmappedMetricCount = 0;
        if (properties == null || properties.isEmpty()) {
            return new MappingSnapshot(0, 0);
        }
        for (String metricCode : properties.keySet()) {
            DevicePropertyMetadata metadata = metadataMap == null ? null : metadataMap.get(metricCode);
            DevicePropertyMetadata.TdengineLegacyMapping mapping = metadata == null ? null : metadata.getTdengineLegacyMapping();
            if (mapping != null
                    && !Boolean.FALSE.equals(mapping.getEnabled())
                    && hasText(mapping.getStable())
                    && hasText(mapping.getColumn())) {
                mappedMetricCount++;
            } else {
                unmappedMetricCount++;
            }
        }
        return new MappingSnapshot(mappedMetricCount, unmappedMetricCount);
    }

    private boolean isLegacyNormalizedFallbackEnabled() {
        return iotProperties.getTelemetry() == null
                || iotProperties.getTelemetry().getLegacyNormalizedFallbackEnabled() == null
                || Boolean.TRUE.equals(iotProperties.getTelemetry().getLegacyNormalizedFallbackEnabled());
    }

    private boolean isLegacyMappingValidateOnlyEnabled() {
        return iotProperties.getTelemetry() != null
                && Boolean.TRUE.equals(iotProperties.getTelemetry().getLegacyMappingValidateOnly());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeTdengineMode() {
        if (iotProperties.getTelemetry() == null || iotProperties.getTelemetry().getTdengineMode() == null) {
            return "legacy-compatible";
        }
        return iotProperties.getTelemetry().getTdengineMode().trim().toLowerCase(Locale.ROOT);
    }

    private record MappingSnapshot(int mappedMetricCount,
                                   int unmappedMetricCount) {
    }
}
