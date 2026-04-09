package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Telemetry v2 raw 批量写入器。
 */
@Service
public class TelemetryRawBatchWriter {

    private final TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    private final TelemetryV2SchemaSupport schemaSupport;
    private final TelemetryV2TableNamingStrategy tableNamingStrategy;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public TelemetryRawBatchWriter(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider,
                                   TelemetryV2SchemaSupport schemaSupport,
                                   TelemetryV2TableNamingStrategy tableNamingStrategy) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.schemaSupport = schemaSupport;
        this.tableNamingStrategy = tableNamingStrategy;
    }

    public List<TelemetryV2Point> toPoints(DeviceProcessingTarget target,
                                           Map<String, Object> properties,
                                           Map<String, DevicePropertyMetadata> metadataMap) {
        if (target == null || target.getDevice() == null || target.getMessage() == null || properties == null || properties.isEmpty()) {
            return List.of();
        }
        LocalDateTime reportedAt = target.getMessage().getTimestamp() == null
                ? LocalDateTime.now()
                : target.getMessage().getTimestamp();
        LocalDateTime ingestedAt = LocalDateTime.now();
        List<TelemetryV2Point> points = new ArrayList<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            DevicePropertyMetadata metadata = metadataMap == null ? null : metadataMap.get(entry.getKey());
            TelemetryV2Point point = new TelemetryV2Point();
            point.setStreamKind(TelemetryStreamKind.resolve(
                    target.getMessage().getMessageType(),
                    entry.getKey(),
                    metadata,
                    entry.getValue()
            ));
            point.setTenantId(target.getDevice().getTenantId());
            point.setDeviceId(target.getDevice().getId());
            point.setProductId(target.getDevice().getProductId());
            point.setDeviceCode(target.getDevice().getDeviceCode());
            point.setProductKey(target.getMessage().getProductKey());
            point.setProtocolCode(target.getMessage().getProtocolCode());
            point.setMetricId(entry.getKey());
            point.setMetricCode(entry.getKey());
            point.setMetricName(resolveMetricName(entry.getKey(), metadata));
            point.setValueType(resolveValueType(metadata, entry.getValue()));
            point.setValueDouble(resolveDoubleValue(entry.getValue()));
            point.setValueLong(resolveLongValue(entry.getValue()));
            point.setValueBool(resolveBooleanValue(entry.getValue()));
            point.setValueText(resolveValueText(entry.getValue()));
            point.setTraceId(target.getMessage().getTraceId());
            point.setSourceMessageType(target.getMessage().getMessageType());
            point.setSensorGroup(point.getStreamKind().name().toLowerCase(Locale.ROOT));
            point.setReportedAt(reportedAt);
            point.setIngestedAt(ingestedAt);
            points.add(point);
        }
        return points;
    }

    public TelemetryPersistResult write(List<TelemetryV2Point> points) {
        if (points == null || points.isEmpty()) {
            return TelemetryPersistResult.skipped("EMPTY_PROPERTIES", "tdengine-v2", 0);
        }
        schemaSupport.ensureTables();
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getJdbcTemplate();
        Map<String, List<TelemetryV2Point>> groupedPoints = new LinkedHashMap<>();
        for (TelemetryV2Point point : points) {
            String childTable = tableNamingStrategy.resolveChildTableName(
                    point.getStreamKind(),
                    point.getTenantId(),
                    point.getDeviceId()
            );
            groupedPoints.computeIfAbsent(childTable, key -> new ArrayList<>()).add(point);
        }
        for (List<TelemetryV2Point> grouped : groupedPoints.values()) {
            TelemetryV2Point samplePoint = grouped.get(0);
            String childTable = tableNamingStrategy.resolveChildTableName(
                    samplePoint.getStreamKind(),
                    samplePoint.getTenantId(),
                    samplePoint.getDeviceId()
            );
            schemaSupport.ensureChildTable(samplePoint);
            jdbcTemplate.batchUpdate(buildInsertSql(childTable), buildBatchArgs(grouped));
        }
        return TelemetryPersistResult.persisted("TDENGINE_V2_RAW", "tdengine-v2", points.size(), 0, 0, 0, 0);
    }

    private String buildInsertSql(String childTable) {
        return "INSERT INTO " + childTable + " ("
                + "ts, metric_id, reported_at, ingested_at, value_double, value_long, value_bool, value_text,"
                + " quality_code, alarm_flag, trace_id, session_id, source_message_type"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private List<Object[]> buildBatchArgs(List<TelemetryV2Point> points) {
        List<Object[]> args = new ArrayList<>();
        for (TelemetryV2Point point : points) {
            args.add(new Object[]{
                    Timestamp.valueOf(point.getReportedAt() == null ? LocalDateTime.now() : point.getReportedAt()),
                    truncate(point.getMetricId(), 128),
                    Timestamp.valueOf(point.getReportedAt() == null ? LocalDateTime.now() : point.getReportedAt()),
                    Timestamp.valueOf(point.getIngestedAt() == null ? LocalDateTime.now() : point.getIngestedAt()),
                    point.getValueDouble(),
                    point.getValueLong(),
                    point.getValueBool(),
                    truncate(point.getValueText(), 1024),
                    truncate(point.getQualityCode(), 32),
                    point.getAlarmFlag(),
                    truncate(point.getTraceId(), 64),
                    truncate(point.getSessionId(), 64),
                    truncate(point.getSourceMessageType(), 32)
            });
        }
        return args;
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

    private Long resolveLongValue(Object value) {
        if (value instanceof Byte number) {
            return number.longValue();
        }
        if (value instanceof Short number) {
            return number.longValue();
        }
        if (value instanceof Integer number) {
            return number.longValue();
        }
        if (value instanceof Long number) {
            return number;
        }
        return null;
    }

    private Double resolveDoubleValue(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return null;
        }
        return number.doubleValue();
    }

    private Boolean resolveBooleanValue(Object value) {
        return value instanceof Boolean bool ? bool : null;
    }

    private String resolveValueText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
