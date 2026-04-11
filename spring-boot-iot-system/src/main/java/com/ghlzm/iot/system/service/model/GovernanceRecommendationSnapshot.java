package com.ghlzm.iot.system.service.model;

import java.util.List;
import lombok.Data;

@Data
public class GovernanceRecommendationSnapshot {

    private String recommendationType;
    private Double confidence;
    private List<String> reasonCodes;
    private String suggestedAction;
    private List<GovernanceEvidenceItem> evidenceItems;
}
