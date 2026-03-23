package com.ghlzm.iot.framework.observability.messageflow;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageFlowMetricsRecorderTest {

    @Test
    void snapshotShouldAggregateSessionCorrelationLookupAndStageMetrics() {
        MessageFlowMetricsRecorder recorder = new MessageFlowMetricsRecorder(new SimpleMeterRegistry());

        recorder.recordSession("HTTP", MessageFlowStatuses.SESSION_PROCESSING);
        recorder.recordSession("HTTP", MessageFlowStatuses.SESSION_COMPLETED);
        recorder.recordCorrelation(MessageFlowMetricsRecorder.CORRELATION_RESULT_PUBLISHED);
        recorder.recordCorrelation(MessageFlowMetricsRecorder.CORRELATION_RESULT_MATCHED);
        recorder.recordLookup(MessageFlowMetricsRecorder.LOOKUP_TARGET_SESSION, MessageFlowMetricsRecorder.LOOKUP_RESULT_HIT);
        recorder.recordStageDuration(MessageFlowStages.INGRESS, "HTTP", MessageFlowStatuses.STEP_SUCCESS, 12L);
        recorder.recordStageDuration(MessageFlowStages.INGRESS, "HTTP", MessageFlowStatuses.STEP_FAILED, 42L);
        recorder.recordStageDuration(MessageFlowStages.INGRESS, "HTTP", MessageFlowStatuses.STEP_SKIPPED, 6L);

        MessageFlowMetricsRecorder.OverviewSnapshot snapshot = recorder.snapshot();

        assertFalse(snapshot.sessionCounts().isEmpty());
        assertFalse(snapshot.correlationCounts().isEmpty());
        assertFalse(snapshot.lookupCounts().isEmpty());
        assertFalse(snapshot.stageMetrics().isEmpty());

        MessageFlowMetricsRecorder.StageMetricSnapshot stageMetric = snapshot.stageMetrics().get(0);
        assertEquals(MessageFlowStages.INGRESS, stageMetric.stage());
        assertEquals(3L, stageMetric.count());
        assertEquals(1L, stageMetric.failureCount());
        assertEquals(1L, stageMetric.skippedCount());
        assertEquals(42L, stageMetric.maxCostMs());
        assertTrue(stageMetric.p95CostMs() >= 12D);
    }

    @Test
    void recordCorrelationMissOnceShouldBeIdempotentPerSession() {
        MessageFlowMetricsRecorder recorder = new MessageFlowMetricsRecorder(new SimpleMeterRegistry());

        assertTrue(recorder.recordCorrelationMissOnce(
                "session-001",
                "MQTT",
                "demo-device-01",
                "$dp",
                LocalDateTime.of(2026, 3, 23, 10, 5)
        ));
        assertFalse(recorder.recordCorrelationMissOnce(
                "session-001",
                "MQTT",
                "demo-device-01",
                "$dp",
                LocalDateTime.of(2026, 3, 23, 10, 5)
        ));

        long missedCount = recorder.snapshot().correlationCounts().stream()
                .filter(item -> MessageFlowMetricsRecorder.CORRELATION_RESULT_MISSED.equals(item.result()))
                .mapToLong(MessageFlowMetricsRecorder.CorrelationCountSnapshot::count)
                .sum();
        assertEquals(1L, missedCount);
    }
}
