package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.protocol.dto.ProtocolDecryptPreviewDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolDecryptProfileUpsertDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolFamilyDefinitionUpsertDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolGovernanceBatchSubmitDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolGovernanceReplayDTO;
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
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/governance/protocol")
public class ProtocolGovernanceController {

    private final ProtocolSecurityGovernanceService service;
    private final ProtocolTemplateGovernanceService templateGovernanceService;
    private final ProtocolTemplateReplayService templateReplayService;
    private final ProtocolGovernanceApprovalService approvalService;
    private final GovernancePermissionGuard permissionGuard;

    public ProtocolGovernanceController(ProtocolSecurityGovernanceService service,
                                        ProtocolTemplateGovernanceService templateGovernanceService,
                                        ProtocolTemplateReplayService templateReplayService,
                                        ProtocolGovernanceApprovalService approvalService,
                                        GovernancePermissionGuard permissionGuard) {
        this.service = service;
        this.templateGovernanceService = templateGovernanceService;
        this.templateReplayService = templateReplayService;
        this.approvalService = approvalService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/families")
    public R<PageResult<ProtocolFamilyDefinitionVO>> pageFamilies(@RequestParam(required = false) String keyword,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) Long pageNum,
                                                                  @RequestParam(required = false) Long pageSize,
                                                                  Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议族定义查询",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_FAMILY_DRAFT,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_FAMILY_PUBLISH,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_FAMILY_ROLLBACK,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(service.pageFamilies(keyword, status, pageNum, pageSize));
    }

    @GetMapping("/families/{familyId}")
    public R<ProtocolFamilyDefinitionVO> getFamilyDetail(@PathVariable Long familyId, Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议族定义详情",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_FAMILY_DRAFT,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_FAMILY_PUBLISH,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_FAMILY_ROLLBACK,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(service.getFamilyDetail(familyId));
    }

