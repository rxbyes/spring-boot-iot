package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceTelemetryMappingService;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevicePropertyMetadataServiceImplTest {

    @Mock
    private ProductModelMapper productModelMapper;
    @Mock
    private DeviceTelemetryMappingService deviceTelemetryMappingService;

    private DevicePropertyMetadataServiceImpl devicePropertyMetadataService;

    @BeforeEach
    void setUp() {
        devicePropertyMetadataService = new DevicePropertyMetadataServiceImpl(productModelMapper, deviceTelemetryMappingService);
    }

    @Test
    void listPropertyMetadataMapShouldParseLegacyMappingFromSpecsJson() {
        when(productModelMapper.selectList(any())).thenReturn(List.of(
                productModel("temperature", "温度", "double",
                        "{\"tdengineLegacy\":{\"enabled\":true,\"stable\":\"s1_zt_1\",\"column\":\"temp\"}}")
        ));
        when(deviceTelemetryMappingService.listMetricMappings(1001L)).thenReturn(Map.of(
                "temperature", mapping("temperature", Boolean.TRUE, "s1_zt_1", "temp", null)
        ));

        Map<String, DevicePropertyMetadata> metadataMap = devicePropertyMetadataService.listPropertyMetadataMap(1001L);

        DevicePropertyMetadata metadata = metadataMap.get("temperature");
        assertNotNull(metadata);
        assertNotNull(metadata.getTdengineLegacyMapping());
        assertEquals("s1_zt_1", metadata.getTdengineLegacyMapping().getStable());
        assertEquals("temp", metadata.getTdengineLegacyMapping().getColumn());
        assertEquals(Boolean.TRUE, metadata.getTdengineLegacyMapping().getEnabled());
    }

    @Test
    void listPropertyMetadataMapShouldHandleDisabledAndInvalidLegacyMapping() {
        when(productModelMapper.selectList(any())).thenReturn(List.of(
                productModel("humidity", "湿度", "double",
                        "{\"tdengineLegacy\":{\"enabled\":false,\"stable\":\"s1_zt_1\",\"column\":\"humidity\"}}"),
                productModel("noise", "噪声", "double", "{\"tdengineLegacy\":{\"stable\":\"invalid-name!\"}}"),
                productModel("status", "状态", "string", "{invalid-json}")
        ));
        when(deviceTelemetryMappingService.listMetricMappings(1001L)).thenReturn(Map.of(
                "humidity", mapping("humidity", Boolean.FALSE, "s1_zt_1", "humidity", "DISABLED"),
                "noise", mapping("noise", Boolean.TRUE, null, null, "INVALID_STABLE"),
                "status", mapping("status", Boolean.TRUE, null, null, "INVALID_SPECS_JSON")
        ));

        Map<String, DevicePropertyMetadata> metadataMap = devicePropertyMetadataService.listPropertyMetadataMap(1001L);

        assertFalse(metadataMap.get("humidity").getTdengineLegacyMapping().getEnabled());
        assertNull(metadataMap.get("noise").getTdengineLegacyMapping());
        assertNull(metadataMap.get("status").getTdengineLegacyMapping());
    }

    private ProductModel productModel(String identifier, String modelName, String dataType, String specsJson) {
        ProductModel productModel = new ProductModel();
        productModel.setIdentifier(identifier);
        productModel.setModelName(modelName);
        productModel.setDataType(dataType);
        productModel.setSpecsJson(specsJson);
        return productModel;
    }

    private TelemetryMetricMapping mapping(String metricCode,
                                           Boolean enabled,
                                           String stable,
                                           String column,
                                           String reason) {
        TelemetryMetricMapping mapping = new TelemetryMetricMapping();
        mapping.setMetricCode(metricCode);
        mapping.setEnabled(enabled);
        mapping.setStable(stable);
        mapping.setColumn(column);
        mapping.setReason(reason);
        mapping.setSource("PRODUCT_SPECS_TDENGINE_LEGACY");
        return mapping;
    }
}
