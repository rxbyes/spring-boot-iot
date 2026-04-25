package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultRiskMetricCatalogPublishRuleTest {

    private final DefaultRiskMetricCatalogPublishRule rule = new DefaultRiskMetricCatalogPublishRule();

    @Test
    void resolveRiskEnabledIdentifiersShouldPublishCrackValueOnly() {
        Product product = product("phase1-crack-p1", "裂缝产品");
        ProductModel value = productModel("value", "裂缝监测值");
        ProductModel sensorState = productModel("sensor_state");

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                product,
                "phase1-crack",
                null,
                List.of(value, sensorState)
        );

        assertEquals(Set.of("value"), identifiers);
    }

    @Test
    void resolveRiskEnabledIdentifiersShouldPublishOnlyGnssTotals() {
        Device device = device("北斗GNSS位移计-L1");
        ProductModel gpsInitial = productModel("gpsInitial");
        ProductModel gpsTotalX = productModel("gpsTotalX");
        ProductModel gpsTotalY = productModel("gpsTotalY");
        ProductModel gpsTotalZ = productModel("gpsTotalZ");

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                device,
                List.of(gpsInitial, gpsTotalX, gpsTotalY, gpsTotalZ)
        );

        assertEquals(Set.of("gpsTotalX", "gpsTotalY", "gpsTotalZ"), identifiers);
    }

    @Test
    void resolveRiskEnabledIdentifiersShouldPublishOnlyDeepDisplacementMetrics() {
        Product product = product("nf-monitor-deep-displacement-v1", "深部位移产品");
        ProductModel dispsX = productModel("dispsX");
        ProductModel dispsY = productModel("dispsY");
        ProductModel sensorState = productModel("sensor_state");

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                product,
                "phase3-deep-displacement",
                null,
                List.of(dispsX, dispsY, sensorState)
        );

        assertEquals(Set.of("dispsX", "dispsY"), identifiers);
    }

    @Test
    void resolveRiskEnabledIdentifiersShouldPublishOnlyRainGaugeCurrentValue() {
        Product product = product("nf-monitor-tipping-bucket-rain-gauge-v1", "翻斗式雨量计");
        ProductModel value = productModel("value", "当前雨量");
        ProductModel totalValue = productModel("totalValue", "累计雨量");

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                product,
                "phase4-rain-gauge",
                null,
                List.of(value, totalValue)
        );

        assertEquals(Set.of("value"), identifiers);
    }

    @Test
    void resolveRiskEnabledIdentifiersShouldPublishOnlyCrackMetricFromMultiDisplacementFullPath() {
        ProductModel crackValue = productModel("L1_LF_1.value");
        ProductModel tiltAngle = productModel("L1_QJ_1.angle");
        ProductModel accelX = productModel("L1_JS_1.gX");

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                null,
                List.of(crackValue, tiltAngle, accelX)
        );

        assertEquals(Set.of("L1_LF_1.value"), identifiers);
    }

    @Test
    void resolveRiskEnabledIdentifiersShouldPublishOnlyGnssTotalFullPathMetrics() {
        ProductModel gpsInitial = productModel("L1_GP_1.gpsInitial");
        ProductModel gpsTotalX = productModel("L1_GP_1.gpsTotalX");
        ProductModel gpsTotalY = productModel("L1_GP_1.gpsTotalY");
        ProductModel gpsTotalZ = productModel("L1_GP_1.gpsTotalZ");
        ProductModel accelX = productModel("L1_JS_1.gX");

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                null,
                List.of(gpsInitial, gpsTotalX, gpsTotalY, gpsTotalZ, accelX)
        );

        assertEquals(Set.of("L1_GP_1.gpsTotalX", "L1_GP_1.gpsTotalY", "L1_GP_1.gpsTotalZ"), identifiers);
    }

    @Test
    void resolveRiskEnabledIdentifiersShouldNotPublishGenericValueWithoutRiskContext() {
        ProductModel genericValue = productModel("value", "监测值");
        ProductModel sensorState = productModel("sensor_state", "设备状态");
        ProductModel totalValue = productModel("totalValue", "累计值");

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                product("generic-monitor-v1", "通用监测产品"),
                null,
                null,
                List.of(genericValue, sensorState, totalValue)
        );

        assertEquals(Set.of(), identifiers);
    }

    @Test
    void resolveRiskEnabledIdentifiersShouldNotPublishShortGnssTotalsWithoutGnssContext() {
        ProductModel gpsTotalX = productModel("gpsTotalX");
        ProductModel gpsTotalY = productModel("gpsTotalY");
        ProductModel gpsTotalZ = productModel("gpsTotalZ");

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                product("generic-monitor-v1", "通用监测产品"),
                null,
                null,
                List.of(gpsTotalX, gpsTotalY, gpsTotalZ)
        );

        assertEquals(Set.of(), identifiers);
    }

    @Test
    void resolveRiskEnabledIdentifiersShouldNotPublishBareLogicalCrackChannelWithoutLeaf() {
        ProductModel bareCrackChannel = productModel("L1_LF_1");
        ProductModel crackValue = productModel("L1_LF_1.value");

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                product("zhd-monitor-multi-displacement-v1", "多维检测仪"),
                null,
                null,
                List.of(bareCrackChannel, crackValue)
        );

        assertEquals(Set.of("L1_LF_1.value"), identifiers);
    }

    private Product product(String productKey, String productName) {
        Product product = new Product();
        product.setProductKey(productKey);
        product.setProductName(productName);
        return product;
    }

    private Device device(String deviceName) {
        Device device = new Device();
        device.setDeviceName(deviceName);
        return device;
    }

    private ProductModel productModel(String identifier, String modelName) {
        ProductModel productModel = productModel(identifier);
        productModel.setModelName(modelName);
        return productModel;
    }

    private ProductModel productModel(String identifier) {
        ProductModel productModel = new ProductModel();
        productModel.setIdentifier(identifier);
        return productModel;
    }
}
