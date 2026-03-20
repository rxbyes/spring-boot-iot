package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.service.DeviceAuthService;
import org.springframework.stereotype.Service;

/**
 * 设备认证服务最小实现。
 * 当前阶段只做最简单的设备身份解析与静态凭证校验，不直接接管 Broker 的认证流程。
 */
@Service
public class DeviceAuthServiceImpl implements DeviceAuthService {

    private final DeviceMapper deviceMapper;

    public DeviceAuthServiceImpl(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    @Override
    public Device findByMqttIdentity(String clientId, String username) {
        // 基础版认证要求 clientId 和 username 都存在且一致，随后按 deviceCode 查找设备。
        if (!hasText(clientId) || !hasText(username) || !clientId.equals(username)) {
            return null;
        }

        return deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceCode, clientId)
                        .eq(Device::getDeleted, 0)
                        .last("limit 1")
        );
    }

    @Override
    public Device authenticateOrThrow(String clientId, String username, String password) {
        if (!hasText(clientId)) {
            throw new BizException("MQTT 认证失败: clientId 不能为空");
        }
        if (!hasText(username)) {
            throw new BizException("MQTT 认证失败: username 不能为空");
        }
        if (!hasText(password)) {
            throw new BizException("MQTT 认证失败: password 不能为空");
        }
        if (!clientId.equals(username)) {
            throw new BizException("MQTT 认证失败: clientId 与 username 必须一致");
        }

        // 基础版认证规则：clientId = deviceCode, username = deviceCode, password = deviceSecret。
        Device device = findByMqttIdentity(clientId, username);
        if (device == null) {
            throw new BizException("MQTT 认证失败: 设备不存在: " + clientId);
        }
        if (!clientId.equals(device.getDeviceCode())) {
            throw new BizException("MQTT 认证失败: clientId 必须等于 deviceCode");
        }
        if (!username.equals(device.getDeviceCode())) {
            throw new BizException("MQTT 认证失败: username 必须等于 deviceCode");
        }
        if (!password.equals(device.getDeviceSecret())) {
            throw new BizException("MQTT 认证失败: password 不正确");
        }
        return device;
    }

    @Override
    public boolean validateSimpleCredential(String clientId, String username, String password) {
        // 为后续 Broker 集成保留布尔型判断接口；具体失败原因由 authenticateOrThrow 提供。
        if (!hasText(password)) {
            return false;
        }
        try {
            authenticateOrThrow(clientId, username, password);
            return true;
        } catch (BizException ex) {
            return false;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
