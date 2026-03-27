USE rm_iot;

SET NAMES utf8mb4;

-- Phase 5 TDengine 映射缺口治理草案（默认 dry-run）。
-- 目标：
-- 1. 先补齐 MySQL 侧 iot_product_model property 基线。
-- 2. 先写 tdengineLegacy 占位结构，不直接写死未经真实环境核验的 stable/column。
-- 3. 当前 TDengine REST / SQL 已可执行 DESCRIBE，但在列名 canonicalization / mixed type 策略未收敛前，仍不允许把草案升级为真实环境最终映射。
--
-- 使用方式：
-- - 默认 @apply_changes = 0：只输出范围预览，不执行 INSERT / UPDATE。
-- - 手工核验 stable/column 后，再把 @apply_changes 改为 1 重跑。
-- - 本脚本不负责 legacy stable DDL，保持与 docs/04、docs/07 的环境治理口径一致。

SET @apply_changes := 0;
SET @mapping_status := 'PENDING_TDENGINE_VERIFICATION';
SET @blocked_reason := 'stable/column/canonicalization 尚未完全收敛，禁止自动落库';
SET @evidence_source := 'docs/superpowers/plans/2026-03-24-dp-standardization-refactor-plan.md';

-- 2026-03-25 实库 DESCRIBE 已确认的代表性 canonical mapping：
-- - l1_lf_1.lf
-- - l1_js_1.gx / gy / gz
-- - l1_qj_1.angle / azi / x / y / z
-- - l1_gp_1.gps_total_x / gps_total_y / gps_total_z
-- - l1_sw_1.disps_x / disps_y
-- - s1_zt_1.signal_nb / signal_db

