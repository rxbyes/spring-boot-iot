package com.ghlzm.iot.alarm.vo;

import lombok.Data;

/**
 * 治理经营驾驶舱概览。
 */
@Data
public class RiskGovernanceDashboardOverviewVO {

    private Long totalProductCount;

    private Long governedProductCount;

    private Long pendingProductGovernanceCount;

    private Long releasedProductCount;

    private Long pendingContractReleaseCount;

    private Long publishedRiskMetricCount;

    private Long boundRiskMetricCount;

    private Long ruleCoveredRiskMetricCount;

    private Long pendingRiskBindingCount;

    private Long pendingPolicyCount;

    private Long pendingThresholdPolicyCount;

    private Long pendingLinkageCount;

    private Long pendingEmergencyPlanCount;

    private Long pendingLinkagePlanCount;

    private Long pendingReplayCount;

    private Double governanceCompletionRate;

    private Double metricBindingCoverageRate;

    private Double policyCoverageRate;

    private Double thresholdPolicyCoverageRate;

    private Double linkageCoverageRate;

    private Double emergencyPlanCoverageRate;

    private Double linkagePlanCoverageRate;

    private Double averageOnboardingDurationHours;

    private Double bottleneckPendingProductGovernanceRate;

    private Double bottleneckPendingContractReleaseRate;

    private Double bottleneckPendingRiskBindingRate;

    private Double bottleneckPendingThresholdPolicyRate;

    private Double bottleneckPendingLinkagePlanRate;

    private Double bottleneckPendingReplayRate;
}
