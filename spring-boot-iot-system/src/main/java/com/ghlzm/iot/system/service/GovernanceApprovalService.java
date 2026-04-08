package com.ghlzm.iot.system.service;

import com.ghlzm.iot.system.service.model.GovernanceApprovalActionCommand;

/**
 * Governance approval service.
 */
public interface GovernanceApprovalService {

    /**
     * Submit a governance action into pending approval state.
     *
     * @param command approval command
     * @return approval order id
     */
    Long submitAction(GovernanceApprovalActionCommand command);

    /**
     * Approve a pending approval order.
     *
     * @param orderId         approval order id
     * @param approverUserId  approver user id
     * @param approvalComment comment
     */
    void approveOrder(Long orderId, Long approverUserId, String approvalComment);

    /**
     * Reject a pending approval order.
     *
     * @param orderId      approval order id
     * @param approverUserId approver user id
     * @param rejectReason reject reason
     */
    void rejectOrder(Long orderId, Long approverUserId, String rejectReason);

    /**
     * Cancel an approval order by operator.
     *
     * @param orderId         approval order id
     * @param operatorUserId  operator user id
     * @param cancelComment   cancel reason
     */
    void cancelOrder(Long orderId, Long operatorUserId, String cancelComment);

    /**
     * Resubmit a rejected order.
     *
     * @param orderId          approval order id
     * @param operatorUserId   operator user id
     * @param approverUserId   approver user id
     * @param resubmitComment  resubmit comment
     */
    void resubmitOrder(Long orderId, Long operatorUserId, Long approverUserId, String resubmitComment);

    /**
     * Persist an approved governance action and return approval order id.
     *
     * @param command approval command
     * @return approval order id
     */
    Long recordApprovedAction(GovernanceApprovalActionCommand command);
}
