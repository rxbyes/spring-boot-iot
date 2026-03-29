package com.ghlzm.iot.framework.observability.invalidreport;

import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 基于 Redis 的无效上报计数与冷却存储。
 */
@Component
public class RedisInvalidReportCounterStore implements InvalidReportCounterStore {

    private static final String FAILURE_STAGE_PREFIX = "iot:invalid-report:bucket:";
    private static final String REASON_PREFIX = "iot:invalid-report:reason:";
    private static final String COOLDOWN_PREFIX = "iot:invalid-report:cooldown:";
    private static final DateTimeFormatter BUCKET_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final StringRedisTemplate stringRedisTemplate;
    private final IotProperties iotProperties;
    private final Clock clock;

    @Autowired
    public RedisInvalidReportCounterStore(StringRedisTemplate stringRedisTemplate,
                                          IotProperties iotProperties) {
        this(stringRedisTemplate, iotProperties, Clock.systemDefaultZone());
    }

    RedisInvalidReportCounterStore(StringRedisTemplate stringRedisTemplate,
                                   IotProperties iotProperties,
                                   Clock clock) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.iotProperties = iotProperties;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    public long incrementFailureStage(String failureStage) {
        if (!StringUtils.hasText(failureStage)) {
            return 0L;
        }
        String key = buildMinuteBucketKey(FAILURE_STAGE_PREFIX, "failure-stage", normalizeKey(failureStage));
        Long value = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, resolveBucketTtl());
        return value == null ? 0L : value;
    }

    @Override
    public long incrementReasonCode(String reasonCode) {
        if (!StringUtils.hasText(reasonCode)) {
            return 0L;
        }
        String key = buildMinuteReasonKey(normalizeKey(reasonCode));
        Long value = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, resolveBucketTtl());
        return value == null ? 0L : value;
    }

    @Override
    public long sumFailureStageSince(String failureStage, Instant startInclusive) {
        if (!StringUtils.hasText(failureStage) || startInclusive == null) {
            return 0L;
        }
        LocalDateTime currentBucket = resolveBucketStart(startInclusive);
        LocalDateTime endBucket = resolveBucketStart(clock.instant());
        if (currentBucket.isAfter(endBucket)) {
            return 0L;
        }
        long sum = 0L;
        String normalizedStage = normalizeKey(failureStage);
        while (!currentBucket.isAfter(endBucket)) {
            String key = buildMinuteBucketKey(FAILURE_STAGE_PREFIX, currentBucket, "failure-stage", normalizedStage);
            sum += parseLong(stringRedisTemplate.opsForValue().get(key));
            currentBucket = currentBucket.plusMinutes(1L);
        }
        return sum;
    }

    @Override
    public boolean tryOpenCooldown(String governanceKey, Duration ttl) {
        if (!StringUtils.hasText(governanceKey) || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(
                COOLDOWN_PREFIX + governanceKey.trim(),
                "1",
                ttl
        ));
    }

    @Override
    public void clearCooldown(String governanceKey) {
        if (!StringUtils.hasText(governanceKey)) {
            return;
        }
        stringRedisTemplate.delete(COOLDOWN_PREFIX + governanceKey.trim());
    }

    private Duration resolveBucketTtl() {
        Integer ttlHours = iotProperties.getObservability().getInvalidReportGovernance().getBucketTtlHours();
        return Duration.ofHours(ttlHours == null || ttlHours < 1 ? 26L : ttlHours.longValue());
    }

    private LocalDateTime resolveBucketStart(Instant instant) {
        ZoneId zoneId = clock.getZone();
        return LocalDateTime.ofInstant(instant, zoneId)
                .withSecond(0)
                .withNano(0);
    }

    private String buildMinuteBucketKey(String prefix, String namespace, String code) {
        return buildMinuteBucketKey(prefix, resolveBucketStart(clock.instant()), namespace, code);
    }

    private String buildMinuteBucketKey(String prefix, LocalDateTime bucketTime, String namespace, String code) {
        return prefix + BUCKET_FORMATTER.format(bucketTime) + ":" + namespace + ":" + code;
    }

    private String buildMinuteReasonKey(String reasonCode) {
        return REASON_PREFIX + BUCKET_FORMATTER.format(resolveBucketStart(clock.instant())) + ":" + reasonCode;
    }

    private String normalizeKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
