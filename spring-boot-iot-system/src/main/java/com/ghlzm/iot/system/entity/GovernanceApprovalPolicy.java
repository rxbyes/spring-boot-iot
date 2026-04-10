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
 * Governance approval policy entity.
 */
@Data
@TableName("sys_governance_approval_policy")
public class GovernanceApprovalPolicy implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String scopeType;

    private String actionCode;

    private String approverMode;

    private Long approverUserId;

    private Integer enabled;

    private String remark;

    private Long createBy;

    private Date createTime;

    private Long updateBy;

    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
