package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.dto.DeviceBatchAddDTO;
import com.ghlzm.iot.device.dto.DeviceBatchDeleteDTO;
import com.ghlzm.iot.device.dto.DeviceReplaceDTO;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.vo.DeviceBatchAddResultVO;
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DeviceMetricOptionVO;
import com.ghlzm.iot.device.vo.DeviceOptionVO;
import com.ghlzm.iot.device.vo.DevicePageVO;
import com.ghlzm.iot.device.vo.DeviceReplaceResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
    public R<DeviceDetailVO> add(@RequestBody @Valid DeviceAddDTO dto) {
        // 设备创建逻辑放在服务层，控制层仅做参数接收。
        return R.ok(deviceService.addDevice(dto));
    }

    @PostMapping("/api/device/batch-add")
    public R<DeviceBatchAddResultVO> batchAdd(@RequestBody @Valid DeviceBatchAddDTO dto) {
        return R.ok(deviceService.batchAddDevices(dto.getItems()));
    }

    @GetMapping("/api/device/{id}")
    public R<DeviceDetailVO> getById(@PathVariable("id") Long id) {
        return R.ok(deviceService.getDetailById(id));
    }

    @GetMapping("/api/device/code/{deviceCode}")
    public R<DeviceDetailVO> getByCode(@PathVariable String deviceCode) {
        return R.ok(deviceService.getDetailByCode(deviceCode));
    }

    @GetMapping("/api/device/page")
    public R<PageResult<DevicePageVO>> page(@RequestParam(required = false) Long deviceId,
                                            @RequestParam(required = false) String productKey,
                                            @RequestParam(required = false) String deviceCode,
                                            @RequestParam(required = false) String deviceName,
                                            @RequestParam(required = false) Integer onlineStatus,
                                            @RequestParam(required = false) Integer activateStatus,
                                            @RequestParam(required = false) Integer deviceStatus,
                                            @RequestParam(defaultValue = "1") Long pageNum,
                                            @RequestParam(defaultValue = "10") Long pageSize) {
        return R.ok(deviceService.pageDevices(
                deviceId,
                productKey,
                deviceCode,
                deviceName,
                onlineStatus,
                activateStatus,
                deviceStatus,
                pageNum,
                pageSize
        ));
    }

    @PutMapping("/api/device/{id}")
    public R<DeviceDetailVO> update(@PathVariable("id") Long id, @RequestBody @Valid DeviceAddDTO dto) {
        return R.ok(deviceService.updateDevice(id, dto));
    }

    @PostMapping("/api/device/{id}/replace")
    public R<DeviceReplaceResultVO> replace(@PathVariable("id") Long id, @RequestBody @Valid DeviceReplaceDTO dto) {
        return R.ok(deviceService.replaceDevice(id, dto));
    }

    @DeleteMapping("/api/device/{id}")
    public R<Void> delete(@PathVariable("id") Long id) {
        deviceService.deleteDevice(id);
        return R.ok();
    }

    @PostMapping("/api/device/batch-delete")
    public R<Void> batchDelete(@RequestBody @Valid DeviceBatchDeleteDTO dto) {
        deviceService.batchDeleteDevices(dto.getIds());
        return R.ok();
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
