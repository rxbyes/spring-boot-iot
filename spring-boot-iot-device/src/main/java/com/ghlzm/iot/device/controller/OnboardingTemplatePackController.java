package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.OnboardingTemplatePackCreateDTO;
import com.ghlzm.iot.device.dto.OnboardingTemplatePackPageQueryDTO;
import com.ghlzm.iot.device.dto.OnboardingTemplatePackUpdateDTO;
import com.ghlzm.iot.device.service.OnboardingTemplatePackService;
import com.ghlzm.iot.device.vo.OnboardingTemplatePackVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
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
 * 无代码接入模板包控制器。
 */
@RestController
@RequestMapping("/api/device/onboarding/template-packs")
public class OnboardingTemplatePackController {

    private final OnboardingTemplatePackService service;
    private final GovernancePermissionGuard permissionGuard;

    public OnboardingTemplatePackController(OnboardingTemplatePackService service,
                                            GovernancePermissionGuard permissionGuard) {
        this.service = service;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public R<PageResult<OnboardingTemplatePackVO>> pagePacks(OnboardingTemplatePackPageQueryDTO query) {
        return R.ok(service.pagePacks(query));
    }

    @PostMapping
    public R<OnboardingTemplatePackVO> createPack(@RequestBody @Valid OnboardingTemplatePackCreateDTO dto,
                                                  Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "无代码接入模板包创建",
                GovernancePermissionCodes.DEVICE_ONBOARDING_TEMPLATE_PACK
        );
        return R.ok(service.createPack(dto, currentUserId));
    }

    @PutMapping("/{packId}")
    public R<OnboardingTemplatePackVO> updatePack(@PathVariable Long packId,
                                                  @RequestBody @Valid OnboardingTemplatePackUpdateDTO dto,
                                                  Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "无代码接入模板包编辑",
                GovernancePermissionCodes.DEVICE_ONBOARDING_TEMPLATE_PACK
        );
        return R.ok(service.updatePack(packId, dto, currentUserId));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException("未登录或登录状态已失效");
        }
        return principal.userId();
    }
}
