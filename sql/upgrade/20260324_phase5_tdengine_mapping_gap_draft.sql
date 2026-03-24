USE rm_iot;

SET NAMES utf8mb4;

-- Phase 5 TDengine 映射缺口治理草案（默认 dry-run）。
-- 目标：
-- 1. 先补齐 MySQL 侧 iot_product_model property 基线。
-- 2. 先写 tdengineLegacy 占位结构，不直接写死未经真实环境核验的 stable/column。
-- 3. 只有 TDengine REST / SQL 认证打通并完成 DESCRIBE 复核后，才允许把草案升级为真实环境最终映射。
--
-- 使用方式：
-- - 默认 @apply_changes = 0：只输出范围预览，不执行 INSERT / UPDATE。
-- - 手工核验 stable/column 后，再把 @apply_changes 改为 1 重跑。
-- - 本脚本不负责 legacy stable DDL，保持与 docs/04、docs/07 的环境治理口径一致。

SET @apply_changes := 0;
SET @mapping_status := 'PENDING_TDENGINE_VERIFICATION';
SET @blocked_reason := 'stable/column 未经真实环境核验，禁止自动落库';
SET @evidence_source := 'docs/superpowers/plans/2026-03-24-dp-standardization-refactor-plan.md';

DROP TEMPORARY TABLE IF EXISTS tmp_tdengine_product_scope;
CREATE TEMPORARY TABLE tmp_tdengine_product_scope (
    product_key VARCHAR(64) NOT NULL,
    gap_summary VARCHAR(500) NOT NULL,
    PRIMARY KEY (product_key)
);

INSERT INTO tmp_tdengine_product_scope (product_key, gap_summary)
VALUES
    ('south_rtu', '裂缝类 property 缺口明显，至少补齐 L1_LF_* 与代表性 S1_ZT_1 状态类指标'),
    ('south_multi_displacement', '多维位移至少补齐 L1_JS_1 / L1_QJ_1 / L1_LF_1 / S1_ZT_1 代表性指标'),
    ('south_gnss_monitor', 'GNSS 位移至少补齐 L1_GP_1 位移量和惯导代表性指标'),
    ('south_deep_displacement', '现有 dispsX/dispsY 仅补占位结构，并为父级 S1_ZT_1 状态类补代表性 seed');

DROP TEMPORARY TABLE IF EXISTS tmp_tdengine_mapping_gap_seed;
CREATE TEMPORARY TABLE tmp_tdengine_mapping_gap_seed (
    product_key VARCHAR(64) NOT NULL,
    identifier VARCHAR(64) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    data_type VARCHAR(32) NOT NULL,
    sort_no INT NOT NULL,
    description VARCHAR(500) NOT NULL,
    specs_json JSON NOT NULL,
    family_code VARCHAR(64) DEFAULT NULL,
    legacy_stable_hint VARCHAR(64) DEFAULT NULL,
    legacy_column_hint VARCHAR(64) DEFAULT NULL,
    PRIMARY KEY (product_key, identifier)
);

