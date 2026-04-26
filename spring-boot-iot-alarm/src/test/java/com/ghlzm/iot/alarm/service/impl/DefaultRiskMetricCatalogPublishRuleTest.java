package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultRiskMetricCatalogPublishRuleTest {

    private final DefaultRiskMetricCatalogPublishRule rule = new DefaultRiskMetricCatalogPublishRule();

    @Test
    void resolveRiskEnabledIdentifiersShouldUseMeasureEntriesFromProductMetadataOnly() {
        Product product = product("zhd-monitor-multi-displacement-v1", "多维检测仪");
        product.setMetadataJson("""
                {
                  "objectInsight": {
                    "customMetrics": [
                      {"identifier":"L1_LF_1.value","displayName":"裂缝量","group":"measure","enabled":true,"includeInTrend":true},
                      {"identifier":"L1_QJ_1.angle","displayName":"倾角","group":"measure","enabled":true,"includeInTrend":true},
                      {"identifier":"L1_JS_1.gX","displayName":"加速度X","group":"measure","enabled":true,"includeInTrend":true},
                      {"identifier":"S1_ZT_1.sensor_state","displayName":"设备状态","group":"status","enabled":true,"includeInTrend":true}
                    ]
                  }
                }
                """);

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                product,
                null,
                null,
                List.of(
                        productModel("L1_LF_1.value"),
                        productModel("L1_QJ_1.angle"),
                        productModel("L1_JS_1.gX"),
                        productModel("S1_ZT_1.sensor_state")
                )
        );

        assertEquals(Set.of("L1_LF_1.value", "L1_QJ_1.angle", "L1_JS_1.gX"), identifiers);
    }

    @Test
    void resolveRiskEnabledIdentifiersShouldIgnoreEntriesNotMarkedAsMeasureTruth() {
        Product product = product("generic-monitor-v1", "通用监测产品");
        product.setMetadataJson("""
                {
                  "objectInsight": {
                    "customMetrics": [
                      {"identifier":"L1_LF_1.value","displayName":"裂缝量","group":"measure","enabled":true,"includeInTrend":false},
                      {"identifier":"L1_QJ_1.angle","displayName":"倾角","group":"runtime","enabled":true,"includeInTrend":true},
                      {"identifier":"L1_JS_1.gX","displayName":"加速度X","group":"status","enabled":true,"includeInTrend":true}
                    ]
                  }
                }
                """);

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                product,
                null,
                null,
                List.of(
                        productModel("L1_LF_1.value"),
                        productModel("L1_QJ_1.angle"),
                        productModel("L1_JS_1.gX")
                )
        );

        assertEquals(Set.of(), identifiers);
    }

    @Test
    void resolveRiskEnabledIdentifiersShouldIntersectMeasureTruthWithReleasedContractsOnly() {
        Product product = product("generic-monitor-v1", "通用监测产品");
        product.setMetadataJson("""
                {
                  "objectInsight": {
                    "customMetrics": [
                      {"identifier":"L1_LF_1.value","displayName":"裂缝量","group":"measure","enabled":true,"includeInTrend":true},
                      {"identifier":"L1_QJ_1.angle","displayName":"倾角","group":"measure","enabled":true,"includeInTrend":true}
                    ]
                  }
                }
                """);

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                product,
                null,
                null,
                List.of(productModel("L1_LF_1.value"))
        );

        assertEquals(Set.of("L1_LF_1.value"), identifiers);
    }

    @Test
    void resolveRiskEnabledIdentifiersShouldReturnEmptyWhenProductMetadataMissing() {
        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                product("generic-monitor-v1", "通用监测产品"),
                null,
                null,
                List.of(productModel("L1_LF_1.value"))
        );

        assertEquals(Set.of(), identifiers);
    }

    private Product product(String productKey, String productName) {
        Product product = new Product();
        product.setProductKey(productKey);
        product.setProductName(productName);
        return product;
    }

    private ProductModel productModel(String identifier) {
        ProductModel productModel = new ProductModel();
        productModel.setModelType("property");
        productModel.setIdentifier(identifier);
        return productModel;
    }
}
