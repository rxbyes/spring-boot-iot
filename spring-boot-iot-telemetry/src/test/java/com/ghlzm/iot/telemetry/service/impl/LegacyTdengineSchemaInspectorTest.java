package com.ghlzm.iot.telemetry.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyTdengineSchemaInspectorTest {

    @Mock
    private TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private LegacyTdengineSchemaInspector schemaInspector;

    @BeforeEach
    void setUp() {
        when(jdbcTemplateProvider.getJdbcTemplate()).thenReturn(jdbcTemplate);
        schemaInspector = new LegacyTdengineSchemaInspector(jdbcTemplateProvider);
    }

    @Test
    void describeStableShouldCacheSchemaAndNormalizeColumnTypes() throws Exception {
        when(jdbcTemplate.query(eq("DESCRIBE s1_zt_1"), any(ResultSetExtractor.class)))
                .thenAnswer(invocation -> {
                    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
                    when(resultSet.next()).thenReturn(true, true, true, false);
                    when(resultSet.getString(1)).thenReturn("ts", "temp", "device_sn");
                    when(resultSet.getString(2)).thenReturn("TIMESTAMP", "DOUBLE", "NCHAR(50)");
                    @SuppressWarnings("unchecked")
                    ResultSetExtractor<LegacyTdengineSchemaInspector.LegacyTdengineTableSchema> extractor = invocation.getArgument(1);
                    return extractor.extractData(resultSet);
                });

        LegacyTdengineSchemaInspector.LegacyTdengineTableSchema schema = schemaInspector.describeStable("s1_zt_1");
        LegacyTdengineSchemaInspector.LegacyTdengineTableSchema cachedSchema = schemaInspector.describeStable("s1_zt_1");

        assertTrue(schema.hasColumn("temp"));
        assertEquals("DOUBLE", schema.getColumnType("temp"));
        assertTrue(schema.hasColumn("device_sn"));
        assertEquals(schema.getStable(), cachedSchema.getStable());
        verify(jdbcTemplate, times(1)).query(eq("DESCRIBE s1_zt_1"), any(ResultSetExtractor.class));
    }
}
