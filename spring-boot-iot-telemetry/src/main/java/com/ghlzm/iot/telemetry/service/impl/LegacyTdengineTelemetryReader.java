package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.telemetry.service.model.TelemetryLatestPoint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 从 legacy stable 读取设备最新遥测。
 */
@Service
public class LegacyTdengineTelemetryReader {

    private final TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    private final LegacyTdengineSchemaInspector schemaInspector;
    private final LegacyTdengineDeviceMetadataResolver deviceMetadataResolver;

    public LegacyTdengineTelemetryReader(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider,
                                         LegacyTdengineSchemaInspector schemaInspector,
                                         LegacyTdengineDeviceMetadataResolver deviceMetadataResolver) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.schemaInspector = schemaInspector;
        this.deviceMetadataResolver = deviceMetadataResolver;
    }

    public List<TelemetryLatestPoint> listLatestPoints(Device device,
                                                       Product product,
                                                       Map<String, DevicePropertyMetadata> metadataMap) {
        if (metadataMap == null || metadataMap.isEmpty()) {
            return List.of();
        }
        Map<String, List<MappedMetric>> mappedMetricsByStable = groupMappedMetrics(metadataMap);
        if (mappedMetricsByStable.isEmpty()) {
            return List.of();
        }

        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getJdbcTemplate();
        LegacyTdengineDeviceMetadataResolver.LegacyTdengineDeviceMetadata deviceMetadata =
                deviceMetadataResolver.resolve(device);
        List<TelemetryLatestPoint> points = new ArrayList<>();

        for (Map.Entry<String, List<MappedMetric>> entry : mappedMetricsByStable.entrySet()) {
            LegacyTdengineSchemaInspector.LegacyTdengineTableSchema schema =
                    schemaInspector.describeStable(entry.getKey());
            List<MappedMetric> validMetrics = entry.getValue().stream()
                    .filter(metric -> schema.hasColumn(metric.column()))
                    .toList();
            if (validMetrics.isEmpty()) {
                continue;
            }
            StableLatestRow row = queryLatestRow(jdbcTemplate, schema, validMetrics, deviceMetadata);
            if (row == null) {
                continue;
            }
            LocalDateTime reportedAt = row.reportedAt();
            for (MappedMetric metric : validMetrics) {
                TelemetryLatestPoint point = new TelemetryLatestPoint();
                point.setReportedAt(reportedAt);
                point.setDeviceCode(device == null ? null : device.getDeviceCode());
                point.setProductKey(product == null ? null : product.getProductKey());
                point.setMetricCode(metric.identifier());
                point.setMetricName(metric.metadata().getPropertyName() == null
                        ? metric.identifier()
                        : metric.metadata().getPropertyName());
                point.setValueType(metric.metadata().getDataType());
                point.setValue(normalizeValue(schema.getColumnType(metric.column()), row.values().get(metric.column())));
                points.add(point);
            }
        }
        return points;
    }

    private Map<String, List<MappedMetric>> groupMappedMetrics(Map<String, DevicePropertyMetadata> metadataMap) {
        Map<String, List<MappedMetric>> mappedMetricsByStable = new LinkedHashMap<>();
        for (Map.Entry<String, DevicePropertyMetadata> entry : metadataMap.entrySet()) {
            DevicePropertyMetadata.TdengineLegacyMapping mapping =
                    entry.getValue() == null ? null : entry.getValue().getTdengineLegacyMapping();
            if (mapping == null || Boolean.FALSE.equals(mapping.getEnabled())) {
                continue;
            }
            mappedMetricsByStable.computeIfAbsent(mapping.getStable(), key -> new ArrayList<>())
                    .add(new MappedMetric(entry.getKey(), mapping.getColumn(), entry.getValue()));
        }
        return mappedMetricsByStable;
    }

    private StableLatestRow queryLatestRow(JdbcTemplate jdbcTemplate,
                                           LegacyTdengineSchemaInspector.LegacyTdengineTableSchema schema,
                                           List<MappedMetric> metrics,
                                           LegacyTdengineDeviceMetadataResolver.LegacyTdengineDeviceMetadata deviceMetadata) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ts, ");
        sql.append(schema.hasColumn("rd") ? "rd" : "ts AS rd");
        for (MappedMetric metric : metrics) {
            sql.append(", ").append(metric.column());
        }
        sql.append(" FROM ").append(schema.getStable()).append(" WHERE device_sn = ?");
        List<Object> args = new ArrayList<>();
        args.add(deviceMetadata.getDeviceSn());
        if (hasText(deviceMetadata.getLocation())) {
            sql.append(" AND location = ?");
            args.add(deviceMetadata.getLocation());
        }
        sql.append(" ORDER BY ts DESC LIMIT 1");
        return jdbcTemplate.query(sql.toString(), rs -> {
            if (!rs.next()) {
                return null;
            }
            Timestamp ts = rs.getTimestamp("ts");
            Timestamp rd = rs.getTimestamp("rd");
            Map<String, Object> values = new LinkedHashMap<>();
            for (MappedMetric metric : metrics) {
                values.put(metric.column(), rs.getObject(metric.column()));
            }
            return new StableLatestRow(rd != null ? rd.toLocalDateTime() : ts == null ? null : ts.toLocalDateTime(), values);
        }, args.toArray());
    }

    private Object normalizeValue(String columnType, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        String normalizedType = columnType == null ? "" : columnType.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedType) {
            case "DOUBLE", "FLOAT" -> value instanceof Number number ? number.doubleValue() : value;
            case "BIGINT", "INT", "INTEGER", "SMALLINT", "TINYINT" -> value instanceof Number number ? number.longValue() : value;
            case "BOOL", "BOOLEAN" -> {
                if (value instanceof Boolean bool) {
                    yield bool;
                }
                String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
                yield "1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized);
            }
            default -> value;
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record MappedMetric(String identifier, String column, DevicePropertyMetadata metadata) {
    }

    private record StableLatestRow(LocalDateTime reportedAt, Map<String, Object> values) {
    }
}
