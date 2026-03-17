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
DROP TABLE IF EXISTS risk_point_device;
DROP TABLE IF EXISTS risk_point;

DROP TABLE IF EXISTS iot_command_record;
DROP TABLE IF EXISTS iot_device_message_log;
DROP TABLE IF EXISTS iot_device_property;
DROP TABLE IF EXISTS iot_device;
DROP TABLE IF EXISTS iot_product_model;
DROP TABLE IF EXISTS iot_product;

DROP TABLE IF EXISTS sys_audit_log;
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
    KEY idx_phone (phone),
    KEY idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE sys_role (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(100) NOT NULL COMMENT '角色编码',
    description VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code_tenant (tenant_id, role_code),
    KEY idx_role_code (role_code)
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
    KEY idx_status (status)
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
    KEY idx_parent_id (parent_id)
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
    KEY idx_parent_id (parent_id)
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
    UNIQUE KEY uk_dict_code_tenant (tenant_id, dict_code)
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
    UNIQUE KEY uk_channel_code_tenant (tenant_id, channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知渠道表';

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
    KEY idx_device_code (device_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

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

CREATE TABLE iot_device (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
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
    UNIQUE KEY uk_device_code_tenant (tenant_id, device_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

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
    region_id BIGINT DEFAULT NULL COMMENT '区域ID',
    region_name VARCHAR(128) DEFAULT NULL COMMENT '区域名称',
    responsible_user BIGINT DEFAULT NULL COMMENT '负责人',
    responsible_phone VARCHAR(32) DEFAULT NULL COMMENT '负责人电话',
    risk_level VARCHAR(20) DEFAULT NULL COMMENT '风险等级',
    description VARCHAR(512) DEFAULT NULL COMMENT '描述',
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
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险点表';

CREATE TABLE risk_point_device (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    risk_point_id BIGINT NOT NULL COMMENT '风险点ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
    device_name VARCHAR(128) DEFAULT NULL COMMENT '设备名称',
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
    KEY idx_risk_device (risk_point_id, device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险点设备绑定表';

CREATE TABLE rule_definition (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
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
    KEY idx_metric_identifier (metric_identifier)
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
    risk_level VARCHAR(20) DEFAULT NULL COMMENT '风险等级',
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
