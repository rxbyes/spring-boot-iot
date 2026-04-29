package com.ghlzm.iot.device.capability;

public enum DeviceCapabilityType {
    COLLECTING,
    MONITORING,
    WARNING,
    VIDEO,
    UNKNOWN;

    public static DeviceCapabilityType from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        for (DeviceCapabilityType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
