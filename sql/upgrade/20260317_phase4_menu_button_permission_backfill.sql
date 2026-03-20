USE rm_iot;

-- 仅补齐菜单管理按钮权限与当前 SUPER_ADMIN 绑定，适用于历史库已升级但按钮权限未落到真实超管角色的场景。

INSERT INTO sys_menu (
    id, parent_id, menu_name, menu_code, path, component, icon, meta_json,
    sort, type, menu_type, status, create_by, create_time, update_by, update_time, deleted
)
VALUES
    (93003301, 93003008, '新增菜单', 'system:menu:add', '', '', '', '{"caption":"新增菜单按钮权限","shortLabel":"增"}', 3801, 2, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93003302, 93003008, '编辑菜单', 'system:menu:update', '', '', '', '{"caption":"编辑菜单按钮权限","shortLabel":"编"}', 3802, 2, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93003303, 93003008, '删除菜单', 'system:menu:delete', '', '', '', '{"caption":"删除菜单按钮权限","shortLabel":"删"}', 3803, 2, 2, 1, 1, NOW(), 1, NOW(), 0)
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

SET @role_super_admin_id = (
    SELECT id
    FROM sys_role
    WHERE role_code = 'SUPER_ADMIN'
      AND deleted = 0
    ORDER BY id
    LIMIT 1
);

SET @role_menu_id = COALESCE((SELECT MAX(id) FROM sys_role_menu), 96000000);

INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT (@role_menu_id := @role_menu_id + 1), 1, @role_super_admin_id, menu_seed.menu_id, 1, NOW(), 1, NOW(), 0
FROM (
    SELECT 93003301 AS menu_id
    UNION ALL
    SELECT 93003302
    UNION ALL
    SELECT 93003303
) menu_seed
WHERE @role_super_admin_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu rm
      WHERE rm.deleted = 0
        AND rm.role_id = @role_super_admin_id
        AND rm.menu_id = menu_seed.menu_id
  );
