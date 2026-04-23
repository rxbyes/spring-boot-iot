package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.entity.GovernanceWorkItem;
import com.ghlzm.iot.system.service.GovernancePriorityScorer;
import com.ghlzm.iot.system.service.model.GovernanceImpactSnapshot;
import com.ghlzm.iot.system.service.model.GovernanceRecommendationSnapshot;
import com.ghlzm.iot.system.service.model.GovernanceRollbackSnapshot;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemCommand;
import com.ghlzm.iot.system.vo.GovernanceDecisionContextVO;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Service
public class GovernancePriorityScorerImpl implements GovernancePriorityScorer {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private static final int RELEASE_IMPACT_WEIGHT = 12;
    private static final int AGING_WEIGHT = 2;
    private static final int BLOCKING_WEIGHT = 10;
    private static final int REPLAY_WEIGHT = 8;
    private static final int LOW_BINDING_BONUS = 16;
    private static final int HIGH_IMPACT_RELEASE_BONUS = 24;
    private static final int MISSING_POLICY_BONUS = 12;
    private static final int REPLAY_SIGNAL_BONUS = 8;

    @Override
    public GovernanceDecisionContextVO buildDecisionContext(GovernanceWorkItemCommand command) {
        return buildDecisionContext(PrioritySource.from(command));
    }

    @Override
    public GovernanceDecisionContextVO buildDecisionContext(GovernanceWorkItem workItem) {
        GovernanceDecisionContextVO context = buildDecisionContext(PrioritySource.from(workItem));
        if (workItem != null) {
            context.setWorkItemId(workItem.getId());
            context.setPriorityLevel(strongerPriority(context.getPriorityLevel(), normalize(workItem.getPriorityLevel())));
        }
        return context;
    }

    private GovernanceDecisionContextVO buildDecisionContext(PrioritySource source) {
        JsonNode snapshotRoot = readJson(source.snapshotJson());
        GovernanceRecommendationSnapshot recommendation = GovernanceSnapshotResolver.resolveRecommendation(
                source.recommendationSnapshotJson(),
                source.evidenceSnapshotJson(),
                source.snapshotJson(),
                defaultRecommendationType(source),
                defaultSuggestedAction(source)
        );
        GovernanceImpactSnapshot impact = GovernanceSnapshotResolver.resolveImpact(
                source.impactSnapshotJson(),
                source.rollbackSnapshotJson(),
                source.snapshotJson(),
                null,
                defaultAffectedType(source)
        );
        GovernanceRollbackSnapshot rollback = GovernanceSnapshotResolver.resolveRollback(
                source.rollbackSnapshotJson(),
                source.impactSnapshotJson(),
                source.snapshotJson()
        );

        long missingBindingCount = longValue(snapshotRoot, "missingBindingCount");
        long missingPolicyCount = longValue(snapshotRoot, "missingPolicyCount");
        long missingRiskMetricCount = longValue(snapshotRoot, "missingRiskMetricCount");
        long affectedCount = resolveAffectedCount(impact, snapshotRoot, missingBindingCount, missingPolicyCount, missingRiskMetricCount);
        long overdueHours = resolveOverdueHours(source.dueTime());
        long blockingCount = resolveBlockingCount(source, snapshotRoot);
        long failureSignalCount = resolveFailureSignalCount(source, snapshotRoot, missingBindingCount, missingPolicyCount, missingRiskMetricCount);

        LinkedHashSet<String> reasonCodes = new LinkedHashSet<>(safeReasonCodes(recommendation));
        if (isRiskBinding(source) || missingBindingCount > 0L) {
            reasonCodes.add("LOW_BINDING_COVERAGE");
        }
        if (isContractRelease(source) && affectedCount >= 3L) {
            reasonCodes.add("HIGH_IMPACT_RELEASE");
        }
        if (isThresholdPolicy(source) || missingPolicyCount > 0L) {
            reasonCodes.add("MISSING_POLICY");
        }
        if (isReplay(source) || failureSignalCount > 0L) {
            reasonCodes.add("REPLAY_SIGNAL_DETECTED");
        }
        if (blockingCount > 0L) {
            reasonCodes.add("BLOCKED_BY_OPERATOR");
        }
        if (overdueHours > 0L) {
            reasonCodes.add("OVERDUE");
        }
        if (reasonCodes.isEmpty()) {
            reasonCodes.add("GENERAL_GOVERNANCE_REVIEW");
        }

        LinkedHashSet<String> affectedModules = resolveAffectedModules(source, impact, snapshotRoot, missingBindingCount, missingPolicyCount);
        int priorityScore = computePriorityScore(affectedCount, overdueHours, blockingCount, failureSignalCount, reasonCodes);

        GovernanceDecisionContextVO context = new GovernanceDecisionContextVO();
        context.setPriorityLevel(strongerPriority(priorityLevelForScore(priorityScore), normalize(source.priorityLevel())));
        context.setPriorityScore(priorityScore);
        context.setProblemSummary(resolveProblemSummary(source, recommendation));
        context.setReasonCodes(List.copyOf(reasonCodes));
        context.setAffectedModules(List.copyOf(affectedModules));
        context.setAffectedCount(affectedCount > 0L ? affectedCount : null);
        context.setRecommendedAction(resolveRecommendedAction(source, recommendation));
        context.setRollbackable(resolveRollbackable(impact, rollback));
        context.setRollbackPlanSummary(resolveRollbackPlanSummary(impact, rollback));
        return context;
    }

