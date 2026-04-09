package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceOfflineTimeoutLeadershipServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private IotProperties iotProperties;
    private DeviceOfflineTimeoutLeadershipService leadershipService;

    @BeforeEach
    void setUp() {
        iotProperties = new IotProperties();
        iotProperties.getDevice().setOfflineTimeoutClusterSingletonEnabled(true);
        iotProperties.getDevice().setOfflineTimeoutLockKey("iot:test:device-offline-timeout:leader");
        iotProperties.getDevice().setOfflineTimeoutLockTtlSeconds(90);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("stringRedisTemplate", stringRedisTemplate);
        leadershipService = new DeviceOfflineTimeoutLeadershipService(
                iotProperties,
                beanFactory.getBeanProvider(StringRedisTemplate.class)
        );
    }

    @Test
    void tryAcquireLeadershipShouldUseConfiguredTtl() {
        when(valueOperations.setIfAbsent(
                "iot:test:device-offline-timeout:leader",
                "node-a",
                Duration.ofSeconds(90)
        )).thenReturn(true);

        boolean acquired = leadershipService.tryAcquireLeadership("node-a");

        assertTrue(acquired);
        verify(valueOperations).setIfAbsent(
                "iot:test:device-offline-timeout:leader",
                "node-a",
                Duration.ofSeconds(90)
        );
    }

    @Test
    void releaseLeadershipShouldUseCompareAndDeleteScript() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("iot:test:device-offline-timeout:leader")), eq("node-a")))
                .thenReturn(1L);

        leadershipService.releaseLeadership("node-a");

        verify(stringRedisTemplate).execute(any(DefaultRedisScript.class), eq(List.of("iot:test:device-offline-timeout:leader")), eq("node-a"));
    }

    @Test
    void resolveLockTtlShouldFallbackToSafeDefault() {
        iotProperties.getDevice().setOfflineTimeoutLockTtlSeconds(0);

        assertEquals(Duration.ofSeconds(90), leadershipService.resolveLockTtl());
    }
}
