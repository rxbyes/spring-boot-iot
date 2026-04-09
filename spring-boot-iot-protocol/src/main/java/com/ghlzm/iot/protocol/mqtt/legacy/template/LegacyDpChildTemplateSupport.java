package com.ghlzm.iot.protocol.mqtt.legacy.template;

import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpFamilyResolver;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LegacyDpChildTemplateSupport {

    private static final String PARENT_SENSOR_STATE_PROPERTY_PREFIX = "S1_ZT_1.sensor_state.";

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();
    private static final LegacyDpFamilyResolver FAMILY_RESOLVER = new LegacyDpFamilyResolver();

    private LegacyDpChildTemplateSupport() {
    }

    static LegacyDpLogicalPayloadDescriptor describe(String logicalCode, Object logicalPayload) {
        if (logicalPayload == null) {
            return null;
        }
        String rawPayload = writeLogicalRawPayload(logicalCode, logicalPayload);
        if (logicalPayload instanceof Map<?, ?> logicalMap && isTimestampContainer(logicalMap)) {
            TimestampedValue latestEntry = selectLatestTimestampValue(logicalMap);
            if (latestEntry == null) {
                return null;
            }
            Object latestValue = latestEntry.value();
            return new LegacyDpLogicalPayloadDescriptor(
                    latestEntry.timestamp(),
                    latestValue instanceof Map<?, ?> ? LegacyDpLogicalPayloadShape.TIMESTAMP_OBJECT : LegacyDpLogicalPayloadShape.TIMESTAMP_SCALAR,
                    latestValue,
                    rawPayload
            );
        }
        return new LegacyDpLogicalPayloadDescriptor(
                null,
                logicalPayload instanceof Map<?, ?> ? LegacyDpLogicalPayloadShape.DIRECT_OBJECT : LegacyDpLogicalPayloadShape.DIRECT_SCALAR,
                logicalPayload,
                rawPayload
        );
    }

    static Map<String, Object> flattenObjectProperties(Object value) {
        if (!(value instanceof Map<?, ?> mapValue)) {
            return Map.of();
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        flattenChildProperties("", mapValue, properties);
        return properties;
    }

    static Object resolveSensorState(String logicalCode, Map<String, Object> parentProperties) {
        if (logicalCode == null || logicalCode.isBlank() || parentProperties == null || parentProperties.isEmpty()) {
            return null;
        }
        return parentProperties.get(PARENT_SENSOR_STATE_PROPERTY_PREFIX + logicalCode);
    }

    static String normalizeStrategy(String strategy) {
        return strategy == null ? null : strategy.trim().toUpperCase();
    }

    private static boolean isTimestampContainer(Map<?, ?> source) {
        if (source.isEmpty()) {
            return false;
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !FAMILY_RESOLVER.isTimestampKey(key)) {
                return false;
            }
        }
        return true;
    }

    private static TimestampedValue selectLatestTimestampValue(Map<?, ?> source) {
        List<TimestampedValue> timestampedValues = new ArrayList<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            LocalDateTime timestamp = FAMILY_RESOLVER.parseTimestamp(key);
            if (timestamp != null) {
                timestampedValues.add(new TimestampedValue(key, timestamp, entry.getValue()));
            }
        }
        if (timestampedValues.isEmpty()) {
            return null;
        }
        timestampedValues.sort(Comparator.comparing(TimestampedValue::timestamp).thenComparing(TimestampedValue::key));
        return timestampedValues.get(timestampedValues.size() - 1);
    }

    private static void flattenChildProperties(String prefix, Map<?, ?> source, Map<String, Object> target) {
        if (isTimestampContainer(source)) {
            TimestampedValue latestEntry = selectLatestTimestampValue(source);
            if (latestEntry == null) {
                return;
            }
            Object latestValue = latestEntry.value();
            if (latestValue instanceof Map<?, ?> latestMap) {
                flattenChildProperties(prefix, latestMap, target);
            } else if (prefix != null && !prefix.isBlank()) {
                target.put(prefix, latestValue);
            }
            return;
        }

        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            String field = prefix == null || prefix.isBlank() ? key : prefix + "." + key;
            Object fieldValue = entry.getValue();
            if (fieldValue instanceof Map<?, ?> nestedMap) {
                flattenChildProperties(field, nestedMap, target);
                continue;
            }
            target.put(field, fieldValue);
        }
    }

    private static String writeLogicalRawPayload(String logicalCode, Object logicalPayload) {
        try {
            Map<String, Object> rawPayload = new LinkedHashMap<>();
            rawPayload.put(logicalCode, logicalPayload);
            return OBJECT_MAPPER.writeValueAsString(rawPayload);
        } catch (Exception ex) {
            return null;
        }
    }

    private record TimestampedValue(String key,
                                    LocalDateTime timestamp,
                                    Object value) {
    }
}
