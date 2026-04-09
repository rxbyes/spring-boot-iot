package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.LinkageRule;
import com.ghlzm.iot.alarm.service.LinkageRuleService;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 联动规则 Controller。
 */
@RestController
@RequestMapping("/api/linkage-rule")
public class LinkageRuleController {

    private final LinkageRuleService linkageRuleService;
    private final GovernancePermissionGuard permissionGuard;

    public LinkageRuleController(LinkageRuleService linkageRuleService,
                                 GovernancePermissionGuard permissionGuard) {
        this.linkageRuleService = linkageRuleService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/list")
    public R<List<LinkageRule>> getRuleList(@RequestParam(required = false) String ruleName,
                                            @RequestParam(required = false) Integer status) {
        return R.ok(linkageRuleService.getRuleList(ruleName, status));
    }

    @GetMapping("/page")
    public R<PageResult<LinkageRule>> pageRuleList(@RequestParam(required = false) String ruleName,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(defaultValue = "1") Long pageNum,
                                                   @RequestParam(defaultValue = "10") Long pageSize) {
        return R.ok(linkageRuleService.pageRuleList(ruleName, status, pageNum, pageSize));
    }

    @GetMapping("/get/{id}")
    public R<LinkageRule> getRuleById(@PathVariable Long id) {
        return R.ok(linkageRuleService.getById(id));
    }

    @PostMapping("/add")
    public R<LinkageRule> addRule(@RequestBody LinkageRule rule,
                                  @RequestHeader("X-Governance-Approver-Id") Long approverUserId,
                                  Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireDualControl(
                currentUserId,
                approverUserId,
                "linkage-rule-create",
                GovernancePermissionCodes.LINKAGE_RULE_EDIT,
                GovernancePermissionCodes.LINKAGE_RULE_APPROVE
        );
        linkageRuleService.addRule(rule);
        return R.ok(rule);
    }

    @PostMapping("/update")
    public R<LinkageRule> updateRule(@RequestBody LinkageRule rule,
                                     @RequestHeader("X-Governance-Approver-Id") Long approverUserId,
                                     Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireDualControl(
                currentUserId,
                approverUserId,
                "linkage-rule-update",
                GovernancePermissionCodes.LINKAGE_RULE_EDIT,
                GovernancePermissionCodes.LINKAGE_RULE_APPROVE
        );
        linkageRuleService.updateRule(rule);
        return R.ok(rule);
    }

    @PostMapping("/delete/{id}")
    public R<Void> deleteRule(@PathVariable Long id,
                              @RequestHeader("X-Governance-Approver-Id") Long approverUserId,
                              Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireDualControl(
                currentUserId,
                approverUserId,
                "linkage-rule-delete",
                GovernancePermissionCodes.LINKAGE_RULE_EDIT,
                GovernancePermissionCodes.LINKAGE_RULE_APPROVE
        );
        linkageRuleService.deleteRule(id);
        return R.ok();
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException("未登录或登录状态已失效");
        }
        return principal.userId();
    }
}
