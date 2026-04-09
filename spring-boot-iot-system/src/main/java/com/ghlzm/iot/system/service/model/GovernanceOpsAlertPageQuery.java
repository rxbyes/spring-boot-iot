package com.ghlzm.iot.system.service.model;

import lombok.Data;

@Data
public class GovernanceOpsAlertPageQuery {

    private String alertType;
    private String alertStatus;
    private String subjectType;
    private Long subjectId;
    private Long productId;
    private Long riskMetricId;
    private String severityLevel;
    private Long assigneeUserId;
    private Long pageNum;
    private Long pageSize;
}
