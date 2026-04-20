package com.ghlzm.iot.report.service;

import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceProgress;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceRequest;

/**
 * 无代码接入案例标准验收执行服务。
 */
public interface DeviceOnboardingAcceptanceService {

    DeviceOnboardingAcceptanceProgress run(DeviceOnboardingAcceptanceRequest request);

    DeviceOnboardingAcceptanceProgress loadProgress(String runId);
}
