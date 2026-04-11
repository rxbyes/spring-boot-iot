package com.ghlzm.iot.system.vo;

import java.util.Date;
import lombok.Data;

/**
 * Governance approval order view.
 */
@Data
public class GovernanceApprovalOrderVO {

    private Long id;

    private String actionCode;

    private String actionName;

    private String subjectType;

    private Long subjectId;

    private Long workItemId;

    private String status;

    private Long operatorUserId;

    private Long approverUserId;

    private String payloadJson;

    private String approvalComment;

    private Date approvedTime;

    private Date createTime;

    private Date updateTime;
}