INSERT INTO tmp_tdengine_mapping_gap_seed (
    product_key,
    identifier,
    model_name,
    data_type,
    sort_no,
    description,
    specs_json,
    family_code,
    legacy_stable_hint,
    legacy_column_hint
)
VALUES
    ('south_rtu', 'L1_LF_1', '裂缝监测点1最新值', 'double', 1010, '代表性裂缝家族 property seed；其余 L1_LF_* 需按 live 导出继续补齐。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_LF_1', 'l1_lf_1', 'value'),
    ('south_rtu', 'L1_LF_2', '裂缝监测点2最新值', 'double', 1020, '代表性裂缝家族 property seed；其余 L1_LF_* 需按 live 导出继续补齐。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_LF_2', 'l1_lf_1', 'value'),
    ('south_rtu', 'L1_LF_3', '裂缝监测点3最新值', 'double', 1030, '代表性裂缝家族 property seed；其余 L1_LF_* 需按 live 导出继续补齐。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_LF_3', 'l1_lf_1', 'value'),
    ('south_rtu', 'S1_ZT_1.ext_power_volt', '外部供电电压', 'double', 1110, '状态类代表指标；stable/column 只保留 hint，不直接落正式映射。',
     JSON_OBJECT('unit', 'V', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'ext_power_volt'),
    ('south_multi_displacement', 'L1_JS_1.gX', '加速度 X', 'double', 2010, '多维位移代表性 live 指标；writer 当前无可命中的 property 元数据。',
     JSON_OBJECT('unit', 'g', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_JS_1', 'l1_js_1', 'gX'),
    ('south_multi_displacement', 'L1_QJ_1.angle', '倾角', 'double', 2020, '多维位移代表性 live 指标；最终列名需以 TDengine DESCRIBE 结果为准。',
     JSON_OBJECT('unit', 'degree', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_QJ_1', 'l1_qj_1', 'angle'),
    ('south_multi_displacement', 'L1_LF_1.value', '裂缝值', 'double', 2030, '裂缝代表指标；建议 identifier 先保留 live key。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_LF_1', 'l1_lf_1', 'value'),
    ('south_multi_displacement', 'S1_ZT_1.pa_state', '设备状态', 'int', 2040, '状态类代表指标；列名只作为 hint 保留。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'pa_state'),
    ('south_gnss_monitor', 'L1_GP_1.gpsTotalX', 'GNSS 总位移 X', 'double', 3010, 'GNSS live 代表指标；建议 identifier 先保持 live key。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_GP_1', 'l1_gp_1', 'gpsTotalX'),
    ('south_gnss_monitor', 'L1_GP_1.gpsTotalY', 'GNSS 总位移 Y', 'double', 3020, 'GNSS live 代表指标；列命名仍需以实库 DESCRIBE 为准。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_GP_1', 'l1_gp_1', 'gpsTotalY'),
    ('south_gnss_monitor', 'L1_GP_1.gpsTotalZ', 'GNSS 总位移 Z', 'double', 3030, 'GNSS live 代表指标；列命名仍需以实库 DESCRIBE 为准。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_GP_1', 'l1_gp_1', 'gpsTotalZ'),
    ('south_gnss_monitor', 'L1_JS_1.gX', '加速度 X', 'double', 3040, 'GNSS 场景下的惯导代表指标。',
     JSON_OBJECT('unit', 'g', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_JS_1', 'l1_js_1', 'gX'),
    ('south_deep_displacement', 'S1_ZT_1.ext_power_volt', '外部供电电压', 'double', 4010, '深部位移父级状态类代表指标；split child 指标由现有 dispsX/dispsY 承接。',
     JSON_OBJECT('unit', 'V', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'ext_power_volt');

DROP TEMPORARY TABLE IF EXISTS tmp_tdengine_existing_patch;
CREATE TEMPORARY TABLE tmp_tdengine_existing_patch (
    product_key VARCHAR(64) NOT NULL,
    identifier VARCHAR(64) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    data_type VARCHAR(32) NOT NULL,
    sort_no INT NOT NULL,
    description VARCHAR(500) NOT NULL,
    specs_patch JSON NOT NULL,
    legacy_stable_hint VARCHAR(64) DEFAULT NULL,
    legacy_column_hint VARCHAR(64) DEFAULT NULL,
    PRIMARY KEY (product_key, identifier)
);

INSERT INTO tmp_tdengine_existing_patch (
    product_key,
    identifier,
    model_name,
    data_type,
    sort_no,
    description,
    specs_patch,
    legacy_stable_hint,
    legacy_column_hint
)
VALUES
    ('south_deep_displacement', 'dispsX', '顺滑动方向累计变形量', 'double', 10, '现有 property 只补 tdengineLegacy 占位结构，不直接写最终 stable/column。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'l1_sw_1', 'dispsX'),
    ('south_deep_displacement', 'dispsY', '垂直坡面方向累计变形量', 'double', 20, '现有 property 只补 tdengineLegacy 占位结构，不直接写最终 stable/column。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'l1_sw_1', 'dispsY');

-- 预览当前产品主数据是否已存在。
SELECT
    scope.product_key,
    product.id AS product_id,
    product.product_name,
    scope.gap_summary
FROM tmp_tdengine_product_scope scope
LEFT JOIN iot_product product
       ON product.product_key = scope.product_key
      AND product.deleted = 0
ORDER BY scope.product_key;

-- 预览代表性缺口 seed。
SELECT
    product_key,
    identifier,
    model_name,
    data_type,
    family_code,
    legacy_stable_hint,
    legacy_column_hint
FROM tmp_tdengine_mapping_gap_seed
ORDER BY product_key, sort_no, identifier;

-- 预览对现有 property 的占位 patch。
SELECT
    product_key,
    identifier,
    legacy_stable_hint,
    legacy_column_hint
FROM tmp_tdengine_existing_patch
ORDER BY product_key, sort_no, identifier;

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
    product.tenant_id,
    product.id,
    'property' AS model_type,
    seed.identifier,
    seed.model_name,
    seed.data_type,
    seed.specs_json,
    NULL AS event_type,
    NULL AS service_input_json,
    NULL AS service_output_json,
    seed.sort_no,
    0 AS required_flag,
    seed.description,
    NOW() AS create_time,
    NOW() AS update_time,
    0 AS deleted
FROM tmp_tdengine_mapping_gap_seed seed
JOIN iot_product product
  ON product.product_key = seed.product_key
 AND product.deleted = 0
LEFT JOIN iot_product_model existing_model
  ON existing_model.product_id = product.id
 AND existing_model.model_type = 'property'
 AND existing_model.identifier = seed.identifier
 AND existing_model.deleted = 0
WHERE @apply_changes = 1
  AND existing_model.id IS NULL
ORDER BY product.id, seed.sort_no, seed.identifier;

UPDATE iot_product_model model
JOIN iot_product product
  ON product.id = model.product_id
 AND product.deleted = 0
JOIN tmp_tdengine_mapping_gap_seed seed
  ON seed.product_key = product.product_key
 AND seed.identifier = model.identifier
SET model.model_name = seed.model_name,
    model.data_type = seed.data_type,
    model.specs_json = seed.specs_json,
    model.sort_no = seed.sort_no,
    model.required_flag = 0,
    model.description = seed.description,
    model.update_time = NOW(),
    model.deleted = 0
WHERE @apply_changes = 1
  AND model.model_type = 'property';

UPDATE iot_product_model model
JOIN iot_product product
  ON product.id = model.product_id
 AND product.deleted = 0
JOIN tmp_tdengine_existing_patch patch
  ON patch.product_key = product.product_key
 AND patch.identifier = model.identifier
SET model.model_name = patch.model_name,
    model.data_type = patch.data_type,
    model.specs_json = JSON_MERGE_PATCH(COALESCE(model.specs_json, JSON_OBJECT()), patch.specs_patch),
    model.sort_no = patch.sort_no,
    model.required_flag = 0,
    model.description = patch.description,
    model.update_time = NOW(),
    model.deleted = 0
WHERE @apply_changes = 1
  AND model.model_type = 'property';

SELECT
    @apply_changes AS apply_changes,
    COUNT(*) AS representative_seed_rows,
    (SELECT COUNT(*) FROM tmp_tdengine_existing_patch) AS existing_patch_rows,
    @evidence_source AS evidence_source,
    'apply_changes=0 时本脚本只做预览；stable/column 必须在 TDengine 实库核验后另行回写。' AS note
FROM tmp_tdengine_mapping_gap_seed;
