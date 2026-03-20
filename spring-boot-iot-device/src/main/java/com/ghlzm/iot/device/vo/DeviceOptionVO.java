package com.ghlzm.iot.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 设备下拉选项，避免直接向前端暴露完整设备实体。
 */
@Data
public class DeviceOptionVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long gatewayId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parentDeviceId;
    private String productKey;
    private String productName;
    private String deviceCode;
    private String deviceName;
    private Integer nodeType;
    private Integer onlineStatus;
    private Integer deviceStatus;
}
