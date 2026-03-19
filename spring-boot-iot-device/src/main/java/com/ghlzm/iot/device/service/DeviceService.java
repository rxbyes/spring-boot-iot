package com.ghlzm.iot.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DeviceMetricOptionVO;
import com.ghlzm.iot.device.vo.DeviceOptionVO;
import com.ghlzm.iot.device.vo.DevicePageVO;

import java.util.List;

/**
 * 设备服务，负责设备新增、设备查询以及设备属性查询。
 */
public interface DeviceService extends IService<Device> {

    /**
     * 新增设备。
     */
    DeviceDetailVO addDevice(DeviceAddDTO dto);

    /**
     * 按主键查询设备，不存在时抛业务异常。
     */
    Device getRequiredById(Long id);

    /**
     * 按 deviceCode 查询设备，不存在时抛业务异常。
     */
    Device getRequiredByCode(String deviceCode);

    /**
     * 按主键查询设备详情。
     */
    DeviceDetailVO getDetailById(Long id);

    /**
     * 按设备编码查询设备详情。
     */
    DeviceDetailVO getDetailByCode(String deviceCode);

    /**
     * 分页查询设备台账。
     */
    PageResult<DevicePageVO> pageDevices(Long deviceId,
                                         String productKey,
                                         String deviceCode,
                                         String deviceName,
                                         Integer onlineStatus,
                                         Integer activateStatus,
                                         Integer deviceStatus,
                                         Long pageNum,
                                         Long pageSize);

    /**
     * 更新设备主数据。
     */
    DeviceDetailVO updateDevice(Long id, DeviceAddDTO dto);

    /**
     * 删除单个设备。
     */
    void deleteDevice(Long id);

    /**
     * 批量删除设备。
     */
    void batchDeleteDevices(List<Long> ids);

    /**
     * 根据设备编码查询最新属性列表。
     */
    List<DeviceProperty> listProperties(String deviceCode);

    /**
     * 查询可用于绑定风险点的设备选项。
     */
    List<DeviceOptionVO> listDeviceOptions();

    /**
     * 查询指定设备的测点选项。
     */
    List<DeviceMetricOptionVO> listMetricOptions(Long deviceId);
}
