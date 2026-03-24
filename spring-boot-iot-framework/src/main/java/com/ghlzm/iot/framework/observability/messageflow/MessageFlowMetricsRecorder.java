package com.ghlzm.iot.framework.observability.messageflow;

import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * message-flow 指标记录器。
 */
@Component
public class MessageFlowMetricsRecorder {

    public static final String SESSION_TOTAL_METRIC = "iot.message.flow.session.total";
    public static final String STAGE_DURATION_METRIC = "iot.message.flow.stage.duration";
    public static final String CORRELATION_TOTAL_METRIC = "iot.message.flow.correlation.total";
    public static final String LOOKUP_TOTAL_METRIC = "iot.message.flow.lookup.total";

    public static final String CORRELATION_RESULT_PUBLISHED = "published";
    public static final String CORRELATION_RESULT_MATCHED = "matched";
    public static final String CORRELATION_RESULT_MISSED = "missed";

    public static final String LOOKUP_TARGET_SESSION = "session";
    public static final String LOOKUP_TARGET_TRACE = "trace";
    public static final String LOOKUP_RESULT_HIT = "hit";
    public static final String LOOKUP_RESULT_MISS = "miss";
    public static final String LOOKUP_RESULT_ERROR = "error";

    private static final Logger messageFlowLogger =
            LoggerFactory.getLogger(MessageFlowLoggingConstants.MESSAGE_FLOW_LOGGER_NAME);

    private static final List<String> STAGE_ORDER = List.of(
            MessageFlowStages.INGRESS,
            MessageFlowStages.TOPIC_ROUTE,
            MessageFlowStages.PROTOCOL_DECODE,
            MessageFlowStages.DEVICE_CONTRACT,
            MessageFlowStages.MESSAGE_LOG,
            MessageFlowStages.PAYLOAD_APPLY,
            MessageFlowStages.TELEMETRY_PERSIST,
            MessageFlowStages.DEVICE_STATE,
            MessageFlowStages.RISK_DISPATCH,
            MessageFlowStages.COMPLETE
    );

    private final MeterRegistry meterRegistry;
    private final LocalDateTime runtimeStartedAt = LocalDateTime.now();
    private final ConcurrentMap<String, LongAdder> sessionCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> correlationCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> lookupCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, StageAccumulator> stageAccumulators = new ConcurrentHashMap<>();
    private final Set<String> correlationMissedSessions = ConcurrentHashMap.newKeySet();

    public MessageFlowMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordSession(String transportMode, String status) {
        String normalizedTransport = normalizeTransportMode(transportMode);
        String normalizedStatus = normalizeStatus(status);
        sessionCounters.computeIfAbsent(normalizedTransport + "|" + normalizedStatus, key -> new LongAdder()).increment();
        meterRegistry.counter(
                SESSION_TOTAL_METRIC,
                "transportMode", normalizedTransport,
                "status", normalizedStatus
        ).increment();
    }

    public void recordStageDuration(String stage, String transportMode, String status, long costMs) {
        String normalizedStage = normalizeStage(stage);
        String normalizedTransport = normalizeTransportMode(transportMode);
        String normalizedStatus = normalizeStatus(status);
        long safeCostMs = Math.max(costMs, 0L);

        stageAccumulators.computeIfAbsent(normalizedStage, StageAccumulator::new).record(normalizedStatus, safeCostMs);
        Timer.builder(STAGE_DURATION_METRIC)
                .tag("stage", normalizedStage)
                .tag("transportMode", normalizedTransport)
                .tag("status", normalizedStatus)
                .publishPercentiles(0.95)
                .register(meterRegistry)
                .record(safeCostMs, TimeUnit.MILLISECONDS);
    }

    public void recordCorrelation(String result) {
        String normalizedResult = normalizeResult(result);
        correlationCounters.computeIfAbsent(normalizedResult, key -> new LongAdder()).increment();
        meterRegistry.counter(CORRELATION_TOTAL_METRIC, "result", normalizedResult).increment();
    }

