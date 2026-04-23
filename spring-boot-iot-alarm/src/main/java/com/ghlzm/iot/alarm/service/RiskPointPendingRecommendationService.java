package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.alarm.vo.RiskPointPendingCandidateBundleVO;
import java.util.List;

/**
 * 风险点待治理推荐服务。
 */
public interface RiskPointPendingRecommendationService {

    RiskPointPendingCandidateBundleVO getCandidates(Long pendingId, Long currentUserId);

    List<String> listRecommendedMetricIdentifiers(Long productId);
}
