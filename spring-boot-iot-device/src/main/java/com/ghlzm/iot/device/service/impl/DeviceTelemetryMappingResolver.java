package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 产品物模型中的 legacy 遥测映射解析器。
 */
final class DeviceTelemetryMappingResolver {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    TelemetryMetricMapping resolve(String identifier, String specsJson) {
        TelemetryMetricMapping mapping = new TelemetryMetricMapping();
        mapping.setMetricCode(identifier);
        if (specsJson == null || specsJson.isBlank()) {
            mapping.addFallbackReason(TelemetryMetricMapping.REASON_MAPPING_NOT_CONFIGURED);
            return mapping;
        }
        try {
            JsonNode root = objectMapper.readTree(specsJson);
            JsonNode mappingNode = root == null ? null : root.get("tdengineLegacy");
            if (mappingNode == null || !mappingNode.isObject()) {
                mapping.addFallbackReason(TelemetryMetricMapping.REASON_MAPPING_NOT_CONFIGURED);
                return mapping;
            }
            mapping.setSource(TelemetryMetricMapping.SOURCE_SPECS_JSON_TDENGINE_LEGACY);
            if (mappingNode.has("enabled")) {
                mapping.setEnabled(mappingNode.path("enabled").asBoolean(true));
            }
            resolveStable(mapping, mappingNode);
            resolveColumn(mapping, mappingNode);
            if (Boolean.FALSE.equals(mapping.getEnabled())) {
                mapping.addFallbackReason(TelemetryMetricMapping.REASON_MAPPING_DISABLED);
            }
            return mapping;
        } catch (Exception ex) {
            mapping.addFallbackReason(TelemetryMetricMapping.REASON_MAPPING_NOT_CONFIGURED);
            return mapping;
        }
    }

    DevicePropertyMetadata.TdengineLegacyMapping toLegacyMapping(TelemetryMetricMapping mapping) {
        if (mapping == null || !hasText(mapping.getStable()) || !hasText(mapping.getColumn())) {
            return null;
        }
        DevicePropertyMetadata.TdengineLegacyMapping legacyMapping = new DevicePropertyMetadata.TdengineLegacyMapping();
        legacyMapping.setEnabled(mapping.getEnabled() == null ? Boolean.TRUE : mapping.getEnabled());
        legacyMapping.setStable(mapping.getStable());
        legacyMapping.setColumn(mapping.getColumn());
        return legacyMapping;
    }

    private void resolveStable(TelemetryMetricMapping mapping, JsonNode mappingNode) {
        String rawStable = trimToNull(mappingNode.path("stable").asText(null));
        if (rawStable == null) {
            mapping.addFallbackReason(TelemetryMetricMapping.REASON_STABLE_MISSING);
            return;
        }
        String stable = normalizeIdentifier(rawStable);
        if (stable == null) {
            mapping.addFallbackReason(TelemetryMetricMapping.REASON_STABLE_INVALID);
            return;
        }
        mapping.setStable(stable);
    }

    private void resolveColumn(TelemetryMetricMapping mapping, JsonNode mappingNode) {
        String rawColumn = trimToNull(mappingNode.path("column").asText(null));
        if (rawColumn == null) {
            mapping.addFallbackReason(TelemetryMetricMapping.REASON_COLUMN_MISSING);
            return;
        }
        String column = normalizeIdentifier(rawColumn);
        if (column == null) {
            mapping.addFallbackReason(TelemetryMetricMapping.REASON_COLUMN_INVALID);
            return;
        }
        mapping.setColumn(column);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeIdentifier(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.matches("[A-Za-z_][A-Za-z0-9_]*") ? normalized : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
