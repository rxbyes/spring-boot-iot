package com.ghlzm.iot.framework.observability.messageflow;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 单条 trace 的处理时间线。
 */
@Data
public class MessageFlowTimeline {

    private String traceId;
    private String sessionId;
    private String flowType;
    private String status;
    private String deviceCode;
    private String productKey;
    private String topic;
    private String protocolCode;
    private String messageType;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long totalCostMs;
    private List<MessageFlowStep> steps = new ArrayList<>();
}
