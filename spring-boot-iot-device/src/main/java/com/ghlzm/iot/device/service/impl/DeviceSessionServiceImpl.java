package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.service.DeviceSessionService;
import com.ghlzm.iot.framework.config.IotProperties;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 设备会话服务最小实现。
 * 当前使用 Redis 保存最小会话快照，并同步维护设备在线状态字段。
 */
@Service
public class DeviceSessionServiceImpl implements DeviceSessionService {

    private static final String SESSION_KEY_PREFIX = "iot:device:session:";

    private final StringRedisTemplate stringRedisTemplate;
    private final DeviceMapper deviceMapper;
    private final IotProperties iotProperties;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public DeviceSessionServiceImpl(StringRedisTemplate stringRedisTemplate,
                                    DeviceMapper deviceMapper,
                                    IotProperties iotProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.deviceMapper = deviceMapper;
        this.iotProperties = iotProperties;
    }

    @Override
    public void online(String deviceCode, String clientId) {
        if (!hasText(deviceCode)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        DeviceSessionRecord sessionRecord = getSessionRecord(deviceCode);
        if (sessionRecord == null) {
            sessionRecord = new DeviceSessionRecord();
            sessionRecord.setDeviceCode(deviceCode);
            sessionRecord.setConnectTime(now);
        }
        sessionRecord.setClientId(clientId);
        sessionRecord.setConnected(Boolean.TRUE);
        if (sessionRecord.getConnectTime() == null) {
            sessionRecord.setConnectTime(now);
        }
        sessionRecord.setLastSeenTime(now);
        saveSessionRecord(sessionRecord);

        updateDeviceOnline(deviceCode, now);
    }

    @Override
    public void offline(String deviceCode) {
        if (!hasText(deviceCode)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        DeviceSessionRecord sessionRecord = getSessionRecord(deviceCode);
        if (sessionRecord == null) {
            sessionRecord = new DeviceSessionRecord();
            sessionRecord.setDeviceCode(deviceCode);
        }
        sessionRecord.setConnected(Boolean.FALSE);
        sessionRecord.setLastSeenTime(now);
        saveSessionRecord(sessionRecord);

        updateDeviceOffline(deviceCode, now);
    }

    @Override
    public void refreshLastSeen(String deviceCode, String clientId, String topic) {
        if (!hasText(deviceCode)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        DeviceSessionRecord sessionRecord = getSessionRecord(deviceCode);
        if (sessionRecord == null) {
            sessionRecord = new DeviceSessionRecord();
            sessionRecord.setDeviceCode(deviceCode);
            sessionRecord.setConnectTime(now);
            sessionRecord.setConnected(Boolean.TRUE);
        }
        if (hasText(clientId)) {
            sessionRecord.setClientId(clientId);
        }
        sessionRecord.setTopic(topic);
        sessionRecord.setConnected(Boolean.TRUE);
        sessionRecord.setLastSeenTime(now);
        if (sessionRecord.getConnectTime() == null) {
            sessionRecord.setConnectTime(now);
        }
        saveSessionRecord(sessionRecord);
    }

    @Override
    public boolean isOnline(String deviceCode) {
        DeviceSessionRecord sessionRecord = getSessionRecord(deviceCode);
        return sessionRecord != null && Boolean.TRUE.equals(sessionRecord.getConnected());
    }

    @Override
    public LocalDateTime getLastSeenTime(String deviceCode) {
        DeviceSessionRecord sessionRecord = getSessionRecord(deviceCode);
        return sessionRecord == null ? null : sessionRecord.getLastSeenTime();
    }

    @Override
    public String buildSessionKey(String deviceCode) {
        return SESSION_KEY_PREFIX + deviceCode;
    }

    @Override
    public Duration getSessionTtl() {
        int onlineTimeoutSeconds = iotProperties.getDevice() == null
                || iotProperties.getDevice().getOnlineTimeoutSeconds() == null
                ? 120
                : iotProperties.getDevice().getOnlineTimeoutSeconds();
        return Duration.ofSeconds(onlineTimeoutSeconds * 2L);
    }

    private DeviceSessionRecord getSessionRecord(String deviceCode) {
        if (!hasText(deviceCode)) {
            return null;
        }
        String sessionJson;
        try {
            sessionJson = stringRedisTemplate.opsForValue().get(buildSessionKey(deviceCode));
        } catch (Exception ex) {
            return null;
        }
        if (!hasText(sessionJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(sessionJson, DeviceSessionRecord.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private void saveSessionRecord(DeviceSessionRecord sessionRecord) {
        try {
            stringRedisTemplate.opsForValue().set(
                    buildSessionKey(sessionRecord.getDeviceCode()),
                    objectMapper.writeValueAsString(sessionRecord),
                    getSessionTtl()
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("设备会话序列化失败", ex);
        } catch (Exception ex) {
            // 当前阶段 Redis 不可用时不阻断消息主链路，仍以数据库在线状态作为兜底结果。
        }
    }

    private void updateDeviceOnline(String deviceCode, LocalDateTime now) {
        Device device = findDevice(deviceCode);
        if (device == null) {
            return;
        }

        Device update = new Device();
        update.setId(device.getId());
        update.setOnlineStatus(1);
        update.setLastOnlineTime(now);
        deviceMapper.updateById(update);
    }

    private void updateDeviceOffline(String deviceCode, LocalDateTime now) {
        Device device = findDevice(deviceCode);
        if (device == null) {
            return;
        }

        Device update = new Device();
        update.setId(device.getId());
        update.setOnlineStatus(0);
        update.setLastOfflineTime(now);
        deviceMapper.updateById(update);
    }

    private Device findDevice(String deviceCode) {
        return deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceCode, deviceCode)
                        .eq(Device::getDeleted, 0)
                        .last("limit 1")
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Data
    public static class DeviceSessionRecord {
        private String deviceCode;
        private String clientId;
        private String topic;
        private Boolean connected;
        private LocalDateTime connectTime;
        private LocalDateTime lastSeenTime;
    }
}
