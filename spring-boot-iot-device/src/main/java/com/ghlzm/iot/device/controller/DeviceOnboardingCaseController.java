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
import jakarta.validation.Valid;
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

    public DeviceOnboardingCaseController(DeviceOnboardingCaseService service) {
        this.service = service;
    }

    @GetMapping
    public R<PageResult<DeviceOnboardingCaseVO>> pageCases(DeviceOnboardingCaseQueryDTO query) {
        return R.ok(service.pageCases(query));
    }

    @PostMapping
    public R<DeviceOnboardingCaseVO> createCase(@RequestBody @Valid DeviceOnboardingCaseCreateDTO dto,
                                                Authentication authentication) {
        return R.ok(service.createCase(dto, requireCurrentUserId(authentication)));
    }

    @PostMapping("/batch-create")
    public R<DeviceOnboardingCaseBatchResultVO> batchCreate(@RequestBody @Valid DeviceOnboardingCaseBatchCreateDTO dto,
                                                            Authentication authentication) {
        return R.ok(service.batchCreateCases(dto, requireCurrentUserId(authentication)));
    }

    @GetMapping("/{caseId}")
    public R<DeviceOnboardingCaseVO> getCase(@PathVariable Long caseId) {
        return R.ok(service.getCase(caseId));
    }

    @PutMapping("/{caseId}")
    public R<DeviceOnboardingCaseVO> updateCase(@PathVariable Long caseId,
                                                @RequestBody @Valid DeviceOnboardingCaseUpdateDTO dto,
                                                Authentication authentication) {
        return R.ok(service.updateCase(caseId, dto, requireCurrentUserId(authentication)));
    }

    @PostMapping("/batch-apply-template")
    public R<DeviceOnboardingCaseBatchResultVO> batchApplyTemplate(
            @RequestBody @Valid DeviceOnboardingCaseBatchTemplateApplyDTO dto,
            Authentication authentication) {
        return R.ok(service.batchApplyTemplatePack(dto, requireCurrentUserId(authentication)));
    }

    @PostMapping("/{caseId}/start-acceptance")
    public R<DeviceOnboardingCaseVO> startAcceptance(@PathVariable Long caseId, Authentication authentication) {
        return R.ok(service.startAcceptance(caseId, requireCurrentUserId(authentication)));
    }

    @PostMapping("/batch-start-acceptance")
    public R<DeviceOnboardingCaseBatchResultVO> batchStartAcceptance(
            @RequestBody @Valid DeviceOnboardingCaseBatchStartAcceptanceDTO dto,
            Authentication authentication) {
        return R.ok(service.batchStartAcceptance(dto, requireCurrentUserId(authentication)));
    }

    @PostMapping("/{caseId}/refresh-status")
    public R<DeviceOnboardingCaseVO> refreshStatus(@PathVariable Long caseId, Authentication authentication) {
        return R.ok(service.refreshStatus(caseId, requireCurrentUserId(authentication)));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException("未登录或登录状态已失效");
        }
        return principal.userId();
    }
}
