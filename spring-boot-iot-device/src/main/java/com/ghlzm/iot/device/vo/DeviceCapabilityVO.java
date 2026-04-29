package com.ghlzm.iot.device.vo;

import lombok.Data;

import java.util.Map;

@Data
public class DeviceCapabilityVO {

    private String code;

    private String name;

    private String group;

    private boolean enabled;

    private boolean requiresOnline;

    private String disabledReason;

    private Map<String, Map<String, Object>> paramsSchema;
}
