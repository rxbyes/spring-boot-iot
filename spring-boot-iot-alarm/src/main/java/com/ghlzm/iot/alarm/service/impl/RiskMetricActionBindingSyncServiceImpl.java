package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.entity.LinkageRule;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RiskMetricEmergencyPlanBinding;
import com.ghlzm.iot.alarm.entity.RiskMetricLinkageBinding;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricEmergencyPlanBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricLinkageBindingMapper;
import com.ghlzm.iot.alarm.service.RiskMetricActionBindingSyncService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class RiskMetricActionBindingSyncServiceImpl implements RiskMetricActionBindingSyncService {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private final RiskMetricCatalogMapper riskMetricCatalogMapper;
    private final RiskMetricLinkageBindingMapper linkageBindingMapper;
    private final RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper;

    public RiskMetricActionBindingSyncServiceImpl(RiskMetricCatalogMapper riskMetricCatalogMapper,
                                                  RiskMetricLinkageBindingMapper linkageBindingMapper,
                                                  RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper) {
        this.riskMetricCatalogMapper = riskMetricCatalogMapper;
        this.linkageBindingMapper = linkageBindingMapper;
        this.emergencyPlanBindingMapper = emergencyPlanBindingMapper;
    }

    @Override
    public void rebuildLinkageBindingsForRule(LinkageRule rule, Long operatorUserId, String bindingOrigin) {
        if (rule == null || rule.getId() == null) {
            return;
        }
        Set<String> desiredIdentifiers = extractMetricIdentifiersFromTriggerCondition(rule.getTriggerCondition());
        syncLinkageBindings(rule, operatorUserId, normalizeOrigin(bindingOrigin), desiredIdentifiers);
    }

    @Override
    public void rebuildEmergencyPlanBindingsForPlan(EmergencyPlan plan, Long operatorUserId, String bindingOrigin) {
        if (plan == null || plan.getId() == null) {
            return;
        }
        String searchableText = buildEmergencyPlanSearchText(plan);
        Set<String> desiredIdentifiers = selectEnabledCatalogs(plan.getTenantId()).stream()
                .map(RiskMetricCatalog::getContractIdentifier)
                .filter(StringUtils::hasText)
                .map(this::normalizeLower)
                .filter(searchableText::contains)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        syncEmergencyPlanBindings(plan, operatorUserId, normalizeOrigin(bindingOrigin), desiredIdentifiers);
    }

    @Override
    public void deactivateLinkageBindings(Long linkageRuleId, Long operatorUserId) {
        if (linkageRuleId == null) {
            return;
        }
        List<RiskMetricLinkageBinding> existingBindings = linkageBindingMapper.selectList(new LambdaQueryWrapper<RiskMetricLinkageBinding>()
                .eq(RiskMetricLinkageBinding::getDeleted, 0)
                .eq(RiskMetricLinkageBinding::getLinkageRuleId, linkageRuleId));
        for (RiskMetricLinkageBinding existingBinding : existingBindings) {
            RiskMetricLinkageBinding retired = new RiskMetricLinkageBinding();
            retired.setId(existingBinding.getId());
            retired.setBindingStatus("INACTIVE");
            retired.setDeleted(1);
            retired.setUpdateBy(operatorUserId);
            linkageBindingMapper.updateById(retired);
        }
    }

    @Override
    public void deactivateEmergencyPlanBindings(Long emergencyPlanId, Long operatorUserId) {
        if (emergencyPlanId == null) {
            return;
        }
        List<RiskMetricEmergencyPlanBinding> existingBindings = emergencyPlanBindingMapper.selectList(new LambdaQueryWrapper<RiskMetricEmergencyPlanBinding>()
                .eq(RiskMetricEmergencyPlanBinding::getDeleted, 0)
                .eq(RiskMetricEmergencyPlanBinding::getEmergencyPlanId, emergencyPlanId));
        for (RiskMetricEmergencyPlanBinding existingBinding : existingBindings) {
            RiskMetricEmergencyPlanBinding retired = new RiskMetricEmergencyPlanBinding();
            retired.setId(existingBinding.getId());
            retired.setBindingStatus("INACTIVE");
            retired.setDeleted(1);
            retired.setUpdateBy(operatorUserId);
            emergencyPlanBindingMapper.updateById(retired);
        }
    }

    private void syncLinkageBindings(LinkageRule rule,
                                     Long operatorUserId,
                                     String bindingOrigin,
                                     Set<String> desiredIdentifiers) {
        Map<String, RiskMetricCatalog> catalogsByIdentifier = selectEnabledCatalogMap(rule.getTenantId());
        Set<Long> desiredRiskMetricIds = desiredIdentifiers.stream()
                .map(catalogsByIdentifier::get)
                .filter(Objects::nonNull)
                .map(RiskMetricCatalog::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<RiskMetricLinkageBinding> existingBindings = linkageBindingMapper.selectList(new LambdaQueryWrapper<RiskMetricLinkageBinding>()
                .eq(RiskMetricLinkageBinding::getDeleted, 0)
                .eq(RiskMetricLinkageBinding::getLinkageRuleId, rule.getId()));
        Map<Long, RiskMetricLinkageBinding> existingByMetricId = existingBindings.stream()
                .filter(binding -> binding.getRiskMetricId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        RiskMetricLinkageBinding::getRiskMetricId,
                        binding -> binding,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        for (Long riskMetricId : desiredRiskMetricIds) {
            if (existingByMetricId.containsKey(riskMetricId)) {
                continue;
            }
            RiskMetricLinkageBinding binding = new RiskMetricLinkageBinding();
            binding.setTenantId(defaultTenantId(rule.getTenantId()));
            binding.setRiskMetricId(riskMetricId);
            binding.setLinkageRuleId(rule.getId());
            binding.setBindingStatus("ACTIVE");
            binding.setBindingOrigin(bindingOrigin);
            binding.setCreateBy(operatorUserId);
            binding.setUpdateBy(operatorUserId);
            binding.setDeleted(0);
            linkageBindingMapper.insert(binding);
        }
        for (RiskMetricLinkageBinding existingBinding : existingBindings) {
            if (existingBinding.getRiskMetricId() != null && desiredRiskMetricIds.contains(existingBinding.getRiskMetricId())) {
                continue;
            }
            RiskMetricLinkageBinding retired = new RiskMetricLinkageBinding();
            retired.setId(existingBinding.getId());
            retired.setBindingStatus("INACTIVE");
            retired.setDeleted(1);
            retired.setUpdateBy(operatorUserId);
            linkageBindingMapper.updateById(retired);
        }
    }

    private void syncEmergencyPlanBindings(EmergencyPlan plan,
                                           Long operatorUserId,
                                           String bindingOrigin,
                                           Set<String> desiredIdentifiers) {
        Map<String, RiskMetricCatalog> catalogsByIdentifier = selectEnabledCatalogMap(plan.getTenantId());
        Set<Long> desiredRiskMetricIds = desiredIdentifiers.stream()
                .map(catalogsByIdentifier::get)
                .filter(Objects::nonNull)
                .map(RiskMetricCatalog::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<RiskMetricEmergencyPlanBinding> existingBindings = emergencyPlanBindingMapper.selectList(new LambdaQueryWrapper<RiskMetricEmergencyPlanBinding>()
                .eq(RiskMetricEmergencyPlanBinding::getDeleted, 0)
                .eq(RiskMetricEmergencyPlanBinding::getEmergencyPlanId, plan.getId()));
        Map<Long, RiskMetricEmergencyPlanBinding> existingByMetricId = existingBindings.stream()
                .filter(binding -> binding.getRiskMetricId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        RiskMetricEmergencyPlanBinding::getRiskMetricId,
                        binding -> binding,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        for (Long riskMetricId : desiredRiskMetricIds) {
            if (existingByMetricId.containsKey(riskMetricId)) {
                continue;
            }
            RiskMetricEmergencyPlanBinding binding = new RiskMetricEmergencyPlanBinding();
            binding.setTenantId(defaultTenantId(plan.getTenantId()));
            binding.setRiskMetricId(riskMetricId);
            binding.setEmergencyPlanId(plan.getId());
            binding.setBindingStatus("ACTIVE");
            binding.setBindingOrigin(bindingOrigin);
            binding.setCreateBy(operatorUserId);
            binding.setUpdateBy(operatorUserId);
            binding.setDeleted(0);
            emergencyPlanBindingMapper.insert(binding);
        }
        for (RiskMetricEmergencyPlanBinding existingBinding : existingBindings) {
            if (existingBinding.getRiskMetricId() != null && desiredRiskMetricIds.contains(existingBinding.getRiskMetricId())) {
                continue;
            }
            RiskMetricEmergencyPlanBinding retired = new RiskMetricEmergencyPlanBinding();
            retired.setId(existingBinding.getId());
            retired.setBindingStatus("INACTIVE");
            retired.setDeleted(1);
            retired.setUpdateBy(operatorUserId);
            emergencyPlanBindingMapper.updateById(retired);
        }
    }

    private Map<String, RiskMetricCatalog> selectEnabledCatalogMap(Long tenantId) {
        return selectEnabledCatalogs(tenantId).stream()
                .filter(catalog -> StringUtils.hasText(catalog.getContractIdentifier()))
                .collect(java.util.stream.Collectors.toMap(
                        catalog -> normalizeLower(catalog.getContractIdentifier()),
                        catalog -> catalog,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private List<RiskMetricCatalog> selectEnabledCatalogs(Long tenantId) {
        return riskMetricCatalogMapper.selectList(new LambdaQueryWrapper<RiskMetricCatalog>()
                .eq(RiskMetricCatalog::getDeleted, 0)
                .eq(RiskMetricCatalog::getEnabled, 1)
                .eq(tenantId != null, RiskMetricCatalog::getTenantId, tenantId));
    }

    private Set<String> extractMetricIdentifiersFromTriggerCondition(String triggerCondition) {
        if (!StringUtils.hasText(triggerCondition)) {
            return Set.of();
        }
        try {
            Object parsed = OBJECT_MAPPER.readValue(triggerCondition, Object.class);
            Set<String> identifiers = new LinkedHashSet<>();
            collectMetricIdentifiers(parsed, identifiers);
            return identifiers;
        } catch (Exception ignored) {
            return Set.of();
        }
    }

    private void collectMetricIdentifiers(Object value, Set<String> collector) {
        if (value instanceof List<?> list) {
            for (Object item : list) {
                collectMetricIdentifiers(item, collector);
            }
            return;
        }
        if (!(value instanceof Map<?, ?> rawMap)) {
            return;
        }
        collectMetricIdentifier(rawMap.get("metricIdentifier"), collector);
        collectMetricIdentifier(rawMap.get("metric"), collector);
        collectMetricIdentifier(rawMap.get("identifier"), collector);
        for (Object nested : rawMap.values()) {
            collectMetricIdentifiers(nested, collector);
        }
    }

    private void collectMetricIdentifier(Object value, Set<String> collector) {
        if (value instanceof String text && StringUtils.hasText(text)) {
            collector.add(normalizeLower(text));
        }
    }

    private String buildEmergencyPlanSearchText(EmergencyPlan plan) {
        StringBuilder builder = new StringBuilder();
        appendSearchable(builder, plan.getPlanName());
        appendSearchable(builder, plan.getDescription());
        appendSearchable(builder, plan.getResponseSteps());
        return builder.toString();
    }

    private void appendSearchable(StringBuilder builder, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(normalizeLower(value));
    }

    private Long defaultTenantId(Long tenantId) {
        return tenantId == null ? 1L : tenantId;
    }

    private String normalizeOrigin(String bindingOrigin) {
        if (!StringUtils.hasText(bindingOrigin)) {
            return "AUTO_INFERRED";
        }
        return bindingOrigin.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
