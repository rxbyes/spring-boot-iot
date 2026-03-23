package com.ghlzm.iot.framework.observability.messageflow;

import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Optional;

/**
 * Redis message-flow 时间线存储实现。
 */
@Component
public class RedisMessageFlowTimelineStore implements MessageFlowTimelineStore {

    private static final Logger messageFlowLogger =
            LoggerFactory.getLogger(MessageFlowLoggingConstants.MESSAGE_FLOW_LOGGER_NAME);

    private static final String TRACE_KEY_PREFIX = "iot:message-flow:trace:";
    private static final String SESSION_KEY_PREFIX = "iot:message-flow:session:";
    private static final String FINGERPRINT_KEY_PREFIX = "iot:message-flow:fingerprint:";
    private static final String RECENT_SESSIONS_KEY = "iot:message-flow:recent:sessions";

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
        indexRecentSession(session);
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
        try {
            stringRedisTemplate.opsForValue().set(
                    buildFingerprintKey(fingerprint),
                    sessionId.trim(),
                    resolveFingerprintTtl()
            );
        } catch (Exception ex) {
            throw storageError("bindFingerprint", buildFingerprintKey(fingerprint), ex);
        }
    }

    @Override
    public Optional<String> getSessionIdByFingerprint(String fingerprint) {
        if (!hasText(fingerprint)) {
            return Optional.empty();
        }
        try {
            String value = stringRedisTemplate.opsForValue().get(buildFingerprintKey(fingerprint));
            return hasText(value) ? Optional.of(value.trim()) : Optional.empty();
        } catch (Exception ex) {
            throw storageError("getFingerprint", buildFingerprintKey(fingerprint), ex);
        }
    }

    @Override
    public List<String> getRecentSessionIds(int limit) {
        int safeLimit = Math.min(resolveRecentSessionLimit(), limit < 1 ? 10 : limit);
        if (safeLimit < 1) {
            return List.of();
        }
        try {
            Set<String> values = stringRedisTemplate.opsForZSet().reverseRange(RECENT_SESSIONS_KEY, 0, safeLimit - 1L);
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            return new ArrayList<>(values);
        } catch (Exception ex) {
            throw storageError("getRecentSessionIds", RECENT_SESSIONS_KEY, ex);
        }
    }

    @Override
    public void removeRecentSession(String sessionId) {
        if (!hasText(sessionId)) {
            return;
        }
        try {
            stringRedisTemplate.opsForZSet().remove(RECENT_SESSIONS_KEY, sessionId.trim());
        } catch (Exception ex) {
            throw storageError("removeRecentSession", RECENT_SESSIONS_KEY, ex);
        }
    }

    private <T> void writeJson(String key, T value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ex) {
            throw storageError("writeJson", key, ex);
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
            throw storageError("readJson", key, ex);
        }
    }

    private void indexRecentSession(MessageFlowSession session) {
        try {
            stringRedisTemplate.opsForZSet().add(
                    RECENT_SESSIONS_KEY,
                    session.getSessionId().trim(),
                    resolveRecentSessionScore(session)
            );
            trimRecentSessionIndex();
            stringRedisTemplate.expire(RECENT_SESSIONS_KEY, resolveTimelineTtl());
        } catch (Exception ex) {
            throw storageError("indexRecentSession", RECENT_SESSIONS_KEY, ex);
        }
    }

    private void trimRecentSessionIndex() {
        int limit = resolveRecentSessionLimit();
        Long currentSize = stringRedisTemplate.opsForZSet().zCard(RECENT_SESSIONS_KEY);
        if (currentSize == null || currentSize <= limit) {
            return;
        }
        long removeEndRank = currentSize - limit - 1L;
        if (removeEndRank >= 0L) {
            stringRedisTemplate.opsForZSet().removeRange(RECENT_SESSIONS_KEY, 0, removeEndRank);
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

    private int resolveRecentSessionLimit() {
        Integer limit = messageFlowProperties.getRecentSessionLimit();
        return limit == null || limit < 1 ? 500 : limit;
    }

    private double resolveRecentSessionScore(MessageFlowSession session) {
        if (session != null && session.getSubmittedAt() != null) {
            return session.getSubmittedAt()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        }
        return System.currentTimeMillis();
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

    private IllegalStateException storageError(String operation, String key, Exception ex) {
        if (messageFlowLogger.isInfoEnabled()) {
            java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
            details.put("operation", operation);
            details.put("redisKey", key);
            details.put("errorClass", ex.getClass().getSimpleName());
            details.put("errorMessage", ex.getMessage());
            messageFlowLogger.info(ObservabilityEventLogSupport.summary(
                    "message_flow_storage_error",
                    "failure",
                    0L,
                    details
            ));
        }
        return new IllegalStateException("message-flow Redis 操作失败: " + operation + ", key=" + key, ex);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
