package com.ghlzm.iot.alarm.vo;

import lombok.Data;

/**
 * 风险治理覆盖率概览。
 */
@Data
public class RiskGovernanceCoverageOverviewVO {

    private Long productId;

    private Long contractPropertyCount;

    private Long publishableContractPropertyCount;

    private Long publishedRiskMetricCount;

    private Long boundRiskMetricCount;

    private Long ruleCoveredRiskMetricCount;

    private Long linkageCoveredRiskMetricCount;

    private Long emergencyPlanCoveredRiskMetricCount;

    private Long linkagePlanCoveredRiskMetricCount;

    private Double contractMetricCoverageRate;

    private Double bindingCoverageRate;

    private Double ruleCoverageRate;

    private Double linkageCoverageRate;

    private Double emergencyPlanCoverageRate;

    private Double linkagePlanCoverageRate;
}
