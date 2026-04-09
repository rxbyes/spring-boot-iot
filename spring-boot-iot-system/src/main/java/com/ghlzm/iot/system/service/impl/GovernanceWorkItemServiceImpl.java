package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.GovernanceWorkItem;
import com.ghlzm.iot.system.mapper.GovernanceWorkItemMapper;
import com.ghlzm.iot.system.service.GovernanceWorkItemService;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemCommand;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemPageQuery;
import com.ghlzm.iot.system.vo.GovernanceWorkItemVO;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceWorkItemServiceImpl implements GovernanceWorkItemService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_ACKED = "ACKED";
    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String DEFAULT_PRIORITY = "P2";

    private final GovernanceWorkItemMapper workItemMapper;

    public GovernanceWorkItemServiceImpl(GovernanceWorkItemMapper workItemMapper) {
        this.workItemMapper = workItemMapper;
    }

    @Override
    public void openOrRefresh(GovernanceWorkItemCommand command) {
        GovernanceWorkItemCommand normalized = requireCommand(command);
        GovernanceWorkItem existing = workItemMapper.selectOne(new LambdaQueryWrapper<GovernanceWorkItem>()
                .eq(GovernanceWorkItem::getDeleted, 0)
                .eq(GovernanceWorkItem::getWorkItemCode, normalized.workItemCode())
                .eq(GovernanceWorkItem::getSubjectType, normalized.subjectType())
                .eq(GovernanceWorkItem::getSubjectId, normalized.subjectId())
                .last("limit 1"));
        if (existing == null) {
            GovernanceWorkItem item = new GovernanceWorkItem();
            item.setWorkItemCode(normalized.workItemCode());
            item.setSubjectType(normalized.subjectType());
            item.setSubjectId(normalized.subjectId());
            item.setProductId(normalized.productId());
            item.setRiskMetricId(normalized.riskMetricId());
            item.setReleaseBatchId(normalized.releaseBatchId());
            item.setApprovalOrderId(normalized.approvalOrderId());
            item.setAssigneeUserId(normalized.assigneeUserId());
            item.setSourceStage(normalize(normalized.sourceStage()));
            item.setBlockingReason(normalize(normalized.blockingReason()));
            item.setSnapshotJson(normalize(normalized.snapshotJson()));
            item.setPriorityLevel(defaultPriority(normalized.priorityLevel()));
            item.setWorkStatus(STATUS_OPEN);
            item.setCreateBy(normalized.operatorUserId());
            item.setUpdateBy(normalized.operatorUserId());
            item.setDeleted(0);
            workItemMapper.insert(item);
            return;
        }

        GovernanceWorkItem refreshed = new GovernanceWorkItem();
        refreshed.setId(existing.getId());
        refreshed.setProductId(normalized.productId());
        refreshed.setRiskMetricId(normalized.riskMetricId());
        refreshed.setReleaseBatchId(normalized.releaseBatchId());
        refreshed.setApprovalOrderId(normalized.approvalOrderId());
        refreshed.setAssigneeUserId(normalized.assigneeUserId());
        refreshed.setSourceStage(normalize(normalized.sourceStage()));
        refreshed.setBlockingReason(normalize(normalized.blockingReason()));
        refreshed.setSnapshotJson(normalize(normalized.snapshotJson()));
        refreshed.setPriorityLevel(defaultPriority(normalized.priorityLevel()));
        refreshed.setWorkStatus(STATUS_OPEN);
        refreshed.setResolvedTime(null);
        refreshed.setClosedTime(null);
        refreshed.setUpdateBy(normalized.operatorUserId());
        workItemMapper.updateById(refreshed);
    }

    @Override
    public void resolve(String workItemCode, String subjectType, Long subjectId, Long operatorUserId, String comment) {
        GovernanceWorkItem item = requireByNaturalKey(workItemCode, subjectType, subjectId);
        GovernanceWorkItem update = new GovernanceWorkItem();
        update.setId(item.getId());
        update.setWorkStatus(STATUS_RESOLVED);
        update.setResolvedTime(new Date());
        update.setBlockingReason(normalize(comment));
        update.setUpdateBy(operatorUserId);
        workItemMapper.updateById(update);
    }

    @Override
    public PageResult<GovernanceWorkItemVO> pageWorkItems(GovernanceWorkItemPageQuery query, Long currentUserId) {
        Page<GovernanceWorkItem> page = PageQueryUtils.buildPage(query == null ? null : query.getPageNum(), query == null ? null : query.getPageSize());
        Page<GovernanceWorkItem> result = workItemMapper.selectPage(page, buildPageWrapper(query));
        List<GovernanceWorkItemVO> rows = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), rows);
    }

    @Override
    public void ack(Long workItemId, Long currentUserId, String comment) {
        GovernanceWorkItem item = requireById(workItemId);
        GovernanceWorkItem update = new GovernanceWorkItem();
        update.setId(item.getId());
        update.setWorkStatus(STATUS_ACKED);
        update.setAssigneeUserId(currentUserId);
        update.setBlockingReason(normalize(comment));
        update.setUpdateBy(currentUserId);
        workItemMapper.updateById(update);
    }

    @Override
    public void block(Long workItemId, Long currentUserId, String comment) {
        GovernanceWorkItem item = requireById(workItemId);
        GovernanceWorkItem update = new GovernanceWorkItem();
        update.setId(item.getId());
        update.setWorkStatus(STATUS_BLOCKED);
        update.setAssigneeUserId(currentUserId);
        update.setBlockingReason(normalize(comment));
        update.setUpdateBy(currentUserId);
        workItemMapper.updateById(update);
    }

    @Override
    public void close(Long workItemId, Long currentUserId, String comment) {
        GovernanceWorkItem item = requireById(workItemId);
        GovernanceWorkItem update = new GovernanceWorkItem();
        update.setId(item.getId());
        update.setWorkStatus(STATUS_CLOSED);
        update.setClosedTime(new Date());
        update.setBlockingReason(normalize(comment));
        update.setUpdateBy(currentUserId);
        workItemMapper.updateById(update);
    }

    private GovernanceWorkItem requireByNaturalKey(String workItemCode, String subjectType, Long subjectId) {
        GovernanceWorkItem item = workItemMapper.selectOne(new LambdaQueryWrapper<GovernanceWorkItem>()
                .eq(GovernanceWorkItem::getDeleted, 0)
                .eq(GovernanceWorkItem::getWorkItemCode, normalize(workItemCode))
                .eq(GovernanceWorkItem::getSubjectType, normalize(subjectType))
                .eq(GovernanceWorkItem::getSubjectId, subjectId)
                .last("limit 1"));
        if (item == null) {
            throw new BizException("治理工作项不存在");
        }
        return item;
    }

    private GovernanceWorkItem requireById(Long workItemId) {
        if (workItemId == null || workItemId <= 0) {
            throw new BizException("治理工作项不存在");
        }
        GovernanceWorkItem item = workItemMapper.selectById(workItemId);
        if (item == null) {
            throw new BizException("治理工作项不存在");
        }
        return item;
    }

    private GovernanceWorkItemCommand requireCommand(GovernanceWorkItemCommand command) {
        if (command == null) {
            throw new BizException("治理工作项命令不能为空");
        }
        if (!StringUtils.hasText(command.workItemCode()) || !StringUtils.hasText(command.subjectType()) || command.subjectId() == null) {
            throw new BizException("治理工作项缺少关键标识");
        }
        if (command.operatorUserId() == null || command.operatorUserId() <= 0) {
            throw new BizException("治理工作项操作人无效");
        }
        return command;
    }

    private LambdaQueryWrapper<GovernanceWorkItem> buildPageWrapper(GovernanceWorkItemPageQuery query) {
        LambdaQueryWrapper<GovernanceWorkItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovernanceWorkItem::getDeleted, 0);
        if (query == null) {
            wrapper.orderByDesc(GovernanceWorkItem::getCreateTime).orderByDesc(GovernanceWorkItem::getId);
            return wrapper;
        }
        wrapper.eq(StringUtils.hasText(normalize(query.getWorkItemCode())), GovernanceWorkItem::getWorkItemCode, normalize(query.getWorkItemCode()));
        wrapper.eq(StringUtils.hasText(normalize(query.getWorkStatus())), GovernanceWorkItem::getWorkStatus, normalize(query.getWorkStatus()));
        wrapper.eq(StringUtils.hasText(normalize(query.getSubjectType())), GovernanceWorkItem::getSubjectType, normalize(query.getSubjectType()));
        wrapper.eq(query.getSubjectId() != null, GovernanceWorkItem::getSubjectId, query.getSubjectId());
        wrapper.eq(query.getProductId() != null, GovernanceWorkItem::getProductId, query.getProductId());
        wrapper.eq(query.getRiskMetricId() != null, GovernanceWorkItem::getRiskMetricId, query.getRiskMetricId());
        wrapper.eq(query.getAssigneeUserId() != null, GovernanceWorkItem::getAssigneeUserId, query.getAssigneeUserId());
        wrapper.orderByDesc(GovernanceWorkItem::getCreateTime).orderByDesc(GovernanceWorkItem::getId);
        return wrapper;
    }

    private GovernanceWorkItemVO toVO(GovernanceWorkItem item) {
        GovernanceWorkItemVO vo = new GovernanceWorkItemVO();
        vo.setId(item.getId());
        vo.setWorkItemCode(item.getWorkItemCode());
        vo.setSubjectType(item.getSubjectType());
        vo.setSubjectId(item.getSubjectId());
        vo.setProductId(item.getProductId());
        vo.setRiskMetricId(item.getRiskMetricId());
        vo.setReleaseBatchId(item.getReleaseBatchId());
        vo.setApprovalOrderId(item.getApprovalOrderId());
        vo.setTraceId(item.getTraceId());
        vo.setDeviceCode(item.getDeviceCode());
        vo.setProductKey(item.getProductKey());
        vo.setWorkStatus(item.getWorkStatus());
        vo.setPriorityLevel(item.getPriorityLevel());
        vo.setAssigneeUserId(item.getAssigneeUserId());
        vo.setSourceStage(item.getSourceStage());
        vo.setBlockingReason(item.getBlockingReason());
        vo.setSnapshotJson(item.getSnapshotJson());
        vo.setDueTime(item.getDueTime());
        vo.setResolvedTime(item.getResolvedTime());
        vo.setClosedTime(item.getClosedTime());
        vo.setCreateTime(item.getCreateTime());
        vo.setUpdateTime(item.getUpdateTime());
        return vo;
    }

    private String defaultPriority(String priorityLevel) {
        String normalized = normalize(priorityLevel);
        return StringUtils.hasText(normalized) ? normalized : DEFAULT_PRIORITY;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
