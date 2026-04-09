package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryV2SchemaSupportTest {

    @Mock
    private TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private TelemetryV2SchemaSupport schemaSupport;

    @BeforeEach
    void setUp() {
        when(jdbcTemplateProvider.getJdbcTemplate()).thenReturn(jdbcTemplate);
        schemaSupport = new TelemetryV2SchemaSupport(jdbcTemplateProvider, new TelemetryV2TableNamingStrategy());
    }

    @Test
    void shouldCreateMeasureStatusAndEventStables() {
        schemaSupport.ensureTables();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(3)).execute(sqlCaptor.capture());
        verify(jdbcTemplate).execute(contains("CREATE STABLE IF NOT EXISTS iot_raw_measure_point"));
        verify(jdbcTemplate).execute(contains("CREATE STABLE IF NOT EXISTS iot_raw_status_point"));
        verify(jdbcTemplate).execute(contains("CREATE STABLE IF NOT EXISTS iot_raw_event_point"));
        sqlCaptor.getAllValues().forEach(sql -> assertFalse(sql.contains("COMPOSITE KEY")));
    }

    @Test
    void shouldCreateChildTableUsingTenantDeviceAndStreamKind() {
        schemaSupport.ensureChildTable(point(TelemetryStreamKind.MEASURE));

        verify(jdbcTemplate, times(1)).execute(contains("CREATE TABLE IF NOT EXISTS tb_m_1_2001"));
        verify(jdbcTemplate, times(1)).execute(contains("USING iot_raw_measure_point"));
    }

    private TelemetryV2Point point(TelemetryStreamKind streamKind) {
        TelemetryV2Point point = new TelemetryV2Point();
        point.setStreamKind(streamKind);
        point.setTenantId(1L);
        point.setDeviceId(2001L);
        point.setProductId(1001L);
        point.setMetricId("temperature");
        point.setMetricCode("temperature");
        point.setMetricName("温度");
        point.setReportedAt(LocalDateTime.of(2026, 3, 27, 9, 0));
        point.setIngestedAt(LocalDateTime.of(2026, 3, 27, 9, 0, 1));
        point.setValueType("double");
        point.setValueDouble(26.5D);
        point.setSensorGroup("telemetry");
        point.setSourceMessageType("property");
        return point;
    }
}
