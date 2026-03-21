USE rm_iot;

SET NAMES utf8mb4;

-- 南方测绘深部位移基准站 -> 8 个子设备初始化脚本。
-- 适用前提：
-- 1. 基准站 iot_device 记录已存在，device_code = 'SK00FB0D1310195'
-- 2. 脚本会自动补齐 dispsX / dispsY 物模型
-- 3. 已执行 Phase 4 风险监测相关表结构脚本

SET @base_station_code := 'SK00FB0D1310195';
SET @risk_point_code := 'RP_SK00FB0D1310195_SW';

SET @base_station_id := (
    SELECT id
    FROM iot_device
    WHERE device_code = @base_station_code
      AND deleted = 0
    ORDER BY id
    LIMIT 1
);

SET @base_tenant_id := (
    SELECT tenant_id
    FROM iot_device
    WHERE id = @base_station_id
    LIMIT 1
);

SET @base_product_id := (
    SELECT product_id
    FROM iot_device
    WHERE id = @base_station_id
    LIMIT 1
);

SET @device_id_seed := COALESCE((SELECT MAX(id) FROM iot_device), 0);
SET @product_model_id_seed := COALESCE((SELECT MAX(id) FROM iot_product_model), 0);

INSERT INTO iot_product_model (
    id,
    tenant_id,
    product_id,
    model_type,
    identifier,
    model_name,
    data_type,
    specs_json,
    event_type,
    service_input_json,
    service_output_json,
    sort_no,
    required_flag,
    description,
    create_time,
    update_time,
    deleted
)
SELECT
    (@product_model_id_seed := @product_model_id_seed + 1) AS id,
    COALESCE(@base_tenant_id, 1) AS tenant_id,
    @base_product_id AS product_id,
    'property' AS model_type,
    metric.identifier,
    metric.model_name,
    'double' AS data_type,
    JSON_OBJECT('unit', 'mm') AS specs_json,
    NULL AS event_type,
    NULL AS service_input_json,
    NULL AS service_output_json,
    metric.sort_no,
    0 AS required_flag,
    metric.description,
    NOW() AS create_time,
    NOW() AS update_time,
    0 AS deleted
FROM (
    SELECT 'dispsX' AS identifier, '顺滑动方向累计变形量' AS model_name, 10 AS sort_no,
           '顺滑动方向随时间的累计变形量，单位毫米' AS description
    UNION ALL
    SELECT 'dispsY', '垂直坡面方向累计变形量', 20,
           '垂直坡面方向随时间的累计变形量，单位毫米'
) metric
WHERE @base_product_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    model_name = VALUES(model_name),
    data_type = VALUES(data_type),
    specs_json = VALUES(specs_json),
    sort_no = VALUES(sort_no),
    required_flag = VALUES(required_flag),
    description = VALUES(description),
    update_time = VALUES(update_time),
    deleted = 0;

INSERT INTO iot_device (
    id,
    tenant_id,
    product_id,
    gateway_id,
    parent_device_id,
    device_name,
    device_code,
    device_secret,
    client_id,
    username,
    password,
    protocol_code,
    node_type,
    online_status,
    activate_status,
    device_status,
    firmware_version,
    ip_address,
    last_online_time,
    last_offline_time,
    last_report_time,
    longitude,
    latitude,
    address,
    metadata_json,
    remark,
    create_by,
    create_time,
    update_by,
    update_time,
    deleted
)
SELECT
    (@device_id_seed := @device_id_seed + 1) AS id,
    base.tenant_id,
    base.product_id,
    COALESCE(base.gateway_id, base.id) AS gateway_id,
    base.id AS parent_device_id,
    mapping.device_name,
    mapping.device_code,
    COALESCE(base.device_secret, mapping.device_code) AS device_secret,
    mapping.device_code AS client_id,
    mapping.device_code AS username,
    COALESCE(base.password, base.device_secret, mapping.device_code) AS password,
    base.protocol_code,
    3 AS node_type,
    0 AS online_status,
    COALESCE(base.activate_status, 0) AS activate_status,
    COALESCE(base.device_status, 1) AS device_status,
    base.firmware_version,
    base.ip_address,
    NULL AS last_online_time,
    NULL AS last_offline_time,
    NULL AS last_report_time,
    base.longitude,
    base.latitude,
    base.address,
    JSON_OBJECT(
        'source', 'south-survey-deep-displacement',
        'baseStationDeviceCode', base.device_code,
        'logicalDeviceCode', mapping.logical_code
    ) AS metadata_json,
    CONCAT('由基准站 ', base.device_code, ' 的逻辑测点 ', mapping.logical_code, ' 初始化') AS remark,
    1 AS create_by,
    NOW() AS create_time,
    1 AS update_by,
    NOW() AS update_time,
    0 AS deleted
