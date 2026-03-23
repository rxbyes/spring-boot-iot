package com.ghlzm.iot.system.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ghlzm.iot.framework.config.DiagnosticLoggingConstants;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogFilterTest {

    private record AuditLogRecorder(AuditLogService service, AtomicReference<AuditLog> lastLog, AtomicInteger count) {
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldWriteSlowHttpSummaryToDiagnosticLogger() throws ServletException, IOException {
        AuditLogRecorder recorder = newRecorder();
        IotProperties properties = new IotProperties();
        properties.getObservability().getPerformance().setSlowHttpThresholdMs(5L);
        AuditLogFilter filter = new AuditLogFilter(recorder.service(), properties);
        Logger logger = (Logger) LoggerFactory.getLogger(DiagnosticLoggingConstants.DIAGNOSTIC_ACCESS_LOGGER_NAME);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        try {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/channel/list");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> {
                LockSupport.parkNanos(20_000_000L);
                res.setContentType("application/json");
                res.getWriter().write("{\"code\":200}");
            };

            filter.doFilter(request, response, chain);

            assertEquals(1, appender.list.size());
            String message = appender.list.get(0).getFormattedMessage();
            assertTrue(message.contains("event=\"slow_http_request\""));
            assertTrue(message.contains("uri=\"/api/system/channel/list\""));
            assertTrue(message.contains("status=200"));
        } finally {
            logger.setLevel(originalLevel);
            logger.detachAndStopAllAppenders();
        }
    }

    @Test
    void shouldRecordBodyAndMaskSensitiveFields() throws ServletException, IOException {
        AuditLogRecorder recorder = newRecorder();
        AuditLogFilter filter = new AuditLogFilter(recorder.service());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/system/channel/add");
        request.setContentType("application/json");
        String body = "{\"tenantId\":1,\"config\":\"{\\\"token\\\":\\\"abc123\\\",\\\"password\\\":\\\"123456\\\"}\","
                + "\"token\":\"abc123\",\"password\":\"123456\"}";
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            req.getInputStream().transferTo(OutputStream.nullOutputStream());
            res.setContentType("application/json");
            res.getWriter().write("{\"code\":200,\"data\":{\"token\":\"server-token\",\"password\":\"server-pass\"}}");
        };

        filter.doFilter(request, response, chain);

        AuditLog log = recorder.lastLog().get();

        assertTrue(log.getRequestParams().contains("\"token\":\"***\""));
        assertTrue(log.getRequestParams().contains("\"password\":\"***\""));
        assertTrue(log.getRequestParams().contains("\\\"token\\\":\\\"***\\\""));
        assertTrue(log.getResponseResult().contains("\"token\":\"***\""));
        assertTrue(log.getResponseResult().contains("\"password\":\"***\""));
        assertEquals(1, log.getOperationResult());
    }

    @Test
    void shouldTruncateOversizePayload() throws ServletException, IOException {
        AuditLogRecorder recorder = newRecorder();
        AuditLogFilter filter = new AuditLogFilter(recorder.service());

        String longBody = "x".repeat(6000);
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/device/add");
        request.setContentType("application/json");
        request.setContent(("{\"payload\":\"" + longBody + "\"}").getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            req.getInputStream().transferTo(OutputStream.nullOutputStream());
            res.setContentType("application/json");
            res.getWriter().write("{\"msg\":\"" + longBody + "\"}");
        };

        filter.doFilter(request, response, chain);

        AuditLog log = recorder.lastLog().get();

        assertTrue(log.getRequestParams().startsWith("body: "));
        assertTrue(log.getResponseResult().contains("...(truncated)"));
    }

    @Test
    void shouldUseUtf8ForJsonResponseBody() throws ServletException, IOException {
        AuditLogRecorder recorder = newRecorder();
        AuditLogFilter filter = new AuditLogFilter(recorder.service());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/channel/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) res;
            wrapper.setContentType("application/json");
            wrapper.setCharacterEncoding("ISO-8859-1");
            wrapper.getOutputStream().write("{\"msg\":\"中文验证\"}".getBytes(StandardCharsets.UTF_8));
        };

        filter.doFilter(request, response, chain);

        AuditLog log = recorder.lastLog().get();

        assertTrue(log.getResponseResult().contains("中文验证"));
    }

    @Test
    void shouldMarkBusinessFailureFromResponseEnvelope() throws ServletException, IOException {
        AuditLogRecorder recorder = newRecorder();
        AuditLogFilter filter = new AuditLogFilter(recorder.service());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/device/add");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            res.setContentType("application/json");
            res.getWriter().write("{\"code\":500,\"msg\":\"设备不存在: demo-device-02\"}");
        };

        filter.doFilter(request, response, chain);

        AuditLog log = recorder.lastLog().get();

        assertEquals(0, log.getOperationResult());
        assertEquals("设备不存在: demo-device-02", log.getResultMessage());
    }

    @Test
    void shouldFallbackOperationMethodToPattern() throws ServletException, IOException {
        AuditLogRecorder recorder = newRecorder();
        AuditLogFilter filter = new AuditLogFilter(recorder.service());

        MockHttpServletRequest rawRequest = new MockHttpServletRequest("DELETE", "/api/system/channel/delete/1");
        rawRequest.setAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingPattern",
                "/api/system/channel/delete/{id}");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> res.getWriter().write("{\"code\":200}");
        filter.doFilter(rawRequest, response, chain);

        AuditLog log = recorder.lastLog().get();

        assertEquals("DELETE:/api/system/channel/delete/{id}", log.getOperationMethod());
    }

    @Test
    void shouldSkipAuditApiItself() throws ServletException, IOException {
        AuditLogRecorder recorder = newRecorder();
        AuditLogFilter filter = new AuditLogFilter(recorder.service());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/audit-log/page");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> res.getWriter().write("{\"code\":200}");

        filter.doFilter(request, response, chain);

        assertEquals(0, recorder.count().get());
    }

    private AuditLogRecorder newRecorder() {
        AtomicReference<AuditLog> lastLog = new AtomicReference<>();
        AtomicInteger count = new AtomicInteger(0);
        AuditLogService service = (AuditLogService) Proxy.newProxyInstance(
                AuditLogService.class.getClassLoader(),
                new Class[]{AuditLogService.class},
                (proxy, method, args) -> {
                    if ("addLog".equals(method.getName()) && args != null && args.length == 1) {
                        lastLog.set((AuditLog) args[0]);
                        count.incrementAndGet();
                        return null;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(boolean.class)) {
                        return false;
                    }
                    if (returnType.equals(int.class) || returnType.equals(long.class)
                            || returnType.equals(short.class) || returnType.equals(byte.class)) {
                        return 0;
                    }
                    if (returnType.equals(float.class) || returnType.equals(double.class)) {
                        return 0.0;
                    }
                    return null;
                });
        return new AuditLogRecorder(service, lastLog, count);
    }
}
