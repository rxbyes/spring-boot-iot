package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.service.EmergencyPlanService;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应急预案Controller
 */
@RestController
@RequestMapping("/api/emergency-plan")
public class EmergencyPlanController {

      private final EmergencyPlanService emergencyPlanService;
      private final GovernancePermissionGuard permissionGuard;

      public EmergencyPlanController(EmergencyPlanService emergencyPlanService,
                                     GovernancePermissionGuard permissionGuard) {
            this.emergencyPlanService = emergencyPlanService;
            this.permissionGuard = permissionGuard;
      }

      /**
       * 获取预案列表
       */
      @GetMapping("/list")
      public R<List<EmergencyPlan>> getPlanList(
                  @RequestParam(required = false) String planName,
                  @RequestParam(required = false) String alarmLevel,
                  @RequestParam(required = false, name = "riskLevel") String legacyRiskLevel,
                  @RequestParam(required = false) Integer status) {
            String normalizedAlarmLevel = StringUtils.hasText(alarmLevel) ? alarmLevel : legacyRiskLevel;
            List<EmergencyPlan> list = emergencyPlanService.getPlanList(planName, normalizedAlarmLevel, status);
            return R.ok(list);
      }

      /**
       * 分页获取预案列表
       */
      @GetMapping("/page")
      public R<PageResult<EmergencyPlan>> pagePlanList(
                  @RequestParam(required = false) String planName,
                  @RequestParam(required = false) String alarmLevel,
                  @RequestParam(required = false, name = "riskLevel") String legacyRiskLevel,
                  @RequestParam(required = false) Integer status,
                  @RequestParam(defaultValue = "1") Long pageNum,
                  @RequestParam(defaultValue = "10") Long pageSize) {
            String normalizedAlarmLevel = StringUtils.hasText(alarmLevel) ? alarmLevel : legacyRiskLevel;
            PageResult<EmergencyPlan> page = emergencyPlanService.pagePlanList(planName, normalizedAlarmLevel, status, pageNum, pageSize);
            return R.ok(page);
      }

      /**
       * 获取预案详情
       */
      @GetMapping("/get/{id}")
      public R<EmergencyPlan> getPlanById(@PathVariable Long id) {
            EmergencyPlan plan = emergencyPlanService.getById(id);
            return R.ok(plan);
      }

      /**
       * 新增预案
       */
      @PostMapping("/add")
      public R<EmergencyPlan> addPlan(@RequestBody EmergencyPlan plan, Authentication authentication) {
            permissionGuard.requireAnyPermission(
                    requireCurrentUserId(authentication),
                    "应急预案维护",
                    GovernancePermissionCodes.EMERGENCY_PLAN_WRITE
            );
            emergencyPlanService.addPlan(plan);
            return R.ok(plan);
      }

      /**
       * 更新预案
       */
      @PostMapping("/update")
      public R<EmergencyPlan> updatePlan(@RequestBody EmergencyPlan plan, Authentication authentication) {
            permissionGuard.requireAnyPermission(
                    requireCurrentUserId(authentication),
                    "应急预案维护",
                    GovernancePermissionCodes.EMERGENCY_PLAN_WRITE
            );
            emergencyPlanService.updatePlan(plan);
            return R.ok(plan);
      }

      /**
       * 删除预案
       */
      @PostMapping("/delete/{id}")
      public R<Void> deletePlan(@PathVariable Long id, Authentication authentication) {
            permissionGuard.requireAnyPermission(
                    requireCurrentUserId(authentication),
                    "应急预案维护",
                    GovernancePermissionCodes.EMERGENCY_PLAN_WRITE
            );
            emergencyPlanService.deletePlan(id);
            return R.ok();
      }

      private Long requireCurrentUserId(Authentication authentication) {
            if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
                  throw new BizException("未登录或登录状态已失效");
            }
            return principal.userId();
      }
}
