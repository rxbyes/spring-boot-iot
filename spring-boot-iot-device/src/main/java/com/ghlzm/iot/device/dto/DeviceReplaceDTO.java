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

    /**
     * 父设备主键，前端表单优先走该字段。
     */
    private Long parentDeviceId;

    /**
     * 父设备编码，批量替换或外部系统对接时可直接传编码。
     */
    private String parentDeviceCode;

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
