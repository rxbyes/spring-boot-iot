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
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void listMetricMappingsShouldParseLegacySpecsAndFlagInvalidEntries() {
        when(productModelMapper.selectList(any())).thenReturn(List.of(
                productModel("temperature", "{\"tdengineLegacy\":{\"enabled\":true,\"stable\":\"s1_zt_1\",\"column\":\"temp\"}}"),
                productModel("humidity", "{\"tdengineLegacy\":{\"enabled\":false,\"stable\":\"s1_zt_1\",\"column\":\"humidity\"}}"),
                productModel("noise", "{\"tdengineLegacy\":{\"column\":\"noise\"}}"),
                productModel("status", "{invalid-json}"),
                productModel("pressure", "{}")
        ));

        Map<String, TelemetryMetricMapping> mappingMap = deviceTelemetryMappingService.listMetricMappings(1001L);

        assertTrue(mappingMap.get("temperature").isLegacyUsable());
        assertEquals("s1_zt_1", mappingMap.get("temperature").getStable());
        assertEquals("temp", mappingMap.get("temperature").getColumn());
        assertEquals("PRODUCT_SPECS_TDENGINE_LEGACY", mappingMap.get("temperature").getSource());
        assertNull(mappingMap.get("temperature").getReason());

        assertFalse(mappingMap.get("humidity").isLegacyUsable());
        assertEquals(Boolean.FALSE, mappingMap.get("humidity").getEnabled());
        assertEquals("DISABLED", mappingMap.get("humidity").getReason());

        assertFalse(mappingMap.get("noise").isLegacyUsable());
        assertEquals("MISSING_STABLE", mappingMap.get("noise").getReason());

        assertFalse(mappingMap.get("status").isLegacyUsable());
        assertEquals("INVALID_SPECS_JSON", mappingMap.get("status").getReason());

        assertFalse(mappingMap.get("pressure").isLegacyUsable());
        assertEquals("MISSING_TDENGINE_LEGACY_MAPPING", mappingMap.get("pressure").getReason());
    }

    private ProductModel productModel(String identifier, String specsJson) {
        ProductModel productModel = new ProductModel();
        productModel.setIdentifier(identifier);
        productModel.setSpecsJson(specsJson);
        productModel.setModelType("property");
        productModel.setDeleted(0);
        return productModel;
    }
}
