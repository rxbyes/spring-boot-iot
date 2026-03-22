package com.ghlzm.iot.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ghlzm.iot.common.model.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备在线会话明细。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iot_device_online_session")
public class DeviceOnlineSession extends BaseEntity {

    private Long productId;

    private Long deviceId;

    private String deviceCode;

    private LocalDateTime onlineTime;

    private LocalDateTime lastSeenTime;

    private LocalDateTime offlineTime;

    private Long durationMinutes;
}
