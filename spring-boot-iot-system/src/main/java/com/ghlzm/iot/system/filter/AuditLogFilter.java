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
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.Date;

/**
 * 系统接口审计日志过滤器。
 */
@Slf4j
@Component
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Long DEFAULT_TENANT_ID = 1L;

    private final AuditLogService auditLogService;

    public AuditLogFilter(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Exception chainException = null;
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            chainException = ex;
            throw ex;
        } finally {
            recordAuditLog(request, response, chainException);
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
        auditLog.setResponseResult("HTTP " + response.getStatus());
        auditLog.setIpAddress(resolveIp(request));
        auditLog.setLocation("");
        auditLog.setOperationResult(chainException == null && response.getStatus() < 400 ? 1 : 0);
        auditLog.setResultMessage(chainException == null ? "OK" : chainException.getMessage());
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
        return StringUtils.hasText(queryString) ? queryString : "";
    }

    private String resolveOperationMethod(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (handler instanceof HandlerMethod handlerMethod) {
            return handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
        }
        return request.getMethod() + ":" + request.getRequestURI();
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
