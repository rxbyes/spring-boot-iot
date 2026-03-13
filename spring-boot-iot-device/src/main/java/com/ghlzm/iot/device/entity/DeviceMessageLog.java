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
@TableName("iot_device_message_log")
public class DeviceMessageLog {

    private Long id;
    private Long tenantId;
    private Long deviceId;
    private Long productId;
    private String messageType;
    private String topic;
    private String payload;
    private LocalDateTime reportTime;
    private LocalDateTime createTime;
}

