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
            return TelemetryPersistResult.skipped("EMPTY_MESSAGE", null, 0);
        }
        if ("reply".equalsIgnoreCase(target.getMessage().getMessageType())) {
            return TelemetryPersistResult.skipped("MESSAGE_TYPE_REPLY", null, 0);
        }
        if (target.getMessage().getFilePayload() != null) {
            return TelemetryPersistResult.skipped("FILE_PAYLOAD", null, 0);
        }
        Map<String, Object> properties = target.getMessage().getProperties();
        if (properties == null || properties.isEmpty()) {
            return TelemetryPersistResult.skipped("EMPTY_PROPERTIES", null, 0);
        }
        return telemetryWriteCoordinator.persist(target);
    }
}
