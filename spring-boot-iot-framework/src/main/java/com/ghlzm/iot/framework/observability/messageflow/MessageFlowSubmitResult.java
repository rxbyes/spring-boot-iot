package com.ghlzm.iot.framework.observability.messageflow;

import lombok.Data;

/**
 * 提交后返回的最小会话结果。
 */
@Data
public class MessageFlowSubmitResult {

    private String sessionId;
    private String traceId;
    private String status;
    private Boolean timelineAvailable;
    private Boolean correlationPending;
}
