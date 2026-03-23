package com.ghlzm.iot.telemetry.service;

import java.util.Map;

/**
 * 时序查询服务。
 */
public interface TelemetryQueryService {

    /**
     * 查询设备最新时序快照。
     */
    Map<String, Object> getLatest(Long deviceId);
}
