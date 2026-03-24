package com.ghlzm.iot.device.vo.messageflow;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * message-flow 最近会话 VO。
 */
@Data
public class MessageFlowRecentSessionVO {

    private String sessionId;
    private String traceId;
    private String transportMode;
    private String status;
    private LocalDateTime submittedAt;
    private String deviceCode;
    private String topic;
    private Boolean correlationPending;
    private Boolean timelineAvailable;
}
