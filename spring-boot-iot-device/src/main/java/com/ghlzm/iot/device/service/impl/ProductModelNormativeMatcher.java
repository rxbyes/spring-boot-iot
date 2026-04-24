package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.entity.Product;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * 产品物模型规范匹配器。
 */
final class ProductModelNormativeMatcher {
    private static final Pattern POINT_PREFIX_PATTERN = Pattern.compile("^([LS]\\d+)_([A-Za-z]+)_\\d+$");

    static final String SCENARIO_PHASE1_CRACK = "phase1-crack";
    static final String SCENARIO_PHASE2_GNSS = "phase2-gnss";
    static final String SCENARIO_PHASE3_DEEP_DISPLACEMENT = "phase3-deep-displacement";
    static final String SCENARIO_PHASE3_WATER_SURFACE = "phase3-water-surface";
    static final String SCENARIO_PHASE4_RAIN_GAUGE = "phase4-rain-gauge";
    static final String SCENARIO_PHASE5_MUD_LEVEL = "phase5-mud-level";
    static final String SCENARIO_PHASE6_RADAR = "phase6-radar";

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
        if (matchesWaterSurface(product.getProductKey())
                || matchesWaterSurface(product.getProductName())
                || matchesWaterSurface(product.getManufacturer())
                || matchesWaterSurface(product.getDescription())) {
            return SCENARIO_PHASE3_WATER_SURFACE;
        }
        if (matchesRainGauge(product.getProductKey())
                || matchesRainGauge(product.getProductName())
                || matchesRainGauge(product.getManufacturer())
                || matchesRainGauge(product.getDescription())) {
            return SCENARIO_PHASE4_RAIN_GAUGE;
        }
        if (matchesMudLevel(product.getProductKey())
                || matchesMudLevel(product.getProductName())
                || matchesMudLevel(product.getManufacturer())
                || matchesMudLevel(product.getDescription())) {
            return SCENARIO_PHASE5_MUD_LEVEL;
        }
        if (matchesRadar(product.getProductKey())
                || matchesRadar(product.getProductName())
                || matchesRadar(product.getManufacturer())
                || matchesRadar(product.getDescription())) {
            return SCENARIO_PHASE6_RADAR;
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

    NormativeMatchResult matchPropertyByRawIdentifier(String canonicalIdentifier,
                                                      List<String> rawIdentifiers,
                                                      List<NormativeMetricDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return null;
        }
        List<String> candidates = rawIdentifiers == null || rawIdentifiers.isEmpty()
                ? (StringUtils.hasText(canonicalIdentifier) ? List.of(canonicalIdentifier) : List.of())
                : rawIdentifiers;
        for (String candidate : candidates) {
            ParsedRawIdentifier parsed = parseRawIdentifier(candidate);
            if (parsed == null) {
                continue;
            }
            NormativeMetricDefinition definition = findByMonitorCodesAndIdentifier(
                    definitions,
                    parsed.monitorContentCode(),
                    parsed.monitorTypeCode(),
                    parsed.leafIdentifier(),
                    canonicalIdentifier
            );
            if (definition == null) {
                continue;
            }
            return new NormativeMatchResult(
                    definition.getIdentifier(),
                    definition.getDisplayName(),
                    Integer.valueOf(1).equals(definition.getRiskEnabled()),
                    rawIdentifiers == null ? List.of() : rawIdentifiers
            );
        }
        return null;
    }

    private NormativeMetricDefinition findByMonitorCodesAndIdentifier(List<NormativeMetricDefinition> definitions,
                                                                      String monitorContentCode,
                                                                      String monitorTypeCode,
                                                                      String leafIdentifier,
                                                                      String canonicalIdentifier) {
        String normalizedLeaf = normalizeLower(leafIdentifier);
        String normalizedCanonical = normalizeLower(canonicalIdentifier);
        for (NormativeMetricDefinition definition : definitions) {
            if (definition == null
                    || !equalsIgnoreCase(definition.getMonitorContentCode(), monitorContentCode)
                    || !equalsIgnoreCase(definition.getMonitorTypeCode(), monitorTypeCode)) {
                continue;
            }
            String normalizedDefinitionIdentifier = normalizeLower(definition.getIdentifier());
            if (normalizedDefinitionIdentifier == null) {
                continue;
            }
            if (normalizedLeaf != null && normalizedLeaf.equals(normalizedDefinitionIdentifier)) {
                return definition;
            }
            if (normalizedCanonical != null && normalizedCanonical.equals(normalizedDefinitionIdentifier)) {
                return definition;
            }
        }
        return null;
    }

    private ParsedRawIdentifier parseRawIdentifier(String rawIdentifier) {
        if (!StringUtils.hasText(rawIdentifier)) {
            return null;
        }
        String trimmed = rawIdentifier.trim();
        int dotIndex = trimmed.indexOf('.');
        String prefix = dotIndex >= 0 ? trimmed.substring(0, dotIndex) : trimmed;
        String leaf = dotIndex >= 0 ? trimmed.substring(dotIndex + 1) : null;
        if (!StringUtils.hasText(leaf)) {
            leaf = "value";
        }
        Matcher matcher = POINT_PREFIX_PATTERN.matcher(prefix);
        if (!matcher.matches()) {
            return null;
        }
        String contentCode = normalizeUpper(matcher.group(1));
        String typeCode = normalizeUpper(matcher.group(2));
        return new ParsedRawIdentifier(contentCode, typeCode, leaf);
    }

    private String normalizeLower(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUpper(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        String normalizedLeft = normalizeUpper(left);
        String normalizedRight = normalizeUpper(right);
        return normalizedLeft != null && normalizedLeft.equals(normalizedRight);
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

    private boolean matchesMudLevel(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("mud-level")
                || normalized.contains("mud_level")
                || normalized.contains("south_mud_level")
                || normalized.contains("_nw_")
                || normalized.contains("-nw-")
                || value.contains("泥位")
                || value.contains("泥水位");
    }

    private boolean matchesWaterSurface(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("water-surface")
                || normalized.contains("water_surface")
                || normalized.contains("surface-water")
                || normalized.contains("surface_water")
                || value.contains("地表水位")
                || value.contains("地表水");
    }

    private boolean matchesRadar(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("radar")
                || normalized.contains("water-radar")
                || normalized.contains("water_radar")
                || value.contains("雷达");
    }

    record NormativeMatchResult(
            String normativeIdentifier,
            String normativeName,
            boolean riskReady,
            List<String> rawIdentifiers
    ) {
    }

    private record ParsedRawIdentifier(String monitorContentCode,
                                       String monitorTypeCode,
                                       String leafIdentifier) {
    }
}
