-- 系统治理分页性能索引补齐（2026-03-17）
-- 目标：为系统治理分页、树表懒加载和系统日志查询补齐真实环境增量索引
SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_user'
          AND INDEX_NAME = 'idx_user_deleted_status_create_time'
    ),
    'SELECT ''skip idx_user_deleted_status_create_time''',
    'CREATE INDEX idx_user_deleted_status_create_time ON sys_user (deleted, status, create_time, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_role'
          AND INDEX_NAME = 'idx_role_deleted_status_create_time'
    ),
    'SELECT ''skip idx_role_deleted_status_create_time''',
    'CREATE INDEX idx_role_deleted_status_create_time ON sys_role (deleted, status, create_time, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_menu'
          AND INDEX_NAME = 'idx_menu_deleted_parent_sort'
    ),
    'SELECT ''skip idx_menu_deleted_parent_sort''',
    'CREATE INDEX idx_menu_deleted_parent_sort ON sys_menu (deleted, parent_id, sort, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_menu'
          AND INDEX_NAME = 'idx_menu_deleted_status_sort'
    ),
    'SELECT ''skip idx_menu_deleted_status_sort''',
    'CREATE INDEX idx_menu_deleted_status_sort ON sys_menu (deleted, status, sort, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_organization'
          AND INDEX_NAME = 'idx_org_deleted_parent_sort'
    ),
    'SELECT ''skip idx_org_deleted_parent_sort''',
    'CREATE INDEX idx_org_deleted_parent_sort ON sys_organization (deleted, parent_id, sort_no, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_organization'
          AND INDEX_NAME = 'idx_org_deleted_status_sort'
    ),
    'SELECT ''skip idx_org_deleted_status_sort''',
    'CREATE INDEX idx_org_deleted_status_sort ON sys_organization (deleted, status, sort_no, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_region'
          AND INDEX_NAME = 'idx_region_deleted_parent_sort'
    ),
    'SELECT ''skip idx_region_deleted_parent_sort''',
    'CREATE INDEX idx_region_deleted_parent_sort ON sys_region (deleted, parent_id, sort_no, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_region'
          AND INDEX_NAME = 'idx_region_deleted_type_sort'
    ),
    'SELECT ''skip idx_region_deleted_type_sort''',
    'CREATE INDEX idx_region_deleted_type_sort ON sys_region (deleted, region_type, sort_no, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_dict'
          AND INDEX_NAME = 'idx_dict_deleted_sort'
    ),
    'SELECT ''skip idx_dict_deleted_sort''',
    'CREATE INDEX idx_dict_deleted_sort ON sys_dict (deleted, sort_no, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_dict'
          AND INDEX_NAME = 'idx_dict_deleted_type_sort'
    ),
    'SELECT ''skip idx_dict_deleted_type_sort''',
    'CREATE INDEX idx_dict_deleted_type_sort ON sys_dict (deleted, dict_type, sort_no, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_notification_channel'
          AND INDEX_NAME = 'idx_channel_deleted_sort'
    ),
    'SELECT ''skip idx_channel_deleted_sort''',
    'CREATE INDEX idx_channel_deleted_sort ON sys_notification_channel (deleted, sort_no, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_notification_channel'
          AND INDEX_NAME = 'idx_channel_deleted_type_sort'
    ),
    'SELECT ''skip idx_channel_deleted_type_sort''',
    'CREATE INDEX idx_channel_deleted_type_sort ON sys_notification_channel (deleted, channel_type, sort_no, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_audit_log'
          AND INDEX_NAME = 'idx_trace_id'
    ),
    'SELECT ''skip idx_trace_id''',
    'CREATE INDEX idx_trace_id ON sys_audit_log (trace_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_audit_log'
          AND INDEX_NAME = 'idx_device_code'
    ),
    'SELECT ''skip idx_device_code''',
    'CREATE INDEX idx_device_code ON sys_audit_log (device_code)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_audit_log'
          AND INDEX_NAME = 'idx_audit_deleted_operation_time'
    ),
    'SELECT ''skip idx_audit_deleted_operation_time''',
    'CREATE INDEX idx_audit_deleted_operation_time ON sys_audit_log (deleted, operation_time, create_time, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_audit_log'
          AND INDEX_NAME = 'idx_audit_deleted_type_time'
    ),
    'SELECT ''skip idx_audit_deleted_type_time''',
    'CREATE INDEX idx_audit_deleted_type_time ON sys_audit_log (deleted, operation_type, operation_time, create_time, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_audit_log'
          AND INDEX_NAME = 'idx_audit_deleted_request_method_time'
    ),
    'SELECT ''skip idx_audit_deleted_request_method_time''',
    'CREATE INDEX idx_audit_deleted_request_method_time ON sys_audit_log (deleted, request_method, operation_time, create_time, id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
