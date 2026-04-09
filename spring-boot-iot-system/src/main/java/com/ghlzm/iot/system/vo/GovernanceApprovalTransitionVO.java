package com.ghlzm.iot.system.vo;

import java.util.Date;
import lombok.Data;

/**
 * Governance approval transition view.
 */
@Data
public class GovernanceApprovalTransitionVO {

    private Long id;

    private String fromStatus;

    private String toStatus;

    private Long actorUserId;

    private String transitionComment;

    private Date createTime;
}
