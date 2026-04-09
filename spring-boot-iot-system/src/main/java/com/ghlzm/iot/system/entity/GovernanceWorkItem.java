package com.ghlzm.iot.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * Governance work item entity.
 */
@Data
@TableName("iot_governance_work_item")
public class GovernanceWorkItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
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
    private Date dueTime;
    private Date resolvedTime;
    private Date closedTime;
    private Long createBy;
    private Date createTime;
    private Long updateBy;
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
