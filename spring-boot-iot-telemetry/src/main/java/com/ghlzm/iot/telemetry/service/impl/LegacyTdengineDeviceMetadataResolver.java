package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Device;
import lombok.Data;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 解析设备上与 TDengine legacy stable 相关的扩展信息。
 */
@Component
public class LegacyTdengineDeviceMetadataResolver {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public LegacyTdengineDeviceMetadata resolve(Device device) {
        LegacyTdengineDeviceMetadata metadata = new LegacyTdengineDeviceMetadata();
        metadata.setDeviceSn(fallbackText(readLegacyField(device, "deviceSn"), device == null ? null : device.getDeviceCode()));
        metadata.setLocation(fallbackText(readLegacyField(device, "location"), device == null ? null : device.getAddress()));
        metadata.setSubTables(readSubTables(device));
        if (!hasText(metadata.getDeviceSn())) {
            metadata.setDeviceSn(device != null && device.getId() != null ? "device_" + device.getId() : "device");
        }
        return metadata;
    }

    public String resolveSubTableName(LegacyTdengineDeviceMetadata metadata, String stable) {
        String configured = metadata == null || metadata.getSubTables() == null ? null : metadata.getSubTables().get(stable);
        if (isValidIdentifier(configured)) {
            return configured;
        }
        return "tb_" + stable + "_" + sanitizeIdentifierSegment(metadata == null ? null : metadata.getDeviceSn());
    }

    private Map<String, String> readSubTables(Device device) {
        JsonNode tdengineLegacyNode = readLegacyNode(device);
        if (tdengineLegacyNode == null) {
            return Map.of();
        }
        JsonNode subTablesNode = tdengineLegacyNode.get("subTables");
        if (subTablesNode == null || !subTablesNode.isObject()) {
            return Map.of();
        }
        Map<String, String> subTables = new LinkedHashMap<>();
        if (subTablesNode instanceof ObjectNode objectNode) {
            objectNode.properties().forEach(entry -> {
                String stable = normalizeIdentifier(entry.getKey());
                String tableName = normalizeIdentifier(entry.getValue() == null ? null : entry.getValue().asText(null));
                if (stable != null && tableName != null) {
                    subTables.put(stable, tableName);
                }
            });
        }
        return subTables;
    }

    private String readLegacyField(Device device, String fieldName) {
        JsonNode tdengineLegacyNode = readLegacyNode(device);
        if (tdengineLegacyNode == null) {
            return null;
        }
        JsonNode fieldNode = tdengineLegacyNode.get(fieldName);
        return fieldNode == null || fieldNode.isNull() ? null : fieldNode.asText(null);
    }

    private JsonNode readLegacyNode(Device device) {
        if (device == null || !hasText(device.getMetadataJson())) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(device.getMetadataJson());
            JsonNode tdengineLegacy = root == null ? null : root.get("tdengineLegacy");
            return tdengineLegacy != null && tdengineLegacy.isObject() ? tdengineLegacy : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String fallbackText(String primary, String fallback) {
        if (hasText(primary)) {
            return primary.trim();
        }
        return hasText(fallback) ? fallback.trim() : null;
    }

    private String normalizeIdentifier(String value) {
        if (!hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return isValidIdentifier(normalized) ? normalized : null;
    }

    private boolean isValidIdentifier(String value) {
        return hasText(value) && value.trim().matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    private String sanitizeIdentifierSegment(String value) {
        if (!hasText(value)) {
            return "device";
        }
        String sanitized = value.trim().replaceAll("[^A-Za-z0-9_]", "_");
        sanitized = sanitized.replaceAll("_+", "_");
        if (sanitized.isBlank()) {
            return "device";
        }
        if (Character.isDigit(sanitized.charAt(0))) {
            return "d_" + sanitized;
        }
        return sanitized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Data
    public static class LegacyTdengineDeviceMetadata {
        private String deviceSn;
        private String location;
        private Map<String, String> subTables = Map.of();
    }
}
