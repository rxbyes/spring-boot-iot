package com.ghlzm.iot.admin.observability.alerting;

import java.util.Map;

/**
 * 规则化运维告警触发信息。
 */
public record ObservabilityAlertTrigger(String ruleType,
                                        String dimensionKey,
                                        String dimensionLabel,
                                        String metricLabel,
                                        long observedValue,
                                        long threshold,
                                        Integer windowMinutes,
                                        Integer durationMinutes,
                                        String summary,
                                        Map<String, Object> context) {

    public ObservabilityAlertTrigger {
        context = context == null ? Map.of() : Map.copyOf(context);
    }
}
