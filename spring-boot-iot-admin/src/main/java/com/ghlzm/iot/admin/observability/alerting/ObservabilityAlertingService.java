package com.ghlzm.iot.admin.observability.alerting;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.service.DeviceAccessErrorLogService;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.message.mqtt.MqttConsumerRuntimeState;
import com.ghlzm.iot.message.mqtt.MqttMessageConsumer;
import com.ghlzm.iot.system.service.AuditLogService;
import com.ghlzm.iot.system.service.InAppMessageBridgeAlertQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 规则化运维告警评估服务。
 */
@Slf4j
@Service
public class ObservabilityAlertingService {

    private static final String SYSTEM_ERROR_BURST = "system-error-burst";
    private static final String MQTT_DISCONNECT_TIMEOUT = "mqtt-disconnect-timeout";
    private static final String FAILURE_STAGE_SPIKE = "failure-stage-spike";
    private static final String IN_APP_BRIDGE_FAILURE_BURST = "in-app-bridge-failure-burst";
    private static final String GLOBAL_DIMENSION_KEY = "global";
    private static final String COOLDOWN_KEY_PREFIX = "iot:observability:alerting:cooldown:";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditLogService auditLogService;
    private final DeviceAccessErrorLogService deviceAccessErrorLogService;
    private final InAppMessageBridgeAlertQueryService inAppMessageBridgeAlertQueryService;
    private final MqttMessageConsumer mqttMessageConsumer;
    private final MqttConsumerRuntimeState mqttConsumerRuntimeState;
    private final ObservabilityAlertNotificationService observabilityAlertNotificationService;
    private final StringRedisTemplate stringRedisTemplate;
    private final IotProperties iotProperties;
    private final Clock clock;

    @Autowired
    public ObservabilityAlertingService(AuditLogService auditLogService,
                                        DeviceAccessErrorLogService deviceAccessErrorLogService,
                                        InAppMessageBridgeAlertQueryService inAppMessageBridgeAlertQueryService,
                                        MqttMessageConsumer mqttMessageConsumer,
                                        MqttConsumerRuntimeState mqttConsumerRuntimeState,
                                        ObservabilityAlertNotificationService observabilityAlertNotificationService,
                                        StringRedisTemplate stringRedisTemplate,
                                        IotProperties iotProperties) {
        this(
                auditLogService,
                deviceAccessErrorLogService,
                inAppMessageBridgeAlertQueryService,
                mqttMessageConsumer,
                mqttConsumerRuntimeState,
                observabilityAlertNotificationService,
                stringRedisTemplate,
                iotProperties,
                Clock.systemDefaultZone()
        );
    }

    ObservabilityAlertingService(AuditLogService auditLogService,
                                 DeviceAccessErrorLogService deviceAccessErrorLogService,
                                 InAppMessageBridgeAlertQueryService inAppMessageBridgeAlertQueryService,
                                 MqttMessageConsumer mqttMessageConsumer,
                                 MqttConsumerRuntimeState mqttConsumerRuntimeState,
                                 ObservabilityAlertNotificationService observabilityAlertNotificationService,
                                 StringRedisTemplate stringRedisTemplate,
                                 IotProperties iotProperties,
                                 Clock clock) {
        this.auditLogService = auditLogService;
        this.deviceAccessErrorLogService = deviceAccessErrorLogService;
        this.inAppMessageBridgeAlertQueryService = inAppMessageBridgeAlertQueryService;
        this.mqttMessageConsumer = mqttMessageConsumer;
        this.mqttConsumerRuntimeState = mqttConsumerRuntimeState;
        this.observabilityAlertNotificationService = observabilityAlertNotificationService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.iotProperties = iotProperties;
        this.clock = clock;
    }

    public void evaluateAlerts() {
        IotProperties.Observability.Alerting config = iotProperties.getObservability().getAlerting();
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }

