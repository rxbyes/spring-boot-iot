package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 按 legacy stable/subtable 读取历史点位并转换为标准化 v2 点模型。
 */
@Service
public class LegacyTelemetryHistoryReader {

    private static final Logger log = LoggerFactory.getLogger(LegacyTelemetryHistoryReader.class);

    private final TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    private final LegacyTdengineSchemaInspector schemaInspector;
    private final LegacyTdengineDeviceMetadataResolver deviceMetadataResolver;

    public LegacyTelemetryHistoryReader(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider,
                                        LegacyTdengineSchemaInspector schemaInspector,
                                        LegacyTdengineDeviceMetadataResolver deviceMetadataResolver) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.schemaInspector = schemaInspector;
        this.deviceMetadataResolver = deviceMetadataResolver;
    }

    public List<TelemetryV2Point> listHistory(Device device,
                                              Product product,
                                              Map<String, DevicePropertyMetadata> metadataMap,
                                              Map<String, TelemetryMetricMapping> mappingMap,
                                              int batchSize) {
        return listHistory(device, product, metadataMap, mappingMap, null, null, batchSize);
    }

    public List<TelemetryV2Point> listHistory(Device device,
                                              Product product,
                                              Map<String, DevicePropertyMetadata> metadataMap,
                                              Map<String, TelemetryMetricMapping> mappingMap,
                                              LocalDateTime windowStart,
                                              LocalDateTime windowEnd,
                                              int batchSize) {
        if (device == null || mappingMap == null || mappingMap.isEmpty() || metadataMap == null || metadataMap.isEmpty()) {
            return List.of();
        }
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getJdbcTemplate();
        LegacyTdengineDeviceMetadataResolver.LegacyTdengineDeviceMetadata deviceMetadata =
                deviceMetadataResolver.resolve(device);
        Map<String, List<MappedMetric>> metricsByStable = groupMappedMetrics(metadataMap, mappingMap);
        List<TelemetryV2Point> points = new ArrayList<>();
        for (Map.Entry<String, List<MappedMetric>> entry : metricsByStable.entrySet()) {
            String stable = entry.getKey();
            LegacyTdengineSchemaInspector.LegacyTdengineTableSchema schema = schemaInspector.describeStable(stable);
            String subTable = deviceMetadataResolver.resolveSubTableName(deviceMetadata, stable);
            points.addAll(readStableRows(jdbcTemplate, device, product, subTable, schema, entry.getValue(), windowStart, windowEnd, batchSize));
        }
        points.sort((left, right) -> {
            LocalDateTime leftTime = left.getReportedAt();
            LocalDateTime rightTime = right.getReportedAt();
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

    private Map<String, List<MappedMetric>> groupMappedMetrics(Map<String, DevicePropertyMetadata> metadataMap,
                                                               Map<String, TelemetryMetricMapping> mappingMap) {
        Map<String, List<MappedMetric>> metricsByStable = new LinkedHashMap<>();
        for (Map.Entry<String, TelemetryMetricMapping> entry : mappingMap.entrySet()) {
            TelemetryMetricMapping mapping = entry.getValue();
            DevicePropertyMetadata metadata = metadataMap.get(entry.getKey());
            if (mapping == null || metadata == null || !mapping.isLegacyMapped()) {
                continue;
            }
            metricsByStable.computeIfAbsent(mapping.getStable(), key -> new ArrayList<>())
                    .add(new MappedMetric(entry.getKey(), mapping.getColumn(), metadata));
        }
        return metricsByStable;
    }

    private List<TelemetryV2Point> readStableRows(JdbcTemplate jdbcTemplate,
                                                  Device device,
                                                  Product product,
                                                  String subTable,
                                                  LegacyTdengineSchemaInspector.LegacyTdengineTableSchema schema,
                                                  List<MappedMetric> metrics,
                                                  LocalDateTime windowStart,
                                                  LocalDateTime windowEnd,
                                                  int batchSize) {
        if (metrics.isEmpty()) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ts, ");
        sql.append(schema.hasColumn("rd") ? "rd" : "ts AS rd");
        for (MappedMetric metric : metrics) {
            sql.append(", ").append(metric.column());
        }
        List<Object> args = new ArrayList<>();
        sql.append(" FROM ").append(subTable);
        if (windowStart != null && windowEnd != null) {
            if (schema.hasColumn("rd")) {
                sql.append(" WHERE ((")
                        .append("rd >= ? AND rd < ?")
                        .append(") OR (")
                        .append("rd IS NULL AND ts >= ? AND ts < ?")
                        .append("))");
                args.add(Timestamp.valueOf(windowStart));
                args.add(Timestamp.valueOf(windowEnd));
                args.add(Timestamp.valueOf(windowStart));
                args.add(Timestamp.valueOf(windowEnd));
            } else {
                sql.append(" WHERE ts >= ? AND ts < ?");
                args.add(Timestamp.valueOf(windowStart));
                args.add(Timestamp.valueOf(windowEnd));
            }
        }
        sql.append(" ORDER BY ts ASC LIMIT ").append(Math.max(batchSize, 1));
        try {
            return jdbcTemplate.query(sql.toString(), rs -> {
                List<TelemetryV2Point> points = new ArrayList<>();
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("ts");
                    Timestamp rd = rs.getTimestamp("rd");
                    LocalDateTime effectiveTime = rd != null ? rd.toLocalDateTime()
                            : ts == null ? null : ts.toLocalDateTime();
                    for (MappedMetric metric : metrics) {
                        Object rawValue = rs.getObject(metric.column());
                        if (rawValue == null) {
                            continue;
                        }
                        Object normalizedValue = normalizeValue(schema.getColumnType(metric.column()), rawValue);
                        TelemetryV2Point point = new TelemetryV2Point();
                        point.setStreamKind(TelemetryStreamKind.resolve("property", metric.identifier(), metric.metadata(), normalizedValue));
                        point.setTenantId(device.getTenantId());
                        point.setDeviceId(device.getId());
                        point.setProductId(device.getProductId());
                        point.setDeviceCode(device.getDeviceCode());
                        point.setProductKey(product == null ? null : product.getProductKey());
                        point.setMetricId(metric.identifier());
                        point.setMetricCode(metric.identifier());
                        point.setMetricName(metric.metadata().getPropertyName() == null
                                ? metric.identifier() : metric.metadata().getPropertyName());
                        point.setValueType(metric.metadata().getDataType());
                        point.setValueText(normalizedValue instanceof String text ? text : null);
                        point.setValueLong(normalizedValue instanceof Byte number ? number.longValue()
                                : normalizedValue instanceof Short number ? number.longValue()
                                : normalizedValue instanceof Integer number ? number.longValue()
                                : normalizedValue instanceof Long number ? number
                                : null);
                        point.setValueDouble(normalizedValue instanceof Float number ? number.doubleValue()
                                : normalizedValue instanceof Double number ? number
                                : null);
                        point.setValueBool(normalizedValue instanceof Boolean bool ? bool : null);
                        point.setSourceMessageType("property");
                        point.setSensorGroup(point.getStreamKind().name().toLowerCase(Locale.ROOT));
                        point.setReportedAt(effectiveTime);
                        point.setIngestedAt(effectiveTime);
                        points.add(point);
                    }
                }
                return points;
            }, args.toArray());
        } catch (Exception ex) {
            log.warn("读取 legacy telemetry 历史失败, table={}, deviceId={}, error={}",
                    subTable,
                    device == null ? null : device.getId(),
                    ex.getMessage());
            return List.of();
        }
    }

    private Object normalizeValue(String columnType, Object value) {
        if (value == null) {
            return null;
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
            default -> value instanceof Timestamp timestamp ? timestamp.toLocalDateTime() : value;
        };
    }

    private record MappedMetric(String identifier, String column, DevicePropertyMetadata metadata) {
    }
}
