package com.ghlzm.iot.alarm.service.spi;

import com.ghlzm.iot.device.entity.Product;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Built-in keyword-based risk metric scenario resolver.
 */
@Component
public class KeywordRiskMetricScenarioResolver implements RiskMetricScenarioResolver {

    private static final String SCENARIO_PHASE1_CRACK = "phase1-crack";
    private static final String SCENARIO_PHASE2_GNSS = "phase2-gnss";

    @Override
    public String resolveScenarioCode(Product product) {
        if (product == null) {
            return null;
        }
        if (matchesGnss(product.getProductKey())
                || matchesGnss(product.getProductName())
                || matchesGnss(product.getManufacturer())
                || matchesGnss(product.getDescription())) {
            return SCENARIO_PHASE2_GNSS;
        }
        if (matchesCrack(product.getProductKey())
                || matchesCrack(product.getProductName())
                || matchesCrack(product.getManufacturer())
                || matchesCrack(product.getDescription())) {
            return SCENARIO_PHASE1_CRACK;
        }
        return null;
    }

    private boolean matchesCrack(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("crack")
                || value.contains("\u88c2\u7f1d")
                || normalized.contains("_lf_")
                || normalized.contains("-lf-")
                || normalized.endsWith("-lf")
                || normalized.startsWith("lf-");
    }

    private boolean matchesGnss(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("gnss")
                || value.contains("\u5317\u6597")
                || value.contains("\u536b\u661f");
    }
}
