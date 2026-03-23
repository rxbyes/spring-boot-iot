package com.ghlzm.iot.framework.observability;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 在线程池与调度线程中透传 traceId / MDC。
 */
public class TraceContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        String capturedTraceId = TraceContextHolder.getTraceId();
        Map<String, String> capturedMdc = MDC.getCopyOfContextMap();
        return () -> {
            String previousTraceId = TraceContextHolder.getTraceId();
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            try {
                if (capturedMdc != null && !capturedMdc.isEmpty()) {
                    MDC.setContextMap(capturedMdc);
                } else {
                    MDC.clear();
                }
                TraceContextHolder.bindTraceId(capturedTraceId);
                runnable.run();
            } finally {
                if (previousMdc != null && !previousMdc.isEmpty()) {
                    MDC.setContextMap(previousMdc);
                } else {
                    MDC.clear();
                }
                if (previousTraceId != null && !previousTraceId.isBlank()) {
                    TraceContextHolder.bindTraceId(previousTraceId);
                } else {
                    TraceContextHolder.clear();
                }
            }
        };
    }
}
