package com.ghlzm.iot.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:30
 */
@Data
@TableName("iot_device_property")
public class DeviceProperty {

    private Long id;
    private Long tenantId;
    private Long deviceId;
    private String identifier;
    private String propertyName;
    private String propertyValue;
    private String valueType;
    private LocalDateTime reportTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

