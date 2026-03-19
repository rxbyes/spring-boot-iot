USE rm_iot;

SET NAMES utf8mb4;

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

INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, path, component, icon, meta_json, sort, type, menu_type, status, create_by, create_time, update_by, update_time, deleted)
VALUES
    (93000001, 0, '接入智维', 'iot-access', '', 'Layout', 'connection', '{"description":"资产、链路与异常观测","menuTitle":"接入智维","menuHint":"覆盖产品定义、设备资产、链路验证、异常观测与数据校验。"}', 10, 0, 0, 1, 1, NOW(), 1, NOW(), 0),
    (93000002, 0, '风险运营', 'risk-ops', '', 'Layout', 'warning', '{"description":"态势、告警与协同闭环","menuTitle":"风险运营","menuHint":"覆盖实时监测、告警运营、事件协同、对象洞察与运营复盘。"}', 20, 0, 0, 1, 1, NOW(), 1, NOW(), 0),
    (93000004, 0, '风险策略', 'risk-config', '', 'Layout', 'operation', '{"description":"对象、阈值与联动配置","menuTitle":"风险策略","menuHint":"覆盖风险对象、阈值策略、联动编排与应急预案库。"}', 30, 0, 0, 1, 1, NOW(), 1, NOW(), 0),
    (93000003, 0, '平台治理', 'system-governance', '', 'Layout', 'setting', '{"description":"组织、权限与审计治理","menuTitle":"平台治理","menuHint":"覆盖组织、账号、角色、导航、区域、字典、通知与审计中心。"}', 40, 0, 0, 1, 1, NOW(), 1, NOW(), 0),
    (93000005, 0, '质量工场', 'quality-workbench', '', 'Layout', 'monitor', '{"description":"自动化与质量基线","menuTitle":"质量工场","menuHint":"覆盖自动化编排、回归计划与质量巡检资产。"}', 50, 0, 0, 1, 1, NOW(), 1, NOW(), 0),

    (93001001, 93000001, '产品定义中心', 'iot:products', '/products', 'ProductWorkbenchView', 'box', '{"caption":"产品台账、协议基线与库存归属","shortLabel":"产"}', 11, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001002, 93000001, '设备资产中心', 'iot:devices', '/devices', 'DeviceWorkbenchView', 'cpu', '{"caption":"设备建档、在线状态与资产运维","shortLabel":"设"}', 12, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001003, 93000001, '链路验证中心', 'iot:reporting', '/reporting', 'ReportWorkbenchView', 'promotion', '{"caption":"HTTP 上报模拟、payload 回放与联调","shortLabel":"验"}', 13, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001006, 93000001, '异常观测台', 'iot:system-log', '/system-log', 'AuditLogView', 'warning', '{"caption":"研发测试定位系统异常与接入问题","shortLabel":"观"}', 14, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001007, 93000001, '链路追踪台', 'iot:message-trace', '/message-trace', 'MessageTraceView', 'tickets', '{"caption":"按 TraceId、设备编码与 Topic 排查设备接入链路","shortLabel":"追"}', 15, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001005, 93000001, '数据校验台', 'iot:file-debug', '/file-debug', 'FilePayloadDebugView', 'document', '{"caption":"文件快照与固件聚合结果核验","shortLabel":"校"}', 16, 1, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93002008, 93000002, '实时监测台', 'risk:monitoring', '/risk-monitoring', 'RealTimeMonitoringView', 'monitor', '{"caption":"风险监测列表、筛选与详情抽屉","shortLabel":"监"}', 21, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002009, 93000002, 'GIS态势图', 'risk:monitoring-gis', '/risk-monitoring-gis', 'RiskGisView', 'map-location', '{"caption":"点位态势、未定位风险点与地图联动","shortLabel":"图"}', 22, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002001, 93000002, '告警运营台', 'risk:alarm', '/alarm-center', 'AlarmCenterView', 'bell', '{"caption":"告警列表、确认、抑制与关闭","shortLabel":"告"}', 23, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002002, 93000002, '事件协同台', 'risk:event', '/event-disposal', 'EventDisposalView', 'flag', '{"caption":"工单派发、处置反馈与事件闭环","shortLabel":"事"}', 24, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001004, 93000002, '对象洞察台', 'iot:insight', '/insight', 'DeviceInsightView', 'data-analysis', '{"caption":"设备属性、消息日志与风险研判线索","shortLabel":"洞"}', 25, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002007, 93000002, '运营分析中心', 'risk:report', '/report-analysis', 'ReportAnalysisView', 'trend-charts', '{"caption":"风险趋势、告警统计与设备健康复盘","shortLabel":"析"}', 26, 1, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93002003, 93000004, '风险对象中心', 'risk:point', '/risk-point', 'RiskPointView', 'location', '{"caption":"风险对象建档、设备绑定与等级治理","shortLabel":"险"}', 31, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002004, 93000004, '阈值策略', 'risk:rule-definition', '/rule-definition', 'RuleDefinitionView', 'set-up', '{"caption":"阈值规则维护与触发条件治理","shortLabel":"阈"}', 32, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002005, 93000004, '联动编排', 'risk:linkage-rule', '/linkage-rule', 'LinkageRuleView', 'operation', '{"caption":"触发条件与联动动作编排","shortLabel":"联"}', 33, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002006, 93000004, '应急预案库', 'risk:emergency-plan', '/emergency-plan', 'EmergencyPlanView', 'tickets', '{"caption":"预案维护、步骤编排与响应协同","shortLabel":"预"}', 34, 1, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93003001, 93000003, '组织架构', 'system:organization', '/organization', 'OrganizationView', 'office-building', '{"caption":"组织树维护与责任主体管理","shortLabel":"组"}', 41, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003002, 93000003, '账号中心', 'system:user', '/user', 'UserView', 'user', '{"caption":"账号维护、状态管理与密码重置","shortLabel":"账"}', 42, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003003, 93000003, '角色权限', 'system:role', '/role', 'RoleView', 'avatar', '{"caption":"角色维护与菜单授权管理","shortLabel":"角"}', 43, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003008, 93000003, '导航编排', 'system:menu', '/menu', 'MenuView', 'menu', '{"caption":"菜单树结构与页面权限项维护","shortLabel":"导"}', 44, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003004, 93000003, '区域版图', 'system:region', '/region', 'RegionView', 'place', '{"caption":"区域树与业务区域归属维护","shortLabel":"区"}', 45, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003005, 93000003, '数据字典', 'system:dict', '/dict', 'DictView', 'collection', '{"caption":"字典类型与字典项配置","shortLabel":"字"}', 46, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003006, 93000003, '通知编排', 'system:channel', '/channel', 'ChannelView', 'chat-dot-round', '{"caption":"通知渠道配置、启停与测试","shortLabel":"通"}', 47, 1, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003007, 93000003, '审计中心', 'system:audit', '/audit-log', 'AuditLogView', 'document-checked', '{"caption":"客户与治理侧业务操作审计","shortLabel":"审"}', 48, 1, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93003009, 93000005, '自动化工场', 'system:automation-test', '/automation-test', 'AutomationTestCenterView', 'monitor', '{"caption":"配置驱动场景编排、执行计划与报告导出","shortLabel":"测"}', 51, 1, 1, 1, 1, NOW(), 1, NOW(), 0)
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
WHERE role_id IN (@role_business_id, @role_management_id, @role_ops_id, @role_developer_id, @role_super_admin_id);

