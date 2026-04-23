package com.ghlzm.iot.framework.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动期幂等补齐 active MySQL 结构对象。
 */
@Component
public class MySqlActiveSchemaBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MySqlActiveSchemaBootstrapRunner.class);

    private final SchemaBootstrapProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final SchemaManifestLoader manifestLoader;

    public MySqlActiveSchemaBootstrapRunner(SchemaBootstrapProperties properties,
                                            JdbcTemplate jdbcTemplate,
                                            SchemaManifestLoader manifestLoader) {
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
        this.manifestLoader = manifestLoader;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isMysqlEnabled()) {
            return;
        }
        try {
            SchemaManifestLoader.MySqlRuntimeManifest manifest = manifestLoader.loadMySql();
            for (SchemaManifestLoader.MySqlTableManifest table : manifest.tables()) {
                ensureTable(table);
            }
            for (SchemaManifestLoader.MySqlViewManifest view : manifest.views()) {
                ensureView(view);
            }
        } catch (Exception ex) {
            handleFailure("MySQL active schema bootstrap failed", ex);
        }
    }

    private void ensureTable(SchemaManifestLoader.MySqlTableManifest table) {
        try {
            if (!tableExists(table.name())) {
                jdbcTemplate.execute(table.createSql());
                return;
            }
            for (SchemaManifestLoader.MySqlColumnManifest column : table.columns()) {
                if (!columnExists(table.name(), column.name())) {
                    jdbcTemplate.execute(column.addSql());
                }
            }
            for (SchemaManifestLoader.MySqlIndexManifest index : table.indexes()) {
                if (!indexExists(table.name(), index.name())) {
                    jdbcTemplate.execute(index.addSql());
                }
            }
        } catch (Exception ex) {
            handleFailure("MySQL active schema bootstrap failed for table " + table.name(), ex);
        }
    }

    private void ensureView(SchemaManifestLoader.MySqlViewManifest view) {
        try {
            jdbcTemplate.execute(view.createOrReplaceSql());
        } catch (Exception ex) {
            handleFailure("MySQL active schema bootstrap failed for view " + view.name(), ex);
        }
    }

    private boolean tableExists(String tableName) {
        return exists(
                """
                SELECT COUNT(1)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND TABLE_TYPE = 'BASE TABLE'
                """,
                tableName
        );
    }

    private boolean columnExists(String tableName, String columnName) {
        return exists(
                """
                SELECT COUNT(1)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                tableName,
                columnName
        );
    }

    private boolean indexExists(String tableName, String indexName) {
        return exists(
                """
                SELECT COUNT(1)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """,
                tableName,
                indexName
        );
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private void handleFailure(String message, Exception ex) {
        if (properties.isFailFast()) {
            throw ex instanceof RuntimeException runtimeException
                    ? runtimeException
                    : new IllegalStateException(message, ex);
        }
        log.warn("{}: {}", message, ex.getMessage());
    }
}
