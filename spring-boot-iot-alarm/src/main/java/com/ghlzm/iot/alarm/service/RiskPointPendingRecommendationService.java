package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.alarm.vo.RiskPointPendingCandidateBundleVO;

/**
 * 风险点待治理推荐服务。
 */
public interface RiskPointPendingRecommendationService {

    RiskPointPendingCandidateBundleVO getCandidates(Long pendingId, Long currentUserId);
}
