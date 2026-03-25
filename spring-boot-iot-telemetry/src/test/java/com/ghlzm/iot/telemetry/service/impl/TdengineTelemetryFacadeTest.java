package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.service.DeviceTelemetryMappingService;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.telemetry.service.model.TelemetryLatestPoint;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TdengineTelemetryFacadeTest {

    @Mock
    private DevicePropertyMetadataService devicePropertyMetadataService;
    @Mock
    private DeviceTelemetryMappingService deviceTelemetryMappingService;
    @Mock
    private TdengineTelemetryStorageService tdengineTelemetryStorageService;
    @Mock
    private LegacyTdengineTelemetryWriter legacyTdengineTelemetryWriter;
    @Mock
    private LegacyTdengineTelemetryReader legacyTdengineTelemetryReader;
    @Mock
    private DeviceMessageLogMapper deviceMessageLogMapper;

    private IotProperties iotProperties;
    private TdengineTelemetryFacade facade;

    @BeforeEach
    void setUp() {
        iotProperties = new IotProperties();
        iotProperties.getTelemetry().setStorageType("tdengine");
        iotProperties.getTelemetry().setTdengineMode("legacy-compatible");
        facade = new TdengineTelemetryFacade(
                iotProperties,
                devicePropertyMetadataService,
                deviceTelemetryMappingService,
                tdengineTelemetryStorageService,
                legacyTdengineTelemetryWriter,
                legacyTdengineTelemetryReader,
                deviceMessageLogMapper
        );
    }

    @Test
    void persistShouldCombineLegacyAndNormalizedFallback() {
        DeviceProcessingTarget target = buildTarget(Map.of("temperature", 26.5D, "humidity", 68D, "noise", 4.2D));
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(metadataMap());
        when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(mappingMap());
        when(legacyTdengineTelemetryWriter.persist(any(), any(), any()))
                .thenReturn(LegacyTdengineTelemetryWriter.LegacyTdenginePersistOutcome.of(
                        1,
                        Set.of("temperature", "humidity"),
                        Map.of("noise", TelemetryMetricMapping.REASON_MAPPING_NOT_CONFIGURED)
                ));
        when(tdengineTelemetryStorageService.persist(any(), any(), any()))
                .thenReturn(TelemetryPersistResult.persisted("TDENGINE", "normalized-table", 1, 0, 0, 1, 0));

        TelemetryPersistResult result = facade.persist(target);

        assertEquals("LEGACY_WITH_NORMALIZED_FALLBACK", result.getBranch());
        assertEquals("legacy-compatible", result.getStorageMode());
        assertEquals(3, result.getPointCount());
        assertEquals(1, result.getLegacyStableCount());
        assertEquals(2, result.getLegacyColumnCount());
        assertEquals(1, result.getNormalizedFallbackCount());
        assertEquals(2, result.getLegacyMappedMetricCount());
        assertEquals(1, result.getLegacyUnmappedMetricCount());
        assertEquals(1, result.getFallbackMetricCount());
        assertEquals(List.of(TelemetryMetricMapping.REASON_MAPPING_NOT_CONFIGURED), result.getFallbackReasons());
        assertEquals(0, result.getSkippedMetricCount());
    }

    @Test
    void persistShouldExposeFallbackOnlyGovernanceSignal() {
        DeviceProcessingTarget target = buildTarget(Map.of("noise", 4.2D, "status", "ok"));
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(metadataMap());
        when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(fallbackOnlyMappingMap());
        when(legacyTdengineTelemetryWriter.persist(any(), any(), any()))
                .thenReturn(LegacyTdengineTelemetryWriter.LegacyTdenginePersistOutcome.of(
                        0,
                        Set.of(),
                        Map.of(
                                "noise", TelemetryMetricMapping.REASON_MAPPING_NOT_CONFIGURED,
                                "status", TelemetryMetricMapping.REASON_COLUMN_MISSING
                        )
                ));
        when(tdengineTelemetryStorageService.persist(any(), any(), any()))
                .thenReturn(TelemetryPersistResult.persisted("TDENGINE", "normalized-table", 2, 0, 0, 2, 0));

        TelemetryPersistResult result = facade.persist(target);

        assertEquals("NORMALIZED_FALLBACK_ONLY", result.getBranch());
        assertEquals(0, result.getLegacyMappedMetricCount());
        assertEquals(2, result.getLegacyUnmappedMetricCount());
        assertEquals(2, result.getFallbackMetricCount());
        assertEquals(
                List.of(TelemetryMetricMapping.REASON_MAPPING_NOT_CONFIGURED, TelemetryMetricMapping.REASON_COLUMN_MISSING),
                result.getFallbackReasons()
        );
    }

    @Test
    void listLatestPointsShouldCompensateTraceIdFromMessageLog() {
        Device device = buildDevice();
        Product product = buildProduct();
        DeviceMessageLog messageLog = new DeviceMessageLog();
        messageLog.setTraceId("trace-legacy-001");
        messageLog.setReportTime(LocalDateTime.of(2026, 3, 23, 10, 0));
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(metadataMap());
        when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(mappingMap());
        when(legacyTdengineTelemetryReader.listLatestPoints(device, product, metadataMap(), mappingMap()))
                .thenReturn(List.of(latestPoint("temperature", 26.5D, null)));
        when(tdengineTelemetryStorageService.listLatestPoints(2001L)).thenReturn(List.of());
        when(deviceMessageLogMapper.selectOne(any())).thenReturn(messageLog).thenReturn((DeviceMessageLog) null);

        List<TelemetryLatestPoint> points = facade.listLatestPoints(device, product);

        assertEquals(1, points.size());
        assertEquals("trace-legacy-001", points.get(0).getTraceId());
    }

    @Test
    void listLatestPointsShouldNotCompensateWhenNoMessageLogMatched() {
        Device device = buildDevice();
        Product product = buildProduct();
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(metadataMap());
        when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(mappingMap());
        when(legacyTdengineTelemetryReader.listLatestPoints(device, product, metadataMap(), mappingMap()))
                .thenReturn(List.of(latestPoint("temperature", 26.5D, null)));
        when(tdengineTelemetryStorageService.listLatestPoints(2001L)).thenReturn(List.of());
        when(deviceMessageLogMapper.selectOne(any())).thenReturn((DeviceMessageLog) null).thenReturn((DeviceMessageLog) null);

        List<TelemetryLatestPoint> points = facade.listLatestPoints(device, product);

        assertEquals(1, points.size());
        assertNull(points.get(0).getTraceId());
    }

    @Test
    void listLatestPointsShouldPreferCloserLaterMessageLogWithinCompensationWindow() {
        Device device = buildDevice();
        Product product = buildProduct();
        DeviceMessageLog earlierMessageLog = new DeviceMessageLog();
        earlierMessageLog.setTraceId("trace-earlier");
        earlierMessageLog.setReportTime(LocalDateTime.of(2026, 3, 23, 10, 0, 24));
        DeviceMessageLog laterMessageLog = new DeviceMessageLog();
        laterMessageLog.setTraceId("trace-later");
        laterMessageLog.setReportTime(LocalDateTime.of(2026, 3, 23, 10, 0, 25));
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(metadataMap());
        when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(mappingMap());
        when(legacyTdengineTelemetryReader.listLatestPoints(device, product, metadataMap(), mappingMap()))
                .thenReturn(List.of(latestPoint("temperature", 26.5D, null, LocalDateTime.of(2026, 3, 23, 10, 0, 24, 531_000_000))));
        when(tdengineTelemetryStorageService.listLatestPoints(2001L)).thenReturn(List.of());
        when(deviceMessageLogMapper.selectOne(any())).thenReturn(earlierMessageLog, laterMessageLog);

        List<TelemetryLatestPoint> points = facade.listLatestPoints(device, product);

        assertEquals(1, points.size());
        assertEquals("trace-later", points.get(0).getTraceId());
    }

    private DeviceProcessingTarget buildTarget(Map<String, Object> properties) {
        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(buildDevice());
        target.setProduct(buildProduct());
        DeviceUpMessage message = new DeviceUpMessage();
        message.setProperties(properties);
        message.setTimestamp(LocalDateTime.of(2026, 3, 23, 10, 0));
        target.setMessage(message);
        return target;
    }

    private Device buildDevice() {
        Device device = new Device();
        device.setId(2001L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");
        return device;
    }

    private Product buildProduct() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        return product;
    }

    private Map<String, DevicePropertyMetadata> metadataMap() {
        Map<String, DevicePropertyMetadata> metadataMap = new LinkedHashMap<>();
        metadataMap.put("temperature", metadata("temperature", "temp"));
        metadataMap.put("humidity", metadata("humidity", "humidity"));
        return metadataMap;
    }

    private Map<String, TelemetryMetricMapping> mappingMap() {
        Map<String, TelemetryMetricMapping> mappingMap = new LinkedHashMap<>();
        mappingMap.put("temperature", mapping("temperature", "s1_zt_1", "temp"));
        mappingMap.put("humidity", mapping("humidity", "s1_zt_1", "humidity"));
        mappingMap.put("noise", fallbackMapping("noise", TelemetryMetricMapping.REASON_MAPPING_NOT_CONFIGURED));
        return mappingMap;
    }

    private Map<String, TelemetryMetricMapping> fallbackOnlyMappingMap() {
        Map<String, TelemetryMetricMapping> mappingMap = new LinkedHashMap<>();
        mappingMap.put("noise", fallbackMapping("noise", TelemetryMetricMapping.REASON_MAPPING_NOT_CONFIGURED));
        mappingMap.put("status", fallbackMapping("status", TelemetryMetricMapping.REASON_COLUMN_MISSING));
        return mappingMap;
    }

    private DevicePropertyMetadata metadata(String identifier, String column) {
        DevicePropertyMetadata metadata = new DevicePropertyMetadata();
        metadata.setIdentifier(identifier);
        DevicePropertyMetadata.TdengineLegacyMapping mapping = new DevicePropertyMetadata.TdengineLegacyMapping();
        mapping.setStable("s1_zt_1");
        mapping.setColumn(column);
        metadata.setTdengineLegacyMapping(mapping);
        return metadata;
    }

    private TelemetryMetricMapping mapping(String metricCode, String stable, String column) {
        TelemetryMetricMapping mapping = new TelemetryMetricMapping();
        mapping.setMetricCode(metricCode);
        mapping.setStable(stable);
        mapping.setColumn(column);
        return mapping;
    }

    private TelemetryMetricMapping fallbackMapping(String metricCode, String reason) {
        TelemetryMetricMapping mapping = new TelemetryMetricMapping();
        mapping.setMetricCode(metricCode);
        mapping.setFallbackReasons(List.of(reason));
        return mapping;
    }

    private TelemetryLatestPoint latestPoint(String metricCode, Object value, String traceId) {
        return latestPoint(metricCode, value, traceId, LocalDateTime.of(2026, 3, 23, 10, 0));
    }

    private TelemetryLatestPoint latestPoint(String metricCode, Object value, String traceId, LocalDateTime reportedAt) {
        TelemetryLatestPoint point = new TelemetryLatestPoint();
        point.setMetricCode(metricCode);
        point.setValue(value);
        point.setReportedAt(reportedAt);
        point.setTraceId(traceId);
        return point;
    }
}
