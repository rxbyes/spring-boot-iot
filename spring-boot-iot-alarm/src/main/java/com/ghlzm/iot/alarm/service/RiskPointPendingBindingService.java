package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.alarm.dto.RiskPointPendingBindingQuery;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.vo.RiskPointPendingBindingItemVO;
import com.ghlzm.iot.common.response.PageResult;

/**
 * 风险点待治理绑定读取服务。
 */
public interface RiskPointPendingBindingService {

    PageResult<RiskPointPendingBindingItemVO> pagePendingBindings(RiskPointPendingBindingQuery query, Long currentUserId);

    RiskPointDevicePendingBinding getRequiredPending(Long pendingId, Long currentUserId);
}
