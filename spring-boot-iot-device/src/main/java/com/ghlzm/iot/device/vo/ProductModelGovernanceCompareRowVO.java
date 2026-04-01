package com.ghlzm.iot.device.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 物模型治理对比结果行。
 */
@Data
public class ProductModelGovernanceCompareRowVO {

    private String modelType;

    private String identifier;

    private ProductModelGovernanceEvidenceVO manualCandidate;

    private ProductModelGovernanceEvidenceVO runtimeCandidate;

    private ProductModelGovernanceEvidenceVO formalModel;

    private String compareStatus;

    private List<String> riskFlags = new ArrayList<>();

    private String suggestedAction;

    private List<String> suspectedMatches = new ArrayList<>();
}
