package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.protocol.dto.ProtocolDecryptPreviewDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolDecryptProfileUpsertDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolFamilyDefinitionUpsertDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolGovernanceSubmitDTO;
import com.ghlzm.iot.framework.protocol.service.ProtocolSecurityGovernanceService;
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

@RestController
@RequestMapping("/api/governance/protocol")
public class ProtocolGovernanceController {

    private final ProtocolSecurityGovernanceService service;
    private final ProtocolGovernanceApprovalService approvalService;
    private final GovernancePermissionGuard permissionGuard;

    public ProtocolGovernanceController(ProtocolSecurityGovernanceService service,
                                        ProtocolGovernanceApprovalService approvalService,
                                        GovernancePermissionGuard permissionGuard) {
        this.service = service;
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
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(service.pageFamilies(keyword, status, pageNum, pageSize));
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
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(service.pageDecryptProfiles(keyword, status, pageNum, pageSize));
    }

    @PostMapping("/families")
    public R<ProtocolFamilyDefinitionVO> saveFamily(@RequestBody @Valid ProtocolFamilyDefinitionUpsertDTO dto,
                                                    Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议族定义维护",
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
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(approvalService.submitFamilyPublish(familyId, currentUserId, submitReasonOf(dto)));
    }

    @PostMapping("/families/{familyId}/submit-rollback")
    public R<GovernanceSubmissionResultVO> submitFamilyRollback(@PathVariable Long familyId,
                                                                @RequestBody(required = false) ProtocolGovernanceSubmitDTO dto,
                                                                Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议族定义回滚",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(approvalService.submitFamilyRollback(familyId, currentUserId, submitReasonOf(dto)));
    }

    @PostMapping("/decrypt-profiles/{profileId}/submit-publish")
    public R<GovernanceSubmissionResultVO> submitDecryptProfilePublish(@PathVariable Long profileId,
                                                                       @RequestBody(required = false) ProtocolGovernanceSubmitDTO dto,
                                                                       Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议解密档案发布",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(approvalService.submitDecryptProfilePublish(profileId, currentUserId, submitReasonOf(dto)));
    }

    @PostMapping("/decrypt-profiles/{profileId}/submit-rollback")
    public R<GovernanceSubmissionResultVO> submitDecryptProfileRollback(@PathVariable Long profileId,
                                                                        @RequestBody(required = false) ProtocolGovernanceSubmitDTO dto,
                                                                        Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议解密档案回滚",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(approvalService.submitDecryptProfileRollback(profileId, currentUserId, submitReasonOf(dto)));
    }

    @PostMapping("/decrypt-profiles/preview")
    public R<ProtocolDecryptPreviewVO> previewDecrypt(@RequestBody @Valid ProtocolDecryptPreviewDTO dto,
                                                      Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "协议解密试算",
                GovernancePermissionCodes.PROTOCOL_GOVERNANCE_EDIT
        );
        return R.ok(service.previewDecrypt(dto));
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
}
