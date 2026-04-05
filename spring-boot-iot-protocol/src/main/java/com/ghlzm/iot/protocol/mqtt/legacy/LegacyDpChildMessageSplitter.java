package com.ghlzm.iot.protocol.mqtt.legacy;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplateContext;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplateExecutionResult;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplateExecutor;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplateMatcher;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplateRegistry;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * `$dp` 基站一包多测点子消息拆分器。
 */
public class LegacyDpChildMessageSplitter {

    private static final String DEEP_DISPLACEMENT_PARENT_STATUS_FAMILY = "S1_ZT_1";
    private static final String PARENT_SENSOR_STATE_PROPERTY_PREFIX = "S1_ZT_1.sensor_state.";
    private static final String CHILD_SENSOR_STATE_PROPERTY = "sensor_state";
    private static final String CHILD_CRACK_VALUE_PROPERTY = "value";
    private static final String CANONICALIZATION_STRATEGY_LEGACY = "LEGACY";
    private static final String CANONICALIZATION_STRATEGY_LF_VALUE = "LF_VALUE";
    private static final String STATUS_MIRROR_STRATEGY_NONE = "NONE";
    private static final String STATUS_MIRROR_STRATEGY_SENSOR_STATE = "SENSOR_STATE";
    private static final Pattern DEEP_DISPLACEMENT_LOGICAL_CODE_PATTERN = Pattern.compile("^L1_SW_\\d+$");
    private static final Pattern CRACK_LOGICAL_CODE_PATTERN = Pattern.compile("^L1_LF_\\d+$");

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final LegacyDpFamilyResolver familyResolver = new LegacyDpFamilyResolver();
    private final IotProperties iotProperties;
    private final LegacyDpRelationResolver relationResolver;
    private final LegacyDpChildTemplateMatcher templateMatcher;
    private final LegacyDpChildTemplateExecutor templateExecutor;

    public LegacyDpChildMessageSplitter(IotProperties iotProperties) {
        this(iotProperties, LegacyDpRelationResolver.noop());
    }

    public LegacyDpChildMessageSplitter(IotProperties iotProperties, LegacyDpRelationResolver relationResolver) {
        this.iotProperties = iotProperties;
        this.relationResolver = relationResolver == null ? LegacyDpRelationResolver.noop() : relationResolver;
        LegacyDpChildTemplateRegistry templateRegistry = new LegacyDpChildTemplateRegistry();
        this.templateMatcher = new LegacyDpChildTemplateMatcher(templateRegistry);
        this.templateExecutor = new LegacyDpChildTemplateExecutor();
    }

    public LegacyDpNormalizeResult split(Map<String, Object> payload,
                                         DeviceUpMessage parentMessage,
                                         LegacyDpNormalizeResult normalizeResult) {
        LegacyDpNormalizeResult result = normalizeResult == null ? new LegacyDpNormalizeResult() : normalizeResult;
        if (parentMessage == null) {
            result.setChildMessages(List.of());
            result.setChildSplitApplied(Boolean.FALSE);
            return result;
        }

        List<LegacyDpRelationRule> relationRules = resolveRelationRules(parentMessage.getDeviceCode());
        if (relationRules.isEmpty()) {
            result.setProperties(collapseStandaloneDeepDisplacementProperties(parentMessage, result));
            result.setChildMessages(List.of());
            result.setChildSplitApplied(Boolean.FALSE);
            return result;
        }

        Object basePayload = parentMessage.getDeviceCode() == null ? null : payload.get(parentMessage.getDeviceCode());
        if (!(basePayload instanceof Map<?, ?> basePayloadMap)) {
            result.setChildMessages(List.of());
            result.setChildSplitApplied(Boolean.FALSE);
            return result;
        }

        List<DeviceUpMessage> childMessages = new ArrayList<>();
        List<String> parentRemovalKeys = new ArrayList<>();
        for (LegacyDpRelationRule rule : relationRules) {
            String logicalCode = rule.logicalChannelCode();
            String childDeviceCode = rule.childDeviceCode();
            if (logicalCode == null || logicalCode.isBlank() || childDeviceCode == null || childDeviceCode.isBlank()) {
                continue;
            }

            LegacyDpChildTemplateContext templateContext = new LegacyDpChildTemplateContext(
                    rule,
                    logicalCode,
                    basePayloadMap.get(logicalCode),
                    result.getProperties()
            );
            LegacyDpChildTemplateExecutionResult templateExecution = templateMatcher.match(templateContext)
                    .map(template -> templateExecutor.execute(template, templateContext))
                    .orElse(null);

            Map<String, Object> childProperties;
            LocalDateTime childTimestamp;
            String childRawPayload;
            if (templateExecution != null && templateExecution.childProperties() != null
                    && !templateExecution.childProperties().isEmpty()) {
                childProperties = templateExecution.childProperties();
                childTimestamp = templateExecution.childTimestamp();
                childRawPayload = templateExecution.rawPayload();
                parentRemovalKeys.addAll(templateExecution.parentRemovalKeys());
            } else {
                LatestLogicalPayload latestLogicalPayload = extractLatestLogicalPayload(rule, basePayloadMap.get(logicalCode));
                if (latestLogicalPayload == null || latestLogicalPayload.properties().isEmpty()) {
                    continue;
                }
                childProperties = mergeDerivedChildProperties(
                        rule,
                        latestLogicalPayload.properties(),
                        result.getProperties()
                );
                childTimestamp = latestLogicalPayload.timestamp();
                childRawPayload = latestLogicalPayload.rawPayload();
                parentRemovalKeys.add(logicalCode);
            }
            DeviceUpMessage childMessage = new DeviceUpMessage();
            childMessage.setTenantId(parentMessage.getTenantId());
            childMessage.setProductKey(parentMessage.getProductKey());
            childMessage.setDeviceCode(childDeviceCode);
            childMessage.setMessageType(parentMessage.getMessageType());
            childMessage.setTopic(parentMessage.getTopic());
            childMessage.setTimestamp(childTimestamp == null
                    ? parentMessage.getTimestamp()
                    : childTimestamp);
            childMessage.setProperties(childProperties);
            childMessage.setRawPayload(childRawPayload);
            childMessages.add(childMessage);
        }

        result.setChildMessages(childMessages);
        if (childMessages.isEmpty()) {
            result.setChildSplitApplied(Boolean.FALSE);
            return result;
        }

        result.setChildSplitApplied(Boolean.TRUE);
        result.setProperties(removeChildLogicalProperties(result.getProperties(), parentRemovalKeys));
        return result;
    }

