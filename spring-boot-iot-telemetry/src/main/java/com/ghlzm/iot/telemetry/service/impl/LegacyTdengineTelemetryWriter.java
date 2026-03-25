package com.ghlzm.iot.telemetry.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 legacy stable 结构写入 TDengine。
 */
@Service
public class LegacyTdengineTelemetryWriter {

    private static final Logger log = LoggerFactory.getLogger(LegacyTdengineTelemetryWriter.class);

    private final TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    private final LegacyTdengineSchemaInspector schemaInspector;
    private final LegacyTdengineDeviceMetadataResolver deviceMetadataResolver;
    private final Set<String> ensuredSubTables = ConcurrentHashMap.newKeySet();

    public LegacyTdengineTelemetryWriter(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider,
                                         LegacyTdengineSchemaInspector schemaInspector,
                                         LegacyTdengineDeviceMetadataResolver deviceMetadataResolver) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.schemaInspector = schemaInspector;
        this.deviceMetadataResolver = deviceMetadataResolver;
    }

    public LegacyTdenginePersistOutcome persist(DeviceProcessingTarget target,
                                                Map<String, Object> properties,
                                                Map<String, TelemetryMetricMapping> mappingMap) {
        if (properties == null || properties.isEmpty()) {
            return LegacyTdenginePersistOutcome.empty();
        }
        BuildWritePlanResult buildResult = buildWritePlans(properties, mappingMap);
        if (buildResult.isEmpty()) {
            return LegacyTdenginePersistOutcome.empty();
        }
        if (buildResult.plans().isEmpty()) {
            return LegacyTdenginePersistOutcome.of(0, Set.of(), buildResult.unmappedMetricReasons());
        }

        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getJdbcTemplate();
        LocalDateTime reportedAt = resolveReportedAt(target);
        LegacyTdengineDeviceMetadataResolver.LegacyTdengineDeviceMetadata deviceMetadata =
                deviceMetadataResolver.resolve(target.getDevice());

        Set<String> persistedMetricCodes = new LinkedHashSet<>();
        for (StableWritePlan plan : buildResult.plans().values()) {
            String subTable = deviceMetadataResolver.resolveSubTableName(deviceMetadata, plan.getStable());
            ensureSubTable(jdbcTemplate, subTable, plan.getStable(), deviceMetadata);
            jdbcTemplate.update(plan.buildInsertSql(subTable), plan.buildInsertArgs(reportedAt));
            persistedMetricCodes.addAll(plan.getMetricCodes());
        }
        return LegacyTdenginePersistOutcome.of(
                buildResult.plans().size(),
                persistedMetricCodes,
                buildResult.unmappedMetricReasons()
        );
    }

    private BuildWritePlanResult buildWritePlans(Map<String, Object> properties,
                                                 Map<String, TelemetryMetricMapping> mappingMap) {
        Map<String, StableWritePlan> plans = new LinkedHashMap<>();
        Map<String, String> unmappedMetricReasons = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            TelemetryMetricMapping mapping = mappingMap == null ? null : mappingMap.get(entry.getKey());
            if (mapping == null) {
                unmappedMetricReasons.put(entry.getKey(), TelemetryMetricMapping.REASON_PROPERTY_METADATA_MISSING);
                continue;
            }
            if (!mapping.isLegacyMapped()) {
                unmappedMetricReasons.put(entry.getKey(), resolveFallbackReason(mapping));
                continue;
            }
            LegacyTdengineSchemaInspector.LegacyTdengineTableSchema schema =
                    schemaInspector.describeStable(mapping.getStable());
            if (!schema.hasColumn(mapping.getColumn())) {
                log.warn("TDengine legacy 映射列不存在, stable={}, column={}, metricCode={}",
                        mapping.getStable(), mapping.getColumn(), entry.getKey());
                unmappedMetricReasons.put(entry.getKey(), TelemetryMetricMapping.REASON_SCHEMA_COLUMN_MISSING);
                continue;
            }
            plans.computeIfAbsent(mapping.getStable(), stable -> new StableWritePlan(stable, schema))
                    .put(mapping.getColumn(), entry.getKey(),
                            convertColumnValue(schema.getColumnType(mapping.getColumn()), entry.getValue()));
        }
        return new BuildWritePlanResult(plans, unmappedMetricReasons);
    }

    private String resolveFallbackReason(TelemetryMetricMapping mapping) {
        String reason = mapping.primaryFallbackReason();
        return reason == null ? TelemetryMetricMapping.REASON_MAPPING_NOT_CONFIGURED : reason;
    }

    private void ensureSubTable(JdbcTemplate jdbcTemplate,
                                String subTable,
                                String stable,
                                LegacyTdengineDeviceMetadataResolver.LegacyTdengineDeviceMetadata deviceMetadata) {
        if (ensuredSubTables.contains(subTable)) {
            return;
        }
        synchronized (ensuredSubTables) {
            if (ensuredSubTables.contains(subTable)) {
                return;
            }
            String sql = "CREATE TABLE IF NOT EXISTS " + subTable
                    + " USING " + stable
                    + " TAGS ('" + escapeTag(deviceMetadata.getLocation()) + "', '" + escapeTag(deviceMetadata.getDeviceSn()) + "')";
            jdbcTemplate.execute(sql);
            ensuredSubTables.add(subTable);
        }
    }

    private LocalDateTime resolveReportedAt(DeviceProcessingTarget target) {
        return target != null && target.getMessage() != null && target.getMessage().getTimestamp() != null
                ? target.getMessage().getTimestamp()
                : LocalDateTime.now();
    }

    private String escapeTag(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private Object convertColumnValue(String columnType, Object value) {
        if (value == null) {
            return null;
        }
        String normalizedType = columnType == null ? "" : columnType.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedType) {
            case "NCHAR", "BINARY", "VARCHAR", "CHAR" -> String.valueOf(value);
            case "DOUBLE", "FLOAT" -> toDouble(value);
            case "BIGINT" -> toLong(value);
            case "INT", "INTEGER", "SMALLINT", "TINYINT" -> toInteger(value);
            case "BOOL", "BOOLEAN" -> toBoolean(value);
            case "TIMESTAMP" -> toTimestamp(value);
            default -> String.valueOf(value);
        };
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value).trim());
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value).trim());
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value).trim());
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return "1".equals(normalized)
                || "true".equals(normalized)
                || "yes".equals(normalized)
                || "ok".equals(normalized)
                || "success".equals(normalized);
    }

    private Timestamp toTimestamp(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return Timestamp.valueOf(localDateTime);
        }
        if (value instanceof Instant instant) {
            return Timestamp.from(instant);
        }
        if (value instanceof Number number) {
            return Timestamp.from(Instant.ofEpochMilli(number.longValue()));
        }
        try {
            return Timestamp.valueOf(LocalDateTime.parse(String.valueOf(value).trim()));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("无法转换为 TIMESTAMP: " + value, ex);
        }
    }

    @Getter
    public static final class LegacyTdenginePersistOutcome {
        private final int stableCount;
        private final Set<String> persistedMetricCodes;
        private final Map<String, String> unmappedMetricReasons;

        private LegacyTdenginePersistOutcome(int stableCount,
                                             Set<String> persistedMetricCodes,
                                             Map<String, String> unmappedMetricReasons) {
            this.stableCount = stableCount;
            this.persistedMetricCodes = Set.copyOf(persistedMetricCodes);
            this.unmappedMetricReasons = Map.copyOf(unmappedMetricReasons);
        }

        public static LegacyTdenginePersistOutcome empty() {
            return new LegacyTdenginePersistOutcome(0, Set.of(), Map.of());
        }

        public static LegacyTdenginePersistOutcome of(int stableCount,
                                                      Set<String> persistedMetricCodes,
                                                      Map<String, String> unmappedMetricReasons) {
            return new LegacyTdenginePersistOutcome(stableCount, persistedMetricCodes, unmappedMetricReasons);
        }

        public int getMetricCount() {
            return persistedMetricCodes.size();
        }

        public int getLegacyMappedMetricCount() {
            return persistedMetricCodes.size();
        }

        public int getLegacyUnmappedMetricCount() {
            return unmappedMetricReasons.size();
        }
    }

    private static final class StableWritePlan {
        private final String stable;
        private final LegacyTdengineSchemaInspector.LegacyTdengineTableSchema schema;
        private final Map<String, Object> columnValues = new LinkedHashMap<>();
        private final Set<String> metricCodes = new LinkedHashSet<>();

        private StableWritePlan(String stable, LegacyTdengineSchemaInspector.LegacyTdengineTableSchema schema) {
            this.stable = stable;
            this.schema = schema;
        }

        public String getStable() {
            return stable;
        }

        public Set<String> getMetricCodes() {
            return metricCodes;
        }

        public void put(String column, String metricCode, Object value) {
            if (columnValues.containsKey(column)) {
                log.warn("TDengine legacy stable 存在重复列映射，后值覆盖前值, stable={}, column={}, metricCode={}",
                        stable, column, metricCode);
            }
            columnValues.put(column, value);
            metricCodes.add(metricCode);
        }

        public String buildInsertSql(String subTable) {
            List<String> columns = new ArrayList<>();
            columns.add("ts");
            if (schema.hasColumn("rd")) {
                columns.add("rd");
            }
            if (schema.hasColumn("id")) {
                columns.add("id");
            }
            columns.addAll(columnValues.keySet());
            String placeholders = String.join(", ", columns.stream().map(column -> "?").toList());
            return "INSERT INTO " + subTable + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";
        }

        public Object[] buildInsertArgs(LocalDateTime reportedAt) {
            List<Object> args = new ArrayList<>();
            args.add(Timestamp.valueOf(reportedAt));
            if (schema.hasColumn("rd")) {
                args.add(Timestamp.valueOf(reportedAt));
            }
            if (schema.hasColumn("id")) {
                args.add(IdWorker.getId());
            }
            args.addAll(columnValues.values());
            return args.toArray();
        }
    }

    private record BuildWritePlanResult(Map<String, StableWritePlan> plans, Map<String, String> unmappedMetricReasons) {

        private boolean isEmpty() {
            return plans.isEmpty() && unmappedMetricReasons.isEmpty();
        }
    }
}
