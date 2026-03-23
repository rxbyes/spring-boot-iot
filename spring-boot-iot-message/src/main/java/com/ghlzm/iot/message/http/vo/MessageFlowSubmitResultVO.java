package com.ghlzm.iot.message.http.vo;

import lombok.Data;

/**
 * message-flow 提交结果。
 */
@Data
public class MessageFlowSubmitResultVO {

    private String sessionId;
    private String traceId;
    private String status;
    private Boolean timelineAvailable;
    private Boolean correlationPending;
}
