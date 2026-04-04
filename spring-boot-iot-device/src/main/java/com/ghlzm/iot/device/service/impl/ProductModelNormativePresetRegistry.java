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
                    List.of("X", "angleX"),
                    "L1",
                    "QJ",
                    "L1_QJ_1"),
            definition(
                    20,
                    "L1_QJ_1.Y",
                    "倾角测点 Y 轴倾角",
                    "double",
                    "°",
                    1,
                    "表 B.1",
                    "倾角测点 Y 轴与水平面的夹角，建议进入 latest 和 telemetry。",
                    List.of("Y", "angleY"),
                    "L1",
                    "QJ",
                    "L1_QJ_1"),
            definition(
                    30,
                    "L1_QJ_1.Z",
                    "倾角测点 Z 轴倾角",
                    "double",
                    "°",
                    1,
                    "表 B.1",
                    "倾角测点 Z 轴与水平面的夹角，建议进入 latest 和 telemetry。",
                    List.of("Z", "angleZ"),
                    "L1",
                    "QJ",
                    "L1_QJ_1"),
            definition(
                    40,
                    "L1_QJ_1.angle",
                    "倾角测点平面夹角",
                    "double",
                    "°",
                    1,
                    "表 B.1",
                    "倾角测点平面夹角，可作为综合倾角结果进入 latest 和 telemetry。",
                    List.of("angle", "tiltAngle", "planeAngle"),
                    "L1",
                    "QJ",
                    "L1_QJ_1"),
            definition(
                    50,
                    "L1_QJ_1.AZI",
                    "倾角测点方位角",
                    "double",
                    "°",
                    1,
                    "表 B.1",
                    "X 轴在水平面的投影与磁北夹角。",
                    List.of("AZI"),
                    "L1",
                    "QJ",
                    "L1_QJ_1"),
            definition(
                    60,
                    "L1_JS_1.gX",
                    "加速度测点 X 轴加速度",
                    "double",
                    "mg",
                    1,
                    "表 C.1",
                    "加速度测点 X 轴加速度，建议进入 latest 和 telemetry。",
                    List.of("gX", "accX"),
                    "L1",
                    "JS",
                    "L1_JS_1"),
            definition(
                    70,
                    "L1_JS_1.gY",
                    "加速度测点 Y 轴加速度",
                    "double",
                    "mg",
                    1,
                    "表 C.1",
                    "加速度测点 Y 轴加速度，建议进入 latest 和 telemetry。",
                    List.of("gY", "accY"),
                    "L1",
                    "JS",
                    "L1_JS_1"),
            definition(
                    80,
                    "L1_JS_1.gZ",
                    "加速度测点 Z 轴加速度",
                    "double",
                    "mg",
                    1,
                    "表 C.1",
                    "加速度测点 Z 轴加速度，建议进入 latest 和 telemetry。",
                    List.of("gZ", "accZ"),
                    "L1",
                    "JS",
                    "L1_JS_1"),
            definition(
                    90,
                    "L1_LF_1.value",
                    "裂缝测点张开度",
                    "double",
                    "mm",
                    1,
                    "表 D.1",
                    "裂缝测点张开度，建议进入 latest 和 telemetry。",
                    List.of("value", "crackValue", "lfValue"),
                    "L1",
                    "LF",
                    "L1_LF_1"),
            definition(
                    150,
                    "S1_ZT_1.signal_4g",
                    "4G 信号强度",
                    "integer",
                    "dBm",
                    0,
                    "表 F.1",
                    "4G 通信强度。",
                    List.of("signal_4g", "status4gSignal"),
                    "S1",
                    "ZT",
                    "S1_ZT_1"));

    private final Map<String, Set<String>> integratedAliasMap = Map.of(
            "L1_QJ_1.X", Set.of("L1_QJ_1.X", "X", "x", "angleX"),
            "L1_QJ_1.Y", Set.of("L1_QJ_1.Y", "Y", "y", "angleY"),
            "L1_QJ_1.Z", Set.of("L1_QJ_1.Z", "Z", "z", "angleZ"),
            "L1_QJ_1.angle", Set.of("L1_QJ_1.angle", "angle", "tiltAngle", "planeAngle"),
            "L1_QJ_1.AZI", Set.of("L1_QJ_1.AZI", "AZI", "azi"),
            "L1_JS_1.gX", Set.of("L1_JS_1.gX", "gX", "gx", "accX"),
            "L1_JS_1.gY", Set.of("L1_JS_1.gY", "gY", "gy", "accY"),
            "L1_JS_1.gZ", Set.of("L1_JS_1.gZ", "gZ", "gz", "accZ"),
            "L1_LF_1.value", Set.of("L1_LF_1.value", "value", "crackValue", "lfValue"),
            "S1_ZT_1.signal_4g", Set.of("S1_ZT_1.signal_4g", "signal_4g", "status4gSignal"));

    public List<ProductModelGovernanceEvidenceVO> buildPropertyPreset(String presetCode,
                                                                      Collection<String> selectedIdentifiers) {
        validatePresetCode(presetCode);
        boolean selectAll = selectedIdentifiers == null;
        Set<String> filter = selectAll
                ? Set.of()
                : new LinkedHashSet<>(selectedIdentifiers);
        return integratedDefinitions.stream()
                .filter(item -> selectAll || filter.contains(item.getIdentifier()))
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

    public Optional<ProductModelGovernanceEvidenceVO> findPropertyDefinition(String presetCode, String identifier) {
        validatePresetCode(presetCode);
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String normalizedIdentifier = identifier.trim();
        return integratedDefinitions.stream()
                .filter(item -> normalizedIdentifier.equals(item.getIdentifier()))
                .findFirst()
                .map(this::copyEvidence);
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
