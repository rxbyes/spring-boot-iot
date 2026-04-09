package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.DeviceTelemetryMappingService;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryMigrationRequest;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryMigrationResult;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import com.ghlzm.iot.telemetry.service.model.TelemetryProjectionTask;
import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetryHistoryMigrationServiceImplTest {

    @Test
    void migrateShouldPreferNormalizedHistoryWhenAvailable() {
        DeviceMapper deviceMapper = mock(DeviceMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        DevicePropertyMetadataService devicePropertyMetadataService = mock(DevicePropertyMetadataService.class);
        DeviceTelemetryMappingService deviceTelemetryMappingService = mock(DeviceTelemetryMappingService.class);
        NormalizedTelemetryHistoryReader normalizedReader = mock(NormalizedTelemetryHistoryReader.class);
        LegacyTelemetryHistoryReader legacyReader = mock(LegacyTelemetryHistoryReader.class);
        TelemetryRawBatchWriter rawBatchWriter = mock(TelemetryRawBatchWriter.class);
        TelemetryLatestProjector latestProjector = mock(TelemetryLatestProjector.class);

        Device device = device(2001L, 1001L, "device-01");
        Product product = product(1001L, "product-01");
        Map<String, DevicePropertyMetadata> metadataMap = Map.of("temperature", propertyMetadata("temperature", "温度", "double"));
        List<TelemetryV2Point> points = List.of(point(2001L, 1001L, "temperature"));

        when(deviceMapper.selectById(2001L)).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(metadataMap);
        when(normalizedReader.hasHistory(2001L)).thenReturn(true);
        when(normalizedReader.listHistory(device, product, metadataMap, 500)).thenReturn(points);
        when(rawBatchWriter.write(points)).thenReturn(TelemetryPersistResult.persisted("TDENGINE_V2_RAW", "tdengine-v2", 1, 0, 0, 0, 0));

        TelemetryHistoryMigrationServiceImpl service = new TelemetryHistoryMigrationServiceImpl(
                deviceMapper,
                productMapper,
                devicePropertyMetadataService,
                deviceTelemetryMappingService,
                normalizedReader,
                legacyReader,
                rawBatchWriter,
                latestProjector
        );

        TelemetryHistoryMigrationResult result = service.migrate(request(2001L, null, 500, false));

        assertEquals("normalized", result.getSource());
        assertEquals(1, result.getMigratedDeviceCount());
        assertEquals(1, result.getScannedPointCount());
        assertEquals(1, result.getWrittenPointCount());
        assertEquals(1, result.getLatestProjectedPointCount());
        verify(normalizedReader).listHistory(device, product, metadataMap, 500);
        verify(rawBatchWriter).write(points);
        verify(latestProjector).project(any(TelemetryProjectionTask.class));
    }

    @Test
    void migrateShouldFallbackToLegacyHistoryWhenNormalizedHistoryMissing() {
        DeviceMapper deviceMapper = mock(DeviceMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        DevicePropertyMetadataService devicePropertyMetadataService = mock(DevicePropertyMetadataService.class);
        DeviceTelemetryMappingService deviceTelemetryMappingService = mock(DeviceTelemetryMappingService.class);
        NormalizedTelemetryHistoryReader normalizedReader = mock(NormalizedTelemetryHistoryReader.class);
        LegacyTelemetryHistoryReader legacyReader = mock(LegacyTelemetryHistoryReader.class);
        TelemetryRawBatchWriter rawBatchWriter = mock(TelemetryRawBatchWriter.class);
        TelemetryLatestProjector latestProjector = mock(TelemetryLatestProjector.class);

        Device device = device(2002L, 1002L, "device-02");
        Product product = product(1002L, "product-02");
        Map<String, DevicePropertyMetadata> metadataMap = Map.of("angle", propertyMetadata("angle", "倾角", "double"));
        Map<String, TelemetryMetricMapping> mappingMap = Map.of("angle", metricMapping("angle", "l1_qj_1", "angle"));
        List<TelemetryV2Point> points = List.of(point(2002L, 1002L, "angle"));

        when(deviceMapper.selectById(2002L)).thenReturn(device);
        when(productMapper.selectById(1002L)).thenReturn(product);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1002L)).thenReturn(metadataMap);
        when(deviceTelemetryMappingService.listMetricMappingMap(1002L)).thenReturn(mappingMap);
        when(normalizedReader.hasHistory(2002L)).thenReturn(false);
        when(legacyReader.listHistory(device, product, metadataMap, mappingMap, 300)).thenReturn(points);
        when(rawBatchWriter.write(points)).thenReturn(TelemetryPersistResult.persisted("TDENGINE_V2_RAW", "tdengine-v2", 1, 0, 0, 0, 0));

        TelemetryHistoryMigrationServiceImpl service = new TelemetryHistoryMigrationServiceImpl(
                deviceMapper,
                productMapper,
                devicePropertyMetadataService,
                deviceTelemetryMappingService,
                normalizedReader,
                legacyReader,
                rawBatchWriter,
                latestProjector
        );

        TelemetryHistoryMigrationResult result = service.migrate(request(2002L, null, 300, false));

        assertEquals("legacy", result.getSource());
        assertEquals(1, result.getScannedPointCount());
        assertEquals(1, result.getWrittenPointCount());
        verify(legacyReader).listHistory(device, product, metadataMap, mappingMap, 300);
        verify(rawBatchWriter).write(points);
        verify(latestProjector).project(any(TelemetryProjectionTask.class));
    }

    private TelemetryHistoryMigrationRequest request(Long deviceId, Long productId, Integer batchSize, boolean preferLegacy) {
        TelemetryHistoryMigrationRequest request = new TelemetryHistoryMigrationRequest();
        request.setDeviceId(deviceId);
        request.setProductId(productId);
        request.setBatchSize(batchSize);
        request.setPreferLegacy(preferLegacy);
        return request;
    }

    private Device device(Long deviceId, Long productId, String deviceCode) {
        Device device = new Device();
        device.setId(deviceId);
        device.setTenantId(1L);
        device.setProductId(productId);
        device.setDeviceCode(deviceCode);
        return device;
    }

    private Product product(Long productId, String productKey) {
        Product product = new Product();
        product.setId(productId);
        product.setTenantId(1L);
        product.setProductKey(productKey);
        return product;
    }

    private DevicePropertyMetadata propertyMetadata(String identifier, String name, String dataType) {
        DevicePropertyMetadata metadata = new DevicePropertyMetadata();
        metadata.setIdentifier(identifier);
        metadata.setPropertyName(name);
        metadata.setDataType(dataType);
        return metadata;
    }

    private TelemetryMetricMapping metricMapping(String metricCode, String stable, String column) {
        TelemetryMetricMapping mapping = new TelemetryMetricMapping();
        mapping.setMetricCode(metricCode);
        mapping.setStable(stable);
        mapping.setColumn(column);
        mapping.setEnabled(Boolean.TRUE);
        mapping.setFallbackReasons(List.of());
        return mapping;
    }

    private TelemetryV2Point point(Long deviceId, Long productId, String metricId) {
        TelemetryV2Point point = new TelemetryV2Point();
        point.setStreamKind(TelemetryStreamKind.MEASURE);
        point.setTenantId(1L);
        point.setDeviceId(deviceId);
        point.setProductId(productId);
        point.setMetricId(metricId);
        point.setMetricCode(metricId);
        point.setMetricName(metricId);
        point.setValueType("double");
        point.setValueDouble(26.5D);
        point.setSourceMessageType("property");
        point.setReportedAt(LocalDateTime.of(2026, 3, 31, 13, 0));
        point.setIngestedAt(LocalDateTime.of(2026, 3, 31, 13, 0, 1));
        return point;
    }
}
