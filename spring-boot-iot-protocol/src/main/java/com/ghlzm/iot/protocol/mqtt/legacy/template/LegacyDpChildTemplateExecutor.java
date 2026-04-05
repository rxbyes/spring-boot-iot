package com.ghlzm.iot.protocol.mqtt.legacy.template;

import java.util.Objects;

/**
 * legacy `$dp` 子模板执行器。
 */
public class LegacyDpChildTemplateExecutor {

    public LegacyDpChildTemplateExecutionResult execute(LegacyDpChildTemplate template,
                                                        LegacyDpChildTemplateContext context) {
        return Objects.requireNonNull(template, "template").execute(context);
    }
}
