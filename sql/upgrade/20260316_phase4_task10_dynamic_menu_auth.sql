USE rm_iot;

ALTER TABLE sys_menu
    ADD COLUMN IF NOT EXISTS meta_json LONGTEXT NULL COMMENT 'ui meta json';

UPDATE sys_menu
SET deleted = 1,
    status = 0,
    update_time = CURRENT_TIMESTAMP
WHERE deleted = 0
  AND menu_code IN ('system', 'system:role', 'system:menu', 'alarm', 'event');

INSERT INTO sys_role (id, tenant_id, role_name, role_code, description, status, create_by, create_time, update_by, update_time, deleted)
VALUES
    (92000001, 1, '业务人员', 'BUSINESS_STAFF', '负责风险监测、告警研判、事件处置与业务复盘。', 1, 1, NOW(), 1, NOW(), 0),
    (92000002, 1, '管理人员', 'MANAGEMENT_STAFF', '负责业务统筹、规则审批、系统治理与运营管理。', 1, 1, NOW(), 1, NOW(), 0),
    (92000003, 1, '运维人员', 'OPS_STAFF', '负责设备接入、联调排障、运行维护与问题闭环。', 1, 1, NOW(), 1, NOW(), 0),
    (92000004, 1, '开发人员', 'DEVELOPER_STAFF', '负责协议联调、规则开发、缺陷定位与功能验证。', 1, 1, NOW(), 1, NOW(), 0),
    (92000005, 1, '超级管理人员', 'SUPER_ADMIN', '拥有全部菜单与操作权限。', 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    tenant_id = VALUES(tenant_id),
    role_name = VALUES(role_name),
    role_code = VALUES(role_code),
    description = VALUES(description),
    status = VALUES(status),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    deleted = VALUES(deleted);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, path, component, icon, meta_json, sort, type, status, create_by, create_time, update_by, update_time, deleted)