DROP TEMPORARY TABLE IF EXISTS tmp_tdengine_product_scope;
CREATE TEMPORARY TABLE tmp_tdengine_product_scope (
    product_key VARCHAR(64) NOT NULL,
    gap_summary VARCHAR(500) NOT NULL,
    PRIMARY KEY (product_key)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

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
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

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
     'L1_LF_1', 'l1_lf_1', 'lf'),
    ('south_rtu', 'L1_LF_2', '裂缝监测点2最新值', 'double', 1020, '代表性裂缝家族 property seed；其余 L1_LF_* 需按 live 导出继续补齐。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_LF_2', 'l1_lf_1', 'lf'),
    ('south_rtu', 'L1_LF_3', '裂缝监测点3最新值', 'double', 1030, '代表性裂缝家族 property seed；其余 L1_LF_* 需按 live 导出继续补齐。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_LF_3', 'l1_lf_1', 'lf'),
    ('south_rtu', 'L1_LF_4', '裂缝监测点4最新值', 'double', 1040, '2026-03-25 live gap 补录；当前实库同时出现 int/double，草案先按裂缝数值 double 占位，待真实环境回归后再收敛。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_LF_4', 'l1_lf_1', 'lf'),
    ('south_rtu', 'L1_LF_5', '裂缝监测点5最新值', 'double', 1050, '2026-03-25 live gap 补录；沿用裂缝家族占位结构，待 TDengine 实库核验后再确认正式映射。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_LF_5', 'l1_lf_1', 'lf'),
    ('south_rtu', 'L1_LF_6', '裂缝监测点6最新值', 'double', 1060, '2026-03-25 live gap 补录；沿用裂缝家族占位结构，待 TDengine 实库核验后再确认正式映射。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_LF_6', 'l1_lf_1', 'lf'),
    ('south_rtu', 'L1_LF_7', '裂缝监测点7最新值', 'double', 1070, '2026-03-25 live gap 补录；沿用裂缝家族占位结构，待 TDengine 实库核验后再确认正式映射。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_LF_7', 'l1_lf_1', 'lf'),
    ('south_rtu', 'L1_LF_8', '裂缝监测点8最新值', 'double', 1080, '2026-03-25 live gap 补录；沿用裂缝家族占位结构，待 TDengine 实库核验后再确认正式映射。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_LF_8', 'l1_lf_1', 'lf'),
    ('south_rtu', 'L1_LF_9', '裂缝监测点9最新值', 'double', 1090, '2026-03-25 live gap 补录；沿用裂缝家族占位结构，待 TDengine 实库核验后再确认正式映射。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_LF_9', 'l1_lf_1', 'lf'),
    ('south_rtu', 'S1_ZT_1.ext_power_volt', '外部供电电压', 'double', 1110, '状态类代表指标；stable/column 只保留 hint，不直接落正式映射。',
     JSON_OBJECT('unit', 'V', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'ext_power_volt'),
    ('south_rtu', 'S1_ZT_1.battery_dump_energy', '电池放电能量', 'int', 1120, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'battery_dump_energy'),
    ('south_rtu', 'S1_ZT_1.battery_volt', '电池电压', 'int', 1130, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'battery_volt'),
    ('south_rtu', 'S1_ZT_1.consume_power', '功耗', 'int', 1140, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'consume_power'),
    ('south_rtu', 'S1_ZT_1.humidity', '湿度', 'double', 1150, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'humidity'),
    ('south_rtu', 'S1_ZT_1.humidity_out', '外部湿度', 'double', 1160, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'humidity_out'),
    ('south_rtu', 'S1_ZT_1.lat', '纬度', 'string', 1170, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'lat'),
    ('south_rtu', 'S1_ZT_1.lon', '经度', 'string', 1180, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'lon'),
    ('south_rtu', 'S1_ZT_1.sensor_state.L1_LF_1', '裂缝监测点1状态', 'int', 1190, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_lf_1'),
    ('south_rtu', 'S1_ZT_1.sensor_state.L1_LF_2', '裂缝监测点2状态', 'int', 1200, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_lf_2'),
    ('south_rtu', 'S1_ZT_1.sensor_state.L1_LF_3', '裂缝监测点3状态', 'int', 1210, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_lf_3'),
    ('south_rtu', 'S1_ZT_1.sensor_state.L1_LF_4', '裂缝监测点4状态', 'int', 1220, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_lf_4'),
    ('south_rtu', 'S1_ZT_1.sensor_state.L1_LF_5', '裂缝监测点5状态', 'int', 1230, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_lf_5'),
    ('south_rtu', 'S1_ZT_1.sensor_state.L1_LF_6', '裂缝监测点6状态', 'int', 1240, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_lf_6'),
    ('south_rtu', 'S1_ZT_1.sensor_state.L1_LF_7', '裂缝监测点7状态', 'int', 1250, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_lf_7'),
    ('south_rtu', 'S1_ZT_1.sensor_state.L1_LF_8', '裂缝监测点8状态', 'int', 1260, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_lf_8'),
    ('south_rtu', 'S1_ZT_1.sensor_state.L1_LF_9', '裂缝监测点9状态', 'int', 1270, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_lf_9'),
    ('south_rtu', 'S1_ZT_1.signal_4g', '4G 信号', 'string', 1280, '2026-03-25 live gap 补录；真实库同时出现 int/string，草案先按 string 占位，待 normalizer 与 TDengine 映射进一步收敛。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'signal_4g'),
    ('south_rtu', 'S1_ZT_1.singal_bd', '北斗信号', 'string', 1290, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'singal_bd'),
    ('south_rtu', 'S1_ZT_1.singal_db', '信号强度 dB', 'int', 1300, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 signal_db 命名，草案继续保留历史 identifier 兼容。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'signal_db'),
    ('south_rtu', 'S1_ZT_1.singal_NB', 'NB 信号', 'string', 1310, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 signal_nb 命名，当前仍需继续收敛历史 identifier 与 mixed type。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'signal_nb'),
    ('south_rtu', 'S1_ZT_1.solar_volt', '太阳能电压', 'int', 1320, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'solar_volt'),
    ('south_rtu', 'S1_ZT_1.supply_power', '供电功率', 'int', 1330, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'supply_power'),
    ('south_rtu', 'S1_ZT_1.sw_version', '软件版本', 'string', 1340, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sw_version'),
    ('south_rtu', 'S1_ZT_1.temp', '温度', 'double', 1350, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'temp'),
    ('south_rtu', 'S1_ZT_1.temp_out', '外部温度', 'double', 1360, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'temp_out'),
    ('south_multi_displacement', 'L1_JS_1.gX', '加速度 X', 'double', 2010, '多维位移代表性 live 指标；TDengine 实库列已确认采用 gx 命名。',
     JSON_OBJECT('unit', 'g', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_JS_1', 'l1_js_1', 'gx'),
    ('south_multi_displacement', 'L1_QJ_1.angle', '倾角', 'double', 2020, '多维位移代表性 live 指标；最终列名需以 TDengine DESCRIBE 结果为准。',
     JSON_OBJECT('unit', 'degree', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_QJ_1', 'l1_qj_1', 'angle'),
    ('south_multi_displacement', 'L1_LF_1.value', '裂缝值', 'double', 2030, '裂缝代表指标；TDengine 实库列已确认采用 lf 命名，identifier 继续保留 live key。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_LF_1', 'l1_lf_1', 'lf'),
    ('south_multi_displacement', 'S1_ZT_1.pa_state', '设备状态', 'bool', 2040, '状态类代表指标；当前 live value_type 为 bool，列名只作为 hint 保留。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'pa_state'),
    ('south_multi_displacement', 'L1_JS_1.gY', '加速度 Y', 'double', 2050, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 gy 命名。',
     JSON_OBJECT('unit', 'g', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_JS_1', 'l1_js_1', 'gy'),
    ('south_multi_displacement', 'L1_JS_1.gZ', '加速度 Z', 'double', 2060, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 gz 命名，但当前列类型仍待 mixed type 策略收敛。',
     JSON_OBJECT('unit', 'g', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_JS_1', 'l1_js_1', 'gz'),
    ('south_multi_displacement', 'L1_LF_1', '裂缝值兼容键', 'double', 2070, '2026-03-25 live gap 补录；当前 real payload 同时出现 `L1_LF_1` 与 `L1_LF_1.value`，TDengine 实库列已确认采用 lf 命名。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_LF_1', 'l1_lf_1', 'lf'),
    ('south_multi_displacement', 'L1_QJ_1.AZI', '方位角', 'double', 2080, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 azi 命名。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_QJ_1', 'l1_qj_1', 'azi'),
    ('south_multi_displacement', 'L1_QJ_1.trend', '倾角趋势', 'double', 2090, '2026-03-25 live gap 补录；倾角家族扩展字段，正式列名需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_QJ_1', 'l1_qj_1', 'trend'),
    ('south_multi_displacement', 'L1_QJ_1.X', '倾角 X', 'double', 2100, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 x 命名。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_QJ_1', 'l1_qj_1', 'x'),
    ('south_multi_displacement', 'L1_QJ_1.Y', '倾角 Y', 'double', 2110, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 y 命名，当前 live 存在 double/int 混型。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_QJ_1', 'l1_qj_1', 'y'),
    ('south_multi_displacement', 'L1_QJ_1.Z', '倾角 Z', 'double', 2120, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 z 命名，当前 live 存在 double/int 混型。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_QJ_1', 'l1_qj_1', 'z'),
    ('south_multi_displacement', 'S1_ZT_1.battery_dump_energy', '电池放电能量', 'int', 2130, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'battery_dump_energy'),
    ('south_multi_displacement', 'S1_ZT_1.ext_power_volt', '外部供电电压', 'double', 2140, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('unit', 'V', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'ext_power_volt'),
    ('south_multi_displacement', 'S1_ZT_1.humidity', '湿度', 'double', 2150, '2026-03-25 live gap 补录；当前 live 存在 double/int 混型，草案先按 double 占位。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'humidity'),
    ('south_multi_displacement', 'S1_ZT_1.lat', '纬度', 'string', 2160, '2026-03-25 live gap 补录；当前 live 以 string 为主，但存在少量 double，草案先按 string 占位。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'lat'),
    ('south_multi_displacement', 'S1_ZT_1.lon', '经度', 'string', 2170, '2026-03-25 live gap 补录；当前 live 以 string 为主，但存在少量 double，草案先按 string 占位。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'lon'),
    ('south_multi_displacement', 'S1_ZT_1.sensor_state.L1_JS_1', '加速度测点状态', 'int', 2180, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_js_1'),
    ('south_multi_displacement', 'S1_ZT_1.sensor_state.L1_LF_1', '裂缝测点状态', 'int', 2190, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_lf_1'),
    ('south_multi_displacement', 'S1_ZT_1.sensor_state.L1_QJ_1', '倾角测点状态', 'int', 2200, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_qj_1'),
    ('south_multi_displacement', 'S1_ZT_1.signal_4g', '4G 信号', 'int', 2210, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'signal_4g'),
    ('south_multi_displacement', 'S1_ZT_1.signal_bd', '北斗信号', 'int', 2220, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'signal_bd'),
    ('south_multi_displacement', 'S1_ZT_1.signal_NB', 'NB 信号', 'int', 2230, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 signal_nb 命名。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'signal_nb'),
    ('south_multi_displacement', 'S1_ZT_1.solar_volt', '太阳能电压', 'double', 2240, '2026-03-25 live gap 补录；当前 live 存在 int/double 混型，草案先按 double 占位。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'solar_volt'),
    ('south_multi_displacement', 'S1_ZT_1.sw_version', '软件版本', 'string', 2250, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sw_version'),
    ('south_multi_displacement', 'S1_ZT_1.temp', '温度', 'double', 2260, '2026-03-25 live gap 补录；当前 live 存在 double/int 混型，草案先按 double 占位。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'temp'),
    ('south_gnss_monitor', 'L1_GP_1.gpsTotalX', 'GNSS 总位移 X', 'double', 3010, 'GNSS live 代表指标；TDengine 实库列已确认采用 gps_total_x 命名。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_GP_1', 'l1_gp_1', 'gps_total_x'),
    ('south_gnss_monitor', 'L1_GP_1.gpsTotalY', 'GNSS 总位移 Y', 'double', 3020, 'GNSS live 代表指标；TDengine 实库列已确认采用 gps_total_y 命名。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_GP_1', 'l1_gp_1', 'gps_total_y'),
    ('south_gnss_monitor', 'L1_GP_1.gpsTotalZ', 'GNSS 总位移 Z', 'double', 3030, 'GNSS live 代表指标；TDengine 实库列已确认采用 gps_total_z 命名。',
     JSON_OBJECT('unit', 'mm', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_GP_1', 'l1_gp_1', 'gps_total_z'),
    ('south_gnss_monitor', 'L1_JS_1.gX', '加速度 X', 'double', 3040, 'GNSS 场景下的惯导代表指标；TDengine 实库列已确认采用 gx 命名。',
     JSON_OBJECT('unit', 'g', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_JS_1', 'l1_js_1', 'gx'),
    ('south_gnss_monitor', 'L1_JS_1.gY', '加速度 Y', 'double', 3050, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 gy 命名，当前 live 存在 double/int 混型。',
     JSON_OBJECT('unit', 'g', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_JS_1', 'l1_js_1', 'gy'),
    ('south_gnss_monitor', 'L1_JS_1.gZ', '加速度 Z', 'double', 3060, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 gz 命名，但当前列类型仍待 mixed type 策略收敛。',
     JSON_OBJECT('unit', 'g', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_JS_1', 'l1_js_1', 'gz'),
    ('south_gnss_monitor', 'L1_QJ_1.angle', '倾角', 'double', 3070, '2026-03-25 live gap 补录；当前 live 存在 double/int 混型，草案先按 double 占位。',
     JSON_OBJECT('unit', 'degree', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_QJ_1', 'l1_qj_1', 'angle'),
    ('south_gnss_monitor', 'L1_QJ_1.AZI', '方位角', 'int', 3080, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 azi 命名。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_QJ_1', 'l1_qj_1', 'azi'),
    ('south_gnss_monitor', 'L1_QJ_1.X', '倾角 X', 'double', 3090, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 x 命名。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_QJ_1', 'l1_qj_1', 'x'),
    ('south_gnss_monitor', 'L1_QJ_1.Y', '倾角 Y', 'double', 3100, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 y 命名，当前 live 存在 double/int 混型。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_QJ_1', 'l1_qj_1', 'y'),
    ('south_gnss_monitor', 'L1_QJ_1.Z', '倾角 Z', 'double', 3110, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 z 命名，当前 live 存在 double/int 混型。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'L1_QJ_1', 'l1_qj_1', 'z'),
    ('south_gnss_monitor', 'S1_ZT_1.battery_dump_energy', '电池放电能量', 'int', 3120, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'battery_dump_energy'),
    ('south_gnss_monitor', 'S1_ZT_1.battery_volt', '电池电压', 'int', 3130, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'battery_volt'),
    ('south_gnss_monitor', 'S1_ZT_1.consume_power', '功耗', 'int', 3140, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'consume_power'),
    ('south_gnss_monitor', 'S1_ZT_1.ext_power_volt', '外部供电电压', 'double', 3150, '2026-03-25 live gap 补录；当前 live 存在 double/int 混型，草案先按 double 占位。',
     JSON_OBJECT('unit', 'V', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'ext_power_volt'),
    ('south_gnss_monitor', 'S1_ZT_1.humidity', '湿度', 'double', 3160, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'humidity'),
    ('south_gnss_monitor', 'S1_ZT_1.humidity_out', '外部湿度', 'double', 3170, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'humidity_out'),
    ('south_gnss_monitor', 'S1_ZT_1.lat', '纬度', 'string', 3180, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'lat'),
    ('south_gnss_monitor', 'S1_ZT_1.lon', '经度', 'string', 3190, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'lon'),
    ('south_gnss_monitor', 'S1_ZT_1.sensor_state.L1_GP_1', 'GNSS 测点状态', 'int', 3200, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_gp_1'),
    ('south_gnss_monitor', 'S1_ZT_1.sensor_state.L1_JS_1', '加速度测点状态', 'int', 3210, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_js_1'),
    ('south_gnss_monitor', 'S1_ZT_1.sensor_state.L1_JZ_1', 'L1_JZ_1 状态', 'int', 3220, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_jz_1'),
    ('south_gnss_monitor', 'S1_ZT_1.sensor_state.L1_QJ_1', '倾角测点状态', 'int', 3230, '2026-03-25 live gap 补录；父级状态容器中的传感器状态字段只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_qj_1'),
    ('south_gnss_monitor', 'S1_ZT_1.signal_4g', '4G 信号', 'int', 3240, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'signal_4g'),
    ('south_gnss_monitor', 'S1_ZT_1.singal_db', '信号强度 dB', 'int', 3250, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 signal_db 命名，草案继续保留历史 identifier 兼容。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'signal_db'),
    ('south_gnss_monitor', 'S1_ZT_1.singal_NB', 'NB 信号', 'int', 3260, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 signal_nb 命名，草案继续保留历史 identifier 兼容。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'signal_nb'),
    ('south_gnss_monitor', 'S1_ZT_1.solar_volt', '太阳能电压', 'int', 3270, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'solar_volt'),
    ('south_gnss_monitor', 'S1_ZT_1.supply_power', '供电功率', 'int', 3280, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'supply_power'),
    ('south_gnss_monitor', 'S1_ZT_1.sw_version', '软件版本', 'string', 3290, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sw_version'),
    ('south_gnss_monitor', 'S1_ZT_1.temp', '温度', 'double', 3300, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'temp'),
    ('south_gnss_monitor', 'S1_ZT_1.temp_out', '外部温度', 'double', 3310, '2026-03-25 live gap 补录；父级状态类指标只补占位结构。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'temp_out'),
    ('south_deep_displacement', 'S1_ZT_1.ext_power_volt', '外部供电电压', 'double', 4010, '深部位移父级状态类代表指标；split child 指标由现有 dispsX/dispsY 承接。',
     JSON_OBJECT('unit', 'V', 'tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'ext_power_volt'),
    ('south_deep_displacement', 'S1_ZT_1.battery_dump_energy', '电池放电能量', 'int', 4020, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'battery_dump_energy'),
    ('south_deep_displacement', 'S1_ZT_1.battery_volt', '电池电压', 'int', 4030, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'battery_volt'),
    ('south_deep_displacement', 'S1_ZT_1.consume_power', '功耗', 'int', 4040, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'consume_power'),
    ('south_deep_displacement', 'S1_ZT_1.humidity', '湿度', 'double', 4050, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'humidity'),
    ('south_deep_displacement', 'S1_ZT_1.humidity_out', '外部湿度', 'double', 4060, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'humidity_out'),
    ('south_deep_displacement', 'S1_ZT_1.lat', '纬度', 'string', 4070, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'lat'),
    ('south_deep_displacement', 'S1_ZT_1.lon', '经度', 'string', 4080, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'lon'),
    ('south_deep_displacement', 'S1_ZT_1.sensor_state.L1_SW_1', '深部位移测点状态', 'int', 4090, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sensor_state_l1_sw_1'),
    ('south_deep_displacement', 'S1_ZT_1.signal_4g', '4G 信号', 'int', 4100, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'signal_4g'),
    ('south_deep_displacement', 'S1_ZT_1.singal_db', '信号强度 dB', 'int', 4110, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 signal_db 命名，草案继续保留历史 identifier 兼容。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'signal_db'),
    ('south_deep_displacement', 'S1_ZT_1.singal_NB', 'NB 信号', 'int', 4120, '2026-03-25 live gap 补录；TDengine 实库列已确认采用 signal_nb 命名，草案继续保留历史 identifier 兼容。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'signal_nb'),
    ('south_deep_displacement', 'S1_ZT_1.solar_volt', '太阳能电压', 'int', 4130, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'solar_volt'),
    ('south_deep_displacement', 'S1_ZT_1.supply_power', '供电功率', 'int', 4140, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'supply_power'),
    ('south_deep_displacement', 'S1_ZT_1.sw_version', '软件版本', 'string', 4150, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'sw_version'),
    ('south_deep_displacement', 'S1_ZT_1.temp', '温度', 'double', 4160, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'temp'),
    ('south_deep_displacement', 'S1_ZT_1.temp_out', '外部温度', 'double', 4170, '2026-03-25 live gap 补录；父级状态类指标只补占位结构，正式映射需待 TDengine DESCRIBE 核验。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'S1_ZT_1', 's1_zt_1', 'temp_out');

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
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

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
    ('south_deep_displacement', 'dispsX', '顺滑动方向累计变形量', 'double', 10, '现有 property 只补 tdengineLegacy 占位结构；TDengine 实库列已确认采用 disps_x 命名。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'l1_sw_1', 'disps_x'),
    ('south_deep_displacement', 'dispsY', '垂直坡面方向累计变形量', 'double', 20, '现有 property 只补 tdengineLegacy 占位结构；TDengine 实库列已确认采用 disps_y 命名。',
     JSON_OBJECT('tdengineLegacy', JSON_OBJECT('enabled', CAST('false' AS JSON), 'mappingStatus', @mapping_status, 'blockedReason', @blocked_reason)),
     'l1_sw_1', 'disps_y');

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

-- 导出当前真实环境 live 缺口。
-- 用途：
-- 1. 找出 iot_device_property 已出现、但 iot_product_model 尚未建模的 identifier。
-- 2. 找出 iot_product_model 已存在、但仍未补 tdengineLegacy 占位结构的 identifier。
-- 3. 标记该缺口当前是否已被 draft seed / patch 覆盖，便于下一轮只补“未追踪 live gap”。
SELECT
    p.product_key,
    prop.identifier,
    CASE
        WHEN model.id IS NULL THEN 'MISSING_MODEL'
        WHEN JSON_EXTRACT(model.specs_json, '$.tdengineLegacy') IS NULL THEN 'MISSING_TDENGINE_LEGACY'
        ELSE 'COVERED'
    END AS current_gap,
    CASE
        WHEN seed.identifier IS NOT NULL THEN 'DRAFT_SEED'
        WHEN patch.identifier IS NOT NULL THEN 'DRAFT_PATCH'
        ELSE 'UNTRACKED_LIVE_GAP'
    END AS draft_coverage,
    COUNT(*) AS property_rows,
    COUNT(DISTINCT prop.device_id) AS device_count,
    MAX(prop.report_time) AS latest_report_time
FROM iot_device_property prop
JOIN iot_device device
  ON device.id = prop.device_id
 AND device.deleted = 0
JOIN iot_product p
  ON p.id = device.product_id
 AND p.deleted = 0
LEFT JOIN iot_product_model model
  ON model.product_id = p.id
 AND model.model_type = 'property'
 AND model.identifier = prop.identifier
 AND model.deleted = 0
LEFT JOIN tmp_tdengine_mapping_gap_seed seed
  ON seed.product_key = p.product_key
 AND seed.identifier = prop.identifier
LEFT JOIN tmp_tdengine_existing_patch patch
  ON patch.product_key = p.product_key
 AND patch.identifier = prop.identifier
WHERE p.product_key IN (
    'south_rtu',
    'south_multi_displacement',
    'south_gnss_monitor',
    'south_deep_displacement'
)
GROUP BY
    p.product_key,
    prop.identifier,
    current_gap,
    draft_coverage
HAVING current_gap <> 'COVERED'
ORDER BY
    p.product_key,
    draft_coverage,
    prop.identifier;
