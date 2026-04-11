#!/usr/bin/env python3
"""Real-environment schema sync for Phase 4 smoke acceptance.

This script aligns known schema gaps in rm_iot without dropping tables.
"""

from __future__ import annotations

import argparse
import os
import sys
from typing import Dict, List, Tuple

import pymysql


CreateSqlMap = Dict[str, str]
ColumnSpecMap = Dict[str, List[Tuple[str, str]]]
IndexSpecMap = Dict[str, List[Tuple[str, str]]]
IndexShapeMap = Dict[Tuple[str, str], Tuple[bool, Tuple[str, ...]]]

GOVERNANCE_REVIEWER_USERNAME = "governance_reviewer"
GOVERNANCE_REVIEWER_ROLE_CODE = "SUPER_ADMIN"
GOVERNANCE_REVIEWER_PREFERRED_ID = 99000001
GOVERNANCE_REVIEWER_PASSWORD_HASH = "$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq"
GOVERNANCE_REVIEWER_NICKNAME = "治理复核专员"
GOVERNANCE_REVIEWER_REAL_NAME = "治理复核专员"
GOVERNANCE_REVIEWER_PHONE = "13800009900"
GOVERNANCE_REVIEWER_EMAIL = "governance-reviewer@ghlzm.com"
GOVERNANCE_REVIEWER_REMARK = "系统级固定治理复核人账号，负责产品契约发布与回滚审批。"
GOVERNANCE_APPROVAL_POLICY_SEEDS = (
    {
        "preferred_id": 99001001,
        "tenant_id": 0,
        "scope_type": "GLOBAL",
        "action_code": "PRODUCT_CONTRACT_RELEASE_APPLY",
        "approver_mode": "FIXED_USER",
        "remark": "产品契约发布固定复核人",
    },
    {
        "preferred_id": 99001002,
        "tenant_id": 0,
        "scope_type": "GLOBAL",
        "action_code": "PRODUCT_CONTRACT_ROLLBACK",
        "approver_mode": "FIXED_USER",
        "remark": "产品契约回滚固定复核人",
    },
)


