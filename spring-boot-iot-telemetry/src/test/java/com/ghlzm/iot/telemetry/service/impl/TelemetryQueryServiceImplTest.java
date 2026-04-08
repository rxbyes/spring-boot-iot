package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.DeviceTelemetryMappingService;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchRequest;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchResponse;
import com.ghlzm.iot.telemetry.service.model.TelemetryLatestPoint;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryQueryServiceImplTest {

    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private DevicePropertyMapper devicePropertyMapper;
    @Mock
    private DevicePropertyMetadataService devicePropertyMetadataService;
    @Mock
    private DeviceTelemetryMappingService deviceTelemetryMappingService;
    @Mock
    private TdengineTelemetryFacade tdengineTelemetryFacade;
    @Mock
    private TelemetryStorageModeResolver storageModeResolver;
    @Mock
    private TelemetryReadRouter telemetryReadRouter;
    @Mock
    private TelemetryLatestProjectionRepository telemetryLatestProjectionRepository;
    @Mock
    private NormalizedTelemetryHistoryReader normalizedTelemetryHistoryReader;
    @Mock
    private LegacyTelemetryHistoryReader legacyTelemetryHistoryReader;

    private TelemetryQueryServiceImpl telemetryQueryService;

    @BeforeEach
    void setUp() {
        telemetryQueryService = new TelemetryQueryServiceImpl(
                deviceMapper,
                productMapper,
                devicePropertyMapper,
                devicePropertyMetadataService,
                deviceTelemetryMappingService,
                tdengineTelemetryFacade,
                storageModeResolver,
                telemetryReadRouter,
                telemetryLatestProjectionRepository,
                normalizedTelemetryHistoryReader,
                legacyTelemetryHistoryReader
        );
    }

    @Test
    void getHistoryBatchShouldZeroFillRequestedBuckets() {
        Device device = buildDevice();
        Product product = buildProduct();
        DevicePropertyMetadata measureMetadata = metadata("泥水位高程", "double");
        DevicePropertyMetadata statusMetadata = metadata("传感器在线状态", "int");
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.historySource()).thenReturn("v2");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(false);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of(
                "L4_NW_1", measureMetadata,
                "S1_ZT_1.sensor_state.L4_NW_1", statusMetadata
        ));
        when(normalizedTelemetryHistoryReader.hasHistory(2001L)).thenReturn(true);
        when(normalizedTelemetryHistoryReader.listHistory(eq(device), eq(product), anyMap(), anyInt())).thenReturn(List.of(
                historyPoint("L4_NW_1", "泥水位高程", 2.6D, LocalDateTime.of(2026, 4, 7, 0, 0)),
                historyPoint("S1_ZT_1.sensor_state.L4_NW_1", "传感器在线状态", 1L, LocalDateTime.of(2026, 4, 7, 0, 0))
        ));

        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("L4_NW_1", "S1_ZT_1.sensor_state.L4_NW_1"));
        request.setRangeCode("7d");
        request.setFillPolicy("ZERO");

        TelemetryHistoryBatchResponse result = telemetryQueryService.getHistoryBatch(request);

        assertEquals(2, result.getPoints().size());
        assertEquals("day", result.getBucket());
        assertEquals(7, result.getPoints().get(0).getBuckets().size());
        assertEquals(0D, result.getPoints().get(0).getBuckets().get(0).getValue());
        assertEquals("measure", result.getPoints().get(0).getSeriesType());
        assertEquals("status", result.getPoints().get(1).getSeriesType());
    }

    @Test
    void getHistoryBatchShouldRejectBlankIdentifiers() {
        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of());
        request.setRangeCode("7d");
        request.setFillPolicy("ZERO");

        assertThrows(BizException.class, () -> telemetryQueryService.getHistoryBatch(request));
    }

    @Test
    void shouldReadV2LatestBeforeLegacyFallback() {
        Device device = buildDevice();
        Product product = buildProduct();
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.latestSource()).thenReturn("v2");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(true);
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(telemetryLatestProjectionRepository.listLatestPoints(2001L)).thenReturn(List.of(
                latestPoint("temperature", 26.8D)
        ));
        when(tdengineTelemetryFacade.listLatestPoints(device, product)).thenReturn(List.of(
                latestPoint("temperature", 26.5D),
                latestPoint("humidity", 68)
        ));

        Map<String, Object> result = telemetryQueryService.getLatest(2001L);

        assertEquals("tdengine", result.get("storageType"));
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) result.get("properties");
        assertEquals(26.8D, properties.get("temperature"));
        assertEquals(68, properties.get("humidity"));
        verify(tdengineTelemetryFacade).listLatestPoints(device, product);
        verify(telemetryLatestProjectionRepository).listLatestPoints(2001L);
    }

    @Test
    void getLatestShouldFallbackToMysqlWhenConfigured() {
        when(storageModeResolver.isTdengineEnabled()).thenReturn(false);
        when(deviceMapper.selectOne(any())).thenReturn(buildDevice());
        when(productMapper.selectById(1001L)).thenReturn(buildProduct());
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
                property("temperature", "double", "26.5"),
                property("enabled", "bool", "true")
        ));

        Map<String, Object> result = telemetryQueryService.getLatest(2001L);

        assertEquals("mysql", result.get("storageType"));
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) result.get("properties");
        assertEquals(26.5D, properties.get("temperature"));
        assertEquals(Boolean.TRUE, properties.get("enabled"));
        verifyNoInteractions(tdengineTelemetryFacade);
    }

    @Test
    void getLatestShouldRejectUnknownDevice() {
        when(deviceMapper.selectOne(any())).thenReturn(null);

        assertThrows(BizException.class, () -> telemetryQueryService.getLatest(9999L));
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

    private TelemetryLatestPoint latestPoint(String metricCode, Object value) {
        TelemetryLatestPoint point = new TelemetryLatestPoint();
        point.setMetricCode(metricCode);
        point.setValue(value);
        point.setReportedAt(LocalDateTime.of(2026, 3, 23, 10, 0));
        point.setTraceId("trace-001");
        return point;
    }

    private DeviceProperty property(String identifier, String valueType, String value) {
        DeviceProperty property = new DeviceProperty();
        property.setIdentifier(identifier);
        property.setValueType(valueType);
        property.setPropertyValue(value);
        property.setReportTime(LocalDateTime.of(2026, 3, 23, 10, 0));
        return property;
    }

    private DevicePropertyMetadata metadata(String propertyName, String dataType) {
        DevicePropertyMetadata metadata = new DevicePropertyMetadata();
        metadata.setPropertyName(propertyName);
        metadata.setDataType(dataType);
        return metadata;
    }

    private TelemetryV2Point historyPoint(String metricCode,
                                          String metricName,
                                          Object value,
                                          LocalDateTime reportedAt) {
        TelemetryV2Point point = new TelemetryV2Point();
        point.setMetricCode(metricCode);
        point.setMetricName(metricName);
        point.setReportedAt(reportedAt);
        if (value instanceof Double number) {
            point.setValueDouble(number);
            point.setValueType("double");
        } else if (value instanceof Long number) {
            point.setValueLong(number);
            point.setValueType("long");
        }
        return point;
    }
}
