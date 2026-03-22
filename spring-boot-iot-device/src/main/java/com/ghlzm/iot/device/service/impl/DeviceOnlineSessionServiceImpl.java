package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceOnlineSession;
import com.ghlzm.iot.device.mapper.DeviceOnlineSessionMapper;
import com.ghlzm.iot.device.service.DeviceOnlineSessionService;
import com.ghlzm.iot.device.vo.ProductActivityStatRow;
import java.sql.SQLSyntaxErrorException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备在线会话服务实现。
 */
@Service
public class DeviceOnlineSessionServiceImpl implements DeviceOnlineSessionService {

    private static final Logger log = LoggerFactory.getLogger(DeviceOnlineSessionServiceImpl.class);

    private final DeviceOnlineSessionMapper deviceOnlineSessionMapper;
    private final AtomicBoolean missingTableWarningLogged = new AtomicBoolean(false);

    public DeviceOnlineSessionServiceImpl(DeviceOnlineSessionMapper deviceOnlineSessionMapper) {
        this.deviceOnlineSessionMapper = deviceOnlineSessionMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordOnlineHeartbeat(Device device, LocalDateTime reportTime) {
        if (device == null || device.getId() == null || reportTime == null) {
            return;
        }

        try {
            DeviceOnlineSession activeSession = findActiveSession(device.getId());
            if (activeSession == null) {
                DeviceOnlineSession session = new DeviceOnlineSession();
                session.setTenantId(device.getTenantId());
                session.setProductId(device.getProductId());
                session.setDeviceId(device.getId());
                session.setDeviceCode(device.getDeviceCode());
                session.setOnlineTime(reportTime);
                session.setLastSeenTime(reportTime);
                deviceOnlineSessionMapper.insert(session);
                return;
            }

            LocalDateTime targetLastSeenTime = resolveLaterTime(activeSession.getLastSeenTime(), reportTime);
            if (targetLastSeenTime == null || targetLastSeenTime.equals(activeSession.getLastSeenTime())) {
                return;
            }
            activeSession.setLastSeenTime(targetLastSeenTime);
            deviceOnlineSessionMapper.updateById(activeSession);
        } catch (RuntimeException ex) {
            if (shouldDegradeForMissingTable(ex)) {
                logMissingTableOnce("recordOnlineHeartbeat", device.getDeviceCode(), ex);
                return;
            }
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeActiveSession(Device device, LocalDateTime offlineTime, String closeReason) {
        if (device == null || device.getId() == null || offlineTime == null) {
            return;
        }

        try {
            DeviceOnlineSession activeSession = findActiveSession(device.getId());
            if (activeSession == null) {
                return;
            }

            LocalDateTime normalizedOfflineTime = normalizeOfflineTime(activeSession.getOnlineTime(), offlineTime);
            LocalDateTime targetLastSeenTime = resolveLaterTime(activeSession.getLastSeenTime(), normalizedOfflineTime);
            activeSession.setLastSeenTime(targetLastSeenTime);
            activeSession.setOfflineTime(normalizedOfflineTime);
            activeSession.setDurationMinutes(resolveDurationMinutes(activeSession.getOnlineTime(), normalizedOfflineTime));
            if (hasText(closeReason)) {
                activeSession.setRemark(closeReason);
            }
            deviceOnlineSessionMapper.updateById(activeSession);
        } catch (RuntimeException ex) {
            if (shouldDegradeForMissingTable(ex)) {
                logMissingTableOnce("closeActiveSession", device.getDeviceCode(), ex);
                return;
            }
            throw ex;
        }
    }

    @Override
    public ProductActivityStatRow loadProductDurationStat(Long productId, LocalDateTime thirtyDaysStart, LocalDateTime statTime) {
        if (productId == null || thirtyDaysStart == null || statTime == null) {
            return null;
        }
        try {
            return deviceOnlineSessionMapper.selectProductDurationStat(productId, thirtyDaysStart, statTime);
        } catch (RuntimeException ex) {
            if (shouldDegradeForMissingTable(ex)) {
                logMissingTableOnce("loadProductDurationStat", String.valueOf(productId), ex);
                return null;
            }
            throw ex;
        }
    }

    private DeviceOnlineSession findActiveSession(Long deviceId) {
        return deviceOnlineSessionMapper.selectOne(
                new LambdaQueryWrapper<DeviceOnlineSession>()
                        .eq(DeviceOnlineSession::getDeviceId, deviceId)
                        .eq(DeviceOnlineSession::getDeleted, 0)
                        .isNull(DeviceOnlineSession::getOfflineTime)
                        .orderByDesc(DeviceOnlineSession::getOnlineTime)
                        .last("limit 1")
        );
    }

    private LocalDateTime normalizeOfflineTime(LocalDateTime onlineTime, LocalDateTime offlineTime) {
        if (onlineTime == null) {
            return offlineTime;
        }
        return offlineTime.isBefore(onlineTime) ? onlineTime : offlineTime;
    }

    private LocalDateTime resolveLaterTime(LocalDateTime currentTime, LocalDateTime candidateTime) {
        if (currentTime == null) {
            return candidateTime;
        }
        if (candidateTime == null) {
            return currentTime;
        }
        return candidateTime.isAfter(currentTime) ? candidateTime : currentTime;
    }

    private long resolveDurationMinutes(LocalDateTime onlineTime, LocalDateTime offlineTime) {
        if (onlineTime == null || offlineTime == null) {
            return 0L;
        }
        long minutes = Duration.between(onlineTime, offlineTime).toMinutes();
        return Math.max(minutes, 0L);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean shouldDegradeForMissingTable(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLSyntaxErrorException sqlSyntaxErrorException
                    && isMissingTableMessage(sqlSyntaxErrorException.getMessage())) {
                return true;
            }
            if (isMissingTableMessage(current.getMessage())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isMissingTableMessage(String message) {
        if (!hasText(message)) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("iot_device_online_session")
                && (normalized.contains("doesn't exist")
                || normalized.contains("does not exist")
                || normalized.contains("unknown table")
                || normalized.contains("1146"));
    }

    private void logMissingTableOnce(String operation, String target, Throwable ex) {
        if (!missingTableWarningLogged.compareAndSet(false, true)) {
            return;
        }
        log.warn(
                "检测到 iot_device_online_session 缺失，在线会话统计将降级跳过，但不影响设备主链路, operation={}, target={}, error={}",
                operation,
                target,
                ex.getMessage()
        );
    }
}
