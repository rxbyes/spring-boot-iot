package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseBatchCreateDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseBatchStartAcceptanceDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseBatchTemplateApplyDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseCreateDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseQueryDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseUpdateDTO;
import com.ghlzm.iot.device.service.DeviceOnboardingCaseService;
import com.ghlzm.iot.device.vo.DeviceOnboardingCaseBatchResultVO;
import com.ghlzm.iot.device.vo.DeviceOnboardingCaseVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 无代码接入案例控制器。
 */
@RestController
@RequestMapping("/api/device/onboarding/cases")
public class DeviceOnboardingCaseController {

    private final DeviceOnboardingCaseService service;
    private final GovernancePermissionGuard permissionGuard;

    public DeviceOnboardingCaseController(DeviceOnboardingCaseService service) {
        this(service, null);
    }

    @Autowired
    public DeviceOnboardingCaseController(DeviceOnboardingCaseService service,
                                          GovernancePermissionGuard permissionGuard) {
        this.service = service;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public R<PageResult<DeviceOnboardingCaseVO>> pageCases(DeviceOnboardingCaseQueryDTO query) {
        return R.ok(service.pageCases(query));
    }

    @PostMapping
    public R<DeviceOnboardingCaseVO> createCase(@RequestBody @Valid DeviceOnboardingCaseCreateDTO dto,
                                                Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(
                currentUserId,
                "无代码接入案例创建",
                GovernancePermissionCodes.DEVICE_ONBOARDING_CREATE_CASE
        );
        return R.ok(service.createCase(dto, currentUserId));
    }

    @PostMapping("/batch-create")
    public R<DeviceOnboardingCaseBatchResultVO> batchCreate(@RequestBody @Valid DeviceOnboardingCaseBatchCreateDTO dto,
                                                            Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(
                currentUserId,
                "无代码接入案例批量创建",
                GovernancePermissionCodes.DEVICE_ONBOARDING_BATCH_CREATE,
                GovernancePermissionCodes.DEVICE_ONBOARDING_CREATE_CASE
        );
        return R.ok(service.batchCreateCases(dto, currentUserId));
    }

    @GetMapping("/{caseId}")
    public R<DeviceOnboardingCaseVO> getCase(@PathVariable Long caseId) {
        return R.ok(service.getCase(caseId));
    }

    @PutMapping("/{caseId}")
    public R<DeviceOnboardingCaseVO> updateCase(@PathVariable Long caseId,
                                                @RequestBody @Valid DeviceOnboardingCaseUpdateDTO dto,
                                                Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(
                currentUserId,
                "无代码接入案例编辑",
                GovernancePermissionCodes.DEVICE_ONBOARDING_UPDATE_CASE
        );
        return R.ok(service.updateCase(caseId, dto, currentUserId));
    }

    @PostMapping("/batch-apply-template")
    public R<DeviceOnboardingCaseBatchResultVO> batchApplyTemplate(
            @RequestBody @Valid DeviceOnboardingCaseBatchTemplateApplyDTO dto,
            Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(
                currentUserId,
                "无代码接入案例批量套用模板",
                GovernancePermissionCodes.DEVICE_ONBOARDING_BATCH_APPLY_TEMPLATE,
                GovernancePermissionCodes.DEVICE_ONBOARDING_TEMPLATE_PACK
        );
        return R.ok(service.batchApplyTemplatePack(dto, currentUserId));
    }

    @PostMapping("/{caseId}/start-acceptance")
    public R<DeviceOnboardingCaseVO> startAcceptance(@PathVariable Long caseId, Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(
                currentUserId,
                "无代码接入案例触发验收",
                GovernancePermissionCodes.DEVICE_ONBOARDING_START_ACCEPTANCE
        );
        return R.ok(service.startAcceptance(caseId, currentUserId));
    }

    @PostMapping("/batch-start-acceptance")
    public R<DeviceOnboardingCaseBatchResultVO> batchStartAcceptance(
            @RequestBody @Valid DeviceOnboardingCaseBatchStartAcceptanceDTO dto,
            Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(
                currentUserId,
                "无代码接入案例批量触发验收",
                GovernancePermissionCodes.DEVICE_ONBOARDING_START_ACCEPTANCE
        );
        return R.ok(service.batchStartAcceptance(dto, currentUserId));
    }

    @PostMapping("/{caseId}/refresh-status")
    public R<DeviceOnboardingCaseVO> refreshStatus(@PathVariable Long caseId, Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(
                currentUserId,
                "无代码接入案例刷新状态",
                GovernancePermissionCodes.DEVICE_ONBOARDING_REFRESH_STATUS
        );
        return R.ok(service.refreshStatus(caseId, currentUserId));
    }

    private void requirePermission(Long currentUserId, String actionName, String... permissionCodes) {
        if (permissionGuard != null) {
            permissionGuard.requireAnyPermission(currentUserId, actionName, permissionCodes);
        }
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException("未登录或登录状态已失效");
        }
        return principal.userId();
    }
}
