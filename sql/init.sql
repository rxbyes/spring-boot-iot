CREATE DATABASE IF NOT EXISTS rm_iot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE rm_iot;

SET NAMES utf8mb4;

-- =========================
-- 0) Drop old tables
-- =========================
DROP VIEW IF EXISTS iot_message_log;

DROP TABLE IF EXISTS iot_event_work_order;
DROP TABLE IF EXISTS iot_event_record;
DROP TABLE IF EXISTS iot_alarm_record;
DROP TABLE IF EXISTS emergency_plan;
DROP TABLE IF EXISTS linkage_rule;
DROP TABLE IF EXISTS rule_definition;
DROP TABLE IF EXISTS risk_metric_catalog;
DROP TABLE IF EXISTS risk_point_highway_detail;
DROP TABLE IF EXISTS risk_point_device_pending_promotion;
DROP TABLE IF EXISTS risk_point_device_pending_binding;
DROP TABLE IF EXISTS risk_point_device;
DROP TABLE IF EXISTS risk_point;

DROP TABLE IF EXISTS iot_command_record;
DROP TABLE IF EXISTS iot_device_metric_latest;
DROP TABLE IF EXISTS iot_device_invalid_report_state;
DROP TABLE IF EXISTS iot_device_access_error_log;
DROP TABLE IF EXISTS iot_device_message_log;
DROP TABLE IF EXISTS iot_device_property;
DROP TABLE IF EXISTS iot_device_relation;
DROP TABLE IF EXISTS iot_device_online_session;
DROP TABLE IF EXISTS iot_device;
DROP TABLE IF EXISTS iot_product_contract_release_batch;
DROP TABLE IF EXISTS iot_vendor_metric_evidence;
DROP TABLE IF EXISTS iot_normative_metric_definition;
DROP TABLE IF EXISTS iot_product_model;
DROP TABLE IF EXISTS iot_product;

DROP TABLE IF EXISTS sys_governance_approval_transition;
DROP TABLE IF EXISTS sys_governance_approval_order;
DROP TABLE IF EXISTS sys_audit_log;
DROP TABLE IF EXISTS sys_help_document;
DROP TABLE IF EXISTS sys_in_app_message_bridge_attempt_log;
DROP TABLE IF EXISTS sys_in_app_message_bridge_log;
DROP TABLE IF EXISTS sys_in_app_message_read;
DROP TABLE IF EXISTS sys_in_app_message;
DROP TABLE IF EXISTS sys_notification_channel;
DROP TABLE IF EXISTS sys_dict_item;
DROP TABLE IF EXISTS sys_dict;
DROP TABLE IF EXISTS sys_region;
DROP TABLE IF EXISTS sys_organization;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_tenant;

