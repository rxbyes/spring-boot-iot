package com.ghlzm.iot.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 设备更换结果。
 */
@Data
public class DeviceReplaceResultVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long sourceDeviceId;

    private String sourceDeviceCode;

    private String sourceDeviceName;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long targetDeviceId;

    private String targetDeviceCode;

    private String targetDeviceName;
}
