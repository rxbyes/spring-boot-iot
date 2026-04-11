package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.service.model.GovernanceEvidenceItem;
import com.ghlzm.iot.system.service.model.GovernanceImpactSnapshot;
import com.ghlzm.iot.system.service.model.GovernanceRecommendationSnapshot;
import com.ghlzm.iot.system.service.model.GovernanceRollbackSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

final class GovernanceSnapshotResolver {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private GovernanceSnapshotResolver() {
    }

    static GovernanceRecommendationSnapshot resolveRecommendation(String recommendationSnapshotJson,
                                                                 String evidenceSnapshotJson,
                                                                 String fallbackSnapshotJson,
                                                                 String defaultRecommendationType,
                                                                 String defaultSuggestedAction) {
        JsonNode fallbackRoot = readJson(fallbackSnapshotJson);
        JsonNode recommendationNode = firstNode(
                readJson(recommendationSnapshotJson),
                childNode(fallbackRoot, "recommendation"),
                containsAny(fallbackRoot, "recommendationType", "confidence", "suggestedAction", "evidenceItems") ? fallbackRoot : null
        );
        JsonNode evidenceNode = firstNode(
                readJson(evidenceSnapshotJson),
                childNode(fallbackRoot, "evidence"),
                childNode(fallbackRoot, "evidenceItems")
        );

        GovernanceRecommendationSnapshot snapshot = new GovernanceRecommendationSnapshot();
        snapshot.setRecommendationType(normalizeCode(firstText(recommendationNode, "recommendationType"), defaultRecommendationType));
        snapshot.setConfidence(firstDouble(recommendationNode, "confidence"));
        snapshot.setReasonCodes(readStringList(recommendationNode, "reasonCodes"));
        snapshot.setSuggestedAction(firstText(recommendationNode, "suggestedAction", defaultSuggestedAction));
        snapshot.setEvidenceItems(resolveEvidenceItems(recommendationNode, evidenceNode));
        return hasRecommendationContent(snapshot) ? snapshot : null;
    }

    static GovernanceImpactSnapshot resolveImpact(String impactSnapshotJson,
                                                  String rollbackSnapshotJson,
                                                  String fallbackSnapshotJson,
                                                  Long fallbackAffectedCount,
                                                  String fallbackAffectedType) {
        JsonNode fallbackRoot = readJson(fallbackSnapshotJson);
        JsonNode impactNode = firstNode(
                readJson(impactSnapshotJson),
                childNode(fallbackRoot, "impact"),
                containsAny(fallbackRoot,
                        "affectedCount",
                        "affectedTypes",
                        "rollbackable",
                        "rollbackPlanSummary",
                        "affectedRiskPointCount",
                        "affectedDeviceCount",
                        "affectedBindingCount") ? fallbackRoot : null
        );
        JsonNode rollbackNode = firstNode(
                readJson(rollbackSnapshotJson),
                childNode(fallbackRoot, "rollback")
        );

        GovernanceImpactSnapshot snapshot = new GovernanceImpactSnapshot();
        snapshot.setAffectedCount(resolveAffectedCount(impactNode, fallbackAffectedCount));
        snapshot.setAffectedTypes(resolveAffectedTypes(impactNode, fallbackAffectedType));
        snapshot.setRollbackable(resolveRollbackable(impactNode, rollbackNode));
        snapshot.setRollbackPlanSummary(firstText(impactNode, "rollbackPlanSummary", firstText(rollbackNode, "rollbackPlanSummary")));
        return hasImpactContent(snapshot) ? snapshot : null;
    }

    static GovernanceRollbackSnapshot resolveRollback(String rollbackSnapshotJson,
                                                      String impactSnapshotJson,
                                                      String fallbackSnapshotJson) {
        JsonNode fallbackRoot = readJson(fallbackSnapshotJson);
        JsonNode rollbackNode = firstNode(
                readJson(rollbackSnapshotJson),
                childNode(fallbackRoot, "rollback")
        );
        JsonNode impactNode = firstNode(
                readJson(impactSnapshotJson),
                childNode(fallbackRoot, "impact"),
                containsAny(fallbackRoot, "rollbackable", "rollbackPlanSummary") ? fallbackRoot : null
        );

        GovernanceRollbackSnapshot snapshot = new GovernanceRollbackSnapshot();
        snapshot.setRollbackable(resolveRollbackable(impactNode, rollbackNode));
        snapshot.setRollbackPlanSummary(firstText(rollbackNode, "rollbackPlanSummary", firstText(impactNode, "rollbackPlanSummary")));
        return hasRollbackContent(snapshot) ? snapshot : null;
    }

