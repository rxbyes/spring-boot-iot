package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import com.ghlzm.iot.telemetry.service.model.TelemetryProjectionTask;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Telemetry v2 写协调器。
 */
@Service
public class TelemetryWriteCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TelemetryWriteCoordinator.class);

    private final TelemetryStorageModeResolver storageModeResolver;
    private final TdengineTelemetryFacade tdengineTelemetryFacade;
    private final TelemetryRawBatchWriter rawBatchWriter;
    private final DevicePropertyMetadataService devicePropertyMetadataService;
    private final TelemetryProjectionQueue projectionQueue;
    private final TelemetryLatestProjector latestProjector;
    private final TelemetryLegacyMirrorProjector legacyMirrorProjector;
    private final Executor applicationTaskExecutor;

    public TelemetryWriteCoordinator(TelemetryStorageModeResolver storageModeResolver,
                                     TdengineTelemetryFacade tdengineTelemetryFacade,
                                     TelemetryRawBatchWriter rawBatchWriter,
                                     DevicePropertyMetadataService devicePropertyMetadataService,
                                     TelemetryProjectionQueue projectionQueue,
                                     TelemetryLatestProjector latestProjector,
                                     TelemetryLegacyMirrorProjector legacyMirrorProjector,
                                     @Qualifier("applicationTaskExecutor") Executor applicationTaskExecutor) {
        this.storageModeResolver = storageModeResolver;
        this.tdengineTelemetryFacade = tdengineTelemetryFacade;
        this.rawBatchWriter = rawBatchWriter;
        this.devicePropertyMetadataService = devicePropertyMetadataService;
        this.projectionQueue = projectionQueue;
        this.latestProjector = latestProjector;
        this.legacyMirrorProjector = legacyMirrorProjector;
        this.applicationTaskExecutor = applicationTaskExecutor;
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

        if (storageModeResolver.isLatestMysqlProjectionEnabled()) {
            TelemetryProjectionTask latestTask = buildProjectionTask(
                    TelemetryProjectionTask.ProjectionType.LATEST,
                    target,
                    points
            );
            publishTask(latestTask);
            dispatchProjection(latestTask, latestProjector::project);
        }
        if (storageModeResolver.isLegacyMirrorEnabled()) {
            TelemetryProjectionTask legacyMirrorTask = buildProjectionTask(
                    TelemetryProjectionTask.ProjectionType.LEGACY_MIRROR,
                    target,
                    points
            );
            publishTask(legacyMirrorTask);
            dispatchProjection(legacyMirrorTask, legacyMirrorProjector::project);
        }
        return persistResult;
    }

    private Map<String, DevicePropertyMetadata> resolvePropertyMetadata(DeviceProcessingTarget target) {
        if (target == null || target.getDevice() == null || target.getDevice().getProductId() == null) {
            return Map.of();
        }
        return devicePropertyMetadataService.listPropertyMetadataMap(target.getDevice().getProductId());
    }

    private TelemetryProjectionTask buildProjectionTask(TelemetryProjectionTask.ProjectionType projectionType,
                                                        DeviceProcessingTarget target,
                                                        List<TelemetryV2Point> points) {
        TelemetryProjectionTask task = new TelemetryProjectionTask();
        task.setProjectionType(projectionType);
        task.setTenantId(target.getDevice() == null ? null : target.getDevice().getTenantId());
        task.setDeviceId(target.getDevice() == null ? null : target.getDevice().getId());
        task.setProductId(target.getDevice() == null ? null : target.getDevice().getProductId());
        task.setDeviceCode(target.getDevice() == null ? null : target.getDevice().getDeviceCode());
        task.setProductKey(target.getMessage() == null ? null : target.getMessage().getProductKey());
        task.setProtocolCode(target.getMessage() == null ? null : target.getMessage().getProtocolCode());
        task.setTraceId(target.getMessage() == null ? null : target.getMessage().getTraceId());
        task.setMessageType(target.getMessage() == null ? null : target.getMessage().getMessageType());
        task.setTopic(target.getMessage() == null ? null : target.getMessage().getTopic());
        task.setReportedAt(target.getMessage() == null ? null : target.getMessage().getTimestamp());
        task.setProperties(target.getMessage() == null || target.getMessage().getProperties() == null
                ? Map.of()
                : target.getMessage().getProperties());
        task.setPoints(points);
        return task;
    }

    private void publishTask(TelemetryProjectionTask task) {
        try {
            projectionQueue.publish(task);
        } catch (Exception ex) {
            log.warn("发布 telemetry 投影任务失败, type={}, error={}",
                    task.getProjectionType(), ex.getMessage());
        }
    }

    private void dispatchProjection(TelemetryProjectionTask task, Consumer<TelemetryProjectionTask> projector) {
        applicationTaskExecutor.execute(() -> {
            try {
                projector.accept(task);
            } catch (Exception ex) {
                log.warn("执行 telemetry 投影失败, type={}, error={}",
                        task.getProjectionType(), ex.getMessage());
            }
        });
    }
}
