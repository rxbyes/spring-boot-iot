package com.ghlzm.iot.framework.observability.messageflow;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis message-flow 时间线存储实现。
 */
@Component
public class RedisMessageFlowTimelineStore implements MessageFlowTimelineStore {

    private static final String TRACE_KEY_PREFIX = "iot:message-flow:trace:";
    private static final String SESSION_KEY_PREFIX = "iot:message-flow:session:";
    private static final String FINGERPRINT_KEY_PREFIX = "iot:message-flow:fingerprint:";

    private final StringRedisTemplate stringRedisTemplate;
    private final MessageFlowProperties messageFlowProperties;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public RedisMessageFlowTimelineStore(StringRedisTemplate stringRedisTemplate,
                                         MessageFlowProperties messageFlowProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.messageFlowProperties = messageFlowProperties;
    }

    @Override
    public void saveSession(MessageFlowSession session) {
        if (session == null || !hasText(session.getSessionId())) {
            return;
        }
        writeJson(buildSessionKey(session.getSessionId()), session, resolveTimelineTtl());
    }

    @Override
    public Optional<MessageFlowSession> getSession(String sessionId) {
        return readJson(buildSessionKey(sessionId), MessageFlowSession.class);
    }

    @Override
    public void saveTimeline(MessageFlowTimeline timeline) {
        if (timeline == null || !hasText(timeline.getTraceId())) {
            return;
        }
        writeJson(buildTraceKey(timeline.getTraceId()), timeline, resolveTimelineTtl());
    }

    @Override
    public Optional<MessageFlowTimeline> getTimeline(String traceId) {
        return readJson(buildTraceKey(traceId), MessageFlowTimeline.class);
    }

    @Override
    public void bindFingerprint(String fingerprint, String sessionId) {
        if (!hasText(fingerprint) || !hasText(sessionId)) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                buildFingerprintKey(fingerprint),
                sessionId.trim(),
                resolveFingerprintTtl()
        );
    }

    @Override
    public Optional<String> getSessionIdByFingerprint(String fingerprint) {
        if (!hasText(fingerprint)) {
            return Optional.empty();
        }
        String value = stringRedisTemplate.opsForValue().get(buildFingerprintKey(fingerprint));
        return hasText(value) ? Optional.of(value.trim()) : Optional.empty();
    }

    private <T> void writeJson(String key, T value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ex) {
            throw new IllegalStateException("写入 message-flow Redis 失败: " + key, ex);
        }
    }

    private <T> Optional<T> readJson(String key, Class<T> type) {
        if (!hasText(key)) {
            return Optional.empty();
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (!hasText(json)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, type));
        } catch (Exception ex) {
            throw new IllegalStateException("读取 message-flow Redis 失败: " + key, ex);
        }
    }

    private Duration resolveTimelineTtl() {
        Integer ttlHours = messageFlowProperties.getTtlHours();
        return Duration.ofHours(ttlHours == null || ttlHours < 1 ? 24L : ttlHours.longValue());
    }

    private Duration resolveFingerprintTtl() {
        Integer seconds = messageFlowProperties.getSessionMatchWindowSeconds();
        return Duration.ofSeconds(seconds == null || seconds < 1 ? 120L : seconds.longValue());
    }

    private String buildTraceKey(String traceId) {
        return TRACE_KEY_PREFIX + traceId.trim();
    }

    private String buildSessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId.trim();
    }

    private String buildFingerprintKey(String fingerprint) {
        return FINGERPRINT_KEY_PREFIX + fingerprint.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
