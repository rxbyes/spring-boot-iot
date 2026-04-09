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
 * Governance approval transition entity.
 */
@Data
@TableName("sys_governance_approval_transition")
public class GovernanceApprovalTransition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long orderId;

    private String fromStatus;

    private String toStatus;

    private Long actorUserId;

    private String transitionComment;

    private Long createBy;

    private Date createTime;

    @TableLogic
    private Integer deleted;
}
