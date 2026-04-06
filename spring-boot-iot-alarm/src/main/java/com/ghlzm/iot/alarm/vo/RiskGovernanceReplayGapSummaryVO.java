package com.ghlzm.iot.alarm.vo;

import lombok.Data;

/**
 * 风险治理回放缺口摘要。
 */
@Data
public class RiskGovernanceReplayGapSummaryVO {

    private Long missingBindingCount;

    private Long missingPolicyCount;

    private Long missingRiskMetricCount;
}
