package com.ghlzm.iot.protocol.mqtt.legacy.template;

import com.ghlzm.iot.framework.protocol.template.ProtocolTemplateDefinitionProvider;
import com.ghlzm.iot.protocol.mqtt.legacy.template.runtime.ConfigDrivenLegacyDpChildTemplate;
import com.ghlzm.iot.protocol.mqtt.legacy.template.runtime.LegacyDpTemplateCompiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * legacy `$dp` 子模板注册表。
 */
public class LegacyDpChildTemplateRegistry {

    private final List<LegacyDpChildTemplate> templates;
    private final ProtocolTemplateDefinitionProvider templateDefinitionProvider;
    private final LegacyDpTemplateCompiler templateCompiler;

    public LegacyDpChildTemplateRegistry() {
        this(List.of(
                new LegacyDpCrackChildTemplate(),
                new LegacyDpDeepDisplacementChildTemplate()
        ));
    }

    public LegacyDpChildTemplateRegistry(List<LegacyDpChildTemplate> templates) {
        this.templates = Collections.unmodifiableList(new ArrayList<>(templates == null ? List.of() : templates));
        this.templateDefinitionProvider = null;
        this.templateCompiler = null;
    }

    public LegacyDpChildTemplateRegistry(ProtocolTemplateDefinitionProvider templateDefinitionProvider) {
        this(templateDefinitionProvider, new LegacyDpTemplateCompiler());
    }

    public LegacyDpChildTemplateRegistry(ProtocolTemplateDefinitionProvider templateDefinitionProvider,
                                         LegacyDpTemplateCompiler templateCompiler) {
        this.templates = Collections.unmodifiableList(new ArrayList<>(List.of(
                new LegacyDpCrackChildTemplate(),
                new LegacyDpDeepDisplacementChildTemplate()
        )));
        this.templateDefinitionProvider = Objects.requireNonNull(templateDefinitionProvider, "templateDefinitionProvider");
        this.templateCompiler = Objects.requireNonNull(templateCompiler, "templateCompiler");
    }

    public List<LegacyDpChildTemplate> listTemplates() {
        if (templateDefinitionProvider != null) {
            List<LegacyDpChildTemplate> publishedTemplates = templateDefinitionProvider.listPublishedDefinitions().stream()
                    .map(templateCompiler::compile)
                    .filter(Objects::nonNull)
                    .map(ConfigDrivenLegacyDpChildTemplate::new)
                    .map(LegacyDpChildTemplate.class::cast)
                    .sorted((left, right) -> String.valueOf(left.getTemplateCode()).compareTo(String.valueOf(right.getTemplateCode())))
                    .toList();
            if (!publishedTemplates.isEmpty()) {
                return publishedTemplates;
            }
        }
        return templates;
    }
}
