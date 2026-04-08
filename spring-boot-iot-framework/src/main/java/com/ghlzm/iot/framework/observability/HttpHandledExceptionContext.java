package com.ghlzm.iot.framework.observability;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 保存已由 HTTP 异常处理器消费、但仍需参与审计记录的异常上下文。
 */
public final class HttpHandledExceptionContext {

    public static final String HANDLED_EXCEPTION_ATTRIBUTE =
            "com.ghlzm.iot.framework.observability.handledException";
    public static final String HANDLED_ERROR_CODE_ATTRIBUTE =
            "com.ghlzm.iot.framework.observability.handledErrorCode";

    private HttpHandledExceptionContext() {
    }

    public static void attach(HttpServletRequest request, Throwable throwable, String errorCode) {
        if (request == null || throwable == null) {
            return;
        }
        request.setAttribute(HANDLED_EXCEPTION_ATTRIBUTE, throwable);
        if (StringUtils.hasText(errorCode)) {
            request.setAttribute(HANDLED_ERROR_CODE_ATTRIBUTE, errorCode);
        }
    }

    public static Throwable resolveThrowable(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object value = request.getAttribute(HANDLED_EXCEPTION_ATTRIBUTE);
        return value instanceof Throwable throwable ? throwable : null;
    }

    public static String resolveErrorCode(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object value = request.getAttribute(HANDLED_ERROR_CODE_ATTRIBUTE);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : null;
    }
}
