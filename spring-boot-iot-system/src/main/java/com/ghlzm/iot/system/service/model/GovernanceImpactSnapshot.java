package com.ghlzm.iot.system.service.model;

import java.util.List;
import lombok.Data;

@Data
public class GovernanceImpactSnapshot {

    private Long affectedCount;
    private List<String> affectedTypes;
    private Boolean rollbackable;
    private String rollbackPlanSummary;
}
