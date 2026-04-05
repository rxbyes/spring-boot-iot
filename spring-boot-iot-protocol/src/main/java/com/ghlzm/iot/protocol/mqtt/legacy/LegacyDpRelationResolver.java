package com.ghlzm.iot.protocol.mqtt.legacy;

import java.util.List;

/**
 * legacy `$dp` 父设备逻辑通道关系解析接口。
 */
@FunctionalInterface
public interface LegacyDpRelationResolver {

    List<LegacyDpRelationRule> listRulesByParentDeviceCode(String parentDeviceCode);

    static LegacyDpRelationResolver noop() {
        return parentDeviceCode -> List.of();
    }
}
