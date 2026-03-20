package com.ghlzm.iot.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 设备更换请求体。
 */
@Data
public class DeviceReplaceDTO {

    /**
     * 为空时沿用原设备所属产品。
     */
    private String productKey;

    @NotBlank(message = "请输入新设备名称")
    private String deviceName;

    @NotBlank(message = "请输入新设备编码")
    private String deviceCode;

    private String deviceSecret;

    private String clientId;

    private String username;

    private String password;

    private Integer activateStatus;

    private Integer deviceStatus;

    private String firmwareVersion;

    private String ipAddress;

    private String address;

    private String metadataJson;
}
