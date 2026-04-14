-- 2026-04-14
-- 配套清理脚本：在正式字段迁移完成后，删除采集器父产品 latest 属性中的短标识重复行。

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

DROP TEMPORARY TABLE IF EXISTS tmp_collector_identifier_mapping;
