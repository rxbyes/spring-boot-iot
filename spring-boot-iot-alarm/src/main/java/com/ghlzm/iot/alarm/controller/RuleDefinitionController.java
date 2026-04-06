package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.service.RuleDefinitionService;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 阈值规则配置Controller
 */
@RestController
@RequestMapping("/api/rule-definition")
public class RuleDefinitionController {

      private final RuleDefinitionService ruleDefinitionService;
      private final GovernancePermissionGuard permissionGuard;

      public RuleDefinitionController(RuleDefinitionService ruleDefinitionService,
                                      GovernancePermissionGuard permissionGuard) {
            this.ruleDefinitionService = ruleDefinitionService;
            this.permissionGuard = permissionGuard;
      }

      /**
       * 获取规则列表
       */
      @GetMapping("/list")
      public R<List<RuleDefinition>> getRuleList(
                  @RequestParam(required = false) String ruleName,
                  @RequestParam(required = false) String metricIdentifier,
                  @RequestParam(required = false) String alarmLevel,
                  @RequestParam(required = false) Integer status) {
            List<RuleDefinition> list = ruleDefinitionService.getRuleList(ruleName, metricIdentifier, alarmLevel, status);
            return R.ok(list);
      }

      /**
       * 分页获取规则列表
       */
      @GetMapping("/page")
      public R<PageResult<RuleDefinition>> pageRuleList(
                  @RequestParam(required = false) String ruleName,
                  @RequestParam(required = false) String metricIdentifier,
                  @RequestParam(required = false) String alarmLevel,
                  @RequestParam(required = false) Integer status,
                  @RequestParam(defaultValue = "1") Long pageNum,
                  @RequestParam(defaultValue = "10") Long pageSize) {
            PageResult<RuleDefinition> page = ruleDefinitionService.pageRuleList(ruleName, metricIdentifier, alarmLevel, status, pageNum, pageSize);
            return R.ok(page);
      }

      /**
       * 获取规则详情
       */
      @GetMapping("/get/{id}")
      public R<RuleDefinition> getRuleById(@PathVariable Long id) {
            RuleDefinition rule = ruleDefinitionService.getById(id);
            return R.ok(rule);
      }

      /**
       * 新增规则
       */
      @PostMapping("/add")
      public R<RuleDefinition> addRule(@RequestBody RuleDefinition rule, Authentication authentication) {
            permissionGuard.requireAnyPermission(
                    requireCurrentUserId(authentication),
                    "阈值策略维护",
                    GovernancePermissionCodes.RULE_DEFINITION_WRITE
            );
            ruleDefinitionService.addRule(rule);
            return R.ok(rule);
      }

      /**
       * 更新规则
       */
      @PostMapping("/update")
      public R<RuleDefinition> updateRule(@RequestBody RuleDefinition rule, Authentication authentication) {
            permissionGuard.requireAnyPermission(
                    requireCurrentUserId(authentication),
                    "阈值策略维护",
                    GovernancePermissionCodes.RULE_DEFINITION_WRITE
            );
            ruleDefinitionService.updateRule(rule);
            return R.ok(rule);
      }

      /**
       * 删除规则
       */
      @PostMapping("/delete/{id}")
      public R<Void> deleteRule(@PathVariable Long id, Authentication authentication) {
            permissionGuard.requireAnyPermission(
                    requireCurrentUserId(authentication),
                    "阈值策略维护",
                    GovernancePermissionCodes.RULE_DEFINITION_WRITE
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
}
