package com.ghlzm.iot.framework.observability.messageflow;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单次消息提交会话。
 */
@Data
public class MessageFlowSession {

    private String sessionId;
    private String transportMode;
    private String status;
    private LocalDateTime submittedAt;
    private String traceId;
    private String deviceCode;
    private String topic;
    private Boolean correlationPending;
}
