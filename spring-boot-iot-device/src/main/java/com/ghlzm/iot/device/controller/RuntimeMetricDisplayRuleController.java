package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.RuntimeMetricDisplayRuleUpsertDTO;
import com.ghlzm.iot.device.service.RuntimeMetricDisplayRuleService;
import com.ghlzm.iot.device.vo.RuntimeMetricDisplayRuleVO;
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
 * 运行态字段显示规则控制器。
 */
@RestController
public class RuntimeMetricDisplayRuleController {

    private final RuntimeMetricDisplayRuleService service;
    private final GovernancePermissionGuard permissionGuard;

    public RuntimeMetricDisplayRuleController(RuntimeMetricDisplayRuleService service,
                                              GovernancePermissionGuard permissionGuard) {
        this.service = service;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/api/device/product/{productId}/runtime-display-rules")
    public R<PageResult<RuntimeMetricDisplayRuleVO>> pageRules(@PathVariable Long productId,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(required = false) Long pageNum,
                                                               @RequestParam(required = false) Long pageSize,
                                                               Authentication authentication) {
        requireGovernancePermission(authentication, "运行态字段显示规则查询");
        return R.ok(service.pageRules(productId, status, pageNum, pageSize));
    }

    @PostMapping("/api/device/product/{productId}/runtime-display-rules")
    public R<RuntimeMetricDisplayRuleVO> addRule(@PathVariable Long productId,
                                                 @RequestBody @Valid RuntimeMetricDisplayRuleUpsertDTO dto,
                                                 Authentication authentication) {
        Long currentUserId = requireGovernancePermission(authentication, "运行态字段显示规则维护");
        return R.ok(service.createAndGet(productId, currentUserId, dto));
    }

    @PutMapping("/api/device/product/{productId}/runtime-display-rules/{ruleId}")
    public R<RuntimeMetricDisplayRuleVO> updateRule(@PathVariable Long productId,
                                                    @PathVariable Long ruleId,
                                                    @RequestBody @Valid RuntimeMetricDisplayRuleUpsertDTO dto,
                                                    Authentication authentication) {
        Long currentUserId = requireGovernancePermission(authentication, "运行态字段显示规则维护");
        return R.ok(service.updateAndGet(productId, ruleId, currentUserId, dto));
    }

    private Long requireGovernancePermission(Authentication authentication, String actionName) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException("未登录或登录状态已失效");
        }
        permissionGuard.requireAnyPermission(
                principal.userId(),
                actionName,
                GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN
        );
        return principal.userId();
    }
}
