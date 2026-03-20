package com.ghlzm.iot.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备详情视图，给前端台账页返回完整维护信息。
 */
@Data
public class DeviceDetailVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productId;

    private String productKey;

    private String productName;

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

    private String address;

    private String metadataJson;

    private LocalDateTime lastOnlineTime;

    private LocalDateTime lastOfflineTime;

    private LocalDateTime lastReportTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
