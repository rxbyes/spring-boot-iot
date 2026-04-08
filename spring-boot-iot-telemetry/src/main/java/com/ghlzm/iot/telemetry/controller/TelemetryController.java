package com.ghlzm.iot.telemetry.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.telemetry.service.TelemetryHistoryMigrationService;
import com.ghlzm.iot.telemetry.service.TelemetryQueryService;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchRequest;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchResponse;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryMigrationRequest;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryMigrationResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:13
 */

@RestController
public class TelemetryController {

    private final TelemetryQueryService telemetryQueryService;
    private final TelemetryHistoryMigrationService telemetryHistoryMigrationService;

    public TelemetryController(TelemetryQueryService telemetryQueryService,
                               TelemetryHistoryMigrationService telemetryHistoryMigrationService) {
        this.telemetryQueryService = telemetryQueryService;
        this.telemetryHistoryMigrationService = telemetryHistoryMigrationService;
    }

    @GetMapping("/api/telemetry/latest")
    public R<Map<String, Object>> latest(@RequestParam("deviceId") Long deviceId) {
        return R.ok(telemetryQueryService.getLatest(deviceId));
    }

    @PostMapping("/api/telemetry/history/batch")
    public R<TelemetryHistoryBatchResponse> historyBatch(@RequestBody TelemetryHistoryBatchRequest request) {
        return R.ok(telemetryQueryService.getHistoryBatch(request));
    }

    @PostMapping("/api/telemetry/migrate-history")
    public R<TelemetryHistoryMigrationResult> migrateHistory(@RequestBody TelemetryHistoryMigrationRequest request) {
        return R.ok(telemetryHistoryMigrationService.migrate(request));
    }
}
