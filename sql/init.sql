CREATE DATABASE IF NOT EXISTS rm_iot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE rm_iot;

SET NAMES utf8mb4;

-- 本文件由 scripts/schema/render_artifacts.py 生成，不要手工编辑。

-- 活跃 MySQL 表
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_tenant;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_region;
DROP TABLE IF EXISTS sys_organization;
DROP TABLE IF EXISTS sys_observability_span_log;
DROP TABLE IF EXISTS sys_notification_channel;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_in_app_message_read;
DROP TABLE IF EXISTS sys_in_app_message_bridge_log;
DROP TABLE IF EXISTS sys_in_app_message_bridge_attempt_log;
DROP TABLE IF EXISTS sys_in_app_message;
DROP TABLE IF EXISTS sys_help_document;
DROP TABLE IF EXISTS sys_dict_item;
DROP TABLE IF EXISTS sys_dict;
DROP TABLE IF EXISTS sys_business_event_log;
DROP TABLE IF EXISTS sys_audit_log;
DROP TABLE IF EXISTS sys_governance_replay_feedback;
DROP TABLE IF EXISTS sys_governance_approval_transition;
DROP TABLE IF EXISTS sys_governance_approval_policy;
DROP TABLE IF EXISTS sys_governance_approval_order;
DROP TABLE IF EXISTS iot_protocol_template_definition_snapshot;
DROP TABLE IF EXISTS iot_protocol_template_definition;
DROP TABLE IF EXISTS iot_protocol_family_definition_snapshot;
DROP TABLE IF EXISTS iot_protocol_family_definition;
DROP TABLE IF EXISTS iot_protocol_decrypt_profile_snapshot;
DROP TABLE IF EXISTS iot_protocol_decrypt_profile;
DROP TABLE IF EXISTS iot_governance_work_item;
DROP TABLE IF EXISTS iot_governance_ops_alert;
DROP TABLE IF EXISTS iot_vendor_metric_mapping_rule_snapshot;
DROP TABLE IF EXISTS iot_vendor_metric_mapping_rule;
DROP TABLE IF EXISTS iot_vendor_metric_evidence;
DROP TABLE IF EXISTS iot_runtime_metric_display_rule;
DROP TABLE IF EXISTS iot_product_model;
DROP TABLE IF EXISTS iot_product_metric_resolver_snapshot;
DROP TABLE IF EXISTS iot_product_contract_release_snapshot;
DROP TABLE IF EXISTS iot_product_contract_release_batch;
DROP TABLE IF EXISTS iot_product;
DROP TABLE IF EXISTS iot_onboarding_template_pack;
DROP TABLE IF EXISTS iot_normative_metric_definition;
DROP TABLE IF EXISTS iot_message_log_archive_batch;
DROP TABLE IF EXISTS iot_message_log_archive;
DROP TABLE IF EXISTS iot_message_log;
DROP TABLE IF EXISTS iot_device_secret_rotation_log;
DROP TABLE IF EXISTS iot_device_relation;
DROP TABLE IF EXISTS iot_device_property;
DROP TABLE IF EXISTS iot_device_online_session;
DROP TABLE IF EXISTS iot_device_onboarding_case;
DROP TABLE IF EXISTS iot_device_metric_latest;
DROP TABLE IF EXISTS iot_device_invalid_report_state;
DROP TABLE IF EXISTS iot_device_access_error_log;
DROP TABLE IF EXISTS iot_device;
DROP TABLE IF EXISTS iot_command_record;
DROP TABLE IF EXISTS rule_definition;
DROP TABLE IF EXISTS risk_point_device_pending_promotion;
DROP TABLE IF EXISTS risk_point_device_pending_binding;
DROP TABLE IF EXISTS risk_point_device_capability_binding;
DROP TABLE IF EXISTS risk_point_device;
DROP TABLE IF EXISTS risk_point;
DROP TABLE IF EXISTS risk_metric_linkage_binding;
DROP TABLE IF EXISTS risk_metric_emergency_plan_binding;
DROP TABLE IF EXISTS risk_metric_catalog;
DROP TABLE IF EXISTS linkage_rule;
DROP TABLE IF EXISTS iot_event_work_order;
DROP TABLE IF EXISTS iot_event_record;
DROP TABLE IF EXISTS iot_alarm_record;
DROP TABLE IF EXISTS emergency_plan;

-- 表：emergency_plan
-- 说明：应急预案表
CREATE TABLE emergency_plan (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  plan_name VARCHAR(128) NOT NULL COMMENT '预案名称',
  alarm_level VARCHAR(16) DEFAULT NULL COMMENT '适用告警等级（红/橙/黄/蓝）',
  risk_level VARCHAR(20) DEFAULT NULL COMMENT '历史风险等级兼容字段',
  description VARCHAR(512) DEFAULT NULL COMMENT '描述',
  response_steps LONGTEXT DEFAULT NULL COMMENT '响应步骤(JSON)',
  contact_list LONGTEXT DEFAULT NULL COMMENT '联系人列表(JSON)',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0启用 1停用',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id)
) COMMENT='应急预案表';

-- 表：iot_alarm_record
-- 说明：告警记录表
CREATE TABLE iot_alarm_record (
  id BIGINT NOT NULL COMMENT '主键',
  alarm_code VARCHAR(64) NOT NULL COMMENT '告警编号',
  alarm_title VARCHAR(255) NOT NULL COMMENT '告警标题',
  alarm_type VARCHAR(32) NOT NULL COMMENT '告警类型',
  alarm_level VARCHAR(16) NOT NULL COMMENT '适用告警等级（红/橙/黄/蓝）',
  region_id BIGINT DEFAULT NULL COMMENT '区域ID',
  region_name VARCHAR(128) DEFAULT NULL COMMENT '区域名称',
  risk_point_id BIGINT DEFAULT NULL COMMENT '风险点ID',
  risk_point_name VARCHAR(128) DEFAULT NULL COMMENT '风险点名称',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  device_code VARCHAR(64) NOT NULL COMMENT '设备编码',
  device_name VARCHAR(128) NOT NULL COMMENT '设备名称',
  metric_name VARCHAR(128) DEFAULT NULL COMMENT '测点名称',
  current_value VARCHAR(255) DEFAULT NULL COMMENT '当前值',
  threshold_value VARCHAR(255) DEFAULT NULL COMMENT '阈值',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-未确认 1-已确认 2-已抑制 3-已关闭',
  trigger_time DATETIME NOT NULL COMMENT '触发时间',
  confirm_time DATETIME DEFAULT NULL COMMENT '确认时间',
  confirm_user BIGINT DEFAULT NULL COMMENT '确认用户',
  suppress_time DATETIME DEFAULT NULL COMMENT '抑制时间',
  suppress_user BIGINT DEFAULT NULL COMMENT '抑制用户',
  close_time DATETIME DEFAULT NULL COMMENT '关闭时间',
  close_user BIGINT DEFAULT NULL COMMENT '关闭用户',
  rule_id BIGINT DEFAULT NULL COMMENT '规则ID',
  rule_name VARCHAR(128) DEFAULT NULL COMMENT '规则名称',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_alarm_code (alarm_code),
  KEY idx_device_status (device_code, status),
  KEY idx_trigger_time (trigger_time),
  KEY idx_status (status)
) COMMENT='告警记录表';

-- 表：iot_event_record
-- 说明：事件记录表
CREATE TABLE iot_event_record (
  id BIGINT NOT NULL COMMENT '主键',
  event_code VARCHAR(64) NOT NULL COMMENT '事件编号',
  event_title VARCHAR(255) NOT NULL COMMENT '事件标题',
  alarm_id BIGINT DEFAULT NULL COMMENT '告警ID',
  alarm_code VARCHAR(64) DEFAULT NULL COMMENT '告警编号',
  alarm_level VARCHAR(16) DEFAULT NULL COMMENT '适用告警等级（红/橙/黄/蓝）',
  risk_level VARCHAR(16) DEFAULT NULL COMMENT '风险等级',
  region_id BIGINT DEFAULT NULL COMMENT '区域ID',
  region_name VARCHAR(128) DEFAULT NULL COMMENT '区域名称',
  risk_point_id BIGINT DEFAULT NULL COMMENT '风险点ID',
  risk_point_name VARCHAR(128) DEFAULT NULL COMMENT '风险点名称',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  device_code VARCHAR(64) NOT NULL COMMENT '设备编码',
  device_name VARCHAR(128) NOT NULL COMMENT '设备名称',
  metric_name VARCHAR(128) DEFAULT NULL COMMENT '测点名称',
  current_value VARCHAR(255) DEFAULT NULL COMMENT '当前值',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待派发 1-已派发 2-处理中 3-待验收 4-已关闭 5-已取消',
  responsible_user BIGINT DEFAULT NULL COMMENT '责任人',
  urgency_level VARCHAR(16) DEFAULT NULL COMMENT '紧急程度',
  arrival_time_limit INT DEFAULT NULL COMMENT '到场时限（分钟）',
  completion_time_limit INT DEFAULT NULL COMMENT '完成时限（分钟）',
  trigger_time DATETIME NOT NULL COMMENT '触发时间',
  dispatch_time DATETIME DEFAULT NULL COMMENT '派发时间',
  dispatch_user BIGINT DEFAULT NULL COMMENT '派发用户',
  start_time DATETIME DEFAULT NULL COMMENT '处理开始时间',
  complete_time DATETIME DEFAULT NULL COMMENT '处理完成时间',
  close_time DATETIME DEFAULT NULL COMMENT '关闭时间',
  close_user BIGINT DEFAULT NULL COMMENT '关闭用户',
  close_reason VARCHAR(500) DEFAULT NULL COMMENT '关闭原因',
  review_notes TEXT DEFAULT NULL COMMENT '复盘记录',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_event_code (event_code),
  KEY idx_device_status (device_code, status),
  KEY idx_trigger_time (trigger_time),
  KEY idx_status (status)
) COMMENT='事件记录表';

