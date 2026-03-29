package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryProjectionTask;

/**
 * Telemetry 投影任务队列抽象。
 */
public interface TelemetryProjectionQueue {

    void publish(TelemetryProjectionTask task);
}
