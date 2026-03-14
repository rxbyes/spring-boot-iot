package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备控制器，只负责设备相关的最小建档与查询入口。
 */
@RestController
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/device/add")
    public R<Device> add(@RequestBody @Valid DeviceAddDTO dto) {
        // 设备创建逻辑放在服务层，控制层仅做参数接收。
        return R.ok(deviceService.addDevice(dto));
    }

    @GetMapping("/device/{id}")
    public R<Device> getById(@PathVariable("id") Long id) {
        return R.ok(deviceService.getRequiredById(id));
    }

    @GetMapping("/device/code/{deviceCode}")
    public R<Device> getByCode(@PathVariable("deviceCode") String deviceCode) {
        return R.ok(deviceService.getRequiredByCode(deviceCode));
    }
}
