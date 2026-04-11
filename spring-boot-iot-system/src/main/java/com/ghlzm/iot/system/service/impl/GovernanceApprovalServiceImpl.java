package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.GovernanceApprovalOrder;
import com.ghlzm.iot.system.entity.GovernanceApprovalTransition;
import com.ghlzm.iot.system.entity.GovernanceWorkItem;
import com.ghlzm.iot.system.mapper.GovernanceApprovalOrderMapper;
import com.ghlzm.iot.system.mapper.GovernanceApprovalTransitionMapper;
import com.ghlzm.iot.system.mapper.GovernanceWorkItemMapper;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.GovernanceApprovalActionExecutor;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionCommand;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionExecutionResult;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Governance approval service implementation.
 */
@Service
public class GovernanceApprovalServiceImpl implements GovernanceApprovalService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String ACTION_PRODUCT_CONTRACT_RELEASE_APPLY = "PRODUCT_CONTRACT_RELEASE_APPLY";
    private static final String ACTION_PRODUCT_CONTRACT_ROLLBACK = "PRODUCT_CONTRACT_ROLLBACK";
    private static final String WORK_ITEM_EXECUTION_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String WORK_ITEM_EXECUTION_EXECUTED = "EXECUTED";
    private static final String WORK_ITEM_EXECUTION_REJECTED = "REJECTED";
    private static final String WORK_ITEM_EXECUTION_CANCELLED = "CANCELLED";

    private final GovernanceApprovalOrderMapper orderMapper;
    private final GovernanceApprovalTransitionMapper transitionMapper;
    private final GovernanceWorkItemMapper workItemMapper;
    private final GovernancePermissionGuard permissionGuard;
    private final List<GovernanceApprovalActionExecutor> actionExecutors;

    public GovernanceApprovalServiceImpl(GovernanceApprovalOrderMapper orderMapper,
                                         GovernanceApprovalTransitionMapper transitionMapper,
                                         GovernanceWorkItemMapper workItemMapper,
                                         GovernancePermissionGuard permissionGuard,
                                         List<GovernanceApprovalActionExecutor> actionExecutors) {
        this.orderMapper = orderMapper;
        this.transitionMapper = transitionMapper;
        this.workItemMapper = workItemMapper;
        this.permissionGuard = permissionGuard;
        this.actionExecutors = actionExecutors == null ? List.of() : List.copyOf(actionExecutors);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitAction(GovernanceApprovalActionCommand command) {
        GovernanceApprovalActionCommand normalized = normalizeAndValidate(command);
        Date now = new Date();
        Long orderId = IdWorker.getId();

        GovernanceApprovalOrder order = new GovernanceApprovalOrder();
        order.setId(orderId);
        order.setActionCode(normalized.actionCode().trim());
        order.setActionName(normalizeText(normalized.actionName()));
        order.setSubjectType(normalizeText(normalized.subjectType()));
        order.setSubjectId(normalized.subjectId());
        order.setWorkItemId(normalized.workItemId());
        order.setStatus(STATUS_PENDING);
        order.setOperatorUserId(normalized.operatorUserId());
        order.setApproverUserId(normalized.approverUserId());
        order.setPayloadJson(normalizeText(normalized.payloadJson()));
        order.setApprovalComment(normalizeText(normalized.approvalComment()));
        order.setCreateBy(normalized.operatorUserId());
        order.setCreateTime(now);
        order.setUpdateBy(normalized.operatorUserId());
        order.setUpdateTime(now);
        if (orderMapper.insert(order) <= 0) {
            throw new BizException("审批主单创建失败");
        }

        if (transitionMapper.insert(buildTransition(
                orderId,
                null,
                STATUS_PENDING,
                normalized.operatorUserId(),
                "submit"
        )) <= 0) {
            throw new BizException("审批轨迹写入失败");
        }
        syncLinkedWorkItem(
                normalized.workItemId(),
                orderId,
                WORK_ITEM_EXECUTION_PENDING_APPROVAL,
                normalized.operatorUserId(),
                null,
                order.getActionCode(),
                null
        );
        return orderId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveOrder(Long orderId, Long approverUserId, String approvalComment) {
        GovernanceApprovalOrder order = requireOrder(orderId);
        validatePositiveUserId(approverUserId, "审批复核人无效");
        ensureStatus(order, STATUS_PENDING, "当前审批状态不支持通过");
        ensureApprover(order, approverUserId);
        requireActionPermission(order, approverUserId, ApprovalOperation.APPROVE);

        GovernanceApprovalActionExecutionResult executionResult = executeAction(order);
        String updatedPayloadJson = resolveUpdatedPayloadJson(order, executionResult);
        Date now = new Date();
        GovernanceApprovalOrder approvedOrder = new GovernanceApprovalOrder();
        approvedOrder.setId(orderId);
        approvedOrder.setStatus(STATUS_APPROVED);
        approvedOrder.setApproverUserId(approverUserId);
        approvedOrder.setPayloadJson(updatedPayloadJson);
        approvedOrder.setApprovalComment(normalizeText(approvalComment));
        approvedOrder.setApprovedTime(now);
        approvedOrder.setUpdateBy(approverUserId);
        approvedOrder.setUpdateTime(now);
        if (orderMapper.updateById(approvedOrder) <= 0) {
            throw new BizException("审批主单更新失败");
        }

        if (transitionMapper.insert(buildTransition(
                orderId,
                STATUS_PENDING,
                STATUS_APPROVED,
                approverUserId,
                normalizeText(approvalComment)
        )) <= 0) {
            throw new BizException("审批轨迹写入失败");
        }
        syncLinkedWorkItem(
                order.getWorkItemId(),
                orderId,
                WORK_ITEM_EXECUTION_EXECUTED,
                approverUserId,
                updatedPayloadJson,
                order.getActionCode(),
                executionResult
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectOrder(Long orderId, Long approverUserId, String rejectReason) {
        GovernanceApprovalOrder order = requireOrder(orderId);
        validatePositiveUserId(approverUserId, "审批复核人无效");
        String normalizedReason = normalizeRequiredText(rejectReason, "驳回原因不能为空");
        ensureStatus(order, STATUS_PENDING, "当前审批状态不支持驳回");
        ensureApprover(order, approverUserId);
        requireActionPermission(order, approverUserId, ApprovalOperation.REJECT);

        Date now = new Date();
        GovernanceApprovalOrder rejectedOrder = new GovernanceApprovalOrder();
        rejectedOrder.setId(orderId);
        rejectedOrder.setStatus(STATUS_REJECTED);
        rejectedOrder.setApprovalComment(normalizedReason);
        rejectedOrder.setApprovedTime(null);
        rejectedOrder.setUpdateBy(approverUserId);
        rejectedOrder.setUpdateTime(now);
        if (orderMapper.updateById(rejectedOrder) <= 0) {
            throw new BizException("审批主单更新失败");
        }

        if (transitionMapper.insert(buildTransition(
                orderId,
                STATUS_PENDING,
                STATUS_REJECTED,
                approverUserId,
                normalizedReason
        )) <= 0) {
            throw new BizException("审批轨迹写入失败");
        }
        syncLinkedWorkItem(
                order.getWorkItemId(),
                orderId,
                WORK_ITEM_EXECUTION_REJECTED,
                approverUserId,
                null,
                order.getActionCode(),
                null
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long operatorUserId, String cancelComment) {
        GovernanceApprovalOrder order = requireOrder(orderId);
        validatePositiveUserId(operatorUserId, "审批执行人无效");
        if (!operatorUserId.equals(order.getOperatorUserId())) {
            throw new BizException("仅执行人可撤销审批单");
        }
        if (!STATUS_PENDING.equals(order.getStatus()) && !STATUS_REJECTED.equals(order.getStatus())) {
            throw new BizException("当前审批状态不支持撤销: " + order.getStatus());
        }
        requireActionPermission(order, operatorUserId, ApprovalOperation.CANCEL);

        Date now = new Date();
        GovernanceApprovalOrder cancelledOrder = new GovernanceApprovalOrder();
        cancelledOrder.setId(orderId);
        cancelledOrder.setStatus(STATUS_CANCELLED);
        cancelledOrder.setApprovalComment(normalizeText(cancelComment));
        cancelledOrder.setUpdateBy(operatorUserId);
        cancelledOrder.setUpdateTime(now);
        if (orderMapper.updateById(cancelledOrder) <= 0) {
            throw new BizException("审批主单更新失败");
        }

        if (transitionMapper.insert(buildTransition(
                orderId,
                order.getStatus(),
                STATUS_CANCELLED,
                operatorUserId,
                normalizeText(cancelComment)
        )) <= 0) {
            throw new BizException("审批轨迹写入失败");
        }
        syncLinkedWorkItem(
                order.getWorkItemId(),
                orderId,
                WORK_ITEM_EXECUTION_CANCELLED,
                operatorUserId,
                null,
                order.getActionCode(),
                null
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmitOrder(Long orderId, Long operatorUserId, Long approverUserId, String resubmitComment) {
        GovernanceApprovalOrder order = requireOrder(orderId);
        validatePositiveUserId(operatorUserId, "审批执行人无效");
        validatePositiveUserId(approverUserId, "审批复核人无效");
        if (operatorUserId.equals(approverUserId)) {
            throw new BizException("执行人与复核人不能为同一账号");
        }
        ensureStatus(order, STATUS_REJECTED, "当前审批状态不支持重新提交");
        if (!operatorUserId.equals(order.getOperatorUserId())) {
            throw new BizException("仅执行人可重新提交审批单");
        }
        requireActionPermission(order, operatorUserId, ApprovalOperation.RESUBMIT);

        Date now = new Date();
        GovernanceApprovalOrder pendingOrder = new GovernanceApprovalOrder();
        pendingOrder.setId(orderId);
        pendingOrder.setStatus(STATUS_PENDING);
        pendingOrder.setApproverUserId(approverUserId);
        pendingOrder.setApprovalComment(normalizeText(resubmitComment));
        pendingOrder.setApprovedTime(null);
        pendingOrder.setUpdateBy(operatorUserId);
        pendingOrder.setUpdateTime(now);
        if (orderMapper.updateById(pendingOrder) <= 0) {
            throw new BizException("审批主单更新失败");
        }

        if (transitionMapper.insert(buildTransition(
                orderId,
                STATUS_REJECTED,
                STATUS_PENDING,
                operatorUserId,
                normalizeText(resubmitComment)
        )) <= 0) {
            throw new BizException("审批轨迹写入失败");
        }
        syncLinkedWorkItem(
                order.getWorkItemId(),
                orderId,
                WORK_ITEM_EXECUTION_PENDING_APPROVAL,
                operatorUserId,
                null,
                order.getActionCode(),
                null
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordApprovedAction(GovernanceApprovalActionCommand command) {
        GovernanceApprovalActionCommand normalized = normalizeAndValidate(command);
        Long orderId = submitAction(normalized);
        approveOrder(orderId, normalized.approverUserId(), normalized.approvalComment());
        return orderId;
    }

    private GovernanceApprovalTransition buildTransition(Long orderId,
                                                         String fromStatus,
                                                         String toStatus,
                                                         Long actorUserId,
                                                         String comment) {
        GovernanceApprovalTransition transition = new GovernanceApprovalTransition();
        transition.setId(IdWorker.getId());
        transition.setOrderId(orderId);
        transition.setFromStatus(normalizeText(fromStatus));
        transition.setToStatus(toStatus);
        transition.setActorUserId(actorUserId);
        transition.setTransitionComment(comment);
        transition.setCreateBy(actorUserId);
        transition.setCreateTime(new Date());
        return transition;
    }

    private GovernanceApprovalActionCommand normalizeAndValidate(GovernanceApprovalActionCommand command) {
        if (command == null) {
            throw new BizException("审批命令不能为空");
        }
        if (!StringUtils.hasText(command.actionCode())) {
            throw new BizException("审批动作编码不能为空");
        }
        if (command.operatorUserId() == null || command.operatorUserId() <= 0) {
            throw new BizException("审批执行人无效");
        }
        if (command.approverUserId() == null || command.approverUserId() <= 0) {
            throw new BizException("审批复核人无效");
        }
        if (command.operatorUserId().equals(command.approverUserId())) {
            throw new BizException("执行人与复核人不能为同一账号");
        }
        return command;
    }

    private GovernanceApprovalOrder requireOrder(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BizException("审批主单不存在: " + orderId);
        }
        GovernanceApprovalOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("审批主单不存在: " + orderId);
        }
        return order;
    }

    private GovernanceApprovalActionExecutionResult executeAction(GovernanceApprovalOrder order) {
        GovernanceApprovalActionExecutor executor = resolveExecutor(order.getActionCode());
        GovernanceApprovalActionExecutionResult result = executor.execute(order);
        if (result == null) {
            return new GovernanceApprovalActionExecutionResult(normalizeText(order.getPayloadJson()));
        }
        return result;
    }

    private GovernanceApprovalActionExecutor resolveExecutor(String actionCode) {
        String normalizedActionCode = normalizeRequiredText(actionCode, "审批动作编码不能为空");
        return actionExecutors.stream()
                .filter(executor -> executor != null && executor.supports(normalizedActionCode))
                .findFirst()
                .orElseThrow(() -> new BizException("审批动作未配置执行器: " + normalizedActionCode));
    }

    private void validatePositiveUserId(Long userId, String message) {
        if (userId == null || userId <= 0) {
            throw new BizException(message);
        }
    }

    private void ensureStatus(GovernanceApprovalOrder order, String expectedStatus, String messagePrefix) {
        if (!expectedStatus.equals(order.getStatus())) {
            throw new BizException(messagePrefix + ": " + order.getStatus());
        }
    }

    private void ensureApprover(GovernanceApprovalOrder order, Long approverUserId) {
        if (order.getApproverUserId() == null || !order.getApproverUserId().equals(approverUserId)) {
            throw new BizException("仅当前复核人可执行该审批动作");
        }
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(errorMessage);
        }
        return value.trim();
    }

    private void requireActionPermission(GovernanceApprovalOrder order, Long actorUserId, ApprovalOperation operation) {
        String actionCode = normalizeRequiredText(order.getActionCode(), "审批动作编码不能为空");
        switch (actionCode) {
            case ACTION_PRODUCT_CONTRACT_RELEASE_APPLY -> requireProductContractReleasePermissions(actorUserId, operation);
            case ACTION_PRODUCT_CONTRACT_ROLLBACK -> requireProductContractRollbackPermissions(actorUserId, operation);
            default -> throw new BizException("审批动作未配置权限映射: " + actionCode);
        }
    }

    private void requireProductContractReleasePermissions(Long actorUserId, ApprovalOperation operation) {
        switch (operation) {
            case APPROVE, REJECT -> permissionGuard.requireAnyPermission(
                    actorUserId,
                    actionLabel(operation),
                    GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE
            );
            case CANCEL, RESUBMIT -> {
                permissionGuard.requireAnyPermission(
                        actorUserId,
                        actionLabel(operation),
                        GovernancePermissionCodes.PRODUCT_CONTRACT_RELEASE
                );
                permissionGuard.requireAnyPermission(
                        actorUserId,
                        actionLabel(operation),
                        GovernancePermissionCodes.RISK_METRIC_CATALOG_TAG
                );
            }
        }
    }

    private void requireProductContractRollbackPermissions(Long actorUserId, ApprovalOperation operation) {
        switch (operation) {
            case APPROVE, REJECT -> permissionGuard.requireAnyPermission(
                    actorUserId,
                    actionLabel(operation),
                    GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE
            );
            case CANCEL, RESUBMIT -> permissionGuard.requireAnyPermission(
                    actorUserId,
                    actionLabel(operation),
                    GovernancePermissionCodes.PRODUCT_CONTRACT_ROLLBACK
            );
        }
    }

    private String actionLabel(ApprovalOperation operation) {
        return switch (operation) {
            case APPROVE -> "治理审批通过";
            case REJECT -> "治理审批驳回";
            case CANCEL -> "治理审批撤销";
            case RESUBMIT -> "治理审批重提";
        };
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveUpdatedPayloadJson(GovernanceApprovalOrder order,
                                             GovernanceApprovalActionExecutionResult result) {
        if (result != null && StringUtils.hasText(result.payloadJson())) {
            return normalizeText(result.payloadJson());
        }
        return normalizeText(order == null ? null : order.getPayloadJson());
    }

    private void syncLinkedWorkItem(Long workItemId,
                                    Long approvalOrderId,
                                    String executionStatus,
                                    Long actorUserId,
                                    String payloadJson,
                                    String actionCode,
                                    GovernanceApprovalActionExecutionResult executionResult) {
        if (workItemMapper == null || workItemId == null || workItemId <= 0) {
            return;
        }
        GovernanceWorkItem update = new GovernanceWorkItem();
        update.setId(workItemId);
        update.setApprovalOrderId(approvalOrderId);
        update.setExecutionStatus(normalizeText(executionStatus));
        update.setUpdateBy(actorUserId);

        String impactSnapshotJson = resolveImpactSnapshotJson(actionCode, payloadJson, executionResult);
        if (impactSnapshotJson != null) {
            update.setImpactSnapshotJson(impactSnapshotJson);
        }
        String rollbackSnapshotJson = resolveRollbackSnapshotJson(actionCode, payloadJson, executionResult);
        if (rollbackSnapshotJson != null) {
            update.setRollbackSnapshotJson(rollbackSnapshotJson);
        }
        workItemMapper.updateById(update);
    }

    private String resolveImpactSnapshotJson(String actionCode,
                                             String payloadJson,
                                             GovernanceApprovalActionExecutionResult executionResult) {
        if (executionResult != null && StringUtils.hasText(executionResult.impactSnapshotJson())) {
            return normalizeText(executionResult.impactSnapshotJson());
        }
        if (!StringUtils.hasText(payloadJson) || isRollbackAction(actionCode)) {
            return null;
        }
        return normalizeText(payloadJson);
    }

    private String resolveRollbackSnapshotJson(String actionCode,
                                               String payloadJson,
                                               GovernanceApprovalActionExecutionResult executionResult) {
        if (executionResult != null && StringUtils.hasText(executionResult.rollbackSnapshotJson())) {
            return normalizeText(executionResult.rollbackSnapshotJson());
        }
        if (!StringUtils.hasText(payloadJson) || !isRollbackAction(actionCode)) {
            return null;
        }
        return normalizeText(payloadJson);
    }

    private boolean isRollbackAction(String actionCode) {
        return ACTION_PRODUCT_CONTRACT_ROLLBACK.equals(normalizeText(actionCode));
    }

    private enum ApprovalOperation {
        APPROVE,
        REJECT,
        CANCEL,
        RESUBMIT
    }
}
