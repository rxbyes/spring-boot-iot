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
 * Governance replay feedback entity.
 */
@Data
@TableName("sys_governance_replay_feedback")
public class GovernanceReplayFeedback implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long workItemId;

    private Long approvalOrderId;

    private Long releaseBatchId;

    private String adoptedDecision;

    private String executionOutcome;

    private String rootCauseCode;

    private String feedbackJson;

    private Long createBy;

    private Date createTime;

    @TableLogic
    private Integer deleted;
}