    private List<LegacyDpRelationRule> resolveRelationRules(String baseDeviceCode) {
        if (baseDeviceCode == null || baseDeviceCode.isBlank()) {
            return List.of();
        }
        List<LegacyDpRelationRule> registryRules = sanitizeRelationRules(relationResolver.listRulesByParentDeviceCode(baseDeviceCode));
        if (!registryRules.isEmpty()) {
            return registryRules;
        }
        Map<String, String> configuredMappings = resolveLegacySubDeviceMappings(baseDeviceCode);
        if (configuredMappings.isEmpty()) {
            return List.of();
        }
        return configuredMappings.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank()
                        && entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> new LegacyDpRelationRule(
                        entry.getKey(),
                        entry.getValue(),
                        resolveLegacyCanonicalizationStrategy(entry.getKey()),
                        resolveLegacyStatusMirrorStrategy(entry.getKey())
                ))
                .sorted(Comparator.comparing(LegacyDpRelationRule::logicalChannelCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private Map<String, String> resolveLegacySubDeviceMappings(String baseDeviceCode) {
        if (baseDeviceCode == null || baseDeviceCode.isBlank() || iotProperties.getDevice() == null
                || iotProperties.getDevice().getSubDeviceMappings() == null) {
            return Map.of();
        }
        Map<String, String> configuredMappings = iotProperties.getDevice().getSubDeviceMappings().get(baseDeviceCode);
        return configuredMappings == null || configuredMappings.isEmpty() ? Map.of() : configuredMappings;
    }

    private List<LegacyDpRelationRule> sanitizeRelationRules(List<LegacyDpRelationRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        return rules.stream()
                .filter(rule -> rule != null
                        && rule.logicalChannelCode() != null
                        && !rule.logicalChannelCode().isBlank()
                        && rule.childDeviceCode() != null
                        && !rule.childDeviceCode().isBlank())
                .sorted(Comparator.comparing(LegacyDpRelationRule::logicalChannelCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private String resolveLegacyCanonicalizationStrategy(String logicalCode) {
        return isCrackLogicalCode(logicalCode) ? CANONICALIZATION_STRATEGY_LF_VALUE : CANONICALIZATION_STRATEGY_LEGACY;
    }

    private String resolveLegacyStatusMirrorStrategy(String logicalCode) {
        return isCrackLogicalCode(logicalCode) ? STATUS_MIRROR_STRATEGY_SENSOR_STATE : STATUS_MIRROR_STRATEGY_NONE;
    }

    private Map<String, Object> collapseStandaloneDeepDisplacementProperties(DeviceUpMessage parentMessage,
                                                                             LegacyDpNormalizeResult result) {
        if (parentMessage == null
                || !"$dp".equals(parentMessage.getTopic())
                || result == null
                || result.getProperties() == null
                || result.getProperties().isEmpty()) {
            return result == null ? Map.of() : result.getProperties();
        }

        List<String> deepDisplacementLogicalCodes = result.getFamilyCodes() == null
                ? List.of()
                : result.getFamilyCodes().stream()
                .filter(this::isDeepDisplacementLogicalCode)
                .toList();
        if (deepDisplacementLogicalCodes.size() != 1) {
            return result.getProperties();
        }

        String logicalCode = deepDisplacementLogicalCodes.get(0);
        if (!containsOnlyStandaloneDeepDisplacementFamilies(result.getFamilyCodes(), logicalCode)) {
            return result.getProperties();
        }
        if (!containsOnlyStandaloneDeepDisplacementLogicalCode(result.getProperties(), logicalCode)) {
            return result.getProperties();
        }
        String logicalPrefix = logicalCode + ".";
        Map<String, Object> collapsedProperties = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : result.getProperties().entrySet()) {
            String key = entry.getKey();
            if (key != null && key.startsWith(logicalPrefix)) {
                collapsedProperties.put(key.substring(logicalPrefix.length()), entry.getValue());
                continue;
            }
            collapsedProperties.put(key, entry.getValue());
        }
        return collapsedProperties;
    }

    private boolean isDeepDisplacementLogicalCode(String familyCode) {
        return familyCode != null && DEEP_DISPLACEMENT_LOGICAL_CODE_PATTERN.matcher(familyCode).matches();
    }

    private boolean isCrackLogicalCode(String logicalCode) {
        return logicalCode != null && CRACK_LOGICAL_CODE_PATTERN.matcher(logicalCode).matches();
    }

    private boolean containsOnlyStandaloneDeepDisplacementFamilies(List<String> familyCodes, String logicalCode) {
        if (familyCodes == null || familyCodes.isEmpty() || logicalCode == null || logicalCode.isBlank()) {
            return false;
        }
        for (String familyCode : familyCodes) {
            if (logicalCode.equals(familyCode) || DEEP_DISPLACEMENT_PARENT_STATUS_FAMILY.equals(familyCode)) {
                continue;
            }
            return false;
        }
        return true;
    }

    private boolean containsOnlyStandaloneDeepDisplacementLogicalCode(Map<String, Object> properties, String logicalCode) {
        if (properties == null || properties.isEmpty() || logicalCode == null || logicalCode.isBlank()) {
            return false;
        }
        String logicalPrefix = logicalCode + ".";
        boolean aliased = false;
        for (String key : properties.keySet()) {
            if (key == null || key.isBlank()) {
                continue;
            }
            if (key.startsWith(logicalPrefix)) {
                aliased = true;
                continue;
            }
            if (isDeepDisplacementLogicalProperty(key)) {
                return false;
            }
        }
        return aliased;
    }

    private boolean isDeepDisplacementLogicalProperty(String propertyKey) {
        if (propertyKey == null || propertyKey.isBlank()) {
            return false;
        }
        int separatorIndex = propertyKey.indexOf('.');
        if (separatorIndex <= 0) {
            return false;
        }
        return isDeepDisplacementLogicalCode(propertyKey.substring(0, separatorIndex));
    }

    private LatestLogicalPayload extractLatestLogicalPayload(LegacyDpRelationRule rule, Object logicalPayload) {
        String logicalCode = rule == null ? null : rule.logicalChannelCode();
        if (logicalPayload == null) {
            return null;
        }

        LocalDateTime logicalTimestamp = null;
        Object latestValue = logicalPayload;
        String rawPayload = writeLogicalRawPayload(logicalCode, logicalPayload);
        if (logicalPayload instanceof Map<?, ?> logicalMap && isTimestampContainer(logicalMap)) {
            TimestampedValue latestEntry = selectLatestTimestampValue(logicalMap);
            if (latestEntry == null) {
                return null;
            }
            logicalTimestamp = latestEntry.timestamp();
            latestValue = latestEntry.value();
            Map<String, Object> latestPayload = new LinkedHashMap<>();
            latestPayload.put(latestEntry.key(), latestEntry.value());
            rawPayload = writeLogicalRawPayload(logicalCode, latestPayload);
        }

        Map<String, Object> properties = toLogicalProperties(rule, latestValue);
        return properties.isEmpty() ? null : new LatestLogicalPayload(logicalTimestamp, properties, rawPayload);
    }

    private boolean isTimestampContainer(Map<?, ?> source) {
        if (source.isEmpty()) {
            return false;
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !familyResolver.isTimestampKey(key)) {
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
            LocalDateTime timestamp = familyResolver.parseTimestamp(key);
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

    private Map<String, Object> toLogicalProperties(LegacyDpRelationRule rule, Object value) {
        Map<String, Object> properties = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> mapValue) {
            flattenChildProperties("", mapValue, properties);
            return properties;
        }
        if (value != null) {
            String propertyKey = resolveChildScalarPropertyKey(rule);
            if (propertyKey != null && !propertyKey.isBlank()) {
                properties.put(propertyKey, value);
            }
        }
        return properties;
    }

    private String resolveChildScalarPropertyKey(LegacyDpRelationRule rule) {
        if (rule == null || rule.logicalChannelCode() == null || rule.logicalChannelCode().isBlank()) {
            return null;
        }
        return CANONICALIZATION_STRATEGY_LF_VALUE.equalsIgnoreCase(normalizeStrategy(rule.canonicalizationStrategy()))
                ? CHILD_CRACK_VALUE_PROPERTY
                : rule.logicalChannelCode();
    }

    private Map<String, Object> mergeDerivedChildProperties(LegacyDpRelationRule rule,
                                                            Map<String, Object> logicalProperties,
                                                            Map<String, Object> parentProperties) {
        Map<String, Object> mergedProperties = new LinkedHashMap<>();
        if (logicalProperties != null && !logicalProperties.isEmpty()) {
            mergedProperties.putAll(logicalProperties);
        }
        Object sensorState = resolveDerivedChildSensorState(rule, parentProperties);
        if (sensorState != null) {
            mergedProperties.put(CHILD_SENSOR_STATE_PROPERTY, sensorState);
        }
        return mergedProperties;
    }

    private Object resolveDerivedChildSensorState(LegacyDpRelationRule rule, Map<String, Object> parentProperties) {
        if (rule == null
                || !STATUS_MIRROR_STRATEGY_SENSOR_STATE.equalsIgnoreCase(normalizeStrategy(rule.statusMirrorStrategy()))
                || parentProperties == null
                || parentProperties.isEmpty()) {
            return null;
        }
        return parentProperties.get(PARENT_SENSOR_STATE_PROPERTY_PREFIX + rule.logicalChannelCode());
    }

    private String normalizeStrategy(String strategy) {
        return strategy == null ? null : strategy.trim().toUpperCase();
    }

    private void flattenChildProperties(String prefix, Map<?, ?> source, Map<String, Object> target) {
        if (isTimestampContainer(source)) {
            TimestampedValue latestEntry = selectLatestTimestampValue(source);
            if (latestEntry == null) {
                return;
            }
            Object latestValue = latestEntry.value();
            if (latestValue instanceof Map<?, ?> latestMap) {
                flattenChildProperties(prefix, latestMap, target);
            } else if (prefix != null && !prefix.isBlank()) {
                target.put(prefix, latestValue);
            }
            return;
        }

        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            String field = prefix == null || prefix.isBlank() ? key : prefix + "." + key;
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nestedMap) {
                flattenChildProperties(field, nestedMap, target);
                continue;
            }
            target.put(field, value);
        }
    }

    private String writeLogicalRawPayload(String logicalCode, Object logicalPayload) {
        try {
            Map<String, Object> rawPayload = new LinkedHashMap<>();
            rawPayload.put(logicalCode, logicalPayload);
            return objectMapper.writeValueAsString(rawPayload);
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Object> removeChildLogicalProperties(Map<String, Object> properties, List<String> logicalCodes) {
        if (properties == null || properties.isEmpty() || logicalCodes == null || logicalCodes.isEmpty()) {
            return properties;
        }
        Map<String, Object> filteredProperties = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (isChildLogicalProperty(entry.getKey(), logicalCodes)) {
                continue;
            }
            filteredProperties.put(entry.getKey(), entry.getValue());
        }
        return filteredProperties;
    }

    private boolean isChildLogicalProperty(String propertyKey, List<String> logicalCodes) {
        if (propertyKey == null || propertyKey.isBlank()) {
            return false;
        }
        for (String logicalCode : logicalCodes) {
            if (logicalCode != null && !logicalCode.isBlank()
                    && (propertyKey.equals(logicalCode) || propertyKey.startsWith(logicalCode + "."))) {
                return true;
            }
        }
        return false;
    }

    private record LatestLogicalPayload(LocalDateTime timestamp,
                                        Map<String, Object> properties,
                                        String rawPayload) {
    }

    private record TimestampedValue(String key,
                                    LocalDateTime timestamp,
                                    Object value) {
    }
}
