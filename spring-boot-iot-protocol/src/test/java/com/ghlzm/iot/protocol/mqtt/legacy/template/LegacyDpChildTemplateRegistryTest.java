package com.ghlzm.iot.protocol.mqtt.legacy.template;

import com.ghlzm.iot.framework.protocol.template.ProtocolTemplateDefinitionProvider;
import com.ghlzm.iot.framework.protocol.template.ProtocolTemplateRuntimeDefinition;
import com.ghlzm.iot.protocol.mqtt.legacy.template.runtime.ConfigDrivenLegacyDpChildTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDpChildTemplateRegistryTest {

    @Test
    void shouldCompilePublishedLegacyDpTemplatesIntoConfigDrivenChildren() {
        ProtocolTemplateDefinitionProvider provider = () -> List.of(
                templateDefinition(
                        "legacy-dp-crack-v1",
                        "^L1_LF_\\d+$",
                        "{\"value\":\"$.value\"}"
                ),
                templateDefinition(
                        "legacy-dp-deep-displacement-v1",
                        "^L1_SW_\\d+$",
                        "{\"dispsX\":\"$.dispsX\",\"dispsY\":\"$.dispsY\"}"
                )
        );

        LegacyDpChildTemplateRegistry registry = new LegacyDpChildTemplateRegistry(provider);

        List<LegacyDpChildTemplate> templates = registry.listTemplates();

        assertEquals(
                List.of("legacy-dp-crack-v1", "legacy-dp-deep-displacement-v1"),
                templates.stream().map(LegacyDpChildTemplate::getTemplateCode).toList()
        );
        assertTrue(templates.stream().allMatch(ConfigDrivenLegacyDpChildTemplate.class::isInstance));
    }

    private ProtocolTemplateRuntimeDefinition templateDefinition(String templateCode,
                                                                String logicalPattern,
                                                                String outputMappingJson) {
        return new ProtocolTemplateRuntimeDefinition(
                templateCode,
                "legacy-dp",
                "mqtt-json",
                templateCode,
                "{\"logicalPattern\":\"" + logicalPattern.replace("\\", "\\\\") + "\"}",
                outputMappingJson,
                1
        );
    }
}
