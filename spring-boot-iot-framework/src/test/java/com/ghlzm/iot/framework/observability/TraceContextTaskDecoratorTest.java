package com.ghlzm.iot.framework.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TraceContextTaskDecoratorTest {

    @AfterEach
    void tearDown() {
        TraceContextHolder.clear();
        MDC.clear();
    }

    @Test
    void decorateShouldPropagateAndRestoreTraceContext() {
        TraceContextHolder.bindTraceId("trace-http-001");
        MDC.put("tenantId", "tenant-demo");
        TraceContextTaskDecorator decorator = new TraceContextTaskDecorator();
        AtomicReference<String> nestedTraceId = new AtomicReference<>();
        AtomicReference<String> nestedTenantId = new AtomicReference<>();

        Runnable decorated = decorator.decorate(() -> {
            nestedTraceId.set(TraceContextHolder.getTraceId());
            nestedTenantId.set(MDC.get("tenantId"));
        });

        TraceContextHolder.bindTraceId("trace-parent-restore");
        MDC.put("tenantId", "tenant-parent");
        decorated.run();

        assertEquals("trace-http-001", nestedTraceId.get());
        assertEquals("tenant-demo", nestedTenantId.get());
        assertEquals("trace-parent-restore", TraceContextHolder.getTraceId());
        assertEquals("tenant-parent", MDC.get("tenantId"));
    }

    @Test
    void decorateShouldCreateTraceIdWhenParentContextMissing() {
        TraceContextTaskDecorator decorator = new TraceContextTaskDecorator();
        AtomicReference<String> nestedTraceId = new AtomicReference<>();

        Runnable decorated = decorator.decorate(() -> nestedTraceId.set(TraceContextHolder.getTraceId()));
        decorated.run();

        assertNotNull(nestedTraceId.get());
    }
}
