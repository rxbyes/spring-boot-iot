package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.service.DeviceSessionService;
import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceSessionTimeoutSchedulerLeadershipTest {

    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private DeviceSessionService deviceSessionService;
    @Mock
    private DeviceOfflineTimeoutLeadershipService leadershipService;

    private DeviceSessionTimeoutScheduler scheduler;

    @BeforeEach
    void setUp() {
        IotProperties properties = new IotProperties();
        IotProperties.Device deviceConfig = new IotProperties.Device();
        deviceConfig.setOnlineTimeoutSeconds(7200);
        properties.setDevice(deviceConfig);
        lenient().when(leadershipService.tryAcquireLeadership(anyString())).thenReturn(true);
        scheduler = new DeviceSessionTimeoutScheduler(deviceMapper, deviceSessionService, leadershipService, properties);
    }

    @Test
    void closeTimedOutSessionsShouldSkipWhenLeadershipNotAcquired() {
        when(leadershipService.tryAcquireLeadership(anyString())).thenReturn(false);

        scheduler.closeTimedOutSessions();

        verify(deviceMapper, never()).selectList(any());
        verify(deviceSessionService, never()).offline(anyString(), any(LocalDateTime.class));
        verify(leadershipService, never()).releaseLeadership(anyString());
    }
}
