package com.ghlzm.iot.telemetry.service.dto;

import lombok.Data;

import java.util.List;

/**
 * 单指标时序序列。
 */
@Data
public class TelemetryHistoryBatchSeries {

    private String identifier;
    private String displayName;
    private String seriesType;
    private List<TelemetryHistoryBucketPoint> buckets = List.of();
}
