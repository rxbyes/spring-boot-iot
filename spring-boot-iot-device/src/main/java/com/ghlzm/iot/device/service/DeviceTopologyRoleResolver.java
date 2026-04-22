package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.model.DeviceTopologyRole;

public interface DeviceTopologyRoleResolver {
    DeviceTopologyRole resolve(Long productId, Integer nodeType, String productKey);
    DeviceTopologyRole resolveByDeviceCode(String deviceCode);
}
