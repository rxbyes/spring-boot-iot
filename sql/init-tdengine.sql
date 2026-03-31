CREATE DATABASE IF NOT EXISTS iot;
USE iot;

-- ========================================
-- TDengine 初始化基线
-- 项目：spring-boot-iot
-- 目标：
-- 1. 创建 TDengine 数据库 `iot`
-- 2. 初始化 telemetry legacy 兼容回退表
-- 3. 初始化 telemetry v2 raw stable
-- 说明：
-- - 本脚本支持重复执行，不要求先删库
-- - 本脚本不预创建设备子表；子表由运行时按 tenant + device + stream 自动派生
-- - 历史现场自定义 legacy stable 仍由环境侧治理，不在本脚本中创建
-- ========================================

-- ========================================
-- 表：iot_device_telemetry_point
-- 表介绍：
-- - TDengine legacy 兼容路径下的标准化回退表
-- - 一条标准化指标占一行，便于兼容查询和历史迁移
-- - 主要用于 legacy fallback 查询，以及手动迁移到 telemetry v2 raw/latest
-- 字段含义：
-- - ts：TDengine 行键时间，要求同表内足够唯一，避免同时间戳覆盖
-- - reported_at：设备真实上报时间
-- - tenant_id：租户 ID
-- - device_id：设备主键
-- - device_code：设备编码
-- - product_id：产品主键
-- - product_key：产品标识
-- - protocol_code：协议编码
-- - message_type：标准化消息类型，如 property/status/event
-- - mqtt_topic：标准化后的原始 Topic
-- - trace_id：全链路追踪 ID
-- - metric_code：标准化指标编码
-- - metric_name：指标显示名称
-- - value_type：标准化值类型
-- - value_text：文本值
-- - value_long：整型值
-- - value_double：浮点值
-- - value_bool：布尔值
-- ========================================
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

-- ========================================
-- Telemetry v2 raw stable 基线
-- 表介绍：
-- - telemetry v2 正式 raw 时序存储
-- - 运行时按 stream kind 自动派生 child table：
--   tb_m_<tenantId>_<deviceId>
--   tb_s_<tenantId>_<deviceId>
--   tb_e_<tenantId>_<deviceId>
-- 共享字段含义：
-- - ts：行键时间
-- - metric_id：标准化指标 ID，声明为 COMPOSITE KEY，与 ts 共同避免同子表同时间多测点覆盖
-- - reported_at：真实上报时间
-- - ingested_at：进入平台的写入时间
-- - value_double/value_long/value_bool/value_text：按类型拆分后的指标值
-- - quality_code：质量码
-- - alarm_flag：是否为告警相关点位
-- - trace_id：全链路追踪 ID
-- - session_id：message-flow 会话 ID
-- - source_message_type：标准化后的来源消息类型
-- 共享 tags 含义：
-- - tenant_id：租户 ID
-- - device_id：设备主键
-- - product_id：产品主键
-- - sensor_group：传感器分组或逻辑流分组
-- - location_code：位置编码
-- - risk_point_id：绑定的风险点 ID
-- stable 对应关系：
-- - iot_raw_measure_point：测量类指标
-- - iot_raw_status_point：状态类指标
-- - iot_raw_event_point：事件类指标
-- ========================================
CREATE STABLE IF NOT EXISTS iot_raw_measure_point (
    ts TIMESTAMP,
    metric_id BINARY(128) COMPOSITE KEY,
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
) TAGS (
    tenant_id BIGINT,
    device_id BIGINT,
    product_id BIGINT,
    sensor_group BINARY(64),
    location_code BINARY(64),
    risk_point_id BIGINT
);

CREATE STABLE IF NOT EXISTS iot_raw_status_point (
    ts TIMESTAMP,
    metric_id BINARY(128) COMPOSITE KEY,
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
) TAGS (
    tenant_id BIGINT,
    device_id BIGINT,
    product_id BIGINT,
    sensor_group BINARY(64),
    location_code BINARY(64),
    risk_point_id BIGINT
);

CREATE STABLE IF NOT EXISTS iot_raw_event_point (
    ts TIMESTAMP,
    metric_id BINARY(128) COMPOSITE KEY,
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
) TAGS (
    tenant_id BIGINT,
    device_id BIGINT,
    product_id BIGINT,
    sensor_group BINARY(64),
    location_code BINARY(64),
    risk_point_id BIGINT
);
