package com.ghlzm.iot.protocol.mqtt.legacy.template.runtime;

import com.ghlzm.iot.protocol.core.model.ProtocolMetricEvidence;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpFamilyResolver;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpRelationRule;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplate;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplateContext;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplateExecutionResult;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigDrivenLegacyDpChildTemplate implements LegacyDpChildTemplate {

    private static final String CHILD_SENSOR_STATE_PROPERTY = "sensor_state";
    private static final String PARENT_SENSOR_STATE_PROPERTY_PREFIX = "S1_ZT_1.sensor_state.";

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();
    private static final LegacyDpFamilyResolver FAMILY_RESOLVER = new LegacyDpFamilyResolver();

    private final CompiledLegacyDpTemplate compiledTemplate;

    public ConfigDrivenLegacyDpChildTemplate(CompiledLegacyDpTemplate compiledTemplate) {
        this.compiledTemplate = compiledTemplate;
    }

    @Override
    public String getTemplateCode() {
        return compiledTemplate == null ? null : compiledTemplate.templateCode();
    }

    @Override
    public boolean matches(LegacyDpChildTemplateContext context) {
        LogicalPayloadDescriptor descriptor = describe(context);
        return descriptor != null
                && context != null
                && StringUtils.hasText(context.logicalCode())
                && compiledTemplate != null
                && compiledTemplate.logicalPattern().matcher(context.logicalCode()).matches()
                && matchesShape(descriptor.objectPayload());
    }

    @Override
    public LegacyDpChildTemplateExecutionResult execute(LegacyDpChildTemplateContext context) {
        LogicalPayloadDescriptor descriptor = describe(context);
        if (descriptor == null || context == null || compiledTemplate == null) {
            return new LegacyDpChildTemplateExecutionResult(
                    getTemplateCode(), Map.of(), List.of(), false, null, null, null, List.of()
            );
        }
        LegacyDpRelationRule relationRule = context.relationRule();
        Map<String, Object> childProperties = new LinkedHashMap<>();
        List<ProtocolMetricEvidence> metricEvidence = new ArrayList<>();
        for (Map.Entry<String, String> entry : compiledTemplate.outputMappings().entrySet()) {
            Object value = resolveMappedValue(descriptor.latestValue(), entry.getValue());
            if (value == null) {
                continue;
            }
            childProperties.put(entry.getKey(), value);
            metricEvidence.add(buildMetricEvidence(
                    resolveRawIdentifier(context.logicalCode(), descriptor.latestValue(), entry.getValue()),
                    entry.getKey(),
                    context.logicalCode(),
                    relationRule == null ? null : relationRule.childDeviceCode(),
                    value
            ));
        }
        boolean statusMirrorApplied = false;
        Object sensorState = shouldMirrorSensorState(relationRule)
                ? resolveSensorState(context.logicalCode(), context.parentProperties())
                : null;
        if (sensorState != null && !childProperties.containsKey(CHILD_SENSOR_STATE_PROPERTY)) {
            childProperties.put(CHILD_SENSOR_STATE_PROPERTY, sensorState);
            statusMirrorApplied = true;
            metricEvidence.add(buildMetricEvidence(
                    PARENT_SENSOR_STATE_PROPERTY_PREFIX + context.logicalCode(),
                    CHILD_SENSOR_STATE_PROPERTY,
                    context.logicalCode(),
                    relationRule == null ? null : relationRule.childDeviceCode(),
                    sensorState
            ));
        }
        List<String> parentRemovalKeys = new ArrayList<>();
        if (StringUtils.hasText(context.logicalCode())) {
            parentRemovalKeys.add(context.logicalCode());
        }
        if (statusMirrorApplied && compiledTemplate.objectPayload()) {
            parentRemovalKeys.add(PARENT_SENSOR_STATE_PROPERTY_PREFIX + context.logicalCode());
        }
        return new LegacyDpChildTemplateExecutionResult(
                getTemplateCode(),
                childProperties,
                parentRemovalKeys,
                statusMirrorApplied,
                normalizeStrategy(relationRule == null ? null : relationRule.canonicalizationStrategy()),
                descriptor.timestamp(),
                descriptor.rawPayload(),
                metricEvidence
        );
    }

    private boolean matchesShape(boolean objectPayload) {
        return compiledTemplate.objectPayload() == objectPayload;
    }

    private boolean shouldMirrorSensorState(LegacyDpRelationRule relationRule) {
        return relationRule != null
                && "SENSOR_STATE".equalsIgnoreCase(normalizeStrategy(relationRule.statusMirrorStrategy()));
    }

    private Object resolveSensorState(String logicalCode, Map<String, Object> parentProperties) {
        if (!StringUtils.hasText(logicalCode) || parentProperties == null || parentProperties.isEmpty()) {
            return null;
        }
        return parentProperties.get(PARENT_SENSOR_STATE_PROPERTY_PREFIX + logicalCode);
    }

    private Object resolveMappedValue(Object latestValue, String sourcePath) {
        String normalizedPath = normalizeText(sourcePath);
        if (!StringUtils.hasText(normalizedPath)) {
            return null;
        }
        if (isScalarRootPath(normalizedPath)) {
            if (latestValue instanceof Map<?, ?> mapValue && mapValue.containsKey("value")) {
                return mapValue.get("value");
            }
            return latestValue;
        }
        Object current = latestValue;
        for (String segment : trimRootPath(normalizedPath).split("\\.")) {
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            if (!(current instanceof Map<?, ?> mapValue)) {
                return null;
            }
            current = mapValue.get(segment);
        }
        return current;
    }

    private String resolveRawIdentifier(String logicalCode, Object latestValue, String sourcePath) {
        if (!StringUtils.hasText(logicalCode)) {
            return null;
        }
        String normalizedPath = normalizeText(sourcePath);
        if (!StringUtils.hasText(normalizedPath) || isScalarRootPath(normalizedPath)) {
            if (!(latestValue instanceof Map<?, ?>)) {
                return logicalCode;
            }
            if ("$.value".equals(normalizedPath) || "value".equalsIgnoreCase(normalizedPath)) {
                return logicalCode;
            }
        }
        String suffix = trimRootPath(normalizedPath);
        return StringUtils.hasText(suffix) ? logicalCode + "." + suffix : logicalCode;
    }

    private boolean isScalarRootPath(String sourcePath) {
        return "$".equals(sourcePath) || "$.value".equals(sourcePath) || "value".equalsIgnoreCase(sourcePath);
    }

    private String trimRootPath(String sourcePath) {
        if (!StringUtils.hasText(sourcePath)) {
            return "";
        }
        if (sourcePath.startsWith("$.")) {
            return sourcePath.substring(2);
        }
        if (sourcePath.startsWith("$")) {
            return sourcePath.substring(1);
        }
        return sourcePath;
    }

    private LogicalPayloadDescriptor describe(LegacyDpChildTemplateContext context) {
        if (context == null || context.logicalPayload() == null) {
            return null;
        }
        String rawPayload = writeLogicalRawPayload(context.logicalCode(), context.logicalPayload());
        if (context.logicalPayload() instanceof Map<?, ?> logicalMap && isTimestampContainer(logicalMap)) {
            TimestampedValue latestEntry = selectLatestTimestampValue(logicalMap);
            if (latestEntry == null) {
                return null;
            }
            return new LogicalPayloadDescriptor(
                    latestEntry.timestamp(),
                    latestEntry.value() instanceof Map<?, ?>,
                    latestEntry.value(),
                    rawPayload
            );
        }
        return new LogicalPayloadDescriptor(
                null,
                context.logicalPayload() instanceof Map<?, ?>,
                context.logicalPayload(),
                rawPayload
        );
    }

    private boolean isTimestampContainer(Map<?, ?> source) {
        if (source.isEmpty()) {
            return false;
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !FAMILY_RESOLVER.isTimestampKey(key)) {
                return false;
            }
        }
        return true;
    }

    private TimestampedValue selectLatestTimestampValue(Map<?, ?> source) {
        List<TimestampedValue> timestampedValues = new ArrayList<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            LocalDateTime timestamp = FAMILY_RESOLVER.parseTimestamp(key);
            if (timestamp != null) {
                timestampedValues.add(new TimestampedValue(key, timestamp, entry.getValue()));
            }
        }
        if (timestampedValues.isEmpty()) {
            return null;
        }
        timestampedValues.sort(Comparator.comparing(TimestampedValue::timestamp).thenComparing(TimestampedValue::key));
        return timestampedValues.get(timestampedValues.size() - 1);
    }

    private String writeLogicalRawPayload(String logicalCode, Object logicalPayload) {
        try {
            Map<String, Object> rawPayload = new LinkedHashMap<>();
            rawPayload.put(logicalCode, logicalPayload);
            return OBJECT_MAPPER.writeValueAsString(rawPayload);
        } catch (Exception ex) {
            return null;
        }
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
        evidence.setEvidenceOrigin("legacy_dp_template_runtime");
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

    private String normalizeStrategy(String strategy) {
        return strategy == null ? null : strategy.trim().toUpperCase();
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record LogicalPayloadDescriptor(LocalDateTime timestamp,
                                            boolean objectPayload,
                                            Object latestValue,
                                            String rawPayload) {
    }

    private record TimestampedValue(String key,
                                    LocalDateTime timestamp,
                                    Object value) {
    }
}
