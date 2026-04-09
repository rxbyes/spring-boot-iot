package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.service.RiskMetricActionBindingSyncService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmergencyPlanServiceImplTest {

    @Mock
    private RiskMetricActionBindingSyncService bindingSyncService;

    private static void initLambdaCache() {
        if (TableInfoHelper.getTableInfo(EmergencyPlan.class) != null) {
            return;
        }
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new Configuration(), "");
        assistant.setCurrentNamespace(EmergencyPlan.class.getName());
        LambdaUtils.installCache(TableInfoHelper.initTableInfo(assistant, EmergencyPlan.class));
    }

    @ParameterizedTest
    @CsvSource({
            "red,red,critical",
            "orange,orange,warning",
            "yellow,yellow,medium",
            "blue,blue,info"
    })
    void pagePlanListShouldExpandAlarmLevelFilterForLegacyValues(String alarmLevel,
                                                                 String normalizedValue,
                                                                 String legacyValue) {
        initLambdaCache();
        EmergencyPlanServiceImpl service = spy(new EmergencyPlanServiceImpl(bindingSyncService));
        Page<EmergencyPlan> page = new Page<>(1L, 10L);
        page.setRecords(java.util.List.of());
        page.setTotal(0L);
        doReturn(page).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        service.pagePlanList(null, alarmLevel, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<EmergencyPlan>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(service).page(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<EmergencyPlan> wrapper = wrapperCaptor.getValue();
        wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().values().contains(normalizedValue));
        assertTrue(wrapper.getParamNameValuePairs().values().contains(legacyValue));
    }

    @Test
    void addPlanShouldNormalizeAlarmLevelAndTriggerBindingSync() {
        EmergencyPlanServiceImpl service = spy(new EmergencyPlanServiceImpl(bindingSyncService));
        EmergencyPlan plan = new EmergencyPlan();
        plan.setAlarmLevel("critical");
        doReturn(true).when(service).save(any(EmergencyPlan.class));

        service.addPlan(plan, 1002L);

        assertEquals("red", plan.getAlarmLevel());
        assertEquals("red", plan.getRiskLevel());
        verify(bindingSyncService).rebuildEmergencyPlanBindingsForPlan(plan, 1002L, "AUTO_INFERRED");
    }
}
