package com.ghlzm.iot.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;

import java.util.List;

/**
 * 设备服务，负责设备新增、设备查询以及设备属性查询。
 */
public interface DeviceService extends IService<Device> {

    /**
     * 新增设备。
     */
    Device addDevice(DeviceAddDTO dto);

    /**
     * 按主键查询设备，不存在时抛业务异常。
     */
    Device getRequiredById(Long id);

    /**
     * 按 deviceCode 查询设备，不存在时抛业务异常。
     */
    Device getRequiredByCode(String deviceCode);

    /**
     * 根据设备编码查询最新属性列表。
     */
    List<DeviceProperty> listProperties(String deviceCode);
}
