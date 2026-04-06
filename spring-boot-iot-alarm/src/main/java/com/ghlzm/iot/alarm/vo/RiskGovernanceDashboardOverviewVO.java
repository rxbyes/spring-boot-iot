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

    private Long pendingReplayCount;

    private Double governanceCompletionRate;

    private Double metricBindingCoverageRate;

    private Double policyCoverageRate;
}
