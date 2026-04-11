package com.ghlzm.iot.system.vo;

import java.util.Date;
import lombok.Data;

@Data
public class GovernanceWorkItemVO {

    private Long id;
    private String workItemCode;
    private String subjectType;
    private Long subjectId;
    private Long productId;
    private Long riskMetricId;
    private Long releaseBatchId;
    private Long approvalOrderId;
    private String traceId;
    private String deviceCode;
    private String productKey;
    private String workStatus;
    private String priorityLevel;
    private Long assigneeUserId;
    private String sourceStage;
    private String blockingReason;
    private String snapshotJson;
    private String taskCategory;
    private String domainCode;
    private String actionCode;
    private String executionStatus;
    private String recommendationSnapshotJson;
    private String evidenceSnapshotJson;
    private String impactSnapshotJson;
    private String rollbackSnapshotJson;
    private Date dueTime;
    private Date resolvedTime;
    private Date closedTime;
    private Date createTime;
    private Date updateTime;
}
