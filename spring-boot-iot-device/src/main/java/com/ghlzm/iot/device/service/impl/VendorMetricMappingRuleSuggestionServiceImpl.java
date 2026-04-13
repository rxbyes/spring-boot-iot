package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.VendorMetricEvidenceMapper;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.VendorMetricMappingRuleSuggestionService;
import com.ghlzm.iot.device.service.VendorMetricMappingRuntimeService;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleSuggestionVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 厂商字段映射规则建议服务实现。
 */
@Service
public class VendorMetricMappingRuleSuggestionServiceImpl implements VendorMetricMappingRuleSuggestionService {

    private static final String RECOMMENDED_SCOPE_TYPE = "PRODUCT";
    private static final String STATUS_READY_TO_CREATE = "READY_TO_CREATE";
    private static final String STATUS_ALREADY_COVERED = "ALREADY_COVERED";
    private static final String STATUS_CONFLICTS_WITH_EXISTING = "CONFLICTS_WITH_EXISTING";
    private static final String STATUS_IGNORED_SAME_IDENTIFIER = "IGNORED_SAME_IDENTIFIER";
    private static final String STATUS_IGNORED_UNKNOWN_CANONICAL = "IGNORED_UNKNOWN_CANONICAL";
    private static final String STATUS_LOW_CONFIDENCE = "LOW_CONFIDENCE";
    private static final String CONFIDENCE_HIGH = "high";
    private static final String CONFIDENCE_MEDIUM = "medium";
    private static final String CONFIDENCE_LOW = "low";

    private final ProductMapper productMapper;
    private final VendorMetricEvidenceMapper evidenceMapper;
    private final PublishedProductContractSnapshotService snapshotService;
    private final NormativeMetricDefinitionService normativeMetricDefinitionService;
    private final VendorMetricMappingRuntimeService runtimeService;
    private final ProductModelNormativeMatcher normativeMatcher = new ProductModelNormativeMatcher();

    public VendorMetricMappingRuleSuggestionServiceImpl(ProductMapper productMapper,
                                                        VendorMetricEvidenceMapper evidenceMapper,
                                                        PublishedProductContractSnapshotService snapshotService,
                                                        NormativeMetricDefinitionService normativeMetricDefinitionService,
                                                        VendorMetricMappingRuntimeService runtimeService) {
        this.productMapper = productMapper;
        this.evidenceMapper = evidenceMapper;
        this.snapshotService = snapshotService;
        this.normativeMetricDefinitionService = normativeMetricDefinitionService;
        this.runtimeService = runtimeService;
    }

    @Override
    public List<VendorMetricMappingRuleSuggestionVO> listSuggestions(Long productId,
                                                                     boolean includeCovered,
                                                                     boolean includeIgnored,
                                                                     int minEvidenceCount) {
        Product product = requireProduct(productId);
        PublishedProductContractSnapshot snapshot = snapshotService == null
                ? PublishedProductContractSnapshot.empty(productId)
                : snapshotService.getRequiredSnapshot(productId);
        Map<String, String> normativeIdentifiers = loadNormativeIdentifiers(product);
        return loadEvidence(productId).stream()
                .map(evidence -> toSuggestion(product, snapshot, normativeIdentifiers, evidence))
                .filter(Objects::nonNull)
                .filter(item -> includeCovered || !STATUS_ALREADY_COVERED.equals(item.getStatus()))
                .filter(item -> includeIgnored || !isIgnoredStatus(item.getStatus()))
                .filter(item -> firstPositive(item.getEvidenceCount()) >= Math.max(1, minEvidenceCount))
                .sorted(suggestionComparator())
                .toList();
    }

    private Product requireProduct(Long productId) {
        Product product = productMapper == null || productId == null ? null : productMapper.selectById(productId);
        if (product == null) {
            throw new BizException("产品不存在: " + productId);
        }
        return product;
    }

    private List<VendorMetricEvidence> loadEvidence(Long productId) {
        if (evidenceMapper == null || productId == null) {
            return List.of();
        }
        List<VendorMetricEvidence> records = evidenceMapper.selectList(new LambdaQueryWrapper<VendorMetricEvidence>()
                .eq(VendorMetricEvidence::getDeleted, 0)
                .eq(VendorMetricEvidence::getProductId, productId)
                .orderByDesc(VendorMetricEvidence::getEvidenceCount)
                .orderByDesc(VendorMetricEvidence::getLastSeenTime)
                .orderByAsc(VendorMetricEvidence::getRawIdentifier));
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Map<String, VendorMetricEvidence> deduplicated = new LinkedHashMap<>();
        for (VendorMetricEvidence record : records) {
            if (record == null || !StringUtils.hasText(record.getRawIdentifier())) {
                continue;
            }
            String dedupeKey = normalizeLower(record.getRawIdentifier()) + "|" + normalizeUpper(record.getLogicalChannelCode());
            deduplicated.compute(dedupeKey, (key, existing) -> chooseBetter(existing, record));
        }
        return new ArrayList<>(deduplicated.values());
    }

