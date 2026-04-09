package com.ghlzm.iot.system.service.model;

public record GovernanceOpsAlertCommand(
        String alertType,
        String alertCode,
        String subjectType,
        Long subjectId,
        Long productId,
        Long riskMetricId,
        Long releaseBatchId,
        String traceId,
        String deviceCode,
        String productKey,
        String severityLevel,
        Long affectedCount,
        String alertTitle,
        String alertMessage,
        String dimensionKey,
        String dimensionLabel,
        String sourceStage,
        String snapshotJson,
        Long operatorUserId
) {
}
