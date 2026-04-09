package com.ghlzm.iot.protocol.mqtt.legacy.template;

import java.util.Objects;
import java.util.Optional;

/**
 * legacy `$dp` 子模板匹配器。
 */
public class LegacyDpChildTemplateMatcher {

    private final LegacyDpChildTemplateRegistry registry;

    public LegacyDpChildTemplateMatcher(LegacyDpChildTemplateRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public Optional<LegacyDpChildTemplate> match(LegacyDpChildTemplateContext context) {
        if (context == null) {
            return Optional.empty();
        }
        return registry.listTemplates().stream()
                .filter(template -> template != null && template.matches(context))
                .findFirst();
    }
}
