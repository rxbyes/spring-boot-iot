package com.ghlzm.iot.telemetry.service.dto;

import lombok.Data;

/**
 * 单个时序桶点位。
 */
@Data
public class TelemetryHistoryBucketPoint {

    private String time;
    private Double value;
    private boolean filled;
}