    private static List<GovernanceEvidenceItem> resolveEvidenceItems(JsonNode recommendationNode, JsonNode evidenceNode) {
        List<GovernanceEvidenceItem> items = new ArrayList<>();
        JsonNode recommendationEvidenceNode = childNode(recommendationNode, "evidenceItems");
        JsonNode nestedEvidenceNode = childNode(evidenceNode, "evidenceItems");
        JsonNode directEvidenceNode = evidenceNode != null && evidenceNode.isArray() ? evidenceNode : null;
        appendEvidenceItems(items, recommendationEvidenceNode);
        if (nestedEvidenceNode != null && nestedEvidenceNode != recommendationEvidenceNode) {
            appendEvidenceItems(items, nestedEvidenceNode);
        }
        if (directEvidenceNode != null && directEvidenceNode != recommendationEvidenceNode) {
            appendEvidenceItems(items, directEvidenceNode);
        }
        return items;
    }

    private static void appendEvidenceItems(List<GovernanceEvidenceItem> target, JsonNode evidenceArrayNode) {
        if (target == null || evidenceArrayNode == null || !evidenceArrayNode.isArray()) {
            return;
        }
        for (JsonNode itemNode : evidenceArrayNode) {
            GovernanceEvidenceItem item = new GovernanceEvidenceItem();
            item.setEvidenceType(normalizeCode(firstText(itemNode, "evidenceType", firstText(itemNode, "sourceType", firstText(itemNode, "source"))), "UNKNOWN"));
            item.setTitle(firstText(itemNode, "title", firstText(itemNode, "label", firstText(itemNode, "metricName"))));
            item.setSummary(firstText(itemNode, "summary", firstText(itemNode, "description", firstText(itemNode, "reason", firstText(itemNode, "source")))));
            item.setSourceType(normalizeCode(firstText(itemNode, "sourceType", firstText(itemNode, "source")), null));
            item.setSourceId(firstText(itemNode, "sourceId", firstText(itemNode, "traceId", firstText(itemNode, "deviceCode"))));
            target.add(item);
        }
    }

    private static Long resolveAffectedCount(JsonNode impactNode, Long fallbackAffectedCount) {
        Long explicit = firstLong(impactNode, "affectedCount");
        if (explicit != null) {
            return explicit;
        }
        long derived = 0L;
        derived += positiveOrZero(firstLong(impactNode, "affectedRiskPointCount"));
        derived += positiveOrZero(firstLong(impactNode, "affectedDeviceCount"));
        derived += positiveOrZero(firstLong(impactNode, "affectedBindingCount"));
        derived += positiveOrZero(firstLong(impactNode, "affectedProductCount"));
        if (derived > 0L) {
            return derived;
        }
        return fallbackAffectedCount != null && fallbackAffectedCount >= 0 ? fallbackAffectedCount : null;
    }

    private static List<String> resolveAffectedTypes(JsonNode impactNode, String fallbackAffectedType) {
        List<String> explicitTypes = readStringList(impactNode, "affectedTypes");
        if (!explicitTypes.isEmpty()) {
            return explicitTypes;
        }
        Set<String> derivedTypes = new LinkedHashSet<>();
        if (positiveOrZero(firstLong(impactNode, "affectedRiskPointCount")) > 0L) {
            derivedTypes.add("RISK_POINT");
        }
        if (positiveOrZero(firstLong(impactNode, "affectedDeviceCount")) > 0L) {
            derivedTypes.add("DEVICE");
        }
        if (positiveOrZero(firstLong(impactNode, "affectedBindingCount")) > 0L) {
            derivedTypes.add("RISK_BINDING");
        }
        if (positiveOrZero(firstLong(impactNode, "affectedProductCount")) > 0L) {
            derivedTypes.add("PRODUCT");
        }
        String normalizedFallbackType = normalizeCode(fallbackAffectedType, null);
        if (derivedTypes.isEmpty() && StringUtils.hasText(normalizedFallbackType)) {
            derivedTypes.add(normalizedFallbackType);
        }
        return derivedTypes.isEmpty() ? List.of() : List.copyOf(derivedTypes);
    }

    private static Boolean resolveRollbackable(JsonNode impactNode, JsonNode rollbackNode) {
        Boolean value = firstBoolean(impactNode, "rollbackable");
        if (value != null) {
            return value;
        }
        return firstBoolean(rollbackNode, "rollbackable");
    }

