package com.ghlzm.iot.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ObservabilitySlowSpanTrendVO {

    private String bucket;
    private LocalDateTime bucketStart;
    private LocalDateTime bucketEnd;
    private Long totalCount;
    private Long successCount;
    private Long errorCount;
    private Integer errorRate;
    private Long avgDurationMs;
    private Long maxDurationMs;
    private Long p95DurationMs;
    private Long p99DurationMs;
}
