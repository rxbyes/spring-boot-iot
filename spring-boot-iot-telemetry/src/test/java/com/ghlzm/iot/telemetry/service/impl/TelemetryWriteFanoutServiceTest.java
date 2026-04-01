package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.telemetry.service.model.TelemetryProjectionTask;
import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryWriteFanoutServiceTest {

    @Mock
    private TelemetryStorageModeResolver storageModeResolver;
    @Mock
    private TelemetryProjectionQueue projectionQueue;
    @Mock
    private TelemetryLatestProjector telemetryLatestProjector;
    @Mock
    private TelemetryLegacyMirrorProjector telemetryLegacyMirrorProjector;
    @Mock
    private TelemetryAggregateProjector telemetryAggregateProjector;
    @Mock
    private TelemetryColdArchiveWriter telemetryColdArchiveWriter;

    private TelemetryWriteFanoutService telemetryWriteFanoutService;

    @BeforeEach
    void setUp() {
        Executor directExecutor = Runnable::run;
        telemetryWriteFanoutService = new TelemetryWriteFanoutService(
                storageModeResolver,
                projectionQueue,
                telemetryLatestProjector,
                telemetryLegacyMirrorProjector,
                telemetryAggregateProjector,
                telemetryColdArchiveWriter,
                directExecutor
        );
    }

    @Test
    void shouldPublishAndDispatchAllEnabledDownstreamTasks() {
        when(storageModeResolver.isLatestMysqlProjectionEnabled()).thenReturn(true);
        when(storageModeResolver.isLegacyMirrorEnabled()).thenReturn(true);
        when(storageModeResolver.isAggregateEnabled()).thenReturn(true);
        when(storageModeResolver.isColdArchiveEnabled()).thenReturn(true);
        DeviceProcessingTarget target = buildTarget();
        List<TelemetryV2Point> points = List.of(point());

        telemetryWriteFanoutService.fanout(target, points);

        ArgumentCaptor<TelemetryProjectionTask> taskCaptor = ArgumentCaptor.forClass(TelemetryProjectionTask.class);
        verify(projectionQueue, times(4)).publish(taskCaptor.capture());
        verify(telemetryLatestProjector).project(any());
        verify(telemetryLegacyMirrorProjector).project(any());
        verify(telemetryAggregateProjector).project(any());
        verify(telemetryColdArchiveWriter).archive(any());
        Set<TelemetryProjectionTask.ProjectionType> types = taskCaptor.getAllValues().stream()
                .map(TelemetryProjectionTask::getProjectionType)
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                TelemetryProjectionTask.ProjectionType.LATEST,
                TelemetryProjectionTask.ProjectionType.LEGACY_MIRROR,
                TelemetryProjectionTask.ProjectionType.AGGREGATE,
                TelemetryProjectionTask.ProjectionType.COLD_ARCHIVE
        ), types);
    }

    @Test
    void shouldIgnoreQueuePublishFailureAndContinueDispatch() {
        when(storageModeResolver.isLatestMysqlProjectionEnabled()).thenReturn(true);
        when(storageModeResolver.isLegacyMirrorEnabled()).thenReturn(false);
        when(storageModeResolver.isAggregateEnabled()).thenReturn(true);
        when(storageModeResolver.isColdArchiveEnabled()).thenReturn(false);
        doThrow(new IllegalStateException("redis unavailable")).when(projectionQueue).publish(any());
        DeviceProcessingTarget target = buildTarget();
        List<TelemetryV2Point> points = List.of(point());

        assertDoesNotThrow(() -> telemetryWriteFanoutService.fanout(target, points));

        verify(telemetryLatestProjector).project(any());
        verify(telemetryAggregateProjector).project(any());
    }

    @Test
    void shouldSkipAllDownstreamWhenDisabled() {
        when(storageModeResolver.isLatestMysqlProjectionEnabled()).thenReturn(false);
        when(storageModeResolver.isLegacyMirrorEnabled()).thenReturn(false);
        when(storageModeResolver.isAggregateEnabled()).thenReturn(false);
        when(storageModeResolver.isColdArchiveEnabled()).thenReturn(false);

        telemetryWriteFanoutService.fanout(buildTarget(), List.of(point()));

        verifyNoInteractions(projectionQueue, telemetryLatestProjector, telemetryLegacyMirrorProjector,
                telemetryAggregateProjector, telemetryColdArchiveWriter);
    }

    private DeviceProcessingTarget buildTarget() {
        Device device = new Device();
        device.setId(2001L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");

        DeviceUpMessage message = new DeviceUpMessage();
        message.setMessageType("property");
        message.setProductKey("demo-product");
        message.setProtocolCode("mqtt-json");
        message.setTraceId("trace-001");
        message.setTopic("/up/demo");
        message.setTimestamp(LocalDateTime.of(2026, 4, 1, 10, 0));

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setProduct(product);
        target.setMessage(message);
        return target;
    }

    private TelemetryV2Point point() {
        TelemetryV2Point point = new TelemetryV2Point();
        point.setStreamKind(TelemetryStreamKind.MEASURE);
        point.setTenantId(1L);
        point.setDeviceId(2001L);
        point.setProductId(1001L);
        point.setMetricId("temperature");
        point.setMetricCode("temperature");
        point.setMetricName("温度");
        point.setValueType("double");
        point.setValueDouble(26.5D);
        point.setReportedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        point.setIngestedAt(LocalDateTime.of(2026, 4, 1, 10, 0, 1));
        point.setSourceMessageType("property");
        return point;
    }
}
