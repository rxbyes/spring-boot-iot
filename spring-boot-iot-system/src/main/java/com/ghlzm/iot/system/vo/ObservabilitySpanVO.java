package com.ghlzm.iot.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ObservabilitySpanVO {

    private Long id;
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
    private String tagsJson;
    private LocalDateTime createTime;
}
