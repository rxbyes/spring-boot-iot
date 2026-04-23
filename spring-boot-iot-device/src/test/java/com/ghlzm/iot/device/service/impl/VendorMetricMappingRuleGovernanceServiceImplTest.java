package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.dto.VendorMetricMappingRuleHitPreviewDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRulePublishSubmitDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleRollbackSubmitDTO;
import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.device.entity.VendorMetricMappingRuleSnapshot;
import com.ghlzm.iot.device.governance.VendorMetricMappingRuleGovernanceApprovalPayloads;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleMapper;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleSnapshotMapper;
import com.ghlzm.iot.device.service.VendorMetricMappingRuleGovernanceService;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleHitPreviewVO;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleLedgerRowVO;
import com.ghlzm.iot.system.service.GovernanceApprovalPolicyResolver;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.vo.GovernanceSubmissionResultVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorMetricMappingRuleGovernanceServiceImplTest {

    @Mock
    private VendorMetricMappingRuleMapper ruleMapper;
    @Mock
    private VendorMetricMappingRuleSnapshotMapper snapshotMapper;
    @Mock
    private GovernanceApprovalPolicyResolver policyResolver;
    @Mock
    private GovernanceApprovalService approvalService;

    private VendorMetricMappingRuleGovernanceService service;

    @BeforeEach
    void setUp() {
        service = new VendorMetricMappingRuleGovernanceServiceImpl(
                ruleMapper,
                snapshotMapper,
                policyResolver,
                approvalService
        );
    }

    @Test
    void submitPublishShouldCreatePendingApprovalUsingFixedReviewer() {
        when(ruleMapper.selectById(7101L)).thenReturn(rule(7101L, 1001L, "disp", "value", "DRAFT", 3));
        when(policyResolver.resolveApproverUserId(
                VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_PUBLISH,
                10001L
        )).thenReturn(20001L);
        when(approvalService.submitAction(any())).thenReturn(99001L);

        GovernanceSubmissionResultVO result = service.submitPublish(
                1001L,
                7101L,
                10001L,
                new VendorMetricMappingRulePublishSubmitDTO("发布 value alias")
        );

        assertEquals(99001L, result.getApprovalOrderId());
        assertEquals("PENDING", result.getApprovalStatus());
        verify(approvalService).submitAction(argThat(command ->
                VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_PUBLISH.equals(command.actionCode())
                        && Long.valueOf(20001L).equals(command.approverUserId())
                        && command.payloadJson().contains("\"expectedVersionNo\":3")
                        && command.payloadJson().contains("\"ruleId\":7101")
        ));
    }

    @Test
    void submitRollbackShouldCreatePendingApprovalUsingFixedReviewer() {
        when(ruleMapper.selectById(7101L)).thenReturn(rule(7101L, 1001L, "disp", "value", "ACTIVE", 4));
        when(policyResolver.resolveApproverUserId(
                VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_ROLLBACK,
                10001L
        )).thenReturn(20001L);
        when(approvalService.submitAction(any())).thenReturn(99002L);

        GovernanceSubmissionResultVO result = service.submitRollback(
                1001L,
                7101L,
                10001L,
                new VendorMetricMappingRuleRollbackSubmitDTO("回滚 value alias")
        );

        assertEquals(99002L, result.getApprovalOrderId());
        assertEquals("PENDING_APPROVAL", result.getExecutionStatus());
        verify(approvalService).submitAction(argThat(command ->
                VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_ROLLBACK.equals(command.actionCode())
                        && Long.valueOf(20001L).equals(command.approverUserId())
                        && command.payloadJson().contains("\"ruleId\":7101")
        ));
    }

    @Test
    void pageLedgerShouldPreferPublishedSnapshotStateOverDraftRow() {
        when(ruleMapper.selectById(7101L)).thenReturn(rule(7101L, 1001L, "disp", "value", "DRAFT", 4));
        when(snapshotMapper.selectLatestPublishedByRuleId(7101L))
                .thenReturn(snapshot(8101L, 7101L, 1001L, "disp", "value", 3, 88001L));

        VendorMetricMappingRuleLedgerRowVO row = service.getLedgerRow(1001L, 7101L);

        assertEquals("DRAFT", row.getDraftStatus());
        assertEquals("PUBLISHED", row.getPublishedStatus());
        assertEquals(3, row.getPublishedVersionNo());
        assertEquals(4, row.getDraftVersionNo());
    }

    @Test
    void previewHitShouldUsePublishedSnapshotBeforeDraftRow() {
        when(snapshotMapper.selectPublishedByProductId(1001L))
                .thenReturn(List.of(snapshot(8101L, 7101L, 1001L, "disp", "value", 3, 88001L)));

        VendorMetricMappingRuleHitPreviewVO result = service.previewHit(
                1001L,
                preview("disp", null)
        );

        assertTrue(result.getMatched());
        assertEquals("PUBLISHED_SNAPSHOT", result.getHitSource());
        assertEquals("value", result.getTargetNormativeIdentifier());
    }

    @Test
    void previewHitShouldFallbackToDraftRowWhenNoPublishedSnapshotMatches() {
        when(snapshotMapper.selectPublishedByProductId(1001L)).thenReturn(List.of());
        when(ruleMapper.selectList(any()))
                .thenReturn(List.of(rule(7101L, 1001L, "disp", "sensor_state", "DRAFT", 4)));

        VendorMetricMappingRuleHitPreviewVO result = service.previewHit(
                1001L,
                preview("disp", null)
        );

        assertTrue(result.getMatched());
        assertEquals("DRAFT_RULE", result.getHitSource());
        assertEquals("sensor_state", result.getTargetNormativeIdentifier());
        assertEquals(7101L, result.getRuleId());
    }

    private VendorMetricMappingRuleHitPreviewDTO preview(String rawIdentifier, String logicalChannelCode) {
        VendorMetricMappingRuleHitPreviewDTO dto = new VendorMetricMappingRuleHitPreviewDTO();
        dto.setRawIdentifier(rawIdentifier);
        dto.setLogicalChannelCode(logicalChannelCode);
        return dto;
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
                                                     String rawIdentifier,
                                                     String targetNormativeIdentifier,
                                                     Integer publishedVersionNo,
                                                     Long approvalOrderId) {
        VendorMetricMappingRuleSnapshot snapshot = new VendorMetricMappingRuleSnapshot();
        snapshot.setId(id);
        snapshot.setRuleId(ruleId);
        snapshot.setProductId(productId);
        snapshot.setPublishedVersionNo(publishedVersionNo);
        snapshot.setApprovalOrderId(approvalOrderId);
        snapshot.setLifecycleStatus("PUBLISHED");
        snapshot.setSnapshotJson("{\"rawIdentifier\":\"" + rawIdentifier + "\",\"targetNormativeIdentifier\":\""
                + targetNormativeIdentifier + "\"}");
        return snapshot;
    }
}
