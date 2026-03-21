package com.ghlzm.iot.system.service.impl;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 站内消息与帮助文档访问规则的共享工具。
 */
final class SystemContentAccessSupport {

    private static final String CSV_SPLIT_REGEX = "[,，;；\\s]+";

    private SystemContentAccessSupport() {
    }

    static List<String> splitCsv(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split(CSV_SPLIT_REGEX))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    static String normalizeCsv(String raw) {
        return String.join(",", splitCsv(raw));
    }

    static String normalizeUpperCaseCsv(String raw) {
        return String.join(",", splitCsv(raw).stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .distinct()
                .toList());
    }

    static List<String> normalizePathList(String raw) {
        return splitCsv(raw).stream()
                .map(SystemContentAccessSupport::normalizePath)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    static String normalizePathCsv(String raw) {
        return String.join(",", normalizePathList(raw));
    }

    static Set<String> toUpperCaseSet(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    static String normalizePath(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return null;
        }
        String trimmed = rawPath.trim();
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        if (trimmed.length() > 1 && trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    static boolean pathMatches(String configuredPath, String currentPath) {
        String normalizedConfigured = normalizePath(configuredPath);
        String normalizedCurrent = normalizePath(currentPath);
        if (!StringUtils.hasText(normalizedConfigured) || !StringUtils.hasText(normalizedCurrent)) {
            return false;
        }
        if (Objects.equals(normalizedConfigured, normalizedCurrent)) {
            return true;
        }
        return normalizedCurrent.startsWith(normalizedConfigured + "/")
                || normalizedConfigured.startsWith(normalizedCurrent + "/");
    }
}
