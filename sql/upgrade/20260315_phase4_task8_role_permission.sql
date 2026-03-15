-- 角色权限管理表
-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(100) NOT NULL COMMENT '角色编码',
    description VARCHAR(500) COMMENT '角色描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_role_code (role_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY COMMENT '主键',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    menu_name VARCHAR(100) NOT NULL COMMENT '菜单名称',
    menu_code VARCHAR(100) NOT NULL COMMENT '菜单编码',
    path VARCHAR(200) COMMENT '路由路径',
    component VARCHAR(200) COMMENT '组件路径',
    icon VARCHAR(100) COMMENT '图标',
    sort INT DEFAULT 0 COMMENT '排序',
    type TINYINT NOT NULL DEFAULT 1 COMMENT '类型 1菜单 2按钮',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_menu_code (menu_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT PRIMARY KEY COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 初始化数据
-- 角色数据
INSERT INTO sys_role (id, tenant_id, role_name, role_code, description, status, create_by) VALUES
(1, 1, '系统管理员', 'admin', '系统管理员，拥有所有权限', 1, 1),
(2, 1, '普通用户', 'user', '普通用户，拥有部分权限', 1, 1);

-- 菜单数据
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, path, component, icon, sort, type, status, create_by) VALUES
(1, 0, '系统管理', 'system', '/system', 'Layout', 'setting', 10, 1, 1, 1),
(2, 1, '角色管理', 'system:role', '/role', 'RoleView', '', 1, 2, 1, 1),
(3, 1, '菜单管理', 'system:menu', '/menu', 'MenuView', '', 2, 2, 1, 1),
(4, 0, '告警中心', 'alarm', '/alarm-center', 'AlarmCenterView', 'bell', 1, 1, 1, 1),
(5, 0, '事件处置', 'event', '/event-disposal', 'EventDisposalView', 'flag', 2, 1, 1, 1);

-- 角色菜单关联数据
INSERT INTO sys_role_menu (id, role_id, menu_id, create_by) VALUES
(1, 1, 1, 1),
(2, 1, 2, 1),
(3, 1, 3, 1),
(4, 1, 4, 1),
(5, 1, 5, 1),
(6, 2, 4, 1),
(7, 2, 5, 1);
