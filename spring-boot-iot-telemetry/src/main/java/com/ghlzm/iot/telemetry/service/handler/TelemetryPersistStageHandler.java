package com.ghlzm.iot.telemetry.service.handler;

import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.telemetry.service.impl.TdengineTelemetryStorageService;
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
    private final TdengineTelemetryStorageService tdengineTelemetryStorageService;

    public TelemetryPersistStageHandler(IotProperties iotProperties,
                                        TdengineTelemetryStorageService tdengineTelemetryStorageService) {
        this.iotProperties = iotProperties;
        this.tdengineTelemetryStorageService = tdengineTelemetryStorageService;
    }

    public TelemetryPersistResult persist(DeviceProcessingTarget target) {
        if (!isTdengineStorageEnabled()) {
            return TelemetryPersistResult.skipped("STORAGE_TYPE_" + normalizeStorageType().toUpperCase(Locale.ROOT));
        }
        if (target == null || target.getMessage() == null) {
            return TelemetryPersistResult.skipped("EMPTY_MESSAGE");
        }
        if ("reply".equalsIgnoreCase(target.getMessage().getMessageType())) {
            return TelemetryPersistResult.skipped("MESSAGE_TYPE_REPLY");
        }
        if (target.getMessage().getFilePayload() != null) {
            return TelemetryPersistResult.skipped("FILE_PAYLOAD");
        }
        Map<String, Object> properties = target.getMessage().getProperties();
        if (properties == null || properties.isEmpty()) {
            return TelemetryPersistResult.skipped("EMPTY_PROPERTIES");
        }
        return tdengineTelemetryStorageService.persist(target);
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
}
