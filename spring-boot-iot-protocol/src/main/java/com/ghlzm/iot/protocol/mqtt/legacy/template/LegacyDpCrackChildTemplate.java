package com.ghlzm.iot.protocol.mqtt.legacy.template;

import com.ghlzm.iot.protocol.core.model.ProtocolMetricEvidence;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpRelationRule;
import java.util.ArrayList;
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
                    TEMPLATE_CODE, Map.of(), List.of(), false, null, null, null, List.of()
            );
        }
        LegacyDpRelationRule relationRule = context.relationRule();
        String canonicalizationStrategy = relationRule == null ? null
                : LegacyDpChildTemplateSupport.normalizeStrategy(relationRule.canonicalizationStrategy());
        Map<String, Object> childProperties = new LinkedHashMap<>();
        List<ProtocolMetricEvidence> metricEvidence = new ArrayList<>();
        if (descriptor.latestValue() != null) {
            String propertyKey = CANONICALIZATION_STRATEGY_LF_VALUE.equalsIgnoreCase(canonicalizationStrategy)
                    ? CHILD_CRACK_VALUE_PROPERTY
                    : context.logicalCode();
            childProperties.put(propertyKey, descriptor.latestValue());
            metricEvidence.add(buildMetricEvidence(
                    context.logicalCode(),
                    propertyKey,
                    context.logicalCode(),
                    relationRule == null ? null : relationRule.childDeviceCode(),
                    descriptor.latestValue()
            ));
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
            metricEvidence.add(buildMetricEvidence(
                    "S1_ZT_1.sensor_state." + context.logicalCode(),
                    CHILD_SENSOR_STATE_PROPERTY,
                    context.logicalCode(),
                    relationRule == null ? null : relationRule.childDeviceCode(),
                    sensorState
            ));
        }
        return new LegacyDpChildTemplateExecutionResult(
                TEMPLATE_CODE,
                childProperties,
                List.of(context.logicalCode()),
                statusMirrorApplied,
                canonicalizationStrategy,
                descriptor.timestamp(),
                descriptor.rawPayload(),
                metricEvidence
        );
    }

    private ProtocolMetricEvidence buildMetricEvidence(String rawIdentifier,
                                                       String canonicalIdentifier,
                                                       String logicalChannelCode,
                                                       String childDeviceCode,
                                                       Object sampleValue) {
        ProtocolMetricEvidence evidence = new ProtocolMetricEvidence();
        evidence.setRawIdentifier(rawIdentifier);
        evidence.setCanonicalIdentifier(canonicalIdentifier);
        evidence.setLogicalChannelCode(logicalChannelCode);
        evidence.setChildDeviceCode(childDeviceCode);
        evidence.setSampleValue(sampleValue == null ? null : String.valueOf(sampleValue));
        evidence.setValueType(resolveValueType(sampleValue));
        evidence.setEvidenceOrigin("legacy_dp_child_template");
        return evidence;
    }

    private String resolveValueType(Object sampleValue) {
        if (sampleValue == null) {
            return null;
        }
        if (sampleValue instanceof Integer || sampleValue instanceof Long) {
            return "integer";
        }
        if (sampleValue instanceof Number) {
            return "double";
        }
        if (sampleValue instanceof Boolean) {
            return "bool";
        }
        return "string";
    }

    private LegacyDpLogicalPayloadDescriptor describe(LegacyDpChildTemplateContext context) {
        return context == null ? null
                : LegacyDpChildTemplateSupport.describe(context.logicalCode(), context.logicalPayload());
    }
}
