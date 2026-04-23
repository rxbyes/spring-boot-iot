package com.ghlzm.iot.system.service.model;

import lombok.Data;

@Data
public class GovernanceRollbackSnapshot {

    private Boolean rollbackable;
    private String rollbackPlanSummary;
}
