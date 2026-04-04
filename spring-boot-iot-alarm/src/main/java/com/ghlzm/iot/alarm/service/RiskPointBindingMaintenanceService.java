package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.alarm.vo.RiskPointBindingDeviceGroupVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingSummaryVO;

import java.util.List;

/**
 * 风险点绑定维护读侧服务。
 */
public interface RiskPointBindingMaintenanceService {

    List<RiskPointBindingSummaryVO> listBindingSummaries(List<Long> riskPointIds, Long currentUserId);

    List<RiskPointBindingDeviceGroupVO> listBindingGroups(Long riskPointId, Long currentUserId);
}
