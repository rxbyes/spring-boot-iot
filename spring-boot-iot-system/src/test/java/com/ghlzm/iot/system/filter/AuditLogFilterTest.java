package com.ghlzm.iot.system.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ghlzm.iot.framework.config.DiagnosticLoggingConstants;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.evidence.BusinessEventLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanLogRecord;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.SQLSyntaxErrorException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogFilterTest {

    private record AuditLogRecorder(AuditLogService service, AtomicReference<AuditLog> lastLog, AtomicInteger count) {
    }

    private static final class EvidenceRecorder implements ObservabilityEvidenceRecorder {
        private final AtomicReference<ObservabilitySpanLogRecord> lastSpan = new AtomicReference<>();
        private final AtomicReference<BusinessEventLogRecord> lastEvent = new AtomicReference<>();
        private final AtomicInteger spanCount = new AtomicInteger();
        private final AtomicInteger eventCount = new AtomicInteger();

        @Override
        public void recordSpan(ObservabilitySpanLogRecord span) {
            lastSpan.set(span);
            spanCount.incrementAndGet();
        }

        @Override
        public void recordBusinessEvent(BusinessEventLogRecord event) {
            lastEvent.set(event);
            eventCount.incrementAndGet();
        }
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
    void shouldRecordHandledHttpExceptionAsSystemError() throws ServletException, IOException {
        AuditLogRecorder recorder = newRecorder();
        AuditLogFilter filter = new AuditLogFilter(recorder.service());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/risk-governance/coverage-overview");
        request.setQueryString("productId=1001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        BadSqlGrammarException handledException = new BadSqlGrammarException(
                "coverageOverview",
                "SELECT id,release_batch_id FROM risk_metric_catalog",
                new SQLSyntaxErrorException("Unknown column 'release_batch_id' in 'field list'")
        );

        FilterChain chain = (req, res) -> {
            req.setAttribute("com.ghlzm.iot.framework.observability.handledException", handledException);
            res.setContentType("application/json");
            res.getWriter().write("{\"code\":500,\"msg\":\"系统繁忙，请稍后再试\"}");
        };

        filter.doFilter(request, response, chain);

        AuditLog log = recorder.lastLog().get();

        assertEquals("system_error", log.getOperationType());
        assertEquals(0, log.getOperationResult());
        assertEquals(BadSqlGrammarException.class.getName(), log.getExceptionClass());
        assertTrue(log.getResultMessage().contains("Unknown column 'release_batch_id'"));
        assertTrue(log.getResponseResult().contains("BadSqlGrammarException"));
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
        EvidenceRecorder evidenceRecorder = new EvidenceRecorder();
        AuditLogFilter filter = new AuditLogFilter(recorder.service());
        filter.setObservabilityEvidenceRecorder(evidenceRecorder);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/audit-log/page");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> res.getWriter().write("{\"code\":200}");

        filter.doFilter(request, response, chain);

        assertEquals(0, recorder.count().get());
        assertEquals(1, evidenceRecorder.spanCount.get());
        assertEquals("HTTP_REQUEST", evidenceRecorder.lastSpan.get().getSpanType());
    }

    @Test
    void shouldRecordLoginAuditAndBusinessEvent() throws ServletException, IOException {
        AuditLogRecorder recorder = newRecorder();
        EvidenceRecorder evidenceRecorder = new EvidenceRecorder();
        AuditLogFilter filter = new AuditLogFilter(recorder.service());
        filter.setObservabilityEvidenceRecorder(evidenceRecorder);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setContentType("application/json");
        request.setContent("{\"username\":\"admin\",\"password\":\"123456\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            req.getInputStream().transferTo(OutputStream.nullOutputStream());
            res.setContentType("application/json");
            res.getWriter().write("{\"code\":200,\"msg\":\"ok\"}");
        };

        filter.doFilter(request, response, chain);

        assertEquals(1, recorder.count().get());
        assertEquals("auth", recorder.lastLog().get().getOperationModule());
        assertTrue(recorder.lastLog().get().getRequestParams().contains("\"password\":\"***\""));
        assertEquals(1, evidenceRecorder.spanCount.get());
        assertEquals(1, evidenceRecorder.eventCount.get());
        assertEquals("auth.login", evidenceRecorder.lastEvent.get().getEventCode());
    }

    @Test
    void shouldResolveProductContractApplyBusinessEvent() throws ServletException, IOException {
        AuditLogRecorder recorder = newRecorder();
        EvidenceRecorder evidenceRecorder = new EvidenceRecorder();
        AuditLogFilter filter = new AuditLogFilter(recorder.service());
        filter.setObservabilityEvidenceRecorder(evidenceRecorder);

        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/device/product/1001/model-governance/apply"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> res.getWriter().write("{\"code\":200}");

        filter.doFilter(request, response, chain);

        BusinessEventLogRecord event = evidenceRecorder.lastEvent.get();
        assertEquals("product.contract.apply", event.getEventCode());
        assertEquals("契约字段提交发布", event.getEventName());
        assertEquals("product_contract", event.getDomainCode());
        assertEquals("apply", event.getActionCode());
        assertEquals("product", event.getObjectType());
        assertEquals("1001", event.getObjectId());
        assertEquals("product.contract.apply", evidenceRecorder.lastSpan.get().getEventCode());
        assertEquals(true, event.getMetadata().get("dictionaryMatched"));
    }

    @Test
    void shouldResolveVendorMappingPublishBusinessEvent() throws ServletException, IOException {
        AuditLogRecorder recorder = newRecorder();
        EvidenceRecorder evidenceRecorder = new EvidenceRecorder();
        AuditLogFilter filter = new AuditLogFilter(recorder.service());
        filter.setObservabilityEvidenceRecorder(evidenceRecorder);

        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/device/product/1001/vendor-mapping-rules/2002/submit-publish"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> res.getWriter().write("{\"code\":200}");

        filter.doFilter(request, response, chain);

        BusinessEventLogRecord event = evidenceRecorder.lastEvent.get();
        assertEquals("product.mapping_rule.publish_submit", event.getEventCode());
        assertEquals("product_mapping_rule", event.getDomainCode());
        assertEquals("publish_submit", event.getActionCode());
        assertEquals("vendor_mapping_rule", event.getObjectType());
        assertEquals("2002", event.getObjectId());
        assertEquals("1001", event.getMetadata().get("pathGroup1"));
    }

    @Test
    void shouldResolveProtocolTemplatePublishBusinessEvent() throws ServletException, IOException {
        AuditLogRecorder recorder = newRecorder();
        EvidenceRecorder evidenceRecorder = new EvidenceRecorder();
        AuditLogFilter filter = new AuditLogFilter(recorder.service());
        filter.setObservabilityEvidenceRecorder(evidenceRecorder);

        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/governance/protocol/templates/3003/publish"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> res.getWriter().write("{\"code\":200}");

        filter.doFilter(request, response, chain);

        BusinessEventLogRecord event = evidenceRecorder.lastEvent.get();
        assertEquals("protocol.template.publish", event.getEventCode());
        assertEquals("protocol_governance", event.getDomainCode());
        assertEquals("publish_template", event.getActionCode());
        assertEquals("protocol_template", event.getObjectType());
        assertEquals("3003", event.getObjectId());
    }

    @Test
    void shouldRecordAcceptanceResultViewBusinessEventForDictionaryGet() throws ServletException, IOException {
        AuditLogRecorder recorder = newRecorder();
        EvidenceRecorder evidenceRecorder = new EvidenceRecorder();
        AuditLogFilter filter = new AuditLogFilter(recorder.service());
        filter.setObservabilityEvidenceRecorder(evidenceRecorder);

        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/report/business-acceptance/results/run-001"
        );
        request.setQueryString("packageCode=core");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> res.getWriter().write("{\"code\":200}");

        filter.doFilter(request, response, chain);

        BusinessEventLogRecord event = evidenceRecorder.lastEvent.get();
        assertEquals(1, evidenceRecorder.eventCount.get());
        assertEquals("acceptance.business_result.view", event.getEventCode());
        assertEquals("acceptance", event.getDomainCode());
        assertEquals("view_business_result", event.getActionCode());
        assertEquals("business_acceptance_run", event.getObjectType());
        assertEquals("run-001", event.getObjectId());
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
