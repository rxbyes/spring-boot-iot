package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.GovernanceWorkItem;
import com.ghlzm.iot.system.mapper.GovernanceWorkItemMapper;
import com.ghlzm.iot.system.service.GovernanceWorkItemContributor;
import com.ghlzm.iot.system.service.GovernanceWorkItemService;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemCommand;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemPageQuery;
import com.ghlzm.iot.system.vo.GovernanceWorkItemVO;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceWorkItemServiceImpl implements GovernanceWorkItemService {

    private static final Logger log = LoggerFactory.getLogger(GovernanceWorkItemServiceImpl.class);

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_ACKED = "ACKED";
    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String DEFAULT_PRIORITY = "P2";
    private static final String DEFAULT_DOMAIN = "PRODUCT";
    private static final String DOMAIN_ALARM = "ALARM";
    private static final String DEFAULT_EXECUTION_STATUS = "PENDING_APPROVAL";
    private static final String EXECUTION_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String EXECUTION_STATUS_REPLAY_REQUIRED = "REPLAY_REQUIRED";
    private static final String EXECUTION_STATUS_RESOLVED = "RESOLVED";
    private static final String EXECUTION_STATUS_CLOSED = "CLOSED";
    private static final long DEFAULT_CONTRIBUTOR_SYNC_INTERVAL_MS = 300_000L;

    private final GovernanceWorkItemMapper workItemMapper;
    private final List<GovernanceWorkItemContributor> contributors;
    private final Executor contributorSyncExecutor;
    private final LongSupplier currentTimeSupplier;
    private final long contributorSyncIntervalMs;
    private final AtomicBoolean contributorSyncInProgress = new AtomicBoolean(false);

    private volatile long lastContributorSyncCompletedAt;

    @Autowired
    public GovernanceWorkItemServiceImpl(GovernanceWorkItemMapper workItemMapper,
                                         List<GovernanceWorkItemContributor> contributors,
                                         @Qualifier("applicationTaskExecutor") Executor contributorSyncExecutor) {
        this(workItemMapper, contributors, contributorSyncExecutor, System::currentTimeMillis, DEFAULT_CONTRIBUTOR_SYNC_INTERVAL_MS);
    }

    GovernanceWorkItemServiceImpl(GovernanceWorkItemMapper workItemMapper,
                                  List<GovernanceWorkItemContributor> contributors) {
        this(workItemMapper, contributors, Runnable::run, System::currentTimeMillis, DEFAULT_CONTRIBUTOR_SYNC_INTERVAL_MS);
    }

    GovernanceWorkItemServiceImpl(GovernanceWorkItemMapper workItemMapper,
                                  List<GovernanceWorkItemContributor> contributors,
                                  Executor contributorSyncExecutor,
                                  LongSupplier currentTimeSupplier,
                                  long contributorSyncIntervalMs) {
        this.workItemMapper = workItemMapper;
        this.contributors = contributors == null ? List.of() : List.copyOf(contributors);
        this.contributorSyncExecutor = contributorSyncExecutor == null ? Runnable::run : contributorSyncExecutor;
        this.currentTimeSupplier = currentTimeSupplier == null ? System::currentTimeMillis : currentTimeSupplier;
        this.contributorSyncIntervalMs = Math.max(0L, contributorSyncIntervalMs);
    }

    @Override
    public void openOrRefresh(GovernanceWorkItemCommand command) {
        openOrRefreshAndGetId(command);
    }

    @Override
    public Long openOrRefreshAndGetId(GovernanceWorkItemCommand command) {
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
            item.setTraceId(normalize(normalized.traceId()));
            item.setDeviceCode(normalize(normalized.deviceCode()));
            item.setProductKey(normalize(normalized.productKey()));
            item.setAssigneeUserId(normalized.assigneeUserId());
            item.setSourceStage(normalize(normalized.sourceStage()));
            item.setBlockingReason(normalize(normalized.blockingReason()));
            item.setSnapshotJson(normalize(normalized.snapshotJson()));
            applyLifecycleHubFields(item, normalized);
            item.setPriorityLevel(defaultPriority(normalized.priorityLevel()));
            item.setWorkStatus(STATUS_OPEN);
            item.setCreateBy(normalized.operatorUserId());
            item.setUpdateBy(normalized.operatorUserId());
            item.setDeleted(0);
            workItemMapper.insert(item);
            return item.getId();
        }

        GovernanceWorkItem refreshed = new GovernanceWorkItem();
        refreshed.setId(existing.getId());
        refreshed.setProductId(normalized.productId());
        refreshed.setRiskMetricId(normalized.riskMetricId());
        refreshed.setReleaseBatchId(normalized.releaseBatchId());
        refreshed.setApprovalOrderId(normalized.approvalOrderId());
        refreshed.setTraceId(normalize(normalized.traceId()));
        refreshed.setDeviceCode(normalize(normalized.deviceCode()));
        refreshed.setProductKey(normalize(normalized.productKey()));
        refreshed.setAssigneeUserId(normalized.assigneeUserId());
        refreshed.setSourceStage(normalize(normalized.sourceStage()));
        refreshed.setBlockingReason(resolveBlockingReason(existing, normalized));
        refreshed.setSnapshotJson(normalize(normalized.snapshotJson()));
        applyLifecycleHubFields(refreshed, normalized);
        refreshed.setPriorityLevel(defaultPriority(normalized.priorityLevel()));
        refreshed.setWorkStatus(resolveWorkStatus(existing));
        refreshed.setExecutionStatus(resolveExecutionStatus(existing, normalized));
        refreshed.setResolvedTime(shouldReopen(existing) ? null : existing.getResolvedTime());
        refreshed.setClosedTime(shouldReopen(existing) ? null : existing.getClosedTime());
        refreshed.setUpdateBy(normalized.operatorUserId());
        if (!shouldReopen(existing) && existing.getAssigneeUserId() != null && normalized.assigneeUserId() == null) {
            refreshed.setAssigneeUserId(existing.getAssigneeUserId());
        }
        workItemMapper.updateById(refreshed);
        return existing.getId();
    }

    @Override
    public void resolve(String workItemCode, String subjectType, Long subjectId, Long operatorUserId, String comment) {
        GovernanceWorkItem item = requireByNaturalKey(workItemCode, subjectType, subjectId);
        GovernanceWorkItem update = new GovernanceWorkItem();
        update.setId(item.getId());
        update.setWorkStatus(STATUS_RESOLVED);
        update.setExecutionStatus(EXECUTION_STATUS_RESOLVED);
        update.setResolvedTime(new Date());
        update.setBlockingReason(normalize(comment));
        update.setUpdateBy(operatorUserId);
        workItemMapper.updateById(update);
    }

    @Override
    public PageResult<GovernanceWorkItemVO> pageWorkItems(GovernanceWorkItemPageQuery query, Long currentUserId) {
        scheduleContributorSyncIfStale();
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
        update.setExecutionStatus(EXECUTION_STATUS_IN_PROGRESS);
        update.setAssigneeUserId(currentUserId);
        update.setBlockingReason(resolveManualComment(item.getBlockingReason(), comment));
        update.setUpdateBy(currentUserId);
        workItemMapper.updateById(update);
    }

    @Override
    public void block(Long workItemId, Long currentUserId, String comment) {
        GovernanceWorkItem item = requireById(workItemId);
        GovernanceWorkItem update = new GovernanceWorkItem();
        update.setId(item.getId());
        update.setWorkStatus(STATUS_BLOCKED);
        update.setExecutionStatus(EXECUTION_STATUS_REPLAY_REQUIRED);
        update.setAssigneeUserId(currentUserId);
        update.setBlockingReason(resolveManualComment(item.getBlockingReason(), comment));
        update.setUpdateBy(currentUserId);
        workItemMapper.updateById(update);
    }

    @Override
    public void close(Long workItemId, Long currentUserId, String comment) {
        GovernanceWorkItem item = requireById(workItemId);
        GovernanceWorkItem update = new GovernanceWorkItem();
        update.setId(item.getId());
        update.setWorkStatus(STATUS_CLOSED);
        update.setExecutionStatus(EXECUTION_STATUS_CLOSED);
        update.setClosedTime(new Date());
        update.setBlockingReason(resolveManualComment(item.getBlockingReason(), comment));
        update.setUpdateBy(currentUserId);
        workItemMapper.updateById(update);
    }

    private void scheduleContributorSyncIfStale() {
        if (contributors.isEmpty()) {
            return;
        }
        long now = currentTimeSupplier.getAsLong();
        if (lastContributorSyncCompletedAt > 0L
                && now - lastContributorSyncCompletedAt < contributorSyncIntervalMs) {
            return;
        }
        if (!contributorSyncInProgress.compareAndSet(false, true)) {
            return;
        }
        try {
            contributorSyncExecutor.execute(() -> {
                try {
                    syncContributedWorkItems();
                    lastContributorSyncCompletedAt = currentTimeSupplier.getAsLong();
                } catch (RuntimeException ex) {
                    log.warn("governance work item contributor sync failed", ex);
                } finally {
                    contributorSyncInProgress.set(false);
                }
            });
        } catch (RuntimeException ex) {
            contributorSyncInProgress.set(false);
            log.warn("governance work item contributor sync dispatch failed", ex);
        }
    }

    private void syncContributedWorkItems() {
        if (contributors.isEmpty()) {
            return;
        }
        List<GovernanceWorkItemCommand> commands = contributors.stream()
                .flatMap(contributor -> contributor.collectWorkItems().stream())
                .toList();
        if (commands.isEmpty()) {
            return;
        }
        Map<String, Map<String, GovernanceWorkItemCommand>> desiredByCode = new LinkedHashMap<>();
        for (GovernanceWorkItemCommand command : commands) {
            GovernanceWorkItemCommand normalized = requireCommand(command);
            desiredByCode
                    .computeIfAbsent(normalized.workItemCode(), key -> new LinkedHashMap<>())
                    .put(naturalKey(normalized.workItemCode(), normalized.subjectType(), normalized.subjectId()), normalized);
        }
        List<GovernanceWorkItem> existingItems = workItemMapper.selectList(new LambdaQueryWrapper<GovernanceWorkItem>()
                .eq(GovernanceWorkItem::getDeleted, 0)
                .in(GovernanceWorkItem::getWorkItemCode, desiredByCode.keySet()));
        Map<String, GovernanceWorkItem> existingByNaturalKey = existingItems.stream()
                .filter(item -> item != null)
                .collect(Collectors.toMap(
                        item -> naturalKey(item.getWorkItemCode(), item.getSubjectType(), item.getSubjectId()),
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        for (Map<String, GovernanceWorkItemCommand> itemsByKey : desiredByCode.values()) {
            for (GovernanceWorkItemCommand command : itemsByKey.values()) {
                syncContributedCommand(command, existingByNaturalKey.get(naturalKey(command.workItemCode(), command.subjectType(), command.subjectId())));
            }
        }
        for (GovernanceWorkItem existing : existingItems) {
            if (existing == null || !StringUtils.hasText(existing.getWorkStatus()) || STATUS_CLOSED.equals(existing.getWorkStatus())) {
                continue;
            }
            Map<String, GovernanceWorkItemCommand> desiredItems = desiredByCode.get(existing.getWorkItemCode());
            if (desiredItems == null) {
                continue;
            }
            if (!desiredItems.containsKey(naturalKey(existing.getWorkItemCode(), existing.getSubjectType(), existing.getSubjectId()))) {
                GovernanceWorkItem update = new GovernanceWorkItem();
                update.setId(existing.getId());
                update.setWorkStatus(STATUS_RESOLVED);
                update.setExecutionStatus(EXECUTION_STATUS_RESOLVED);
                update.setResolvedTime(new Date());
                update.setUpdateBy(1L);
                workItemMapper.updateById(update);
            }
        }
    }

    private void syncContributedCommand(GovernanceWorkItemCommand command, GovernanceWorkItem existing) {
        GovernanceWorkItemCommand normalized = requireCommand(command);
        if (existing == null) {
            GovernanceWorkItem item = new GovernanceWorkItem();
            item.setWorkItemCode(normalized.workItemCode());
            item.setSubjectType(normalized.subjectType());
            item.setSubjectId(normalized.subjectId());
            item.setProductId(normalized.productId());
            item.setRiskMetricId(normalized.riskMetricId());
            item.setReleaseBatchId(normalized.releaseBatchId());
            item.setApprovalOrderId(normalized.approvalOrderId());
            item.setTraceId(normalize(normalized.traceId()));
            item.setDeviceCode(normalize(normalized.deviceCode()));
            item.setProductKey(normalize(normalized.productKey()));
            item.setAssigneeUserId(normalized.assigneeUserId());
            item.setSourceStage(normalize(normalized.sourceStage()));
            item.setBlockingReason(normalize(normalized.blockingReason()));
            item.setSnapshotJson(normalize(normalized.snapshotJson()));
            applyLifecycleHubFields(item, normalized);
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
        refreshed.setTraceId(normalize(normalized.traceId()));
        refreshed.setDeviceCode(normalize(normalized.deviceCode()));
        refreshed.setProductKey(normalize(normalized.productKey()));
        refreshed.setAssigneeUserId(normalized.assigneeUserId());
        refreshed.setSourceStage(normalize(normalized.sourceStage()));
        refreshed.setBlockingReason(resolveBlockingReason(existing, normalized));
        refreshed.setSnapshotJson(normalize(normalized.snapshotJson()));
        applyLifecycleHubFields(refreshed, normalized);
        refreshed.setPriorityLevel(defaultPriority(normalized.priorityLevel()));
        refreshed.setWorkStatus(resolveWorkStatus(existing));
        refreshed.setExecutionStatus(resolveExecutionStatus(existing, normalized));
        refreshed.setResolvedTime(shouldReopen(existing) ? null : existing.getResolvedTime());
        refreshed.setClosedTime(shouldReopen(existing) ? null : existing.getClosedTime());
        refreshed.setUpdateBy(normalized.operatorUserId());
        if (!shouldReopen(existing) && existing.getAssigneeUserId() != null && normalized.assigneeUserId() == null) {
            refreshed.setAssigneeUserId(existing.getAssigneeUserId());
        }
        workItemMapper.updateById(refreshed);
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

    private String resolveManualComment(String existingReason, String comment) {
        String normalizedComment = normalize(comment);
        return StringUtils.hasText(normalizedComment) ? normalizedComment : normalize(existingReason);
    }

    private String resolveBlockingReason(GovernanceWorkItem existing, GovernanceWorkItemCommand command) {
        String generated = normalize(command.blockingReason());
        if (existing == null || shouldReopen(existing)) {
            return generated;
        }
        return StringUtils.hasText(existing.getBlockingReason()) ? existing.getBlockingReason() : generated;
    }

    private String resolveWorkStatus(GovernanceWorkItem existing) {
        if (existing == null || shouldReopen(existing)) {
            return STATUS_OPEN;
        }
        return StringUtils.hasText(existing.getWorkStatus()) ? existing.getWorkStatus() : STATUS_OPEN;
    }

    private boolean shouldReopen(GovernanceWorkItem existing) {
        return existing == null || STATUS_RESOLVED.equals(existing.getWorkStatus());
    }

    private String naturalKey(String workItemCode, String subjectType, Long subjectId) {
        return normalize(workItemCode) + "|" + normalize(subjectType) + "|" + (subjectId == null ? "" : subjectId);
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
        vo.setTaskCategory(item.getTaskCategory());
        vo.setDomainCode(item.getDomainCode());
        vo.setActionCode(item.getActionCode());
        vo.setExecutionStatus(item.getExecutionStatus());
        vo.setRecommendationSnapshotJson(item.getRecommendationSnapshotJson());
        vo.setEvidenceSnapshotJson(item.getEvidenceSnapshotJson());
        vo.setImpactSnapshotJson(item.getImpactSnapshotJson());
        vo.setRollbackSnapshotJson(item.getRollbackSnapshotJson());
        vo.setRecommendation(GovernanceSnapshotResolver.resolveRecommendation(
                item.getRecommendationSnapshotJson(),
                item.getEvidenceSnapshotJson(),
                item.getSnapshotJson(),
                defaultRecommendationType(item),
                defaultSuggestedAction(item.getBlockingReason(), item.getActionCode())
        ));
        vo.setImpact(GovernanceSnapshotResolver.resolveImpact(
                item.getImpactSnapshotJson(),
                item.getRollbackSnapshotJson(),
                item.getSnapshotJson(),
                null,
                defaultAffectedType(item.getSubjectType(), item.getTaskCategory())
        ));
        vo.setRollback(GovernanceSnapshotResolver.resolveRollback(
                item.getRollbackSnapshotJson(),
                item.getImpactSnapshotJson(),
                item.getSnapshotJson()
        ));
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

    private void applyLifecycleHubFields(GovernanceWorkItem item, GovernanceWorkItemCommand command) {
        String normalizedSnapshot = normalize(command.snapshotJson());
        item.setTaskCategory(resolveTaskCategory(command));
        item.setDomainCode(resolveDomainCode(command));
        item.setActionCode(resolveActionCode(command));
        item.setExecutionStatus(resolveExecutionStatus(command));
        item.setRecommendationSnapshotJson(resolveLifecycleSnapshot(command.recommendationSnapshotJson(), normalizedSnapshot, "confidence"));
        item.setEvidenceSnapshotJson(resolveLifecycleSnapshot(command.evidenceSnapshotJson(), normalizedSnapshot, "evidenceItems"));
        item.setImpactSnapshotJson(resolveLifecycleSnapshot(command.impactSnapshotJson(), normalizedSnapshot, "affectedRiskPointCount"));
        item.setRollbackSnapshotJson(resolveLifecycleSnapshot(command.rollbackSnapshotJson(), normalizedSnapshot, "rollbackable"));
    }

    private String resolveTaskCategory(GovernanceWorkItemCommand command) {
        String explicit = normalize(command.taskCategory());
        if (StringUtils.hasText(explicit)) {
            return explicit;
        }
        String workItemCode = normalize(command.workItemCode());
        if ("PENDING_CONTRACT_RELEASE".equals(workItemCode)) {
            return "CONTRACT_RELEASE";
        }
        if ("PENDING_RISK_BINDING".equals(workItemCode)) {
            return "RISK_BINDING";
        }
        if ("PENDING_THRESHOLD_POLICY".equals(workItemCode)) {
            return "THRESHOLD_POLICY";
        }
        if ("PENDING_LINKAGE_PLAN".equals(workItemCode)) {
            return "LINKAGE_PLAN";
        }
        if ("PENDING_REPLAY".equals(workItemCode)) {
            return "REPLAY";
        }
        return "PRODUCT_GOVERNANCE";
    }

    private String resolveDomainCode(GovernanceWorkItemCommand command) {
        String explicit = normalize(command.domainCode());
        if (StringUtils.hasText(explicit)) {
            return explicit;
        }
        if ("PENDING_RISK_BINDING".equals(normalize(command.workItemCode()))) {
            return DOMAIN_ALARM;
        }
        return DEFAULT_DOMAIN;
    }

    private String resolveActionCode(GovernanceWorkItemCommand command) {
        String explicit = normalize(command.actionCode());
        if (StringUtils.hasText(explicit)) {
            return explicit;
        }
        String workItemCode = normalize(command.workItemCode());
        if ("PENDING_CONTRACT_RELEASE".equals(workItemCode)) {
            return "PRODUCT_CONTRACT_RELEASE_APPLY";
        }
        if ("PENDING_RISK_BINDING".equals(workItemCode)) {
            return "RISK_POINT_PENDING_PROMOTION";
        }
        if ("PENDING_THRESHOLD_POLICY".equals(workItemCode)) {
            return "RISK_THRESHOLD_POLICY_REVIEW";
        }
        if ("PENDING_LINKAGE_PLAN".equals(workItemCode)) {
            return "RISK_LINKAGE_PLAN_REVIEW";
        }
        if ("PENDING_REPLAY".equals(workItemCode)) {
            return "RISK_REPLAY_REVIEW";
        }
        return "PRODUCT_GOVERNANCE_REVIEW";
    }

    private String resolveExecutionStatus(GovernanceWorkItemCommand command) {
        String explicit = normalize(command.executionStatus());
        return StringUtils.hasText(explicit) ? explicit : DEFAULT_EXECUTION_STATUS;
    }

    private String resolveExecutionStatus(GovernanceWorkItem existing, GovernanceWorkItemCommand command) {
        if (existing == null || shouldReopen(existing)) {
            return resolveExecutionStatus(command);
        }
        String existingExecutionStatus = normalize(existing.getExecutionStatus());
        if (StringUtils.hasText(existingExecutionStatus)) {
            return existingExecutionStatus;
        }
        return resolveExecutionStatus(command);
    }

    private String resolveLifecycleSnapshot(String explicitSnapshot, String fallbackSnapshot, String marker) {
        String normalized = normalize(explicitSnapshot);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        if (StringUtils.hasText(fallbackSnapshot) && fallbackSnapshot.contains(marker)) {
            return fallbackSnapshot;
        }
        return null;
    }

    private String defaultRecommendationType(GovernanceWorkItem item) {
        if (item == null) {
            return null;
        }
        String workItemCode = normalize(item.getWorkItemCode());
        if ("PENDING_RISK_BINDING".equals(workItemCode)) {
            return "PROMOTE";
        }
        if ("PENDING_CONTRACT_RELEASE".equals(workItemCode)) {
            return "PUBLISH";
        }
        if ("PENDING_REPLAY".equals(workItemCode)) {
            return "REPLAY";
        }
        if ("PENDING_THRESHOLD_POLICY".equals(workItemCode)
                || "PENDING_LINKAGE_PLAN".equals(workItemCode)) {
            return "CREATE_POLICY";
        }
        return "IGNORE";
    }

    private String defaultSuggestedAction(String blockingReason, String actionCode) {
        String normalizedReason = normalize(blockingReason);
        if (StringUtils.hasText(normalizedReason)) {
            return normalizedReason;
        }
        return normalize(actionCode);
    }

    private String defaultAffectedType(String subjectType, String taskCategory) {
        String normalizedSubjectType = normalize(subjectType);
        if (StringUtils.hasText(normalizedSubjectType)) {
            return normalizedSubjectType;
        }
        return normalize(taskCategory);
    }
}
