package com.ghlzm.iot.framework.config;

import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import com.ghlzm.iot.framework.observability.TraceContextTaskDecorator;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.framework.observability.evidence.BusinessEventLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceStatus;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 统一线程池与调度器，保证异步链路透传 traceId。
 */
@Configuration
public class ObservabilityAsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityAsyncConfig.class);
    private ObservabilityEvidenceRecorder evidenceRecorder = ObservabilityEvidenceRecorder.noop();

    @Autowired(required = false)
    public void setObservabilityEvidenceRecorder(ObservabilityEvidenceRecorder evidenceRecorder) {
        if (evidenceRecorder != null) {
            this.evidenceRecorder = evidenceRecorder;
        }
    }

    @Bean
    public TaskDecorator traceContextTaskDecorator() {
        return new TraceContextTaskDecorator();
    }

    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    public Executor applicationTaskExecutor(TaskDecorator traceContextTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("iot-app-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.setTaskDecorator(traceContextTaskDecorator);
        executor.initialize();
        return executor;
    }

    @Bean(name = "taskScheduler")
    public TaskScheduler taskScheduler(TaskDecorator traceContextTaskDecorator) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("iot-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.setTaskDecorator(traceContextTaskDecorator);
        scheduler.setErrorHandler(buildSchedulerErrorHandler());
        scheduler.initialize();
        return scheduler;
    }

    private ErrorHandler buildSchedulerErrorHandler() {
        return throwable -> {
            String traceId = TraceContextHolder.currentOrCreate();
            Map<String, Object> details = Map.of(
                    "traceId", traceId,
                    "handler", "taskScheduler",
                    "errorClass", throwable.getClass().getSimpleName()
            );
            log.error(ObservabilityEventLogSupport.summary(
                    "scheduled_task",
                    "failure",
                    null,
                    details
            ), throwable);
            recordScheduledFailure(traceId, throwable, details);
        };
    }

    private void recordScheduledFailure(String traceId, Throwable throwable, Map<String, Object> details) {
        LocalDateTime now = LocalDateTime.now();
        ObservabilitySpanLogRecord span = new ObservabilitySpanLogRecord();
        span.setTenantId(1L);
        span.setTraceId(traceId);
        span.setSpanType(ObservabilitySpanTypes.SCHEDULED_TASK);
        span.setSpanName("taskScheduler");
        span.setDomainCode("platform");
        span.setEventCode("platform.scheduled.failure");
        span.setStatus(ObservabilityEvidenceStatus.FAILURE);
        span.setStartedAt(now);
        span.setFinishedAt(now);
        span.setErrorClass(throwable.getClass().getName());
        span.setErrorMessage(throwable.getMessage());
        span.getTags().putAll(details);
        evidenceRecorder.recordSpan(span);

        BusinessEventLogRecord event = new BusinessEventLogRecord();
        event.setTenantId(1L);
        event.setTraceId(traceId);
        event.setEventCode("platform.scheduled.failure");
        event.setEventName("调度任务执行异常");
        event.setDomainCode("platform");
        event.setActionCode("scheduled_failure");
        event.setObjectType("scheduled_task");
        event.setObjectId("taskScheduler");
        event.setResultStatus(ObservabilityEvidenceStatus.FAILURE);
        event.setSourceType("SCHEDULED_TASK");
        event.setErrorMessage(throwable.getMessage());
        event.setOccurredAt(now);
        event.getMetadata().putAll(details);
        evidenceRecorder.recordBusinessEvent(event);
    }
}
