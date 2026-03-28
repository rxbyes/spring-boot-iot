package com.ghlzm.iot.framework.observability.invalidreport;

import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisInvalidReportCounterStoreTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisInvalidReportCounterStore store;

    @BeforeEach
    void setUp() {
        IotProperties properties = new IotProperties();
        properties.getObservability().getInvalidReportGovernance().setEnabled(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        store = new RedisInvalidReportCounterStore(
                stringRedisTemplate,
                properties,
                Clock.fixed(Instant.parse("2026-03-27T13:45:00Z"), ZoneId.of("Asia/Shanghai"))
        );
    }

    @Test
    void shouldIncrementFailureStageBucketAndReadWindowSummary() {
        when(valueOperations.increment(anyString())).thenReturn(3L);
        when(valueOperations.get("iot:invalid-report:bucket:202603272144:failure-stage:device_validate"))
                .thenReturn("0");
        when(valueOperations.get("iot:invalid-report:bucket:202603272145:failure-stage:device_validate"))
                .thenReturn("3");

        long hitCount = store.incrementFailureStage("device_validate");
        long recentCount = store.sumFailureStageSince("device_validate", Instant.parse("2026-03-27T13:44:00Z"));

        assertEquals(3L, hitCount);
        assertEquals(3L, recentCount);
    }

    @Test
    void shouldUseCooldownKeyForSuppression() {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true, false);

        boolean firstAcquired = store.tryOpenCooldown("tenant=1|device=missing-01", Duration.ofMinutes(30));
        boolean secondAcquired = store.tryOpenCooldown("tenant=1|device=missing-01", Duration.ofMinutes(30));

        assertTrue(firstAcquired);
        assertFalse(secondAcquired);
    }
}
