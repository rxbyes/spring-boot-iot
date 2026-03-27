package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import org.springframework.stereotype.Component;

/**
 * Telemetry v2 子表命名策略。
 */
@Component
public class TelemetryV2TableNamingStrategy {

    public String resolveStableName(TelemetryStreamKind streamKind) {
        return streamKind.getStableName();
    }

    public String resolveChildTableName(TelemetryStreamKind streamKind, Long tenantId, Long deviceId) {
        long safeTenantId = tenantId == null ? 0L : tenantId;
        long safeDeviceId = deviceId == null ? 0L : deviceId;
        return "tb_" + streamKind.getTablePrefix() + "_" + safeTenantId + "_" + safeDeviceId;
    }
}