-- 表：iot_event_work_order
-- 说明：事件工单表
CREATE TABLE iot_event_work_order (
  id BIGINT NOT NULL COMMENT '主键',
  event_id BIGINT NOT NULL COMMENT '事件ID',
  event_code VARCHAR(64) NOT NULL COMMENT '事件编号',
  work_order_code VARCHAR(64) NOT NULL COMMENT '工单编号',
  work_order_type VARCHAR(32) NOT NULL DEFAULT 'event-dispatch' COMMENT '工单类型',
  assign_user BIGINT NOT NULL COMMENT '派发用户',
  receive_user BIGINT DEFAULT NULL COMMENT '接收用户',
  receive_time DATETIME DEFAULT NULL COMMENT '接收时间',
  start_time DATETIME DEFAULT NULL COMMENT '开始时间',
  complete_time DATETIME DEFAULT NULL COMMENT '完成时间',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待接收 1-已接收 2-处理中 3-已完成 4-已取消',
  feedback TEXT DEFAULT NULL COMMENT '现场反馈',
  photos LONGTEXT DEFAULT NULL COMMENT '照片URL（JSON数组）',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_work_order_code (work_order_code),
  KEY idx_event_status (event_id, status),
  KEY idx_receive_time (receive_time)
) COMMENT='事件工单表';

-- 表：linkage_rule
-- 说明：联动规则表
CREATE TABLE linkage_rule (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
  description VARCHAR(512) DEFAULT NULL COMMENT '描述',
  trigger_condition LONGTEXT DEFAULT NULL COMMENT '触发条件(JSON)',
  action_list LONGTEXT DEFAULT NULL COMMENT '动作列表(JSON)',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0启用 1停用',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id)
) COMMENT='联动规则表';

-- 表：risk_metric_catalog
-- 说明：风险指标目录表
CREATE TABLE risk_metric_catalog (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  product_id BIGINT NOT NULL COMMENT '产品ID',
  release_batch_id BIGINT DEFAULT NULL COMMENT '发布批次ID',
  product_model_id BIGINT DEFAULT NULL COMMENT '产品物模型ID',
  normative_identifier VARCHAR(64) DEFAULT NULL COMMENT '规范指标标识',
  contract_identifier VARCHAR(64) NOT NULL COMMENT '合同字段标识',
  risk_metric_code VARCHAR(64) NOT NULL COMMENT '风险指标编码',
  risk_metric_name VARCHAR(128) NOT NULL COMMENT '风险指标名称',
  risk_category VARCHAR(64) DEFAULT NULL COMMENT '风险类别',
  metric_role VARCHAR(32) DEFAULT NULL COMMENT '指标角色',
  lifecycle_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '生命周期状态',
  source_scenario_code VARCHAR(64) DEFAULT NULL COMMENT '来源场景编码',
  metric_unit VARCHAR(32) DEFAULT NULL COMMENT '指标单位',
  metric_dimension VARCHAR(64) DEFAULT NULL COMMENT '指标量纲',
  threshold_type VARCHAR(32) DEFAULT NULL COMMENT '阈值类型',
  semantic_direction VARCHAR(32) DEFAULT NULL COMMENT '语义方向',
  threshold_direction VARCHAR(32) DEFAULT NULL COMMENT '阈值方向',
  trend_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用趋势展示',
  gis_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用地图展示',
  insight_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用对象洞察',
  analytics_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用运营分析',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '启用标记',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_risk_metric_catalog (product_id, contract_identifier)
) COMMENT='风险指标目录表';

-- 表：risk_metric_emergency_plan_binding
-- 说明：风险指标与应急预案绑定表
CREATE TABLE risk_metric_emergency_plan_binding (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  risk_metric_id BIGINT NOT NULL COMMENT '风险指标ID',
  emergency_plan_id BIGINT NOT NULL COMMENT '应急预案ID',
  binding_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '绑定状态业务字段',
  binding_origin VARCHAR(32) NOT NULL DEFAULT 'AUTO_INFERRED' COMMENT '绑定来源',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_risk_metric_plan_active (tenant_id, risk_metric_id, emergency_plan_id, deleted),
  KEY idx_risk_metric_plan_rule (emergency_plan_id, binding_status, deleted),
  KEY idx_risk_metric_plan_metric (risk_metric_id, binding_status, deleted)
) COMMENT='风险指标与应急预案绑定表';

-- 表：risk_metric_linkage_binding
-- 说明：风险指标与联动规则绑定表
CREATE TABLE risk_metric_linkage_binding (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  risk_metric_id BIGINT NOT NULL COMMENT '风险指标ID',
  linkage_rule_id BIGINT NOT NULL COMMENT '联动规则ID',
  binding_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '绑定状态业务字段',
  binding_origin VARCHAR(32) NOT NULL DEFAULT 'AUTO_INFERRED' COMMENT '绑定来源',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_risk_metric_linkage_active (tenant_id, risk_metric_id, linkage_rule_id, deleted),
  KEY idx_risk_metric_linkage_rule (linkage_rule_id, binding_status, deleted),
  KEY idx_risk_metric_linkage_metric (risk_metric_id, binding_status, deleted)
) COMMENT='风险指标与联动规则绑定表';

-- 表：risk_point
-- 说明：风险点表
CREATE TABLE risk_point (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  risk_point_code VARCHAR(64) NOT NULL COMMENT '风险点编号',
  risk_point_name VARCHAR(128) NOT NULL COMMENT '风险点名称',
  org_id BIGINT DEFAULT NULL COMMENT '所属组织ID',
  org_name VARCHAR(128) DEFAULT NULL COMMENT '所属组织名称',
  region_id BIGINT DEFAULT NULL COMMENT '区域ID',
  region_name VARCHAR(128) DEFAULT NULL COMMENT '区域名称',
  responsible_user BIGINT DEFAULT NULL COMMENT '负责人',
  responsible_phone VARCHAR(32) DEFAULT NULL COMMENT '负责人电话',
  risk_point_level VARCHAR(16) DEFAULT NULL COMMENT '风险点档案等级（一级/二级/三级）',
  current_risk_level VARCHAR(16) DEFAULT NULL COMMENT '当前风险态势等级（红/橙/黄/蓝）',
  risk_level VARCHAR(20) DEFAULT NULL COMMENT '历史风险等级兼容字段',
  risk_type VARCHAR(32) NOT NULL DEFAULT 'GENERAL' COMMENT '风险点类型 SLOPE/BRIDGE/TUNNEL/GENERAL',
  location_text VARCHAR(255) DEFAULT NULL COMMENT '位置描述/桩号/区间',
  longitude DECIMAL(10,6) DEFAULT NULL COMMENT '风险点经度',
  latitude DECIMAL(10,6) DEFAULT NULL COMMENT '风险点纬度',
  description VARCHAR(1000) DEFAULT NULL COMMENT '描述',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-启用 1-停用',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_risk_point_code_tenant (tenant_id, risk_point_code),
  KEY idx_region (region_id),
  KEY idx_status (status),
  KEY idx_risk_type_status (risk_type, status)
) COMMENT='风险点表';

-- 表：risk_point_device
-- 说明：风险点设备绑定表
CREATE TABLE risk_point_device (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  risk_point_id BIGINT NOT NULL COMMENT '风险点ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
  device_name VARCHAR(128) DEFAULT NULL COMMENT '设备名称',
  risk_metric_id BIGINT DEFAULT NULL COMMENT '风险指标ID',
  metric_identifier VARCHAR(64) NOT NULL COMMENT '测点标识符',
  metric_name VARCHAR(64) DEFAULT NULL COMMENT '测点名称',
  default_threshold VARCHAR(64) DEFAULT NULL COMMENT '默认阈值',
  threshold_unit VARCHAR(20) DEFAULT NULL COMMENT '阈值单位',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_risk_point_metric (risk_point_id, device_id, metric_identifier),
  KEY idx_risk_device (risk_point_id, device_id),
  KEY idx_risk_point_device_metric_catalog (risk_metric_id)
) COMMENT='风险点设备绑定表';

-- 表：risk_point_device_capability_binding
-- 说明：风险点设备级正式绑定表
CREATE TABLE risk_point_device_capability_binding (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  risk_point_id BIGINT NOT NULL COMMENT '风险点ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
  device_name VARCHAR(128) DEFAULT NULL COMMENT '设备名称',
  device_capability_type VARCHAR(32) NOT NULL COMMENT '设备能力类型',
  extension_status VARCHAR(64) DEFAULT NULL COMMENT '扩展能力状态',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_risk_point_device_only (risk_point_id, device_id),
  KEY idx_risk_point_device_only (risk_point_id, device_id),
  KEY idx_risk_device_capability_type (tenant_id, device_capability_type, deleted)
) COMMENT='风险点设备级正式绑定表';

-- 表：risk_point_device_pending_binding
-- 说明：风险点设备待治理导入表
CREATE TABLE risk_point_device_pending_binding (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  batch_no VARCHAR(64) NOT NULL COMMENT '导入批次号',
  source_file_name VARCHAR(255) DEFAULT NULL COMMENT '来源文件名',
  source_row_no INT NOT NULL COMMENT '导入源行号',
  risk_point_name VARCHAR(128) NOT NULL COMMENT '来源风险点名称',
  risk_point_id BIGINT DEFAULT NULL COMMENT '匹配到的风险点ID',
  risk_point_code VARCHAR(64) DEFAULT NULL COMMENT '匹配到的风险点编号',
  device_code VARCHAR(64) NOT NULL COMMENT '来源设备编码',
  device_id BIGINT DEFAULT NULL COMMENT '匹配到的设备ID',
  device_name VARCHAR(128) DEFAULT NULL COMMENT '匹配到的设备名称',
  resolution_status VARCHAR(64) NOT NULL DEFAULT 'PENDING_METRIC_GOVERNANCE' COMMENT '治理状态',
  resolution_note VARCHAR(500) DEFAULT NULL COMMENT '治理说明',
  metric_identifier VARCHAR(64) DEFAULT NULL COMMENT '后续补录测点标识',
  metric_name VARCHAR(128) DEFAULT NULL COMMENT '后续补录测点名称',
  promoted_binding_id BIGINT DEFAULT NULL COMMENT '转正后的正式绑定ID',
  promoted_time DATETIME DEFAULT NULL COMMENT '转正时间',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_pending_binding_batch_row (tenant_id, batch_no, source_row_no),
  KEY idx_pending_binding_status (tenant_id, resolution_status, deleted),
  KEY idx_pending_binding_risk_device (risk_point_id, device_id, deleted),
  KEY idx_pending_binding_device_code (tenant_id, device_code, deleted)
) COMMENT='风险点设备待治理导入表';

