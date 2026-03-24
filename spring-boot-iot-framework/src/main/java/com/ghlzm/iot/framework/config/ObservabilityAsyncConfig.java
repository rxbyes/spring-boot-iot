package com.ghlzm.iot.framework.config;

import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import com.ghlzm.iot.framework.observability.TraceContextTaskDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 统一线程池与调度器，保证异步链路透传 traceId。
 */
@Configuration
public class ObservabilityAsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityAsyncConfig.class);

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
        return throwable -> log.error(ObservabilityEventLogSupport.summary(
                "scheduled_task",
                "failure",
                null,
                Map.of("handler", "taskScheduler", "errorClass", throwable.getClass().getSimpleName())
        ), throwable);
    }
}
