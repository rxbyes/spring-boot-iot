package com.ghlzm.iot.protocol.mqtt.legacy.template;

import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpRelationRule;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class LegacyDpCrackChildTemplate implements LegacyDpChildTemplate {

    private static final Pattern CRACK_LOGICAL_CODE_PATTERN = Pattern.compile("^L1_LF_\\d+$");
    private static final String TEMPLATE_CODE = "crack_child_template";
    private static final String CANONICALIZATION_STRATEGY_LF_VALUE = "LF_VALUE";
    private static final String STATUS_MIRROR_STRATEGY_SENSOR_STATE = "SENSOR_STATE";
    private static final String CHILD_CRACK_VALUE_PROPERTY = "value";
    private static final String CHILD_SENSOR_STATE_PROPERTY = "sensor_state";

    @Override
    public String getTemplateCode() {
        return TEMPLATE_CODE;
    }

    @Override
    public boolean matches(LegacyDpChildTemplateContext context) {
        LegacyDpLogicalPayloadDescriptor descriptor = describe(context);
        return descriptor != null
                && descriptor.shape() == LegacyDpLogicalPayloadShape.TIMESTAMP_SCALAR
                && context.logicalCode() != null
                && CRACK_LOGICAL_CODE_PATTERN.matcher(context.logicalCode()).matches();
    }

    @Override
    public LegacyDpChildTemplateExecutionResult execute(LegacyDpChildTemplateContext context) {
        LegacyDpLogicalPayloadDescriptor descriptor = describe(context);
        if (descriptor == null || context == null) {
            return new LegacyDpChildTemplateExecutionResult(
                    TEMPLATE_CODE, Map.of(), List.of(), false, null, null, null
            );
        }
        LegacyDpRelationRule relationRule = context.relationRule();
        String canonicalizationStrategy = relationRule == null ? null
                : LegacyDpChildTemplateSupport.normalizeStrategy(relationRule.canonicalizationStrategy());
        Map<String, Object> childProperties = new LinkedHashMap<>();
        if (descriptor.latestValue() != null) {
            String propertyKey = CANONICALIZATION_STRATEGY_LF_VALUE.equalsIgnoreCase(canonicalizationStrategy)
                    ? CHILD_CRACK_VALUE_PROPERTY
                    : context.logicalCode();
            childProperties.put(propertyKey, descriptor.latestValue());
        }
        boolean statusMirrorApplied = false;
        String statusMirrorStrategy = relationRule == null ? null
                : LegacyDpChildTemplateSupport.normalizeStrategy(relationRule.statusMirrorStrategy());
        Object sensorState = STATUS_MIRROR_STRATEGY_SENSOR_STATE.equalsIgnoreCase(statusMirrorStrategy)
                ? LegacyDpChildTemplateSupport.resolveSensorState(context.logicalCode(), context.parentProperties())
                : null;
        if (sensorState != null) {
            childProperties.put(CHILD_SENSOR_STATE_PROPERTY, sensorState);
            statusMirrorApplied = true;
        }
        return new LegacyDpChildTemplateExecutionResult(
                TEMPLATE_CODE,
                childProperties,
                List.of(context.logicalCode()),
                statusMirrorApplied,
                canonicalizationStrategy,
                descriptor.timestamp(),
                descriptor.rawPayload()
        );
    }

    private LegacyDpLogicalPayloadDescriptor describe(LegacyDpChildTemplateContext context) {
        return context == null ? null
                : LegacyDpChildTemplateSupport.describe(context.logicalCode(), context.logicalPayload());
    }
}
