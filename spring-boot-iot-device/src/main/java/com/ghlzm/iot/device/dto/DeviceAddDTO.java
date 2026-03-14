package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceAddDTO {

    @NotBlank
    private String productKey;

    @NotBlank
    private String deviceName;

    @NotBlank
    private String deviceCode;

    private String deviceSecret;

    private String clientId;

    private String username;

    private String password;

    private String firmwareVersion;

    private String ipAddress;

    private String address;

    private String metadataJson;
}
