package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.service.RuleDefinitionService;
import com.ghlzm.iot.alarm.vo.RuleDefinitionBatchAddResultVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.GovernanceApprovalPolicyResolver;
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
 * 阈值策略配置 Controller。
 */
@RestController
@RequestMapping("/api/rule-definition")
public class RuleDefinitionController {

    private static final String ACTION_RULE_DEFINITION_CREATE = "RULE_DEFINITION_CREATE";
    private static final String ACTION_RULE_DEFINITION_BATCH_CREATE = "RULE_DEFINITION_BATCH_CREATE";
    private static final String ACTION_RULE_DEFINITION_UPDATE = "RULE_DEFINITION_UPDATE";
    private static final String ACTION_RULE_DEFINITION_DELETE = "RULE_DEFINITION_DELETE";

    private final RuleDefinitionService ruleDefinitionService;
    private final GovernancePermissionGuard permissionGuard;
    private final GovernanceApprovalPolicyResolver governanceApprovalPolicyResolver;

    public RuleDefinitionController(RuleDefinitionService ruleDefinitionService,
                                    GovernancePermissionGuard permissionGuard,
                                    GovernanceApprovalPolicyResolver governanceApprovalPolicyResolver) {
        this.ruleDefinitionService = ruleDefinitionService;
        this.permissionGuard = permissionGuard;
        this.governanceApprovalPolicyResolver = governanceApprovalPolicyResolver;
    }

    @GetMapping("/list")
    public R<List<RuleDefinition>> getRuleList(@RequestParam(required = false) String ruleName,
                                               @RequestParam(required = false) String metricIdentifier,
                                               @RequestParam(required = false) String alarmLevel,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(required = false) String ruleScope,
                                               @RequestParam(required = false) Long productId,
                                               @RequestParam(required = false) String productType) {
        List<RuleDefinition> list = ruleDefinitionService.getRuleList(ruleName, metricIdentifier, alarmLevel, status,
                ruleScope, productId, productType);
        return R.ok(list);
    }

    @GetMapping("/page")
    public R<PageResult<RuleDefinition>> pageRuleList(@RequestParam(required = false) String ruleName,
                                                      @RequestParam(required = false) String metricIdentifier,
                                                      @RequestParam(required = false) String alarmLevel,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) String ruleScope,
                                                      @RequestParam(required = false) Long productId,
                                                      @RequestParam(required = false) String productType,
                                                      @RequestParam(defaultValue = "1") Long pageNum,
                                                      @RequestParam(defaultValue = "10") Long pageSize) {
        PageResult<RuleDefinition> page =
                ruleDefinitionService.pageRuleList(ruleName, metricIdentifier, alarmLevel, status, ruleScope,
                        productId, productType, pageNum, pageSize);
        return R.ok(page);
    }

    @GetMapping("/get/{id}")
    public R<RuleDefinition> getRuleById(@PathVariable Long id) {
        return R.ok(ruleDefinitionService.getById(id));
    }

    @PostMapping("/add")
    public R<RuleDefinition> addRule(@RequestBody RuleDefinition rule,
                                     @RequestHeader(value = "X-Governance-Approver-Id", required = false) Long approverUserId,
                                     Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        Long resolvedApproverUserId = resolveApproverUserId(ACTION_RULE_DEFINITION_CREATE, currentUserId, approverUserId);
        permissionGuard.requireDualControl(
                currentUserId,
                resolvedApproverUserId,
                "rule-definition-create",
                GovernancePermissionCodes.RULE_DEFINITION_EDIT,
                GovernancePermissionCodes.RULE_DEFINITION_APPROVE
        );
        ruleDefinitionService.addRule(rule);
        return R.ok(rule);
    }

    @PostMapping("/batch-add")
    public R<RuleDefinitionBatchAddResultVO> batchAddRules(@RequestBody List<RuleDefinition> rules,
                                                           @RequestHeader(value = "X-Governance-Approver-Id", required = false) Long approverUserId,
                                                           Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        Long resolvedApproverUserId =
                resolveApproverUserId(ACTION_RULE_DEFINITION_BATCH_CREATE, currentUserId, approverUserId);
        permissionGuard.requireDualControl(
                currentUserId,
                resolvedApproverUserId,
                "rule-definition-batch-create",
                GovernancePermissionCodes.RULE_DEFINITION_EDIT,
                GovernancePermissionCodes.RULE_DEFINITION_APPROVE
        );
        return R.ok(ruleDefinitionService.batchAddRules(rules));
    }

    @PostMapping("/update")
    public R<RuleDefinition> updateRule(@RequestBody RuleDefinition rule,
                                        @RequestHeader(value = "X-Governance-Approver-Id", required = false) Long approverUserId,
                                        Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        Long resolvedApproverUserId = resolveApproverUserId(ACTION_RULE_DEFINITION_UPDATE, currentUserId, approverUserId);
        permissionGuard.requireDualControl(
                currentUserId,
                resolvedApproverUserId,
                "rule-definition-update",
                GovernancePermissionCodes.RULE_DEFINITION_EDIT,
                GovernancePermissionCodes.RULE_DEFINITION_APPROVE
        );
        ruleDefinitionService.updateRule(rule);
        return R.ok(rule);
    }

    @PostMapping("/delete/{id}")
    public R<Void> deleteRule(@PathVariable Long id,
                              @RequestHeader(value = "X-Governance-Approver-Id", required = false) Long approverUserId,
                              Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        Long resolvedApproverUserId = resolveApproverUserId(ACTION_RULE_DEFINITION_DELETE, currentUserId, approverUserId);
        permissionGuard.requireDualControl(
                currentUserId,
                resolvedApproverUserId,
                "rule-definition-delete",
                GovernancePermissionCodes.RULE_DEFINITION_EDIT,
                GovernancePermissionCodes.RULE_DEFINITION_APPROVE
        );
        ruleDefinitionService.deleteRule(id);
        return R.ok();
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException("未登录或登录状态已失效");
        }
        return principal.userId();
    }

    private Long resolveApproverUserId(String actionCode, Long currentUserId, Long approverUserId) {
        if (approverUserId != null && approverUserId > 0) {
            return approverUserId;
        }
        return governanceApprovalPolicyResolver.resolveApproverUserId(actionCode, currentUserId);
    }
}
