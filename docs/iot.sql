

CREATE DATABASE IF NOT EXISTS rm_iot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE rm_iot;

-- 一期建议所有业务表统一带上这些公共字段：
-- id：主键
-- tenant_id：租户 ID
-- remark：备注
-- create_by
-- create_time
-- update_by
-- update_time
-- deleted：逻辑删除

-- 系统用户表 sys_user
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
                          deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                          PRIMARY KEY (id),
                          UNIQUE KEY uk_username_tenant (tenant_id, username),
                          KEY idx_phone (phone),
                          KEY idx_email (email),
                          KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 角色表 sys_role
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


-- 用户角色关联表 sys_user_role
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
                               id BIGINT NOT NULL COMMENT '主键',
                               tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
                               user_id BIGINT NOT NULL COMMENT '用户ID',
                               role_id BIGINT NOT NULL COMMENT '角色ID',
                               create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (id),
                               UNIQUE KEY uk_user_role (tenant_id, user_id, role_id),
                               KEY idx_user_id (user_id),
                               KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 菜单表 sys_menu
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
                          id BIGINT NOT NULL COMMENT '主键',
                          tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
                          parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID',
                          menu_name VARCHAR(64) NOT NULL COMMENT '菜单名称',
                          menu_type TINYINT NOT NULL COMMENT '菜单类型 1目录 2菜单 3按钮',
                          route_path VARCHAR(255) DEFAULT NULL COMMENT '路由地址',
                          component VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
                          permission VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
                          icon VARCHAR(64) DEFAULT NULL COMMENT '图标',
                          sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
                          visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示',
                          status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
                          create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          deleted TINYINT NOT NULL DEFAULT 0,
                          PRIMARY KEY (id),
                          KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 角色菜单关联表 sys_role_menu
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
                               id BIGINT NOT NULL COMMENT '主键',
                               tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
                               role_id BIGINT NOT NULL COMMENT '角色ID',
                               menu_id BIGINT NOT NULL COMMENT '菜单ID',
                               create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (id),
                               UNIQUE KEY uk_role_menu (tenant_id, role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 租户表 sys_tenant
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

-- 产品表 iot_product
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
                             UNIQUE KEY uk_product_key_tenant (tenant_id, product_key),
                             KEY idx_protocol_code (protocol_code),
                             KEY idx_node_type (node_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

-- 产品物模型表 iot_product_model
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
                                   UNIQUE KEY uk_product_identifier (product_id, model_type, identifier),
                                   KEY idx_product_id (product_id),
                                   KEY idx_model_type (model_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品物模型表';

-- 设备表 iot_device
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
                            UNIQUE KEY uk_device_code_tenant (tenant_id, device_code),
                            KEY idx_product_id (product_id),
                            KEY idx_gateway_id (gateway_id),
                            KEY idx_parent_device_id (parent_device_id),
                            KEY idx_online_status (online_status),
                            KEY idx_protocol_code (protocol_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

-- 网关表 iot_gateway
DROP TABLE IF EXISTS iot_gateway;
CREATE TABLE iot_gateway (
                             id BIGINT NOT NULL COMMENT '主键',
                             tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
                             device_id BIGINT NOT NULL COMMENT '关联设备ID',
                             gateway_code VARCHAR(64) NOT NULL COMMENT '网关编码',
                             gateway_name VARCHAR(128) NOT NULL COMMENT '网关名称',
                             protocol_code VARCHAR(64) NOT NULL COMMENT '协议编码',
                             online_status TINYINT NOT NULL DEFAULT 0 COMMENT '在线状态',
                             heartbeat_time DATETIME DEFAULT NULL COMMENT '心跳时间',
                             ip_address VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
                             version VARCHAR(64) DEFAULT NULL COMMENT '网关版本',
                             config_json JSON DEFAULT NULL COMMENT '配置JSON',
                             remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
                             create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             deleted TINYINT NOT NULL DEFAULT 0,
                             PRIMARY KEY (id),
                             UNIQUE KEY uk_gateway_code_tenant (tenant_id, gateway_code),
                             UNIQUE KEY uk_gateway_device_id (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关表';

-- 网关拓扑表 iot_gateway_topology
DROP TABLE IF EXISTS iot_gateway_topology;
CREATE TABLE iot_gateway_topology (
                                      id BIGINT NOT NULL COMMENT '主键',
                                      tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
                                      gateway_id BIGINT NOT NULL COMMENT '网关ID',
                                      sub_device_id BIGINT NOT NULL COMMENT '子设备ID',
                                      bind_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
                                      unbind_time DATETIME DEFAULT NULL COMMENT '解绑时间',
                                      bind_status TINYINT NOT NULL DEFAULT 1 COMMENT '绑定状态 1已绑定 0已解绑',
                                      create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY (id),
                                      UNIQUE KEY uk_gateway_sub_device (gateway_id, sub_device_id),
                                      KEY idx_sub_device_id (sub_device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关子设备拓扑表';

-- 设备影子表 iot_device_shadow
DROP TABLE IF EXISTS iot_device_shadow;
CREATE TABLE iot_device_shadow (
                                   id BIGINT NOT NULL COMMENT '主键',
                                   tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
                                   device_id BIGINT NOT NULL COMMENT '设备ID',
                                   shadow_json JSON NOT NULL COMMENT '设备影子JSON',
                                   version BIGINT NOT NULL DEFAULT 1 COMMENT '版本号',
                                   update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   PRIMARY KEY (id),
                                   UNIQUE KEY uk_device_shadow (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备影子表';

-- 设备最新属性表 iot_device_property
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
                                     UNIQUE KEY uk_device_identifier (device_id, identifier),
                                     KEY idx_report_time (report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备最新属性表';

-- 指令记录表 iot_command_record
DROP TABLE IF EXISTS iot_command_record;
CREATE TABLE iot_command_record (
                                    id BIGINT NOT NULL COMMENT '主键',
                                    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
                                    device_id BIGINT NOT NULL COMMENT '设备ID',
                                    product_id BIGINT DEFAULT NULL COMMENT '产品ID',
                                    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
                                    command_type VARCHAR(64) NOT NULL COMMENT '命令类型',
                                    command_name VARCHAR(128) DEFAULT NULL COMMENT '命令名称',
                                    command_payload JSON DEFAULT NULL COMMENT '命令内容',
                                    send_status TINYINT NOT NULL DEFAULT 0 COMMENT '发送状态 0待发送 1已发送 2发送失败',
                                    reply_status TINYINT NOT NULL DEFAULT 0 COMMENT '应答状态 0未应答 1成功 2失败 3超时',
                                    send_time DATETIME DEFAULT NULL COMMENT '发送时间',
                                    reply_time DATETIME DEFAULT NULL COMMENT '应答时间',
                                    reply_payload JSON DEFAULT NULL COMMENT '应答内容',
                                    error_msg VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
                                    create_by BIGINT DEFAULT NULL,
                                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    PRIMARY KEY (id),
                                    UNIQUE KEY uk_message_id (message_id),
                                    KEY idx_device_id (device_id),
                                    KEY idx_send_status (send_status),
                                    KEY idx_reply_status (reply_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指令记录表';

-- 规则链表 iot_rule_chain
DROP TABLE IF EXISTS iot_rule_chain;
CREATE TABLE iot_rule_chain (
                                id BIGINT NOT NULL COMMENT '主键',
                                tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
                                chain_name VARCHAR(128) NOT NULL COMMENT '规则链名称',
                                chain_code VARCHAR(64) NOT NULL COMMENT '规则链编码',
                                status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
                                description VARCHAR(500) DEFAULT NULL COMMENT '描述',
                                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                deleted TINYINT NOT NULL DEFAULT 0,
                                PRIMARY KEY (id),
                                UNIQUE KEY uk_chain_code_tenant (tenant_id, chain_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则链表';

-- 告警记录表 iot_alarm_record
DROP TABLE IF EXISTS iot_alarm_record;
CREATE TABLE iot_alarm_record (
                                  id BIGINT NOT NULL COMMENT '主键',
                                  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
                                  device_id BIGINT NOT NULL COMMENT '设备ID',
                                  product_id BIGINT DEFAULT NULL COMMENT '产品ID',
                                  alarm_code VARCHAR(64) NOT NULL COMMENT '告警编码',
                                  alarm_name VARCHAR(128) NOT NULL COMMENT '告警名称',
                                  alarm_level TINYINT NOT NULL DEFAULT 1 COMMENT '告警级别 1提示 2一般 3严重 4紧急',
                                  alarm_status TINYINT NOT NULL DEFAULT 1 COMMENT '告警状态 1活动中 2已恢复 3已确认',
                                  source_type VARCHAR(32) DEFAULT 'rule' COMMENT '来源类型',
                                  trigger_value VARCHAR(255) DEFAULT NULL COMMENT '触发值',
                                  trigger_time DATETIME NOT NULL COMMENT '触发时间',
                                  clear_time DATETIME DEFAULT NULL COMMENT '恢复时间',
                                  confirm_by BIGINT DEFAULT NULL COMMENT '确认人',
                                  confirm_time DATETIME DEFAULT NULL COMMENT '确认时间',
                                  description VARCHAR(500) DEFAULT NULL COMMENT '描述',
                                  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  PRIMARY KEY (id),
                                  KEY idx_device_id (device_id),
                                  KEY idx_alarm_code (alarm_code),
                                  KEY idx_alarm_status (alarm_status),
                                  KEY idx_trigger_time (trigger_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- OTA 包表 iot_ota_package
DROP TABLE IF EXISTS iot_ota_package;
CREATE TABLE iot_ota_package (
                                 id BIGINT NOT NULL COMMENT '主键',
                                 tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
                                 product_id BIGINT NOT NULL COMMENT '产品ID',
                                 package_name VARCHAR(128) NOT NULL COMMENT '升级包名称',
                                 version VARCHAR(64) NOT NULL COMMENT '版本号',
                                 file_url VARCHAR(500) NOT NULL COMMENT '文件地址',
                                 file_md5 VARCHAR(64) DEFAULT NULL COMMENT '文件MD5',
                                 file_size BIGINT DEFAULT NULL COMMENT '文件大小',
                                 description VARCHAR(500) DEFAULT NULL COMMENT '描述',
                                 status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
                                 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 deleted TINYINT NOT NULL DEFAULT 0,
                                 PRIMARY KEY (id),
                                 KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA升级包表';

-- 时序明细表 iot_device_message_log
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
                                        PRIMARY KEY (id),
                                        KEY idx_device_time (device_id, report_time),
                                        KEY idx_message_type (message_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备消息日志表';

-- 初始化数据建议
INSERT INTO sys_tenant (id, tenant_name, tenant_code, status, create_time, update_time, deleted)
VALUES (1, '默认租户', 'default', 1, NOW(), NOW(), 0);

INSERT INTO sys_role (id, tenant_id, role_name, role_code, status, create_time, update_time, deleted)
VALUES (1, 1, '超级管理员', 'SUPER_ADMIN', 1, NOW(), NOW(), 0);

INSERT INTO sys_user (
    id, tenant_id, username, password, nickname, real_name, status, is_admin, create_time, update_time, deleted
) VALUES (
             1, 1, 'admin', '$2a$10$replace_with_bcrypt_password', '管理员', '系统管理员', 1, 1, NOW(), NOW(), 0
         );

INSERT INTO sys_user_role (id, tenant_id, user_id, role_id, create_time)
VALUES (1, 1, 1, 1, NOW());


INSERT INTO iot_product (
    id, tenant_id, product_key, product_name, protocol_code, node_type, data_format, status, create_time, update_time, deleted
) VALUES (
             1001, 1, 'demo-product', '演示产品', 'mqtt-json', 1, 'JSON', 1, NOW(), NOW(), 0
         );

INSERT INTO iot_device (
    id, tenant_id, product_id, device_name, device_code, device_secret, protocol_code,
    node_type, online_status, activate_status, device_status, create_time, update_time, deleted
) VALUES (
             2001, 1, 1001, '演示设备-01', 'demo-device-01', '123456',
             'mqtt-json', 1, 0, 1, 1, NOW(), NOW(), 0
         );