package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.entity.DeviceInvalidReportState;

import java.time.LocalDateTime;

/**
 * 无效 MQTT 上报最新态服务。
 */
public interface DeviceInvalidReportStateService {

    void upsertState(DeviceInvalidReportState state);

    void markResolvedByDevice(String productKey, String deviceCode, LocalDateTime resolvedTime);
}
