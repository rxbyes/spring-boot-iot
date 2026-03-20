package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT 报文安全校验器。
 * 负责签名、时间戳、防重放三类校验。
 *
 * 兼容原则：
 * 1. 老报文没有安全头时不阻断现有主链路
 * 2. 一旦报文带了签名字段，就按完整安全规则校验
 */
@Component
public class MqttPayloadSecurityValidator {

    private final IotProperties iotProperties;
    private final MqttMessageSignerRegistry mqttMessageSignerRegistry;
    private final StringRedisTemplate stringRedisTemplate;
    private final Map<String, Instant> localReplayCache = new ConcurrentHashMap<>();

    public MqttPayloadSecurityValidator(IotProperties iotProperties,
                                        MqttMessageSignerRegistry mqttMessageSignerRegistry,
                                        ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider) {
        this.iotProperties = iotProperties;
        this.mqttMessageSignerRegistry = mqttMessageSignerRegistry;
        this.stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
    }

    @SuppressWarnings("unchecked")
    public void validateEnvelope(String appId, Map<String, Object> payloadMap, String bodyContent) {
        if (!(payloadMap.get("header") instanceof Map<?, ?> headerMap)) {
            return;
        }

        String signature = textValue(headerMap.get("signature"));
        if (!hasText(signature)) {
            signature = textValue(headerMap.get("sign"));
        }

        String timestamp = firstNonBlank(
                textValue(headerMap.get("timestamp")),
                textValue(headerMap.get("ts"))
        );
        String nonce = textValue(headerMap.get("nonce"));
        String algorithm = firstNonBlank(
                textValue(headerMap.get("signAlgorithm")),
                textValue(headerMap.get("signatureAlgorithm")),
                textValue(headerMap.get("algorithm")),
                iotProperties.getProtocol().getSecurity().getDefaultSignAlgorithm()
        );

        if (!hasText(signature) && !hasText(timestamp) && !hasText(nonce)) {
            return;
        }

        if (!hasText(timestamp)) {
            throw new BizException("安全报文缺少 timestamp");
        }
        if (!hasText(nonce)) {
            throw new BizException("安全报文缺少 nonce");
        }
        if (!hasText(signature)) {
            throw new BizException("安全报文缺少 signature");
        }

        validateTimestamp(timestamp);
        validateReplay(appId, nonce, timestamp);

        String signContent = buildSignContent(appId, timestamp, nonce, bodyContent);
        boolean verified = mqttMessageSignerRegistry.verify(algorithm, appId, signContent, signature);
        if (!verified) {
            throw new BizException("MQTT 报文签名校验失败");
        }
    }

    public String buildSignContent(String appId, String timestamp, String nonce, String bodyContent) {
        return "appId=" + safe(appId)
                + "&timestamp=" + safe(timestamp)
                + "&nonce=" + safe(nonce)
                + "&body=" + safe(bodyContent);
    }

    private void validateTimestamp(String timestamp) {
        long epochMillis;
        try {
            epochMillis = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            throw new BizException("timestamp 格式非法: " + timestamp);
        }
        long allowedSkewSeconds = defaultIfNull(iotProperties.getProtocol().getSecurity().getAllowedTimestampSkewSeconds(), 300);
        long now = Instant.now().toEpochMilli();
        long diff = Math.abs(now - epochMillis);
        if (diff > allowedSkewSeconds * 1000L) {
            throw new BizException("timestamp 超出允许时间窗");
        }
    }

    private void validateReplay(String appId, String nonce, String timestamp) {
        String replayKey = iotProperties.getProtocol().getSecurity().getReplayKeyPrefix() + appId + ":" + nonce + ":" + timestamp;
        Duration ttl = Duration.ofSeconds(defaultIfNull(iotProperties.getProtocol().getSecurity().getReplayWindowSeconds(), 600));

        if (stringRedisTemplate != null) {
            try {
                Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(replayKey, "1", ttl);
                if (Boolean.FALSE.equals(success)) {
                    throw new BizException("检测到重复报文");
                }
                return;
            } catch (BizException ex) {
                throw ex;
            } catch (Exception ignored) {
                // Redis 不可用时回退到本地缓存，保证最小可运行能力。
            }
        }

        cleanupExpiredReplayRecord();
        Instant expireAt = Instant.now().plus(ttl);
        Instant existed = localReplayCache.putIfAbsent(replayKey, expireAt);
        if (existed != null && existed.isAfter(Instant.now())) {
            throw new BizException("检测到重复报文");
        }
    }

    private void cleanupExpiredReplayRecord() {
        Instant now = Instant.now();
        localReplayCache.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isBefore(now));
    }

    private String textValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (hasText(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private int defaultIfNull(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String safe(String value) {
        return Objects.toString(value, "");
    }
}