    private int computePriorityScore(long affectedCount,
                                     long overdueHours,
                                     long blockingCount,
                                     long failureSignalCount,
                                     Set<String> reasonCodes) {
        long score = affectedCount * RELEASE_IMPACT_WEIGHT
                + overdueHours * AGING_WEIGHT
                + blockingCount * BLOCKING_WEIGHT
                + failureSignalCount * REPLAY_WEIGHT;
        if (reasonCodes.contains("LOW_BINDING_COVERAGE")) {
            score += LOW_BINDING_BONUS;
        }
        if (reasonCodes.contains("HIGH_IMPACT_RELEASE")) {
            score += HIGH_IMPACT_RELEASE_BONUS;
        }
        if (reasonCodes.contains("MISSING_POLICY")) {
            score += MISSING_POLICY_BONUS;
        }
        if (reasonCodes.contains("REPLAY_SIGNAL_DETECTED")) {
            score += REPLAY_SIGNAL_BONUS;
        }
        return score > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) score;
    }

    private LinkedHashSet<String> resolveAffectedModules(PrioritySource source,
                                                         GovernanceImpactSnapshot impact,
                                                         JsonNode snapshotRoot,
                                                         long missingBindingCount,
                                                         long missingPolicyCount) {
        LinkedHashSet<String> modules = new LinkedHashSet<>();
        if (impact != null && impact.getAffectedTypes() != null) {
            for (String affectedType : impact.getAffectedTypes()) {
                addAffectedModule(modules, affectedType);
            }
        }
        if (source.productId() != null || source.releaseBatchId() != null) {
            modules.add("PRODUCT");
        }
        if (isRiskBinding(source)
                || missingBindingCount > 0L
                || hasTextValue(snapshotRoot, "riskPointId")
                || longValue(snapshotRoot, "riskPointCount") > 0L
                || containsIgnoreCase(source.subjectType(), "risk")) {
            modules.add("RISK_POINT");
        }
        if (source.riskMetricId() != null
                || isThresholdPolicy(source)
                || isLinkagePlan(source)
                || missingPolicyCount > 0L) {
            modules.add("RULE");
        }
        if (modules.isEmpty()) {
            addAffectedModule(modules, source.subjectType());
            addAffectedModule(modules, source.taskCategory());
        }
        if (modules.isEmpty()) {
            modules.add("PRODUCT");
        }
        return modules;
    }

    private void addAffectedModule(Set<String> modules, String rawValue) {
        String value = normalizeUpper(rawValue);
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (value.contains("PRODUCT")) {
            modules.add("PRODUCT");
            return;
        }
        if (value.contains("RISK") || value.contains("BINDING")) {
            modules.add("RISK_POINT");
            return;
        }
        if (value.contains("RULE") || value.contains("POLICY") || value.contains("LINKAGE") || value.contains("EMERGENCY")) {
            modules.add("RULE");
        }
    }

    private long resolveAffectedCount(GovernanceImpactSnapshot impact,
                                      JsonNode snapshotRoot,
                                      long missingBindingCount,
                                      long missingPolicyCount,
                                      long missingRiskMetricCount) {
        if (impact != null && impact.getAffectedCount() != null && impact.getAffectedCount() > 0L) {
            return impact.getAffectedCount();
        }
        long explicit = longValue(snapshotRoot, "affectedCount");
        if (explicit > 0L) {
            return explicit;
        }
        long derived = missingBindingCount
                + missingPolicyCount
                + missingRiskMetricCount
                + longValue(snapshotRoot, "bindingCount")
                + longValue(snapshotRoot, "riskPointCount");
        return Math.max(derived, 0L);
    }

    private long resolveFailureSignalCount(PrioritySource source,
                                           JsonNode snapshotRoot,
                                           long missingBindingCount,
                                           long missingPolicyCount,
                                           long missingRiskMetricCount) {
        long explicit = longValue(snapshotRoot, "failureSignalCount");
        if (explicit > 0L) {
            return explicit;
        }
        long replaySignals = longValue(snapshotRoot, "matchedAccessErrorCount")
                + longValue(snapshotRoot, "matchedMessageCount");
        if (replaySignals > 0L) {
            return replaySignals;
        }
        if (isReplay(source)) {
            return Math.max(1L, longValue(snapshotRoot, "bindingCount") + missingPolicyCount + missingRiskMetricCount);
        }
        return missingBindingCount + missingPolicyCount + missingRiskMetricCount;
    }

    private long resolveBlockingCount(PrioritySource source, JsonNode snapshotRoot) {
        long explicit = longValue(snapshotRoot, "blockingCount");
        if (explicit > 0L) {
            return explicit;
        }
        return "BLOCKED".equalsIgnoreCase(normalize(source.workStatus())) ? 1L : 0L;
    }

    private long resolveOverdueHours(Date dueTime) {
        if (dueTime == null) {
            return 0L;
        }
        long delta = System.currentTimeMillis() - dueTime.getTime();
        return delta <= 0L ? 0L : delta / 3_600_000L;
    }

    private String resolveProblemSummary(PrioritySource source, GovernanceRecommendationSnapshot recommendation) {
        String blockingReason = normalize(source.blockingReason());
        if (StringUtils.hasText(blockingReason)) {
            return blockingReason;
        }
        String suggestedAction = recommendation == null ? null : normalize(recommendation.getSuggestedAction());
        if (StringUtils.hasText(suggestedAction)) {
            return suggestedAction;
        }
        return defaultSuggestedAction(source);
    }

    private String resolveRecommendedAction(PrioritySource source, GovernanceRecommendationSnapshot recommendation) {
        String suggestedAction = recommendation == null ? null : normalize(recommendation.getSuggestedAction());
        return StringUtils.hasText(suggestedAction) ? suggestedAction : defaultSuggestedAction(source);
    }

    private Boolean resolveRollbackable(GovernanceImpactSnapshot impact, GovernanceRollbackSnapshot rollback) {
        if (impact != null && impact.getRollbackable() != null) {
            return impact.getRollbackable();
        }
        return rollback == null ? null : rollback.getRollbackable();
    }

    private String resolveRollbackPlanSummary(GovernanceImpactSnapshot impact, GovernanceRollbackSnapshot rollback) {
        String impactSummary = impact == null ? null : normalize(impact.getRollbackPlanSummary());
        if (StringUtils.hasText(impactSummary)) {
            return impactSummary;
        }
        return rollback == null ? null : normalize(rollback.getRollbackPlanSummary());
    }

    private String priorityLevelForScore(int priorityScore) {
        if (priorityScore >= 80) {
            return "P1";
        }
        if (priorityScore >= 35) {
            return "P2";
        }
        return "P3";
    }

    private String strongerPriority(String left, String right) {
        String normalizedLeft = normalizeUpper(left);
        String normalizedRight = normalizeUpper(right);
        if (!StringUtils.hasText(normalizedLeft)) {
            return normalizedRight;
        }
        if (!StringUtils.hasText(normalizedRight)) {
            return normalizedLeft;
        }
        return priorityRank(normalizedLeft) <= priorityRank(normalizedRight) ? normalizedLeft : normalizedRight;
    }

    private int priorityRank(String priorityLevel) {
        String normalized = normalizeUpper(priorityLevel);
        if ("P0".equals(normalized)) {
            return 0;
        }
        if ("P1".equals(normalized)) {
            return 1;
        }
        if ("P2".equals(normalized)) {
            return 2;
        }
        if ("P3".equals(normalized)) {
            return 3;
        }
        return 9;
    }

    private String defaultRecommendationType(PrioritySource source) {
        String workItemCode = normalizeUpper(source.workItemCode());
        if ("PENDING_RISK_BINDING".equals(workItemCode)) {
            return "PROMOTE";
        }
        if ("PENDING_CONTRACT_RELEASE".equals(workItemCode)) {
            return "PUBLISH";
        }
        if ("PENDING_REPLAY".equals(workItemCode)) {
            return "REPLAY";
        }
        if ("PENDING_THRESHOLD_POLICY".equals(workItemCode) || "PENDING_LINKAGE_PLAN".equals(workItemCode)) {
            return "CREATE_POLICY";
        }
        return "IGNORE";
    }

    private String defaultSuggestedAction(PrioritySource source) {
        String blockingReason = normalize(source.blockingReason());
        if (StringUtils.hasText(blockingReason)) {
            return blockingReason;
        }
        String actionCode = normalize(source.actionCode());
        if (StringUtils.hasText(actionCode)) {
            return actionCode;
        }
        return defaultRecommendationType(source);
    }

    private String defaultAffectedType(PrioritySource source) {
        String subjectType = normalize(source.subjectType());
        if (StringUtils.hasText(subjectType)) {
            return subjectType;
        }
        return normalize(source.taskCategory());
    }

    private boolean isRiskBinding(PrioritySource source) {
        return "PENDING_RISK_BINDING".equals(normalizeUpper(source.workItemCode()))
                || "RISK_BINDING".equals(normalizeUpper(source.taskCategory()));
    }

    private boolean isContractRelease(PrioritySource source) {
        return "PENDING_CONTRACT_RELEASE".equals(normalizeUpper(source.workItemCode()))
                || "CONTRACT_RELEASE".equals(normalizeUpper(source.taskCategory()));
    }

    private boolean isThresholdPolicy(PrioritySource source) {
        return "PENDING_THRESHOLD_POLICY".equals(normalizeUpper(source.workItemCode()))
                || "THRESHOLD_POLICY".equals(normalizeUpper(source.taskCategory()));
    }

    private boolean isLinkagePlan(PrioritySource source) {
        return "PENDING_LINKAGE_PLAN".equals(normalizeUpper(source.workItemCode()))
                || "LINKAGE_PLAN".equals(normalizeUpper(source.taskCategory()));
    }

    private boolean isReplay(PrioritySource source) {
        return "PENDING_REPLAY".equals(normalizeUpper(source.workItemCode()))
                || "REPLAY".equals(normalizeUpper(source.taskCategory()));
    }

    private List<String> safeReasonCodes(GovernanceRecommendationSnapshot recommendation) {
        if (recommendation == null || recommendation.getReasonCodes() == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String reasonCode : recommendation.getReasonCodes()) {
            String normalized = normalizeUpper(reasonCode);
            if (StringUtils.hasText(normalized)) {
                values.add(normalized);
            }
        }
        return values;
    }

    private long longValue(JsonNode root, String fieldName) {
        if (root == null || !StringUtils.hasText(fieldName)) {
            return 0L;
        }
        JsonNode node = root.path(fieldName);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0L;
        }
        if (node.isNumber()) {
            return Math.max(0L, node.asLong());
        }
        if (node.isTextual()) {
            try {
                return Math.max(0L, Long.parseLong(node.asText().trim()));
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private boolean hasTextValue(JsonNode root, String fieldName) {
        if (root == null || !StringUtils.hasText(fieldName)) {
            return false;
        }
        JsonNode node = root.path(fieldName);
        return node != null && !node.isMissingNode() && !node.isNull() && StringUtils.hasText(node.asText());
    }

    private JsonNode readJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUpper(String value) {
        String normalized = normalize(value);
        return StringUtils.hasText(normalized) ? normalized.toUpperCase(Locale.ROOT) : null;
    }

    private boolean containsIgnoreCase(String value, String fragment) {
        if (!StringUtils.hasText(value) || !StringUtils.hasText(fragment)) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(fragment.toLowerCase(Locale.ROOT));
    }

    private record PrioritySource(String workItemCode,
                                  String taskCategory,
                                  String subjectType,
                                  Long productId,
                                  Long riskMetricId,
                                  Long releaseBatchId,
                                  String workStatus,
                                  String priorityLevel,
                                  String blockingReason,
                                  String snapshotJson,
                                  String actionCode,
                                  String recommendationSnapshotJson,
                                  String evidenceSnapshotJson,
                                  String impactSnapshotJson,
                                  String rollbackSnapshotJson,
                                  Date dueTime) {

        private static PrioritySource from(GovernanceWorkItemCommand command) {
            return new PrioritySource(
                    command == null ? null : command.workItemCode(),
                    command == null ? null : command.taskCategory(),
                    command == null ? null : command.subjectType(),
                    command == null ? null : command.productId(),
                    command == null ? null : command.riskMetricId(),
                    command == null ? null : command.releaseBatchId(),
                    "OPEN",
                    command == null ? null : command.priorityLevel(),
                    command == null ? null : command.blockingReason(),
                    command == null ? null : command.snapshotJson(),
                    command == null ? null : command.actionCode(),
                    command == null ? null : command.recommendationSnapshotJson(),
                    command == null ? null : command.evidenceSnapshotJson(),
                    command == null ? null : command.impactSnapshotJson(),
                    command == null ? null : command.rollbackSnapshotJson(),
                    null
            );
        }

        private static PrioritySource from(GovernanceWorkItem workItem) {
            if (workItem == null) {
                return new PrioritySource(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
            }
            return new PrioritySource(
                    workItem.getWorkItemCode(),
                    workItem.getTaskCategory(),
                    workItem.getSubjectType(),
                    workItem.getProductId(),
                    workItem.getRiskMetricId(),
                    workItem.getReleaseBatchId(),
                    workItem.getWorkStatus(),
                    workItem.getPriorityLevel(),
                    workItem.getBlockingReason(),
                    workItem.getSnapshotJson(),
                    workItem.getActionCode(),
                    workItem.getRecommendationSnapshotJson(),
                    workItem.getEvidenceSnapshotJson(),
                    workItem.getImpactSnapshotJson(),
                    workItem.getRollbackSnapshotJson(),
                    workItem.getDueTime()
            );
        }
    }
}
