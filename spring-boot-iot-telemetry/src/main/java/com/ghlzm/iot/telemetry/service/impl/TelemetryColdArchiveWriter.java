package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryProjectionTask;
import org.springframework.stereotype.Service;

/**
 * Telemetry 冷历史归档写入器。
 * 当前 Phase 1 仅提供稳定扩展位，后续再接入离线归档实现。
 */
@Service
public class TelemetryColdArchiveWriter {

    public void archive(TelemetryProjectionTask task) {
        if (task == null || task.getPoints() == null || task.getPoints().isEmpty()) {
            return;
        }
        // Phase 1: no-op hook for future cold archive pipeline.
    }
}
