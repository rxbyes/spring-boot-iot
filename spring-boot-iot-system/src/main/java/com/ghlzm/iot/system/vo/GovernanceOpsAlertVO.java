package com.ghlzm.iot.system.vo;

import java.util.Date;
import lombok.Data;

@Data
public class GovernanceOpsAlertVO {

    private Long id;
    private String alertType;
    private String alertCode;
    private String subjectType;
    private Long subjectId;
    private Long productId;
    private Long riskMetricId;
    private Long releaseBatchId;
    private String traceId;
    private String deviceCode;
    private String productKey;
    private String alertStatus;
    private String severityLevel;
    private Long affectedCount;
    private String alertTitle;
    private String alertMessage;
    private String dimensionKey;
    private String dimensionLabel;
    private String sourceStage;
    private String snapshotJson;
    private Long assigneeUserId;
    private Date firstSeenTime;
    private Date lastSeenTime;
    private Date resolvedTime;
    private Date closedTime;
    private Date createTime;
    private Date updateTime;
}
