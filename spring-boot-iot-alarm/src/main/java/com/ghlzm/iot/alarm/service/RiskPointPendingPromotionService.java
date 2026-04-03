package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.alarm.dto.RiskPointPendingIgnoreRequest;
import com.ghlzm.iot.alarm.dto.RiskPointPendingPromotionRequest;
import com.ghlzm.iot.alarm.vo.RiskPointPendingPromotionResultVO;

/**
 * 风险点待治理转正写侧服务。
 */
public interface RiskPointPendingPromotionService {

    RiskPointPendingPromotionResultVO promote(Long pendingId, RiskPointPendingPromotionRequest request, Long currentUserId);

    void ignore(Long pendingId, RiskPointPendingIgnoreRequest request, Long currentUserId);
}
