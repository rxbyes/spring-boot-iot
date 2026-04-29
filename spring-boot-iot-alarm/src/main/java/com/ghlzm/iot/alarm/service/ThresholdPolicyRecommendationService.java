package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.device.entity.Product;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 阈值策略推荐服务。
 */
public interface ThresholdPolicyRecommendationService {

    ThresholdPolicyRecommendation recommend(Product product, String metricIdentifier, Set<Long> deviceIds);

    record ThresholdPolicyRecommendation(
            Integer windowDays,
            Long sampleCount,
            BigDecimal minValue,
            BigDecimal maxValue,
            BigDecimal avgValue,
            String recommendedExpression,
            String recommendedLowerExpression,
            String recommendedUpperExpression,
            String status,
            String direction,
            String reason
    ) {
    }
}
