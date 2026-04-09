package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.RiskMetricEmergencyPlanBinding;
import com.ghlzm.iot.alarm.entity.RiskMetricLinkageBinding;
import com.ghlzm.iot.alarm.mapper.EmergencyPlanMapper;
import com.ghlzm.iot.alarm.mapper.LinkageRuleMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricEmergencyPlanBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricLinkageBindingMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskMetricActionBindingBackfillServiceImplTest {

    @Mock
    private LinkageRuleMapper linkageRuleMapper;

    @Mock
    private EmergencyPlanMapper emergencyPlanMapper;

    @Mock
    private RiskMetricLinkageBindingMapper linkageBindingMapper;

    @Mock
    private RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper;

    @Mock
    private RiskMetricActionBindingSyncServiceImpl bindingSyncService;

    @Test
    void ensureBindingsReadyForReadShouldBackfillOnlyMissingSides() {
        RiskMetricActionBindingBackfillServiceImpl backfillService = spy(new RiskMetricActionBindingBackfillServiceImpl(
                linkageRuleMapper,
                emergencyPlanMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                bindingSyncService
        ));
        doNothing().when(backfillService).rebuildAllLinkageBindings();

        RiskMetricEmergencyPlanBinding emergencyPlanBinding = new RiskMetricEmergencyPlanBinding();
        emergencyPlanBinding.setEmergencyPlanId(3001L);
        emergencyPlanBinding.setBindingStatus("ACTIVE");
        emergencyPlanBinding.setDeleted(0);

        when(linkageRuleMapper.selectCount(any())).thenReturn(2L);
        when(emergencyPlanMapper.selectCount(any())).thenReturn(1L);
        when(linkageBindingMapper.selectCount(any())).thenReturn(0L);
        when(emergencyPlanBindingMapper.selectCount(any())).thenReturn(1L);
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(java.util.List.of(emergencyPlanBinding));

        backfillService.ensureBindingsReadyForRead();

        verify(backfillService).rebuildAllLinkageBindings();
        verify(backfillService, never()).rebuildAllEmergencyPlanBindings();
    }

    @Test
    void ensureBindingsReadyForReadShouldBackfillWhenOnlyPartOfActiveRulesHaveBindings() {
        RiskMetricActionBindingBackfillServiceImpl backfillService = spy(new RiskMetricActionBindingBackfillServiceImpl(
                linkageRuleMapper,
                emergencyPlanMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                bindingSyncService
        ));
        doNothing().when(backfillService).rebuildAllLinkageBindings();

        RiskMetricLinkageBinding linkageBinding = new RiskMetricLinkageBinding();
        linkageBinding.setLinkageRuleId(1001L);
        linkageBinding.setBindingStatus("ACTIVE");
        linkageBinding.setDeleted(0);

        when(linkageRuleMapper.selectCount(any())).thenReturn(2L);
        when(emergencyPlanMapper.selectCount(any())).thenReturn(0L);
        when(linkageBindingMapper.selectCount(any())).thenReturn(1L);
        when(linkageBindingMapper.selectList(any())).thenReturn(java.util.List.of(linkageBinding));

        backfillService.ensureBindingsReadyForRead();

        verify(backfillService).rebuildAllLinkageBindings();
        verify(backfillService, never()).rebuildAllEmergencyPlanBindings();
    }

    @Test
    void ensureBindingsReadyForReadShouldBackfillWhenOnlyPartOfActivePlansHaveBindings() {
        RiskMetricActionBindingBackfillServiceImpl backfillService = spy(new RiskMetricActionBindingBackfillServiceImpl(
                linkageRuleMapper,
                emergencyPlanMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                bindingSyncService
        ));
        doNothing().when(backfillService).rebuildAllEmergencyPlanBindings();

        RiskMetricEmergencyPlanBinding emergencyPlanBinding = new RiskMetricEmergencyPlanBinding();
        emergencyPlanBinding.setEmergencyPlanId(2001L);
        emergencyPlanBinding.setBindingStatus("ACTIVE");
        emergencyPlanBinding.setDeleted(0);

        when(linkageRuleMapper.selectCount(any())).thenReturn(0L);
        when(emergencyPlanMapper.selectCount(any())).thenReturn(2L);
        when(emergencyPlanBindingMapper.selectCount(any())).thenReturn(1L);
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(java.util.List.of(emergencyPlanBinding));

        backfillService.ensureBindingsReadyForRead();

        verify(backfillService).rebuildAllEmergencyPlanBindings();
        verify(backfillService, never()).rebuildAllLinkageBindings();
    }
}
