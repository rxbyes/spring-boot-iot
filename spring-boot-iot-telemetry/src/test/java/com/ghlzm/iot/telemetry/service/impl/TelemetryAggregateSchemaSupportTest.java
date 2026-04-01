package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryAggregateSchemaSupportTest {

    @Mock
    private TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private TelemetryAggregateSchemaSupport schemaSupport;

    @BeforeEach
    void setUp() {
        when(jdbcTemplateProvider.getJdbcTemplate()).thenReturn(jdbcTemplate);
        schemaSupport = new TelemetryAggregateSchemaSupport(
                jdbcTemplateProvider,
                new TelemetryAggregateTableNamingStrategy()
        );
    }

    @Test
    void shouldCreateChildTableWhenStableExists() {
        stubStableExists(true);

        schemaSupport.ensureChildTable(point());

        verify(jdbcTemplate).query(contains("SHOW STABLES LIKE 'iot_agg_measure_hour'"), any(ResultSetExtractor.class));
        verify(jdbcTemplate).execute(contains("CREATE TABLE IF NOT EXISTS tb_ah_1_2001"));
        verify(jdbcTemplate).execute(contains("USING iot_agg_measure_hour"));
    }

    @Test
    void shouldCreateChildTableIdempotently() {
        stubStableExists(true);

        schemaSupport.ensureChildTable(point());
        schemaSupport.ensureChildTable(point());

        verify(jdbcTemplate, times(1)).execute(contains("CREATE TABLE IF NOT EXISTS tb_ah_1_2001"));
    }

    @Test
    void shouldThrowWhenStableMissing() {
        stubStableExists(false);

        assertThrows(IllegalStateException.class, () -> schemaSupport.ensureChildTable(point()));

        verify(jdbcTemplate, never()).execute(anyString());
    }

    private void stubStableExists(boolean stableExists) {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class))).thenAnswer(invocation -> {
            ResultSetExtractor<?> extractor = invocation.getArgument(1, ResultSetExtractor.class);
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.next()).thenReturn(stableExists);
            return extractor.extractData(resultSet);
        });
    }

    private TelemetryV2Point point() {
        TelemetryV2Point point = new TelemetryV2Point();
        point.setStreamKind(TelemetryStreamKind.MEASURE);
        point.setTenantId(1L);
        point.setDeviceId(2001L);
        point.setProductId(1001L);
        point.setMetricId("temperature");
        point.setMetricCode("temperature");
        point.setMetricName("温度");
        point.setReportedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        point.setIngestedAt(LocalDateTime.of(2026, 4, 1, 10, 0, 1));
        point.setValueType("double");
        point.setValueDouble(26.5D);
        point.setSensorGroup("measure");
        return point;
    }
}
