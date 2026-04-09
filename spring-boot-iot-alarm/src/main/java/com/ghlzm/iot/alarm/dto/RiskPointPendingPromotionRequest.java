package com.ghlzm.iot.alarm.dto;

import lombok.Data;

import java.util.List;

/**
 * 待治理转正请求。
 */
@Data
public class RiskPointPendingPromotionRequest {

    private List<RiskPointPendingPromotionMetricDTO> metrics;

    private Boolean completePending;

    private String promotionNote;
}
