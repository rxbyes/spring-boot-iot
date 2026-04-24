package com.ghlzm.iot.device.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class DeviceCapabilityExecuteDTO {

    private Map<String, Object> params = new HashMap<>();
}
