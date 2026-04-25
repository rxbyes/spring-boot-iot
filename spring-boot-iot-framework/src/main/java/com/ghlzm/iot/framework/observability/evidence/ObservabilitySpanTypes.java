package com.ghlzm.iot.framework.observability.evidence;

/**
 * 轻量 Span 类型。
 */
public final class ObservabilitySpanTypes {

    public static final String HTTP_REQUEST = "HTTP_REQUEST";
    public static final String MESSAGE_FLOW = "MESSAGE_FLOW";
    public static final String SCHEDULED_TASK = "SCHEDULED_TASK";
    public static final String SLOW_SQL = "SLOW_SQL";

    private ObservabilitySpanTypes() {
    }
}
