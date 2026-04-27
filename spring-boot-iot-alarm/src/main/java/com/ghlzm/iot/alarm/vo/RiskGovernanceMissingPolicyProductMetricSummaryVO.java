package com.ghlzm.iot.alarm.vo;

import lombok.Data;

/**
 * 待配置阈值策略的产品测点聚合摘要。
 */
@Data
public class RiskGovernanceMissingPolicyProductMetricSummaryVO {

    private Long productId;

    private String productKey;

    private String productName;

    private Long riskMetricId;

    private String metricIdentifier;

    private String metricName;

    private Long bindingCount;

    private Long riskPointCount;

    private Long deviceCount;
}
