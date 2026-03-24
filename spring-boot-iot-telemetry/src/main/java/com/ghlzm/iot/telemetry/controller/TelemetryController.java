package com.ghlzm.iot.telemetry.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.telemetry.service.TelemetryQueryService;
import org.springframework.web.bind.annotation.GetMapping;
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

    public TelemetryController(TelemetryQueryService telemetryQueryService) {
        this.telemetryQueryService = telemetryQueryService;
    }

    @GetMapping("/api/telemetry/latest")
    public R<Map<String, Object>> latest(@RequestParam("deviceId") Long deviceId) {
        return R.ok(telemetryQueryService.getLatest(deviceId));
    }
}
