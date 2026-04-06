package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.mapper.NormativeMetricDefinitionMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NormativeMetricDefinitionServiceImplTest {

    @Mock
    private NormativeMetricDefinitionMapper normativeMetricDefinitionMapper;

    @Test
    void listPhaseOneCrackDefinitionsShouldExposeCanonicalRiskEligibleFields() {
        NormativeMetricDefinition value = new NormativeMetricDefinition();
        value.setScenarioCode("phase1-crack");
        value.setIdentifier("value");
        value.setDisplayName("裂缝监测值");
        value.setRiskEnabled(1);

        NormativeMetricDefinition sensorState = new NormativeMetricDefinition();
        sensorState.setScenarioCode("phase1-crack");
        sensorState.setIdentifier("sensor_state");
        sensorState.setDisplayName("传感器状态");
        sensorState.setRiskEnabled(0);

        when(normativeMetricDefinitionMapper.selectList(any())).thenReturn(List.of(value, sensorState));

        NormativeMetricDefinitionServiceImpl service =
                new NormativeMetricDefinitionServiceImpl(normativeMetricDefinitionMapper);

        List<NormativeMetricDefinition> rows = service.listByScenario("phase1-crack");

        assertEquals(
                List.of("value", "sensor_state"),
                rows.stream().map(NormativeMetricDefinition::getIdentifier).toList()
        );
        assertEquals(1, rows.get(0).getRiskEnabled());
        assertEquals(0, rows.get(1).getRiskEnabled());
    }
}
