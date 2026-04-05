package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.vo.ProductModelCandidateVO;
import com.ghlzm.iot.device.vo.ProductModelProtocolTemplateEvidenceVO;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpProtocolMetadata;
import com.ghlzm.iot.protocol.core.model.ProtocolTemplateEvidence;
import com.ghlzm.iot.protocol.core.model.ProtocolTemplateExecutionEvidence;
import com.ghlzm.iot.protocol.core.registry.ProtocolAdapterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 为产品治理运行期候选补齐协议模板执行证据。
 */
final class ProductModelProtocolTemplateEvidenceResolver {

    private static final String MQTT_JSON_PROTOCOL_CODE = "mqtt-json";
    private static final Set<String> PROPERTY_LOG_TYPES = Set.of("property", "status");

    private final ProtocolAdapterRegistry protocolAdapterRegistry;
    private final ProductModelPropertyCandidateFilter propertyCandidateFilter = new ProductModelPropertyCandidateFilter();

    ProductModelProtocolTemplateEvidenceResolver(ProtocolAdapterRegistry protocolAdapterRegistry) {
        this.protocolAdapterRegistry = protocolAdapterRegistry;
    }

    void attach(Product product,
                List<Device> devices,
                List<DeviceProperty> properties,
                List<DeviceMessageLog> logs,
                List<ProductModelCandidateVO> candidates) {
        if (protocolAdapterRegistry == null
                || product == null
                || candidates == null
                || candidates.isEmpty()
                || logs == null
                || logs.isEmpty()) {
            return;
        }
        ProtocolAdapter adapter = resolveAdapter(product, logs);
        if (adapter == null) {
            return;
        }

        Map<String, ProductModelCandidateVO> candidatesByIdentifier = new LinkedHashMap<>();
        Map<String, List<String>> candidateIdentifiersByObservedKey = new LinkedHashMap<>();
        for (ProductModelCandidateVO candidate : candidates) {
            String identifier = normalize(candidate == null ? null : candidate.getIdentifier());
            if (identifier == null) {
                continue;
            }
            candidatesByIdentifier.put(identifier, candidate);
            addObservedKey(candidateIdentifiersByObservedKey, identifier, identifier);
            for (String rawIdentifier : safeList(candidate.getRawIdentifiers())) {
                addObservedKey(candidateIdentifiersByObservedKey, rawIdentifier, identifier);
                ProductModelPropertyCandidateFilter.NormalizedPropertyIdentifier normalizedIdentifier =
                        propertyCandidateFilter.normalizeIdentifier(rawIdentifier);
                addObservedKey(candidateIdentifiersByObservedKey, normalizedIdentifier.identifier(), identifier);
            }
        }
        if (candidatesByIdentifier.isEmpty()) {
            return;
        }

        Map<Long, String> deviceCodeById = new LinkedHashMap<>();
        for (Device device : devices == null ? List.<Device>of() : devices) {
            if (device == null || device.getId() == null) {
                continue;
            }
            String deviceCode = normalize(device.getDeviceCode());
            if (deviceCode != null) {
                deviceCodeById.put(device.getId(), deviceCode);
            }
        }

        Map<String, Set<String>> propertyCandidateIdentifiersByDeviceCode =
                buildPropertyCandidateIdentifiersByDeviceCode(properties, deviceCodeById, candidateIdentifiersByObservedKey);
        Map<String, TemplateEvidenceAccumulator> evidenceByIdentifier = new LinkedHashMap<>();

        for (DeviceMessageLog log : logs) {
            if (!isPropertyLog(log)) {
                continue;
            }
            try {
                DeviceUpMessage decodedMessage = adapter.decode(
                        safePayload(log.getPayload()),
                        buildProtocolContext(product, log)
                );
                DeviceUpMessage targetMessage = resolveTargetMessage(decodedMessage, log);
                if (targetMessage == null || targetMessage.getProperties() == null || targetMessage.getProperties().isEmpty()) {
                    continue;
                }
                Set<String> matchedIdentifiers = new LinkedHashSet<>();
                for (String propertyIdentifier : targetMessage.getProperties().keySet()) {
                    matchedIdentifiers.addAll(resolveCandidateIdentifiers(propertyIdentifier, candidateIdentifiersByObservedKey));
                }
                if (matchedIdentifiers.isEmpty()) {
                    continue;
                }
                List<ProtocolTemplateExecutionEvidence> relevantExecutions = resolveRelevantExecutions(targetMessage);
                for (String identifier : matchedIdentifiers) {
                    TemplateEvidenceAccumulator accumulator = evidenceByIdentifier.computeIfAbsent(
                            identifier,
                            ignored -> new TemplateEvidenceAccumulator()
                    );
                    accumulator.markAttempted();
                    accumulator.acceptExecutions(relevantExecutions);
                }
            } catch (RuntimeException ex) {
                for (String identifier : propertyCandidateIdentifiersByDeviceCode.getOrDefault(normalize(log.getDeviceCode()), Set.of())) {
                    evidenceByIdentifier.computeIfAbsent(identifier, ignored -> new TemplateEvidenceAccumulator())
                            .incrementDecodeFailure();
                }
            }
        }

        for (Map.Entry<String, TemplateEvidenceAccumulator> entry : evidenceByIdentifier.entrySet()) {
            ProductModelCandidateVO candidate = candidatesByIdentifier.get(entry.getKey());
            if (candidate != null && entry.getValue().wasAttempted()) {
                candidate.setProtocolTemplateEvidence(entry.getValue().toValueObject());
            }
        }
    }

