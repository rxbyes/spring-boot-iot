package com.ghlzm.iot.telemetry.service.model;

import lombok.Data;

/**
 * 时序落库结果。
 */
@Data
public class TelemetryPersistResult {

    private String branch;
    private String storageMode;
    private Integer pointCount;
    private Integer legacyStableCount;
    private Integer legacyColumnCount;
    private Integer normalizedFallbackCount;
    private Integer skippedMetricCount;
    private boolean skipped;

    public static TelemetryPersistResult persisted(int pointCount) {
        return persisted("TDENGINE", "normalized-table", pointCount, 0, 0, pointCount, 0);
    }

    public static TelemetryPersistResult persisted(String branch,
                                                   String storageMode,
                                                   int pointCount,
                                                   int legacyStableCount,
                                                   int legacyColumnCount,
                                                   int normalizedFallbackCount,
                                                   int skippedMetricCount) {
        TelemetryPersistResult result = new TelemetryPersistResult();
        result.setBranch(branch);
        result.setStorageMode(storageMode);
        result.setPointCount(pointCount);
        result.setLegacyStableCount(legacyStableCount);
        result.setLegacyColumnCount(legacyColumnCount);
        result.setNormalizedFallbackCount(normalizedFallbackCount);
        result.setSkippedMetricCount(skippedMetricCount);
        return result;
    }

    public static TelemetryPersistResult skipped(String branch) {
        return skipped(branch, null, 0);
    }

    public static TelemetryPersistResult skipped(String branch, String storageMode, int skippedMetricCount) {
        TelemetryPersistResult result = new TelemetryPersistResult();
        result.setBranch(branch);
        result.setStorageMode(storageMode);
        result.setSkipped(true);
        result.setPointCount(0);
        result.setLegacyStableCount(0);
        result.setLegacyColumnCount(0);
        result.setNormalizedFallbackCount(0);
        result.setSkippedMetricCount(skippedMetricCount);
        return result;
    }
}
