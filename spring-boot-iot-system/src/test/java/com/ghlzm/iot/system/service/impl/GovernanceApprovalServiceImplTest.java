package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import com.ghlzm.iot.system.entity.GovernanceApprovalOrder;
import com.ghlzm.iot.system.entity.GovernanceApprovalTransition;
import com.ghlzm.iot.system.entity.GovernanceWorkItem;
import com.ghlzm.iot.system.mapper.GovernanceApprovalOrderMapper;
import com.ghlzm.iot.system.mapper.GovernanceApprovalTransitionMapper;
import com.ghlzm.iot.system.mapper.GovernanceWorkItemMapper;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.GovernanceApprovalActionExecutor;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionCommand;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionExecutionResult;
import com.ghlzm.iot.system.service.model.GovernanceRecommendationSnapshot;
import com.ghlzm.iot.system.service.model.GovernanceSimulationResult;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Mock
    private GovernanceWorkItemMapper workItemMapper;

    @Mock
    private ObservabilityEvidenceRecorder evidenceRecorder;

    private GovernanceApprovalServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(executor.supports(anyString())).thenReturn(false);
        service = new GovernanceApprovalServiceImpl(orderMapper, transitionMapper, workItemMapper, permissionGuard, List.of(executor));
        service.setObservabilityEvidenceRecorder(evidenceRecorder);
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
        writeField(order, "workItemId", 73001L);
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
        verify(workItemMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                Long.valueOf(73001L).equals(item.getId())
                        && Long.valueOf(88001L).equals(item.getApprovalOrderId())
                        && "EXECUTED".equals(readString(item, "executionStatus"))
                        && readString(item, "impactSnapshotJson").contains("releaseBatchId")
        ));
    }

    @Test
    void approveOrderShouldRecordMappingPublishBusinessEvent() {
        GovernanceApprovalOrder order = mockOrder(88011L, "PENDING", 10001L, 20002L, "VENDOR_MAPPING_RULE_PUBLISH");
        order.setPayloadJson("""
                {
                  "ruleId": 9001,
                  "productId": 1001,
                  "rawIdentifier": "L1_LF_1.value",
                  "targetNormativeIdentifier": "value",
                  "scopeType": "PRODUCT"
                }
                """);
        when(orderMapper.selectById(88011L)).thenReturn(order);
        when(orderMapper.updateById(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(transitionMapper.insert(any(GovernanceApprovalTransition.class))).thenReturn(1);
        when(executor.supports("VENDOR_MAPPING_RULE_PUBLISH")).thenReturn(true);
        when(executor.execute(order)).thenReturn(new GovernanceApprovalActionExecutionResult("""
                {
                  "ruleId": 9001,
                  "productId": 1001,
                  "rawIdentifier": "L1_LF_1.value",
                  "targetNormativeIdentifier": "value",
                  "scopeType": "PRODUCT",
                  "execution": {
                    "approvalOrderId": 88011,
                    "publishedVersionNo": 3,
                    "lifecycleStatus": "PUBLISHED"
                  }
                }
                """));

        service.approveOrder(88011L, 20002L, "approve");

        verify(evidenceRecorder).recordBusinessEvent(org.mockito.ArgumentMatchers.argThat(event ->
                "product.mapping_rule.published".equals(event.getEventCode())
                        && "vendor_mapping_rule".equals(event.getObjectType())
                        && "9001".equals(event.getObjectId())
                        && Integer.valueOf(3).equals(event.getMetadata().get("publishedVersionNo"))
                        && "value".equals(event.getMetadata().get("targetNormativeIdentifier"))
                        && Long.valueOf(88011L).equals(event.getMetadata().get("approvalOrderId"))
        ));
    }

    @Test
    void approveOrderShouldRecordContractRollbackBusinessEvent() {
        GovernanceApprovalOrder order = mockOrder(88012L, "PENDING", 10001L, 20002L, "PRODUCT_CONTRACT_ROLLBACK");
        order.setPayloadJson("""
                {
                  "request": {
                    "batchId": 7001
                  }
                }
                """);
        when(orderMapper.selectById(88012L)).thenReturn(order);
        when(orderMapper.updateById(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(transitionMapper.insert(any(GovernanceApprovalTransition.class))).thenReturn(1);
        when(executor.supports("PRODUCT_CONTRACT_ROLLBACK")).thenReturn(true);
        when(executor.execute(order)).thenReturn(new GovernanceApprovalActionExecutionResult("""
                {
                  "request": {
                    "batchId": 7001
                  },
                  "execution": {
                    "executedAt": "2026-04-25T10:00:00",
                    "result": {
                      "targetBatchId": 7001,
                      "rolledBackBatchId": 6801,
                      "productId": 1001,
                      "scenarioCode": "phase1-crack",
                      "releaseSource": "manual_compare_apply",
                      "restoredFieldCount": 4
                    }
                  }
                }
                """));

        service.approveOrder(88012L, 20002L, "approve");

        verify(evidenceRecorder).recordBusinessEvent(org.mockito.ArgumentMatchers.argThat(event ->
                "product.contract.rolled_back".equals(event.getEventCode())
                        && "contract_release_batch".equals(event.getObjectType())
                        && "7001".equals(event.getObjectId())
                        && Long.valueOf(6801L).equals(event.getMetadata().get("rolledBackBatchId"))
                        && "phase1-crack".equals(event.getMetadata().get("scenarioCode"))
        ));
    }

    @Test
    void rejectOrderShouldMovePendingToRejected() {
        GovernanceApprovalOrder order = mockOrder(88002L, "PENDING", 10001L, 20002L, "PRODUCT_CONTRACT_RELEASE_APPLY");
        writeField(order, "workItemId", 73002L);
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
        verify(workItemMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                Long.valueOf(73002L).equals(item.getId())
                        && Long.valueOf(88002L).equals(item.getApprovalOrderId())
                        && "REJECTED".equals(readString(item, "executionStatus"))
        ));
    }

    @Test
    void cancelOrderShouldMovePendingToCancelled() {
        GovernanceApprovalOrder order = mockOrder(88003L, "PENDING", 10001L, 20002L, "PRODUCT_CONTRACT_RELEASE_APPLY");
        writeField(order, "workItemId", 73003L);
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
        verify(workItemMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                Long.valueOf(73003L).equals(item.getId())
                        && Long.valueOf(88003L).equals(item.getApprovalOrderId())
                        && "CANCELLED".equals(readString(item, "executionStatus"))
        ));
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
        writeField(order, "workItemId", 73006L);
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
        verify(workItemMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                Long.valueOf(73006L).equals(item.getId())
                        && "EXECUTED".equals(readString(item, "executionStatus"))
                        && readString(item, "rollbackSnapshotJson").contains("rolledBackBatchId")
        ));
    }

    @Test
    void approveOrderShouldUseContractApprovePermissionForVendorMappingPublishAction() {
        GovernanceApprovalOrder order = mockOrder(88016L, "PENDING", 10001L, 20002L, "VENDOR_MAPPING_RULE_PUBLISH");
        when(orderMapper.selectById(88016L)).thenReturn(order);
        when(orderMapper.updateById(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(transitionMapper.insert(any(GovernanceApprovalTransition.class))).thenReturn(1);
        when(executor.supports("VENDOR_MAPPING_RULE_PUBLISH")).thenReturn(true);
        when(executor.execute(order)).thenReturn(new GovernanceApprovalActionExecutionResult("{\"published\":true}"));

        service.approveOrder(88016L, 20002L, "approve publish");

        verify(permissionGuard).requireAnyPermission(
                20002L,
                "治理审批通过",
                GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE
        );
    }

    @Test
    void approveOrderShouldUseProtocolGovernanceApprovePermissionForProtocolFamilyPublishAction() {
        GovernanceApprovalOrder order = mockOrder(88018L, "PENDING", 10001L, 20002L, "PROTOCOL_FAMILY_PUBLISH");
        when(orderMapper.selectById(88018L)).thenReturn(order);
        when(orderMapper.updateById(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(transitionMapper.insert(any(GovernanceApprovalTransition.class))).thenReturn(1);
        when(executor.supports("PROTOCOL_FAMILY_PUBLISH")).thenReturn(true);
        when(executor.execute(order)).thenReturn(new GovernanceApprovalActionExecutionResult("{\"published\":true}"));

        service.approveOrder(88018L, 20002L, "approve protocol family");

        verify(permissionGuard).requireAnyPermission(
                20002L,
                "治理审批通过",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_APPROVE
        );
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

    @Test
    void cancelOrderShouldUseRollbackPermissionForVendorMappingRollbackAction() {
        GovernanceApprovalOrder order = mockOrder(88017L, "PENDING", 10001L, 20002L, "VENDOR_MAPPING_RULE_ROLLBACK");
        when(orderMapper.selectById(88017L)).thenReturn(order);
        when(orderMapper.updateById(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(transitionMapper.insert(any(GovernanceApprovalTransition.class))).thenReturn(1);

        service.cancelOrder(88017L, 10001L, "cancel vendor mapping rollback");

        verify(permissionGuard).requireAnyPermission(
                10001L,
                "治理审批撤销",
                GovernancePermissionCodes.PRODUCT_CONTRACT_ROLLBACK
        );
    }

    @Test
    void cancelOrderShouldUseProtocolGovernanceEditPermissionForProtocolProfileRollbackAction() {
        GovernanceApprovalOrder order = mockOrder(88019L, "PENDING", 10001L, 20002L, "PROTOCOL_DECRYPT_PROFILE_ROLLBACK");
        when(orderMapper.selectById(88019L)).thenReturn(order);
        when(orderMapper.updateById(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(transitionMapper.insert(any(GovernanceApprovalTransition.class))).thenReturn(1);

        service.cancelOrder(88019L, 10001L, "cancel protocol profile rollback");

        verify(permissionGuard).requireAnyPermission(
                10001L,
                "治理审批撤销",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
    }

    @Test
    void submitActionShouldPersistLinkedWorkItemIdAndMarkWorkItemPendingApproval() {
        GovernanceApprovalActionCommand command = buildCommand(
                "PRODUCT_CONTRACT_RELEASE_APPLY",
                "contract release apply",
                "PRODUCT",
                1001L,
                73010L,
                10001L,
                20002L,
                "{\"productId\":1001}",
                null
        );
        when(orderMapper.insert(any(GovernanceApprovalOrder.class))).thenReturn(1);
        when(transitionMapper.insert(any(GovernanceApprovalTransition.class))).thenReturn(1);
        when(workItemMapper.updateById(any(GovernanceWorkItem.class))).thenReturn(1);

        Long approvalOrderId = service.submitAction(command);

        ArgumentCaptor<GovernanceApprovalOrder> orderCaptor = ArgumentCaptor.forClass(GovernanceApprovalOrder.class);
        verify(orderMapper).insert(orderCaptor.capture());
        GovernanceApprovalOrder savedOrder = orderCaptor.getValue();
        assertEquals(73010L, readLong(savedOrder, "workItemId"));
        verify(workItemMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                Long.valueOf(73010L).equals(item.getId())
                        && approvalOrderId.equals(item.getApprovalOrderId())
                        && "PENDING_APPROVAL".equals(readString(item, "executionStatus"))
        ));
    }

    @Test
    void simulateOrderShouldReturnDryRunSummaryAndAutoDraftSuggestion() {
        GovernanceApprovalOrder order = mockOrder(88011L, "PENDING", 10001L, 20002L, "PRODUCT_CONTRACT_RELEASE_APPLY");
        order.setPayloadJson("{\"request\":{\"productId\":1001}}");
        writeField(order, "workItemId", 73011L);
        when(orderMapper.selectById(88011L)).thenReturn(order);
        when(executor.supports("PRODUCT_CONTRACT_RELEASE_APPLY")).thenReturn(true);
        when(executor.simulate(order)).thenReturn(new GovernanceSimulationResult(
                88011L,
                73011L,
                "PRODUCT_CONTRACT_RELEASE_APPLY",
                true,
                1L,
                List.of("RISK_METRIC", "RISK_POINT", "RULE"),
                true,
                "可通过合同回滚恢复正式批次",
                null,
                null,
                null,
                false,
                null
        ));
        GovernanceWorkItem workItem = new GovernanceWorkItem();
        workItem.setId(73011L);
        workItem.setRecommendationSnapshotJson("{\"recommendationType\":\"PUBLISH\",\"confidence\":0.96,\"reasonCodes\":[\"HIGH_CONFIDENCE\"],\"suggestedAction\":\"建议生成审批意见草稿\"}");
        when(workItemMapper.selectById(73011L)).thenReturn(workItem);

        GovernanceSimulationResult result = service.simulateOrder(88011L);

        assertNotNull(result);
        assertTrue(result.executable());
        assertEquals(1L, result.affectedCount());
        assertTrue(result.rollbackable());
        assertEquals(List.of("RISK_METRIC", "RISK_POINT", "RULE"), result.affectedTypes());
        assertTrue(result.autoDraftEligible());
        assertNotNull(result.recommendation());
        assertEquals("PUBLISH", result.recommendation().getRecommendationType());
        assertEquals(0.96D, result.recommendation().getConfidence());
        assertTrue(result.autoDraftComment().contains("审批意见草稿"));
        verify(executor).simulate(order);
        verify(orderMapper, never()).updateById(any(GovernanceApprovalOrder.class));
        verify(transitionMapper, never()).insert(any(GovernanceApprovalTransition.class));
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

    private GovernanceApprovalActionCommand buildCommand(String actionCode,
                                                         String actionName,
                                                         String subjectType,
                                                         Long subjectId,
                                                         Long workItemId,
                                                         Long operatorUserId,
                                                         Long approverUserId,
                                                         String payloadJson,
                                                         String approvalComment) {
        try {
            for (Constructor<?> constructor : GovernanceApprovalActionCommand.class.getDeclaredConstructors()) {
                if (constructor.getParameterCount() == 9) {
                    return (GovernanceApprovalActionCommand) constructor.newInstance(
                            actionCode,
                            actionName,
                            subjectType,
                            subjectId,
                            workItemId,
                            operatorUserId,
                            approverUserId,
                            payloadJson,
                            approvalComment
                    );
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return new GovernanceApprovalActionCommand(
                actionCode,
                actionName,
                subjectType,
                subjectId,
                operatorUserId,
                approverUserId,
                payloadJson,
                approvalComment
        );
    }

    private void writeField(Object target, String fieldName, Object value) {
        if (target == null) {
            return;
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // compatibility for pre-bridge classes
        }
    }

    private Long readLong(Object target, String fieldName) {
        Object value = readField(target, fieldName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private String readString(Object target, String fieldName) {
        Object value = readField(target, fieldName);
        return value == null ? "" : String.valueOf(value);
    }

    private Object readField(Object target, String fieldName) {
        if (target == null) {
            return null;
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return null;
        }
    }
}
