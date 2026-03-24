package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * MQTT consumer 集群单活协调服务。
 */
@Service
public class MqttClusterLeadershipService {

    private static final DefaultRedisScript<Long> COMPARE_AND_EXPIRE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('expire', KEYS[1], tonumber(ARGV[2]))
            end
            return 0
            """,
            Long.class
    );

    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """,
            Long.class
    );

    private final IotProperties iotProperties;
    private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;

    public MqttClusterLeadershipService(IotProperties iotProperties,
                                        ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider) {
        this.iotProperties = iotProperties;
        this.stringRedisTemplateProvider = stringRedisTemplateProvider;
    }

    public boolean isEnabled() {
        return iotProperties.getMqtt() != null
                && Boolean.TRUE.equals(iotProperties.getMqtt().getClusterSingletonEnabled());
    }

    public boolean tryAcquireLeadership(String ownerId) {
        if (!isEnabled()) {
            return true;
        }
        Boolean acquired = redisTemplate().opsForValue().setIfAbsent(
                resolveLockKey(),
                normalizeOwnerId(ownerId),
                resolveLockTtl()
        );
        return Boolean.TRUE.equals(acquired);
    }

    public boolean renewLeadership(String ownerId) {
        if (!isEnabled()) {
            return true;
        }
        Long renewed = redisTemplate().execute(
                COMPARE_AND_EXPIRE_SCRIPT,
                List.of(resolveLockKey()),
                normalizeOwnerId(ownerId),
                String.valueOf(resolveLockTtlSeconds())
        );
        return renewed != null && renewed > 0L;
    }

    public void releaseLeadership(String ownerId) {
        if (!isEnabled()) {
            return;
        }
        redisTemplate().execute(
                COMPARE_AND_DELETE_SCRIPT,
                List.of(resolveLockKey()),
                normalizeOwnerId(ownerId)
        );
    }

    public Optional<String> getCurrentLeaderOwnerId() {
        if (!isEnabled()) {
            return Optional.empty();
        }
        String value = redisTemplate().opsForValue().get(resolveLockKey());
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    public Duration resolveRenewInterval() {
        return Duration.ofSeconds(resolveRenewIntervalSeconds());
    }

    public Duration resolveAcquireInterval() {
        return Duration.ofSeconds(resolveAcquireIntervalSeconds());
    }

    public String resolveLockKey() {
        String configured = iotProperties.getMqtt() == null ? null : iotProperties.getMqtt().getClusterLockKey();
        return configured == null || configured.isBlank()
                ? "iot:mqtt:consumer:leader"
                : configured.trim();
    }

    private Duration resolveLockTtl() {
        return Duration.ofSeconds(resolveLockTtlSeconds());
    }

    private long resolveLockTtlSeconds() {
        Integer ttlSeconds = iotProperties.getMqtt() == null ? null : iotProperties.getMqtt().getClusterLockTtlSeconds();
        return ttlSeconds == null || ttlSeconds < 5 ? 30L : ttlSeconds.longValue();
    }

    private long resolveRenewIntervalSeconds() {
        Integer renewSeconds = iotProperties.getMqtt() == null ? null : iotProperties.getMqtt().getClusterLockRenewIntervalSeconds();
        long ttlSeconds = resolveLockTtlSeconds();
        long safeMax = Math.max(1L, ttlSeconds - 1L);
        long safeDefault = Math.min(10L, safeMax);
        if (renewSeconds == null || renewSeconds < 1) {
            return safeDefault;
        }
        return Math.min(renewSeconds.longValue(), safeMax);
    }

    private long resolveAcquireIntervalSeconds() {
        Integer acquireSeconds = iotProperties.getMqtt() == null ? null : iotProperties.getMqtt().getClusterLockAcquireIntervalSeconds();
        if (acquireSeconds == null || acquireSeconds < 1) {
            return 5L;
        }
        return acquireSeconds.longValue();
    }

    private String normalizeOwnerId(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("MQTT 集群 ownerId 不能为空");
        }
        return ownerId.trim();
    }

    private StringRedisTemplate redisTemplate() {
        StringRedisTemplate template = stringRedisTemplateProvider.getIfAvailable();
        if (template == null) {
            throw new IllegalStateException("MQTT 集群单活依赖 Redis，但 StringRedisTemplate 不可用");
        }
        return template;
    }
}
