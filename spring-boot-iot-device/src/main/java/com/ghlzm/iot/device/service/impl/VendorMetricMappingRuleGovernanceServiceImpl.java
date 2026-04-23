package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleHitPreviewDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRulePublishSubmitDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleRollbackSubmitDTO;
import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.device.entity.VendorMetricMappingRuleSnapshot;
import com.ghlzm.iot.device.governance.VendorMetricMappingRuleGovernanceApprovalPayloads;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleMapper;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleSnapshotMapper;
import com.ghlzm.iot.device.service.VendorMetricMappingRuleGovernanceService;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleHitPreviewVO;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleLedgerRowVO;
import com.ghlzm.iot.system.service.GovernanceApprovalPolicyResolver;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.vo.GovernanceSubmissionResultVO;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 厂商字段映射规则治理服务实现。
 */
@Service
public class VendorMetricMappingRuleGovernanceServiceImpl implements VendorMetricMappingRuleGovernanceService {

    private static final String HIT_SOURCE_PUBLISHED_SNAPSHOT = "PUBLISHED_SNAPSHOT";
    private static final String HIT_SOURCE_DRAFT_RULE = "DRAFT_RULE";
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private final VendorMetricMappingRuleMapper ruleMapper;
    private final VendorMetricMappingRuleSnapshotMapper snapshotMapper;
    private final GovernanceApprovalPolicyResolver approvalPolicyResolver;
    private final GovernanceApprovalService governanceApprovalService;

    public VendorMetricMappingRuleGovernanceServiceImpl(VendorMetricMappingRuleMapper ruleMapper,
                                                        VendorMetricMappingRuleSnapshotMapper snapshotMapper,
                                                        GovernanceApprovalPolicyResolver approvalPolicyResolver,
                                                        GovernanceApprovalService governanceApprovalService) {
        this.ruleMapper = ruleMapper;
        this.snapshotMapper = snapshotMapper;
        this.approvalPolicyResolver = approvalPolicyResolver;
        this.governanceApprovalService = governanceApprovalService;
    }

    @Override
    public GovernanceSubmissionResultVO submitPublish(Long productId,
                                                      Long ruleId,
                                                      Long operatorUserId,
                                                      VendorMetricMappingRulePublishSubmitDTO dto) {
        VendorMetricMappingRule rule = requireRule(productId, ruleId);
        Long approverUserId = approvalPolicyResolver.resolveApproverUserId(
                VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_PUBLISH,
                operatorUserId
        );
        Long approvalOrderId = governanceApprovalService.submitAction(
                VendorMetricMappingRuleGovernanceApprovalPayloads.buildPublishCommand(
                        rule,
                        operatorUserId,
                        approverUserId,
                        dto == null ? null : dto.getSubmitReason()
                )
        );
        return GovernanceSubmissionResultVO.pendingApproval(null, approvalOrderId);
    }

    @Override
    public GovernanceSubmissionResultVO submitRollback(Long productId,
                                                       Long ruleId,
                                                       Long operatorUserId,
                                                       VendorMetricMappingRuleRollbackSubmitDTO dto) {
        VendorMetricMappingRule rule = requireRule(productId, ruleId);
        Long approverUserId = approvalPolicyResolver.resolveApproverUserId(
                VendorMetricMappingRuleGovernanceApprovalPayloads.ACTION_VENDOR_MAPPING_RULE_ROLLBACK,
                operatorUserId
        );
        Long approvalOrderId = governanceApprovalService.submitAction(
                VendorMetricMappingRuleGovernanceApprovalPayloads.buildRollbackCommand(
                        rule,
                        operatorUserId,
                        approverUserId,
                        dto == null ? null : dto.getSubmitReason()
                )
        );
        return GovernanceSubmissionResultVO.pendingApproval(null, approvalOrderId);
    }

