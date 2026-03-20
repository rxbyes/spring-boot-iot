package com.ghlzm.iot.framework.observability;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.NamedThreadLocal;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 统一链路追踪上下文。
 */
public final class TraceContextHolder {

    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String TRACE_PARAM = "traceId";
    public static final String TRACE_REQUEST_ATTRIBUTE = TraceContextHolder.class.getName() + ".TRACE_ID";

    private static final String MDC_KEY = "traceId";
    private static final Pattern SAFE_TRACE_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{8,64}");
    private static final ThreadLocal<String> TRACE_ID_HOLDER = new NamedThreadLocal<>("iot-trace-id");

    private TraceContextHolder() {
    }

    public static String getTraceId() {
        return TRACE_ID_HOLDER.get();
    }

    public static String currentOrCreate() {
        String traceId = getTraceId();
        return StringUtils.hasText(traceId) ? traceId : bindTraceId(null);
    }

    public static String bindTraceId(String candidate) {
        String traceId = normalizeTraceId(candidate);
        if (!StringUtils.hasText(traceId)) {
            traceId = generateTraceId();
        }
        TRACE_ID_HOLDER.set(traceId);
        MDC.put(MDC_KEY, traceId);
        return traceId;
    }

    public static String bindHttpTrace(HttpServletRequest request, HttpServletResponse response) {
        String traceId = bindTraceId(resolveHttpCandidate(request));
        request.setAttribute(TRACE_REQUEST_ATTRIBUTE, traceId);
        if (response != null) {
            response.setHeader(TRACE_HEADER, traceId);
        }
        return traceId;
    }

    public static void clear() {
        TRACE_ID_HOLDER.remove();
        MDC.remove(MDC_KEY);
    }

    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String resolveHttpCandidate(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object attribute = request.getAttribute(TRACE_REQUEST_ATTRIBUTE);
        if (attribute instanceof String value && StringUtils.hasText(value)) {
            return value;
        }
        String headerTraceId = request.getHeader(TRACE_HEADER);
        if (StringUtils.hasText(headerTraceId)) {
            return headerTraceId;
        }
        String paramTraceId = request.getParameter(TRACE_PARAM);
        if (StringUtils.hasText(paramTraceId)) {
            return paramTraceId;
        }
        return null;
    }

    private static String normalizeTraceId(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        String traceId = candidate.trim();
        if (!SAFE_TRACE_PATTERN.matcher(traceId).matches()) {
            return null;
        }
        return traceId;
    }
}
