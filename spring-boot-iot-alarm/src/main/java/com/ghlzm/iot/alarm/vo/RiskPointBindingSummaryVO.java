package com.ghlzm.iot.alarm.vo;

import lombok.Data;

/**
 * 风险点绑定摘要。
 */
@Data
public class RiskPointBindingSummaryVO {

    private Long riskPointId;

    private Integer boundDeviceCount;

    private Integer boundMetricCount;

    private Integer pendingBindingCount;
}
