package com.ghlzm.iot.device.service.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceCapabilityCommandResult {

    private String commandId;

    private String deviceCode;

    private String capabilityCode;

    private String status;

    private String topic;

    private LocalDateTime sentAt;
}
