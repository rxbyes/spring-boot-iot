package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.mapper.AuditLogMapper;
import com.ghlzm.iot.system.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog>
        implements AuditLogService {

    private static final String SYSTEM_ERROR_TYPE = "system_error";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addLog(AuditLog log) {
        this.save(log);
    }

    @Override
    public List<AuditLog> listLogs(AuditLog log, Boolean excludeSystemError) {
        return this.list(buildQueryWrapper(log, excludeSystemError));
    }

    @Override
    public PageResult<AuditLog> pageLogs(AuditLog log, Boolean excludeSystemError, Integer pageNum, Integer pageSize) {
        Page<AuditLog> page = PageQueryUtils.buildPage(pageNum, pageSize);
        Page<AuditLog> result = this.page(page, buildQueryWrapper(log, excludeSystemError));
        return PageQueryUtils.toPageResult(result);
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

    private LambdaQueryWrapper<AuditLog> buildQueryWrapper(AuditLog log, Boolean excludeSystemError) {
        LambdaQueryWrapper<AuditLog> queryWrapper = new LambdaQueryWrapper<>();
        if (log == null) {
            if (Boolean.TRUE.equals(excludeSystemError)) {
                queryWrapper.ne(AuditLog::getOperationType, SYSTEM_ERROR_TYPE);
            }
            queryWrapper.orderByDesc(AuditLog::getOperationTime)
                    .orderByDesc(AuditLog::getCreateTime);
            return queryWrapper;
        }

        if (log.getTenantId() != null) {
            queryWrapper.eq(AuditLog::getTenantId, log.getTenantId());
        }
        if (log.getUserId() != null) {
            queryWrapper.eq(AuditLog::getUserId, log.getUserId());
        }
        if (StringUtils.hasText(log.getTraceId())) {
            queryWrapper.eq(AuditLog::getTraceId, log.getTraceId().trim());
        }
        if (StringUtils.hasText(log.getDeviceCode())) {
            queryWrapper.like(AuditLog::getDeviceCode, log.getDeviceCode().trim());
        }
        if (StringUtils.hasText(log.getProductKey())) {
            queryWrapper.like(AuditLog::getProductKey, log.getProductKey().trim());
        }
        if (StringUtils.hasText(log.getUserName())) {
            queryWrapper.like(AuditLog::getUserName, log.getUserName().trim());
        }
        if (StringUtils.hasText(log.getOperationType())) {
            queryWrapper.eq(AuditLog::getOperationType, log.getOperationType().trim());
        }
        if (StringUtils.hasText(log.getOperationModule())) {
            queryWrapper.like(AuditLog::getOperationModule, log.getOperationModule().trim());
        }
        if (StringUtils.hasText(log.getRequestMethod())) {
            queryWrapper.eq(AuditLog::getRequestMethod, log.getRequestMethod().trim());
        }
        if (StringUtils.hasText(log.getRequestUrl())) {
            queryWrapper.like(AuditLog::getRequestUrl, log.getRequestUrl().trim());
        }
        if (StringUtils.hasText(log.getResultMessage())) {
            queryWrapper.like(AuditLog::getResultMessage, log.getResultMessage().trim());
        }
        if (StringUtils.hasText(log.getErrorCode())) {
            queryWrapper.eq(AuditLog::getErrorCode, log.getErrorCode().trim());
        }
        if (StringUtils.hasText(log.getExceptionClass())) {
            queryWrapper.like(AuditLog::getExceptionClass, log.getExceptionClass().trim());
        }
        if (log.getOperationResult() != null) {
            queryWrapper.eq(AuditLog::getOperationResult, log.getOperationResult());
        }
        if (Boolean.TRUE.equals(excludeSystemError)) {
            queryWrapper.ne(AuditLog::getOperationType, SYSTEM_ERROR_TYPE);
        }

        queryWrapper.orderByDesc(AuditLog::getOperationTime)
                .orderByDesc(AuditLog::getCreateTime);
        return queryWrapper;
    }
}
