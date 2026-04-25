package com.ghlzm.iot.system.observability;

import java.util.Map;

/**
 * HTTP 业务事件字典解析结果。
 */
public record BusinessEventDefinition(
        String eventCode,
        String eventName,
        String domainCode,
        String actionCode,
        String objectType,
        String objectId,
        boolean dictionaryMatched,
        Map<String, Object> metadata
) {
}
