package com.ghlzm.iot.message.mqtt;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MQTT 消费端运行态快照，用于 actuator 健康检查与联调排障。
 */
@Component
public class MqttConsumerRuntimeState {

    private volatile List<String> subscribeTopics = List.of();
    private volatile LocalDateTime lastConnectAt;
    private volatile LocalDateTime lastMessageAt;
    private volatile LocalDateTime lastDispatchSuccessAt;
    private volatile LocalDateTime lastFailureAt;
    private volatile String lastFailureStage;
    private volatile String lastFailureTraceId;

    public synchronized void markConnected() {
        lastConnectAt = LocalDateTime.now();
    }

    public synchronized void markSubscribed(List<String> topics) {
        subscribeTopics = topics == null ? List.of() : List.copyOf(topics);
    }

    public synchronized void markMessageReceived() {
        lastMessageAt = LocalDateTime.now();
    }

    public synchronized void markDispatchSuccess(String traceId) {
        lastDispatchSuccessAt = LocalDateTime.now();
    }

    public synchronized void markFailure(String failureStage, String traceId) {
        lastFailureAt = LocalDateTime.now();
        if (failureStage != null && !failureStage.isBlank()) {
            lastFailureStage = failureStage.trim();
        }
        if (traceId != null && !traceId.isBlank()) {
            lastFailureTraceId = traceId.trim();
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(
                subscribeTopics,
                lastConnectAt,
                lastMessageAt,
                lastDispatchSuccessAt,
                lastFailureAt,
                lastFailureStage,
                lastFailureTraceId
        );
    }

    public record Snapshot(List<String> subscribeTopics,
                           LocalDateTime lastConnectAt,
                           LocalDateTime lastMessageAt,
                           LocalDateTime lastDispatchSuccessAt,
                           LocalDateTime lastFailureAt,
                           String lastFailureStage,
                           String lastFailureTraceId) {
    }
}
