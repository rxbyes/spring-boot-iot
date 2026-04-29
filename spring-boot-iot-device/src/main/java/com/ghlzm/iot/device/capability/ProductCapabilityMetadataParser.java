package com.ghlzm.iot.device.capability;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProductCapabilityMetadataParser {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public ProductCapabilityMetadata parse(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return ProductCapabilityMetadata.unknown();
        }
        try {
            JsonNode root = objectMapper.readTree(metadataJson);
            JsonNode governance = root == null ? null : root.get("governance");
            if (governance == null || governance.isNull()) {
                return ProductCapabilityMetadata.unknown();
            }
            return new ProductCapabilityMetadata(
                    DeviceCapabilityType.from(text(governance, "productCapabilityType")),
                    WarningDeviceKind.from(text(governance, "warningDeviceKind")),
                    VideoDeviceKind.from(text(governance, "videoDeviceKind"))
            );
        } catch (Exception ignored) {
            return ProductCapabilityMetadata.unknown();
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
