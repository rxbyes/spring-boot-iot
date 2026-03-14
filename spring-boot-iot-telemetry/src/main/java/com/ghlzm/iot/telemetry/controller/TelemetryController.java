package com.ghlzm.iot.telemetry.controller;

import com.ghlzm.iot.common.response.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:13
 */

@RestController
public class TelemetryController {

    @GetMapping("/api/telemetry/latest")
    public R<?> latest(@RequestParam("deviceId") Long deviceId) {
        Map<String, Object> result = new HashMap<>();
        result.put("deviceId", deviceId);
        result.put("temperature", 26.5);
        result.put("humidity", 68);
        return R.ok(result);
    }
}
