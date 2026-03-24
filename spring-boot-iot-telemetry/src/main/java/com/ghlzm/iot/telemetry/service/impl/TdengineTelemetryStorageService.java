package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.telemetry.service.model.TelemetryLatestPoint;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TDengine 通用兼容表存储服务。
 */
@Service
public class TdengineTelemetryStorageService {

    private static final String INSERT_SQL = """
            INSERT INTO iot_device_telemetry_point (
                ts,
                reported_at,
                tenant_id,
                device_id,
                device_code,
                product_id,
                product_key,
                protocol_code,
                message_type,
                mqtt_topic,
                trace_id,
                metric_code,
                metric_name,
                value_type,
                value_text,
                value_long,
                value_double,
                value_bool
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_LATEST_SQL = """
            SELECT
                p.ts,
                p.reported_at,
                p.device_code,
                p.product_key,
                p.metric_code,
                p.metric_name,
                p.value_type,
                p.value_text,
                p.value_long,
                p.value_double,
                p.value_bool,
                p.trace_id
            FROM iot_device_telemetry_point p
            WHERE p.device_id = ?
            ORDER BY p.metric_code ASC
                   , p.reported_at DESC
                   , p.ts DESC
            """;

    private final TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    private final TdengineTelemetrySchemaSupport tdengineTelemetrySchemaSupport;
    private final DevicePropertyMetadataService devicePropertyMetadataService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final AtomicLong lastStorageEpochMillis = new AtomicLong(0L);

    public TdengineTelemetryStorageService(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider,
                                           TdengineTelemetrySchemaSupport tdengineTelemetrySchemaSupport,
                                           DevicePropertyMetadataService devicePropertyMetadataService) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.tdengineTelemetrySchemaSupport = tdengineTelemetrySchemaSupport;
        this.devicePropertyMetadataService = devicePropertyMetadataService;
    }

    public TelemetryPersistResult persist(DeviceProcessingTarget target) {
        Map<String, DevicePropertyMetadata> metadataMap =
                devicePropertyMetadataService.listPropertyMetadataMap(target.getDevice().getProductId());
        return persist(target, target.getMessage().getProperties(), metadataMap);
    }

    public TelemetryPersistResult persist(DeviceProcessingTarget target,
                                          Map<String, Object> properties,
                                          Map<String, DevicePropertyMetadata> metadataMap) {
        tdengineTelemetrySchemaSupport.ensureTable();
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getJdbcTemplate();
        LocalDateTime reportedAt = resolveReportedAt(target);
        int pointCount = 0;
        if (properties == null || properties.isEmpty()) {
            return TelemetryPersistResult.skipped("EMPTY_PROPERTIES", "normalized-table", 0);
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            DevicePropertyMetadata metadata = metadataMap.get(entry.getKey());
            jdbcTemplate.update(
                    INSERT_SQL,
                    Timestamp.valueOf(resolveRowTimestamp()),
                    Timestamp.valueOf(reportedAt),
                    target.getDevice().getTenantId(),
                    target.getDevice().getId(),
                    truncate(target.getDevice().getDeviceCode(), 128),
                    target.getDevice().getProductId(),
                    truncate(target.getMessage().getProductKey(), 128),
                    truncate(target.getMessage().getProtocolCode(), 64),
                    truncate(target.getMessage().getMessageType(), 32),
                    truncate(target.getMessage().getTopic(), 512),
                    truncate(target.getMessage().getTraceId(), 64),
                    truncate(entry.getKey(), 128),
                    truncate(resolveMetricName(entry.getKey(), metadata), 128),
                    truncate(resolveValueType(entry.getValue(), metadata), 32),
                    truncate(resolveValueText(entry.getValue()), 1024),
                    resolveLongValue(entry.getValue()),
                    resolveDoubleValue(entry.getValue()),
                    resolveBooleanValue(entry.getValue())
            );
            pointCount++;
        }
        return TelemetryPersistResult.persisted("TDENGINE", "normalized-table", pointCount, 0, 0, pointCount, 0);
    }

    public List<TelemetryLatestPoint> listLatestPoints(Long deviceId) {
        tdengineTelemetrySchemaSupport.ensureTable();
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getJdbcTemplate();
        return jdbcTemplate.query(
                SELECT_LATEST_SQL,
                rs -> {
                    Map<String, TelemetryLatestPoint> latestPointMap = new LinkedHashMap<>();
                    while (rs.next()) {
                        String metricCode = rs.getString("metric_code");
                        if (latestPointMap.containsKey(metricCode)) {
                            continue;
                        }
                        TelemetryLatestPoint point = new TelemetryLatestPoint();
                        Timestamp reportedAt = rs.getTimestamp("reported_at");
                        Timestamp fallbackTs = rs.getTimestamp("ts");
                        Timestamp effectiveTs = reportedAt == null ? fallbackTs : reportedAt;
                        point.setReportedAt(effectiveTs == null ? null : effectiveTs.toLocalDateTime());
                        point.setDeviceCode(rs.getString("device_code"));
                        point.setProductKey(rs.getString("product_key"));
                        point.setMetricCode(metricCode);
                        point.setMetricName(rs.getString("metric_name"));
                        point.setValueType(rs.getString("value_type"));
                        point.setValue(resolvePointValue(
                                rs.getString("value_type"),
                                rs.getString("value_text"),
                                rs.getObject("value_long"),
                                rs.getObject("value_double"),
                                rs.getObject("value_bool")
                        ));
                        point.setTraceId(rs.getString("trace_id"));
                        latestPointMap.put(metricCode, point);
                    }
                    return new ArrayList<>(latestPointMap.values());
                },
                deviceId
        );
    }

    private LocalDateTime resolveReportedAt(DeviceProcessingTarget target) {
        return target.getMessage().getTimestamp() == null ? LocalDateTime.now() : target.getMessage().getTimestamp();
    }

    /**
     * `ts` 是共享 fallback 表的存储主时间轴，必须跨 target 唯一。
     * 真实设备时间继续保存在 `reported_at`，latest 查询优先按 `reported_at` 判定新旧。
     */
    private LocalDateTime resolveRowTimestamp() {
        long candidate = System.currentTimeMillis();
        while (true) {
            long previous = lastStorageEpochMillis.get();
            long next = Math.max(candidate, previous + 1L);
            if (lastStorageEpochMillis.compareAndSet(previous, next)) {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(next), ZoneId.systemDefault());
            }
        }
    }

    private String resolveMetricName(String identifier, DevicePropertyMetadata metadata) {
        if (metadata != null && metadata.getPropertyName() != null && !metadata.getPropertyName().isBlank()) {
            return metadata.getPropertyName();
        }
        return identifier;
    }

    private String resolveValueType(Object value, DevicePropertyMetadata metadata) {
        if (metadata != null && metadata.getDataType() != null && !metadata.getDataType().isBlank()) {
            return metadata.getDataType();
        }
        if (value == null) {
            return "string";
        }
        if (value instanceof Boolean) {
            return "bool";
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return "int";
        }
        if (value instanceof Number) {
            return "double";
        }
        return "string";
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

    private Object resolvePointValue(String valueType,
                                     String valueText,
                                     Object valueLong,
                                     Object valueDouble,
                                     Object valueBool) {
        String normalizedType = valueType == null ? "" : valueType.trim().toLowerCase();
        if ("bool".equals(normalizedType)) {
            return coerceBooleanValue(valueBool != null ? valueBool : valueText);
        }
        if ("int".equals(normalizedType)) {
            return coerceIntegerPointValue(valueLong != null ? valueLong : valueText);
        }
        if ("long".equals(normalizedType)) {
            return coerceLongPointValue(valueLong != null ? valueLong : valueText);
        }
        if ("double".equals(normalizedType) || "float".equals(normalizedType)
                || "decimal".equals(normalizedType) || "number".equals(normalizedType)) {
            return coerceDoublePointValue(valueDouble != null ? valueDouble : valueLong);
        }
        if (valueText == null || valueText.isBlank()) {
            return null;
        }
        return tryReadJson(valueText);
    }

    private Object coerceIntegerPointValue(Object value) {
        if (value instanceof Number number) {
            long longValue = number.longValue();
            if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                return (int) longValue;
            }
            return longValue;
        }
        if (value instanceof String text) {
            try {
                long longValue = Long.parseLong(text.trim());
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    return (int) longValue;
                }
                return longValue;
            } catch (NumberFormatException ignored) {
                return text;
            }
        }
        return value;
    }

    private Object coerceLongPointValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return text;
            }
        }
        return value;
    }

    private Object coerceDoublePointValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return text;
            }
        }
        return value;
    }

    private Object coerceBooleanValue(Object value) {
        if (value instanceof Boolean) {
            return value;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text.trim());
        }
        return value;
    }

    private Object tryReadJson(String valueText) {
        String trimmed = valueText.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                return objectMapper.readValue(trimmed, Object.class);
            } catch (Exception ex) {
                return valueText;
            }
        }
        return valueText;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