    @Override
    public VendorMetricMappingRuleLedgerRowVO getLedgerRow(Long productId, Long ruleId) {
        VendorMetricMappingRule rule = requireRule(productId, ruleId);
        VendorMetricMappingRuleSnapshot snapshot = snapshotMapper.selectLatestPublishedByRuleId(ruleId);
        VendorMetricMappingRuleLedgerRowVO row = new VendorMetricMappingRuleLedgerRowVO();
        row.setRuleId(rule.getId());
        row.setProductId(rule.getProductId());
        row.setRawIdentifier(rule.getRawIdentifier());
        row.setTargetNormativeIdentifier(rule.getTargetNormativeIdentifier());
        row.setScopeType(rule.getScopeType());
        row.setDraftStatus(rule.getStatus());
        row.setDraftVersionNo(rule.getVersionNo());
        row.setPublishedStatus(snapshot == null ? null : snapshot.getLifecycleStatus());
        row.setPublishedVersionNo(snapshot == null ? null : snapshot.getPublishedVersionNo());
        row.setLatestApprovalOrderId(snapshot == null ? null : snapshot.getApprovalOrderId());
        row.setPublishedSource(snapshot == null ? "draft_table" : "published_snapshot");
        return row;
    }

    @Override
    public VendorMetricMappingRuleHitPreviewVO previewHit(Long productId, VendorMetricMappingRuleHitPreviewDTO dto) {
        String normalizedRawIdentifier = normalizeLower(dto == null ? null : dto.getRawIdentifier());
        String normalizedLogicalChannelCode = normalizeUpper(dto == null ? null : dto.getLogicalChannelCode());
        VendorMetricMappingRuleHitPreviewVO snapshotMatch = buildPreviewFromSnapshot(
                findPublishedSnapshot(productId, normalizedRawIdentifier, normalizedLogicalChannelCode)
        );
        if (snapshotMatch != null) {
            return snapshotMatch;
        }
        VendorMetricMappingRuleHitPreviewVO draftMatch = buildPreviewFromDraft(
                findDraftRule(productId, normalizedRawIdentifier, normalizedLogicalChannelCode)
        );
        if (draftMatch != null) {
            return draftMatch;
        }
        VendorMetricMappingRuleHitPreviewVO miss = new VendorMetricMappingRuleHitPreviewVO();
        miss.setMatched(Boolean.FALSE);
        miss.setHitSource("MISS");
        miss.setRawIdentifier(normalizedRawIdentifier);
        miss.setLogicalChannelCode(normalizedLogicalChannelCode);
        return miss;
    }

    private VendorMetricMappingRule requireRule(Long productId, Long ruleId) {
        if (ruleId == null || ruleId <= 0) {
            throw new BizException("厂商字段映射规则不存在: " + ruleId);
        }
        VendorMetricMappingRule rule = ruleMapper.selectById(ruleId);
        if (rule == null || Integer.valueOf(1).equals(rule.getDeleted()) || !Objects.equals(productId, rule.getProductId())) {
            throw new BizException("厂商字段映射规则不存在: " + ruleId);
        }
        return rule;
    }

