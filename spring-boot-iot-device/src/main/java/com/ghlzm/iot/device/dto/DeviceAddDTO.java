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

    /**
     * 激活状态，1=已激活，0=未激活；为空时沿用系统默认值。
     */
    private Integer activateStatus;

    /**
     * 设备状态，1=启用，0=禁用；为空时默认启用。
     */
    private Integer deviceStatus;

    private String firmwareVersion;

    private String ipAddress;

    private String address;

    private String metadataJson;
}
