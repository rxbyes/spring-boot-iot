package com.ghlzm.iot.protocol.mqtt.legacy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LegacyDpPropertyNormalizer {

    private static final List<String> PROPERTY_CONTAINER_ALIASES = List.of(
            "properties", "property", "data", "params", "reported"
    );
    private static final List<String> RESERVED_PROPERTY_KEYS = List.of(
            "messageType", "productKey", "product_code", "productCode", "product_key", "pk",
            "deviceCode", "device_code", "deviceId", "device_id", "devId", "dev_id", "imei", "sn",
            "topic", "clientId", "client_id", "timestamp", "ts", "header", "headers", "body", "bodies",
            "_dataFormatType", "_fileStreamLength", "_fileStreamBase64", "_firmwarePacket", "_binaryLength"
    );

    private final LegacyDpFamilyResolver familyResolver = new LegacyDpFamilyResolver();

    @SuppressWarnings("unchecked")
    public LegacyDpNormalizeResult normalize(Map<String, Object> payload,
                                             String resolvedDeviceCode,
                                             List<String> familyCodes) {
        List<String> resolvedFamilyCodes = familyCodes == null || familyCodes.isEmpty()
                ? familyResolver.resolveFamilyCodes(payload, resolvedDeviceCode)
                : familyCodes;
        Map<String, Object> properties = resolveProperties(payload, resolvedDeviceCode);
        TimestampResolution timestampResolution = resolveTimestamp(payload, resolvedDeviceCode);
        String messageType = familyResolver.inferMessageType(payload, resolvedDeviceCode, resolvedFamilyCodes);
        return new LegacyDpNormalizeResult(
                resolvedFamilyCodes,
                properties,
                messageType,
                timestampResolution.timestamp(),
                timestampResolution.source()
        );
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
            entries.sort(Comparator.comparing(Map.Entry::getKey));
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

    private TimestampResolution resolveTimestamp(Map<String, Object> payload, String resolvedDeviceCode) {
        List<LocalDateTime> timestamps = new ArrayList<>();
        Object body = resolvedDeviceCode != null && payload.get(resolvedDeviceCode) instanceof Map<?, ?> devicePayload
                ? devicePayload
                : payload;
        collectTimestamps(body, timestamps);
        if (!timestamps.isEmpty()) {
            timestamps.sort(LocalDateTime::compareTo);
            return new TimestampResolution(timestamps.get(timestamps.size() - 1), "PAYLOAD_LATEST_TIMESTAMP");
        }
        return new TimestampResolution(LocalDateTime.now(), "SERVER_NOW");
    }

    private void collectTimestamps(Object source, List<LocalDateTime> timestamps) {
        if (!(source instanceof Map<?, ?> map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key && isTimestampKey(key)) {
                LocalDateTime parsed = parseTimestamp(key);
                if (parsed != null) {
                    timestamps.add(parsed);
                }
            }
            if (entry.getKey() instanceof String key
                    && ("at".equalsIgnoreCase(key) || "timestamp".equalsIgnoreCase(key) || "ts".equalsIgnoreCase(key))
                    && entry.getValue() != null) {
                LocalDateTime parsed = parseTimestamp(String.valueOf(entry.getValue()));
                if (parsed != null) {
                    timestamps.add(parsed);
                }
            }
            collectTimestamps(entry.getValue(), timestamps);
        }
    }

    private boolean isTimestampContainer(Map<?, ?> source) {
        if (source.isEmpty()) {
            return false;
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !isTimestampKey(key)) {
                return false;
            }
        }
        return true;
    }

    private boolean isTimestampKey(String key) {
        return parseTimestamp(key) != null;
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                long epochMillis = Long.parseLong(value);
                return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
            } catch (NumberFormatException numberFormatException) {
                return null;
            }
        }
    }

    private record TimestampResolution(LocalDateTime timestamp,
                                       String source) {
    }
}
