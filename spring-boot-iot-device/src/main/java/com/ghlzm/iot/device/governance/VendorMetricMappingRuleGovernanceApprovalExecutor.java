package com.ghlzm.iot.device.governance;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.device.entity.VendorMetricMappingRuleSnapshot;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleMapper;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleSnapshotMapper;
import com.ghlzm.iot.system.entity.GovernanceApprovalOrder;
import com.ghlzm.iot.system.service.GovernanceApprovalActionExecutor;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionExecutionResult;
import com.ghlzm.iot.system.service.model.GovernanceImpactSnapshot;
import com.ghlzm.iot.system.service.model.GovernanceRollbackSnapshot;
import com.ghlzm.iot.system.service.model.GovernanceSimulationResult;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 厂商字段映射规则治理审批执行器。
 */
@Service
public class VendorMetricMappingRuleGovernanceApprovalExecutor implements GovernanceApprovalActionExecutor {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String SNAPSHOT_STATUS_PUBLISHED = "PUBLISHED";
    private static final String SNAPSHOT_STATUS_ROLLED_BACK = "ROLLED_BACK";
    private static final List<String> AFFECTED_TYPES = List.of("PRODUCT", "TELEMETRY");

    private final VendorMetricMappingRuleMapper ruleMapper;
    private final VendorMetricMappingRuleSnapshotMapper snapshotMapper;

    public VendorMetricMappingRuleGovernanceApprovalExecutor(VendorMetricMappingRuleMapper ruleMapper,
                                                             VendorMetricMappingRuleSnapshotMapper snapshotMapper) {
        this.ruleMapper = ruleMapper;
        this.snapshotMapper = snapshotMapper;
    }

    @Override
    public boolean supports(String actionCode) {
        if (!StringUtils.hasText(actionCode)) {
            return false;
        }
        String normalized = actionCode.trim();
        return VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_PUBLISH.equals(normalized)
                || VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_ROLLBACK.equals(normalized);
    }

    @Override
    public GovernanceApprovalActionExecutionResult execute(GovernanceApprovalOrder order) {
        String actionCode = normalizeActionCode(order);
        return switch (actionCode) {
            case VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_PUBLISH -> executePublish(order);
            case VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_ROLLBACK -> executeRollback(order);
            default -> throw new BizException("审批动作不支持执行: " + actionCode);
        };
    }

    @Override
    public GovernanceSimulationResult simulate(GovernanceApprovalOrder order) {
        String actionCode = normalizeActionCode(order);
        return switch (actionCode) {
            case VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_PUBLISH -> simulatePublish(order);
            case VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_ROLLBACK -> simulateRollback(order);
            default -> throw new BizException("审批动作不支持预演: " + actionCode);
        };
    }

    private GovernanceApprovalActionExecutionResult executePublish(GovernanceApprovalOrder order) {
        VendorMetricMappingRuleGovernanceApprovalPayloads.RuleApprovalPayload payload =
                VendorMetricMappingRuleGovernanceApprovalPayloads.readPublishPayload(order.getPayloadJson());
        VendorMetricMappingRule rule = requireRule(payload.ruleId(), payload.productId());
        ensureExpectedVersion(rule, payload.expectedVersionNo());

        VendorMetricMappingRuleSnapshot snapshot = new VendorMetricMappingRuleSnapshot();
        snapshot.setRuleId(rule.getId());
        snapshot.setProductId(rule.getProductId());
        snapshot.setApprovalOrderId(order.getId());
        snapshot.setPublishedVersionNo(rule.getVersionNo());
        snapshot.setSnapshotJson(VendorMetricMappingRuleGovernanceApprovalPayloads.writeSnapshotJson(rule));
        snapshot.setLifecycleStatus(SNAPSHOT_STATUS_PUBLISHED);
        if (snapshotMapper.insert(snapshot) <= 0) {
            throw new BizException("厂商字段映射规则发布快照写入失败");
        }

        VendorMetricMappingRule update = new VendorMetricMappingRule();
        update.setId(rule.getId());
        update.setStatus(STATUS_ACTIVE);
        update.setApprovalOrderId(order.getId());
        update.setUpdateBy(order.getOperatorUserId());
        if (ruleMapper.updateById(update) <= 0) {
            throw new BizException("厂商字段映射规则发布状态更新失败");
        }
        String updatedPayloadJson = VendorMetricMappingRuleGovernanceApprovalPayloads.writePublishExecutionPayload(
                payload,
                order.getId(),
                rule.getVersionNo()
        );
        return new GovernanceApprovalActionExecutionResult(updatedPayloadJson, updatedPayloadJson, null);
    }

