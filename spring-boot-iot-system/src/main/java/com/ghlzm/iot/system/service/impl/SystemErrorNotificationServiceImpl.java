package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import com.ghlzm.iot.framework.observability.BackendExceptionEvent;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.service.NotificationChannelDispatcher;
import com.ghlzm.iot.system.service.SystemErrorNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统异常自动通知服务实现。
 */
@Slf4j
@Service
public class SystemErrorNotificationServiceImpl implements SystemErrorNotificationService {

    private static final String SYSTEM_ERROR_SCENE = "system_error";
    private static final String APP_NAME = "spring-boot-iot";
    private static final String DEFAULT_TITLE = "spring-boot-iot 后台异常";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_RESPONSE_LOG_LENGTH = 500;

    private final NotificationChannelDispatcher notificationChannelDispatcher;
    private final IotProperties iotProperties;
    private final Map<String, Long> throttleTracker = new ConcurrentHashMap<>();

    public SystemErrorNotificationServiceImpl(NotificationChannelDispatcher notificationChannelDispatcher,
                                              IotProperties iotProperties) {
        this.notificationChannelDispatcher = notificationChannelDispatcher;
        this.iotProperties = iotProperties;
    }

    @Override
    public void notifySystemError(BackendExceptionEvent event, AuditLog auditLog) {
        if (!Boolean.TRUE.equals(iotProperties.getObservability().getSystemErrorNotifyEnabled())) {
            return;
        }

        for (NotificationChannelDispatcher.DispatchChannel channel : notificationChannelDispatcher.listSceneChannels(SYSTEM_ERROR_SCENE)) {
            if (shouldThrottle(buildThrottleKey(channel.channel().getChannelCode(), event), channel.config().minIntervalSeconds())) {
                log.debug(ObservabilityEventLogSupport.summary(
                        "notification_dispatch",
                        "throttled",
                        null,
                        buildDispatchDetails(SYSTEM_ERROR_SCENE, channel, event, auditLog, null, "cooldown")
                ));
                continue;
            }
            long startNs = System.nanoTime();
            NotificationChannelDispatcher.DispatchResult result =
                    notificationChannelDispatcher.send(channel, buildSystemErrorEnvelope(event, auditLog));
            if (!result.success()) {
                log.warn(ObservabilityEventLogSupport.summary(
                        "notification_dispatch",
                        "failure",
                        elapsedMillis(startNs),
                        buildDispatchDetails(
                                SYSTEM_ERROR_SCENE,
                                channel,
                                event,
                                auditLog,
                                result,
                                truncate(safeText(result.responseBody(), result.errorMessage()))
                        )
                ));
                continue;
            }
            log.info(ObservabilityEventLogSupport.summary(
                    "notification_dispatch",
                    "success",
                    elapsedMillis(startNs),
                    buildDispatchDetails(SYSTEM_ERROR_SCENE, channel, event, auditLog, result, null)
            ));
        }
    }

    @Override
    public void sendTestNotification(String channelCode) {
        sendTestNotification(null, channelCode);
    }

    @Override
    public void sendTestNotification(Long currentUserId, String channelCode) {
        NotificationChannelDispatcher.DispatchChannel channel = notificationChannelDispatcher.requireTestChannel(currentUserId, channelCode);
        long startNs = System.nanoTime();
        NotificationChannelDispatcher.DispatchResult result =
                notificationChannelDispatcher.send(channel, buildTestEnvelope(channel.channel().getChannelCode(), channel.channel().getChannelType()));
        if (!result.success()) {
            log.warn(ObservabilityEventLogSupport.summary(
                    "notification_dispatch",
                    "failure",
                    elapsedMillis(startNs),
                    buildDispatchDetails(
                            "notification_test",
                            channel,
                            null,
                            null,
                            result,
                            truncate(safeText(result.responseBody(), result.errorMessage()))
                    )
            ));
            return;
        }
        log.info(ObservabilityEventLogSupport.summary(
                "notification_dispatch",
                "success",
                elapsedMillis(startNs),
                buildDispatchDetails("notification_test", channel, null, null, result, null)
        ));
    }

