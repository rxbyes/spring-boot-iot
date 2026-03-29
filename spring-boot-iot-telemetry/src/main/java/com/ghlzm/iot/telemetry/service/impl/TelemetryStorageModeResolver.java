package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Telemetry 存储与读写模式解析器。
 */
@Component
public class TelemetryStorageModeResolver {

    private final IotProperties iotProperties;

    public TelemetryStorageModeResolver(IotProperties iotProperties) {
        this.iotProperties = iotProperties;
    }

    public boolean isTdengineEnabled() {
        return "tdengine".equals(normalizedStorageType());
    }

    public boolean isV2PrimaryEnabled() {
        return isTdengineEnabled() && "tdengine-v2".equals(normalizedPrimaryStorage());
    }

    public boolean isLegacyMirrorEnabled() {
        return isTdengineEnabled()
                && Boolean.TRUE.equals(iotProperties.getTelemetry().getLegacyMirror().getEnabled());
    }

    public boolean isLatestRedisEnabled() {
        return Boolean.TRUE.equals(iotProperties.getTelemetry().getLatest().getRedisEnabled());
    }

    public boolean isLatestMysqlProjectionEnabled() {
        return Boolean.TRUE.equals(iotProperties.getTelemetry().getLatest().getMysqlProjectionEnabled());
    }

    public boolean isLegacyReadFallbackEnabled() {
        return Boolean.TRUE.equals(iotProperties.getTelemetry().getReadRouting().getLegacyReadFallbackEnabled());
    }

    public String latestSource() {
        return normalize(iotProperties.getTelemetry().getReadRouting().getLatestSource(), "legacy");
    }

    public String historySource() {
        return normalize(iotProperties.getTelemetry().getReadRouting().getHistorySource(), "legacy");
    }

    public String aggregateSource() {
        return normalize(iotProperties.getTelemetry().getReadRouting().getAggregateSource(), "legacy");
    }

    public Integer rawRetentionDays() {
        return iotProperties.getTelemetry().getRaw().getRetentionDays();
    }

    public String tenantRoutingMode() {
        return normalize(iotProperties.getTelemetry().getTenantRouting().getMode(), "tenant-device");
    }

    private String normalizedStorageType() {
        return normalize(iotProperties.getTelemetry().getStorageType(), "mysql");
    }

    private String normalizedPrimaryStorage() {
        return normalize(iotProperties.getTelemetry().getPrimaryStorage(), "legacy-compatible");
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
