package com.ghlzm.iot.protocol.mqtt.legacy.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * legacy `$dp` 子模板注册表。
 */
public class LegacyDpChildTemplateRegistry {

    private final List<LegacyDpChildTemplate> templates;

    public LegacyDpChildTemplateRegistry() {
        this(List.of(
                new LegacyDpCrackChildTemplate(),
                new LegacyDpDeepDisplacementChildTemplate()
        ));
    }

    public LegacyDpChildTemplateRegistry(List<LegacyDpChildTemplate> templates) {
        this.templates = Collections.unmodifiableList(new ArrayList<>(templates == null ? List.of() : templates));
    }

    public List<LegacyDpChildTemplate> listTemplates() {
        return templates;
    }
}
