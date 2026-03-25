package com.ghlzm.iot.protocol.mqtt.legacy;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * `$dp` 基站一包多测点子消息拆分器。
 */
public class LegacyDpChildMessageSplitter {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final LegacyDpFamilyResolver familyResolver = new LegacyDpFamilyResolver();
    private final IotProperties iotProperties;

    public LegacyDpChildMessageSplitter(IotProperties iotProperties) {
        this.iotProperties = iotProperties;
    }

    public LegacyDpNormalizeResult split(Map<String, Object> payload,
                                         DeviceUpMessage parentMessage,
                                         LegacyDpNormalizeResult normalizeResult) {
        LegacyDpNormalizeResult result = normalizeResult == null ? new LegacyDpNormalizeResult() : normalizeResult;
        if (parentMessage == null) {
            result.setChildMessages(List.of());
            result.setChildSplitApplied(Boolean.FALSE);
            return result;
        }

        Map<String, String> subDeviceMappings = resolveSubDeviceMappings(parentMessage.getDeviceCode());
        if (subDeviceMappings.isEmpty()) {
            result.setChildMessages(List.of());
            result.setChildSplitApplied(Boolean.FALSE);
            return result;
        }

        Object basePayload = parentMessage.getDeviceCode() == null ? null : payload.get(parentMessage.getDeviceCode());
        if (!(basePayload instanceof Map<?, ?> basePayloadMap)) {
            result.setChildMessages(List.of());
            result.setChildSplitApplied(Boolean.FALSE);
            return result;
        }

        List<DeviceUpMessage> childMessages = new ArrayList<>();
        List<String> logicalCodes = new ArrayList<>();
        List<Map.Entry<String, String>> mappingEntries = new ArrayList<>(subDeviceMappings.entrySet());
        mappingEntries.sort(Map.Entry.comparingByKey());
        for (Map.Entry<String, String> entry : mappingEntries) {
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
            childMessage.setTenantId(parentMessage.getTenantId());
            childMessage.setProductKey(parentMessage.getProductKey());
            childMessage.setDeviceCode(childDeviceCode);
            childMessage.setMessageType(parentMessage.getMessageType());
            childMessage.setTopic(parentMessage.getTopic());
            childMessage.setTimestamp(latestLogicalPayload.timestamp() == null
                    ? parentMessage.getTimestamp()
                    : latestLogicalPayload.timestamp());
            childMessage.setProperties(latestLogicalPayload.properties());
            childMessage.setRawPayload(latestLogicalPayload.rawPayload());
            childMessages.add(childMessage);
            logicalCodes.add(logicalCode);
        }

        result.setChildMessages(childMessages);
        if (childMessages.isEmpty()) {
            result.setChildSplitApplied(Boolean.FALSE);
            return result;
        }

        result.setChildSplitApplied(Boolean.TRUE);
        result.setProperties(removeChildLogicalProperties(result.getProperties(), logicalCodes));
        return result;
    }

    private Map<String, String> resolveSubDeviceMappings(String baseDeviceCode) {
        if (baseDeviceCode == null || baseDeviceCode.isBlank() || iotProperties.getDevice() == null
                || iotProperties.getDevice().getSubDeviceMappings() == null) {
            return Map.of();
        }
        Map<String, String> configuredMappings = iotProperties.getDevice().getSubDeviceMappings().get(baseDeviceCode);
        return configuredMappings == null || configuredMappings.isEmpty() ? Map.of() : configuredMappings;
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

    private TimestampedValue selectLatestTimestampValue(Map<?, ?> source) {
        List<TimestampedValue> timestampedValues = new ArrayList<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            LocalDateTime timestamp = familyResolver.parseTimestamp(key);
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

    private Map<String, Object> removeChildLogicalProperties(Map<String, Object> properties, List<String> logicalCodes) {
        if (properties == null || properties.isEmpty() || logicalCodes == null || logicalCodes.isEmpty()) {
            return properties;
        }
        Map<String, Object> filteredProperties = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (isChildLogicalProperty(entry.getKey(), logicalCodes)) {
                continue;
            }
            filteredProperties.put(entry.getKey(), entry.getValue());
        }
        return filteredProperties;
    }

    private boolean isChildLogicalProperty(String propertyKey, List<String> logicalCodes) {
        if (propertyKey == null || propertyKey.isBlank()) {
            return false;
        }
        for (String logicalCode : logicalCodes) {
            if (logicalCode != null && !logicalCode.isBlank()
                    && (propertyKey.equals(logicalCode) || propertyKey.startsWith(logicalCode + "."))) {
                return true;
            }
        }
        return false;
    }

    private record LatestLogicalPayload(LocalDateTime timestamp,
                                        Map<String, Object> properties,
                                        String rawPayload) {
    }

    private record TimestampedValue(String key,
                                    LocalDateTime timestamp,
                                    Object value) {
    }
}
