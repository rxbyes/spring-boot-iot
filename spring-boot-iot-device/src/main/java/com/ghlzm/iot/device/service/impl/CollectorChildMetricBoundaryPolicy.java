package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.Product;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 采集器父产品治理边界策略。
 */
final class CollectorChildMetricBoundaryPolicy {

    private static final int COLLECTOR_NODE_TYPE = 2;
    private static final String DEVICE_STRUCTURE_COMPOSITE = "composite";
    private static final String SAMPLE_TYPE_STATUS = "status";
    private static final String STATUS_PREFIX = "S1_ZT_1.";
    private static final String PARENT_SENSOR_STATE_PREFIX = "S1_ZT_1.sensor_state.";
    private static final Set<String> COLLECTOR_PRODUCT_KEY_HINTS = Set.of(
            "collector",
            "collect-rtu",
            "collect_rtu"
    );
    private static final Set<String> CHILD_OWNED_TELEMETRY_SEGMENTS = Set.of(
            "angle",
            "value",
            "gx",
            "gy",
            "gz",
            "x",
            "y",
            "z",
            "azi",
            "gpsinitial",
            "gpstotalx",
            "gpstotaly",
            "gpstotalz",
            "gpssinglex",
            "gpssingley",
            "gpssinglez",
            "dispsx",
            "dispsy"
    );

    boolean applies(Product product, String deviceStructure) {
        return appliesToProduct(product)
                && DEVICE_STRUCTURE_COMPOSITE.equals(normalizeKeyword(deviceStructure));
    }

    boolean appliesToProduct(Product product) {
        if (product == null) {
            return false;
        }
        if (Integer.valueOf(COLLECTOR_NODE_TYPE).equals(product.getNodeType())) {
            return true;
        }
        return matchesCollectorProductKey(product.getProductKey())
                || matchesCollectorProductLabel(product.getProductName())
                || matchesCollectorProductLabel(product.getDescription());
    }

    boolean shouldKeepLeaf(String sampleType, String rawIdentifier, List<String> logicalChannelCodes) {
        String normalizedIdentifier = normalizeText(rawIdentifier);
        if (normalizedIdentifier == null) {
            return false;
        }
        return !isChildBusinessIdentifier(normalizedIdentifier, logicalChannelCodes)
                && !isChildSensorState(sampleType, normalizedIdentifier, logicalChannelCodes);
    }

    String toCollectorIdentifier(String sampleType, String rawIdentifier) {
        String normalizedIdentifier = normalizeText(rawIdentifier);
        if (normalizedIdentifier == null) {
            return null;
        }
        if (SAMPLE_TYPE_STATUS.equals(normalizeKeyword(sampleType))
                && normalizedIdentifier.startsWith(STATUS_PREFIX)
                && !normalizedIdentifier.startsWith(PARENT_SENSOR_STATE_PREFIX)) {
            return normalizeText(normalizedIdentifier.substring(STATUS_PREFIX.length()));
        }
        return normalizedIdentifier;
    }

    boolean isChildOwnedFormalIdentifier(String identifier) {
        String normalizedIdentifier = normalizeText(identifier);
        if (normalizedIdentifier == null) {
            return false;
        }
        String normalizedKeyword = normalizedIdentifier.toLowerCase(Locale.ROOT);
        String lastSegment = lastIdentifierSegment(normalizedKeyword);
        if ("sensor_state".equals(lastSegment) || normalizedKeyword.contains("sensor_state")) {
            return true;
        }
        return CHILD_OWNED_TELEMETRY_SEGMENTS.contains(lastSegment)
                || lastSegment.startsWith("gps")
                || lastSegment.startsWith("disp");
    }

    private boolean isChildBusinessIdentifier(String rawIdentifier, List<String> logicalChannelCodes) {
        if (logicalChannelCodes == null || logicalChannelCodes.isEmpty()) {
            return false;
        }
        for (String logicalChannelCode : logicalChannelCodes) {
            String normalizedChannelCode = normalizeText(logicalChannelCode);
            if (normalizedChannelCode == null) {
                continue;
            }
            if (rawIdentifier.equals(normalizedChannelCode) || rawIdentifier.startsWith(normalizedChannelCode + ".")) {
                return true;
            }
        }
        return false;
    }

    private boolean isChildSensorState(String sampleType, String rawIdentifier, List<String> logicalChannelCodes) {
        if (!SAMPLE_TYPE_STATUS.equals(normalizeKeyword(sampleType))
                || logicalChannelCodes == null
                || logicalChannelCodes.isEmpty()) {
            return false;
        }
        for (String logicalChannelCode : logicalChannelCodes) {
            String normalizedChannelCode = normalizeText(logicalChannelCode);
            if (normalizedChannelCode == null) {
                continue;
            }
            if (rawIdentifier.equals(PARENT_SENSOR_STATE_PREFIX + normalizedChannelCode)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeKeyword(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private boolean matchesCollectorProductKey(String value) {
        String normalized = normalizeKeyword(value);
        if (normalized == null) {
            return false;
        }
        return COLLECTOR_PRODUCT_KEY_HINTS.stream().anyMatch(normalized::contains);
    }

    private boolean matchesCollectorProductLabel(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return false;
        }
        return normalized.contains("采集器")
                || (normalized.contains("采集") && normalized.contains("终端"));
    }

    private String lastIdentifierSegment(String identifier) {
        int separatorIndex = identifier.lastIndexOf('.');
        if (separatorIndex < 0 || separatorIndex == identifier.length() - 1) {
            return identifier;
        }
        return identifier.substring(separatorIndex + 1);
    }
}
