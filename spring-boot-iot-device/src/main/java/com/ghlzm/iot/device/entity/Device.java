package com.ghlzm.iot.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ghlzm.iot.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 13:55
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iot_device")
public class Device extends BaseEntity {

    private Long productId;
    private Long gatewayId;
    private Long parentDeviceId;
    private String deviceName;
    private String deviceCode;
    private String deviceSecret;
    private String clientId;
    private String username;
    private String password;
    private String protocolCode;
    private Integer nodeType;
    private Integer onlineStatus;
    private Integer activateStatus;
    private Integer deviceStatus;
    private String firmwareVersion;
    private String ipAddress;
    private LocalDateTime lastOnlineTime;
    private LocalDateTime lastOfflineTime;
    private LocalDateTime lastReportTime;
    private String address;
}

