package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 读取标准化兼容表中的历史点位。
 */
@Service
public class NormalizedTelemetryHistoryReader {

    private static final String COUNT_SQL = """
            SELECT COUNT(1)
            FROM iot_device_telemetry_point
            WHERE device_id = ?
            """;

    private static final String SELECT_SQL = """
            SELECT
                ts,
                reported_at,
                trace_id,
                message_type,
                metric_code,
                metric_name,
                value_type,
                value_text,
                value_long,
                value_double,
                value_bool
            FROM iot_device_telemetry_point
            WHERE device_id = ?
            ORDER BY reported_at ASC, ts ASC
            """;

    private final TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    private final TdengineTelemetrySchemaSupport schemaSupport;

    public NormalizedTelemetryHistoryReader(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider,
                                            TdengineTelemetrySchemaSupport schemaSupport) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.schemaSupport = schemaSupport;
    }

    public boolean hasHistory(Long deviceId) {
        if (deviceId == null) {
            return false;
        }
        schemaSupport.ensureTable();
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getJdbcTemplate();
        Integer count = jdbcTemplate.queryForObject(COUNT_SQL, Integer.class, deviceId);
        return count != null && count > 0;
    }

    public List<TelemetryV2Point> listHistory(Device device,
                                              Product product,
                                              Map<String, DevicePropertyMetadata> metadataMap,
                                              int batchSize) {
        if (device == null || device.getId() == null) {
            return List.of();
        }
        schemaSupport.ensureTable();
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getJdbcTemplate();
        return jdbcTemplate.query(SELECT_SQL, rs -> {
            List<TelemetryV2Point> points = new ArrayList<>();
            while (rs.next()) {
                String metricCode = rs.getString("metric_code");
                DevicePropertyMetadata metadata = metadataMap == null ? null : metadataMap.get(metricCode);
                Object value = resolveValue(
                        rs.getString("value_type"),
                        rs.getString("value_text"),
                        rs.getObject("value_long"),
                        rs.getObject("value_double"),
                        rs.getObject("value_bool")
                );
                Timestamp reportedAt = rs.getTimestamp("reported_at");
                Timestamp ts = rs.getTimestamp("ts");
                LocalDateTime effectiveTime = reportedAt != null ? reportedAt.toLocalDateTime()
                        : ts == null ? null : ts.toLocalDateTime();
                TelemetryV2Point point = new TelemetryV2Point();
                point.setStreamKind(TelemetryStreamKind.resolve(
                        rs.getString("message_type"),
                        metricCode,
                        metadata,
                        value
                ));
                point.setTenantId(device.getTenantId());
                point.setDeviceId(device.getId());
                point.setProductId(device.getProductId());
                point.setDeviceCode(device.getDeviceCode());
                point.setProductKey(product == null ? null : product.getProductKey());
                point.setMetricId(metricCode);
                point.setMetricCode(metricCode);
                point.setMetricName(resolveMetricName(metricCode, rs.getString("metric_name"), metadata));
                point.setValueType(resolveValueType(rs.getString("value_type"), metadata));
                point.setValueText(value instanceof String text ? text : null);
                point.setValueLong(value instanceof Byte number ? number.longValue()
                        : value instanceof Short number ? number.longValue()
                        : value instanceof Integer number ? number.longValue()
                        : value instanceof Long number ? number
                        : null);
                point.setValueDouble(value instanceof Float number ? number.doubleValue()
                        : value instanceof Double number ? number
                        : null);
                point.setValueBool(value instanceof Boolean bool ? bool : null);
                point.setTraceId(rs.getString("trace_id"));
                point.setSourceMessageType(rs.getString("message_type"));
                point.setSensorGroup(point.getStreamKind().name().toLowerCase(Locale.ROOT));
                point.setReportedAt(effectiveTime);
                point.setIngestedAt(effectiveTime);
                points.add(point);
            }
            return points;
        }, device.getId());
    }

    private String resolveMetricName(String metricCode, String rowMetricName, DevicePropertyMetadata metadata) {
        if (metadata != null && metadata.getPropertyName() != null && !metadata.getPropertyName().isBlank()) {
            return metadata.getPropertyName();
        }
        if (rowMetricName != null && !rowMetricName.isBlank()) {
            return rowMetricName;
        }
        return metricCode;
    }

    private String resolveValueType(String rowValueType, DevicePropertyMetadata metadata) {
        if (metadata != null && metadata.getDataType() != null && !metadata.getDataType().isBlank()) {
            return metadata.getDataType();
        }
        return rowValueType;
    }

    private Object resolveValue(String valueType,
                                String valueText,
                                Object valueLong,
                                Object valueDouble,
                                Object valueBool) {
        String normalizedType = valueType == null ? "" : valueType.trim().toLowerCase(Locale.ROOT);
        if ("bool".equals(normalizedType) || "boolean".equals(normalizedType)) {
            return valueBool instanceof Boolean bool ? bool : valueText == null ? null : Boolean.parseBoolean(valueText);
        }
        if ("int".equals(normalizedType) || "integer".equals(normalizedType) || "long".equals(normalizedType)) {
            return valueLong instanceof Number number ? number.longValue() : null;
        }
        if ("double".equals(normalizedType)
                || "float".equals(normalizedType)
                || "decimal".equals(normalizedType)
                || "number".equals(normalizedType)) {
            return valueDouble instanceof Number number ? number.doubleValue() : null;
        }
        return valueText;
    }
}
