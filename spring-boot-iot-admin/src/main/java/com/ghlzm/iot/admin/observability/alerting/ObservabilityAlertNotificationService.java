package com.ghlzm.iot.admin.observability.alerting;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.system.service.NotificationChannelDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则化运维告警通知分发服务。
 */
@Slf4j
@Service
public class ObservabilityAlertNotificationService {

    static final String DEFAULT_SCENE = "observability_alert";

    private static final String APP_NAME = "spring-boot-iot";
    private static final String DEFAULT_TITLE = "spring-boot-iot 规则化运维告警";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_RESPONSE_LOG_LENGTH = 500;

    private final NotificationChannelDispatcher notificationChannelDispatcher;
    private final IotProperties iotProperties;

    public ObservabilityAlertNotificationService(NotificationChannelDispatcher notificationChannelDispatcher,
                                                 IotProperties iotProperties) {
        this.notificationChannelDispatcher = notificationChannelDispatcher;
        this.iotProperties = iotProperties;
    }

    public DispatchSummary dispatchAlert(ObservabilityAlertTrigger trigger) {
        String scene = resolveScene();
        List<NotificationChannelDispatcher.DispatchChannel> channels =
                notificationChannelDispatcher.listSceneChannels(scene);
        if (channels.isEmpty()) {
            return new DispatchSummary(scene, 0, 0, 0, List.of());
        }

        NotificationChannelDispatcher.NotificationEnvelope envelope = buildEnvelope(trigger, scene);
        List<String> channelCodes = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        for (NotificationChannelDispatcher.DispatchChannel channel : channels) {
            channelCodes.add(channel.channel().getChannelCode());
            long startNs = System.nanoTime();
            NotificationChannelDispatcher.DispatchResult result =
                    notificationChannelDispatcher.send(channel, envelope);
            if (result.success()) {
                successCount++;
                log.info(ObservabilityEventLogSupport.summary(
                        "notification_dispatch",
                        "success",
                        elapsedMillis(startNs),
                        buildDispatchDetails(scene, trigger, channel, result, null)
                ));
                continue;
            }
            failureCount++;
            log.warn(ObservabilityEventLogSupport.summary(
                    "notification_dispatch",
                    "failure",
                    elapsedMillis(startNs),
                    buildDispatchDetails(
                            scene,
                            trigger,
                            channel,
                            result,
                            truncate(safeText(result.responseBody(), result.errorMessage()))
                    )
            ));
        }
        return new DispatchSummary(scene, channels.size(), successCount, failureCount, List.copyOf(channelCodes));
    }

    private NotificationChannelDispatcher.NotificationEnvelope buildEnvelope(ObservabilityAlertTrigger trigger, String scene) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> genericPayload = new LinkedHashMap<>();
        genericPayload.put("application", APP_NAME);
        genericPayload.put("eventType", scene);
        genericPayload.put("title", DEFAULT_TITLE);
        genericPayload.put("ruleType", trigger.ruleType());
        genericPayload.put("dimensionKey", trigger.dimensionKey());
        genericPayload.put("dimensionLabel", trigger.dimensionLabel());
        genericPayload.put("metricLabel", trigger.metricLabel());
        genericPayload.put("observedValue", trigger.observedValue());
        genericPayload.put("threshold", trigger.threshold());
        genericPayload.put("windowMinutes", trigger.windowMinutes());
        genericPayload.put("durationMinutes", trigger.durationMinutes());
        genericPayload.put("summary", trigger.summary());
        genericPayload.put("traceId", TraceContextHolder.getTraceId());
        genericPayload.put("triggeredAt", now.format(TIME_FORMATTER));
        if (!trigger.context().isEmpty()) {
            genericPayload.put("context", trigger.context());
        }

        String markdown = """
                ### spring-boot-iot 规则化运维告警
                - 规则：%s
                - 维度：%s
                - 指标：%s
                - 当前值：%s
                - 阈值：%s
                - 时间窗：%s
                - 持续时长：%s
                - 触发时间：%s
                - 摘要：%s
                """.formatted(
                resolveRuleLabel(trigger.ruleType()),
                safeText(trigger.dimensionLabel(), trigger.dimensionKey()),
                safeText(trigger.metricLabel()),
                trigger.observedValue(),
                trigger.threshold(),
                formatMinutes(trigger.windowMinutes()),
                formatMinutes(trigger.durationMinutes()),
                now.format(TIME_FORMATTER),
                safeText(trigger.summary())
        );
        String plainText = """
                spring-boot-iot 规则化运维告警
                规则: %s
                维度: %s
                指标: %s
                当前值: %s
                阈值: %s
                时间窗: %s
                持续时长: %s
                触发时间: %s
                摘要: %s
                """.formatted(
                resolveRuleLabel(trigger.ruleType()),
                safeText(trigger.dimensionLabel(), trigger.dimensionKey()),
                safeText(trigger.metricLabel()),
                trigger.observedValue(),
                trigger.threshold(),
                formatMinutes(trigger.windowMinutes()),
                formatMinutes(trigger.durationMinutes()),
                now.format(TIME_FORMATTER),
                safeText(trigger.summary())
        );
        return new NotificationChannelDispatcher.NotificationEnvelope(DEFAULT_TITLE, plainText, markdown, genericPayload);
    }

    private Map<String, Object> buildDispatchDetails(String scene,
                                                     ObservabilityAlertTrigger trigger,
                                                     NotificationChannelDispatcher.DispatchChannel channel,
                                                     NotificationChannelDispatcher.DispatchResult result,
                                                     String responseSummary) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("scene", scene);
        details.put("ruleType", trigger.ruleType());
        details.put("dimensionKey", trigger.dimensionKey());
        details.put("channelCode", channel.channel().getChannelCode());
        details.put("channelType", channel.channel().getChannelType());
        details.put("statusCode", result == null ? null : result.statusCode());
        details.put("traceId", TraceContextHolder.getTraceId());
        details.put("response", responseSummary);
        return details;
    }

    private String resolveScene() {
        String configured = iotProperties.getObservability().getAlerting().getScene();
        return StringUtils.hasText(configured) ? configured.trim() : DEFAULT_SCENE;
    }

    private String resolveRuleLabel(String ruleType) {
        if (!StringUtils.hasText(ruleType)) {
            return "未知规则";
        }
        return switch (ruleType.trim()) {
            case "system-error-burst" -> "系统异常突增";
            case "mqtt-disconnect-timeout" -> "MQTT 断连超时";
            case "failure-stage-spike" -> "接入失败阶段突增";
            case "in-app-bridge-failure-burst" -> "站内信桥接失败突增";
            default -> ruleType;
        };
    }

    private String formatMinutes(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return "-";
        }
        return minutes + " 分钟";
    }

    private String safeText(Object value) {
        return safeText(value, "-");
    }

    private String safeText(Object primary, Object fallback) {
        if (primary != null && StringUtils.hasText(String.valueOf(primary))) {
            return String.valueOf(primary);
        }
        return fallback == null ? "-" : String.valueOf(fallback);
    }

    private String truncate(String text) {
        if (!StringUtils.hasText(text) || text.length() <= MAX_RESPONSE_LOG_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_RESPONSE_LOG_LENGTH) + "...(truncated)";
    }

    private long elapsedMillis(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    public record DispatchSummary(String scene,
                                  int channelCount,
                                  int successCount,
                                  int failureCount,
                                  List<String> channelCodes) {
    }
}
