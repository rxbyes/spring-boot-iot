package com.ghlzm.iot.system.vo;

import java.util.List;
import lombok.Data;

@Data
public class GovernanceDecisionContextVO {

    private Long workItemId;
    private String priorityLevel;
    private Integer priorityScore;
    private String problemSummary;
    private List<String> reasonCodes;
    private List<String> affectedModules;
    private Long affectedCount;
    private String recommendedAction;
    private Boolean rollbackable;
    private String rollbackPlanSummary;
}