    private static boolean hasRecommendationContent(GovernanceRecommendationSnapshot snapshot) {
        return snapshot != null
                && (snapshot.getConfidence() != null
                || StringUtils.hasText(snapshot.getRecommendationType())
                || StringUtils.hasText(snapshot.getSuggestedAction())
                || !safeList(snapshot.getReasonCodes()).isEmpty()
                || !safeList(snapshot.getEvidenceItems()).isEmpty());
    }

    private static boolean hasImpactContent(GovernanceImpactSnapshot snapshot) {
        return snapshot != null
                && (snapshot.getAffectedCount() != null
                || !safeList(snapshot.getAffectedTypes()).isEmpty()
                || snapshot.getRollbackable() != null
                || StringUtils.hasText(snapshot.getRollbackPlanSummary()));
    }

    private static boolean hasRollbackContent(GovernanceRollbackSnapshot snapshot) {
        return snapshot != null
                && (snapshot.getRollbackable() != null
                || StringUtils.hasText(snapshot.getRollbackPlanSummary()));
    }

    private static JsonNode readJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonNode firstNode(JsonNode... nodes) {
        if (nodes == null) {
            return null;
        }
        for (JsonNode node : nodes) {
            if (node != null && !node.isNull() && !node.isMissingNode()) {
                return node;
            }
        }
        return null;
    }

    private static JsonNode childNode(JsonNode node, String fieldName) {
        if (node == null || !StringUtils.hasText(fieldName)) {
            return null;
        }
        JsonNode child = node.path(fieldName);
        return child == null || child.isMissingNode() || child.isNull() ? null : child;
    }

    private static boolean containsAny(JsonNode node, String... fieldNames) {
        if (node == null || fieldNames == null) {
            return false;
        }
        for (String fieldName : fieldNames) {
            if (node.has(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private static String firstText(JsonNode node, String fieldName) {
        return firstText(node, fieldName, null);
    }

    private static String firstText(JsonNode node, String fieldName, String fallback) {
        if (node != null && StringUtils.hasText(fieldName)) {
            JsonNode valueNode = node.path(fieldName);
            if (valueNode != null && !valueNode.isMissingNode() && !valueNode.isNull()) {
                String value = valueNode.isTextual() ? valueNode.asText() : valueNode.toString();
                if (StringUtils.hasText(value)) {
                    return value.trim();
                }
            }
        }
        return StringUtils.hasText(fallback) ? fallback.trim() : null;
    }

    private static Double firstDouble(JsonNode node, String fieldName) {
        if (node == null || !StringUtils.hasText(fieldName)) {
            return null;
        }
        JsonNode valueNode = node.path(fieldName);
        if (valueNode == null || valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isNumber()) {
            return valueNode.asDouble();
        }
        if (valueNode.isTextual()) {
            try {
                return Double.valueOf(valueNode.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Long firstLong(JsonNode node, String fieldName) {
        if (node == null || !StringUtils.hasText(fieldName)) {
            return null;
        }
        JsonNode valueNode = node.path(fieldName);
        if (valueNode == null || valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isNumber()) {
            return valueNode.asLong();
        }
        if (valueNode.isTextual()) {
            try {
                return Long.valueOf(valueNode.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Boolean firstBoolean(JsonNode node, String fieldName) {
        if (node == null || !StringUtils.hasText(fieldName)) {
            return null;
        }
        JsonNode valueNode = node.path(fieldName);
        if (valueNode == null || valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isBoolean()) {
            return valueNode.asBoolean();
        }
        if (valueNode.isTextual()) {
            String value = valueNode.asText().trim();
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.valueOf(value);
            }
        }
        return null;
    }

    private static List<String> readStringList(JsonNode node, String fieldName) {
        JsonNode valueNode = childNode(node, fieldName);
        if (valueNode == null || !valueNode.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode child : valueNode) {
            if (child == null || child.isNull()) {
                continue;
            }
            String value = child.isTextual() ? child.asText() : child.toString();
            if (StringUtils.hasText(value)) {
                values.add(value.trim());
            }
        }
        return values;
    }

    private static String normalizeCode(String value, String fallback) {
        String raw = StringUtils.hasText(value) ? value.trim() : null;
        if (!StringUtils.hasText(raw)) {
            raw = StringUtils.hasText(fallback) ? fallback.trim() : null;
        }
        return StringUtils.hasText(raw) ? raw.toUpperCase(Locale.ROOT) : null;
    }

    private static long positiveOrZero(Long value) {
        return value == null || value < 0 ? 0L : value;
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
