package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Telemetry 读路由解析器。
 */
@Component
public class TelemetryReadRouter {

    private final IotProperties iotProperties;

    public TelemetryReadRouter(IotProperties iotProperties) {
        this.iotProperties = iotProperties;
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

    public boolean isLegacyReadFallbackEnabled() {
        return Boolean.TRUE.equals(iotProperties.getTelemetry().getReadRouting().getLegacyReadFallbackEnabled());
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