        TraceContextHolder.bindTraceId(null);
        try {
            Instant now = clock.instant();
            evaluateSafely(SYSTEM_ERROR_BURST, () -> evaluateSystemErrorAlerts(now, config));
            evaluateSafely(MQTT_DISCONNECT_TIMEOUT, () -> evaluateMqttDisconnectAlert(now, config));
            evaluateSafely(FAILURE_STAGE_SPIKE, () -> evaluateFailureStageAlerts(now, config));
            evaluateSafely(IN_APP_BRIDGE_FAILURE_BURST, () -> evaluateInAppBridgeAlerts(now, config));
        } finally {
            TraceContextHolder.clear();
        }
    }

    private void evaluateSystemErrorAlerts(Instant now, IotProperties.Observability.Alerting alertingConfig) {
        IotProperties.Observability.Alerting.SystemError config = alertingConfig.getSystemError();
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }
        int windowMinutes = positiveOrDefault(config.getWindowMinutes(), 10);
        int threshold = positiveOrDefault(config.getThreshold(), 5);
        Date startTime = Date.from(now.minus(Duration.ofMinutes(windowMinutes)));
        long count = safeLong(auditLogService.countSystemErrorsSince(startTime));
        if (count < threshold) {
            return;
        }
        triggerAlert(
                new ObservabilityAlertTrigger(
                        SYSTEM_ERROR_BURST,
                        GLOBAL_DIMENSION_KEY,
                        "全局 system_error",
                        "最近窗口内 system_error 数",
                        count,
                        threshold,
                        windowMinutes,
                        null,
                        "最近 %d 分钟内 system_error 达到 %d 次，触发阈值 %d 次。".formatted(windowMinutes, count, threshold),
                        Map.of("windowStart", formatInstant(startTime.toInstant()), "windowEnd", formatInstant(now))
                ),
                alertingConfig
        );
    }

    private void evaluateMqttDisconnectAlert(Instant now, IotProperties.Observability.Alerting alertingConfig) {
        IotProperties.Observability.Alerting.MqttDisconnect config = alertingConfig.getMqttDisconnect();
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }
        if (iotProperties.getMqtt() == null || !Boolean.TRUE.equals(iotProperties.getMqtt().getEnabled())) {
            return;
        }
        if (mqttMessageConsumer.isConnected()) {
            return;
        }

        MqttConsumerRuntimeState.Snapshot snapshot = mqttConsumerRuntimeState.snapshot();
        LocalDateTime disconnectedAt = snapshot.lastDisconnectAt();
        if (disconnectedAt == null) {
            return;
        }
        long disconnectedMinutes = Duration.between(disconnectedAt.atZone(clock.getZone()).toInstant(), now).toMinutes();
        int durationMinutes = positiveOrDefault(config.getDurationMinutes(), 5);
        if (disconnectedMinutes < durationMinutes) {
            return;
        }

        String clientId = resolveClientId();
        String brokerUrl = maskBrokerUrl(iotProperties.getMqtt().getBrokerUrl());
        triggerAlert(
                new ObservabilityAlertTrigger(
                        MQTT_DISCONNECT_TIMEOUT,
                        normalizeKey(clientId, "mqtt-consumer"),
                        clientId + " @ " + brokerUrl,
                        "MQTT consumer 持续断连分钟数",
                        disconnectedMinutes,
                        durationMinutes,
                        null,
                        durationMinutes,
                        "MQTT consumer %s 已持续断连 %d 分钟，达到阈值 %d 分钟。".formatted(clientId, disconnectedMinutes, durationMinutes),
                        Map.of(
                                "clientId", clientId,
                                "brokerUrl", brokerUrl,
                                "lastDisconnectAt", disconnectedAt.format(TIME_FORMATTER),
                                "lastConnectAt", snapshot.lastConnectAt() == null ? "-" : snapshot.lastConnectAt().format(TIME_FORMATTER),
                                "lastFailureStage", snapshot.lastFailureStage() == null ? "-" : snapshot.lastFailureStage()
                        )
                ),
                alertingConfig
        );
    }

    private void evaluateFailureStageAlerts(Instant now, IotProperties.Observability.Alerting alertingConfig) {
        IotProperties.Observability.Alerting.FailureStage config = alertingConfig.getFailureStage();
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }
        int windowMinutes = positiveOrDefault(config.getWindowMinutes(), 10);
        int threshold = positiveOrDefault(config.getThreshold(), 10);
        Date startTime = Date.from(now.minus(Duration.ofMinutes(windowMinutes)));
        List<DeviceAccessErrorLogService.FailureStageCount> counts =
                deviceAccessErrorLogService.listFailureStageCountsSince(startTime);
        for (DeviceAccessErrorLogService.FailureStageCount bucket : counts) {
            if (bucket == null || bucket.failureCount() < threshold) {
                continue;
            }
            String failureStage = normalizeKey(bucket.failureStage(), "unknown-stage");
            triggerAlert(
                    new ObservabilityAlertTrigger(
                            FAILURE_STAGE_SPIKE,
                            failureStage,
                            "failureStage=" + failureStage,
                            "最近窗口内同一 failureStage 失败数",
                            bucket.failureCount(),
                            threshold,
                            windowMinutes,
                            null,
                            "最近 %d 分钟内 failureStage=%s 失败达到 %d 次，触发阈值 %d 次。"
                                    .formatted(windowMinutes, failureStage, bucket.failureCount(), threshold),
                            Map.of("windowStart", formatInstant(startTime.toInstant()), "windowEnd", formatInstant(now))
                    ),
                    alertingConfig
            );
        }
    }

    private void evaluateInAppBridgeAlerts(Instant now, IotProperties.Observability.Alerting alertingConfig) {
        IotProperties.Observability.Alerting.InAppBridge config = alertingConfig.getInAppBridge();
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }
        int windowMinutes = positiveOrDefault(config.getWindowMinutes(), 10);
        int threshold = positiveOrDefault(config.getThreshold(), 3);
        Date startTime = Date.from(now.minus(Duration.ofMinutes(windowMinutes)));
        List<InAppMessageBridgeAlertQueryService.ChannelFailureCount> counts =
                inAppMessageBridgeAlertQueryService.listFailedAttemptCountsSince(startTime);
        for (InAppMessageBridgeAlertQueryService.ChannelFailureCount bucket : counts) {
            if (bucket == null || bucket.failureCount() < threshold) {
                continue;
            }
            String channelCode = normalizeKey(bucket.channelCode(), "unknown-channel");
            String channelName = StringUtils.hasText(bucket.channelName()) ? bucket.channelName() : bucket.channelCode();
            triggerAlert(
                    new ObservabilityAlertTrigger(
                            IN_APP_BRIDGE_FAILURE_BURST,
                            channelCode,
                            channelName + " (" + channelCode + ")",
                            "最近窗口内站内信桥接失败次数",
                            bucket.failureCount(),
                            threshold,
                            windowMinutes,
                            null,
                            "最近 %d 分钟内渠道 %s 的站内信桥接失败达到 %d 次，触发阈值 %d 次。"
                                    .formatted(windowMinutes, channelName, bucket.failureCount(), threshold),
                            Map.of("channelCode", channelCode, "channelName", channelName)
                    ),
                    alertingConfig
            );
        }
    }

    private void triggerAlert(ObservabilityAlertTrigger trigger, IotProperties.Observability.Alerting alertingConfig) {
        int cooldownMinutes = positiveOrDefault(alertingConfig.getCooldownMinutes(), 30);
        if (!acquireCooldown(trigger, cooldownMinutes)) {
            return;
        }
        ObservabilityAlertNotificationService.DispatchSummary dispatchSummary =
                observabilityAlertNotificationService.dispatchAlert(trigger);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", TraceContextHolder.getTraceId());
        details.put("scene", dispatchSummary.scene());
        details.put("ruleType", trigger.ruleType());
        details.put("dimensionKey", trigger.dimensionKey());
        details.put("metricLabel", trigger.metricLabel());
        details.put("observedValue", trigger.observedValue());
        details.put("threshold", trigger.threshold());
        details.put("windowMinutes", trigger.windowMinutes());
        details.put("durationMinutes", trigger.durationMinutes());
        details.put("cooldownMinutes", cooldownMinutes);
        details.put("channelCount", dispatchSummary.channelCount());
        details.put("successCount", dispatchSummary.successCount());
        details.put("failureCount", dispatchSummary.failureCount());
        details.put("summary", trigger.summary());
        log.warn(ObservabilityEventLogSupport.summary(
                "observability_alert",
                dispatchSummary.channelCount() > 0 ? "triggered" : "no_channel",
                null,
                details
        ));
    }

    private boolean acquireCooldown(ObservabilityAlertTrigger trigger, int cooldownMinutes) {
        String key = buildCooldownKey(trigger);
        try {
            ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
            Boolean acquired = valueOperations.setIfAbsent(
                    key,
                    formatInstant(clock.instant()),
                    Duration.ofMinutes(cooldownMinutes)
            );
            return Boolean.TRUE.equals(acquired);
        } catch (Exception ex) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("traceId", TraceContextHolder.getTraceId());
            details.put("ruleType", trigger.ruleType());
            details.put("dimensionKey", trigger.dimensionKey());
            details.put("cooldownMinutes", cooldownMinutes);
            details.put("reason", ex.getMessage());
            log.warn(ObservabilityEventLogSupport.summary("observability_alert_cooldown", "failure", null, details));
            return false;
        }
    }

    private void evaluateSafely(String ruleType, Runnable task) {
        try {
            task.run();
        } catch (BizException ex) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("traceId", TraceContextHolder.getTraceId());
            details.put("ruleType", ruleType);
            details.put("reason", ex.getMessage());
            log.warn(ObservabilityEventLogSupport.summary("observability_alert_evaluation", "skipped", null, details));
        } catch (Exception ex) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("traceId", TraceContextHolder.getTraceId());
            details.put("ruleType", ruleType);
            details.put("reason", ex.getMessage());
            log.error(ObservabilityEventLogSupport.summary("observability_alert_evaluation", "failure", null, details), ex);
        }
    }

    private String buildCooldownKey(ObservabilityAlertTrigger trigger) {
        return COOLDOWN_KEY_PREFIX
                + normalizeKey(trigger.ruleType(), "unknown-rule")
                + ":"
                + normalizeKey(trigger.dimensionKey(), GLOBAL_DIMENSION_KEY);
    }

    private String resolveClientId() {
        String clientId = mqttMessageConsumer.getEffectiveClientId();
        if (StringUtils.hasText(clientId)) {
            return clientId.trim();
        }
        if (iotProperties.getMqtt() != null && StringUtils.hasText(iotProperties.getMqtt().getClientId())) {
            return iotProperties.getMqtt().getClientId().trim();
        }
        return "mqtt-consumer";
    }

    private String maskBrokerUrl(String brokerUrl) {
        if (!StringUtils.hasText(brokerUrl)) {
            return "-";
        }
        int schemeIndex = brokerUrl.indexOf("://");
        int atIndex = brokerUrl.indexOf('@');
        if (schemeIndex < 0 || atIndex < 0 || atIndex <= schemeIndex + 3) {
            return brokerUrl;
        }
        return brokerUrl.substring(0, schemeIndex + 3) + "***" + brokerUrl.substring(atIndex);
    }

    private String normalizeKey(String value, String fallback) {
        String normalized = StringUtils.hasText(value)
                ? value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9:_-]+", "_")
                : fallback;
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private int positiveOrDefault(Integer value, int defaultValue) {
        return value == null || value < 1 ? defaultValue : value;
    }

    private String formatInstant(Instant instant) {
        return LocalDateTime.ofInstant(instant, resolveZone()).format(TIME_FORMATTER);
    }

    private ZoneId resolveZone() {
        return clock.getZone() == null ? ZoneId.systemDefault() : clock.getZone();
    }
}
