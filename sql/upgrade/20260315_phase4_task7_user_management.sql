-- 用户管理表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY COMMENT '主键',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) COMMENT '密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(255) COMMENT '头像',
    status TINYINT DEFAULT 1 COMMENT '状态 1启用 0禁用',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_email (email),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY COMMENT '主键',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    description VARCHAR(255) COMMENT '角色描述',
    status TINYINT DEFAULT 1 COMMENT '状态 1启用 0禁用',
    sort_no INT DEFAULT 0 COMMENT '排序',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_role_code (role_code),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY COMMENT '主键',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    menu_code VARCHAR(100) COMMENT '菜单编码',
    path VARCHAR(255) COMMENT '路由路径',
    component VARCHAR(255) COMMENT '组件路径',
    icon VARCHAR(50) COMMENT '图标',
    sort_no INT DEFAULT 0 COMMENT '排序',
    type TINYINT DEFAULT 0 COMMENT '类型 0目录 1菜单 2按钮',
    permission VARCHAR(100) COMMENT '权限标识',
    status TINYINT DEFAULT 1 COMMENT '状态 1启用 0禁用',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_menu_code (menu_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT PRIMARY KEY COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 区域表
CREATE TABLE IF NOT EXISTS sys_region (
    id BIGINT PRIMARY KEY COMMENT '主键',
    parent_id BIGINT DEFAULT 0 COMMENT '父区域ID',
    region_name VARCHAR(100) NOT NULL COMMENT '区域名称',
    region_code VARCHAR(50) COMMENT '区域编码',
    region_type TINYINT DEFAULT 0 COMMENT '区域类型 0省 1市 2区县',
    sort_no INT DEFAULT 0 COMMENT '排序',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_region_code (region_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域表';

-- 字典表
CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGINT PRIMARY KEY COMMENT '主键',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_code VARCHAR(50) NOT NULL COMMENT '字典编码',
    description VARCHAR(255) COMMENT '字典描述',
    sort_no INT DEFAULT 0 COMMENT '排序',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_dict_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典表';

-- 字典项表
CREATE TABLE IF NOT EXISTS sys_dict_item (
    id BIGINT PRIMARY KEY COMMENT '主键',
    dict_id BIGINT NOT NULL COMMENT '字典ID',
    item_name VARCHAR(100) NOT NULL COMMENT '项名称',
    item_value VARCHAR(100) NOT NULL COMMENT '项值',
    sort_no INT DEFAULT 0 COMMENT '排序',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_dict_id (dict_id),
    INDEX idx_item_value (item_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项表';

-- 通知渠道表
CREATE TABLE IF NOT EXISTS sys_notification_channel (
    id BIGINT PRIMARY KEY COMMENT '主键',
    channel_name VARCHAR(50) NOT NULL COMMENT '渠道名称',
    channel_code VARCHAR(50) NOT NULL COMMENT '渠道编码',
    config TEXT COMMENT '配置信息',
    status TINYINT DEFAULT 1 COMMENT '状态 1启用 0禁用',
    sort_no INT DEFAULT 0 COMMENT '排序',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_channel_code (channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知渠道表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT PRIMARY KEY COMMENT '主键',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    operation VARCHAR(50) COMMENT '操作类型',
    method VARCHAR(255) COMMENT '方法名称',
    params TEXT COMMENT '请求参数',
    result VARCHAR(255) COMMENT '操作结果',
    ip VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(255) COMMENT '用户代理',
    cost_time BIGINT DEFAULT 0 COMMENT '耗时(毫秒)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_operation (operation),
    INDEX idx_create_time (create_time),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';
