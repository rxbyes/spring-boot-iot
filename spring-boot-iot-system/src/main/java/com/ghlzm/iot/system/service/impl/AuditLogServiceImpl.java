package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.mapper.AuditLogMapper;
import com.ghlzm.iot.system.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 审计日志 Service 实现类
 */
@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog>
            implements AuditLogService {

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void addLog(AuditLog log) {
            this.save(log);
      }

      @Override
      public List<AuditLog> listLogs(AuditLog log) {
            return this.list(buildQueryWrapper(log));
      }

      @Override
      public PageResult<AuditLog> pageLogs(AuditLog log, Integer pageNum, Integer pageSize) {
            long safePageNum = pageNum == null || pageNum < 1 ? 1L : pageNum;
            long safePageSize = pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize, 100);
            List<AuditLog> allRecords = this.list(buildQueryWrapper(log));
            if (allRecords.isEmpty()) {
                  return PageResult.empty(safePageNum, safePageSize);
            }
            int fromIndex = (int) Math.min((safePageNum - 1) * safePageSize, allRecords.size());
            int toIndex = (int) Math.min(fromIndex + safePageSize, allRecords.size());
            return PageResult.of((long) allRecords.size(), safePageNum, safePageSize, allRecords.subList(fromIndex, toIndex));
      }

      @Override
      public AuditLog getById(Long id) {
            return super.getById(id);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteLog(Long id) {
            this.removeById(id);
      }

      private LambdaQueryWrapper<AuditLog> buildQueryWrapper(AuditLog log) {
            LambdaQueryWrapper<AuditLog> queryWrapper = new LambdaQueryWrapper<>();
            if (log == null) {
                  queryWrapper.orderByDesc(AuditLog::getOperationTime);
                  return queryWrapper;
            }
            if (log.getTenantId() != null) {
                  queryWrapper.eq(AuditLog::getTenantId, log.getTenantId());
            }
            if (log.getUserId() != null) {
                  queryWrapper.eq(AuditLog::getUserId, log.getUserId());
            }
            if (log.getUserName() != null && !log.getUserName().isEmpty()) {
                  queryWrapper.like(AuditLog::getUserName, log.getUserName());
            }
            if (log.getOperationType() != null && !log.getOperationType().isEmpty()) {
                  queryWrapper.eq(AuditLog::getOperationType, log.getOperationType());
            }
            if (log.getOperationModule() != null && !log.getOperationModule().isEmpty()) {
                  queryWrapper.like(AuditLog::getOperationModule, log.getOperationModule());
            }
            queryWrapper.orderByDesc(AuditLog::getOperationTime);
            return queryWrapper;
      }
}
