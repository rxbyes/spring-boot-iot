package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Telemetry v2 raw schema 支持。
 */
@Component
public class TelemetryV2SchemaSupport {

    private static final Logger log = LoggerFactory.getLogger(TelemetryV2SchemaSupport.class);

    private final TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    private final TelemetryV2TableNamingStrategy tableNamingStrategy;
    private final AtomicBoolean stablesInitialized = new AtomicBoolean(false);
    private final Set<String> ensuredChildTables = ConcurrentHashMap.newKeySet();

    public TelemetryV2SchemaSupport(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider,
                                    TelemetryV2TableNamingStrategy tableNamingStrategy) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.tableNamingStrategy = tableNamingStrategy;
    }

    public void ensureTables() {
        if (stablesInitialized.get()) {
            return;
        }
        synchronized (this) {
            if (stablesInitialized.get()) {
                return;
            }
            JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getJdbcTemplate();
            try {
                for (TelemetryStreamKind streamKind : TelemetryStreamKind.values()) {
                    jdbcTemplate.execute(buildStableSql(streamKind));
                }
                stablesInitialized.set(true);
            } catch (Exception ex) {
                log.warn("初始化 telemetry v2 stable 失败，将在运行时重试, error={}", ex.getMessage());
            }
        }
    }

    public void ensureChildTable(TelemetryV2Point point) {
        ensureTables();
        if (point == null || point.getStreamKind() == null) {
            return;
        }
        String childTable = tableNamingStrategy.resolveChildTableName(
                point.getStreamKind(),
                point.getTenantId(),
                point.getDeviceId()
        );
        if (!ensuredChildTables.add(childTable)) {
            return;
        }
        try {
            jdbcTemplateProvider.getJdbcTemplate().execute(buildChildTableSql(point, childTable));
        } catch (Exception ex) {
            ensuredChildTables.remove(childTable);
            log.warn("初始化 telemetry v2 子表失败, table={}, error={}", childTable, ex.getMessage());
        }
    }

    private String buildStableSql(TelemetryStreamKind streamKind) {
        return "CREATE STABLE IF NOT EXISTS " + tableNamingStrategy.resolveStableName(streamKind) + " ("
                + "ts TIMESTAMP,"
                + " metric_id BINARY(128),"
                + " reported_at TIMESTAMP,"
                + " ingested_at TIMESTAMP,"
                + " value_double DOUBLE,"
                + " value_long BIGINT,"
                + " value_bool BOOL,"
                + " value_text NCHAR(1024),"
                + " quality_code BINARY(32),"
                + " alarm_flag BOOL,"
                + " trace_id BINARY(64),"
                + " session_id BINARY(64),"
                + " source_message_type BINARY(32)"
                + ") TAGS ("
                + "tenant_id BIGINT,"
                + " device_id BIGINT,"
                + " product_id BIGINT,"
                + " sensor_group BINARY(64),"
                + " location_code BINARY(64),"
                + " risk_point_id BIGINT"
                + ")";
    }

    private String buildChildTableSql(TelemetryV2Point point, String childTable) {
        return "CREATE TABLE IF NOT EXISTS " + childTable
                + " USING " + tableNamingStrategy.resolveStableName(point.getStreamKind())
                + " TAGS ("
                + safeLong(point.getTenantId()) + ", "
                + safeLong(point.getDeviceId()) + ", "
                + safeLong(point.getProductId()) + ", "
                + quote(point.getSensorGroup()) + ", "
                + quote(point.getLocationCode()) + ", "
                + nullableLong(point.getRiskPointId())
                + ")";
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String nullableLong(Long value) {
        return value == null ? "NULL" : String.valueOf(value);
    }

    private String quote(String value) {
        return "'" + (value == null ? "" : value.replace("'", "''")) + "'";
    }
}
