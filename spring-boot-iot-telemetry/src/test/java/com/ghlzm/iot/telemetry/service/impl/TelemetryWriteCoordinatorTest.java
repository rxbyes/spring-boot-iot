package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryWriteCoordinatorTest {

    @Mock
    private TelemetryStorageModeResolver storageModeResolver;
    @Mock
    private TdengineTelemetryFacade tdengineTelemetryFacade;
    @Mock
    private TelemetryRawBatchWriter rawBatchWriter;
    @Mock
    private DevicePropertyMetadataService devicePropertyMetadataService;
    @Mock
    private TelemetryWriteFanoutService telemetryWriteFanoutService;

    private TelemetryWriteCoordinator coordinator;

    @Test
    void shouldWriteV2RawBeforeDelegatingFanout() {
        coordinator = new TelemetryWriteCoordinator(
                storageModeResolver,
                tdengineTelemetryFacade,
                rawBatchWriter,
                devicePropertyMetadataService,
                telemetryWriteFanoutService
        );
        DeviceProcessingTarget target = buildTarget();
        List<TelemetryV2Point> points = List.of(point());
        TelemetryPersistResult rawResult = TelemetryPersistResult.persisted("TDENGINE_V2_RAW", "tdengine-v2", 1, 0, 0, 0, 0);
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(storageModeResolver.isV2PrimaryEnabled()).thenReturn(true);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of());
        when(rawBatchWriter.toPoints(target, target.getMessage().getProperties(), Map.of())).thenReturn(points);
        when(rawBatchWriter.write(points)).thenReturn(rawResult);

        TelemetryPersistResult result = coordinator.persist(target);

        assertEquals("TDENGINE_V2_RAW", result.getBranch());
        InOrder inOrder = inOrder(rawBatchWriter, telemetryWriteFanoutService);
        inOrder.verify(rawBatchWriter).write(points);
        inOrder.verify(telemetryWriteFanoutService).fanout(target, points);
    }

    @Test
    void shouldNotDelegateFanoutWhenWriteSkipped() {
        coordinator = new TelemetryWriteCoordinator(
                storageModeResolver,
                tdengineTelemetryFacade,
                rawBatchWriter,
                devicePropertyMetadataService,
                telemetryWriteFanoutService
        );
        DeviceProcessingTarget target = buildTarget();
        TelemetryPersistResult rawResult = TelemetryPersistResult.skipped("EMPTY_PROPERTIES", "tdengine-v2", 0);
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(storageModeResolver.isV2PrimaryEnabled()).thenReturn(true);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of());
        when(rawBatchWriter.toPoints(target, target.getMessage().getProperties(), Map.of())).thenReturn(List.of());
        when(rawBatchWriter.write(List.of())).thenReturn(rawResult);

        TelemetryPersistResult result = coordinator.persist(target);

        assertEquals("EMPTY_PROPERTIES", result.getBranch());
        verify(telemetryWriteFanoutService, never()).fanout(target, List.of());
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
        message.setProperties(Map.of("temperature", 26.5D));
        message.setTimestamp(LocalDateTime.of(2026, 3, 27, 9, 0));

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
        point.setReportedAt(LocalDateTime.of(2026, 3, 27, 9, 0));
        point.setSourceMessageType("property");
        return point;
    }
}
