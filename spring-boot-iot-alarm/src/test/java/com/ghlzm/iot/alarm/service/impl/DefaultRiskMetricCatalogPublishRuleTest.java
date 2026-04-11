package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.device.entity.ProductModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultRiskMetricCatalogPublishRuleTest {

    private final DefaultRiskMetricCatalogPublishRule rule = new DefaultRiskMetricCatalogPublishRule();

    @Test
    void resolveRiskEnabledIdentifiersShouldPublishCrackValueOnly() {
        ProductModel value = productModel("value");
        ProductModel sensorState = productModel("sensor_state");

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(null, List.of(value, sensorState));

        assertEquals(Set.of("value"), identifiers);
    }

    @Test
    void resolveRiskEnabledIdentifiersShouldPublishOnlyGnssTotals() {
        ProductModel gpsInitial = productModel("gpsInitial");
        ProductModel gpsTotalX = productModel("gpsTotalX");
        ProductModel gpsTotalY = productModel("gpsTotalY");
        ProductModel gpsTotalZ = productModel("gpsTotalZ");

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                null,
                List.of(gpsInitial, gpsTotalX, gpsTotalY, gpsTotalZ)
        );

        assertEquals(Set.of("gpsTotalX", "gpsTotalY", "gpsTotalZ"), identifiers);
    }

    @Test
    void resolveRiskEnabledIdentifiersShouldPublishOnlyDeepDisplacementMetrics() {
        ProductModel dispsX = productModel("dispsX");
        ProductModel dispsY = productModel("dispsY");
        ProductModel sensorState = productModel("sensor_state");

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                null,
                List.of(dispsX, dispsY, sensorState)
        );

        assertEquals(Set.of("dispsX", "dispsY"), identifiers);
    }

    @Test
    void resolveRiskEnabledIdentifiersShouldPublishOnlyRainGaugeCurrentValue() {
        ProductModel value = productModel("value");
        ProductModel totalValue = productModel("totalValue");

        Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
                null,
                List.of(value, totalValue)
        );

        assertEquals(Set.of("value"), identifiers);
    }

    private ProductModel productModel(String identifier) {
        ProductModel productModel = new ProductModel();
        productModel.setIdentifier(identifier);
        return productModel;
    }
}