VALUES
    (93000001, 0, '设备接入', 'iot-access', '', 'Layout', 'connection', '{"description":"接入与运维","menuTitle":"设备接入与运维","menuHint":"管理产品模板、设备台账、上报回放与设备侧联调能力。"}', 10, 0, 1, 1, NOW(), 1, NOW(), 0),
    (93000002, 0, '预警处置', 'risk-ops', '', 'Layout', 'warning', '{"description":"闭环与复盘","menuTitle":"风险处置闭环","menuHint":"覆盖告警、事件、风险点、规则、预案与分析报表。"}', 20, 0, 1, 1, NOW(), 1, NOW(), 0),
    (93000003, 0, '系统治理', 'system-governance', '', 'Layout', 'setting', '{"description":"组织与审计","menuTitle":"组织治理与审计","menuHint":"维护组织、用户、权限、区域、字典、通知和审计日志。"}', 30, 0, 1, 1, NOW(), 1, NOW(), 0),

    (93001001, 93000001, '产品模板中心', 'iot:products', '/products', 'ProductWorkbenchView', 'box', '{"caption":"产品模板建模、协议绑定与设备归属","shortLabel":"产"}', 11, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001002, 93000001, '设备运维中心', 'iot:devices', '/devices', 'DeviceWorkbenchView', 'cpu', '{"caption":"设备建档、在线状态核查与基础运维","shortLabel":"设"}', 12, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001003, 93000001, '接入回放台', 'iot:reporting', '/reporting', 'ReportWorkbenchView', 'promotion', '{"caption":"HTTP 上报模拟、payload 回放与联调","shortLabel":"报"}', 13, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001004, 93000001, '风险点工作台', 'iot:insight', '/insight', 'DeviceInsightView', 'data-analysis', '{"caption":"设备属性、消息日志与风险研判线索","shortLabel":"洞"}', 14, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001005, 93000001, '文件与固件校验', 'iot:file-debug', '/file-debug', 'FilePayloadDebugView', 'document', '{"caption":"文件快照与固件聚合结果核验","shortLabel":"校"}', 15, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93002001, 93000002, '告警中心', 'risk:alarm', '/alarm-center', 'AlarmCenterView', 'bell', '{"caption":"告警列表、确认、抑制与关闭","shortLabel":"警"}', 21, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002002, 93000002, '事件处置', 'risk:event', '/event-disposal', 'EventDisposalView', 'flag', '{"caption":"工单派发、处置反馈与事件闭环","shortLabel":"事"}', 22, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002003, 93000002, '风险点管理', 'risk:point', '/risk-point', 'RiskPointView', 'location', '{"caption":"风险点建档、设备绑定与等级治理","shortLabel":"点"}', 23, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002004, 93000002, '阈值规则配置', 'risk:rule-definition', '/rule-definition', 'RuleDefinitionView', 'set-up', '{"caption":"阈值规则维护与触发条件治理","shortLabel":"阈"}', 24, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002005, 93000002, '联动规则', 'risk:linkage-rule', '/linkage-rule', 'LinkageRuleView', 'operation', '{"caption":"触发条件与联动动作编排","shortLabel":"联"}', 25, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002006, 93000002, '应急预案', 'risk:emergency-plan', '/emergency-plan', 'EmergencyPlanView', 'tickets', '{"caption":"预案维护、步骤编排与响应协同","shortLabel":"预"}', 26, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002007, 93000002, '分析报表', 'risk:report', '/report-analysis', 'ReportAnalysisView', 'trend-charts', '{"caption":"风险趋势、告警统计与设备健康复盘","shortLabel":"报"}', 27, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002008, 93000002, '实时监测', 'risk:monitoring', '/risk-monitoring', 'RealTimeMonitoringView', 'monitor', '{"caption":"风险监测列表、筛选与详情抽屉","shortLabel":"监"}', 28, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002009, 93000002, 'GIS 风险态势', 'risk:monitoring-gis', '/risk-monitoring-gis', 'RiskGisView', 'map-location', '{"caption":"点位态势、未定位风险点与地图联动","shortLabel":"图"}', 29, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93003001, 93000003, '组织机构', 'system:organization', '/organization', 'OrganizationView', 'office-building', '{"caption":"组织树维护与责任主体管理","shortLabel":"组"}', 31, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003002, 93000003, '用户管理', 'system:user', '/user', 'UserView', 'user', '{"caption":"用户维护、状态管理与密码重置","shortLabel":"用"}', 32, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003003, 93000003, '角色管理', 'system:role', '/role', 'RoleView', 'avatar', '{"caption":"角色维护与菜单授权管理","shortLabel":"角"}', 33, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003004, 93000003, '区域管理', 'system:region', '/region', 'RegionView', 'place', '{"caption":"区域树与业务区域归属维护","shortLabel":"区"}', 34, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003005, 93000003, '字典配置', 'system:dict', '/dict', 'DictView', 'collection', '{"caption":"字典类型与字典项配置","shortLabel":"字"}', 35, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003006, 93000003, '通知渠道', 'system:channel', '/channel', 'ChannelView', 'chat-dot-round', '{"caption":"通知渠道配置、启停与测试","shortLabel":"通"}', 36, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003007, 93000003, '审计日志', 'system:audit', '/audit-log', 'AuditLogView', 'document-checked', '{"caption":"关键操作记录审计与追踪","shortLabel":"审"}', 37, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93003101, 93003002, '新增用户', 'system:user:add', '', '', '', '{"caption":"新增用户按钮权限","shortLabel":"增"}', 3201, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93003102, 93003002, '编辑用户', 'system:user:update', '', '', '', '{"caption":"编辑用户按钮权限","shortLabel":"编"}', 3202, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93003103, 93003002, '删除用户', 'system:user:delete', '', '', '', '{"caption":"删除用户按钮权限","shortLabel":"删"}', 3203, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93003104, 93003002, '重置密码', 'system:user:reset-password', '', '', '', '{"caption":"重置密码按钮权限","shortLabel":"密"}', 3204, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93003201, 93003003, '新增角色', 'system:role:add', '', '', '', '{"caption":"新增角色按钮权限","shortLabel":"增"}', 3301, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93003202, 93003003, '编辑角色', 'system:role:update', '', '', '', '{"caption":"编辑角色按钮权限","shortLabel":"编"}', 3302, 2, 1, 1, NOW(), 1, NOW(), 0),
    (93003203, 93003003, '删除角色', 'system:role:delete', '', '', '', '{"caption":"删除角色按钮权限","shortLabel":"删"}', 3303, 2, 1, 1, NOW(), 1, NOW(), 0)
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
    status = VALUES(status),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    deleted = VALUES(deleted);