    private VendorMetricMappingRuleSnapshot findPublishedSnapshot(Long productId,
                                                                  String rawIdentifier,
                                                                  String logicalChannelCode) {
        if (productId == null || productId <= 0 || rawIdentifier == null) {
            return null;
        }
        List<VendorMetricMappingRuleSnapshot> snapshots = snapshotMapper.selectPublishedByProductId(productId);
        if (snapshots == null || snapshots.isEmpty()) {
            return null;
        }
        return snapshots.stream()
                .filter(Objects::nonNull)
                .filter(snapshot -> matchesSnapshot(snapshot, rawIdentifier, logicalChannelCode))
                .sorted(Comparator
                        .comparing((VendorMetricMappingRuleSnapshot snapshot) -> exactLogicalMatch(snapshot, logicalChannelCode))
                        .reversed()
                        .thenComparing(VendorMetricMappingRuleSnapshot::getPublishedVersionNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(VendorMetricMappingRuleSnapshot::getId, Comparator.nullsLast(Long::compareTo)))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesSnapshot(VendorMetricMappingRuleSnapshot snapshot,
                                    String rawIdentifier,
                                    String logicalChannelCode) {
        SnapshotPayload payload = readSnapshotPayload(snapshot);
        if (payload == null || !Objects.equals(normalizeLower(payload.rawIdentifier), rawIdentifier)) {
            return false;
        }
        if (logicalChannelCode == null) {
            return true;
        }
        String payloadLogicalChannelCode = normalizeUpper(payload.logicalChannelCode);
        return payloadLogicalChannelCode == null || Objects.equals(payloadLogicalChannelCode, logicalChannelCode);
    }

    private boolean exactLogicalMatch(VendorMetricMappingRuleSnapshot snapshot, String logicalChannelCode) {
        if (logicalChannelCode == null) {
            return true;
        }
        SnapshotPayload payload = readSnapshotPayload(snapshot);
        return payload != null && Objects.equals(normalizeUpper(payload.logicalChannelCode), logicalChannelCode);
    }

    private VendorMetricMappingRule findDraftRule(Long productId,
                                                  String rawIdentifier,
                                                  String logicalChannelCode) {
        if (productId == null || productId <= 0 || rawIdentifier == null) {
            return null;
        }
        List<VendorMetricMappingRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<VendorMetricMappingRule>()
                .eq(VendorMetricMappingRule::getDeleted, 0)
                .eq(VendorMetricMappingRule::getProductId, productId)
                .eq(VendorMetricMappingRule::getRawIdentifier, rawIdentifier)
                .orderByDesc(VendorMetricMappingRule::getVersionNo)
                .orderByDesc(VendorMetricMappingRule::getId));
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        return rules.stream()
                .filter(Objects::nonNull)
                .filter(rule -> !SetHolder.DISABLED_STATUSES.contains(normalizeUpper(rule.getStatus())))
                .filter(rule -> matchesLogicalChannel(rule.getLogicalChannelCode(), logicalChannelCode))
                .sorted(Comparator
                        .comparing((VendorMetricMappingRule rule) -> exactLogicalMatch(rule.getLogicalChannelCode(), logicalChannelCode))
                        .reversed()
                        .thenComparing(VendorMetricMappingRule::getVersionNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(VendorMetricMappingRule::getId, Comparator.nullsLast(Long::compareTo)))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesLogicalChannel(String candidate, String requested) {
        if (requested == null) {
            return true;
        }
        String normalizedCandidate = normalizeUpper(candidate);
        return normalizedCandidate == null || Objects.equals(normalizedCandidate, requested);
    }

    private boolean exactLogicalMatch(String candidate, String requested) {
        if (requested == null) {
            return true;
        }
        return Objects.equals(normalizeUpper(candidate), requested);
    }

    private VendorMetricMappingRuleHitPreviewVO buildPreviewFromSnapshot(VendorMetricMappingRuleSnapshot snapshot) {
        SnapshotPayload payload = readSnapshotPayload(snapshot);
        if (snapshot == null || payload == null) {
            return null;
        }
        VendorMetricMappingRuleHitPreviewVO result = new VendorMetricMappingRuleHitPreviewVO();
        result.setMatched(Boolean.TRUE);
        result.setHitSource(HIT_SOURCE_PUBLISHED_SNAPSHOT);
        result.setRuleId(snapshot.getRuleId());
        result.setRawIdentifier(normalizeLower(payload.rawIdentifier));
        result.setLogicalChannelCode(normalizeUpper(payload.logicalChannelCode));
        result.setTargetNormativeIdentifier(normalizeText(payload.targetNormativeIdentifier));
        result.setPublishedVersionNo(snapshot.getPublishedVersionNo());
        result.setApprovalOrderId(snapshot.getApprovalOrderId());
        return result;
    }

    private VendorMetricMappingRuleHitPreviewVO buildPreviewFromDraft(VendorMetricMappingRule rule) {
        if (rule == null) {
            return null;
        }
        VendorMetricMappingRuleHitPreviewVO result = new VendorMetricMappingRuleHitPreviewVO();
        result.setMatched(Boolean.TRUE);
        result.setHitSource(HIT_SOURCE_DRAFT_RULE);
        result.setRuleId(rule.getId());
        result.setRawIdentifier(normalizeLower(rule.getRawIdentifier()));
        result.setLogicalChannelCode(normalizeUpper(rule.getLogicalChannelCode()));
        result.setTargetNormativeIdentifier(normalizeText(rule.getTargetNormativeIdentifier()));
        return result;
    }

    private SnapshotPayload readSnapshotPayload(VendorMetricMappingRuleSnapshot snapshot) {
        if (snapshot == null || !StringUtils.hasText(snapshot.getSnapshotJson())) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(snapshot.getSnapshotJson(), SnapshotPayload.class);
        } catch (Exception ex) {
            throw new BizException("厂商字段映射规则快照解析失败: " + snapshot.getId());
        }
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeLower(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeUpper(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static final class SetHolder {
        private static final java.util.Set<String> DISABLED_STATUSES = java.util.Set.of("INACTIVE", "DISABLED", "RETIRED");

        private SetHolder() {
        }
    }

    private static final class SnapshotPayload {
        public String rawIdentifier;
        public String logicalChannelCode;
        public String targetNormativeIdentifier;
    }
}
