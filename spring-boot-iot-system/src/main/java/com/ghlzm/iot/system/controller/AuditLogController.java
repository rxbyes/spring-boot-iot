package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.service.AuditLogService;
import com.ghlzm.iot.system.vo.SystemErrorStatsVO;
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

      /**
       * 查询审计日志列表
       */
      @GetMapping("/list")
      public R<List<AuditLog>> listLogs(AuditLog log,
                  @RequestParam(defaultValue = "false") Boolean excludeSystemError) {
            List<AuditLog> logs = auditLogService.listLogs(log, excludeSystemError);
            return R.ok(logs);
      }

      /**
       * 分页查询审计日志
       */
      @GetMapping("/page")
      public R<Map<String, Object>> pageLogs(AuditLog log,
                  @RequestParam(defaultValue = "false") Boolean excludeSystemError,
                  @RequestParam(defaultValue = "1") Integer pageNum,
                  @RequestParam(defaultValue = "10") Integer pageSize) {
            PageResult<AuditLog> page = auditLogService.pageLogs(log, excludeSystemError, pageNum, pageSize);
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
      public R<SystemErrorStatsVO> getSystemErrorStats(AuditLog log) {
            return R.ok(auditLogService.getSystemErrorStats(log));
      }

      @GetMapping("/business/stats")
      public R<Map<String, Object>> getBusinessAuditStats(AuditLog log) {
            return R.ok(auditLogService.getBusinessAuditStats(log));
      }

      /**
       * 根据ID查询审计日志
       */
      @GetMapping("/get/{id}")
      public R<AuditLog> getById(@PathVariable Long id) {
            AuditLog log = auditLogService.getById(id);
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
      public R<Void> deleteLog(@PathVariable Long id) {
            auditLogService.deleteLog(id);
            return R.ok();
      }
}
