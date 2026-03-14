package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.entity.Device;

/**
 * 设备认证服务骨架。
 * Phase 2 Task 1 只提供最小扩展点定义，不在此阶段接管 Broker 认证流程。
 */
public interface DeviceAuthService {

    /**
     * 按 MQTT 基础认证信息查找设备。
     * 当前基础版认证约定 clientId 和 username 都必须等于 deviceCode。
     */
    Device findByMqttIdentity(String clientId, String username);

    /**
     * 执行基础 MQTT 认证，认证成功时返回设备，失败时抛出业务异常。
     */
    Device authenticateOrThrow(String clientId, String username, String password);

    /**
     * 最小静态凭证校验。
     * 认证失败时返回 false，便于后续 Broker 集成侧按需复用。
     */
    boolean validateSimpleCredential(String clientId, String username, String password);
}
