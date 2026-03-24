package com.ghlzm.iot.framework.observability.messageflow;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 链路阶段步骤。
 */
@Data
public class MessageFlowStep {

    private String stage;
    private String handlerClass;
    private String handlerMethod;
    private String status;
    private Long costMs;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Map<String, Object> summary = new LinkedHashMap<>();
    private String errorClass;
    private String errorMessage;
    private String branch;
}
