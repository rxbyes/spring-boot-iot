CREATE DATABASE IF NOT EXISTS iot;
USE iot;

-- 本文件由 scripts/schema/render_artifacts.py 生成，不要手工编辑。

-- ========================================
-- 对象：iot_agg_measure_hour
-- 类型：tdengine_stable
-- 说明：数值点位小时聚合表
-- 字段字典：
-- - ts: 时序时间
-- - metric_id: 指标ID
-- - metric_code: 指标编码
-- - metric_name: 指标名称
-- - value_type: 值类型
-- - first_reported_at: 首次上报时间
-- - last_reported_at: 最后上报时间
-- - min_value_double: 最小数值
-- - max_value_double: 最大数值
-- - sum_value_double: 累计数值
-- - last_value_double: 最近数值
-- - sample_count: 样本数量
-- - trace_id: 链路追踪ID
-- - source_message_type: 来源消息类型
-- - tenant_id: 租户ID（TAG）
-- - device_id: 设备ID（TAG）
-- - product_id: 产品ID（TAG）
-- - sensor_group: 传感器分组（TAG）
-- - location_code: 位置编码（TAG）
-- - risk_point_id: 风险点ID（TAG）
CREATE STABLE IF NOT EXISTS iot_agg_measure_hour (
  ts TIMESTAMP,
  metric_id BINARY(128),
  metric_code BINARY(128),
  metric_name NCHAR(128),
  value_type BINARY(32),
  first_reported_at TIMESTAMP,
  last_reported_at TIMESTAMP,
  min_value_double DOUBLE,
  max_value_double DOUBLE,
  sum_value_double DOUBLE,
  last_value_double DOUBLE,
  sample_count BIGINT,
  trace_id BINARY(64),
  source_message_type BINARY(32)
)
TAGS (
  tenant_id BIGINT,
  device_id BIGINT,
  product_id BIGINT,
  sensor_group BINARY(64),
  location_code BINARY(64),
  risk_point_id BIGINT
);

-- ========================================
-- 对象：iot_raw_event_point
-- 类型：tdengine_stable
-- 说明：原始事件点位表
-- 字段字典：
-- - ts: 时序时间
-- - metric_id: 指标ID
-- - reported_at: 设备上报时间
-- - ingested_at: 平台入库时间
-- - value_double: 数值
-- - value_long: 整数值
-- - value_bool: 布尔值
-- - value_text: 文本值
-- - quality_code: 质量编码
-- - alarm_flag: 告警标记
-- - trace_id: 链路追踪ID
-- - session_id: 会话ID
-- - source_message_type: 来源消息类型
-- - tenant_id: 租户ID（TAG）
-- - device_id: 设备ID（TAG）
-- - product_id: 产品ID（TAG）
-- - sensor_group: 传感器分组（TAG）
-- - location_code: 位置编码（TAG）
-- - risk_point_id: 风险点ID（TAG）
CREATE STABLE IF NOT EXISTS iot_raw_event_point (
  ts TIMESTAMP,
  metric_id BINARY(128),
  reported_at TIMESTAMP,
  ingested_at TIMESTAMP,
  value_double DOUBLE,
  value_long BIGINT,
  value_bool BOOL,
  value_text NCHAR(1024),
  quality_code BINARY(32),
  alarm_flag BOOL,
  trace_id BINARY(64),
  session_id BINARY(64),
  source_message_type BINARY(32)
)
TAGS (
  tenant_id BIGINT,
  device_id BIGINT,
  product_id BIGINT,
  sensor_group BINARY(64),
  location_code BINARY(64),
  risk_point_id BIGINT
);

