package com.ghlzm.iot.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Governance approval resubmit request DTO.
 */
@Data
public class GovernanceApprovalResubmitDTO {

    /**
     * New approver user id.
     */
    @NotNull(message = "复核人不能为空")
    @Positive(message = "复核人无效")
    private Long approverUserId;

    /**
     * Resubmit comment.
     */
    private String comment;
}