-- =========================
-- 1) System domain
-- =========================
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
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (tenant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

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
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username_tenant (tenant_id, username),
    KEY idx_user_org_id (org_id),
    KEY idx_phone (phone),
    KEY idx_email (email),
    KEY idx_user_deleted_status_create_time (deleted, status, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE sys_role (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(100) NOT NULL COMMENT '角色编码',
    description VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
    data_scope_type VARCHAR(32) NOT NULL DEFAULT 'TENANT' COMMENT '数据范围类型',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code_tenant (tenant_id, role_code),
    KEY idx_role_code (role_code),
    KEY idx_role_deleted_status_create_time (deleted, status, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE sys_user_role (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (tenant_id, user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

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
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_menu_code_tenant (tenant_id, menu_code),
    KEY idx_parent_id (parent_id),
    KEY idx_status (status),
    KEY idx_menu_deleted_parent_sort (deleted, parent_id, sort, id),
    KEY idx_menu_deleted_status_sort (deleted, status, sort, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

CREATE TABLE sys_role_menu (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (tenant_id, role_id, menu_id),
    KEY idx_role_id (role_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

CREATE TABLE sys_organization (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父ID',
    org_name VARCHAR(128) NOT NULL COMMENT '组织名称',
    org_code VARCHAR(64) NOT NULL COMMENT '组织编码',
    org_type VARCHAR(32) DEFAULT NULL COMMENT '组织类型 dept/position/team',
    leader_user_id BIGINT DEFAULT NULL COMMENT '负责人ID',
    leader_name VARCHAR(64) DEFAULT NULL COMMENT '负责人姓名',
    phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    email VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code_tenant (tenant_id, org_code),
    KEY idx_parent_id (parent_id),
    KEY idx_org_deleted_parent_sort (deleted, parent_id, sort_no, id),
    KEY idx_org_deleted_status_sort (deleted, status, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织机构表';

CREATE TABLE sys_region (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    region_name VARCHAR(128) NOT NULL COMMENT '区域名称',
    region_code VARCHAR(64) NOT NULL COMMENT '区域编码',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父ID',
    region_type VARCHAR(32) NOT NULL COMMENT '区域类型 province/city/district/street',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_region_code_tenant (tenant_id, region_code),
    KEY idx_parent_id (parent_id),
    KEY idx_region_deleted_parent_sort (deleted, parent_id, sort_no, id),
    KEY idx_region_deleted_type_sort (deleted, region_type, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域表';

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
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_code_tenant (tenant_id, dict_code),
    KEY idx_dict_deleted_sort (deleted, sort_no, id),
    KEY idx_dict_deleted_type_sort (deleted, dict_type, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典表';

CREATE TABLE sys_dict_item (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    dict_id BIGINT NOT NULL COMMENT '字典ID',
    item_name VARCHAR(128) NOT NULL COMMENT '项名称',
    item_value VARCHAR(255) NOT NULL COMMENT '项值',
    item_type VARCHAR(32) DEFAULT NULL COMMENT '项类型 string/number/boolean',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_value_tenant (tenant_id, dict_id, item_value),
    KEY idx_dict_id (dict_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项表';

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
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_channel_code_tenant (tenant_id, channel_code),
    KEY idx_channel_deleted_sort (deleted, sort_no, id),
    KEY idx_channel_deleted_type_sort (deleted, channel_type, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知渠道表';

CREATE TABLE sys_in_app_message (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    message_type VARCHAR(32) NOT NULL COMMENT '消息类型 system/business/error',
    priority VARCHAR(32) NOT NULL DEFAULT 'medium' COMMENT '优先级 critical/high/medium/low',
    title VARCHAR(128) NOT NULL COMMENT '消息标题',
    summary VARCHAR(500) DEFAULT NULL COMMENT '消息摘要',
    content LONGTEXT DEFAULT NULL COMMENT '消息正文',
    target_type VARCHAR(16) NOT NULL DEFAULT 'all' COMMENT '推送范围 all/role/user',
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
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_in_app_message_deleted_status_time (deleted, status, publish_time, id),
    KEY idx_in_app_message_deleted_type_time (deleted, message_type, publish_time, id),
    KEY idx_in_app_message_deleted_target_sort (deleted, target_type, sort_no, id),
    KEY idx_in_app_message_source (source_type, source_id),
    KEY idx_in_app_message_tenant_dedup (tenant_id, dedup_key, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

CREATE TABLE sys_in_app_message_read (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    message_id BIGINT NOT NULL COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    read_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_in_app_message_read_user (tenant_id, message_id, user_id),
    KEY idx_in_app_message_read_user_time (user_id, read_time, message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息已读表';

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
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_in_app_message_bridge_message_channel (tenant_id, message_id, channel_code, bridge_scene),
    KEY idx_in_app_message_bridge_status_time (bridge_status, last_attempt_time),
    KEY idx_in_app_message_bridge_message (message_id, channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息未读桥接日志表';

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
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_in_app_message_bridge_attempt (bridge_log_id, attempt_no),
    KEY idx_in_app_message_bridge_attempt_log_time (bridge_log_id, attempt_time DESC),
    KEY idx_in_app_message_bridge_attempt_message (message_id, channel_code, attempt_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息桥接尝试明细表';

CREATE TABLE sys_help_document (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_category VARCHAR(32) NOT NULL COMMENT '文档分类 business/technical/faq',
    title VARCHAR(128) NOT NULL COMMENT '文档标题',
    summary VARCHAR(500) DEFAULT NULL COMMENT '文档摘要',
    content LONGTEXT NOT NULL COMMENT '文档正文',
    keywords VARCHAR(500) DEFAULT NULL COMMENT '关键词，逗号分隔',
    related_paths VARCHAR(500) DEFAULT NULL COMMENT '关联页面路径，逗号分隔',
    visible_role_codes VARCHAR(500) DEFAULT NULL COMMENT '可见角色编码，逗号分隔',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_help_document_deleted_category_sort (deleted, doc_category, sort_no, id),
    KEY idx_help_document_deleted_status_sort (deleted, status, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帮助文档表';

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
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'trace id',
    device_code VARCHAR(64) DEFAULT NULL COMMENT 'device code',
    product_key VARCHAR(64) DEFAULT NULL COMMENT 'product key',
    error_code VARCHAR(64) DEFAULT NULL COMMENT 'error code',
    exception_class VARCHAR(255) DEFAULT NULL COMMENT 'exception class',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_operation_time (operation_time),
    KEY idx_trace_id (trace_id),
    KEY idx_device_code (device_code),
    KEY idx_audit_deleted_operation_time (deleted, operation_time, create_time, id),
    KEY idx_audit_deleted_type_time (deleted, operation_type, operation_time, create_time, id),
    KEY idx_audit_deleted_request_method_time (deleted, request_method, operation_time, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';


CREATE TABLE sys_governance_approval_order (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='governance approval order';

CREATE TABLE sys_governance_approval_transition (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='governance approval transition';

-- =========================
-- 2) IoT device domain
-- =========================
CREATE TABLE iot_product (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    product_key VARCHAR(64) NOT NULL COMMENT '产品Key',
    product_name VARCHAR(128) NOT NULL COMMENT '产品名称',
    protocol_code VARCHAR(64) NOT NULL COMMENT '协议编码',
    node_type TINYINT NOT NULL DEFAULT 1 COMMENT '节点类型 1直连设备 2网关设备 3网关子设备',
    data_format VARCHAR(32) NOT NULL DEFAULT 'JSON' COMMENT '数据格式',
    manufacturer VARCHAR(128) DEFAULT NULL COMMENT '厂商',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    metadata_json JSON DEFAULT NULL COMMENT '产品扩展元数据',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_key_tenant (tenant_id, product_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

CREATE TABLE iot_product_model (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    model_type VARCHAR(32) NOT NULL COMMENT '模型类型 property/event/service',
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
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_identifier (product_id, model_type, identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品物模型表';

CREATE TABLE iot_normative_metric_definition (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规范字段定义表';

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
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vendor_metric_evidence (product_id, raw_identifier, logical_channel_code),
    KEY idx_vendor_metric_product_seen (product_id, last_seen_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂商字段证据表';

CREATE TABLE iot_vendor_metric_mapping_rule (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂商字段映射规则表';

CREATE TABLE iot_product_contract_release_batch (
    id BIGINT NOT NULL COMMENT 'Primary key',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
    product_id BIGINT NOT NULL COMMENT 'Product ID',
    scenario_code VARCHAR(64) NOT NULL COMMENT 'Scenario code',
    release_source VARCHAR(64) NOT NULL COMMENT 'Release source',
    released_field_count INT NOT NULL DEFAULT 0 COMMENT 'Released field count',
    approval_order_id BIGINT DEFAULT NULL COMMENT 'Approval order ID',
    release_reason VARCHAR(500) DEFAULT NULL COMMENT 'Release reason',
    release_status VARCHAR(16) NOT NULL DEFAULT 'RELEASED' COMMENT 'RELEASED/ROLLED_BACK',
    create_by BIGINT DEFAULT NULL COMMENT 'Operator user ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rollback_by BIGINT DEFAULT NULL COMMENT 'Rollback operator user ID',
    rollback_time DATETIME DEFAULT NULL COMMENT 'Rollback time',
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_product_contract_release_product_time (product_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product contract release batch';

CREATE TABLE iot_product_contract_release_snapshot (
    id BIGINT NOT NULL COMMENT 'Primary key',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
    batch_id BIGINT NOT NULL COMMENT 'Release batch ID',
    product_id BIGINT NOT NULL COMMENT 'Product ID',
    snapshot_stage VARCHAR(32) NOT NULL COMMENT 'Snapshot stage',
    snapshot_json JSON NOT NULL COMMENT 'Snapshot payload JSON',
    create_by BIGINT DEFAULT NULL COMMENT 'Operator user ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_release_snapshot_batch_stage (batch_id, snapshot_stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product contract release snapshot';


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
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_code_tenant (tenant_id, device_code),
    KEY idx_device_tenant_org_deleted (tenant_id, org_id, deleted, last_report_time, id),
    KEY idx_device_deleted_product_stats (deleted, product_id, last_report_time, online_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

CREATE TABLE iot_device_relation (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备逻辑通道关系表';

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
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_online_session_device_active (deleted, device_id, offline_time),
    KEY idx_online_session_product_time (deleted, product_id, online_time, offline_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备在线会话表';

CREATE TABLE iot_device_property (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    identifier VARCHAR(64) NOT NULL COMMENT '属性标识',
    property_name VARCHAR(128) DEFAULT NULL COMMENT '属性名称',
    property_value VARCHAR(1024) DEFAULT NULL COMMENT '属性值',
    value_type VARCHAR(32) DEFAULT NULL COMMENT '值类型',
    report_time DATETIME NOT NULL COMMENT '上报时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_identifier (device_id, identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备最新属性表';

CREATE TABLE iot_device_metric_latest (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    metric_id VARCHAR(128) NOT NULL COMMENT '指标唯一键',
    metric_code VARCHAR(128) NOT NULL COMMENT '指标编码',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT '指标名称',
    value_type VARCHAR(32) DEFAULT NULL COMMENT '值类型',
    value_double DOUBLE DEFAULT NULL COMMENT '浮点值',
    value_long BIGINT DEFAULT NULL COMMENT '整型值',
    value_bool TINYINT(1) DEFAULT NULL COMMENT '布尔值',
    value_text TEXT DEFAULT NULL COMMENT '文本值',
    quality_code VARCHAR(32) DEFAULT NULL COMMENT '质量码',
    alarm_flag TINYINT(1) DEFAULT NULL COMMENT '告警标记',
    reported_at DATETIME DEFAULT NULL COMMENT '实际上报时间',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'trace id',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tel_latest_tenant_device_metric (tenant_id, device_id, metric_id),
    KEY idx_tel_latest_device_reported (device_id, reported_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='telemetry v2 latest投影表';

CREATE TABLE iot_device_message_log (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    product_id BIGINT DEFAULT NULL COMMENT '产品ID',
    message_type VARCHAR(32) NOT NULL COMMENT '消息类型 telemetry/event/property/reply',
    topic VARCHAR(255) DEFAULT NULL COMMENT '主题',
    payload JSON DEFAULT NULL COMMENT '原始消息',
    report_time DATETIME NOT NULL COMMENT '上报时间',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'trace id',
    device_code VARCHAR(64) DEFAULT NULL COMMENT 'device code',
    product_key VARCHAR(64) DEFAULT NULL COMMENT 'product key',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_device_time (device_id, report_time),
    KEY idx_message_type (message_type),
    KEY idx_trace_id (trace_id),
    KEY idx_device_code_time (device_code, report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备消息日志表';

CREATE TABLE iot_device_access_error_log (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'trace id',
    protocol_code VARCHAR(64) DEFAULT NULL COMMENT '协议编码',
    request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方式',
    failure_stage VARCHAR(32) DEFAULT NULL COMMENT '失败阶段',
    device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
    product_key VARCHAR(64) DEFAULT NULL COMMENT '产品Key',
    gateway_device_code VARCHAR(64) DEFAULT NULL COMMENT '网关设备编码',
    sub_device_code VARCHAR(64) DEFAULT NULL COMMENT '子设备编码',
    topic_route_type VARCHAR(32) DEFAULT NULL COMMENT 'topic 路由类型',
    message_type VARCHAR(32) DEFAULT NULL COMMENT '消息类型',
    topic VARCHAR(255) DEFAULT NULL COMMENT 'topic',
    client_id VARCHAR(128) DEFAULT NULL COMMENT '客户端ID',
    payload_size INT DEFAULT NULL COMMENT 'payload 大小',
    payload_encoding VARCHAR(16) DEFAULT NULL COMMENT 'payload 编码',
    payload_truncated TINYINT NOT NULL DEFAULT 0 COMMENT 'payload 是否截断',
    raw_payload LONGTEXT DEFAULT NULL COMMENT '原始 payload',
    error_code VARCHAR(64) DEFAULT NULL COMMENT '错误码',
    exception_class VARCHAR(255) DEFAULT NULL COMMENT '异常类型',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '错误消息',
    contract_snapshot LONGTEXT DEFAULT NULL COMMENT '设备契约快照',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_access_error_trace (trace_id),
    KEY idx_access_error_device_time (device_code, create_time),
    KEY idx_access_error_stage_time (failure_stage, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备接入失败归档表';

CREATE TABLE iot_device_invalid_report_state (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    governance_key VARCHAR(255) NOT NULL COMMENT '治理唯一键',
    reason_code VARCHAR(64) NOT NULL COMMENT '治理原因编码',
    request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方式',
    failure_stage VARCHAR(32) DEFAULT NULL COMMENT '失败阶段',
    device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
    product_key VARCHAR(64) DEFAULT NULL COMMENT '产品Key',
    protocol_code VARCHAR(64) DEFAULT NULL COMMENT '协议编码',
    topic_route_type VARCHAR(32) DEFAULT NULL COMMENT 'topic 路由类型',
    topic VARCHAR(255) DEFAULT NULL COMMENT '最近 topic',
    client_id VARCHAR(128) DEFAULT NULL COMMENT '最近 clientId',
    payload_size INT DEFAULT NULL COMMENT '最近 payload 大小',
    payload_encoding VARCHAR(16) DEFAULT NULL COMMENT '最近 payload 编码',
    last_payload LONGTEXT DEFAULT NULL COMMENT '最近 payload',
    last_trace_id VARCHAR(64) DEFAULT NULL COMMENT '最近 traceId',
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
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invalid_report_state_governance_key (governance_key),
    KEY idx_invalid_report_device_resolved (device_code, product_key, resolved, last_seen_time),
    KEY idx_invalid_report_reason_time (reason_code, last_seen_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='无效 MQTT 上报最新态表';

CREATE TABLE iot_command_record (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    command_id VARCHAR(64) NOT NULL COMMENT '业务命令ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    device_code VARCHAR(64) NOT NULL COMMENT '设备编码',
    product_key VARCHAR(64) NOT NULL COMMENT '产品Key',
    gateway_device_code VARCHAR(64) DEFAULT NULL COMMENT '网关设备编码',
    sub_device_code VARCHAR(64) DEFAULT NULL COMMENT '子设备编码',
    topic VARCHAR(255) NOT NULL COMMENT '下发Topic',
    command_type VARCHAR(32) NOT NULL COMMENT '命令类型 property/service',
    service_identifier VARCHAR(64) DEFAULT NULL COMMENT '服务标识',
    request_payload LONGTEXT DEFAULT NULL COMMENT '下发请求报文',
    reply_payload LONGTEXT DEFAULT NULL COMMENT '设备回执报文',
    qos TINYINT NOT NULL DEFAULT 0 COMMENT 'MQTT QoS',
    retained TINYINT NOT NULL DEFAULT 0 COMMENT '是否保留消息 1是 0否',
    status VARCHAR(32) NOT NULL COMMENT '命令状态 CREATED/SENT/SUCCESS/FAILED/TIMEOUT',
    send_time DATETIME DEFAULT NULL COMMENT '发送时间',
    ack_time DATETIME DEFAULT NULL COMMENT '回执时间',
    timeout_time DATETIME DEFAULT NULL COMMENT '超时时间',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_command_id (command_id),
    KEY idx_device_status (device_code, status),
    KEY idx_status_timeout (status, timeout_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备命令记录表';

-- =========================
-- 3) Alarm / risk domain
-- =========================
CREATE TABLE iot_alarm_record (
    id BIGINT NOT NULL COMMENT '主键',
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
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_alarm_code (alarm_code),
    KEY idx_device_status (device_code, status),
    KEY idx_trigger_time (trigger_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

CREATE TABLE iot_event_record (
    id BIGINT NOT NULL COMMENT '主键',
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
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_code (event_code),
    KEY idx_device_status (device_code, status),
    KEY idx_trigger_time (trigger_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件记录表';

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
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_order_code (work_order_code),
    KEY idx_event_status (event_id, status),
    KEY idx_receive_time (receive_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件工单表';

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
    risk_point_level VARCHAR(16) DEFAULT NULL COMMENT '风险点档案等级 level_1/level_2/level_3',
    current_risk_level VARCHAR(16) DEFAULT NULL COMMENT '当前风险态势等级 red/orange/yellow/blue',
    risk_level VARCHAR(20) DEFAULT NULL COMMENT '历史风险等级兼容字段',
    risk_type VARCHAR(32) NOT NULL DEFAULT 'GENERAL' COMMENT '风险点类型 SLOPE/BRIDGE/TUNNEL/GENERAL',
    location_text VARCHAR(255) DEFAULT NULL COMMENT '位置描述/桩号/区间',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '风险点经度',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '风险点纬度',
    description VARCHAR(1000) DEFAULT NULL COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-启用 1-停用',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_point_code_tenant (tenant_id, risk_point_code),
    KEY idx_region (region_id),
    KEY idx_status (status),
    KEY idx_risk_type_status (risk_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险点表';

CREATE TABLE risk_point_highway_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    risk_point_id BIGINT NOT NULL COMMENT '风险点ID',
    project_name VARCHAR(255) NOT NULL COMMENT '项目名称',
    project_type VARCHAR(32) NOT NULL COMMENT '项目类型',
    project_summary TEXT DEFAULT NULL COMMENT '项目简介',
    route_code VARCHAR(64) NOT NULL COMMENT '路线编号',
    route_name VARCHAR(128) DEFAULT NULL COMMENT '路线名称',
    road_level VARCHAR(64) DEFAULT NULL COMMENT '公路等级',
    project_risk_level VARCHAR(32) DEFAULT NULL COMMENT '项目风险等级原始值',
    admin_region_code VARCHAR(32) DEFAULT NULL COMMENT '行政区域末级编码',
    admin_region_path_json VARCHAR(255) DEFAULT NULL COMMENT '行政区域路径JSON',
    maintenance_org_name VARCHAR(128) DEFAULT NULL COMMENT '管养单位名称',
    source_row_no INT DEFAULT NULL COMMENT 'Excel来源行号',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_point_highway (risk_point_id),
    KEY idx_route_code (route_code),
    KEY idx_admin_region_code (admin_region_code),
    KEY idx_project_type (project_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='高速公路风险点扩展表';

CREATE TABLE risk_metric_catalog (
    id BIGINT NOT NULL COMMENT 'Primary key',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
    product_id BIGINT NOT NULL COMMENT 'Product ID',
    release_batch_id BIGINT DEFAULT NULL COMMENT 'Source release batch ID',
    product_model_id BIGINT DEFAULT NULL COMMENT 'Product model ID',
    normative_identifier VARCHAR(64) DEFAULT NULL COMMENT 'Normative identifier',
    contract_identifier VARCHAR(64) NOT NULL COMMENT 'Contract identifier',
    risk_metric_code VARCHAR(64) NOT NULL COMMENT 'Risk metric code',
    risk_metric_name VARCHAR(128) NOT NULL COMMENT 'Risk metric name',
    risk_category VARCHAR(64) DEFAULT NULL COMMENT 'Risk category',
    metric_role VARCHAR(32) DEFAULT NULL COMMENT 'Metric role',
    lifecycle_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/RETIRED',
    source_scenario_code VARCHAR(64) DEFAULT NULL COMMENT 'Source scenario code',
    metric_unit VARCHAR(32) DEFAULT NULL COMMENT 'Metric unit',
    metric_dimension VARCHAR(64) DEFAULT NULL COMMENT 'Metric dimension',
    threshold_type VARCHAR(32) DEFAULT NULL COMMENT 'Threshold type',
    semantic_direction VARCHAR(32) DEFAULT NULL COMMENT 'Semantic direction',
    threshold_direction VARCHAR(32) DEFAULT NULL COMMENT 'Threshold direction',
    trend_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'Trend enabled flag',
    gis_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'GIS enabled flag',
    insight_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'Insight enabled flag',
    analytics_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'Analytics enabled flag',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT 'Enable status',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_metric_catalog (product_id, contract_identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Risk metric catalog';


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
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_point_metric (risk_point_id, device_id, metric_identifier),
    KEY idx_risk_device (risk_point_id, device_id),
    KEY idx_risk_point_device_metric_catalog (risk_metric_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险点设备绑定表';

CREATE TABLE risk_point_device_pending_binding (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_no VARCHAR(64) NOT NULL COMMENT '导入批次号',
    source_file_name VARCHAR(255) DEFAULT NULL COMMENT '来源文件名',
    source_row_no INT NOT NULL COMMENT '来源行号',
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
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pending_binding_batch_row (tenant_id, batch_no, source_row_no),
    KEY idx_pending_binding_status (tenant_id, resolution_status, deleted),
    KEY idx_pending_binding_risk_device (risk_point_id, device_id, deleted),
    KEY idx_pending_binding_device_code (tenant_id, device_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险点设备待治理导入表';

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
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_pending_promotion_pending_id (pending_binding_id),
    KEY idx_pending_promotion_binding_id (risk_point_device_id),
    KEY idx_pending_promotion_status (tenant_id, promotion_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险点设备待治理转正明细表';

CREATE TABLE rule_definition (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    risk_metric_id BIGINT DEFAULT NULL COMMENT '风险指标ID',
    metric_identifier VARCHAR(64) NOT NULL COMMENT '测点标识符',
    metric_name VARCHAR(64) DEFAULT NULL COMMENT '测点名称',
    expression VARCHAR(256) DEFAULT NULL COMMENT '表达式',
    duration INT NOT NULL DEFAULT 0 COMMENT '持续时间(秒)',
    alarm_level VARCHAR(20) DEFAULT NULL COMMENT '告警等级',
    notification_methods VARCHAR(64) DEFAULT NULL COMMENT '通知方式',
    convert_to_event TINYINT NOT NULL DEFAULT 0 COMMENT '是否转事件',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0启用 1停用',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_metric_identifier (metric_identifier),
    KEY idx_rule_definition_metric_catalog (risk_metric_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阈值规则表';

CREATE TABLE linkage_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    description VARCHAR(512) DEFAULT NULL COMMENT '描述',
    trigger_condition LONGTEXT DEFAULT NULL COMMENT '触发条件(JSON)',
    action_list LONGTEXT DEFAULT NULL COMMENT '动作列表(JSON)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0启用 1停用',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联动规则表';

CREATE TABLE emergency_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_name VARCHAR(128) NOT NULL COMMENT '预案名称',
    alarm_level VARCHAR(16) DEFAULT NULL COMMENT '适用告警等级 red/orange/yellow/blue',
    risk_level VARCHAR(20) DEFAULT NULL COMMENT '历史风险等级兼容字段',
    description VARCHAR(512) DEFAULT NULL COMMENT '描述',
    response_steps LONGTEXT DEFAULT NULL COMMENT '响应步骤(JSON)',
    contact_list LONGTEXT DEFAULT NULL COMMENT '联系人列表(JSON)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0启用 1停用',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应急预案表';

-- 文档统一命名兼容视图

DROP TABLE IF EXISTS iot_device_secret_rotation_log;

CREATE TABLE iot_device_secret_rotation_log (
    id BIGINT NOT NULL COMMENT 'Primary key',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
    device_id BIGINT NOT NULL COMMENT 'Device ID',
    device_code VARCHAR(64) NOT NULL COMMENT 'Device code',
    product_key VARCHAR(64) DEFAULT NULL COMMENT 'Product key',
    rotation_batch_id VARCHAR(64) NOT NULL COMMENT 'Rotation batch ID',
    reason VARCHAR(500) DEFAULT NULL COMMENT 'Rotation reason',
    previous_secret_digest VARCHAR(128) DEFAULT NULL COMMENT 'Previous secret digest',
    current_secret_digest VARCHAR(128) DEFAULT NULL COMMENT 'Current secret digest',
    rotated_by BIGINT NOT NULL COMMENT 'Operator user ID',
    approved_by BIGINT NOT NULL COMMENT 'Approver user ID',
    rotate_time DATETIME NOT NULL COMMENT 'Rotate time',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_device_rotation_device_time (device_id, rotate_time),
    KEY idx_device_rotation_batch (rotation_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Device secret rotation log';

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
FROM iot_device_message_log;