-- 表：risk_point_device_pending_promotion
-- 说明：风险点设备待治理转正明细表
CREATE TABLE risk_point_device_pending_promotion (
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
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_pending_promotion_pending_id (pending_binding_id),
  KEY idx_pending_promotion_binding_id (risk_point_device_id),
  KEY idx_pending_promotion_status (tenant_id, promotion_status, deleted)
) COMMENT='风险点设备待治理转正明细表';

-- 表：rule_definition
-- 说明：阈值规则表
CREATE TABLE rule_definition (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
  risk_metric_id BIGINT DEFAULT NULL COMMENT '风险指标ID',
  metric_identifier VARCHAR(64) NOT NULL COMMENT '测点标识符',
  metric_name VARCHAR(64) DEFAULT NULL COMMENT '测点名称',
  rule_scope VARCHAR(32) NOT NULL DEFAULT 'METRIC' COMMENT '策略作用域',
  product_id BIGINT DEFAULT NULL COMMENT '产品ID',
  device_id BIGINT DEFAULT NULL COMMENT '设备ID',
  risk_point_device_id BIGINT DEFAULT NULL COMMENT '风险点设备绑定ID',
  expression VARCHAR(256) DEFAULT NULL COMMENT '表达式',
  duration INT NOT NULL DEFAULT 0 COMMENT '持续时间(秒)',
  alarm_level VARCHAR(20) DEFAULT NULL COMMENT '适用告警等级（红/橙/黄/蓝）',
  notification_methods VARCHAR(64) DEFAULT NULL COMMENT '通知方式',
  convert_to_event TINYINT NOT NULL DEFAULT 0 COMMENT '是否转事件',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0启用 1停用',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_metric_identifier (metric_identifier),
  KEY idx_rule_definition_metric_catalog (risk_metric_id)
) COMMENT='阈值规则表';

-- 表：iot_command_record
-- 说明：设备命令记录表

CREATE TABLE iot_command_record (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  command_id VARCHAR(64) NOT NULL COMMENT '业务命令ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  device_code VARCHAR(64) NOT NULL COMMENT '设备编码',
  product_key VARCHAR(64) NOT NULL COMMENT '产品标识',
  gateway_device_code VARCHAR(64) DEFAULT NULL COMMENT '网关设备编码',
  sub_device_code VARCHAR(64) DEFAULT NULL COMMENT '子设备编码',
  topic VARCHAR(255) NOT NULL COMMENT '下发主题',
  command_type VARCHAR(32) NOT NULL COMMENT '命令类型（属性下发/服务调用）',
  service_identifier VARCHAR(64) DEFAULT NULL COMMENT '服务标识',
  request_payload LONGTEXT DEFAULT NULL COMMENT '下发请求报文',
  reply_payload LONGTEXT DEFAULT NULL COMMENT '设备回执报文',
  qos TINYINT NOT NULL DEFAULT 0 COMMENT '服务质量等级',
  retained TINYINT NOT NULL DEFAULT 0 COMMENT '是否保留消息 1是 0否',
  status VARCHAR(32) NOT NULL COMMENT '命令状态 CREATED/SENT/SUCCESS/FAILED/TIMEOUT',
  send_time DATETIME DEFAULT NULL COMMENT '发送时间',
  ack_time DATETIME DEFAULT NULL COMMENT '回执时间',
  timeout_time DATETIME DEFAULT NULL COMMENT '超时时间',
  error_message VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_command_id (command_id),
  KEY idx_device_status (device_code, status),
  KEY idx_status_timeout (status, timeout_time)
) COMMENT='设备命令记录表';

-- 表：iot_device
-- 说明：设备表
CREATE TABLE iot_device (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  org_id BIGINT DEFAULT NULL COMMENT '所属机构ID',
  org_name VARCHAR(128) DEFAULT NULL COMMENT '所属机构名称',
  product_id BIGINT NOT NULL COMMENT '产品ID',
  gateway_id BIGINT DEFAULT NULL COMMENT '所属网关ID',
  parent_device_id BIGINT DEFAULT NULL COMMENT '父设备ID',
  device_name VARCHAR(128) NOT NULL COMMENT '设备名称',
  device_code VARCHAR(64) NOT NULL COMMENT '设备编码',
  device_secret VARCHAR(128) DEFAULT NULL COMMENT '设备密钥',
  client_id VARCHAR(128) DEFAULT NULL COMMENT '客户端ID',
  username VARCHAR(128) DEFAULT NULL COMMENT '接入用户名',
  password VARCHAR(128) DEFAULT NULL COMMENT '接入密码',
  protocol_code VARCHAR(64) NOT NULL COMMENT '协议编码',
  node_type TINYINT NOT NULL DEFAULT 1 COMMENT '节点类型 1直连设备 2网关设备 3子设备',
  online_status TINYINT NOT NULL DEFAULT 0 COMMENT '在线状态 1在线 0离线',
  activate_status TINYINT NOT NULL DEFAULT 0 COMMENT '激活状态 1已激活 0未激活',
  device_status TINYINT NOT NULL DEFAULT 1 COMMENT '设备状态 1启用 0禁用',
  firmware_version VARCHAR(64) DEFAULT NULL COMMENT '固件版本',
  ip_address VARCHAR(64) DEFAULT NULL COMMENT '设备IP',
  last_online_time DATETIME DEFAULT NULL COMMENT '最后上线时间',
  last_offline_time DATETIME DEFAULT NULL COMMENT '最后离线时间',
  last_report_time DATETIME DEFAULT NULL COMMENT '最后上报时间',
  longitude DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  latitude DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  address VARCHAR(255) DEFAULT NULL COMMENT '安装地址',
  metadata_json JSON DEFAULT NULL COMMENT '设备扩展信息',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_device_code_tenant (tenant_id, device_code),
  KEY idx_device_tenant_org_deleted (tenant_id, org_id, deleted, last_report_time, id),
  KEY idx_device_deleted_product_stats (deleted, product_id, last_report_time, online_status)
) COMMENT='设备表';

-- 表：iot_device_access_error_log
-- 说明：设备接入失败归档表
CREATE TABLE iot_device_access_error_log (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  protocol_code VARCHAR(64) DEFAULT NULL COMMENT '协议编码',
  request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方式',
  failure_stage VARCHAR(32) DEFAULT NULL COMMENT '失败阶段',
  device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
  product_key VARCHAR(64) DEFAULT NULL COMMENT '产品标识',
  gateway_device_code VARCHAR(64) DEFAULT NULL COMMENT '网关设备编码',
  sub_device_code VARCHAR(64) DEFAULT NULL COMMENT '子设备编码',
  topic_route_type VARCHAR(32) DEFAULT NULL COMMENT '主题路由类型',
  message_type VARCHAR(32) DEFAULT NULL COMMENT '消息类型',
  topic VARCHAR(255) DEFAULT NULL COMMENT '消息主题',
  client_id VARCHAR(128) DEFAULT NULL COMMENT '客户端ID',
  payload_size INT DEFAULT NULL COMMENT '载荷大小',
  payload_encoding VARCHAR(16) DEFAULT NULL COMMENT '载荷编码',
  payload_truncated TINYINT NOT NULL DEFAULT 0 COMMENT '载荷是否截断',
  raw_payload LONGTEXT DEFAULT NULL COMMENT '原始载荷',
  error_code VARCHAR(64) DEFAULT NULL COMMENT '错误编码',
  exception_class VARCHAR(255) DEFAULT NULL COMMENT '异常类型',
  error_message VARCHAR(500) DEFAULT NULL COMMENT '错误消息',
  contract_snapshot LONGTEXT DEFAULT NULL COMMENT '设备契约快照',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_access_error_trace (trace_id),
  KEY idx_access_error_device_time (device_code, create_time),
  KEY idx_access_error_stage_time (failure_stage, create_time)
) COMMENT='设备接入失败归档表';

-- 表：iot_device_invalid_report_state
-- 说明：无效 MQTT 上报最新态表
CREATE TABLE iot_device_invalid_report_state (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  governance_key VARCHAR(255) NOT NULL COMMENT '治理唯一键',
  reason_code VARCHAR(64) NOT NULL COMMENT '治理原因编码',
  request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方式',
  failure_stage VARCHAR(32) DEFAULT NULL COMMENT '失败阶段',
  device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
  product_key VARCHAR(64) DEFAULT NULL COMMENT '产品标识',
  protocol_code VARCHAR(64) DEFAULT NULL COMMENT '协议编码',
  topic_route_type VARCHAR(32) DEFAULT NULL COMMENT '主题路由类型',
  topic VARCHAR(255) DEFAULT NULL COMMENT '最近主题',
  client_id VARCHAR(128) DEFAULT NULL COMMENT '最近客户端标识',
  payload_size INT DEFAULT NULL COMMENT '载荷大小',
  payload_encoding VARCHAR(16) DEFAULT NULL COMMENT '载荷编码',
  last_payload LONGTEXT DEFAULT NULL COMMENT '最近载荷',
  last_trace_id VARCHAR(64) DEFAULT NULL COMMENT '最近链路追踪ID',
  sample_error_message VARCHAR(500) DEFAULT NULL COMMENT '样本错误消息',
  sample_exception_class VARCHAR(255) DEFAULT NULL COMMENT '样本异常类',
  first_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次命中时间',
  last_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近命中时间',
  hit_count BIGINT NOT NULL DEFAULT 0 COMMENT '总命中次数',
  sampled_count BIGINT NOT NULL DEFAULT 0 COMMENT '已采样次数',
  suppressed_count BIGINT NOT NULL DEFAULT 0 COMMENT '被抑制次数',
  suppressed_until DATETIME DEFAULT NULL COMMENT '抑制截止时间',
  resolved TINYINT NOT NULL DEFAULT 0 COMMENT '是否已解封',
  resolved_time DATETIME DEFAULT NULL COMMENT '解封时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_invalid_report_state_governance_key (governance_key),
  KEY idx_invalid_report_device_resolved (device_code, product_key, resolved, last_seen_time),
  KEY idx_invalid_report_reason_time (reason_code, last_seen_time)
) COMMENT='无效 MQTT 上报最新态表';

