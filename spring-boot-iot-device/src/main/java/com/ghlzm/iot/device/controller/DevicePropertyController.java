package com.ghlzm.iot.device.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:37
 */
@RestController
public class DevicePropertyController {

    private final DeviceMapper deviceMapper;
    private final DevicePropertyMapper devicePropertyMapper;

    public DevicePropertyController(DeviceMapper deviceMapper,
                                    DevicePropertyMapper devicePropertyMapper) {
        this.deviceMapper = deviceMapper;
        this.devicePropertyMapper = devicePropertyMapper;
    }

    @GetMapping("/device/{deviceCode}/properties")
    public R<?> getProperties(@PathVariable String deviceCode) {
        Device device = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceCode, deviceCode)
                        .eq(Device::getDeleted, 0)
                        .last("limit 1")
        );

        if (device == null) {
            return R.fail("设备不存在");
        }

        List<DeviceProperty> list = devicePropertyMapper.selectList(
                new LambdaQueryWrapper<DeviceProperty>()
                        .eq(DeviceProperty::getDeviceId, device.getId())
                        .orderByDesc(DeviceProperty::getUpdateTime)
        );

        return R.ok(list);
    }
}

