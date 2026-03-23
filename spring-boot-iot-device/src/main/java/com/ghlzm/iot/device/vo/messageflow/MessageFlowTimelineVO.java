package com.ghlzm.iot.device.vo.messageflow;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * message-flow 时间线 VO。
 */
@Data
public class MessageFlowTimelineVO {

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
    private List<MessageFlowStepVO> steps = new ArrayList<>();
}
