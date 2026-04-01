package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
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
 * Telemetry raw 写入后的 downstream fanout 服务。
 */
@Service
public class TelemetryWriteFanoutService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryWriteFanoutService.class);

    private final TelemetryStorageModeResolver storageModeResolver;
    private final TelemetryProjectionQueue projectionQueue;
    private final TelemetryLatestProjector telemetryLatestProjector;
    private final TelemetryLegacyMirrorProjector telemetryLegacyMirrorProjector;
    private final TelemetryAggregateProjector telemetryAggregateProjector;
    private final TelemetryColdArchiveWriter telemetryColdArchiveWriter;
    private final Executor applicationTaskExecutor;

    public TelemetryWriteFanoutService(TelemetryStorageModeResolver storageModeResolver,
                                       TelemetryProjectionQueue projectionQueue,
                                       TelemetryLatestProjector telemetryLatestProjector,
                                       TelemetryLegacyMirrorProjector telemetryLegacyMirrorProjector,
                                       TelemetryAggregateProjector telemetryAggregateProjector,
                                       TelemetryColdArchiveWriter telemetryColdArchiveWriter,
                                       @Qualifier("applicationTaskExecutor") Executor applicationTaskExecutor) {
        this.storageModeResolver = storageModeResolver;
        this.projectionQueue = projectionQueue;
        this.telemetryLatestProjector = telemetryLatestProjector;
        this.telemetryLegacyMirrorProjector = telemetryLegacyMirrorProjector;
        this.telemetryAggregateProjector = telemetryAggregateProjector;
        this.telemetryColdArchiveWriter = telemetryColdArchiveWriter;
        this.applicationTaskExecutor = applicationTaskExecutor;
    }

    public void fanout(DeviceProcessingTarget target, List<TelemetryV2Point> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        if (storageModeResolver.isLatestMysqlProjectionEnabled()) {
            TelemetryProjectionTask latestTask = buildProjectionTask(TelemetryProjectionTask.ProjectionType.LATEST, target, points);
            publishTask(latestTask);
            dispatch(latestTask, telemetryLatestProjector::project);
        }
        if (storageModeResolver.isLegacyMirrorEnabled()) {
            TelemetryProjectionTask legacyMirrorTask = buildProjectionTask(TelemetryProjectionTask.ProjectionType.LEGACY_MIRROR, target, points);
            publishTask(legacyMirrorTask);
            dispatch(legacyMirrorTask, telemetryLegacyMirrorProjector::project);
        }
        if (storageModeResolver.isAggregateEnabled()) {
            TelemetryProjectionTask aggregateTask = buildProjectionTask(TelemetryProjectionTask.ProjectionType.AGGREGATE, target, points);
            publishTask(aggregateTask);
            dispatch(aggregateTask, telemetryAggregateProjector::project);
        }
        if (storageModeResolver.isColdArchiveEnabled()) {
            TelemetryProjectionTask coldArchiveTask = buildProjectionTask(TelemetryProjectionTask.ProjectionType.COLD_ARCHIVE, target, points);
            publishTask(coldArchiveTask);
            dispatch(coldArchiveTask, telemetryColdArchiveWriter::archive);
        }
    }

    private TelemetryProjectionTask buildProjectionTask(TelemetryProjectionTask.ProjectionType projectionType,
                                                        DeviceProcessingTarget target,
                                                        List<TelemetryV2Point> points) {
        TelemetryProjectionTask task = new TelemetryProjectionTask();
        task.setProjectionType(projectionType);
        task.setTenantId(target == null || target.getDevice() == null ? null : target.getDevice().getTenantId());
        task.setDeviceId(target == null || target.getDevice() == null ? null : target.getDevice().getId());
        task.setProductId(target == null || target.getDevice() == null ? null : target.getDevice().getProductId());
        task.setDeviceCode(target == null || target.getDevice() == null ? null : target.getDevice().getDeviceCode());
        task.setProductKey(target == null || target.getMessage() == null ? null : target.getMessage().getProductKey());
        task.setProtocolCode(target == null || target.getMessage() == null ? null : target.getMessage().getProtocolCode());
        task.setTraceId(target == null || target.getMessage() == null ? null : target.getMessage().getTraceId());
        task.setMessageType(target == null || target.getMessage() == null ? null : target.getMessage().getMessageType());
        task.setTopic(target == null || target.getMessage() == null ? null : target.getMessage().getTopic());
        task.setReportedAt(target == null || target.getMessage() == null ? null : target.getMessage().getTimestamp());
        task.setProperties(target == null || target.getMessage() == null || target.getMessage().getProperties() == null
                ? Map.of()
                : target.getMessage().getProperties());
        task.setPoints(points == null ? List.of() : points);
        return task;
    }

    private void publishTask(TelemetryProjectionTask task) {
        try {
            projectionQueue.publish(task);
        } catch (Exception ex) {
            log.warn("发布 telemetry 投影任务失败, type={}, error={}", task.getProjectionType(), ex.getMessage());
        }
    }

    private void dispatch(TelemetryProjectionTask task, Consumer<TelemetryProjectionTask> consumer) {
        applicationTaskExecutor.execute(() -> {
            try {
                consumer.accept(task);
            } catch (Exception ex) {
                log.warn("执行 telemetry 投影失败, type={}, error={}", task.getProjectionType(), ex.getMessage());
            }
        });
    }
}
