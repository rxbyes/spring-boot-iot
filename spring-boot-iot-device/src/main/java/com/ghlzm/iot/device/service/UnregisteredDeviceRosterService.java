package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.vo.DevicePageVO;

import java.util.List;

/**
 * 未登记设备名单服务。
 */
public interface UnregisteredDeviceRosterService {

    /**
     * 统计满足筛选条件的未登记上报设备数量。
     */
    long countByFilters(String productKey, String deviceCode);

    /**
     * 分页查询未登记上报设备。
     */
    List<DevicePageVO> listByFilters(String productKey, String deviceCode, long offset, long limit);
}
