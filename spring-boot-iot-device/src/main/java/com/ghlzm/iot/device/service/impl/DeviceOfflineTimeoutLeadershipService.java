package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 设备离线超时调度集群单活协调服务。
 */
@Service
public class DeviceOfflineTimeoutLeadershipService {

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

    public DeviceOfflineTimeoutLeadershipService(IotProperties iotProperties,
                                                 ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider) {
        this.iotProperties = iotProperties;
        this.stringRedisTemplateProvider = stringRedisTemplateProvider;
    }

    public boolean isEnabled() {
        return iotProperties.getDevice() != null
                && Boolean.TRUE.equals(iotProperties.getDevice().getOfflineTimeoutClusterSingletonEnabled());
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

    public Duration resolveLockTtl() {
        Integer ttlSeconds = iotProperties.getDevice() == null ? null : iotProperties.getDevice().getOfflineTimeoutLockTtlSeconds();
        long safeSeconds = ttlSeconds == null || ttlSeconds < 5 ? 90L : ttlSeconds.longValue();
        return Duration.ofSeconds(safeSeconds);
    }

    String resolveLockKey() {
        String configured = iotProperties.getDevice() == null ? null : iotProperties.getDevice().getOfflineTimeoutLockKey();
        return configured == null || configured.isBlank()
                ? "iot:device:offline-timeout:leader"
                : configured.trim();
    }

    private String normalizeOwnerId(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("设备离线超时调度 ownerId 不能为空");
        }
        return ownerId.trim();
    }

    private StringRedisTemplate redisTemplate() {
        StringRedisTemplate template = stringRedisTemplateProvider.getIfAvailable();
        if (template == null) {
            throw new IllegalStateException("设备离线超时调度集群单活依赖 Redis，但 StringRedisTemplate 不可用");
        }
        return template;
    }
}
