package com.ghlzm.iot.system.filter;

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
        Date now = new Date();
        auditLog.setTenantId(DEFAULT_TENANT_ID);
        fillUserInfo(auditLog);
        auditLog.setOperationType(resolveOperationType(request.getMethod()));
        auditLog.setOperationModule(resolveOperationModule(request.getRequestURI()));
        auditLog.setOperationMethod(resolveOperationMethod(request));
        auditLog.setRequestUrl(request.getRequestURI());
        auditLog.setRequestMethod(request.getMethod());
        auditLog.setRequestParams(resolveRequestParams(request));
        auditLog.setResponseResult(resolveResponseResult(response));
        auditLog.setIpAddress(resolveIp(request));
        auditLog.setLocation("");
        auditLog.setOperationResult(chainException == null && response.getStatus() < 400 ? 1 : 0);
        auditLog.setResultMessage(resolveResultMessage(response, chainException));
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

    private String resolveResponseResult(HttpServletResponse response) {
        if (!(response instanceof ContentCachingResponseWrapper wrapper)) {
            return "HTTP " + response.getStatus();
        }
        byte[] content = wrapper.getContentAsByteArray();
        if (content.length == 0) {
            return "HTTP " + response.getStatus();
        }
        Charset charset = resolveCharset(wrapper.getCharacterEncoding(), wrapper.getContentType());
        String responseBody = sanitizeAndTruncate(new String(content, charset));
        if (!StringUtils.hasText(responseBody)) {
            return "HTTP " + response.getStatus();
        }
        return "HTTP " + response.getStatus() + " body: " + responseBody;
    }

    private String resolveResultMessage(HttpServletResponse response, Exception chainException) {
        if (chainException != null && StringUtils.hasText(chainException.getMessage())) {
            return truncate(chainException.getMessage());
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

    private String sanitizeAndTruncate(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String sanitized = maskSensitive(text);
        return truncate(sanitized);
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

    private String truncate(String text) {
        if (!StringUtils.hasText(text) || text.length() <= MAX_CAPTURE_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_CAPTURE_LENGTH) + "...(truncated)";
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
}
