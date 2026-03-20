USE rm_iot;

SET NAMES utf8mb4;

-- 产品定义中心首个可用闭环：补齐页面定位、按钮权限与角色授权。
-- 适用场景：历史库已具备五工作台菜单，但 /products 尚未具备正式产品台账的增改删导出权限。

SET @role_business_id = (
    SELECT id
    FROM sys_role
    WHERE role_code = 'BUSINESS_STAFF'
      AND deleted = 0
    ORDER BY id
    LIMIT 1
);
SET @role_management_id = (
    SELECT id
    FROM sys_role
    WHERE role_code = 'MANAGEMENT_STAFF'
      AND deleted = 0
    ORDER BY id
    LIMIT 1
);
SET @role_ops_id = (
    SELECT id
    FROM sys_role
    WHERE role_code = 'OPS_STAFF'
      AND deleted = 0
    ORDER BY id
    LIMIT 1
);
SET @role_developer_id = (
    SELECT id
    FROM sys_role
    WHERE role_code = 'DEVELOPER_STAFF'
      AND deleted = 0
    ORDER BY id
    LIMIT 1
);
SET @role_super_admin_id = (
    SELECT id
    FROM sys_role
    WHERE role_code = 'SUPER_ADMIN'
      AND deleted = 0
    ORDER BY id
    LIMIT 1
);

INSERT INTO sys_menu (
    id, parent_id, menu_name, menu_code, path, component, icon, meta_json,
    sort, type, menu_type, status, create_by, create_time, update_by, update_time, deleted
)
VALUES
    (93001001, 93000001, '产品定义中心', 'iot:products', '/products', 'ProductWorkbenchView', 'box', '{"caption":"产品台账、协议基线与库存归属","shortLabel":"产"}', 11, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001011, 93001001, '新增产品', 'iot:products:add', '', '', '', '{"caption":"产品定义中心新增产品按钮权限","shortLabel":"增"}', 1101, 2, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93001012, 93001001, '编辑产品', 'iot:products:update', '', '', '', '{"caption":"产品定义中心编辑产品按钮权限","shortLabel":"编"}', 1102, 2, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93001013, 93001001, '删除产品', 'iot:products:delete', '', '', '', '{"caption":"产品定义中心删除产品按钮权限","shortLabel":"删"}', 1103, 2, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93001014, 93001001, '导出产品', 'iot:products:export', '', '', '', '{"caption":"产品定义中心导出产品按钮权限","shortLabel":"导"}', 1104, 2, 2, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    menu_name = VALUES(menu_name),
    menu_code = VALUES(menu_code),
    path = VALUES(path),
    component = VALUES(component),
    icon = VALUES(icon),
    meta_json = VALUES(meta_json),
    sort = VALUES(sort),
    type = VALUES(type),
    menu_type = VALUES(menu_type),
    status = VALUES(status),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    deleted = VALUES(deleted);

DELETE FROM sys_role_menu
WHERE role_id IN (@role_business_id, @role_management_id, @role_ops_id, @role_developer_id, @role_super_admin_id)
  AND menu_id IN (93000001, 93001001, 93001011, 93001012, 93001013, 93001014);

SET @role_menu_id = COALESCE((SELECT MAX(id) FROM sys_role_menu), 96000000);

INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_time)
SELECT (@role_menu_id := @role_menu_id + 1), 1, seed.role_id, seed.menu_id, NOW()
FROM (
    SELECT @role_business_id AS role_id, 93000001 AS menu_id
    UNION ALL SELECT @role_business_id, 93001001
    UNION ALL SELECT @role_business_id, 93001014

    UNION ALL SELECT @role_management_id, 93000001
    UNION ALL SELECT @role_management_id, 93001001
    UNION ALL SELECT @role_management_id, 93001011
    UNION ALL SELECT @role_management_id, 93001012
    UNION ALL SELECT @role_management_id, 93001013
    UNION ALL SELECT @role_management_id, 93001014

    UNION ALL SELECT @role_ops_id, 93000001
    UNION ALL SELECT @role_ops_id, 93001001
    UNION ALL SELECT @role_ops_id, 93001011
    UNION ALL SELECT @role_ops_id, 93001012
    UNION ALL SELECT @role_ops_id, 93001013
    UNION ALL SELECT @role_ops_id, 93001014

    UNION ALL SELECT @role_developer_id, 93000001
    UNION ALL SELECT @role_developer_id, 93001001
    UNION ALL SELECT @role_developer_id, 93001011
    UNION ALL SELECT @role_developer_id, 93001012
    UNION ALL SELECT @role_developer_id, 93001013
    UNION ALL SELECT @role_developer_id, 93001014

    UNION ALL SELECT @role_super_admin_id, 93000001
    UNION ALL SELECT @role_super_admin_id, 93001001
    UNION ALL SELECT @role_super_admin_id, 93001011
    UNION ALL SELECT @role_super_admin_id, 93001012
    UNION ALL SELECT @role_super_admin_id, 93001013
    UNION ALL SELECT @role_super_admin_id, 93001014
) seed
WHERE seed.role_id IS NOT NULL;
