package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.service.DeviceSessionService;
import com.ghlzm.iot.framework.config.IotProperties;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceSessionTimeoutSchedulerTest {

    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private DeviceSessionService deviceSessionService;
    @Mock
    private DeviceOfflineTimeoutLeadershipService leadershipService;

    private DeviceSessionTimeoutScheduler deviceSessionTimeoutScheduler;

    @BeforeEach
    void setUp() {
        IotProperties properties = new IotProperties();
        IotProperties.Device deviceConfig = new IotProperties.Device();
        deviceConfig.setOnlineTimeoutSeconds(120);
        properties.setDevice(deviceConfig);
        lenient().when(leadershipService.tryAcquireLeadership(anyString())).thenReturn(true);
        deviceSessionTimeoutScheduler = new DeviceSessionTimeoutScheduler(deviceMapper, deviceSessionService, leadershipService, properties);
    }

    @Test
    void closeTimedOutSessionsShouldMarkExpiredDevicesOffline() {
        Device device = new Device();
        device.setDeviceCode("demo-device-01");
        device.setLastReportTime(LocalDateTime.now().minusMinutes(5));
        when(deviceMapper.selectList(any())).thenReturn(List.of(device));

        deviceSessionTimeoutScheduler.closeTimedOutSessions();

        verify(deviceSessionService).offline(eq("demo-device-01"), any(LocalDateTime.class));
    }

    @Test
    void closeTimedOutSessionsShouldSkipWhenNoExpiredDevices() {
        when(deviceMapper.selectList(any())).thenReturn(List.of());

        deviceSessionTimeoutScheduler.closeTimedOutSessions();

        verify(deviceSessionService, never()).offline(any(String.class), any(LocalDateTime.class));
    }

    @Test
    void closeTimedOutSessionsShouldPreferLastOnlineTimeOverOlderReportTime() {
        Device device = new Device();
        device.setDeviceCode("demo-device-02");
        device.setLastReportTime(LocalDateTime.now().minusMinutes(5));
        device.setLastOnlineTime(LocalDateTime.now().minusSeconds(30));
        when(deviceMapper.selectList(any())).thenReturn(List.of(device));

        deviceSessionTimeoutScheduler.closeTimedOutSessions();

        verify(deviceSessionService, never()).offline(eq("demo-device-02"), any(LocalDateTime.class));
    }
}