-- ========================================
-- 对象：iot_raw_measure_point
-- 类型：tdengine_stable
-- 说明：原始数值点位表
-- 字段字典：
-- - ts: 时序时间
-- - metric_id: 指标ID
-- - reported_at: 设备上报时间
-- - ingested_at: 平台入库时间
-- - value_double: 数值
-- - value_long: 整数值
-- - value_bool: 布尔值
-- - value_text: 文本值
-- - quality_code: 质量编码
-- - alarm_flag: 告警标记
-- - trace_id: 链路追踪ID
-- - session_id: 会话ID
-- - source_message_type: 来源消息类型
-- - tenant_id: 租户ID（TAG）
-- - device_id: 设备ID（TAG）
-- - product_id: 产品ID（TAG）
-- - sensor_group: 传感器分组（TAG）
-- - location_code: 位置编码（TAG）
-- - risk_point_id: 风险点ID（TAG）
CREATE STABLE IF NOT EXISTS iot_raw_measure_point (
  ts TIMESTAMP,
  metric_id BINARY(128),
  reported_at TIMESTAMP,
  ingested_at TIMESTAMP,
  value_double DOUBLE,
  value_long BIGINT,
  value_bool BOOL,
  value_text NCHAR(1024),
  quality_code BINARY(32),
  alarm_flag BOOL,
  trace_id BINARY(64),
  session_id BINARY(64),
  source_message_type BINARY(32)
)
TAGS (
  tenant_id BIGINT,
  device_id BIGINT,
  product_id BIGINT,
  sensor_group BINARY(64),
  location_code BINARY(64),
  risk_point_id BIGINT
);

-- ========================================
-- 对象：iot_raw_status_point
-- 类型：tdengine_stable
-- 说明：原始状态点位表
-- 字段字典：
-- - ts: 时序时间
-- - metric_id: 指标ID
-- - reported_at: 设备上报时间
-- - ingested_at: 平台入库时间
-- - value_double: 数值
-- - value_long: 整数值
-- - value_bool: 布尔值
-- - value_text: 文本值
-- - quality_code: 质量编码
-- - alarm_flag: 告警标记
-- - trace_id: 链路追踪ID
-- - session_id: 会话ID
-- - source_message_type: 来源消息类型
-- - tenant_id: 租户ID（TAG）
-- - device_id: 设备ID（TAG）
-- - product_id: 产品ID（TAG）
-- - sensor_group: 传感器分组（TAG）
-- - location_code: 位置编码（TAG）
-- - risk_point_id: 风险点ID（TAG）
CREATE STABLE IF NOT EXISTS iot_raw_status_point (
  ts TIMESTAMP,
  metric_id BINARY(128),
  reported_at TIMESTAMP,
  ingested_at TIMESTAMP,
  value_double DOUBLE,
  value_long BIGINT,
  value_bool BOOL,
  value_text NCHAR(1024),
  quality_code BINARY(32),
  alarm_flag BOOL,
  trace_id BINARY(64),
  session_id BINARY(64),
  source_message_type BINARY(32)
)
TAGS (
  tenant_id BIGINT,
  device_id BIGINT,
  product_id BIGINT,
  sensor_group BINARY(64),
  location_code BINARY(64),
  risk_point_id BIGINT
);

-- ========================================
-- 对象：iot_device_telemetry_point
-- 类型：tdengine_table
-- 说明：设备时序兼容点位表
-- 字段字典：
-- - ts: 时序时间
-- - reported_at: 设备上报时间
-- - tenant_id: 租户ID
-- - device_id: 设备ID
-- - device_code: 设备编码
-- - product_id: 产品ID
-- - product_key: 产品标识
-- - protocol_code: 协议编码
-- - message_type: 消息类型
-- - mqtt_topic: MQTT主题
-- - trace_id: 链路追踪ID
-- - metric_code: 指标编码
-- - metric_name: 指标名称
-- - value_type: 值类型
-- - value_text: 文本值
-- - value_long: 整数值
-- - value_double: 数值
-- - value_bool: 布尔值
CREATE TABLE IF NOT EXISTS iot_device_telemetry_point (
  ts TIMESTAMP,
  reported_at TIMESTAMP,
  tenant_id BIGINT,
  device_id BIGINT,
  device_code BINARY(128),
  product_id BIGINT,
  product_key BINARY(128),
  protocol_code BINARY(64),
  message_type BINARY(32),
  mqtt_topic BINARY(512),
  trace_id BINARY(64),
  metric_code BINARY(128),
  metric_name NCHAR(128),
  value_type BINARY(32),
  value_text NCHAR(1024),
  value_long BIGINT,
  value_double DOUBLE,
  value_bool BOOL
);
