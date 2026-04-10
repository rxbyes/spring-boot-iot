package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricEmergencyPlanBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricLinkageBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
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
    private RiskMetricLinkageBindingMapper linkageBindingMapper;

    @Mock
    private RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper;

    @Test
    void summarizeProductContractImpactShouldAggregateDownstreamDependencyCounts() {
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(
                catalog(9101L, 1001L, "value", "metric.value"),
                catalog(9102L, 1001L, "humidity", "metric.humidity")
        ));
        when(riskPointDeviceMapper.selectCount(any())).thenReturn(3L);
        when(ruleDefinitionMapper.selectCount(any())).thenReturn(2L);
        when(linkageBindingMapper.selectCount(any())).thenReturn(4L);
        when(emergencyPlanBindingMapper.selectCount(any())).thenReturn(1L);

        RiskGovernanceImpactDependencyQueryServiceImpl service = new RiskGovernanceImpactDependencyQueryServiceImpl(
                riskMetricCatalogMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper
        );

        GovernanceImpactDependencySummary summary = service.summarizeProductContractImpact(1001L, Set.of("value", "humidity"));

        assertEquals(2L, summary.affectedRiskMetricCount());
        assertEquals(3L, summary.affectedRiskPointBindingCount());
        assertEquals(2L, summary.affectedRuleCount());
        assertEquals(4L, summary.affectedLinkageBindingCount());
        assertEquals(1L, summary.affectedEmergencyPlanBindingCount());
    }

    @Test
    void summarizeProductContractImpactShouldReturnEmptyWhenInputMissing() {
        RiskGovernanceImpactDependencyQueryServiceImpl service = new RiskGovernanceImpactDependencyQueryServiceImpl(
                riskMetricCatalogMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper
        );

        GovernanceImpactDependencySummary summary = service.summarizeProductContractImpact(1001L, Set.of());

        assertEquals(0L, summary.affectedRiskMetricCount());
        assertEquals(0L, summary.affectedRiskPointBindingCount());
        assertEquals(0L, summary.affectedRuleCount());
        assertEquals(0L, summary.affectedLinkageBindingCount());
        assertEquals(0L, summary.affectedEmergencyPlanBindingCount());
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
}
