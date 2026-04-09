package com.ghlzm.iot.telemetry.service.dto;

import lombok.Data;

import java.util.List;

/**
 * 时序历史批量查询请求。
 */
@Data
public class TelemetryHistoryBatchRequest {

    private Long deviceId;
    private List<String> identifiers = List.of();
    private String rangeCode;
    private String fillPolicy;
}
