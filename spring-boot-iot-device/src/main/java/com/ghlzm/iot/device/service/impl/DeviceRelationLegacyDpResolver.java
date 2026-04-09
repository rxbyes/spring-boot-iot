package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.service.DeviceRelationService;
import com.ghlzm.iot.device.service.model.DeviceRelationRule;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpRelationResolver;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpRelationRule;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 基于设备关系主数据的 legacy `$dp` 关系解析器。
 */
@Component
public class DeviceRelationLegacyDpResolver implements LegacyDpRelationResolver {

    private final DeviceRelationService deviceRelationService;

    public DeviceRelationLegacyDpResolver(DeviceRelationService deviceRelationService) {
        this.deviceRelationService = deviceRelationService;
    }

    @Override
    public List<LegacyDpRelationRule> listRulesByParentDeviceCode(String parentDeviceCode) {
        List<DeviceRelationRule> rules = deviceRelationService.listEnabledRulesByParentDeviceCode(parentDeviceCode);
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        return rules.stream()
                .map(rule -> new LegacyDpRelationRule(
                        rule.getLogicalChannelCode(),
                        rule.getChildDeviceCode(),
                        rule.getCanonicalizationStrategy(),
                        rule.getStatusMirrorStrategy()
                ))
                .toList();
    }
}
