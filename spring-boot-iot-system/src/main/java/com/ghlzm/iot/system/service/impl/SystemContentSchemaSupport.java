package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统内容表结构支持类，负责识别真实库中的站内消息与帮助文档 schema 漂移。
 */
@Slf4j
@Component
public class SystemContentSchemaSupport {

    static final String SCHEMA_HINT =
            "系统内容依赖表缺失，请先按最新 sql/init.sql、sql/init-data.sql 对齐当前初始化基线；若无法重建，可执行 python scripts/run-real-env-schema-sync.py";
    static final String BRIDGE_SCHEMA_HINT =
            "站内消息渠道桥接依赖表缺失，请先按最新 sql/init.sql 对齐当前初始化基线；若无法重建，可执行 python scripts/run-real-env-schema-sync.py";
    static final String BRIDGE_ATTEMPT_SCHEMA_HINT =
            "站内消息桥接尝试明细依赖表缺失，请先按最新 sql/init.sql 对齐当前初始化基线；若无法重建，可执行 python scripts/run-real-env-schema-sync.py";

    private static final String TABLE_EXISTS_SQL =
            "SELECT COUNT(1) FROM information_schema.tables "
                    + "WHERE table_schema = DATABASE() AND table_name = ?";
    private static final String TABLE_COLUMNS_SQL =
            "SELECT column_name FROM information_schema.columns "
                    + "WHERE table_schema = DATABASE() AND table_name = ?";

    private static final String HELP_DOCUMENT_TABLE = "sys_help_document";
    private static final String IN_APP_MESSAGE_TABLE = "sys_in_app_message";
    private static final String IN_APP_MESSAGE_READ_TABLE = "sys_in_app_message_read";
    private static final String IN_APP_MESSAGE_BRIDGE_LOG_TABLE = "sys_in_app_message_bridge_log";
    private static final String IN_APP_MESSAGE_BRIDGE_ATTEMPT_LOG_TABLE = "sys_in_app_message_bridge_attempt_log";

    private static final List<String> HELP_DOCUMENT_REQUIRED_COLUMNS = List.of(
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

    private static final List<String> IN_APP_MESSAGE_REQUIRED_COLUMNS = List.of(
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

    private static final List<String> IN_APP_MESSAGE_READ_REQUIRED_COLUMNS = List.of(
            "id",
            "tenant_id",
            "message_id",
            "user_id",
            "read_time",
            "create_time",
            "update_time");

    private static final List<String> IN_APP_MESSAGE_BRIDGE_LOG_REQUIRED_COLUMNS = List.of(
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

    private static final List<String> IN_APP_MESSAGE_BRIDGE_ATTEMPT_LOG_REQUIRED_COLUMNS = List.of(
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

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, TableSnapshot> snapshotCache = new ConcurrentHashMap<>();

    public SystemContentSchemaSupport(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureHelpDocumentReady() {
        ensureTableReady(HELP_DOCUMENT_TABLE, HELP_DOCUMENT_REQUIRED_COLUMNS);
    }

    public void ensureInAppMessageReady() {
        ensureTableReady(IN_APP_MESSAGE_TABLE, IN_APP_MESSAGE_REQUIRED_COLUMNS);
    }

    public void ensureInAppMessageReadReady() {
        ensureTableReady(IN_APP_MESSAGE_READ_TABLE, IN_APP_MESSAGE_READ_REQUIRED_COLUMNS);
    }

    public void ensureInAppMessageBridgeLogReady() {
        ensureTableReady(IN_APP_MESSAGE_BRIDGE_LOG_TABLE, IN_APP_MESSAGE_BRIDGE_LOG_REQUIRED_COLUMNS, BRIDGE_SCHEMA_HINT);
    }

    public void ensureInAppMessageBridgeAttemptLogReady() {
        ensureTableReady(
                IN_APP_MESSAGE_BRIDGE_ATTEMPT_LOG_TABLE,
                IN_APP_MESSAGE_BRIDGE_ATTEMPT_LOG_REQUIRED_COLUMNS,
                BRIDGE_ATTEMPT_SCHEMA_HINT
        );
    }

    public void refresh() {
        snapshotCache.clear();
    }

    private void ensureTableReady(String tableName, List<String> requiredColumns) {
        ensureTableReady(tableName, requiredColumns, SCHEMA_HINT);
    }

    private void ensureTableReady(String tableName, List<String> requiredColumns, String schemaHint) {
        TableSnapshot cachedSnapshot = snapshotCache.computeIfAbsent(tableName, this::loadSnapshot);
        if (isReady(cachedSnapshot, requiredColumns)) {
            return;
        }

        TableSnapshot refreshedSnapshot = loadSnapshot(tableName);
        snapshotCache.put(tableName, refreshedSnapshot);
        validateSnapshot(refreshedSnapshot, requiredColumns, schemaHint);
    }

    private boolean isReady(TableSnapshot snapshot, List<String> requiredColumns) {
        return snapshot.exists() && snapshot.columns().containsAll(requiredColumns);
    }

    private void validateSnapshot(TableSnapshot snapshot, List<String> requiredColumns, String schemaHint) {
        if (!snapshot.exists()) {
            throw new BizException(schemaHint);
        }
        List<String> missingColumns = requiredColumns.stream()
                .filter(requiredColumn -> !snapshot.columns().contains(requiredColumn))
                .toList();
        if (!missingColumns.isEmpty()) {
            throw new BizException(schemaHint + "，缺少列: " + String.join(", ", missingColumns));
        }
    }

    private TableSnapshot loadSnapshot(String tableName) {
        try {
            Integer tableCount = jdbcTemplate.queryForObject(TABLE_EXISTS_SQL, Integer.class, tableName);
            boolean exists = tableCount != null && tableCount > 0;
            if (!exists) {
                return new TableSnapshot(false, Collections.emptySet());
            }

            List<String> columns = jdbcTemplate.queryForList(TABLE_COLUMNS_SQL, String.class, tableName);
            Set<String> normalizedColumns = new LinkedHashSet<>();
            for (String column : columns) {
                if (column != null && !column.isBlank()) {
                    normalizedColumns.add(column.toLowerCase(Locale.ROOT));
                }
            }
            return new TableSnapshot(true, Collections.unmodifiableSet(normalizedColumns));
        } catch (Exception ex) {
            log.warn("加载系统内容表结构失败，将按缺表处理, table={}, error={}", tableName, ex.getMessage());
            return new TableSnapshot(false, Collections.emptySet());
        }
    }

    private record TableSnapshot(boolean exists, Set<String> columns) {
    }
}
