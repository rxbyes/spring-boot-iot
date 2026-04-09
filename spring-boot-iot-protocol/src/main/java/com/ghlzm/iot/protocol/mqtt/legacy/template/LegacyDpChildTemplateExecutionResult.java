package com.ghlzm.iot.protocol.mqtt.legacy.template;

import com.ghlzm.iot.protocol.core.model.ProtocolMetricEvidence;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * legacy `$dp` 子模板执行结果。
 */
public record LegacyDpChildTemplateExecutionResult(String templateCode,
                                                   Map<String, Object> childProperties,
                                                   List<String> parentRemovalKeys,
                                                   boolean statusMirrorApplied,
                                                   String canonicalizationStrategy,
                                                   LocalDateTime childTimestamp,
                                                   String rawPayload,
                                                   List<ProtocolMetricEvidence> metricEvidence) {

    public LegacyDpChildTemplateExecutionResult {
        if (childProperties == null || childProperties.isEmpty()) {
            childProperties = Map.of();
        } else {
            childProperties = Collections.unmodifiableMap(new LinkedHashMap<>(childProperties));
        }
        parentRemovalKeys = parentRemovalKeys == null ? List.of() : List.copyOf(parentRemovalKeys);
        metricEvidence = metricEvidence == null ? List.of() : List.copyOf(metricEvidence);
    }
}
