-- 2026-04-14
-- 历史设备属性快照去重：
-- 当同一 device_id 下已经存在全路径 identifier（如 S1_ZT_1.signal_4g）时，
-- 删除同后缀的短 identifier（如 signal_4g），避免对象洞察台出现双份快照。

DELETE FROM iot_device_property
WHERE id IN (
  SELECT id
  FROM (
    SELECT DISTINCT short_row.id
    FROM iot_device_property short_row
    JOIN iot_device_property full_row
      ON full_row.device_id = short_row.device_id
     AND full_row.identifier IS NOT NULL
     AND TRIM(full_row.identifier) <> ''
     AND full_row.identifier LIKE '%.%'
     AND LOWER(TRIM(SUBSTRING_INDEX(full_row.identifier, '.', -1))) = LOWER(TRIM(short_row.identifier))
    WHERE short_row.identifier IS NOT NULL
      AND TRIM(short_row.identifier) <> ''
      AND short_row.identifier NOT LIKE '%.%'
  ) dup_ids
);
