package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceLaunch;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceProgress;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceRequest;

/**
 * 无代码接入案例标准验收桥接接口，由 report 模块提供实现。
 */
public interface DeviceOnboardingAcceptanceGateway {

    DeviceOnboardingAcceptanceLaunch launch(DeviceOnboardingAcceptanceRequest request);

    DeviceOnboardingAcceptanceProgress getProgress(String jobId, String runId);
}
