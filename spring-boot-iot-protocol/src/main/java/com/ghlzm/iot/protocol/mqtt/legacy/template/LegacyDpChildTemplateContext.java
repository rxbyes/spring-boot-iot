package com.ghlzm.iot.protocol.mqtt.legacy.template;

import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpRelationRule;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * legacy `$dp` 子模板匹配与执行上下文。
 */
public record LegacyDpChildTemplateContext(LegacyDpRelationRule relationRule,
                                           String logicalCode,
                                           Object logicalPayload,
                                           Map<String, Object> parentProperties) {

    public LegacyDpChildTemplateContext {
        if (parentProperties == null || parentProperties.isEmpty()) {
            parentProperties = Map.of();
        } else {
            parentProperties = Collections.unmodifiableMap(new LinkedHashMap<>(parentProperties));
        }
    }
}
