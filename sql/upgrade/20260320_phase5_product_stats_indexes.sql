-- 产品定义中心统计聚合索引补齐（2026-03-20）
-- 目标：为 `/products` 分页场景下的关联设备数、在线数和最近上报时间聚合查询补齐真实环境增量索引
SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'iot_device'
          AND INDEX_NAME = 'idx_device_deleted_product_stats'
    ),
    'SELECT ''skip idx_device_deleted_product_stats''',
    'CREATE INDEX idx_device_deleted_product_stats ON iot_device (deleted, product_id, last_report_time, online_status)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
