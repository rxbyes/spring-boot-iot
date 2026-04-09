package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleUpsertDTO;
import com.ghlzm.iot.device.service.VendorMetricMappingRuleService;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 厂商字段映射规则控制器。
 */
@RestController
public class VendorMetricMappingRuleController {

    private final VendorMetricMappingRuleService service;
    private final GovernancePermissionGuard permissionGuard;

    public VendorMetricMappingRuleController(VendorMetricMappingRuleService service,
                                             GovernancePermissionGuard permissionGuard) {
        this.service = service;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/api/device/product/{productId}/vendor-mapping-rules")
    public R<PageResult<VendorMetricMappingRuleVO>> pageRules(@PathVariable Long productId,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(required = false) Long pageNum,
                                                              @RequestParam(required = false) Long pageSize) {
        return R.ok(service.pageRules(productId, status, pageNum, pageSize));
    }

    @PostMapping("/api/device/product/{productId}/vendor-mapping-rules")
    public R<VendorMetricMappingRuleVO> addRule(@PathVariable Long productId,
                                                @RequestBody @Valid VendorMetricMappingRuleUpsertDTO dto,
                                                Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "厂商字段映射规则维护",
                GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN
        );
        return R.ok(service.createAndGet(productId, currentUserId, dto));
    }

    @PutMapping("/api/device/product/{productId}/vendor-mapping-rules/{ruleId}")
    public R<VendorMetricMappingRuleVO> updateRule(@PathVariable Long productId,
                                                   @PathVariable Long ruleId,
                                                   @RequestBody @Valid VendorMetricMappingRuleUpsertDTO dto,
                                                   Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireAnyPermission(
                currentUserId,
                "厂商字段映射规则维护",
                GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN
        );
        return R.ok(service.updateAndGet(productId, ruleId, currentUserId, dto));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException("未登录或登录状态已失效");
        }
        return principal.userId();
    }
}
