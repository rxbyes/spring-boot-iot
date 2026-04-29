package com.ghlzm.iot.message.service.impl;

import com.ghlzm.iot.device.service.DeviceCapabilityCommandGateway;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandRequest;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandResult;
import com.ghlzm.iot.message.service.DeviceCapabilityDownCommandService;
import org.springframework.stereotype.Service;

@Service
public class DeviceCapabilityCommandGatewayImpl implements DeviceCapabilityCommandGateway {

    private final DeviceCapabilityDownCommandService deviceCapabilityDownCommandService;

    public DeviceCapabilityCommandGatewayImpl(DeviceCapabilityDownCommandService deviceCapabilityDownCommandService) {
        this.deviceCapabilityDownCommandService = deviceCapabilityDownCommandService;
    }

    @Override
    public DeviceCapabilityCommandResult execute(DeviceCapabilityCommandRequest request) {
        return deviceCapabilityDownCommandService.execute(request);
    }
}
