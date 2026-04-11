package com.ghlzm.iot.system.service.model;

import java.util.List;

/**
 * Side-effect-free governance simulation result.
 */
public record GovernanceSimulationResult(
        Long orderId,
        Long workItemId,
        String actionCode,
        boolean executable,
        Long affectedCount,
        List<String> affectedTypes,
        boolean rollbackable,
        String rollbackPlanSummary,
        GovernanceRecommendationSnapshot recommendation,
        GovernanceImpactSnapshot impact,
        GovernanceRollbackSnapshot rollback,
        boolean autoDraftEligible,
        String autoDraftComment
) {

    public GovernanceSimulationResult {
        affectedTypes = affectedTypes == null ? List.of() : List.copyOf(affectedTypes);
    }
}
