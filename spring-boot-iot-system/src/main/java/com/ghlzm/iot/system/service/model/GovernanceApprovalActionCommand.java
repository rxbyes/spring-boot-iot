package com.ghlzm.iot.system.service.model;

/**
 * Governance approval action command.
 */
public record GovernanceApprovalActionCommand(
        String actionCode,
        String actionName,
        String subjectType,
        Long subjectId,
        Long operatorUserId,
        Long approverUserId,
        String payloadJson,
        String approvalComment
) {
}
