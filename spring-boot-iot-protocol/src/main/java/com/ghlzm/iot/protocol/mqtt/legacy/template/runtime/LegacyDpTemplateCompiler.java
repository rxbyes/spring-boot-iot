package com.ghlzm.iot.protocol.mqtt.legacy.template.runtime;

import com.ghlzm.iot.framework.protocol.template.ProtocolTemplateRuntimeDefinition;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class LegacyDpTemplateCompiler {

    private static final String LEGACY_DP_FAMILY_CODE = "legacy-dp";

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public CompiledLegacyDpTemplate compile(ProtocolTemplateRuntimeDefinition definition) {
        if (definition == null || !StringUtils.hasText(definition.templateCode())) {
            return null;
        }
        if (!LEGACY_DP_FAMILY_CODE.equalsIgnoreCase(normalizeText(definition.familyCode()))) {
            return null;
        }
        String logicalPattern = readLogicalPattern(definition.expressionJson());
        if (!StringUtils.hasText(logicalPattern)) {
            return null;
        }
        Map<String, String> outputMappings = resolveOutputMappings(definition.outputMappingJson(), logicalPattern);
        if (outputMappings.isEmpty()) {
            return null;
        }
        try {
            return new CompiledLegacyDpTemplate(
                    normalizeText(definition.templateCode()),
                    normalizeText(definition.familyCode()),
                    normalizeText(definition.protocolCode()),
                    Pattern.compile(logicalPattern),
                    inferObjectPayload(outputMappings),
                    outputMappings
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private String readLogicalPattern(String expressionJson) {
        JsonNode expressionNode = readJson(expressionJson);
        if (expressionNode == null || !expressionNode.isObject()) {
            return null;
        }
        String logicalPattern = normalizeText(expressionNode.path("logicalPattern").asText(null));
        if (StringUtils.hasText(logicalPattern)) {
            return logicalPattern;
        }
        return normalizeText(expressionNode.path("logicalCodePattern").asText(null));
    }

    private Map<String, String> resolveOutputMappings(String outputMappingJson, String logicalPattern) {
        Map<String, String> mappings = new LinkedHashMap<>();
        if (StringUtils.hasText(outputMappingJson)) {
            try {
                Map<String, String> parsedMappings = objectMapper.readValue(outputMappingJson,
                        new TypeReference<LinkedHashMap<String, String>>() {
                        });
                if (parsedMappings != null) {
                    parsedMappings.forEach((targetIdentifier, sourcePath) -> {
                        String normalizedTarget = normalizeText(targetIdentifier);
                        String normalizedSource = normalizeText(sourcePath);
                        if (StringUtils.hasText(normalizedTarget) && StringUtils.hasText(normalizedSource)) {
                            mappings.put(normalizedTarget, normalizedSource);
                        }
                    });
                }
            } catch (Exception ignored) {
                // Ignore invalid mapping JSON here and let the caller fall back to defaults or skip compilation.
            }
        }
        if (!mappings.isEmpty()) {
            return mappings;
        }
        if (logicalPattern != null && logicalPattern.contains("L1_LF_")) {
            mappings.put("value", "$.value");
            return mappings;
        }
        if (logicalPattern != null && logicalPattern.contains("L1_SW_")) {
            mappings.put("dispsX", "$.dispsX");
            mappings.put("dispsY", "$.dispsY");
        }
        return mappings;
    }

    private boolean inferObjectPayload(Map<String, String> outputMappings) {
        for (String sourcePath : outputMappings.values()) {
            if (!isScalarRootPath(sourcePath)) {
                return true;
            }
        }
        return false;
    }

    private boolean isScalarRootPath(String sourcePath) {
        String normalized = normalizeText(sourcePath);
        return "$".equals(normalized) || "$.value".equals(normalized) || "value".equalsIgnoreCase(normalized);
    }

    private JsonNode readJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
