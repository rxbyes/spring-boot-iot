package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.AuditLogService;
import com.ghlzm.iot.system.vo.SystemErrorStatsVO;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计日志 Controller
 */
@RestController
@RequestMapping("/api/system/audit-log")
public class AuditLogController {

      @Autowired
      private AuditLogService auditLogService;

      @Autowired(required = false)
      private GovernancePermissionGuard permissionGuard;

      /**
       * 查询审计日志列表
       */
      @GetMapping("/list")
      public R<List<AuditLog>> listLogs(AuditLog log,
                  @RequestParam(defaultValue = "false") Boolean excludeSystemError,
                  Authentication authentication) {
            List<AuditLog> logs = auditLogService.listLogs(requireCurrentUserId(authentication), log, excludeSystemError);
            return R.ok(logs);
      }

      /**
       * 分页查询审计日志
       */
      @GetMapping("/page")
      public R<Map<String, Object>> pageLogs(AuditLog log,
                  @RequestParam(defaultValue = "false") Boolean excludeSystemError,
                  @RequestParam(defaultValue = "1") Integer pageNum,
                  @RequestParam(defaultValue = "10") Integer pageSize,
                  Authentication authentication) {
            PageResult<AuditLog> page = auditLogService.pageLogs(requireCurrentUserId(authentication), log, excludeSystemError, pageNum, pageSize);
            // 显式返回标准分页结构，避免历史序列化差异导致前端拿到数组而非对象
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("total", page.getTotal());
            payload.put("pageNum", page.getPageNum());
            payload.put("pageSize", page.getPageSize());
            payload.put("records", page.getRecords());
            return R.ok(payload);
      }

      /**
       * 查询 system_error 统计概览
       */
      @GetMapping("/system-error/stats")
      public R<SystemErrorStatsVO> getSystemErrorStats(AuditLog log, Authentication authentication) {
            return R.ok(auditLogService.getSystemErrorStats(requireCurrentUserId(authentication), log));
      }

      @GetMapping("/business/stats")
      public R<Map<String, Object>> getBusinessAuditStats(AuditLog log, Authentication authentication) {
            return R.ok(auditLogService.getBusinessAuditStats(requireCurrentUserId(authentication), log));
      }

      /**
       * 根据ID查询审计日志
       */
      @GetMapping("/get/{id}")
      public R<AuditLog> getById(@PathVariable Long id, Authentication authentication) {
            AuditLog log = auditLogService.getById(requireCurrentUserId(authentication), id);
            if (log == null) {
                  return R.fail(404, "审计日志不存在或已删除");
            }
            return R.ok(log);
      }

      /**
       * 添加审计日志
       */
      @PostMapping("/add")
      public R<Void> addLog(@RequestBody AuditLog log) {
            auditLogService.addLog(log);
            return R.ok();
      }

      /**
       * 删除审计日志
       */
      @DeleteMapping("/delete/{id}")
      public R<Void> deleteLog(@PathVariable Long id, Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            if (permissionGuard != null) {
                  permissionGuard.requireAnyPermission(currentUserId, "删除审计日志", GovernancePermissionCodes.AUDIT_DELETE);
            }
            auditLogService.deleteLog(currentUserId, id);
            return R.ok();
      }

      private Long requireCurrentUserId(Authentication authentication) {
            if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
                  throw new com.ghlzm.iot.common.exception.BizException(401, "未认证，请先登录");
            }
            return principal.userId();
      }
}