    private GovernanceApprovalActionExecutionResult executeRollback(GovernanceApprovalOrder order) {
        VendorMetricMappingRuleGovernanceApprovalPayloads.RuleApprovalPayload payload =
                VendorMetricMappingRuleGovernanceApprovalPayloads.readRollbackPayload(order.getPayloadJson());
        VendorMetricMappingRule rule = requireRule(payload.ruleId(), payload.productId());
        ensureExpectedVersion(rule, payload.expectedVersionNo());

        VendorMetricMappingRuleSnapshot latestSnapshot = snapshotMapper.selectLatestPublishedByRuleId(rule.getId());
        if (latestSnapshot == null || latestSnapshot.getId() == null) {
            throw new BizException("厂商字段映射规则不存在可回滚的正式快照: " + rule.getId());
        }

        VendorMetricMappingRuleSnapshot snapshotUpdate = new VendorMetricMappingRuleSnapshot();
        snapshotUpdate.setId(latestSnapshot.getId());
        snapshotUpdate.setLifecycleStatus(SNAPSHOT_STATUS_ROLLED_BACK);
        if (snapshotMapper.updateById(snapshotUpdate) <= 0) {
            throw new BizException("厂商字段映射规则回滚快照更新失败");
        }

        VendorMetricMappingRule update = new VendorMetricMappingRule();
        update.setId(rule.getId());
        update.setStatus(STATUS_DRAFT);
        update.setApprovalOrderId(order.getId());
        update.setUpdateBy(order.getOperatorUserId());
        if (ruleMapper.updateById(update) <= 0) {
            throw new BizException("厂商字段映射规则回滚状态更新失败");
        }
        String updatedPayloadJson = VendorMetricMappingRuleGovernanceApprovalPayloads.writeRollbackExecutionPayload(
                payload,
                order.getId(),
                latestSnapshot.getPublishedVersionNo()
        );
        return new GovernanceApprovalActionExecutionResult(updatedPayloadJson, null, updatedPayloadJson);
    }

    private GovernanceSimulationResult simulatePublish(GovernanceApprovalOrder order) {
        VendorMetricMappingRuleGovernanceApprovalPayloads.RuleApprovalPayload payload =
                VendorMetricMappingRuleGovernanceApprovalPayloads.readPublishPayload(order.getPayloadJson());
        requireRule(payload.ruleId(), payload.productId());
        GovernanceImpactSnapshot impact = new GovernanceImpactSnapshot();
        impact.setAffectedCount(1L);
        impact.setAffectedTypes(AFFECTED_TYPES);
        impact.setRollbackable(Boolean.TRUE);
        impact.setRollbackPlanSummary("审批通过后可通过映射规则回滚恢复上一正式快照");

        GovernanceRollbackSnapshot rollback = new GovernanceRollbackSnapshot();
        rollback.setRollbackable(Boolean.TRUE);
        rollback.setRollbackPlanSummary("审批通过后可通过映射规则回滚恢复上一正式快照");
        return new GovernanceSimulationResult(
                order.getId(),
                order.getWorkItemId(),
                order.getActionCode(),
                true,
                1L,
                AFFECTED_TYPES,
                true,
                impact.getRollbackPlanSummary(),
                null,
                impact,
                rollback,
                false,
                null
        );
    }

    private GovernanceSimulationResult simulateRollback(GovernanceApprovalOrder order) {
        VendorMetricMappingRuleGovernanceApprovalPayloads.RuleApprovalPayload payload =
                VendorMetricMappingRuleGovernanceApprovalPayloads.readRollbackPayload(order.getPayloadJson());
        requireRule(payload.ruleId(), payload.productId());
        GovernanceImpactSnapshot impact = new GovernanceImpactSnapshot();
        impact.setAffectedCount(1L);
        impact.setAffectedTypes(AFFECTED_TYPES);
        impact.setRollbackable(Boolean.TRUE);
        impact.setRollbackPlanSummary("回滚后可重新提交映射规则发布审批恢复正式快照");

        GovernanceRollbackSnapshot rollback = new GovernanceRollbackSnapshot();
        rollback.setRollbackable(Boolean.TRUE);
        rollback.setRollbackPlanSummary("回滚后可重新提交映射规则发布审批恢复正式快照");
        return new GovernanceSimulationResult(
                order.getId(),
                order.getWorkItemId(),
                order.getActionCode(),
                true,
                1L,
                AFFECTED_TYPES,
                true,
                impact.getRollbackPlanSummary(),
                null,
                impact,
                rollback,
                false,
                null
        );
    }

    private VendorMetricMappingRule requireRule(Long ruleId, Long productId) {
        if (ruleId == null || ruleId <= 0) {
            throw new BizException("厂商字段映射规则不存在: " + ruleId);
        }
        VendorMetricMappingRule rule = ruleMapper.selectById(ruleId);
        if (rule == null || Integer.valueOf(1).equals(rule.getDeleted()) || !Objects.equals(productId, rule.getProductId())) {
            throw new BizException("厂商字段映射规则不存在: " + ruleId);
        }
        return rule;
    }

    private void ensureExpectedVersion(VendorMetricMappingRule rule, Integer expectedVersionNo) {
        if (rule == null || expectedVersionNo == null || Objects.equals(rule.getVersionNo(), expectedVersionNo)) {
            return;
        }
        throw new BizException("厂商字段映射规则版本已变更，请刷新后重试");
    }

    private String normalizeActionCode(GovernanceApprovalOrder order) {
        if (order == null || !StringUtils.hasText(order.getActionCode())) {
            throw new BizException("审批动作不存在");
        }
        return order.getActionCode().trim();
    }
}
