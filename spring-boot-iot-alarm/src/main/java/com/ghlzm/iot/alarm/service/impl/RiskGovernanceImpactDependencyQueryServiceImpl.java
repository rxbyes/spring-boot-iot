package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.entity.LinkageRule;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RiskMetricEmergencyPlanBinding;
import com.ghlzm.iot.alarm.entity.RiskMetricLinkageBinding;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.EmergencyPlanMapper;
import com.ghlzm.iot.alarm.mapper.LinkageRuleMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricEmergencyPlanBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricLinkageBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.system.service.GovernanceImpactDependencyQueryService;
import com.ghlzm.iot.system.service.model.GovernanceImpactDependencySummary;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private final RiskPointMapper riskPointMapper;
    private final LinkageRuleMapper linkageRuleMapper;
    private final EmergencyPlanMapper emergencyPlanMapper;

    public RiskGovernanceImpactDependencyQueryServiceImpl(RiskMetricCatalogMapper riskMetricCatalogMapper,
                                                          RiskPointDeviceMapper riskPointDeviceMapper,
                                                          RuleDefinitionMapper ruleDefinitionMapper,
                                                          RiskMetricLinkageBindingMapper linkageBindingMapper,
                                                          RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper,
                                                          RiskPointMapper riskPointMapper,
                                                          LinkageRuleMapper linkageRuleMapper,
                                                          EmergencyPlanMapper emergencyPlanMapper) {
        this.riskMetricCatalogMapper = riskMetricCatalogMapper;
        this.riskPointDeviceMapper = riskPointDeviceMapper;
        this.ruleDefinitionMapper = ruleDefinitionMapper;
        this.linkageBindingMapper = linkageBindingMapper;
        this.emergencyPlanBindingMapper = emergencyPlanBindingMapper;
        this.riskPointMapper = riskPointMapper;
        this.linkageRuleMapper = linkageRuleMapper;
        this.emergencyPlanMapper = emergencyPlanMapper;
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

        List<RiskPointDevice> impactedRiskPointBindings = listRiskPointBindings(riskMetricIds, metricIdentifiers);
        List<RuleDefinition> impactedRules = listRuleDefinitions(riskMetricIds, metricIdentifiers);
        List<RiskMetricLinkageBinding> impactedLinkageBindings = listLinkageBindings(riskMetricIds);
        List<RiskMetricEmergencyPlanBinding> impactedEmergencyPlanBindings = listEmergencyPlanBindings(riskMetricIds);

        return new GovernanceImpactDependencySummary(
                (long) impactedCatalogs.size(),
                impactedRiskPointBindings.size(),
                impactedRules.size(),
                impactedLinkageBindings.size(),
                impactedEmergencyPlanBindings.size(),
                toRiskMetricDetails(impactedCatalogs),
                toRiskPointBindingDetails(impactedRiskPointBindings),
                toRuleDetails(impactedRules),
                toLinkageBindingDetails(impactedLinkageBindings),
                toEmergencyPlanBindingDetails(impactedEmergencyPlanBindings)
        );
    }

    private List<RiskPointDevice> listRiskPointBindings(Set<Long> riskMetricIds, Set<String> metricIdentifiers) {
        if (riskMetricIds.isEmpty() && metricIdentifiers.isEmpty()) {
            return List.of();
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
        return deduplicateByKey(riskPointDeviceMapper.selectList(wrapper), item ->
                item.getId() != null ? "id:" + item.getId() : "binding:" + normalize(item.getRiskPointId()) + ":"
                        + normalize(item.getDeviceId()) + ":" + normalize(item.getMetricIdentifier()));
    }

    private List<RuleDefinition> listRuleDefinitions(Set<Long> riskMetricIds, Set<String> metricIdentifiers) {
        if (riskMetricIds.isEmpty() && metricIdentifiers.isEmpty()) {
            return List.of();
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
        return deduplicateByKey(ruleDefinitionMapper.selectList(wrapper), item ->
                item.getId() != null ? "id:" + item.getId() : "rule:" + normalize(item.getRuleName()) + ":"
                        + normalize(item.getMetricIdentifier()));
    }

    private List<RiskMetricLinkageBinding> listLinkageBindings(Set<Long> riskMetricIds) {
        if (riskMetricIds.isEmpty()) {
            return List.of();
        }
        return deduplicateByKey(linkageBindingMapper.selectList(new LambdaQueryWrapper<RiskMetricLinkageBinding>()
                .eq(RiskMetricLinkageBinding::getDeleted, 0)
                .eq(RiskMetricLinkageBinding::getBindingStatus, ACTIVE_BINDING_STATUS)
                .in(RiskMetricLinkageBinding::getRiskMetricId, riskMetricIds)), item ->
                item.getId() != null ? "id:" + item.getId() : "binding:" + normalize(item.getLinkageRuleId()) + ":"
                        + normalize(item.getRiskMetricId()));
    }

    private List<RiskMetricEmergencyPlanBinding> listEmergencyPlanBindings(Set<Long> riskMetricIds) {
        if (riskMetricIds.isEmpty()) {
            return List.of();
        }
        return deduplicateByKey(emergencyPlanBindingMapper.selectList(new LambdaQueryWrapper<RiskMetricEmergencyPlanBinding>()
                .eq(RiskMetricEmergencyPlanBinding::getDeleted, 0)
                .eq(RiskMetricEmergencyPlanBinding::getBindingStatus, ACTIVE_BINDING_STATUS)
                .in(RiskMetricEmergencyPlanBinding::getRiskMetricId, riskMetricIds)), item ->
                item.getId() != null ? "id:" + item.getId() : "binding:" + normalize(item.getEmergencyPlanId()) + ":"
                        + normalize(item.getRiskMetricId()));
    }

    private List<GovernanceImpactDependencySummary.RiskMetricDetail> toRiskMetricDetails(List<RiskMetricCatalog> catalogs) {
        if (catalogs == null || catalogs.isEmpty()) {
            return List.of();
        }
        List<GovernanceImpactDependencySummary.RiskMetricDetail> details = new java.util.ArrayList<>();
        for (RiskMetricCatalog catalog : catalogs) {
            if (catalog == null) {
                continue;
            }
            details.add(new GovernanceImpactDependencySummary.RiskMetricDetail(
                    catalog.getId(),
                    normalize(catalog.getContractIdentifier()),
                    normalize(catalog.getNormativeIdentifier()),
                    normalize(catalog.getRiskMetricCode()),
                    normalize(catalog.getRiskMetricName()),
                    normalize(catalog.getMetricRole()),
                    normalize(catalog.getLifecycleStatus())
            ));
        }
        return List.copyOf(details);
    }

    private List<GovernanceImpactDependencySummary.RiskPointBindingDetail> toRiskPointBindingDetails(List<RiskPointDevice> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }
        Map<Long, RiskPoint> riskPointMap = loadRiskPoints(bindings.stream()
                .map(RiskPointDevice::getRiskPointId)
                .filter(id -> id != null && id > 0L)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
        List<GovernanceImpactDependencySummary.RiskPointBindingDetail> details = new java.util.ArrayList<>();
        for (RiskPointDevice binding : bindings) {
            if (binding == null) {
                continue;
            }
            RiskPoint riskPoint = riskPointMap.get(binding.getRiskPointId());
            details.add(new GovernanceImpactDependencySummary.RiskPointBindingDetail(
                    binding.getId(),
                    binding.getRiskPointId(),
                    riskPoint == null ? null : normalize(riskPoint.getRiskPointName()),
                    binding.getDeviceId(),
                    normalize(binding.getDeviceCode()),
                    normalize(binding.getDeviceName()),
                    binding.getRiskMetricId(),
                    normalize(binding.getMetricIdentifier()),
                    normalize(binding.getMetricName())
            ));
        }
        return List.copyOf(details);
    }

    private List<GovernanceImpactDependencySummary.RuleDetail> toRuleDetails(List<RuleDefinition> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        List<GovernanceImpactDependencySummary.RuleDetail> details = new java.util.ArrayList<>();
        for (RuleDefinition rule : rules) {
            if (rule == null) {
                continue;
            }
            details.add(new GovernanceImpactDependencySummary.RuleDetail(
                    rule.getId(),
                    normalize(rule.getRuleName()),
                    rule.getRiskMetricId(),
                    normalize(rule.getMetricIdentifier()),
                    normalize(rule.getMetricName()),
                    normalize(rule.getAlarmLevel())
            ));
        }
        return List.copyOf(details);
    }

    private List<GovernanceImpactDependencySummary.LinkageBindingDetail> toLinkageBindingDetails(List<RiskMetricLinkageBinding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }
        Map<Long, LinkageRule> linkageRuleMap = loadLinkageRules(bindings.stream()
                .map(RiskMetricLinkageBinding::getLinkageRuleId)
                .filter(id -> id != null && id > 0L)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
        List<GovernanceImpactDependencySummary.LinkageBindingDetail> details = new java.util.ArrayList<>();
        for (RiskMetricLinkageBinding binding : bindings) {
            if (binding == null) {
                continue;
            }
            LinkageRule linkageRule = linkageRuleMap.get(binding.getLinkageRuleId());
            details.add(new GovernanceImpactDependencySummary.LinkageBindingDetail(
                    binding.getId(),
                    binding.getLinkageRuleId(),
                    linkageRule == null ? null : normalize(linkageRule.getRuleName()),
                    binding.getRiskMetricId(),
                    normalize(binding.getBindingStatus())
            ));
        }
        return List.copyOf(details);
    }

    private List<GovernanceImpactDependencySummary.EmergencyPlanBindingDetail> toEmergencyPlanBindingDetails(List<RiskMetricEmergencyPlanBinding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }
        Map<Long, EmergencyPlan> emergencyPlanMap = loadEmergencyPlans(bindings.stream()
                .map(RiskMetricEmergencyPlanBinding::getEmergencyPlanId)
                .filter(id -> id != null && id > 0L)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
        List<GovernanceImpactDependencySummary.EmergencyPlanBindingDetail> details = new java.util.ArrayList<>();
        for (RiskMetricEmergencyPlanBinding binding : bindings) {
            if (binding == null) {
                continue;
            }
            EmergencyPlan emergencyPlan = emergencyPlanMap.get(binding.getEmergencyPlanId());
            details.add(new GovernanceImpactDependencySummary.EmergencyPlanBindingDetail(
                    binding.getId(),
                    binding.getEmergencyPlanId(),
                    emergencyPlan == null ? null : normalize(emergencyPlan.getPlanName()),
                    binding.getRiskMetricId(),
                    normalize(binding.getBindingStatus()),
                    emergencyPlan == null ? null : normalize(emergencyPlan.getAlarmLevel())
            ));
        }
        return List.copyOf(details);
    }

    private Map<Long, RiskPoint> loadRiskPoints(Set<Long> riskPointIds) {
        return toEntityMap(filterExisting(riskPointIds) ? riskPointMapper.selectBatchIds(riskPointIds) : List.of(),
                RiskPoint::getId,
                item -> item != null && item.getDeleted() != null && item.getDeleted() == 0);
    }

    private Map<Long, LinkageRule> loadLinkageRules(Set<Long> linkageRuleIds) {
        return toEntityMap(filterExisting(linkageRuleIds) ? linkageRuleMapper.selectBatchIds(linkageRuleIds) : List.of(),
                LinkageRule::getId,
                item -> item != null && item.getDeleted() != null && item.getDeleted() == 0);
    }

    private Map<Long, EmergencyPlan> loadEmergencyPlans(Set<Long> emergencyPlanIds) {
        return toEntityMap(filterExisting(emergencyPlanIds) ? emergencyPlanMapper.selectBatchIds(emergencyPlanIds) : List.of(),
                EmergencyPlan::getId,
                item -> item != null && item.getDeleted() != null && item.getDeleted() == 0);
    }

    private <T> Map<Long, T> toEntityMap(List<T> entities,
                                         java.util.function.Function<T, Long> idExtractor,
                                         java.util.function.Predicate<T> filter) {
        if (entities == null || entities.isEmpty()) {
            return Map.of();
        }
        Map<Long, T> entityMap = new LinkedHashMap<>();
        for (T entity : entities) {
            if (entity == null || (filter != null && !filter.test(entity))) {
                continue;
            }
            Long id = idExtractor.apply(entity);
            if (id != null && id > 0L) {
                entityMap.put(id, entity);
            }
        }
        return entityMap;
    }

    private boolean filterExisting(Collection<? extends Serializable> ids) {
        return ids != null && !ids.isEmpty();
    }

    private <T> List<T> deduplicateByKey(List<T> items, java.util.function.Function<T, String> keyExtractor) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Map<String, T> unique = new LinkedHashMap<>();
        for (T item : items) {
            if (item == null) {
                continue;
            }
            String key = normalize(keyExtractor.apply(item));
            if (!StringUtils.hasText(key)) {
                continue;
            }
            unique.putIfAbsent(key, item);
        }
        return List.copyOf(unique.values());
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

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
