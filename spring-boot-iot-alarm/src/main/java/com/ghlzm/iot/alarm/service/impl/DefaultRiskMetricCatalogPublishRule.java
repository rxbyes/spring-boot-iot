package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.service.RiskMetricCatalogPublishRule;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 风险指标目录发布规则默认实现。
 */
@Component
public class DefaultRiskMetricCatalogPublishRule implements RiskMetricCatalogPublishRule {

    /**
     * 风险闭环只发布主监测值，状态、初始值和倾角/加速度等治理字段不进入该目录。
     */
    private static final Set<String> GNSS_TOTAL_IDENTIFIERS = Set.of("gpsTotalX", "gpsTotalY", "gpsTotalZ");
    private static final Set<String> DEEP_DISPLACEMENT_IDENTIFIERS = Set.of("dispsX", "dispsY");
    private static final Pattern LOGICAL_CHANNEL_PATTERN = Pattern.compile("(?i)^(L\\d+)_([A-Z]+)_(\\d+)$");

    @Override
    public Set<String> resolveRiskEnabledIdentifiers(Device device, List<ProductModel> releasedContracts) {
        return resolveRiskEnabledIdentifiers(null, null, device, releasedContracts);
    }

    @Override
    public Set<String> resolveRiskEnabledIdentifiers(Product product,
                                                     String scenarioCode,
                                                     Device device,
                                                     List<ProductModel> releasedContracts) {
        if (releasedContracts == null || releasedContracts.isEmpty()) {
            return Set.of();
        }
        Map<String, ProductModel> releasedByIdentifier = collectReleasedByIdentifier(releasedContracts);
        if (releasedByIdentifier.isEmpty()) {
            return Set.of();
        }
        PublishContext context = PublishContext.from(product, scenarioCode, device);
        Set<String> enabledIdentifiers = new LinkedHashSet<>();
        for (Map.Entry<String, ProductModel> entry : releasedByIdentifier.entrySet()) {
            String identifier = entry.getKey();
            if (isRiskReadyIdentifier(identifier, context, entry.getValue())) {
                enabledIdentifiers.add(identifier);
            }
        }
        return enabledIdentifiers;
    }

    private Map<String, ProductModel> collectReleasedByIdentifier(List<ProductModel> releasedContracts) {
        Map<String, ProductModel> normalizedByIdentifier = new LinkedHashMap<>();
        for (ProductModel contract : releasedContracts) {
            String identifier = normalize(contract == null ? null : contract.getIdentifier());
            if (identifier != null) {
                normalizedByIdentifier.putIfAbsent(identifier, contract);
            }
        }
        return normalizedByIdentifier;
    }

    private boolean isRiskReadyIdentifier(String identifier, PublishContext context, ProductModel contract) {
        ParsedIdentifier parsed = ParsedIdentifier.parse(identifier);
        if (parsed == null) {
            return false;
        }
        if ("L1".equals(parsed.level()) && "LF".equals(parsed.monitorType())) {
            return "value".equals(parsed.leaf());
        }
        if ("L1".equals(parsed.level()) && "GP".equals(parsed.monitorType())) {
            return GNSS_TOTAL_IDENTIFIERS.contains(parsed.leaf());
        }
        String original = parsed.original();
        if (DEEP_DISPLACEMENT_IDENTIFIERS.contains(original)) {
            return context.matchesDeepDisplacement(contract);
        }
        if (GNSS_TOTAL_IDENTIFIERS.contains(original)) {
            return context.matchesGnss(contract);
        }
        if ("value".equals(original)) {
            return context.matchesSingleValueRisk(contract);
        }
        return false;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record PublishContext(String productKey, String productName, String scenarioCode, String deviceName) {

        private static PublishContext from(Product product,
                                           String scenarioCode,
                                           Device device) {
            return new PublishContext(
                    normalizeValue(product == null ? null : product.getProductKey()),
                    normalizeValue(product == null ? null : product.getProductName()),
                    normalizeValue(scenarioCode),
                    normalizeValue(device == null ? null : device.getDeviceName())
            );
        }

        private boolean matchesSingleValueRisk(ProductModel contract) {
            return matchesCrack(contract) || matchesLaser(contract) || matchesRain(contract);
        }

        private boolean matchesCrack(ProductModel contract) {
            return matchesAny(contract, "phase1-crack", "crack", "裂缝");
        }

        private boolean matchesLaser(ProductModel contract) {
            return matchesAny(contract, "laser-rangefinder", "laser_rangefinder", "laser", "激光", "测距");
        }

        private boolean matchesRain(ProductModel contract) {
            return matchesAny(contract, "phase4-rain-gauge", "rain-gauge", "rain_gauge", "rain", "雨量");
        }

        private boolean matchesGnss(ProductModel contract) {
            return matchesAny(contract, "phase2-gnss", "gnss", "北斗");
        }

        private boolean matchesDeepDisplacement(ProductModel contract) {
            return matchesAny(contract, "phase3-deep-displacement", "deep-displacement", "deep_displacement", "深部位移");
        }

        private boolean matchesAny(ProductModel contract, String... tokens) {
            String blob = String.join(" ",
                    safeLower(productKey),
                    safeLower(productName),
                    safeLower(scenarioCode),
                    safeLower(deviceName),
                    safeLower(contract == null ? null : contract.getModelName()),
                    safeLower(contract == null ? null : contract.getDescription())
            );
            for (String token : tokens) {
                if (StringUtils.hasText(token) && blob.contains(token.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            return false;
        }

        private static String normalizeValue(String value) {
            return StringUtils.hasText(value) ? value.trim() : null;
        }

        private static String safeLower(String value) {
            return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
        }
    }

    private record ParsedIdentifier(String original, String level, String monitorType, String channelNo, String leaf) {

        private static ParsedIdentifier parse(String identifier) {
            if (!StringUtils.hasText(identifier)) {
                return null;
            }
            String original = identifier.trim();
            String prefix = null;
            String leaf = original;
            int dotIndex = original.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < original.length() - 1) {
                prefix = original.substring(0, dotIndex);
                leaf = original.substring(dotIndex + 1);
            } else if (isLogicalChannel(original)) {
                prefix = original;
                leaf = null;
            }
            if (!StringUtils.hasText(prefix)) {
                return new ParsedIdentifier(original, null, null, null, leaf);
            }
            Matcher matcher = LOGICAL_CHANNEL_PATTERN.matcher(prefix);
            if (!matcher.matches()) {
                return new ParsedIdentifier(original, null, null, null, leaf);
            }
            return new ParsedIdentifier(
                    original,
                    matcher.group(1).toUpperCase(Locale.ROOT),
                    matcher.group(2).toUpperCase(Locale.ROOT),
                    matcher.group(3),
                    leaf
            );
        }

        private static boolean isLogicalChannel(String value) {
            return StringUtils.hasText(value) && LOGICAL_CHANNEL_PATTERN.matcher(value.trim()).matches();
        }
    }
}
