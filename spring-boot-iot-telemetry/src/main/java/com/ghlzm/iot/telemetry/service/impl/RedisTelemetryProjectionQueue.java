package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryProjectionTask;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

/**
 * 基于 Redis List 的 telemetry 投影队列。
 */
@Component
public class RedisTelemetryProjectionQueue implements TelemetryProjectionQueue {

    static final String QUEUE_KEY = "iot:telemetry:v2:projection:queue";
    private static final Duration QUEUE_TTL = Duration.ofDays(3);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public RedisTelemetryProjectionQueue(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void publish(TelemetryProjectionTask task) {
        try {
            stringRedisTemplate.opsForList().rightPush(QUEUE_KEY, objectMapper.writeValueAsString(task));
            stringRedisTemplate.expire(QUEUE_KEY, QUEUE_TTL);
        } catch (Exception ex) {
            throw new IllegalStateException("发布 telemetry 投影任务失败", ex);
        }
    }
}
