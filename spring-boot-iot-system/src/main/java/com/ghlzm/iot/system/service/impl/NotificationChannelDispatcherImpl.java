package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.system.entity.NotificationChannel;
import com.ghlzm.iot.system.service.NotificationChannelDispatcher;
import com.ghlzm.iot.system.service.NotificationChannelService;
import com.ghlzm.iot.system.service.NotificationHttpClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 通知渠道分发器，统一承接渠道配置解析与 webhook/企业消息发送。
 */
@Service
public class NotificationChannelDispatcherImpl implements NotificationChannelDispatcher {

    private static final Set<String> SUPPORTED_CHANNEL_TYPES = Set.of("webhook", "wechat", "feishu", "dingtalk");

    private final NotificationChannelService notificationChannelService;
    private final NotificationHttpClient notificationHttpClient;
    private final IotProperties iotProperties;
    private final ObjectMapper objectMapper;

    public NotificationChannelDispatcherImpl(NotificationChannelService notificationChannelService,
                                             NotificationHttpClient notificationHttpClient,
                                             IotProperties iotProperties,
                                             ObjectMapper objectMapper) {
        this.notificationChannelService = notificationChannelService;
        this.notificationHttpClient = notificationHttpClient;
        this.iotProperties = iotProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<DispatchChannel> listSceneChannels(String scene) {
        if (!StringUtils.hasText(scene)) {
            return List.of();
        }
        String normalizedScene = scene.trim().toLowerCase(Locale.ROOT);
        List<DispatchChannel> channels = new ArrayList<>();
        for (NotificationChannel channel : notificationChannelService.listChannels(null, null, null)) {
            if (!isEnabled(channel) || !supportsChannelType(channel)) {
                continue;
            }
            ChannelConfig config = parseChannelConfig(channel, false);
            if (config == null || !config.scenes().contains(normalizedScene)) {
                continue;
            }
            channels.add(new DispatchChannel(channel, config));
        }
        return List.copyOf(channels);
    }

    @Override
    public DispatchChannel requireTestChannel(String channelCode) {
        NotificationChannel channel = notificationChannelService.getByCode(channelCode);
        if (channel == null) {
            throw new BizException("通知渠道不存在: " + channelCode);
        }
        if (!isEnabled(channel)) {
            throw new BizException("通知渠道未启用: " + channelCode);
        }
        if (!supportsChannelType(channel)) {
            throw new BizException("当前测试仅支持 webhook/wechat/feishu/dingtalk 渠道");
        }
        return new DispatchChannel(channel, parseChannelConfig(channel, true));
    }

    @Override
    public DispatchResult send(DispatchChannel dispatchChannel, NotificationEnvelope envelope) {
        try {
            String requestBody = buildRequestBody(dispatchChannel.channel(), envelope);
            NotificationHttpClient.HttpResult response = notificationHttpClient.postJson(
                    dispatchChannel.config().url(),
                    dispatchChannel.config().headers(),
                    requestBody,
                    Duration.ofMillis(dispatchChannel.config().timeoutMs())
            );
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            return new DispatchResult(success, response.statusCode(), response.responseBody(), success ? null : "HTTP " + response.statusCode());
        } catch (Exception ex) {
            return new DispatchResult(false, null, null, ex.getMessage());
        }
    }

    private boolean isEnabled(NotificationChannel channel) {
        return channel != null
                && (channel.getDeleted() == null || channel.getDeleted() == 0)
                && Integer.valueOf(1).equals(channel.getStatus());
    }

    private boolean supportsChannelType(NotificationChannel channel) {
        return channel != null
                && StringUtils.hasText(channel.getChannelType())
                && SUPPORTED_CHANNEL_TYPES.contains(channel.getChannelType().trim().toLowerCase(Locale.ROOT));
    }

    private ChannelConfig parseChannelConfig(NotificationChannel channel, boolean failFast) {
        if (!StringUtils.hasText(channel.getConfig())) {
            if (failFast) {
                throw new BizException("通知渠道缺少 config 配置: " + channel.getChannelCode());
            }
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(channel.getConfig());
            String url = readText(root, "url");
            if (!StringUtils.hasText(url)) {
                if (failFast) {
                    throw new BizException("通知渠道配置缺少 url: " + channel.getChannelCode());
                }
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
            return null;
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
}
