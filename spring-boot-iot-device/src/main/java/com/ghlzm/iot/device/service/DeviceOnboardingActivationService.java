package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.dto.DeviceOnboardingBatchActivateDTO;
import com.ghlzm.iot.device.vo.DeviceOnboardingBatchResultVO;

public interface DeviceOnboardingActivationService {

    DeviceOnboardingBatchResultVO activate(DeviceOnboardingBatchActivateDTO dto);

    DeviceOnboardingBatchResultVO activate(Long currentUserId, DeviceOnboardingBatchActivateDTO dto);
}
