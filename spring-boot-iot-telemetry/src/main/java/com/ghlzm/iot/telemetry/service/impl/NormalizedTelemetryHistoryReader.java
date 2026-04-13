package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.MetricIdentifierResolver;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.device.service.model.MetricIdentifierResolution;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    private final TdengineTelemetrySchemaSupport schemaSupport;
    private final PublishedProductContractSnapshotService snapshotService;
    private final MetricIdentifierResolver metricIdentifierResolver;

    @Autowired
    public NormalizedTelemetryHistoryReader(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider,
                                            TdengineTelemetrySchemaSupport schemaSupport,
                                            PublishedProductContractSnapshotService snapshotService,
                                            MetricIdentifierResolver metricIdentifierResolver) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.schemaSupport = schemaSupport;
        this.snapshotService = snapshotService;
        this.metricIdentifierResolver = metricIdentifierResolver;
    }

    public NormalizedTelemetryHistoryReader(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider,
                                            TdengineTelemetrySchemaSupport schemaSupport) {
        this(jdbcTemplateProvider, schemaSupport, null, null);
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
        return listHistory(device, product, metadataMap, null, null, batchSize);
    }

    public List<TelemetryV2Point> listHistory(Device device,
                                              Product product,
                                              Map<String, DevicePropertyMetadata> metadataMap,
                                              LocalDateTime windowStart,
                                              LocalDateTime windowEnd,
                                              int batchSize) {
        if (device == null || device.getId() == null) {
            return List.of();
        }
        schemaSupport.ensureTable();
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getJdbcTemplate();
        List<Object> args = new ArrayList<>();
        args.add(device.getId());
        String sql = buildSelectSql(windowStart, windowEnd, batchSize);
        if (windowStart != null && windowEnd != null) {
            args.add(Timestamp.valueOf(windowStart));
            args.add(Timestamp.valueOf(windowEnd));
            args.add(Timestamp.valueOf(windowStart));
            args.add(Timestamp.valueOf(windowEnd));
        }
        return jdbcTemplate.query(sql, rs -> {
            List<TelemetryV2Point> points = new ArrayList<>();
            while (rs.next()) {
                String metricCode = resolveMetricCode(product, rs.getString("metric_code"));
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
        }, args.toArray());
    }

    private String resolveMetricCode(Product product, String metricCode) {
        if (metricCode == null || metricCode.isBlank()
                || product == null || product.getId() == null
                || snapshotService == null || metricIdentifierResolver == null) {
            return metricCode;
        }
        PublishedProductContractSnapshot snapshot = snapshotService.getRequiredSnapshot(product.getId());
        if (snapshot == null) {
            return metricCode;
        }
        MetricIdentifierResolution resolution = metricIdentifierResolver.resolveForRead(snapshot, metricCode);
        if (resolution == null || resolution.canonicalIdentifier() == null || resolution.canonicalIdentifier().isBlank()) {
            return metricCode;
        }
        if (MetricIdentifierResolution.SOURCE_RAW_IDENTIFIER.equals(resolution.source())) {
            return metricCode;
        }
        return resolution.canonicalIdentifier();
    }

    private String buildSelectSql(LocalDateTime windowStart,
                                  LocalDateTime windowEnd,
                                  int batchSize) {
        StringBuilder sql = new StringBuilder("""
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
                """);
        if (windowStart != null && windowEnd != null) {
            sql.append(" AND ((")
                    .append("reported_at >= ? AND reported_at < ?")
                    .append(") OR (")
                    .append("reported_at IS NULL AND ts >= ? AND ts < ?")
                    .append("))");
        }
        sql.append(" ORDER BY reported_at ASC, ts ASC")
                .append(" LIMIT ")
                .append(Math.max(batchSize, 1));
        return sql.toString();
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
