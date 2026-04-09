package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.entity.LinkageRule;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RiskMetricEmergencyPlanBinding;
import com.ghlzm.iot.alarm.entity.RiskMetricLinkageBinding;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricEmergencyPlanBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricLinkageBindingMapper;
import com.ghlzm.iot.alarm.service.RiskMetricActionBindingSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskMetricActionBindingSyncServiceImplTest {

    @Mock
    private RiskMetricCatalogMapper riskMetricCatalogMapper;

    @Mock
    private RiskMetricLinkageBindingMapper linkageBindingMapper;

    @Mock
    private RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper;

    @Test
    void rebuildLinkageBindingsForRuleShouldInsertNewBindingsAndRetireStaleOnes() {
        RiskMetricActionBindingSyncService service = new RiskMetricActionBindingSyncServiceImpl(
                riskMetricCatalogMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper
        );

        LinkageRule rule = new LinkageRule();
        rule.setId(7001L);
        rule.setTenantId(1L);
        rule.setTriggerCondition("[{\"metricIdentifier\":\"value\"},{\"metricIdentifier\":\"gpsTotalX\"}]");

        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(
                enabledCatalog(9101L, "value"),
                enabledCatalog(9102L, "gpsTotalX")
        ));
        when(linkageBindingMapper.selectList(any())).thenReturn(List.of(
                linkageBinding(9901L, 1L, 9103L, 7001L, "ACTIVE", "AUTO_INFERRED", 0)
        ));

        service.rebuildLinkageBindingsForRule(rule, 1001L, "AUTO_INFERRED");

        verify(linkageBindingMapper).insert(org.mockito.ArgumentMatchers.<RiskMetricLinkageBinding>argThat(binding ->
                Long.valueOf(9101L).equals(binding.getRiskMetricId())
                        && Long.valueOf(7001L).equals(binding.getLinkageRuleId())
                        && "ACTIVE".equals(binding.getBindingStatus())
                        && "AUTO_INFERRED".equals(binding.getBindingOrigin())
                        && Long.valueOf(1001L).equals(binding.getCreateBy())
        ));
        verify(linkageBindingMapper).updateById(org.mockito.ArgumentMatchers.<RiskMetricLinkageBinding>argThat(binding ->
                Long.valueOf(9901L).equals(binding.getId())
                        && "INACTIVE".equals(binding.getBindingStatus())
                        && Integer.valueOf(1).equals(binding.getDeleted())
        ));
    }

    @Test
    void rebuildEmergencyPlanBindingsForPlanShouldMatchMetricsFromPlanSearchText() {
        RiskMetricActionBindingSyncService service = new RiskMetricActionBindingSyncServiceImpl(
                riskMetricCatalogMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper
        );

        EmergencyPlan plan = new EmergencyPlan();
        plan.setId(7101L);
        plan.setTenantId(1L);
        plan.setPlanName("裂缝 value 指标预案");
        plan.setDescription("针对 gpsTotalX 和 value 的处置说明");

        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(
                enabledCatalog(9101L, "value"),
                enabledCatalog(9102L, "gpsTotalX")
        ));
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of());

        service.rebuildEmergencyPlanBindingsForPlan(plan, 1002L, "AUTO_INFERRED");

        verify(emergencyPlanBindingMapper).insert(org.mockito.ArgumentMatchers.<RiskMetricEmergencyPlanBinding>argThat(binding ->
                Long.valueOf(9101L).equals(binding.getRiskMetricId())
                        && Long.valueOf(7101L).equals(binding.getEmergencyPlanId())
                        && "ACTIVE".equals(binding.getBindingStatus())
                        && "AUTO_INFERRED".equals(binding.getBindingOrigin())
                        && Long.valueOf(1002L).equals(binding.getCreateBy())
        ));
        verify(emergencyPlanBindingMapper).insert(org.mockito.ArgumentMatchers.<RiskMetricEmergencyPlanBinding>argThat(binding ->
                Long.valueOf(9102L).equals(binding.getRiskMetricId())
                        && Long.valueOf(7101L).equals(binding.getEmergencyPlanId())
                        && "ACTIVE".equals(binding.getBindingStatus())
                        && "AUTO_INFERRED".equals(binding.getBindingOrigin())
        ));
    }

    private RiskMetricCatalog enabledCatalog(Long id, String contractIdentifier) {
        RiskMetricCatalog value = new RiskMetricCatalog();
        value.setId(id);
        value.setTenantId(1L);
        value.setEnabled(1);
        value.setContractIdentifier(contractIdentifier);
        return value;
    }

    private RiskMetricLinkageBinding linkageBinding(Long id,
                                                    Long tenantId,
                                                    Long riskMetricId,
                                                    Long linkageRuleId,
                                                    String bindingStatus,
                                                    String bindingOrigin,
                                                    Integer deleted) {
        RiskMetricLinkageBinding value = new RiskMetricLinkageBinding();
        value.setId(id);
        value.setTenantId(tenantId);
        value.setRiskMetricId(riskMetricId);
        value.setLinkageRuleId(linkageRuleId);
        value.setBindingStatus(bindingStatus);
        value.setBindingOrigin(bindingOrigin);
        value.setDeleted(deleted);
        return value;
    }
}
