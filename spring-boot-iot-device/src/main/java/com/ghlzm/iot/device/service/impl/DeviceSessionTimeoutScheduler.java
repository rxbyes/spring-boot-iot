package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.service.DeviceSessionService;
import com.ghlzm.iot.framework.config.IotProperties;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 设备会话超时巡检，统一把长时间无上报的设备收口为离线。
 */
@Component
public class DeviceSessionTimeoutScheduler {

    private final DeviceMapper deviceMapper;
    private final DeviceSessionService deviceSessionService;
    private final IotProperties iotProperties;

    public DeviceSessionTimeoutScheduler(DeviceMapper deviceMapper,
                                         DeviceSessionService deviceSessionService,
                                         IotProperties iotProperties) {
        this.deviceMapper = deviceMapper;
        this.deviceSessionService = deviceSessionService;
        this.iotProperties = iotProperties;
    }

    @Scheduled(
            fixedDelayString = "${iot.device.online-timeout-check-delay-millis:30000}",
            initialDelayString = "${iot.device.online-timeout-check-delay-millis:30000}"
    )
    public void closeTimedOutSessions() {
        int timeoutSeconds = resolveOnlineTimeoutSeconds();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusSeconds(timeoutSeconds);
        List<Device> timedOutDevices = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeleted, 0)
                        .eq(Device::getOnlineStatus, 1)
                        .isNotNull(Device::getLastReportTime)
                        .lt(Device::getLastReportTime, threshold)
        );
        for (Device device : timedOutDevices) {
            LocalDateTime offlineTime = resolveOfflineTime(device, timeoutSeconds, now);
            deviceSessionService.offline(device.getDeviceCode(), offlineTime);
        }
    }

    private int resolveOnlineTimeoutSeconds() {
        if (iotProperties.getDevice() == null || iotProperties.getDevice().getOnlineTimeoutSeconds() == null) {
            return 120;
        }
        return iotProperties.getDevice().getOnlineTimeoutSeconds();
    }

    private LocalDateTime resolveOfflineTime(Device device, int timeoutSeconds, LocalDateTime now) {
        if (device == null || device.getLastReportTime() == null) {
            return now;
        }
        LocalDateTime inferredOfflineTime = device.getLastReportTime().plusSeconds(timeoutSeconds);
        return inferredOfflineTime.isAfter(now) ? now : inferredOfflineTime;
    }
}
