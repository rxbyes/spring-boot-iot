package com.ghlzm.iot.alarm.dto;

import lombok.Data;

/**
 * 风险点正式绑定测点选择项。
 */
@Data
public class RiskPointBindMetricDTO {

    private Long riskMetricId;

    private String metricIdentifier;

    private String metricName;
}
