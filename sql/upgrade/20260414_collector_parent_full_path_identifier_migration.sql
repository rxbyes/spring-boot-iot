-- 2026-04-14
-- 采集器父产品正式字段统一切换为全路径标识符真相。
-- 适用范围：采集器 / 采集型遥测终端同类产品。

DROP TEMPORARY TABLE IF EXISTS tmp_collector_identifier_mapping;

CREATE TEMPORARY TABLE tmp_collector_identifier_mapping AS
SELECT p.id AS product_id,
       mapping.legacy_identifier,
       mapping.target_identifier
FROM iot_product p
JOIN (
    SELECT 'ext_power_volt' AS legacy_identifier, 'S1_ZT_1.ext_power_volt' AS target_identifier
    UNION ALL SELECT 'solar_volt', 'S1_ZT_1.solar_volt'
    UNION ALL SELECT 'battery_dump_energy', 'S1_ZT_1.battery_dump_energy'
    UNION ALL SELECT 'battery_volt', 'S1_ZT_1.battery_volt'
    UNION ALL SELECT 'supply_power', 'S1_ZT_1.supply_power'
    UNION ALL SELECT 'consume_power', 'S1_ZT_1.consume_power'
    UNION ALL SELECT 'temp', 'S1_ZT_1.temp'
    UNION ALL SELECT 'humidity', 'S1_ZT_1.humidity'
    UNION ALL SELECT 'temp_out', 'S1_ZT_1.temp_out'
    UNION ALL SELECT 'humidity_out', 'S1_ZT_1.humidity_out'
    UNION ALL SELECT 'lon', 'S1_ZT_1.lon'
    UNION ALL SELECT 'lat', 'S1_ZT_1.lat'
    UNION ALL SELECT 'signal_4g', 'S1_ZT_1.signal_4g'
    UNION ALL SELECT 'signal_NB', 'S1_ZT_1.signal_NB'
    UNION ALL SELECT 'singal_NB', 'S1_ZT_1.signal_NB'
    UNION ALL SELECT 'signal_db', 'S1_ZT_1.signal_db'
    UNION ALL SELECT 'singal_db', 'S1_ZT_1.signal_db'
    UNION ALL SELECT 'sw_version', 'S1_ZT_1.sw_version'
) mapping
WHERE p.deleted = 0
  AND (
      LOWER(COALESCE(p.product_key, '')) LIKE '%collector%'
      OR LOWER(COALESCE(p.product_key, '')) LIKE '%collect-rtu%'
      OR p.product_name LIKE '%采集器%'
      OR (p.product_name LIKE '%采集%' AND p.product_name LIKE '%终端%')
  );

-- 先删除已经存在目标正式字段时的旧短标识重复行，再更新剩余正式字段。
DELETE legacy_model
FROM iot_product_model legacy_model
JOIN tmp_collector_identifier_mapping mapping
  ON mapping.product_id = legacy_model.product_id
 AND mapping.legacy_identifier = legacy_model.identifier
JOIN iot_product_model target_model
  ON target_model.product_id = legacy_model.product_id
 AND target_model.model_type = legacy_model.model_type
 AND target_model.identifier = mapping.target_identifier
WHERE legacy_model.deleted = 0
  AND target_model.deleted = 0;

UPDATE iot_product_model product_model
JOIN tmp_collector_identifier_mapping mapping
  ON mapping.product_id = product_model.product_id
 AND mapping.legacy_identifier = product_model.identifier
SET product_model.identifier = mapping.target_identifier,
    product_model.update_time = NOW()
WHERE product_model.deleted = 0;

-- 对象洞察重点指标配置与正式字段保持一致。
UPDATE iot_product product
JOIN (
    SELECT product.id AS product_id,
           JSON_ARRAYAGG(
               JSON_SET(
                   metrics.metric_json,
                   '$.identifier',
                   COALESCE(mapping.target_identifier, metrics.metric_identifier)
               )
               ORDER BY metrics.sort_no
           ) AS custom_metrics
    FROM iot_product product
    JOIN JSON_TABLE(
        COALESCE(product.metadata_json, JSON_OBJECT()),
        '$.objectInsight.customMetrics[*]'
        COLUMNS (
            sort_no FOR ORDINALITY,
            metric_json JSON PATH '$',
            metric_identifier VARCHAR(128) PATH '$.identifier'
        )
    ) metrics
    LEFT JOIN tmp_collector_identifier_mapping mapping
      ON mapping.product_id = product.id
     AND mapping.legacy_identifier = metrics.metric_identifier
    GROUP BY product.id
) transformed
  ON transformed.product_id = product.id
SET product.metadata_json = JSON_SET(
        COALESCE(product.metadata_json, JSON_OBJECT()),
        '$.objectInsight.customMetrics',
        transformed.custom_metrics
    ),
    product.update_time = NOW()
WHERE transformed.custom_metrics IS NOT NULL;

-- 风险指标目录同步迁移；若目标行已存在则优先保留目标行。
DELETE legacy_catalog
FROM risk_metric_catalog legacy_catalog
JOIN tmp_collector_identifier_mapping mapping
  ON mapping.product_id = legacy_catalog.product_id
 AND mapping.legacy_identifier = legacy_catalog.contract_identifier
