package com.ghlzm.iot.system.filter;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 系统接口审计日志过滤器。
 */
@Slf4j
@Component
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final int MAX_CAPTURE_LENGTH = 4000;
    private static final int MAX_RESULT_MESSAGE_LENGTH = 500;
    private static final int MAX_REQUEST_URL_LENGTH = 255;
    private static final int MAX_OPERATION_METHOD_LENGTH = 255;
    private static final Pattern JSON_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)\"(password|token|secret|authorization|accessToken|refreshToken|clientSecret)\"\\s*:\\s*\"[^\"]*\"");
    private static final Pattern ESCAPED_JSON_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)\\\\\"(password|token|secret|authorization|accessToken|refreshToken|clientSecret)\\\\\"\\s*:\\s*\\\\\"[^\\\\\"]*\\\\\"");
    private static final Pattern ESCAPED_JSON_SENSITIVE_GROUP_PATTERN = Pattern.compile(
            "(?i)(\\\\\"(?:password|token|secret|authorization|accessToken|refreshToken|clientSecret)\\\\\"\\s*:\\s*\\\\\")[^\\\\\"]*(\\\\\")");
    private static final Pattern KV_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(password|token|secret|authorization|accessToken|refreshToken|clientSecret)=([^&\\s]+)");
    private static final Pattern AUTHORIZATION_HEADER_PATTERN = Pattern.compile(
            "(?i)(authorization\\s*:\\s*bearer\\s+)([^\\s,;]+)");

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public AuditLogFilter(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
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
            recordAuditLog(requestWrapper, responseWrapper, chainException);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void recordAuditLog(HttpServletRequest request, HttpServletResponse response, Exception chainException) {
        if (!shouldRecord(request.getRequestURI())) {
            return;
        }

        AuditLog auditLog = new AuditLog();
        ResponseCapture responseCapture = captureResponse(response);
        Date now = new Date();
        auditLog.setTenantId(DEFAULT_TENANT_ID);
        fillUserInfo(auditLog);
        auditLog.setTraceId(TraceContextHolder.currentOrCreate());
        auditLog.setOperationType(resolveOperationType(request.getMethod()));
        auditLog.setOperationModule(resolveOperationModule(request.getRequestURI()));
        auditLog.setOperationMethod(truncate(resolveOperationMethod(request), MAX_OPERATION_METHOD_LENGTH));
        auditLog.setRequestUrl(truncate(request.getRequestURI(), MAX_REQUEST_URL_LENGTH));
        auditLog.setRequestMethod(request.getMethod());
        auditLog.setRequestParams(resolveRequestParams(request));
        auditLog.setResponseResult(responseCapture.responseResult());
        auditLog.setIpAddress(resolveIp(request));
        auditLog.setLocation("");
        auditLog.setOperationResult(resolveOperationResult(response, chainException, responseCapture));
        auditLog.setResultMessage(resolveResultMessage(response, chainException, responseCapture));
        auditLog.setOperationTime(now);
        auditLog.setCreateTime(now);
        auditLog.setDeleted(0);

        try {
            auditLogService.addLog(auditLog);
        } catch (Exception saveEx) {
            // 审计失败不影响业务请求
            log.warn("写入审计日志失败, uri={}, error={}", request.getRequestURI(), saveEx.getMessage());
        }
    }

    private boolean shouldRecord(String uri) {
        if (!StringUtils.hasText(uri) || !uri.startsWith("/api/")) {
            return false;
        }
        if (uri.startsWith("/api/system/audit-log/")) {
            return false;
        }
        return !"/api/auth/login".equals(uri);
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

    private String resolveOperationType(String method) {
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
                                           Exception chainException,
                                           ResponseCapture responseCapture) {
        if (chainException != null || response.getStatus() >= 400) {
            return 0;
        }
        if (responseCapture.businessCode() != null && responseCapture.businessCode() != R.SUCCESS) {
            return 0;
        }
        return 1;
    }

    private String resolveResultMessage(HttpServletResponse response,
                                        Exception chainException,
                                        ResponseCapture responseCapture) {
        if (chainException != null) {
            if (StringUtils.hasText(chainException.getMessage())) {
                return truncate(chainException.getMessage(), MAX_RESULT_MESSAGE_LENGTH);
            }
            return chainException.getClass().getSimpleName();
        }
        if (responseCapture.businessCode() != null && responseCapture.businessCode() != R.SUCCESS) {
            if (StringUtils.hasText(responseCapture.businessMessage())) {
                return truncate(maskSensitive(responseCapture.businessMessage()), MAX_RESULT_MESSAGE_LENGTH);
            }
            return "业务失败: code=" + responseCapture.businessCode();
        }
        return response.getStatus() < 400 ? "OK" : "HTTP " + response.getStatus();
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
        String sanitized = maskSensitive(text);
        return truncate(sanitized, MAX_CAPTURE_LENGTH);
    }

    private String maskSensitive(String text) {
        String masked = replaceWithPattern(text, JSON_SENSITIVE_PATTERN, "\"$1\":\"***\"");
        masked = replaceWithPattern(masked, ESCAPED_JSON_SENSITIVE_PATTERN, "\\\\\"$1\\\\\":\\\\\"***\\\\\"");
        masked = replaceWithPattern(masked, ESCAPED_JSON_SENSITIVE_GROUP_PATTERN, "$1***$2");
        masked = replaceWithPattern(masked, KV_SENSITIVE_PATTERN, "$1=***");
        masked = replaceWithPattern(masked, AUTHORIZATION_HEADER_PATTERN, "$1***");
        return masked;
    }

    private String replaceWithPattern(String text, Pattern pattern, String replacement) {
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll(replacement);
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

    private record ResponseCapture(String responseResult, Integer businessCode, String businessMessage) {
    }

    private record BusinessResponse(Integer code, String message) {
    }
}
