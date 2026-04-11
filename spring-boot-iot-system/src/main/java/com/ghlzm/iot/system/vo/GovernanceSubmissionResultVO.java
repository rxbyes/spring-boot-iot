package com.ghlzm.iot.system.vo;

import lombok.Data;

/**
 * Shared submission result for governance-aware domain writes.
 */
@Data
public class GovernanceSubmissionResultVO {

    private Long workItemId;
    private Long approvalOrderId;
    private String approvalStatus;
    private String executionStatus;

    public static GovernanceSubmissionResultVO directApplied(Long workItemId) {
        GovernanceSubmissionResultVO result = new GovernanceSubmissionResultVO();
        result.setWorkItemId(workItemId);
        result.setExecutionStatus("DIRECT_APPLIED");
        return result;
    }

    public static GovernanceSubmissionResultVO pendingApproval(Long workItemId, Long approvalOrderId) {
        GovernanceSubmissionResultVO result = new GovernanceSubmissionResultVO();
        result.setWorkItemId(workItemId);
        result.setApprovalOrderId(approvalOrderId);
        result.setApprovalStatus("PENDING");
        result.setExecutionStatus("PENDING_APPROVAL");
        return result;
    }
}
