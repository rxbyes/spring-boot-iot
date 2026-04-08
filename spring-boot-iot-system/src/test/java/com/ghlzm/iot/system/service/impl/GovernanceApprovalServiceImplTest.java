package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.GovernanceApprovalOrder;
import com.ghlzm.iot.system.entity.GovernanceApprovalTransition;
import com.ghlzm.iot.system.mapper.GovernanceApprovalOrderMapper;
import com.ghlzm.iot.system.mapper.GovernanceApprovalTransitionMapper;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.GovernanceApprovalActionExecutor;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionCommand;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionExecutionResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceApprovalServiceImplTest {

    @Mock
    private GovernanceApprovalOrderMapper orderMapper;

    @Mock
    private GovernanceApprovalTransitionMapper transitionMapper;

    @Mock
    private GovernancePermissionGuard permissionGuard;

    @Mock
    private GovernanceApprovalActionExecutor executor;

    private GovernanceApprovalServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(executor.supports(anyString())).thenReturn(false);
        service = new GovernanceApprovalServiceImpl(orderMapper, transitionMapper, permissionGuard, List.of(executor));
    }

    @Test
    void recordApprovedActionShouldPersistOrderAndTransitions() {
        GovernanceApprovalActionCommand command = new GovernanceApprovalActionCommand(
                "PRODUCT_CONTRACT_RELEASE_APPLY",
                "contract release apply",
                "PRODUCT",
                1001L,
                10001L,
                20002L,
                "{\"productId\":1001}",
                "dual-control approved"
        );
        when(orderMapper.insert(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(orderMapper.selectById(any(Long.class))).thenAnswer(invocation -> {
            Long orderId = invocation.getArgument(0);
            GovernanceApprovalOrder order = new GovernanceApprovalOrder();
            order.setId(orderId);
            order.setStatus("PENDING");
            order.setOperatorUserId(10001L);
            order.setApproverUserId(20002L);
            order.setActionCode("PRODUCT_CONTRACT_RELEASE_APPLY");
            order.setPayloadJson("{\"productId\":1001}");
            return order;
        });
        when(orderMapper.updateById(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(transitionMapper.insert(any(GovernanceApprovalTransition.class))).thenReturn(1);
        when(executor.supports("PRODUCT_CONTRACT_RELEASE_APPLY")).thenReturn(true);
        when(executor.execute(any(GovernanceApprovalOrder.class)))
                .thenReturn(new GovernanceApprovalActionExecutionResult("{\"productId\":1001,\"executed\":true}"));

        Long approvalOrderId = service.recordApprovedAction(command);

        assertNotNull(approvalOrderId);
        ArgumentCaptor<GovernanceApprovalOrder> updateCaptor = ArgumentCaptor.forClass(GovernanceApprovalOrder.class);
        verify(orderMapper).updateById(updateCaptor.capture());
        GovernanceApprovalOrder approvedOrder = updateCaptor.getValue();
        assertEquals("APPROVED", approvedOrder.getStatus());
        assertEquals(20002L, approvedOrder.getApproverUserId());
        assertNotNull(approvedOrder.getApprovedTime());
        assertEquals("{\"productId\":1001,\"executed\":true}", approvedOrder.getPayloadJson());

        ArgumentCaptor<GovernanceApprovalTransition> transitionCaptor = ArgumentCaptor.forClass(GovernanceApprovalTransition.class);
        verify(transitionMapper, times(2)).insert(transitionCaptor.capture());
        List<GovernanceApprovalTransition> transitions = transitionCaptor.getAllValues();
        assertEquals("PENDING", transitions.get(0).getToStatus());
        assertEquals("APPROVED", transitions.get(1).getToStatus());
        assertEquals(20002L, transitions.get(1).getActorUserId());
        verify(executor).execute(any(GovernanceApprovalOrder.class));
    }

    @Test
    void recordApprovedActionShouldRejectSameOperatorAndApprover() {
        GovernanceApprovalActionCommand command = new GovernanceApprovalActionCommand(
                "PRODUCT_CONTRACT_ROLLBACK",
                "contract release rollback",
                "RELEASE_BATCH",
                88001L,
                10001L,
                10001L,
                null,
                null
        );

        assertThrows(BizException.class, () -> service.recordApprovedAction(command));
    }

    @Test
    void recordApprovedActionShouldRejectBlankActionCode() {
        GovernanceApprovalActionCommand command = new GovernanceApprovalActionCommand(
                " ",
                "contract release apply",
                "PRODUCT",
                1001L,
                10001L,
                20002L,
                null,
                null
        );

        assertThrows(BizException.class, () -> service.recordApprovedAction(command));
    }

    @Test
    void approveOrderShouldMovePendingToApproved() {
        GovernanceApprovalOrder order = mockOrder(88001L, "PENDING", 10001L, 20002L, "PRODUCT_CONTRACT_RELEASE_APPLY");
        order.setPayloadJson("{\"request\":true}");
        when(orderMapper.selectById(88001L)).thenReturn(order);
        when(orderMapper.updateById(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(transitionMapper.insert(any(GovernanceApprovalTransition.class))).thenReturn(1);
        when(executor.supports("PRODUCT_CONTRACT_RELEASE_APPLY")).thenReturn(true);
        when(executor.execute(order))
                .thenReturn(new GovernanceApprovalActionExecutionResult("{\"request\":true,\"result\":{\"releaseBatchId\":99001}}"));

        service.approveOrder(88001L, 20002L, "approve");

        ArgumentCaptor<GovernanceApprovalOrder> updateCaptor = ArgumentCaptor.forClass(GovernanceApprovalOrder.class);
        verify(orderMapper).updateById(updateCaptor.capture());
        GovernanceApprovalOrder updated = updateCaptor.getValue();
        assertEquals("APPROVED", updated.getStatus());
        assertEquals(20002L, updated.getApproverUserId());
        assertNotNull(updated.getApprovedTime());
        assertEquals("{\"request\":true,\"result\":{\"releaseBatchId\":99001}}", updated.getPayloadJson());

        ArgumentCaptor<GovernanceApprovalTransition> transitionCaptor = ArgumentCaptor.forClass(GovernanceApprovalTransition.class);
        verify(transitionMapper).insert(transitionCaptor.capture());
        GovernanceApprovalTransition transition = transitionCaptor.getValue();
        assertEquals("PENDING", transition.getFromStatus());
        assertEquals("APPROVED", transition.getToStatus());
        assertEquals(20002L, transition.getActorUserId());
        verify(permissionGuard).requireAnyPermission(
                20002L,
                "治理审批通过",
                GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE
        );
        verify(executor).execute(order);
    }

    @Test
    void rejectOrderShouldMovePendingToRejected() {
        GovernanceApprovalOrder order = mockOrder(88002L, "PENDING", 10001L, 20002L, "PRODUCT_CONTRACT_RELEASE_APPLY");
        when(orderMapper.selectById(88002L)).thenReturn(order);
        when(orderMapper.updateById(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(transitionMapper.insert(any(GovernanceApprovalTransition.class))).thenReturn(1);

        service.rejectOrder(88002L, 20002L, "need more evidence");

        ArgumentCaptor<GovernanceApprovalOrder> updateCaptor = ArgumentCaptor.forClass(GovernanceApprovalOrder.class);
        verify(orderMapper).updateById(updateCaptor.capture());
        GovernanceApprovalOrder updated = updateCaptor.getValue();
        assertEquals("REJECTED", updated.getStatus());
        assertEquals(20002L, updated.getUpdateBy());

        ArgumentCaptor<GovernanceApprovalTransition> transitionCaptor = ArgumentCaptor.forClass(GovernanceApprovalTransition.class);
        verify(transitionMapper).insert(transitionCaptor.capture());
        GovernanceApprovalTransition transition = transitionCaptor.getValue();
        assertEquals("PENDING", transition.getFromStatus());
        assertEquals("REJECTED", transition.getToStatus());
        assertEquals("need more evidence", transition.getTransitionComment());
        verify(permissionGuard).requireAnyPermission(
                20002L,
                "治理审批驳回",
                GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE
        );
        verify(executor, never()).execute(any(GovernanceApprovalOrder.class));
    }

    @Test
    void cancelOrderShouldMovePendingToCancelled() {
        GovernanceApprovalOrder order = mockOrder(88003L, "PENDING", 10001L, 20002L, "PRODUCT_CONTRACT_RELEASE_APPLY");
        when(orderMapper.selectById(88003L)).thenReturn(order);
        when(orderMapper.updateById(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(transitionMapper.insert(any(GovernanceApprovalTransition.class))).thenReturn(1);

        service.cancelOrder(88003L, 10001L, "cancel current apply");

        ArgumentCaptor<GovernanceApprovalOrder> updateCaptor = ArgumentCaptor.forClass(GovernanceApprovalOrder.class);
        verify(orderMapper).updateById(updateCaptor.capture());
        GovernanceApprovalOrder updated = updateCaptor.getValue();
        assertEquals("CANCELLED", updated.getStatus());
        assertEquals(10001L, updated.getUpdateBy());

        ArgumentCaptor<GovernanceApprovalTransition> transitionCaptor = ArgumentCaptor.forClass(GovernanceApprovalTransition.class);
        verify(transitionMapper).insert(transitionCaptor.capture());
        GovernanceApprovalTransition transition = transitionCaptor.getValue();
        assertEquals("PENDING", transition.getFromStatus());
        assertEquals("CANCELLED", transition.getToStatus());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "治理审批撤销",
                GovernancePermissionCodes.PRODUCT_CONTRACT_RELEASE
        );
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "治理审批撤销",
                GovernancePermissionCodes.RISK_METRIC_CATALOG_TAG
        );
        verify(executor, never()).execute(any(GovernanceApprovalOrder.class));
    }

    @Test
    void resubmitOrderShouldMoveRejectedToPending() {
        GovernanceApprovalOrder order = mockOrder(88004L, "REJECTED", 10001L, 20002L, "PRODUCT_CONTRACT_RELEASE_APPLY");
        when(orderMapper.selectById(88004L)).thenReturn(order);
        when(orderMapper.updateById(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(transitionMapper.insert(any(GovernanceApprovalTransition.class))).thenReturn(1);

        service.resubmitOrder(88004L, 10001L, 30003L, "resubmit");

        ArgumentCaptor<GovernanceApprovalOrder> updateCaptor = ArgumentCaptor.forClass(GovernanceApprovalOrder.class);
        verify(orderMapper).updateById(updateCaptor.capture());
        GovernanceApprovalOrder updated = updateCaptor.getValue();
        assertEquals("PENDING", updated.getStatus());
        assertEquals(30003L, updated.getApproverUserId());
        assertEquals(10001L, updated.getUpdateBy());

        ArgumentCaptor<GovernanceApprovalTransition> transitionCaptor = ArgumentCaptor.forClass(GovernanceApprovalTransition.class);
        verify(transitionMapper).insert(transitionCaptor.capture());
        GovernanceApprovalTransition transition = transitionCaptor.getValue();
        assertEquals("REJECTED", transition.getFromStatus());
        assertEquals("PENDING", transition.getToStatus());
        assertEquals(10001L, transition.getActorUserId());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "治理审批重提",
                GovernancePermissionCodes.PRODUCT_CONTRACT_RELEASE
        );
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "治理审批重提",
                GovernancePermissionCodes.RISK_METRIC_CATALOG_TAG
        );
        verify(executor, never()).execute(any(GovernanceApprovalOrder.class));
    }

    @Test
    void rejectOrderShouldRejectNonPendingStatus() {
        GovernanceApprovalOrder order = mockOrder(88005L, "APPROVED", 10001L, 20002L, "PRODUCT_CONTRACT_RELEASE_APPLY");
        when(orderMapper.selectById(88005L)).thenReturn(order);

        assertThrows(BizException.class, () -> service.rejectOrder(88005L, 20002L, "invalid"));
        verify(orderMapper, never()).updateById(any(GovernanceApprovalOrder.class));
        verify(transitionMapper, never()).insert(any(GovernanceApprovalTransition.class));
    }

    @Test
    void approveOrderShouldUseRollbackApproverPermissionForRollbackAction() {
        GovernanceApprovalOrder order = mockOrder(88006L, "PENDING", 10001L, 20002L, "PRODUCT_CONTRACT_ROLLBACK");
        when(orderMapper.selectById(88006L)).thenReturn(order);
        when(orderMapper.updateById(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(transitionMapper.insert(any(GovernanceApprovalTransition.class))).thenReturn(1);
        when(executor.supports("PRODUCT_CONTRACT_ROLLBACK")).thenReturn(true);
        when(executor.execute(order)).thenReturn(new GovernanceApprovalActionExecutionResult("{\"rolledBackBatchId\":88006}"));

        service.approveOrder(88006L, 20002L, "approve rollback");

        verify(permissionGuard).requireAnyPermission(
                20002L,
                "治理审批通过",
                GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE
        );
        verify(executor).execute(order);
    }

    @Test
    void cancelOrderShouldUseRollbackOperatorPermissionForRollbackAction() {
        GovernanceApprovalOrder order = mockOrder(88007L, "PENDING", 10001L, 20002L, "PRODUCT_CONTRACT_ROLLBACK");
        when(orderMapper.selectById(88007L)).thenReturn(order);
        when(orderMapper.updateById(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(transitionMapper.insert(any(GovernanceApprovalTransition.class))).thenReturn(1);

        service.cancelOrder(88007L, 10001L, "cancel rollback");

        verify(permissionGuard).requireAnyPermission(
                10001L,
                "治理审批撤销",
                GovernancePermissionCodes.PRODUCT_CONTRACT_ROLLBACK
        );
    }

    private GovernanceApprovalOrder mockOrder(Long orderId,
                                              String status,
                                              Long operatorUserId,
                                              Long approverUserId,
                                              String actionCode) {
        GovernanceApprovalOrder order = new GovernanceApprovalOrder();
        order.setId(orderId);
        order.setStatus(status);
        order.setOperatorUserId(operatorUserId);
        order.setApproverUserId(approverUserId);
        order.setActionCode(actionCode);
        return order;
    }
}
