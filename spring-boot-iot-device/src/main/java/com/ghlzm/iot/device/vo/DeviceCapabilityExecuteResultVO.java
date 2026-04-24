package com.ghlzm.iot.device.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceCapabilityExecuteResultVO {

    private String commandId;

    private String deviceCode;

    private String capabilityCode;

    private String status;

    private String topic;

    private LocalDateTime sentAt;
}
