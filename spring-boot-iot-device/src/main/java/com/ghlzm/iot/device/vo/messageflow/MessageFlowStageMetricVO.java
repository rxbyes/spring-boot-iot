package com.ghlzm.iot.device.vo.messageflow;

import lombok.Data;

/**
 * message-flow 阶段聚合指标。
 */
@Data
public class MessageFlowStageMetricVO {

    private String stage;
    private Long count;
    private Long failureCount;
    private Long skippedCount;
    private Double avgCostMs;
    private Double p95CostMs;
    private Long maxCostMs;
}
