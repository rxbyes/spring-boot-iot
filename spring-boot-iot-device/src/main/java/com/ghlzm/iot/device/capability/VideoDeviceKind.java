package com.ghlzm.iot.device.capability;

public enum VideoDeviceKind {
    FIXED_CAMERA,
    PTZ_CAMERA,
    UNKNOWN;

    public static VideoDeviceKind from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        for (VideoDeviceKind kind : values()) {
            if (kind.name().equalsIgnoreCase(value.trim())) {
                return kind;
            }
        }
        return UNKNOWN;
    }
}
