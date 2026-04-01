package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.vo.ProductModelCandidateResultVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateSummaryVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceCompareRowVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceCompareVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceEvidenceVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceSummaryVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 产品物模型双证据治理对比器。
 */
public class ProductModelGovernanceComparator {

    private static final Map<String, Integer> MODEL_TYPE_ORDER = Map.of(
            "property", 1,
            "event", 2,
            "service", 3
    );
    private static final String STATUS_DOUBLE_ALIGNED = "double_aligned";
    private static final String STATUS_MANUAL_ONLY = "manual_only";
    private static final String STATUS_RUNTIME_ONLY = "runtime_only";
    private static final String STATUS_FORMAL_EXISTS = "formal_exists";
    private static final String STATUS_SUSPECTED_CONFLICT = "suspected_conflict";
    private static final String STATUS_EVIDENCE_INSUFFICIENT = "evidence_insufficient";

    public ProductModelGovernanceCompareVO compare(Long productId,
                                                   List<ProductModel> formalModels,
                                                   ProductModelCandidateResultVO manualResult,
                                                   ProductModelCandidateResultVO runtimeResult) {
        Map<String, ProductModelGovernanceEvidenceVO> manualMap = toCandidateEvidenceMap(manualResult);
        Map<String, ProductModelGovernanceEvidenceVO> runtimeMap = toCandidateEvidenceMap(runtimeResult);
        Map<String, ProductModelGovernanceEvidenceVO> formalMap = toFormalEvidenceMap(formalModels);
        Map<String, List<String>> suspectedMatches = buildSuspectedMatches(manualMap, runtimeMap, formalMap);

        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(formalMap.keySet());
        keys.addAll(manualMap.keySet());
        keys.addAll(runtimeMap.keySet());

        List<ProductModelGovernanceCompareRowVO> rows = keys.stream()
                .map(key -> buildRow(key, manualMap.get(key), runtimeMap.get(key), formalMap.get(key), suspectedMatches.get(key)))
                .sorted(Comparator
                        .comparing((ProductModelGovernanceCompareRowVO row) -> MODEL_TYPE_ORDER.getOrDefault(row.getModelType(), 99))
                        .thenComparing(ProductModelGovernanceCompareRowVO::getIdentifier, Comparator.nullsLast(String::compareTo)))
                .toList();

        ProductModelGovernanceCompareVO compareVO = new ProductModelGovernanceCompareVO();
        compareVO.setProductId(productId);
        compareVO.setSummary(buildOverallSummary(rows));
        compareVO.setManualSummary(copySummary(manualResult == null ? null : manualResult.getSummary()));
        compareVO.setRuntimeSummary(copySummary(runtimeResult == null ? null : runtimeResult.getSummary()));
        compareVO.setFormalSummary(buildFormalSummary(formalModels));
        compareVO.setCompareRows(rows);
        return compareVO;
    }

    private ProductModelGovernanceCompareRowVO buildRow(String key,
                                                        ProductModelGovernanceEvidenceVO manual,
                                                        ProductModelGovernanceEvidenceVO runtime,
                                                        ProductModelGovernanceEvidenceVO formal,
                                                        List<String> suspectedMatches) {
        String[] segments = splitKey(key);
        ProductModelGovernanceCompareRowVO row = new ProductModelGovernanceCompareRowVO();
        row.setModelType(segments[0]);
        row.setIdentifier(segments[1]);
        row.setManualCandidate(manual);
        row.setRuntimeCandidate(runtime);
        row.setFormalModel(formal);
        row.setCompareStatus(resolveCompareStatus(manual, runtime, formal));
        row.setSuggestedAction(resolveSuggestedAction(row.getCompareStatus()));
        row.setRiskFlags(resolveRiskFlags(manual, runtime, formal, row.getCompareStatus(), suspectedMatches));
        row.setSuspectedMatches(suspectedMatches == null ? List.of() : suspectedMatches);
        return row;
    }

    private String resolveCompareStatus(ProductModelGovernanceEvidenceVO manual,
                                        ProductModelGovernanceEvidenceVO runtime,
                                        ProductModelGovernanceEvidenceVO formal) {
        if (definitionMismatch(manual, runtime, formal)) {
            return STATUS_SUSPECTED_CONFLICT;
        }
        if (formal != null) {
            return STATUS_FORMAL_EXISTS;
        }
        if (manual != null && runtime != null) {
            return STATUS_DOUBLE_ALIGNED;
        }
        if (manual != null) {
            return STATUS_MANUAL_ONLY;
        }
        if (runtime != null) {
            return STATUS_RUNTIME_ONLY;
        }
        return STATUS_EVIDENCE_INSUFFICIENT;
    }

