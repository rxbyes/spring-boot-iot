CREATE DATABASE IF NOT EXISTS rm_iot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE rm_iot;

DROP TABLE IF EXISTS sys_tenant;
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

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    real_name VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
    phone VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
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
    UNIQUE KEY uk_username_tenant (tenant_id, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code_tenant (tenant_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (tenant_id, user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

DROP TABLE IF EXISTS iot_product;
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

DROP TABLE IF EXISTS iot_product_model;
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

DROP TABLE IF EXISTS iot_device;
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

DROP TABLE IF EXISTS iot_device_property;
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

DROP TABLE IF EXISTS iot_device_message_log;
CREATE TABLE iot_device_message_log (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    product_id BIGINT DEFAULT NULL COMMENT '产品ID',
    message_type VARCHAR(32) NOT NULL COMMENT '消息类型 telemetry/event/property/reply',
    topic VARCHAR(255) DEFAULT NULL COMMENT '主题',
    payload JSON DEFAULT NULL COMMENT '原始消息',
    report_time DATETIME NOT NULL COMMENT '上报时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备消息日志表';