CREATE_TABLE_SQL: CreateSqlMap = {
    "iot_device_relation": """
CREATE TABLE IF NOT EXISTS iot_device_relation (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    parent_device_id BIGINT NOT NULL COMMENT '父设备ID',
    parent_device_code VARCHAR(64) NOT NULL COMMENT '父设备编码',
    logical_channel_code VARCHAR(64) NOT NULL COMMENT '逻辑通道编码',
    child_device_id BIGINT NOT NULL COMMENT '子设备ID',
    child_device_code VARCHAR(64) NOT NULL COMMENT '子设备编码',
    child_product_id BIGINT DEFAULT NULL COMMENT '子产品ID',
    child_product_key VARCHAR(64) DEFAULT NULL COMMENT '子产品 productKey',
    relation_type VARCHAR(32) NOT NULL COMMENT '关系类型 collector_child/gateway_child',
    canonicalization_strategy VARCHAR(32) NOT NULL COMMENT '归一化策略 LEGACY/LF_VALUE',
    status_mirror_strategy VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '状态镜像策略 NONE/SENSOR_STATE',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_relation_parent_channel (tenant_id, parent_device_id, logical_channel_code, deleted),
    KEY idx_relation_parent_code (tenant_id, parent_device_code, enabled, deleted),
    KEY idx_relation_child_code (tenant_id, child_device_code, enabled, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备逻辑通道关系表'
""",
    "iot_device_online_session": """
CREATE TABLE IF NOT EXISTS iot_device_online_session (
    id BIGINT NOT NULL COMMENT 'pk',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    product_id BIGINT NOT NULL COMMENT 'product id',
    device_id BIGINT NOT NULL COMMENT 'device id',
    device_code VARCHAR(64) NOT NULL COMMENT 'device code',
    online_time DATETIME NOT NULL COMMENT 'online time',
    last_seen_time DATETIME DEFAULT NULL COMMENT 'last seen time',
    offline_time DATETIME DEFAULT NULL COMMENT 'offline time',
    duration_minutes BIGINT DEFAULT NULL COMMENT 'duration minutes',
    remark VARCHAR(500) DEFAULT NULL COMMENT 'remark',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_online_session_device_active (deleted, device_id, offline_time),
    KEY idx_online_session_product_time (deleted, product_id, online_time, offline_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='device online session'
""",
    "iot_device_metric_latest": """
CREATE TABLE IF NOT EXISTS iot_device_metric_latest (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'pk',
    tenant_id BIGINT NOT NULL COMMENT 'tenant id',
    device_id BIGINT NOT NULL COMMENT 'device id',
    product_id BIGINT NOT NULL COMMENT 'product id',
    metric_id VARCHAR(128) NOT NULL COMMENT 'metric id',
    metric_code VARCHAR(128) NOT NULL COMMENT 'metric code',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT 'metric name',
    value_type VARCHAR(32) DEFAULT NULL COMMENT 'value type',
    value_double DOUBLE DEFAULT NULL COMMENT 'double value',
    value_long BIGINT DEFAULT NULL COMMENT 'long value',
    value_bool TINYINT(1) DEFAULT NULL COMMENT 'bool value',
    value_text TEXT DEFAULT NULL COMMENT 'text value',
    quality_code VARCHAR(32) DEFAULT NULL COMMENT 'quality code',
    alarm_flag TINYINT(1) DEFAULT NULL COMMENT 'alarm flag',
    reported_at DATETIME DEFAULT NULL COMMENT 'reported at',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'trace id',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tel_latest_tenant_device_metric (tenant_id, device_id, metric_id),
    KEY idx_tel_latest_device_reported (device_id, reported_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='telemetry v2 latest projection'
""",
    "iot_normative_metric_definition": """
CREATE TABLE IF NOT EXISTS iot_normative_metric_definition (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    scenario_code VARCHAR(64) NOT NULL COMMENT '治理场景编码',
    device_family VARCHAR(64) NOT NULL COMMENT '设备族编码',
    identifier VARCHAR(64) NOT NULL COMMENT '规范字段标识',
    display_name VARCHAR(128) NOT NULL COMMENT '规范字段名称',
    unit VARCHAR(32) DEFAULT NULL COMMENT '单位',
    precision_digits INT DEFAULT NULL COMMENT '精度',
    monitor_content_code VARCHAR(32) DEFAULT NULL COMMENT '监测内容编码',
    monitor_type_code VARCHAR(32) DEFAULT NULL COMMENT '监测类型编码',
    risk_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许进入风险闭环',
    trend_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许趋势分析',
    metric_dimension VARCHAR(64) DEFAULT NULL COMMENT '量纲',
    threshold_type VARCHAR(32) DEFAULT NULL COMMENT '阈值类型',
    semantic_direction VARCHAR(32) DEFAULT NULL COMMENT '语义方向',
    gis_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持GIS',
    insight_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持对象洞察',
    analytics_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持运营分析',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
    metadata_json JSON DEFAULT NULL COMMENT '扩展元数据',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_normative_metric_scenario_identifier (scenario_code, identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规范字段定义表'
""",
    "iot_vendor_metric_evidence": """
CREATE TABLE IF NOT EXISTS iot_vendor_metric_evidence (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    parent_device_code VARCHAR(64) DEFAULT NULL COMMENT '父设备编码',
    child_device_code VARCHAR(64) DEFAULT NULL COMMENT '子设备编码',
    raw_identifier VARCHAR(128) NOT NULL COMMENT '原始字段标识',
    canonical_identifier VARCHAR(64) DEFAULT NULL COMMENT '建议规范字段标识',
    logical_channel_code VARCHAR(64) DEFAULT NULL COMMENT '逻辑通道编码',
    evidence_origin VARCHAR(32) NOT NULL COMMENT '证据来源',
    sample_value VARCHAR(255) DEFAULT NULL COMMENT '样例值',
    value_type VARCHAR(32) DEFAULT NULL COMMENT '值类型',
    evidence_count INT NOT NULL DEFAULT 0 COMMENT '命中次数',
    last_seen_time DATETIME DEFAULT NULL COMMENT '最后出现时间',
    metadata_json JSON DEFAULT NULL COMMENT '扩展元数据',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vendor_metric_evidence (product_id, raw_identifier, logical_channel_code),
    KEY idx_vendor_metric_product_seen (product_id, last_seen_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂商字段证据表'
""",
    "iot_vendor_metric_mapping_rule": """
CREATE TABLE IF NOT EXISTS iot_vendor_metric_mapping_rule (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    scope_type VARCHAR(32) NOT NULL COMMENT 'PRODUCT/PROTOCOL/SCENARIO',
    product_id BIGINT DEFAULT NULL COMMENT '产品ID',
    protocol_code VARCHAR(64) DEFAULT NULL COMMENT '协议编码',
    scenario_code VARCHAR(64) DEFAULT NULL COMMENT '治理场景编码',
    device_family VARCHAR(64) DEFAULT NULL COMMENT '设备族编码',
    raw_identifier VARCHAR(128) NOT NULL COMMENT '原始字段标识',
    logical_channel_code VARCHAR(64) DEFAULT NULL COMMENT '逻辑通道编码',
    relation_condition_json JSON DEFAULT NULL COMMENT '关系条件JSON',
    normalization_rule_json JSON DEFAULT NULL COMMENT '归一化规则JSON',
    target_normative_identifier VARCHAR(64) NOT NULL COMMENT '目标规范字段标识',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
    approval_order_id BIGINT DEFAULT NULL COMMENT '审批单ID',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂商字段映射规则表'
""",
    "iot_product_contract_release_batch": """
CREATE TABLE IF NOT EXISTS iot_product_contract_release_batch (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    scenario_code VARCHAR(64) NOT NULL COMMENT '治理场景编码',
    release_source VARCHAR(64) NOT NULL COMMENT '发布来源',
    released_field_count INT NOT NULL DEFAULT 0 COMMENT '发布字段数',
    approval_order_id BIGINT DEFAULT NULL COMMENT '审批单ID',
    release_reason VARCHAR(500) DEFAULT NULL COMMENT '发布说明',
    release_status VARCHAR(16) NOT NULL DEFAULT 'RELEASED' COMMENT 'RELEASED/ROLLED_BACK',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rollback_by BIGINT DEFAULT NULL COMMENT '回滚执行人',
    rollback_time DATETIME DEFAULT NULL COMMENT '回滚时间',
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_product_contract_release_product_time (product_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='契约发布批次表'
""",
    "iot_product_contract_release_snapshot": """
CREATE TABLE IF NOT EXISTS iot_product_contract_release_snapshot (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    batch_id BIGINT NOT NULL COMMENT '发布批次ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    snapshot_stage VARCHAR(32) NOT NULL COMMENT '快照阶段',
    snapshot_json JSON NOT NULL COMMENT '快照载荷',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_release_snapshot_batch_stage (batch_id, snapshot_stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='契约发布快照表'
""",
    "iot_device_secret_rotation_log": """
CREATE TABLE IF NOT EXISTS iot_device_secret_rotation_log (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    device_code VARCHAR(64) NOT NULL COMMENT '设备编码',
    product_key VARCHAR(64) DEFAULT NULL COMMENT '产品key',
    rotation_batch_id VARCHAR(64) NOT NULL COMMENT '轮换批次ID',
    reason VARCHAR(500) DEFAULT NULL COMMENT '轮换原因',
    previous_secret_digest VARCHAR(128) DEFAULT NULL COMMENT '旧密钥摘要',
    current_secret_digest VARCHAR(128) DEFAULT NULL COMMENT '新密钥摘要',
    rotated_by BIGINT NOT NULL COMMENT '执行人',
    approved_by BIGINT NOT NULL COMMENT '复核人',
    rotate_time DATETIME NOT NULL COMMENT '轮换时间',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_device_rotation_device_time (device_id, rotate_time),
    KEY idx_device_rotation_batch (rotation_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备密钥轮换日志表'
""",
    "iot_device_access_error_log": """
CREATE TABLE IF NOT EXISTS iot_device_access_error_log (
    id BIGINT NOT NULL COMMENT 'pk',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'trace id',
    protocol_code VARCHAR(64) DEFAULT NULL COMMENT 'protocol code',
    request_method VARCHAR(16) DEFAULT NULL COMMENT 'request method',
    failure_stage VARCHAR(32) DEFAULT NULL COMMENT 'failure stage',
    device_code VARCHAR(64) DEFAULT NULL COMMENT 'device code',
    product_key VARCHAR(64) DEFAULT NULL COMMENT 'product key',
    gateway_device_code VARCHAR(64) DEFAULT NULL COMMENT 'gateway device code',
    sub_device_code VARCHAR(64) DEFAULT NULL COMMENT 'sub device code',
    topic_route_type VARCHAR(32) DEFAULT NULL COMMENT 'topic route type',
    message_type VARCHAR(32) DEFAULT NULL COMMENT 'message type',
    topic VARCHAR(255) DEFAULT NULL COMMENT 'topic',
    client_id VARCHAR(128) DEFAULT NULL COMMENT 'client id',
    payload_size INT DEFAULT NULL COMMENT 'payload size',
    payload_encoding VARCHAR(16) DEFAULT NULL COMMENT 'payload encoding',
    payload_truncated TINYINT NOT NULL DEFAULT 0 COMMENT 'payload truncated',
    raw_payload LONGTEXT DEFAULT NULL COMMENT 'raw payload',
    error_code VARCHAR(64) DEFAULT NULL COMMENT 'error code',
    exception_class VARCHAR(255) DEFAULT NULL COMMENT 'exception class',
    error_message VARCHAR(500) DEFAULT NULL COMMENT 'error message',
    contract_snapshot LONGTEXT DEFAULT NULL COMMENT 'contract snapshot',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_access_error_trace (trace_id),
    KEY idx_access_error_device_time (device_code, create_time),
    KEY idx_access_error_stage_time (failure_stage, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='device access error archive'
""",
    "iot_device_invalid_report_state": """
CREATE TABLE IF NOT EXISTS iot_device_invalid_report_state (
    id BIGINT NOT NULL COMMENT 'pk',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    governance_key VARCHAR(255) NOT NULL COMMENT 'governance key',
    reason_code VARCHAR(64) NOT NULL COMMENT 'reason code',
    request_method VARCHAR(16) DEFAULT NULL COMMENT 'request method',
    failure_stage VARCHAR(32) DEFAULT NULL COMMENT 'failure stage',
    device_code VARCHAR(64) DEFAULT NULL COMMENT 'device code',
    product_key VARCHAR(64) DEFAULT NULL COMMENT 'product key',
    protocol_code VARCHAR(64) DEFAULT NULL COMMENT 'protocol code',
    topic_route_type VARCHAR(32) DEFAULT NULL COMMENT 'topic route type',
    topic VARCHAR(255) DEFAULT NULL COMMENT 'topic',
    client_id VARCHAR(128) DEFAULT NULL COMMENT 'client id',
    payload_size INT DEFAULT NULL COMMENT 'payload size',
    payload_encoding VARCHAR(16) DEFAULT NULL COMMENT 'payload encoding',
    last_payload LONGTEXT DEFAULT NULL COMMENT 'last payload',
    last_trace_id VARCHAR(64) DEFAULT NULL COMMENT 'last trace id',
    sample_error_message VARCHAR(500) DEFAULT NULL COMMENT 'sample error message',
    sample_exception_class VARCHAR(255) DEFAULT NULL COMMENT 'sample exception class',
    first_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'first seen time',
    last_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'last seen time',
    hit_count BIGINT NOT NULL DEFAULT 0 COMMENT 'hit count',
    sampled_count BIGINT NOT NULL DEFAULT 0 COMMENT 'sampled count',
    suppressed_count BIGINT NOT NULL DEFAULT 0 COMMENT 'suppressed count',
    suppressed_until DATETIME DEFAULT NULL COMMENT 'suppressed until',
    resolved TINYINT NOT NULL DEFAULT 0 COMMENT 'resolved',
    resolved_time DATETIME DEFAULT NULL COMMENT 'resolved time',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invalid_report_state_governance_key (governance_key),
    KEY idx_invalid_report_device_resolved (device_code, product_key, resolved, last_seen_time),
    KEY idx_invalid_report_reason_time (reason_code, last_seen_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='invalid mqtt report latest state'
""",
    "risk_point_device": """
CREATE TABLE IF NOT EXISTS risk_point_device (
    id BIGINT NOT NULL COMMENT 'pk',
    risk_point_id BIGINT DEFAULT NULL COMMENT 'risk point id',
    device_id BIGINT DEFAULT NULL COMMENT 'device id',
    device_code VARCHAR(64) DEFAULT NULL COMMENT 'device code',
    device_name VARCHAR(128) DEFAULT NULL COMMENT 'device name',
    metric_identifier VARCHAR(64) DEFAULT NULL COMMENT 'metric identifier',
    metric_name VARCHAR(64) DEFAULT NULL COMMENT 'metric name',
    default_threshold VARCHAR(64) DEFAULT NULL COMMENT 'default threshold',
    threshold_unit VARCHAR(20) DEFAULT NULL COMMENT 'threshold unit',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_risk_device (risk_point_id, device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='risk point device'
""",
    "risk_point_device_pending_binding": """
CREATE TABLE IF NOT EXISTS risk_point_device_pending_binding (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'pk',
    batch_no VARCHAR(64) NOT NULL COMMENT 'import batch no',
    source_file_name VARCHAR(255) DEFAULT NULL COMMENT 'source file name',
    source_row_no INT NOT NULL COMMENT 'source row no',
    risk_point_name VARCHAR(128) NOT NULL COMMENT 'source risk point name',
    risk_point_id BIGINT DEFAULT NULL COMMENT 'resolved risk point id',
    risk_point_code VARCHAR(64) DEFAULT NULL COMMENT 'resolved risk point code',
    device_code VARCHAR(64) NOT NULL COMMENT 'source device code',
    device_id BIGINT DEFAULT NULL COMMENT 'resolved device id',
    device_name VARCHAR(128) DEFAULT NULL COMMENT 'resolved device name',
    resolution_status VARCHAR(64) NOT NULL DEFAULT 'PENDING_METRIC_GOVERNANCE' COMMENT 'resolution status',
    resolution_note VARCHAR(500) DEFAULT NULL COMMENT 'resolution note',
    metric_identifier VARCHAR(64) DEFAULT NULL COMMENT 'future metric identifier',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT 'future metric name',
    promoted_binding_id BIGINT DEFAULT NULL COMMENT 'promoted binding id',
    promoted_time DATETIME DEFAULT NULL COMMENT 'promoted time',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pending_binding_batch_row (tenant_id, batch_no, source_row_no),
    KEY idx_pending_binding_status (tenant_id, resolution_status, deleted),
    KEY idx_pending_binding_risk_device (risk_point_id, device_id, deleted),
    KEY idx_pending_binding_device_code (tenant_id, device_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='risk point device pending binding'
""",
    "risk_point_device_pending_promotion": """
CREATE TABLE IF NOT EXISTS risk_point_device_pending_promotion (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    pending_binding_id BIGINT NOT NULL COMMENT '来源待治理记录ID',
    risk_point_device_id BIGINT DEFAULT NULL COMMENT '正式绑定ID',
    risk_point_id BIGINT NOT NULL COMMENT '风险点ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    device_code VARCHAR(64) NOT NULL COMMENT '设备编码',
    device_name VARCHAR(128) DEFAULT NULL COMMENT '设备名称',
    metric_identifier VARCHAR(64) NOT NULL COMMENT '测点标识',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT '测点名称',
    promotion_status VARCHAR(32) NOT NULL COMMENT '转正结果',
    recommendation_level VARCHAR(16) DEFAULT NULL COMMENT '推荐等级',
    recommendation_score INT DEFAULT NULL COMMENT '推荐评分',
    evidence_snapshot_json JSON DEFAULT NULL COMMENT '证据快照',
    promotion_note VARCHAR(500) DEFAULT NULL COMMENT '治理说明',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(128) DEFAULT NULL COMMENT '操作人姓名',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_pending_promotion_pending_id (pending_binding_id),
    KEY idx_pending_promotion_binding_id (risk_point_device_id),
    KEY idx_pending_promotion_status (tenant_id, promotion_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险点设备待治理转正明细表'
""",
    "risk_metric_catalog": """
CREATE TABLE IF NOT EXISTS risk_metric_catalog (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    release_batch_id BIGINT DEFAULT NULL COMMENT '来源发布批次',
    product_model_id BIGINT DEFAULT NULL COMMENT '合同字段ID',
    normative_identifier VARCHAR(64) DEFAULT NULL COMMENT '来源规范字段标识',
    contract_identifier VARCHAR(64) NOT NULL COMMENT '合同字段标识',
    risk_metric_code VARCHAR(64) NOT NULL COMMENT '风险指标编码',
    risk_metric_name VARCHAR(128) NOT NULL COMMENT '风险指标名称',
    risk_category VARCHAR(64) DEFAULT NULL COMMENT '风险指标类别',
    metric_role VARCHAR(32) DEFAULT NULL COMMENT '指标角色',
    lifecycle_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/RETIRED',
    source_scenario_code VARCHAR(64) DEFAULT NULL COMMENT '来源场景编码',
    metric_unit VARCHAR(32) DEFAULT NULL COMMENT '指标单位',
    metric_dimension VARCHAR(64) DEFAULT NULL COMMENT '指标量纲',
    threshold_type VARCHAR(32) DEFAULT NULL COMMENT '阈值类型',
    semantic_direction VARCHAR(32) DEFAULT NULL COMMENT '语义方向',
    threshold_direction VARCHAR(32) DEFAULT NULL COMMENT '阈值方向',
    trend_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持趋势分析',
    gis_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否用于GIS',
    insight_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否用于对象洞察',
    analytics_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否用于运营分析',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_metric_catalog (product_id, contract_identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险指标目录表'
""",
    "risk_metric_linkage_binding": """
CREATE TABLE IF NOT EXISTS risk_metric_linkage_binding (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    risk_metric_id BIGINT NOT NULL COMMENT '风险指标ID',
    linkage_rule_id BIGINT NOT NULL COMMENT '联动规则ID',
    binding_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    binding_origin VARCHAR(32) NOT NULL DEFAULT 'AUTO_INFERRED' COMMENT 'AUTO_INFERRED/MANUAL_CONFIRMED/BACKFILL',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_metric_linkage_active (tenant_id, risk_metric_id, linkage_rule_id, deleted),
    KEY idx_risk_metric_linkage_rule (linkage_rule_id, binding_status, deleted),
    KEY idx_risk_metric_linkage_metric (risk_metric_id, binding_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险指标与联动规则绑定表'
""",
    "risk_metric_emergency_plan_binding": """
CREATE TABLE IF NOT EXISTS risk_metric_emergency_plan_binding (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    risk_metric_id BIGINT NOT NULL COMMENT '风险指标ID',
    emergency_plan_id BIGINT NOT NULL COMMENT '应急预案ID',
    binding_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    binding_origin VARCHAR(32) NOT NULL DEFAULT 'AUTO_INFERRED' COMMENT 'AUTO_INFERRED/MANUAL_CONFIRMED/BACKFILL',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_metric_plan_active (tenant_id, risk_metric_id, emergency_plan_id, deleted),
    KEY idx_risk_metric_plan_rule (emergency_plan_id, binding_status, deleted),
    KEY idx_risk_metric_plan_metric (risk_metric_id, binding_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险指标与应急预案绑定表'
""",
    "sys_governance_approval_order": """
CREATE TABLE IF NOT EXISTS sys_governance_approval_order (
    id BIGINT NOT NULL COMMENT 'approval order id',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    action_code VARCHAR(64) NOT NULL COMMENT 'approval action code',
    action_name VARCHAR(128) DEFAULT NULL COMMENT 'approval action name',
    subject_type VARCHAR(64) DEFAULT NULL COMMENT 'approval subject type',
    subject_id BIGINT DEFAULT NULL COMMENT 'approval subject id',
    status VARCHAR(32) NOT NULL COMMENT 'approval status',
    operator_user_id BIGINT NOT NULL COMMENT 'operator user id',
    approver_user_id BIGINT NOT NULL COMMENT 'approver user id',
    payload_json LONGTEXT DEFAULT NULL COMMENT 'approval payload',
    approval_comment VARCHAR(500) DEFAULT NULL COMMENT 'approval comment',
    approved_time DATETIME DEFAULT NULL COMMENT 'approved time',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_governance_approval_order_subject (subject_type, subject_id, deleted),
    KEY idx_governance_approval_order_status_time (status, create_time, deleted),
    KEY idx_governance_approval_order_operator (operator_user_id, approver_user_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='governance approval order'
""",
    "sys_governance_approval_transition": """
CREATE TABLE IF NOT EXISTS sys_governance_approval_transition (
    id BIGINT NOT NULL COMMENT 'approval transition id',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    order_id BIGINT NOT NULL COMMENT 'approval order id',
    from_status VARCHAR(32) DEFAULT NULL COMMENT 'from status',
    to_status VARCHAR(32) NOT NULL COMMENT 'to status',
    actor_user_id BIGINT NOT NULL COMMENT 'actor user id',
    transition_comment VARCHAR(500) DEFAULT NULL COMMENT 'transition comment',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_governance_approval_transition_order (order_id, create_time, deleted),
    KEY idx_governance_approval_transition_actor (actor_user_id, create_time, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='governance approval transition'
""",
    "sys_governance_approval_policy": """
CREATE TABLE IF NOT EXISTS sys_governance_approval_policy (
    id BIGINT NOT NULL COMMENT 'approval policy id',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT 'tenant id, 0 means global',
    scope_type VARCHAR(16) NOT NULL COMMENT 'GLOBAL/TENANT',
    action_code VARCHAR(64) NOT NULL COMMENT 'approval action code',
    approver_mode VARCHAR(32) NOT NULL COMMENT 'FIXED_USER',
    approver_user_id BIGINT NOT NULL COMMENT 'fixed approver user id',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT 'enabled',
    remark VARCHAR(255) DEFAULT NULL COMMENT 'remark',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    UNIQUE KEY uk_governance_approval_policy_scope_action (scope_type, tenant_id, action_code, deleted),
    KEY idx_governance_approval_policy_enabled (enabled, scope_type, tenant_id, action_code, deleted),
    KEY idx_governance_approval_policy_approver (approver_user_id, enabled, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='governance approval policy'
""",
    "iot_governance_work_item": """
CREATE TABLE IF NOT EXISTS iot_governance_work_item (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    work_item_code VARCHAR(64) NOT NULL COMMENT '工作项编码',
    subject_type VARCHAR(64) NOT NULL COMMENT '主题类型 PRODUCT/RISK_METRIC/RELEASE_BATCH/REPLAY_CASE',
    subject_id BIGINT NOT NULL COMMENT '主题ID',
    product_id BIGINT DEFAULT NULL COMMENT '产品ID',
    risk_metric_id BIGINT DEFAULT NULL COMMENT '风险指标ID',
    release_batch_id BIGINT DEFAULT NULL COMMENT '发布批次ID',
    approval_order_id BIGINT DEFAULT NULL COMMENT '审批单ID',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
    product_key VARCHAR(64) DEFAULT NULL COMMENT '产品Key',
    work_status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/ACKED/BLOCKED/RESOLVED/CLOSED/CANCELLED',
    priority_level VARCHAR(16) NOT NULL DEFAULT 'P2' COMMENT 'P1/P2/P3',
    assignee_user_id BIGINT DEFAULT NULL COMMENT '责任人',
    source_stage VARCHAR(64) DEFAULT NULL COMMENT '来源阶段',
    blocking_reason VARCHAR(255) DEFAULT NULL COMMENT '阻塞原因',
    snapshot_json JSON DEFAULT NULL COMMENT '上下文快照',
    task_category VARCHAR(64) NOT NULL DEFAULT 'PRODUCT_GOVERNANCE' COMMENT '任务分类',
    domain_code VARCHAR(64) NOT NULL DEFAULT 'PRODUCT' COMMENT '领域编码',
    action_code VARCHAR(64) NOT NULL DEFAULT 'PRODUCT_GOVERNANCE_REVIEW' COMMENT '动作编码',
    execution_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_APPROVAL' COMMENT '执行状态',
    recommendation_snapshot_json JSON DEFAULT NULL COMMENT '建议快照',
    evidence_snapshot_json JSON DEFAULT NULL COMMENT '证据快照',
    impact_snapshot_json JSON DEFAULT NULL COMMENT '影响快照',
    rollback_snapshot_json JSON DEFAULT NULL COMMENT '回滚快照',
    due_time DATETIME DEFAULT NULL COMMENT '截止时间',
    resolved_time DATETIME DEFAULT NULL COMMENT '解决时间',
    closed_time DATETIME DEFAULT NULL COMMENT '关闭时间',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_governance_work_item_subject (subject_type, subject_id, work_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='治理与运营工作项表'
""",
    "iot_governance_ops_alert": """
CREATE TABLE IF NOT EXISTS iot_governance_ops_alert (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    alert_type VARCHAR(64) NOT NULL COMMENT '告警类型',
    alert_code VARCHAR(128) NOT NULL COMMENT '告警业务编码',
    subject_type VARCHAR(64) NOT NULL COMMENT '主题类型 PRODUCT/RISK_METRIC/RELEASE_BATCH/DEVICE',
    subject_id BIGINT DEFAULT NULL COMMENT '主题ID',
    product_id BIGINT DEFAULT NULL COMMENT '产品ID',
    risk_metric_id BIGINT DEFAULT NULL COMMENT '风险指标ID',
    release_batch_id BIGINT DEFAULT NULL COMMENT '发布批次ID',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
    product_key VARCHAR(64) DEFAULT NULL COMMENT '产品Key',
    alert_status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/ACKED/SUPPRESSED/RESOLVED/CLOSED',
    severity_level VARCHAR(16) NOT NULL DEFAULT 'WARN' COMMENT 'INFO/WARN/CRITICAL',
    affected_count BIGINT NOT NULL DEFAULT 0 COMMENT '影响数量',
    alert_title VARCHAR(255) NOT NULL COMMENT '告警标题',
    alert_message VARCHAR(1000) DEFAULT NULL COMMENT '告警内容',
    dimension_key VARCHAR(128) DEFAULT NULL COMMENT '维度键',
    dimension_label VARCHAR(255) DEFAULT NULL COMMENT '维度标签',
    source_stage VARCHAR(64) DEFAULT NULL COMMENT '来源阶段',
    snapshot_json JSON DEFAULT NULL COMMENT '上下文快照',
    assignee_user_id BIGINT DEFAULT NULL COMMENT '责任人',
    first_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次出现时间',
    last_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近出现时间',
    resolved_time DATETIME DEFAULT NULL COMMENT '恢复时间',
    closed_time DATETIME DEFAULT NULL COMMENT '关闭时间',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_governance_ops_alert_code (tenant_id, alert_type, alert_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='治理运维告警表'
""",
    "sys_notification_channel": """
CREATE TABLE IF NOT EXISTS sys_notification_channel (
    id BIGINT NOT NULL COMMENT 'pk',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    channel_name VARCHAR(128) DEFAULT NULL COMMENT 'channel name',
    channel_code VARCHAR(64) DEFAULT NULL COMMENT 'channel code',
    channel_type VARCHAR(32) DEFAULT NULL COMMENT 'channel type',
    config LONGTEXT DEFAULT NULL COMMENT 'channel config',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'status',
    sort_no INT NOT NULL DEFAULT 0 COMMENT 'sort',
    remark VARCHAR(500) DEFAULT NULL COMMENT 'remark',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_channel_code (channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys notification channel'
""",
}

