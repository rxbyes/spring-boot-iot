package com.ghlzm.iot.framework.observability.messageflow;

/**
 * message-flow 状态常量。
 */
public final class MessageFlowStatuses {

    public static final String STEP_SUCCESS = "SUCCESS";
    public static final String STEP_FAILED = "FAILED";
    public static final String STEP_SKIPPED = "SKIPPED";

    public static final String SESSION_PUBLISHED = "PUBLISHED";
    public static final String SESSION_PROCESSING = "PROCESSING";
    public static final String SESSION_COMPLETED = "COMPLETED";
    public static final String SESSION_FAILED = "FAILED";

    private MessageFlowStatuses() {
    }
}
