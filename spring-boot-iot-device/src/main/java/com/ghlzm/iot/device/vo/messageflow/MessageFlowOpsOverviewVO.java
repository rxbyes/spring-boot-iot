package com.ghlzm.iot.device.vo.messageflow;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * message-flow 运维概览。
 */
@Data
public class MessageFlowOpsOverviewVO {

    private LocalDateTime runtimeStartedAt;
    private List<MessageFlowSessionCountVO> sessionCounts = new ArrayList<>();
    private List<MessageFlowCorrelationCountVO> correlationCounts = new ArrayList<>();
    private List<MessageFlowLookupCountVO> lookupCounts = new ArrayList<>();
    private List<MessageFlowStageMetricVO> stageMetrics = new ArrayList<>();
}
