package com.ghlzm.iot.telemetry.service.dto;

import lombok.Data;

import java.util.List;

/**
 * 时序历史批量查询响应。
 */
@Data
public class TelemetryHistoryBatchResponse {

    private Long deviceId;
    private String rangeCode;
    private String bucket;
    private List<TelemetryHistoryBatchSeries> points = List.of();
}
