package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceOnlineSession;
import com.ghlzm.iot.device.mapper.DeviceOnlineSessionMapper;
import com.ghlzm.iot.device.vo.ProductActivityStatRow;
import java.sql.SQLSyntaxErrorException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceOnlineSessionServiceImplTest {

    @Mock
    private DeviceOnlineSessionMapper deviceOnlineSessionMapper;

    private DeviceOnlineSessionServiceImpl deviceOnlineSessionService;

    @BeforeEach
    void setUp() {
        deviceOnlineSessionService = new DeviceOnlineSessionServiceImpl(deviceOnlineSessionMapper);
    }

    @Test
    void recordOnlineHeartbeatShouldInsertSessionWhenNoActiveSessionExists() {
        Device device = buildDevice();
        LocalDateTime reportTime = LocalDateTime.of(2026, 3, 21, 19, 0);
        when(deviceOnlineSessionMapper.selectOne(any())).thenReturn(null);

        deviceOnlineSessionService.recordOnlineHeartbeat(device, reportTime);

        ArgumentCaptor<DeviceOnlineSession> captor = ArgumentCaptor.forClass(DeviceOnlineSession.class);
        verify(deviceOnlineSessionMapper).insert(captor.capture());
        assertEquals(device.getProductId(), captor.getValue().getProductId());
        assertEquals(device.getId(), captor.getValue().getDeviceId());
        assertEquals(reportTime, captor.getValue().getOnlineTime());
        assertEquals(reportTime, captor.getValue().getLastSeenTime());
        verify(deviceOnlineSessionMapper, never()).updateById(any(DeviceOnlineSession.class));
    }

    @Test
    void closeActiveSessionShouldUpdateOfflineTimeAndDuration() {
        Device device = buildDevice();
        DeviceOnlineSession activeSession = new DeviceOnlineSession();
        activeSession.setId(3001L);
        activeSession.setDeviceId(device.getId());
        activeSession.setOnlineTime(LocalDateTime.of(2026, 3, 21, 18, 0));
        activeSession.setLastSeenTime(LocalDateTime.of(2026, 3, 21, 18, 30));
        when(deviceOnlineSessionMapper.selectOne(any())).thenReturn(activeSession);

        LocalDateTime offlineTime = LocalDateTime.of(2026, 3, 21, 19, 15);
        deviceOnlineSessionService.closeActiveSession(device, offlineTime, "timeout");

        ArgumentCaptor<DeviceOnlineSession> captor = ArgumentCaptor.forClass(DeviceOnlineSession.class);
        verify(deviceOnlineSessionMapper).updateById(captor.capture());
        assertEquals(offlineTime, captor.getValue().getOfflineTime());
        assertEquals(75L, captor.getValue().getDurationMinutes());
        assertEquals("timeout", captor.getValue().getRemark());
    }

    @Test
    void loadProductDurationStatShouldDelegateToMapper() {
        ProductActivityStatRow statRow = new ProductActivityStatRow();
        statRow.setAvgOnlineDuration(30L);
        statRow.setMaxOnlineDuration(90L);
        LocalDateTime thirtyDaysStart = LocalDateTime.of(2026, 2, 20, 0, 0);
        LocalDateTime statTime = LocalDateTime.of(2026, 3, 21, 19, 0);
        when(deviceOnlineSessionMapper.selectProductDurationStat(1001L, thirtyDaysStart, statTime)).thenReturn(statRow);

        ProductActivityStatRow result = deviceOnlineSessionService.loadProductDurationStat(1001L, thirtyDaysStart, statTime);

        assertEquals(30L, result.getAvgOnlineDuration());
        assertEquals(90L, result.getMaxOnlineDuration());
        verify(deviceOnlineSessionMapper).selectProductDurationStat(1001L, thirtyDaysStart, statTime);
    }

    @Test
    void recordOnlineHeartbeatShouldDegradeWhenSessionTableMissing() {
        Device device = buildDevice();
        LocalDateTime reportTime = LocalDateTime.of(2026, 3, 21, 19, 0);
        when(deviceOnlineSessionMapper.selectOne(any()))
                .thenThrow(new RuntimeException(new SQLSyntaxErrorException("Table 'rm_iot.iot_device_online_session' doesn't exist")));

        assertDoesNotThrow(() -> deviceOnlineSessionService.recordOnlineHeartbeat(device, reportTime));

        verify(deviceOnlineSessionMapper, never()).insert(any(DeviceOnlineSession.class));
        verify(deviceOnlineSessionMapper, never()).updateById(any(DeviceOnlineSession.class));
    }

    @Test
    void loadProductDurationStatShouldReturnNullWhenSessionTableMissing() {
        LocalDateTime thirtyDaysStart = LocalDateTime.of(2026, 2, 20, 0, 0);
        LocalDateTime statTime = LocalDateTime.of(2026, 3, 21, 19, 0);
        when(deviceOnlineSessionMapper.selectProductDurationStat(1001L, thirtyDaysStart, statTime))
                .thenThrow(new RuntimeException(new SQLSyntaxErrorException("Table 'rm_iot.iot_device_online_session' doesn't exist")));

        ProductActivityStatRow result = deviceOnlineSessionService.loadProductDurationStat(1001L, thirtyDaysStart, statTime);

        assertNull(result);
    }

    @Test
    void recordOnlineHeartbeatShouldRethrowUnexpectedMapperFailure() {
        Device device = buildDevice();
        when(deviceOnlineSessionMapper.selectOne(any())).thenThrow(new IllegalStateException("mapper unavailable"));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> deviceOnlineSessionService.recordOnlineHeartbeat(device, LocalDateTime.now())
        );

        assertEquals("mapper unavailable", ex.getMessage());
    }

    private Device buildDevice() {
        Device device = new Device();
        device.setId(2001L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");
        return device;
    }
}
