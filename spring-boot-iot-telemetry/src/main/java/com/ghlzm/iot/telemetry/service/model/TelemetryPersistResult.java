package com.ghlzm.iot.telemetry.service.model;

import lombok.Data;

/**
 * 时序落库结果。
 */
@Data
public class TelemetryPersistResult {

    private String branch;
    private Integer pointCount;
    private boolean skipped;

    public static TelemetryPersistResult persisted(int pointCount) {
        TelemetryPersistResult result = new TelemetryPersistResult();
        result.setBranch("TDENGINE");
        result.setPointCount(pointCount);
        return result;
    }

    public static TelemetryPersistResult skipped(String branch) {
        TelemetryPersistResult result = new TelemetryPersistResult();
        result.setBranch(branch);
        result.setSkipped(true);
        result.setPointCount(0);
        return result;
    }
}