    private boolean definitionMismatch(ProductModelGovernanceEvidenceVO manual,
                                       ProductModelGovernanceEvidenceVO runtime,
                                       ProductModelGovernanceEvidenceVO formal) {
        List<ProductModelGovernanceEvidenceVO> evidences = new ArrayList<>();
        if (manual != null) {
            evidences.add(manual);
        }
        if (runtime != null) {
            evidences.add(runtime);
        }
        if (formal != null) {
            evidences.add(formal);
        }
        if (evidences.size() < 2) {
            return false;
        }
        String modelType = firstModelType(evidences);
        if ("property".equals(modelType)) {
            return hasConflictingValues(evidences.stream().map(ProductModelGovernanceEvidenceVO::getDataType).toList())
                    || hasConflictingValues(evidences.stream().map(ProductModelGovernanceEvidenceVO::getSpecsJson).toList());
        }
        if ("event".equals(modelType)) {
            return hasConflictingValues(evidences.stream().map(ProductModelGovernanceEvidenceVO::getEventType).toList());
        }
        if ("service".equals(modelType)) {
            return hasConflictingValues(evidences.stream().map(ProductModelGovernanceEvidenceVO::getServiceInputJson).toList())
                    || hasConflictingValues(evidences.stream().map(ProductModelGovernanceEvidenceVO::getServiceOutputJson).toList());
        }
        return false;
    }

