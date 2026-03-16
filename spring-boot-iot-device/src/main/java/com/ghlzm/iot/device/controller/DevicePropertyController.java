package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.service.DeviceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备属性控制器，提供按设备编码查询最新属性的只读接口。
 */
@RestController
public class DevicePropertyController {

    private final DeviceService deviceService;

    public DevicePropertyController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/api/device/{deviceCode}/properties")
    public R<List<DeviceProperty>> getProperties(@PathVariable String deviceCode) {
        return R.ok(deviceService.listProperties(deviceCode));
    }
}
