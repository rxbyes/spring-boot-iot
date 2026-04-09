package com.ghlzm.iot.protocol.mqtt.legacy;

import com.ghlzm.iot.protocol.core.model.ProtocolMetricEvidence;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
    private static final Pattern CRACK_LOGICAL_CODE_PATTERN = Pattern.compile("^L1_LF_\\d+$");
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
        result.setMetricEvidence(resolveMetricEvidence(payload, resolvedDeviceCode));
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ProtocolMetricEvidence> resolveMetricEvidence(Map<String, Object> payload, String resolvedDeviceCode) {
        Object body = resolvedDeviceCode != null && payload.get(resolvedDeviceCode) instanceof Map<?, ?> devicePayload
                ? devicePayload
                : payload;
        if (!(body instanceof Map<?, ?> bodyMap)) {
            return List.of();
        }
        List<ProtocolMetricEvidence> evidence = new ArrayList<>();
        List<Map.Entry<String, Object>> entries = new ArrayList<>();
        for (Map.Entry<?, ?> entry : bodyMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                entries.add(Map.entry(key, entry.getValue()));
            }
        }
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        for (Map.Entry<String, Object> entry : entries) {
            String key = entry.getKey();
            if (CRACK_LOGICAL_CODE_PATTERN.matcher(key).matches()) {
                Object latestValue = extractLatestValue(entry.getValue());
                if (latestValue instanceof Map<?, ?> latestMap && latestMap.containsKey("value")) {
                    latestValue = latestMap.get("value");
                }
                if (latestValue != null) {
                    evidence.add(metricEvidence(
                            key,
                            "value",
                            key,
                            resolvedDeviceCode,
                            null,
                            latestValue,
                            "legacy_dp_normalizer"
                    ));
                }
                continue;
            }
            if (!"S1_ZT_1".equals(key)) {
                continue;
            }
            Object latestValue = extractLatestValue(entry.getValue());
            if (!(latestValue instanceof Map<?, ?> latestMap)) {
                continue;
            }
            Object sensorState = latestMap.get("sensor_state");
            if (!(sensorState instanceof Map<?, ?> sensorStateMap)) {
                continue;
            }
            List<Map.Entry<String, Object>> sensorEntries = new ArrayList<>();
            for (Map.Entry<?, ?> sensorEntry : sensorStateMap.entrySet()) {
                if (sensorEntry.getKey() instanceof String sensorKey) {
                    sensorEntries.add(Map.entry(sensorKey, sensorEntry.getValue()));
                }
            }
            sensorEntries.sort(Comparator.comparing(Map.Entry::getKey));
            for (Map.Entry<String, Object> sensorEntry : sensorEntries) {
                if (!CRACK_LOGICAL_CODE_PATTERN.matcher(sensorEntry.getKey()).matches()) {
                    continue;
                }
                evidence.add(metricEvidence(
                        "S1_ZT_1.sensor_state." + sensorEntry.getKey(),
                        "sensor_state",
                        sensorEntry.getKey(),
                        resolvedDeviceCode,
                        null,
                        sensorEntry.getValue(),
                        "legacy_dp_normalizer"
                ));
            }
        }
        return evidence;
    }

    private Object extractLatestValue(Object source) {
        if (!(source instanceof Map<?, ?> map)) {
            return source;
        }
        if (!isTimestampContainer(map)) {
            return source;
        }
        List<Map.Entry<String, Object>> entries = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key) {
                entries.add(Map.entry(key, entry.getValue()));
            }
        }
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        return entries.isEmpty() ? null : extractLatestValue(entries.get(entries.size() - 1).getValue());
    }

    private ProtocolMetricEvidence metricEvidence(String rawIdentifier,
                                                  String canonicalIdentifier,
                                                  String logicalChannelCode,
                                                  String parentDeviceCode,
                                                  String childDeviceCode,
                                                  Object sampleValue,
                                                  String evidenceOrigin) {
        ProtocolMetricEvidence evidence = new ProtocolMetricEvidence();
        evidence.setRawIdentifier(rawIdentifier);
        evidence.setCanonicalIdentifier(canonicalIdentifier);
        evidence.setLogicalChannelCode(logicalChannelCode);
        evidence.setParentDeviceCode(parentDeviceCode);
        evidence.setChildDeviceCode(childDeviceCode);
        evidence.setSampleValue(sampleValue == null ? null : String.valueOf(sampleValue));
        evidence.setValueType(resolveValueType(sampleValue));
        evidence.setEvidenceOrigin(evidenceOrigin);
        return evidence;
    }

    private String resolveValueType(Object sampleValue) {
        if (sampleValue == null) {
            return null;
        }
        if (sampleValue instanceof Integer || sampleValue instanceof Long) {
            return "integer";
        }
        if (sampleValue instanceof Number) {
            return "double";
        }
        if (sampleValue instanceof Boolean) {
            return "bool";
        }
        return "string";
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
