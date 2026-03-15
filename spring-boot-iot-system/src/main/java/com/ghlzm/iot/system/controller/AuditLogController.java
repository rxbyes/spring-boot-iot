package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审计日志 Controller
 */
@RestController
@RequestMapping("/system/audit-log")
public class AuditLogController {

      @Autowired
      private AuditLogService auditLogService;

      /**
       * 查询审计日志列表
       */
      @GetMapping("/list")
      public R<List<AuditLog>> listLogs(AuditLog log) {
            List<AuditLog> logs = auditLogService.listLogs(log);
            return R.ok(logs);
      }

      /**
       * 分页查询审计日志
       */
      @GetMapping("/page")
      public R<List<AuditLog>> pageLogs(AuditLog log,
                  @RequestParam(defaultValue = "1") Integer pageNum,
                  @RequestParam(defaultValue = "10") Integer pageSize) {
            List<AuditLog> logs = auditLogService.pageLogs(log, pageNum, pageSize);
            return R.ok(logs);
      }

      /**
       * 根据ID查询审计日志
       */
      @GetMapping("/get/{id}")
      public R<AuditLog> getById(@PathVariable Long id) {
            AuditLog log = auditLogService.getById(id);
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
