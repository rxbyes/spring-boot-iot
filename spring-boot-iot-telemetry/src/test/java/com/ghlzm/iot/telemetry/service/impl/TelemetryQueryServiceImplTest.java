package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.telemetry.service.model.TelemetryLatestPoint;
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
    private TdengineTelemetryFacade tdengineTelemetryFacade;
    @Mock
    private TelemetryStorageModeResolver storageModeResolver;
    @Mock
    private TelemetryReadRouter telemetryReadRouter;
    @Mock
    private TelemetryLatestProjectionRepository telemetryLatestProjectionRepository;

    private TelemetryQueryServiceImpl telemetryQueryService;

    @BeforeEach
    void setUp() {
        telemetryQueryService = new TelemetryQueryServiceImpl(
                deviceMapper,
                productMapper,
                devicePropertyMapper,
                tdengineTelemetryFacade,
                storageModeResolver,
                telemetryReadRouter,
                telemetryLatestProjectionRepository
        );
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
}