    private Map<String, Set<String>> buildPropertyCandidateIdentifiersByDeviceCode(List<DeviceProperty> properties,
                                                                                   Map<Long, String> deviceCodeById,
                                                                                   Map<String, List<String>> candidateIdentifiersByObservedKey) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (DeviceProperty property : properties == null ? List.<DeviceProperty>of() : properties) {
            String deviceCode = property == null ? null : deviceCodeById.get(property.getDeviceId());
            if (deviceCode == null) {
                continue;
            }
            Set<String> identifiers = result.computeIfAbsent(deviceCode, ignored -> new LinkedHashSet<>());
            identifiers.addAll(resolveCandidateIdentifiers(property == null ? null : property.getIdentifier(), candidateIdentifiersByObservedKey));
        }
        return result;
    }

    private Set<String> resolveCandidateIdentifiers(String observedIdentifier,
                                                    Map<String, List<String>> candidateIdentifiersByObservedKey) {
        Set<String> matchedIdentifiers = new LinkedHashSet<>();
        String normalizedObservedIdentifier = normalize(observedIdentifier);
        if (normalizedObservedIdentifier == null) {
            return matchedIdentifiers;
        }
        matchedIdentifiers.addAll(candidateIdentifiersByObservedKey.getOrDefault(normalizedObservedIdentifier, List.of()));
        ProductModelPropertyCandidateFilter.NormalizedPropertyIdentifier normalizedIdentifier =
                propertyCandidateFilter.normalizeIdentifier(normalizedObservedIdentifier);
        String canonicalIdentifier = normalize(normalizedIdentifier.identifier());
        if (canonicalIdentifier != null) {
            matchedIdentifiers.addAll(candidateIdentifiersByObservedKey.getOrDefault(canonicalIdentifier, List.of()));
        }
        return matchedIdentifiers;
    }

    private void addObservedKey(Map<String, List<String>> mapping, String observedKey, String candidateIdentifier) {
        String normalizedObservedKey = normalize(observedKey);
        String normalizedCandidateIdentifier = normalize(candidateIdentifier);
        if (normalizedObservedKey == null || normalizedCandidateIdentifier == null) {
            return;
        }
        mapping.computeIfAbsent(normalizedObservedKey, ignored -> new ArrayList<>());
        if (!mapping.get(normalizedObservedKey).contains(normalizedCandidateIdentifier)) {
            mapping.get(normalizedObservedKey).add(normalizedCandidateIdentifier);
        }
    }

    private ProtocolAdapter resolveAdapter(Product product, List<DeviceMessageLog> logs) {
        List<String> protocolCodes = new ArrayList<>();
        addProtocolCode(protocolCodes, product == null ? null : product.getProtocolCode());
        if (logs.stream().anyMatch(this::looksLikeMqttJsonLog)) {
            addProtocolCode(protocolCodes, MQTT_JSON_PROTOCOL_CODE);
        }
        for (String protocolCode : protocolCodes) {
            ProtocolAdapter adapter = protocolAdapterRegistry.getAdapter(protocolCode);
            if (adapter != null) {
                return adapter;
            }
        }
        return null;
    }

    private void addProtocolCode(List<String> protocolCodes, String protocolCode) {
        String normalizedProtocolCode = normalize(protocolCode);
        if (normalizedProtocolCode != null && !protocolCodes.contains(normalizedProtocolCode)) {
            protocolCodes.add(normalizedProtocolCode);
        }
    }

    private boolean looksLikeMqttJsonLog(DeviceMessageLog log) {
        String topic = normalize(log == null ? null : log.getTopic());
        if ("$dp".equals(topic)) {
            return true;
        }
        return topic != null && topic.startsWith("/sys/");
    }

    private boolean isPropertyLog(DeviceMessageLog log) {
        String messageType = normalize(log == null ? null : log.getMessageType());
        return messageType != null && PROPERTY_LOG_TYPES.contains(messageType.toLowerCase(Locale.ROOT));
    }

    private ProtocolContext buildProtocolContext(Product product, DeviceMessageLog log) {
        ProtocolContext context = new ProtocolContext();
        context.setTenantCode(log == null || log.getTenantId() == null ? null : String.valueOf(log.getTenantId()));
        context.setProductKey(product == null ? null : product.getProductKey());
        context.setDeviceCode(shouldBypassContextDeviceCode(log) ? null : normalize(log == null ? null : log.getDeviceCode()));
        context.setMessageType(log == null ? null : log.getMessageType());
        context.setTopic(log == null ? null : log.getTopic());
        if ("$dp".equals(normalize(log == null ? null : log.getTopic()))) {
            context.setTopicRouteType("legacy");
        }
        return context;
    }

    private boolean shouldBypassContextDeviceCode(DeviceMessageLog log) {
        return "$dp".equals(normalize(log == null ? null : log.getTopic()));
    }

    private DeviceUpMessage resolveTargetMessage(DeviceUpMessage decodedMessage, DeviceMessageLog log) {
        if (decodedMessage == null) {
            return null;
        }
        String logDeviceCode = normalize(log == null ? null : log.getDeviceCode());
        if (logDeviceCode == null) {
            return decodedMessage;
        }
        if (logDeviceCode.equalsIgnoreCase(normalize(decodedMessage.getDeviceCode()))) {
            return decodedMessage;
        }
        for (DeviceUpMessage childMessage : safeList(decodedMessage.getChildMessages())) {
            if (logDeviceCode.equalsIgnoreCase(normalize(childMessage == null ? null : childMessage.getDeviceCode()))) {
                return childMessage;
            }
        }
        return decodedMessage;
    }

    private List<ProtocolTemplateExecutionEvidence> resolveRelevantExecutions(DeviceUpMessage targetMessage) {
        DeviceUpProtocolMetadata protocolMetadata = targetMessage == null ? null : targetMessage.getProtocolMetadata();
        ProtocolTemplateEvidence templateEvidence = protocolMetadata == null ? null : protocolMetadata.getTemplateEvidence();
        if (templateEvidence == null || templateEvidence.getExecutions() == null || templateEvidence.getExecutions().isEmpty()) {
            return List.of();
        }
        String targetDeviceCode = normalize(targetMessage.getDeviceCode());
        if (targetDeviceCode == null) {
            return List.copyOf(templateEvidence.getExecutions());
        }
        return templateEvidence.getExecutions().stream()
                .filter(execution -> {
                    String childDeviceCode = normalize(execution == null ? null : execution.getChildDeviceCode());
                    return childDeviceCode == null || targetDeviceCode.equalsIgnoreCase(childDeviceCode);
                })
                .toList();
    }

    private byte[] safePayload(String payload) {
        return (payload == null ? "" : payload).getBytes(StandardCharsets.UTF_8);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static final class TemplateEvidenceAccumulator {
        private final LinkedHashSet<String> templateCodes = new LinkedHashSet<>();
        private final LinkedHashSet<String> logicalChannelCodes = new LinkedHashSet<>();
        private final LinkedHashSet<String> childDeviceCodes = new LinkedHashSet<>();
        private final LinkedHashSet<String> canonicalizationStrategies = new LinkedHashSet<>();
        private final LinkedHashSet<String> parentRemovalKeys = new LinkedHashSet<>();
        private boolean statusMirrorApplied;
        private int templateExecutionCount;
        private int decodeFailureCount;
        private boolean attempted;

        private void markAttempted() {
            this.attempted = true;
        }

        private void acceptExecutions(List<ProtocolTemplateExecutionEvidence> executions) {
            if (executions == null || executions.isEmpty()) {
                return;
            }
            for (ProtocolTemplateExecutionEvidence execution : executions) {
                if (execution == null) {
                    continue;
                }
                this.attempted = true;
                addIfPresent(templateCodes, execution.getTemplateCode());
                addIfPresent(logicalChannelCodes, execution.getLogicalChannelCode());
                addIfPresent(childDeviceCodes, execution.getChildDeviceCode());
                addIfPresent(canonicalizationStrategies, execution.getCanonicalizationStrategy());
                if (execution.getParentRemovalKeys() != null) {
                    parentRemovalKeys.addAll(execution.getParentRemovalKeys().stream()
                            .map(this::normalize)
                            .filter(value -> value != null)
                            .toList());
                }
                if (Boolean.TRUE.equals(execution.getStatusMirrorApplied())) {
                    statusMirrorApplied = true;
                }
                templateExecutionCount++;
            }
        }

        private void incrementDecodeFailure() {
            this.attempted = true;
            this.decodeFailureCount++;
        }

        private ProductModelProtocolTemplateEvidenceVO toValueObject() {
            ProductModelProtocolTemplateEvidenceVO valueObject = new ProductModelProtocolTemplateEvidenceVO();
            valueObject.setTemplateCodes(copyOrNull(templateCodes));
            valueObject.setLogicalChannelCodes(copyOrNull(logicalChannelCodes));
            valueObject.setChildDeviceCodes(copyOrNull(childDeviceCodes));
            valueObject.setCanonicalizationStrategies(copyOrNull(canonicalizationStrategies));
            valueObject.setStatusMirrorApplied(statusMirrorApplied);
            valueObject.setParentRemovalKeys(copyOrNull(parentRemovalKeys));
            valueObject.setTemplateExecutionCount(templateExecutionCount);
            valueObject.setDecodeFailureCount(decodeFailureCount);
            return valueObject;
        }

        private boolean wasAttempted() {
            return attempted;
        }

        private void addIfPresent(Set<String> target, String value) {
            String normalized = normalize(value);
            if (normalized != null) {
                target.add(normalized);
            }
        }

        private String normalize(String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.trim();
            return normalized.isEmpty() ? null : normalized;
        }

        private List<String> copyOrNull(Set<String> values) {
            return values.isEmpty() ? null : List.copyOf(values);
        }
    }
}
