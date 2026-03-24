package com.ghlzm.iot.protocol.mqtt.legacy;

import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LegacyDpChildMessageSplitter {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public SplitResult split(Map<String, Object> payload,
                             String baseDeviceCode,
                             String tenantId,
                             String productKey,
                             String messageType,
                             String topic,
                             LocalDateTime fallbackTimestamp,
                             Map<String, String> subDeviceMappings) {
        if (subDeviceMappings == null || subDeviceMappings.isEmpty()) {
            return SplitResult.empty();
        }
        Object basePayload = baseDeviceCode == null ? null : payload.get(baseDeviceCode);
        if (!(basePayload instanceof Map<?, ?> basePayloadMap)) {
            return SplitResult.empty();
        }

        List<DeviceUpMessage> childMessages = new ArrayList<>();
        List<String> logicalCodes = new ArrayList<>();
        for (Map.Entry<String, String> entry : subDeviceMappings.entrySet()) {
            String logicalCode = entry.getKey();
            String childDeviceCode = entry.getValue();
            if (logicalCode == null || logicalCode.isBlank() || childDeviceCode == null || childDeviceCode.isBlank()) {
                continue;
            }

            LatestLogicalPayload latestLogicalPayload = extractLatestLogicalPayload(logicalCode, basePayloadMap.get(logicalCode));
            if (latestLogicalPayload == null || latestLogicalPayload.properties().isEmpty()) {
                continue;
            }

            DeviceUpMessage childMessage = new DeviceUpMessage();
            childMessage.setTenantId(tenantId);
            childMessage.setProductKey(productKey);
            childMessage.setDeviceCode(childDeviceCode);
            childMessage.setMessageType(messageType);
            childMessage.setTopic(topic);
            childMessage.setTimestamp(latestLogicalPayload.timestamp() == null ? fallbackTimestamp : latestLogicalPayload.timestamp());
            childMessage.setProperties(latestLogicalPayload.properties());
            childMessage.setRawPayload(latestLogicalPayload.rawPayload());
            childMessages.add(childMessage);
            logicalCodes.add(logicalCode);
        }
        return new SplitResult(childMessages, logicalCodes);
    }

    private LatestLogicalPayload extractLatestLogicalPayload(String logicalCode, Object logicalPayload) {
        if (logicalPayload == null) {
            return null;
        }

        LocalDateTime logicalTimestamp = null;
        Object latestValue = logicalPayload;
        String rawPayload = writeLogicalRawPayload(logicalCode, logicalPayload);
        if (logicalPayload instanceof Map<?, ?> logicalMap && isTimestampContainer(logicalMap)) {
            TimestampedValue latestEntry = selectLatestTimestampValue(logicalMap);
            if (latestEntry == null) {
                return null;
            }
            logicalTimestamp = latestEntry.timestamp();
            latestValue = latestEntry.value();
            Map<String, Object> latestPayload = new LinkedHashMap<>();
            latestPayload.put(latestEntry.key(), latestEntry.value());
            rawPayload = writeLogicalRawPayload(logicalCode, latestPayload);
        }

        Map<String, Object> properties = toLogicalProperties(logicalCode, latestValue);
        return properties.isEmpty() ? null : new LatestLogicalPayload(logicalTimestamp, properties, rawPayload);
    }

    private TimestampedValue selectLatestTimestampValue(Map<?, ?> source) {
        List<TimestampedValue> timestampedValues = new ArrayList<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            LocalDateTime timestamp = parseTimestamp(key);
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

    private Map<String, Object> toLogicalProperties(String logicalCode, Object value) {
        Map<String, Object> properties = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> mapValue) {
            flattenChildProperties("", mapValue, properties);
            return properties;
        }
        if (value != null) {
            properties.put(logicalCode, value);
        }
        return properties;
    }

    private void flattenChildProperties(String prefix, Map<?, ?> source, Map<String, Object> target) {
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
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nestedMap) {
                flattenChildProperties(field, nestedMap, target);
                continue;
            }
            target.put(field, value);
        }
    }

    private String writeLogicalRawPayload(String logicalCode, Object logicalPayload) {
        try {
            Map<String, Object> rawPayload = new LinkedHashMap<>();
            rawPayload.put(logicalCode, logicalPayload);
            return objectMapper.writeValueAsString(rawPayload);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isTimestampContainer(Map<?, ?> source) {
        if (source.isEmpty()) {
            return false;
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key) || parseTimestamp(key) == null) {
                return false;
            }
        }
        return true;
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

    private record LatestLogicalPayload(LocalDateTime timestamp,
                                        Map<String, Object> properties,
                                        String rawPayload) {
    }

    private record TimestampedValue(String key,
                                    LocalDateTime timestamp,
                                    Object value) {
    }

    public record SplitResult(List<DeviceUpMessage> messages,
                              List<String> logicalCodes) {

        private static SplitResult empty() {
            return new SplitResult(List.of(), List.of());
        }
    }
}
