package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.LinkageRule;
import com.ghlzm.iot.alarm.service.LinkageRuleService;
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
 * 联动规则Controller
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

      /**
       * 获取规则列表
       */
      @GetMapping("/list")
      public R<List<LinkageRule>> getRuleList(
                  @RequestParam(required = false) String ruleName,
                  @RequestParam(required = false) Integer status) {
            List<LinkageRule> list = linkageRuleService.getRuleList(ruleName, status);
            return R.ok(list);
      }

      /**
       * 分页获取规则列表
       */
      @GetMapping("/page")
      public R<PageResult<LinkageRule>> pageRuleList(
                  @RequestParam(required = false) String ruleName,
                  @RequestParam(required = false) Integer status,
                  @RequestParam(defaultValue = "1") Long pageNum,
                  @RequestParam(defaultValue = "10") Long pageSize) {
            PageResult<LinkageRule> page = linkageRuleService.pageRuleList(ruleName, status, pageNum, pageSize);
            return R.ok(page);
      }

      /**
       * 获取规则详情
       */
      @GetMapping("/get/{id}")
      public R<LinkageRule> getRuleById(@PathVariable Long id) {
            LinkageRule rule = linkageRuleService.getById(id);
            return R.ok(rule);
      }

      /**
       * 新增规则
       */
      @PostMapping("/add")
      public R<LinkageRule> addRule(@RequestBody LinkageRule rule, Authentication authentication) {
            permissionGuard.requireAnyPermission(
                    requireCurrentUserId(authentication),
                    "联动编排维护",
                    GovernancePermissionCodes.LINKAGE_RULE_WRITE
            );
            linkageRuleService.addRule(rule);
            return R.ok(rule);
      }

      /**
       * 更新规则
       */
      @PostMapping("/update")
      public R<LinkageRule> updateRule(@RequestBody LinkageRule rule, Authentication authentication) {
            permissionGuard.requireAnyPermission(
                    requireCurrentUserId(authentication),
                    "联动编排维护",
                    GovernancePermissionCodes.LINKAGE_RULE_WRITE
            );
            linkageRuleService.updateRule(rule);
            return R.ok(rule);
      }

      /**
       * 删除规则
       */
      @PostMapping("/delete/{id}")
      public R<Void> deleteRule(@PathVariable Long id, Authentication authentication) {
            permissionGuard.requireAnyPermission(
                    requireCurrentUserId(authentication),
                    "联动编排维护",
                    GovernancePermissionCodes.LINKAGE_RULE_WRITE
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
