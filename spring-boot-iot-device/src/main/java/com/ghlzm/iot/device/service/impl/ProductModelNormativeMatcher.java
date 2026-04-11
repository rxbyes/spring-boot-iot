package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.entity.Product;
import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * 产品物模型规范匹配器。
 */
final class ProductModelNormativeMatcher {

    static final String SCENARIO_PHASE1_CRACK = "phase1-crack";
    static final String SCENARIO_PHASE2_GNSS = "phase2-gnss";
    static final String SCENARIO_PHASE3_DEEP_DISPLACEMENT = "phase3-deep-displacement";
    static final String SCENARIO_PHASE4_RAIN_GAUGE = "phase4-rain-gauge";

    String resolveScenarioCode(Product product) {
        if (product == null) {
            return null;
        }
        if (matchesDeepDisplacement(product.getProductKey())
                || matchesDeepDisplacement(product.getProductName())
                || matchesDeepDisplacement(product.getManufacturer())
                || matchesDeepDisplacement(product.getDescription())) {
            return SCENARIO_PHASE3_DEEP_DISPLACEMENT;
        }
        if (matchesRainGauge(product.getProductKey())
                || matchesRainGauge(product.getProductName())
                || matchesRainGauge(product.getManufacturer())
                || matchesRainGauge(product.getDescription())) {
            return SCENARIO_PHASE4_RAIN_GAUGE;
        }
        if (matchesGnss(product.getProductKey())
                || matchesGnss(product.getProductName())
                || matchesGnss(product.getManufacturer())
                || matchesGnss(product.getDescription())) {
            return SCENARIO_PHASE2_GNSS;
        }
        if (matchesLaser(product.getProductKey())
                || matchesLaser(product.getProductName())
                || matchesLaser(product.getManufacturer())
                || matchesLaser(product.getDescription())) {
            return SCENARIO_PHASE1_CRACK;
        }
        if (matchesCrack(product.getProductKey())
                || matchesCrack(product.getProductName())
                || matchesCrack(product.getManufacturer())
                || matchesCrack(product.getDescription())) {
            return SCENARIO_PHASE1_CRACK;
        }
        return null;
    }

    NormativeMatchResult matchProperty(String canonicalIdentifier,
                                       List<String> rawIdentifiers,
                                       List<NormativeMetricDefinition> definitions) {
        if (!StringUtils.hasText(canonicalIdentifier) || definitions == null || definitions.isEmpty()) {
            return null;
        }
        NormativeMetricDefinition definition = definitions.stream()
                .filter(item -> canonicalIdentifier.equals(item.getIdentifier()))
                .findFirst()
                .orElseThrow(() -> new BizException("未找到规范字段定义: " + canonicalIdentifier));
        return new NormativeMatchResult(
                definition.getIdentifier(),
                definition.getDisplayName(),
                Integer.valueOf(1).equals(definition.getRiskEnabled()),
                rawIdentifiers == null ? List.of() : rawIdentifiers
        );
    }

    private boolean matchesCrack(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("crack")
                || value.contains("裂缝")
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
                || value.contains("北斗")
                || value.contains("卫星");
    }

    private boolean matchesLaser(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("laser-rangefinder")
                || normalized.contains("laser_rangefinder")
                || normalized.contains("south_laser_rangefinder")
                || value.contains("激光")
                || value.contains("测距");
    }

    private boolean matchesDeepDisplacement(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("deep-displacement")
                || normalized.contains("deep_displacement")
                || normalized.contains("south_deep_displacement")
                || value.contains("深部位移");
    }

    private boolean matchesRainGauge(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("rain-gauge")
                || normalized.contains("rain_gauge")
                || normalized.contains("tipping-bucket")
                || normalized.contains("tipping_bucket")
                || normalized.contains("south_rain_gauge")
                || value.contains("翻斗")
                || value.contains("雨量");
    }

    record NormativeMatchResult(
            String normativeIdentifier,
            String normativeName,
            boolean riskReady,
            List<String> rawIdentifiers
    ) {
    }
}
