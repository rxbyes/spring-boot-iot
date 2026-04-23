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
    long countByFilters(String keyword, String productKey, String productName, String deviceCode);

    /**
     * 按租户统计满足筛选条件的未登记上报设备数量。
     */
    long countByFilters(Long tenantId, String keyword, String productKey, String productName, String deviceCode);

    /**
     * 分页查询未登记上报设备。
     */
    List<DevicePageVO> listByFilters(String keyword, String productKey, String productName, String deviceCode, long offset, long limit);

    /**
     * 按租户分页查询未登记上报设备。
     */
    List<DevicePageVO> listByFilters(Long tenantId, String keyword, String productKey, String productName, String deviceCode, long offset, long limit);

    /**
     * 按 traceId 查询未登记上报线索。
     */
    default DevicePageVO findByTraceId(String traceId) {
        return findByTraceId(null, traceId);
    }

    /**
     * 按租户和 traceId 查询未登记上报线索。
     */
    DevicePageVO findByTraceId(Long tenantId, String traceId);
}
