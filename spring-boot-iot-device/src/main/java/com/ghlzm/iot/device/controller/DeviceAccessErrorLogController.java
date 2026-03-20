package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceAccessErrorQuery;
import com.ghlzm.iot.device.entity.DeviceAccessErrorLog;
import com.ghlzm.iot.device.service.DeviceAccessErrorLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备接入失败归档控制器。
 */
@RestController
@RequestMapping("/api/device/access-error")
public class DeviceAccessErrorLogController {

    private final DeviceAccessErrorLogService deviceAccessErrorLogService;

    public DeviceAccessErrorLogController(DeviceAccessErrorLogService deviceAccessErrorLogService) {
        this.deviceAccessErrorLogService = deviceAccessErrorLogService;
    }

    @GetMapping("/page")
    public R<PageResult<DeviceAccessErrorLog>> pageLogs(DeviceAccessErrorQuery query,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(deviceAccessErrorLogService.pageLogs(query, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<DeviceAccessErrorLog> getById(@PathVariable Long id) {
        DeviceAccessErrorLog log = deviceAccessErrorLogService.getById(id);
        if (log == null) {
            return R.fail(404, "失败报文归档不存在或已删除");
        }
        return R.ok(log);
    }
}
