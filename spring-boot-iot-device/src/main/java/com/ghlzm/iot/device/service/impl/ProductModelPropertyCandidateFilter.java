package com.ghlzm.iot.device.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 属性候选过滤器，负责 candidate zone 的已知别名归一。
 */
final class ProductModelPropertyCandidateFilter {

    private static final Map<String, String> KNOWN_IDENTIFIER_ALIASES = Map.of(
            "singal_nb", "signal_NB",
            "singal_db", "signal_db"
    );

    NormalizedPropertyIdentifier normalizeIdentifier(String identifier) {
        if (identifier == null) {
            return new NormalizedPropertyIdentifier(null, List.of());
        }
        String trimmed = identifier.trim();
        if (trimmed.isEmpty()) {
            return new NormalizedPropertyIdentifier(null, List.of());
        }
        int separatorIndex = trimmed.lastIndexOf('.');
        String prefix = separatorIndex >= 0 ? trimmed.substring(0, separatorIndex + 1) : "";
        String tail = separatorIndex >= 0 ? trimmed.substring(separatorIndex + 1) : trimmed;
        String normalizedTail = KNOWN_IDENTIFIER_ALIASES.getOrDefault(tail.toLowerCase(Locale.ROOT), tail);
        String normalizedIdentifier = prefix + normalizedTail;
        if (normalizedIdentifier.equals(trimmed)) {
            return new NormalizedPropertyIdentifier(trimmed, List.of());
        }
        return new NormalizedPropertyIdentifier(normalizedIdentifier, List.of(trimmed));
    }

    record NormalizedPropertyIdentifier(String identifier, List<String> rawIdentifiers) {
    }
}
