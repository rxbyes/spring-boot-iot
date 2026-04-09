package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.GovernanceOpsAlert;
import com.ghlzm.iot.system.mapper.GovernanceOpsAlertMapper;
import com.ghlzm.iot.system.service.GovernanceOpsAlertService;
import com.ghlzm.iot.system.service.model.GovernanceOpsAlertCommand;
import com.ghlzm.iot.system.service.model.GovernanceOpsAlertPageQuery;
import com.ghlzm.iot.system.vo.GovernanceOpsAlertVO;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceOpsAlertServiceImpl implements GovernanceOpsAlertService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_ACKED = "ACKED";
    private static final String STATUS_SUPPRESSED = "SUPPRESSED";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String DEFAULT_SEVERITY = "WARN";

    private final GovernanceOpsAlertMapper alertMapper;

    public GovernanceOpsAlertServiceImpl(GovernanceOpsAlertMapper alertMapper) {
        this.alertMapper = alertMapper;
    }

    @Override
    public void raiseOrRefresh(GovernanceOpsAlertCommand command) {
        GovernanceOpsAlertCommand normalized = requireCommand(command);
        GovernanceOpsAlert existing = alertMapper.selectOne(new LambdaQueryWrapper<GovernanceOpsAlert>()
                .eq(GovernanceOpsAlert::getDeleted, 0)
                .eq(GovernanceOpsAlert::getAlertType, normalized.alertType())
                .eq(GovernanceOpsAlert::getAlertCode, normalized.alertCode())
                .last("limit 1"));
        Date now = new Date();
        if (existing == null) {
            GovernanceOpsAlert alert = new GovernanceOpsAlert();
            alert.setAlertType(normalized.alertType());
            alert.setAlertCode(normalized.alertCode());
            alert.setSubjectType(normalized.subjectType());
            alert.setSubjectId(normalized.subjectId());
            alert.setProductId(normalized.productId());
            alert.setRiskMetricId(normalized.riskMetricId());
            alert.setReleaseBatchId(normalized.releaseBatchId());
            alert.setTraceId(normalize(normalized.traceId()));
            alert.setDeviceCode(normalize(normalized.deviceCode()));
            alert.setProductKey(normalize(normalized.productKey()));
            alert.setAlertStatus(STATUS_OPEN);
            alert.setSeverityLevel(defaultSeverity(normalized.severityLevel()));
            alert.setAffectedCount(defaultAffectedCount(normalized.affectedCount()));
            alert.setAlertTitle(normalize(normalized.alertTitle()));
            alert.setAlertMessage(normalize(normalized.alertMessage()));
            alert.setDimensionKey(normalize(normalized.dimensionKey()));
            alert.setDimensionLabel(normalize(normalized.dimensionLabel()));
            alert.setSourceStage(normalize(normalized.sourceStage()));
            alert.setSnapshotJson(normalize(normalized.snapshotJson()));
            alert.setFirstSeenTime(now);
            alert.setLastSeenTime(now);
            alert.setCreateBy(normalized.operatorUserId());
            alert.setUpdateBy(normalized.operatorUserId());
            alert.setDeleted(0);
            alertMapper.insert(alert);
            return;
        }

        GovernanceOpsAlert refreshed = new GovernanceOpsAlert();
        refreshed.setId(existing.getId());
        refreshed.setSubjectType(normalized.subjectType());
        refreshed.setSubjectId(normalized.subjectId());
        refreshed.setProductId(normalized.productId());
        refreshed.setRiskMetricId(normalized.riskMetricId());
        refreshed.setReleaseBatchId(normalized.releaseBatchId());
        refreshed.setTraceId(normalize(normalized.traceId()));
        refreshed.setDeviceCode(normalize(normalized.deviceCode()));
        refreshed.setProductKey(normalize(normalized.productKey()));
        refreshed.setAlertStatus(resolveAlertStatus(existing));
        refreshed.setSeverityLevel(defaultSeverity(normalized.severityLevel()));
        refreshed.setAffectedCount(defaultAffectedCount(normalized.affectedCount()));
        refreshed.setAlertTitle(normalize(normalized.alertTitle()));
        refreshed.setAlertMessage(resolveAlertMessage(existing, normalized));
        refreshed.setDimensionKey(normalize(normalized.dimensionKey()));
        refreshed.setDimensionLabel(normalize(normalized.dimensionLabel()));
        refreshed.setSourceStage(normalize(normalized.sourceStage()));
        refreshed.setSnapshotJson(normalize(normalized.snapshotJson()));
        refreshed.setLastSeenTime(now);
        refreshed.setResolvedTime(shouldReopen(existing) ? null : existing.getResolvedTime());
        refreshed.setClosedTime(shouldReopen(existing) ? null : existing.getClosedTime());
        refreshed.setUpdateBy(normalized.operatorUserId());
        if (!shouldReopen(existing) && existing.getAssigneeUserId() != null) {
            refreshed.setAssigneeUserId(existing.getAssigneeUserId());
        }
        alertMapper.updateById(refreshed);
    }

    @Override
    public void recover(String alertType, String alertCode, Long operatorUserId, String comment) {
        GovernanceOpsAlert alert = requireByNaturalKey(alertType, alertCode);
        GovernanceOpsAlert update = new GovernanceOpsAlert();
        update.setId(alert.getId());
        update.setAlertStatus(STATUS_RESOLVED);
        update.setResolvedTime(new Date());
        update.setAlertMessage(normalize(comment));
        update.setUpdateBy(operatorUserId);
        alertMapper.updateById(update);
    }

    @Override
    public PageResult<GovernanceOpsAlertVO> pageAlerts(GovernanceOpsAlertPageQuery query, Long currentUserId) {
        Page<GovernanceOpsAlert> page = PageQueryUtils.buildPage(query == null ? null : query.getPageNum(), query == null ? null : query.getPageSize());
        Page<GovernanceOpsAlert> result = alertMapper.selectPage(page, buildPageWrapper(query));
        List<GovernanceOpsAlertVO> rows = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), rows);
    }

    @Override
    public void ack(Long alertId, Long currentUserId, String comment) {
        GovernanceOpsAlert alert = requireById(alertId);
        GovernanceOpsAlert update = new GovernanceOpsAlert();
        update.setId(alert.getId());
        update.setAlertStatus(STATUS_ACKED);
        update.setAssigneeUserId(currentUserId);
        update.setAlertMessage(resolveManualComment(alert.getAlertMessage(), comment));
        update.setUpdateBy(currentUserId);
        alertMapper.updateById(update);
    }

    @Override
    public void suppress(Long alertId, Long currentUserId, String comment) {
        GovernanceOpsAlert alert = requireById(alertId);
        GovernanceOpsAlert update = new GovernanceOpsAlert();
        update.setId(alert.getId());
        update.setAlertStatus(STATUS_SUPPRESSED);
        update.setAssigneeUserId(currentUserId);
        update.setAlertMessage(resolveManualComment(alert.getAlertMessage(), comment));
        update.setUpdateBy(currentUserId);
        alertMapper.updateById(update);
    }

    @Override
    public void close(Long alertId, Long currentUserId, String comment) {
        GovernanceOpsAlert alert = requireById(alertId);
        GovernanceOpsAlert update = new GovernanceOpsAlert();
        update.setId(alert.getId());
        update.setAlertStatus(STATUS_CLOSED);
        update.setClosedTime(new Date());
        update.setAlertMessage(resolveManualComment(alert.getAlertMessage(), comment));
        update.setUpdateBy(currentUserId);
        alertMapper.updateById(update);
    }

    private String resolveAlertStatus(GovernanceOpsAlert existing) {
        if (existing == null || shouldReopen(existing)) {
            return STATUS_OPEN;
        }
        return StringUtils.hasText(existing.getAlertStatus()) ? existing.getAlertStatus() : STATUS_OPEN;
    }

    private boolean shouldReopen(GovernanceOpsAlert existing) {
        return existing == null || STATUS_RESOLVED.equals(existing.getAlertStatus());
    }

    private String resolveAlertMessage(GovernanceOpsAlert existing, GovernanceOpsAlertCommand command) {
        String generated = normalize(command.alertMessage());
        if (existing == null || shouldReopen(existing)) {
            return generated;
        }
        return StringUtils.hasText(existing.getAlertMessage()) ? existing.getAlertMessage() : generated;
    }

    private String resolveManualComment(String existingMessage, String comment) {
        String normalizedComment = normalize(comment);
        return StringUtils.hasText(normalizedComment) ? normalizedComment : normalize(existingMessage);
    }

    private GovernanceOpsAlert requireByNaturalKey(String alertType, String alertCode) {
        GovernanceOpsAlert alert = alertMapper.selectOne(new LambdaQueryWrapper<GovernanceOpsAlert>()
                .eq(GovernanceOpsAlert::getDeleted, 0)
                .eq(GovernanceOpsAlert::getAlertType, normalize(alertType))
                .eq(GovernanceOpsAlert::getAlertCode, normalize(alertCode))
                .last("limit 1"));
        if (alert == null) {
            throw new BizException("治理运维告警不存在");
        }
        return alert;
    }

    private GovernanceOpsAlert requireById(Long alertId) {
        if (alertId == null || alertId <= 0) {
            throw new BizException("治理运维告警不存在");
        }
        GovernanceOpsAlert alert = alertMapper.selectById(alertId);
        if (alert == null) {
            throw new BizException("治理运维告警不存在");
        }
        return alert;
    }

    private GovernanceOpsAlertCommand requireCommand(GovernanceOpsAlertCommand command) {
        if (command == null) {
            throw new BizException("治理运维告警命令不能为空");
        }
        if (!StringUtils.hasText(command.alertType()) || !StringUtils.hasText(command.alertCode())) {
            throw new BizException("治理运维告警缺少关键标识");
        }
        if (command.operatorUserId() == null || command.operatorUserId() <= 0) {
            throw new BizException("治理运维告警操作人无效");
        }
        return command;
    }

    private LambdaQueryWrapper<GovernanceOpsAlert> buildPageWrapper(GovernanceOpsAlertPageQuery query) {
        LambdaQueryWrapper<GovernanceOpsAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovernanceOpsAlert::getDeleted, 0);
        if (query == null) {
            wrapper.orderByDesc(GovernanceOpsAlert::getLastSeenTime).orderByDesc(GovernanceOpsAlert::getId);
            return wrapper;
        }
        wrapper.eq(StringUtils.hasText(normalize(query.getAlertType())), GovernanceOpsAlert::getAlertType, normalize(query.getAlertType()));
        wrapper.eq(StringUtils.hasText(normalize(query.getAlertStatus())), GovernanceOpsAlert::getAlertStatus, normalize(query.getAlertStatus()));
        wrapper.eq(StringUtils.hasText(normalize(query.getSubjectType())), GovernanceOpsAlert::getSubjectType, normalize(query.getSubjectType()));
        wrapper.eq(query.getSubjectId() != null, GovernanceOpsAlert::getSubjectId, query.getSubjectId());
        wrapper.eq(query.getProductId() != null, GovernanceOpsAlert::getProductId, query.getProductId());
        wrapper.eq(query.getRiskMetricId() != null, GovernanceOpsAlert::getRiskMetricId, query.getRiskMetricId());
        wrapper.eq(StringUtils.hasText(normalize(query.getSeverityLevel())), GovernanceOpsAlert::getSeverityLevel, normalize(query.getSeverityLevel()));
        wrapper.eq(query.getAssigneeUserId() != null, GovernanceOpsAlert::getAssigneeUserId, query.getAssigneeUserId());
        wrapper.orderByDesc(GovernanceOpsAlert::getLastSeenTime).orderByDesc(GovernanceOpsAlert::getId);
        return wrapper;
    }

    private GovernanceOpsAlertVO toVO(GovernanceOpsAlert item) {
        GovernanceOpsAlertVO vo = new GovernanceOpsAlertVO();
        vo.setId(item.getId());
        vo.setAlertType(item.getAlertType());
        vo.setAlertCode(item.getAlertCode());
        vo.setSubjectType(item.getSubjectType());
        vo.setSubjectId(item.getSubjectId());
        vo.setProductId(item.getProductId());
        vo.setRiskMetricId(item.getRiskMetricId());
        vo.setReleaseBatchId(item.getReleaseBatchId());
        vo.setTraceId(item.getTraceId());
        vo.setDeviceCode(item.getDeviceCode());
        vo.setProductKey(item.getProductKey());
        vo.setAlertStatus(item.getAlertStatus());
        vo.setSeverityLevel(item.getSeverityLevel());
        vo.setAffectedCount(item.getAffectedCount());
        vo.setAlertTitle(item.getAlertTitle());
        vo.setAlertMessage(item.getAlertMessage());
        vo.setDimensionKey(item.getDimensionKey());
        vo.setDimensionLabel(item.getDimensionLabel());
        vo.setSourceStage(item.getSourceStage());
        vo.setSnapshotJson(item.getSnapshotJson());
        vo.setAssigneeUserId(item.getAssigneeUserId());
        vo.setFirstSeenTime(item.getFirstSeenTime());
        vo.setLastSeenTime(item.getLastSeenTime());
        vo.setResolvedTime(item.getResolvedTime());
        vo.setClosedTime(item.getClosedTime());
        vo.setCreateTime(item.getCreateTime());
        vo.setUpdateTime(item.getUpdateTime());
        return vo;
    }

    private String defaultSeverity(String severityLevel) {
        String normalized = normalize(severityLevel);
        return StringUtils.hasText(normalized) ? normalized : DEFAULT_SEVERITY;
    }

    private Long defaultAffectedCount(Long affectedCount) {
        return affectedCount == null || affectedCount < 0 ? 0L : affectedCount;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
