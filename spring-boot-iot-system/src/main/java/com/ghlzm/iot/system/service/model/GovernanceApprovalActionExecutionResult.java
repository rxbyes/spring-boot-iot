package com.ghlzm.iot.system.service.model;

/**
 * Result returned after a governance approval action is executed.
 *
 * @param payloadJson updated payload json persisted back to the approval order
 */
public record GovernanceApprovalActionExecutionResult(String payloadJson) {
}
