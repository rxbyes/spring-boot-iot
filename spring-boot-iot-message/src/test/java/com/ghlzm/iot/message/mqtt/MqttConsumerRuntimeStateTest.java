package com.ghlzm.iot.message.mqtt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MqttConsumerRuntimeStateTest {

    @Test
    void snapshotShouldTrackRecentConnectDispatchAndFailure() {
        MqttConsumerRuntimeState state = new MqttConsumerRuntimeState();

        state.markConnected();
        state.markSubscribed(List.of("$dp", "/sys/+/+/thing/property/post"));
        state.markMessageReceived();
        state.markDispatchSuccess("trace-success-001");
        state.markFailure("device_validate", "trace-failure-001");

        MqttConsumerRuntimeState.Snapshot snapshot = state.snapshot();
        assertEquals(List.of("$dp", "/sys/+/+/thing/property/post"), snapshot.subscribeTopics());
        assertNotNull(snapshot.lastConnectAt());
        assertNotNull(snapshot.lastMessageAt());
        assertNotNull(snapshot.lastDispatchSuccessAt());
        assertNotNull(snapshot.lastFailureAt());
        assertEquals("device_validate", snapshot.lastFailureStage());
        assertEquals("trace-failure-001", snapshot.lastFailureTraceId());
    }
}
