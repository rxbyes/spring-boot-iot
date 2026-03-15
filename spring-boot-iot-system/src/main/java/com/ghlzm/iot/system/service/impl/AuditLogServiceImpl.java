package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
            LambdaQueryWrapper<AuditLog> queryWrapper = new LambdaQueryWrapper<>();
            if (log.getTenantId() != null) {
                  queryWrapper.eq(AuditLog::getTenantId, log.getTenantId());
            }
            if (log.getUserId() != null) {
                  queryWrapper.eq(AuditLog::getUserId, log.getUserId());
            }
            if (log.getOperationType() != null && !log.getOperationType().isEmpty()) {
                  queryWrapper.eq(AuditLog::getOperationType, log.getOperationType());
            }
            if (log.getOperationModule() != null && !log.getOperationModule().isEmpty()) {
                  queryWrapper.eq(AuditLog::getOperationModule, log.getOperationModule());
            }
            queryWrapper.orderByDesc(AuditLog::getOperationTime);
            return this.list(queryWrapper);
      }

      @Override
      public List<AuditLog> pageLogs(AuditLog log, Integer pageNum, Integer pageSize) {
            LambdaQueryWrapper<AuditLog> queryWrapper = new LambdaQueryWrapper<>();
            if (log.getTenantId() != null) {
                  queryWrapper.eq(AuditLog::getTenantId, log.getTenantId());
            }
            if (log.getUserId() != null) {
                  queryWrapper.eq(AuditLog::getUserId, log.getUserId());
            }
            if (log.getOperationType() != null && !log.getOperationType().isEmpty()) {
                  queryWrapper.eq(AuditLog::getOperationType, log.getOperationType());
            }
            if (log.getOperationModule() != null && !log.getOperationModule().isEmpty()) {
                  queryWrapper.eq(AuditLog::getOperationModule, log.getOperationModule());
            }
            queryWrapper.orderByDesc(AuditLog::getOperationTime);
            return this.list(queryWrapper);
      }

      @Override
      public AuditLog getById(Long id) {
            return this.getById(id);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteLog(Long id) {
            this.removeById(id);
      }
}
