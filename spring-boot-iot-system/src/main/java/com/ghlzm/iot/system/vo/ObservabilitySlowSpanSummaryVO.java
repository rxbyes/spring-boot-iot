package com.ghlzm.iot.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ObservabilitySlowSpanSummaryVO {

    private String spanType;
    private String domainCode;
    private String eventCode;
    private String objectType;
    private String objectId;
    private Long totalCount;
    private Long avgDurationMs;
    private Long maxDurationMs;
    private String latestTraceId;
    private LocalDateTime latestStartedAt;
}
