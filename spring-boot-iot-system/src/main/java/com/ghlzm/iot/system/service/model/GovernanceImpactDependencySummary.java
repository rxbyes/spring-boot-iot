package com.ghlzm.iot.system.service.model;

import java.util.List;

/**
 * Aggregated downstream dependency counts and object-level details for
 * contract impact analysis.
 */
public record GovernanceImpactDependencySummary(long affectedRiskMetricCount,
                                                long affectedRiskPointBindingCount,
                                                long affectedRuleCount,
                                                long affectedLinkageBindingCount,
                                                long affectedEmergencyPlanBindingCount,
                                                List<RiskMetricDetail> affectedRiskMetrics,
                                                List<RiskPointBindingDetail> affectedRiskPointBindings,
                                                List<RuleDetail> affectedRules,
                                                List<LinkageBindingDetail> affectedLinkageBindings,
                                                List<EmergencyPlanBindingDetail> affectedEmergencyPlanBindings) {

    public GovernanceImpactDependencySummary {
        affectedRiskMetrics = affectedRiskMetrics == null ? List.of() : List.copyOf(affectedRiskMetrics);
        affectedRiskPointBindings = affectedRiskPointBindings == null ? List.of() : List.copyOf(affectedRiskPointBindings);
        affectedRules = affectedRules == null ? List.of() : List.copyOf(affectedRules);
        affectedLinkageBindings = affectedLinkageBindings == null ? List.of() : List.copyOf(affectedLinkageBindings);
        affectedEmergencyPlanBindings = affectedEmergencyPlanBindings == null ? List.of() : List.copyOf(affectedEmergencyPlanBindings);
    }

    public static GovernanceImpactDependencySummary empty() {
        return new GovernanceImpactDependencySummary(0L, 0L, 0L, 0L, 0L, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public record RiskMetricDetail(Long riskMetricId,
                                   String contractIdentifier,
                                   String normativeIdentifier,
                                   String riskMetricCode,
                                   String riskMetricName,
                                   String metricRole,
                                   String lifecycleStatus) {
    }

    public record RiskPointBindingDetail(Long bindingId,
                                         Long riskPointId,
                                         String riskPointName,
                                         Long deviceId,
                                         String deviceCode,
                                         String deviceName,
                                         Long riskMetricId,
                                         String metricIdentifier,
                                         String metricName) {
    }

    public record RuleDetail(Long ruleId,
                             String ruleName,
                             Long riskMetricId,
                             String metricIdentifier,
                             String metricName,
                             String alarmLevel) {
    }

    public record LinkageBindingDetail(Long bindingId,
                                       Long linkageRuleId,
                                       String linkageRuleName,
                                       Long riskMetricId,
                                       String bindingStatus) {
    }

    public record EmergencyPlanBindingDetail(Long bindingId,
                                             Long emergencyPlanId,
                                             String emergencyPlanName,
                                             Long riskMetricId,
                                             String bindingStatus,
                                             String alarmLevel) {
    }
}
