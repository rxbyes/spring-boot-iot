package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Telemetry v2 写协调器。
 */
@Service
public class TelemetryWriteCoordinator {

    private final TelemetryStorageModeResolver storageModeResolver;
    private final TdengineTelemetryFacade tdengineTelemetryFacade;
    private final TelemetryRawBatchWriter rawBatchWriter;
    private final DevicePropertyMetadataService devicePropertyMetadataService;
    private final TelemetryWriteFanoutService telemetryWriteFanoutService;

    public TelemetryWriteCoordinator(TelemetryStorageModeResolver storageModeResolver,
                                     TdengineTelemetryFacade tdengineTelemetryFacade,
                                     TelemetryRawBatchWriter rawBatchWriter,
                                     DevicePropertyMetadataService devicePropertyMetadataService,
                                     TelemetryWriteFanoutService telemetryWriteFanoutService) {
        this.storageModeResolver = storageModeResolver;
        this.tdengineTelemetryFacade = tdengineTelemetryFacade;
        this.rawBatchWriter = rawBatchWriter;
        this.devicePropertyMetadataService = devicePropertyMetadataService;
        this.telemetryWriteFanoutService = telemetryWriteFanoutService;
    }

    public TelemetryPersistResult persist(DeviceProcessingTarget target) {
        if (!storageModeResolver.isTdengineEnabled()) {
            return TelemetryPersistResult.skipped("STORAGE_TYPE_MYSQL", "mysql", 0);
        }
        if (!storageModeResolver.isV2PrimaryEnabled()) {
            return tdengineTelemetryFacade.persist(target);
        }
        Map<String, Object> properties = target == null || target.getMessage() == null || target.getMessage().getProperties() == null
                ? Map.of()
                : target.getMessage().getProperties();
        Map<String, DevicePropertyMetadata> metadataMap = resolvePropertyMetadata(target);
        List<TelemetryV2Point> points = rawBatchWriter.toPoints(target, properties, metadataMap);
        TelemetryPersistResult persistResult = rawBatchWriter.write(points);
        if (persistResult.isSkipped() || points.isEmpty()) {
            return persistResult;
        }
        telemetryWriteFanoutService.fanout(target, points);
        return persistResult;
    }

    private Map<String, DevicePropertyMetadata> resolvePropertyMetadata(DeviceProcessingTarget target) {
        if (target == null || target.getDevice() == null || target.getDevice().getProductId() == null) {
            return Map.of();
        }
        return devicePropertyMetadataService.listPropertyMetadataMap(target.getDevice().getProductId());
    }
}
