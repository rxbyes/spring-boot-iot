package com.ghlzm.iot.device.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备接入失败原始报文归档。
 */
@Data
public class DeviceAccessErrorLog {

    private Long id;
    private Long tenantId;
    private String traceId;
    private String protocolCode;
    private String requestMethod;
    private String failureStage;
    private String deviceCode;
    private String productKey;
    private String gatewayDeviceCode;
    private String subDeviceCode;
    private String topicRouteType;
    private String messageType;
    private String topic;
    private String clientId;
    private Integer payloadSize;
    private String payloadEncoding;
    private Integer payloadTruncated;
    private String rawPayload;
    private String errorCode;
    private String exceptionClass;
    private String errorMessage;
    private String contractSnapshot;
    private LocalDateTime createTime;
    private Integer deleted;
}
