USE rm_iot;

SET NAMES utf8mb4;

-- 设备资产中心首个可用闭环：补齐按钮权限与角色授权。
-- 适用场景：历史库已具备五工作台菜单，但 /devices 尚未具备新增/编辑/删除/导出按钮权限。

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
    (93001101, 93001002, '新增设备', 'iot:devices:add', '', '', '', '{"caption":"设备资产中心新增设备按钮权限","shortLabel":"增"}', 1201, 2, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93001102, 93001002, '编辑设备', 'iot:devices:update', '', '', '', '{"caption":"设备资产中心编辑设备按钮权限","shortLabel":"编"}', 1202, 2, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93001103, 93001002, '删除设备', 'iot:devices:delete', '', '', '', '{"caption":"设备资产中心删除设备按钮权限","shortLabel":"删"}', 1203, 2, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93001104, 93001002, '导出设备', 'iot:devices:export', '', '', '', '{"caption":"设备资产中心导出设备按钮权限","shortLabel":"导"}', 1204, 2, 2, 1, 1, NOW(), 1, NOW(), 0)
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
  AND menu_id IN (93000001, 93001002, 93001101, 93001102, 93001103, 93001104);

SET @role_menu_id = COALESCE((SELECT MAX(id) FROM sys_role_menu), 96000000);

INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT (@role_menu_id := @role_menu_id + 1), 1, seed.role_id, seed.menu_id, 1, NOW(), 1, NOW(), 0
FROM (
    SELECT @role_business_id AS role_id, 93000001 AS menu_id
    UNION ALL SELECT @role_business_id, 93001002
    UNION ALL SELECT @role_business_id, 93001104

    UNION ALL SELECT @role_management_id, 93000001
    UNION ALL SELECT @role_management_id, 93001002
    UNION ALL SELECT @role_management_id, 93001101
    UNION ALL SELECT @role_management_id, 93001102
    UNION ALL SELECT @role_management_id, 93001103
    UNION ALL SELECT @role_management_id, 93001104

    UNION ALL SELECT @role_ops_id, 93000001
    UNION ALL SELECT @role_ops_id, 93001002
    UNION ALL SELECT @role_ops_id, 93001101
    UNION ALL SELECT @role_ops_id, 93001102
    UNION ALL SELECT @role_ops_id, 93001103
    UNION ALL SELECT @role_ops_id, 93001104

    UNION ALL SELECT @role_developer_id, 93000001
    UNION ALL SELECT @role_developer_id, 93001002
    UNION ALL SELECT @role_developer_id, 93001101
    UNION ALL SELECT @role_developer_id, 93001102
    UNION ALL SELECT @role_developer_id, 93001103
    UNION ALL SELECT @role_developer_id, 93001104

    UNION ALL SELECT @role_super_admin_id, 93000001
    UNION ALL SELECT @role_super_admin_id, 93001002
    UNION ALL SELECT @role_super_admin_id, 93001101
    UNION ALL SELECT @role_super_admin_id, 93001102
    UNION ALL SELECT @role_super_admin_id, 93001103
    UNION ALL SELECT @role_super_admin_id, 93001104
) seed
WHERE seed.role_id IS NOT NULL;