-- 表：iot_device_metric_latest
-- 说明：时序最新值投影表
CREATE TABLE iot_device_metric_latest (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  product_id BIGINT NOT NULL COMMENT '产品ID',
  metric_id VARCHAR(128) NOT NULL COMMENT '指标唯一键',
  metric_code VARCHAR(128) NOT NULL COMMENT '指标编码',
  metric_name VARCHAR(128) DEFAULT NULL COMMENT '指标名称',
  value_type VARCHAR(32) DEFAULT NULL COMMENT '值类型',
  value_double DOUBLE DEFAULT NULL COMMENT '数值',
  value_long BIGINT DEFAULT NULL COMMENT '整数值',
  value_bool TINYINT(1) DEFAULT NULL COMMENT '布尔值',
  value_text TEXT DEFAULT NULL COMMENT '文本值',
  quality_code VARCHAR(32) DEFAULT NULL COMMENT '质量编码',
  alarm_flag TINYINT(1) DEFAULT NULL COMMENT '告警标记',
  reported_at DATETIME DEFAULT NULL COMMENT '设备上报时间',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tel_latest_tenant_device_metric (tenant_id, device_id, metric_id),
  KEY idx_tel_latest_device_reported (device_id, reported_at)
) COMMENT='时序最新值投影表';

-- 表：iot_device_onboarding_case
-- 说明：设备无代码接入案例表
CREATE TABLE iot_device_onboarding_case (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  case_code VARCHAR(64) NOT NULL COMMENT '案例编码',
  case_name VARCHAR(128) NOT NULL COMMENT '案例名称',
  scenario_code VARCHAR(64) DEFAULT NULL COMMENT '场景编码',
  device_family VARCHAR(64) DEFAULT NULL COMMENT '设备族',
  protocol_family_code VARCHAR(64) DEFAULT NULL COMMENT '协议族编码',
  decrypt_profile_code VARCHAR(64) DEFAULT NULL COMMENT '解密档案编码',
  protocol_template_code VARCHAR(64) DEFAULT NULL COMMENT '协议模板编码',
  template_pack_id BIGINT DEFAULT NULL COMMENT '模板包ID',
  product_id BIGINT DEFAULT NULL COMMENT '产品ID',
  release_batch_id BIGINT DEFAULT NULL COMMENT '合同发布批次ID',
  device_code VARCHAR(64) DEFAULT NULL COMMENT '验收设备编码',
  acceptance_job_id VARCHAR(64) DEFAULT NULL COMMENT '标准接入验收任务ID',
  acceptance_run_id VARCHAR(64) DEFAULT NULL COMMENT '标准接入验收运行ID',
  current_step VARCHAR(32) NOT NULL DEFAULT 'PROTOCOL_GOVERNANCE' COMMENT '当前步骤',
  status VARCHAR(32) NOT NULL DEFAULT 'BLOCKED' COMMENT '状态',
  blocker_summary_json JSON DEFAULT NULL COMMENT '阻塞摘要JSON',
  evidence_summary_json JSON DEFAULT NULL COMMENT '证据摘要JSON',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_onboarding_case_code (tenant_id, case_code),
  KEY idx_onboarding_case_step_status (tenant_id, current_step, status),
  KEY idx_onboarding_case_template_pack (tenant_id, template_pack_id)
) COMMENT='设备无代码接入案例表';

-- 表：iot_device_online_session
-- 说明：设备在线会话表
CREATE TABLE iot_device_online_session (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  product_id BIGINT NOT NULL COMMENT '产品ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  device_code VARCHAR(64) NOT NULL COMMENT '设备编码',
  online_time DATETIME NOT NULL COMMENT '会话开始时间',
  last_seen_time DATETIME DEFAULT NULL COMMENT '会话最后活跃时间',
  offline_time DATETIME DEFAULT NULL COMMENT '会话结束时间',
  duration_minutes BIGINT DEFAULT NULL COMMENT '在线时长（分钟）',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_online_session_device_active (deleted, device_id, offline_time),
  KEY idx_online_session_product_time (deleted, product_id, online_time, offline_time)
) COMMENT='设备在线会话表';

-- 表：iot_device_property
-- 说明：设备最新属性表
CREATE TABLE iot_device_property (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  identifier VARCHAR(64) NOT NULL COMMENT '属性标识',
  property_name VARCHAR(128) DEFAULT NULL COMMENT '属性名称',
  property_value VARCHAR(1024) DEFAULT NULL COMMENT '属性值',
  value_type VARCHAR(32) DEFAULT NULL COMMENT '值类型',
  report_time DATETIME NOT NULL COMMENT '上报时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_device_identifier (device_id, identifier)
) COMMENT='设备最新属性表';

-- 表：iot_device_relation
-- 说明：设备逻辑通道关系表
CREATE TABLE iot_device_relation (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  parent_device_id BIGINT NOT NULL COMMENT '父设备ID',
  parent_device_code VARCHAR(64) NOT NULL COMMENT '父设备编码',
  logical_channel_code VARCHAR(64) NOT NULL COMMENT '逻辑通道编码',
  child_device_id BIGINT NOT NULL COMMENT '子设备ID',
  child_device_code VARCHAR(64) NOT NULL COMMENT '子设备编码',
  child_product_id BIGINT DEFAULT NULL COMMENT '子产品ID',
  child_product_key VARCHAR(64) DEFAULT NULL COMMENT '子产品标识',
  relation_type VARCHAR(32) NOT NULL COMMENT '关系类型（采集器子设备/网关子设备）',
  canonicalization_strategy VARCHAR(32) NOT NULL COMMENT '归一化策略 LEGACY/LF_VALUE',
  status_mirror_strategy VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '状态镜像策略 NONE/SENSOR_STATE',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_relation_parent_channel (tenant_id, parent_device_id, logical_channel_code, deleted),
  UNIQUE KEY uk_relation_parent_code_channel (tenant_id, parent_device_code, logical_channel_code, deleted),
  KEY idx_relation_parent_code (tenant_id, parent_device_code, enabled, deleted),
  KEY idx_relation_child_code (tenant_id, child_device_code, enabled, deleted)
) COMMENT='设备逻辑通道关系表';

-- 表：iot_device_secret_rotation_log
-- 说明：设备密钥轮换日志表
CREATE TABLE iot_device_secret_rotation_log (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  device_code VARCHAR(64) NOT NULL COMMENT '设备编码',
  product_key VARCHAR(64) DEFAULT NULL COMMENT '产品标识',
  rotation_batch_id VARCHAR(64) NOT NULL COMMENT '轮换批次ID',
  reason VARCHAR(500) DEFAULT NULL COMMENT '轮换原因',
  previous_secret_digest VARCHAR(128) DEFAULT NULL COMMENT '上一版密钥摘要',
  current_secret_digest VARCHAR(128) DEFAULT NULL COMMENT '当前密钥摘要',
  rotated_by BIGINT NOT NULL COMMENT '轮换执行人',
  approved_by BIGINT NOT NULL COMMENT '审批人',
  rotate_time DATETIME NOT NULL COMMENT '轮换时间',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_device_rotation_device_time (device_id, rotate_time),
  KEY idx_device_rotation_batch (rotation_batch_id)
) COMMENT='设备密钥轮换日志表';

-- 表：iot_message_log
-- 说明：设备消息日志表
CREATE TABLE iot_message_log (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  product_id BIGINT DEFAULT NULL COMMENT '产品ID',
  message_type VARCHAR(32) NOT NULL COMMENT '消息类型（遥测/事件/属性/应答）',
  topic VARCHAR(255) DEFAULT NULL COMMENT '主题',
  payload JSON DEFAULT NULL COMMENT '原始消息',
  report_time DATETIME NOT NULL COMMENT '上报时间',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
  product_key VARCHAR(64) DEFAULT NULL COMMENT '产品标识',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志创建时间',
  PRIMARY KEY (id),
  KEY idx_device_time (device_id, report_time),
  KEY idx_message_type (message_type),
  KEY idx_trace_id (trace_id),
  KEY idx_device_code_time (device_code, report_time)
) COMMENT='设备消息日志表';

-- 表：iot_message_log_archive
-- 说明：设备消息日志冷归档表
CREATE TABLE iot_message_log_archive (
  id BIGINT NOT NULL COMMENT '主键',
  original_log_id BIGINT NOT NULL COMMENT '原热表日志ID',
  archive_batch_id BIGINT NOT NULL COMMENT '归档批次ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  product_id BIGINT DEFAULT NULL COMMENT '产品ID',
  message_type VARCHAR(32) NOT NULL COMMENT '消息类型（遥测/事件/属性/应答）',
  topic VARCHAR(255) DEFAULT NULL COMMENT '主题',
  payload JSON DEFAULT NULL COMMENT '原始消息',
  report_time DATETIME NOT NULL COMMENT '上报时间',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
  product_key VARCHAR(64) DEFAULT NULL COMMENT '产品标识',
  create_time DATETIME NOT NULL COMMENT '原日志创建时间',
  archived_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_original_log_id (original_log_id),
  KEY idx_archive_batch_id (archive_batch_id),
  KEY idx_trace_id_report_time (trace_id, report_time),
  KEY idx_device_code_report_time (device_code, report_time)
) COMMENT='设备消息日志冷归档表';

-- 表：iot_message_log_archive_batch
-- 说明：设备消息日志冷归档批次表
CREATE TABLE iot_message_log_archive_batch (
  id BIGINT NOT NULL COMMENT '主键',
  batch_no VARCHAR(64) NOT NULL COMMENT '批次号',
  source_table VARCHAR(64) NOT NULL COMMENT '来源热表',
  governance_mode VARCHAR(16) NOT NULL COMMENT '治理模式',
  status VARCHAR(32) NOT NULL COMMENT '批次状态',
  retention_days INT NOT NULL COMMENT '保留天数',
  cutoff_at DATETIME NOT NULL COMMENT '过期阈值时间',
  confirm_report_path VARCHAR(500) DEFAULT NULL COMMENT '确认报告路径',
  confirm_report_generated_at DATETIME DEFAULT NULL COMMENT '确认报告生成时间',
  confirmed_expired_rows INT NOT NULL DEFAULT 0 COMMENT '确认过期行数',
  candidate_rows INT NOT NULL DEFAULT 0 COMMENT '候选过期行数',
  archived_rows INT NOT NULL DEFAULT 0 COMMENT '已归档行数',
  deleted_rows INT NOT NULL DEFAULT 0 COMMENT '已删除行数',
  failed_reason VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
  artifacts_json JSON DEFAULT NULL COMMENT '批次附加证据',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_batch_no (batch_no),
  KEY idx_source_status_time (source_table, status, create_time)
) COMMENT='设备消息日志冷归档批次表';

