package com.ghlzm.iot.telemetry.service.impl;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 读取 TDengine legacy stable 结构并做运行期缓存。
 */
@Component
public class LegacyTdengineSchemaInspector {

    private final TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    private final ConcurrentMap<String, LegacyTdengineTableSchema> schemaCache = new ConcurrentHashMap<>();

    public LegacyTdengineSchemaInspector(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public LegacyTdengineTableSchema describeStable(String stable) {
        String normalizedStable = normalizeIdentifier(stable);
        if (normalizedStable == null) {
            throw new IllegalArgumentException("非法 TDengine stable 名称: " + stable);
        }
        return schemaCache.computeIfAbsent(normalizedStable, this::loadSchema);
    }

    private LegacyTdengineTableSchema loadSchema(String stable) {
        Map<String, String> columnTypes = jdbcTemplateProvider.getJdbcTemplate().query(
                "DESCRIBE " + stable,
                rs -> {
                    Map<String, String> columns = new LinkedHashMap<>();
                    while (rs.next()) {
                        String columnName = normalizeIdentifier(rs.getString(1));
                        if (columnName == null) {
                            continue;
                        }
                        columns.put(columnName.toLowerCase(Locale.ROOT), normalizeType(rs.getString(2)));
                    }
                    return columns;
                }
        );
        if (columnTypes == null || columnTypes.isEmpty()) {
            throw new IllegalStateException("TDengine stable 结构为空: " + stable);
        }
        return new LegacyTdengineTableSchema(stable, columnTypes);
    }

    private String normalizeType(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        int bracketIndex = normalized.indexOf('(');
        return bracketIndex >= 0 ? normalized.substring(0, bracketIndex) : normalized;
    }

    private String normalizeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.matches("[A-Za-z_][A-Za-z0-9_]*") ? normalized : null;
    }

    public static final class LegacyTdengineTableSchema {
        private final String stable;
        private final Map<String, String> columnTypes;

        private LegacyTdengineTableSchema(String stable, Map<String, String> columnTypes) {
            this.stable = stable;
            this.columnTypes = Collections.unmodifiableMap(new LinkedHashMap<>(columnTypes));
        }

        public String getStable() {
            return stable;
        }

        public boolean hasColumn(String column) {
            return column != null && columnTypes.containsKey(column.trim().toLowerCase(Locale.ROOT));
        }

        public String getColumnType(String column) {
            return column == null ? null : columnTypes.get(column.trim().toLowerCase(Locale.ROOT));
        }
    }
}
