package com.ghlzm.iot.message.service;

import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandRequest;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandResult;

public interface DeviceCapabilityDownCommandService {

    DeviceCapabilityCommandResult execute(DeviceCapabilityCommandRequest request);
}
