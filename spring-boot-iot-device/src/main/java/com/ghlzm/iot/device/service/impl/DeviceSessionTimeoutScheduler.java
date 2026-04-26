package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.service.DeviceSessionService;
import com.ghlzm.iot.framework.config.IotProperties;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 设备会话超时巡检，统一把长时间无上报的设备收口为离线。
 */
@Component
@ConditionalOnProperty(value = "iot.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class DeviceSessionTimeoutScheduler {

    private final DeviceMapper deviceMapper;
    private final DeviceSessionService deviceSessionService;
    private final DeviceOfflineTimeoutLeadershipService leadershipService;
    private final IotProperties iotProperties;
    private final String leadershipOwnerId;

    public DeviceSessionTimeoutScheduler(DeviceMapper deviceMapper,
                                         DeviceSessionService deviceSessionService,
                                         DeviceOfflineTimeoutLeadershipService leadershipService,
                                         IotProperties iotProperties) {
        this.deviceMapper = deviceMapper;
        this.deviceSessionService = deviceSessionService;
        this.leadershipService = leadershipService;
        this.iotProperties = iotProperties;
        this.leadershipOwnerId = "device-offline-timeout@" + ManagementFactory.getRuntimeMXBean().getName() + ":" + UUID.randomUUID();
    }

    @Scheduled(
            fixedDelayString = "${iot.device.online-timeout-check-delay-millis:30000}",
            initialDelayString = "${iot.device.online-timeout-check-delay-millis:30000}"
    )
    public void closeTimedOutSessions() {
        if (!leadershipService.tryAcquireLeadership(leadershipOwnerId)) {
            return;
        }
        try {
            int timeoutSeconds = resolveOnlineTimeoutSeconds();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threshold = now.minusSeconds(timeoutSeconds);
            List<Device> timedOutDevices = deviceMapper.selectList(
                    new LambdaQueryWrapper<Device>()
                            .eq(Device::getDeleted, 0)
                            .eq(Device::getOnlineStatus, 1)
                            .and(wrapper -> wrapper
                                    .lt(Device::getLastOnlineTime, threshold)
                                    .or()
                                    .lt(Device::getLastReportTime, threshold))
            );
            for (Device device : timedOutDevices) {
                LocalDateTime lastSeenTime = resolveLastSeenTime(device);
                if (lastSeenTime == null || !lastSeenTime.isBefore(threshold)) {
                    continue;
                }
                LocalDateTime offlineTime = resolveOfflineTime(lastSeenTime, timeoutSeconds, now);
                deviceSessionService.offline(device.getDeviceCode(), offlineTime);
            }
        } finally {
            leadershipService.releaseLeadership(leadershipOwnerId);
        }
    }

    private int resolveOnlineTimeoutSeconds() {
        if (iotProperties.getDevice() == null || iotProperties.getDevice().getOnlineTimeoutSeconds() == null) {
            return 120;
        }
        return iotProperties.getDevice().getOnlineTimeoutSeconds();
    }

    private LocalDateTime resolveLastSeenTime(Device device) {
        if (device == null) {
            return null;
        }
        if (device.getLastOnlineTime() != null) {
            return device.getLastOnlineTime();
        }
        return device.getLastReportTime();
    }

    private LocalDateTime resolveOfflineTime(LocalDateTime lastSeenTime, int timeoutSeconds, LocalDateTime now) {
        if (lastSeenTime == null) {
            return now;
        }
        LocalDateTime inferredOfflineTime = lastSeenTime.plusSeconds(timeoutSeconds);
        return inferredOfflineTime.isAfter(now) ? now : inferredOfflineTime;
    }
}
