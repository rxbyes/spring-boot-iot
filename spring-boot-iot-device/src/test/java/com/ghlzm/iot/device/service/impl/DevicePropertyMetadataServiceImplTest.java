package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
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

    private DevicePropertyMetadataServiceImpl devicePropertyMetadataService;

    @BeforeEach
    void setUp() {
        devicePropertyMetadataService = new DevicePropertyMetadataServiceImpl(productModelMapper);
    }

    @Test
    void listPropertyMetadataMapShouldParseLegacyMappingFromSpecsJson() {
        when(productModelMapper.selectList(any())).thenReturn(List.of(
                productModel("temperature", "温度", "double",
                        "{\"tdengineLegacy\":{\"enabled\":true,\"stable\":\"s1_zt_1\",\"column\":\"temp\"}}")
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

        Map<String, DevicePropertyMetadata> metadataMap = devicePropertyMetadataService.listPropertyMetadataMap(1001L);

        assertFalse(metadataMap.get("humidity").getTdengineLegacyMapping().getEnabled());
        assertNull(metadataMap.get("noise").getTdengineLegacyMapping());
        assertNull(metadataMap.get("status").getTdengineLegacyMapping());
    }

    @Test
    void listPropertyMetadataMapShouldKeepPublishedMixedCaseCanonicalIdentifier() {
        when(productModelMapper.selectList(any())).thenReturn(List.of(
                productModel("L1_GNSS_1.gpsTotalX", "GNSS X", "double",
                        "{\"tdengineLegacy\":{\"enabled\":true,\"stable\":\"s1_zt_1\",\"column\":\"gpsx\"}}")
        ));

        Map<String, DevicePropertyMetadata> metadataMap = devicePropertyMetadataService.listPropertyMetadataMap(1001L);

        assertNotNull(metadataMap.get("gpsTotalX"));
        assertNull(metadataMap.get("gpstotalx"));
    }

    private ProductModel productModel(String identifier, String modelName, String dataType, String specsJson) {
        ProductModel productModel = new ProductModel();
        productModel.setIdentifier(identifier);
        productModel.setModelName(modelName);
        productModel.setDataType(dataType);
        productModel.setSpecsJson(specsJson);
        return productModel;
    }
}
