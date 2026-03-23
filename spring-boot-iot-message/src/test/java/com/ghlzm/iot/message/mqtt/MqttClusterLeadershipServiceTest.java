package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttClusterLeadershipServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private IotProperties iotProperties;
    private MqttClusterLeadershipService mqttClusterLeadershipService;

    @BeforeEach
    void setUp() {
        iotProperties = new IotProperties();
        iotProperties.getMqtt().setClusterSingletonEnabled(true);
        iotProperties.getMqtt().setClusterLockKey("iot:test:mqtt:leader");
        iotProperties.getMqtt().setClusterLockTtlSeconds(30);
        iotProperties.getMqtt().setClusterLockRenewIntervalSeconds(10);
        iotProperties.getMqtt().setClusterLockAcquireIntervalSeconds(5);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("stringRedisTemplate", stringRedisTemplate);
        mqttClusterLeadershipService = new MqttClusterLeadershipService(
                iotProperties,
                beanFactory.getBeanProvider(StringRedisTemplate.class)
        );
    }

    @Test
    void tryAcquireLeadershipShouldUseConfiguredTtl() {
        when(valueOperations.setIfAbsent("iot:test:mqtt:leader", "node-a", Duration.ofSeconds(30))).thenReturn(true);

        boolean acquired = mqttClusterLeadershipService.tryAcquireLeadership("node-a");

        assertTrue(acquired);
        verify(valueOperations).setIfAbsent("iot:test:mqtt:leader", "node-a", Duration.ofSeconds(30));
    }

    @Test
    void renewLeadershipShouldUseCompareAndExpireScript() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("iot:test:mqtt:leader")), eq("node-a"), eq("30")))
                .thenReturn(1L);

        boolean renewed = mqttClusterLeadershipService.renewLeadership("node-a");

        assertTrue(renewed);
        verify(stringRedisTemplate).execute(any(DefaultRedisScript.class), eq(List.of("iot:test:mqtt:leader")), eq("node-a"), eq("30"));
    }

    @Test
    void releaseLeadershipShouldUseCompareAndDeleteScript() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("iot:test:mqtt:leader")), eq("node-a")))
                .thenReturn(1L);

        mqttClusterLeadershipService.releaseLeadership("node-a");

        verify(stringRedisTemplate).execute(any(DefaultRedisScript.class), eq(List.of("iot:test:mqtt:leader")), eq("node-a"));
    }

    @Test
    void currentLeaderShouldTrimRedisValue() {
        when(valueOperations.get("iot:test:mqtt:leader")).thenReturn(" node-a ");

        assertTrue(mqttClusterLeadershipService.getCurrentLeaderOwnerId().isPresent());
        assertEquals("node-a", mqttClusterLeadershipService.getCurrentLeaderOwnerId().orElseThrow());
    }

    @Test
    void resolveIntervalsShouldFallbackToSafeDefaults() {
        iotProperties.getMqtt().setClusterLockTtlSeconds(3);
        iotProperties.getMqtt().setClusterLockRenewIntervalSeconds(99);
        iotProperties.getMqtt().setClusterLockAcquireIntervalSeconds(0);

        assertEquals(Duration.ofSeconds(29), mqttClusterLeadershipService.resolveRenewInterval());
        assertEquals(Duration.ofSeconds(5), mqttClusterLeadershipService.resolveAcquireInterval());
    }

    @Test
    void disabledModeShouldShortCircuit() {
        iotProperties.getMqtt().setClusterSingletonEnabled(false);

        assertFalse(mqttClusterLeadershipService.getCurrentLeaderOwnerId().isPresent());
        assertTrue(mqttClusterLeadershipService.tryAcquireLeadership("node-a"));
        assertTrue(mqttClusterLeadershipService.renewLeadership("node-a"));
    }
}
