package com.ghlzm.iot.protocol.mqtt.legacy.template;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class LegacyDpDeepDisplacementChildTemplate implements LegacyDpChildTemplate {

    private static final Pattern DEEP_DISPLACEMENT_LOGICAL_CODE_PATTERN = Pattern.compile("^L1_SW_\\d+$");
    private static final String TEMPLATE_CODE = "deep_displacement_child_template";

    @Override
    public String getTemplateCode() {
        return TEMPLATE_CODE;
    }

    @Override
    public boolean matches(LegacyDpChildTemplateContext context) {
        LegacyDpLogicalPayloadDescriptor descriptor = describe(context);
        return descriptor != null
                && descriptor.shape() == LegacyDpLogicalPayloadShape.TIMESTAMP_OBJECT
                && context.logicalCode() != null
                && DEEP_DISPLACEMENT_LOGICAL_CODE_PATTERN.matcher(context.logicalCode()).matches();
    }

    @Override
    public LegacyDpChildTemplateExecutionResult execute(LegacyDpChildTemplateContext context) {
        LegacyDpLogicalPayloadDescriptor descriptor = describe(context);
        if (descriptor == null || context == null) {
            return new LegacyDpChildTemplateExecutionResult(
                    TEMPLATE_CODE, Map.of(), List.of(), false, null, null, null, List.of()
            );
        }
        return new LegacyDpChildTemplateExecutionResult(
                TEMPLATE_CODE,
                LegacyDpChildTemplateSupport.flattenObjectProperties(descriptor.latestValue()),
                List.of(context.logicalCode()),
                false,
                context.relationRule() == null ? null
                        : LegacyDpChildTemplateSupport.normalizeStrategy(context.relationRule().canonicalizationStrategy()),
                descriptor.timestamp(),
                descriptor.rawPayload(),
                List.of()
        );
    }

    private LegacyDpLogicalPayloadDescriptor describe(LegacyDpChildTemplateContext context) {
        return context == null ? null
                : LegacyDpChildTemplateSupport.describe(context.logicalCode(), context.logicalPayload());
    }
}