    private NotificationChannelDispatcher.NotificationEnvelope buildSystemErrorEnvelope(BackendExceptionEvent event, AuditLog auditLog) {
        String summary = auditLog != null && StringUtils.hasText(auditLog.getResultMessage())
                ? auditLog.getResultMessage()
                : resolveThrowableSummary(event.throwable());
        LocalDateTime operationTime = auditLog != null && auditLog.getOperationTime() != null
                ? LocalDateTime.ofInstant(auditLog.getOperationTime().toInstant(), ZoneId.systemDefault())
                : LocalDateTime.now();

        Map<String, Object> genericPayload = new LinkedHashMap<>();
        genericPayload.put("application", APP_NAME);
        genericPayload.put("eventType", SYSTEM_ERROR_SCENE);
        genericPayload.put("title", DEFAULT_TITLE);
        genericPayload.put("summary", summary);
        genericPayload.put("auditLogId", auditLog == null ? null : auditLog.getId());
        genericPayload.put("traceId", auditLog == null ? null : auditLog.getTraceId());
        genericPayload.put("deviceCode", auditLog == null ? null : auditLog.getDeviceCode());
        genericPayload.put("productKey", auditLog == null ? null : auditLog.getProductKey());
        genericPayload.put("errorCode", auditLog == null ? null : auditLog.getErrorCode());
        genericPayload.put("exceptionClass", auditLog == null ? null : auditLog.getExceptionClass());
        genericPayload.put("operationModule", auditLog == null ? event.operationModule() : auditLog.getOperationModule());
        genericPayload.put("operationMethod", auditLog == null ? event.operationMethod() : auditLog.getOperationMethod());
        genericPayload.put("requestUrl", auditLog == null ? event.requestUrl() : auditLog.getRequestUrl());
        genericPayload.put("requestMethod", auditLog == null ? event.requestMethod() : auditLog.getRequestMethod());
        genericPayload.put("operationTime", operationTime.format(TIME_FORMATTER));
        genericPayload.put("context", event.context());

        String markdown = """
                ### spring-boot-iot 后台异常
                - 时间：%s
                - 模块：%s
                - 方法：%s
                - 目标：%s
                - 通道：%s
                - 摘要：%s
                - traceId：%s
                - 设备：%s
                - 产品：%s
                - 错误码：%s
                - 异常类：%s
                - 审计ID：%s
                """.formatted(
                operationTime.format(TIME_FORMATTER),
                safeText(genericPayload.get("operationModule")),
                safeText(genericPayload.get("operationMethod")),
                safeText(genericPayload.get("requestUrl")),
                safeText(genericPayload.get("requestMethod")),
                safeText(summary),
                safeText(genericPayload.get("traceId")),
                safeText(genericPayload.get("deviceCode")),
                safeText(genericPayload.get("productKey")),
                safeText(genericPayload.get("errorCode")),
                safeText(genericPayload.get("exceptionClass")),
                safeText(genericPayload.get("auditLogId"))
        );
        String plainText = """
                spring-boot-iot 后台异常
                时间: %s
                模块: %s
                方法: %s
                目标: %s
                通道: %s
                摘要: %s
                traceId: %s
                设备: %s
                产品: %s
                错误码: %s
                异常类: %s
                审计ID: %s
                """.formatted(
                operationTime.format(TIME_FORMATTER),
                safeText(genericPayload.get("operationModule")),
                safeText(genericPayload.get("operationMethod")),
                safeText(genericPayload.get("requestUrl")),
                safeText(genericPayload.get("requestMethod")),
                safeText(summary),
                safeText(genericPayload.get("traceId")),
                safeText(genericPayload.get("deviceCode")),
                safeText(genericPayload.get("productKey")),
                safeText(genericPayload.get("errorCode")),
                safeText(genericPayload.get("exceptionClass")),
                safeText(genericPayload.get("auditLogId"))
        );
        return new NotificationChannelDispatcher.NotificationEnvelope(DEFAULT_TITLE, plainText, markdown, genericPayload);
    }

