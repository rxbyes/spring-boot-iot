package com.ghlzm.iot.device.vo;

import lombok.Data;

/**
 * 设备下拉选项，避免直接向前端暴露完整设备实体。
 */
@Data
public class DeviceOptionVO {

    private Long id;
    private Long productId;
    private String deviceCode;
    private String deviceName;
    private Integer onlineStatus;
}
