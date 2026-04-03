package com.ghlzm.iot.alarm.vo;

import lombok.Data;

/**
 * 单个测点转正结果。
 */
@Data
public class RiskPointPendingPromotionItemVO {

    private String metricIdentifier;

    private String metricName;

    private String promotionStatus;

    private Long bindingId;
}
