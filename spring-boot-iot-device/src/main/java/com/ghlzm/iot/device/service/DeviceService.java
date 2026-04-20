package com.ghlzm.iot.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingSuggestionQuery;
import com.ghlzm.iot.device.dto.DeviceReplaceDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.vo.DeviceBatchAddResultVO;
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DeviceMetricOptionVO;
import com.ghlzm.iot.device.vo.DeviceOnboardingSuggestionVO;
import com.ghlzm.iot.device.vo.DeviceOptionVO;
import com.ghlzm.iot.device.vo.DevicePageVO;
import com.ghlzm.iot.device.vo.DeviceReplaceResultVO;

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
     * 按当前登录用户上下文新增设备。
     */
    DeviceDetailVO addDevice(Long currentUserId, DeviceAddDTO dto);

    /**
     * 按主键查询设备，不存在时抛业务异常。
     */
    Device getRequiredById(Long id);

    /**
     * 按当前登录用户上下文查询设备主数据，不存在或越权时抛业务异常。
     */
    Device getRequiredById(Long currentUserId, Long id);

    /**
     * 按 deviceCode 查询设备，不存在时抛业务异常。
     */
    Device getRequiredByCode(String deviceCode);

    /**
     * 按当前登录用户上下文查询设备主数据，不存在或越权时抛业务异常。
     */
    Device getRequiredByCode(Long currentUserId, String deviceCode);

    /**
     * 按主键查询设备详情。
     */
    DeviceDetailVO getDetailById(Long id);

    /**
     * 按当前登录用户上下文查询设备详情。
     */
    DeviceDetailVO getDetailById(Long currentUserId, Long id);

    /**
     * 按设备编码查询设备详情。
     */
    DeviceDetailVO getDetailByCode(String deviceCode);

    /**
     * 按当前登录用户上下文查询设备详情。
     */
    DeviceDetailVO getDetailByCode(Long currentUserId, String deviceCode);

    /**
     * 分页查询设备台账。
     */
    PageResult<DevicePageVO> pageDevices(Long deviceId,
                                         String keyword,
                                         String productKey,
                                         String productName,
                                         String deviceCode,
                                         String deviceName,
                                         Integer onlineStatus,
                                         Integer activateStatus,
                                         Integer deviceStatus,
                                         Integer registrationStatus,
                                         Long pageNum,
                                         Long pageSize);

    /**
     * 按当前登录用户上下文分页查询设备台账。
     */
    PageResult<DevicePageVO> pageDevices(Long currentUserId,
                                         Long deviceId,
                                         String keyword,
                                         String productKey,
                                         String productName,
                                         String deviceCode,
                                         String deviceName,
                                         Integer onlineStatus,
                                         Integer activateStatus,
                                         Integer deviceStatus,
                                         Integer registrationStatus,
                                         Long pageNum,
                                         Long pageSize);

    /**
     * 更新设备主数据。
     */
    DeviceDetailVO updateDevice(Long id, DeviceAddDTO dto);

    /**
     * 按当前登录用户上下文更新设备主数据。
     */
    DeviceDetailVO updateDevice(Long currentUserId, Long id, DeviceAddDTO dto);

    /**
     * 批量新增设备。
     */
    DeviceBatchAddResultVO batchAddDevices(List<DeviceAddDTO> items);

    /**
     * 按当前登录用户上下文批量新增设备。
     */
    DeviceBatchAddResultVO batchAddDevices(Long currentUserId, List<DeviceAddDTO> items);

    /**
     * 更换设备。
     */
    DeviceReplaceResultVO replaceDevice(Long id, DeviceReplaceDTO dto);

    /**
     * 按当前登录用户上下文更换设备。
     */
    DeviceReplaceResultVO replaceDevice(Long currentUserId, Long id, DeviceReplaceDTO dto);

    /**
     * 删除单个设备。
     */
    void deleteDevice(Long id);

    /**
     * 按当前登录用户上下文删除单个设备。
     */
    void deleteDevice(Long currentUserId, Long id);

    /**
     * 批量删除设备。
     */
    void batchDeleteDevices(List<Long> ids);

    /**
     * 按当前登录用户上下文批量删除设备。
     */
    void batchDeleteDevices(Long currentUserId, List<Long> ids);

    /**
     * 根据设备编码查询最新属性列表。
     */
    List<DeviceProperty> listProperties(String deviceCode);

    /**
     * 按当前登录用户上下文查询最新属性列表。
     */
    List<DeviceProperty> listProperties(Long currentUserId, String deviceCode);

    /**
     * 查询可用于绑定风险点的设备选项。
     */
    List<DeviceOptionVO> listDeviceOptions(boolean includeDisabled);

    /**
     * 按当前登录用户上下文查询可用于绑定风险点的设备选项。
     */
    List<DeviceOptionVO> listDeviceOptions(Long currentUserId, boolean includeDisabled);

    /**
     * 查询指定设备的测点选项。
     */
    List<DeviceMetricOptionVO> listMetricOptions(Long deviceId);

    /**
     * 按当前登录用户上下文查询指定设备的测点选项。
     */
    List<DeviceMetricOptionVO> listMetricOptions(Long currentUserId, Long deviceId);

    /**
     * 查询未登记设备的接入建议。
     */
    DeviceOnboardingSuggestionVO getOnboardingSuggestion(DeviceOnboardingSuggestionQuery query);

    /**
     * 按当前登录用户上下文查询未登记设备的接入建议。
     */
    DeviceOnboardingSuggestionVO getOnboardingSuggestion(Long currentUserId, DeviceOnboardingSuggestionQuery query);
}
