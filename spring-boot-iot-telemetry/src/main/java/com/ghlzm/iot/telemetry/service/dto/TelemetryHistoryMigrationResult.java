package com.ghlzm.iot.telemetry.service.dto;

import lombok.Data;

/**
 * 历史 TDengine 数据迁移结果。
 */
@Data
public class TelemetryHistoryMigrationResult {

    private String source;
    private int migratedDeviceCount;
    private int scannedPointCount;
    private int writtenPointCount;
    private int latestProjectedPointCount;
}
