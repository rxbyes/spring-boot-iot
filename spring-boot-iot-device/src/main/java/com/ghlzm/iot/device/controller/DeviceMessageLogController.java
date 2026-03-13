package com.ghlzm.iot.device.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:38
 */
@RestController
public class DeviceMessageLogController {

    private final DeviceMapper deviceMapper;
    private final DeviceMessageLogMapper deviceMessageLogMapper;

    public DeviceMessageLogController(DeviceMapper deviceMapper,
                                      DeviceMessageLogMapper deviceMessageLogMapper) {
        this.deviceMapper = deviceMapper;
        this.deviceMessageLogMapper = deviceMessageLogMapper;
    }

    @GetMapping("/device/{deviceCode}/message-logs")
    public R<?> getLogs(@PathVariable String deviceCode) {
        Device device = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceCode, deviceCode)
                        .eq(Device::getDeleted, 0)
                        .last("limit 1")
        );

        if (device == null) {
            return R.fail("设备不存在");
        }

        List<DeviceMessageLog> logs = deviceMessageLogMapper.selectList(
                new LambdaQueryWrapper<DeviceMessageLog>()
                        .eq(DeviceMessageLog::getDeviceId, device.getId())
                        .orderByDesc(DeviceMessageLog::getReportTime)
                        .last("limit 20")
        );

        return R.ok(logs);
    }
}

