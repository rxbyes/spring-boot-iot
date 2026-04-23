package com.ghlzm.iot.system.service;

import com.ghlzm.iot.system.service.model.GovernanceImpactDependencySummary;
import java.util.Set;

/**
 * Queries downstream governance dependencies impacted by product contract changes
 * without introducing reverse module dependencies into the device domain.
 */
public interface GovernanceImpactDependencyQueryService {

    GovernanceImpactDependencySummary summarizeProductContractImpact(Long productId, Set<String> contractIdentifiers);
}
