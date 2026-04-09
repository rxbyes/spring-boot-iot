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
 * Governance approval order entity.
 */
@Data
@TableName("sys_governance_approval_order")
public class GovernanceApprovalOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String actionCode;

    private String actionName;

    private String subjectType;

    private Long subjectId;

    private String status;

    private Long operatorUserId;

    private Long approverUserId;

    private String payloadJson;

    private String approvalComment;

    private Date approvedTime;

    private Long createBy;

    private Date createTime;

    private Long updateBy;

    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
