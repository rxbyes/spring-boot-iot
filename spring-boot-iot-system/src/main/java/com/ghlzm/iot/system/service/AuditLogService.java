package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.system.entity.AuditLog;

import java.util.List;

/**
 * 审计日志 Service
 */
public interface AuditLogService extends IService<AuditLog> {

      /**
       * 添加审计日志
       */
      void addLog(AuditLog log);

      /**
       * 查询审计日志列表
       */
      List<AuditLog> listLogs(AuditLog log);

      /**
       * 分页查询审计日志
       */
      List<AuditLog> pageLogs(AuditLog log, Integer pageNum, Integer pageSize);

      /**
       * 根据ID查询审计日志
       */
      AuditLog getById(Long id);

      /**
       * 删除审计日志
       */
      void deleteLog(Long id);
}
