package com.ghlzm.iot.framework.observability;

import com.ghlzm.iot.framework.observability.evidence.BusinessEventLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceStatus;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 为调度任务写入统一运行台账，沉淀到既有 span / business event 证据链。
 */
public class ScheduledTaskLedgerTaskDecorator implements TaskDecorator {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskLedgerTaskDecorator.class);
    private static final String EVENT_CODE = "platform.scheduled.run";

    private final TaskDecorator delegate;
    private final ObservabilityEvidenceRecorder evidenceRecorder;

    public ScheduledTaskLedgerTaskDecorator(TaskDecorator delegate,
                                            ObservabilityEvidenceRecorder evidenceRecorder) {
        this.delegate = delegate == null ? runnable -> runnable : delegate;
        this.evidenceRecorder = evidenceRecorder == null ? ObservabilityEvidenceRecorder.noop() : evidenceRecorder;
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        ScheduledTaskMetadata metadata = ScheduledTaskMetadata.resolve(runnable);
        Runnable instrumented = () -> recordRun(runnable, metadata);
        return delegate.decorate(instrumented);
    }

    private void recordRun(Runnable runnable, ScheduledTaskMetadata metadata) {
        String traceId = TraceContextHolder.currentOrCreate();
        LocalDateTime startedAt = LocalDateTime.now();
        Throwable failure = null;
        try {
            runnable.run();
        } catch (Throwable throwable) {
            failure = throwable;
            log.error(ObservabilityEventLogSupport.summary(
                    "scheduled_task",
                    "failure",
                    null,
                    metadata.toLogDetails(traceId, throwable, Thread.currentThread().getName())
            ), throwable);
        } finally {
            LocalDateTime finishedAt = LocalDateTime.now();
            long durationMs = Math.max(0L, ChronoUnit.MILLIS.between(startedAt, finishedAt));
            recordSpan(traceId, metadata, startedAt, finishedAt, durationMs, failure);
            recordBusinessEvent(traceId, metadata, finishedAt, durationMs, failure);
        }
    }

    private void recordSpan(String traceId,
                            ScheduledTaskMetadata metadata,
                            LocalDateTime startedAt,
                            LocalDateTime finishedAt,
                            long durationMs,
                            Throwable failure) {
        ObservabilitySpanLogRecord span = new ObservabilitySpanLogRecord();
        span.setTenantId(1L);
        span.setTraceId(traceId);
        span.setSpanType(ObservabilitySpanTypes.SCHEDULED_TASK);
        span.setSpanName(metadata.taskName);
        span.setDomainCode(metadata.domainCode);
        span.setEventCode(EVENT_CODE);
        span.setObjectType("scheduled_task");
        span.setObjectId(metadata.taskCode);
        span.setTransportType(metadata.triggerType);
        span.setStatus(resolveStatus(failure));
        span.setDurationMs(durationMs);
        span.setStartedAt(startedAt);
        span.setFinishedAt(finishedAt);
        if (failure != null) {
            span.setErrorClass(failure.getClass().getName());
            span.setErrorMessage(failure.getMessage());
        }
        span.getTags().putAll(metadata.toEvidenceTags(traceId, Thread.currentThread().getName(), durationMs, failure));
        evidenceRecorder.recordSpan(span);
    }

    private void recordBusinessEvent(String traceId,
                                     ScheduledTaskMetadata metadata,
                                     LocalDateTime occurredAt,
                                     long durationMs,
                                     Throwable failure) {
        BusinessEventLogRecord event = new BusinessEventLogRecord();
        event.setTenantId(1L);
        event.setTraceId(traceId);
        event.setEventCode(EVENT_CODE);
        event.setEventName("调度任务执行");
        event.setDomainCode(metadata.domainCode);
        event.setActionCode("scheduled_run");
        event.setObjectType("scheduled_task");
        event.setObjectId(metadata.taskCode);
        event.setObjectName(metadata.taskName);
        event.setResultStatus(resolveStatus(failure));
        event.setSourceType("SCHEDULED_TASK");
        event.setDurationMs(durationMs);
        event.setOccurredAt(occurredAt);
        if (failure != null) {
            event.setErrorMessage(failure.getMessage());
        }
        event.getMetadata().putAll(metadata.toEvidenceTags(traceId, Thread.currentThread().getName(), durationMs, failure));
        evidenceRecorder.recordBusinessEvent(event);
    }

    private String resolveStatus(Throwable failure) {
        return failure == null ? ObservabilityEvidenceStatus.SUCCESS : ObservabilityEvidenceStatus.FAILURE;
    }

    private static final class ScheduledTaskMetadata {

        private static final int MAX_UNWRAP_DEPTH = 8;

        private final String taskCode;
        private final String taskName;
        private final String taskClassName;
        private final String taskMethodName;
        private final String domainCode;
        private final String triggerType;
        private final String triggerExpression;
        private final String initialDelayExpression;
        private final Long initialDelayMs;

        private ScheduledTaskMetadata(String taskCode,
                                      String taskName,
                                      String taskClassName,
                                      String taskMethodName,
                                      String domainCode,
                                      String triggerType,
                                      String triggerExpression,
                                      String initialDelayExpression,
                                      Long initialDelayMs) {
            this.taskCode = taskCode;
            this.taskName = taskName;
            this.taskClassName = taskClassName;
            this.taskMethodName = taskMethodName;
            this.domainCode = domainCode;
            this.triggerType = triggerType;
            this.triggerExpression = triggerExpression;
            this.initialDelayExpression = initialDelayExpression;
            this.initialDelayMs = initialDelayMs;
        }

        private static ScheduledTaskMetadata resolve(Runnable runnable) {
            ScheduledMethodRunnable scheduledMethodRunnable = findScheduledMethodRunnable(
                    runnable,
                    0,
                    Collections.newSetFromMap(new IdentityHashMap<>())
            );
            if (scheduledMethodRunnable == null) {
                Class<?> userClass = ClassUtils.getUserClass(runnable.getClass());
                String className = userClass.getName();
                String simpleName = StringUtils.hasText(userClass.getSimpleName())
                        ? userClass.getSimpleName()
                        : "Runnable";
                return new ScheduledTaskMetadata(
                        simpleName,
                        simpleName,
                        className,
                        null,
                        resolveDomainCode(className),
                        "UNKNOWN",
                        null,
                        null,
                        null
                );
            }

            Object target = scheduledMethodRunnable.getTarget();
            Method method = scheduledMethodRunnable.getMethod();
            Class<?> userClass = target == null
                    ? ClassUtils.getUserClass(method.getDeclaringClass())
                    : ClassUtils.getUserClass(target.getClass());
            String className = userClass.getName();
            String simpleName = StringUtils.hasText(userClass.getSimpleName())
                    ? userClass.getSimpleName()
                    : method.getDeclaringClass().getSimpleName();
            String taskCode = simpleName + "#" + method.getName();
            Scheduled scheduled = AnnotatedElementUtils.findMergedAnnotation(method, Scheduled.class);
            String triggerType = resolveTriggerType(scheduled);
            String triggerExpression = resolveTriggerExpression(scheduled, triggerType);
            String initialDelayExpression = resolveInitialDelayExpression(scheduled);
            Long initialDelayMs = resolveInitialDelayMs(scheduled);
            return new ScheduledTaskMetadata(
                    taskCode,
                    taskCode,
                    className,
                    method.getName(),
                    resolveDomainCode(className),
                    triggerType,
                    triggerExpression,
                    initialDelayExpression,
                    initialDelayMs
            );
        }

        private Map<String, Object> toEvidenceTags(String traceId,
                                                   String threadName,
                                                   long durationMs,
                                                   Throwable failure) {
            Map<String, Object> tags = new LinkedHashMap<>();
            tags.put("traceId", traceId);
            tags.put("taskCode", taskCode);
            tags.put("taskName", taskName);
            tags.put("taskClassName", taskClassName);
            tags.put("taskMethodName", taskMethodName);
            tags.put("triggerType", triggerType);
            if (StringUtils.hasText(triggerExpression)) {
                tags.put("triggerExpression", triggerExpression);
            }
            if (StringUtils.hasText(initialDelayExpression)) {
                tags.put("initialDelayExpression", initialDelayExpression);
            }
            if (initialDelayMs != null) {
                tags.put("initialDelayMs", initialDelayMs);
            }
            tags.put("retryCount", 0);
            tags.put("durationMs", durationMs);
            tags.put("threadName", threadName);
            if (failure != null) {
                tags.put("errorClass", failure.getClass().getSimpleName());
            }
            return tags;
        }

        private Map<String, Object> toLogDetails(String traceId,
                                                 Throwable failure,
                                                 String threadName) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("traceId", traceId);
            details.put("taskCode", taskCode);
            details.put("triggerType", triggerType);
            details.put("threadName", threadName);
            if (failure != null) {
                details.put("errorClass", failure.getClass().getSimpleName());
            }
            return details;
        }

        private static ScheduledMethodRunnable findScheduledMethodRunnable(Object candidate,
                                                                          int depth,
                                                                          Set<Object> visited) {
            if (!(candidate instanceof Runnable runnable) || depth > MAX_UNWRAP_DEPTH || !visited.add(candidate)) {
                return null;
            }
            if (runnable instanceof ScheduledMethodRunnable scheduledMethodRunnable) {
                return scheduledMethodRunnable;
            }
            Class<?> cursor = candidate.getClass();
            while (cursor != null && cursor != Object.class) {
                for (Field field : cursor.getDeclaredFields()) {
                    if (!makeAccessible(field)) {
                        continue;
                    }
                    try {
                        Object nested = field.get(candidate);
                        if (!(nested instanceof Runnable)) {
                            continue;
                        }
                        ScheduledMethodRunnable resolved =
                                findScheduledMethodRunnable(nested, depth + 1, visited);
                        if (resolved != null) {
                            return resolved;
                        }
                    } catch (IllegalAccessException ignored) {
                        // ignore inaccessible wrapper fields
                    }
                }
                cursor = cursor.getSuperclass();
            }
            return null;
        }

        private static boolean makeAccessible(Field field) {
            try {
                return field.trySetAccessible();
            } catch (RuntimeException ignored) {
                return false;
            }
        }

        private static String resolveTriggerType(Scheduled scheduled) {
            if (scheduled == null) {
                return "UNKNOWN";
            }
            if (StringUtils.hasText(scheduled.cron())) {
                return "CRON";
            }
            if (StringUtils.hasText(scheduled.fixedDelayString()) || scheduled.fixedDelay() >= 0L) {
                return "FIXED_DELAY";
            }
            if (StringUtils.hasText(scheduled.fixedRateString()) || scheduled.fixedRate() >= 0L) {
                return "FIXED_RATE";
            }
            return "UNKNOWN";
        }

        private static String resolveTriggerExpression(Scheduled scheduled, String triggerType) {
            if (scheduled == null) {
                return null;
            }
            return switch (triggerType) {
                case "CRON" -> normalizeScheduledValue(scheduled.cron(), -1L);
                case "FIXED_DELAY" -> normalizeScheduledValue(scheduled.fixedDelayString(), scheduled.fixedDelay());
                case "FIXED_RATE" -> normalizeScheduledValue(scheduled.fixedRateString(), scheduled.fixedRate());
                default -> null;
            };
        }

        private static String resolveInitialDelayExpression(Scheduled scheduled) {
            if (scheduled == null) {
                return null;
            }
            return normalizeScheduledValue(scheduled.initialDelayString(), scheduled.initialDelay());
        }

        private static Long resolveInitialDelayMs(Scheduled scheduled) {
            if (scheduled == null) {
                return null;
            }
            if (scheduled.initialDelay() >= 0L) {
                return scheduled.initialDelay();
            }
            return parseLong(normalizeScheduledValue(scheduled.initialDelayString(), -1L));
        }

        private static String normalizeScheduledValue(String stringValue, long numericValue) {
            if (StringUtils.hasText(stringValue)) {
                return stringValue.trim();
            }
            if (numericValue >= 0L) {
                return String.valueOf(numericValue);
            }
            return null;
        }

        private static Long parseLong(String value) {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private static String resolveDomainCode(String className) {
            if (!StringUtils.hasText(className)) {
                return "platform";
            }
            if (className.contains(".device.")) {
                return "device";
            }
            if (className.contains(".system.")) {
                return "system";
            }
            if (className.contains(".admin.")) {
                return "admin";
            }
            if (className.contains(".report.")) {
                return "report";
            }
            if (className.contains(".alarm.")) {
                return "alarm";
            }
            if (className.contains(".message.")) {
                return "message";
            }
            if (className.contains(".protocol.")) {
                return "protocol";
            }
            if (className.contains(".telemetry.")) {
                return "telemetry";
            }
            if (className.contains(".rule.")) {
                return "rule";
            }
            if (className.contains(".auth.")) {
                return "auth";
            }
            return "platform";
        }
    }
}
