INSERT INTO sys_menu (
    id, tenant_id, parent_id, menu_name, menu_code, path, component, icon, meta_json, sort, type, menu_type,
    route_path, permission, sort_no, visible, status, create_by, create_time, update_by, update_time, deleted
) VALUES (
    93003009, 1, 93000003, '自动化测试', 'system:automation-test', '/automation-test', 'AutomationTestCenterView', 'monitor',
    '{"caption":"配置驱动场景编排、执行计划与报告导出"}', 39, 1, 1, '/automation-test', 'system:automation-test', 39,
    1, 1, 1, NOW(), 1, NOW(), 0
)
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
    route_path = VALUES(route_path),
    permission = VALUES(permission),
    sort_no = VALUES(sort_no),
    visible = VALUES(visible),
    status = VALUES(status),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    deleted = VALUES(deleted);

INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT 96030009, 1, r.id, 93003009, 1, NOW(), 1, NOW(), 0
FROM sys_role r
WHERE r.role_code = 'SUPER_ADMIN'
  AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu rm
      WHERE rm.role_id = r.id
        AND rm.menu_id = 93003009
        AND rm.deleted = 0
  );
