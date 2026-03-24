USE rm_iot;

SET NAMES utf8mb4;

SET @has_dedup_key := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_in_app_message'
      AND column_name = 'dedup_key'
);
SET @ddl := IF(
    @has_dedup_key = 0,
    'ALTER TABLE sys_in_app_message ADD COLUMN dedup_key VARCHAR(32) DEFAULT NULL COMMENT ''去重键'' AFTER source_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_source_index := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_in_app_message'
      AND index_name = 'idx_in_app_message_source'
);
SET @ddl := IF(
    @has_source_index = 0,
    'ALTER TABLE sys_in_app_message ADD INDEX idx_in_app_message_source (source_type, source_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_dedup_index := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_in_app_message'
      AND index_name = 'idx_in_app_message_tenant_dedup'
);
SET @ddl := IF(
    @has_dedup_index = 0,
    'ALTER TABLE sys_in_app_message ADD INDEX idx_in_app_message_tenant_dedup (tenant_id, dedup_key, deleted)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_in_app_message
SET source_type = 'manual'
WHERE source_type IN ('system_maintenance', 'daily_report');

UPDATE sys_in_app_message
SET source_type = 'governance'
WHERE source_type = 'governance_task';

UPDATE sys_in_app_message
SET dedup_key = MD5(
    CONCAT_WS('|',
        LOWER(TRIM(source_type)),
        TRIM(source_id),
        CASE
            WHEN LOWER(TRIM(target_type)) = 'all' THEN 'all'
            WHEN LOWER(TRIM(target_type)) = 'role' THEN CONCAT('role:', UPPER(REPLACE(IFNULL(target_role_codes, ''), ' ', '')))
            WHEN LOWER(TRIM(target_type)) = 'user' THEN CONCAT('user:', REPLACE(IFNULL(target_user_ids, ''), ' ', ''))
            ELSE ''
        END,
        LOWER(TRIM(message_type))
    )
)
WHERE deleted = 0
  AND source_type IS NOT NULL
  AND TRIM(source_type) <> ''
  AND source_id IS NOT NULL
  AND TRIM(source_id) <> ''
  AND message_type IS NOT NULL
  AND TRIM(message_type) <> ''
  AND target_type IS NOT NULL
  AND TRIM(target_type) <> ''
  AND (dedup_key IS NULL OR TRIM(dedup_key) = '');
