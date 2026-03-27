package com.ghlzm.iot.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备台账分页项，返回列表页需要展示的主数据。
 */
@Data
public class DevicePageVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long gatewayId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parentDeviceId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long sourceRecordId;

    private String productKey;

    private String productName;

    private String gatewayDeviceCode;

    private String gatewayDeviceName;

    private String parentDeviceCode;

    private String parentDeviceName;

    private String deviceName;

    private String deviceCode;

    private String protocolCode;

    private Integer nodeType;

    private Integer onlineStatus;

    private Integer activateStatus;

    private Integer deviceStatus;

    private Integer registrationStatus;

    private String assetSourceType;

    private String firmwareVersion;

    private String ipAddress;

    private String address;

    private String lastFailureStage;

    private String lastErrorMessage;

    private String lastReportTopic;

    private String lastTraceId;

    private String lastPayload;

    private LocalDateTime lastOnlineTime;

    private LocalDateTime lastOfflineTime;

    private LocalDateTime lastReportTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
