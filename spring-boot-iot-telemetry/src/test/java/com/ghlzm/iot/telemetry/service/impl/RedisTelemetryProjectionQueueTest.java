package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryProjectionTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTelemetryProjectionQueueTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ListOperations<String, String> listOperations;

    private RedisTelemetryProjectionQueue queue;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        queue = new RedisTelemetryProjectionQueue(stringRedisTemplate);
    }

    @Test
    void shouldPublishProjectionTaskIntoRedisList() {
        TelemetryProjectionTask task = new TelemetryProjectionTask();
        task.setProjectionType(TelemetryProjectionTask.ProjectionType.LATEST);
        task.setDeviceId(2001L);

        queue.publish(task);

        verify(listOperations).rightPush(anyString(), anyString());
        verify(stringRedisTemplate).expire(anyString(), any());
    }
}
