package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.Product;
import java.util.List;
import java.util.Locale;

/**
 * 采集器父产品治理边界策略。
 */
final class CollectorChildMetricBoundaryPolicy {

    private static final int COLLECTOR_NODE_TYPE = 2;
    private static final String DEVICE_STRUCTURE_COMPOSITE = "composite";
    private static final String SAMPLE_TYPE_STATUS = "status";
    private static final String STATUS_PREFIX = "S1_ZT_1.";
    private static final String PARENT_SENSOR_STATE_PREFIX = "S1_ZT_1.sensor_state.";

    boolean applies(Product product, String deviceStructure) {
        return product != null
                && Integer.valueOf(COLLECTOR_NODE_TYPE).equals(product.getNodeType())
                && DEVICE_STRUCTURE_COMPOSITE.equals(normalizeKeyword(deviceStructure));
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
}
