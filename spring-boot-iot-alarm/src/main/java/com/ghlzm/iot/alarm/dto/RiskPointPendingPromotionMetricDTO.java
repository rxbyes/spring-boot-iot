package com.ghlzm.iot.alarm.dto;

import lombok.Data;

/**
 * 待治理转正式绑定的测点选择项。
 */
@Data
public class RiskPointPendingPromotionMetricDTO {

    private Long riskMetricId;

    private String metricIdentifier;

    private String metricName;
}
