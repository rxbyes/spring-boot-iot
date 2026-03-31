package com.ghlzm.iot.telemetry.service;

import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryMigrationRequest;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryMigrationResult;

/**
 * TDengine 历史数据迁移服务。
 */
public interface TelemetryHistoryMigrationService {

    TelemetryHistoryMigrationResult migrate(TelemetryHistoryMigrationRequest request);
}
