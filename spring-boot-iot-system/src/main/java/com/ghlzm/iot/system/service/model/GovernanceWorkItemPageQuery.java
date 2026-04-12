package com.ghlzm.iot.system.service.model;

import lombok.Data;

@Data
public class GovernanceWorkItemPageQuery {

    private String workItemCode;
    private String workStatus;
    private String executionStatus;
    private String subjectType;
    private Long subjectId;
    private Long productId;
    private Long riskMetricId;
    private Long assigneeUserId;
    private String keyword;
    private Long pageNum;
    private Long pageSize;
}
