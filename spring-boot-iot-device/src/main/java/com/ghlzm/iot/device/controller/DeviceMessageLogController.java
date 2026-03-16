package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.service.DeviceMessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备消息日志控制器，提供按设备编码查询最近日志的只读接口。
 */
@RestController
public class DeviceMessageLogController {

    private final DeviceMessageService deviceMessageService;

    public DeviceMessageLogController(DeviceMessageService deviceMessageService) {
        this.deviceMessageService = deviceMessageService;
    }

    @GetMapping("/api/device/{deviceCode}/message-logs")
    public R<List<DeviceMessageLog>> getLogs(@PathVariable String deviceCode) {
        return R.ok(deviceMessageService.listMessageLogs(deviceCode));
    }
}
