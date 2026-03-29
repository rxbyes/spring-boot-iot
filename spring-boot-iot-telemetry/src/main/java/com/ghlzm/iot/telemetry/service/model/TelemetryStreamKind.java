package com.ghlzm.iot.telemetry.service.model;

import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;

import java.util.Locale;

/**
 * Telemetry v2 raw 流分类。
 */
public enum TelemetryStreamKind {

    MEASURE("m", "iot_raw_measure_point"),
    STATUS("s", "iot_raw_status_point"),
    EVENT("e", "iot_raw_event_point");

    private final String tablePrefix;
    private final String stableName;

    TelemetryStreamKind(String tablePrefix, String stableName) {
        this.tablePrefix = tablePrefix;
        this.stableName = stableName;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public String getStableName() {
        return stableName;
    }

    public static TelemetryStreamKind resolve(String messageType,
                                              String metricCode,
                                              DevicePropertyMetadata metadata,
                                              Object value) {
        String normalizedMessageType = normalize(messageType);
        if ("event".equals(normalizedMessageType)
                || "reply".equals(normalizedMessageType)
                || "ack".equals(normalizedMessageType)) {
            return EVENT;
        }
        String normalizedMetricCode = normalize(metricCode);
        if (normalizedMetricCode.contains("status")
                || normalizedMetricCode.contains("state")
                || normalizedMetricCode.contains("signal")
                || normalizedMetricCode.contains("battery")
                || normalizedMetricCode.contains("power")
                || normalizedMetricCode.contains("online")
                || normalizedMetricCode.contains("firmware")
                || normalizedMetricCode.contains("version")) {
            return STATUS;
        }
        String dataType = normalize(metadata == null ? null : metadata.getDataType());
        if ("bool".equals(dataType)
                || "boolean".equals(dataType)
                || value instanceof Boolean) {
            return STATUS;
        }
        if (value instanceof Number) {
            return MEASURE;
        }
        if ("int".equals(dataType)
                || "integer".equals(dataType)
                || "long".equals(dataType)
                || "double".equals(dataType)
                || "float".equals(dataType)
                || "decimal".equals(dataType)
                || "number".equals(dataType)) {
            return MEASURE;
        }
        return STATUS;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
