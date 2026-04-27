package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.alarm.dto.RiskPointBindingReplaceRequest;
import com.ghlzm.iot.alarm.dto.RiskPointBindingRenameRequest;
import com.ghlzm.iot.alarm.dto.RiskPointBatchBindDeviceRequest;
import com.ghlzm.iot.alarm.dto.RiskPointDeviceCapabilityBindingRequest;
import com.ghlzm.iot.alarm.entity.RiskPointDeviceCapabilityBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.vo.RiskPointBindingDeviceGroupVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingMetricVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingSummaryVO;
import com.ghlzm.iot.device.vo.DeviceMetricOptionVO;
import com.ghlzm.iot.system.vo.GovernanceSubmissionResultVO;

import java.util.List;

/**
 * 风险点绑定维护服务。
 */
public interface RiskPointBindingMaintenanceService {

    List<RiskPointBindingSummaryVO> listBindingSummaries(List<Long> riskPointIds, Long currentUserId);

    List<RiskPointBindingDeviceGroupVO> listBindingGroups(Long riskPointId, Long currentUserId);

    List<DeviceMetricOptionVO> listFormalBindingMetricOptions(Long deviceId, Long currentUserId);

    GovernanceSubmissionResultVO submitBindDevice(RiskPointBatchBindDeviceRequest request, Long currentUserId);

    GovernanceSubmissionResultVO submitBindDeviceCapability(RiskPointDeviceCapabilityBindingRequest request, Long currentUserId);

    GovernanceSubmissionResultVO submitUnbindDevice(Long riskPointId, Long deviceId, Long currentUserId);

    RiskPointDevice bindDevice(RiskPointDevice riskPointDevice, Long currentUserId);

    List<RiskPointDevice> bindDevices(RiskPointBatchBindDeviceRequest request, Long currentUserId);

    RiskPointDeviceCapabilityBinding bindDeviceCapability(RiskPointDeviceCapabilityBindingRequest request, Long currentUserId);

    void unbindDevice(Long riskPointId, Long deviceId, Long currentUserId);

    void removeBinding(Long bindingId, Long currentUserId);

    RiskPointBindingMetricVO renameBindingMetric(Long bindingId, RiskPointBindingRenameRequest request, Long currentUserId);

    RiskPointBindingMetricVO replaceBindingMetric(Long bindingId, RiskPointBindingReplaceRequest request, Long currentUserId);
}
