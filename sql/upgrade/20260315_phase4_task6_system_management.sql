-- 系统管理表
-- 组织机构表
DROP TABLE IF EXISTS sys_organization;
CREATE TABLE sys_organization (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父ID',
    org_name VARCHAR(128) NOT NULL COMMENT '组织名称',
    org_code VARCHAR(64) NOT NULL COMMENT '组织编码',
    org_type VARCHAR(32) NOT NULL COMMENT '组织类型 dept/position/team',
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
    KEY idx_parent_id (parent_id),
    KEY idx_tenant_code (tenant_id, org_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织机构表';

-- 区域管理表
DROP TABLE IF EXISTS sys_region;
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
    KEY idx_parent_id (parent_id),
    KEY idx_tenant_code (tenant_id, region_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域管理表';

-- 字典配置表
DROP TABLE IF EXISTS sys_dict;
CREATE TABLE sys_dict (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    dict_name VARCHAR(128) NOT NULL COMMENT '字典名称',
    dict_code VARCHAR(64) NOT NULL COMMENT '字典编码',
    dict_type VARCHAR(32) NOT NULL COMMENT '字典类型 text/number/boolean/date',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典配置表';

-- 字典项表
DROP TABLE IF EXISTS sys_dict_item;
CREATE TABLE sys_dict_item (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    dict_id BIGINT NOT NULL COMMENT '字典ID',
    item_name VARCHAR(128) NOT NULL COMMENT '项名称',
    item_value VARCHAR(255) NOT NULL COMMENT '项值',
    item_type VARCHAR(32) NOT NULL COMMENT '项类型 string/number/boolean',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_dict_id (dict_id),
    UNIQUE KEY uk_dict_value_tenant (tenant_id, dict_id, item_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项表';

-- 通知渠道表
DROP TABLE IF EXISTS sys_notification_channel;
CREATE TABLE sys_notification_channel (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    channel_name VARCHAR(128) NOT NULL COMMENT '渠道名称',
    channel_code VARCHAR(64) NOT NULL COMMENT '渠道编码 email/sms/webhook/dingtalk/feishu',
    channel_config JSON DEFAULT NULL COMMENT '渠道配置',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认 1是 0否',
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

-- 审计日志表
DROP TABLE IF EXISTS sys_audit_log;
CREATE TABLE sys_audit_log (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型 login/logout/crud',
    operation_module VARCHAR(128) NOT NULL COMMENT '操作模块',
    operation_method VARCHAR(255) NOT NULL COMMENT '操作方法',
    operation_desc VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
    request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
    request_url VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
    request_ip VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
    request_user_agent VARCHAR(500) DEFAULT NULL COMMENT '请求User-Agent',
    request_params LONGTEXT DEFAULT NULL COMMENT '请求参数',
    response_code INT DEFAULT NULL COMMENT '响应码',
    response_msg VARCHAR(500) DEFAULT NULL COMMENT '响应消息',
    execute_time BIGINT DEFAULT NULL COMMENT '执行时间(毫秒)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1成功 0失败',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_operation_type (operation_type),
    KEY idx_create_time (create_time),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- 通知记录表
DROP TABLE IF EXISTS sys_notification_record;
CREATE TABLE sys_notification_record (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    channel_id BIGINT NOT NULL COMMENT '渠道ID',
    channel_code VARCHAR(64) NOT NULL COMMENT '渠道编码',
    title VARCHAR(255) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    receivers VARCHAR(1024) NOT NULL COMMENT '接收人',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0待发送 1发送中 2发送成功 3发送失败',
    send_time DATETIME DEFAULT NULL COMMENT '发送时间',
    response_code INT DEFAULT NULL COMMENT '响应码',
    response_msg VARCHAR(500) DEFAULT NULL COMMENT '响应消息',
    error_detail LONGTEXT DEFAULT NULL COMMENT '错误详情',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_channel_id (channel_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知记录表';
