package com.ghlzm.iot.device.dto;

import lombok.Data;

/**
 * 设备接入失败归档查询条件。
 */
@Data
public class DeviceAccessErrorQuery {

    private String traceId;
    private String protocolCode;
    private String failureStage;
    private String deviceCode;
    private String productKey;
    private String topicRouteType;
    private String messageType;
    private String topic;
    private String clientId;
    private String errorCode;
    private String exceptionClass;
}
