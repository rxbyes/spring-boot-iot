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
 * Governance ops alert entity.
 */
@Data
@TableName("iot_governance_ops_alert")
public class GovernanceOpsAlert implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
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
    private Long createBy;
    private Date createTime;
    private Long updateBy;
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
