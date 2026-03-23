package com.ghlzm.iot.telemetry.service.handler;

import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.telemetry.service.impl.TdengineTelemetryFacade;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Locale;

/**
 * 时序落库 stage。
 */
@Component
public class TelemetryPersistStageHandler {

    private final IotProperties iotProperties;
    private final TdengineTelemetryFacade tdengineTelemetryFacade;

    public TelemetryPersistStageHandler(IotProperties iotProperties,
                                        TdengineTelemetryFacade tdengineTelemetryFacade) {
        this.iotProperties = iotProperties;
        this.tdengineTelemetryFacade = tdengineTelemetryFacade;
    }

    public TelemetryPersistResult persist(DeviceProcessingTarget target) {
        if (!isTdengineStorageEnabled()) {
            return TelemetryPersistResult.skipped(
                    "STORAGE_TYPE_" + normalizeStorageType().toUpperCase(Locale.ROOT),
                    normalizeTdengineMode(),
                    0
            );
        }
        if (target == null || target.getMessage() == null) {
            return TelemetryPersistResult.skipped("EMPTY_MESSAGE", normalizeTdengineMode(), 0);
        }
        if ("reply".equalsIgnoreCase(target.getMessage().getMessageType())) {
            return TelemetryPersistResult.skipped("MESSAGE_TYPE_REPLY", normalizeTdengineMode(), 0);
        }
        if (target.getMessage().getFilePayload() != null) {
            return TelemetryPersistResult.skipped("FILE_PAYLOAD", normalizeTdengineMode(), 0);
        }
        Map<String, Object> properties = target.getMessage().getProperties();
        if (properties == null || properties.isEmpty()) {
            return TelemetryPersistResult.skipped("EMPTY_PROPERTIES", normalizeTdengineMode(), 0);
        }
        return tdengineTelemetryFacade.persist(target);
    }

    private boolean isTdengineStorageEnabled() {
        return "tdengine".equalsIgnoreCase(normalizeStorageType());
    }

    private String normalizeStorageType() {
        if (iotProperties.getTelemetry() == null || iotProperties.getTelemetry().getStorageType() == null) {
            return "mysql";
        }
        return iotProperties.getTelemetry().getStorageType().trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeTdengineMode() {
        if (iotProperties.getTelemetry() == null || iotProperties.getTelemetry().getTdengineMode() == null) {
            return "legacy-compatible";
        }
        return iotProperties.getTelemetry().getTdengineMode().trim().toLowerCase(Locale.ROOT);
    }
}
