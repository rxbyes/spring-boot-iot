package com.ghlzm.iot.telemetry.service;

import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchRequest;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchResponse;

import java.util.Map;

/**
 * 时序查询服务。
 */
public interface TelemetryQueryService {

    /**
     * 查询设备最新时序快照。
     */
    Map<String, Object> getLatest(Long deviceId);

    /**
     * 按设备和指标批量查询历史时序。
     */
    TelemetryHistoryBatchResponse getHistoryBatch(TelemetryHistoryBatchRequest request);
}
