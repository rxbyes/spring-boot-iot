package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandRequest;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandResult;

public interface DeviceCapabilityCommandGateway {

    DeviceCapabilityCommandResult execute(DeviceCapabilityCommandRequest request);
}