DELETE FROM sys_role_menu
WHERE role_id IN (92000001, 92000002, 92000003, 92000004, 92000005);

INSERT INTO sys_role_menu (id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
VALUES
    (94000001, 92000001, 93000002, 1, NOW(), 1, NOW(), 0),
    (94000002, 92000001, 93002001, 1, NOW(), 1, NOW(), 0),
    (94000003, 92000001, 93002002, 1, NOW(), 1, NOW(), 0),
    (94000004, 92000001, 93002003, 1, NOW(), 1, NOW(), 0),
    (94000005, 92000001, 93002007, 1, NOW(), 1, NOW(), 0),

    (94000011, 92000002, 93000002, 1, NOW(), 1, NOW(), 0),
    (94000012, 92000002, 93002001, 1, NOW(), 1, NOW(), 0),
    (94000013, 92000002, 93002002, 1, NOW(), 1, NOW(), 0),
    (94000014, 92000002, 93002003, 1, NOW(), 1, NOW(), 0),
    (94000015, 92000002, 93002004, 1, NOW(), 1, NOW(), 0),
    (94000016, 92000002, 93002005, 1, NOW(), 1, NOW(), 0),
    (94000017, 92000002, 93002006, 1, NOW(), 1, NOW(), 0),
    (94000018, 92000002, 93002007, 1, NOW(), 1, NOW(), 0),
    (94000019, 92000002, 93000003, 1, NOW(), 1, NOW(), 0),
    (94000020, 92000002, 93003001, 1, NOW(), 1, NOW(), 0),
    (94000021, 92000002, 93003002, 1, NOW(), 1, NOW(), 0),
    (94000022, 92000002, 93003003, 1, NOW(), 1, NOW(), 0),
    (94000023, 92000002, 93003004, 1, NOW(), 1, NOW(), 0),
    (94000024, 92000002, 93003005, 1, NOW(), 1, NOW(), 0),
    (94000025, 92000002, 93003006, 1, NOW(), 1, NOW(), 0),
    (94000026, 92000002, 93003007, 1, NOW(), 1, NOW(), 0),
    (94000027, 92000002, 93003101, 1, NOW(), 1, NOW(), 0),
    (94000028, 92000002, 93003102, 1, NOW(), 1, NOW(), 0),
    (94000029, 92000002, 93003104, 1, NOW(), 1, NOW(), 0),
    (94000030, 92000002, 93003201, 1, NOW(), 1, NOW(), 0),
    (94000031, 92000002, 93003202, 1, NOW(), 1, NOW(), 0),

    (94000041, 92000003, 93000001, 1, NOW(), 1, NOW(), 0),
    (94000042, 92000003, 93001001, 1, NOW(), 1, NOW(), 0),
    (94000043, 92000003, 93001002, 1, NOW(), 1, NOW(), 0),
    (94000044, 92000003, 93001003, 1, NOW(), 1, NOW(), 0),
    (94000045, 92000003, 93001004, 1, NOW(), 1, NOW(), 0),
    (94000046, 92000003, 93001005, 1, NOW(), 1, NOW(), 0),
    (94000047, 92000003, 93000002, 1, NOW(), 1, NOW(), 0),
    (94000048, 92000003, 93002001, 1, NOW(), 1, NOW(), 0),
    (94000049, 92000003, 93002002, 1, NOW(), 1, NOW(), 0),
    (94000050, 92000003, 93002003, 1, NOW(), 1, NOW(), 0),

    (94000061, 92000004, 93000001, 1, NOW(), 1, NOW(), 0),
    (94000062, 92000004, 93001001, 1, NOW(), 1, NOW(), 0),
    (94000063, 92000004, 93001002, 1, NOW(), 1, NOW(), 0),
    (94000064, 92000004, 93001003, 1, NOW(), 1, NOW(), 0),
    (94000065, 92000004, 93001004, 1, NOW(), 1, NOW(), 0),
    (94000066, 92000004, 93001005, 1, NOW(), 1, NOW(), 0),
    (94000067, 92000004, 93000002, 1, NOW(), 1, NOW(), 0),
    (94000068, 92000004, 93002001, 1, NOW(), 1, NOW(), 0),
    (94000069, 92000004, 93002002, 1, NOW(), 1, NOW(), 0),
    (94000070, 92000004, 93002003, 1, NOW(), 1, NOW(), 0),
    (94000071, 92000004, 93002004, 1, NOW(), 1, NOW(), 0),
    (94000072, 92000004, 93002005, 1, NOW(), 1, NOW(), 0),
    (94000073, 92000004, 93002006, 1, NOW(), 1, NOW(), 0),
    (94000074, 92000004, 93002008, 1, NOW(), 1, NOW(), 0),
    (94000075, 92000004, 93002009, 1, NOW(), 1, NOW(), 0),

    (94000091, 92000005, 93000001, 1, NOW(), 1, NOW(), 0),
    (94000092, 92000005, 93000002, 1, NOW(), 1, NOW(), 0),
    (94000093, 92000005, 93000003, 1, NOW(), 1, NOW(), 0),
    (94000094, 92000005, 93001001, 1, NOW(), 1, NOW(), 0),
    (94000095, 92000005, 93001002, 1, NOW(), 1, NOW(), 0),
    (94000096, 92000005, 93001003, 1, NOW(), 1, NOW(), 0),
    (94000097, 92000005, 93001004, 1, NOW(), 1, NOW(), 0),
    (94000098, 92000005, 93001005, 1, NOW(), 1, NOW(), 0),
    (94000099, 92000005, 93002001, 1, NOW(), 1, NOW(), 0),
    (94000100, 92000005, 93002002, 1, NOW(), 1, NOW(), 0),
    (94000101, 92000005, 93002003, 1, NOW(), 1, NOW(), 0),
    (94000102, 92000005, 93002004, 1, NOW(), 1, NOW(), 0),
    (94000103, 92000005, 93002005, 1, NOW(), 1, NOW(), 0),
    (94000104, 92000005, 93002006, 1, NOW(), 1, NOW(), 0),
    (94000105, 92000005, 93002007, 1, NOW(), 1, NOW(), 0),
    (94000106, 92000005, 93002008, 1, NOW(), 1, NOW(), 0),
    (94000107, 92000005, 93002009, 1, NOW(), 1, NOW(), 0),
    (94000108, 92000005, 93003001, 1, NOW(), 1, NOW(), 0),
    (94000109, 92000005, 93003002, 1, NOW(), 1, NOW(), 0),
    (94000110, 92000005, 93003003, 1, NOW(), 1, NOW(), 0),
    (94000111, 92000005, 93003004, 1, NOW(), 1, NOW(), 0),
    (94000112, 92000005, 93003005, 1, NOW(), 1, NOW(), 0),
    (94000113, 92000005, 93003006, 1, NOW(), 1, NOW(), 0),
    (94000114, 92000005, 93003007, 1, NOW(), 1, NOW(), 0),
    (94000115, 92000005, 93003101, 1, NOW(), 1, NOW(), 0),
    (94000116, 92000005, 93003102, 1, NOW(), 1, NOW(), 0),
    (94000117, 92000005, 93003103, 1, NOW(), 1, NOW(), 0),
    (94000118, 92000005, 93003104, 1, NOW(), 1, NOW(), 0),
    (94000119, 92000005, 93003201, 1, NOW(), 1, NOW(), 0),
    (94000120, 92000005, 93003202, 1, NOW(), 1, NOW(), 0),
    (94000121, 92000005, 93003203, 1, NOW(), 1, NOW(), 0);

INSERT INTO sys_user_role (id, user_id, role_id, create_by, create_time, update_by, update_time, deleted)
SELECT 95000001, u.id, 92000005, 1, NOW(), 1, NOW(), 0
FROM sys_user u
WHERE u.deleted = 0
  AND u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = 92000005
  );
