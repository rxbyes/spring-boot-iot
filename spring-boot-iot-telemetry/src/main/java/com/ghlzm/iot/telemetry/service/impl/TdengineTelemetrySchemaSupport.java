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
    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS iot_device_telemetry_point (
                ts TIMESTAMP,
                tenant_id BIGINT,
                device_id BIGINT,
                device_code BINARY(128),
                product_id BIGINT,
                product_key BINARY(128),
                protocol_code BINARY(64),
                message_type BINARY(32),
                mqtt_topic BINARY(512),
                trace_id BINARY(64),
                metric_code BINARY(128),
                metric_name NCHAR(128),
                value_type BINARY(32),
                value_text NCHAR(1024),
                value_long BIGINT,
                value_double DOUBLE,
                value_bool BOOL
            )
            """;

    private final TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public TdengineTelemetrySchemaSupport(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
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
                jdbcTemplateProvider.getJdbcTemplate().execute(CREATE_TABLE_SQL);
                initialized.set(true);
            } catch (Exception ex) {
                log.warn("初始化 TDengine 时序表失败，将在运行时重试, error={}", ex.getMessage());
            }
        }
    }
}
