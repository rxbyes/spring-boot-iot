package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.vo.ProductActivityStatRow;
import java.time.LocalDateTime;

/**
 * 设备在线会话服务。
 */
public interface DeviceOnlineSessionService {

    /**
     * 记录设备在线心跳，若不存在活动会话则新建。
     */
    void recordOnlineHeartbeat(Device device, LocalDateTime reportTime);

    /**
     * 关闭当前活动会话。
     */
    void closeActiveSession(Device device, LocalDateTime offlineTime, String closeReason);

    /**
     * 查询产品最近 30 天在线时长统计。
     */
    ProductActivityStatRow loadProductDurationStat(Long productId, LocalDateTime thirtyDaysStart, LocalDateTime statTime);
}
