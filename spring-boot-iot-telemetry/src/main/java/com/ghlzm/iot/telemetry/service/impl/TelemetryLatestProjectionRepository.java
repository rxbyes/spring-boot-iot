package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryLatestPoint;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Telemetry v2 latest MySQL 投影仓储。
 */
@Repository
public class TelemetryLatestProjectionRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO iot_device_metric_latest (
                tenant_id,
                device_id,
                product_id,
                metric_id,
                metric_code,
                metric_name,
                value_type,
                value_double,
                value_long,
                value_bool,
                value_text,
                quality_code,
                alarm_flag,
                reported_at,
                trace_id,
                update_time
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE
                product_id = VALUES(product_id),
                metric_code = VALUES(metric_code),
                metric_name = VALUES(metric_name),
                value_type = IF(reported_at IS NULL OR VALUES(reported_at) >= reported_at, VALUES(value_type), value_type),
                value_double = IF(reported_at IS NULL OR VALUES(reported_at) >= reported_at, VALUES(value_double), value_double),
                value_long = IF(reported_at IS NULL OR VALUES(reported_at) >= reported_at, VALUES(value_long), value_long),
                value_bool = IF(reported_at IS NULL OR VALUES(reported_at) >= reported_at, VALUES(value_bool), value_bool),
                value_text = IF(reported_at IS NULL OR VALUES(reported_at) >= reported_at, VALUES(value_text), value_text),
                quality_code = IF(reported_at IS NULL OR VALUES(reported_at) >= reported_at, VALUES(quality_code), quality_code),
                alarm_flag = IF(reported_at IS NULL OR VALUES(reported_at) >= reported_at, VALUES(alarm_flag), alarm_flag),
                trace_id = IF(reported_at IS NULL OR VALUES(reported_at) >= reported_at, VALUES(trace_id), trace_id),
                reported_at = IF(reported_at IS NULL OR VALUES(reported_at) >= reported_at, VALUES(reported_at), reported_at),
                update_time = IF(reported_at IS NULL OR VALUES(reported_at) >= reported_at, NOW(), update_time)
            """;

    private static final String SELECT_LATEST_SQL = """
            SELECT
                device_id,
                metric_code,
                metric_name,
                value_type,
                value_double,
                value_long,
                value_bool,
                value_text,
                reported_at,
                trace_id
            FROM iot_device_metric_latest
            WHERE device_id = ?
            ORDER BY metric_code ASC
            """;

    private final JdbcTemplate jdbcTemplate;

    public TelemetryLatestProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void bulkUpsert(List<TelemetryV2Point> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        List<Object[]> batchArgs = new ArrayList<>();
        for (TelemetryV2Point point : points) {
            batchArgs.add(new Object[]{
                    point.getTenantId(),
                    point.getDeviceId(),
                    point.getProductId(),
                    point.getMetricId(),
                    point.getMetricCode(),
                    point.getMetricName(),
                    point.getValueType(),
                    point.getValueDouble(),
                    point.getValueLong(),
                    point.getValueBool(),
                    point.getValueText(),
                    point.getQualityCode(),
                    point.getAlarmFlag(),
                    point.getReportedAt() == null ? null : Timestamp.valueOf(point.getReportedAt()),
                    point.getTraceId()
            });
        }
        jdbcTemplate.batchUpdate(UPSERT_SQL, batchArgs);
    }

    public List<TelemetryLatestPoint> listLatestPoints(Long deviceId) {
        return jdbcTemplate.query(
                SELECT_LATEST_SQL,
                rs -> {
                    List<TelemetryLatestPoint> points = new ArrayList<>();
                    while (rs.next()) {
                        TelemetryLatestPoint point = new TelemetryLatestPoint();
                        point.setMetricCode(rs.getString("metric_code"));
                        point.setMetricName(rs.getString("metric_name"));
                        point.setValueType(rs.getString("value_type"));
                        point.setValue(resolveValue(
                                rs.getString("value_type"),
                                rs.getObject("value_double"),
                                rs.getObject("value_long"),
                                rs.getObject("value_bool"),
                                rs.getString("value_text")
                        ));
                        Timestamp reportedAt = rs.getTimestamp("reported_at");
                        point.setReportedAt(reportedAt == null ? null : reportedAt.toLocalDateTime());
                        point.setTraceId(rs.getString("trace_id"));
                        points.add(point);
                    }
                    return points;
                },
                deviceId
        );
    }

    private Object resolveValue(String valueType,
                                Object valueDouble,
                                Object valueLong,
                                Object valueBool,
                                String valueText) {
        String normalized = valueType == null ? "" : valueType.trim().toLowerCase();
        if ("bool".equals(normalized) || "boolean".equals(normalized)) {
            return valueBool;
        }
        if ("int".equals(normalized) || "integer".equals(normalized) || "long".equals(normalized)) {
            if (valueLong instanceof Number number) {
                return number.longValue();
            }
        }
        if ("double".equals(normalized)
                || "float".equals(normalized)
                || "decimal".equals(normalized)
                || "number".equals(normalized)) {
            if (valueDouble instanceof Number number) {
                return number.doubleValue();
            }
        }
        return valueText;
    }
}
