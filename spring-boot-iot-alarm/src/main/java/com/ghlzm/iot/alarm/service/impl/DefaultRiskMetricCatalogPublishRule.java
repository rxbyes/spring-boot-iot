package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.service.RiskMetricCatalogPublishRule;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 风险指标目录发布规则默认实现。
 *
 * <p>目录准入真相源只来自契约字段页显式保存的
 * {@code metadataJson.objectInsight.customMetrics[]} 中
 * {@code group=measure && enabled=true && includeInTrend=true} 的正式字段。</p>
 */
@Component
public class DefaultRiskMetricCatalogPublishRule implements RiskMetricCatalogPublishRule {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Override
    public Set<String> resolveRiskEnabledIdentifiers(Device device, List<ProductModel> releasedContracts) {
        return Set.of();
    }

    @Override
    public Set<String> resolveRiskEnabledIdentifiers(Product product,
                                                     String scenarioCode,
                                                     Device device,
                                                     List<ProductModel> releasedContracts) {
        if (releasedContracts == null || releasedContracts.isEmpty() || product == null) {
            return Set.of();
        }
        Set<String> measureTruthIdentifiers = resolveMeasureTruthIdentifiers(product.getMetadataJson());
        if (measureTruthIdentifiers.isEmpty()) {
            return Set.of();
        }
        Set<String> enabledIdentifiers = new LinkedHashSet<>();
        for (ProductModel contract : releasedContracts) {
            if (contract == null) {
                continue;
            }
            String modelType = normalize(contract.getModelType());
            if (StringUtils.hasText(modelType) && !"property".equalsIgnoreCase(modelType)) {
                continue;
            }
            String identifier = normalize(contract.getIdentifier());
            if (StringUtils.hasText(identifier) && measureTruthIdentifiers.contains(identifier)) {
                enabledIdentifiers.add(identifier);
            }
        }
        return enabledIdentifiers;
    }

    private Set<String> resolveMeasureTruthIdentifiers(String metadataJson) {
        JsonNode customMetrics = readCustomMetrics(metadataJson);
        if (customMetrics == null || !customMetrics.isArray()) {
            return Set.of();
        }
        Set<String> identifiers = new LinkedHashSet<>();
        for (JsonNode item : customMetrics) {
            if (item == null || !item.isObject()) {
                continue;
            }
            String identifier = normalize(item.path("identifier").asText(null));
            String group = normalize(item.path("group").asText(null));
            boolean enabled = !item.has("enabled") || item.path("enabled").asBoolean(true);
            boolean includeInTrend = !item.has("includeInTrend") || item.path("includeInTrend").asBoolean(true);
            if (StringUtils.hasText(identifier)
                    && "measure".equalsIgnoreCase(group)
                    && enabled
                    && includeInTrend) {
                identifiers.add(identifier);
            }
        }
        return identifiers;
    }

    private JsonNode readCustomMetrics(String metadataJson) {
        String normalized = normalize(metadataJson);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(normalized);
            if (root == null || !root.isObject()) {
                return null;
            }
            return root.path("objectInsight").path("customMetrics");
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
