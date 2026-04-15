package com.ghlzm.iot.telemetry.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TDengine 时序表结构支持。
 */
@Component
public class TdengineTelemetrySchemaSupport {

    public static final String TABLE_NAME = "iot_device_telemetry_point";

    private static final Logger log = LoggerFactory.getLogger(TdengineTelemetrySchemaSupport.class);

    private final TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    private final TdengineSchemaManifestSupport schemaManifestSupport;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public TdengineTelemetrySchemaSupport(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider,
                                          TdengineSchemaManifestSupport schemaManifestSupport) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.schemaManifestSupport = schemaManifestSupport;
    }

    public void ensureTable() {
        if (initialized.get()) {
            return;
        }
        synchronized (this) {
            if (initialized.get()) {
                return;
            }
            try {
                jdbcTemplateProvider.getJdbcTemplate().execute(
                        schemaManifestSupport.requireObject(TABLE_NAME).createSql()
                );
                ensureReportedAtColumn();
                initialized.set(true);
            } catch (Exception ex) {
                log.warn("初始化 TDengine 时序表失败，将在运行时重试, error={}", ex.getMessage());
            }
        }
    }

    private void ensureReportedAtColumn() {
        boolean hasReportedAt = Boolean.TRUE.equals(jdbcTemplateProvider.getJdbcTemplate().query(
                "DESCRIBE " + TABLE_NAME,
                rs -> {
                    while (rs.next()) {
                        if ("reported_at".equalsIgnoreCase(rs.getString(1))) {
                            return true;
                        }
                    }
                    return false;
                }
        ));
        if (!hasReportedAt) {
            jdbcTemplateProvider.getJdbcTemplate().execute(
                    "ALTER TABLE " + TABLE_NAME + " ADD COLUMN reported_at TIMESTAMP"
            );
        }
    }
}