JOIN risk_metric_catalog target_catalog
  ON target_catalog.product_id = legacy_catalog.product_id
 AND target_catalog.contract_identifier = mapping.target_identifier
 AND target_catalog.id <> legacy_catalog.id
WHERE legacy_catalog.deleted = 0
  AND target_catalog.deleted = 0;

UPDATE risk_metric_catalog catalog
JOIN tmp_collector_identifier_mapping mapping
  ON mapping.product_id = catalog.product_id
 AND mapping.legacy_identifier = catalog.contract_identifier
SET catalog.contract_identifier = mapping.target_identifier,
    catalog.normative_identifier = CASE
        WHEN catalog.normative_identifier IS NULL OR catalog.normative_identifier = mapping.legacy_identifier
            THEN mapping.target_identifier
        ELSE catalog.normative_identifier
    END,
    catalog.risk_metric_code = CONCAT('RM_', catalog.product_id, '_', UPPER(mapping.target_identifier)),
    catalog.update_time = NOW()
WHERE catalog.deleted = 0;

-- 风险点正式绑定与待治理记录跟随同一正式字段。
DELETE legacy_binding
FROM risk_point_device legacy_binding
JOIN iot_device device
  ON device.id = legacy_binding.device_id
 AND device.deleted = 0
JOIN tmp_collector_identifier_mapping mapping
  ON mapping.product_id = device.product_id
 AND mapping.legacy_identifier = legacy_binding.metric_identifier
JOIN risk_point_device target_binding
  ON target_binding.risk_point_id = legacy_binding.risk_point_id
 AND target_binding.device_id = legacy_binding.device_id
 AND target_binding.metric_identifier = mapping.target_identifier
 AND target_binding.id <> legacy_binding.id
WHERE legacy_binding.deleted = 0
  AND target_binding.deleted = 0;

UPDATE risk_point_device binding_row
JOIN iot_device device
  ON device.id = binding_row.device_id
 AND device.deleted = 0
JOIN tmp_collector_identifier_mapping mapping
  ON mapping.product_id = device.product_id
 AND mapping.legacy_identifier = binding_row.metric_identifier
SET binding_row.metric_identifier = mapping.target_identifier,
    binding_row.update_time = NOW()
WHERE binding_row.deleted = 0;

UPDATE risk_point_device_pending_binding pending_binding
JOIN iot_device device
  ON device.deleted = 0
 AND (
     (pending_binding.device_id IS NOT NULL AND device.id = pending_binding.device_id)
     OR (pending_binding.device_id IS NULL AND device.device_code = pending_binding.device_code)
 )
JOIN tmp_collector_identifier_mapping mapping
  ON mapping.product_id = device.product_id
 AND mapping.legacy_identifier = pending_binding.metric_identifier
SET pending_binding.metric_identifier = mapping.target_identifier,
    pending_binding.update_time = NOW()
WHERE pending_binding.deleted = 0;

UPDATE risk_point_device_pending_promotion pending_promotion
JOIN iot_device device
  ON device.id = pending_promotion.device_id
 AND device.deleted = 0
JOIN tmp_collector_identifier_mapping mapping
  ON mapping.product_id = device.product_id
 AND mapping.legacy_identifier = pending_promotion.metric_identifier
SET pending_promotion.metric_identifier = mapping.target_identifier,
    pending_promotion.update_time = NOW()
WHERE pending_promotion.deleted = 0;

-- 阈值规则只在已经绑定到目录行的情况下做产品级迁移。
UPDATE rule_definition rule_row
JOIN risk_metric_catalog catalog
  ON catalog.id = rule_row.risk_metric_id
 AND catalog.deleted = 0
JOIN tmp_collector_identifier_mapping mapping
  ON mapping.product_id = catalog.product_id
 AND mapping.legacy_identifier = rule_row.metric_identifier
SET rule_row.metric_identifier = mapping.target_identifier,
    rule_row.update_time = NOW()
WHERE rule_row.deleted = 0;

-- latest 属性先删重复，再补齐尚未切换为全路径的历史行。
DELETE legacy_property
FROM iot_device_property legacy_property
JOIN iot_device device
  ON device.id = legacy_property.device_id
 AND device.deleted = 0
JOIN tmp_collector_identifier_mapping mapping
  ON mapping.product_id = device.product_id
 AND mapping.legacy_identifier = legacy_property.identifier
JOIN iot_device_property target_property
  ON target_property.device_id = legacy_property.device_id
 AND target_property.identifier = mapping.target_identifier;

UPDATE iot_device_property device_property
JOIN iot_device device
  ON device.id = device_property.device_id
 AND device.deleted = 0
JOIN tmp_collector_identifier_mapping mapping
  ON mapping.product_id = device.product_id
 AND mapping.legacy_identifier = device_property.identifier
LEFT JOIN iot_device_property target_property
  ON target_property.device_id = device_property.device_id
 AND target_property.identifier = mapping.target_identifier
SET device_property.identifier = mapping.target_identifier,
    device_property.update_time = NOW()
WHERE target_property.id IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_collector_identifier_mapping;
