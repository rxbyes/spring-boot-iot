package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.vo.DeviceMetricOptionVO;
import com.ghlzm.iot.device.vo.DeviceOptionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备控制器，只负责设备相关的最小建档与查询入口。
 */
@RestController
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/api/device/add")
    public R<Device> add(@RequestBody @Valid DeviceAddDTO dto) {
        // 设备创建逻辑放在服务层，控制层仅做参数接收。
        return R.ok(deviceService.addDevice(dto));
    }

    @GetMapping("/api/device/{id}")
    public R<Device> getById(@PathVariable("id") Long id) {
        return R.ok(deviceService.getRequiredById(id));
    }

    @GetMapping("/api/device/code/{deviceCode}")
    public R<Device> getByCode(@PathVariable String deviceCode) {
        return R.ok(deviceService.getRequiredByCode(deviceCode));
    }

    @GetMapping("/api/device/list")
    public R<List<DeviceOptionVO>> listDeviceOptions() {
        return R.ok(deviceService.listDeviceOptions());
    }

    @GetMapping("/api/device/{deviceId}/metrics")
    public R<List<DeviceMetricOptionVO>> listMetricOptions(@PathVariable Long deviceId) {
        return R.ok(deviceService.listMetricOptions(deviceId));
    }
}
