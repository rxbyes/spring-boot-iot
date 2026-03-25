package com.ghlzm.iot.protocol.mqtt.legacy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * `$dp` 历史地灾报文属性标准化器。
 */
public class LegacyDpPropertyNormalizer {

    private static final List<String> LEGACY_STATUS_FIELD_ALIASES = List.of(
            "ext_power_volt", "solar_volt", "battery_dump_energy", "signal_4g", "sensor_state", "lon", "lat"
    );
    private static final List<String> PROPERTY_CONTAINER_ALIASES = List.of(
            "properties", "property", "data", "params", "reported"
    );
    private static final List<String> RESERVED_PROPERTY_KEYS = List.of(
            "messageType", "productKey", "product_code", "productCode", "product_key", "pk",
            "deviceCode", "device_code", "deviceId", "device_id", "devId", "dev_id", "imei", "sn",
            "topic", "clientId", "client_id", "timestamp", "ts", "header", "headers", "body", "bodies",
            "_dataFormatType", "_fileStreamLength", "_fileStreamBase64", "_firmwarePacket", "_binaryLength"
    );

    private final LegacyDpFamilyResolver familyResolver;

    public LegacyDpPropertyNormalizer(LegacyDpFamilyResolver familyResolver) {
        this.familyResolver = familyResolver;
    }

    @SuppressWarnings("unchecked")
    public LegacyDpNormalizeResult normalize(Map<String, Object> payload, String resolvedDeviceCode) {
        LegacyDpNormalizeResult result = new LegacyDpNormalizeResult();
        result.setProperties(resolveProperties(payload, resolvedDeviceCode));
        ResolvedTimestamp resolvedTimestamp = resolveTimestamp(payload, resolvedDeviceCode);
        result.setTimestamp(resolvedTimestamp.timestamp());
        result.setTimestampSource(resolvedTimestamp.timestampSource());
        result.setMessageType(inferLegacyMessageType(payload, resolvedDeviceCode));
        result.setFamilyCodes(familyResolver.detectFamilyCodes(payload, resolvedDeviceCode));
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveProperties(Map<String, Object> payload, String resolvedDeviceCode) {
        for (String alias : PROPERTY_CONTAINER_ALIASES) {
            Object value = payload.get(alias);
            if (value instanceof Map<?, ?> mapValue) {
                return (Map<String, Object>) mapValue;
            }
        }

        Object legacyBody = payload;
        if (resolvedDeviceCode != null && payload.get(resolvedDeviceCode) instanceof Map<?, ?> devicePayload) {
            legacyBody = devicePayload;
        }

        if (legacyBody instanceof Map<?, ?> legacyMap) {
            Map<String, Object> flattened = new LinkedHashMap<>();
            flattenLegacyProperties("", legacyMap, flattened);
            if (!flattened.isEmpty()) {
                return flattened;
            }
        }

        Map<String, Object> fallback = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (RESERVED_PROPERTY_KEYS.contains(entry.getKey())) {
                continue;
            }
            fallback.put(entry.getKey(), entry.getValue());
        }
        return fallback;
    }

    private void flattenLegacyProperties(String prefix, Map<?, ?> source, Map<String, Object> target) {
        if (isTimestampContainer(source)) {
            List<Map.Entry<String, ?>> entries = new ArrayList<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    entries.add(Map.entry(key, entry.getValue()));
                }
            }
            entries.sort((left, right) -> left.getKey().compareTo(right.getKey()));
            if (!entries.isEmpty()) {
                Object latestValue = entries.get(entries.size() - 1).getValue();
                if (latestValue instanceof Map<?, ?> latestMap) {
                    flattenLegacyProperties(prefix, latestMap, target);
                } else if (prefix != null && !prefix.isBlank()) {
                    target.put(prefix, latestValue);
                }
            }
            return;
        }

        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            if ((prefix == null || prefix.isBlank()) && RESERVED_PROPERTY_KEYS.contains(key)) {
                continue;
            }
            String field = prefix == null || prefix.isBlank() ? key : prefix + "." + key;
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nestedMap) {
                flattenLegacyProperties(field, nestedMap, target);
                continue;
            }
            target.put(field, value);
        }
    }

    private boolean isTimestampContainer(Map<?, ?> source) {
        if (source.isEmpty()) {
            return false;
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !familyResolver.isTimestampKey(key)) {
                return false;
            }
        }
        return true;
    }

    private ResolvedTimestamp resolveTimestamp(Map<String, Object> payload, String resolvedDeviceCode) {
        List<LocalDateTime> timestamps = new ArrayList<>();
        Object body = resolvedDeviceCode != null && payload.get(resolvedDeviceCode) instanceof Map<?, ?> devicePayload
                ? devicePayload
                : payload;
        collectTimestamps(body, timestamps);
        if (!timestamps.isEmpty()) {
            timestamps.sort(LocalDateTime::compareTo);
            return new ResolvedTimestamp(timestamps.get(timestamps.size() - 1), "PAYLOAD_TIMESTAMP");
        }
        return new ResolvedTimestamp(LocalDateTime.now(), "SERVER_TIME");
    }

    private void collectTimestamps(Object source, List<LocalDateTime> timestamps) {
        if (!(source instanceof Map<?, ?> map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key && familyResolver.isTimestampKey(key)) {
                LocalDateTime parsed = familyResolver.parseTimestamp(key);
                if (parsed != null) {
                    timestamps.add(parsed);
                }
            }
            if (entry.getKey() instanceof String key
                    && ("at".equalsIgnoreCase(key) || "timestamp".equalsIgnoreCase(key) || "ts".equalsIgnoreCase(key))
                    && entry.getValue() != null) {
                LocalDateTime parsed = familyResolver.parseTimestamp(String.valueOf(entry.getValue()));
                if (parsed != null) {
                    timestamps.add(parsed);
                }
            }
            collectTimestamps(entry.getValue(), timestamps);
        }
    }

    private String inferLegacyMessageType(Map<String, Object> payload, String resolvedDeviceCode) {
        Object body = resolvedDeviceCode != null && payload.get(resolvedDeviceCode) instanceof Map<?, ?> devicePayload
                ? devicePayload
                : payload;
        if (!(body instanceof Map<?, ?> bodyMap)) {
            return null;
        }
        for (Map.Entry<?, ?> entry : bodyMap.entrySet()) {
            if (entry.getKey() instanceof String key && key.contains("_ZT_")) {
                return "status";
            }
        }

        for (String field : LEGACY_STATUS_FIELD_ALIASES) {
            if (containsField(bodyMap, field)) {
                return "status";
            }
        }
        return "property";
    }

    private boolean containsField(Map<?, ?> source, String expectedField) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (expectedField.equals(entry.getKey())) {
                return true;
            }
            if (entry.getValue() instanceof Map<?, ?> nestedMap && containsField(nestedMap, expectedField)) {
                return true;
            }
        }
        return false;
    }

    private record ResolvedTimestamp(LocalDateTime timestamp,
                                     String timestampSource) {
    }
}
