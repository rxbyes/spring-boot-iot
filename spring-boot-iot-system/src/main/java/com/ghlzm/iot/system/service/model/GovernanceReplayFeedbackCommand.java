package com.ghlzm.iot.system.service.model;

public record GovernanceReplayFeedbackCommand(
        Long workItemId,
        Long approvalOrderId,
        Long releaseBatchId,
        String traceId,
        String deviceCode,
        String productKey,
        String recommendedDecision,
        String adoptedDecision,
        String executionOutcome,
        String rootCauseCode,
        String operatorSummary
) {
}
