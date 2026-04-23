package com.ghlzm.iot.system.service;

import com.ghlzm.iot.system.service.model.GovernanceWorkItemCommand;
import java.util.List;

/**
 * Collects desired governance work items from upstream domains so the control plane can
 * sync first-class backlog objects without introducing reverse module dependencies.
 */
public interface GovernanceWorkItemContributor {

    List<GovernanceWorkItemCommand> collectWorkItems();
}
