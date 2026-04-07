package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.service.EmergencyPlanService;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 应急预案 Controller。
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

    @GetMapping("/list")
    public R<List<EmergencyPlan>> getPlanList(@RequestParam(required = false) String planName,
                                              @RequestParam(required = false) String alarmLevel,
                                              @RequestParam(required = false, name = "riskLevel") String legacyRiskLevel,
                                              @RequestParam(required = false) Integer status) {
        String normalizedAlarmLevel = StringUtils.hasText(alarmLevel) ? alarmLevel : legacyRiskLevel;
        return R.ok(emergencyPlanService.getPlanList(planName, normalizedAlarmLevel, status));
    }

    @GetMapping("/page")
    public R<PageResult<EmergencyPlan>> pagePlanList(@RequestParam(required = false) String planName,
                                                      @RequestParam(required = false) String alarmLevel,
                                                      @RequestParam(required = false, name = "riskLevel") String legacyRiskLevel,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(defaultValue = "1") Long pageNum,
                                                      @RequestParam(defaultValue = "10") Long pageSize) {
        String normalizedAlarmLevel = StringUtils.hasText(alarmLevel) ? alarmLevel : legacyRiskLevel;
        return R.ok(emergencyPlanService.pagePlanList(planName, normalizedAlarmLevel, status, pageNum, pageSize));
    }

    @GetMapping("/get/{id}")
    public R<EmergencyPlan> getPlanById(@PathVariable Long id) {
        return R.ok(emergencyPlanService.getById(id));
    }

    @PostMapping("/add")
    public R<EmergencyPlan> addPlan(@RequestBody EmergencyPlan plan,
                                    @RequestHeader("X-Governance-Approver-Id") Long approverUserId,
                                    Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireDualControl(
                currentUserId,
                approverUserId,
                "emergency-plan-create",
                GovernancePermissionCodes.EMERGENCY_PLAN_EDIT,
                GovernancePermissionCodes.EMERGENCY_PLAN_APPROVE
        );
        emergencyPlanService.addPlan(plan);
        return R.ok(plan);
    }

    @PostMapping("/update")
    public R<EmergencyPlan> updatePlan(@RequestBody EmergencyPlan plan,
                                       @RequestHeader("X-Governance-Approver-Id") Long approverUserId,
                                       Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireDualControl(
                currentUserId,
                approverUserId,
                "emergency-plan-update",
                GovernancePermissionCodes.EMERGENCY_PLAN_EDIT,
                GovernancePermissionCodes.EMERGENCY_PLAN_APPROVE
        );
        emergencyPlanService.updatePlan(plan);
        return R.ok(plan);
    }

    @PostMapping("/delete/{id}")
    public R<Void> deletePlan(@PathVariable Long id,
                              @RequestHeader("X-Governance-Approver-Id") Long approverUserId,
                              Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        permissionGuard.requireDualControl(
                currentUserId,
                approverUserId,
                "emergency-plan-delete",
                GovernancePermissionCodes.EMERGENCY_PLAN_EDIT,
                GovernancePermissionCodes.EMERGENCY_PLAN_APPROVE
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
