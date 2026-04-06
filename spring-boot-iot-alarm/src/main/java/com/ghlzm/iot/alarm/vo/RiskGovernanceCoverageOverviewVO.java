package com.ghlzm.iot.alarm.vo;

import lombok.Data;

/**
 * 风险治理覆盖率概览。
 */
@Data
public class RiskGovernanceCoverageOverviewVO {

    private Long productId;

    private Long contractPropertyCount;

    private Long publishedRiskMetricCount;

    private Long boundRiskMetricCount;

    private Long ruleCoveredRiskMetricCount;

    private Double contractMetricCoverageRate;

    private Double bindingCoverageRate;

    private Double ruleCoverageRate;
}
