package com.ghlzm.iot.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ObservabilityBusinessEventVO {

    private Long id;
    private Long tenantId;
    private String traceId;
    private String eventCode;
    private String eventName;
    private String domainCode;
    private String actionCode;
    private String objectType;
    private String objectId;
    private String objectName;
    private Long actorUserId;
    private String actorName;
    private String resultStatus;
    private String sourceType;
    private String evidenceType;
    private String evidenceId;
    private String requestMethod;
    private String requestUri;
    private Long durationMs;
    private String errorCode;
    private String errorMessage;
    private String metadataJson;
    private LocalDateTime occurredAt;
    private LocalDateTime createTime;
}
