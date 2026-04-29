package com.ghlzm.iot.alarm.vo;

import lombok.Data;

import java.math.BigDecimal;

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

    private Integer recommendationWindowDays;

    private Long recommendationSampleCount;

    private BigDecimal recommendationMinValue;

    private BigDecimal recommendationMaxValue;

    private BigDecimal recommendationAvgValue;

    private String recommendedExpression;

    private String recommendedLowerExpression;

    private String recommendedUpperExpression;

    private String recommendationStatus;

    private String recommendationDirection;

    private String recommendationReason;
}
