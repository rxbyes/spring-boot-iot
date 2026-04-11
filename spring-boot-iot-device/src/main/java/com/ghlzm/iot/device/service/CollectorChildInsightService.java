package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.vo.CollectorChildInsightOverviewVO;

/**
 * 采集器子设备总览服务。
 */
public interface CollectorChildInsightService {

    /**
     * 按当前登录用户上下文查询采集器下子设备只读总览。
     */
    CollectorChildInsightOverviewVO getOverview(Long currentUserId, String parentDeviceCode);
}
