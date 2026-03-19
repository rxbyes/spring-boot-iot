USE rm_iot;

-- 真实基础数据（可重复执行）
-- 前置：已执行最新 sql/init.sql。

SET NAMES utf8mb4;

-- =========================
-- 1) 租户/用户/角色基础数据
-- =========================
INSERT INTO sys_tenant (
    id, tenant_name, tenant_code, contact_name, contact_phone, contact_email, status, remark, create_time, update_time, deleted
) VALUES (
    1, '默认租户', 'default', '平台管理员', '13800000000', 'admin@ghlzm.com', 1, '真实环境基础租户', NOW(), NOW(), 0
)
ON DUPLICATE KEY UPDATE
    tenant_name = VALUES(tenant_name),
    contact_name = VALUES(contact_name),
    contact_phone = VALUES(contact_phone),
    contact_email = VALUES(contact_email),
    status = VALUES(status),
    remark = VALUES(remark),
    update_time = NOW(),
    deleted = 0;

INSERT INTO sys_role (
    id, tenant_id, role_name, role_code, description, status, create_by, create_time, update_by, update_time, deleted
) VALUES
    (92000001, 1, '业务人员', 'BUSINESS_STAFF', '负责风险监测、告警研判、事件处置与业务复盘。', 1, 1, NOW(), 1, NOW(), 0),
    (92000002, 1, '管理人员', 'MANAGEMENT_STAFF', '负责业务统筹、规则审批、系统治理与运营管理。', 1, 1, NOW(), 1, NOW(), 0),
    (92000003, 1, '运维人员', 'OPS_STAFF', '负责设备接入、联调排障、运行维护与问题闭环。', 1, 1, NOW(), 1, NOW(), 0),
    (92000004, 1, '开发人员', 'DEVELOPER_STAFF', '负责协议联调、规则开发、缺陷定位与功能验证。', 1, 1, NOW(), 1, NOW(), 0),
    (92000005, 1, '超级管理员', 'SUPER_ADMIN', '拥有全部菜单与操作权限。', 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    description = VALUES(description),
    status = VALUES(status),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

-- 初始化演示账号默认密码：123456（BCrypt）
INSERT INTO sys_user (
    id, tenant_id, username, password, nickname, real_name, phone, email, status, is_admin,
    remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (1, 1, 'admin', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '平台总控官', '超级管理员', '13800000000', 'admin@ghlzm.com', 1, 1,
     '超级管理员演示账号，默认查看平台治理与全量菜单。', 1, NOW(), 1, NOW(), 0),
    (2, 1, 'biz_demo', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '风险运营专员', '业务演示账号', '13800000001', 'biz_demo@ghlzm.com', 1, 0,
     '业务人员演示账号，默认进入风险运营工作台。', 1, NOW(), 1, NOW(), 0),
    (3, 1, 'manager_demo', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '运营管理负责人', '管理演示账号', '13800000002', 'manager_demo@ghlzm.com', 1, 0,
     '管理人员演示账号，默认进入风险运营并覆盖风险策略、平台治理。', 1, NOW(), 1, NOW(), 0),
    (4, 1, 'ops_demo', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '接入运维工程师', '运维演示账号', '13800000003', 'ops_demo@ghlzm.com', 1, 0,
     '运维人员演示账号，默认进入接入智维工作台。', 1, NOW(), 1, NOW(), 0),
    (5, 1, 'dev_demo', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '平台开发工程师', '开发演示账号', '13800000004', 'dev_demo@ghlzm.com', 1, 0,
     '开发人员演示账号，默认进入接入智维并开放质量工场。', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    real_name = VALUES(real_name),
    phone = VALUES(phone),
    email = VALUES(email),
    status = VALUES(status),
    is_admin = VALUES(is_admin),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

SET @user_admin_id = (SELECT id FROM sys_user WHERE tenant_id = 1 AND username = 'admin' AND deleted = 0 ORDER BY id LIMIT 1);
SET @user_business_demo_id = (SELECT id FROM sys_user WHERE tenant_id = 1 AND username = 'biz_demo' AND deleted = 0 ORDER BY id LIMIT 1);
SET @user_management_demo_id = (SELECT id FROM sys_user WHERE tenant_id = 1 AND username = 'manager_demo' AND deleted = 0 ORDER BY id LIMIT 1);
SET @user_ops_demo_id = (SELECT id FROM sys_user WHERE tenant_id = 1 AND username = 'ops_demo' AND deleted = 0 ORDER BY id LIMIT 1);
SET @user_developer_demo_id = (SELECT id FROM sys_user WHERE tenant_id = 1 AND username = 'dev_demo' AND deleted = 0 ORDER BY id LIMIT 1);

SET @role_business_id = (SELECT id FROM sys_role WHERE role_code = 'BUSINESS_STAFF' AND deleted = 0 ORDER BY id LIMIT 1);
SET @role_management_id = (SELECT id FROM sys_role WHERE role_code = 'MANAGEMENT_STAFF' AND deleted = 0 ORDER BY id LIMIT 1);
SET @role_ops_id = (SELECT id FROM sys_role WHERE role_code = 'OPS_STAFF' AND deleted = 0 ORDER BY id LIMIT 1);
SET @role_developer_id = (SELECT id FROM sys_role WHERE role_code = 'DEVELOPER_STAFF' AND deleted = 0 ORDER BY id LIMIT 1);
SET @role_super_admin_id = (SELECT id FROM sys_role WHERE role_code = 'SUPER_ADMIN' AND deleted = 0 ORDER BY id LIMIT 1);

DELETE FROM sys_user_role
WHERE tenant_id = 1
  AND user_id IN (@user_admin_id, @user_business_demo_id, @user_management_demo_id, @user_ops_demo_id, @user_developer_demo_id);

SET @user_role_seed := COALESCE((SELECT MAX(id) FROM sys_user_role), 0);

INSERT INTO sys_user_role (
    id, tenant_id, user_id, role_id, create_by, create_time, update_by, update_time, deleted
)
SELECT
    (@user_role_seed := @user_role_seed + 1),
    1,
    t.user_id,
    t.role_id,
    1,
    NOW(),
    1,
    NOW(),
    0
FROM (
    SELECT 1 AS sort_no, @user_admin_id AS user_id, @role_super_admin_id AS role_id
    UNION ALL
    SELECT 2, @user_business_demo_id, @role_business_id
    UNION ALL
    SELECT 3, @user_management_demo_id, @role_management_id
    UNION ALL
    SELECT 4, @user_ops_demo_id, @role_ops_id
    UNION ALL
    SELECT 5, @user_developer_demo_id, @role_developer_id
) t
WHERE t.user_id IS NOT NULL
  AND t.role_id IS NOT NULL
ORDER BY t.sort_no;

INSERT INTO sys_menu (
    id, tenant_id, parent_id, menu_name, menu_code, path, component, icon, meta_json, sort, type, menu_type,
    route_path, permission, sort_no, visible, status, create_by, create_time, update_by, update_time, deleted
) VALUES
    (93000001, 1, 0, '接入智维', 'iot-access', '', 'Layout', 'connection', '{"description":"资产、链路与异常观测","menuTitle":"接入智维","menuHint":"覆盖产品定义、设备资产、链路验证、异常观测与数据校验。"}', 10, 0, 0, '', 'iot-access', 10, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000002, 1, 0, '风险运营', 'risk-ops', '', 'Layout', 'warning', '{"description":"态势、告警与协同闭环","menuTitle":"风险运营","menuHint":"覆盖实时监测、告警运营、事件协同、对象洞察与运营复盘。"}', 20, 0, 0, '', 'risk-ops', 20, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000004, 1, 0, '风险策略', 'risk-config', '', 'Layout', 'operation', '{"description":"对象、阈值与联动配置","menuTitle":"风险策略","menuHint":"覆盖风险对象、阈值策略、联动编排与应急预案库。"}', 30, 0, 0, '', 'risk-config', 30, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000003, 1, 0, '平台治理', 'system-governance', '', 'Layout', 'setting', '{"description":"组织、权限与审计治理","menuTitle":"平台治理","menuHint":"覆盖组织、账号、角色、导航、区域、字典、通知与审计中心。"}', 40, 0, 0, '', 'system-governance', 40, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000005, 1, 0, '质量工场', 'quality-workbench', '', 'Layout', 'monitor', '{"description":"自动化与质量基线","menuTitle":"质量工场","menuHint":"覆盖自动化编排、回归计划与质量巡检资产。"}', 50, 0, 0, '', 'quality-workbench', 50, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93001001, 1, 93000001, '产品定义中心', 'iot:products', '/products', 'ProductWorkbenchView', 'box', '{"caption":"产品模型、协议绑定与设备归属基线"}', 11, 1, 1, '/products', 'iot:products', 11, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001002, 1, 93000001, '设备资产中心', 'iot:devices', '/devices', 'DeviceWorkbenchView', 'cpu', '{"caption":"设备建档、在线状态与资产运维"}', 12, 1, 1, '/devices', 'iot:devices', 12, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001003, 1, 93000001, '链路验证中心', 'iot:reporting', '/reporting', 'ReportWorkbenchView', 'promotion', '{"caption":"HTTP 上报模拟、payload 回放与联调"}', 13, 1, 1, '/reporting', 'iot:reporting', 13, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001006, 1, 93000001, '异常观测台', 'iot:system-log', '/system-log', 'AuditLogView', 'warning', '{"caption":"研发测试定位系统异常与接入问题"}', 14, 1, 1, '/system-log', 'iot:system-log', 14, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001007, 1, 93000001, '链路追踪台', 'iot:message-trace', '/message-trace', 'MessageTraceView', 'tickets', '{"caption":"按 TraceId、设备编码与 Topic 排查设备接入链路"}', 15, 1, 1, '/message-trace', 'iot:message-trace', 15, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001005, 1, 93000001, '数据校验台', 'iot:file-debug', '/file-debug', 'FilePayloadDebugView', 'document', '{"caption":"文件快照与固件聚合结果核验"}', 16, 1, 1, '/file-debug', 'iot:file-debug', 16, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93002008, 1, 93000002, '实时监测台', 'risk:monitoring', '/risk-monitoring', 'RealTimeMonitoringView', 'monitor', '{"caption":"风险监测列表、筛选与详情抽屉"}', 21, 1, 1, '/risk-monitoring', 'risk:monitoring', 21, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002009, 1, 93000002, 'GIS态势图', 'risk:monitoring-gis', '/risk-monitoring-gis', 'RiskGisView', 'map-location', '{"caption":"点位态势、未定位风险点与地图联动"}', 22, 1, 1, '/risk-monitoring-gis', 'risk:monitoring-gis', 22, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002001, 1, 93000002, '告警运营台', 'risk:alarm', '/alarm-center', 'AlarmCenterView', 'bell', '{"caption":"告警列表、确认、抑制与关闭"}', 23, 1, 1, '/alarm-center', 'risk:alarm', 23, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002002, 1, 93000002, '事件协同台', 'risk:event', '/event-disposal', 'EventDisposalView', 'flag', '{"caption":"工单派发、处置反馈与事件闭环"}', 24, 1, 1, '/event-disposal', 'risk:event', 24, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001004, 1, 93000002, '对象洞察台', 'iot:insight', '/insight', 'DeviceInsightView', 'data-analysis', '{"caption":"设备属性、消息日志与风险研判线索"}', 25, 1, 1, '/insight', 'iot:insight', 25, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002007, 1, 93000002, '运营分析中心', 'risk:report', '/report-analysis', 'ReportAnalysisView', 'trend-charts', '{"caption":"风险趋势、告警统计与设备健康复盘"}', 26, 1, 1, '/report-analysis', 'risk:report', 26, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93002003, 1, 93000004, '风险对象中心', 'risk:point', '/risk-point', 'RiskPointView', 'location', '{"caption":"风险对象建档、设备绑定与等级治理"}', 31, 1, 1, '/risk-point', 'risk:point', 31, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002004, 1, 93000004, '阈值策略', 'risk:rule-definition', '/rule-definition', 'RuleDefinitionView', 'set-up', '{"caption":"阈值规则维护与触发条件治理"}', 32, 1, 1, '/rule-definition', 'risk:rule-definition', 32, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002005, 1, 93000004, '联动编排', 'risk:linkage-rule', '/linkage-rule', 'LinkageRuleView', 'operation', '{"caption":"触发条件与联动动作编排"}', 33, 1, 1, '/linkage-rule', 'risk:linkage-rule', 33, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002006, 1, 93000004, '应急预案库', 'risk:emergency-plan', '/emergency-plan', 'EmergencyPlanView', 'tickets', '{"caption":"预案维护、步骤编排与响应协同"}', 34, 1, 1, '/emergency-plan', 'risk:emergency-plan', 34, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93003001, 1, 93000003, '组织架构', 'system:organization', '/organization', 'OrganizationView', 'office-building', '{"caption":"组织树维护与责任主体管理"}', 41, 1, 1, '/organization', 'system:organization', 41, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003002, 1, 93000003, '账号中心', 'system:user', '/user', 'UserView', 'user', '{"caption":"账号维护、状态管理与密码重置"}', 42, 1, 1, '/user', 'system:user', 42, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003003, 1, 93000003, '角色权限', 'system:role', '/role', 'RoleView', 'avatar', '{"caption":"角色维护与菜单授权管理"}', 43, 1, 1, '/role', 'system:role', 43, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003008, 1, 93000003, '导航编排', 'system:menu', '/menu', 'MenuView', 'menu', '{"caption":"菜单树结构与页面权限项维护"}', 44, 1, 1, '/menu', 'system:menu', 44, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003004, 1, 93000003, '区域版图', 'system:region', '/region', 'RegionView', 'place', '{"caption":"区域树与业务区域归属维护"}', 45, 1, 1, '/region', 'system:region', 45, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003005, 1, 93000003, '数据字典', 'system:dict', '/dict', 'DictView', 'collection', '{"caption":"字典类型与字典项配置"}', 46, 1, 1, '/dict', 'system:dict', 46, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003006, 1, 93000003, '通知编排', 'system:channel', '/channel', 'ChannelView', 'chat-dot-round', '{"caption":"通知渠道配置、启停与测试"}', 47, 1, 1, '/channel', 'system:channel', 47, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003007, 1, 93000003, '审计中心', 'system:audit', '/audit-log', 'AuditLogView', 'document-checked', '{"caption":"客户与治理侧业务操作审计"}', 48, 1, 1, '/audit-log', 'system:audit', 48, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93003009, 1, 93000005, '自动化工场', 'system:automation-test', '/automation-test', 'AutomationTestCenterView', 'monitor', '{"caption":"配置驱动场景编排、执行计划与报告导出"}', 51, 1, 1, '/automation-test', 'system:automation-test', 51, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001101, 1, 93001002, '新增设备', 'iot:devices:add', '', '', '', '{"caption":"设备资产中心新增设备按钮权限"}', 1201, 2, 2, '', 'iot:devices:add', 1201, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001102, 1, 93001002, '编辑设备', 'iot:devices:update', '', '', '', '{"caption":"设备资产中心编辑设备按钮权限"}', 1202, 2, 2, '', 'iot:devices:update', 1202, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001103, 1, 93001002, '删除设备', 'iot:devices:delete', '', '', '', '{"caption":"设备资产中心删除设备按钮权限"}', 1203, 2, 2, '', 'iot:devices:delete', 1203, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001104, 1, 93001002, '导出设备', 'iot:devices:export', '', '', '', '{"caption":"设备资产中心导出设备按钮权限"}', 1204, 2, 2, '', 'iot:devices:export', 1204, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93003101, 1, 93003002, '新增用户', 'system:user:add', '', '', '', '{"caption":"新增用户按钮权限"}', 3201, 2, 2, '', 'system:user:add', 3201, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003102, 1, 93003002, '编辑用户', 'system:user:update', '', '', '', '{"caption":"编辑用户按钮权限"}', 3202, 2, 2, '', 'system:user:update', 3202, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003103, 1, 93003002, '删除用户', 'system:user:delete', '', '', '', '{"caption":"删除用户按钮权限"}', 3203, 2, 2, '', 'system:user:delete', 3203, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003104, 1, 93003002, '重置密码', 'system:user:reset-password', '', '', '', '{"caption":"重置密码按钮权限"}', 3204, 2, 2, '', 'system:user:reset-password', 3204, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003201, 1, 93003003, '新增角色', 'system:role:add', '', '', '', '{"caption":"新增角色按钮权限"}', 3301, 2, 2, '', 'system:role:add', 3301, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003202, 1, 93003003, '编辑角色', 'system:role:update', '', '', '', '{"caption":"编辑角色按钮权限"}', 3302, 2, 2, '', 'system:role:update', 3302, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003203, 1, 93003003, '删除角色', 'system:role:delete', '', '', '', '{"caption":"删除角色按钮权限"}', 3303, 2, 2, '', 'system:role:delete', 3303, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003301, 1, 93003008, '新增菜单', 'system:menu:add', '', '', '', '{"caption":"新增菜单按钮权限"}', 3801, 2, 2, '', 'system:menu:add', 3801, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003302, 1, 93003008, '编辑菜单', 'system:menu:update', '', '', '', '{"caption":"编辑菜单按钮权限"}', 3802, 2, 2, '', 'system:menu:update', 3802, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003303, 1, 93003008, '删除菜单', 'system:menu:delete', '', '', '', '{"caption":"删除菜单按钮权限"}', 3803, 2, 2, '', 'system:menu:delete', 3803, 1, 1, 1, NOW(), 1, NOW(), 0)
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

DELETE FROM sys_role_menu
WHERE role_id IN (@role_business_id, @role_management_id, @role_ops_id, @role_developer_id, @role_super_admin_id);

INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
VALUES
    (96010001, 1, @role_business_id, 93000002, 1, NOW(), 1, NOW(), 0),
    (96010002, 1, @role_business_id, 93002008, 1, NOW(), 1, NOW(), 0),
    (96010003, 1, @role_business_id, 93002009, 1, NOW(), 1, NOW(), 0),
    (96010004, 1, @role_business_id, 93002001, 1, NOW(), 1, NOW(), 0),
    (96010005, 1, @role_business_id, 93002002, 1, NOW(), 1, NOW(), 0),
    (96010006, 1, @role_business_id, 93001004, 1, NOW(), 1, NOW(), 0),
    (96010007, 1, @role_business_id, 93002007, 1, NOW(), 1, NOW(), 0),
    (96010008, 1, @role_business_id, 93000001, 1, NOW(), 1, NOW(), 0),
    (96010009, 1, @role_business_id, 93001002, 1, NOW(), 1, NOW(), 0),
    (96010010, 1, @role_business_id, 93001104, 1, NOW(), 1, NOW(), 0),

    (96010021, 1, @role_management_id, 93000002, 1, NOW(), 1, NOW(), 0),
    (96010022, 1, @role_management_id, 93002008, 1, NOW(), 1, NOW(), 0),
    (96010023, 1, @role_management_id, 93002009, 1, NOW(), 1, NOW(), 0),
    (96010024, 1, @role_management_id, 93002001, 1, NOW(), 1, NOW(), 0),
    (96010025, 1, @role_management_id, 93002002, 1, NOW(), 1, NOW(), 0),
    (96010026, 1, @role_management_id, 93001004, 1, NOW(), 1, NOW(), 0),
    (96010027, 1, @role_management_id, 93002007, 1, NOW(), 1, NOW(), 0),
    (96010028, 1, @role_management_id, 93000004, 1, NOW(), 1, NOW(), 0),
    (96010029, 1, @role_management_id, 93002003, 1, NOW(), 1, NOW(), 0),
    (96010030, 1, @role_management_id, 93002004, 1, NOW(), 1, NOW(), 0),
    (96010031, 1, @role_management_id, 93002005, 1, NOW(), 1, NOW(), 0),
    (96010032, 1, @role_management_id, 93002006, 1, NOW(), 1, NOW(), 0),
    (96010033, 1, @role_management_id, 93000003, 1, NOW(), 1, NOW(), 0),
    (96010034, 1, @role_management_id, 93003001, 1, NOW(), 1, NOW(), 0),
    (96010035, 1, @role_management_id, 93003002, 1, NOW(), 1, NOW(), 0),
    (96010036, 1, @role_management_id, 93003003, 1, NOW(), 1, NOW(), 0),
    (96010037, 1, @role_management_id, 93003004, 1, NOW(), 1, NOW(), 0),
    (96010038, 1, @role_management_id, 93003005, 1, NOW(), 1, NOW(), 0),
    (96010039, 1, @role_management_id, 93003006, 1, NOW(), 1, NOW(), 0),
    (96010040, 1, @role_management_id, 93003007, 1, NOW(), 1, NOW(), 0),
    (96010041, 1, @role_management_id, 93003101, 1, NOW(), 1, NOW(), 0),
    (96010042, 1, @role_management_id, 93003102, 1, NOW(), 1, NOW(), 0),
    (96010043, 1, @role_management_id, 93003104, 1, NOW(), 1, NOW(), 0),
    (96010044, 1, @role_management_id, 93003201, 1, NOW(), 1, NOW(), 0),
    (96010045, 1, @role_management_id, 93003202, 1, NOW(), 1, NOW(), 0),
    (96010046, 1, @role_management_id, 93000001, 1, NOW(), 1, NOW(), 0),
    (96010047, 1, @role_management_id, 93001002, 1, NOW(), 1, NOW(), 0),
    (96010048, 1, @role_management_id, 93001101, 1, NOW(), 1, NOW(), 0),
    (96010049, 1, @role_management_id, 93001102, 1, NOW(), 1, NOW(), 0),
    (96010050, 1, @role_management_id, 93001103, 1, NOW(), 1, NOW(), 0),
    (96010051, 1, @role_management_id, 93001104, 1, NOW(), 1, NOW(), 0),

    (96010061, 1, @role_ops_id, 93000001, 1, NOW(), 1, NOW(), 0),
    (96010062, 1, @role_ops_id, 93001001, 1, NOW(), 1, NOW(), 0),
    (96010063, 1, @role_ops_id, 93001002, 1, NOW(), 1, NOW(), 0),
    (96010064, 1, @role_ops_id, 93001003, 1, NOW(), 1, NOW(), 0),
    (96010065, 1, @role_ops_id, 93001006, 1, NOW(), 1, NOW(), 0),
    (96010066, 1, @role_ops_id, 93001007, 1, NOW(), 1, NOW(), 0),
    (96010067, 1, @role_ops_id, 93001005, 1, NOW(), 1, NOW(), 0),
    (96010068, 1, @role_ops_id, 93000002, 1, NOW(), 1, NOW(), 0),
    (96010069, 1, @role_ops_id, 93002008, 1, NOW(), 1, NOW(), 0),
    (96010070, 1, @role_ops_id, 93002001, 1, NOW(), 1, NOW(), 0),
    (96010071, 1, @role_ops_id, 93002002, 1, NOW(), 1, NOW(), 0),
    (96010072, 1, @role_ops_id, 93001004, 1, NOW(), 1, NOW(), 0),
    (96010073, 1, @role_ops_id, 93000004, 1, NOW(), 1, NOW(), 0),
    (96010074, 1, @role_ops_id, 93002003, 1, NOW(), 1, NOW(), 0),
    (96010075, 1, @role_ops_id, 93002004, 1, NOW(), 1, NOW(), 0),
    (96010076, 1, @role_ops_id, 93001101, 1, NOW(), 1, NOW(), 0),
    (96010077, 1, @role_ops_id, 93001102, 1, NOW(), 1, NOW(), 0),
    (96010078, 1, @role_ops_id, 93001103, 1, NOW(), 1, NOW(), 0),
    (96010079, 1, @role_ops_id, 93001104, 1, NOW(), 1, NOW(), 0),

    (96010091, 1, @role_developer_id, 93000001, 1, NOW(), 1, NOW(), 0),
    (96010092, 1, @role_developer_id, 93001001, 1, NOW(), 1, NOW(), 0),
    (96010093, 1, @role_developer_id, 93001002, 1, NOW(), 1, NOW(), 0),
    (96010094, 1, @role_developer_id, 93001003, 1, NOW(), 1, NOW(), 0),
    (96010095, 1, @role_developer_id, 93001006, 1, NOW(), 1, NOW(), 0),
    (96010096, 1, @role_developer_id, 93001007, 1, NOW(), 1, NOW(), 0),
    (96010097, 1, @role_developer_id, 93001005, 1, NOW(), 1, NOW(), 0),
    (96010098, 1, @role_developer_id, 93000002, 1, NOW(), 1, NOW(), 0),
    (96010099, 1, @role_developer_id, 93002008, 1, NOW(), 1, NOW(), 0),
    (96010100, 1, @role_developer_id, 93002009, 1, NOW(), 1, NOW(), 0),
    (96010101, 1, @role_developer_id, 93002001, 1, NOW(), 1, NOW(), 0),
    (96010102, 1, @role_developer_id, 93002002, 1, NOW(), 1, NOW(), 0),
    (96010103, 1, @role_developer_id, 93001004, 1, NOW(), 1, NOW(), 0),
    (96010104, 1, @role_developer_id, 93000004, 1, NOW(), 1, NOW(), 0),
    (96010105, 1, @role_developer_id, 93002003, 1, NOW(), 1, NOW(), 0),
    (96010106, 1, @role_developer_id, 93002004, 1, NOW(), 1, NOW(), 0),
    (96010107, 1, @role_developer_id, 93002005, 1, NOW(), 1, NOW(), 0),
    (96010108, 1, @role_developer_id, 93002006, 1, NOW(), 1, NOW(), 0),
    (96010109, 1, @role_developer_id, 93000005, 1, NOW(), 1, NOW(), 0),
    (96010110, 1, @role_developer_id, 93003009, 1, NOW(), 1, NOW(), 0),
    (96010111, 1, @role_developer_id, 93001101, 1, NOW(), 1, NOW(), 0),
    (96010112, 1, @role_developer_id, 93001102, 1, NOW(), 1, NOW(), 0),
    (96010113, 1, @role_developer_id, 93001103, 1, NOW(), 1, NOW(), 0),
    (96010114, 1, @role_developer_id, 93001104, 1, NOW(), 1, NOW(), 0);

SET @role_menu_id := 96010900;
INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT (@role_menu_id := @role_menu_id + 1), 1, @role_super_admin_id, m.id, 1, NOW(), 1, NOW(), 0
FROM sys_menu m
WHERE m.deleted = 0
  AND @role_super_admin_id IS NOT NULL
ORDER BY m.sort, m.id;

-- =========================
-- 2) IoT 产品/设备/消息基线
-- =========================
INSERT INTO iot_product (
    id, tenant_id, product_key, product_name, protocol_code, node_type, data_format,
    manufacturer, description, status, remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (1001, 1, 'accept-http-product-01', '验收产品-HTTP-01', 'mqtt-json', 1, 'JSON', 'GHLZM', 'HTTP 主链路验收产品', 1, '真实环境基线', 1, NOW(), 1, NOW(), 0),
    (1002, 1, 'accept-mqtt-product-01', '验收产品-MQTT-01', 'mqtt-json', 1, 'JSON', 'GHLZM', 'MQTT 主链路验收产品', 1, '真实环境基线', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    product_name = VALUES(product_name),
    protocol_code = VALUES(protocol_code),
    node_type = VALUES(node_type),
    data_format = VALUES(data_format),
    manufacturer = VALUES(manufacturer),
    description = VALUES(description),
    status = VALUES(status),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO iot_product_model (
    id, tenant_id, product_id, model_type, identifier, model_name, data_type, specs_json,
    sort_no, required_flag, description, create_time, update_time, deleted
) VALUES
    (3001, 1, 1001, 'property', 'temperature', '温度', 'double', JSON_OBJECT('unit', '℃', 'min', -40, 'max', 200), 1, 0, '温度测点', NOW(), NOW(), 0),
    (3002, 1, 1001, 'property', 'humidity', '湿度', 'double', JSON_OBJECT('unit', '%', 'min', 0, 'max', 100), 2, 0, '湿度测点', NOW(), NOW(), 0),
    (3003, 1, 1001, 'property', 'pressure', '压力', 'double', JSON_OBJECT('unit', 'kPa', 'min', 80, 'max', 140), 3, 0, '压力测点', NOW(), NOW(), 0),
    (3004, 1, 1002, 'property', 'temperature', '温度', 'double', JSON_OBJECT('unit', '℃', 'min', -40, 'max', 200), 1, 0, '温度测点', NOW(), NOW(), 0),
    (3005, 1, 1002, 'property', 'vibration', '振动', 'double', JSON_OBJECT('unit', 'mm/s', 'min', 0, 'max', 30), 2, 0, '振动测点', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    model_name = VALUES(model_name),
    data_type = VALUES(data_type),
    specs_json = VALUES(specs_json),
    sort_no = VALUES(sort_no),
    required_flag = VALUES(required_flag),
    description = VALUES(description),
    update_time = NOW(),
    deleted = 0;

INSERT INTO iot_device (
    id, tenant_id, product_id, device_name, device_code, device_secret, client_id, username, password,
    protocol_code, node_type, online_status, activate_status, device_status, firmware_version,
    ip_address, last_online_time, last_report_time, longitude, latitude, address, metadata_json,
    remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (2001, 1, 1001, '验收设备-HTTP-01', 'accept-http-device-01', '123456', 'accept-http-device-01', 'accept-http-device-01', '123456',
     'mqtt-json', 1, 1, 1, 1, '1.0.0', '10.10.1.11', DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), 121.473700, 31.230400, '上海市黄浦区中山南路', JSON_OBJECT('line', 'A', 'workshop', 'W1'),
     'HTTP 链路验收设备', 1, NOW(), 1, NOW(), 0),
    (2002, 1, 1002, '验收设备-MQTT-01', 'accept-mqtt-device-01', '123456', 'accept-mqtt-device-01', 'accept-mqtt-device-01', '123456',
     'mqtt-json', 1, 1, 1, 1, '1.2.3', '10.10.1.12', DATE_SUB(NOW(), INTERVAL 8 MINUTE), DATE_SUB(NOW(), INTERVAL 1 MINUTE), 121.478900, 31.226600, '上海市黄浦区人民路', JSON_OBJECT('line', 'B', 'workshop', 'W2'),
     'MQTT 链路验收设备', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    product_id = VALUES(product_id),
    device_name = VALUES(device_name),
    protocol_code = VALUES(protocol_code),
    online_status = VALUES(online_status),
    activate_status = VALUES(activate_status),
    device_status = VALUES(device_status),
    firmware_version = VALUES(firmware_version),
    ip_address = VALUES(ip_address),
    last_online_time = VALUES(last_online_time),
    last_report_time = VALUES(last_report_time),
    longitude = VALUES(longitude),
    latitude = VALUES(latitude),
    address = VALUES(address),
    metadata_json = VALUES(metadata_json),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO iot_device_property (
    id, tenant_id, device_id, identifier, property_name, property_value, value_type, report_time, create_time, update_time
) VALUES
    (4001, 1, 2001, 'temperature', '温度', '26.5', 'double', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (4002, 1, 2001, 'humidity', '湿度', '68', 'double', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (4003, 1, 2001, 'pressure', '压力', '101.3', 'double', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (4004, 1, 2002, 'temperature', '温度', '31.2', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (4005, 1, 2002, 'vibration', '振动', '5.6', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW())
ON DUPLICATE KEY UPDATE
    property_name = VALUES(property_name),
    property_value = VALUES(property_value),
    value_type = VALUES(value_type),
    report_time = VALUES(report_time),
    update_time = NOW();

INSERT INTO iot_device_message_log (
    id, tenant_id, device_id, product_id, trace_id, device_code, product_key, message_type, topic, payload, report_time, create_time
) VALUES
    (5001, 1, 2001, 1001, 'trace-accept-http-0001', 'accept-http-device-01', 'accept-http-product-01', 'property', '/sys/accept-http-product-01/accept-http-device-01/thing/property/post',
     JSON_OBJECT('messageType', 'property', 'properties', JSON_OBJECT('temperature', 26.5, 'humidity', 68, 'pressure', 101.3)),
     DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW()),
    (5002, 1, 2002, 1002, 'trace-accept-mqtt-0001', 'accept-mqtt-device-01', 'accept-mqtt-product-01', 'property', '/sys/accept-mqtt-product-01/accept-mqtt-device-01/thing/property/post',
     JSON_OBJECT('messageType', 'property', 'properties', JSON_OBJECT('temperature', 31.2, 'vibration', 5.6)),
     DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW())
ON DUPLICATE KEY UPDATE
    payload = VALUES(payload),
    report_time = VALUES(report_time),
    create_time = NOW();

INSERT INTO iot_command_record (
    id, tenant_id, command_id, device_id, device_code, product_key, topic, command_type, service_identifier,
    request_payload, reply_payload, qos, retained, status, send_time, ack_time, timeout_time, error_message,
    remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (6001, 1, 'CMD-ACCEPT-001', 2001, 'accept-http-device-01', 'accept-http-product-01',
     '/sys/accept-http-product-01/accept-http-device-01/thing/property/set', 'property', NULL,
     '{"switch":1,"targetTemperature":23.0}', '{"code":0,"msg":"ok"}', 1, 0, 'SUCCESS',
     DATE_SUB(NOW(), INTERVAL 3 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), DATE_ADD(NOW(), INTERVAL 2 MINUTE), NULL,
     '验收下行指令样例', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    request_payload = VALUES(request_payload),
    reply_payload = VALUES(reply_payload),
    status = VALUES(status),
    send_time = VALUES(send_time),
    ack_time = VALUES(ack_time),
    timeout_time = VALUES(timeout_time),
    error_message = VALUES(error_message),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

-- =========================
-- 3) 系统管理基础数据
-- =========================
INSERT INTO sys_region (
    id, tenant_id, region_name, region_code, parent_id, region_type, longitude, latitude,
    status, sort_no, remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (7001, 1, '华东示范区', 'EAST-DEMO', 0, 'province', 121.473700, 31.230400, 1, 1, '真实环境演示区域', 1, NOW(), 1, NOW(), 0),
    (7002, 1, '黄浦厂区', 'HP-PLANT', 7001, 'district', 121.478900, 31.226600, 1, 1, '风险点所属区域', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    region_name = VALUES(region_name),
    parent_id = VALUES(parent_id),
    region_type = VALUES(region_type),
    longitude = VALUES(longitude),
    latitude = VALUES(latitude),
    status = VALUES(status),
    sort_no = VALUES(sort_no),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO sys_organization (
    id, tenant_id, parent_id, org_name, org_code, org_type, leader_user_id, leader_name,
    phone, email, status, sort_no, remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (7101, 1, 0, '平台运维中心', 'OPS-CENTER', 'dept', 1, '系统管理员', '13800000000', 'ops@ghlzm.com', 1, 1, '运维中心', 1, NOW(), 1, NOW(), 0),
    (7102, 1, 7101, '告警处置组', 'ALARM-TEAM', 'team', 1, '系统管理员', '13800000000', 'alarm@ghlzm.com', 1, 2, '告警事件处置团队', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    org_name = VALUES(org_name),
    parent_id = VALUES(parent_id),
    org_type = VALUES(org_type),
    leader_user_id = VALUES(leader_user_id),
    leader_name = VALUES(leader_name),
    phone = VALUES(phone),
    email = VALUES(email),
    status = VALUES(status),
    sort_no = VALUES(sort_no),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO sys_dict (
    id, tenant_id, dict_name, dict_code, dict_type, status, sort_no, remark,
    create_by, create_time, update_by, update_time, deleted
) VALUES
    (7201, 1, '风险等级', 'risk_level', 'text', 1, 1, '风险等级字典', 1, NOW(), 1, NOW(), 0),
    (7202, 1, '告警等级', 'alarm_level', 'text', 1, 2, '告警等级字典', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    dict_name = VALUES(dict_name),
    dict_type = VALUES(dict_type),
    status = VALUES(status),
    sort_no = VALUES(sort_no),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO sys_dict_item (
    id, tenant_id, dict_id, item_name, item_value, item_type, status, sort_no, remark,
    create_by, create_time, update_by, update_time, deleted
) VALUES
    (7301, 1, 7201, '严重', 'critical', 'string', 1, 1, '风险等级-严重', 1, NOW(), 1, NOW(), 0),
    (7302, 1, 7201, '警告', 'warning', 'string', 1, 2, '风险等级-警告', 1, NOW(), 1, NOW(), 0),
    (7303, 1, 7201, '提醒', 'info', 'string', 1, 3, '风险等级-提醒', 1, NOW(), 1, NOW(), 0),
    (7304, 1, 7202, '严重', 'critical', 'string', 1, 1, '告警等级-严重', 1, NOW(), 1, NOW(), 0),
    (7305, 1, 7202, '警告', 'warning', 'string', 1, 2, '告警等级-警告', 1, NOW(), 1, NOW(), 0),
    (7306, 1, 7202, '提醒', 'info', 'string', 1, 3, '告警等级-提醒', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    item_name = VALUES(item_name),
    item_type = VALUES(item_type),
    status = VALUES(status),
    sort_no = VALUES(sort_no),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO sys_notification_channel (
    id, tenant_id, channel_name, channel_code, channel_type, config, status, sort_no, remark,
    create_by, create_time, update_by, update_time, deleted
) VALUES
    (7401, 1, '邮件通知', 'email-default', 'email', JSON_OBJECT('host', 'smtp.example.com', 'port', 465, 'from', 'iot-alert@ghlzm.com'), 1, 1, '默认邮件通知', 1, NOW(), 1, NOW(), 0),
    (7402, 1, 'Webhook通知', 'webhook-default', 'webhook', JSON_OBJECT('url', 'https://example.com/iot/webhook', 'headers', JSON_OBJECT('Authorization', 'Bearer demo-token'), 'scenes', JSON_ARRAY('system_error'), 'timeoutMs', 3000, 'minIntervalSeconds', 300), 1, 2, '默认Webhook通知（含 system_error 自动通知场景）', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    channel_name = VALUES(channel_name),
    channel_type = VALUES(channel_type),
    config = VALUES(config),
    status = VALUES(status),
    sort_no = VALUES(sort_no),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO sys_audit_log (
    id, tenant_id, user_id, user_name, trace_id, device_code, product_key, operation_type, operation_module, operation_method,
    request_url, request_method, request_params, response_result, ip_address, location,
    operation_result, result_message, error_code, exception_class, operation_time, create_time, deleted
) VALUES
    (7501, 1, 1, 'admin', 'trace-audit-0001', NULL, NULL, 'select', 'device', 'DeviceService.list', '/api/device/list', 'GET', '{}', 'HTTP 200', '127.0.0.1', 'local', 1, 'OK', NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 MINUTE), NOW(), 0),
    (7502, 1, 1, 'admin', 'trace-audit-0002', NULL, NULL, 'update', 'alarm', 'AlarmRecordService.closeAlarm', '/api/alarm/2033491770125463554/close', 'POST', '{"closeUser":1}', 'HTTP 200', '127.0.0.1', 'local', 1, 'OK', NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), 0)
ON DUPLICATE KEY UPDATE
    response_result = VALUES(response_result),
    operation_result = VALUES(operation_result),
    result_message = VALUES(result_message),
    operation_time = VALUES(operation_time),
    create_time = NOW(),
    deleted = 0;

-- =========================
-- 4) 风险平台（告警/事件/风险点）
-- =========================
INSERT INTO risk_point (
    id, risk_point_code, risk_point_name, region_id, region_name, responsible_user, responsible_phone,
    risk_level, description, status, tenant_id, create_by, create_time, update_by, update_time, deleted
) VALUES
    (8001, 'RP-HP-001', '锅炉温压监测点', 7002, '黄浦厂区', 1, '13800000000', 'critical', '锅炉区高温高压风险监测', 0, 1, 1, NOW(), 1, NOW(), 0),
    (8002, 'RP-HP-002', '振动监测点', 7002, '黄浦厂区', 1, '13800000000', 'warning', '关键设备振动风险监测', 0, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    risk_point_name = VALUES(risk_point_name),
    region_id = VALUES(region_id),
    region_name = VALUES(region_name),
    responsible_user = VALUES(responsible_user),
    responsible_phone = VALUES(responsible_phone),
    risk_level = VALUES(risk_level),
    description = VALUES(description),
    status = VALUES(status),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO risk_point_device (
    id, risk_point_id, device_id, device_code, device_name, metric_identifier, metric_name,
    default_threshold, threshold_unit, tenant_id, create_by, create_time, update_by, update_time, deleted
) VALUES
    (8101, 8001, 2001, 'accept-http-device-01', '验收设备-HTTP-01', 'temperature', '温度', '80', '℃', 1, 1, NOW(), 1, NOW(), 0),
    (8102, 8001, 2001, 'accept-http-device-01', '验收设备-HTTP-01', 'pressure', '压力', '120', 'kPa', 1, 1, NOW(), 1, NOW(), 0),
    (8103, 8002, 2002, 'accept-mqtt-device-01', '验收设备-MQTT-01', 'vibration', '振动', '10', 'mm/s', 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    risk_point_id = VALUES(risk_point_id),
    device_id = VALUES(device_id),
    device_name = VALUES(device_name),
    metric_name = VALUES(metric_name),
    default_threshold = VALUES(default_threshold),
    threshold_unit = VALUES(threshold_unit),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO rule_definition (
    id, rule_name, metric_identifier, metric_name, expression, duration, alarm_level,
    notification_methods, convert_to_event, status, tenant_id, create_by, create_time, update_by, update_time, deleted
) VALUES
    (8201, '锅炉温度超限', 'temperature', '温度', 'value > 80', 60, 'critical', 'email,webhook', 1, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8202, '设备振动超限', 'vibration', '振动', 'value > 10', 120, 'warning', 'webhook', 1, 0, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    rule_name = VALUES(rule_name),
    metric_name = VALUES(metric_name),
    expression = VALUES(expression),
    duration = VALUES(duration),
    alarm_level = VALUES(alarm_level),
    notification_methods = VALUES(notification_methods),
    convert_to_event = VALUES(convert_to_event),
    status = VALUES(status),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO linkage_rule (
    id, rule_name, description, trigger_condition, action_list, status, tenant_id,
    create_by, create_time, update_by, update_time, deleted
) VALUES
    (8301, '温度超限联动规则', '温度超限触发通知并创建工单',
     JSON_OBJECT('metric', 'temperature', 'op', '>', 'threshold', 80),
     JSON_ARRAY(JSON_OBJECT('type', 'notify', 'channel', 'email-default'), JSON_OBJECT('type', 'createWorkOrder', 'priority', 'high')),
     0, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    rule_name = VALUES(rule_name),
    description = VALUES(description),
    trigger_condition = VALUES(trigger_condition),
    action_list = VALUES(action_list),
    status = VALUES(status),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO emergency_plan (
    id, plan_name, risk_level, description, response_steps, contact_list, status, tenant_id,
    create_by, create_time, update_by, update_time, deleted
) VALUES
    (8401, '锅炉超温应急预案', 'critical', '锅炉区域出现超温告警时执行',
     JSON_ARRAY('确认现场状态', '远程降载', '派发现场工单', '复盘关闭事件'),
     JSON_ARRAY(JSON_OBJECT('name', '值班长', 'phone', '13800000000'), JSON_OBJECT('name', '安全员', 'phone', '13800000001')),
     0, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    plan_name = VALUES(plan_name),
    risk_level = VALUES(risk_level),
    description = VALUES(description),
    response_steps = VALUES(response_steps),
    contact_list = VALUES(contact_list),
    status = VALUES(status),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO iot_alarm_record (
    id, alarm_code, alarm_title, alarm_type, alarm_level,
    region_id, region_name, risk_point_id, risk_point_name,
    device_id, device_code, device_name, metric_name, current_value, threshold_value,
    status, trigger_time, confirm_time, confirm_user, suppress_time, suppress_user, close_time, close_user,
    rule_id, rule_name, tenant_id, remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (8501, 'ALARM-20260317001', '锅炉温度超限', 'threshold', 'critical',
     7002, '黄浦厂区', 8001, '锅炉温压监测点',
     2001, 'accept-http-device-01', '验收设备-HTTP-01', 'temperature', '92.4', '80',
     0, DATE_SUB(NOW(), INTERVAL 30 MINUTE), NULL, NULL, NULL, NULL, NULL, NULL,
     8201, '锅炉温度超限', 1, '待确认告警', 1, NOW(), 1, NOW(), 0),
    (8502, 'ALARM-20260317002', '设备振动异常', 'threshold', 'warning',
     7002, '黄浦厂区', 8002, '振动监测点',
     2002, 'accept-mqtt-device-01', '验收设备-MQTT-01', 'vibration', '12.1', '10',
     3, DATE_SUB(NOW(), INTERVAL 50 MINUTE), DATE_SUB(NOW(), INTERVAL 45 MINUTE), 1, NULL, NULL, DATE_SUB(NOW(), INTERVAL 20 MINUTE), 1,
     8202, '设备振动超限', 1, '已闭环告警', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    alarm_title = VALUES(alarm_title),
    alarm_type = VALUES(alarm_type),
    alarm_level = VALUES(alarm_level),
    region_id = VALUES(region_id),
    region_name = VALUES(region_name),
    risk_point_id = VALUES(risk_point_id),
    risk_point_name = VALUES(risk_point_name),
    device_id = VALUES(device_id),
    device_code = VALUES(device_code),
    device_name = VALUES(device_name),
    metric_name = VALUES(metric_name),
    current_value = VALUES(current_value),
    threshold_value = VALUES(threshold_value),
    status = VALUES(status),
    trigger_time = VALUES(trigger_time),
    confirm_time = VALUES(confirm_time),
    confirm_user = VALUES(confirm_user),
    suppress_time = VALUES(suppress_time),
    suppress_user = VALUES(suppress_user),
    close_time = VALUES(close_time),
    close_user = VALUES(close_user),
    rule_id = VALUES(rule_id),
    rule_name = VALUES(rule_name),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO iot_event_record (
    id, event_code, event_title, alarm_id, alarm_code, alarm_level, risk_level,
    region_id, region_name, risk_point_id, risk_point_name,
    device_id, device_code, device_name, metric_name, current_value,
    status, responsible_user, urgency_level, arrival_time_limit, completion_time_limit,
    trigger_time, dispatch_time, dispatch_user, start_time, complete_time, close_time, close_user, close_reason, review_notes,
    tenant_id, remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (8601, 'EVENT-20260317001', '锅炉超温处置事件', 8501, 'ALARM-20260317001', 'critical', 'critical',
     7002, '黄浦厂区', 8001, '锅炉温压监测点',
     2001, 'accept-http-device-01', '验收设备-HTTP-01', 'temperature', '92.4',
     2, 1, 'high', 15, 120,
     DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 25 MINUTE), 1, DATE_SUB(NOW(), INTERVAL 20 MINUTE), NULL, NULL, NULL, NULL, '处理中',
     1, '进行中事件', 1, NOW(), 1, NOW(), 0),
    (8602, 'EVENT-20260317002', '振动异常复盘事件', 8502, 'ALARM-20260317002', 'warning', 'warning',
     7002, '黄浦厂区', 8002, '振动监测点',
     2002, 'accept-mqtt-device-01', '验收设备-MQTT-01', 'vibration', '12.1',
     4, 1, 'medium', 30, 240,
     DATE_SUB(NOW(), INTERVAL 50 MINUTE), DATE_SUB(NOW(), INTERVAL 45 MINUTE), 1, DATE_SUB(NOW(), INTERVAL 40 MINUTE), DATE_SUB(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE), 1, '处置完成关闭', '已完成复盘',
     1, '已闭环事件', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    event_title = VALUES(event_title),
    alarm_id = VALUES(alarm_id),
    alarm_code = VALUES(alarm_code),
    alarm_level = VALUES(alarm_level),
    risk_level = VALUES(risk_level),
    region_id = VALUES(region_id),
    region_name = VALUES(region_name),
    risk_point_id = VALUES(risk_point_id),
    risk_point_name = VALUES(risk_point_name),
    device_id = VALUES(device_id),
    device_code = VALUES(device_code),
    device_name = VALUES(device_name),
    metric_name = VALUES(metric_name),
    current_value = VALUES(current_value),
    status = VALUES(status),
    responsible_user = VALUES(responsible_user),
    urgency_level = VALUES(urgency_level),
    arrival_time_limit = VALUES(arrival_time_limit),
    completion_time_limit = VALUES(completion_time_limit),
    trigger_time = VALUES(trigger_time),
    dispatch_time = VALUES(dispatch_time),
    dispatch_user = VALUES(dispatch_user),
    start_time = VALUES(start_time),
    complete_time = VALUES(complete_time),
    close_time = VALUES(close_time),
    close_user = VALUES(close_user),
    close_reason = VALUES(close_reason),
    review_notes = VALUES(review_notes),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO iot_event_work_order (
    id, event_id, event_code, work_order_code, work_order_type,
    assign_user, receive_user, receive_time, start_time, complete_time,
    status, feedback, photos, tenant_id, remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (8701, 8601, 'EVENT-20260317001', 'WO-20260317001', 'onsite',
     1, 1, DATE_SUB(NOW(), INTERVAL 24 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE), NULL,
     2, '已到场排查，准备降载', JSON_ARRAY('https://example.com/img/wo-20260317001-1.jpg'), 1, '处理中工单', 1, NOW(), 1, NOW(), 0),
    (8702, 8602, 'EVENT-20260317002', 'WO-20260317002', 'inspection',
     1, 1, DATE_SUB(NOW(), INTERVAL 44 MINUTE), DATE_SUB(NOW(), INTERVAL 40 MINUTE), DATE_SUB(NOW(), INTERVAL 26 MINUTE),
     3, '处置完成，设备恢复稳定', JSON_ARRAY('https://example.com/img/wo-20260317002-1.jpg'), 1, '已完成工单', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    event_id = VALUES(event_id),
    event_code = VALUES(event_code),
    work_order_type = VALUES(work_order_type),
    assign_user = VALUES(assign_user),
    receive_user = VALUES(receive_user),
    receive_time = VALUES(receive_time),
    start_time = VALUES(start_time),
    complete_time = VALUES(complete_time),
    status = VALUES(status),
    feedback = VALUES(feedback),
    photos = VALUES(photos),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

-- 数据就绪说明
-- 1. IoT 主链路：产品/设备/物模型/属性/消息日志
-- 2. 风险平台：风险点、绑定、规则、联动、预案、告警、事件、工单
-- 3. 系统管理：组织、区域、字典、通知渠道、审计日志