    public boolean recordCorrelationMissOnce(String sessionId,
                                             String transportMode,
                                             String deviceCode,
                                             String topic,
                                             LocalDateTime submittedAt) {
        if (!hasText(sessionId) || !correlationMissedSessions.add(sessionId.trim())) {
            return false;
        }
        recordCorrelation(CORRELATION_RESULT_MISSED);
        if (messageFlowLogger.isInfoEnabled()) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("sessionId", sessionId.trim());
            details.put("transportMode", normalizeTransportMode(transportMode));
            details.put("deviceCode", normalizeText(deviceCode));
            details.put("topic", normalizeText(topic));
            details.put("submittedAt", submittedAt);
            messageFlowLogger.info(ObservabilityEventLogSupport.summary(
                    "message_flow_correlation_miss",
                    "timeout",
                    0L,
                    details
            ));
        }
        return true;
    }

    public void recordLookup(String target, String result) {
        String normalizedTarget = normalizeTarget(target);
        String normalizedResult = normalizeResult(result);
        lookupCounters.computeIfAbsent(normalizedTarget + "|" + normalizedResult, key -> new LongAdder()).increment();
        meterRegistry.counter(
                LOOKUP_TOTAL_METRIC,
                "target", normalizedTarget,
                "result", normalizedResult
        ).increment();
    }

    public OverviewSnapshot snapshot() {
        List<SessionCountSnapshot> sessionCounts = new ArrayList<>();
        for (Map.Entry<String, LongAdder> entry : sessionCounters.entrySet()) {
            String[] keyParts = splitKey(entry.getKey(), 2);
            sessionCounts.add(new SessionCountSnapshot(keyParts[0], keyParts[1], entry.getValue().sum()));
        }
        sessionCounts.sort(Comparator
                .comparing(SessionCountSnapshot::transportMode)
                .thenComparing(SessionCountSnapshot::status));

        List<CorrelationCountSnapshot> correlationCounts = new ArrayList<>();
        for (Map.Entry<String, LongAdder> entry : correlationCounters.entrySet()) {
            correlationCounts.add(new CorrelationCountSnapshot(entry.getKey(), entry.getValue().sum()));
        }
        correlationCounts.sort(Comparator.comparing(CorrelationCountSnapshot::result));

        List<LookupCountSnapshot> lookupCounts = new ArrayList<>();
        for (Map.Entry<String, LongAdder> entry : lookupCounters.entrySet()) {
            String[] keyParts = splitKey(entry.getKey(), 2);
            lookupCounts.add(new LookupCountSnapshot(keyParts[0], keyParts[1], entry.getValue().sum()));
        }
        lookupCounts.sort(Comparator
                .comparing(LookupCountSnapshot::target)
                .thenComparing(LookupCountSnapshot::result));

        List<StageMetricSnapshot> stageMetrics = new ArrayList<>();
        for (Map.Entry<String, StageAccumulator> entry : stageAccumulators.entrySet()) {
            stageMetrics.add(entry.getValue().snapshot());
        }
        stageMetrics.sort(Comparator
                .comparingInt((StageMetricSnapshot snapshot) -> stageOrder(snapshot.stage()))
                .thenComparing(StageMetricSnapshot::stage));

        return new OverviewSnapshot(runtimeStartedAt, sessionCounts, correlationCounts, lookupCounts, stageMetrics);
    }

    private String[] splitKey(String value, int expectedSize) {
        String[] parts = value.split("\\|", expectedSize);
        if (parts.length == expectedSize) {
            return parts;
        }
        String[] fallback = new String[expectedSize];
        Arrays.fill(fallback, "UNKNOWN");
        System.arraycopy(parts, 0, fallback, 0, Math.min(parts.length, expectedSize));
        return fallback;
    }

    private int stageOrder(String stage) {
        int index = STAGE_ORDER.indexOf(stage);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private String normalizeTarget(String target) {
        if (!hasText(target)) {
            return "unknown";
        }
        return target.trim().toLowerCase();
    }

    private String normalizeResult(String result) {
        if (!hasText(result)) {
            return "unknown";
        }
        return result.trim().toLowerCase();
    }

    private String normalizeStage(String stage) {
        if (!hasText(stage)) {
            return "UNKNOWN";
        }
        return stage.trim().toUpperCase();
    }

    private String normalizeTransportMode(String transportMode) {
        if (!hasText(transportMode)) {
            return "UNKNOWN";
        }
        return transportMode.trim().toUpperCase();
    }

    private String normalizeStatus(String status) {
        if (!hasText(status)) {
            return "UNKNOWN";
        }
        return status.trim().toUpperCase();
    }

    private String normalizeText(String value) {
        return hasText(value) ? value.trim() : "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record OverviewSnapshot(LocalDateTime runtimeStartedAt,
                                   List<SessionCountSnapshot> sessionCounts,
                                   List<CorrelationCountSnapshot> correlationCounts,
                                   List<LookupCountSnapshot> lookupCounts,
                                   List<StageMetricSnapshot> stageMetrics) {
    }

    public record SessionCountSnapshot(String transportMode, String status, long count) {
    }

    public record CorrelationCountSnapshot(String result, long count) {
    }

    public record LookupCountSnapshot(String target, String result, long count) {
    }

    public record StageMetricSnapshot(String stage,
                                      long count,
                                      long failureCount,
                                      long skippedCount,
                                      double avgCostMs,
                                      double p95CostMs,
                                      long maxCostMs) {
    }

    private static final class StageAccumulator {

        private static final int RESERVOIR_SIZE = 512;

        private final String stage;
        private final LongAdder count = new LongAdder();
        private final LongAdder failureCount = new LongAdder();
        private final LongAdder skippedCount = new LongAdder();
        private final LongAdder totalCostMs = new LongAdder();
        private final LongAccumulator maxCostMs = new LongAccumulator(Long::max, 0L);
        private final long[] reservoir = new long[RESERVOIR_SIZE];
        private int reservoirCount;
        private int reservoirCursor;

        private StageAccumulator(String stage) {
            this.stage = stage;
        }

        private synchronized void record(String status, long costMs) {
            count.increment();
            totalCostMs.add(costMs);
            maxCostMs.accumulate(costMs);
            if (MessageFlowStatuses.STEP_FAILED.equalsIgnoreCase(status)) {
                failureCount.increment();
            }
            if (MessageFlowStatuses.STEP_SKIPPED.equalsIgnoreCase(status)) {
                skippedCount.increment();
            }
            reservoir[reservoirCursor] = costMs;
            reservoirCursor = (reservoirCursor + 1) % RESERVOIR_SIZE;
            if (reservoirCount < RESERVOIR_SIZE) {
                reservoirCount++;
            }
        }

        private synchronized StageMetricSnapshot snapshot() {
            long currentCount = count.sum();
            long currentFailureCount = failureCount.sum();
            long currentSkippedCount = skippedCount.sum();
            long currentTotalCostMs = totalCostMs.sum();
            long currentMaxCostMs = maxCostMs.get();
            double avgCostMs = currentCount < 1 ? 0D : ((double) currentTotalCostMs) / currentCount;
            double p95CostMs = 0D;
            if (reservoirCount > 0) {
                long[] samples = Arrays.copyOf(reservoir, reservoirCount);
                Arrays.sort(samples);
                int index = Math.max(0, (int) Math.ceil(samples.length * 0.95D) - 1);
                p95CostMs = samples[index];
            }
            return new StageMetricSnapshot(
                    stage,
                    currentCount,
                    currentFailureCount,
                    currentSkippedCount,
                    avgCostMs,
                    p95CostMs,
                    currentMaxCostMs
            );
        }
    }
}
