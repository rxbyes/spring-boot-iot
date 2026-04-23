package com.ghlzm.iot.device.governance;

import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.device.entity.VendorMetricMappingRuleSnapshot;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleMapper;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleSnapshotMapper;
import com.ghlzm.iot.system.entity.GovernanceApprovalOrder;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorMetricMappingRuleGovernanceApprovalExecutorTest {

    @Mock
    private VendorMetricMappingRuleMapper ruleMapper;
    @Mock
    private VendorMetricMappingRuleSnapshotMapper snapshotMapper;

    private VendorMetricMappingRuleGovernanceApprovalExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new VendorMetricMappingRuleGovernanceApprovalExecutor(ruleMapper, snapshotMapper);
    }

    @Test
    void executePublishShouldWritePublishedSnapshotAndActivateRule() {
        GovernanceApprovalOrder order = approvalOrder(
                88001L,
                VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_PUBLISH,
                "{\"ruleId\":7101,\"productId\":1001,\"expectedVersionNo\":3}"
        );
        when(ruleMapper.selectById(7101L)).thenReturn(rule(7101L, 1001L, "disp", "value", "DRAFT", 3));
        when(snapshotMapper.insert(org.mockito.ArgumentMatchers.any(VendorMetricMappingRuleSnapshot.class))).thenReturn(1);
        when(ruleMapper.updateById(org.mockito.ArgumentMatchers.any(VendorMetricMappingRule.class))).thenReturn(1);

        GovernanceApprovalActionExecutionResult result = executor.execute(order);

        assertNotNull(result);
        verify(snapshotMapper).insert(argThat((VendorMetricMappingRuleSnapshot snapshot) ->
                Long.valueOf(7101L).equals(snapshot.getRuleId())
                        && Long.valueOf(1001L).equals(snapshot.getProductId())
                        && Long.valueOf(88001L).equals(snapshot.getApprovalOrderId())
                        && Integer.valueOf(3).equals(snapshot.getPublishedVersionNo())
                        && "PUBLISHED".equals(snapshot.getLifecycleStatus())
        ));
        verify(ruleMapper).updateById(argThat((VendorMetricMappingRule rule) ->
                Long.valueOf(7101L).equals(rule.getId())
                        && Long.valueOf(88001L).equals(rule.getApprovalOrderId())
                        && "ACTIVE".equals(rule.getStatus())
        ));
    }

    @Test
    void executeRollbackShouldRetireLatestSnapshotAndMoveRuleBackToDraft() {
        GovernanceApprovalOrder order = approvalOrder(
                88002L,
                VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_ROLLBACK,
                "{\"ruleId\":7101,\"productId\":1001,\"expectedVersionNo\":3}"
        );
        when(ruleMapper.selectById(7101L)).thenReturn(rule(7101L, 1001L, "disp", "value", "ACTIVE", 3));
        when(snapshotMapper.selectLatestPublishedByRuleId(7101L))
                .thenReturn(snapshot(8101L, 7101L, 1001L, 3, 77001L, "PUBLISHED"));
        when(snapshotMapper.updateById(org.mockito.ArgumentMatchers.any(VendorMetricMappingRuleSnapshot.class))).thenReturn(1);
        when(ruleMapper.updateById(org.mockito.ArgumentMatchers.any(VendorMetricMappingRule.class))).thenReturn(1);

        GovernanceApprovalActionExecutionResult result = executor.execute(order);

        assertNotNull(result);
        verify(snapshotMapper).updateById(argThat((VendorMetricMappingRuleSnapshot snapshot) ->
                Long.valueOf(8101L).equals(snapshot.getId())
                        && "ROLLED_BACK".equals(snapshot.getLifecycleStatus())
        ));
        verify(ruleMapper).updateById(argThat((VendorMetricMappingRule rule) ->
                Long.valueOf(7101L).equals(rule.getId())
                        && Long.valueOf(88002L).equals(rule.getApprovalOrderId())
                        && "DRAFT".equals(rule.getStatus())
        ));
    }

    private GovernanceApprovalOrder approvalOrder(Long orderId, String actionCode, String payloadJson) {
        GovernanceApprovalOrder order = new GovernanceApprovalOrder();
        order.setId(orderId);
        order.setActionCode(actionCode);
        order.setOperatorUserId(10001L);
        order.setPayloadJson(payloadJson);
        return order;
    }

    private VendorMetricMappingRule rule(Long id,
                                         Long productId,
                                         String rawIdentifier,
                                         String targetNormativeIdentifier,
                                         String status,
                                         Integer versionNo) {
        VendorMetricMappingRule rule = new VendorMetricMappingRule();
        rule.setId(id);
        rule.setProductId(productId);
        rule.setScopeType("PRODUCT");
        rule.setRawIdentifier(rawIdentifier);
        rule.setTargetNormativeIdentifier(targetNormativeIdentifier);
        rule.setStatus(status);
        rule.setVersionNo(versionNo);
        rule.setDeleted(0);
        return rule;
    }

    private VendorMetricMappingRuleSnapshot snapshot(Long id,
                                                     Long ruleId,
                                                     Long productId,
                                                     Integer publishedVersionNo,
                                                     Long approvalOrderId,
                                                     String lifecycleStatus) {
        VendorMetricMappingRuleSnapshot snapshot = new VendorMetricMappingRuleSnapshot();
        snapshot.setId(id);
        snapshot.setRuleId(ruleId);
        snapshot.setProductId(productId);
        snapshot.setPublishedVersionNo(publishedVersionNo);
        snapshot.setApprovalOrderId(approvalOrderId);
        snapshot.setLifecycleStatus(lifecycleStatus);
        return snapshot;
    }
}
