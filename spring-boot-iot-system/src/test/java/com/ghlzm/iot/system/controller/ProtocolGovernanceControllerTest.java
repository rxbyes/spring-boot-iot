package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.protocol.dto.ProtocolGovernanceBatchSubmitDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolGovernanceReplayDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolDecryptPreviewDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolDecryptProfileUpsertDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolFamilyDefinitionUpsertDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolGovernanceSubmitDTO;
import com.ghlzm.iot.framework.protocol.service.ProtocolSecurityGovernanceService;
import com.ghlzm.iot.framework.protocol.template.dto.ProtocolTemplateReplayDTO;
import com.ghlzm.iot.framework.protocol.template.dto.ProtocolTemplateSubmitDTO;
import com.ghlzm.iot.framework.protocol.template.dto.ProtocolTemplateUpsertDTO;
import com.ghlzm.iot.framework.protocol.template.service.ProtocolTemplateGovernanceService;
import com.ghlzm.iot.framework.protocol.template.service.ProtocolTemplateReplayService;
import com.ghlzm.iot.framework.protocol.template.vo.ProtocolTemplateDefinitionVO;
import com.ghlzm.iot.framework.protocol.template.vo.ProtocolTemplateReplayVO;
import com.ghlzm.iot.framework.protocol.vo.ProtocolGovernanceBatchSubmitResultVO;
import com.ghlzm.iot.framework.protocol.vo.ProtocolGovernanceReplayVO;
import com.ghlzm.iot.framework.protocol.vo.ProtocolDecryptPreviewVO;
import com.ghlzm.iot.framework.protocol.vo.ProtocolDecryptProfileVO;
import com.ghlzm.iot.framework.protocol.vo.ProtocolFamilyDefinitionVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.ProtocolGovernanceApprovalService;
import com.ghlzm.iot.system.vo.GovernanceSubmissionResultVO;
import java.util.List;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProtocolGovernanceControllerTest {

    @Mock
    private ProtocolSecurityGovernanceService service;
    @Mock
    private ProtocolTemplateGovernanceService templateGovernanceService;
    @Mock
    private ProtocolTemplateReplayService templateReplayService;
    @Mock
    private ProtocolGovernanceApprovalService approvalService;
    @Mock
    private GovernancePermissionGuard permissionGuard;

    private ProtocolGovernanceController controller;

    @BeforeEach
    void setUp() {
        controller = new ProtocolGovernanceController(
                service,
                templateGovernanceService,
                templateReplayService,
                approvalService,
                permissionGuard
        );
    }

    @Test
    void pageFamiliesShouldRequireProtocolGovernanceEditPermission() {
        ProtocolFamilyDefinitionVO row = new ProtocolFamilyDefinitionVO();
        row.setId(9101L);
        row.setFamilyCode("legacy-dp-crack");
        when(service.pageFamilies(null, null, 1L, 10L))
                .thenReturn(PageResult.of(1L, 1L, 10L, List.of(row)));

        R<PageResult<ProtocolFamilyDefinitionVO>> response =
                controller.pageFamilies(null, null, 1L, 10L, authentication(10001L));

        assertEquals(1L, response.getData().getTotal());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "协议族定义查询",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
    }

    @Test
    void saveDecryptProfileShouldRequireProtocolGovernanceEditPermission() {
        ProtocolDecryptProfileVO vo = new ProtocolDecryptProfileVO();
        vo.setId(9201L);
        vo.setProfileCode("des-62000001");
        vo.setStatus("DRAFT");
        when(service.saveDecryptProfile(any(), eq(10001L))).thenReturn(vo);

        R<ProtocolDecryptProfileVO> response = controller.saveDecryptProfile(
                new ProtocolDecryptProfileUpsertDTO(
                        "des-62000001",
                        "DES",
                        "IOT_PROTOCOL_CRYPTO",
                        "62000001",
                        "DES/CBC/PKCS5Padding",
                        "demo-secret"
                ),
                authentication(10001L)
        );

        assertEquals("des-62000001", response.getData().getProfileCode());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "协议解密档案维护",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
    }

    @Test
    void submitFamilyPublishShouldRequireProtocolGovernanceEditPermission() {
        when(approvalService.submitFamilyPublish(eq(9101L), eq(10001L), eq("发布裂缝协议族")))
                .thenReturn(GovernanceSubmissionResultVO.pendingApproval(null, 99101L));

        R<GovernanceSubmissionResultVO> response = controller.submitFamilyPublish(
                9101L,
                new ProtocolGovernanceSubmitDTO("发布裂缝协议族"),
                authentication(10001L)
        );

        assertEquals(99101L, response.getData().getApprovalOrderId());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "协议族定义发布",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
    }

    @Test
    void previewDecryptShouldRequireProtocolGovernanceEditPermission() {
        ProtocolDecryptPreviewVO vo = new ProtocolDecryptPreviewVO();
        vo.setMatched(Boolean.TRUE);
        vo.setResolvedProfileCode("des-62000001");
        when(service.previewDecrypt(any())).thenReturn(vo);

        R<ProtocolDecryptPreviewVO> response = controller.previewDecrypt(
                new ProtocolDecryptPreviewDTO("62000000", "mqtt-json", "legacy-dp-crack"),
                authentication(10001L)
        );

        assertEquals("des-62000001", response.getData().getResolvedProfileCode());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "协议解密试算",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
    }

    @Test
    void saveFamilyShouldDelegateToServiceWithCurrentUser() {
        ProtocolFamilyDefinitionVO vo = new ProtocolFamilyDefinitionVO();
        vo.setId(9101L);
        vo.setFamilyCode("legacy-dp-crack");
        when(service.saveFamily(any(), eq(10001L))).thenReturn(vo);

        R<ProtocolFamilyDefinitionVO> response = controller.saveFamily(
                new ProtocolFamilyDefinitionUpsertDTO(
                        "legacy-dp-crack",
                        "mqtt-json",
                        "裂缝旧 $dp 协议族",
                        "des-62000001",
                        "AES",
                        "LEGACY_DP"
                ),
                authentication(10001L)
        );

        assertEquals("legacy-dp-crack", response.getData().getFamilyCode());
        verify(service).saveFamily(any(), eq(10001L));
    }

    @Test
    void getFamilyDetailShouldRequireProtocolGovernanceEditPermission() {
        ProtocolFamilyDefinitionVO detail = new ProtocolFamilyDefinitionVO();
        detail.setId(9101L);
        detail.setFamilyCode("legacy-dp-crack");
        when(service.getFamilyDetail(9101L)).thenReturn(detail);

        R<ProtocolFamilyDefinitionVO> response = controller.getFamilyDetail(9101L, authentication(10001L));

        assertEquals(9101L, response.getData().getId());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "协议族定义详情",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
    }

    @Test
    void replayDecryptShouldRequireProtocolGovernanceEditPermission() {
        ProtocolGovernanceReplayVO replay = new ProtocolGovernanceReplayVO();
        replay.setMatched(Boolean.TRUE);
        replay.setResolvedProfileCode("des-62000001");
        when(service.replayDecrypt(any())).thenReturn(replay);

        R<ProtocolGovernanceReplayVO> response = controller.replayDecrypt(
                new ProtocolGovernanceReplayDTO("legacy-dp-crack", "mqtt-json", "62000001"),
                authentication(10001L)
        );

        assertEquals("des-62000001", response.getData().getResolvedProfileCode());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "协议解密命中回放",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
    }

    @Test
    void pageTemplatesShouldRequireProtocolGovernanceEditPermission() {
        ProtocolTemplateDefinitionVO row = new ProtocolTemplateDefinitionVO();
        row.setId(9301L);
        row.setTemplateCode("legacy-dp-crack-v1");
        when(templateGovernanceService.pageTemplates(null, null, 1L, 10L))
                .thenReturn(PageResult.of(1L, 1L, 10L, List.of(row)));

        R<PageResult<ProtocolTemplateDefinitionVO>> response =
                controller.pageTemplates(null, null, 1L, 10L, authentication(10001L));

        assertEquals(1L, response.getData().getTotal());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "协议模板查询",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
    }

    @Test
    void saveTemplateShouldDelegateToGovernanceServiceWithCurrentUser() {
        ProtocolTemplateDefinitionVO vo = new ProtocolTemplateDefinitionVO();
        vo.setId(9301L);
        vo.setTemplateCode("legacy-dp-crack-v1");
        when(templateGovernanceService.saveTemplate(any(), eq(10001L))).thenReturn(vo);

        R<ProtocolTemplateDefinitionVO> response = controller.saveTemplate(
                new ProtocolTemplateUpsertDTO() {{
                    setTemplateCode("legacy-dp-crack-v1");
                    setFamilyCode("legacy-dp");
                    setProtocolCode("mqtt-json");
                    setDisplayName("裂缝 legacy 子模板");
                    setExpressionJson("{\"logicalPattern\":\"^L1_LF_\\\\d+$\"}");
                    setOutputMappingJson("{\"value\":\"$.value\"}");
                }},
                authentication(10001L)
        );

        assertEquals("legacy-dp-crack-v1", response.getData().getTemplateCode());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "协议模板维护",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        verify(templateGovernanceService).saveTemplate(any(), eq(10001L));
    }

    @Test
    void publishTemplateShouldRequireProtocolGovernanceEditPermission() {
        ProtocolTemplateDefinitionVO vo = new ProtocolTemplateDefinitionVO();
        vo.setId(9301L);
        vo.setTemplateCode("legacy-dp-crack-v1");
        vo.setStatus("ACTIVE");
        vo.setPublishedStatus("PUBLISHED");
        when(templateGovernanceService.publishTemplate(eq(9301L), eq(10001L), any())).thenReturn(vo);

        R<ProtocolTemplateDefinitionVO> response = controller.publishTemplate(
                9301L,
                new ProtocolTemplateSubmitDTO("首次发布"),
                authentication(10001L)
        );

        assertEquals("PUBLISHED", response.getData().getPublishedStatus());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "协议模板发布",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
    }

    @Test
    void replayTemplateShouldRequireProtocolGovernanceEditPermission() {
        ProtocolTemplateReplayVO replay = new ProtocolTemplateReplayVO();
        replay.setMatched(Boolean.TRUE);
        replay.setTemplateCode("legacy-dp-crack-v1");
        when(templateReplayService.replay(any())).thenReturn(replay);

        R<ProtocolTemplateReplayVO> response = controller.replayTemplate(
                new ProtocolTemplateReplayDTO() {{
                    setTemplateCode("legacy-dp-crack-v1");
                    setPayloadJson("{\"L1_LF_1\":{\"2026-04-05T08:23:10.000Z\":0.2136}}");
                }},
                authentication(10001L)
        );

        assertEquals("legacy-dp-crack-v1", response.getData().getTemplateCode());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "协议模板回放",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
    }

    @Test
    void submitFamilyBatchPublishShouldCollectSuccessAndFailure() {
        when(approvalService.submitFamilyPublish(eq(9101L), eq(10001L), eq("批量发布协议族")))
                .thenReturn(GovernanceSubmissionResultVO.pendingApproval(null, 99101L));
        doThrow(new BizException("协议族定义不存在: 9102"))
                .when(approvalService)
                .submitFamilyPublish(eq(9102L), eq(10001L), eq("批量发布协议族"));

        R<ProtocolGovernanceBatchSubmitResultVO> response = controller.submitFamilyBatchPublish(
                new ProtocolGovernanceBatchSubmitDTO(List.of(9101L, 9102L), "批量发布协议族"),
                authentication(10001L)
        );

        assertEquals(2, response.getData().getTotalCount());
        assertEquals(1, response.getData().getSubmittedCount());
        assertEquals(1, response.getData().getFailedCount());
        assertEquals(2, response.getData().getItems().size());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "协议族定义批量发布",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
