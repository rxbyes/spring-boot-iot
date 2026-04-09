package com.ghlzm.iot.alarm.service.spi;

import com.ghlzm.iot.device.entity.Product;

/**
 * Risk metric scenario resolver SPI.
 */
@FunctionalInterface
public interface RiskMetricScenarioResolver {

    /**
     * Resolve scenario code for current product. Return null when not matched.
     */
    String resolveScenarioCode(Product product);
}
