package com.ghlzm.iot.telemetry.service.handler;

import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.telemetry.service.impl.TelemetryWriteCoordinator;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 时序落库 stage。
 */
@Component
public class TelemetryPersistStageHandler {

    private final TelemetryWriteCoordinator telemetryWriteCoordinator;

    public TelemetryPersistStageHandler(TelemetryWriteCoordinator telemetryWriteCoordinator) {
        this.telemetryWriteCoordinator = telemetryWriteCoordinator;
    }

    public TelemetryPersistResult persist(DeviceProcessingTarget target) {
        if (target == null || target.getMessage() == null) {
            return decorateBoundary(target, TelemetryPersistResult.skipped("EMPTY_MESSAGE", null, 0));
        }
        if ("reply".equalsIgnoreCase(target.getMessage().getMessageType())) {
            return decorateBoundary(target, TelemetryPersistResult.skipped("MESSAGE_TYPE_REPLY", null, 0));
        }
        if (target.getMessage().getFilePayload() != null) {
            return decorateBoundary(target, TelemetryPersistResult.skipped("FILE_PAYLOAD", null, 0));
        }
        Map<String, Object> properties = target.getMessage().getProperties();
        if (properties == null || properties.isEmpty()) {
            return decorateBoundary(target, TelemetryPersistResult.skipped("EMPTY_PROPERTIES", null, 0));
        }
        return decorateBoundary(target, telemetryWriteCoordinator.persist(target));
    }

    private TelemetryPersistResult decorateBoundary(DeviceProcessingTarget target, TelemetryPersistResult result) {
        if (result == null) {
            return null;
        }
        boolean childTarget = target != null && Boolean.TRUE.equals(target.getChildTarget());
        String targetDeviceCode = target == null || target.getDevice() == null ? null : target.getDevice().getDeviceCode();
        result.setTargetDeviceCode(targetDeviceCode);
        result.setChildTarget(childTarget);
        result.setTargetRole(childTarget ? "CHILD" : "PRIMARY");
        return result;
    }
}
