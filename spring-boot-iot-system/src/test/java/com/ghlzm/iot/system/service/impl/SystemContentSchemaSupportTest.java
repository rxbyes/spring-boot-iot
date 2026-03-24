package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemContentSchemaSupportTest {

    private static final String TABLE_EXISTS_SQL =
            "SELECT COUNT(1) FROM information_schema.tables "
                    + "WHERE table_schema = DATABASE() AND table_name = ?";
    private static final String TABLE_COLUMNS_SQL =
            "SELECT column_name FROM information_schema.columns "
                    + "WHERE table_schema = DATABASE() AND table_name = ?";

    private static final List<String> HELP_DOCUMENT_COLUMNS = List.of(
            "id",
            "tenant_id",
            "doc_category",
            "title",
            "summary",
            "content",
            "keywords",
            "related_paths",
            "visible_role_codes",
            "status",
            "sort_no",
            "create_by",
            "create_time",
            "update_by",
            "update_time",
            "deleted");

    private static final List<String> IN_APP_MESSAGE_COLUMNS = List.of(
            "id",
            "tenant_id",
            "message_type",
            "priority",
            "title",
            "summary",
            "content",
            "target_type",
            "target_role_codes",
            "target_user_ids",
            "related_path",
            "source_type",
            "source_id",
            "dedup_key",
            "publish_time",
            "expire_time",
            "status",
            "sort_no",
            "create_by",
            "create_time",
            "update_by",
            "update_time",
            "deleted");

    private static final List<String> IN_APP_MESSAGE_BRIDGE_LOG_COLUMNS = List.of(
            "id",
            "tenant_id",
            "message_id",
            "channel_code",
            "bridge_scene",
            "unread_count",
            "recipient_snapshot",
            "bridge_status",
            "response_status_code",
            "response_body",
            "last_attempt_time",
            "success_time",
            "attempt_count",
            "create_time",
            "update_time");

    private static final List<String> IN_APP_MESSAGE_BRIDGE_ATTEMPT_LOG_COLUMNS = List.of(
            "id",
            "tenant_id",
            "bridge_log_id",
            "message_id",
            "channel_code",
            "bridge_scene",
            "attempt_no",
            "bridge_status",
            "unread_count",
            "recipient_snapshot",
            "response_status_code",
            "response_body",
            "attempt_time",
            "create_time");

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPassWhenHelpDocumentTableIsComplete() {
        SystemContentSchemaSupport support = new SystemContentSchemaSupport(jdbcTemplate);
        when(jdbcTemplate.queryForObject(TABLE_EXISTS_SQL, Integer.class, "sys_help_document")).thenReturn(1);
        when(jdbcTemplate.queryForList(TABLE_COLUMNS_SQL, String.class, "sys_help_document")).thenReturn(HELP_DOCUMENT_COLUMNS);

        assertDoesNotThrow(support::ensureHelpDocumentReady);
    }

    @Test
    void shouldThrowSchemaHintWhenHelpDocumentTableMissing() {
        SystemContentSchemaSupport support = new SystemContentSchemaSupport(jdbcTemplate);
        when(jdbcTemplate.queryForObject(TABLE_EXISTS_SQL, Integer.class, "sys_help_document")).thenReturn(0, 0);

        BizException exception = assertThrows(BizException.class, support::ensureHelpDocumentReady);

        assertEquals(SystemContentSchemaSupport.SCHEMA_HINT, exception.getMessage());
    }

    @Test
    void shouldThrowMissingColumnsHintWhenInAppMessageTableIncomplete() {
        SystemContentSchemaSupport support = new SystemContentSchemaSupport(jdbcTemplate);
        when(jdbcTemplate.queryForObject(TABLE_EXISTS_SQL, Integer.class, "sys_in_app_message")).thenReturn(1, 1);
        when(jdbcTemplate.queryForList(TABLE_COLUMNS_SQL, String.class, "sys_in_app_message")).thenReturn(
                IN_APP_MESSAGE_COLUMNS.stream()
                        .filter(column -> !"source_id".equals(column))
                        .filter(column -> !"dedup_key".equals(column))
                        .toList(),
                IN_APP_MESSAGE_COLUMNS.stream()
                        .filter(column -> !"source_id".equals(column))
                        .filter(column -> !"dedup_key".equals(column))
                        .toList()
        );

        BizException exception = assertThrows(BizException.class, support::ensureInAppMessageReady);

        assertEquals(SystemContentSchemaSupport.SCHEMA_HINT + "，缺少列: source_id, dedup_key", exception.getMessage());
    }

    @Test
    void shouldRefreshCachedSnapshotBeforeDeclaringSchemaMissing() {
        SystemContentSchemaSupport support = new SystemContentSchemaSupport(jdbcTemplate);
        when(jdbcTemplate.queryForObject(TABLE_EXISTS_SQL, Integer.class, "sys_help_document")).thenReturn(0, 0, 1);
        when(jdbcTemplate.queryForList(TABLE_COLUMNS_SQL, String.class, "sys_help_document")).thenReturn(HELP_DOCUMENT_COLUMNS);

        assertThrows(BizException.class, support::ensureHelpDocumentReady);
        assertDoesNotThrow(support::ensureHelpDocumentReady);
    }

    @Test
    void shouldPassWhenBridgeLogTableIsComplete() {
        SystemContentSchemaSupport support = new SystemContentSchemaSupport(jdbcTemplate);
        when(jdbcTemplate.queryForObject(TABLE_EXISTS_SQL, Integer.class, "sys_in_app_message_bridge_log")).thenReturn(1);
        when(jdbcTemplate.queryForList(TABLE_COLUMNS_SQL, String.class, "sys_in_app_message_bridge_log"))
                .thenReturn(IN_APP_MESSAGE_BRIDGE_LOG_COLUMNS);

        assertDoesNotThrow(support::ensureInAppMessageBridgeLogReady);
    }

    @Test
    void shouldPassWhenBridgeAttemptLogTableIsComplete() {
        SystemContentSchemaSupport support = new SystemContentSchemaSupport(jdbcTemplate);
        when(jdbcTemplate.queryForObject(TABLE_EXISTS_SQL, Integer.class, "sys_in_app_message_bridge_attempt_log")).thenReturn(1);
        when(jdbcTemplate.queryForList(TABLE_COLUMNS_SQL, String.class, "sys_in_app_message_bridge_attempt_log"))
                .thenReturn(IN_APP_MESSAGE_BRIDGE_ATTEMPT_LOG_COLUMNS);

        assertDoesNotThrow(support::ensureInAppMessageBridgeAttemptLogReady);
    }
}
