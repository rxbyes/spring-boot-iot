package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.vo.ProductModelGovernanceEvidenceVO;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 首批产品契约治理的规范字段预设。
 */
public class ProductModelNormativePresetRegistry {

    public static final String GOVERNANCE_MODE_NORMATIVE = "normative";
    public static final String PRESET_INTEGRATED = "landslide-integrated-tilt-accel-crack-v1";

    private final List<ProductModelGovernanceEvidenceVO> integratedDefinitions = List.of(
            definition(
                    10,
                    "L1_QJ_1.X",
                    "倾角测点 X 轴倾角",
                    "double",
                    "°",
                    1,
                    "表 B.1",
                    "倾角测点 X 轴与水平面的夹角，建议进入 latest 和 telemetry。",
                    List.of("L1_QJ_1.X", "X", "x", "angleX"),
                    "L1",
                    "QJ",
                    "L1_QJ_1"),
            definition(
                    150,
                    "S1_ZT_1.signal_4g",
                    "4G 信号强度",
                    "integer",
                    "dBm",
                    0,
                    "表 F.1",
                    "4G 通信强度。",
                    List.of("S1_ZT_1.signal_4g", "signal_4g", "status4gSignal"),
                    "S1",
                    "ZT",
                    "S1_ZT_1"));

    private final Map<String, Set<String>> integratedAliasMap = Map.of(
            "L1_QJ_1.X", Set.of("L1_QJ_1.X", "X", "x", "angleX"),
            "S1_ZT_1.signal_4g", Set.of("S1_ZT_1.signal_4g", "signal_4g", "status4gSignal"));

    public List<ProductModelGovernanceEvidenceVO> buildPropertyPreset(String presetCode,
                                                                      Collection<String> selectedIdentifiers) {
        validatePresetCode(presetCode);
        Set<String> filter = selectedIdentifiers == null
                ? Set.of()
                : new LinkedHashSet<>(selectedIdentifiers);
        return integratedDefinitions.stream()
                .filter(item -> filter.isEmpty() || filter.contains(item.getIdentifier()))
                .map(this::copyEvidence)
                .toList();
    }

    public Optional<String> findNormativeIdentifier(String presetCode, String rawIdentifier) {
        validatePresetCode(presetCode);
        if (rawIdentifier == null || rawIdentifier.isBlank()) {
            return Optional.empty();
        }
        String normalizedRawIdentifier = rawIdentifier.trim();
        return integratedAliasMap.entrySet().stream()
                .filter(entry -> entry.getValue().contains(normalizedRawIdentifier))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private void validatePresetCode(String presetCode) {
        if (!PRESET_INTEGRATED.equals(presetCode)) {
            throw new BizException("暂不支持的规范预设: " + presetCode);
        }
    }

    private ProductModelGovernanceEvidenceVO definition(Integer sortNo,
                                                        String identifier,
                                                        String modelName,
                                                        String dataType,
                                                        String unit,
                                                        Integer requiredFlag,
                                                        String normativeSource,
                                                        String description,
                                                        List<String> rawIdentifiers,
                                                        String monitorContentCode,
                                                        String monitorTypeCode,
                                                        String sensorCode) {
        ProductModelGovernanceEvidenceVO evidence = new ProductModelGovernanceEvidenceVO();
        evidence.setModelType("property");
        evidence.setSortNo(sortNo);
        evidence.setEvidenceOrigin(GOVERNANCE_MODE_NORMATIVE);
        evidence.setIdentifier(identifier);
        evidence.setModelName(modelName);
        evidence.setDataType(dataType);
        evidence.setUnit(unit);
        evidence.setRequiredFlag(requiredFlag);
        evidence.setNormativeSource(normativeSource);
        evidence.setDescription(description);
        evidence.setRawIdentifiers(rawIdentifiers);
        evidence.setMonitorContentCode(monitorContentCode);
        evidence.setMonitorTypeCode(monitorTypeCode);
        evidence.setSensorCode(sensorCode);
        return evidence;
    }

    private ProductModelGovernanceEvidenceVO copyEvidence(ProductModelGovernanceEvidenceVO source) {
        ProductModelGovernanceEvidenceVO target = new ProductModelGovernanceEvidenceVO();
        target.setModelId(source.getModelId());
        target.setModelType(source.getModelType());
        target.setIdentifier(source.getIdentifier());
        target.setModelName(source.getModelName());
        target.setDataType(source.getDataType());
        target.setSpecsJson(source.getSpecsJson());
        target.setEventType(source.getEventType());
        target.setServiceInputJson(source.getServiceInputJson());
        target.setServiceOutputJson(source.getServiceOutputJson());
        target.setSortNo(source.getSortNo());
        target.setRequiredFlag(source.getRequiredFlag());
        target.setDescription(source.getDescription());
        target.setGroupKey(source.getGroupKey());
        target.setEvidenceOrigin(source.getEvidenceOrigin());
        target.setUnit(source.getUnit());
        target.setNormativeSource(source.getNormativeSource());
        target.setRawIdentifiers(source.getRawIdentifiers());
        target.setMonitorContentCode(source.getMonitorContentCode());
        target.setMonitorTypeCode(source.getMonitorTypeCode());
        target.setSensorCode(source.getSensorCode());
        target.setConfidence(source.getConfidence());
        target.setNeedsReview(source.getNeedsReview());
        target.setCandidateStatus(source.getCandidateStatus());
        target.setReviewReason(source.getReviewReason());
        target.setEvidenceCount(source.getEvidenceCount());
        target.setMessageEvidenceCount(source.getMessageEvidenceCount());
        target.setLastReportTime(source.getLastReportTime());
        target.setSourceTables(source.getSourceTables());
        return target;
    }
}
