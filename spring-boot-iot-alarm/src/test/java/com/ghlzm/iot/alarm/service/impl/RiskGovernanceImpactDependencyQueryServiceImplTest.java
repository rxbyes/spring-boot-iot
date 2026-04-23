package com.ghlzm.iot.alarm.service.impl;

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
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.system.service.model.GovernanceImpactDependencySummary;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskGovernanceImpactDependencyQueryServiceImplTest {

    @Mock
    private RiskMetricCatalogMapper riskMetricCatalogMapper;

    @Mock
    private RiskPointDeviceMapper riskPointDeviceMapper;

    @Mock
    private RuleDefinitionMapper ruleDefinitionMapper;

    @Mock
    private RiskPointMapper riskPointMapper;

    @Mock
    private RiskMetricLinkageBindingMapper linkageBindingMapper;

    @Mock
    private RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper;

    @Mock
    private LinkageRuleMapper linkageRuleMapper;

    @Mock
    private EmergencyPlanMapper emergencyPlanMapper;

    @Test
    void summarizeProductContractImpactShouldAggregateDownstreamDependencyCounts() {
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(
                catalog(9101L, 1001L, "value", "metric.value"),
                catalog(9102L, 1001L, "humidity", "metric.humidity")
        ));
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(
                riskPointBinding(3001L, 7101L, "device-001", "value"),
                riskPointBinding(3002L, 7102L, "device-002", "humidity"),
                riskPointBinding(3003L, 7103L, "device-003", "humidity")
        ));
        when(riskPointMapper.selectBatchIds(any())).thenReturn(List.of(
                riskPoint(7101L, "风险点 A"),
                riskPoint(7102L, "风险点 B"),
                riskPoint(7103L, "风险点 C")
        ));
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(
                ruleDefinition(4101L, "规则 A", "value"),
                ruleDefinition(4102L, "规则 B", "humidity")
        ));
        when(linkageBindingMapper.selectList(any())).thenReturn(List.of(
                linkageBinding(5101L, 9101L, 6101L),
                linkageBinding(5102L, 9101L, 6102L),
                linkageBinding(5103L, 9102L, 6103L),
                linkageBinding(5104L, 9102L, 6104L)
        ));
        when(linkageRuleMapper.selectBatchIds(any())).thenReturn(List.of(
                linkageRule(6101L, "联动 A"),
                linkageRule(6102L, "联动 B"),
                linkageRule(6103L, "联动 C"),
                linkageRule(6104L, "联动 D")
        ));
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of(
                emergencyPlanBinding(5201L, 9101L, 6201L)
        ));
        when(emergencyPlanMapper.selectBatchIds(any())).thenReturn(List.of(
                emergencyPlan(6201L, "预案 A")
        ));

        RiskGovernanceImpactDependencyQueryServiceImpl service = newService();

        GovernanceImpactDependencySummary summary = service.summarizeProductContractImpact(1001L, Set.of("value", "humidity"));

        assertEquals(2L, summary.affectedRiskMetricCount());
        assertEquals(3L, summary.affectedRiskPointBindingCount());
        assertEquals(2L, summary.affectedRuleCount());
        assertEquals(4L, summary.affectedLinkageBindingCount());
        assertEquals(1L, summary.affectedEmergencyPlanBindingCount());
    }

    @Test
    void summarizeProductContractImpactShouldReturnEmptyWhenInputMissing() {
        RiskGovernanceImpactDependencyQueryServiceImpl service = newService();

        GovernanceImpactDependencySummary summary = service.summarizeProductContractImpact(1001L, Set.of());

        assertEquals(0L, summary.affectedRiskMetricCount());
        assertEquals(0L, summary.affectedRiskPointBindingCount());
        assertEquals(0L, summary.affectedRuleCount());
        assertEquals(0L, summary.affectedLinkageBindingCount());
        assertEquals(0L, summary.affectedEmergencyPlanBindingCount());
    }

    @Test
    void summarizeProductContractImpactShouldExposeObjectLevelDependencyDetails() {
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(
                catalog(9101L, 1001L, "value", "metric.value")
        ));
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(
                riskPointBinding(3001L, 7101L, "device-001", "value")
        ));
        when(riskPointMapper.selectBatchIds(any())).thenReturn(List.of(
                riskPoint(7101L, "裂缝点 A")
        ));
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(
                ruleDefinition(4101L, "裂缝阈值规则", "value")
        ));
        when(linkageBindingMapper.selectList(any())).thenReturn(List.of(
                linkageBinding(5101L, 9101L, 6101L)
        ));
        when(linkageRuleMapper.selectBatchIds(any())).thenReturn(List.of(
                linkageRule(6101L, "裂缝联动规则")
        ));
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of(
                emergencyPlanBinding(5201L, 9101L, 6201L)
        ));
        when(emergencyPlanMapper.selectBatchIds(any())).thenReturn(List.of(
                emergencyPlan(6201L, "裂缝应急预案")
        ));

        RiskGovernanceImpactDependencyQueryServiceImpl service = newService();

        GovernanceImpactDependencySummary summary = service.summarizeProductContractImpact(1001L, Set.of("value"));

        assertEquals(1, summary.affectedRiskMetrics().size());
        assertEquals("metric.value", summary.affectedRiskMetrics().get(0).riskMetricCode());
        assertEquals("value", summary.affectedRiskMetrics().get(0).contractIdentifier());
        assertEquals(1, summary.affectedRiskPointBindings().size());
        assertEquals("裂缝点 A", summary.affectedRiskPointBindings().get(0).riskPointName());
        assertEquals("device-001", summary.affectedRiskPointBindings().get(0).deviceCode());
        assertEquals(1, summary.affectedRules().size());
        assertEquals("裂缝阈值规则", summary.affectedRules().get(0).ruleName());
        assertEquals("value", summary.affectedRules().get(0).metricIdentifier());
        assertEquals(1, summary.affectedLinkageBindings().size());
        assertEquals("裂缝联动规则", summary.affectedLinkageBindings().get(0).linkageRuleName());
        assertEquals(1, summary.affectedEmergencyPlanBindings().size());
        assertEquals("裂缝应急预案", summary.affectedEmergencyPlanBindings().get(0).emergencyPlanName());
    }

    private RiskGovernanceImpactDependencyQueryServiceImpl newService() {
        return new RiskGovernanceImpactDependencyQueryServiceImpl(
                riskMetricCatalogMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                riskPointMapper,
                linkageRuleMapper,
                emergencyPlanMapper
        );
    }

    private RiskMetricCatalog catalog(Long id, Long productId, String contractIdentifier, String riskMetricCode) {
        RiskMetricCatalog catalog = new RiskMetricCatalog();
        catalog.setId(id);
        catalog.setProductId(productId);
        catalog.setContractIdentifier(contractIdentifier);
        catalog.setRiskMetricCode(riskMetricCode);
        catalog.setEnabled(1);
        catalog.setDeleted(0);
        return catalog;
    }

    private RiskPointDevice riskPointBinding(Long id, Long riskPointId, String deviceCode, String metricIdentifier) {
        RiskPointDevice binding = new RiskPointDevice();
        binding.setId(id);
        binding.setRiskPointId(riskPointId);
        binding.setDeviceCode(deviceCode);
        binding.setMetricIdentifier(metricIdentifier);
        binding.setDeleted(0);
        return binding;
    }

    private RiskPoint riskPoint(Long id, String name) {
        RiskPoint point = new RiskPoint();
        point.setId(id);
        point.setRiskPointName(name);
        point.setDeleted(0);
        return point;
    }

    private RuleDefinition ruleDefinition(Long id, String ruleName, String metricIdentifier) {
        RuleDefinition rule = new RuleDefinition();
        rule.setId(id);
        rule.setRuleName(ruleName);
        rule.setMetricIdentifier(metricIdentifier);
        rule.setStatus(0);
        rule.setDeleted(0);
        return rule;
    }

    private RiskMetricLinkageBinding linkageBinding(Long id, Long riskMetricId, Long linkageRuleId) {
        RiskMetricLinkageBinding binding = new RiskMetricLinkageBinding();
        binding.setId(id);
        binding.setRiskMetricId(riskMetricId);
        binding.setLinkageRuleId(linkageRuleId);
        binding.setBindingStatus("ACTIVE");
        binding.setDeleted(0);
        return binding;
    }

    private LinkageRule linkageRule(Long id, String ruleName) {
        LinkageRule rule = new LinkageRule();
        rule.setId(id);
        rule.setRuleName(ruleName);
        rule.setDeleted(0);
        return rule;
    }

    private RiskMetricEmergencyPlanBinding emergencyPlanBinding(Long id, Long riskMetricId, Long emergencyPlanId) {
        RiskMetricEmergencyPlanBinding binding = new RiskMetricEmergencyPlanBinding();
        binding.setId(id);
        binding.setRiskMetricId(riskMetricId);
        binding.setEmergencyPlanId(emergencyPlanId);
        binding.setBindingStatus("ACTIVE");
        binding.setDeleted(0);
        return binding;
    }

    private EmergencyPlan emergencyPlan(Long id, String planName) {
        EmergencyPlan plan = new EmergencyPlan();
        plan.setId(id);
        plan.setPlanName(planName);
        plan.setDeleted(0);
        return plan;
    }
}
