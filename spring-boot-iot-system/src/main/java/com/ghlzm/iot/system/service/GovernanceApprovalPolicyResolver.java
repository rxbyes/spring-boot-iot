package com.ghlzm.iot.system.service;

/**
 * Resolve fixed governance approvers for managed actions.
 */
public interface GovernanceApprovalPolicyResolver {

    Long resolveApproverUserId(String actionCode, Long operatorUserId);
}