    private VendorMetricEvidence chooseBetter(VendorMetricEvidence left, VendorMetricEvidence right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        if (firstPositive(right.getEvidenceCount()) > firstPositive(left.getEvidenceCount())) {
            return right;
        }
        if (firstPositive(right.getEvidenceCount()) < firstPositive(left.getEvidenceCount())) {
            return left;
        }
        LocalDateTime leftTime = left.getLastSeenTime();
        LocalDateTime rightTime = right.getLastSeenTime();
        if (leftTime == null) {
            return right;
        }
        if (rightTime == null) {
            return left;
        }
        return rightTime.isAfter(leftTime) ? right : left;
    }

    private Map<String, String> loadNormativeIdentifiers(Product product) {
        String scenarioCode = normativeMatcher.resolveScenarioCode(product);
        if (!StringUtils.hasText(scenarioCode) || normativeMetricDefinitionService == null) {
            return Map.of();
        }
        List<NormativeMetricDefinition> definitions = normativeMetricDefinitionService.listByScenario(scenarioCode);
        if (definitions == null || definitions.isEmpty()) {
            return Map.of();
        }
        return definitions.stream()
                .filter(Objects::nonNull)
                .map(NormativeMetricDefinition::getIdentifier)
                .filter(StringUtils::hasText)
                .collect(Collectors.toMap(
                        this::normalizeLower,
                        identifier -> identifier,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private VendorMetricMappingRuleSuggestionVO toSuggestion(Product product,
                                                            PublishedProductContractSnapshot snapshot,
                                                            Map<String, String> normativeIdentifiers,
                                                            VendorMetricEvidence evidence) {
        String rawIdentifier = normalizeText(evidence == null ? null : evidence.getRawIdentifier());
        String canonicalIdentifier = normalizeText(evidence == null ? null : evidence.getCanonicalIdentifier());
        if (!StringUtils.hasText(rawIdentifier)) {
            return null;
        }
        VendorMetricMappingRuleSuggestionVO suggestion = new VendorMetricMappingRuleSuggestionVO();
        suggestion.setRawIdentifier(rawIdentifier);
        suggestion.setLogicalChannelCode(normalizeText(evidence == null ? null : evidence.getLogicalChannelCode()));
        suggestion.setRecommendedScopeType(RECOMMENDED_SCOPE_TYPE);
        suggestion.setEvidenceCount(evidence == null ? null : evidence.getEvidenceCount());
        suggestion.setSampleValue(normalizeText(evidence == null ? null : evidence.getSampleValue()));
        suggestion.setValueType(normalizeText(evidence == null ? null : evidence.getValueType()));
        suggestion.setEvidenceOrigin(normalizeText(evidence == null ? null : evidence.getEvidenceOrigin()));
        suggestion.setLastSeenTime(evidence == null ? null : evidence.getLastSeenTime());

        if (equalsIgnoreCase(rawIdentifier, canonicalIdentifier)) {
            suggestion.setTargetNormativeIdentifier(canonicalIdentifier);
            suggestion.setStatus(STATUS_IGNORED_SAME_IDENTIFIER);
            suggestion.setConfidence(CONFIDENCE_LOW);
            suggestion.setReason("rawIdentifier 与 canonicalIdentifier 一致，无需创建映射规则");
            return suggestion;
        }

        ResolvedCanonical resolvedCanonical = resolveCanonicalIdentifier(snapshot, normativeIdentifiers, canonicalIdentifier);
        if (resolvedCanonical.identifier() == null) {
            suggestion.setTargetNormativeIdentifier(canonicalIdentifier);
            suggestion.setStatus(STATUS_IGNORED_UNKNOWN_CANONICAL);
            suggestion.setConfidence(CONFIDENCE_LOW);
            suggestion.setReason("canonicalIdentifier 未命中已发布快照或规范字段");
            return suggestion;
        }
        suggestion.setTargetNormativeIdentifier(resolvedCanonical.identifier());

        try {
            VendorMetricMappingRuntimeService.MappingResolution resolution =
                    runtimeService == null ? null : runtimeService.resolveForGovernance(
                            product,
                            rawIdentifier,
                            suggestion.getLogicalChannelCode()
                    );
            if (resolution != null) {
                suggestion.setExistingRuleId(resolution.ruleId());
                suggestion.setExistingTargetNormativeIdentifier(normalizeText(resolution.targetNormativeIdentifier()));
                if (equalsIgnoreCase(resolution.targetNormativeIdentifier(), resolvedCanonical.identifier())) {
                    suggestion.setStatus(STATUS_ALREADY_COVERED);
                    suggestion.setConfidence(resolveConfidence(resolvedCanonical.source(), evidence == null ? null : evidence.getEvidenceCount()));
                    suggestion.setReason("当前产品已命中同目标映射规则");
                    return suggestion;
                }
                suggestion.setStatus(STATUS_CONFLICTS_WITH_EXISTING);
                suggestion.setConfidence(CONFIDENCE_LOW);
                suggestion.setReason("当前产品已命中不同目标映射规则");
                return suggestion;
            }
        } catch (BizException ex) {
            suggestion.setStatus(STATUS_CONFLICTS_WITH_EXISTING);
            suggestion.setConfidence(CONFIDENCE_LOW);
            suggestion.setReason(normalizeText(ex.getMessage()));
            return suggestion;
        }

        suggestion.setConfidence(resolveConfidence(resolvedCanonical.source(), evidence == null ? null : evidence.getEvidenceCount()));
        if (firstPositive(evidence == null ? null : evidence.getEvidenceCount()) <= 1) {
            suggestion.setStatus(STATUS_LOW_CONFIDENCE);
            suggestion.setReason("证据次数不足，建议继续观察");
            return suggestion;
        }
        suggestion.setStatus(STATUS_READY_TO_CREATE);
        suggestion.setReason("已命中正式 canonical，且当前未发现覆盖规则");
        return suggestion;
    }

    private ResolvedCanonical resolveCanonicalIdentifier(PublishedProductContractSnapshot snapshot,
                                                         Map<String, String> normativeIdentifiers,
                                                         String canonicalIdentifier) {
        String normalizedIdentifier = normalizeText(canonicalIdentifier);
        if (!StringUtils.hasText(normalizedIdentifier)) {
            return new ResolvedCanonical(null, "NONE");
        }
        if (snapshot != null) {
            String snapshotCanonical = snapshot.canonicalAliasOf(normalizedIdentifier).orElse(null);
            if (StringUtils.hasText(snapshotCanonical)) {
                return new ResolvedCanonical(snapshotCanonical, "SNAPSHOT");
            }
            if (snapshot.containsPublishedIdentifier(normalizedIdentifier)) {
                return new ResolvedCanonical(normalizedIdentifier, "SNAPSHOT");
            }
        }
        String normativeCanonical = normativeIdentifiers == null ? null : normativeIdentifiers.get(normalizeLower(normalizedIdentifier));
        if (StringUtils.hasText(normativeCanonical)) {
            return new ResolvedCanonical(normativeCanonical, "NORMATIVE");
        }
        return new ResolvedCanonical(null, "NONE");
    }

    private String resolveConfidence(String canonicalSource, Integer evidenceCount) {
        int count = firstPositive(evidenceCount);
        if ("SNAPSHOT".equals(canonicalSource) && count >= 3) {
            return CONFIDENCE_HIGH;
        }
        if ("NORMATIVE".equals(canonicalSource) && count >= 2) {
            return CONFIDENCE_MEDIUM;
        }
        return CONFIDENCE_LOW;
    }

    private Comparator<VendorMetricMappingRuleSuggestionVO> suggestionComparator() {
        return Comparator
                .comparingInt((VendorMetricMappingRuleSuggestionVO item) -> statusOrder(item.getStatus()))
                .thenComparingInt(item -> confidenceOrder(item.getConfidence()))
                .thenComparing((VendorMetricMappingRuleSuggestionVO item) -> firstPositive(item.getEvidenceCount()), Comparator.reverseOrder())
                .thenComparing(VendorMetricMappingRuleSuggestionVO::getLastSeenTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(item -> normalizeLower(item.getRawIdentifier()), Comparator.nullsLast(String::compareTo));
    }

    private int statusOrder(String status) {
        if (STATUS_READY_TO_CREATE.equals(status)) {
            return 1;
        }
        if (STATUS_CONFLICTS_WITH_EXISTING.equals(status)) {
            return 2;
        }
        if (STATUS_LOW_CONFIDENCE.equals(status)) {
            return 3;
        }
        if (STATUS_ALREADY_COVERED.equals(status)) {
            return 4;
        }
        return 5;
    }

    private int confidenceOrder(String confidence) {
        if (CONFIDENCE_HIGH.equalsIgnoreCase(confidence)) {
            return 1;
        }
        if (CONFIDENCE_MEDIUM.equalsIgnoreCase(confidence)) {
            return 2;
        }
        return 3;
    }

    private boolean isIgnoredStatus(String status) {
        return StringUtils.hasText(status) && status.startsWith("IGNORED_");
    }

    private int firstPositive(Integer value) {
        return value == null || value <= 0 ? 0 : value;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        String normalizedLeft = normalizeText(left);
        String normalizedRight = normalizeText(right);
        if (normalizedLeft == null && normalizedRight == null) {
            return true;
        }
        if (normalizedLeft == null || normalizedRight == null) {
            return false;
        }
        return normalizedLeft.equalsIgnoreCase(normalizedRight);
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

    private record ResolvedCanonical(String identifier, String source) {
    }
}
