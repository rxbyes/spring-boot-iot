package com.ghlzm.iot.device.capability;

import java.util.Map;

public record DeviceCapabilityDefinition(
        String code,
        String name,
        String group,
        boolean requiresOnline,
        Map<String, Map<String, Object>> paramsSchema
) {
}
