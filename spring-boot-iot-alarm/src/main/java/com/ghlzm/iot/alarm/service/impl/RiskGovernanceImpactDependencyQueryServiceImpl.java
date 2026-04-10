package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RiskMetricEmergencyPlanBinding;
import com.ghlzm.iot.alarm.entity.RiskMetricLinkageBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricEmergencyPlanBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricLinkageBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.system.service.GovernanceImpactDependencyQueryService;
import com.ghlzm.iot.system.service.model.GovernanceImpactDependencySummary;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Reads downstream risk-governance dependencies impacted by product contract changes.
 */
@Service
public class RiskGovernanceImpactDependencyQueryServiceImpl implements GovernanceImpactDependencyQueryService {

    private static final String ACTIVE_BINDING_STATUS = "ACTIVE";

    private final RiskMetricCatalogMapper riskMetricCatalogMapper;
    private final RiskPointDeviceMapper riskPointDeviceMapper;
    private final RuleDefinitionMapper ruleDefinitionMapper;
    private final RiskMetricLinkageBindingMapper linkageBindingMapper;
    private final RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper;

    public RiskGovernanceImpactDependencyQueryServiceImpl(RiskMetricCatalogMapper riskMetricCatalogMapper,
                                                          RiskPointDeviceMapper riskPointDeviceMapper,
                                                          RuleDefinitionMapper ruleDefinitionMapper,
                                                          RiskMetricLinkageBindingMapper linkageBindingMapper,
                                                          RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper) {
        this.riskMetricCatalogMapper = riskMetricCatalogMapper;
        this.riskPointDeviceMapper = riskPointDeviceMapper;
        this.ruleDefinitionMapper = ruleDefinitionMapper;
        this.linkageBindingMapper = linkageBindingMapper;
        this.emergencyPlanBindingMapper = emergencyPlanBindingMapper;
    }

    @Override
    public GovernanceImpactDependencySummary summarizeProductContractImpact(Long productId, Set<String> contractIdentifiers) {
        Set<String> normalizedIdentifiers = normalizeIdentifiers(contractIdentifiers);
        if (productId == null || normalizedIdentifiers.isEmpty()) {
            return GovernanceImpactDependencySummary.empty();
        }

        List<RiskMetricCatalog> impactedCatalogs = riskMetricCatalogMapper.selectList(new LambdaQueryWrapper<RiskMetricCatalog>()
                .eq(RiskMetricCatalog::getDeleted, 0)
                .eq(RiskMetricCatalog::getProductId, productId)
                .eq(RiskMetricCatalog::getEnabled, 1)
                .in(RiskMetricCatalog::getContractIdentifier, normalizedIdentifiers));

        Set<Long> riskMetricIds = impactedCatalogs.stream()
                .map(RiskMetricCatalog::getId)
                .filter(id -> id != null && id > 0L)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> metricIdentifiers = new LinkedHashSet<>(normalizedIdentifiers);
        impactedCatalogs.forEach(catalog -> addCatalogAliases(metricIdentifiers, catalog));

        return new GovernanceImpactDependencySummary(
                (long) impactedCatalogs.size(),
                countRiskPointBindings(riskMetricIds, metricIdentifiers),
                countRuleDefinitions(riskMetricIds, metricIdentifiers),
                countLinkageBindings(riskMetricIds),
                countEmergencyPlanBindings(riskMetricIds)
        );
    }

    private long countRiskPointBindings(Set<Long> riskMetricIds, Set<String> metricIdentifiers) {
        if (riskMetricIds.isEmpty() && metricIdentifiers.isEmpty()) {
            return 0L;
        }
        LambdaQueryWrapper<RiskPointDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskPointDevice::getDeleted, 0);
        if (!riskMetricIds.isEmpty() && !metricIdentifiers.isEmpty()) {
            wrapper.and(query -> query.in(RiskPointDevice::getRiskMetricId, riskMetricIds)
                    .or()
                    .in(RiskPointDevice::getMetricIdentifier, metricIdentifiers));
        } else if (!riskMetricIds.isEmpty()) {
            wrapper.in(RiskPointDevice::getRiskMetricId, riskMetricIds);
        } else {
            wrapper.in(RiskPointDevice::getMetricIdentifier, metricIdentifiers);
        }
        return safeCount(riskPointDeviceMapper.selectCount(wrapper));
    }

    private long countRuleDefinitions(Set<Long> riskMetricIds, Set<String> metricIdentifiers) {
        if (riskMetricIds.isEmpty() && metricIdentifiers.isEmpty()) {
            return 0L;
        }
        LambdaQueryWrapper<RuleDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RuleDefinition::getDeleted, 0)
                .eq(RuleDefinition::getStatus, 0);
        if (!riskMetricIds.isEmpty() && !metricIdentifiers.isEmpty()) {
            wrapper.and(query -> query.in(RuleDefinition::getRiskMetricId, riskMetricIds)
                    .or()
                    .in(RuleDefinition::getMetricIdentifier, metricIdentifiers));
        } else if (!riskMetricIds.isEmpty()) {
            wrapper.in(RuleDefinition::getRiskMetricId, riskMetricIds);
        } else {
            wrapper.in(RuleDefinition::getMetricIdentifier, metricIdentifiers);
        }
        return safeCount(ruleDefinitionMapper.selectCount(wrapper));
    }

    private long countLinkageBindings(Set<Long> riskMetricIds) {
        if (riskMetricIds.isEmpty()) {
            return 0L;
        }
        return safeCount(linkageBindingMapper.selectCount(new LambdaQueryWrapper<RiskMetricLinkageBinding>()
                .eq(RiskMetricLinkageBinding::getDeleted, 0)
                .eq(RiskMetricLinkageBinding::getBindingStatus, ACTIVE_BINDING_STATUS)
                .in(RiskMetricLinkageBinding::getRiskMetricId, riskMetricIds)));
    }

    private long countEmergencyPlanBindings(Set<Long> riskMetricIds) {
        if (riskMetricIds.isEmpty()) {
            return 0L;
        }
        return safeCount(emergencyPlanBindingMapper.selectCount(new LambdaQueryWrapper<RiskMetricEmergencyPlanBinding>()
                .eq(RiskMetricEmergencyPlanBinding::getDeleted, 0)
                .eq(RiskMetricEmergencyPlanBinding::getBindingStatus, ACTIVE_BINDING_STATUS)
                .in(RiskMetricEmergencyPlanBinding::getRiskMetricId, riskMetricIds)));
    }

    private Set<String> normalizeIdentifiers(Set<String> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String identifier : identifiers) {
            if (StringUtils.hasText(identifier)) {
                normalized.add(identifier.trim());
            }
        }
        return normalized;
    }

    private void addCatalogAliases(Set<String> metricIdentifiers, RiskMetricCatalog catalog) {
        if (metricIdentifiers == null || catalog == null) {
            return;
        }
        addIfPresent(metricIdentifiers, catalog.getContractIdentifier());
        addIfPresent(metricIdentifiers, catalog.getNormativeIdentifier());
        addIfPresent(metricIdentifiers, catalog.getRiskMetricCode());
    }

    private void addIfPresent(Set<String> values, String candidate) {
        if (values != null && StringUtils.hasText(candidate)) {
            values.add(candidate.trim());
        }
    }

    private long safeCount(Long count) {
        return count == null || count < 0L ? 0L : count;
    }
}
