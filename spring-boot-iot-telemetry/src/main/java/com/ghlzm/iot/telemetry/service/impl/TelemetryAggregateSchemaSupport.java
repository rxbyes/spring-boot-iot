package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Telemetry 小时聚合 schema 支持。
 * 只负责校验 stable 已存在，并在运行时派生 child table。
 */
@Component
public class TelemetryAggregateSchemaSupport {

    private final TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    private final TelemetryAggregateTableNamingStrategy tableNamingStrategy;
    private final TdengineSchemaManifestSupport schemaManifestSupport;
    private final AtomicBoolean stableVerified = new AtomicBoolean(false);
    private final Set<String> ensuredChildTables = ConcurrentHashMap.newKeySet();

    public TelemetryAggregateSchemaSupport(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider,
                                           TelemetryAggregateTableNamingStrategy tableNamingStrategy,
                                           TdengineSchemaManifestSupport schemaManifestSupport) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.tableNamingStrategy = tableNamingStrategy;
        this.schemaManifestSupport = schemaManifestSupport;
    }

    public void ensureChildTable(TelemetryV2Point point) {
        if (point == null) {
            return;
        }
        ensureStableExists();
        String childTable = tableNamingStrategy.resolveChildTableName(point.getTenantId(), point.getDeviceId());
        if (!ensuredChildTables.add(childTable)) {
            return;
        }
        try {
            jdbcTemplateProvider.getJdbcTemplate().execute(buildChildTableSql(point, childTable));
        } catch (Exception ex) {
            ensuredChildTables.remove(childTable);
            throw ex;
        }
    }

    private void ensureStableExists() {
        if (stableVerified.get()) {
            return;
        }
        synchronized (this) {
            if (stableVerified.get()) {
                return;
            }
            String stableName = schemaManifestSupport.requireObject(tableNamingStrategy.resolveStableName()).name();
            boolean stableExists = Boolean.TRUE.equals(jdbcTemplateProvider.getJdbcTemplate().query(
                    "SHOW STABLES LIKE '" + stableName + "'",
                    (ResultSetExtractor<Boolean>) rs -> rs.next()
            ));
            if (!stableExists) {
                throw new IllegalStateException(
                        "telemetry aggregate stable missing and requires manual bootstrap: " + stableName
                );
            }
            stableVerified.set(true);
        }
    }

    private String buildChildTableSql(TelemetryV2Point point, String childTable) {
        return "CREATE TABLE IF NOT EXISTS " + childTable
                + " USING " + tableNamingStrategy.resolveStableName()
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
