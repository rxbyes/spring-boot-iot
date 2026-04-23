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

    public OnboardingTemplatePackController(OnboardingTemplatePackService service) {
        this.service = service;
    }

    @GetMapping
    public R<PageResult<OnboardingTemplatePackVO>> pagePacks(OnboardingTemplatePackPageQueryDTO query) {
        return R.ok(service.pagePacks(query));
    }

    @PostMapping
    public R<OnboardingTemplatePackVO> createPack(@RequestBody @Valid OnboardingTemplatePackCreateDTO dto,
                                                  Authentication authentication) {
        return R.ok(service.createPack(dto, requireCurrentUserId(authentication)));
    }

    @PutMapping("/{packId}")
    public R<OnboardingTemplatePackVO> updatePack(@PathVariable Long packId,
                                                  @RequestBody @Valid OnboardingTemplatePackUpdateDTO dto,
                                                  Authentication authentication) {
        return R.ok(service.updatePack(packId, dto, requireCurrentUserId(authentication)));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException("未登录或登录状态已失效");
        }
        return principal.userId();
    }
}
