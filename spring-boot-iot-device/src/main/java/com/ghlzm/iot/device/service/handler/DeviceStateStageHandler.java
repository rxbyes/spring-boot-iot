package com.ghlzm.iot.device.service.handler;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.service.DeviceOnlineSessionService;
import com.ghlzm.iot.device.service.DeviceSessionService;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 在线状态与会话刷新 stage。
 */
@Component
public class DeviceStateStageHandler {

    private final DeviceMapper deviceMapper;
    private final DeviceOnlineSessionService deviceOnlineSessionService;
    private final DeviceSessionService deviceSessionService;
    private final IotProperties iotProperties;

    public DeviceStateStageHandler(DeviceMapper deviceMapper,
                                   DeviceOnlineSessionService deviceOnlineSessionService,
                                   DeviceSessionService deviceSessionService,
                                   IotProperties iotProperties) {
        this.deviceMapper = deviceMapper;
        this.deviceOnlineSessionService = deviceOnlineSessionService;
        this.deviceSessionService = deviceSessionService;
        this.iotProperties = iotProperties;
    }

    public void refresh(DeviceProcessingTarget target) {
        LocalDateTime reportTime = target.getMessage().getTimestamp() == null ? LocalDateTime.now() : target.getMessage().getTimestamp();
        deviceOnlineSessionService.recordOnlineHeartbeat(target.getDevice(), reportTime);

        Device update = new Device();
        update.setId(target.getDevice().getId());
        update.setOnlineStatus(1);
        update.setLastOnlineTime(reportTime);
        update.setLastReportTime(reportTime);
        boolean activateDefault = iotProperties.getDevice() != null
                && Boolean.TRUE.equals(iotProperties.getDevice().getActivateDefault());
        if (activateDefault) {
            update.setActivateStatus(1);
        }
        deviceMapper.updateById(update);

        String clientId = hasText(target.getMessage().getDeviceCode())
                ? target.getMessage().getDeviceCode()
                : target.getDevice().getDeviceCode();
        deviceSessionService.online(target.getDevice().getDeviceCode(), clientId, reportTime);
        deviceSessionService.refreshLastSeen(target.getDevice().getDeviceCode(), clientId, target.getMessage().getTopic(), reportTime);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
