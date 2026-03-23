package com.ghlzm.iot.framework.observability.messageflow;

/**
 * 固定消息链路阶段。
 */
public final class MessageFlowStages {

    public static final String INGRESS = "INGRESS";
    public static final String TOPIC_ROUTE = "TOPIC_ROUTE";
    public static final String PROTOCOL_DECODE = "PROTOCOL_DECODE";
    public static final String DEVICE_CONTRACT = "DEVICE_CONTRACT";
    public static final String MESSAGE_LOG = "MESSAGE_LOG";
    public static final String PAYLOAD_APPLY = "PAYLOAD_APPLY";
    public static final String TELEMETRY_PERSIST = "TELEMETRY_PERSIST";
    public static final String DEVICE_STATE = "DEVICE_STATE";
    public static final String RISK_DISPATCH = "RISK_DISPATCH";
    public static final String COMPLETE = "COMPLETE";

    private MessageFlowStages() {
    }
}
