package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleBatchStatusDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleHitPreviewDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRulePublishSubmitDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleReplayDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleRollbackSubmitDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleUpsertDTO;
import com.ghlzm.iot.device.service.VendorMetricMappingRuleGovernanceService;
import com.ghlzm.iot.device.service.VendorMetricMappingRuleService;
import com.ghlzm.iot.device.service.VendorMetricMappingRuleSuggestionService;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleHitPreviewVO;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleReplayVO;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleVO;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleSuggestionVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.vo.GovernanceSubmissionResultVO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorMetricMappingRuleControllerTest {

    @Mock
    private VendorMetricMappingRuleService service;
    @Mock
    private VendorMetricMappingRuleSuggestionService suggestionService;
    @Mock
    private VendorMetricMappingRuleGovernanceService governanceService;

    @Mock
    private GovernancePermissionGuard permissionGuard;

    private VendorMetricMappingRuleController controller;

    @BeforeEach
    void setUp() {
        controller = new VendorMetricMappingRuleController(service, suggestionService, governanceService, permissionGuard);
    }

    @Test
    void pageRulesShouldDelegateToService() {
        VendorMetricMappingRuleVO row = new VendorMetricMappingRuleVO();
        row.setId(9201L);
        row.setRawIdentifier("temp_a");
        when(service.pageRules(1001L, "ACTIVE", 1L, 10L))
                .thenReturn(PageResult.of(1L, 1L, 10L, List.of(row)));

        R<PageResult<VendorMetricMappingRuleVO>> response = controller.pageRules(1001L, "ACTIVE", 1L, 10L);

        assertEquals(1L, response.getData().getTotal());
        assertEquals("temp_a", response.getData().getRecords().get(0).getRawIdentifier());
    }

    @Test
    void addRuleShouldRequireGovernPermissionAndDelegateToService() {
        VendorMetricMappingRuleUpsertDTO dto = new VendorMetricMappingRuleUpsertDTO();
        dto.setScopeType("PRODUCT");
        dto.setRawIdentifier("temp_a");
        dto.setTargetNormativeIdentifier("value");
        VendorMetricMappingRuleVO row = new VendorMetricMappingRuleVO();
        row.setId(9201L);
        row.setRawIdentifier("temp_a");
        when(service.createAndGet(1001L, 10001L, dto)).thenReturn(row);

        R<VendorMetricMappingRuleVO> response = controller.addRule(1001L, dto, authentication(10001L));

        assertEquals(9201L, response.getData().getId());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "厂商字段映射规则维护",
                "iot:product-contract:govern"
        );
        verify(service).createAndGet(1001L, 10001L, dto);
    }

    @Test
    void updateRuleShouldRequireGovernPermissionAndDelegateToService() {
        VendorMetricMappingRuleUpsertDTO dto = new VendorMetricMappingRuleUpsertDTO();
        dto.setScopeType("PRODUCT");
        dto.setRawIdentifier("temp_b");
        dto.setTargetNormativeIdentifier("value");
        VendorMetricMappingRuleVO row = new VendorMetricMappingRuleVO();
        row.setId(9202L);
        row.setRawIdentifier("temp_b");
        when(service.updateAndGet(1001L, 9202L, 10001L, dto)).thenReturn(row);

        R<VendorMetricMappingRuleVO> response = controller.updateRule(1001L, 9202L, dto, authentication(10001L));

        assertEquals(9202L, response.getData().getId());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "厂商字段映射规则维护",
                "iot:product-contract:govern"
        );
        verify(service).updateAndGet(1001L, 9202L, 10001L, dto);
    }

    @Test
    void listSuggestionsShouldRequireGovernPermissionAndDelegateToService() {
        VendorMetricMappingRuleSuggestionVO row = new VendorMetricMappingRuleSuggestionVO();
        row.setRawIdentifier("disp");
        row.setStatus("READY_TO_CREATE");
        when(suggestionService.listSuggestions(1001L, false, false, 1))
                .thenReturn(List.of(row));

        R<List<VendorMetricMappingRuleSuggestionVO>> response =
                controller.listSuggestions(1001L, false, false, 1, authentication(10001L));

        assertEquals("READY_TO_CREATE", response.getData().get(0).getStatus());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "厂商字段映射规则建议",
                "iot:product-contract:govern"
        );
        verify(suggestionService).listSuggestions(1001L, false, false, 1);
    }

    @Test
    void submitPublishShouldRequireProductContractGovernPermission() {
        when(governanceService.submitPublish(eq(1001L), eq(7101L), eq(10001L), any()))
                .thenReturn(GovernanceSubmissionResultVO.pendingApproval(null, 99001L));

        R<GovernanceSubmissionResultVO> response = controller.submitPublish(
                1001L,
                7101L,
                new VendorMetricMappingRulePublishSubmitDTO("发布 value alias"),
                authentication(10001L)
        );

        assertEquals(99001L, response.getData().getApprovalOrderId());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "厂商字段映射规则发布",
                GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN
        );
        verify(governanceService).submitPublish(eq(1001L), eq(7101L), eq(10001L), any());
    }

    @Test
    void submitRollbackShouldRequireProductContractRollbackPermission() {
        when(governanceService.submitRollback(eq(1001L), eq(7101L), eq(10001L), any()))
                .thenReturn(GovernanceSubmissionResultVO.pendingApproval(null, 99002L));

        R<GovernanceSubmissionResultVO> response = controller.submitRollback(
                1001L,
                7101L,
                new VendorMetricMappingRuleRollbackSubmitDTO("回滚 value alias"),
                authentication(10001L)
        );

        assertEquals(99002L, response.getData().getApprovalOrderId());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "厂商字段映射规则回滚",
                GovernancePermissionCodes.PRODUCT_CONTRACT_ROLLBACK
        );
        verify(governanceService).submitRollback(eq(1001L), eq(7101L), eq(10001L), any());
    }

    @Test
    void previewHitShouldRequireGovernPermissionAndDelegateToService() {
        VendorMetricMappingRuleHitPreviewDTO dto = new VendorMetricMappingRuleHitPreviewDTO();
        dto.setRawIdentifier("disp");
        VendorMetricMappingRuleHitPreviewVO row = new VendorMetricMappingRuleHitPreviewVO();
        row.setMatched(Boolean.TRUE);
        row.setHitSource("PUBLISHED_SNAPSHOT");
        row.setTargetNormativeIdentifier("value");
        when(governanceService.previewHit(1001L, dto)).thenReturn(row);

        R<VendorMetricMappingRuleHitPreviewVO> response = controller.previewHit(1001L, dto, authentication(10001L));

        assertEquals("value", response.getData().getTargetNormativeIdentifier());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "厂商字段映射规则试命中",
                GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN
        );
        verify(governanceService).previewHit(1001L, dto);
    }

    @Test
    void batchStatusShouldRequireGovernPermissionAndDelegateToService() {
        VendorMetricMappingRuleBatchStatusDTO dto = new VendorMetricMappingRuleBatchStatusDTO();
        dto.setRuleIds(List.of(7101L, 7102L));
        dto.setTargetStatus("DISABLED");
        when(service.batchStatus(1001L, 10001L, dto)).thenReturn(Map.of(
                "requestedCount", 2,
                "matchedCount", 2,
                "changedCount", 2,
                "targetStatus", "DISABLED"
        ));

        R<Map<String, Object>> response = controller.batchStatus(1001L, dto, authentication(10001L));

        assertEquals(2, response.getData().get("changedCount"));
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "厂商字段映射规则维护",
                GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN
        );
        verify(service).batchStatus(1001L, 10001L, dto);
    }

    @Test
    void replayShouldRequireGovernPermissionAndDelegateToService() {
        VendorMetricMappingRuleReplayDTO dto = new VendorMetricMappingRuleReplayDTO();
        dto.setRawIdentifier("disp");
        dto.setLogicalChannelCode("L1_LF_1");
        dto.setSampleValue("0.2136");
        VendorMetricMappingRuleReplayVO replay = new VendorMetricMappingRuleReplayVO();
        replay.setMatched(Boolean.TRUE);
        replay.setHitSource("PUBLISHED_SNAPSHOT");
        replay.setMatchedScopeType("PRODUCT");
        replay.setRawIdentifier("disp");
        replay.setLogicalChannelCode("L1_LF_1");
        replay.setTargetNormativeIdentifier("value");
        replay.setCanonicalIdentifier("value");
        replay.setSampleValue("0.2136");
        when(service.replay(1001L, dto)).thenReturn(replay);

        R<VendorMetricMappingRuleReplayVO> response = controller.replay(1001L, dto, authentication(10001L));

        assertEquals("PRODUCT", response.getData().getMatchedScopeType());
        assertEquals("value", response.getData().getCanonicalIdentifier());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "厂商字段映射规则回放校验",
                GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN
        );
        verify(service).replay(1001L, dto);
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
