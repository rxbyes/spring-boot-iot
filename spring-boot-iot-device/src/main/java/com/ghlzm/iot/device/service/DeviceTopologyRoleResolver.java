package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.model.DeviceTopologyRole;

public interface DeviceTopologyRoleResolver {
    /**
     * Resolution priority: nodeType=2 -> collectorRtu productKey -> childRelation -> STANDALONE.
     */
    DeviceTopologyRole resolve(Long productId, Integer nodeType, String productKey);
    DeviceTopologyRole resolveByDeviceCode(String deviceCode);
}