-- 表：iot_normative_metric_definition
-- 说明：规范字段定义表
CREATE TABLE iot_normative_metric_definition (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  scenario_code VARCHAR(64) NOT NULL COMMENT '场景编码',
  device_family VARCHAR(64) NOT NULL COMMENT '设备族编码',
  identifier VARCHAR(64) NOT NULL COMMENT '规范字段标识',
  display_name VARCHAR(128) NOT NULL COMMENT '规范字段名称',
  unit VARCHAR(32) DEFAULT NULL COMMENT '单位',
  precision_digits INT DEFAULT NULL COMMENT '精度',
  monitor_content_code VARCHAR(32) DEFAULT NULL COMMENT '监测内容编码',
  monitor_type_code VARCHAR(32) DEFAULT NULL COMMENT '监测类型编码',
  risk_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许进入风险闭环',
  trend_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用趋势展示',
  metric_dimension VARCHAR(64) DEFAULT NULL COMMENT '指标量纲',
  threshold_type VARCHAR(32) DEFAULT NULL COMMENT '阈值类型',
  semantic_direction VARCHAR(32) DEFAULT NULL COMMENT '语义方向',
  gis_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用地图展示',
  insight_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用对象洞察',
  analytics_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用运营分析',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
  metadata_json JSON DEFAULT NULL COMMENT '扩展元数据',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_normative_metric_scenario_identifier (scenario_code, identifier)
) COMMENT='规范字段定义表';

-- 表：iot_onboarding_template_pack
-- 说明：设备无代码接入模板包表
CREATE TABLE iot_onboarding_template_pack (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  pack_code VARCHAR(64) NOT NULL COMMENT '模板包编码',
  pack_name VARCHAR(128) NOT NULL COMMENT '模板包名称',
  scenario_code VARCHAR(64) DEFAULT NULL COMMENT '场景编码',
  device_family VARCHAR(64) DEFAULT NULL COMMENT '设备族',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '模板包状态',
  version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
  protocol_family_code VARCHAR(64) DEFAULT NULL COMMENT '协议族编码',
  decrypt_profile_code VARCHAR(64) DEFAULT NULL COMMENT '解密档案编码',
  protocol_template_code VARCHAR(64) DEFAULT NULL COMMENT '协议模板编码',
  default_governance_config_json JSON DEFAULT NULL COMMENT '默认治理配置JSON',
  default_insight_config_json JSON DEFAULT NULL COMMENT '默认对象洞察配置JSON',
  default_acceptance_profile_json JSON DEFAULT NULL COMMENT '默认验收配置JSON',
  description VARCHAR(500) DEFAULT NULL COMMENT '描述',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_onboarding_template_pack_code (tenant_id, pack_code),
  KEY idx_onboarding_template_pack_status (tenant_id, status)
) COMMENT='设备无代码接入模板包表';

-- 表：iot_product
-- 说明：产品表
CREATE TABLE iot_product (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  product_key VARCHAR(64) NOT NULL COMMENT '产品标识',
  product_name VARCHAR(128) NOT NULL COMMENT '产品名称',
  protocol_code VARCHAR(64) NOT NULL COMMENT '协议编码',
  node_type TINYINT NOT NULL DEFAULT 1 COMMENT '节点类型 1直连设备 2网关设备 3网关子设备',
  data_format VARCHAR(32) NOT NULL DEFAULT 'JSON' COMMENT '数据格式',
  manufacturer VARCHAR(128) DEFAULT NULL COMMENT '厂商',
  description VARCHAR(500) DEFAULT NULL COMMENT '描述',
  metadata_json JSON DEFAULT NULL COMMENT '产品扩展元数据',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_product_key_tenant (tenant_id, product_key)
) COMMENT='产品表';

-- 表：iot_product_contract_release_batch
-- 说明：产品合同发布批次表
CREATE TABLE iot_product_contract_release_batch (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  product_id BIGINT NOT NULL COMMENT '产品ID',
  scenario_code VARCHAR(64) NOT NULL COMMENT '场景编码',
  release_source VARCHAR(64) NOT NULL COMMENT '发布来源',
  released_field_count INT NOT NULL DEFAULT 0 COMMENT '发布字段数量',
  approval_order_id BIGINT DEFAULT NULL COMMENT '审批单ID',
  release_reason VARCHAR(500) DEFAULT NULL COMMENT '发布原因',
  release_status VARCHAR(16) NOT NULL DEFAULT 'RELEASED' COMMENT '发布状态',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  rollback_by BIGINT DEFAULT NULL COMMENT '回滚执行人',
  rollback_time DATETIME DEFAULT NULL COMMENT '回滚时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_product_contract_release_product_time (product_id, create_time)
) COMMENT='产品合同发布批次表';

-- 表：iot_product_contract_release_snapshot
-- 说明：产品合同发布快照表
CREATE TABLE iot_product_contract_release_snapshot (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  batch_id BIGINT NOT NULL COMMENT '批次ID',
  product_id BIGINT NOT NULL COMMENT '产品ID',
  snapshot_stage VARCHAR(32) NOT NULL COMMENT '快照阶段',
  snapshot_json JSON NOT NULL COMMENT '快照JSON内容',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_release_snapshot_batch_stage (batch_id, snapshot_stage)
) COMMENT='产品合同发布快照表';

-- 表：iot_product_metric_resolver_snapshot
-- 说明：产品指标解析快照表
CREATE TABLE iot_product_metric_resolver_snapshot (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  product_id BIGINT NOT NULL COMMENT '产品ID',
  release_batch_id BIGINT NOT NULL COMMENT '发布批次ID',
  snapshot_json JSON NOT NULL COMMENT '快照JSON内容',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_metric_resolver_snapshot_batch (product_id, release_batch_id, deleted)
) COMMENT='产品指标解析快照表';

-- 表：iot_product_model
-- 说明：产品物模型表
CREATE TABLE iot_product_model (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  product_id BIGINT NOT NULL COMMENT '产品ID',
  model_type VARCHAR(32) NOT NULL COMMENT '模型类型（属性/事件/服务）',
  identifier VARCHAR(64) NOT NULL COMMENT '标识符',
  model_name VARCHAR(128) NOT NULL COMMENT '名称',
  data_type VARCHAR(32) NOT NULL COMMENT '数据类型',
  specs_json JSON DEFAULT NULL COMMENT '规格JSON',
  event_type VARCHAR(32) DEFAULT NULL COMMENT '事件类型',
  service_input_json JSON DEFAULT NULL COMMENT '服务输入定义',
  service_output_json JSON DEFAULT NULL COMMENT '服务输出定义',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
  required_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否必填',
  description VARCHAR(500) DEFAULT NULL COMMENT '描述',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_product_identifier (product_id, model_type, identifier)
) COMMENT='产品物模型表';

-- 表：iot_runtime_metric_display_rule
-- 说明：运行态字段显示规则表
CREATE TABLE iot_runtime_metric_display_rule (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  scope_type VARCHAR(32) NOT NULL COMMENT '作用域类型',
  product_id BIGINT DEFAULT NULL COMMENT '产品ID',
  protocol_code VARCHAR(64) DEFAULT NULL COMMENT '协议编码',
  scenario_code VARCHAR(64) DEFAULT NULL COMMENT '场景编码',
  device_family VARCHAR(64) DEFAULT NULL COMMENT '设备族编码',
  raw_identifier VARCHAR(128) NOT NULL COMMENT '运行态原始字段标识',
  display_name VARCHAR(128) NOT NULL COMMENT '显示名称',
  unit VARCHAR(64) DEFAULT NULL COMMENT '显示单位',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_runtime_metric_display_rule_lookup (product_id, raw_identifier, scope_type, deleted),
  KEY idx_runtime_metric_display_rule_scope (scope_type, scenario_code, device_family, protocol_code)
) COMMENT='运行态字段显示规则表';

-- 表：iot_vendor_metric_evidence
-- 说明：厂商字段证据表
CREATE TABLE iot_vendor_metric_evidence (
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
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_vendor_metric_evidence (product_id, raw_identifier, logical_channel_code),
  KEY idx_vendor_metric_product_seen (product_id, last_seen_time)
) COMMENT='厂商字段证据表';

-- 表：iot_vendor_metric_mapping_rule
-- 说明：厂商字段映射规则表
CREATE TABLE iot_vendor_metric_mapping_rule (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  scope_type VARCHAR(32) NOT NULL COMMENT '作用域类型',
  product_id BIGINT DEFAULT NULL COMMENT '产品ID',
  protocol_code VARCHAR(64) DEFAULT NULL COMMENT '协议编码',
  scenario_code VARCHAR(64) DEFAULT NULL COMMENT '场景编码',
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
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id)
) COMMENT='厂商字段映射规则表';

-- 表：iot_vendor_metric_mapping_rule_snapshot
-- 说明：厂商字段映射规则发布快照表
CREATE TABLE iot_vendor_metric_mapping_rule_snapshot (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  rule_id BIGINT NOT NULL COMMENT '规则ID',
  product_id BIGINT NOT NULL COMMENT '产品ID',
  approval_order_id BIGINT NOT NULL COMMENT '审批主单ID',
  published_version_no INT NOT NULL COMMENT '发布版本号',
  snapshot_json JSON NOT NULL COMMENT '规则快照JSON',
  lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT '生命周期状态',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_vendor_metric_rule_snapshot_rule_status (rule_id, lifecycle_status, deleted),
  KEY idx_vendor_metric_rule_snapshot_product_status (product_id, lifecycle_status, deleted)
) COMMENT='厂商字段映射规则发布快照表';

