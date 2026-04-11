package com.ghlzm.iot.system.service;

import com.ghlzm.iot.system.entity.GovernanceWorkItem;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemCommand;
import com.ghlzm.iot.system.vo.GovernanceDecisionContextVO;

public interface GovernancePriorityScorer {

    GovernanceDecisionContextVO buildDecisionContext(GovernanceWorkItemCommand command);

    GovernanceDecisionContextVO buildDecisionContext(GovernanceWorkItem workItem);
}
