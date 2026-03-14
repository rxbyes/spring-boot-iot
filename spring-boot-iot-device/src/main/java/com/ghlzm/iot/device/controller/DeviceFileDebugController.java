package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.service.DeviceFileService;
import com.ghlzm.iot.device.vo.DeviceFileSnapshotVO;
import com.ghlzm.iot.device.vo.DeviceFirmwareAggregateVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备文件调试控制器。
 * 只提供调试查询接口，便于前端联调查看 C.3 / C.4 在 Redis 中的消费结果。
 */
@RestController
public class DeviceFileDebugController {

    private final DeviceFileService deviceFileService;

    public DeviceFileDebugController(DeviceFileService deviceFileService) {
        this.deviceFileService = deviceFileService;
    }

    @GetMapping("/device/{deviceCode}/file-snapshots")
    public R<List<DeviceFileSnapshotVO>> getFileSnapshots(@PathVariable("deviceCode") String deviceCode) {
        return R.ok(deviceFileService.listFileSnapshots(deviceCode));
    }

    @GetMapping("/device/{deviceCode}/firmware-aggregates")
    public R<List<DeviceFirmwareAggregateVO>> getFirmwareAggregates(@PathVariable("deviceCode") String deviceCode) {
        return R.ok(deviceFileService.listFirmwareAggregates(deviceCode));
    }
}
