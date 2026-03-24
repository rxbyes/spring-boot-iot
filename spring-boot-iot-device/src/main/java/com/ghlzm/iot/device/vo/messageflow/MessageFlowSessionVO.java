package com.ghlzm.iot.device.vo.messageflow;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * message-flow 会话 VO。
 */
@Data
public class MessageFlowSessionVO {

    private String sessionId;
    private String transportMode;
    private String status;
    private LocalDateTime submittedAt;
    private String traceId;
    private String deviceCode;
    private String topic;
    private Boolean correlationPending;
    private MessageFlowTimelineVO timeline;
}
