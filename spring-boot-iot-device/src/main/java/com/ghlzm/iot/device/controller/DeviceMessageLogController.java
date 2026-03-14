package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.service.DeviceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DeviceMessageLogController {

    private final DeviceService deviceService;

    public DeviceMessageLogController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/device/{deviceCode}/message-logs")
    public R<List<DeviceMessageLog>> getLogs(@PathVariable("deviceCode") String deviceCode) {
        return R.ok(deviceService.listMessageLogs(deviceCode));
    }
}
