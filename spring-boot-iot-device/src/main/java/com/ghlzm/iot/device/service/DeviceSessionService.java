package com.ghlzm.iot.device.service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 设备会话服务。
 * Phase 2 Task 4 提供最小 Redis 会话管理和在线状态维护能力。
 */
public interface DeviceSessionService {

    /**
     * 标记设备上线，并初始化最小会话信息。
     */
    void online(String deviceCode, String clientId);

    /**
     * 标记设备下线，并同步清理会话。
     */
    void offline(String deviceCode);

    /**
     * 收到 MQTT 消息时刷新最后活跃时间。
     */
    void refreshLastSeen(String deviceCode, String clientId, String topic);

    /**
     * 查询设备是否在线。
     */
    boolean isOnline(String deviceCode);

    /**
     * 查询最后一次收到消息的时间。
     */
    LocalDateTime getLastSeenTime(String deviceCode);

    /**
     * 返回当前设备会话的 Redis Key。
     */
    String buildSessionKey(String deviceCode);

    /**
     * 返回建议的会话过期时间，便于后续扩展使用。
     */
    Duration getSessionTtl();
}
