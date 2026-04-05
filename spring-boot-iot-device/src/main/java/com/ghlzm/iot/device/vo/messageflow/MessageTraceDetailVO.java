package com.ghlzm.iot.device.vo.messageflow;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 链路追踪详情 VO。
 */
@Data
public class MessageTraceDetailVO {

    private Long id;
    private String traceId;
    private String deviceCode;
    private String productKey;
    private String messageType;
    private String topic;
    private String rawPayload;
    private String decryptedPayload;
    private Map<String, Object> decodedPayload = new LinkedHashMap<>();
    private LocalDateTime reportTime;
    private LocalDateTime createTime;
    private MessageFlowTimelineVO timeline;
    private Boolean timelineLookupError;
}
