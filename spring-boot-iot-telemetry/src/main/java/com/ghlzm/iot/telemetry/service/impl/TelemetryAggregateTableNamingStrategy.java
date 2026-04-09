package com.ghlzm.iot.telemetry.service.impl;

import org.springframework.stereotype.Component;

/**
 * Telemetry 小时聚合表命名策略。
 */
@Component
public class TelemetryAggregateTableNamingStrategy {

    public static final String STABLE_NAME = "iot_agg_measure_hour";

    public String resolveStableName() {
        return STABLE_NAME;
    }

    public String resolveChildTableName(Long tenantId, Long deviceId) {
        long safeTenantId = tenantId == null ? 0L : tenantId;
        long safeDeviceId = deviceId == null ? 0L : deviceId;
        return "tb_ah_" + safeTenantId + "_" + safeDeviceId;
    }
}
