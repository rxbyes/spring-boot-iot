package com.ghlzm.iot.system.service.model;

public record GovernanceWorkItemCommand(
        String workItemCode,
        String subjectType,
        Long subjectId,
        Long productId,
        Long riskMetricId,
        Long releaseBatchId,
        Long approvalOrderId,
        String traceId,
        String deviceCode,
        String productKey,
        Long assigneeUserId,
        String sourceStage,
        String blockingReason,
        String snapshotJson,
        String priorityLevel,
        Long operatorUserId
) {
}
