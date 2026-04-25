package com.ghlzm.iot.system.filter;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.config.DiagnosticLoggingConstants;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.HttpHandledExceptionContext;
import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import com.ghlzm.iot.framework.observability.SensitiveLogSanitizer;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.framework.observability.evidence.BusinessEventLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceStatus;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanTypes;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.observability.BusinessEventDefinition;
import com.ghlzm.iot.system.observability.HttpBusinessEventDictionaryResolver;
import com.ghlzm.iot.system.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * 系统接口审计日志过滤器。
 */
@Slf4j
@Component
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Logger diagnosticAccessLog =
            LoggerFactory.getLogger(DiagnosticLoggingConstants.DIAGNOSTIC_ACCESS_LOGGER_NAME);
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final int MAX_CAPTURE_LENGTH = 4000;
    private static final int MAX_RESULT_MESSAGE_LENGTH = 500;
    private static final int MAX_REQUEST_URL_LENGTH = 255;
    private static final int MAX_OPERATION_METHOD_LENGTH = 255;
    private static final int MAX_ERROR_CODE_LENGTH = 64;
    private static final int MAX_EXCEPTION_CLASS_LENGTH = 255;
    private static final String SYSTEM_ERROR_TYPE = "system_error";

    private final AuditLogService auditLogService;
    private final IotProperties iotProperties;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final HttpBusinessEventDictionaryResolver businessEventDictionaryResolver =
            new HttpBusinessEventDictionaryResolver();
    private ObservabilityEvidenceRecorder evidenceRecorder = ObservabilityEvidenceRecorder.noop();

    @Autowired
    public AuditLogFilter(AuditLogService auditLogService, IotProperties iotProperties) {
        this.auditLogService = auditLogService;
        this.iotProperties = iotProperties;
    }

    AuditLogFilter(AuditLogService auditLogService) {
        this(auditLogService, new IotProperties());
    }

    @Autowired(required = false)
    public void setObservabilityEvidenceRecorder(ObservabilityEvidenceRecorder evidenceRecorder) {
        if (evidenceRecorder != null) {
            this.evidenceRecorder = evidenceRecorder;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNs = System.nanoTime();
        ContentCachingRequestWrapper requestWrapper = request instanceof ContentCachingRequestWrapper
                ? (ContentCachingRequestWrapper) request
                : new ContentCachingRequestWrapper(request, MAX_CAPTURE_LENGTH);
        ContentCachingResponseWrapper responseWrapper = response instanceof ContentCachingResponseWrapper
                ? (ContentCachingResponseWrapper) response
                : new ContentCachingResponseWrapper(response);
        Exception chainException = null;
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } catch (Exception ex) {
            chainException = ex;
            throw ex;
        } finally {
            long costMs = (System.nanoTime() - startNs) / 1_000_000L;
            recordAuditLog(requestWrapper, responseWrapper, chainException, costMs);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void recordAuditLog(HttpServletRequest request,
                                HttpServletResponse response,
                                Exception chainException,
                                long costMs) {
        boolean recordEvidence = shouldRecordEvidence(request.getRequestURI());
        boolean recordAudit = shouldRecordAudit(request.getRequestURI());
        if (!recordEvidence && !recordAudit) {
            return;
        }

        AuditLog auditLog = new AuditLog();
        ResponseCapture responseCapture = captureResponse(response);
        Throwable auditThrowable = resolveAuditThrowable(request, chainException);
        Date now = new Date();
        auditLog.setTenantId(DEFAULT_TENANT_ID);
        fillUserInfo(auditLog);
        auditLog.setTraceId(TraceContextHolder.currentOrCreate());
        auditLog.setOperationType(resolveOperationType(request.getMethod(), auditThrowable));
        auditLog.setOperationModule(resolveOperationModule(request.getRequestURI()));
        auditLog.setOperationMethod(truncate(resolveOperationMethod(request), MAX_OPERATION_METHOD_LENGTH));
        auditLog.setRequestUrl(truncate(request.getRequestURI(), MAX_REQUEST_URL_LENGTH));
        auditLog.setRequestMethod(request.getMethod());
        auditLog.setRequestParams(resolveRequestParams(request));
        auditLog.setResponseResult(resolveResponseResult(responseCapture, auditThrowable));
        auditLog.setIpAddress(resolveIp(request));
        auditLog.setLocation("");
        auditLog.setOperationResult(resolveOperationResult(response, auditThrowable, responseCapture));
        auditLog.setResultMessage(resolveResultMessage(response, auditThrowable, responseCapture));
        auditLog.setErrorCode(resolveErrorCode(request, auditThrowable));
        auditLog.setExceptionClass(resolveExceptionClass(auditThrowable));
        auditLog.setOperationTime(now);
        auditLog.setCreateTime(now);
        auditLog.setDeleted(0);
        String businessAction = resolveBusinessAction(request.getMethod(), auditThrowable);
        BusinessEventDefinition businessEventDefinition = businessEventDictionaryResolver.resolve(
                request.getMethod(),
                request.getRequestURI(),
                resolveRequestPattern(request),
                auditLog.getOperationModule(),
                businessAction
        );

        if (recordAudit) {
            try {
                auditLogService.addLog(auditLog);
            } catch (Exception saveEx) {
                // 审计失败不影响业务请求
                log.warn("写入审计日志失败, uri={}, error={}", request.getRequestURI(), saveEx.getMessage());
            }
        }

        recordHttpSpan(request, response, responseCapture, auditThrowable, auditLog, costMs, businessEventDefinition);
        if (shouldRecordBusinessEvent(request, auditThrowable, businessEventDefinition)) {
            recordHttpBusinessEvent(request, responseCapture, auditThrowable, auditLog, costMs, recordAudit,
                    businessEventDefinition);
        }
        maybeLogSlowRequest(auditLog, response, responseCapture, chainException, costMs);
    }

    private boolean shouldRecordEvidence(String uri) {
        return StringUtils.hasText(uri) && uri.startsWith("/api/");
    }

    private boolean shouldRecordAudit(String uri) {
        if (!StringUtils.hasText(uri) || !uri.startsWith("/api/")) {
            return false;
        }
        if (uri.startsWith("/api/system/audit-log")) {
            return false;
        }
        return true;
    }

    private boolean shouldRecordBusinessEvent(HttpServletRequest request,
                                              Throwable auditThrowable,
                                              BusinessEventDefinition businessEventDefinition) {
        String uri = request.getRequestURI();
        if (!StringUtils.hasText(uri) || uri.startsWith("/api/system/audit-log")) {
            return false;
        }
        if (auditThrowable != null) {
            return true;
        }
        if (businessEventDefinition.dictionaryMatched()
                && "GET".equalsIgnoreCase(request.getMethod())
                && "acceptance".equals(businessEventDefinition.domainCode())) {
            return true;
        }
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private void fillUserInfo(AuditLog auditLog) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtUserPrincipal jwtUserPrincipal) {
            auditLog.setUserId(jwtUserPrincipal.userId());
            auditLog.setUserName(jwtUserPrincipal.username());
        }
    }

    private String resolveOperationType(String method, Throwable auditThrowable) {
        if (auditThrowable != null) {
            return SYSTEM_ERROR_TYPE;
        }
        if (!StringUtils.hasText(method)) {
            return "unknown";
        }
        return switch (method.toUpperCase()) {
            case "GET" -> "select";
            case "POST" -> "insert";
            case "PUT", "PATCH" -> "update";
            case "DELETE" -> "delete";
            default -> method.toLowerCase();
        };
    }

    private String resolveOperationModule(String uri) {
        String[] parts = uri.split("/");
        return parts.length > 2 ? parts[2] : "unknown";
    }

    private String resolveRequestParams(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String queryPart = StringUtils.hasText(queryString) ? "query: " + sanitizeAndTruncate(queryString) : "";
        String bodyPart = resolveRequestBody(request);
        if (StringUtils.hasText(queryPart) && StringUtils.hasText(bodyPart)) {
            return queryPart + "\nbody: " + bodyPart;
        }
        return StringUtils.hasText(bodyPart) ? "body: " + bodyPart : queryPart;
    }

    private ResponseCapture captureResponse(HttpServletResponse response) {
        if (!(response instanceof ContentCachingResponseWrapper wrapper)) {
            return new ResponseCapture("HTTP " + response.getStatus(), null, null);
        }
        byte[] content = wrapper.getContentAsByteArray();
        if (content.length == 0) {
            return new ResponseCapture("HTTP " + response.getStatus(), null, null);
        }

        Charset charset = resolveCharset(wrapper.getCharacterEncoding(), wrapper.getContentType());
        String rawResponseBody = new String(content, charset);
        String responseBody = sanitizeAndTruncate(rawResponseBody);
        BusinessResponse businessResponse = parseBusinessResponse(rawResponseBody);
        if (!StringUtils.hasText(responseBody)) {
            return new ResponseCapture("HTTP " + response.getStatus(), businessResponse.code(), businessResponse.message());
        }
        return new ResponseCapture(
                "HTTP " + response.getStatus() + " body: " + responseBody,
                businessResponse.code(),
                businessResponse.message()
        );
    }

    private String resolveRequestBody(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return "";
        }
        byte[] content = wrapper.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        Charset charset = resolveCharset(wrapper.getCharacterEncoding(), wrapper.getContentType());
        return sanitizeAndTruncate(new String(content, charset));
    }

    private Integer resolveOperationResult(HttpServletResponse response,
                                           Throwable auditThrowable,
                                           ResponseCapture responseCapture) {
        if (auditThrowable != null || response.getStatus() >= 400) {
            return 0;
        }
        if (responseCapture.businessCode() != null && responseCapture.businessCode() != R.SUCCESS) {
            return 0;
        }
        return 1;
    }

    private String resolveResultMessage(HttpServletResponse response,
                                        Throwable auditThrowable,
                                        ResponseCapture responseCapture) {
        if (auditThrowable != null) {
            return truncate(resolveThrowableSummary(auditThrowable), MAX_RESULT_MESSAGE_LENGTH);
        }
        if (responseCapture.businessCode() != null && responseCapture.businessCode() != R.SUCCESS) {
            if (StringUtils.hasText(responseCapture.businessMessage())) {
                return truncate(SensitiveLogSanitizer.sanitize(responseCapture.businessMessage()), MAX_RESULT_MESSAGE_LENGTH);
            }
            return "业务失败: code=" + responseCapture.businessCode();
        }
        return response.getStatus() < 400 ? "OK" : "HTTP " + response.getStatus();
    }

    private Throwable resolveAuditThrowable(HttpServletRequest request, Exception chainException) {
        if (chainException != null) {
            return chainException;
        }
        return HttpHandledExceptionContext.resolveThrowable(request);
    }

    private String resolveResponseResult(ResponseCapture responseCapture, Throwable auditThrowable) {
        if (auditThrowable != null) {
            return buildThrowableDetail(auditThrowable);
        }
        return responseCapture.responseResult();
    }

    private String resolveThrowableSummary(Throwable throwable) {
        String message = throwable.getMessage();
        String rootCauseMessage = resolveRootCauseMessage(throwable);
        if (!StringUtils.hasText(message) && !StringUtils.hasText(rootCauseMessage)) {
            return throwable.getClass().getSimpleName();
        }
        String detail;
        if (!StringUtils.hasText(message)) {
            detail = rootCauseMessage;
        } else if (StringUtils.hasText(rootCauseMessage) && !message.contains(rootCauseMessage)) {
            detail = message + "; rootCause=" + rootCauseMessage;
        } else {
            detail = message;
        }
        return SensitiveLogSanitizer.sanitize(throwable.getClass().getSimpleName() + ": " + detail);
    }

    private String resolveRootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        String message = null;
        while (current != null) {
            if (StringUtils.hasText(current.getMessage())) {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        return message;
    }

    private String resolveErrorCode(HttpServletRequest request, Throwable auditThrowable) {
        String errorCode = HttpHandledExceptionContext.resolveErrorCode(request);
        if (StringUtils.hasText(errorCode)) {
            return truncate(errorCode, MAX_ERROR_CODE_LENGTH);
        }
        if (auditThrowable instanceof BizException bizException && bizException.getCode() != null) {
            return truncate(String.valueOf(bizException.getCode()), MAX_ERROR_CODE_LENGTH);
        }
        return null;
    }

    private String resolveExceptionClass(Throwable auditThrowable) {
        if (auditThrowable == null) {
            return null;
        }
        return truncate(auditThrowable.getClass().getName(), MAX_EXCEPTION_CLASS_LENGTH);
    }

    private String buildThrowableDetail(Throwable throwable) {
        StringWriter writer = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            throwable.printStackTrace(printWriter);
        }
        return truncate(writer.toString(), MAX_CAPTURE_LENGTH);
    }

    private String resolveOperationMethod(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (handler instanceof HandlerMethod handlerMethod) {
            return handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
        }
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null) {
            return request.getMethod() + ":" + pattern;
        }
        return request.getMethod() + ":" + request.getRequestURI();
    }

    private String resolveRequestPattern(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? request.getRequestURI() : String.valueOf(pattern);
    }

    private Charset resolveCharset(String characterEncoding, String contentType) {
        return StandardCharsets.UTF_8;
    }

    private BusinessResponse parseBusinessResponse(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return new BusinessResponse(null, null);
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (!root.isObject()) {
                return new BusinessResponse(null, null);
            }
            Integer code = root.has("code") && root.get("code").canConvertToInt() ? root.get("code").asInt() : null;
            String message = root.has("msg") && !root.get("msg").isNull() ? root.get("msg").asText() : null;
            return new BusinessResponse(code, message);
        } catch (Exception ex) {
            return new BusinessResponse(null, null);
        }
    }

    private String sanitizeAndTruncate(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String sanitized = SensitiveLogSanitizer.sanitize(text);
        return truncate(sanitized, MAX_CAPTURE_LENGTH);
    }

    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...(truncated)";
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            int commaIndex = xff.indexOf(',');
            return commaIndex > 0 ? xff.substring(0, commaIndex).trim() : xff.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void maybeLogSlowRequest(AuditLog auditLog,
                                     HttpServletResponse response,
                                     ResponseCapture responseCapture,
                                     Exception chainException,
                                     long costMs) {
        long thresholdMs = resolveSlowHttpThresholdMs();
        if (thresholdMs <= 0 || costMs < thresholdMs || !diagnosticAccessLog.isInfoEnabled()) {
            return;
        }
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", auditLog.getTraceId());
        details.put("method", auditLog.getRequestMethod());
        details.put("uri", auditLog.getRequestUrl());
        details.put("status", response.getStatus());
        details.put("operationModule", auditLog.getOperationModule());
        details.put("operationMethod", auditLog.getOperationMethod());
        details.put("userId", auditLog.getUserId());
        if (responseCapture.businessCode() != null) {
            details.put("businessCode", responseCapture.businessCode());
        }
        if (chainException != null) {
            details.put("errorClass", chainException.getClass().getSimpleName());
        }
        diagnosticAccessLog.info(ObservabilityEventLogSupport.summary(
                "slow_http_request",
                Integer.valueOf(1).equals(auditLog.getOperationResult()) ? "success" : "failure",
                costMs,
                details
        ));
    }

    private void recordHttpSpan(HttpServletRequest request,
                                HttpServletResponse response,
                                ResponseCapture responseCapture,
                                Throwable auditThrowable,
                                AuditLog auditLog,
                                long costMs,
                                BusinessEventDefinition businessEventDefinition) {
        LocalDateTime finishedAt = LocalDateTime.now();
        ObservabilitySpanLogRecord span = new ObservabilitySpanLogRecord();
        span.setTenantId(DEFAULT_TENANT_ID);
        span.setTraceId(auditLog.getTraceId());
        span.setSpanType(ObservabilitySpanTypes.HTTP_REQUEST);
        span.setSpanName(request.getMethod() + " " + resolveRequestPattern(request));
        span.setDomainCode(businessEventDefinition.domainCode());
        span.setEventCode(businessEventDefinition.dictionaryMatched()
                ? businessEventDefinition.eventCode()
                : "platform.http.request");
        span.setObjectType(businessEventDefinition.objectType());
        span.setObjectId(businessEventDefinition.objectId());
        span.setTransportType("HTTP");
        span.setStatus(Integer.valueOf(1).equals(auditLog.getOperationResult())
                ? ObservabilityEvidenceStatus.SUCCESS
                : ObservabilityEvidenceStatus.FAILURE);
        span.setDurationMs(costMs);
        span.setStartedAt(finishedAt.minusNanos(costMs * 1_000_000L));
        span.setFinishedAt(finishedAt);
        span.setErrorClass(auditLog.getExceptionClass());
        span.setErrorMessage(auditLog.getResultMessage());
        span.getTags().putAll(buildHttpTags(request, response, responseCapture, auditThrowable, auditLog,
                businessEventDefinition));
        evidenceRecorder.recordSpan(span);
    }

    private void recordHttpBusinessEvent(HttpServletRequest request,
                                         ResponseCapture responseCapture,
                                         Throwable auditThrowable,
                                         AuditLog auditLog,
                                         long costMs,
                                         boolean auditPersisted,
                                         BusinessEventDefinition businessEventDefinition) {
        BusinessEventLogRecord event = new BusinessEventLogRecord();
        event.setTenantId(DEFAULT_TENANT_ID);
        event.setTraceId(auditLog.getTraceId());
        event.setDomainCode(businessEventDefinition.domainCode());
        event.setActionCode(businessEventDefinition.actionCode());
        event.setEventCode(businessEventDefinition.eventCode());
        event.setEventName(businessEventDefinition.eventName());
        event.setObjectType(businessEventDefinition.objectType());
        event.setObjectId(businessEventDefinition.objectId());
        event.setActorUserId(auditLog.getUserId());
        event.setActorName(auditLog.getUserName());
        event.setResultStatus(Integer.valueOf(1).equals(auditLog.getOperationResult())
                ? ObservabilityEvidenceStatus.SUCCESS
                : ObservabilityEvidenceStatus.FAILURE);
        event.setSourceType("HTTP");
        event.setEvidenceType(auditPersisted ? "sys_audit_log" : "http_request");
        event.setEvidenceId(auditPersisted && auditLog.getId() != null ? String.valueOf(auditLog.getId()) : null);
        event.setRequestMethod(auditLog.getRequestMethod());
        event.setRequestUri(auditLog.getRequestUrl());
        event.setDurationMs(costMs);
        event.setErrorCode(auditLog.getErrorCode());
        event.setErrorMessage(auditThrowable == null ? null : auditLog.getResultMessage());
        event.setOccurredAt(LocalDateTime.now());
        event.getMetadata().putAll(buildHttpTags(request, null, responseCapture, auditThrowable, auditLog,
                businessEventDefinition));
        evidenceRecorder.recordBusinessEvent(event);
    }

    private Map<String, Object> buildHttpTags(HttpServletRequest request,
                                              HttpServletResponse response,
                                              ResponseCapture responseCapture,
                                              Throwable auditThrowable,
                                              AuditLog auditLog,
                                              BusinessEventDefinition businessEventDefinition) {
        Map<String, Object> tags = new LinkedHashMap<>();
        tags.put("requestPattern", resolveRequestPattern(request));
        tags.put("requestUri", auditLog.getRequestUrl());
        tags.put("requestMethod", auditLog.getRequestMethod());
        tags.put("operationType", auditLog.getOperationType());
        tags.put("operationMethod", auditLog.getOperationMethod());
        tags.put("operationModule", auditLog.getOperationModule());
        tags.put("userId", auditLog.getUserId());
        tags.put("ipAddress", auditLog.getIpAddress());
        if (response != null) {
            tags.put("httpStatus", response.getStatus());
        }
        if (responseCapture.businessCode() != null) {
            tags.put("businessCode", responseCapture.businessCode());
        }
        if (auditThrowable != null) {
            tags.put("errorClass", auditThrowable.getClass().getName());
        }
        tags.put("businessEventCode", businessEventDefinition.eventCode());
        tags.put("businessEventName", businessEventDefinition.eventName());
        tags.put("businessDomainCode", businessEventDefinition.domainCode());
        tags.put("businessActionCode", businessEventDefinition.actionCode());
        tags.put("businessObjectType", businessEventDefinition.objectType());
        tags.put("businessObjectId", businessEventDefinition.objectId());
        tags.putAll(businessEventDefinition.metadata());
        return tags;
    }

    private String resolveBusinessAction(String method, Throwable auditThrowable) {
        if (auditThrowable != null) {
            return "failure";
        }
        if (!StringUtils.hasText(method)) {
            return "unknown";
        }
        return switch (method.toUpperCase(Locale.ROOT)) {
            case "GET" -> "query";
            case "POST" -> "create_or_execute";
            case "PUT", "PATCH" -> "update";
            case "DELETE" -> "delete";
            default -> method.toLowerCase(Locale.ROOT);
        };
    }

    private long resolveSlowHttpThresholdMs() {
        IotProperties.Observability observability = iotProperties.getObservability();
        if (observability == null || observability.getPerformance() == null) {
            return 0L;
        }
        Long thresholdMs = observability.getPerformance().getSlowHttpThresholdMs();
        return thresholdMs == null ? 0L : thresholdMs;
    }

    private record ResponseCapture(String responseResult, Integer businessCode, String businessMessage) {
    }

    private record BusinessResponse(Integer code, String message) {
    }
}