-- 表：iot_governance_ops_alert
-- 说明：治理运维告警表
CREATE TABLE iot_governance_ops_alert (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  alert_type VARCHAR(64) NOT NULL COMMENT '告警类型',
  alert_code VARCHAR(128) NOT NULL COMMENT '告警业务编码',
  subject_type VARCHAR(64) NOT NULL COMMENT '审批主体类型',
  subject_id BIGINT DEFAULT NULL COMMENT '主题ID',
  product_id BIGINT DEFAULT NULL COMMENT '产品ID',
  risk_metric_id BIGINT DEFAULT NULL COMMENT '风险指标ID',
  release_batch_id BIGINT DEFAULT NULL COMMENT '发布批次ID',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
  product_key VARCHAR(64) DEFAULT NULL COMMENT '产品标识',
  alert_status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT '告警状态',
  severity_level VARCHAR(16) NOT NULL DEFAULT 'WARN' COMMENT '严重程度等级',
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
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_governance_ops_alert_code (tenant_id, alert_type, alert_code, deleted)
) COMMENT='治理运维告警表';

-- 表：iot_governance_work_item
-- 说明：治理与运营工作项表
CREATE TABLE iot_governance_work_item (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  work_item_code VARCHAR(64) NOT NULL COMMENT '工作项编码',
  subject_type VARCHAR(64) NOT NULL COMMENT '审批主体类型',
  subject_id BIGINT NOT NULL COMMENT '主题ID',
  product_id BIGINT DEFAULT NULL COMMENT '产品ID',
  risk_metric_id BIGINT DEFAULT NULL COMMENT '风险指标ID',
  release_batch_id BIGINT DEFAULT NULL COMMENT '发布批次ID',
  approval_order_id BIGINT DEFAULT NULL COMMENT '审批单ID',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
  product_key VARCHAR(64) DEFAULT NULL COMMENT '产品标识',
  work_status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT '工作状态业务字段',
  priority_level VARCHAR(16) NOT NULL DEFAULT 'P2' COMMENT '优先级等级',
  assignee_user_id BIGINT DEFAULT NULL COMMENT '责任人',
  source_stage VARCHAR(64) DEFAULT NULL COMMENT '来源阶段',
  blocking_reason VARCHAR(255) DEFAULT NULL COMMENT '阻塞原因',
  snapshot_json JSON DEFAULT NULL COMMENT '上下文快照',
  task_category VARCHAR(64) DEFAULT NULL COMMENT '生命周期任务分类',
  domain_code VARCHAR(64) DEFAULT NULL COMMENT '生命周期域编码',
  action_code VARCHAR(128) DEFAULT NULL COMMENT '操作编码',
  execution_status VARCHAR(64) DEFAULT NULL COMMENT '生命周期执行状态',
  recommendation_snapshot_json LONGTEXT DEFAULT NULL COMMENT '推荐快照',
  evidence_snapshot_json LONGTEXT DEFAULT NULL COMMENT '证据快照',
  impact_snapshot_json LONGTEXT DEFAULT NULL COMMENT '影响快照',
  rollback_snapshot_json LONGTEXT DEFAULT NULL COMMENT '回滚快照',
  due_time DATETIME DEFAULT NULL COMMENT '截止时间',
  resolved_time DATETIME DEFAULT NULL COMMENT '解决时间',
  closed_time DATETIME DEFAULT NULL COMMENT '关闭时间',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_governance_work_item_subject (subject_type, subject_id, work_status, deleted)
) COMMENT='治理与运营工作项表';

-- 表：iot_protocol_decrypt_profile
-- 说明：协议解密档案治理主表
CREATE TABLE iot_protocol_decrypt_profile (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  profile_code VARCHAR(128) NOT NULL COMMENT '解密档案编码',
  algorithm VARCHAR(64) NOT NULL COMMENT '解密算法',
  merchant_source VARCHAR(64) NOT NULL COMMENT '密钥来源',
  merchant_key VARCHAR(128) NOT NULL COMMENT '商户键',
  transformation VARCHAR(128) DEFAULT NULL COMMENT '转换表达式',
  signature_secret VARCHAR(255) DEFAULT NULL COMMENT '签名密钥',
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
  approval_order_id BIGINT DEFAULT NULL COMMENT '审批主单ID',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_protocol_decrypt_profile_code (tenant_id, profile_code, deleted)
) COMMENT='协议解密档案治理主表';

-- 表：iot_protocol_decrypt_profile_snapshot
-- 说明：协议解密档案发布快照表
CREATE TABLE iot_protocol_decrypt_profile_snapshot (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  profile_id BIGINT NOT NULL COMMENT '解密档案主表ID',
  approval_order_id BIGINT NOT NULL COMMENT '审批主单ID',
  published_version_no INT NOT NULL COMMENT '发布版本号',
  snapshot_json LONGTEXT NOT NULL COMMENT '发布快照',
  lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT '生命周期状态',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_protocol_decrypt_snapshot_lookup (profile_id, lifecycle_status, published_version_no, deleted)
) COMMENT='协议解密档案发布快照表';

-- 表：iot_protocol_family_definition
-- 说明：协议族定义治理主表
CREATE TABLE iot_protocol_family_definition (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  family_code VARCHAR(128) NOT NULL COMMENT '协议族编码',
  protocol_code VARCHAR(64) NOT NULL COMMENT '协议编码',
  display_name VARCHAR(255) NOT NULL COMMENT '显示名称',
  decrypt_profile_code VARCHAR(128) DEFAULT NULL COMMENT '绑定解密档案编码',
  sign_algorithm VARCHAR(64) DEFAULT NULL COMMENT '签名算法',
  normalization_strategy VARCHAR(64) DEFAULT NULL COMMENT '归一化策略',
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
  approval_order_id BIGINT DEFAULT NULL COMMENT '审批主单ID',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_protocol_family_definition_code (tenant_id, family_code, deleted)
) COMMENT='协议族定义治理主表';

-- 表：iot_protocol_family_definition_snapshot
-- 说明：协议族定义发布快照表
CREATE TABLE iot_protocol_family_definition_snapshot (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  family_id BIGINT NOT NULL COMMENT '协议族主表ID',
  approval_order_id BIGINT NOT NULL COMMENT '审批主单ID',
  published_version_no INT NOT NULL COMMENT '发布版本号',
  snapshot_json LONGTEXT NOT NULL COMMENT '发布快照',
  lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT '生命周期状态',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_protocol_family_snapshot_lookup (family_id, lifecycle_status, published_version_no, deleted)
) COMMENT='协议族定义发布快照表';

-- 表：iot_protocol_template_definition
-- 说明：协议模板治理主表
CREATE TABLE iot_protocol_template_definition (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  template_code VARCHAR(128) NOT NULL COMMENT '模板编码',
  family_code VARCHAR(128) NOT NULL COMMENT '协议族编码',
  protocol_code VARCHAR(64) NOT NULL COMMENT '协议编码',
  display_name VARCHAR(255) NOT NULL COMMENT '显示名称',
  expression_json LONGTEXT NOT NULL COMMENT '表达式JSON',
  output_mapping_json LONGTEXT DEFAULT NULL COMMENT '输出映射JSON',
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
  approval_order_id BIGINT DEFAULT NULL COMMENT '审批主单ID',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_protocol_template_definition_code (tenant_id, template_code, deleted)
) COMMENT='协议模板治理主表';

-- 表：iot_protocol_template_definition_snapshot
-- 说明：协议模板发布快照表
CREATE TABLE iot_protocol_template_definition_snapshot (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  template_id BIGINT NOT NULL COMMENT '协议模板主表ID',
  template_code VARCHAR(128) NOT NULL COMMENT '模板编码',
  family_code VARCHAR(128) NOT NULL COMMENT '协议族编码',
  protocol_code VARCHAR(64) NOT NULL COMMENT '协议编码',
  published_version_no INT NOT NULL COMMENT '发布版本号',
  snapshot_json LONGTEXT NOT NULL COMMENT '发布快照',
  lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT '生命周期状态',
  approval_order_id BIGINT DEFAULT NULL COMMENT '审批主单ID',
  submit_reason VARCHAR(255) DEFAULT NULL COMMENT '提交原因',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_protocol_template_snapshot_lookup (template_id, lifecycle_status, published_version_no, deleted)
) COMMENT='协议模板发布快照表';

-- 表：sys_governance_approval_order
-- 说明：治理审批工单表
CREATE TABLE sys_governance_approval_order (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  action_code VARCHAR(64) NOT NULL COMMENT '操作编码',
  action_name VARCHAR(128) DEFAULT NULL COMMENT '操作名称',
  subject_type VARCHAR(64) DEFAULT NULL COMMENT '审批主体类型',
  subject_id BIGINT DEFAULT NULL COMMENT '审批对象ID',
  work_item_id BIGINT DEFAULT NULL COMMENT '工作项ID',
  status VARCHAR(32) NOT NULL COMMENT '状态',
  operator_user_id BIGINT NOT NULL COMMENT '操作人用户ID',
  approver_user_id BIGINT NOT NULL COMMENT '复核人用户ID',
  payload_json LONGTEXT DEFAULT NULL COMMENT '载荷JSON内容',
  approval_comment VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  approved_time DATETIME DEFAULT NULL COMMENT '审批时间',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_governance_approval_order_subject (subject_type, subject_id, deleted),
  KEY idx_governance_approval_order_status_time (status, create_time, deleted),
  KEY idx_governance_approval_order_operator (operator_user_id, approver_user_id, deleted)
) COMMENT='治理审批工单表';

-- 表：sys_governance_approval_policy
-- 说明：治理审批策略表
CREATE TABLE sys_governance_approval_policy (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  scope_type VARCHAR(16) NOT NULL COMMENT '作用域类型',
  action_code VARCHAR(64) NOT NULL COMMENT '操作编码',
  approver_mode VARCHAR(32) NOT NULL COMMENT '复核人模式',
  approver_user_id BIGINT NOT NULL COMMENT '复核人用户ID',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '启用标记',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_governance_approval_policy_scope_action (scope_type, tenant_id, action_code, deleted),
  KEY idx_governance_approval_policy_enabled (enabled, scope_type, tenant_id, action_code, deleted),
  KEY idx_governance_approval_policy_approver (approver_user_id, enabled, deleted)
) COMMENT='治理审批策略表';

