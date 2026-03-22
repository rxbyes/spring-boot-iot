SET @contract_snapshot_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'iot_device_access_error_log'
      AND COLUMN_NAME = 'contract_snapshot'
);

SET @contract_snapshot_ddl = IF(
    @contract_snapshot_exists = 0,
    'ALTER TABLE iot_device_access_error_log ADD COLUMN contract_snapshot LONGTEXT DEFAULT NULL COMMENT ''设备契约快照'' AFTER error_message',
    'SELECT ''iot_device_access_error_log.contract_snapshot already exists'''
);

PREPARE contract_snapshot_stmt FROM @contract_snapshot_ddl;
EXECUTE contract_snapshot_stmt;
DEALLOCATE PREPARE contract_snapshot_stmt;