INSERT INTO sys_role_menu (id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
VALUES
    (94020001, @role_business_id, 93000002, 1, NOW(), 1, NOW(), 0),
    (94020002, @role_business_id, 93002008, 1, NOW(), 1, NOW(), 0),
    (94020003, @role_business_id, 93002009, 1, NOW(), 1, NOW(), 0),
    (94020004, @role_business_id, 93002001, 1, NOW(), 1, NOW(), 0),
    (94020005, @role_business_id, 93002002, 1, NOW(), 1, NOW(), 0),
    (94020006, @role_business_id, 93001004, 1, NOW(), 1, NOW(), 0),
    (94020007, @role_business_id, 93002007, 1, NOW(), 1, NOW(), 0),

    (94020021, @role_management_id, 93000002, 1, NOW(), 1, NOW(), 0),
    (94020022, @role_management_id, 93002008, 1, NOW(), 1, NOW(), 0),
    (94020023, @role_management_id, 93002009, 1, NOW(), 1, NOW(), 0),
    (94020024, @role_management_id, 93002001, 1, NOW(), 1, NOW(), 0),
    (94020025, @role_management_id, 93002002, 1, NOW(), 1, NOW(), 0),
    (94020026, @role_management_id, 93001004, 1, NOW(), 1, NOW(), 0),
    (94020027, @role_management_id, 93002007, 1, NOW(), 1, NOW(), 0),
    (94020028, @role_management_id, 93000004, 1, NOW(), 1, NOW(), 0),
    (94020029, @role_management_id, 93002003, 1, NOW(), 1, NOW(), 0),
    (94020030, @role_management_id, 93002004, 1, NOW(), 1, NOW(), 0),
    (94020031, @role_management_id, 93002005, 1, NOW(), 1, NOW(), 0),
    (94020032, @role_management_id, 93002006, 1, NOW(), 1, NOW(), 0),
    (94020033, @role_management_id, 93000003, 1, NOW(), 1, NOW(), 0),
    (94020034, @role_management_id, 93003001, 1, NOW(), 1, NOW(), 0),
    (94020035, @role_management_id, 93003002, 1, NOW(), 1, NOW(), 0),
    (94020036, @role_management_id, 93003003, 1, NOW(), 1, NOW(), 0),
    (94020037, @role_management_id, 93003004, 1, NOW(), 1, NOW(), 0),
    (94020038, @role_management_id, 93003005, 1, NOW(), 1, NOW(), 0),
    (94020039, @role_management_id, 93003006, 1, NOW(), 1, NOW(), 0),
    (94020040, @role_management_id, 93003007, 1, NOW(), 1, NOW(), 0),
    (94020041, @role_management_id, 93003101, 1, NOW(), 1, NOW(), 0),
    (94020042, @role_management_id, 93003102, 1, NOW(), 1, NOW(), 0),
    (94020043, @role_management_id, 93003104, 1, NOW(), 1, NOW(), 0),
    (94020044, @role_management_id, 93003201, 1, NOW(), 1, NOW(), 0),
    (94020045, @role_management_id, 93003202, 1, NOW(), 1, NOW(), 0),

    (94020061, @role_ops_id, 93000001, 1, NOW(), 1, NOW(), 0),
    (94020062, @role_ops_id, 93001001, 1, NOW(), 1, NOW(), 0),
    (94020063, @role_ops_id, 93001002, 1, NOW(), 1, NOW(), 0),
    (94020064, @role_ops_id, 93001003, 1, NOW(), 1, NOW(), 0),
    (94020065, @role_ops_id, 93001006, 1, NOW(), 1, NOW(), 0),
    (94020066, @role_ops_id, 93001007, 1, NOW(), 1, NOW(), 0),
    (94020067, @role_ops_id, 93001005, 1, NOW(), 1, NOW(), 0),
    (94020068, @role_ops_id, 93000002, 1, NOW(), 1, NOW(), 0),
    (94020069, @role_ops_id, 93002008, 1, NOW(), 1, NOW(), 0),
    (94020070, @role_ops_id, 93002001, 1, NOW(), 1, NOW(), 0),
    (94020071, @role_ops_id, 93002002, 1, NOW(), 1, NOW(), 0),
    (94020072, @role_ops_id, 93001004, 1, NOW(), 1, NOW(), 0),
    (94020073, @role_ops_id, 93000004, 1, NOW(), 1, NOW(), 0),
    (94020074, @role_ops_id, 93002003, 1, NOW(), 1, NOW(), 0),
    (94020075, @role_ops_id, 93002004, 1, NOW(), 1, NOW(), 0),

    (94020091, @role_developer_id, 93000001, 1, NOW(), 1, NOW(), 0),
    (94020092, @role_developer_id, 93001001, 1, NOW(), 1, NOW(), 0),
    (94020093, @role_developer_id, 93001002, 1, NOW(), 1, NOW(), 0),
    (94020094, @role_developer_id, 93001003, 1, NOW(), 1, NOW(), 0),
    (94020095, @role_developer_id, 93001006, 1, NOW(), 1, NOW(), 0),
    (94020096, @role_developer_id, 93001007, 1, NOW(), 1, NOW(), 0),
    (94020097, @role_developer_id, 93001005, 1, NOW(), 1, NOW(), 0),
    (94020098, @role_developer_id, 93000002, 1, NOW(), 1, NOW(), 0),
    (94020099, @role_developer_id, 93002008, 1, NOW(), 1, NOW(), 0),
    (94020100, @role_developer_id, 93002009, 1, NOW(), 1, NOW(), 0),
    (94020101, @role_developer_id, 93002001, 1, NOW(), 1, NOW(), 0),
    (94020102, @role_developer_id, 93002002, 1, NOW(), 1, NOW(), 0),
    (94020103, @role_developer_id, 93001004, 1, NOW(), 1, NOW(), 0),
    (94020104, @role_developer_id, 93000004, 1, NOW(), 1, NOW(), 0),
    (94020105, @role_developer_id, 93002003, 1, NOW(), 1, NOW(), 0),
    (94020106, @role_developer_id, 93002004, 1, NOW(), 1, NOW(), 0),
    (94020107, @role_developer_id, 93002005, 1, NOW(), 1, NOW(), 0),
    (94020108, @role_developer_id, 93002006, 1, NOW(), 1, NOW(), 0),
    (94020109, @role_developer_id, 93000005, 1, NOW(), 1, NOW(), 0),
    (94020110, @role_developer_id, 93003009, 1, NOW(), 1, NOW(), 0);

SET @role_menu_id := 94020900;
INSERT INTO sys_role_menu (id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT (@role_menu_id := @role_menu_id + 1), @role_super_admin_id, m.id, 1, NOW(), 1, NOW(), 0
FROM sys_menu m
WHERE m.deleted = 0
  AND @role_super_admin_id IS NOT NULL
ORDER BY m.sort, m.id;
