package com.ghlzm.iot.framework.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MySqlActiveSchemaBootstrapRunnerTest {

    @Test
    void runShouldCreateMissingActiveObjectsButSkipArchivedOnes() throws Exception {
        SchemaBootstrapProperties properties = new SchemaBootstrapProperties();
        properties.setMysqlEnabled(true);

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            String sql = (String) args[0];
            if (sql.contains("information_schema.TABLES")) {
                String tableName = String.valueOf(args[2]);
                return "iot_device_relation".equals(tableName) ? 0 : 1;
            }
            if (sql.contains("information_schema.COLUMNS") || sql.contains("information_schema.STATISTICS")) {
                return 1;
            }
            return 1;
        }).when(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), ArgumentMatchers.<Object[]>any());

        MySqlActiveSchemaBootstrapRunner runner = new MySqlActiveSchemaBootstrapRunner(
                properties,
                jdbcTemplate,
                new SchemaManifestLoader(
                        new ObjectMapper(),
                        new DefaultResourceLoader(),
                        "classpath:schema/runtime-bootstrap/mysql-active-schema.json",
                        SchemaManifestLoader.TDENGINE_MANIFEST_LOCATION
                )
        );

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate).execute(ArgumentMatchers.contains("CREATE TABLE IF NOT EXISTS iot_device_relation"));
        verify(jdbcTemplate, never()).execute(ArgumentMatchers.contains("risk_point_highway_detail"));
    }

    @Test
    void runShouldAddMissingColumnAndIndexWhenTableAlreadyExists() throws Exception {
        SchemaBootstrapProperties properties = new SchemaBootstrapProperties();
        properties.setMysqlEnabled(true);

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            String sql = (String) args[0];
            if (sql.contains("information_schema.TABLES")) {
                return 1;
            }
            if (sql.contains("information_schema.COLUMNS")) {
                String tableName = String.valueOf(args[2]);
                String columnName = String.valueOf(args[3]);
                return "iot_device_relation".equals(tableName) && "child_product_key".equals(columnName) ? 0 : 1;
            }
            if (sql.contains("information_schema.STATISTICS")) {
                String tableName = String.valueOf(args[2]);
                String indexName = String.valueOf(args[3]);
                return "iot_device_relation".equals(tableName) && "idx_relation_child_code".equals(indexName) ? 0 : 1;
            }
            return 1;
        }).when(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), ArgumentMatchers.<Object[]>any());

        MySqlActiveSchemaBootstrapRunner runner = new MySqlActiveSchemaBootstrapRunner(
                properties,
                jdbcTemplate,
                new SchemaManifestLoader(
                        new ObjectMapper(),
                        new DefaultResourceLoader(),
                        "classpath:schema/runtime-bootstrap/mysql-active-schema.json",
                        SchemaManifestLoader.TDENGINE_MANIFEST_LOCATION
                )
        );

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate).execute(ArgumentMatchers.contains("ALTER TABLE `iot_device_relation` ADD COLUMN `child_product_key`"));
        verify(jdbcTemplate).execute(ArgumentMatchers.contains("ALTER TABLE `iot_device_relation` ADD INDEX `idx_relation_child_code`"));
    }

    @Test
    void runShouldDoNothingWhenMysqlBootstrapDisabled() throws Exception {
        SchemaBootstrapProperties properties = new SchemaBootstrapProperties();
        properties.setMysqlEnabled(false);

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        MySqlActiveSchemaBootstrapRunner runner = new MySqlActiveSchemaBootstrapRunner(
                properties,
                jdbcTemplate,
                new SchemaManifestLoader(
                        new ObjectMapper(),
                        new DefaultResourceLoader(),
                        "classpath:schema/runtime-bootstrap/mysql-active-schema.json",
                        SchemaManifestLoader.TDENGINE_MANIFEST_LOCATION
                )
        );

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Integer.class), ArgumentMatchers.<Object[]>any());
        verify(jdbcTemplate, never()).execute(anyString());
    }
}
