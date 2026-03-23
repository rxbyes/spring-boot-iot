package com.ghlzm.iot.device.service.model;

import lombok.Data;

/**
 * 设备属性元数据。
 */
@Data
public class DevicePropertyMetadata {

    private String identifier;
    private String propertyName;
    private String dataType;
}
