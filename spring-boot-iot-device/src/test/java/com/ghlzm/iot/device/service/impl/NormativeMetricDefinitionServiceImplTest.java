package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.dto.NormativeMetricDefinitionImportDTO;
import com.ghlzm.iot.device.dto.NormativeMetricDefinitionImportItemDTO;
import com.ghlzm.iot.device.mapper.NormativeMetricDefinitionMapper;
import com.ghlzm.iot.device.vo.NormativeMetricDefinitionImportResultVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NormativeMetricDefinitionServiceImplTest {

    @Mock
    private NormativeMetricDefinitionMapper normativeMetricDefinitionMapper;
    private NormativeMetricDefinitionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NormativeMetricDefinitionServiceImpl(normativeMetricDefinitionMapper);
    }

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

        List<NormativeMetricDefinition> rows = service.listByScenario("phase1-crack");

        assertEquals(
                List.of("value", "sensor_state"),
                rows.stream().map(NormativeMetricDefinition::getIdentifier).toList()
        );
        assertEquals(1, rows.get(0).getRiskEnabled());
        assertEquals(0, rows.get(1).getRiskEnabled());
    }

    @Test
    void springBeanFactoryShouldResolveRuntimeConstructorWhenMultipleConstructorsExist() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(NormativeMetricDefinitionMapper.class, () -> normativeMetricDefinitionMapper);
            context.registerBean(NormativeMetricDefinitionServiceImpl.class);

            context.refresh();
            NormativeMetricDefinitionServiceImpl bean = context.getBean(NormativeMetricDefinitionServiceImpl.class);

            assertEquals(NormativeMetricDefinitionServiceImpl.class, bean.getClass());
        }
    }

    @Test
    void previewImportShouldReportDuplicateFallbackKeyAgainstExistingActiveDefinition() {
        NormativeMetricDefinition existing = normativeDefinition(920041L, "phase5-mud-level", "MUD_LEVEL", "value");
        existing.setMonitorContentCode("L4");
        existing.setMonitorTypeCode("NW");
        existing.setStatus("ACTIVE");
        when(normativeMetricDefinitionMapper.selectList(any())).thenReturn(List.of(existing));

        NormativeMetricDefinitionImportResultVO result = service.previewImport(importPayload(
                importItem(null, "phase5-mud-level-v2", "MUD_LEVEL", "value", "L4", "NW")
        ));

        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getReadyCount());
        assertEquals(1, result.getConflictCount());
        assertEquals("CONFLICT_DUPLICATE_FALLBACK_KEY", result.getRows().get(0).getStatus());
        verify(normativeMetricDefinitionMapper, never()).insert(any(NormativeMetricDefinition.class));
        verify(normativeMetricDefinitionMapper, never()).updateById(any(NormativeMetricDefinition.class));
    }

    @Test
    void applyImportShouldCreateReadyRowsWithSafeDefaults() {
        when(normativeMetricDefinitionMapper.selectList(any())).thenReturn(List.of());
        when(normativeMetricDefinitionMapper.insert(any(NormativeMetricDefinition.class))).thenReturn(1);

        NormativeMetricDefinitionImportResultVO result = service.applyImport(importPayload(
                importItem(null, "phase4-surface-flow-speed", "SURFACE_FLOW_SPEED", "value", "L4", "BMLS")
        ));

        assertEquals(1, result.getAppliedCount());
        assertEquals("APPLIED_CREATE", result.getRows().get(0).getStatus());
        ArgumentCaptor<NormativeMetricDefinition> captor = ArgumentCaptor.forClass(NormativeMetricDefinition.class);
        verify(normativeMetricDefinitionMapper).insert(captor.capture());
        NormativeMetricDefinition inserted = captor.getValue();
        assertEquals("phase4-surface-flow-speed", inserted.getScenarioCode());
        assertEquals("SURFACE_FLOW_SPEED", inserted.getDeviceFamily());
        assertEquals("value", inserted.getIdentifier());
        assertEquals("表面流速", inserted.getDisplayName());
        assertEquals("ACTIVE", inserted.getStatus());
        assertEquals(0, inserted.getRiskEnabled());
        assertEquals(1, inserted.getTrendEnabled());
        assertEquals(1, inserted.getInsightEnabled());
        assertEquals(1, inserted.getAnalyticsEnabled());
        assertEquals(0, inserted.getDeleted());
    }

    private NormativeMetricDefinition normativeDefinition(Long id,
                                                          String scenarioCode,
                                                          String deviceFamily,
                                                          String identifier) {
        NormativeMetricDefinition definition = new NormativeMetricDefinition();
        definition.setId(id);
        definition.setScenarioCode(scenarioCode);
        definition.setDeviceFamily(deviceFamily);
        definition.setIdentifier(identifier);
        definition.setDeleted(0);
        return definition;
    }

    private NormativeMetricDefinitionImportDTO importPayload(NormativeMetricDefinitionImportItemDTO... items) {
        NormativeMetricDefinitionImportDTO dto = new NormativeMetricDefinitionImportDTO();
        dto.setItems(List.of(items));
        return dto;
    }

    private NormativeMetricDefinitionImportItemDTO importItem(Long id,
                                                              String scenarioCode,
                                                              String deviceFamily,
                                                              String identifier,
                                                              String monitorContentCode,
                                                              String monitorTypeCode) {
        NormativeMetricDefinitionImportItemDTO item = new NormativeMetricDefinitionImportItemDTO();
        item.setId(id);
        item.setScenarioCode(scenarioCode);
        item.setDeviceFamily(deviceFamily);
        item.setIdentifier(identifier);
        item.setDisplayName("表面流速");
        item.setUnit("m/s");
        item.setPrecisionDigits(3);
        item.setMonitorContentCode(monitorContentCode);
        item.setMonitorTypeCode(monitorTypeCode);
        return item;
    }
}
