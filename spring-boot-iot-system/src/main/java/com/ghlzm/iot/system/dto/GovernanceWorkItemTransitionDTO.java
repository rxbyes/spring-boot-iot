package com.ghlzm.iot.system.dto;

import lombok.Data;

@Data
public class GovernanceWorkItemTransitionDTO {

    private String comment;

    private Long workItemId;

    private Long approvalOrderId;

    private Long releaseBatchId;

    private String traceId;

    private String deviceCode;

    private String productKey;

    private String recommendedDecision;

    private String adoptedDecision;

    private String executionOutcome;

    private String rootCauseCode;

    private String operatorSummary;
}
