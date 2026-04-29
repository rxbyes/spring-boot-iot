package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskMetricCatalogRebuildServiceImplTest {

    @Test
    void rebuildReleasedContractsShouldUseMeasureTruthIdentifiersFromProductMetadata() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductModelMapper productModelMapper = mock(ProductModelMapper.class);
        ProductContractReleaseBatchMapper releaseBatchMapper = mock(ProductContractReleaseBatchMapper.class);
        RiskMetricCatalogService riskMetricCatalogService = mock(RiskMetricCatalogService.class);
        RiskMetricCatalogRebuildServiceImpl service = new RiskMetricCatalogRebuildServiceImpl(
                productMapper,
                productModelMapper,
                releaseBatchMapper,
                new DefaultRiskMetricCatalogPublishRule(),
                riskMetricCatalogService
        );

        Product product = new Product();
        product.setId(2002L);
        product.setMetadataJson("""
                {
                  "objectInsight": {
                    "customMetrics": [
                      {
                        "identifier": "L1_LF_1.value",
                        "displayName": "裂缝量",
                        "group": "measure",
                        "enabled": true,
                        "includeInTrend": true
                      },
                      {
                        "identifier": "L1_QJ_1.angle",
                        "displayName": "倾角",
                        "group": "statusEvent",
                        "enabled": true,
                        "includeInTrend": true
                      },
                      {
                        "identifier": "L1_JS_1.gX",
                        "displayName": "X轴加速度",
                        "group": "measure",
                        "enabled": true,
                        "includeInTrend": false
                      }
                    ]
                  }
                }
                """);
        when(productMapper.selectById(2002L)).thenReturn(product);

        ProductModel crackValue = productModel(4101L, 2002L, "L1_LF_1.value", "裂缝量");
        ProductModel tiltAngle = productModel(4102L, 2002L, "L1_QJ_1.angle", "倾角");
        ProductModel accelX = productModel(4103L, 2002L, "L1_JS_1.gX", "X轴加速度");

        assertTrue(service.rebuildReleasedContracts(2002L, 8001L, List.of(crackValue, tiltAngle, accelX)));

        verify(riskMetricCatalogService).publishFromReleasedContracts(
                eq(2002L),
                eq(8001L),
                eq(List.of(crackValue, tiltAngle, accelX)),
                eq(Set.of("L1_LF_1.value"))
        );
    }

    @Test
    void rebuildLatestReleaseShouldLoadLatestBatchBeforePublishingCatalog() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductModelMapper productModelMapper = mock(ProductModelMapper.class);
        ProductContractReleaseBatchMapper releaseBatchMapper = mock(ProductContractReleaseBatchMapper.class);
        RiskMetricCatalogService riskMetricCatalogService = mock(RiskMetricCatalogService.class);
        RiskMetricCatalogRebuildServiceImpl service = new RiskMetricCatalogRebuildServiceImpl(
                productMapper,
                productModelMapper,
                releaseBatchMapper,
                new DefaultRiskMetricCatalogPublishRule(),
                riskMetricCatalogService
        );

        Product product = new Product();
        product.setId(2002L);
        product.setMetadataJson("""
                {
                  "objectInsight": {
                    "customMetrics": [
                      {
                        "identifier": "L1_LF_1.value",
                        "displayName": "裂缝量",
                        "group": "measure",
                        "enabled": true,
                        "includeInTrend": true
                      }
                    ]
                  }
                }
                """);
        when(productMapper.selectById(2002L)).thenReturn(product);

        ProductContractReleaseBatch latestBatch = new ProductContractReleaseBatch();
        latestBatch.setId(8002L);
        when(releaseBatchMapper.selectList(any())).thenReturn(List.of(latestBatch));

        ProductModel crackValue = productModel(4101L, 2002L, "L1_LF_1.value", "裂缝量");
        when(productModelMapper.selectList(any())).thenReturn(List.of(crackValue));

        assertTrue(service.rebuildLatestRelease(2002L));

        verify(riskMetricCatalogService).publishFromReleasedContracts(
                eq(2002L),
                eq(8002L),
                eq(List.of(crackValue)),
                eq(Set.of("L1_LF_1.value"))
        );
    }

    @Test
    void rebuildLatestReleaseShouldFallbackToCurrentFormalContractsWhenReleaseBatchMissing() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductModelMapper productModelMapper = mock(ProductModelMapper.class);
        ProductContractReleaseBatchMapper releaseBatchMapper = mock(ProductContractReleaseBatchMapper.class);
        RiskMetricCatalogService riskMetricCatalogService = mock(RiskMetricCatalogService.class);
        RiskMetricCatalogRebuildServiceImpl service = new RiskMetricCatalogRebuildServiceImpl(
                productMapper,
                productModelMapper,
                releaseBatchMapper,
                new DefaultRiskMetricCatalogPublishRule(),
                riskMetricCatalogService
        );

        Product product = new Product();
        product.setId(2002L);
        product.setMetadataJson("""
                {
                  "objectInsight": {
                    "customMetrics": [
                      {
                        "identifier": "L1_LF_1.value",
                        "displayName": "裂缝量",
                        "group": "measure",
                        "enabled": true,
                        "includeInTrend": true
                      },
                      {
                        "identifier": "L1_QJ_1.angle",
                        "displayName": "倾角",
                        "group": "measure",
                        "enabled": true,
                        "includeInTrend": true
                      },
                      {
                        "identifier": "L1_JS_1.gX",
                        "displayName": "X轴加速度",
                        "group": "measure",
                        "enabled": true,
                        "includeInTrend": true
                      }
                    ]
                  }
                }
                """);
        when(productMapper.selectById(2002L)).thenReturn(product);
        when(releaseBatchMapper.selectList(any())).thenReturn(List.of());

        ProductModel crackValue = productModel(4101L, 2002L, "L1_LF_1.value", "裂缝量");
        ProductModel tiltAngle = productModel(4102L, 2002L, "L1_QJ_1.angle", "倾角");
        ProductModel accelX = productModel(4103L, 2002L, "L1_JS_1.gX", "X轴加速度");
        when(productModelMapper.selectList(any())).thenReturn(List.of(crackValue, tiltAngle, accelX));

        assertTrue(service.rebuildLatestRelease(2002L));

        verify(riskMetricCatalogService).publishFromReleasedContracts(
                eq(2002L),
                eq(null),
                eq(List.of(crackValue, tiltAngle, accelX)),
                eq(Set.of("L1_LF_1.value", "L1_QJ_1.angle", "L1_JS_1.gX"))
        );
    }

    private ProductModel productModel(Long id, Long productId, String identifier, String modelName) {
        ProductModel model = new ProductModel();
        model.setId(id);
        model.setProductId(productId);
        model.setModelType("property");
        model.setIdentifier(identifier);
        model.setModelName(modelName);
        model.setDeleted(0);
        return model;
    }
}
