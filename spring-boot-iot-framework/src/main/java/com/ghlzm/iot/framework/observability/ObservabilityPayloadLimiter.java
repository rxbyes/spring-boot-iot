package com.ghlzm.iot.framework.observability;

import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量证据表 payload 治理器，统一负责脱敏、限项与限长。
 */
public final class ObservabilityPayloadLimiter {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();
    private static final int MAX_STRING_LENGTH = 255;
    private static final int MAX_OBJECT_ENTRIES = 30;
    private static final int MAX_ARRAY_ENTRIES = 10;
    private static final int MAX_SERIALIZED_LENGTH = 2000;
    private static final int MAX_KEY_LENGTH = 64;
    private static final int MAX_DEPTH = 5;
    private static final int FALLBACK_PREVIEW_ENTRIES = 5;
    private static final String TRUNCATED_SUFFIX = "...(truncated)";
    private static final String TRUNCATED_KEY = "_truncated";

    private ObservabilityPayloadLimiter() {
    }

    public static String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            Object normalized = normalizeValue(value, 0);
            String json = OBJECT_MAPPER.writeValueAsString(normalized);
            if (json.length() <= MAX_SERIALIZED_LENGTH) {
                return json;
            }
            return OBJECT_MAPPER.writeValueAsString(buildSerializedFallback(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object normalizeValue(Object value, int depth) {
        if (value == null) {
            return null;
        }
        if (depth >= MAX_DEPTH) {
            return TRUNCATED_SUFFIX;
        }
        if (value instanceof Map<?, ?> mapValue) {
            return normalizeMap(mapValue, depth + 1);
        }
        if (value instanceof Iterable<?> iterableValue) {
            return normalizeIterable(iterableValue, depth + 1);
        }
        if (value.getClass().isArray()) {
            return normalizeArray(value, depth + 1);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return truncateText(SensitiveLogSanitizer.sanitize(String.valueOf(value)), MAX_STRING_LENGTH);
    }

    private static Map<String, Object> normalizeMap(Map<?, ?> source, int depth) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        int index = 0;
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (index >= MAX_OBJECT_ENTRIES) {
                normalized.put(TRUNCATED_KEY, TRUNCATED_SUFFIX);
                break;
            }
            String key = truncateText(SensitiveLogSanitizer.sanitize(String.valueOf(entry.getKey())), MAX_KEY_LENGTH);
            String normalizedKey = StringUtils.hasText(key) ? key : "unknown";
            normalized.put(normalizedKey, normalizeMapValue(normalizedKey, entry.getValue(), depth));
            index++;
        }
        return normalized;
    }

    private static List<Object> normalizeIterable(Iterable<?> source, int depth) {
        List<Object> normalized = new ArrayList<>();
        int index = 0;
        for (Object item : source) {
            if (index >= MAX_ARRAY_ENTRIES) {
                normalized.add(TRUNCATED_SUFFIX);
                break;
            }
            normalized.add(normalizeValue(item, depth));
            index++;
        }
        return normalized;
    }

    private static List<Object> normalizeArray(Object array, int depth) {
        List<Object> normalized = new ArrayList<>();
        int length = Array.getLength(array);
        for (int index = 0; index < length; index++) {
            if (index >= MAX_ARRAY_ENTRIES) {
                normalized.add(TRUNCATED_SUFFIX);
                break;
            }
            normalized.add(normalizeValue(Array.get(array, index), depth));
        }
        return normalized;
    }

    private static Map<String, Object> buildSerializedFallback(Map<String, Object> source) {
        Map<String, Object> fallback = new LinkedHashMap<>();
        int index = 0;
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (index >= FALLBACK_PREVIEW_ENTRIES) {
                break;
            }
            String key = truncateText(SensitiveLogSanitizer.sanitize(String.valueOf(entry.getKey())), MAX_KEY_LENGTH);
            String normalizedKey = StringUtils.hasText(key) ? key : "unknown";
            fallback.put(normalizedKey, previewValue(normalizedKey, entry.getValue()));
            index++;
        }
        fallback.put(TRUNCATED_KEY, TRUNCATED_SUFFIX);
        return fallback;
    }

    private static Object normalizeMapValue(String key, Object value, int depth) {
        if (SensitiveLogSanitizer.isSensitiveKey(key)) {
            return "***";
        }
        return normalizeValue(value, depth);
    }

    private static Object previewValue(String key, Object value) {
        if (SensitiveLogSanitizer.isSensitiveKey(key)) {
            return "***";
        }
        if (value == null || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return truncateText(SensitiveLogSanitizer.sanitize(String.valueOf(value)), MAX_STRING_LENGTH);
    }

    private static String truncateText(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return text;
        }
        if (maxLength <= TRUNCATED_SUFFIX.length()) {
            return text.substring(0, maxLength);
        }
        return text.substring(0, maxLength - TRUNCATED_SUFFIX.length()) + TRUNCATED_SUFFIX;
    }
}
