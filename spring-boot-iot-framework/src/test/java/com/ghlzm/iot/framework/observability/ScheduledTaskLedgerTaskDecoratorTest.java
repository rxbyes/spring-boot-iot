package com.ghlzm.iot.framework.observability;

import com.ghlzm.iot.framework.observability.evidence.BusinessEventLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceStatus;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ScheduledTaskLedgerTaskDecoratorTest {

    @AfterEach
    void tearDown() {
        TraceContextHolder.clear();
    }

    @Test
    void decorateShouldRecordSuccessfulScheduledRunWithTaskMetadata() throws Exception {
        RecordingEvidenceRecorder recorder = new RecordingEvidenceRecorder();
        ScheduledTaskLedgerTaskDecorator decorator =
                new ScheduledTaskLedgerTaskDecorator(new TraceContextTaskDecorator(), recorder);
        SampleScheduledBean target = new SampleScheduledBean();
        Runnable decorated = decorator.decorate(buildScheduledRunnable(target, "sweep"));

        decorated.run();

        assertEquals(1, recorder.spans.size());
        assertEquals(1, recorder.events.size());
        ObservabilitySpanLogRecord span = recorder.spans.get(0);
        assertNotNull(span.getTraceId());
        assertEquals(ObservabilitySpanTypes.SCHEDULED_TASK, span.getSpanType());
        assertEquals("SampleScheduledBean#sweep", span.getSpanName());
        assertEquals("platform.scheduled.run", span.getEventCode());
        assertEquals("scheduled_task", span.getObjectType());
        assertEquals("SampleScheduledBean#sweep", span.getObjectId());
        assertEquals("FIXED_DELAY", span.getTransportType());
        assertEquals(ObservabilityEvidenceStatus.SUCCESS, span.getStatus());
        assertEquals("platform", span.getDomainCode());
        assertEquals("SampleScheduledBean#sweep", span.getTags().get("taskCode"));
        assertEquals("sweep", span.getTags().get("taskMethodName"));
        assertEquals("FIXED_DELAY", span.getTags().get("triggerType"));
        assertEquals("30000", span.getTags().get("triggerExpression"));
        assertEquals(5000L, span.getTags().get("initialDelayMs"));
        assertEquals(0, span.getTags().get("retryCount"));

        BusinessEventLogRecord event = recorder.events.get(0);
        assertEquals(span.getTraceId(), event.getTraceId());
        assertEquals("platform.scheduled.run", event.getEventCode());
        assertEquals("调度任务执行", event.getEventName());
        assertEquals("scheduled_run", event.getActionCode());
        assertEquals("scheduled_task", event.getObjectType());
        assertEquals("SampleScheduledBean#sweep", event.getObjectId());
        assertEquals(ObservabilityEvidenceStatus.SUCCESS, event.getResultStatus());
    }

    @Test
    void decorateShouldRecordFailureAndKeepSchedulerFlowAlive() throws Exception {
        RecordingEvidenceRecorder recorder = new RecordingEvidenceRecorder();
        ScheduledTaskLedgerTaskDecorator decorator =
                new ScheduledTaskLedgerTaskDecorator(new TraceContextTaskDecorator(), recorder);
        SampleScheduledBean target = new SampleScheduledBean();
        Runnable decorated = decorator.decorate(buildScheduledRunnable(target, "failFast"));

        assertDoesNotThrow(decorated::run);
        assertEquals(1, recorder.spans.size());
        assertEquals(1, recorder.events.size());
        assertEquals(1, target.failureInvocations.get());

        ObservabilitySpanLogRecord span = recorder.spans.get(0);
        assertEquals(ObservabilityEvidenceStatus.FAILURE, span.getStatus());
        assertEquals("FIXED_RATE", span.getTransportType());
        assertEquals("IllegalStateException", span.getTags().get("errorClass"));
        assertEquals("FIXED_RATE", span.getTags().get("triggerType"));
        assertEquals("15000", span.getTags().get("triggerExpression"));
        assertEquals("boom", span.getErrorMessage());

        BusinessEventLogRecord event = recorder.events.get(0);
        assertEquals(ObservabilityEvidenceStatus.FAILURE, event.getResultStatus());
        assertEquals("boom", event.getErrorMessage());
        assertEquals("FIXED_RATE", event.getMetadata().get("triggerType"));
    }

    @Test
    void decorateShouldIgnoreInaccessibleJdkScheduledFutureFields() {
        ScheduledTaskLedgerTaskDecorator decorator =
                new ScheduledTaskLedgerTaskDecorator(new TraceContextTaskDecorator(), new RecordingEvidenceRecorder());
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        try {
            Runnable scheduledFutureTask = (Runnable) executor.scheduleWithFixedDelay(
                    () -> {
                    },
                    1,
                    1,
                    TimeUnit.DAYS
            );

            assertDoesNotThrow(() -> decorator.decorate(scheduledFutureTask));
        } finally {
            executor.shutdownNow();
        }
    }

    private Runnable buildScheduledRunnable(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return new ScheduledMethodRunnable(target, method);
    }

    private static class RecordingEvidenceRecorder implements ObservabilityEvidenceRecorder {

        private final List<ObservabilitySpanLogRecord> spans = new ArrayList<>();
        private final List<BusinessEventLogRecord> events = new ArrayList<>();

        @Override
        public void recordSpan(ObservabilitySpanLogRecord span) {
            spans.add(span);
        }

        @Override
        public void recordBusinessEvent(BusinessEventLogRecord event) {
            events.add(event);
        }
    }

    private static class SampleScheduledBean {

        private final AtomicInteger failureInvocations = new AtomicInteger();

        @Scheduled(initialDelayString = "5000", fixedDelayString = "30000")
        public void sweep() {
            // no-op
        }

        @Scheduled(fixedRateString = "15000")
        public void failFast() {
            failureInvocations.incrementAndGet();
            throw new IllegalStateException("boom");
        }
    }
}
