package com.ghlzm.iot.message.pipeline;

import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStages;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * 挂载到原始异常上的 pipeline 失败元数据，避免 MQTT 消费端在异常链路上丢失阶段与原始上下文。
 */
public final class MessagePipelineFailureMetadata extends RuntimeException {

    private final String stage;
    private final String failureStage;
    private final String traceId;
    private final RawDeviceMessage rawDeviceMessage;
    private final DeviceUpMessage upMessage;

    private MessagePipelineFailureMetadata(String stage,
                                           String traceId,
                                           RawDeviceMessage rawDeviceMessage,
                                           DeviceUpMessage upMessage) {
        super("message-pipeline-failure-metadata", null, false, false);
        this.stage = normalizeText(stage);
        this.failureStage = mapFailureStage(stage);
        this.traceId = normalizeText(traceId);
        this.rawDeviceMessage = rawDeviceMessage;
        this.upMessage = upMessage;
    }

    public static void attach(Throwable throwable,
                              String stage,
                              String traceId,
                              RawDeviceMessage rawDeviceMessage,
                              DeviceUpMessage upMessage) {
        if (throwable == null || find(throwable).isPresent()) {
            return;
        }
        throwable.addSuppressed(new MessagePipelineFailureMetadata(stage, traceId, rawDeviceMessage, upMessage));
    }

    public static Optional<MessagePipelineFailureMetadata> find(Throwable throwable) {
        if (throwable == null) {
            return Optional.empty();
        }
        for (Throwable suppressed : throwable.getSuppressed()) {
            if (suppressed instanceof MessagePipelineFailureMetadata metadata) {
                return Optional.of(metadata);
            }
        }
        return Optional.empty();
    }

    public String getStage() {
        return stage;
    }

    public String getFailureStage() {
        return failureStage;
    }

    public String getTraceId() {
        return traceId;
    }

    public RawDeviceMessage getRawDeviceMessage() {
        return rawDeviceMessage;
    }

    public DeviceUpMessage getUpMessage() {
        return upMessage;
    }

    public RawDeviceMessage resolveRawDeviceMessage() {
        if (rawDeviceMessage != null) {
            return rawDeviceMessage;
        }
        if (upMessage == null) {
            return null;
        }
        RawDeviceMessage rebuilt = new RawDeviceMessage();
        rebuilt.setTraceId(resolveText(traceId, upMessage.getTraceId()));
        rebuilt.setDeviceCode(normalizeText(upMessage.getDeviceCode()));
        rebuilt.setProductKey(normalizeText(upMessage.getProductKey()));
        rebuilt.setProtocolCode(normalizeText(upMessage.getProtocolCode()));
        rebuilt.setMessageType(normalizeText(upMessage.getMessageType()));
        rebuilt.setTopic(normalizeText(upMessage.getTopic()));
        return rebuilt;
    }

    private static String mapFailureStage(String stage) {
        if (!StringUtils.hasText(stage)) {
            return null;
        }
        String normalizedStage = stage.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedStage) {
            case MessageFlowStages.INGRESS -> "ingress";
            case MessageFlowStages.TOPIC_ROUTE -> "topic_route";
            case MessageFlowStages.PROTOCOL_DECODE -> "protocol_decode";
            case MessageFlowStages.DEVICE_CONTRACT -> "device_contract";
            case MessageFlowStages.MESSAGE_LOG -> "message_log";
            case MessageFlowStages.PAYLOAD_APPLY -> "payload_apply";
            case MessageFlowStages.TELEMETRY_PERSIST -> "telemetry_persist";
            case MessageFlowStages.DEVICE_STATE -> "device_state";
            case MessageFlowStages.RISK_DISPATCH -> "risk_dispatch";
            case MessageFlowStages.COMPLETE -> "complete";
            default -> stage.trim().toLowerCase(Locale.ROOT);
        };
    }

    private static String resolveText(String primary, String fallback) {
        String normalizedPrimary = normalizeText(primary);
        return normalizedPrimary != null ? normalizedPrimary : normalizeText(fallback);
    }

    private static String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