    private boolean hasConflictingValues(Collection<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (normalize(value) != null) {
                normalized.add(normalize(value));
            }
        }
        return normalized.size() > 1;
    }

    private String firstModelType(List<ProductModelGovernanceEvidenceVO> evidences) {
        return evidences.stream()
                .map(ProductModelGovernanceEvidenceVO::getModelType)
                .map(this::normalize)
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }

    private String resolveSuggestedAction(String compareStatus) {
        return switch (compareStatus) {
            case STATUS_DOUBLE_ALIGNED -> "纳入新增";
            case STATUS_MANUAL_ONLY, STATUS_RUNTIME_ONLY, STATUS_EVIDENCE_INSUFFICIENT -> "继续观察";
            case STATUS_FORMAL_EXISTS -> "忽略";
            case STATUS_SUSPECTED_CONFLICT -> "人工裁决";
            default -> "继续观察";
        };
    }

    private List<String> resolveRiskFlags(ProductModelGovernanceEvidenceVO manual,
                                          ProductModelGovernanceEvidenceVO runtime,
                                          ProductModelGovernanceEvidenceVO formal,
                                          String compareStatus,
                                          List<String> suspectedMatches) {
        List<String> riskFlags = new ArrayList<>();
        if (STATUS_SUSPECTED_CONFLICT.equals(compareStatus)) {
            riskFlags.add("definition_mismatch");
        }
        if (Boolean.TRUE.equals(manual == null ? null : manual.getNeedsReview())
                || Boolean.TRUE.equals(runtime == null ? null : runtime.getNeedsReview())) {
            riskFlags.add("needs_review");
        }
        if (formal != null) {
            riskFlags.add("formal_baseline");
        }
        if (manual == null) {
            riskFlags.add("manual_missing");
        }
        if (runtime == null) {
            riskFlags.add("runtime_missing");
        }
        if (suspectedMatches != null && !suspectedMatches.isEmpty()) {
            riskFlags.add("suspected_match");
        }
        return riskFlags;
    }

    private Map<String, ProductModelGovernanceEvidenceVO> toCandidateEvidenceMap(ProductModelCandidateResultVO result) {
        Map<String, ProductModelGovernanceEvidenceVO> map = new LinkedHashMap<>();
        if (result == null) {
            return map;
        }
        List<ProductModelCandidateVO> candidates = new ArrayList<>();
        candidates.addAll(safeList(result.getPropertyCandidates()));
        candidates.addAll(safeList(result.getEventCandidates()));
        candidates.addAll(safeList(result.getServiceCandidates()));
        for (ProductModelCandidateVO candidate : candidates) {
            String key = toKey(candidate.getModelType(), candidate.getIdentifier());
            if (key == null) {
                continue;
            }
            map.put(key, toEvidence(candidate));
        }
        return map;
    }

    private Map<String, ProductModelGovernanceEvidenceVO> toFormalEvidenceMap(List<ProductModel> models) {
        Map<String, ProductModelGovernanceEvidenceVO> map = new LinkedHashMap<>();
        for (ProductModel model : safeList(models)) {
            String key = toKey(model.getModelType(), model.getIdentifier());
            if (key == null) {
                continue;
            }
            map.put(key, toEvidence(model));
        }
        return map;
    }

    private Map<String, List<String>> buildSuspectedMatches(Map<String, ProductModelGovernanceEvidenceVO> manualMap,
                                                            Map<String, ProductModelGovernanceEvidenceVO> runtimeMap,
                                                            Map<String, ProductModelGovernanceEvidenceVO> formalMap) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (String key : unionKeys(manualMap, runtimeMap, formalMap)) {
            String[] segments = splitKey(key);
            String canonical = segments[0] + "::" + canonicalIdentifier(segments[1]);
            groups.computeIfAbsent(canonical, ignored -> new ArrayList<>()).add(key);
        }
        for (List<String> group : groups.values()) {
            if (group.size() < 2) {
                continue;
            }
            for (String key : group) {
                List<String> others = group.stream()
                        .filter(candidate -> !candidate.equals(key))
                        .map(candidate -> splitKey(candidate)[1])
                        .distinct()
                        .toList();
                if (!others.isEmpty()) {
                    result.put(key, others);
                }
            }
        }
        return result;
    }

    private Set<String> unionKeys(Map<String, ProductModelGovernanceEvidenceVO> manualMap,
                                  Map<String, ProductModelGovernanceEvidenceVO> runtimeMap,
                                  Map<String, ProductModelGovernanceEvidenceVO> formalMap) {
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(manualMap.keySet());
        keys.addAll(runtimeMap.keySet());
        keys.addAll(formalMap.keySet());
        return keys;
    }

    private ProductModelGovernanceSummaryVO buildOverallSummary(List<ProductModelGovernanceCompareRowVO> rows) {
        ProductModelGovernanceSummaryVO summary = new ProductModelGovernanceSummaryVO();
        summary.setManualCount((int) rows.stream().filter(row -> row.getManualCandidate() != null).count());
        summary.setRuntimeCount((int) rows.stream().filter(row -> row.getRuntimeCandidate() != null).count());
        summary.setFormalCount((int) rows.stream().filter(row -> row.getFormalModel() != null).count());
        summary.setPropertyCount((int) rows.stream().filter(row -> "property".equals(row.getModelType())).count());
        summary.setEventCount((int) rows.stream().filter(row -> "event".equals(row.getModelType())).count());
        summary.setServiceCount((int) rows.stream().filter(row -> "service".equals(row.getModelType())).count());
        summary.setDoubleAlignedCount(countByStatus(rows, STATUS_DOUBLE_ALIGNED));
        summary.setManualOnlyCount(countByStatus(rows, STATUS_MANUAL_ONLY));
        summary.setRuntimeOnlyCount(countByStatus(rows, STATUS_RUNTIME_ONLY));
        summary.setFormalExistsCount(countByStatus(rows, STATUS_FORMAL_EXISTS));
        summary.setSuspectedConflictCount(countByStatus(rows, STATUS_SUSPECTED_CONFLICT));
        summary.setEvidenceInsufficientCount(countByStatus(rows, STATUS_EVIDENCE_INSUFFICIENT));
        summary.setLastComparedAt(LocalDateTime.now());
        return summary;
    }

    private ProductModelGovernanceSummaryVO buildFormalSummary(List<ProductModel> formalModels) {
        List<ProductModel> models = safeList(formalModels);
        ProductModelGovernanceSummaryVO summary = new ProductModelGovernanceSummaryVO();
        summary.setManualCount(0);
        summary.setRuntimeCount(0);
        summary.setFormalCount(models.size());
        summary.setPropertyCount((int) models.stream().filter(model -> "property".equals(model.getModelType())).count());
        summary.setEventCount((int) models.stream().filter(model -> "event".equals(model.getModelType())).count());
        summary.setServiceCount((int) models.stream().filter(model -> "service".equals(model.getModelType())).count());
        summary.setDoubleAlignedCount(0);
        summary.setManualOnlyCount(0);
        summary.setRuntimeOnlyCount(0);
        summary.setFormalExistsCount(models.size());
        summary.setSuspectedConflictCount(0);
        summary.setEvidenceInsufficientCount(0);
        summary.setLastComparedAt(LocalDateTime.now());
        return summary;
    }

    private ProductModelCandidateSummaryVO copySummary(ProductModelCandidateSummaryVO summary) {
        ProductModelCandidateSummaryVO copied = new ProductModelCandidateSummaryVO();
        if (summary == null) {
            copied.setPropertyEvidenceCount(0);
            copied.setPropertyCandidateCount(0);
            copied.setEventEvidenceCount(0);
            copied.setEventCandidateCount(0);
            copied.setServiceEvidenceCount(0);
            copied.setServiceCandidateCount(0);
            copied.setNeedsReviewCount(0);
            copied.setExistingModelCount(0);
            copied.setCreatedCount(0);
            copied.setSkippedCount(0);
            copied.setConflictCount(0);
            return copied;
        }
        copied.setExtractionMode(summary.getExtractionMode());
        copied.setSampleType(summary.getSampleType());
        copied.setSampleDeviceCode(summary.getSampleDeviceCode());
        copied.setPropertyEvidenceCount(summary.getPropertyEvidenceCount());
        copied.setPropertyCandidateCount(summary.getPropertyCandidateCount());
        copied.setEventEvidenceCount(summary.getEventEvidenceCount());
        copied.setEventCandidateCount(summary.getEventCandidateCount());
        copied.setServiceEvidenceCount(summary.getServiceEvidenceCount());
        copied.setServiceCandidateCount(summary.getServiceCandidateCount());
        copied.setNeedsReviewCount(summary.getNeedsReviewCount());
        copied.setExistingModelCount(summary.getExistingModelCount());
        copied.setCreatedCount(summary.getCreatedCount());
        copied.setSkippedCount(summary.getSkippedCount());
        copied.setConflictCount(summary.getConflictCount());
        copied.setEventHint(summary.getEventHint());
        copied.setServiceHint(summary.getServiceHint());
        copied.setIgnoredFieldCount(summary.getIgnoredFieldCount());
        copied.setLastExtractedAt(summary.getLastExtractedAt());
        return copied;
    }

    private int countByStatus(List<ProductModelGovernanceCompareRowVO> rows, String status) {
        return (int) rows.stream()
                .filter(row -> status.equals(row.getCompareStatus()))
                .count();
    }

    private ProductModelGovernanceEvidenceVO toEvidence(ProductModelCandidateVO candidate) {
        ProductModelGovernanceEvidenceVO evidence = new ProductModelGovernanceEvidenceVO();
        evidence.setModelType(candidate.getModelType());
        evidence.setIdentifier(candidate.getIdentifier());
        evidence.setModelName(candidate.getModelName());
        evidence.setDataType(candidate.getDataType());
        evidence.setSpecsJson(candidate.getSpecsJson());
        evidence.setEventType(candidate.getEventType());
        evidence.setServiceInputJson(candidate.getServiceInputJson());
        evidence.setServiceOutputJson(candidate.getServiceOutputJson());
        evidence.setSortNo(candidate.getSortNo());
        evidence.setRequiredFlag(candidate.getRequiredFlag());
        evidence.setDescription(candidate.getDescription());
        evidence.setGroupKey(candidate.getGroupKey());
        evidence.setConfidence(candidate.getConfidence());
        evidence.setNeedsReview(candidate.getNeedsReview());
        evidence.setCandidateStatus(candidate.getCandidateStatus());
        evidence.setReviewReason(candidate.getReviewReason());
        evidence.setEvidenceCount(candidate.getEvidenceCount());
        evidence.setMessageEvidenceCount(candidate.getMessageEvidenceCount());
        evidence.setLastReportTime(candidate.getLastReportTime());
        evidence.setSourceTables(candidate.getSourceTables());
        return evidence;
    }

    private ProductModelGovernanceEvidenceVO toEvidence(ProductModel model) {
        ProductModelGovernanceEvidenceVO evidence = new ProductModelGovernanceEvidenceVO();
        evidence.setModelId(model.getId());
        evidence.setModelType(model.getModelType());
        evidence.setIdentifier(model.getIdentifier());
        evidence.setModelName(model.getModelName());
        evidence.setDataType("property".equals(model.getModelType()) ? model.getDataType() : null);
        evidence.setSpecsJson(model.getSpecsJson());
        evidence.setEventType(model.getEventType());
        evidence.setServiceInputJson(model.getServiceInputJson());
        evidence.setServiceOutputJson(model.getServiceOutputJson());
        evidence.setSortNo(model.getSortNo());
        evidence.setRequiredFlag(model.getRequiredFlag());
        evidence.setDescription(model.getDescription());
        evidence.setSourceTables(List.of("iot_product_model"));
        return evidence;
    }

    private String toKey(String modelType, String identifier) {
        String normalizedModelType = normalize(modelType);
        String normalizedIdentifier = normalize(identifier);
        if (normalizedModelType == null || normalizedIdentifier == null) {
            return null;
        }
        return normalizedModelType + "::" + normalizedIdentifier;
    }

    private String[] splitKey(String key) {
        int index = key.indexOf("::");
        if (index < 0) {
            return new String[] {key, key};
        }
        return new String[] {key.substring(0, index), key.substring(index + 2)};
    }

    private String canonicalIdentifier(String identifier) {
        String normalized = normalize(identifier);
        if (normalized == null) {
            return "";
        }
        return normalized.toLowerCase(Locale.ROOT)
                .replace("singal", "signal")
                .replace("temperature", "temp")
                .replace("restart", "reboot")
                .replace("_", "")
                .replace("-", "")
                .replace(".", "");
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
