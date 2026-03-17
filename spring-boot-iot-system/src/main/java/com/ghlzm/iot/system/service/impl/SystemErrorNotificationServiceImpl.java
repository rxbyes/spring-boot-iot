package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.BackendExceptionEvent;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.entity.NotificationChannel;
import com.ghlzm.iot.system.service.NotificationChannelService;
import com.ghlzm.iot.system.service.NotificationHttpClient;
import com.ghlzm.iot.system.service.SystemErrorNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private static final Set<String> WEBHOOK_CHANNEL_TYPES = Set.of("webhook", "wechat", "feishu", "dingtalk");
    private static final int MAX_RESPONSE_LOG_LENGTH = 500;

    private final NotificationChannelService notificationChannelService;
    private final NotificationHttpClient notificationHttpClient;
    private final IotProperties iotProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, Long> throttleTracker = new ConcurrentHashMap<>();

    public SystemErrorNotificationServiceImpl(NotificationChannelService notificationChannelService,
                                              NotificationHttpClient notificationHttpClient,
                                              IotProperties iotProperties,
                                              ObjectMapper objectMapper) {
        this.notificationChannelService = notificationChannelService;
        this.notificationHttpClient = notificationHttpClient;
        this.iotProperties = iotProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void notifySystemError(BackendExceptionEvent event, AuditLog auditLog) {
        if (!Boolean.TRUE.equals(iotProperties.getObservability().getSystemErrorNotifyEnabled())) {
            return;
        }

        for (NotificationChannel channel : notificationChannelService.listChannels(null, null, null)) {
            if (!isEnabled(channel) || !supportsWebhook(channel)) {
                continue;
            }
            ChannelConfig channelConfig = parseChannelConfig(channel, false);
            if (channelConfig == null || !channelConfig.scenes().contains(SYSTEM_ERROR_SCENE)) {
                continue;
            }
            if (shouldThrottle(buildThrottleKey(channel, event), channelConfig.minIntervalSeconds())) {
                log.debug("系统异常通知已节流, channelCode={}, requestUrl={}", channel.getChannelCode(), event.requestUrl());
                continue;
            }
            sendToChannel(channel, channelConfig, buildSystemErrorEnvelope(event, auditLog));
        }
    }

    @Override
    public void sendTestNotification(String channelCode) {
        NotificationChannel channel = notificationChannelService.getByCode(channelCode);
        if (channel == null || channel.getDeleted() != null && channel.getDeleted() == 1) {
            throw new BizException("通知渠道不存在: " + channelCode);
        }
        if (!isEnabled(channel)) {
            throw new BizException("通知渠道未启用: " + channelCode);
        }
        if (!supportsWebhook(channel)) {
            throw new BizException("当前测试仅支持 webhook/wechat/feishu/dingtalk 渠道");
        }
        ChannelConfig channelConfig = parseChannelConfig(channel, true);
        sendToChannel(channel, channelConfig, buildTestEnvelope(channel));
    }

    private boolean isEnabled(NotificationChannel channel) {
        return channel != null && Integer.valueOf(1).equals(channel.getStatus());
    }

    private boolean supportsWebhook(NotificationChannel channel) {
        return channel != null
                && StringUtils.hasText(channel.getChannelType())
                && WEBHOOK_CHANNEL_TYPES.contains(channel.getChannelType().trim().toLowerCase(Locale.ROOT));
    }

    private ChannelConfig parseChannelConfig(NotificationChannel channel, boolean failFast) {
        if (!StringUtils.hasText(channel.getConfig())) {
            if (failFast) {
                throw new BizException("通知渠道缺少 config 配置: " + channel.getChannelCode());
            }
            log.warn("系统异常通知跳过空配置渠道, channelCode={}", channel.getChannelCode());
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(channel.getConfig());
            String url = readText(root, "url");
            if (!StringUtils.hasText(url)) {
                if (failFast) {
                    throw new BizException("通知渠道配置缺少 url: " + channel.getChannelCode());
                }
                log.warn("系统异常通知跳过无 url 渠道, channelCode={}", channel.getChannelCode());
                return null;
            }

            Map<String, String> headers = new LinkedHashMap<>();
            JsonNode headersNode = root.get("headers");
            if (headersNode != null && headersNode.isObject()) {
                headersNode.forEachEntry((key, value) -> headers.put(key, value.asText("")));
            }

            List<String> scenes = new ArrayList<>();
            JsonNode scenesNode = root.get("scenes");
            if (scenesNode != null && scenesNode.isArray()) {
                scenesNode.forEach(item -> {
                    String scene = item.asText("");
                    if (StringUtils.hasText(scene)) {
                        scenes.add(scene.trim().toLowerCase(Locale.ROOT));
                    }
                });
            }
            String scene = readText(root, "scene");
            if (StringUtils.hasText(scene)) {
                scenes.add(scene.trim().toLowerCase(Locale.ROOT));
            }

            Integer timeoutMs = readInteger(root, "timeoutMs", iotProperties.getObservability().getNotificationTimeoutMs());
            Integer minIntervalSeconds = readInteger(root, "minIntervalSeconds", iotProperties.getObservability().getSystemErrorNotifyCooldownSeconds());
            return new ChannelConfig(
                    url.trim(),
                    headers,
                    List.copyOf(scenes),
                    Math.max(timeoutMs == null ? 3000 : timeoutMs, 1000),
                    Math.max(minIntervalSeconds == null ? 300 : minIntervalSeconds, 0)
            );
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            if (failFast) {
                throw new BizException("通知渠道配置 JSON 不合法: " + channel.getChannelCode());
            }
            log.warn("系统异常通知跳过非法配置渠道, channelCode={}, error={}", channel.getChannelCode(), ex.getMessage());
            return null;
        }
    }

    private NotificationEnvelope buildSystemErrorEnvelope(BackendExceptionEvent event, AuditLog auditLog) {
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
        return new NotificationEnvelope(DEFAULT_TITLE, plainText, markdown, genericPayload);
    }

    private NotificationEnvelope buildTestEnvelope(NotificationChannel channel) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> genericPayload = new LinkedHashMap<>();
        genericPayload.put("application", APP_NAME);
        genericPayload.put("eventType", "test");
        genericPayload.put("title", "spring-boot-iot 通知渠道测试");
        genericPayload.put("channelCode", channel.getChannelCode());
        genericPayload.put("channelType", channel.getChannelType());
        genericPayload.put("sentAt", now.format(TIME_FORMATTER));
        genericPayload.put("summary", "这是一条来自 spring-boot-iot 的测试通知，用于验证 webhook 渠道配置是否可用。");

        String markdown = """
                ### spring-boot-iot 通知渠道测试
                - 渠道编码：%s
                - 渠道类型：%s
                - 发送时间：%s
                - 说明：这是一条测试通知，用于验证 webhook 渠道配置是否可用。
                """.formatted(channel.getChannelCode(), channel.getChannelType(), now.format(TIME_FORMATTER));
        String plainText = """
                spring-boot-iot 通知渠道测试
                渠道编码: %s
                渠道类型: %s
                发送时间: %s
                说明: 这是一条测试通知，用于验证 webhook 渠道配置是否可用。
                """.formatted(channel.getChannelCode(), channel.getChannelType(), now.format(TIME_FORMATTER));
        return new NotificationEnvelope("spring-boot-iot 通知渠道测试", plainText, markdown, genericPayload);
    }

    private void sendToChannel(NotificationChannel channel,
                               ChannelConfig channelConfig,
                               NotificationEnvelope envelope) {
        try {
            String requestBody = buildRequestBody(channel, envelope);
            NotificationHttpClient.HttpResult response = notificationHttpClient.postJson(
                    channelConfig.url(),
                    channelConfig.headers(),
                    requestBody,
                    Duration.ofMillis(channelConfig.timeoutMs())
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("系统异常通知发送失败, channelCode={}, statusCode={}, response={}",
                        channel.getChannelCode(), response.statusCode(), truncate(response.responseBody()));
                return;
            }
            log.info("系统异常通知发送成功, channelCode={}, statusCode={}",
                    channel.getChannelCode(), response.statusCode());
        } catch (Exception ex) {
            // 通知失败不再反向写审计，避免递归异常风暴。
            log.warn("系统异常通知发送异常, channelCode={}, error={}", channel.getChannelCode(), ex.getMessage());
        }
    }

    private String buildRequestBody(NotificationChannel channel, NotificationEnvelope envelope) throws Exception {
        String normalizedType = channel.getChannelType().trim().toLowerCase(Locale.ROOT);
        return switch (normalizedType) {
            case "webhook" -> objectMapper.writeValueAsString(envelope.genericPayload());
            case "dingtalk" -> objectMapper.writeValueAsString(Map.of(
                    "msgtype", "markdown",
                    "markdown", Map.of(
                            "title", envelope.title(),
                            "text", envelope.markdownText()
                    )
            ));
            case "wechat" -> objectMapper.writeValueAsString(Map.of(
                    "msgtype", "markdown",
                    "markdown", Map.of("content", envelope.markdownText())
            ));
            case "feishu" -> objectMapper.writeValueAsString(Map.of(
                    "msg_type", "text",
                    "content", Map.of("text", envelope.plainText())
            ));
            default -> throw new BizException("不支持的通知渠道类型: " + channel.getChannelType());
        };
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

    private String buildThrottleKey(NotificationChannel channel, BackendExceptionEvent event) {
        return channel.getChannelCode() + "|" + event.operationModule() + "|" + event.operationMethod()
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

    private String readText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? null : node.asText();
    }

    private Integer readInteger(JsonNode root, String fieldName, Integer defaultValue) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        if (node.canConvertToInt()) {
            return node.asInt();
        }
        String text = node.asText();
        return StringUtils.hasText(text) ? Integer.parseInt(text.trim()) : defaultValue;
    }

    private record ChannelConfig(String url,
                                 Map<String, String> headers,
                                 List<String> scenes,
                                 int timeoutMs,
                                 int minIntervalSeconds) {
    }

    private record NotificationEnvelope(String title,
                                        String plainText,
                                        String markdownText,
                                        Map<String, Object> genericPayload) {
    }
}
