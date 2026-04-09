package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;

/**
 * 厂商字段映射规则运行时解析服务。
 */
public interface VendorMetricMappingRuntimeService {

    MappingResolution resolveForGovernance(Product product, String rawIdentifier, String logicalChannelCode);

    MappingResolution resolveForRuntime(Product product, DeviceUpMessage upMessage, String rawIdentifier, String logicalChannelCode);

    String normalizeApplyIdentifier(Product product, String identifier);

    record MappingResolution(Long ruleId,
                             String targetNormativeIdentifier,
                             String rawIdentifier,
                             String logicalChannelCode) {
    }
}
