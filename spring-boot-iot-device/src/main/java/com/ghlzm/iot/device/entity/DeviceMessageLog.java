package com.ghlzm.iot.device.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:30
 */
@Data
@TableName("iot_message_log")
public class DeviceMessageLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long deviceId;
    private Long productId;
    private String traceId;
    private String deviceCode;
    private String productKey;
    private String messageType;
    private String topic;
    private String payload;
    private LocalDateTime reportTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
