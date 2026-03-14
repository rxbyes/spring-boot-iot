package com.ghlzm.iot.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;

import java.util.List;

public interface DeviceService extends IService<Device> {

    Device addDevice(DeviceAddDTO dto);

    Device getRequiredById(Long id);

    Device getRequiredByCode(String deviceCode);

    List<DeviceProperty> listProperties(String deviceCode);

    List<DeviceMessageLog> listMessageLogs(String deviceCode);
}