VIEW_SQL: CreateSqlMap = {
    "iot_message_log": """
CREATE OR REPLACE VIEW iot_message_log AS
SELECT
    id,
    tenant_id,
    device_id,
    product_id,
    trace_id,
    device_code,
    product_key,
    message_type,
    topic,
    payload,
    report_time,
    create_time
FROM iot_device_message_log
"""
}


COLUMNS_TO_ADD: ColumnSpecMap = {
    "iot_product": [
        ("metadata_json", "JSON DEFAULT NULL COMMENT '产品扩展元数据'"),
    ],
    "iot_normative_metric_definition": [
        ("metric_dimension", "VARCHAR(64) DEFAULT NULL COMMENT '量纲'"),
        ("threshold_type", "VARCHAR(32) DEFAULT NULL COMMENT '阈值类型'"),
        ("semantic_direction", "VARCHAR(32) DEFAULT NULL COMMENT '语义方向'"),
        ("gis_enabled", "TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持GIS'"),
        ("insight_enabled", "TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持对象洞察'"),
        ("analytics_enabled", "TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持运营分析'"),
        ("status", "VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态'"),
        ("version_no", "INT NOT NULL DEFAULT 1 COMMENT '版本号'"),
    ],
    "iot_device": [
        ("org_id", "BIGINT DEFAULT NULL COMMENT 'organization id' AFTER `tenant_id`"),
        ("org_name", "VARCHAR(128) DEFAULT NULL COMMENT 'organization name' AFTER `org_id`"),
    ],
    "iot_device_access_error_log": [
        ("contract_snapshot", "LONGTEXT DEFAULT NULL COMMENT 'contract snapshot'"),
    ],
    "iot_product_contract_release_batch": [
        ("approval_order_id", "BIGINT DEFAULT NULL COMMENT 'approval order id'"),
        ("release_reason", "VARCHAR(500) DEFAULT NULL COMMENT 'release reason'"),
        ("release_status", "VARCHAR(16) NOT NULL DEFAULT 'RELEASED' COMMENT 'RELEASED/ROLLED_BACK'"),
        ("rollback_by", "BIGINT DEFAULT NULL COMMENT 'rollback operator user id'"),
        ("rollback_time", "DATETIME DEFAULT NULL COMMENT 'rollback time'"),
    ],
    "iot_governance_work_item": [
        ("task_category", "VARCHAR(64) NOT NULL DEFAULT 'PRODUCT_GOVERNANCE' COMMENT 'task category'"),
        ("domain_code", "VARCHAR(64) NOT NULL DEFAULT 'PRODUCT' COMMENT 'domain code'"),
        ("action_code", "VARCHAR(64) NOT NULL DEFAULT 'PRODUCT_GOVERNANCE_REVIEW' COMMENT 'action code'"),
        ("execution_status", "VARCHAR(32) NOT NULL DEFAULT 'PENDING_APPROVAL' COMMENT 'execution status'"),
        ("recommendation_snapshot_json", "JSON DEFAULT NULL COMMENT 'recommendation snapshot json'"),
        ("evidence_snapshot_json", "JSON DEFAULT NULL COMMENT 'evidence snapshot json'"),
        ("impact_snapshot_json", "JSON DEFAULT NULL COMMENT 'impact snapshot json'"),
        ("rollback_snapshot_json", "JSON DEFAULT NULL COMMENT 'rollback snapshot json'"),
    ],
    "iot_command_record": [
        ("device_code", "VARCHAR(64) DEFAULT NULL COMMENT 'device code'"),
        ("product_key", "VARCHAR(64) DEFAULT NULL COMMENT 'product key'"),
        ("topic", "VARCHAR(255) DEFAULT NULL COMMENT 'topic'"),
        ("request_payload", "LONGTEXT DEFAULT NULL COMMENT 'request payload'"),
        ("qos", "TINYINT NOT NULL DEFAULT 0 COMMENT 'mqtt qos'"),
        ("retained", "TINYINT NOT NULL DEFAULT 0 COMMENT 'retained'"),
        ("status", "VARCHAR(32) DEFAULT NULL COMMENT 'status'"),
        ("command_id", "VARCHAR(64) DEFAULT NULL COMMENT 'business command id'"),
        ("remark", "VARCHAR(500) DEFAULT NULL COMMENT 'remark'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
        ("deleted", "TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted'"),
    ],
    "iot_alarm_record": [
        ("remark", "VARCHAR(500) DEFAULT NULL COMMENT 'remark'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "iot_event_record": [
        ("remark", "VARCHAR(500) DEFAULT NULL COMMENT 'remark'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "iot_event_work_order": [
        ("remark", "VARCHAR(500) DEFAULT NULL COMMENT 'remark'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "risk_point": [
        ("org_id", "BIGINT DEFAULT NULL COMMENT 'organization id'"),
        ("org_name", "VARCHAR(128) DEFAULT NULL COMMENT 'organization name'"),
        ("risk_point_level", "VARCHAR(16) DEFAULT NULL COMMENT 'archive risk point level'"),
        ("current_risk_level", "VARCHAR(16) DEFAULT NULL COMMENT 'current risk level'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "risk_metric_catalog": [
        ("release_batch_id", "BIGINT DEFAULT NULL COMMENT 'source release batch id'"),
        ("normative_identifier", "VARCHAR(64) DEFAULT NULL COMMENT 'normative identifier'"),
        ("risk_category", "VARCHAR(64) DEFAULT NULL COMMENT 'risk category'"),
        ("metric_role", "VARCHAR(32) DEFAULT NULL COMMENT 'metric role'"),
        ("lifecycle_status", "VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/RETIRED'"),
        ("source_scenario_code", "VARCHAR(64) DEFAULT NULL COMMENT 'source scenario code'"),
        ("metric_unit", "VARCHAR(32) DEFAULT NULL COMMENT 'metric unit'"),
        ("metric_dimension", "VARCHAR(64) DEFAULT NULL COMMENT 'metric dimension'"),
        ("threshold_type", "VARCHAR(32) DEFAULT NULL COMMENT 'threshold type'"),
        ("semantic_direction", "VARCHAR(32) DEFAULT NULL COMMENT 'semantic direction'"),
        ("threshold_direction", "VARCHAR(32) DEFAULT NULL COMMENT 'threshold direction'"),
        ("trend_enabled", "TINYINT NOT NULL DEFAULT 0 COMMENT 'trend enabled flag'"),
        ("gis_enabled", "TINYINT NOT NULL DEFAULT 0 COMMENT 'GIS enabled flag'"),
        ("insight_enabled", "TINYINT NOT NULL DEFAULT 0 COMMENT 'Insight enabled flag'"),
        ("analytics_enabled", "TINYINT NOT NULL DEFAULT 0 COMMENT 'Analytics enabled flag'"),
    ],
    "risk_point_device": [
        ("risk_metric_id", "BIGINT DEFAULT NULL COMMENT '风险指标ID'"),
    ],
    "rule_definition": [
        ("risk_metric_id", "BIGINT DEFAULT NULL COMMENT '风险指标ID'"),
        ("metric_name", "VARCHAR(64) DEFAULT NULL COMMENT 'metric name'"),
        ("expression", "VARCHAR(256) DEFAULT NULL COMMENT 'expression'"),
        ("notification_methods", "VARCHAR(64) DEFAULT NULL COMMENT 'notification methods'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "linkage_rule": [
        ("description", "VARCHAR(512) DEFAULT NULL COMMENT 'description'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "emergency_plan": [
        ("alarm_level", "VARCHAR(16) DEFAULT NULL COMMENT 'alarm level'"),
        ("description", "VARCHAR(512) DEFAULT NULL COMMENT 'description'"),
        ("response_steps", "LONGTEXT DEFAULT NULL COMMENT 'response steps'"),
        ("contact_list", "LONGTEXT DEFAULT NULL COMMENT 'contact list'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "sys_organization": [
        ("org_type", "VARCHAR(32) DEFAULT NULL COMMENT 'org type'"),
        ("leader_user_id", "BIGINT DEFAULT NULL COMMENT 'leader user id'"),
        ("leader_name", "VARCHAR(64) DEFAULT NULL COMMENT 'leader name'"),
    ],
    "sys_role": [
        ("description", "VARCHAR(500) DEFAULT NULL COMMENT 'description'"),
    ],
    "sys_menu": [
        ("menu_code", "VARCHAR(100) DEFAULT NULL COMMENT 'menu code'"),
        ("path", "VARCHAR(255) DEFAULT NULL COMMENT 'route path'"),
        ("meta_json", "LONGTEXT DEFAULT NULL COMMENT 'meta json'"),
        ("sort", "INT DEFAULT 0 COMMENT 'sort'"),
        ("type", "TINYINT DEFAULT NULL COMMENT 'menu type'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "sys_audit_log": [
        ("user_name", "VARCHAR(64) DEFAULT NULL COMMENT 'user name'"),
        ("trace_id", "VARCHAR(64) DEFAULT NULL COMMENT 'trace id'"),
        ("device_code", "VARCHAR(64) DEFAULT NULL COMMENT 'device code'"),
        ("product_key", "VARCHAR(64) DEFAULT NULL COMMENT 'product key'"),
        ("request_url", "VARCHAR(255) DEFAULT NULL COMMENT 'request url'"),
        ("location", "VARCHAR(128) DEFAULT NULL COMMENT 'location'"),
        ("operation_result", "TINYINT DEFAULT NULL COMMENT 'operation result'"),
        ("result_message", "VARCHAR(500) DEFAULT NULL COMMENT 'result message'"),
        ("error_code", "VARCHAR(64) DEFAULT NULL COMMENT 'error code'"),
        ("exception_class", "VARCHAR(255) DEFAULT NULL COMMENT 'exception class'"),
        ("operation_time", "DATETIME DEFAULT NULL COMMENT 'operation time'"),
        ("deleted", "TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted'"),
    ],
    "iot_device_message_log": [
        ("trace_id", "VARCHAR(64) DEFAULT NULL COMMENT 'trace id'"),
        ("device_code", "VARCHAR(64) DEFAULT NULL COMMENT 'device code'"),
        ("product_key", "VARCHAR(64) DEFAULT NULL COMMENT 'product key'"),
    ],
}

INDEXES_TO_ADD: IndexSpecMap = {
    "iot_device": [
        (
            "idx_device_tenant_org_deleted",
            "ALTER TABLE `iot_device` ADD INDEX `idx_device_tenant_org_deleted` (`tenant_id`, `org_id`, `deleted`, `last_report_time`, `id`)",
        ),
    ],
    "risk_point_device": [
        (
            "idx_risk_point_device_metric_catalog",
            "ALTER TABLE `risk_point_device` ADD INDEX `idx_risk_point_device_metric_catalog` (`risk_metric_id`)",
        ),
    ],
    "rule_definition": [
        (
            "idx_rule_definition_metric_catalog",
            "ALTER TABLE `rule_definition` ADD INDEX `idx_rule_definition_metric_catalog` (`risk_metric_id`)",
        ),
    ],
    "risk_metric_linkage_binding": [
        (
            "uk_risk_metric_linkage_active",
            "ALTER TABLE `risk_metric_linkage_binding` ADD UNIQUE INDEX `uk_risk_metric_linkage_active` (`tenant_id`, `risk_metric_id`, `linkage_rule_id`, `deleted`)",
        ),
        (
            "idx_risk_metric_linkage_rule",
            "ALTER TABLE `risk_metric_linkage_binding` ADD INDEX `idx_risk_metric_linkage_rule` (`linkage_rule_id`, `binding_status`, `deleted`)",
        ),
        (
            "idx_risk_metric_linkage_metric",
            "ALTER TABLE `risk_metric_linkage_binding` ADD INDEX `idx_risk_metric_linkage_metric` (`risk_metric_id`, `binding_status`, `deleted`)",
        ),
    ],
    "risk_metric_emergency_plan_binding": [
        (
            "uk_risk_metric_plan_active",
            "ALTER TABLE `risk_metric_emergency_plan_binding` ADD UNIQUE INDEX `uk_risk_metric_plan_active` (`tenant_id`, `risk_metric_id`, `emergency_plan_id`, `deleted`)",
        ),
        (
            "idx_risk_metric_plan_rule",
            "ALTER TABLE `risk_metric_emergency_plan_binding` ADD INDEX `idx_risk_metric_plan_rule` (`emergency_plan_id`, `binding_status`, `deleted`)",
        ),
        (
            "idx_risk_metric_plan_metric",
            "ALTER TABLE `risk_metric_emergency_plan_binding` ADD INDEX `idx_risk_metric_plan_metric` (`risk_metric_id`, `binding_status`, `deleted`)",
        ),
    ],
    "iot_governance_work_item": [
        (
            "idx_governance_work_item_subject",
            "ALTER TABLE `iot_governance_work_item` ADD INDEX `idx_governance_work_item_subject` (`subject_type`, `subject_id`, `work_status`, `deleted`)",
        ),
    ],
    "iot_governance_ops_alert": [
        (
            "uk_governance_ops_alert_code",
            "ALTER TABLE `iot_governance_ops_alert` ADD UNIQUE INDEX `uk_governance_ops_alert_code` (`tenant_id`, `alert_type`, `alert_code`, `deleted`)",
        ),
    ],
}

UNIQUE_INDEX_DUPLICATE_GUARDS: Dict[Tuple[str, str], Tuple[str, ...]] = {
    ("risk_metric_linkage_binding", "uk_risk_metric_linkage_active"): (
        "tenant_id",
        "risk_metric_id",
        "linkage_rule_id",
        "deleted",
    ),
    ("risk_metric_emergency_plan_binding", "uk_risk_metric_plan_active"): (
        "tenant_id",
        "risk_metric_id",
        "emergency_plan_id",
        "deleted",
    ),
    ("iot_governance_ops_alert", "uk_governance_ops_alert_code"): (
        "tenant_id",
        "alert_type",
        "alert_code",
        "deleted",
    ),
}

EXPECTED_INDEX_SHAPES: IndexShapeMap = {
    ("risk_metric_linkage_binding", "uk_risk_metric_linkage_active"): (
        True,
        ("tenant_id", "risk_metric_id", "linkage_rule_id", "deleted"),
    ),
    ("risk_metric_linkage_binding", "idx_risk_metric_linkage_rule"): (
        False,
        ("linkage_rule_id", "binding_status", "deleted"),
    ),
    ("risk_metric_linkage_binding", "idx_risk_metric_linkage_metric"): (
        False,
        ("risk_metric_id", "binding_status", "deleted"),
    ),
    ("risk_metric_emergency_plan_binding", "uk_risk_metric_plan_active"): (
        True,
        ("tenant_id", "risk_metric_id", "emergency_plan_id", "deleted"),
    ),
    ("risk_metric_emergency_plan_binding", "idx_risk_metric_plan_rule"): (
        False,
        ("emergency_plan_id", "binding_status", "deleted"),
    ),
    ("risk_metric_emergency_plan_binding", "idx_risk_metric_plan_metric"): (
        False,
        ("risk_metric_id", "binding_status", "deleted"),
    ),
    ("iot_governance_work_item", "idx_governance_work_item_subject"): (
        False,
        ("subject_type", "subject_id", "work_status", "deleted"),
    ),
    ("iot_governance_ops_alert", "uk_governance_ops_alert_code"): (
        True,
        ("tenant_id", "alert_type", "alert_code", "deleted"),
    ),
}

BINDING_INDEX_EXPECTED_SHAPES: Dict[Tuple[str, str], Tuple[bool, Tuple[str, ...]]] = {
    ("risk_metric_linkage_binding", "uk_risk_metric_linkage_active"): (
        True,
        ("tenant_id", "risk_metric_id", "linkage_rule_id", "deleted"),
    ),
    ("risk_metric_linkage_binding", "idx_risk_metric_linkage_rule"): (
        False,
        ("linkage_rule_id", "binding_status", "deleted"),
    ),
    ("risk_metric_linkage_binding", "idx_risk_metric_linkage_metric"): (
        False,
        ("risk_metric_id", "binding_status", "deleted"),
    ),
    ("risk_metric_emergency_plan_binding", "uk_risk_metric_plan_active"): (
        True,
        ("tenant_id", "risk_metric_id", "emergency_plan_id", "deleted"),
    ),
    ("risk_metric_emergency_plan_binding", "idx_risk_metric_plan_rule"): (
        False,
        ("emergency_plan_id", "binding_status", "deleted"),
    ),
    ("risk_metric_emergency_plan_binding", "idx_risk_metric_plan_metric"): (
        False,
        ("risk_metric_id", "binding_status", "deleted"),
    ),
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run rm_iot schema sync for real environment.")
    parser.add_argument("--host", default=os.getenv("IOT_MYSQL_HOST", "8.130.107.120"))
    parser.add_argument("--port", type=int, default=int(os.getenv("IOT_MYSQL_PORT", "3306")))
    parser.add_argument("--db", default=os.getenv("IOT_MYSQL_DB", "rm_iot"))
    parser.add_argument("--user", default=os.getenv("IOT_MYSQL_USERNAME", "root"))
    parser.add_argument("--password", default=os.getenv("IOT_MYSQL_PASSWORD", "mI8%pB1*gD"))
    return parser.parse_args()


def table_exists(cur: pymysql.cursors.Cursor, db: str, table: str) -> bool:
    cur.execute(
        """
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s
        LIMIT 1
        """,
        (db, table),
    )
    return cur.fetchone() is not None


def column_exists(cur: pymysql.cursors.Cursor, db: str, table: str, column: str) -> bool:
    cur.execute(
        """
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s AND COLUMN_NAME=%s
        LIMIT 1
        """,
        (db, table, column),
    )
    return cur.fetchone() is not None


def index_exists(cur: pymysql.cursors.Cursor, db: str, table: str, index: str) -> bool:
    cur.execute(
        """
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s AND INDEX_NAME=%s
        LIMIT 1
        """,
        (db, table, index),
    )
    return cur.fetchone() is not None


def load_index_shape(
    cur: pymysql.cursors.Cursor, db: str, table: str, index: str
) -> Tuple[bool, Tuple[str, ...]] | None:
    cur.execute(
        """
        SELECT NON_UNIQUE, COLUMN_NAME, SEQ_IN_INDEX
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s AND INDEX_NAME=%s
        ORDER BY SEQ_IN_INDEX
        """,
        (db, table, index),
    )
    rows = cur.fetchall()
    if not rows:
        return None
    is_unique = int(rows[0][0]) == 0
    columns = tuple(row[1] for row in rows)
    return is_unique, columns


def ensure_existing_index_shape(cur: pymysql.cursors.Cursor, db: str, table: str, index: str) -> None:
    expected = EXPECTED_INDEX_SHAPES.get((table, index))
    if expected is None:
        return
    actual = load_index_shape(cur, db, table, index)
    if actual is None:
        return
    if actual != expected:
        expected_unique, expected_columns = expected
        actual_unique, actual_columns = actual
        expected_kind = "UNIQUE" if expected_unique else "INDEX"
        actual_kind = "UNIQUE" if actual_unique else "INDEX"
        raise RuntimeError(
            f"Existing index {table}.{index} drifts from expected shape: "
            f"expected {expected_kind} {expected_columns}, got {actual_kind} {actual_columns}."
        )


def ensure_dict_defaults(cur: pymysql.cursors.Cursor) -> None:
    cur.execute(
        "ALTER TABLE sys_dict MODIFY COLUMN dict_value VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'legacy dict value'"
    )
    cur.execute(
        "ALTER TABLE sys_dict MODIFY COLUMN dict_label VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'legacy dict label'"
    )
    cur.execute("UPDATE sys_dict SET dict_value='' WHERE dict_value IS NULL")
    cur.execute("UPDATE sys_dict SET dict_label='' WHERE dict_label IS NULL")


def next_table_id(cur: pymysql.cursors.Cursor, table: str) -> int:
    cur.execute(f"SELECT COALESCE(MAX(id), 0) + 1 FROM `{table}`")
    return int(cur.fetchone()[0])


def next_preferred_id(cur: pymysql.cursors.Cursor, table: str, preferred_id: int | None) -> int:
    if preferred_id is None:
        return next_table_id(cur, table)
    cur.execute(f"SELECT COUNT(1) FROM `{table}` WHERE id = %s", (preferred_id,))
    if int(cur.fetchone()[0]) == 0:
        return preferred_id
    return next_table_id(cur, table)


def cleanup_duplicate_level_dicts(
    cur: pymysql.cursors.Cursor,
    canonical_dict_id: int,
    dict_code: str,
) -> None:
    cur.execute(
        """
        UPDATE sys_dict
        SET status = 0,
            deleted = 1,
            update_by = 1,
            update_time = NOW()
        WHERE tenant_id = 1
          AND dict_code = %s
          AND id <> %s
        """,
        (dict_code, canonical_dict_id),
    )
    cur.execute(
        """
        UPDATE sys_dict_item
        SET status = 0,
            deleted = 1,
            update_by = 1,
            update_time = NOW()
        WHERE tenant_id = 1
          AND dict_id IN (
              SELECT id FROM (
                  SELECT id
                  FROM sys_dict
                  WHERE tenant_id = 1
                    AND dict_code = %s
                    AND id <> %s
              ) duplicated
          )
        """,
        (dict_code, canonical_dict_id),
    )


def ensure_level_dict(
    cur: pymysql.cursors.Cursor,
    db: str,
    *,
    dict_code: str,
    dict_name: str,
    sort_no: int,
    dict_remark: str,
    preferred_dict_id: int | None,
    target_items: List[Tuple[str, str, int, str, List[str], int | None]],
) -> None:
    if not table_exists(cur, db, "sys_dict") or not table_exists(cur, db, "sys_dict_item"):
        return

    cur.execute(
        """
        SELECT id
        FROM sys_dict
        WHERE tenant_id = 1 AND dict_code = %s
        ORDER BY deleted ASC, id ASC
        LIMIT 1
        """,
        (dict_code,),
    )
    existing_dict = cur.fetchone()
    dict_id = int(existing_dict[0]) if existing_dict else next_preferred_id(cur, "sys_dict", preferred_dict_id)
    cleanup_duplicate_level_dicts(cur, dict_id, dict_code)

    if existing_dict:
        cur.execute(
            """
            UPDATE sys_dict
            SET dict_name=%s,
                dict_type='text',
                status=1,
                sort_no=%s,
                remark=%s,
                update_by=1,
                update_time=NOW(),
                deleted=0
            WHERE id=%s
            """,
            (dict_name, sort_no, dict_remark, dict_id),
        )
    else:
        cur.execute(
            """
            INSERT INTO sys_dict (
                id, tenant_id, dict_name, dict_code, dict_type, status, sort_no, remark,
                create_by, create_time, update_by, update_time, deleted
            ) VALUES (%s, 1, %s, %s, 'text', 1, %s, %s, 1, NOW(), 1, NOW(), 0)
            """,
            (dict_id, dict_name, dict_code, sort_no, dict_remark),
        )

    cur.execute(
        """
        SELECT id, item_value
        FROM sys_dict_item
        WHERE tenant_id = 1 AND dict_id = %s
        ORDER BY deleted ASC, sort_no ASC, id ASC
        """,
        (dict_id,),
    )
    existing_items = cur.fetchall()
    items_by_value: Dict[str, List[int]] = {}
    for item_id, item_value in existing_items:
        normalized_value = (item_value or "").strip().lower()
        items_by_value.setdefault(normalized_value, []).append(int(item_id))

    retained_ids: List[int] = []

    for value, label, item_sort_no, remark, legacy_values, preferred_item_id in target_items:
        candidate_id = None
        for candidate_value in [value, *legacy_values]:
            if not candidate_value:
                continue
            candidate_ids = items_by_value.get(candidate_value, [])
            while candidate_ids:
                maybe_id = candidate_ids.pop(0)
                if maybe_id not in retained_ids:
                    candidate_id = maybe_id
                    break
            if candidate_id is not None:
                break

        if candidate_id is None:
            candidate_id = next_preferred_id(cur, "sys_dict_item", preferred_item_id)
            cur.execute(
                """
                INSERT INTO sys_dict_item (
                    id, tenant_id, dict_id, item_name, item_value, item_type, status, sort_no, remark,
                    create_by, create_time, update_by, update_time, deleted
                ) VALUES (%s, 1, %s, %s, %s, 'string', 1, %s, %s, 1, NOW(), 1, NOW(), 0)
                """,
                (candidate_id, dict_id, label, value, item_sort_no, remark),
            )
        else:
            cur.execute(
                """
                UPDATE sys_dict_item
                SET dict_id=%s,
                    item_name=%s,
                    item_value=%s,
                    item_type='string',
                    status=1,
                    sort_no=%s,
                    remark=%s,
                    update_by=1,
                    update_time=NOW(),
                    deleted=0
                WHERE id=%s
                """,
                (dict_id, label, value, item_sort_no, remark, candidate_id),
            )
        retained_ids.append(candidate_id)

    if retained_ids:
        retained_placeholders = ", ".join(["%s"] * len(retained_ids))
        cur.execute(
            f"""
            UPDATE sys_dict_item
            SET status=0,
                deleted=1,
                update_by=1,
                update_time=NOW()
            WHERE tenant_id = 1
              AND dict_id = %s
              AND id NOT IN ({retained_placeholders})
            """,
            (dict_id, *retained_ids),
        )


def level_dict_targets() -> Dict[str, Dict[str, object]]:
    return {
        "risk_point_level": {
            "dict_name": "风险点等级",
            "sort_no": 1,
            "dict_remark": "风险点档案等级字典",
            "preferred_dict_id": 7201,
            "target_items": [
                ("level_1", "一级风险点", 1, "风险点等级-一级风险点", [], 7301),
                ("level_2", "二级风险点", 2, "风险点等级-二级风险点", [], 7302),
                ("level_3", "三级风险点", 3, "风险点等级-三级风险点", [], 7303),
            ],
        },
        "alarm_level": {
            "dict_name": "告警等级",
            "sort_no": 2,
            "dict_remark": "告警等级四色字典",
            "preferred_dict_id": 7202,
            "target_items": [
                ("red", "红色", 1, "告警等级-红色", ["critical"], 7304),
                ("orange", "橙色", 2, "告警等级-橙色", ["warning", "high"], 7305),
                ("yellow", "黄色", 3, "告警等级-黄色", ["medium"], 7306),
                ("blue", "蓝色", 4, "告警等级-蓝色", ["info", "low"], 7307),
            ],
        },
        "risk_level": {
            "dict_name": "风险态势等级",
            "sort_no": 3,
            "dict_remark": "运行态风险颜色字典",
            "preferred_dict_id": 7203,
            "target_items": [
                ("red", "红色", 1, "风险态势等级-红色", ["critical"], 7308),
                ("orange", "橙色", 2, "风险态势等级-橙色", ["warning"], 7309),
                ("yellow", "黄色", 3, "风险态势等级-黄色", [], 7310),
                ("blue", "蓝色", 4, "风险态势等级-蓝色", ["info"], 7311),
            ],
        },
    }


def ensure_level_dicts(cur: pymysql.cursors.Cursor, db: str) -> None:
    for dict_code, definition in level_dict_targets().items():
        ensure_level_dict(cur, db, dict_code=dict_code, **definition)


def system_governance_dict_targets() -> Dict[str, Dict[str, object]]:
    return {
        "help_doc_category": {
            "dict_name": "帮助文档分类",
            "sort_no": 4,
            "dict_remark": "帮助中心分类字典",
            "preferred_dict_id": 7204,
            "target_items": [
                ("business", "业务类", 1, "帮助文档分类-业务类", [], 7312),
                ("technical", "技术类", 2, "帮助文档分类-技术类", [], 7313),
                ("faq", "常见问题", 3, "帮助文档分类-常见问题", [], 7314),
            ],
        },
        "notification_channel_type": {
            "dict_name": "通知渠道类型",
            "sort_no": 5,
            "dict_remark": "通知渠道类型字典",
            "preferred_dict_id": 7205,
            "target_items": [
                ("email", "邮件", 1, "通知渠道类型-邮件", [], 7315),
                ("sms", "短信", 2, "通知渠道类型-短信", [], 7316),
                ("webhook", "Webhook", 3, "通知渠道类型-Webhook", [], 7317),
                ("wechat", "微信", 4, "通知渠道类型-微信", [], 7318),
                ("feishu", "飞书", 5, "通知渠道类型-飞书", [], 7319),
                ("dingtalk", "钉钉", 6, "通知渠道类型-钉钉", [], 7320),
            ],
        },
    }


def ensure_system_governance_dicts(cur: pymysql.cursors.Cursor, db: str) -> None:
    for dict_code, definition in system_governance_dict_targets().items():
        ensure_level_dict(cur, db, dict_code=dict_code, **definition)


def normalize_color_case(column: str) -> str:
    return f"""
    CASE LOWER(TRIM({column}))
        WHEN 'critical' THEN 'red'
        WHEN 'high' THEN 'orange'
        WHEN 'warning' THEN 'orange'
        WHEN 'medium' THEN 'yellow'
        WHEN 'info' THEN 'blue'
        WHEN 'low' THEN 'blue'
        WHEN 'red' THEN 'red'
        WHEN 'orange' THEN 'orange'
        WHEN 'yellow' THEN 'yellow'
        WHEN 'blue' THEN 'blue'
        ELSE LOWER(TRIM({column}))
    END
    """


def migrate_level_values(cur: pymysql.cursors.Cursor, db: str) -> None:
    if table_exists(cur, db, "risk_point") and column_exists(cur, db, "risk_point", "current_risk_level"):
        cur.execute(
            f"""
            UPDATE `risk_point`
            SET current_risk_level = {normalize_color_case('current_risk_level')}
            WHERE current_risk_level IS NOT NULL
              AND TRIM(current_risk_level) <> ''
            """
        )
        if column_exists(cur, db, "risk_point", "risk_level"):
            cur.execute(
                f"""
                UPDATE `risk_point`
                SET current_risk_level = {normalize_color_case('risk_level')}
                WHERE (current_risk_level IS NULL OR TRIM(current_risk_level) = '')
                  AND risk_level IS NOT NULL
                  AND TRIM(risk_level) <> ''
                """
            )
            cur.execute(
                f"""
                UPDATE `risk_point`
                SET risk_level = {normalize_color_case('risk_level')}
                WHERE risk_level IS NOT NULL
                  AND TRIM(risk_level) <> ''
                """
            )

    if table_exists(cur, db, "iot_event_record") and column_exists(cur, db, "iot_event_record", "risk_level"):
        cur.execute(
            f"""
            UPDATE `iot_event_record`
            SET risk_level = {normalize_color_case('risk_level')}
            WHERE risk_level IS NOT NULL
              AND TRIM(risk_level) <> ''
            """
        )

    for table in ("rule_definition", "iot_alarm_record", "iot_event_record"):
        if not table_exists(cur, db, table) or not column_exists(cur, db, table, "alarm_level"):
            continue
        cur.execute(
            f"""
            UPDATE `{table}`
            SET alarm_level = {normalize_color_case('alarm_level')}
            WHERE alarm_level IS NOT NULL
              AND TRIM(alarm_level) <> ''
            """
        )

    if table_exists(cur, db, "emergency_plan") and column_exists(cur, db, "emergency_plan", "alarm_level"):
        cur.execute(
            f"""
            UPDATE `emergency_plan`
            SET alarm_level = {normalize_color_case('alarm_level')}
            WHERE alarm_level IS NOT NULL
              AND TRIM(alarm_level) <> ''
            """
        )
        if column_exists(cur, db, "emergency_plan", "risk_level"):
            cur.execute(
                f"""
                UPDATE `emergency_plan`
                SET alarm_level = {normalize_color_case('risk_level')}
                WHERE (alarm_level IS NULL OR TRIM(alarm_level) = '')
                  AND risk_level IS NOT NULL
                  AND TRIM(risk_level) <> ''
                """
            )
            cur.execute(
                f"""
                UPDATE `emergency_plan`
                SET risk_level = {normalize_color_case('risk_level')}
                WHERE risk_level IS NOT NULL
                  AND TRIM(risk_level) <> ''
                """
            )


def ensure_audit_defaults(cur: pymysql.cursors.Cursor) -> None:
    cur.execute(
        "ALTER TABLE sys_audit_log MODIFY COLUMN log_type VARCHAR(16) NOT NULL DEFAULT 'manual'"
    )
    cur.execute(
        "ALTER TABLE sys_audit_log MODIFY COLUMN operation_uri VARCHAR(255) NOT NULL DEFAULT ''"
    )
    cur.execute(
        "ALTER TABLE sys_audit_log MODIFY COLUMN operation_method VARCHAR(255) NOT NULL DEFAULT ''"
    )


def ensure_legacy_phase4_defaults(cur: pymysql.cursors.Cursor, db: str) -> None:
    if table_exists(cur, db, "iot_command_record") and column_exists(cur, db, "iot_command_record", "message_id"):
        cur.execute("ALTER TABLE iot_command_record MODIFY COLUMN message_id VARCHAR(64) NULL DEFAULT NULL")

    if table_exists(cur, db, "iot_event_work_order") and column_exists(
        cur, db, "iot_event_work_order", "work_order_type"
    ):
        cur.execute(
            "ALTER TABLE iot_event_work_order MODIFY COLUMN work_order_type VARCHAR(32) NOT NULL DEFAULT 'event-dispatch'"
        )

    if table_exists(cur, db, "risk_point_device") and column_exists(cur, db, "risk_point_device", "id"):
        cur.execute("ALTER TABLE risk_point_device MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT")

    if table_exists(cur, db, "rule_definition") and column_exists(cur, db, "rule_definition", "rule_code"):
        cur.execute("ALTER TABLE rule_definition MODIFY COLUMN rule_code VARCHAR(64) NULL DEFAULT NULL")
    if table_exists(cur, db, "rule_definition") and column_exists(
        cur, db, "rule_definition", "condition_expression"
    ):
        cur.execute(
            "ALTER TABLE rule_definition MODIFY COLUMN condition_expression VARCHAR(255) NOT NULL DEFAULT ''"
        )

    if table_exists(cur, db, "linkage_rule") and column_exists(cur, db, "linkage_rule", "rule_code"):
        cur.execute("ALTER TABLE linkage_rule MODIFY COLUMN rule_code VARCHAR(64) NULL DEFAULT NULL")

    if table_exists(cur, db, "emergency_plan") and column_exists(cur, db, "emergency_plan", "plan_code"):
        cur.execute("ALTER TABLE emergency_plan MODIFY COLUMN plan_code VARCHAR(64) NULL DEFAULT NULL")
    if table_exists(cur, db, "emergency_plan") and column_exists(
        cur, db, "emergency_plan", "applicable_scenario"
    ):
        cur.execute(
            "ALTER TABLE emergency_plan MODIFY COLUMN applicable_scenario VARCHAR(255) NOT NULL DEFAULT ''"
        )
    if table_exists(cur, db, "emergency_plan") and column_exists(cur, db, "emergency_plan", "disposal_steps"):
        cur.execute("ALTER TABLE emergency_plan MODIFY COLUMN disposal_steps JSON NULL")


def ensure_menu_compat(cur: pymysql.cursors.Cursor, db: str) -> None:
    if not table_exists(cur, db, "sys_menu"):
        return

    # 兼容历史 sys_menu 字段命名，保证权限菜单查询可落在统一实体字段上。
    if column_exists(cur, db, "sys_menu", "menu_code") and column_exists(cur, db, "sys_menu", "permission"):
        cur.execute(
            "UPDATE sys_menu SET menu_code = permission WHERE (menu_code IS NULL OR menu_code = '') AND permission IS NOT NULL AND permission <> ''"
        )
    if column_exists(cur, db, "sys_menu", "menu_code"):
        cur.execute("UPDATE sys_menu SET menu_code = CONCAT('menu-', id) WHERE menu_code IS NULL OR menu_code = ''")

    if column_exists(cur, db, "sys_menu", "path") and column_exists(cur, db, "sys_menu", "route_path"):
        cur.execute("UPDATE sys_menu SET path = route_path WHERE (path IS NULL OR path = '') AND route_path IS NOT NULL")

    if column_exists(cur, db, "sys_menu", "sort") and column_exists(cur, db, "sys_menu", "sort_no"):
        cur.execute("UPDATE sys_menu SET sort = sort_no WHERE sort IS NULL")

    if column_exists(cur, db, "sys_menu", "type") and column_exists(cur, db, "sys_menu", "menu_type"):
        cur.execute("UPDATE sys_menu SET type = menu_type WHERE type IS NULL")


def ensure_legacy_governance_write_permissions(cur: pymysql.cursors.Cursor, db: str) -> None:
    if not table_exists(cur, db, "sys_menu") or not table_exists(cur, db, "sys_role_menu"):
        return
    if not column_exists(cur, db, "sys_menu", "menu_code"):
        return
    if not all(
        column_exists(cur, db, "sys_role_menu", column) for column in ("menu_id", "deleted", "update_by", "update_time")
    ):
        return
    if not all(column_exists(cur, db, "sys_menu", column) for column in ("id", "deleted", "update_by", "update_time")):
        return

    legacy_codes = "', '".join(
        (
            "risk:rule-definition:write",
            "risk:linkage-rule:write",
            "risk:emergency-plan:write",
        )
    )
    cur.execute(
        f"""
        UPDATE sys_role_menu rm
        INNER JOIN sys_menu m ON m.id = rm.menu_id
        SET rm.deleted = 1,
            rm.update_by = 1,
            rm.update_time = NOW()
        WHERE m.menu_code IN ('{legacy_codes}')
          AND rm.deleted = 0
        """
    )
    cur.execute(
        f"""
        UPDATE sys_menu
        SET deleted = 1,
            update_by = 1,
            update_time = NOW()
        WHERE menu_code IN ('{legacy_codes}')
          AND deleted = 0
        """
    )


def ensure_governance_fine_grained_permissions(cur: pymysql.cursors.Cursor, db: str) -> None:
    if not table_exists(cur, db, "sys_menu") or not table_exists(cur, db, "sys_role") or not table_exists(cur, db, "sys_role_menu"):
        return

    required_menu_columns = (
        "id",
        "tenant_id",
        "parent_id",
        "menu_name",
        "menu_code",
        "path",
        "component",
        "icon",
        "meta_json",
        "sort",
        "type",
        "menu_type",
        "route_path",
        "permission",
        "sort_no",
        "visible",
        "status",
        "create_by",
        "create_time",
        "update_by",
        "update_time",
        "deleted",
    )
    required_role_columns = ("id", "tenant_id", "role_code", "deleted")
    required_role_menu_columns = ("id", "tenant_id", "role_id", "menu_id", "create_by", "create_time", "update_by", "update_time", "deleted")
    if not all(column_exists(cur, db, "sys_menu", column) for column in required_menu_columns):
        print("[skip] governance permission seeds: sys_menu columns missing")
        return
    if not all(column_exists(cur, db, "sys_role", column) for column in required_role_columns):
        print("[skip] governance permission seeds: sys_role columns missing")
        return
    if not all(column_exists(cur, db, "sys_role_menu", column) for column in required_role_menu_columns):
        print("[skip] governance permission seeds: sys_role_menu columns missing")
        return

    menu_seeds = (
        {
            "preferred_id": 93001029,
            "parent_id": 93001001,
            "menu_name": "规范库复核",
            "menu_code": "iot:normative-library:approve",
            "permission": "iot:normative-library:approve",
            "meta_json": '{"caption":"规范库关键写动作复核授权"}',
            "sort_no": 1129,
            "role_codes": ("MANAGEMENT_STAFF",),
        },
        {
            "preferred_id": 93002051,
            "parent_id": 93002003,
            "menu_name": "风险指标复核",
            "menu_code": "risk:metric-catalog:approve",
            "permission": "risk:metric-catalog:approve",
            "meta_json": '{"caption":"风险指标标注关键写动作双人复核"}',
            "sort_no": 3145,
            "role_codes": ("MANAGEMENT_STAFF", "OPS_STAFF"),
        },
    )

    for seed in menu_seeds:
        cur.execute(
            """
            SELECT id
            FROM sys_menu
            WHERE tenant_id = 1
              AND menu_code = %s
            ORDER BY deleted ASC, id ASC
            LIMIT 1
            """,
            (seed["menu_code"],),
        )
        existing_menu = cur.fetchone()
        menu_id = int(existing_menu[0]) if existing_menu else next_preferred_id(cur, "sys_menu", int(seed["preferred_id"]))
        if existing_menu:
            cur.execute(
                """
                UPDATE sys_menu
                SET parent_id = %s,
                    menu_name = %s,
                    path = '',
                    component = '',
                    icon = '',
                    meta_json = %s,
                    sort = %s,
                    type = 2,
                    menu_type = 2,
                    route_path = '',
                    permission = %s,
                    sort_no = %s,
                    visible = 1,
                    status = 1,
                    update_by = 1,
                    update_time = NOW(),
                    deleted = 0
                WHERE id = %s
                """,
                (
                    int(seed["parent_id"]),
                    seed["menu_name"],
                    seed["meta_json"],
                    int(seed["sort_no"]),
                    seed["permission"],
                    int(seed["sort_no"]),
                    menu_id,
                ),
            )
        else:
            cur.execute(
                """
                INSERT INTO sys_menu (
                    id, tenant_id, parent_id, menu_name, menu_code, path, component, icon, meta_json,
                    sort, type, menu_type, route_path, permission, sort_no, visible, status,
                    create_by, create_time, update_by, update_time, deleted
                ) VALUES (
                    %s, 1, %s, %s, %s, '', '', '', %s,
                    %s, 2, 2, '', %s, %s, 1, 1,
                    1, NOW(), 1, NOW(), 0
                )
                """,
                (
                    menu_id,
                    int(seed["parent_id"]),
                    seed["menu_name"],
                    seed["menu_code"],
                    seed["meta_json"],
                    int(seed["sort_no"]),
                    seed["permission"],
                    int(seed["sort_no"]),
                ),
            )

        for role_code in seed["role_codes"]:
            cur.execute(
                """
                SELECT id
                FROM sys_role
                WHERE tenant_id = 1
                  AND role_code = %s
                  AND deleted = 0
                ORDER BY id ASC
                LIMIT 1
                """,
                (role_code,),
            )
            role_row = cur.fetchone()
            if role_row is None:
                continue
            role_id = int(role_row[0])
            cur.execute(
                """
                SELECT id
                FROM sys_role_menu
                WHERE tenant_id = 1
                  AND role_id = %s
                  AND menu_id = %s
                ORDER BY deleted ASC, id ASC
                LIMIT 1
                """,
                (role_id, menu_id),
            )
            existing_binding = cur.fetchone()
            if existing_binding:
                cur.execute(
                    """
                    UPDATE sys_role_menu
                    SET deleted = 0,
                        update_by = 1,
                        update_time = NOW()
                    WHERE id = %s
                    """,
                    (int(existing_binding[0]),),
                )
            else:
                binding_id = next_table_id(cur, "sys_role_menu")
                cur.execute(
                    """
                    INSERT INTO sys_role_menu (
                        id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted
                    ) VALUES (%s, 1, %s, %s, 1, NOW(), 1, NOW(), 0)
                    """,
                    (binding_id, role_id, menu_id),
                )


def ensure_governance_approval_policy_defaults(cur: pymysql.cursors.Cursor, db: str) -> None:
    required_tables = (
        "sys_user",
        "sys_role",
        "sys_user_role",
        "sys_governance_approval_policy",
    )
    if any(not table_exists(cur, db, table) for table in required_tables):
        print("[skip] governance approval policy seeds: required tables missing")
        return

    required_user_columns = (
        "id",
        "tenant_id",
        "username",
        "password",
        "nickname",
        "real_name",
        "phone",
        "email",
        "status",
        "is_admin",
        "remark",
        "create_by",
        "create_time",
        "update_by",
        "update_time",
        "deleted",
    )
    required_role_columns = ("id", "tenant_id", "role_code", "deleted")
    required_user_role_columns = (
        "id",
        "tenant_id",
        "user_id",
        "role_id",
        "create_by",
        "create_time",
        "update_by",
        "update_time",
        "deleted",
    )
    required_policy_columns = (
        "id",
        "tenant_id",
        "scope_type",
        "action_code",
        "approver_mode",
        "approver_user_id",
        "enabled",
        "remark",
        "create_by",
        "create_time",
        "update_by",
        "update_time",
        "deleted",
    )
    if not all(column_exists(cur, db, "sys_user", column) for column in required_user_columns):
        print("[skip] governance approval policy seeds: sys_user columns missing")
        return
    if not all(column_exists(cur, db, "sys_role", column) for column in required_role_columns):
        print("[skip] governance approval policy seeds: sys_role columns missing")
        return
    if not all(column_exists(cur, db, "sys_user_role", column) for column in required_user_role_columns):
        print("[skip] governance approval policy seeds: sys_user_role columns missing")
        return
    if not all(column_exists(cur, db, "sys_governance_approval_policy", column) for column in required_policy_columns):
        print("[skip] governance approval policy seeds: sys_governance_approval_policy columns missing")
        return

    reviewer_org_id = None
    if table_exists(cur, db, "sys_organization") and all(
        column_exists(cur, db, "sys_organization", column) for column in ("id", "tenant_id", "deleted")
    ):
        cur.execute(
            """
            SELECT id
            FROM sys_organization
            WHERE tenant_id = 1
              AND deleted = 0
            ORDER BY id ASC
            LIMIT 1
            """
        )
        org_row = cur.fetchone()
        reviewer_org_id = int(org_row[0]) if org_row else None

    cur.execute(
        """
        SELECT id
        FROM sys_user
        WHERE tenant_id = 1
          AND username = %s
        ORDER BY deleted ASC, id ASC
        LIMIT 1
        """,
        (GOVERNANCE_REVIEWER_USERNAME,),
    )
    existing_user = cur.fetchone()
    reviewer_user_id = (
        int(existing_user[0])
        if existing_user
        else next_preferred_id(cur, "sys_user", GOVERNANCE_REVIEWER_PREFERRED_ID)
    )
    if existing_user:
        if reviewer_org_id is not None:
            cur.execute(
                """
                UPDATE sys_user
                SET org_id = %s,
                    nickname = %s,
                    real_name = %s,
                    phone = %s,
                    email = %s,
                    status = 1,
                    is_admin = 1,
                    remark = %s,
                    update_by = 1,
                    update_time = NOW(),
                    deleted = 0
                WHERE id = %s
                """,
                (
                    reviewer_org_id,
                    GOVERNANCE_REVIEWER_NICKNAME,
                    GOVERNANCE_REVIEWER_REAL_NAME,
                    GOVERNANCE_REVIEWER_PHONE,
                    GOVERNANCE_REVIEWER_EMAIL,
                    GOVERNANCE_REVIEWER_REMARK,
                    reviewer_user_id,
                ),
            )
        else:
            cur.execute(
                """
                UPDATE sys_user
                SET nickname = %s,
                    real_name = %s,
                    phone = %s,
                    email = %s,
                    status = 1,
                    is_admin = 1,
                    remark = %s,
                    update_by = 1,
                    update_time = NOW(),
                    deleted = 0
                WHERE id = %s
                """,
                (
                    GOVERNANCE_REVIEWER_NICKNAME,
                    GOVERNANCE_REVIEWER_REAL_NAME,
                    GOVERNANCE_REVIEWER_PHONE,
                    GOVERNANCE_REVIEWER_EMAIL,
                    GOVERNANCE_REVIEWER_REMARK,
                    reviewer_user_id,
                ),
            )
    else:
        cur.execute(
            """
            INSERT INTO sys_user (
                id, tenant_id, org_id, username, password, nickname, real_name, phone, email,
                status, is_admin, remark, create_by, create_time, update_by, update_time, deleted
            ) VALUES (
                %s, 1, %s, %s, %s, %s, %s, %s, %s,
                1, 1, %s, 1, NOW(), 1, NOW(), 0
            )
            """,
            (
                reviewer_user_id,
                reviewer_org_id,
                GOVERNANCE_REVIEWER_USERNAME,
                GOVERNANCE_REVIEWER_PASSWORD_HASH,
                GOVERNANCE_REVIEWER_NICKNAME,
                GOVERNANCE_REVIEWER_REAL_NAME,
                GOVERNANCE_REVIEWER_PHONE,
                GOVERNANCE_REVIEWER_EMAIL,
                GOVERNANCE_REVIEWER_REMARK,
            ),
        )

    cur.execute(
        """
        SELECT id
        FROM sys_role
        WHERE tenant_id = 1
          AND role_code = %s
          AND deleted = 0
        ORDER BY id ASC
        LIMIT 1
        """,
        (GOVERNANCE_REVIEWER_ROLE_CODE,),
    )
    role_row = cur.fetchone()
    if role_row is None:
        print("[skip] governance approval policy seeds: SUPER_ADMIN role missing")
        return
    reviewer_role_id = int(role_row[0])

    cur.execute(
        """
        SELECT id
        FROM sys_user_role
        WHERE tenant_id = 1
          AND user_id = %s
          AND role_id = %s
        ORDER BY deleted ASC, id ASC
        LIMIT 1
        """,
        (reviewer_user_id, reviewer_role_id),
    )
    existing_binding = cur.fetchone()
    if existing_binding:
        cur.execute(
            """
            UPDATE sys_user_role
            SET deleted = 0,
                update_by = 1,
                update_time = NOW()
            WHERE id = %s
            """,
            (int(existing_binding[0]),),
        )
    else:
        binding_id = next_table_id(cur, "sys_user_role")
        cur.execute(
            """
            INSERT INTO sys_user_role (
                id, tenant_id, user_id, role_id, create_by, create_time, update_by, update_time, deleted
            ) VALUES (%s, 1, %s, %s, 1, NOW(), 1, NOW(), 0)
            """,
            (binding_id, reviewer_user_id, reviewer_role_id),
        )

    for seed in GOVERNANCE_APPROVAL_POLICY_SEEDS:
        cur.execute(
            """
            SELECT id
            FROM sys_governance_approval_policy
            WHERE scope_type = %s
              AND tenant_id = %s
              AND action_code = %s
            ORDER BY deleted ASC, id ASC
            LIMIT 1
            """,
            (seed["scope_type"], int(seed["tenant_id"]), seed["action_code"]),
        )
        existing_policy = cur.fetchone()
        policy_id = (
            int(existing_policy[0])
            if existing_policy
            else next_preferred_id(cur, "sys_governance_approval_policy", int(seed["preferred_id"]))
        )
        if existing_policy:
            cur.execute(
                """
                UPDATE sys_governance_approval_policy
                SET approver_mode = %s,
                    approver_user_id = %s,
                    enabled = 1,
                    remark = %s,
                    update_by = 1,
                    update_time = NOW(),
                    deleted = 0
                WHERE id = %s
                """,
                (
                    seed["approver_mode"],
                    reviewer_user_id,
                    seed["remark"],
                    policy_id,
                ),
            )
        else:
            cur.execute(
                """
                INSERT INTO sys_governance_approval_policy (
                    id, tenant_id, scope_type, action_code, approver_mode, approver_user_id, enabled, remark,
                    create_by, create_time, update_by, update_time, deleted
                ) VALUES (
                    %s, %s, %s, %s, %s, %s, 1, %s,
                    1, NOW(), 1, NOW(), 0
                )
                """,
                (
                    policy_id,
                    int(seed["tenant_id"]),
                    seed["scope_type"],
                    seed["action_code"],
                    seed["approver_mode"],
                    reviewer_user_id,
                    seed["remark"],
                ),
            )


def ensure_indexes(cur: pymysql.cursors.Cursor, db: str) -> None:
    for table, specs in INDEXES_TO_ADD.items():
        if not table_exists(cur, db, table):
            print(f"[skip] table missing for indexes: {table}")
            continue
        for index_name, ddl in specs:
            if index_exists(cur, db, table, index_name):
                expected_shape = BINDING_INDEX_EXPECTED_SHAPES.get((table, index_name))
                if expected_shape and binding_index_shape_drifts(cur, db, table, index_name, expected_shape):
                    raise RuntimeError(
                        f"Existing index {table}.{index_name} drifts from expected shape and must be "
                        "corrected before schema sync can continue."
                    )
                ensure_existing_index_shape(cur, db, table, index_name)
                continue
            unique_columns = UNIQUE_INDEX_DUPLICATE_GUARDS.get((table, index_name))
            if unique_columns and has_duplicate_unique_key_rows(cur, table, unique_columns):
                raise RuntimeError(
                    f"Cannot add unique index {table}.{index_name}: duplicate rows detected; "
                    "duplicate rows must be cleaned before schema sync can continue."
                )
            cur.execute(ddl)
            print(f"[index] {table}.{index_name} added")


def has_duplicate_unique_key_rows(
    cur: pymysql.cursors.Cursor, table: str, unique_columns: Tuple[str, ...]
) -> bool:
    group_columns = ", ".join(f"`{column}`" for column in unique_columns)
    cur.execute(
        f"""
        SELECT 1
        FROM `{table}`
        GROUP BY {group_columns}
        HAVING COUNT(1) > 1
        LIMIT 1
        """
    )
    return cur.fetchone() is not None


def binding_index_shape_drifts(
    cur: pymysql.cursors.Cursor,
    db: str,
    table: str,
    index: str,
    expected_shape: Tuple[bool, Tuple[str, ...]],
) -> bool:
    existing_shape = load_index_shape(cur, db, table, index)
    if existing_shape is None:
        return True
    return existing_shape != expected_shape


def load_index_shape(
    cur: pymysql.cursors.Cursor, db: str, table: str, index: str
) -> Tuple[bool, Tuple[str, ...]] | None:
    cur.execute(
        """
        SELECT NON_UNIQUE, COLUMN_NAME, SEQ_IN_INDEX
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s AND INDEX_NAME=%s
        ORDER BY SEQ_IN_INDEX ASC
        """,
        (db, table, index),
    )
    rows = cur.fetchall()
    if not rows:
        return None
    is_unique = int(rows[0][0]) == 0
    columns = tuple(str(row[1]) for row in rows)
    return is_unique, columns


def find_multi_risk_point_conflicts(cur: pymysql.cursors.Cursor, db: str) -> List[Tuple[int, str, str, str]]:
    required_tables = ("iot_device", "risk_point_device", "risk_point")
    if any(not table_exists(cur, db, table) for table in required_tables):
        return []

    cur.execute(
        """
        SELECT
            d.id,
            d.device_code,
            GROUP_CONCAT(DISTINCT CAST(rp.id AS CHAR) ORDER BY rp.id SEPARATOR ',') AS risk_point_ids,
            GROUP_CONCAT(DISTINCT COALESCE(rp.risk_point_name, CONCAT('risk-point-', rp.id)) ORDER BY rp.id SEPARATOR ' / ') AS risk_point_names
        FROM iot_device d
        INNER JOIN risk_point_device rpd
            ON rpd.deleted = 0
           AND (
               (rpd.device_id IS NOT NULL AND rpd.device_id = d.id)
               OR (rpd.device_id IS NULL AND rpd.device_code IS NOT NULL AND rpd.device_code = d.device_code)
           )
        INNER JOIN risk_point rp
            ON rp.id = rpd.risk_point_id
           AND rp.deleted = 0
        WHERE d.deleted = 0
        GROUP BY d.id, d.device_code
        HAVING COUNT(DISTINCT rp.id) > 1
        ORDER BY d.id
        """
    )
    return [
        (int(device_id), str(device_code), str(risk_point_ids), str(risk_point_names))
        for device_id, device_code, risk_point_ids, risk_point_names in cur.fetchall()
    ]


def ensure_device_org_backfill(cur: pymysql.cursors.Cursor, db: str) -> None:
    if not table_exists(cur, db, "iot_device"):
        print("[skip] table missing: iot_device")
        return
    if not column_exists(cur, db, "iot_device", "org_id") or not column_exists(cur, db, "iot_device", "org_name"):
        print("[skip] iot_device org columns missing")
        return

    conflicts = find_multi_risk_point_conflicts(cur, db)
    if conflicts:
        print("[error] detected active multi-risk-point bindings. aborting device org backfill.")
        for device_id, device_code, risk_point_ids, risk_point_names in conflicts:
            print(
                f"  - device_id={device_id}, device_code={device_code}, risk_point_ids={risk_point_ids}, risk_point_names={risk_point_names}"
            )
        raise RuntimeError("device org backfill blocked by multi-risk-point bindings")

    cur.execute(
        """
        UPDATE iot_device d
        INNER JOIN (
            SELECT
                d_ref.id AS device_id,
                MAX(rp.org_id) AS org_id,
                MAX(rp.org_name) AS org_name
            FROM iot_device d_ref
            INNER JOIN risk_point_device rpd
                ON rpd.deleted = 0
               AND (
                   (rpd.device_id IS NOT NULL AND rpd.device_id = d_ref.id)
                   OR (rpd.device_id IS NULL AND rpd.device_code IS NOT NULL AND rpd.device_code = d_ref.device_code)
               )
            INNER JOIN risk_point rp
                ON rp.id = rpd.risk_point_id
               AND rp.deleted = 0
            WHERE d_ref.deleted = 0
              AND rp.org_id IS NOT NULL
              AND rp.org_name IS NOT NULL
            GROUP BY d_ref.id
        ) src
            ON src.device_id = d.id
        SET d.org_id = src.org_id,
            d.org_name = src.org_name
        WHERE d.deleted = 0
          AND d.org_id IS NULL
        """
    )
    print(f"[backfill] iot_device org fields updated from risk points: {cur.rowcount}")


def main() -> int:
    args = parse_args()

    conn = pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        charset="utf8mb4",
        autocommit=True,
        connect_timeout=20,
        read_timeout=60,
        write_timeout=60,
    )

    try:
        try:
            with conn.cursor() as cur:
                cur.execute(f"USE `{args.db}`")

                for table, ddl in CREATE_TABLE_SQL.items():
                    cur.execute(ddl)
                    print(f"[table] ensured {table}")

                for table, specs in COLUMNS_TO_ADD.items():
                    if not table_exists(cur, args.db, table):
                        print(f"[skip] table missing: {table}")
                        continue
                    for column, definition in specs:
                        if column_exists(cur, args.db, table, column):
                            continue
                        cur.execute(f"ALTER TABLE `{table}` ADD COLUMN `{column}` {definition}")
                        print(f"[column] {table}.{column} added")

                ensure_indexes(cur, args.db)

                if table_exists(cur, args.db, "sys_dict"):
                    if column_exists(cur, args.db, "sys_dict", "dict_value") and column_exists(
                        cur, args.db, "sys_dict", "dict_label"
                    ):
                        ensure_dict_defaults(cur)
                        print("[dict] sys_dict default constraints aligned")
                    ensure_level_dicts(cur, args.db)
                    print("[dict] risk_point_level / alarm_level / risk_level items aligned")
                    ensure_system_governance_dicts(cur, args.db)
                    print("[dict] help_doc_category / notification_channel_type items aligned")

                if table_exists(cur, args.db, "sys_audit_log"):
                    ensure_audit_defaults(cur)
                    print("[audit] sys_audit_log legacy constraints aligned")

                ensure_legacy_phase4_defaults(cur, args.db)
                print("[phase4] legacy strict columns aligned")

                migrate_level_values(cur, args.db)
                print("[phase4] risk point and alarm semantics values migrated")

                ensure_device_org_backfill(cur, args.db)

                ensure_menu_compat(cur, args.db)
                print("[menu] sys_menu legacy columns aligned")
                ensure_legacy_governance_write_permissions(cur, args.db)
                print("[menu] legacy governance write permissions cleaned")
                ensure_governance_fine_grained_permissions(cur, args.db)
                print("[menu] governance fine-grained permission seeds aligned")
                ensure_governance_approval_policy_defaults(cur, args.db)
                print("[governance] fixed reviewer and approval policy seeds aligned")

                for view_name, ddl in VIEW_SQL.items():
                    cur.execute(ddl)
                    print(f"[view] ensured {view_name}")

            print("Schema sync completed.")
            return 0
        except Exception as exc:
            print(f"[fatal] {exc}")
            return 1
    finally:
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
