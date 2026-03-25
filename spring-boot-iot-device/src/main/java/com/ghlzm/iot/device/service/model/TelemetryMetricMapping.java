package com.ghlzm.iot.device.service.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 遥测指标与 legacy TDengine stable/column 的显式映射。
 */
@Data
public class TelemetryMetricMapping {

    public static final String SOURCE_SPECS_JSON_TDENGINE_LEGACY = "specsJson.tdengineLegacy";

    public static final String REASON_MAPPING_NOT_CONFIGURED = "LEGACY_MAPPING_NOT_CONFIGURED";
    public static final String REASON_MAPPING_DISABLED = "LEGACY_MAPPING_DISABLED";
    public static final String REASON_STABLE_MISSING = "LEGACY_STABLE_MISSING";
    public static final String REASON_STABLE_INVALID = "LEGACY_STABLE_INVALID";
    public static final String REASON_COLUMN_MISSING = "LEGACY_COLUMN_MISSING";
    public static final String REASON_COLUMN_INVALID = "LEGACY_COLUMN_INVALID";
    public static final String REASON_PROPERTY_METADATA_MISSING = "LEGACY_PROPERTY_METADATA_MISSING";
    public static final String REASON_SCHEMA_COLUMN_MISSING = "LEGACY_SCHEMA_COLUMN_MISSING";

    private String metricCode;
    private Boolean enabled = Boolean.TRUE;
    private String stable;
    private String column;
    private String source;
    private List<String> fallbackReasons = new ArrayList<>();

    public void setFallbackReasons(List<String> fallbackReasons) {
        this.fallbackReasons = fallbackReasons == null
                ? new ArrayList<>()
                : new ArrayList<>(new LinkedHashSet<>(fallbackReasons));
    }

    public void addFallbackReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return;
        }
        if (fallbackReasons == null) {
            fallbackReasons = new ArrayList<>();
        }
        if (!fallbackReasons.contains(reason)) {
            fallbackReasons.add(reason);
        }
    }

    public boolean isLegacyMapped() {
        return Boolean.TRUE.equals(enabled)
                && hasText(stable)
                && hasText(column)
                && (fallbackReasons == null || fallbackReasons.isEmpty());
    }

    public String primaryFallbackReason() {
        return fallbackReasons == null || fallbackReasons.isEmpty() ? null : fallbackReasons.get(0);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
