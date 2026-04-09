package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.alarm.dto.RiskPointBindingReplaceRequest;
import com.ghlzm.iot.alarm.vo.RiskPointBindingDeviceGroupVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingMetricVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingSummaryVO;

import java.util.List;

/**
 * 风险点绑定维护服务。
 */
public interface RiskPointBindingMaintenanceService {

    List<RiskPointBindingSummaryVO> listBindingSummaries(List<Long> riskPointIds, Long currentUserId);

    List<RiskPointBindingDeviceGroupVO> listBindingGroups(Long riskPointId, Long currentUserId);

    void removeBinding(Long bindingId, Long currentUserId);

    RiskPointBindingMetricVO replaceBindingMetric(Long bindingId, RiskPointBindingReplaceRequest request, Long currentUserId);
}
