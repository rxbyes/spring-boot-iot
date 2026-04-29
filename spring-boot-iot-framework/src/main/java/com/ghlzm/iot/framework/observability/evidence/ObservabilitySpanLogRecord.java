package com.ghlzm.iot.framework.observability.evidence;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 轻量调用 Span 证据记录。
 */
@Data
public class ObservabilitySpanLogRecord {

    private Long tenantId;
    private String traceId;
    private Long parentSpanId;
    private String spanType;
    private String spanName;
    private String domainCode;
    private String eventCode;
    private String objectType;
    private String objectId;
    private String transportType;
    private String status;
    private Long durationMs;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorClass;
    private String errorMessage;
    private Map<String, Object> tags = new LinkedHashMap<>();
}