FROM iot_device base
JOIN (
    SELECT 1 AS sort_no, 'L1_SW_1' AS logical_code, '84330701' AS device_code, '深部位移子设备-L1_SW_1' AS device_name
    UNION ALL SELECT 2, 'L1_SW_2', '84330695', '深部位移子设备-L1_SW_2'
    UNION ALL SELECT 3, 'L1_SW_3', '84330697', '深部位移子设备-L1_SW_3'
    UNION ALL SELECT 4, 'L1_SW_4', '84330699', '深部位移子设备-L1_SW_4'
    UNION ALL SELECT 5, 'L1_SW_5', '84330686', '深部位移子设备-L1_SW_5'
    UNION ALL SELECT 6, 'L1_SW_6', '84330687', '深部位移子设备-L1_SW_6'
    UNION ALL SELECT 7, 'L1_SW_7', '84330691', '深部位移子设备-L1_SW_7'
    UNION ALL SELECT 8, 'L1_SW_8', '84330696', '深部位移子设备-L1_SW_8'
) mapping
WHERE base.id = @base_station_id
ORDER BY mapping.sort_no
ON DUPLICATE KEY UPDATE
    product_id = VALUES(product_id),
    gateway_id = VALUES(gateway_id),
    parent_device_id = VALUES(parent_device_id),
    device_name = VALUES(device_name),
    device_secret = VALUES(device_secret),
    client_id = VALUES(client_id),
    username = VALUES(username),
    password = VALUES(password),
    protocol_code = VALUES(protocol_code),
    node_type = VALUES(node_type),
    device_status = VALUES(device_status),
    firmware_version = VALUES(firmware_version),
    ip_address = VALUES(ip_address),
    longitude = VALUES(longitude),
    latitude = VALUES(latitude),
    address = VALUES(address),
    metadata_json = VALUES(metadata_json),
    remark = VALUES(remark),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    deleted = 0;

INSERT INTO risk_point (
    risk_point_code,
    risk_point_name,
    region_id,
    region_name,
    responsible_user,
    responsible_phone,
    risk_level,
    description,
    status,
    tenant_id,
    create_by,
    create_time,
    update_by,
    update_time,
    deleted
)
SELECT
    @risk_point_code,
    CONCAT(@base_station_code, ' 深部位移监测点'),
    NULL,
    NULL,
    NULL,
    NULL,
    'info',
    CONCAT('基准站 ', @base_station_code, ' 下挂 8 个深部位移子设备的初始风险点'),
    0,
    COALESCE(@base_tenant_id, 1),
    1,
    NOW(),
    1,
    NOW(),
    0
FROM DUAL
WHERE @base_station_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    risk_point_name = VALUES(risk_point_name),
    description = VALUES(description),
    status = VALUES(status),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    deleted = 0;

SET @risk_point_id := (
    SELECT id
    FROM risk_point
    WHERE tenant_id = COALESCE(@base_tenant_id, 1)
      AND risk_point_code = @risk_point_code
      AND deleted = 0
    ORDER BY id
    LIMIT 1
);

-- 防止脚本重复执行时，同一风险点-设备-测点绑定被重复插入。
DELETE duplicate_binding
FROM risk_point_device duplicate_binding
JOIN risk_point_device keep_binding
    ON duplicate_binding.risk_point_id = keep_binding.risk_point_id
   AND duplicate_binding.device_code = keep_binding.device_code
   AND duplicate_binding.metric_identifier = keep_binding.metric_identifier
   AND duplicate_binding.tenant_id = keep_binding.tenant_id
   AND duplicate_binding.deleted = keep_binding.deleted
WHERE duplicate_binding.id > keep_binding.id
  AND duplicate_binding.risk_point_id = @risk_point_id
  AND duplicate_binding.device_code IN (
      '84330701', '84330695', '84330697', '84330699',
      '84330686', '84330687', '84330691', '84330696'
  )
  AND duplicate_binding.metric_identifier IN ('dispsX', 'dispsY')
  AND duplicate_binding.deleted = 0
  AND keep_binding.deleted = 0
  AND @risk_point_id IS NOT NULL;

INSERT INTO risk_point_device (
    risk_point_id,
    device_id,
    device_code,
    device_name,
    metric_identifier,
    metric_name,
    default_threshold,
    threshold_unit,
    tenant_id,
    create_by,
    create_time,
    update_by,
    update_time,
    deleted
)
SELECT
    @risk_point_id AS risk_point_id,
    child.id AS device_id,
    child.device_code,
    child.device_name,
    metric.metric_identifier,
    metric.metric_name,
    NULL AS default_threshold,
    'mm' AS threshold_unit,
    child.tenant_id,
    1 AS create_by,
    NOW() AS create_time,
    1 AS update_by,
    NOW() AS update_time,
    0 AS deleted
FROM iot_device child
JOIN (
    SELECT '84330701' AS device_code
    UNION ALL SELECT '84330695'
    UNION ALL SELECT '84330697'
    UNION ALL SELECT '84330699'
    UNION ALL SELECT '84330686'
    UNION ALL SELECT '84330687'
    UNION ALL SELECT '84330691'
    UNION ALL SELECT '84330696'
) child_codes
    ON child.device_code = child_codes.device_code
JOIN (
    SELECT 'dispsX' AS metric_identifier, '顺滑动方向累计变形量' AS metric_name
    UNION ALL
    SELECT 'dispsY', '垂直坡面方向累计变形量'
) metric
LEFT JOIN risk_point_device existing
    ON existing.risk_point_id = @risk_point_id
   AND existing.device_id = child.id
   AND existing.metric_identifier = metric.metric_identifier
   AND existing.tenant_id = child.tenant_id
   AND existing.deleted = 0
WHERE child.deleted = 0
  AND child.tenant_id = COALESCE(@base_tenant_id, 1)
  AND @risk_point_id IS NOT NULL
  AND existing.id IS NULL;
