package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.service.RiskMetricCatalogPublishRule;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.ProductModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 风险指标目录发布规则默认实现。
 */
@Component
public class DefaultRiskMetricCatalogPublishRule implements RiskMetricCatalogPublishRule {

    /**
     * 以场景键聚合可发布指标，后续扩展只需追加场景与字段集合。
     */
    private static final Map<String, Set<String>> SCENARIO_PUBLISHABLE_IDENTIFIERS = Map.of(
            "phase1-crack", Set.of("value"),
            "phase2-gnss", Set.of("gpsTotalX", "gpsTotalY", "gpsTotalZ")
    );

    @Override
    public Set<String> resolveRiskEnabledIdentifiers(Device device, List<ProductModel> releasedContracts) {
        if (releasedContracts == null || releasedContracts.isEmpty()) {
            return Set.of();
        }
        Set<String> releasedIdentifiers = collectReleasedIdentifiers(releasedContracts);
        if (releasedIdentifiers.isEmpty()) {
            return Set.of();
        }
        Set<String> enabledIdentifiers = new LinkedHashSet<>();
        for (Set<String> publishableIdentifiers : SCENARIO_PUBLISHABLE_IDENTIFIERS.values()) {
            for (String identifier : publishableIdentifiers) {
                if (releasedIdentifiers.contains(identifier)) {
                    enabledIdentifiers.add(identifier);
                }
            }
        }
        return enabledIdentifiers;
    }

    private Set<String> collectReleasedIdentifiers(List<ProductModel> releasedContracts) {
        Map<String, String> normalizedByIdentifier = new LinkedHashMap<>();
        for (ProductModel contract : releasedContracts) {
            String identifier = normalize(contract == null ? null : contract.getIdentifier());
            if (identifier != null) {
                normalizedByIdentifier.put(identifier, identifier);
            }
        }
        return new LinkedHashSet<>(normalizedByIdentifier.values());
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