-- 表：sys_governance_approval_transition
-- 说明：治理审批流转记录表
CREATE TABLE sys_governance_approval_transition (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  order_id BIGINT NOT NULL COMMENT '工单ID',
  from_status VARCHAR(32) DEFAULT NULL COMMENT '原状态',
  to_status VARCHAR(32) NOT NULL COMMENT '目标状态',
  actor_user_id BIGINT NOT NULL COMMENT '操作人用户ID',
  transition_comment VARCHAR(500) DEFAULT NULL COMMENT '流转备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_governance_approval_transition_order (order_id, create_time, deleted),
  KEY idx_governance_approval_transition_actor (actor_user_id, create_time, deleted)
) COMMENT='治理审批流转记录表';

-- 表：sys_governance_replay_feedback
-- 说明：治理复盘反馈表
CREATE TABLE sys_governance_replay_feedback (
  id BIGINT NOT NULL COMMENT '主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  work_item_id BIGINT NOT NULL COMMENT '工作项ID',
  approval_order_id BIGINT DEFAULT NULL COMMENT '审批单ID',
  release_batch_id BIGINT DEFAULT NULL COMMENT '发布批次ID',
  adopted_decision VARCHAR(64) DEFAULT NULL COMMENT '采纳结论',
  execution_outcome VARCHAR(64) DEFAULT NULL COMMENT '执行结果',
  root_cause_code VARCHAR(64) DEFAULT NULL COMMENT '根因编码',
  feedback_json LONGTEXT DEFAULT NULL COMMENT '复盘反馈内容',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_governance_replay_feedback_work_item (work_item_id, create_time, deleted),
  KEY idx_governance_replay_feedback_release_batch (release_batch_id, create_time, deleted)
) COMMENT='治理复盘反馈表';

-- 表：sys_audit_log
-- 说明：审计日志表
CREATE TABLE sys_audit_log (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  user_id BIGINT DEFAULT NULL COMMENT '用户ID',
  user_name VARCHAR(64) DEFAULT NULL COMMENT '用户名',
  operation_type VARCHAR(64) DEFAULT NULL COMMENT '操作类型',
  operation_module VARCHAR(128) DEFAULT NULL COMMENT '操作模块',
  operation_method VARCHAR(255) NOT NULL DEFAULT '' COMMENT '操作方法',
  request_url VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
  request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
  request_params LONGTEXT DEFAULT NULL COMMENT '请求参数',
  response_result LONGTEXT DEFAULT NULL COMMENT '响应结果',
  ip_address VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  location VARCHAR(128) DEFAULT NULL COMMENT '位置',
  operation_result TINYINT DEFAULT NULL COMMENT '操作结果 1成功 0失败',
  result_message VARCHAR(500) DEFAULT NULL COMMENT '结果消息',
  operation_time DATETIME DEFAULT NULL COMMENT '操作时间',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
  product_key VARCHAR(64) DEFAULT NULL COMMENT '产品标识',
  error_code VARCHAR(64) DEFAULT NULL COMMENT '错误编码',
  exception_class VARCHAR(255) DEFAULT NULL COMMENT '异常类型',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_operation_time (operation_time),
  KEY idx_trace_id (trace_id),
  KEY idx_device_code (device_code),
  KEY idx_audit_deleted_operation_time (deleted, operation_time, create_time, id),
  KEY idx_audit_deleted_type_time (deleted, operation_type, operation_time, create_time, id),
  KEY idx_audit_deleted_request_method_time (deleted, request_method, operation_time, create_time, id)
) COMMENT='审计日志表';

-- 表：sys_business_event_log
-- 说明：业务事件日志表
CREATE TABLE sys_business_event_log (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  event_code VARCHAR(128) NOT NULL COMMENT '业务事件编码',
  event_name VARCHAR(128) DEFAULT NULL COMMENT '业务事件名称',
  domain_code VARCHAR(64) NOT NULL COMMENT '业务域编码',
  action_code VARCHAR(64) NOT NULL COMMENT '业务动作编码',
  object_type VARCHAR(64) DEFAULT NULL COMMENT '业务对象类型',
  object_id VARCHAR(128) DEFAULT NULL COMMENT '业务对象标识',
  object_name VARCHAR(255) DEFAULT NULL COMMENT '业务对象名称',
  actor_user_id BIGINT DEFAULT NULL COMMENT '操作者用户ID',
  actor_name VARCHAR(64) DEFAULT NULL COMMENT '操作者名称',
  result_status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '事件结果状态',
  source_type VARCHAR(32) NOT NULL DEFAULT 'SYSTEM' COMMENT '事件来源类型',
  evidence_type VARCHAR(64) DEFAULT NULL COMMENT '关联证据类型',
  evidence_id VARCHAR(128) DEFAULT NULL COMMENT '关联证据标识',
  request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
  request_uri VARCHAR(255) DEFAULT NULL COMMENT '请求URI',
  duration_ms BIGINT DEFAULT NULL COMMENT '耗时毫秒',
  error_code VARCHAR(64) DEFAULT NULL COMMENT '错误编码',
  error_message VARCHAR(500) DEFAULT NULL COMMENT '错误摘要',
  metadata_json JSON DEFAULT NULL COMMENT '事件扩展元数据',
  occurred_at DATETIME NOT NULL COMMENT '事件发生时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_business_event_trace (trace_id),
  KEY idx_business_event_domain_time (deleted, domain_code, occurred_at, id),
  KEY idx_business_event_code_time (event_code, occurred_at),
  KEY idx_business_event_object (object_type, object_id),
  KEY idx_business_event_result_time (result_status, occurred_at)
) COMMENT='业务事件日志表';

-- 表：sys_dict
-- 说明：字典表
CREATE TABLE sys_dict (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  dict_name VARCHAR(128) NOT NULL COMMENT '字典名称',
  dict_code VARCHAR(64) NOT NULL COMMENT '字典编码',
  dict_type VARCHAR(32) DEFAULT NULL COMMENT '字典类型',
  dict_value VARCHAR(255) NOT NULL DEFAULT '' COMMENT '历史兼容字段',
  dict_label VARCHAR(128) NOT NULL DEFAULT '' COMMENT '历史兼容字段',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dict_code_tenant (tenant_id, dict_code),
  KEY idx_dict_deleted_sort (deleted, sort_no, id),
  KEY idx_dict_deleted_type_sort (deleted, dict_type, sort_no, id)
) COMMENT='字典表';

-- 表：sys_dict_item
-- 说明：字典项表
CREATE TABLE sys_dict_item (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  dict_id BIGINT NOT NULL COMMENT '字典ID',
  item_name VARCHAR(128) NOT NULL COMMENT '项名称',
  item_value VARCHAR(255) NOT NULL COMMENT '项值',
  item_type VARCHAR(32) DEFAULT NULL COMMENT '条目类型（字符串/数值/布尔）',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dict_value_tenant (tenant_id, dict_id, item_value),
  KEY idx_dict_id (dict_id)
) COMMENT='字典项表';

-- 表：sys_help_document
-- 说明：帮助文档表
CREATE TABLE sys_help_document (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  doc_category VARCHAR(32) NOT NULL COMMENT '文档分类（业务/技术/常见问题）',
  title VARCHAR(128) NOT NULL COMMENT '文档标题',
  summary VARCHAR(500) DEFAULT NULL COMMENT '文档摘要',
  content LONGTEXT NOT NULL COMMENT '文档正文',
  keywords VARCHAR(500) DEFAULT NULL COMMENT '关键词，逗号分隔',
  related_paths VARCHAR(500) DEFAULT NULL COMMENT '关联页面路径，逗号分隔',
  visible_role_codes VARCHAR(500) DEFAULT NULL COMMENT '可见角色编码，逗号分隔',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (id),
  KEY idx_help_document_deleted_category_sort (deleted, doc_category, sort_no, id),
  KEY idx_help_document_deleted_status_sort (deleted, status, sort_no, id)
) COMMENT='帮助文档表';

-- 表：sys_in_app_message
-- 说明：站内消息表
CREATE TABLE sys_in_app_message (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  message_type VARCHAR(32) NOT NULL COMMENT '消息类型（系统/业务/错误）',
  priority VARCHAR(32) NOT NULL DEFAULT 'medium' COMMENT '优先级（紧急/高/中/低）',
  title VARCHAR(128) NOT NULL COMMENT '消息标题',
  summary VARCHAR(500) DEFAULT NULL COMMENT '消息摘要',
  content LONGTEXT DEFAULT NULL COMMENT '消息正文',
  target_type VARCHAR(16) NOT NULL DEFAULT 'all' COMMENT '推送范围（全部/角色/用户）',
  target_role_codes VARCHAR(500) DEFAULT NULL COMMENT '目标角色编码，逗号分隔',
  target_user_ids VARCHAR(500) DEFAULT NULL COMMENT '目标用户ID，逗号分隔',
  related_path VARCHAR(255) DEFAULT NULL COMMENT '关联页面路径',
  source_type VARCHAR(64) DEFAULT NULL COMMENT '来源类型',
  source_id VARCHAR(64) DEFAULT NULL COMMENT '来源业务ID',
  dedup_key VARCHAR(32) DEFAULT NULL COMMENT '去重键',
  publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1发布中 0停用',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (id),
  KEY idx_in_app_message_deleted_status_time (deleted, status, publish_time, id),
  KEY idx_in_app_message_deleted_type_time (deleted, message_type, publish_time, id),
  KEY idx_in_app_message_deleted_target_sort (deleted, target_type, sort_no, id),
  KEY idx_in_app_message_source (source_type, source_id),
  KEY idx_in_app_message_tenant_dedup (tenant_id, dedup_key, deleted)
) COMMENT='站内消息表';

