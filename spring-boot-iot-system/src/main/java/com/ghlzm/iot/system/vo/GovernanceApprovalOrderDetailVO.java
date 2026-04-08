package com.ghlzm.iot.system.vo;

import java.util.List;
import lombok.Data;

/**
 * Governance approval detail view.
 */
@Data
public class GovernanceApprovalOrderDetailVO {

    private GovernanceApprovalOrderVO order;

    private List<GovernanceApprovalTransitionVO> transitions;
}
