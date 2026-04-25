package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.AlarmRecord;
import com.ghlzm.iot.alarm.service.AlarmRecordService;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警记录控制器
 */
@RestController
@RequestMapping("/api/alarm")
public class AlarmRecordController {

    private final AlarmRecordService alarmRecordService;
    private final GovernancePermissionGuard permissionGuard;

    public AlarmRecordController(AlarmRecordService alarmRecordService) {
        this(alarmRecordService, null);
    }

    @Autowired
    public AlarmRecordController(AlarmRecordService alarmRecordService, GovernancePermissionGuard permissionGuard) {
        this.alarmRecordService = alarmRecordService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * 新增告警记录
     */
    @PostMapping("/add")
    public R<AlarmRecord> add(@RequestBody AlarmRecord alarm) {
        return R.ok(alarmRecordService.addAlarm(alarm));
    }

    /**
     * 查询告警列表
     */
    @GetMapping("/list")
    public R<List<AlarmRecord>> list(
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String alarmLevel) {
        return R.ok(alarmRecordService.listAlarms(deviceCode, status, alarmLevel));
    }

    /**
     * 根据ID查询告警记录
     */
    @GetMapping("/{id}")
    public R<AlarmRecord> getById(@PathVariable Long id) {
        return R.ok(alarmRecordService.getRequiredById(id));
    }

    /**
     * 确认告警
     */
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id,
                           @RequestParam("confirmUser") Long confirmUser,
                           Authentication authentication) {
        requirePermission(authentication, confirmUser, "确认告警", GovernancePermissionCodes.ALARM_CONFIRM);
        alarmRecordService.confirmAlarm(id, confirmUser);
        return R.ok();
    }

    /**
     * 抑制告警
     */
    @PostMapping("/{id}/suppress")
    public R<Void> suppress(@PathVariable Long id,
                            @RequestParam("suppressUser") Long suppressUser,
                            Authentication authentication) {
        requirePermission(authentication, suppressUser, "抑制告警", GovernancePermissionCodes.ALARM_SUPPRESS);
        alarmRecordService.suppressAlarm(id, suppressUser);
        return R.ok();
    }

    /**
     * 关闭告警
     */
    @PostMapping("/{id}/close")
    public R<Void> close(@PathVariable Long id,
                         @RequestParam("closeUser") Long closeUser,
                         Authentication authentication) {
        requirePermission(authentication, closeUser, "关闭告警", GovernancePermissionCodes.ALARM_CLOSE);
        alarmRecordService.closeAlarm(id, closeUser);
        return R.ok();
    }

    private void requirePermission(Authentication authentication,
                                   Long fallbackUserId,
                                   String actionName,
                                   String permissionCode) {
        if (permissionGuard == null) {
            return;
        }
        Long currentUserId = fallbackUserId;
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserPrincipal principal) {
            currentUserId = principal.userId();
        }
        permissionGuard.requireAnyPermission(currentUserId, actionName, permissionCode);
    }
}
