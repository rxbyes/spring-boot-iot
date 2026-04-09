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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 直接读取 telemetry v2 raw stable / child table 的历史点位。
 */
@Service
public class TelemetryRawHistoryReader {

    private static final String SELECT_COLUMNS = """
            SELECT
                ts,
                metric_id,
                reported_at,
                ingested_at,
                value_double,
                value_long,
                value_bool,
                value_text,
                quality_code,
                alarm_flag,
                trace_id,
                session_id,
                source_message_type
            FROM
            """;

    private final TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    private final TelemetryV2SchemaSupport schemaSupport;
    private final TelemetryV2TableNamingStrategy tableNamingStrategy;

    public TelemetryRawHistoryReader(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider,
                                     TelemetryV2SchemaSupport schemaSupport,
                                     TelemetryV2TableNamingStrategy tableNamingStrategy) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.schemaSupport = schemaSupport;
        this.tableNamingStrategy = tableNamingStrategy;
    }

    public List<TelemetryV2Point> listHistory(Device device,
                                              Product product,
                                              Map<String, DevicePropertyMetadata> metadataMap,
                                              List<String> identifiers,
                                              int batchSize) {
        if (device == null || device.getId() == null || identifiers == null || identifiers.isEmpty()) {
            return List.of();
        }
        schemaSupport.ensureTables();
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getJdbcTemplate();
        Set<TelemetryStreamKind> streamKinds = resolveRequestedStreamKinds(identifiers, metadataMap);
        List<TelemetryV2Point> points = new ArrayList<>();
        for (TelemetryStreamKind streamKind : streamKinds) {
            String childTable = tableNamingStrategy.resolveChildTableName(streamKind, device.getTenantId(), device.getId());
            points.addAll(readChildTable(jdbcTemplate, childTable, streamKind, device, product, metadataMap, identifiers, batchSize));
        }
        points.sort((left, right) -> {
            LocalDateTime leftTime = resolveHistoryTime(left);
            LocalDateTime rightTime = resolveHistoryTime(right);
            if (leftTime == null && rightTime == null) {
                return 0;
            }
            if (leftTime == null) {
                return -1;
            }
            if (rightTime == null) {
                return 1;
            }
            return leftTime.compareTo(rightTime);
        });
        return points;
    }

    private List<TelemetryV2Point> readChildTable(JdbcTemplate jdbcTemplate,
                                                  String childTable,
                                                  TelemetryStreamKind streamKind,
                                                  Device device,
                                                  Product product,
                                                  Map<String, DevicePropertyMetadata> metadataMap,
                                                  List<String> identifiers,
                                                  int batchSize) {
        String sql = buildSelectSql(childTable, identifiers, batchSize);
        try {
            return jdbcTemplate.query(sql, rs -> {
                List<TelemetryV2Point> points = new ArrayList<>();
                while (rs.next()) {
                    String metricCode = rs.getString("metric_id");
                    DevicePropertyMetadata metadata = metadataMap == null ? null : metadataMap.get(metricCode);
                    Timestamp reportedAt = rs.getTimestamp("reported_at");
                    Timestamp ts = rs.getTimestamp("ts");
                    Timestamp ingestedAt = rs.getTimestamp("ingested_at");
                    Object value = resolveValue(
                            rs.getObject("value_double"),
                            rs.getObject("value_long"),
                            rs.getObject("value_bool"),
                            rs.getString("value_text")
                    );
                    TelemetryV2Point point = new TelemetryV2Point();
                    point.setStreamKind(streamKind);
                    point.setTenantId(device.getTenantId());
                    point.setDeviceId(device.getId());
                    point.setProductId(device.getProductId());
                    point.setDeviceCode(device.getDeviceCode());
                    point.setProductKey(product == null ? null : product.getProductKey());
                    point.setMetricId(metricCode);
                    point.setMetricCode(metricCode);
                    point.setMetricName(resolveMetricName(metricCode, metadata));
                    point.setValueType(resolveValueType(metadata, value));
                    point.setValueDouble(value instanceof Double number ? number : null);
                    point.setValueLong(value instanceof Byte number ? number.longValue()
                            : value instanceof Short number ? number.longValue()
                            : value instanceof Integer number ? number.longValue()
                            : value instanceof Long number ? number
                            : null);
                    point.setValueBool(value instanceof Boolean bool ? bool : null);
                    point.setValueText(value instanceof String text ? text : null);
                    point.setQualityCode(rs.getString("quality_code"));
                    point.setAlarmFlag(rs.getObject("alarm_flag") instanceof Boolean flag ? flag : Boolean.FALSE);
                    point.setTraceId(rs.getString("trace_id"));
                    point.setSessionId(rs.getString("session_id"));
                    point.setSourceMessageType(rs.getString("source_message_type"));
                    point.setSensorGroup(streamKind.name().toLowerCase(Locale.ROOT));
                    point.setReportedAt(reportedAt == null ? null : reportedAt.toLocalDateTime());
                    point.setIngestedAt(ingestedAt == null
                            ? (ts == null ? null : ts.toLocalDateTime())
                            : ingestedAt.toLocalDateTime());
                    points.add(point);
                }
                return points;
            }, identifiers.toArray());
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Set<TelemetryStreamKind> resolveRequestedStreamKinds(List<String> identifiers,
                                                                 Map<String, DevicePropertyMetadata> metadataMap) {
        Set<TelemetryStreamKind> kinds = new LinkedHashSet<>();
        for (String identifier : identifiers) {
            if (identifier == null || identifier.isBlank()) {
                continue;
            }
            DevicePropertyMetadata metadata = metadataMap == null ? null : metadataMap.get(identifier);
            TelemetryStreamKind streamKind = TelemetryStreamKind.resolve("property", identifier, metadata, null);
            if (streamKind != TelemetryStreamKind.EVENT) {
                kinds.add(streamKind);
            }
        }
        if (kinds.isEmpty()) {
            kinds.add(TelemetryStreamKind.MEASURE);
            kinds.add(TelemetryStreamKind.STATUS);
        }
        return kinds;
    }

    private String buildSelectSql(String childTable, List<String> identifiers, int batchSize) {
        String placeholders = String.join(", ", identifiers.stream().map(item -> "?").toList());
        return SELECT_COLUMNS
                + childTable
                + " WHERE metric_id IN (" + placeholders + ")"
                + " ORDER BY ts ASC"
                + " LIMIT " + Math.max(batchSize, 1);
    }

    private LocalDateTime resolveHistoryTime(TelemetryV2Point point) {
        if (point == null) {
            return null;
        }
        if (point.getIngestedAt() != null) {
            return point.getIngestedAt();
        }
        return point.getReportedAt();
    }

    private String resolveMetricName(String metricCode, DevicePropertyMetadata metadata) {
        if (metadata != null && metadata.getPropertyName() != null && !metadata.getPropertyName().isBlank()) {
            return metadata.getPropertyName();
        }
        return metricCode;
    }

    private String resolveValueType(DevicePropertyMetadata metadata, Object value) {
        if (metadata != null && metadata.getDataType() != null && !metadata.getDataType().isBlank()) {
            return metadata.getDataType();
        }
        if (value instanceof Boolean) {
            return "bool";
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return "long";
        }
        if (value instanceof Number) {
            return "double";
        }
        return "string";
    }

    private Object resolveValue(Object valueDouble,
                                Object valueLong,
                                Object valueBool,
                                String valueText) {
        if (valueDouble instanceof Number number) {
            return number.doubleValue();
        }
        if (valueLong instanceof Number number) {
            return number.longValue();
        }
        if (valueBool instanceof Boolean bool) {
            return bool;
        }
        if (valueText == null || valueText.isBlank()) {
            return null;
        }
        return valueText;
    }
}
