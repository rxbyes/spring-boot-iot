-- Phase 4 Task 1: 告警中心基础能力 - 告警记录表
USE rm_iot;

-- 告警记录表
DROP TABLE IF EXISTS iot_alarm_record;
CREATE TABLE iot_alarm_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    alarm_code VARCHAR(64) NOT NULL COMMENT '告警编号',
    alarm_title VARCHAR(255) NOT NULL COMMENT '告警标题',
    alarm_type VARCHAR(32) NOT NULL COMMENT '告警类型',
    alarm_level VARCHAR(16) NOT NULL COMMENT '告警等级',
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
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_alarm_code (alarm_code),
    KEY idx_device_status (device_code, status),
    KEY idx_trigger_time (trigger_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- 事件记录表
DROP TABLE IF EXISTS iot_event_record;
CREATE TABLE iot_event_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_code VARCHAR(64) NOT NULL COMMENT '事件编号',
    event_title VARCHAR(255) NOT NULL COMMENT '事件标题',
    alarm_id BIGINT DEFAULT NULL COMMENT '告警ID',
    alarm_code VARCHAR(64) DEFAULT NULL COMMENT '告警编号',
    alarm_level VARCHAR(16) DEFAULT NULL COMMENT '告警等级',
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
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_code (event_code),
    KEY idx_device_status (device_code, status),
    KEY idx_trigger_time (trigger_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件记录表';

-- 事件工单表
DROP TABLE IF EXISTS iot_event_work_order;
CREATE TABLE iot_event_work_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_id BIGINT NOT NULL COMMENT '事件ID',
    event_code VARCHAR(64) NOT NULL COMMENT '事件编号',
    work_order_code VARCHAR(64) NOT NULL COMMENT '工单编号',
    work_order_type VARCHAR(32) NOT NULL COMMENT '工单类型',
    assign_user BIGINT NOT NULL COMMENT '派发用户',
    receive_user BIGINT DEFAULT NULL COMMENT '接收用户',
    receive_time DATETIME DEFAULT NULL COMMENT '接收时间',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    complete_time DATETIME DEFAULT NULL COMMENT '完成时间',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待接收 1-已接收 2-处理中 3-已完成 4-已取消',
    feedback TEXT DEFAULT NULL COMMENT '现场反馈',
    photos JSON DEFAULT NULL COMMENT '照片URL（JSON数组）',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_order_code (work_order_code),
    KEY idx_event_status (event_id, status),
    KEY idx_receive_time (receive_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件工单表';

-- 风险点表
DROP TABLE IF EXISTS risk_point;
CREATE TABLE risk_point (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    risk_point_code VARCHAR(64) NOT NULL COMMENT '风险点编号',
    risk_point_name VARCHAR(128) NOT NULL COMMENT '风险点名称',
    region_id BIGINT DEFAULT NULL COMMENT '区域ID',
    region_name VARCHAR(128) DEFAULT NULL COMMENT '区域名称',
    responsible_user BIGINT DEFAULT NULL COMMENT '负责人',
    responsible_phone VARCHAR(32) DEFAULT NULL COMMENT '负责人电话',
    risk_level VARCHAR(16) NOT NULL COMMENT '风险等级',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_point_code_tenant (tenant_id, risk_point_code),
    KEY idx_region (region_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险点表';

-- 规则定义表
DROP TABLE IF EXISTS rule_definition;
CREATE TABLE rule_definition (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_code VARCHAR(64) NOT NULL COMMENT '规则编码',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    risk_point_id BIGINT DEFAULT NULL COMMENT '风险点ID',
    metric_identifier VARCHAR(64) NOT NULL COMMENT '测点标识符',
    condition_expression VARCHAR(255) NOT NULL COMMENT '表达式',
    duration INT NOT NULL DEFAULT 0 COMMENT '持续时间（秒）',
    alarm_level VARCHAR(16) NOT NULL COMMENT '告警等级',
    notify_methods JSON DEFAULT NULL COMMENT '通知方式（JSON数组）',
    convert_to_event TINYINT NOT NULL DEFAULT 0 COMMENT '是否转事件',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_code_tenant (tenant_id, rule_code),
    KEY idx_risk_point (risk_point_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则定义表';

-- 联动规则表
DROP TABLE IF EXISTS linkage_rule;
CREATE TABLE linkage_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_code VARCHAR(64) NOT NULL COMMENT '规则编码',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    trigger_condition VARCHAR(255) NOT NULL COMMENT '触发条件',
    action_list JSON NOT NULL COMMENT '执行动作（JSON数组）',
    execution_order INT NOT NULL DEFAULT 1 COMMENT '执行顺序',
    timeout INT NOT NULL DEFAULT 30 COMMENT '执行超时（秒）',
    retry_strategy VARCHAR(64) DEFAULT NULL COMMENT '重试策略',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_code_tenant (tenant_id, rule_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联动规则表';

-- 应急预案表
DROP TABLE IF EXISTS emergency_plan;
CREATE TABLE emergency_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_code VARCHAR(64) NOT NULL COMMENT '预案编码',
    plan_name VARCHAR(128) NOT NULL COMMENT '预案名称',
    risk_level VARCHAR(16) NOT NULL COMMENT '风险等级',
    applicable_scenario VARCHAR(255) NOT NULL COMMENT '适用场景',
    disposal_steps JSON NOT NULL COMMENT '处置步骤（JSON数组）',
    responsible_user BIGINT DEFAULT NULL COMMENT '责任人',
    resource_requirements VARCHAR(500) DEFAULT NULL COMMENT '资源需求',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_plan_code_tenant (tenant_id, plan_code),
    KEY idx_risk_level (risk_level),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应急预案表';

-- 通知记录表
DROP TABLE IF EXISTS iot_notification_record;
CREATE TABLE iot_notification_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    alarm_id BIGINT DEFAULT NULL COMMENT '告警ID',
    event_id BIGINT DEFAULT NULL COMMENT '事件ID',
    notification_type VARCHAR(32) NOT NULL COMMENT '通知类型',
    notification_content TEXT NOT NULL COMMENT '通知内容',
    notification_status VARCHAR(16) NOT NULL COMMENT '通知状态',
    receiver VARCHAR(128) NOT NULL COMMENT '接收人',
    send_time DATETIME NOT NULL COMMENT '发送时间',
    response_time DATETIME DEFAULT NULL COMMENT '响应时间',
    response_result VARCHAR(255) DEFAULT NULL COMMENT '响应结果',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_alarm_event (alarm_id, event_id),
    KEY idx_send_time (send_time),
    KEY idx_status (notification_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知记录表';

-- 组织机构表
DROP TABLE IF EXISTS sys_organization;
CREATE TABLE sys_organization (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    org_name VARCHAR(128) NOT NULL COMMENT '组织名称',
    org_code VARCHAR(64) NOT NULL COMMENT '组织编码',
    parent_id BIGINT DEFAULT 0 COMMENT '父组织ID',
    org_path VARCHAR(500) DEFAULT NULL COMMENT '组织路径',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    leader_user BIGINT DEFAULT NULL COMMENT '负责人',
    phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code_tenant (tenant_id, org_code),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织机构表';

-- 区域管理表
DROP TABLE IF EXISTS sys_region;
CREATE TABLE sys_region (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    region_name VARCHAR(128) NOT NULL COMMENT '区域名称',
    region_code VARCHAR(64) NOT NULL COMMENT '区域编码',
    parent_id BIGINT DEFAULT 0 COMMENT '父区域ID',
    region_path VARCHAR(500) DEFAULT NULL COMMENT '区域路径',
    region_type VARCHAR(16) NOT NULL COMMENT '区域类型',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
    boundary JSON DEFAULT NULL COMMENT '边界坐标（JSON数组）',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    responsible_user BIGINT DEFAULT NULL COMMENT '负责人',
    phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_region_code_tenant (tenant_id, region_code),
    KEY idx_parent_id (parent_id),
    KEY idx_region_type (region_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域管理表';

-- 字典配置表
DROP TABLE IF EXISTS sys_dict;
CREATE TABLE sys_dict (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    dict_name VARCHAR(128) NOT NULL COMMENT '字典名称',
    dict_code VARCHAR(64) NOT NULL COMMENT '字典编码',
    dict_type VARCHAR(32) NOT NULL COMMENT '字典类型',
    dict_value VARCHAR(255) NOT NULL COMMENT '字典值',
    dict_label VARCHAR(128) NOT NULL COMMENT '字典标签',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY uk_dict_code_tenant (tenant_id, dict_code),
    KEY idx_dict_type (dict_type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典配置表';

-- 审计日志表
DROP TABLE IF EXISTS sys_audit_log;
CREATE TABLE sys_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    log_type VARCHAR(16) NOT NULL COMMENT '日志类型',
    operation_module VARCHAR(128) NOT NULL COMMENT '操作模块',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    operation_uri VARCHAR(255) NOT NULL COMMENT '操作URI',
    operation_method VARCHAR(16) NOT NULL COMMENT '操作方法',
    request_method VARCHAR(16) NOT NULL COMMENT '请求方法',
    request_params TEXT DEFAULT NULL COMMENT '请求参数',
    response_result TEXT DEFAULT NULL COMMENT '响应结果',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1成功 0失败',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    execute_time BIGINT NOT NULL DEFAULT 0 COMMENT '执行时间（毫秒）',
    ip_address VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    username VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_log_type (log_type),
    KEY idx_operation_module (operation_module),
    KEY idx_create_time (create_time),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';
