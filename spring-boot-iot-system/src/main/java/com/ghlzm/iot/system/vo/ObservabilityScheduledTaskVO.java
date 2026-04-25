package com.ghlzm.iot.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ObservabilityScheduledTaskVO {

    private Long id;
    private Long tenantId;
    private String traceId;
    private String domainCode;
    private String taskCode;
    private String taskName;
    private String taskClassName;
    private String taskMethodName;
    private String triggerType;
    private String triggerExpression;
    private String initialDelayExpression;
    private Long initialDelayMs;
    private Integer retryCount;
    private String threadName;
    private String status;
    private Long durationMs;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorClass;
    private String errorMessage;
    private String tagsJson;
}