    @GetMapping("/decrypt-profiles")
    public R<PageResult<ProtocolDecryptProfileVO>> pageDecryptProfiles(@RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) String status,
                                                                       @RequestParam(required = false) Long pageNum,
                                                                       @RequestParam(required = false) Long pageSize,
                                                                       Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议解密档案查询",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_DRAFT,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_PREVIEW,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_REPLAY,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_PUBLISH,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_ROLLBACK,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(service.pageDecryptProfiles(keyword, status, pageNum, pageSize));
    }

    @GetMapping("/decrypt-profiles/{profileId}")
    public R<ProtocolDecryptProfileVO> getDecryptProfileDetail(@PathVariable Long profileId,
                                                               Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议解密档案详情",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_DRAFT,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_PREVIEW,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_REPLAY,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_PUBLISH,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_ROLLBACK,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(service.getDecryptProfileDetail(profileId));
    }

    @PostMapping("/families")
    public R<ProtocolFamilyDefinitionVO> saveFamily(@RequestBody @Valid ProtocolFamilyDefinitionUpsertDTO dto,
                                                    Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议族定义维护",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_FAMILY_DRAFT,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(service.saveFamily(dto, currentUserId));
    }

    @PostMapping("/decrypt-profiles")
    public R<ProtocolDecryptProfileVO> saveDecryptProfile(@RequestBody @Valid ProtocolDecryptProfileUpsertDTO dto,
                                                          Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议解密档案维护",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_DRAFT,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(service.saveDecryptProfile(dto, currentUserId));
    }

    @PostMapping("/families/{familyId}/submit-publish")
    public R<GovernanceSubmissionResultVO> submitFamilyPublish(@PathVariable Long familyId,
                                                               @RequestBody(required = false) ProtocolGovernanceSubmitDTO dto,
                                                               Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议族定义发布",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_FAMILY_PUBLISH,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(approvalService.submitFamilyPublish(familyId, currentUserId, submitReasonOf(dto)));
    }

    @PostMapping("/families/batch-submit-publish")
    public R<ProtocolGovernanceBatchSubmitResultVO> submitFamilyBatchPublish(
            @RequestBody @Valid ProtocolGovernanceBatchSubmitDTO dto,
            Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议族定义批量发布",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_FAMILY_PUBLISH,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(submitFamilyBatch(dto, currentUserId, false));
    }

    @PostMapping("/families/{familyId}/submit-rollback")
    public R<GovernanceSubmissionResultVO> submitFamilyRollback(@PathVariable Long familyId,
                                                                @RequestBody(required = false) ProtocolGovernanceSubmitDTO dto,
                                                                Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议族定义回滚",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_FAMILY_ROLLBACK,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(approvalService.submitFamilyRollback(familyId, currentUserId, submitReasonOf(dto)));
    }

    @PostMapping("/families/batch-submit-rollback")
    public R<ProtocolGovernanceBatchSubmitResultVO> submitFamilyBatchRollback(
            @RequestBody @Valid ProtocolGovernanceBatchSubmitDTO dto,
            Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议族定义批量回滚",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_FAMILY_ROLLBACK,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(submitFamilyBatch(dto, currentUserId, true));
    }

    @PostMapping("/decrypt-profiles/{profileId}/submit-publish")
    public R<GovernanceSubmissionResultVO> submitDecryptProfilePublish(@PathVariable Long profileId,
                                                                       @RequestBody(required = false) ProtocolGovernanceSubmitDTO dto,
                                                                       Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议解密档案发布",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_PUBLISH,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(approvalService.submitDecryptProfilePublish(profileId, currentUserId, submitReasonOf(dto)));
    }

    @PostMapping("/decrypt-profiles/batch-submit-publish")
    public R<ProtocolGovernanceBatchSubmitResultVO> submitDecryptProfileBatchPublish(
            @RequestBody @Valid ProtocolGovernanceBatchSubmitDTO dto,
            Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议解密档案批量发布",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_PUBLISH,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(submitProfileBatch(dto, currentUserId, false));
    }

    @PostMapping("/decrypt-profiles/{profileId}/submit-rollback")
    public R<GovernanceSubmissionResultVO> submitDecryptProfileRollback(@PathVariable Long profileId,
                                                                        @RequestBody(required = false) ProtocolGovernanceSubmitDTO dto,
                                                                        Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议解密档案回滚",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_ROLLBACK,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(approvalService.submitDecryptProfileRollback(profileId, currentUserId, submitReasonOf(dto)));
    }

    @PostMapping("/decrypt-profiles/batch-submit-rollback")
    public R<ProtocolGovernanceBatchSubmitResultVO> submitDecryptProfileBatchRollback(
            @RequestBody @Valid ProtocolGovernanceBatchSubmitDTO dto,
            Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议解密档案批量回滚",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_ROLLBACK,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(submitProfileBatch(dto, currentUserId, true));
    }

    @PostMapping("/decrypt-profiles/preview")
    public R<ProtocolDecryptPreviewVO> previewDecrypt(@RequestBody @Valid ProtocolDecryptPreviewDTO dto,
                                                      Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议解密试算",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_PREVIEW,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(service.previewDecrypt(dto));
    }

    @PostMapping("/decrypt-profiles/replay")
    public R<ProtocolGovernanceReplayVO> replayDecrypt(@RequestBody ProtocolGovernanceReplayDTO dto,
                                                       Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议解密命中回放",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_DECRYPT_REPLAY,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(service.replayDecrypt(dto));
    }

    @GetMapping("/templates")
    public R<PageResult<ProtocolTemplateDefinitionVO>> pageTemplates(@RequestParam(required = false) String keyword,
                                                                     @RequestParam(required = false) String status,
                                                                     @RequestParam(required = false) Long pageNum,
                                                                     @RequestParam(required = false) Long pageSize,
                                                                     Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议模板查询",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_TEMPLATE_DRAFT,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_TEMPLATE_REPLAY,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_TEMPLATE_PUBLISH,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(templateGovernanceService.pageTemplates(keyword, status, pageNum, pageSize));
    }

    @GetMapping("/templates/{templateId}")
    public R<ProtocolTemplateDefinitionVO> getTemplateDetail(@PathVariable Long templateId,
                                                             Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议模板详情",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_TEMPLATE_DRAFT,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_TEMPLATE_REPLAY,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_TEMPLATE_PUBLISH,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(templateGovernanceService.getTemplateDetail(templateId));
    }

    @PostMapping("/templates")
    public R<ProtocolTemplateDefinitionVO> saveTemplate(@RequestBody @Valid ProtocolTemplateUpsertDTO dto,
                                                        Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议模板维护",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_TEMPLATE_DRAFT,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(templateGovernanceService.saveTemplate(dto, currentUserId));
    }

    @PostMapping("/templates/{templateId}/publish")
    public R<ProtocolTemplateDefinitionVO> publishTemplate(@PathVariable Long templateId,
                                                           @RequestBody(required = false) ProtocolTemplateSubmitDTO dto,
                                                           Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议模板发布",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_TEMPLATE_PUBLISH,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(templateGovernanceService.publishTemplate(templateId, currentUserId, dto));
    }

    @PostMapping("/templates/replay")
    public R<ProtocolTemplateReplayVO> replayTemplate(@RequestBody @Valid ProtocolTemplateReplayDTO dto,
                                                      Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议模板回放",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_TEMPLATE_REPLAY,
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(templateReplayService.replay(dto));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }

    private String submitReasonOf(ProtocolGovernanceSubmitDTO dto) {
        return dto == null ? null : dto.getSubmitReason();
    }

    private ProtocolGovernanceBatchSubmitResultVO submitFamilyBatch(ProtocolGovernanceBatchSubmitDTO dto,
                                                                    Long currentUserId,
                                                                    boolean rollback) {
        String reason = submitReasonOf(dto);
        ProtocolGovernanceBatchSubmitResultVO result = new ProtocolGovernanceBatchSubmitResultVO();
        List<ProtocolGovernanceBatchSubmitResultVO.Item> items = new ArrayList<>();
        int submitted = 0;
        int failed = 0;
        for (Long recordId : uniqueRecordIds(dto)) {
            ProtocolGovernanceBatchSubmitResultVO.Item item = new ProtocolGovernanceBatchSubmitResultVO.Item();
            item.setRecordId(recordId);
            try {
                GovernanceSubmissionResultVO submission = rollback
                        ? approvalService.submitFamilyRollback(recordId, currentUserId, reason)
                        : approvalService.submitFamilyPublish(recordId, currentUserId, reason);
                item.setSuccess(Boolean.TRUE);
                item.setApprovalOrderId(submission == null ? null : submission.getApprovalOrderId());
                submitted++;
            } catch (RuntimeException ex) {
                item.setSuccess(Boolean.FALSE);
                item.setErrorMessage(ex.getMessage());
                failed++;
            }
            items.add(item);
        }
        result.setItems(items);
        result.setTotalCount(items.size());
        result.setSubmittedCount(submitted);
        result.setFailedCount(failed);
        return result;
    }

    private ProtocolGovernanceBatchSubmitResultVO submitProfileBatch(ProtocolGovernanceBatchSubmitDTO dto,
                                                                     Long currentUserId,
                                                                     boolean rollback) {
        String reason = submitReasonOf(dto);
        ProtocolGovernanceBatchSubmitResultVO result = new ProtocolGovernanceBatchSubmitResultVO();
        List<ProtocolGovernanceBatchSubmitResultVO.Item> items = new ArrayList<>();
        int submitted = 0;
        int failed = 0;
        for (Long recordId : uniqueRecordIds(dto)) {
            ProtocolGovernanceBatchSubmitResultVO.Item item = new ProtocolGovernanceBatchSubmitResultVO.Item();
            item.setRecordId(recordId);
            try {
                GovernanceSubmissionResultVO submission = rollback
                        ? approvalService.submitDecryptProfileRollback(recordId, currentUserId, reason)
                        : approvalService.submitDecryptProfilePublish(recordId, currentUserId, reason);
                item.setSuccess(Boolean.TRUE);
                item.setApprovalOrderId(submission == null ? null : submission.getApprovalOrderId());
                submitted++;
            } catch (RuntimeException ex) {
                item.setSuccess(Boolean.FALSE);
                item.setErrorMessage(ex.getMessage());
                failed++;
            }
            items.add(item);
        }
        result.setItems(items);
        result.setTotalCount(items.size());
        result.setSubmittedCount(submitted);
        result.setFailedCount(failed);
        return result;
    }

    private List<Long> uniqueRecordIds(ProtocolGovernanceBatchSubmitDTO dto) {
        Set<Long> uniqueIds = new LinkedHashSet<>();
        if (dto != null && dto.getRecordIds() != null) {
            for (Long recordId : dto.getRecordIds()) {
                if (recordId != null && recordId > 0) {
                    uniqueIds.add(recordId);
                }
            }
        }
        return List.copyOf(uniqueIds);
    }

    private String submitReasonOf(ProtocolGovernanceBatchSubmitDTO dto) {
        return dto == null ? null : dto.getSubmitReason();
    }
}
