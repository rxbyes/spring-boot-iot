package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.observability.BackendExceptionEvent;
import com.ghlzm.iot.framework.observability.BackendExceptionRecorder;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.service.AuditLogService;
import com.ghlzm.iot.system.service.SystemErrorNotificationService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Map;

/**
 * 基于审计日志表的后台异常记录器。
 */
@Component
public class AuditLogBackendExceptionRecorder implements BackendExceptionRecorder {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final int MAX_TEXT_CAPTURE_LENGTH = 4000;
    private static final int MAX_OPERATION_MODULE_LENGTH = 128;
    private static final int MAX_OPERATION_METHOD_LENGTH = 255;
    private static final int MAX_REQUEST_URL_LENGTH = 255;
    private static final int MAX_REQUEST_METHOD_LENGTH = 16;
    private static final int MAX_RESULT_MESSAGE_LENGTH = 500;
    private static final int MAX_TRACE_ID_LENGTH = 64;
    private static final int MAX_DEVICE_CODE_LENGTH = 64;
    private static final int MAX_PRODUCT_KEY_LENGTH = 64;
    private static final int MAX_ERROR_CODE_LENGTH = 64;
    private static final int MAX_EXCEPTION_CLASS_LENGTH = 255;
    private static final String SYSTEM_USER_NAME = "SYSTEM";
    private static final String SYSTEM_ERROR_TYPE = "system_error";
    private static final String DEFAULT_REQUEST_METHOD = "SYSTEM";

    private final AuditLogService auditLogService;
    private final SystemErrorNotificationService systemErrorNotificationService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public AuditLogBackendExceptionRecorder(AuditLogService auditLogService,
                                            SystemErrorNotificationService systemErrorNotificationService) {
        this.auditLogService = auditLogService;
        this.systemErrorNotificationService = systemErrorNotificationService;
    }

    @Override
    public void record(BackendExceptionEvent event) {
        if (event == null || event.throwable() == null) {
            return;
        }

        AuditLog auditLog = new AuditLog();
        Date now = new Date();
        auditLog.setTenantId(DEFAULT_TENANT_ID);
        auditLog.setUserName(SYSTEM_USER_NAME);
        auditLog.setTraceId(truncate(resolveContextText(event.context(), "traceId", TraceContextHolder.getTraceId()), MAX_TRACE_ID_LENGTH));
        auditLog.setDeviceCode(truncate(resolveContextText(event.context(), "deviceCode", null), MAX_DEVICE_CODE_LENGTH));
        auditLog.setProductKey(truncate(resolveContextText(event.context(), "productKey", null), MAX_PRODUCT_KEY_LENGTH));
        auditLog.setOperationType(SYSTEM_ERROR_TYPE);
        auditLog.setOperationModule(truncate(defaultText(event.operationModule(), "unknown"), MAX_OPERATION_MODULE_LENGTH));
        auditLog.setOperationMethod(truncate(defaultText(event.operationMethod(), "unknown"), MAX_OPERATION_METHOD_LENGTH));
        auditLog.setRequestUrl(truncate(defaultText(event.requestUrl(), "unknown"), MAX_REQUEST_URL_LENGTH));
        auditLog.setRequestMethod(truncate(defaultText(event.requestMethod(), DEFAULT_REQUEST_METHOD), MAX_REQUEST_METHOD_LENGTH));
        auditLog.setRequestParams(serializeContext(event.context()));
        auditLog.setResponseResult(buildThrowableDetail(event.throwable()));
        auditLog.setIpAddress("");
        auditLog.setLocation("");
        auditLog.setOperationResult(0);
        auditLog.setResultMessage(truncate(resolveResultMessage(event.throwable()), MAX_RESULT_MESSAGE_LENGTH));
        auditLog.setErrorCode(truncate(resolveErrorCode(event), MAX_ERROR_CODE_LENGTH));
        auditLog.setExceptionClass(truncate(event.throwable().getClass().getName(), MAX_EXCEPTION_CLASS_LENGTH));
        auditLog.setOperationTime(now);
        auditLog.setCreateTime(now);
        auditLog.setDeleted(0);
        auditLogService.addLog(auditLog);
        try {
            systemErrorNotificationService.notifySystemError(event, auditLog);
        } catch (Exception ex) {
            // 通知失败不影响主链路。
        }
    }

    private String serializeContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "";
        }
        try {
            return truncate(objectMapper.writeValueAsString(context), MAX_TEXT_CAPTURE_LENGTH);
        } catch (Exception ex) {
            return truncate(String.valueOf(context), MAX_TEXT_CAPTURE_LENGTH);
        }
    }

    private String buildThrowableDetail(Throwable throwable) {
        StringWriter writer = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            throwable.printStackTrace(printWriter);
        }
        return truncate(writer.toString(), MAX_TEXT_CAPTURE_LENGTH);
    }

    private String resolveResultMessage(Throwable throwable) {
        if (!StringUtils.hasText(throwable.getMessage())) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
    }

    private String resolveErrorCode(BackendExceptionEvent event) {
        String errorCode = resolveContextText(event.context(), "errorCode", null);
        if (StringUtils.hasText(errorCode)) {
            return errorCode;
        }
        Throwable throwable = event.throwable();
        if (throwable instanceof BizException bizException && bizException.getCode() != null) {
            return String.valueOf(bizException.getCode());
        }
        return null;
    }

    private String resolveContextText(Map<String, Object> context, String key, String fallback) {
        if (context == null || context.isEmpty()) {
            return fallback;
        }
        Object value = context.get(key);
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : fallback;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...(truncated)";
    }
}
