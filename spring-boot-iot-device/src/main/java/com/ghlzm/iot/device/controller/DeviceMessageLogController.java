package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceMessageTraceQuery;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.service.DeviceMessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备消息日志控制器。
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

    @GetMapping("/api/device/message-trace/page")
    public R<PageResult<DeviceMessageLog>> pageTraceLogs(DeviceMessageTraceQuery query,
                                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(deviceMessageService.pageMessageTraceLogs(query, pageNum, pageSize));
    }
}
