package com.ghlzm.iot.device.capability;

public enum WarningDeviceKind {
    BROADCAST,
    LED,
    FLASH,
    UNKNOWN;

    public static WarningDeviceKind from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        for (WarningDeviceKind kind : values()) {
            if (kind.name().equalsIgnoreCase(value.trim())) {
                return kind;
            }
        }
        return UNKNOWN;
    }
}
