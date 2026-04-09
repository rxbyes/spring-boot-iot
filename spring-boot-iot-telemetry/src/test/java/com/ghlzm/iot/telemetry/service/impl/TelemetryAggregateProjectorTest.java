package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryProjectionTask;
import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryAggregateProjectorTest {

    @Mock
    private TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private TelemetryAggregateSchemaSupport schemaSupport;

    private TelemetryAggregateProjector projector;

    @BeforeEach
    void setUp() {
        when(jdbcTemplateProvider.getJdbcTemplate()).thenReturn(jdbcTemplate);
        projector = new TelemetryAggregateProjector(
                jdbcTemplateProvider,
                schemaSupport,
                new TelemetryAggregateTableNamingStrategy()
        );
    }

    @Test
    void shouldAggregateOnlyMeasurePointsWithinSameHourWindow() {
        stubExistingRows(sql -> null);
        TelemetryProjectionTask task = new TelemetryProjectionTask();
        task.setProjectionType(TelemetryProjectionTask.ProjectionType.AGGREGATE);
        task.setPoints(List.of(
                measurePoint("temperature", "温度", 20.0D, null,
                        LocalDateTime.of(2026, 4, 1, 10, 5), "trace-1"),
                measurePoint("temperature", "温度", null, 22L,
                        LocalDateTime.of(2026, 4, 1, 10, 20), "trace-2"),
                measurePoint("humidity", "湿度", 40.0D, null,
                        LocalDateTime.of(2026, 4, 1, 10, 25), "trace-3"),
                statusPoint("online", LocalDateTime.of(2026, 4, 1, 10, 15)),
                eventPoint("alarm", LocalDateTime.of(2026, 4, 1, 10, 30))
        ));

        projector.project(task);

        verify(schemaSupport, times(1)).ensureChildTable(any());
        ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(contains("INSERT INTO tb_ah_1_2001"), batchCaptor.capture());
        List<Object[]> rows = batchCaptor.getValue();
        assertEquals(2, rows.size());

        Object[] temperatureRow = findRow(rows, "temperature");
        assertEquals(Timestamp.valueOf(LocalDateTime.of(2026, 4, 1, 10, 0)), temperatureRow[0]);
        assertEquals("temperature", temperatureRow[1]);
        assertEquals("温度", temperatureRow[3]);
        assertEquals("double", temperatureRow[4]);
        assertEquals(20.0D, temperatureRow[7]);
        assertEquals(22.0D, temperatureRow[8]);
        assertEquals(42.0D, temperatureRow[9]);
        assertEquals(22.0D, temperatureRow[10]);
        assertEquals(2L, temperatureRow[11]);
        assertEquals("trace-2", temperatureRow[12]);

        Object[] humidityRow = findRow(rows, "humidity");
        assertEquals(40.0D, humidityRow[7]);
        assertEquals(40.0D, humidityRow[8]);
        assertEquals(40.0D, humidityRow[9]);
        assertEquals(40.0D, humidityRow[10]);
        assertEquals(1L, humidityRow[11]);
    }

    @Test
    void shouldMergeExistingHourAggregateBeforeUpsert() {
        stubExistingRows(sql -> sql.contains("metric_id = 'temperature'")
                ? new ExistingAggregateRow(
                LocalDateTime.of(2026, 4, 1, 10, 1),
                LocalDateTime.of(2026, 4, 1, 10, 10),
                10.0D,
                15.0D,
                25.0D,
                15.0D,
                2L,
                "trace-old",
                "property"
        ) : null);
        TelemetryProjectionTask task = new TelemetryProjectionTask();
        task.setProjectionType(TelemetryProjectionTask.ProjectionType.AGGREGATE);
        task.setPoints(List.of(
                measurePoint("temperature", "温度", 12.0D, null,
                        LocalDateTime.of(2026, 4, 1, 10, 20), "trace-2"),
                measurePoint("temperature", "温度", null, 8L,
                        LocalDateTime.of(2026, 4, 1, 10, 30), "trace-3")
        ));

        projector.project(task);

        ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(contains("INSERT INTO tb_ah_1_2001"), batchCaptor.capture());
        List<Object[]> rows = batchCaptor.getValue();
        assertEquals(1, rows.size());
        Object[] row = rows.get(0);
        assertEquals(Timestamp.valueOf(LocalDateTime.of(2026, 4, 1, 10, 1)), row[5]);
        assertEquals(Timestamp.valueOf(LocalDateTime.of(2026, 4, 1, 10, 30)), row[6]);
        assertEquals(8.0D, row[7]);
        assertEquals(15.0D, row[8]);
        assertEquals(45.0D, row[9]);
        assertEquals(8.0D, row[10]);
        assertEquals(4L, row[11]);
        assertEquals("trace-3", row[12]);
    }

    @Test
    void shouldSwallowSchemaFailuresWithoutBreakingMainFlow() {
        TelemetryProjectionTask task = new TelemetryProjectionTask();
        task.setProjectionType(TelemetryProjectionTask.ProjectionType.AGGREGATE);
        task.setPoints(List.of(
                measurePoint("temperature", "温度", 26.5D, null,
                        LocalDateTime.of(2026, 4, 1, 10, 5), "trace-1")
        ));
        doThrow(new IllegalStateException("stable missing")).when(schemaSupport).ensureChildTable(any());

        assertDoesNotThrow(() -> projector.project(task));

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void shouldSwallowBatchWriteFailuresWithoutBreakingMainFlow() {
        stubExistingRows(sql -> null);
        TelemetryProjectionTask task = new TelemetryProjectionTask();
        task.setProjectionType(TelemetryProjectionTask.ProjectionType.AGGREGATE);
        task.setPoints(List.of(
                measurePoint("temperature", "娓╁害", 26.5D, null,
                        LocalDateTime.of(2026, 4, 1, 10, 5), "trace-1")
        ));
        doThrow(new IllegalStateException("write failed"))
                .when(jdbcTemplate)
                .batchUpdate(eq("INSERT INTO tb_ah_1_2001 (ts, metric_id, metric_code, metric_name, value_type, first_reported_at, last_reported_at,"
                        + " min_value_double, max_value_double, sum_value_double, last_value_double, sample_count,"
                        + " trace_id, source_message_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"), any(List.class));

        assertDoesNotThrow(() -> projector.project(task));

        verify(schemaSupport, times(1)).ensureChildTable(any());
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any(List.class));
    }

    private void stubExistingRows(Function<String, ExistingAggregateRow> rowResolver) {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            ResultSetExtractor<?> extractor = invocation.getArgument(1, ResultSetExtractor.class);
            ExistingAggregateRow row = rowResolver.apply(sql);
            ResultSet resultSet = mock(ResultSet.class);
            if (row == null) {
                when(resultSet.next()).thenReturn(false);
            } else {
                when(resultSet.next()).thenReturn(true, false);
                when(resultSet.getTimestamp("first_reported_at")).thenReturn(Timestamp.valueOf(row.firstReportedAt()));
                when(resultSet.getTimestamp("last_reported_at")).thenReturn(Timestamp.valueOf(row.lastReportedAt()));
                when(resultSet.getDouble("min_value_double")).thenReturn(row.minValueDouble());
                when(resultSet.getDouble("max_value_double")).thenReturn(row.maxValueDouble());
                when(resultSet.getDouble("sum_value_double")).thenReturn(row.sumValueDouble());
                when(resultSet.getDouble("last_value_double")).thenReturn(row.lastValueDouble());
                when(resultSet.getLong("sample_count")).thenReturn(row.sampleCount());
                when(resultSet.getString("trace_id")).thenReturn(row.traceId());
                when(resultSet.getString("source_message_type")).thenReturn(row.sourceMessageType());
            }
            return extractor.extractData(resultSet);
        });
    }

    private Object[] findRow(List<Object[]> rows, String metricId) {
        return rows.stream()
                .filter(row -> metricId.equals(row[1]))
                .findFirst()
                .orElseThrow();
    }

    private TelemetryV2Point measurePoint(String metricId,
                                          String metricName,
                                          Double valueDouble,
                                          Long valueLong,
                                          LocalDateTime reportedAt,
                                          String traceId) {
        TelemetryV2Point point = basePoint(metricId, metricName, reportedAt, traceId);
        point.setStreamKind(TelemetryStreamKind.MEASURE);
        point.setValueType("double");
        point.setValueDouble(valueDouble);
        point.setValueLong(valueLong);
        return point;
    }

    private TelemetryV2Point statusPoint(String metricId, LocalDateTime reportedAt) {
        TelemetryV2Point point = basePoint(metricId, metricId, reportedAt, "trace-status");
        point.setStreamKind(TelemetryStreamKind.STATUS);
        point.setValueType("bool");
        point.setValueBool(Boolean.TRUE);
        return point;
    }

    private TelemetryV2Point eventPoint(String metricId, LocalDateTime reportedAt) {
        TelemetryV2Point point = basePoint(metricId, metricId, reportedAt, "trace-event");
        point.setStreamKind(TelemetryStreamKind.EVENT);
        point.setValueType("string");
        point.setValueText("ALARM");
        return point;
    }

    private TelemetryV2Point basePoint(String metricId,
                                       String metricName,
                                       LocalDateTime reportedAt,
                                       String traceId) {
        TelemetryV2Point point = new TelemetryV2Point();
        point.setTenantId(1L);
        point.setDeviceId(2001L);
        point.setProductId(1001L);
        point.setDeviceCode("demo-device-01");
        point.setProductKey("demo-product");
        point.setMetricId(metricId);
        point.setMetricCode(metricId);
        point.setMetricName(metricName);
        point.setReportedAt(reportedAt);
        point.setIngestedAt(reportedAt.plusSeconds(1));
        point.setTraceId(traceId);
        point.setSourceMessageType("property");
        point.setSensorGroup("measure");
        return point;
    }

    private record ExistingAggregateRow(LocalDateTime firstReportedAt,
                                        LocalDateTime lastReportedAt,
                                        double minValueDouble,
                                        double maxValueDouble,
                                        double sumValueDouble,
                                        double lastValueDouble,
                                        long sampleCount,
                                        String traceId,
                                        String sourceMessageType) {
    }
}
