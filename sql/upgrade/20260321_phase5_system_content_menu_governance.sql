USE rm_iot;

-- 为平台治理工作台补齐“站内消息 / 帮助文档”菜单与按钮权限，
-- 适用于历史库已经完成五工作台改造，但尚未把系统内容编排页接入真实菜单授权的场景。

UPDATE sys_menu
SET meta_json = '{"description":"组织、权限与审计治理","menuTitle":"平台治理","menuHint":"覆盖组织、账号、角色、导航、区域、字典、通知、帮助与审计中心。"}',
    update_by = 1,
    update_time = NOW()
WHERE id = 93000003
  AND deleted = 0;

INSERT INTO sys_menu (
    id, parent_id, menu_name, menu_code, path, component, icon, meta_json,
    sort, type, menu_type, status, create_by, create_time, update_by, update_time, deleted
)
VALUES
    (93003010, 93000003, '站内消息', 'system:in-app-message', '/in-app-message', 'InAppMessageView', 'bell', '{"caption":"通知中心站内消息的分类、范围与时间窗口编排","shortLabel":"信"}', 48, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003011, 93000003, '帮助文档', 'system:help-doc', '/help-doc', 'HelpDocView', 'document-copy', '{"caption":"帮助中心业务类、技术类和 FAQ 资料编排","shortLabel":"帮"}', 49, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003007, 93000003, '审计中心', 'system:audit', '/audit-log', 'AuditLogView', 'document-checked', '{"caption":"客户与治理侧业务操作审计","shortLabel":"审"}', 50, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003401, 93003010, '新增站内消息', 'system:in-app-message:add', '', '', '', '{"caption":"新增站内消息按钮权限","shortLabel":"增"}', 3901, 2, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93003402, 93003010, '编辑站内消息', 'system:in-app-message:update', '', '', '', '{"caption":"编辑站内消息按钮权限","shortLabel":"编"}', 3902, 2, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93003403, 93003010, '删除站内消息', 'system:in-app-message:delete', '', '', '', '{"caption":"删除站内消息按钮权限","shortLabel":"删"}', 3903, 2, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93003501, 93003011, '新增帮助文档', 'system:help-doc:add', '', '', '', '{"caption":"新增帮助文档按钮权限","shortLabel":"增"}', 4001, 2, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93003502, 93003011, '编辑帮助文档', 'system:help-doc:update', '', '', '', '{"caption":"编辑帮助文档按钮权限","shortLabel":"编"}', 4002, 2, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93003503, 93003011, '删除帮助文档', 'system:help-doc:delete', '', '', '', '{"caption":"删除帮助文档按钮权限","shortLabel":"删"}', 4003, 2, 2, 1, 1, NOW(), 1, NOW(), 0)
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

SET @role_management_id = (
    SELECT id
    FROM sys_role
    WHERE role_code = 'MANAGEMENT_STAFF'
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

SET @role_menu_id = COALESCE((SELECT MAX(id) FROM sys_role_menu), 96000000);

INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT (@role_menu_id := @role_menu_id + 1), 1, target.role_id, target.menu_id, 1, NOW(), 1, NOW(), 0
FROM (
    SELECT @role_management_id AS role_id, 93003010 AS menu_id
    UNION ALL SELECT @role_management_id, 93003011
    UNION ALL SELECT @role_management_id, 93003401
    UNION ALL SELECT @role_management_id, 93003402
    UNION ALL SELECT @role_management_id, 93003403
    UNION ALL SELECT @role_management_id, 93003501
    UNION ALL SELECT @role_management_id, 93003502
    UNION ALL SELECT @role_management_id, 93003503
    UNION ALL SELECT @role_super_admin_id, 93003010
    UNION ALL SELECT @role_super_admin_id, 93003011
    UNION ALL SELECT @role_super_admin_id, 93003401
    UNION ALL SELECT @role_super_admin_id, 93003402
    UNION ALL SELECT @role_super_admin_id, 93003403
    UNION ALL SELECT @role_super_admin_id, 93003501
    UNION ALL SELECT @role_super_admin_id, 93003502
    UNION ALL SELECT @role_super_admin_id, 93003503
) target
WHERE target.role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu rm
      WHERE rm.deleted = 0
        AND rm.role_id = target.role_id
        AND rm.menu_id = target.menu_id
  );
