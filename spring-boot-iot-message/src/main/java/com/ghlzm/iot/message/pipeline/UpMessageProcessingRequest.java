package com.ghlzm.iot.message.pipeline;

import lombok.Data;

/**
 * 上行 pipeline 入口请求。
 */
@Data
public class UpMessageProcessingRequest {

    private String transportMode;
    private String sessionId;
    private String protocolCode;
    private String productKey;
    private String deviceCode;
    private String topic;
    private String clientId;
    private String tenantId;
    private byte[] payload;
}
