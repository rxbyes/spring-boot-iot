package com.ghlzm.iot.system.service.model;

/**
 * Aggregated downstream dependency counts for contract impact analysis.
 */
public record GovernanceImpactDependencySummary(long affectedRiskMetricCount,
                                                long affectedRiskPointBindingCount,
                                                long affectedRuleCount,
                                                long affectedLinkageBindingCount,
                                                long affectedEmergencyPlanBindingCount) {

    public static GovernanceImpactDependencySummary empty() {
        return new GovernanceImpactDependencySummary(0L, 0L, 0L, 0L, 0L);
    }
}
