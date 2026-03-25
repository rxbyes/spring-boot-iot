package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceTelemetryMappingServiceImplTest {

    @Mock
    private ProductModelMapper productModelMapper;

    private DeviceTelemetryMappingServiceImpl deviceTelemetryMappingService;

    @BeforeEach
    void setUp() {
        deviceTelemetryMappingService = new DeviceTelemetryMappingServiceImpl(productModelMapper);
    }

    @Test
    void listMetricMappingMapShouldParseLegacyMappingAndMarkFallbackReasons() {
        when(productModelMapper.selectList(any())).thenReturn(List.of(
                productModel("temperature", "{\"tdengineLegacy\":{\"enabled\":true,\"stable\":\"s1_zt_1\",\"column\":\"temp\"}}"),
                productModel("humidity", "{\"tdengineLegacy\":{\"enabled\":false,\"stable\":\"s1_zt_1\",\"column\":\"humidity\"}}"),
                productModel("noise", "{\"tdengineLegacy\":{\"stable\":\"invalid-name!\",\"column\":\"noise\"}}"),
                productModel("status", "{\"tdengineLegacy\":{\"stable\":\"s1_zt_1\"}}"),
                productModel("voltage", "{\"scale\":2}")
        ));

        Map<String, TelemetryMetricMapping> mappingMap = deviceTelemetryMappingService.listMetricMappingMap(1001L);

        TelemetryMetricMapping temperature = mappingMap.get("temperature");
        assertTrue(temperature.isLegacyMapped());
        assertEquals("s1_zt_1", temperature.getStable());
        assertEquals("temp", temperature.getColumn());
        assertEquals(TelemetryMetricMapping.SOURCE_SPECS_JSON_TDENGINE_LEGACY, temperature.getSource());
        assertTrue(temperature.getFallbackReasons().isEmpty());

        TelemetryMetricMapping humidity = mappingMap.get("humidity");
        assertFalse(humidity.isLegacyMapped());
        assertEquals(Boolean.FALSE, humidity.getEnabled());
        assertIterableEquals(List.of(TelemetryMetricMapping.REASON_MAPPING_DISABLED), humidity.getFallbackReasons());

        TelemetryMetricMapping noise = mappingMap.get("noise");
        assertFalse(noise.isLegacyMapped());
        assertIterableEquals(List.of(TelemetryMetricMapping.REASON_STABLE_INVALID), noise.getFallbackReasons());

        TelemetryMetricMapping status = mappingMap.get("status");
        assertFalse(status.isLegacyMapped());
        assertIterableEquals(List.of(TelemetryMetricMapping.REASON_COLUMN_MISSING), status.getFallbackReasons());

        TelemetryMetricMapping voltage = mappingMap.get("voltage");
        assertFalse(voltage.isLegacyMapped());
        assertIterableEquals(List.of(TelemetryMetricMapping.REASON_MAPPING_NOT_CONFIGURED), voltage.getFallbackReasons());
    }

    private ProductModel productModel(String identifier, String specsJson) {
        ProductModel productModel = new ProductModel();
        productModel.setIdentifier(identifier);
        productModel.setSpecsJson(specsJson);
        return productModel;
    }
}
