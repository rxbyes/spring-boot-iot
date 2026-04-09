package com.ghlzm.iot.device.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.service.DeviceOnlineSessionService;
import com.ghlzm.iot.framework.config.IotProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceSessionServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private DeviceOnlineSessionService deviceOnlineSessionService;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private DeviceSessionServiceImpl deviceSessionService;

    @BeforeEach
    void setUp() {
        IotProperties properties = new IotProperties();
        IotProperties.Device deviceConfig = new IotProperties.Device();
        deviceConfig.setOnlineTimeoutSeconds(7200);
        properties.setDevice(deviceConfig);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        deviceSessionService = new DeviceSessionServiceImpl(
                stringRedisTemplate,
                deviceMapper,
                deviceOnlineSessionService,
                properties
        );
    }

    @Test
    void onlineShouldUseProvidedSeenTimeWhenUpdatingDevice() {
        Device device = buildDevice();
        LocalDateTime reportTime = LocalDateTime.of(2026, 3, 28, 10, 48, 4);
        when(deviceMapper.selectOne(any())).thenReturn(device);

        deviceSessionService.online(device.getDeviceCode(), "client-1", reportTime);

        ArgumentCaptor<Device> updateCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceMapper).updateById(updateCaptor.capture());
        assertEquals(1, updateCaptor.getValue().getOnlineStatus());
        assertEquals(reportTime, updateCaptor.getValue().getLastOnlineTime());
    }

    @Test
    void refreshLastSeenShouldPersistProvidedSeenTimeToSessionRecord() throws Exception {
        DeviceSessionServiceImpl.DeviceSessionRecord existingRecord = new DeviceSessionServiceImpl.DeviceSessionRecord();
        existingRecord.setDeviceCode("demo-device-01");
        existingRecord.setConnected(Boolean.TRUE);
        existingRecord.setConnectTime(LocalDateTime.of(2026, 3, 28, 9, 0));
        existingRecord.setLastSeenTime(LocalDateTime.of(2026, 3, 28, 9, 30));
        when(valueOperations.get("iot:device:session:demo-device-01"))
                .thenReturn(objectMapper.writeValueAsString(existingRecord));

        LocalDateTime reportTime = LocalDateTime.of(2026, 3, 28, 10, 48, 4);
        deviceSessionService.refreshLastSeen("demo-device-01", "client-1", "$dp", reportTime);

        ArgumentCaptor<String> sessionJsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq("iot:device:session:demo-device-01"),
                sessionJsonCaptor.capture(),
                eq(Duration.ofSeconds(14400))
        );
        DeviceSessionServiceImpl.DeviceSessionRecord savedRecord = objectMapper.readValue(
                sessionJsonCaptor.getValue(),
                DeviceSessionServiceImpl.DeviceSessionRecord.class
        );
        assertEquals(reportTime, savedRecord.getLastSeenTime());
        assertEquals("$dp", savedRecord.getTopic());
        assertEquals("client-1", savedRecord.getClientId());
    }

    private Device buildDevice() {
        Device device = new Device();
        device.setId(2001L);
        device.setTenantId(1L);
        device.setDeviceCode("demo-device-01");
        return device;
    }
}
