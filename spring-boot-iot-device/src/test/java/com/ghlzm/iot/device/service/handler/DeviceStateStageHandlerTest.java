package com.ghlzm.iot.device.service.handler;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.service.DeviceOnlineSessionService;
import com.ghlzm.iot.device.service.DeviceSessionService;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeviceStateStageHandlerTest {

    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private DeviceOnlineSessionService deviceOnlineSessionService;
    @Mock
    private DeviceSessionService deviceSessionService;

    private DeviceStateStageHandler deviceStateStageHandler;

    @BeforeEach
    void setUp() {
        IotProperties properties = new IotProperties();
        IotProperties.Device deviceConfig = new IotProperties.Device();
        deviceConfig.setActivateDefault(true);
        properties.setDevice(deviceConfig);
        deviceStateStageHandler = new DeviceStateStageHandler(
                deviceMapper,
                deviceOnlineSessionService,
                deviceSessionService,
                properties
        );
    }

    @Test
    void refreshShouldRefreshLastSeenWithoutDuplicatingOnlineUpdate() {
        LocalDateTime reportTime = LocalDateTime.of(2026, 3, 28, 10, 48, 4);
        Device device = new Device();
        device.setId(2001L);
        device.setDeviceCode("demo-device-01");

        DeviceUpMessage message = new DeviceUpMessage();
        message.setDeviceCode("client-1");
        message.setTopic("$dp");
        message.setTimestamp(reportTime);

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setMessage(message);

        deviceStateStageHandler.refresh(target);

        verify(deviceSessionService, never()).online("demo-device-01", "client-1", reportTime);
        verify(deviceSessionService).refreshLastSeen("demo-device-01", "client-1", "$dp", reportTime);

        ArgumentCaptor<Device> updateCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceMapper).updateById(updateCaptor.capture());
        assertEquals(reportTime, updateCaptor.getValue().getLastOnlineTime());
        assertEquals(reportTime, updateCaptor.getValue().getLastReportTime());
    }
}
