package com.ghlzm.iot.system.service;

import com.ghlzm.iot.system.entity.GovernanceApprovalOrder;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionExecutionResult;

/**
 * Executes business side effects for a governance approval action.
 */
public interface GovernanceApprovalActionExecutor {

    /**
     * Whether this executor supports the given action code.
     *
     * @param actionCode governance action code
     * @return true if supported
     */
    boolean supports(String actionCode);

    /**
     * Execute the side effect for the approval order and return the updated payload.
     *
     * @param order approval order
     * @return execution result
     */
    GovernanceApprovalActionExecutionResult execute(GovernanceApprovalOrder order);
}
