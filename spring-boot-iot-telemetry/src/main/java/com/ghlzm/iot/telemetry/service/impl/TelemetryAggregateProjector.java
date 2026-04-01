package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryProjectionTask;
import org.springframework.stereotype.Service;

/**
 * Telemetry 聚合投影器。
 * 当前 Phase 1 仅提供稳定扩展位，后续再接入小时/天级聚合存储。
 */
@Service
public class TelemetryAggregateProjector {

    public void project(TelemetryProjectionTask task) {
        if (task == null || task.getPoints() == null || task.getPoints().isEmpty()) {
            return;
        }
        // Phase 1: no-op hook for future aggregate projection.
    }
}
