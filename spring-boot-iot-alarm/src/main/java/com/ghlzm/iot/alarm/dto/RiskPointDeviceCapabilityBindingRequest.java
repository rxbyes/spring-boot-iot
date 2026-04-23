package com.ghlzm.iot.alarm.dto;

import lombok.Data;

/**
 * 风险点设备级正式绑定请求。
 */
@Data
public class RiskPointDeviceCapabilityBindingRequest {

    private Long riskPointId;

    private Long deviceId;

    private String deviceCapabilityType;
}
