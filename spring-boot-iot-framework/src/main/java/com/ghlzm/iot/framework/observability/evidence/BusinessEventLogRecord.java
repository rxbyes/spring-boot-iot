package com.ghlzm.iot.framework.observability.evidence;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务事件证据记录，面向跨域业务动作字典。
 */
@Data
public class BusinessEventLogRecord {

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
    private LocalDateTime occurredAt;
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