-- 表：sys_in_app_message_bridge_attempt_log
-- 说明：站内消息桥接尝试明细表
CREATE TABLE sys_in_app_message_bridge_attempt_log (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  bridge_log_id BIGINT NOT NULL COMMENT '桥接日志ID',
  message_id BIGINT NOT NULL COMMENT '消息ID',
  channel_code VARCHAR(64) NOT NULL COMMENT '渠道编码',
  bridge_scene VARCHAR(64) NOT NULL COMMENT '桥接场景',
  attempt_no INT NOT NULL COMMENT '尝试序号',
  bridge_status TINYINT NOT NULL DEFAULT 0 COMMENT '桥接状态 0失败 1成功',
  unread_count INT NOT NULL DEFAULT 0 COMMENT '本次桥接时的未读人数',
  recipient_snapshot VARCHAR(500) DEFAULT NULL COMMENT '本次桥接时的未读对象摘要',
  response_status_code INT DEFAULT NULL COMMENT '本次响应状态码',
  response_body VARCHAR(1000) DEFAULT NULL COMMENT '本次响应摘要',
  attempt_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '尝试时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_in_app_message_bridge_attempt (bridge_log_id, attempt_no),
  KEY idx_in_app_message_bridge_attempt_log_time (bridge_log_id, attempt_time),
  KEY idx_in_app_message_bridge_attempt_message (message_id, channel_code, attempt_time)
) COMMENT='站内消息桥接尝试明细表';

-- 表：sys_in_app_message_bridge_log
-- 说明：站内消息未读桥接日志表
CREATE TABLE sys_in_app_message_bridge_log (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  message_id BIGINT NOT NULL COMMENT '消息ID',
  channel_code VARCHAR(64) NOT NULL COMMENT '渠道编码',
  bridge_scene VARCHAR(64) NOT NULL COMMENT '桥接场景',
  unread_count INT NOT NULL DEFAULT 0 COMMENT '最近一次桥接时的未读人数',
  recipient_snapshot VARCHAR(500) DEFAULT NULL COMMENT '未读对象摘要',
  bridge_status TINYINT NOT NULL DEFAULT 0 COMMENT '桥接状态 0失败/待重试 1成功',
  response_status_code INT DEFAULT NULL COMMENT '最近一次响应状态码',
  response_body VARCHAR(1000) DEFAULT NULL COMMENT '最近一次响应摘要',
  last_attempt_time DATETIME DEFAULT NULL COMMENT '最近一次尝试时间',
  success_time DATETIME DEFAULT NULL COMMENT '成功时间',
  attempt_count INT NOT NULL DEFAULT 0 COMMENT '尝试次数',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_in_app_message_bridge_message_channel (tenant_id, message_id, channel_code, bridge_scene),
  KEY idx_in_app_message_bridge_status_time (bridge_status, last_attempt_time),
  KEY idx_in_app_message_bridge_message (message_id, channel_code)
) COMMENT='站内消息未读桥接日志表';

-- 表：sys_in_app_message_read
-- 说明：站内消息已读表
CREATE TABLE sys_in_app_message_read (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  message_id BIGINT NOT NULL COMMENT '消息ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  read_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_in_app_message_read_user (tenant_id, message_id, user_id),
  KEY idx_in_app_message_read_user_time (user_id, read_time, message_id)
) COMMENT='站内消息已读表';

-- 表：sys_menu
-- 说明：菜单表
CREATE TABLE sys_menu (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID',
  menu_name VARCHAR(100) NOT NULL COMMENT '菜单名称',
  menu_code VARCHAR(100) DEFAULT NULL COMMENT '菜单编码',
  path VARCHAR(255) DEFAULT NULL COMMENT '路由路径',
  component VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
  icon VARCHAR(100) DEFAULT NULL COMMENT '图标',
  meta_json LONGTEXT DEFAULT NULL COMMENT 'UI 元数据',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  type TINYINT NOT NULL DEFAULT 1 COMMENT '类型 0目录 1菜单 2按钮',
  menu_type TINYINT NOT NULL DEFAULT 1 COMMENT '兼容历史菜单类型',
  route_path VARCHAR(255) DEFAULT NULL COMMENT '历史路由字段',
  permission VARCHAR(128) DEFAULT NULL COMMENT '历史权限标识字段',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '历史排序字段',
  visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_menu_code_tenant (tenant_id, menu_code),
  KEY idx_parent_id (parent_id),
  KEY idx_status (status),
  KEY idx_menu_deleted_parent_sort (deleted, parent_id, sort, id),
  KEY idx_menu_deleted_status_sort (deleted, status, sort, id)
) COMMENT='菜单表';

-- 表：sys_notification_channel
-- 说明：通知渠道表
CREATE TABLE sys_notification_channel (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  channel_name VARCHAR(128) NOT NULL COMMENT '渠道名称',
  channel_code VARCHAR(64) NOT NULL COMMENT '渠道编码',
  channel_type VARCHAR(32) DEFAULT NULL COMMENT '渠道类型',
  config LONGTEXT DEFAULT NULL COMMENT '渠道配置(JSON)',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_channel_code_tenant (tenant_id, channel_code),
  KEY idx_channel_deleted_sort (deleted, sort_no, id),
  KEY idx_channel_deleted_type_sort (deleted, channel_type, sort_no, id)
) COMMENT='通知渠道表';

-- 表：sys_observability_span_log
-- 说明：可观测调用片段日志表
CREATE TABLE sys_observability_span_log (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  parent_span_id BIGINT DEFAULT NULL COMMENT '父调用片段标识',
  span_type VARCHAR(64) NOT NULL COMMENT '调用片段类型',
  span_name VARCHAR(128) NOT NULL COMMENT '调用片段名称',
  domain_code VARCHAR(64) DEFAULT NULL COMMENT '业务域编码',
  event_code VARCHAR(128) DEFAULT NULL COMMENT '关联事件编码',
  object_type VARCHAR(64) DEFAULT NULL COMMENT '业务对象类型',
  object_id VARCHAR(128) DEFAULT NULL COMMENT '业务对象标识',
  transport_type VARCHAR(32) DEFAULT NULL COMMENT '传输类型',
  status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '调用片段状态',
  duration_ms BIGINT DEFAULT NULL COMMENT '耗时毫秒',
  started_at DATETIME NOT NULL COMMENT '开始时间',
  finished_at DATETIME DEFAULT NULL COMMENT '结束时间',
  error_class VARCHAR(255) DEFAULT NULL COMMENT '异常类型',
  error_message VARCHAR(500) DEFAULT NULL COMMENT '异常摘要',
  tags_json JSON DEFAULT NULL COMMENT '调用片段标签数据',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  KEY idx_observability_span_trace (trace_id),
  KEY idx_observability_span_type_time (span_type, started_at),
  KEY idx_observability_span_status_time (status, started_at),
  KEY idx_observability_span_event (event_code, started_at),
  KEY idx_observability_span_object (object_type, object_id)
) COMMENT='可观测调用片段日志表';

-- 表：sys_organization
-- 说明：组织机构表
CREATE TABLE sys_organization (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父ID',
  org_name VARCHAR(128) NOT NULL COMMENT '组织名称',
  org_code VARCHAR(64) NOT NULL COMMENT '组织编码',
  org_type VARCHAR(32) DEFAULT NULL COMMENT '组织类型（部门/岗位/班组）',
  leader_user_id BIGINT DEFAULT NULL COMMENT '负责人ID',
  leader_name VARCHAR(64) DEFAULT NULL COMMENT '负责人姓名',
  phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
  email VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_org_code_tenant (tenant_id, org_code),
  KEY idx_parent_id (parent_id),
  KEY idx_org_deleted_parent_sort (deleted, parent_id, sort_no, id),
  KEY idx_org_deleted_status_sort (deleted, status, sort_no, id)
) COMMENT='组织机构表';

-- 表：sys_region
-- 说明：区域表
CREATE TABLE sys_region (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  region_name VARCHAR(128) NOT NULL COMMENT '区域名称',
  region_code VARCHAR(64) NOT NULL COMMENT '区域编码',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父ID',
  region_type VARCHAR(32) NOT NULL COMMENT '区域类型（省/市/区县/街道）',
  longitude DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  latitude DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_region_code_tenant (tenant_id, region_code),
  KEY idx_parent_id (parent_id),
  KEY idx_region_deleted_parent_sort (deleted, parent_id, sort_no, id),
  KEY idx_region_deleted_type_sort (deleted, region_type, sort_no, id)
) COMMENT='区域表';

-- 表：sys_role
-- 说明：角色表
CREATE TABLE sys_role (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
  role_code VARCHAR(100) NOT NULL COMMENT '角色编码',
  description VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
  data_scope_type VARCHAR(32) NOT NULL DEFAULT 'TENANT' COMMENT '数据范围类型',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code_tenant (tenant_id, role_code),
  KEY idx_role_code (role_code),
  KEY idx_role_deleted_status_create_time (deleted, status, create_time, id)
) COMMENT='角色表';

-- 表：sys_role_menu
-- 说明：角色菜单关联表
CREATE TABLE sys_role_menu (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  menu_id BIGINT NOT NULL COMMENT '菜单ID',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_menu (tenant_id, role_id, menu_id),
  KEY idx_role_id (role_id),
  KEY idx_menu_id (menu_id)
) COMMENT='角色菜单关联表';

-- 表：sys_tenant
-- 说明：租户表
CREATE TABLE sys_tenant (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_name VARCHAR(128) NOT NULL COMMENT '租户名称',
  tenant_code VARCHAR(64) NOT NULL COMMENT '租户编码',
  contact_name VARCHAR(64) DEFAULT NULL COMMENT '联系人',
  contact_phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
  contact_email VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  expire_time DATETIME DEFAULT NULL COMMENT '到期时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_code (tenant_code)
) COMMENT='租户表';

-- 表：sys_user
-- 说明：系统用户表
CREATE TABLE sys_user (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  org_id BIGINT DEFAULT NULL COMMENT '主机构ID',
  username VARCHAR(64) NOT NULL COMMENT '用户名',
  password VARCHAR(255) NOT NULL COMMENT '密码',
  nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  real_name VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
  phone VARCHAR(32) DEFAULT NULL COMMENT '手机号',
  email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  is_admin TINYINT NOT NULL DEFAULT 0 COMMENT '是否管理员 1是 0否',
  last_login_ip VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
  last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username_tenant (tenant_id, username),
  KEY idx_user_org_id (org_id),
  KEY idx_phone (phone),
  KEY idx_email (email),
  KEY idx_user_deleted_status_create_time (deleted, status, create_time, id)
) COMMENT='系统用户表';

-- 表：sys_user_role
-- 说明：用户角色关联表
CREATE TABLE sys_user_role (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (tenant_id, user_id, role_id),
  KEY idx_user_id (user_id),
  KEY idx_role_id (role_id)
) COMMENT='用户角色关联表';