    private NotificationChannelDispatcher.NotificationEnvelope buildTestEnvelope(String channelCode, String channelType) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> genericPayload = new LinkedHashMap<>();
        genericPayload.put("application", APP_NAME);
        genericPayload.put("eventType", "test");
        genericPayload.put("title", "spring-boot-iot 通知渠道测试");
        genericPayload.put("channelCode", channelCode);
        genericPayload.put("channelType", channelType);
        genericPayload.put("sentAt", now.format(TIME_FORMATTER));
        genericPayload.put("summary", "这是一条来自 spring-boot-iot 的测试通知，用于验证 webhook 渠道配置是否可用。");

        String markdown = """
                ### spring-boot-iot 通知渠道测试
                - 渠道编码：%s
                - 渠道类型：%s
                - 发送时间：%s
                - 说明：这是一条测试通知，用于验证 webhook 渠道配置是否可用。
                """.formatted(channelCode, channelType, now.format(TIME_FORMATTER));
        String plainText = """
                spring-boot-iot 通知渠道测试
                渠道编码: %s
                渠道类型: %s
                发送时间: %s
                说明: 这是一条测试通知，用于验证 webhook 渠道配置是否可用。
                """.formatted(channelCode, channelType, now.format(TIME_FORMATTER));
        return new NotificationChannelDispatcher.NotificationEnvelope("spring-boot-iot 通知渠道测试", plainText, markdown, genericPayload);
    }

    private boolean shouldThrottle(String throttleKey, int minIntervalSeconds) {
        if (!StringUtils.hasText(throttleKey) || minIntervalSeconds <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        long minIntervalMillis = minIntervalSeconds * 1000L;
        Long lastSent = throttleTracker.get(throttleKey);
        if (lastSent != null && now - lastSent < minIntervalMillis) {
            return true;
        }
        throttleTracker.put(throttleKey, now);
        cleanupThrottleTracker(now, minIntervalMillis);
        return false;
    }

    private void cleanupThrottleTracker(long now, long minIntervalMillis) {
        if (throttleTracker.size() < 512) {
            return;
        }
        throttleTracker.entrySet().removeIf(entry -> now - entry.getValue() > Math.max(minIntervalMillis, 3600_000L));
    }

    private String buildThrottleKey(String channelCode, BackendExceptionEvent event) {
        return channelCode + "|" + event.operationModule() + "|" + event.operationMethod()
                + "|" + event.requestUrl() + "|" + resolveThrowableSummary(event.throwable());
    }

    private String resolveThrowableSummary(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        if (!StringUtils.hasText(throwable.getMessage())) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
    }

    private String truncate(String text) {
        if (!StringUtils.hasText(text) || text.length() <= MAX_RESPONSE_LOG_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_RESPONSE_LOG_LENGTH) + "...(truncated)";
    }

    private String safeText(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String safeText(Object primary, Object fallback) {
        return primary == null ? safeText(fallback) : String.valueOf(primary);
    }

    private Map<String, Object> buildDispatchDetails(String scene,
                                                     NotificationChannelDispatcher.DispatchChannel channel,
                                                     BackendExceptionEvent event,
                                                     AuditLog auditLog,
                                                     NotificationChannelDispatcher.DispatchResult result,
                                                     String responseSummary) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("scene", scene);
        details.put("channelCode", channel == null ? null : channel.channel().getChannelCode());
        details.put("channelType", channel == null ? null : channel.channel().getChannelType());
        details.put("statusCode", result == null ? null : result.statusCode());
        details.put("traceId", auditLog != null ? auditLog.getTraceId() : event == null ? null : event.context().get("traceId"));
        details.put("requestUrl", auditLog != null ? auditLog.getRequestUrl() : event == null ? null : event.requestUrl());
        details.put("operationMethod", auditLog != null ? auditLog.getOperationMethod() : event == null ? null : event.operationMethod());
        details.put("auditLogId", auditLog == null ? null : auditLog.getId());
        details.put("response", responseSummary);
        return details;
    }

    private long elapsedMillis(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }
}
