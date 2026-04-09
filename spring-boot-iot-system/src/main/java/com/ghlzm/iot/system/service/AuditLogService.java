package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.vo.SystemErrorStatsVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
      List<AuditLog> listLogs(AuditLog log, Boolean excludeSystemError);
      List<AuditLog> listLogs(Long currentUserId, AuditLog log, Boolean excludeSystemError);

      /**
       * 分页查询审计日志
       */
      PageResult<AuditLog> pageLogs(AuditLog log, Boolean excludeSystemError, Integer pageNum, Integer pageSize);
      PageResult<AuditLog> pageLogs(Long currentUserId,
                                    AuditLog log,
                                    Boolean excludeSystemError,
                                    Integer pageNum,
                                    Integer pageSize);

      /**
       * 查询 system_error 统计概览。
       */
      SystemErrorStatsVO getSystemErrorStats(AuditLog log);
      SystemErrorStatsVO getSystemErrorStats(Long currentUserId, AuditLog log);

      /**
       * 统计指定时间之后的 system_error 数量。
       */
      Long countSystemErrorsSince(Date startTime);

      Map<String, Object> getBusinessAuditStats(AuditLog log);
      Map<String, Object> getBusinessAuditStats(Long currentUserId, AuditLog log);

      /**
       * 根据ID查询审计日志
       */
      AuditLog getById(Long id);
      AuditLog getById(Long currentUserId, Long id);

      /**
       * 删除审计日志
       */
      void deleteLog(Long id);
      void deleteLog(Long currentUserId, Long id);
}
