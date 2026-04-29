package com.ghlzm.iot.device.capability;

public record ProductCapabilityMetadata(
        DeviceCapabilityType capabilityType,
        WarningDeviceKind warningDeviceKind,
        VideoDeviceKind videoDeviceKind
) {

    public static ProductCapabilityMetadata unknown() {
        return new ProductCapabilityMetadata(
                DeviceCapabilityType.UNKNOWN,
                WarningDeviceKind.UNKNOWN,
                VideoDeviceKind.UNKNOWN
        );
    }
}
