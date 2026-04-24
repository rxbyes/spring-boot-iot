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
    id, tenant_id, role_name, role_code, description, data_scope_type, status, create_by, create_time, update_by, update_time, deleted
) VALUES
    (92000001, 1, '业务人员', 'BUSINESS_STAFF', '负责风险监测、告警研判、事件处置与业务复盘。', 'SELF', 1, 1, NOW(), 1, NOW(), 0),
    (92000002, 1, '管理人员', 'MANAGEMENT_STAFF', '负责业务统筹、规则审批、系统治理与运营管理。', 'ORG_AND_CHILDREN', 1, 1, NOW(), 1, NOW(), 0),
    (92000003, 1, '运维人员', 'OPS_STAFF', '负责设备接入、联调排障、运行维护与问题闭环。', 'TENANT', 1, 1, NOW(), 1, NOW(), 0),
    (92000004, 1, '开发人员', 'DEVELOPER_STAFF', '负责协议联调、规则开发、缺陷定位与功能验证。', 'TENANT', 1, 1, NOW(), 1, NOW(), 0),
    (92000005, 1, '超级管理员', 'SUPER_ADMIN', '拥有全部菜单与操作权限。', 'ALL', 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    description = VALUES(description),
    data_scope_type = VALUES(data_scope_type),
    status = VALUES(status),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

-- 初始化演示账号默认密码：123456（BCrypt）
INSERT INTO sys_user (
    id, tenant_id, org_id, username, password, nickname, real_name, phone, email, status, is_admin,
    remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (1, 1, 7101, 'admin', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '平台总控官', '超级管理员', '13800000000', 'admin@ghlzm.com', 1, 1,
     '超级管理员演示账号，默认查看平台治理与全量菜单。', 1, NOW(), 1, NOW(), 0),
    (2, 1, 7102, 'biz_demo', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '风险运营专员', '业务演示账号', '13800000001', 'biz_demo@ghlzm.com', 1, 0,
     '业务人员演示账号，默认进入风险运营工作台。', 1, NOW(), 1, NOW(), 0),
    (3, 1, 7101, 'manager_demo', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '运营管理负责人', '管理演示账号', '13800000002', 'manager_demo@ghlzm.com', 1, 0,
     '管理人员演示账号，默认进入风险运营并覆盖风险策略、平台治理。', 1, NOW(), 1, NOW(), 0),
    (4, 1, 7101, 'ops_demo', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '接入运维工程师', '运维演示账号', '13800000003', 'ops_demo@ghlzm.com', 1, 0,
     '运维人员演示账号，默认进入接入智维工作台。', 1, NOW(), 1, NOW(), 0),
    (5, 1, 7101, 'dev_demo', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '平台开发工程师', '开发演示账号', '13800000004', 'dev_demo@ghlzm.com', 1, 0,
     '开发人员演示账号，默认进入接入智维并开放质量工场。', 1, NOW(), 1, NOW(), 0),
    (99000001, 1, 7101, 'governance_reviewer', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '治理复核专员', '治理复核专员', '13800009900', 'governance-reviewer@ghlzm.com', 1, 1,
     '系统级固定治理复核人账号，负责产品契约发布与回滚审批。', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    org_id = VALUES(org_id),
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
SET @user_governance_reviewer_id = (SELECT id FROM sys_user WHERE tenant_id = 1 AND username = 'governance_reviewer' AND deleted = 0 ORDER BY id LIMIT 1);

SET @role_business_id = (SELECT id FROM sys_role WHERE role_code = 'BUSINESS_STAFF' AND deleted = 0 ORDER BY id LIMIT 1);
SET @role_management_id = (SELECT id FROM sys_role WHERE role_code = 'MANAGEMENT_STAFF' AND deleted = 0 ORDER BY id LIMIT 1);
SET @role_ops_id = (SELECT id FROM sys_role WHERE role_code = 'OPS_STAFF' AND deleted = 0 ORDER BY id LIMIT 1);
SET @role_developer_id = (SELECT id FROM sys_role WHERE role_code = 'DEVELOPER_STAFF' AND deleted = 0 ORDER BY id LIMIT 1);
SET @role_super_admin_id = (SELECT id FROM sys_role WHERE role_code = 'SUPER_ADMIN' AND deleted = 0 ORDER BY id LIMIT 1);

DELETE FROM sys_user_role
WHERE tenant_id = 1
  AND user_id IN (@user_admin_id, @user_business_demo_id, @user_management_demo_id, @user_ops_demo_id, @user_developer_demo_id, @user_governance_reviewer_id);

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
    UNION ALL
    SELECT 6, @user_governance_reviewer_id, @role_super_admin_id
) t
WHERE t.user_id IS NOT NULL
  AND t.role_id IS NOT NULL
ORDER BY t.sort_no;

INSERT INTO sys_governance_approval_policy (
    id, tenant_id, scope_type, action_code, approver_mode, approver_user_id, enabled, remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (99001001, 0, 'GLOBAL', 'PRODUCT_CONTRACT_RELEASE_APPLY', 'FIXED_USER', 99000001, 1, '产品契约发布固定复核人', 1, NOW(), 1, NOW(), 0),
    (99001002, 0, 'GLOBAL', 'PRODUCT_CONTRACT_ROLLBACK', 'FIXED_USER', 99000001, 1, '产品契约回滚固定复核人', 1, NOW(), 1, NOW(), 0),
    (99001003, 0, 'GLOBAL', 'VENDOR_MAPPING_RULE_PUBLISH', 'FIXED_USER', 99000001, 1, '厂商字段映射规则发布固定复核人', 1, NOW(), 1, NOW(), 0),
    (99001004, 0, 'GLOBAL', 'VENDOR_MAPPING_RULE_ROLLBACK', 'FIXED_USER', 99000001, 1, '厂商字段映射规则回滚固定复核人', 1, NOW(), 1, NOW(), 0),
    (99001005, 0, 'GLOBAL', 'PROTOCOL_FAMILY_PUBLISH', 'FIXED_USER', 99000001, 1, '协议族定义发布固定复核人', 1, NOW(), 1, NOW(), 0),
    (99001006, 0, 'GLOBAL', 'PROTOCOL_FAMILY_ROLLBACK', 'FIXED_USER', 99000001, 1, '协议族定义回滚固定复核人', 1, NOW(), 1, NOW(), 0),
    (99001007, 0, 'GLOBAL', 'PROTOCOL_DECRYPT_PROFILE_PUBLISH', 'FIXED_USER', 99000001, 1, '协议解密档案发布固定复核人', 1, NOW(), 1, NOW(), 0),
    (99001008, 0, 'GLOBAL', 'PROTOCOL_DECRYPT_PROFILE_ROLLBACK', 'FIXED_USER', 99000001, 1, '协议解密档案回滚固定复核人', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    approver_mode = VALUES(approver_mode),
    approver_user_id = VALUES(approver_user_id),
    enabled = VALUES(enabled),
    remark = VALUES(remark),
    update_by = VALUES(update_by),
    update_time = NOW(),
    deleted = 0;

INSERT INTO sys_menu (
    id, tenant_id, parent_id, menu_name, menu_code, path, component, icon, meta_json, sort, type, menu_type,
    route_path, permission, sort_no, visible, status, create_by, create_time, update_by, update_time, deleted
) VALUES
    (93000001, 1, 0, '接入智维', 'iot-access', '', 'Layout', 'connection', '{"description":"资产、链路与异常观测","menuTitle":"接入智维","menuHint":"覆盖产品定义、设备资产、链路验证、异常观测与数据校验。"}', 10, 0, 0, '', 'iot-access', 10, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000002, 1, 0, '风险运营', 'risk-ops', '', 'Layout', 'warning', '{"description":"态势、告警与协同闭环","menuTitle":"风险运营","menuHint":"覆盖实时监测、告警运营、事件协同、对象洞察与运营复盘。"}', 20, 0, 0, '', 'risk-ops', 20, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000004, 1, 0, '风险策略', 'risk-config', '', 'Layout', 'operation', '{"description":"对象、阈值与联动配置","menuTitle":"风险策略","menuHint":"覆盖风险对象、阈值策略、联动编排与应急预案库。"}', 30, 0, 0, '', 'risk-config', 30, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000003, 1, 0, '平台治理', 'system-governance', '', 'Layout', 'setting', '{"description":"组织、权限与审计治理","menuTitle":"平台治理","menuHint":"覆盖组织、账号、角色、导航、区域、字典、通知、帮助与审计中心。"}', 40, 0, 0, '', 'system-governance', 40, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000005, 1, 0, '质量工场', 'quality-workbench', '', 'Layout', 'monitor', '{"description":"研发工场、执行组织与结果基线","menuTitle":"质量工场","menuHint":"覆盖研发资产编排、执行组织与结果基线治理。"}', 50, 0, 0, '', 'quality-workbench', 50, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93001008, 1, 93000001, '无代码接入台', 'iot:device-onboarding', '/device-onboarding', 'DeviceOnboardingWorkbenchView', 'guide', '{"caption":"统一查看接入案例、阻塞原因和下一步动作"}', 10, 1, 1, '/device-onboarding', 'iot:device-onboarding', 10, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001009, 1, 93000001, '协议治理工作台', 'iot:protocol-governance', '/protocol-governance', 'ProtocolGovernanceWorkbenchView', 'guide', '{"caption":"协议族、解密档案与协议模板治理"}', 11, 1, 1, '/protocol-governance', 'iot:protocol-governance', 11, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001001, 1, 93000001, '产品定义中心', 'iot:products', '/products', 'ProductWorkbenchView', 'box', '{"caption":"产品台账、协议基线与库存归属"}', 11, 1, 1, '/products', 'iot:products', 11, 1, 1, 1, NOW(), 1, NOW(), 0),
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
    (93003010, 1, 93000003, '站内消息', 'system:in-app-message', '/in-app-message', 'InAppMessageView', 'bell', '{"caption":"通知中心站内消息的分类、范围与时间窗口编排"}', 48, 1, 1, '/in-app-message', 'system:in-app-message', 48, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003011, 1, 93000003, '帮助文档', 'system:help-doc', '/help-doc', 'HelpDocView', 'document-copy', '{"caption":"帮助中心业务类、技术类和 FAQ 资料编排"}', 49, 1, 1, '/help-doc', 'system:help-doc', 49, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003024, 1, 93000003, '治理任务台', 'system:governance-task', '/governance-task', 'GovernanceTaskView', 'tickets', '{"caption":"统一查看治理待办、人工处理状态与上下文快照"}', 50, 1, 1, '/governance-task', 'system:governance-task', 50, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003025, 1, 93000003, '治理运维台', 'system:governance-ops', '/governance-ops', 'GovernanceOpsWorkbenchView', 'warning', '{"caption":"统一查看字段漂移、合同差异与风险指标缺失告警"}', 51, 1, 1, '/governance-ops', 'system:governance-ops', 51, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003022, 1, 93000003, '治理审批台', 'system:governance-approval', '/governance-approval', 'GovernanceApprovalView', 'finished', '{"caption":"统一查看治理审批主单、执行结果与状态流转"}', 52, 1, 1, '/governance-approval', 'system:governance-approval', 52, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003023, 1, 93000003, '权限与密钥治理', 'system:governance-security', '/governance-security', 'GovernanceSecurityView', 'lock', '{"caption":"统一查看治理权限矩阵与设备密钥轮换台账"}', 53, 1, 1, '/governance-security', 'system:governance-security', 53, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003007, 1, 93000003, '审计中心', 'system:audit', '/audit-log', 'AuditLogView', 'document-checked', '{"caption":"客户与治理侧业务操作审计"}', 54, 1, 1, '/audit-log', 'system:audit', 54, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93003020, 1, 93000005, '业务验收台', 'system:business-acceptance', '/business-acceptance', 'BusinessAcceptanceWorkbenchView', 'finished', '{"caption":"按交付清单运行预置业务验收包"}', 51, 1, 1, '/business-acceptance', 'system:business-acceptance', 51, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003015, 1, 93000005, '研发工场', 'system:rd-workbench', '/rd-workbench', 'RdWorkbenchLandingView', 'edit-pen', '{"caption":"研发自动化资产编排主入口"}', 52, 1, 1, '/rd-workbench', 'system:rd-workbench', 52, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003016, 1, 93000005, '页面盘点台', 'system:rd-automation-inventory', '/rd-automation-inventory', 'AutomationInventoryView', 'document', '{"caption":"页面清单、覆盖缺口与人工补录"}', 53, 1, 1, '/rd-automation-inventory', 'system:rd-automation-inventory', 53, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003017, 1, 93000005, '场景模板台', 'system:rd-automation-templates', '/rd-automation-templates', 'AutomationTemplatesView', 'files', '{"caption":"沉淀页面冒烟、表单提交与列表详情模板"}', 54, 1, 1, '/rd-automation-templates', 'system:rd-automation-templates', 54, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003018, 1, 93000005, '计划编排台', 'system:rd-automation-plans', '/rd-automation-plans', 'AutomationPlansView', 'edit', '{"caption":"维护场景顺序、步骤、断言与导入导出"}', 55, 1, 1, '/rd-automation-plans', 'system:rd-automation-plans', 55, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003019, 1, 93000005, '交付打包台', 'system:rd-automation-handoff', '/rd-automation-handoff', 'AutomationHandoffView', 'promotion', '{"caption":"整理执行建议、基线说明与验收备注"}', 56, 1, 1, '/rd-automation-handoff', 'system:rd-automation-handoff', 56, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003013, 1, 93000005, '执行中心', 'system:automation-execution', '/automation-execution', 'AutomationExecutionView', 'operation', '{"caption":"目标环境、命令预览与统一验收注册表"}', 57, 1, 1, '/automation-execution', 'system:automation-execution', 57, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003014, 1, 93000005, '结果与基线中心', 'system:automation-results', '/automation-results', 'AutomationResultsView', 'data-analysis', '{"caption":"运行结果导入、失败复盘与质量建议"}', 58, 1, 1, '/automation-results', 'system:automation-results', 58, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003012, 1, 93000005, '自动化资产中心（兼容入口）', 'system:automation-assets', '/automation-assets', 'AutomationAssetsView', 'document', '{"caption":"兼容旧入口，第一轮直接落到研发工场总览"}', 59, 1, 1, '/automation-assets', 'system:automation-assets', 59, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003009, 1, 93000005, '自动化工场（兼容入口）', 'system:automation-test', '/automation-test', 'AutomationTestCenterView', 'monitor', '{"caption":"兼容旧入口，第一轮直接落到研发工场总览"}', 60, 1, 1, '/automation-test', 'system:automation-test', 60, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001011, 1, 93001001, '新增产品', 'iot:products:add', '', '', '', '{"caption":"产品定义中心新增产品按钮权限"}', 1101, 2, 2, '', 'iot:products:add', 1101, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001012, 1, 93001001, '编辑产品', 'iot:products:update', '', '', '', '{"caption":"产品定义中心编辑产品按钮权限"}', 1102, 2, 2, '', 'iot:products:update', 1102, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001013, 1, 93001001, '删除产品', 'iot:products:delete', '', '', '', '{"caption":"产品定义中心删除产品按钮权限"}', 1103, 2, 2, '', 'iot:products:delete', 1103, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001014, 1, 93001001, '导出产品', 'iot:products:export', '', '', '', '{"caption":"产品定义中心导出产品按钮权限"}', 1104, 2, 2, '', 'iot:products:export', 1104, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001101, 1, 93001002, '新增设备', 'iot:devices:add', '', '', '', '{"caption":"设备资产中心新增设备按钮权限"}', 1201, 2, 2, '', 'iot:devices:add', 1201, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001102, 1, 93001002, '编辑设备', 'iot:devices:update', '', '', '', '{"caption":"设备资产中心编辑设备按钮权限"}', 1202, 2, 2, '', 'iot:devices:update', 1202, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001103, 1, 93001002, '删除设备', 'iot:devices:delete', '', '', '', '{"caption":"设备资产中心删除设备按钮权限"}', 1203, 2, 2, '', 'iot:devices:delete', 1203, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001104, 1, 93001002, '导出设备', 'iot:devices:export', '', '', '', '{"caption":"设备资产中心导出设备按钮权限"}', 1204, 2, 2, '', 'iot:devices:export', 1204, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001105, 1, 93001002, '批量导入设备', 'iot:devices:import', '', '', '', '{"caption":"设备资产中心批量导入设备按钮权限"}', 1205, 2, 2, '', 'iot:devices:import', 1205, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001106, 1, 93001002, '更换设备', 'iot:devices:replace', '', '', '', '{"caption":"设备资产中心更换设备按钮权限"}', 1206, 2, 2, '', 'iot:devices:replace', 1206, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93003101, 1, 93003002, '新增用户', 'system:user:add', '', '', '', '{"caption":"新增用户按钮权限"}', 3201, 2, 2, '', 'system:user:add', 3201, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003102, 1, 93003002, '编辑用户', 'system:user:update', '', '', '', '{"caption":"编辑用户按钮权限"}', 3202, 2, 2, '', 'system:user:update', 3202, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003103, 1, 93003002, '删除用户', 'system:user:delete', '', '', '', '{"caption":"删除用户按钮权限"}', 3203, 2, 2, '', 'system:user:delete', 3203, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003104, 1, 93003002, '重置密码', 'system:user:reset-password', '', '', '', '{"caption":"重置密码按钮权限"}', 3204, 2, 2, '', 'system:user:reset-password', 3204, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003201, 1, 93003003, '新增角色', 'system:role:add', '', '', '', '{"caption":"新增角色按钮权限"}', 3301, 2, 2, '', 'system:role:add', 3301, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003202, 1, 93003003, '编辑角色', 'system:role:update', '', '', '', '{"caption":"编辑角色按钮权限"}', 3302, 2, 2, '', 'system:role:update', 3302, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003203, 1, 93003003, '删除角色', 'system:role:delete', '', '', '', '{"caption":"删除角色按钮权限"}', 3303, 2, 2, '', 'system:role:delete', 3303, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003301, 1, 93003008, '新增菜单', 'system:menu:add', '', '', '', '{"caption":"新增菜单按钮权限"}', 3801, 2, 2, '', 'system:menu:add', 3801, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003302, 1, 93003008, '编辑菜单', 'system:menu:update', '', '', '', '{"caption":"编辑菜单按钮权限"}', 3802, 2, 2, '', 'system:menu:update', 3802, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003303, 1, 93003008, '删除菜单', 'system:menu:delete', '', '', '', '{"caption":"删除菜单按钮权限"}', 3803, 2, 2, '', 'system:menu:delete', 3803, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003401, 1, 93003010, '新增站内消息', 'system:in-app-message:add', '', '', '', '{"caption":"新增站内消息按钮权限"}', 3901, 2, 2, '', 'system:in-app-message:add', 3901, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003402, 1, 93003010, '编辑站内消息', 'system:in-app-message:update', '', '', '', '{"caption":"编辑站内消息按钮权限"}', 3902, 2, 2, '', 'system:in-app-message:update', 3902, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003403, 1, 93003010, '删除站内消息', 'system:in-app-message:delete', '', '', '', '{"caption":"删除站内消息按钮权限"}', 3903, 2, 2, '', 'system:in-app-message:delete', 3903, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003501, 1, 93003011, '新增帮助文档', 'system:help-doc:add', '', '', '', '{"caption":"新增帮助文档按钮权限"}', 4001, 2, 2, '', 'system:help-doc:add', 4001, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003502, 1, 93003011, '编辑帮助文档', 'system:help-doc:update', '', '', '', '{"caption":"编辑帮助文档按钮权限"}', 4002, 2, 2, '', 'system:help-doc:update', 4002, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003503, 1, 93003011, '删除帮助文档', 'system:help-doc:delete', '', '', '', '{"caption":"删除帮助文档按钮权限"}', 4003, 2, 2, '', 'system:help-doc:delete', 4003, 1, 1, 1, NOW(), 1, NOW(), 0)
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

UPDATE sys_role_menu rm
JOIN sys_menu m ON m.id = rm.menu_id
SET rm.deleted = 1,
    rm.update_by = 1,
    rm.update_time = NOW()
WHERE m.tenant_id = 1
  AND m.menu_code IN ('risk:rule-definition:write', 'risk:linkage-rule:write', 'risk:emergency-plan:write')
  AND rm.deleted = 0;

UPDATE sys_menu
SET deleted = 1,
    visible = 0,
    status = 0,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND menu_code IN ('risk:rule-definition:write', 'risk:linkage-rule:write', 'risk:emergency-plan:write')
  AND deleted = 0;

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
    (96010013, 1, @role_business_id, 93000005, 1, NOW(), 1, NOW(), 0),
    (96010014, 1, @role_business_id, 93003020, 1, NOW(), 1, NOW(), 0),
    (96010011, 1, @role_business_id, 93001001, 1, NOW(), 1, NOW(), 0),
    (96010009, 1, @role_business_id, 93001002, 1, NOW(), 1, NOW(), 0),
    (96010010, 1, @role_business_id, 93001104, 1, NOW(), 1, NOW(), 0),
    (96010012, 1, @role_business_id, 93001014, 1, NOW(), 1, NOW(), 0),

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
    (96010170, 1, @role_management_id, 93001008, 1, NOW(), 1, NOW(), 0),
    (96010052, 1, @role_management_id, 93001001, 1, NOW(), 1, NOW(), 0),
    (96010047, 1, @role_management_id, 93001002, 1, NOW(), 1, NOW(), 0),
    (96010048, 1, @role_management_id, 93001101, 1, NOW(), 1, NOW(), 0),
    (96010049, 1, @role_management_id, 93001102, 1, NOW(), 1, NOW(), 0),
    (96010050, 1, @role_management_id, 93001103, 1, NOW(), 1, NOW(), 0),
    (96010051, 1, @role_management_id, 93001104, 1, NOW(), 1, NOW(), 0),
    (96010057, 1, @role_management_id, 93001105, 1, NOW(), 1, NOW(), 0),
    (96010058, 1, @role_management_id, 93001106, 1, NOW(), 1, NOW(), 0),
    (96010053, 1, @role_management_id, 93001011, 1, NOW(), 1, NOW(), 0),
    (96010054, 1, @role_management_id, 93001012, 1, NOW(), 1, NOW(), 0),
    (96010055, 1, @role_management_id, 93001013, 1, NOW(), 1, NOW(), 0),
    (96010056, 1, @role_management_id, 93001014, 1, NOW(), 1, NOW(), 0),
    (96010131, 1, @role_management_id, 93003010, 1, NOW(), 1, NOW(), 0),
    (96010132, 1, @role_management_id, 93003011, 1, NOW(), 1, NOW(), 0),
    (96010133, 1, @role_management_id, 93003401, 1, NOW(), 1, NOW(), 0),
    (96010134, 1, @role_management_id, 93003402, 1, NOW(), 1, NOW(), 0),
    (96010135, 1, @role_management_id, 93003403, 1, NOW(), 1, NOW(), 0),
    (96010136, 1, @role_management_id, 93003501, 1, NOW(), 1, NOW(), 0),
    (96010137, 1, @role_management_id, 93003502, 1, NOW(), 1, NOW(), 0),
    (96010138, 1, @role_management_id, 93003503, 1, NOW(), 1, NOW(), 0),
    (96010145, 1, @role_management_id, 93003022, 1, NOW(), 1, NOW(), 0),
    (96010148, 1, @role_management_id, 93003023, 1, NOW(), 1, NOW(), 0),
    (96010151, 1, @role_management_id, 93003024, 1, NOW(), 1, NOW(), 0),
    (96010152, 1, @role_management_id, 93003025, 1, NOW(), 1, NOW(), 0),
    (96010142, 1, @role_management_id, 93000005, 1, NOW(), 1, NOW(), 0),
    (96010143, 1, @role_management_id, 93003020, 1, NOW(), 1, NOW(), 0),

    (96010061, 1, @role_ops_id, 93000001, 1, NOW(), 1, NOW(), 0),
    (96010171, 1, @role_ops_id, 93001008, 1, NOW(), 1, NOW(), 0),
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
    (96010084, 1, @role_ops_id, 93001105, 1, NOW(), 1, NOW(), 0),
    (96010085, 1, @role_ops_id, 93001106, 1, NOW(), 1, NOW(), 0),
    (96010080, 1, @role_ops_id, 93001011, 1, NOW(), 1, NOW(), 0),
    (96010081, 1, @role_ops_id, 93001012, 1, NOW(), 1, NOW(), 0),
    (96010082, 1, @role_ops_id, 93001013, 1, NOW(), 1, NOW(), 0),
    (96010083, 1, @role_ops_id, 93001014, 1, NOW(), 1, NOW(), 0),
    (96010146, 1, @role_ops_id, 93003022, 1, NOW(), 1, NOW(), 0),
    (96010149, 1, @role_ops_id, 93003023, 1, NOW(), 1, NOW(), 0),
    (96010153, 1, @role_ops_id, 93003024, 1, NOW(), 1, NOW(), 0),
    (96010154, 1, @role_ops_id, 93003025, 1, NOW(), 1, NOW(), 0),

    (96010091, 1, @role_developer_id, 93000001, 1, NOW(), 1, NOW(), 0),
    (96010172, 1, @role_developer_id, 93001008, 1, NOW(), 1, NOW(), 0),
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
    (96010144, 1, @role_developer_id, 93003020, 1, NOW(), 1, NOW(), 0),
    (96010136, 1, @role_developer_id, 93003015, 1, NOW(), 1, NOW(), 0),
    (96010137, 1, @role_developer_id, 93003016, 1, NOW(), 1, NOW(), 0),
    (96010138, 1, @role_developer_id, 93003017, 1, NOW(), 1, NOW(), 0),
    (96010139, 1, @role_developer_id, 93003018, 1, NOW(), 1, NOW(), 0),
    (96010140, 1, @role_developer_id, 93003019, 1, NOW(), 1, NOW(), 0),
    (96010121, 1, @role_developer_id, 93003012, 1, NOW(), 1, NOW(), 0),
    (96010122, 1, @role_developer_id, 93003013, 1, NOW(), 1, NOW(), 0),
    (96010123, 1, @role_developer_id, 93003014, 1, NOW(), 1, NOW(), 0),
    (96010110, 1, @role_developer_id, 93003009, 1, NOW(), 1, NOW(), 0),
    (96010111, 1, @role_developer_id, 93001101, 1, NOW(), 1, NOW(), 0),
    (96010112, 1, @role_developer_id, 93001102, 1, NOW(), 1, NOW(), 0),
    (96010113, 1, @role_developer_id, 93001103, 1, NOW(), 1, NOW(), 0),
    (96010114, 1, @role_developer_id, 93001104, 1, NOW(), 1, NOW(), 0),
    (96010119, 1, @role_developer_id, 93001105, 1, NOW(), 1, NOW(), 0),
    (96010120, 1, @role_developer_id, 93001106, 1, NOW(), 1, NOW(), 0),
    (96010115, 1, @role_developer_id, 93001011, 1, NOW(), 1, NOW(), 0),
    (96010116, 1, @role_developer_id, 93001012, 1, NOW(), 1, NOW(), 0),
    (96010117, 1, @role_developer_id, 93001013, 1, NOW(), 1, NOW(), 0),
    (96010118, 1, @role_developer_id, 93001014, 1, NOW(), 1, NOW(), 0),
    (96010147, 1, @role_developer_id, 93003022, 1, NOW(), 1, NOW(), 0),
    (96010150, 1, @role_developer_id, 93003023, 1, NOW(), 1, NOW(), 0),
    (96010155, 1, @role_developer_id, 93003024, 1, NOW(), 1, NOW(), 0),
    (96010156, 1, @role_developer_id, 93003025, 1, NOW(), 1, NOW(), 0);

SET @role_menu_id := 96010900;
INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT (@role_menu_id := @role_menu_id + 1), 1, @role_super_admin_id, m.id, 1, NOW(), 1, NOW(), 0
FROM sys_menu m
WHERE m.deleted = 0
  AND @role_super_admin_id IS NOT NULL
ORDER BY m.sort, m.id;

-- governance fine-grained permission seeds
INSERT INTO sys_menu (
    id, tenant_id, parent_id, menu_name, menu_code, path, component, icon, meta_json, sort, type, menu_type,
    route_path, permission, sort_no, visible, status, create_by, create_time, update_by, update_time, deleted
) VALUES
    (93001021, 1, 93001001, '规范库维护', 'iot:normative-library:write', '', '', '', '{"caption":"维护规范字段库与契约基础语义"}', 1121, 2, 2, '', 'iot:normative-library:write', 1121, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001022, 1, 93001001, '契约治理', 'iot:product-contract:govern', '', '', '', '{"caption":"执行 compare 与治理决策"}', 1122, 2, 2, '', 'iot:product-contract:govern', 1122, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001023, 1, 93001001, '契约发布', 'iot:product-contract:release', '', '', '', '{"caption":"发布正式契约并生成发布批次"}', 1123, 2, 2, '', 'iot:product-contract:release', 1123, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001024, 1, 93001001, '契约回滚', 'iot:product-contract:rollback', '', '', '', '{"caption":"回滚最新契约发布批次"}', 1124, 2, 2, '', 'iot:product-contract:rollback', 1124, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001025, 1, 93001001, '契约复核', 'iot:product-contract:approve', '', '', '', '{"caption":"关键发布/回滚动作双人复核"}', 1125, 2, 2, '', 'iot:product-contract:approve', 1125, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001026, 1, 93003023, '密钥托管查看', 'iot:secret-custody:view', '', '', '', '{"caption":"查看密钥托管与轮换记录"}', 1126, 2, 2, '', 'iot:secret-custody:view', 1126, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001027, 1, 93003023, '密钥轮换执行', 'iot:secret-custody:rotate', '', '', '', '{"caption":"执行设备密钥轮换"}', 1127, 2, 2, '', 'iot:secret-custody:rotate', 1127, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001028, 1, 93003023, '密钥轮换复核', 'iot:secret-custody:approve', '', '', '', '{"caption":"关键密钥轮换动作双人复核"}', 1128, 2, 2, '', 'iot:secret-custody:approve', 1128, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001029, 1, 93001001, '规范库复核', 'iot:normative-library:approve', '', '', '', '{"caption":"规范库关键写动作复核授权"}', 1129, 2, 2, '', 'iot:normative-library:approve', 1129, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001030, 1, 93001009, '协议治理执行', 'iot:protocol-governance:edit', '', '', '', '{"caption":"维护协议族定义与解密档案草稿"}', 1130, 2, 2, '', 'iot:protocol-governance:edit', 1130, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001031, 1, 93001009, '协议治理复核', 'iot:protocol-governance:approve', '', '', '', '{"caption":"协议治理发布与回滚双人复核"}', 1131, 2, 2, '', 'iot:protocol-governance:approve', 1131, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002044, 1, 93002003, '风险指标标注', 'risk:metric-catalog:tag', '', '', '', '{"caption":"维护风险指标语义标签"}', 3144, 2, 2, '', 'risk:metric-catalog:tag', 3144, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002051, 1, 93002003, '风险指标复核', 'risk:metric-catalog:approve', '', '', '', '{"caption":"风险指标标注关键写动作双人复核"}', 3145, 2, 2, '', 'risk:metric-catalog:approve', 3145, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002045, 1, 93002004, '阈值策略执行', 'risk:rule-definition:edit', '', '', '', '{"caption":"新增/更新/删除阈值策略"}', 3245, 2, 2, '', 'risk:rule-definition:edit', 3245, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002046, 1, 93002004, '阈值策略复核', 'risk:rule-definition:approve', '', '', '', '{"caption":"阈值策略关键写操作双人复核"}', 3246, 2, 2, '', 'risk:rule-definition:approve', 3246, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002047, 1, 93002005, '联动编排执行', 'risk:linkage-rule:edit', '', '', '', '{"caption":"新增/更新/删除联动规则"}', 3347, 2, 2, '', 'risk:linkage-rule:edit', 3347, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002048, 1, 93002005, '联动编排复核', 'risk:linkage-rule:approve', '', '', '', '{"caption":"联动规则关键写操作双人复核"}', 3348, 2, 2, '', 'risk:linkage-rule:approve', 3348, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002049, 1, 93002006, '预案执行', 'risk:emergency-plan:edit', '', '', '', '{"caption":"新增/更新/删除应急预案"}', 3449, 2, 2, '', 'risk:emergency-plan:edit', 3449, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002050, 1, 93002006, '预案复核', 'risk:emergency-plan:approve', '', '', '', '{"caption":"预案关键写操作双人复核"}', 3450, 2, 2, '', 'risk:emergency-plan:approve', 3450, 1, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    menu_name = VALUES(menu_name),
    menu_code = VALUES(menu_code),
    permission = VALUES(permission),
    meta_json = VALUES(meta_json),
    sort = VALUES(sort),
    sort_no = VALUES(sort_no),
    visible = VALUES(visible),
    status = VALUES(status),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    deleted = 0;

SET @extra_role_menu_id := 96010950;
INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT (@extra_role_menu_id := @extra_role_menu_id + 1), 1, role_scope.role_id, role_scope.menu_id, 1, NOW(), 1, NOW(), 0
FROM (
    SELECT @role_management_id AS role_id, 93001021 AS menu_id
    UNION ALL SELECT @role_management_id, 93001029
    UNION ALL SELECT @role_management_id, 93001022
    UNION ALL SELECT @role_management_id, 93001023
    UNION ALL SELECT @role_management_id, 93001024
    UNION ALL SELECT @role_management_id, 93001025
    UNION ALL SELECT @role_management_id, 93001026
    UNION ALL SELECT @role_management_id, 93001027
    UNION ALL SELECT @role_management_id, 93001028
    UNION ALL SELECT @role_management_id, 93001030
    UNION ALL SELECT @role_management_id, 93001031
    UNION ALL SELECT @role_management_id, 93002044
    UNION ALL SELECT @role_management_id, 93002051
    UNION ALL SELECT @role_management_id, 93002045
    UNION ALL SELECT @role_management_id, 93002046
    UNION ALL SELECT @role_management_id, 93002047
    UNION ALL SELECT @role_management_id, 93002048
    UNION ALL SELECT @role_management_id, 93002049
    UNION ALL SELECT @role_management_id, 93002050
    UNION ALL SELECT @role_ops_id, 93001022
    UNION ALL SELECT @role_ops_id, 93001023
    UNION ALL SELECT @role_ops_id, 93001024
    UNION ALL SELECT @role_ops_id, 93001025
    UNION ALL SELECT @role_ops_id, 93001026
    UNION ALL SELECT @role_ops_id, 93001027
    UNION ALL SELECT @role_ops_id, 93001028
    UNION ALL SELECT @role_ops_id, 93001030
    UNION ALL SELECT @role_ops_id, 93001031
    UNION ALL SELECT @role_ops_id, 93002044
    UNION ALL SELECT @role_ops_id, 93002051
    UNION ALL SELECT @role_ops_id, 93002045
    UNION ALL SELECT @role_ops_id, 93002046
    UNION ALL SELECT @role_ops_id, 93002047
    UNION ALL SELECT @role_ops_id, 93002048
    UNION ALL SELECT @role_ops_id, 93002049
    UNION ALL SELECT @role_ops_id, 93002050
    UNION ALL SELECT @role_developer_id, 93001021
    UNION ALL SELECT @role_developer_id, 93001022
    UNION ALL SELECT @role_developer_id, 93001023
    UNION ALL SELECT @role_developer_id, 93001024
    UNION ALL SELECT @role_developer_id, 93001026
    UNION ALL SELECT @role_developer_id, 93001027
    UNION ALL SELECT @role_developer_id, 93001030
    UNION ALL SELECT @role_developer_id, 93002044
    UNION ALL SELECT @role_developer_id, 93002045
    UNION ALL SELECT @role_developer_id, 93002047
    UNION ALL SELECT @role_developer_id, 93002049
) role_scope
WHERE role_scope.role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.tenant_id = 1
      AND rm.role_id = role_scope.role_id
      AND rm.menu_id = role_scope.menu_id
      AND rm.deleted = 0
  );

INSERT INTO sys_menu (
    id, tenant_id, parent_id, menu_name, menu_code, path, component, icon, meta_json, sort, type, menu_type,
    route_path, permission, sort_no, visible, status, create_by, create_time, update_by, update_time, deleted
) VALUES
    (93001032, 1, 93001009, '协议族草稿', 'iot:protocol-governance:family-draft', '', '', '', '{"caption":"维护协议族定义草稿"}', 1132, 2, 2, '', 'iot:protocol-governance:family-draft', 1132, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001033, 1, 93001009, '协议族发布', 'iot:protocol-governance:family-publish', '', '', '', '{"caption":"提交协议族发布审批"}', 1133, 2, 2, '', 'iot:protocol-governance:family-publish', 1133, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001034, 1, 93001009, '协议族回滚', 'iot:protocol-governance:family-rollback', '', '', '', '{"caption":"提交协议族回滚审批"}', 1134, 2, 2, '', 'iot:protocol-governance:family-rollback', 1134, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001035, 1, 93001009, '解密档案草稿', 'iot:protocol-governance:decrypt-draft', '', '', '', '{"caption":"维护解密档案草稿"}', 1135, 2, 2, '', 'iot:protocol-governance:decrypt-draft', 1135, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001036, 1, 93001009, '解密命中试算', 'iot:protocol-governance:decrypt-preview', '', '', '', '{"caption":"查看解密命中试算结果"}', 1136, 2, 2, '', 'iot:protocol-governance:decrypt-preview', 1136, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001037, 1, 93001009, '解密回放', 'iot:protocol-governance:decrypt-replay', '', '', '', '{"caption":"执行解密链路回放"}', 1137, 2, 2, '', 'iot:protocol-governance:decrypt-replay', 1137, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001038, 1, 93001009, '协议模板草稿', 'iot:protocol-governance:template-draft', '', '', '', '{"caption":"维护协议模板草稿"}', 1138, 2, 2, '', 'iot:protocol-governance:template-draft', 1138, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001039, 1, 93001009, '模板回放', 'iot:protocol-governance:template-replay', '', '', '', '{"caption":"执行协议模板回放"}', 1139, 2, 2, '', 'iot:protocol-governance:template-replay', 1139, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001040, 1, 93001009, '模板发布', 'iot:protocol-governance:template-publish', '', '', '', '{"caption":"发布协议模板快照"}', 1140, 2, 2, '', 'iot:protocol-governance:template-publish', 1140, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001041, 1, 93001001, '总览工作区', 'iot:products:workbench-overview', '', '', '', '{"caption":"切换产品总览工作区"}', 1141, 2, 2, '', 'iot:products:workbench-overview', 1141, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001042, 1, 93001001, '契约字段工作区', 'iot:products:workbench-models', '', '', '', '{"caption":"切换契约字段工作区"}', 1142, 2, 2, '', 'iot:products:workbench-models', 1142, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001043, 1, 93001001, '关联设备工作区', 'iot:products:workbench-devices', '', '', '', '{"caption":"切换关联设备工作区"}', 1143, 2, 2, '', 'iot:products:workbench-devices', 1143, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001044, 1, 93001001, '产品编辑工作区', 'iot:products:workbench-edit', '', '', '', '{"caption":"切换产品编辑工作区"}', 1144, 2, 2, '', 'iot:products:workbench-edit', 1144, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001045, 1, 93001001, '契约版本台账', 'iot:product-contract:ledger', '', '', '', '{"caption":"查看正式合同发布台账"}', 1145, 2, 2, '', 'iot:product-contract:ledger', 1145, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001046, 1, 93001001, '批次差异', 'iot:product-contract:diff', '', '', '', '{"caption":"查看合同批次差异"}', 1146, 2, 2, '', 'iot:product-contract:diff', 1146, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001047, 1, 93001001, '映射规则建议', 'iot:vendor-mapping-rule:suggestion', '', '', '', '{"caption":"查看厂商字段映射规则建议"}', 1147, 2, 2, '', 'iot:vendor-mapping-rule:suggestion', 1147, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001048, 1, 93001001, '映射规则台账', 'iot:vendor-mapping-rule:ledger', '', '', '', '{"caption":"查看厂商字段映射规则台账"}', 1148, 2, 2, '', 'iot:vendor-mapping-rule:ledger', 1148, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001049, 1, 93001001, '映射命中预览', 'iot:vendor-mapping-rule:preview', '', '', '', '{"caption":"预览映射规则命中结果"}', 1149, 2, 2, '', 'iot:vendor-mapping-rule:preview', 1149, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001050, 1, 93001001, '映射回放', 'iot:vendor-mapping-rule:replay', '', '', '', '{"caption":"回放映射规则治理证据"}', 1150, 2, 2, '', 'iot:vendor-mapping-rule:replay', 1150, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001051, 1, 93001002, '设备详情', 'iot:devices:detail', '', '', '', '{"caption":"打开设备详情抽屉"}', 1251, 2, 2, '', 'iot:devices:detail', 1251, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001052, 1, 93001002, '导出列设置', 'iot:devices:export-config', '', '', '', '{"caption":"维护设备导出列设置"}', 1252, 2, 2, '', 'iot:devices:export-config', 1252, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001053, 1, 93001002, '导出选中', 'iot:devices:export-selected', '', '', '', '{"caption":"导出选中的设备结果"}', 1253, 2, 2, '', 'iot:devices:export-selected', 1253, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001054, 1, 93001002, '导出当前结果', 'iot:devices:export-current', '', '', '', '{"caption":"导出当前筛选结果"}', 1254, 2, 2, '', 'iot:devices:export-current', 1254, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001055, 1, 93001002, '对象洞察跳转', 'iot:devices:insight', '', '', '', '{"caption":"从设备资产中心跳转对象洞察"}', 1255, 2, 2, '', 'iot:devices:insight', 1255, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001056, 1, 93001003, '结果复盘工作区', 'iot:reporting:replay-workspace', '', '', '', '{"caption":"切换结果复盘工作区"}', 1356, 2, 2, '', 'iot:reporting:replay-workspace', 1356, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001057, 1, 93001003, '模拟上报工作区', 'iot:reporting:simulate-workspace', '', '', '', '{"caption":"切换模拟上报工作区"}', 1357, 2, 2, '', 'iot:reporting:simulate-workspace', 1357, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001058, 1, 93001003, '最近会话工作区', 'iot:reporting:recent-workspace', '', '', '', '{"caption":"切换最近会话工作区"}', 1358, 2, 2, '', 'iot:reporting:recent-workspace', 1358, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001059, 1, 93001003, '继续链路追踪', 'iot:reporting:jump-message-trace', '', '', '', '{"caption":"从链路验证跳转到链路追踪"}', 1359, 2, 2, '', 'iot:reporting:jump-message-trace', 1359, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001060, 1, 93001003, '复制实际Payload', 'iot:reporting:copy-actual-payload', '', '', '', '{"caption":"复制实际 payload"}', 1360, 2, 2, '', 'iot:reporting:copy-actual-payload', 1360, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001061, 1, 93001003, '复制响应', 'iot:reporting:copy-response', '', '', '', '{"caption":"复制模拟响应"}', 1361, 2, 2, '', 'iot:reporting:copy-response', 1361, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001062, 1, 93001007, '链路详情', 'iot:message-trace:detail', '', '', '', '{"caption":"打开链路详情工作台"}', 1562, 2, 2, '', 'iot:message-trace:detail', 1562, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001063, 1, 93001007, '处理时间线', 'iot:message-trace:timeline', '', '', '', '{"caption":"查看处理时间线"}', 1563, 2, 2, '', 'iot:message-trace:timeline', 1563, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001064, 1, 93001007, 'Payload 对比', 'iot:message-trace:payload-comparison', '', '', '', '{"caption":"查看 payload 对比"}', 1564, 2, 2, '', 'iot:message-trace:payload-comparison', 1564, 1, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    menu_name = VALUES(menu_name),
    menu_code = VALUES(menu_code),
    path = VALUES(path),
    component = VALUES(component),
    icon = VALUES(icon),
    permission = VALUES(permission),
    meta_json = VALUES(meta_json),
    sort = VALUES(sort),
    type = VALUES(type),
    menu_type = VALUES(menu_type),
    route_path = VALUES(route_path),
    sort_no = VALUES(sort_no),
    visible = VALUES(visible),
    status = VALUES(status),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    deleted = 0;

INSERT INTO sys_menu (
    id, tenant_id, parent_id, menu_name, menu_code, path, component, icon, meta_json, sort, type, menu_type,
    route_path, permission, sort_no, visible, status, create_by, create_time, update_by, update_time, deleted
) VALUES
    (93002031, 1, 93002003, '新增风险点', 'risk:point:add', '', '', '', '{"caption":"新增风险点"}', 3131, 2, 2, '', 'risk:point:add', 3131, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002032, 1, 93002003, '编辑风险点', 'risk:point:update', '', '', '', '{"caption":"编辑风险点"}', 3132, 2, 2, '', 'risk:point:update', 3132, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002033, 1, 93002003, '删除风险点', 'risk:point:delete', '', '', '', '{"caption":"删除风险点"}', 3133, 2, 2, '', 'risk:point:delete', 3133, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002034, 1, 93002003, '绑定设备', 'risk:point:bind-device', '', '', '', '{"caption":"为风险点绑定设备"}', 3134, 2, 2, '', 'risk:point:bind-device', 3134, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002035, 1, 93002003, '维护绑定', 'risk:point:binding-maintain', '', '', '', '{"caption":"维护正式风险绑定"}', 3135, 2, 2, '', 'risk:point:binding-maintain', 3135, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002036, 1, 93002003, '待治理转正', 'risk:point:pending-promote', '', '', '', '{"caption":"处理待治理绑定转正"}', 3136, 2, 2, '', 'risk:point:pending-promote', 3136, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002037, 1, 93002003, '风险点详情', 'risk:point:detail', '', '', '', '{"caption":"查看风险点详情"}', 3137, 2, 2, '', 'risk:point:detail', 3137, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002038, 1, 93002003, '治理历史', 'risk:point:history', '', '', '', '{"caption":"查看风险绑定治理历史"}', 3138, 2, 2, '', 'risk:point:history', 3138, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002039, 1, 93002001, '告警详情', 'risk:alarm:detail', '', '', '', '{"caption":"查看告警详情"}', 2339, 2, 2, '', 'risk:alarm:detail', 2339, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002040, 1, 93002001, '告警确认', 'risk:alarm:confirm', '', '', '', '{"caption":"确认告警"}', 2340, 2, 2, '', 'risk:alarm:confirm', 2340, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002041, 1, 93002001, '告警抑制', 'risk:alarm:suppress', '', '', '', '{"caption":"抑制告警"}', 2341, 2, 2, '', 'risk:alarm:suppress', 2341, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002042, 1, 93002001, '告警关闭', 'risk:alarm:close', '', '', '', '{"caption":"关闭告警"}', 2342, 2, 2, '', 'risk:alarm:close', 2342, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002052, 1, 93002002, '事件详情', 'risk:event:detail', '', '', '', '{"caption":"查看事件详情"}', 2452, 2, 2, '', 'risk:event:detail', 2452, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002053, 1, 93002002, '事件派发', 'risk:event:dispatch', '', '', '', '{"caption":"派发事件工单"}', 2453, 2, 2, '', 'risk:event:dispatch', 2453, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002054, 1, 93002002, '事件关闭', 'risk:event:close', '', '', '', '{"caption":"关闭事件"}', 2454, 2, 2, '', 'risk:event:close', 2454, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003601, 1, 93003001, '新增组织', 'system:organization:add', '', '', '', '{"caption":"新增组织节点"}', 4101, 2, 2, '', 'system:organization:add', 4101, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003602, 1, 93003001, '编辑组织', 'system:organization:update', '', '', '', '{"caption":"编辑组织节点"}', 4102, 2, 2, '', 'system:organization:update', 4102, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003603, 1, 93003001, '删除组织', 'system:organization:delete', '', '', '', '{"caption":"删除组织节点"}', 4103, 2, 2, '', 'system:organization:delete', 4103, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003604, 1, 93003001, '新增下级组织', 'system:organization:add-child', '', '', '', '{"caption":"新增下级组织节点"}', 4104, 2, 2, '', 'system:organization:add-child', 4104, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003605, 1, 93003001, '导出组织', 'system:organization:export', '', '', '', '{"caption":"导出组织树结果"}', 4105, 2, 2, '', 'system:organization:export', 4105, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003606, 1, 93003004, '新增区域', 'system:region:add', '', '', '', '{"caption":"新增区域节点"}', 4506, 2, 2, '', 'system:region:add', 4506, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003607, 1, 93003004, '编辑区域', 'system:region:update', '', '', '', '{"caption":"编辑区域节点"}', 4507, 2, 2, '', 'system:region:update', 4507, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003608, 1, 93003004, '删除区域', 'system:region:delete', '', '', '', '{"caption":"删除区域节点"}', 4508, 2, 2, '', 'system:region:delete', 4508, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003609, 1, 93003004, '新增下级区域', 'system:region:add-child', '', '', '', '{"caption":"新增下级区域节点"}', 4509, 2, 2, '', 'system:region:add-child', 4509, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003610, 1, 93003004, '导出区域', 'system:region:export', '', '', '', '{"caption":"导出区域树结果"}', 4510, 2, 2, '', 'system:region:export', 4510, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003611, 1, 93003005, '新增字典', 'system:dict:add', '', '', '', '{"caption":"新增字典类型"}', 4611, 2, 2, '', 'system:dict:add', 4611, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003612, 1, 93003005, '编辑字典', 'system:dict:update', '', '', '', '{"caption":"编辑字典类型"}', 4612, 2, 2, '', 'system:dict:update', 4612, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003613, 1, 93003005, '删除字典', 'system:dict:delete', '', '', '', '{"caption":"删除字典类型"}', 4613, 2, 2, '', 'system:dict:delete', 4613, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003614, 1, 93003005, '导出字典', 'system:dict:export', '', '', '', '{"caption":"导出字典类型结果"}', 4614, 2, 2, '', 'system:dict:export', 4614, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003615, 1, 93003005, '新增字典项', 'system:dict-item:add', '', '', '', '{"caption":"新增字典项"}', 4615, 2, 2, '', 'system:dict-item:add', 4615, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003616, 1, 93003005, '编辑字典项', 'system:dict-item:update', '', '', '', '{"caption":"编辑字典项"}', 4616, 2, 2, '', 'system:dict-item:update', 4616, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003617, 1, 93003005, '删除字典项', 'system:dict-item:delete', '', '', '', '{"caption":"删除字典项"}', 4617, 2, 2, '', 'system:dict-item:delete', 4617, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003618, 1, 93003005, '导出字典项', 'system:dict-item:export', '', '', '', '{"caption":"导出字典项结果"}', 4618, 2, 2, '', 'system:dict-item:export', 4618, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003619, 1, 93003006, '新增渠道', 'system:channel:add', '', '', '', '{"caption":"新增通知渠道"}', 4719, 2, 2, '', 'system:channel:add', 4719, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003620, 1, 93003006, '编辑渠道', 'system:channel:update', '', '', '', '{"caption":"编辑通知渠道"}', 4720, 2, 2, '', 'system:channel:update', 4720, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003621, 1, 93003006, '删除渠道', 'system:channel:delete', '', '', '', '{"caption":"删除通知渠道"}', 4721, 2, 2, '', 'system:channel:delete', 4721, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003622, 1, 93003006, '渠道测试', 'system:channel:test', '', '', '', '{"caption":"执行通知渠道测试"}', 4722, 2, 2, '', 'system:channel:test', 4722, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003623, 1, 93003006, '导出渠道', 'system:channel:export', '', '', '', '{"caption":"导出通知渠道结果"}', 4723, 2, 2, '', 'system:channel:export', 4723, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003624, 1, 93003010, '消息详情', 'system:in-app-message:detail', '', '', '', '{"caption":"查看站内消息详情"}', 4824, 2, 2, '', 'system:in-app-message:detail', 4824, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003625, 1, 93003010, '桥接统计', 'system:in-app-message:bridge-stats', '', '', '', '{"caption":"查看未读桥接统计"}', 4825, 2, 2, '', 'system:in-app-message:bridge-stats', 4825, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003626, 1, 93003010, '桥接结果', 'system:in-app-message:bridge-results', '', '', '', '{"caption":"查看未读桥接结果"}', 4826, 2, 2, '', 'system:in-app-message:bridge-results', 4826, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003627, 1, 93003010, '桥接详情', 'system:in-app-message:bridge-detail', '', '', '', '{"caption":"查看未读桥接详情"}', 4827, 2, 2, '', 'system:in-app-message:bridge-detail', 4827, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003628, 1, 93003024, '决策说明', 'system:governance-task:decision-context', '', '', '', '{"caption":"查看任务排序与决策说明"}', 5028, 2, 2, '', 'system:governance-task:decision-context', 5028, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003629, 1, 93003024, '去处理', 'system:governance-task:dispatch', '', '', '', '{"caption":"从控制面派发到领域工作台"}', 5029, 2, 2, '', 'system:governance-task:dispatch', 5029, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003630, 1, 93003024, '治理复盘', 'system:governance-task:replay', '', '', '', '{"caption":"打开治理链路复盘"}', 5030, 2, 2, '', 'system:governance-task:replay', 5030, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003631, 1, 93003024, '确认任务', 'system:governance-task:ack', '', '', '', '{"caption":"确认治理任务"}', 5031, 2, 2, '', 'system:governance-task:ack', 5031, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003632, 1, 93003024, '阻塞任务', 'system:governance-task:block', '', '', '', '{"caption":"标记治理任务阻塞"}', 5032, 2, 2, '', 'system:governance-task:block', 5032, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003633, 1, 93003024, '关闭任务', 'system:governance-task:close', '', '', '', '{"caption":"关闭治理任务"}', 5033, 2, 2, '', 'system:governance-task:close', 5033, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003634, 1, 93003024, '提交复盘结论', 'system:governance-task:replay-feedback', '', '', '', '{"caption":"提交治理复盘结论"}', 5034, 2, 2, '', 'system:governance-task:replay-feedback', 5034, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003635, 1, 93003025, '告警复盘', 'system:governance-ops:replay', '', '', '', '{"caption":"打开治理运维复盘"}', 5135, 2, 2, '', 'system:governance-ops:replay', 5135, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003636, 1, 93003025, '确认告警', 'system:governance-ops:ack', '', '', '', '{"caption":"确认治理运维告警"}', 5136, 2, 2, '', 'system:governance-ops:ack', 5136, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003637, 1, 93003025, '抑制告警', 'system:governance-ops:suppress', '', '', '', '{"caption":"抑制治理运维告警"}', 5137, 2, 2, '', 'system:governance-ops:suppress', 5137, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003638, 1, 93003025, '关闭告警', 'system:governance-ops:close', '', '', '', '{"caption":"关闭治理运维告警"}', 5138, 2, 2, '', 'system:governance-ops:close', 5138, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003639, 1, 93003025, '提交复盘结论', 'system:governance-ops:replay-feedback', '', '', '', '{"caption":"提交治理运维复盘结论"}', 5139, 2, 2, '', 'system:governance-ops:replay-feedback', 5139, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003640, 1, 93003022, '审批详情', 'system:governance-approval:detail', '', '', '', '{"caption":"查看审批详情"}', 5240, 2, 2, '', 'system:governance-approval:detail', 5240, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003641, 1, 93003022, '审批通过', 'system:governance-approval:approve', '', '', '', '{"caption":"审批通过治理主单"}', 5241, 2, 2, '', 'system:governance-approval:approve', 5241, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003642, 1, 93003022, '审批驳回', 'system:governance-approval:reject', '', '', '', '{"caption":"审批驳回治理主单"}', 5242, 2, 2, '', 'system:governance-approval:reject', 5242, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003643, 1, 93003022, '撤销审批', 'system:governance-approval:cancel', '', '', '', '{"caption":"撤销治理审批主单"}', 5243, 2, 2, '', 'system:governance-approval:cancel', 5243, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003644, 1, 93003022, '原单重提', 'system:governance-approval:resubmit', '', '', '', '{"caption":"按原单重新提交审批"}', 5244, 2, 2, '', 'system:governance-approval:resubmit', 5244, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003645, 1, 93003022, '审批预演', 'system:governance-approval:simulation', '', '', '', '{"caption":"查看审批预演结果"}', 5245, 2, 2, '', 'system:governance-approval:simulation', 5245, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003646, 1, 93003022, '影响分析', 'system:governance-approval:impact', '', '', '', '{"caption":"查看审批影响分析"}', 5246, 2, 2, '', 'system:governance-approval:impact', 5246, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003701, 1, 93003020, '发起验收', 'system:business-acceptance:launch', '', '', '', '{"caption":"发起业务验收"}', 5701, 2, 2, '', 'system:business-acceptance:launch', 5701, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003702, 1, 93003020, '打开结果', 'system:business-acceptance:open-result', '', '', '', '{"caption":"打开最近一次验收结果"}', 5702, 2, 2, '', 'system:business-acceptance:open-result', 5702, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003703, 1, 93003016, '刷新盘点', 'system:rd-automation-inventory:refresh', '', '', '', '{"caption":"刷新页面盘点结果"}', 5303, 2, 2, '', 'system:rd-automation-inventory:refresh', 5303, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003704, 1, 93003016, '勾选未覆盖', 'system:rd-automation-inventory:select-uncovered', '', '', '', '{"caption":"勾选未覆盖页面"}', 5304, 2, 2, '', 'system:rd-automation-inventory:select-uncovered', 5304, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003705, 1, 93003016, '生成脚手架', 'system:rd-automation-inventory:generate-scaffold', '', '', '', '{"caption":"一键生成自动化脚手架"}', 5305, 2, 2, '', 'system:rd-automation-inventory:generate-scaffold', 5305, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003706, 1, 93003017, '新增页面冒烟模板', 'system:rd-automation-templates:add-page-smoke', '', '', '', '{"caption":"新增页面冒烟模板"}', 5406, 2, 2, '', 'system:rd-automation-templates:add-page-smoke', 5406, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003707, 1, 93003017, '新增表单提交模板', 'system:rd-automation-templates:add-form-submit', '', '', '', '{"caption":"新增表单提交模板"}', 5407, 2, 2, '', 'system:rd-automation-templates:add-form-submit', 5407, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003708, 1, 93003017, '新增列表详情模板', 'system:rd-automation-templates:add-list-detail', '', '', '', '{"caption":"新增列表详情模板"}', 5408, 2, 2, '', 'system:rd-automation-templates:add-list-detail', 5408, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003709, 1, 93003018, '导入计划', 'system:rd-automation-plans:import', '', '', '', '{"caption":"导入自动化计划"}', 5509, 2, 2, '', 'system:rd-automation-plans:import', 5509, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003710, 1, 93003018, '导出计划', 'system:rd-automation-plans:export', '', '', '', '{"caption":"导出自动化计划 JSON"}', 5510, 2, 2, '', 'system:rd-automation-plans:export', 5510, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003711, 1, 93003018, '重置计划', 'system:rd-automation-plans:reset', '', '', '', '{"caption":"恢复默认自动化计划"}', 5511, 2, 2, '', 'system:rd-automation-plans:reset', 5511, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003712, 1, 93003019, '复制命令', 'system:rd-automation-handoff:copy-command', '', '', '', '{"caption":"复制交付命令"}', 5612, 2, 2, '', 'system:rd-automation-handoff:copy-command', 5612, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003713, 1, 93003019, '导出计划文件', 'system:rd-automation-handoff:export-plan', '', '', '', '{"caption":"导出交付计划"}', 5613, 2, 2, '', 'system:rd-automation-handoff:export-plan', 5613, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003714, 1, 93003013, '复制执行命令', 'system:automation-execution:copy-command', '', '', '', '{"caption":"复制执行中心命令"}', 5714, 2, 2, '', 'system:automation-execution:copy-command', 5714, 1, 1, 1, NOW(), 1, NOW(), 0)
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
    deleted = 0;

SET @menu_permission_role_menu_id := 96011000;
INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT (@menu_permission_role_menu_id := @menu_permission_role_menu_id + 1), 1, parent_rm.role_id, child.id, 1, NOW(), 1, NOW(), 0
FROM sys_menu child
JOIN sys_menu parent
  ON parent.id = child.parent_id
 AND parent.tenant_id = 1
 AND parent.deleted = 0
JOIN sys_role_menu parent_rm
  ON parent_rm.menu_id = parent.id
 AND parent_rm.tenant_id = 1
 AND parent_rm.deleted = 0
WHERE child.tenant_id = 1
  AND child.deleted = 0
  AND child.id = 93001009
  AND parent_rm.role_id IN (@role_business_id, @role_management_id, @role_ops_id, @role_developer_id)
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu existed
      WHERE existed.tenant_id = 1
        AND existed.role_id = parent_rm.role_id
        AND existed.menu_id = child.id
        AND existed.deleted = 0
  );

INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
SELECT (@menu_permission_role_menu_id := @menu_permission_role_menu_id + 1), 1, parent_rm.role_id, child.id, 1, NOW(), 1, NOW(), 0
FROM sys_menu child
JOIN sys_menu parent
  ON parent.id = child.parent_id
 AND parent.tenant_id = 1
 AND parent.deleted = 0
JOIN sys_role_menu parent_rm
  ON parent_rm.menu_id = parent.id
 AND parent_rm.tenant_id = 1
 AND parent_rm.deleted = 0
WHERE child.tenant_id = 1
  AND child.deleted = 0
  AND child.id IN (
      93001032, 93001033, 93001034, 93001035, 93001036, 93001037, 93001038, 93001039, 93001040,
      93001041, 93001042, 93001043, 93001044, 93001045, 93001046, 93001047, 93001048, 93001049, 93001050,
      93001051, 93001052, 93001053, 93001054, 93001055,
      93001056, 93001057, 93001058, 93001059, 93001060, 93001061,
      93001062, 93001063, 93001064,
      93002031, 93002032, 93002033, 93002034, 93002035, 93002036, 93002037, 93002038,
      93002039, 93002040, 93002041, 93002042,
      93002052, 93002053, 93002054,
      93003601, 93003602, 93003603, 93003604, 93003605,
      93003606, 93003607, 93003608, 93003609, 93003610,
      93003611, 93003612, 93003613, 93003614, 93003615, 93003616, 93003617, 93003618,
      93003619, 93003620, 93003621, 93003622, 93003623,
      93003624, 93003625, 93003626, 93003627,
      93003628, 93003629, 93003630, 93003631, 93003632, 93003633, 93003634,
      93003635, 93003636, 93003637, 93003638, 93003639,
      93003640, 93003641, 93003642, 93003643, 93003644, 93003645, 93003646,
      93003701, 93003702, 93003703, 93003704, 93003705, 93003706, 93003707, 93003708,
      93003709, 93003710, 93003711, 93003712, 93003713, 93003714
  )
  AND parent_rm.role_id IN (@role_business_id, @role_management_id, @role_ops_id, @role_developer_id)
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu existed
      WHERE existed.tenant_id = 1
        AND existed.role_id = parent_rm.role_id
        AND existed.menu_id = child.id
        AND existed.deleted = 0
  );
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

INSERT INTO iot_normative_metric_definition (
    id, tenant_id, scenario_code, device_family, identifier, display_name, unit,
    precision_digits, monitor_content_code, monitor_type_code, risk_enabled, trend_enabled,
    metric_dimension, threshold_type, semantic_direction, gis_enabled, insight_enabled, analytics_enabled,
    status, version_no, metadata_json
) VALUES
    (920001, 1, 'phase1-crack', 'CRACK', 'value', '裂缝监测值', 'mm', 4, 'L1', 'LF', 1, 1,
     'displacement', 'absolute', 'HIGHER_IS_RISKIER', 1, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'CRACK', 'metricRole', 'PRIMARY')),
    (920002, 1, 'phase1-crack', 'CRACK', 'sensor_state', '传感器状态', NULL, 0, 'S1', 'ZT', 0, 0,
     'health_state', 'enum', 'STATE_IS_RISK', 0, 1, 0, 'ACTIVE', 1,
     JSON_OBJECT('usage', 'health_state', 'riskCategory', 'DEVICE_HEALTH', 'metricRole', 'STATE')),
    (920011, 1, 'phase2-gnss', 'GNSS', 'gpsInitial', 'GNSS 原始观测基础数据', NULL, 0, 'L1', 'GP', 0, 0,
     'raw_observation', 'snapshot', 'REFERENCE_ONLY', 0, 0, 0, 'ACTIVE', 1,
     JSON_OBJECT('usage', 'raw_observation', 'metricRole', 'CONTEXT')),
    (920012, 1, 'phase2-gnss', 'GNSS', 'gpsTotalX', 'GNSS 累计位移 X', 'mm', 4, 'L1', 'GP', 1, 1,
     'displacement', 'absolute', 'HIGHER_IS_RISKIER', 1, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'GNSS', 'metricRole', 'PRIMARY')),
    (920013, 1, 'phase2-gnss', 'GNSS', 'gpsTotalY', 'GNSS 累计位移 Y', 'mm', 4, 'L1', 'GP', 1, 1,
     'displacement', 'absolute', 'HIGHER_IS_RISKIER', 1, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'GNSS', 'metricRole', 'PRIMARY')),
    (920014, 1, 'phase2-gnss', 'GNSS', 'gpsTotalZ', 'GNSS 累计位移 Z', 'mm', 4, 'L1', 'GP', 1, 1,
     'displacement', 'absolute', 'HIGHER_IS_RISKIER', 1, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'GNSS', 'metricRole', 'PRIMARY')),
    (920015, 1, 'phase2-gnss', 'GNSS', 'sensor_state', '传感器状态', NULL, 0, 'S1', 'ZT', 0, 0,
     'health_state', 'enum', 'STATE_IS_RISK', 0, 1, 0, 'ACTIVE', 1,
     JSON_OBJECT('usage', 'health_state', 'riskCategory', 'DEVICE_HEALTH', 'metricRole', 'STATE')),
    (920021, 1, 'phase3-deep-displacement', 'DEEP_DISPLACEMENT', 'dispsX', '顺滑动方向累计变形量', 'mm', 4, 'L1', 'SW', 1, 1,
     'displacement', 'absolute', 'HIGHER_IS_RISKIER', 1, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'DEEP_DISPLACEMENT', 'metricRole', 'PRIMARY')),
    (920022, 1, 'phase3-deep-displacement', 'DEEP_DISPLACEMENT', 'dispsY', '垂直坡面方向累计变形量', 'mm', 4, 'L1', 'SW', 1, 1,
     'displacement', 'absolute', 'HIGHER_IS_RISKIER', 1, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'DEEP_DISPLACEMENT', 'metricRole', 'PRIMARY')),
    (920023, 1, 'phase3-deep-displacement', 'DEEP_DISPLACEMENT', 'sensor_state', '传感器状态', NULL, 0, 'S1', 'ZT', 0, 0,
     'health_state', 'enum', 'STATE_IS_RISK', 0, 1, 0, 'ACTIVE', 1,
     JSON_OBJECT('usage', 'health_state', 'riskCategory', 'DEVICE_HEALTH', 'metricRole', 'STATE')),
    (920031, 1, 'phase4-rain-gauge', 'RAIN_GAUGE', 'value', '当前雨量', 'mm', 2, 'L3', 'YL', 1, 1,
     'rainfall', 'absolute', 'HIGHER_IS_RISKIER', 0, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'RAIN_GAUGE', 'metricRole', 'PRIMARY')),
    (920032, 1, 'phase4-rain-gauge', 'RAIN_GAUGE', 'totalValue', '累计雨量', 'mm', 2, 'L3', 'YL', 0, 1,
     'rainfall', 'cumulative', 'HIGHER_IS_RISKIER', 0, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'cumulative', 'riskCategory', 'RAIN_GAUGE', 'metricRole', 'CONTEXT')),
    (920033, 1, 'phase3-weather', 'AIR_TEMPERATURE', 'value', '气温', '℃', 2, 'L3', 'QW', 0, 1,
     'temperature', 'absolute', 'REFERENCE_ONLY', 0, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'WEATHER', 'metricRole', 'CONTEXT')),
    (920034, 1, 'phase3-water-surface', 'SURFACE_WATER', 'temp', '地表水温', '℃', 2, 'L3', 'DB', 0, 1,
     'temperature', 'absolute', 'REFERENCE_ONLY', 0, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'SURFACE_WATER', 'metricRole', 'CONTEXT')),
    (920035, 1, 'phase3-water-surface', 'SURFACE_WATER', 'value', '地表水位', 'm', 3, 'L3', 'DB', 0, 1,
     'water_level', 'absolute', 'HIGHER_IS_RISKIER', 0, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'SURFACE_WATER', 'metricRole', 'PRIMARY')),
    (920041, 1, 'phase5-mud-level', 'MUD_LEVEL', 'value', '泥水位高程', 'm', 3, 'L4', 'NW', 0, 1,
     'water_level', 'absolute', 'HIGHER_IS_RISKIER', 0, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'MUD_LEVEL', 'metricRole', 'PRIMARY')),
    (920051, 1, 'phase6-radar', 'RADAR', 'X', '雷达 X 坐标', 'm', 3, 'L4', 'LD', 0, 1,
     'position', 'absolute', 'REFERENCE_ONLY', 0, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'RADAR', 'metricRole', 'CONTEXT')),
    (920052, 1, 'phase6-radar', 'RADAR', 'Y', '雷达 Y 坐标', 'm', 3, 'L4', 'LD', 0, 1,
     'position', 'absolute', 'REFERENCE_ONLY', 0, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'RADAR', 'metricRole', 'CONTEXT')),
    (920053, 1, 'phase6-radar', 'RADAR', 'Z', '雷达 Z 坐标', 'm', 3, 'L4', 'LD', 0, 1,
     'position', 'absolute', 'REFERENCE_ONLY', 0, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'RADAR', 'metricRole', 'CONTEXT')),
    (920054, 1, 'phase6-radar', 'RADAR', 'speed', '雷达移动速度', 'm/s', 3, 'L4', 'LD', 0, 1,
     'speed', 'absolute', 'HIGHER_IS_RISKIER', 0, 1, 1, 'ACTIVE', 1,
     JSON_OBJECT('thresholdKind', 'absolute', 'riskCategory', 'RADAR', 'metricRole', 'PRIMARY'))
ON DUPLICATE KEY UPDATE
    identifier = VALUES(identifier),
    display_name = VALUES(display_name),
    unit = VALUES(unit),
    precision_digits = VALUES(precision_digits),
    monitor_content_code = VALUES(monitor_content_code),
    monitor_type_code = VALUES(monitor_type_code),
    risk_enabled = VALUES(risk_enabled),
    trend_enabled = VALUES(trend_enabled),
    metric_dimension = VALUES(metric_dimension),
    threshold_type = VALUES(threshold_type),
    semantic_direction = VALUES(semantic_direction),
    gis_enabled = VALUES(gis_enabled),
    insight_enabled = VALUES(insight_enabled),
    analytics_enabled = VALUES(analytics_enabled),
    status = VALUES(status),
    version_no = VALUES(version_no),
    metadata_json = VALUES(metadata_json),
    deleted = 0;

INSERT INTO iot_device (
    id, tenant_id, org_id, org_name, product_id, device_name, device_code, device_secret, client_id, username, password,
    protocol_code, node_type, online_status, activate_status, device_status, firmware_version,
    ip_address, last_online_time, last_report_time, longitude, latitude, address, metadata_json,
    remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (2001, 1, 7101, '平台运维中心', 1001, '验收设备-HTTP-01', 'accept-http-device-01', '123456', 'accept-http-device-01', 'accept-http-device-01', '123456',
     'mqtt-json', 1, 1, 1, 1, '1.0.0', '10.10.1.11', DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), 121.473700, 31.230400, '上海市黄浦区中山南路', JSON_OBJECT('line', 'A', 'workshop', 'W1'),
     'HTTP 链路验收设备', 1, NOW(), 1, NOW(), 0),
    (2002, 1, 7102, '告警处置组', 1002, '验收设备-MQTT-01', 'accept-mqtt-device-01', '123456', 'accept-mqtt-device-01', 'accept-mqtt-device-01', '123456',
     'mqtt-json', 1, 1, 1, 1, '1.2.3', '10.10.1.12', DATE_SUB(NOW(), INTERVAL 8 MINUTE), DATE_SUB(NOW(), INTERVAL 1 MINUTE), 121.478900, 31.226600, '上海市黄浦区人民路', JSON_OBJECT('line', 'B', 'workshop', 'W2'),
     'MQTT 链路验收设备', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    org_id = VALUES(org_id),
    org_name = VALUES(org_name),
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

INSERT INTO sys_region (
    id, tenant_id, region_name, region_code, parent_id, region_type, longitude, latitude,
    status, sort_no, remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (62, 1, '甘肃省', '62', 0, 'province', NULL, NULL, 1, 1, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (6201, 1, '兰州市', '6201', 62, 'city', NULL, NULL, 1, 11, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (6202, 1, '嘉峪关市', '6202', 62, 'city', NULL, NULL, 1, 12, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (6205, 1, '天水市', '6205', 62, 'city', NULL, NULL, 1, 13, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (6206, 1, '武威市', '6206', 62, 'city', NULL, NULL, 1, 14, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (6208, 1, '平凉市', '6208', 62, 'city', NULL, NULL, 1, 15, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (6210, 1, '庆阳市', '6210', 62, 'city', NULL, NULL, 1, 16, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (6211, 1, '定西市', '6211', 62, 'city', NULL, NULL, 1, 17, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (6212, 1, '陇南市', '6212', 62, 'city', NULL, NULL, 1, 18, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (6229, 1, '临夏回族自治州', '6229', 62, 'city', NULL, NULL, 1, 19, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (620102, 1, '城关区', '620102', 6201, 'district', NULL, NULL, 1, 101, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (620103, 1, '七里河区', '620103', 6201, 'district', NULL, NULL, 1, 102, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (620104, 1, '西固区', '620104', 6201, 'district', NULL, NULL, 1, 103, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (620111, 1, '红古区', '620111', 6201, 'district', NULL, NULL, 1, 104, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (620121, 1, '永登县', '620121', 6201, 'district', NULL, NULL, 1, 105, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (620122, 1, '皋兰县', '620122', 6201, 'district', NULL, NULL, 1, 106, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (620123, 1, '榆中县', '620123', 6201, 'district', NULL, NULL, 1, 107, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (620201101, 1, '峪泉镇', '620201101', 6202, 'street', NULL, NULL, 1, 108, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (620503, 1, '麦积区', '620503', 6205, 'district', NULL, NULL, 1, 109, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (620522, 1, '秦安县', '620522', 6205, 'district', NULL, NULL, 1, 110, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (620602, 1, '凉州区', '620602', 6206, 'district', NULL, NULL, 1, 111, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (620622, 1, '古浪县', '620622', 6206, 'district', NULL, NULL, 1, 112, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (620881, 1, '华亭市', '620881', 6208, 'district', NULL, NULL, 1, 113, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (621021, 1, '庆城县', '621021', 6210, 'district', NULL, NULL, 1, 114, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (621026, 1, '宁县', '621026', 6210, 'district', NULL, NULL, 1, 115, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (621102, 1, '安定区', '621102', 6211, 'district', NULL, NULL, 1, 116, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (621123, 1, '渭源县', '621123', 6211, 'district', NULL, NULL, 1, 117, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (621202, 1, '武都区', '621202', 6212, 'district', NULL, NULL, 1, 118, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (621221, 1, '成县', '621221', 6212, 'district', NULL, NULL, 1, 119, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (621222, 1, '文县', '621222', 6212, 'district', NULL, NULL, 1, 120, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (621225, 1, '西和县', '621225', 6212, 'district', NULL, NULL, 1, 121, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (622901, 1, '临夏市', '622901', 6229, 'district', NULL, NULL, 1, 122, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (622923, 1, '永靖县', '622923', 6229, 'district', NULL, NULL, 1, 123, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0),
    (622925, 1, '和政县', '622925', 6229, 'district', NULL, NULL, 1, 124, '高速公路风险点行政区划最小基线', 1, NOW(), 1, NOW(), 0)
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

INSERT INTO sys_organization (
    id, tenant_id, parent_id, org_name, org_code, org_type, leader_user_id, leader_name,
    phone, email, status, sort_no, remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (7111, 1, 0, '成县所', 'HW-ORG-CXS', 'dept', 1, '系统管理员', '13800000000', NULL, 1, 101, '高速公路风险点管养单位最小基线', 1, NOW(), 1, NOW(), 0),
    (7112, 1, 0, '成武所', 'HW-ORG-CWS', 'dept', 1, '系统管理员', '13800000000', NULL, 1, 102, '高速公路风险点管养单位最小基线', 1, NOW(), 1, NOW(), 0),
    (7113, 1, 0, '武都所', 'HW-ORG-WDS', 'dept', 1, '系统管理员', '13800000000', NULL, 1, 103, '高速公路风险点管养单位最小基线', 1, NOW(), 1, NOW(), 0),
    (7114, 1, 0, '甘肃公航旅高速公路运营管理兰州分公司', 'HW-ORG-LZFGS', 'dept', 1, '系统管理员', '13800000000', NULL, 1, 104, '高速公路风险点管养单位最小基线', 1, NOW(), 1, NOW(), 0),
    (7115, 1, 0, '甘肃公航旅高速公路运营管理临夏分公司', 'HW-ORG-LXFGS', 'dept', 1, '系统管理员', '13800000000', NULL, 1, 105, '高速公路风险点管养单位最小基线', 1, NOW(), 1, NOW(), 0),
    (7116, 1, 0, '甘肃公航旅高速公路运营管理平凉分公司', 'HW-ORG-PLFGS', 'dept', 1, '系统管理员', '13800000000', NULL, 1, 106, '高速公路风险点管养单位最小基线', 1, NOW(), 1, NOW(), 0),
    (7117, 1, 0, '甘肃公航旅高速公路运营管理武威分公司', 'HW-ORG-WWFGS', 'dept', 1, '系统管理员', '13800000000', NULL, 1, 107, '高速公路风险点管养单位最小基线', 1, NOW(), 1, NOW(), 0),
    (7118, 1, 0, '甘肃公航旅高速公路运营管理天水分公司', 'HW-ORG-TSFGS', 'dept', 1, '系统管理员', '13800000000', NULL, 1, 108, '高速公路风险点管养单位最小基线', 1, NOW(), 1, NOW(), 0),
    (7119, 1, 0, '甘肃公航旅高速公路运营管理定西分公司', 'HW-ORG-DXFGS', 'dept', 1, '系统管理员', '13800000000', NULL, 1, 109, '高速公路风险点管养单位最小基线', 1, NOW(), 1, NOW(), 0),
    (7120, 1, 0, '甘肃公航旅高速公路运营管理敦煌分公司', 'HW-ORG-DHFGS', 'dept', 1, '系统管理员', '13800000000', NULL, 1, 110, '高速公路风险点管养单位最小基线', 1, NOW(), 1, NOW(), 0)
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
    (7201, 1, '风险点等级', 'risk_point_level', 'text', 1, 1, '风险点档案等级字典', 1, NOW(), 1, NOW(), 0),
    (7202, 1, '告警等级', 'alarm_level', 'text', 1, 2, '告警等级四色字典', 1, NOW(), 1, NOW(), 0),
    (7203, 1, '风险态势等级', 'risk_level', 'text', 1, 3, '运行态风险颜色字典', 1, NOW(), 1, NOW(), 0),
    (7204, 1, '帮助文档分类', 'help_doc_category', 'text', 1, 4, '帮助中心分类字典', 1, NOW(), 1, NOW(), 0),
    (7205, 1, '通知渠道类型', 'notification_channel_type', 'text', 1, 5, '通知渠道类型字典', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    dict_name = VALUES(dict_name),
    dict_type = VALUES(dict_type),
    status = VALUES(status),
    sort_no = VALUES(sort_no),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

UPDATE sys_dict
SET status = 0,
    deleted = 1,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND dict_code IN ('risk_point_level', 'alarm_level', 'risk_level', 'help_doc_category', 'notification_channel_type')
  AND id NOT IN (7201, 7202, 7203, 7204, 7205);

INSERT INTO sys_dict_item (
    id, tenant_id, dict_id, item_name, item_value, item_type, status, sort_no, remark,
    create_by, create_time, update_by, update_time, deleted
) VALUES
    (7301, 1, 7201, '一级风险点', 'level_1', 'string', 1, 1, '风险点等级-一级风险点', 1, NOW(), 1, NOW(), 0),
    (7302, 1, 7201, '二级风险点', 'level_2', 'string', 1, 2, '风险点等级-二级风险点', 1, NOW(), 1, NOW(), 0),
    (7303, 1, 7201, '三级风险点', 'level_3', 'string', 1, 3, '风险点等级-三级风险点', 1, NOW(), 1, NOW(), 0),
    (7304, 1, 7202, '红色', 'red', 'string', 1, 1, '告警等级-红色', 1, NOW(), 1, NOW(), 0),
    (7305, 1, 7202, '橙色', 'orange', 'string', 1, 2, '告警等级-橙色', 1, NOW(), 1, NOW(), 0),
    (7306, 1, 7202, '黄色', 'yellow', 'string', 1, 3, '告警等级-黄色', 1, NOW(), 1, NOW(), 0),
    (7307, 1, 7202, '蓝色', 'blue', 'string', 1, 4, '告警等级-蓝色', 1, NOW(), 1, NOW(), 0),
    (7308, 1, 7203, '红色', 'red', 'string', 1, 1, '风险态势等级-红色', 1, NOW(), 1, NOW(), 0),
    (7309, 1, 7203, '橙色', 'orange', 'string', 1, 2, '风险态势等级-橙色', 1, NOW(), 1, NOW(), 0),
    (7310, 1, 7203, '黄色', 'yellow', 'string', 1, 3, '风险态势等级-黄色', 1, NOW(), 1, NOW(), 0),
    (7311, 1, 7203, '蓝色', 'blue', 'string', 1, 4, '风险态势等级-蓝色', 1, NOW(), 1, NOW(), 0),
    (7312, 1, 7204, '业务类', 'business', 'string', 1, 1, '帮助文档分类-业务类', 1, NOW(), 1, NOW(), 0),
    (7313, 1, 7204, '技术类', 'technical', 'string', 1, 2, '帮助文档分类-技术类', 1, NOW(), 1, NOW(), 0),
    (7314, 1, 7204, '常见问题', 'faq', 'string', 1, 3, '帮助文档分类-常见问题', 1, NOW(), 1, NOW(), 0),
    (7315, 1, 7205, '邮件', 'email', 'string', 1, 1, '通知渠道类型-邮件', 1, NOW(), 1, NOW(), 0),
    (7316, 1, 7205, '短信', 'sms', 'string', 1, 2, '通知渠道类型-短信', 1, NOW(), 1, NOW(), 0),
    (7317, 1, 7205, 'Webhook', 'webhook', 'string', 1, 3, '通知渠道类型-Webhook', 1, NOW(), 1, NOW(), 0),
    (7318, 1, 7205, '微信', 'wechat', 'string', 1, 4, '通知渠道类型-微信', 1, NOW(), 1, NOW(), 0),
    (7319, 1, 7205, '飞书', 'feishu', 'string', 1, 5, '通知渠道类型-飞书', 1, NOW(), 1, NOW(), 0),
    (7320, 1, 7205, '钉钉', 'dingtalk', 'string', 1, 6, '通知渠道类型-钉钉', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    dict_id = VALUES(dict_id),
    item_name = VALUES(item_name),
    item_value = VALUES(item_value),
    item_type = VALUES(item_type),
    status = VALUES(status),
    sort_no = VALUES(sort_no),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

UPDATE sys_dict_item
SET status = 0,
    deleted = 1,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND (
      (dict_id = 7201 AND item_value NOT IN ('level_1', 'level_2', 'level_3'))
      OR (dict_id = 7202 AND item_value NOT IN ('red', 'orange', 'yellow', 'blue'))
      OR (dict_id = 7203 AND item_value NOT IN ('red', 'orange', 'yellow', 'blue'))
      OR (dict_id = 7204 AND item_value NOT IN ('business', 'technical', 'faq'))
      OR (dict_id = 7205 AND item_value NOT IN ('email', 'sms', 'webhook', 'wechat', 'feishu', 'dingtalk'))
  );

INSERT INTO sys_notification_channel (
    id, tenant_id, channel_name, channel_code, channel_type, config, status, sort_no, remark,
    create_by, create_time, update_by, update_time, deleted
) VALUES
    (7401, 1, '邮件通知', 'email-default', 'email', JSON_OBJECT('host', 'smtp.example.com', 'port', 465, 'from', 'iot-alert@ghlzm.com'), 1, 1, '默认邮件通知', 1, NOW(), 1, NOW(), 0),
    (7402, 1, 'Webhook通知', 'webhook-default', 'webhook', JSON_OBJECT('url', 'https://example.com/iot/webhook', 'headers', JSON_OBJECT('Authorization', 'Bearer demo-token'), 'scenes', JSON_ARRAY('system_error', 'observability_alert', 'in_app_unread_bridge'), 'timeoutMs', 3000, 'minIntervalSeconds', 300), 1, 2, '默认Webhook通知（含 system_error、observability_alert 与高优未读桥接场景）', 1, NOW(), 1, NOW(), 0)
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

INSERT INTO sys_in_app_message (
    id, tenant_id, message_type, priority, title, summary, content, target_type, target_role_codes, target_user_ids,
    related_path, source_type, source_id, dedup_key, publish_time, expire_time, status, sort_no, create_by, create_time, update_by, update_time, deleted
) VALUES
    (760101, 1, 'system', 'critical', '系统维护窗口提醒', '今晚 23:00 将执行日志链路维护，请提前完成排障导出。', '平台计划在今晚 23:00 至 23:30 执行日志链路维护。维护期间通知中心、审计中心与异常观测台可能存在短时延迟，请运维与研发同事提前导出必要信息。', 'all', NULL, NULL,
     '/system-log', 'manual', 'maintenance-20260321', MD5('manual|maintenance-20260321|all|system'), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, 1, 1, NOW(), 1, NOW(), 0),
    (760102, 1, 'business', 'high', '风险运营日报待确认', '请业务与管理角色在 18:00 前确认今日告警闭环情况。', '风险运营日报已生成，请优先核对今日告警确认率、事件派工状态和待关闭事项。如存在跨班次未闭环问题，请在事件协同台补充反馈。', 'role', 'BUSINESS_STAFF,MANAGEMENT_STAFF', NULL,
     '/alarm-center', 'manual', 'risk-report-20260321', MD5('manual|risk-report-20260321|role:BUSINESS_STAFF,MANAGEMENT_STAFF|business'), DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_ADD(NOW(), INTERVAL 2 DAY), 1, 2, 1, NOW(), 1, NOW(), 0),
    (760103, 1, 'error', 'high', '接入链路异常排查提示', '检测到最近 30 分钟内存在 MQTT 分发失败，请研发和运维优先复核 TraceId。', '系统在最近 30 分钟内检测到 MQTT 分发失败与设备不存在异常。请先在异常观测台定位 system_error，再到链路追踪台按 TraceId 复核上下游日志。', 'role', 'OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN', NULL,
     '/message-trace', 'system_error', 'mqtt-dispatch-failed', MD5('system_error|mqtt-dispatch-failed|role:OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN|error'), DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_ADD(NOW(), INTERVAL 3 DAY), 1, 3, 1, NOW(), 1, NOW(), 0),
    (760104, 1, 'business', 'medium', '通知编排配置复核', '管理员请复核默认 webhook 渠道是否仍指向有效地址。', '近期已补齐系统异常与规则化运维告警通知能力，请管理员复核默认 webhook 地址、超时时间与 system_error / observability_alert 场景配置，避免真实环境异常无人接收。', 'user', NULL, '1',
     '/channel', 'governance', 'channel-review-admin', MD5('governance|channel-review-admin|user:1|business'), DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 5 DAY), 1, 4, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    message_type = VALUES(message_type),
    priority = VALUES(priority),
    title = VALUES(title),
    summary = VALUES(summary),
    content = VALUES(content),
    target_type = VALUES(target_type),
    target_role_codes = VALUES(target_role_codes),
    target_user_ids = VALUES(target_user_ids),
    related_path = VALUES(related_path),
    source_type = VALUES(source_type),
    source_id = VALUES(source_id),
    dedup_key = VALUES(dedup_key),
    publish_time = VALUES(publish_time),
    expire_time = VALUES(expire_time),
    status = VALUES(status),
    sort_no = VALUES(sort_no),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO sys_in_app_message_read (
    id, tenant_id, message_id, user_id, read_time, create_time, update_time
)
SELECT 760201, 1, 760101, @user_admin_id, DATE_SUB(NOW(), INTERVAL 30 MINUTE), NOW(), NOW()
FROM DUAL
WHERE @user_admin_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    read_time = VALUES(read_time),
    update_time = NOW();

INSERT INTO sys_in_app_message_read (
    id, tenant_id, message_id, user_id, read_time, create_time, update_time
)
SELECT 760202, 1, 760102, @user_business_demo_id, DATE_SUB(NOW(), INTERVAL 15 MINUTE), NOW(), NOW()
FROM DUAL
WHERE @user_business_demo_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    read_time = VALUES(read_time),
    update_time = NOW();

INSERT INTO sys_help_document (
    id, tenant_id, doc_category, title, summary, content, keywords, related_paths, visible_role_codes,
    status, sort_no, create_by, create_time, update_by, update_time, deleted
) VALUES
    (760301, 1, 'business', '产品与设备建档指南', '面向业务与管理角色的产品建档、设备建档和父子拓扑维护指引。', '适用角色：业务人员、管理人员。适用页面：产品定义中心、设备资产中心。使用场景：新设备入库、项目上线前建档、父子设备关系维护。操作步骤：1. 先在产品定义中心确认 productKey、协议、节点类型和厂商口径。2. 产品启用后再在设备资产中心新增设备，并通过 productKey 继承协议与节点类型。3. 存在网关或父设备时，同时维护 parentDeviceId 或 parentDeviceCode。4. 需要批量导入时，先核对产品启用状态和父设备编码。结果判断：产品列表可查询，设备详情能看到产品、父设备和关联网关信息，建档后可继续上报与下发。常见问题：产品停用后不能继续建档；产品删除前需先清空关联库存设备。延伸阅读：产品与设备字段口径、建档顺序、父子拓扑维护规则。', '产品,设备,建档,拓扑,productKey', '/products,/devices', 'BUSINESS_STAFF,MANAGEMENT_STAFF',
     1, 1, 1, NOW(), 1, NOW(), 0),
    (760302, 1, 'business', '告警确认、抑制与关闭操作', '面向业务与管理角色的告警闭环操作说明。', '适用角色：业务人员、管理人员。适用页面：告警运营台。使用场景：日常告警研判、值班交接、闭环复核。操作步骤：1. 先在告警列表按等级、状态和时间范围筛选目标告警。2. 确认异常已被识别时执行确认，并补充处置说明。3. 明确无需继续打扰的告警可执行抑制。4. 风险已消除且处置完成后再执行关闭。结果判断：告警详情中能看到确认、抑制或关闭后的最新状态和处置痕迹。常见问题：关闭前应先确认现场或联动结果，避免把未处理完成的异常提前结束。延伸阅读：告警等级、事件联动关系、运营分析中心闭环统计口径。', '告警,确认,抑制,关闭,闭环', '/alarm-center', 'BUSINESS_STAFF,MANAGEMENT_STAFF',
     1, 2, 1, NOW(), 1, NOW(), 0),
    (760303, 1, 'business', '事件派工、接收、处理、完结流程', '面向业务与管理角色的事件协同闭环指引。', '适用角色：业务人员、管理人员。适用页面：事件协同台。使用场景：告警转事件后的派工、接收、处理和完结跟踪。操作步骤：1. 在事件列表确认事件等级、来源和待办状态。2. 管理人员先完成派工，明确负责人和处理要求。3. 执行人员依次完成接收、开始处理、处理反馈和完成。4. 结果复核通过后再关闭事件。结果判断：事件详情可看到派工记录、工单状态和反馈留痕，运营分析中心可统计闭环结果。常见问题：若处理中发现条件变化，应先补反馈再调整派工，不要跳过状态流转。延伸阅读：告警到事件闭环、工单处理状态、事件关闭统计口径。', '事件,派工,工单,接收,完结', '/event-disposal', 'BUSINESS_STAFF,MANAGEMENT_STAFF',
     1, 3, 1, NOW(), 1, NOW(), 0),
    (760304, 1, 'business', '风险对象、阈值策略、联动预案配置说明', '面向业务与管理角色的风险策略编排说明。', '适用角色：业务人员、管理人员。适用页面：风险对象中心、阈值策略、联动编排、应急预案库。使用场景：新增风险对象、配置告警阈值、定义联动动作和应急预案。操作步骤：1. 先在风险对象中心完成风险点和设备测点绑定。2. 再配置阈值策略，明确指标、表达式和告警等级。3. 需要自动处置时，继续补齐联动规则与应急预案。4. 上线前结合真实设备或演示数据验证命中效果。结果判断：风险对象、规则、联动和预案都可独立查询，命中结果会在告警或事件留痕中体现。常见问题：若角色暂无对应菜单权限，帮助中心不会推荐无权访问的策略资料。延伸阅读：风险点绑定、阈值规则、联动规则、应急预案接口与流程。', '风险对象,阈值,联动,预案,策略', '/risk-point,/rule-definition,/linkage-rule,/emergency-plan', 'BUSINESS_STAFF,MANAGEMENT_STAFF',
     1, 4, 1, NOW(), 1, NOW(), 0),
    (760305, 1, 'business', '运营分析中心指标查看说明', '面向业务与管理角色的报表查看指引。', '适用角色：业务人员、管理人员。适用页面：运营分析中心。使用场景：日报、周报、闭环复盘和管理复核。操作步骤：1. 按时间范围查看风险趋势、告警统计、事件闭环和设备健康。2. 对比异常波峰与闭环效率，定位需要跟进的风险点或工单。3. 必要时回到告警运营台或事件协同台继续核实明细。结果判断：能在同一页面完成趋势观察、告警统计和闭环结果复核，并与业务处置页面形成往返。常见问题：报表用于运营复盘，不替代原始告警或事件详情。延伸阅读：风险趋势、告警统计、事件闭环、设备健康四类分析接口。', '报表,运营分析,风险趋势,告警统计,事件闭环', '/report-analysis', 'BUSINESS_STAFF,MANAGEMENT_STAFF',
     1, 5, 1, NOW(), 1, NOW(), 0),
    (760306, 1, 'technical', 'HTTP 上报与链路验证中心使用说明', '面向运维与研发角色的 HTTP 模拟上报与链路验证资料。', '适用角色：运维人员、开发人员、超级管理员。适用页面：链路验证中心。使用场景：联调 HTTP 上报、验证协议解码、复核属性和消息日志是否落库。操作步骤：1. 在链路验证中心选择明文或密文模式。2. 明文模式按 C.1、C.2、C.3 组织正文，密文模式直接传封包 JSON。3. 发送后立即查看属性查询、消息日志和设备在线状态是否刷新。4. 如失败，再结合 TraceId 到异常观测台和链路追踪台继续排查。结果判断：接口返回成功，设备属性、消息日志和在线状态同步更新。常见问题：明文二进制模拟要按单字节编码发送，密文模式则直接传原始封包。延伸阅读：HTTP 上报入口、链路验证中心明文/密文模式、属性与消息日志查询。', 'HTTP,链路验证,上报,明文,密文', '/reporting', 'OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN',
     1, 6, 1, NOW(), 1, NOW(), 0),
    (760307, 1, 'technical', 'MQTT Topic 规范与 mqtt-json / $dp 兼容说明', '面向运维与研发角色的 MQTT 接入规范说明。', '适用角色：运维人员、开发人员、超级管理员。适用页面：链路验证中心、链路追踪台。使用场景：联调标准 Topic、兼容历史 `$dp`、定位上行报文格式不匹配问题。操作步骤：1. 优先确认设备使用标准 `/sys/{productKey}/{deviceCode}/thing/.../post` Topic 还是历史 `$dp`。2. 标准 Topic 场景按产品协议选择 `mqtt-json` 等适配器。3. 历史 `$dp` 场景重点核对解密、测点映射和子设备拆分。4. 若下行验证失败，再复核目标产品状态和下发 Topic。结果判断：消息可被正确解码，属性和日志按目标设备落库，必要时支持下行最小发布。常见问题：协议、Topic、productKey 和 deviceCode 任一不一致，都可能导致设备不存在或分发失败。延伸阅读：MQTT Topic 规范、`mqtt-json` 解码、`$dp` 历史兼容链路。', 'MQTT,Topic,mqtt-json,$dp,协议', '/reporting,/message-trace', 'OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN',
     1, 7, 1, NOW(), 1, NOW(), 0),
    (760308, 1, 'technical', 'TraceId、链路追踪台、异常观测台排障说明', '面向运维与研发角色的统一排障路径说明。', '适用角色：运维人员、开发人员、超级管理员。适用页面：链路追踪台、异常观测台。使用场景：出现 MQTT 分发失败、设备不存在、后台异常或接口报错时快速串联上下游链路。操作步骤：1. 先获取请求或日志里的 TraceId。2. 在异常观测台查看是否存在 system_error 记录。3. 再到链路追踪台按 TraceId、设备编码、产品标识或 Topic 检索消息日志。4. 结合接口返回和设备属性结果确认故障落点。结果判断：能够把 HTTP、MQTT、系统异常和消息日志串成同一条排障链路。常见问题：没有 TraceId 时可先按设备编码或 Topic 缩小范围，再回溯对应异常记录。延伸阅读：X-Trace-Id、system_error 审计、消息追踪与消息日志查询。', 'TraceId,链路追踪,异常观测,system_error,排障', '/message-trace,/system-log', 'OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN',
     1, 8, 1, NOW(), 1, NOW(), 0),
    (760309, 1, 'technical', '真实环境启动、环境变量与依赖检查说明', '面向运维与研发角色的真实环境启动前检查清单。', '适用角色：运维人员、开发人员、超级管理员。适用页面：链路验证中心、异常观测台。使用场景：本地联调、共享环境排障、历史库升级后复验。操作步骤：1. 先确认唯一启动模块仍为 `spring-boot-iot-admin`。2. 以 `application-dev.yml` 为基线检查 MySQL、Redis、TDengine、MQTT 等依赖是否可达。3. 需要覆盖默认配置时，优先使用环境变量。4. 历史库若缺少系统内容能力或治理菜单，先补齐对应初始化或升级数据再验收。结果判断：后端能按 `dev` 配置启动，前端可正常访问 `/api/system/help-doc/**`、`/api/system/in-app-message/**` 和主要业务接口。常见问题：真实环境不可用时只能记录为环境阻塞，不能回退到旧 H2 验收路径。延伸阅读：运行基线、关键环境变量、历史库升级与排障入口。', '启动,环境变量,依赖检查,application-dev,验收', '/reporting,/system-log', 'OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN',
     1, 9, 1, NOW(), 1, NOW(), 0),
    (760310, 1, 'faq', 'FAQ：产品和设备有什么区别', '统一解释产品、设备、父子拓扑和建档顺序。', '适用角色：所有登录用户。适用页面：产品定义中心、设备资产中心。使用场景：首次接触设备建档、解释产品模板和现场资产实例的差异。操作步骤：1. 先把产品理解为接入模板。2. 再把设备理解为现场资产实例。3. 有父子拓扑时，把关系维护在设备侧而不是产品侧。结果判断：能够区分哪些信息属于产品主数据，哪些信息属于设备实例信息。常见问题：同一项目下多台同类设备通常共用一个产品，不因安装位置不同重复建产品。延伸阅读：产品与设备逻辑关系、字段口径、建档顺序。', '产品,设备,FAQ,建档,拓扑', '/products,/devices', '',
     1, 10, 1, NOW(), 1, NOW(), 0),
    (760311, 1, 'faq', 'FAQ：为什么我看不到某个页面或帮助文档', '说明菜单权限、角色范围和帮助资料过滤规则。', '适用角色：所有登录用户。适用页面：通用。使用场景：当前账号能登录，但看不到目标页面、按钮或帮助资料时。操作步骤：1. 先确认当前账号角色是否具备目标菜单授权。2. 再确认帮助文档是否配置了可见角色或关联页面路径。3. 如果页面本身无权访问，帮助中心也不会继续推荐对应资料。结果判断：角色和菜单授权补齐后，页面入口和帮助资料会一起恢复可见。常见问题：帮助中心按权限过滤内容，故意不展示“看得到但点不进去”的伪入口。延伸阅读：角色默认范围、菜单授权、帮助文档可见性规则。', '权限,角色,菜单,帮助文档,可见性', NULL, '',
     1, 11, 1, NOW(), 1, NOW(), 0),
    (760312, 1, 'faq', 'FAQ：通知中心与帮助中心怎么用', '说明右上角壳层入口的分类规则和使用方式。', '适用角色：所有登录用户。适用页面：通用。使用场景：首次使用头部壳层入口或需要快速查找消息、帮助资料时。操作步骤：1. 在右上角打开通知中心查看系统事件、业务事件和错误事件。2. 在帮助中心查看业务类、技术类和 FAQ 资料。3. 需要更多内容时，通过“查看更多”进入列表抽屉，再做分类筛选和关键字搜索。结果判断：能从摘要面板进入列表和详情，并跳转到关联页面继续处理。常见问题：帮助中心只展示当前账号真正有权访问的资料，通知已读也需要显式操作。延伸阅读：壳层摘要、列表抽屉、详情抽屉与权限过滤规则。', '通知中心,帮助中心,FAQ,壳层,搜索', NULL, '',
     1, 12, 1, NOW(), 1, NOW(), 0),
    (760313, 1, 'faq', 'FAQ：为什么会出现 401、无权限或系统内容缺表提示', '说明常见认证、授权和环境初始化问题的排查方向。', '适用角色：所有登录用户。适用页面：通用。使用场景：接口返回 `401`、页面提示无权限，或系统内容接口提示缺少依赖数据时。操作步骤：1. `401` 场景先重新登录并确认前端已携带 Bearer token。2. 无权限场景先检查角色、菜单和按钮授权。3. 系统内容依赖缺失时，请管理员补齐帮助文档和站内消息所需的数据表与初始化数据。结果判断：认证恢复后接口可访问，授权补齐后页面入口与按钮恢复，系统内容初始化完成后帮助中心和通知中心可正常返回数据。常见问题：环境阻塞要明确记录为环境问题，不能用旧验收路径替代真实环境。延伸阅读：鉴权规范、菜单授权、真实环境初始化与升级说明。', '401,无权限,系统内容,初始化,FAQ', NULL, '',
     1, 13, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    doc_category = VALUES(doc_category),
    title = VALUES(title),
    summary = VALUES(summary),
    content = VALUES(content),
    keywords = VALUES(keywords),
    related_paths = VALUES(related_paths),
    visible_role_codes = VALUES(visible_role_codes),
    status = VALUES(status),
    sort_no = VALUES(sort_no),
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
    id, risk_point_code, risk_point_name, org_id, org_name, region_id, region_name, responsible_user, responsible_phone,
    risk_level, risk_type, location_text, longitude, latitude, description, status, tenant_id, create_by, create_time, update_by, update_time, deleted
) VALUES
    (8001, 'RP-HP-001', '锅炉温压监测点', 7101, '平台运维中心', 7002, '黄浦厂区', 1, '13800000000', 'red', 'GENERAL', NULL, NULL, NULL, '锅炉区高温高压风险监测', 0, 1, 1, NOW(), 1, NOW(), 0),
    (8002, 'RP-HP-002', '振动监测点', 7102, '告警处置组', 7002, '黄浦厂区', 1, '13800000000', 'orange', 'GENERAL', NULL, NULL, NULL, '关键设备振动风险监测', 0, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    risk_point_name = VALUES(risk_point_name),
    org_id = VALUES(org_id),
    org_name = VALUES(org_name),
    region_id = VALUES(region_id),
    region_name = VALUES(region_name),
    responsible_user = VALUES(responsible_user),
    responsible_phone = VALUES(responsible_phone),
    risk_level = VALUES(risk_level),
    risk_type = VALUES(risk_type),
    location_text = VALUES(location_text),
    longitude = VALUES(longitude),
    latitude = VALUES(latitude),
    description = VALUES(description),
    status = VALUES(status),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO risk_point (
    id, risk_point_code, risk_point_name, org_id, org_name, region_id, region_name, responsible_user, responsible_phone,
    risk_level, risk_type, location_text, longitude, latitude, description, status, tenant_id, create_by, create_time, update_by, update_time, deleted
) VALUES
    (8501, 'RP-HW-SLOPE-001', 'G7011十天高速K595', 7111, '成县所', 621221, '成县', 1, '13800000000', 'blue', 'SLOPE', 'SXK595+818-SX595+960', 105.65682, 33.698381, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8502, 'RP-HW-SLOPE-002', 'G22青兰高速平定段K1458+75', 7116, '甘肃公航旅高速公路运营管理平凉分公司', 621026, '宁县', 1, '13800000000', 'blue', 'SLOPE', 'G22', 107.75108, 35.33165, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8503, 'RP-HW-SLOPE-003', 'G2012定武高速XK450+182', 7117, '甘肃公航旅高速公路运营管理武威分公司', 620622, '古浪县', 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8504, 'RP-HW-TUNNEL-001', 'G8513平绵高速成武段SK384+870-SK384+920', 7112, '成武所', 621202, '武都区', 1, '13800000000', 'blue', 'TUNNEL', 'G8513', 105.09881, 33.469798, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8505, 'RP-HW-SLOPE-004', 'S38王夏高速K14+650', 7115, '甘肃公航旅高速公路运营管理临夏分公司', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'S38', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8506, 'RP-HW-SLOPE-005', 'S32临大高速K10+176-K10+560', 7115, '甘肃公航旅高速公路运营管理临夏分公司', 622901, '临夏市', 1, '13800000000', 'blue', 'SLOPE', 'S32', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8507, 'RP-HW-SLOPE-006', 'G30连霍高速树徐段K1740+300', 7114, '甘肃公航旅高速公路运营管理兰州分公司', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G30', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8508, 'RP-HW-SLOPE-007', 'G30连霍高速柳忠段K1689+900-K1690+300', 7114, '甘肃公航旅高速公路运营管理兰州分公司', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G30', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8509, 'RP-HW-SLOPE-008', 'G6京藏高速刘白段K1414+770-K1415+550', 7114, '甘肃公航旅高速公路运营管理兰州分公司', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G6', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8510, 'RP-HW-SLOPE-009', 'G75兰海高速兰临段K14+325', 7114, '甘肃公航旅高速公路运营管理兰州分公司', 620103, '七里河区', 1, '13800000000', 'blue', 'SLOPE', 'G75', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8511, 'RP-HW-SLOPE-010', 'G75兰海高速兰临段K11+325', 7114, '甘肃公航旅高速公路运营管理兰州分公司', 620103, '七里河区', 1, '13800000000', 'blue', 'SLOPE', 'G75', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8512, 'RP-HW-SLOPE-011', 'G75兰海高速兰临段K14+149.5', 7114, '甘肃公航旅高速公路运营管理兰州分公司', 620103, '七里河区', 1, '13800000000', 'blue', 'SLOPE', 'G75', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8513, 'RP-HW-BRIDGE-001', 'G6 京藏高速兰海段K1661+350', 7114, '甘肃公航旅高速公路运营管理兰州分公司', 620111, '红古区', 1, '13800000000', 'blue', 'BRIDGE', 'G6', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8514, 'RP-HW-SLOPE-012', 'G6京藏高速白兰段K1548+810', 7114, '甘肃公航旅高速公路运营管理兰州分公司', 620122, '皋兰县', 1, '13800000000', 'blue', 'SLOPE', 'G6', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8515, 'RP-HW-SLOPE-013', 'G2201南绕城高速K39+800', 7114, '甘肃公航旅高速公路运营管理兰州分公司', 620104, '西固区', 1, '13800000000', 'blue', 'SLOPE', 'G2201', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8516, 'RP-HW-SLOPE-014', 'G30连霍高速柳忠段K1695+500～K1699+800', 7114, '甘肃公航旅高速公路运营管理兰州分公司', 620102, '城关区', 1, '13800000000', 'blue', 'SLOPE', 'G30', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8517, 'RP-HW-BRIDGE-002', 'G568兰永一级K75+253', 7114, '甘肃公航旅高速公路运营管理兰州分公司', 622923, '永靖县', 1, '13800000000', 'blue', 'BRIDGE', 'G568', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8518, 'RP-HW-BRIDGE-003', 'G3011柳格高速K74+830', 7120, '甘肃公航旅高速公路运营管理敦煌分公司', 620201101, '峪泉镇', 1, '13800000000', 'blue', 'BRIDGE', 'G3011', 95.896419, 40.517818, 'G3011柳格高速K74+830', 0, 1, 1, NOW(), 1, NOW(), 0),
    (8519, 'RP-HW-SLOPE-015', 'G2012定武高速SK394+700～SK394+900', 7117, '甘肃公航旅高速公路运营管理武威分公司', 620602, '凉州区', 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8520, 'RP-HW-SLOPE-016', 'G2012定武高速K393+170', 7117, '甘肃公航旅高速公路运营管理武威分公司', 620602, '凉州区', 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8521, 'RP-HW-SLOPE-017', 'G2012定武高速K413+446', 7117, '甘肃公航旅高速公路运营管理武威分公司', 620602, '凉州区', 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8522, 'RP-HW-SLOPE-018', 'G2012定武高速K400+315', 7117, '甘肃公航旅高速公路运营管理武威分公司', 620602, '凉州区', 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8523, 'RP-HW-SLOPE-019', 'G2012定武高速K392+410', 7117, '甘肃公航旅高速公路运营管理武威分公司', 620602, '凉州区', 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8524, 'RP-HW-SLOPE-020', 'G2012定武高速K392+790', 7117, '甘肃公航旅高速公路运营管理武威分公司', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8525, 'RP-HW-SLOPE-021', 'G2012定武高速K479+665', 7117, '甘肃公航旅高速公路运营管理武威分公司', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8526, 'RP-HW-SLOPE-022', 'G2012定武高速K465+175', 7117, '甘肃公航旅高速公路运营管理武威分公司', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8527, 'RP-HW-SLOPE-023', 'G2012定武高速K462+995', 7117, '甘肃公航旅高速公路运营管理武威分公司', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8528, 'RP-HW-SLOPE-024', 'G2012定武高速K420+891', 7117, '甘肃公航旅高速公路运营管理武威分公司', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8529, 'RP-HW-SLOPE-025', 'G2012定武高速K437+982', 7117, '甘肃公航旅高速公路运营管理武威分公司', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8530, 'RP-HW-SLOPE-026', 'G2012定武高速SK450+182', 7117, '甘肃公航旅高速公路运营管理武威分公司', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8531, 'RP-HW-SLOPE-027', 'G3017金武高速K67+050-K67+700', 7117, '甘肃公航旅高速公路运营管理武威分公司', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G3017', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8532, 'RP-HW-SLOPE-028', 'G3017金武高速K68+860-K69+100', 7117, '甘肃公航旅高速公路运营管理武威分公司', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G3017', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8533, 'RP-HW-BRIDGE-004', 'G22青兰高速巉柳段SK1856+400-500桥梁', 7119, '甘肃公航旅高速公路运营管理定西分公司', 620123, '榆中县', 1, '13800000000', 'blue', 'BRIDGE', 'G22', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8534, 'RP-HW-TUNNEL-002', 'G22青兰高速巉柳段SK1839+928隧道', 7119, '甘肃公航旅高速公路运营管理定西分公司', 620123, '榆中县', 1, '13800000000', 'blue', 'TUNNEL', 'G22', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8535, 'RP-HW-SLOPE-029', 'G22青兰高速巉柳段K1858+250沉陷', 7119, '甘肃公航旅高速公路运营管理定西分公司', 620123, '榆中县', 1, '13800000000', 'blue', 'SLOPE', 'G22', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8536, 'RP-HW-SLOPE-030', 'G22青兰高速巉柳段K1855+575沉陷', 7119, '甘肃公航旅高速公路运营管理定西分公司', 620123, '榆中县', 1, '13800000000', 'blue', 'SLOPE', 'G22', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8537, 'RP-HW-TUNNEL-003', 'G22青兰高速雷西段K1374+670-K1374+780', 7116, '甘肃公航旅高速公路运营管理平凉分公司', 621021, '庆城县', 1, '13800000000', 'blue', 'TUNNEL', 'G22', 107.70219, 35.904263, '棚洞设计标准受控于庆城隧道设计。隧道设计车速80km/h，按分离式设计，上、下行间距为36m 左右，每幅为单向双车道，棚洞采用变截面拱门式侧墙，侧墙内轮廓半径 5.43m，侧墙距电缆槽顶面高度 3.1m，采用钢筋混凝土扩大基础，横向设 C25 钢筋混凝土支撑梁，4m 间距设一道，横向支撑梁与侧墙混凝土一体浇筑完成。棚洞拱顶采用 I18 钢拱架为骨架，纵向间距为 3.0m，钢拱 架上沿纵向铺设[10 槽钢，槽钢与钢拱架焊接，顶部铺设蓝色遮光板，采用螺栓与槽钢连接。棚洞高填方路基下设钢波纹管涵，将沟内汇水排至下游沟谷。          经调查，上下行线棚洞病害情况相近，主要病害为棚洞拱墙开裂、局部混凝土剥落，裂缝最大 宽度达 2.2cm，个别部位拱圈整体剥落，拱墙大面积渗水、泛碱严重，涂装剥蚀泛碱严重。', 0, 1, 1, NOW(), 1, NOW(), 0),
    (8538, 'RP-HW-SLOPE-031', 'G8513平绵高速平天段YK54+925', 7116, '甘肃公航旅高速公路运营管理平凉分公司', 620881, '华亭市', 1, '13800000000', 'blue', 'SLOPE', 'G8513', 106.58477, 35.17848, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8539, 'RP-HW-BRIDGE-005', 'G8513平绵高速平天段K176+953', 7118, '甘肃公航旅高速公路运营管理天水分公司', 620522, '秦安县', 1, '13800000000', 'blue', 'BRIDGE', 'G8513', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8540, 'RP-HW-SLOPE-032', 'G30连霍高速宝天段K1329+165', 7118, '甘肃公航旅高速公路运营管理天水分公司', 620503, '麦积区', 1, '13800000000', 'blue', 'SLOPE', 'G30', 106.13355, 34.35039, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8541, 'RP-HW-BRIDGE-006', 'G30连霍高速宝天段K1306+742', 7118, '甘肃公航旅高速公路运营管理天水分公司', 620503, '麦积区', 1, '13800000000', 'blue', 'BRIDGE', 'G30', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8542, 'RP-HW-BRIDGE-007', 'G30连霍高速宝天段K1310+835', 7118, '甘肃公航旅高速公路运营管理天水分公司', 620503, '麦积区', 1, '13800000000', 'blue', 'BRIDGE', 'G30', 106.31153, 34.318538, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8543, 'RP-HW-BRIDGE-008', 'G30连霍高速宝天段K1324+528', 7118, '甘肃公航旅高速公路运营管理天水分公司', 620503, '麦积区', 1, '13800000000', 'blue', 'BRIDGE', 'G30', 106.16548, 34.336527, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8544, 'RP-HW-BRIDGE-009', 'G30连霍高速宝天段K1289+900', 7118, '甘肃公航旅高速公路运营管理天水分公司', 620503, '麦积区', 1, '13800000000', 'blue', 'BRIDGE', 'G30', 106.51702, 34.336102, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8545, 'RP-HW-TUNNEL-004', 'G8513平绵高速成武段XK368+300-XK368+350', 7112, '成武所', 621202, '武都区', 1, '13800000000', 'blue', 'TUNNEL', 'G8513', 105.27762, 33.43379, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8546, 'RP-HW-TUNNEL-005', 'G8513平绵高速成武段XK385+260-XK385+360', 7112, '成武所', 621202, '武都区', 1, '13800000000', 'blue', 'TUNNEL', 'G8513', 105.10659, 33.467689, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8547, 'RP-HW-BRIDGE-010', 'G8513平绵高速成武段K396+935', 7112, '成武所', 621202, '武都区', 1, '13800000000', 'blue', 'BRIDGE', 'G8513', 105.01642, 33.488265, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8548, 'RP-HW-SLOPE-033', 'G75兰海高速兰临段K22+200-K22+400滑坡', 7114, '甘肃公航旅高速公路运营管理兰州分公司', 620102, '城关区', 1, '13800000000', 'blue', 'SLOPE', 'G75', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8549, 'RP-HW-SLOPE-034', 'G8513平绵高速成武段K374+960水毁', 7112, '成武所', 621202, '武都区', 1, '13800000000', 'blue', 'SLOPE', 'G8513', 105.21959, 33.436574, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8550, 'RP-HW-SLOPE-035', 'G7011十天高速K653+900', 7111, '成县所', 621225, '西和县', 1, '13800000000', 'blue', 'SLOPE', 'G7011', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8551, 'RP-HW-SLOPE-036', 'G568兰永一级K78+965-K79+140', 7114, '甘肃公航旅高速公路运营管理兰州分公司', 620102, '城关区', 1, '13800000000', 'blue', 'SLOPE', 'G568', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8552, 'RP-HW-SLOPE-037', 'G7011十天高速K646+945', 7111, '成县所', 621225, '西和县', 1, '13800000000', 'blue', 'SLOPE', 'G7011', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8553, 'RP-HW-SLOPE-038', 'G7011十天高速K583+445滑坡', 7111, '成县所', 621221, '成县', 1, '13800000000', 'blue', 'SLOPE', 'G7011', 105.77499, 33.741783, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8554, 'RP-HW-BRIDGE-011', 'G75 兰海高速武罐段K522+510水毁', 7113, '武都所', 621222, '文县', 1, '13800000000', 'blue', 'BRIDGE', 'G75', 105.41266, 32.762192, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8555, 'RP-HW-SLOPE-039', 'G6京藏高速兰海段K1652+225崩塌', 7114, '甘肃公航旅高速公路运营管理兰州分公司', 620104, '西固区', 1, '13800000000', 'blue', 'SLOPE', 'G6', 103.20301, 36.17702, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8556, 'RP-HW-SLOPE-040', 'G1816 乌玛高速康临段K745+950-K745+990隧道', 7115, '甘肃公航旅高速公路运营管理临夏分公司', 622925, '和政县', 1, '13800000000', 'blue', 'SLOPE', 'G1816', 103.32034, 35.452103, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8557, 'RP-HW-SLOPE-041', 'G1816乌玛高速康临段K753+240水毁', 7115, '甘肃公航旅高速公路运营管理临夏分公司', 622925, '和政县', 1, '13800000000', 'blue', 'SLOPE', 'G1816', 103.248, 35.481186, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8558, 'RP-HW-SLOPE-042', 'G22青兰高速平定段K1652+855水毁', 7119, '甘肃公航旅高速公路运营管理定西分公司', 621102, '安定区', 1, '13800000000', 'blue', 'SLOPE', 'G22', 105.78201, 35.546959, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8559, 'RP-HW-SLOPE-043', 'G8513平绵高速成武段K333+520', 7112, '成武所', 621202, '武都区', 1, '13800000000', 'blue', 'SLOPE', 'G8513', 105.46746, 33.639921, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8560, 'RP-HW-BRIDGE-012', 'G8513平绵高速成武段K401+694', 7112, '成武所', 621202, '武都区', 1, '13800000000', 'blue', 'BRIDGE', 'G8513', 104.95312, 33.462721, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8561, 'RP-HW-SLOPE-044', 'G30连霍高速宝天段K1323+880泥石流', 7118, '甘肃公航旅高速公路运营管理天水分公司', 620503, '麦积区', 1, '13800000000', 'blue', 'SLOPE', 'G30', 106.16524, 34.336093, 'G30 连霍高速宝鸡至天水段下行线 K1323+880 处，位于甘肃省天水市麦积区党川镇。下行线左侧与泥石流沟口基本呈正交关系，采用 2×2m 涵洞导排泥石流。', 0, 1, 1, NOW(), 1, NOW(), 0),
    (8562, 'RP-HW-SLOPE-045', 'G6京藏高速K1623+400滑坡', 7114, '甘肃公航旅高速公路运营管理兰州分公司', 620111, '红古区', 1, '13800000000', 'blue', 'SLOPE', 'G6', 103.8313, 36.066116, 'G6 京藏高速公路兰海段 K1623+300～K1623+400 段右侧边坡滑塌，位于甘肃省兰州市西固区河口乡。项目区地处陇西黄土高原向青藏高原过度带，为黄土峁梁沟壑相间地貌，山高坡陡，沟壑纵横，呈沟谷地形，沟谷深且多呈“V”字“W”字型断面，总体地破碎。坡体植被极为稀少，山顶多为黄土覆盖，下部多红泥岩出露。', 0, 1, 1, NOW(), 1, NOW(), 0),
    (8563, 'RP-HW-BRIDGE-013', 'G30连霍高速 K1731+077', 7114, '甘肃公航旅高速公路运营管理兰州分公司', 620121, '永登县', 1, '13800000000', 'blue', 'BRIDGE', 'G30', 103.59, 36.4, 'G30连霍高速K1731+077 匝道桥，位于甘肃省兰州市永登县树屏镇。 项目区地处陇西黄土高原向青藏高原过度带，为黄土峁梁沟壑相间地貌，沟壑纵横，呈沟谷地形，沟谷深且多呈“V”字“W”字型断面，总体地形破碎。坡体植被极为稀少，山顶多为黄土覆盖，下部多红泥岩出露。', 0, 1, 1, NOW(), 1, NOW(), 0),
    (8564, 'RP-HW-SLOPE-046', 'G30连霍高速K1328+500', 7118, '甘肃公航旅高速公路运营管理天水分公司', 620503, '麦积区', 1, '13800000000', 'blue', 'SLOPE', 'G30', 106.13379, 34.344941, 'G30 连霍高速公路宝天段 SK1328+800~SK1328+950 段右侧临河路堤水毁,位于甘肃省天水市麦积区党川乡。 路段位于两中山间的永宁河(党川河)河谷区，河床较开阔，为山区常年性河流，区内地形起伏不大，地面标高介于 1488.40~1595.10m，党川河两岸山体基岩露头较多， 山体坡度约 48°微地貌单元为山间河谷冲洪积及山间洪坡积裙地貌。', 0, 1, 1, NOW(), 1, NOW(), 0),
    (8565, 'RP-HW-SLOPE-047', 'G75 兰海高速K134+738边坡', 7119, '甘肃公航旅高速公路运营管理定西分公司', 621123, '渭源县', 1, '13800000000', 'blue', 'SLOPE', 'G75', 103.79258, 36.087916, 'G75 兰海高速K134+738 滑坡，位于甘肃省定西市渭源县清源镇，东北侧果园村申家山。 山脊走向近东西向，地貌属于低山丘陵地貌，斜坡高程在 2180~2330 之间，坡体下部平均坡度约 30°，中部平均坡度约 8°。', 0, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    risk_point_name = VALUES(risk_point_name),
    org_id = VALUES(org_id),
    org_name = VALUES(org_name),
    region_id = VALUES(region_id),
    region_name = VALUES(region_name),
    responsible_user = VALUES(responsible_user),
    responsible_phone = VALUES(responsible_phone),
    risk_level = VALUES(risk_level),
    risk_type = VALUES(risk_type),
    location_text = VALUES(location_text),
    longitude = VALUES(longitude),
    latitude = VALUES(latitude),
    description = VALUES(description),
    status = VALUES(status),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO risk_point_highway_detail (
    id, risk_point_id, project_name, project_type, project_summary, route_code, route_name, road_level, project_risk_level,
    admin_region_code, admin_region_path_json, maintenance_org_name, source_row_no, tenant_id, create_by, create_time, update_by, update_time, deleted
) VALUES
    (9501, 8501, 'G7011十天高速K595', '边坡', NULL, 'SXK595+818-SX595+960', '十天', '高速公路', NULL, '621221', '["62","6212","621221"]', '成县所', 2, 1, 1, NOW(), 1, NOW(), 0),
    (9502, 8502, 'G22青兰高速平定段K1458+75', '边坡', NULL, 'G22', '青兰高速', '高速公路', NULL, '621026', '["62","6210","621026"]', '甘肃公航旅高速公路运营管理平凉分公司', 3, 1, 1, NOW(), 1, NOW(), 0),
    (9503, 8503, 'G2012定武高速XK450+182', '边坡', NULL, 'G2012', '定武高速', '高速公路', NULL, '620622', '["62","6206","620622"]', '甘肃公航旅高速公路运营管理武威分公司', 4, 1, 1, NOW(), 1, NOW(), 0),
    (9504, 8504, 'G8513平绵高速成武段SK384+870-SK384+920', '隧道', NULL, 'G8513', '平绵高速', '高速公路', NULL, '621202', '["62","6212","621202"]', '成武所', 5, 1, 1, NOW(), 1, NOW(), 0),
    (9505, 8505, 'S38王夏高速K14+650', '边坡', NULL, 'S38', '王夏高速', '高速公路', NULL, NULL, NULL, '甘肃公航旅高速公路运营管理临夏分公司', 6, 1, 1, NOW(), 1, NOW(), 0),
    (9506, 8506, 'S32临大高速K10+176-K10+560', '边坡', NULL, 'S32', '临大高速', '高速公路', NULL, '622901', '["62","6229","622901"]', '甘肃公航旅高速公路运营管理临夏分公司', 7, 1, 1, NOW(), 1, NOW(), 0),
    (9507, 8507, 'G30连霍高速树徐段K1740+300', '边坡', NULL, 'G30', '连霍高速', '高速公路', NULL, NULL, NULL, '甘肃公航旅高速公路运营管理兰州分公司', 8, 1, 1, NOW(), 1, NOW(), 0),
    (9508, 8508, 'G30连霍高速柳忠段K1689+900-K1690+300', '边坡', NULL, 'G30', '连霍高速', '高速公路', NULL, NULL, NULL, '甘肃公航旅高速公路运营管理兰州分公司', 9, 1, 1, NOW(), 1, NOW(), 0),
    (9509, 8509, 'G6京藏高速刘白段K1414+770-K1415+550', '边坡', NULL, 'G6', '京藏高速', '高速公路', NULL, NULL, NULL, '甘肃公航旅高速公路运营管理兰州分公司', 10, 1, 1, NOW(), 1, NOW(), 0),
    (9510, 8510, 'G75兰海高速兰临段K14+325', '边坡', NULL, 'G75', '兰海高速', '高速公路', NULL, '620103', '["62","6201","620103"]', '甘肃公航旅高速公路运营管理兰州分公司', 11, 1, 1, NOW(), 1, NOW(), 0),
    (9511, 8511, 'G75兰海高速兰临段K11+325', '边坡', NULL, 'G75', '兰海高速', '高速公路', NULL, '620103', '["62","6201","620103"]', '甘肃公航旅高速公路运营管理兰州分公司', 12, 1, 1, NOW(), 1, NOW(), 0),
    (9512, 8512, 'G75兰海高速兰临段K14+149.5', '边坡', NULL, 'G75', '兰海高速', '高速公路', NULL, '620103', '["62","6201","620103"]', '甘肃公航旅高速公路运营管理兰州分公司', 13, 1, 1, NOW(), 1, NOW(), 0),
    (9513, 8513, 'G6 京藏高速兰海段K1661+350', '桥梁', NULL, 'G6', '京藏高速', '高速公路', NULL, '620111', '["62","6201","620111"]', '甘肃公航旅高速公路运营管理兰州分公司', 14, 1, 1, NOW(), 1, NOW(), 0),
    (9514, 8514, 'G6京藏高速白兰段K1548+810', '边坡', NULL, 'G6', '京藏高速', '高速公路', NULL, '620122', '["62","6201","620122"]', '甘肃公航旅高速公路运营管理兰州分公司', 15, 1, 1, NOW(), 1, NOW(), 0),
    (9515, 8515, 'G2201南绕城高速K39+800', '边坡', NULL, 'G2201', '南绕城高速', '高速公路', NULL, '620104', '["62","6201","620104"]', '甘肃公航旅高速公路运营管理兰州分公司', 16, 1, 1, NOW(), 1, NOW(), 0),
    (9516, 8516, 'G30连霍高速柳忠段K1695+500～K1699+800', '边坡', NULL, 'G30', '连霍高速', '高速公路', NULL, '620102', '["62","6201","620102"]', '甘肃公航旅高速公路运营管理兰州分公司', 17, 1, 1, NOW(), 1, NOW(), 0),
    (9517, 8517, 'G568兰永一级K75+253', '桥梁', NULL, 'G568', '兰永一级', '一级公路', NULL, '622923', '["62","6229","622923"]', '甘肃公航旅高速公路运营管理兰州分公司', 18, 1, 1, NOW(), 1, NOW(), 0),
    (9518, 8518, 'G3011柳格高速K74+830', '桥梁', 'G3011柳格高速K74+830', 'G3011', '柳格高速', '高速公路', NULL, '620201101', '["62","6202","620201101"]', '甘肃公航旅高速公路运营管理敦煌分公司', 19, 1, 1, NOW(), 1, NOW(), 0),
    (9519, 8519, 'G2012定武高速SK394+700～SK394+900', '边坡', NULL, 'G2012', '定武高速', '高速公路', NULL, '620602', '["62","6206","620602"]', '甘肃公航旅高速公路运营管理武威分公司', 20, 1, 1, NOW(), 1, NOW(), 0),
    (9520, 8520, 'G2012定武高速K393+170', '边坡', NULL, 'G2012', '定武高速', '高速公路', NULL, '620602', '["62","6206","620602"]', '甘肃公航旅高速公路运营管理武威分公司', 21, 1, 1, NOW(), 1, NOW(), 0),
    (9521, 8521, 'G2012定武高速K413+446', '边坡', NULL, 'G2012', '定武高速', '高速公路', NULL, '620602', '["62","6206","620602"]', '甘肃公航旅高速公路运营管理武威分公司', 22, 1, 1, NOW(), 1, NOW(), 0),
    (9522, 8522, 'G2012定武高速K400+315', '边坡', NULL, 'G2012', '定武高速', '高速公路', NULL, '620602', '["62","6206","620602"]', '甘肃公航旅高速公路运营管理武威分公司', 23, 1, 1, NOW(), 1, NOW(), 0),
    (9523, 8523, 'G2012定武高速K392+410', '边坡', NULL, 'G2012', '定武高速', '高速公路', NULL, '620602', '["62","6206","620602"]', '甘肃公航旅高速公路运营管理武威分公司', 24, 1, 1, NOW(), 1, NOW(), 0),
    (9524, 8524, 'G2012定武高速K392+790', '边坡', NULL, 'G2012', '定武高速', '高速公路', NULL, NULL, NULL, '甘肃公航旅高速公路运营管理武威分公司', 25, 1, 1, NOW(), 1, NOW(), 0),
    (9525, 8525, 'G2012定武高速K479+665', '边坡', NULL, 'G2012', '定武高速', '高速公路', NULL, NULL, NULL, '甘肃公航旅高速公路运营管理武威分公司', 26, 1, 1, NOW(), 1, NOW(), 0),
    (9526, 8526, 'G2012定武高速K465+175', '边坡', NULL, 'G2012', '定武高速', '高速公路', NULL, NULL, NULL, '甘肃公航旅高速公路运营管理武威分公司', 27, 1, 1, NOW(), 1, NOW(), 0),
    (9527, 8527, 'G2012定武高速K462+995', '边坡', NULL, 'G2012', '定武高速', '高速公路', NULL, NULL, NULL, '甘肃公航旅高速公路运营管理武威分公司', 28, 1, 1, NOW(), 1, NOW(), 0),
    (9528, 8528, 'G2012定武高速K420+891', '边坡', NULL, 'G2012', '定武高速', '高速公路', NULL, NULL, NULL, '甘肃公航旅高速公路运营管理武威分公司', 29, 1, 1, NOW(), 1, NOW(), 0),
    (9529, 8529, 'G2012定武高速K437+982', '边坡', NULL, 'G2012', '定武高速', '高速公路', NULL, NULL, NULL, '甘肃公航旅高速公路运营管理武威分公司', 30, 1, 1, NOW(), 1, NOW(), 0),
    (9530, 8530, 'G2012定武高速SK450+182', '边坡', NULL, 'G2012', '定武高速', '高速公路', NULL, NULL, NULL, '甘肃公航旅高速公路运营管理武威分公司', 31, 1, 1, NOW(), 1, NOW(), 0),
    (9531, 8531, 'G3017金武高速K67+050-K67+700', '边坡', NULL, 'G3017', '金武高速', '高速公路', NULL, NULL, NULL, '甘肃公航旅高速公路运营管理武威分公司', 32, 1, 1, NOW(), 1, NOW(), 0),
    (9532, 8532, 'G3017金武高速K68+860-K69+100', '边坡', NULL, 'G3017', '金武高速', '高速公路', NULL, NULL, NULL, '甘肃公航旅高速公路运营管理武威分公司', 33, 1, 1, NOW(), 1, NOW(), 0),
    (9533, 8533, 'G22青兰高速巉柳段SK1856+400-500桥梁', '桥梁', NULL, 'G22', '青兰高速', '高速公路', NULL, '620123', '["62","6201","620123"]', '甘肃公航旅高速公路运营管理定西分公司', 34, 1, 1, NOW(), 1, NOW(), 0),
    (9534, 8534, 'G22青兰高速巉柳段SK1839+928隧道', '隧道', NULL, 'G22', '青兰高速', '高速公路', NULL, '620123', '["62","6201","620123"]', '甘肃公航旅高速公路运营管理定西分公司', 35, 1, 1, NOW(), 1, NOW(), 0),
    (9535, 8535, 'G22青兰高速巉柳段K1858+250沉陷', '边坡', NULL, 'G22', '青兰高速', '高速公路', NULL, '620123', '["62","6201","620123"]', '甘肃公航旅高速公路运营管理定西分公司', 36, 1, 1, NOW(), 1, NOW(), 0),
    (9536, 8536, 'G22青兰高速巉柳段K1855+575沉陷', '边坡', NULL, 'G22', '青兰高速', '高速公路', NULL, '620123', '["62","6201","620123"]', '甘肃公航旅高速公路运营管理定西分公司', 37, 1, 1, NOW(), 1, NOW(), 0),
    (9537, 8537, 'G22青兰高速雷西段K1374+670-K1374+780', '隧道', '棚洞设计标准受控于庆城隧道设计。隧道设计车速80km/h，按分离式设计，上、下行间距为36m 左右，每幅为单向双车道，棚洞采用变截面拱门式侧墙，侧墙内轮廓半径 5.43m，侧墙距电缆槽顶面高度 3.1m，采用钢筋混凝土扩大基础，横向设 C25 钢筋混凝土支撑梁，4m 间距设一道，横向支撑梁与侧墙混凝土一体浇筑完成。棚洞拱顶采用 I18 钢拱架为骨架，纵向间距为 3.0m，钢拱 架上沿纵向铺设[10 槽钢，槽钢与钢拱架焊接，顶部铺设蓝色遮光板，采用螺栓与槽钢连接。棚洞高填方路基下设钢波纹管涵，将沟内汇水排至下游沟谷。          经调查，上下行线棚洞病害情况相近，主要病害为棚洞拱墙开裂、局部混凝土剥落，裂缝最大 宽度达 2.2cm，个别部位拱圈整体剥落，拱墙大面积渗水、泛碱严重，涂装剥蚀泛碱严重。', 'G22', '青兰高速', '高速公路', NULL, '621021', '["62","6210","621021"]', '甘肃公航旅高速公路运营管理平凉分公司', 38, 1, 1, NOW(), 1, NOW(), 0),
    (9538, 8538, 'G8513平绵高速平天段YK54+925', '边坡', NULL, 'G8513', '平绵高速', '高速公路', NULL, '620881', '["62","6208","620881"]', '甘肃公航旅高速公路运营管理平凉分公司', 39, 1, 1, NOW(), 1, NOW(), 0),
    (9539, 8539, 'G8513平绵高速平天段K176+953', '桥梁', NULL, 'G8513', '平绵高速', '高速公路', NULL, '620522', '["62","6205","620522"]', '甘肃公航旅高速公路运营管理天水分公司', 40, 1, 1, NOW(), 1, NOW(), 0),
    (9540, 8540, 'G30连霍高速宝天段K1329+165', '边坡', NULL, 'G30', '连霍高速', '高速公路', NULL, '620503', '["62","6205","620503"]', '甘肃公航旅高速公路运营管理天水分公司', 41, 1, 1, NOW(), 1, NOW(), 0),
    (9541, 8541, 'G30连霍高速宝天段K1306+742', '桥梁', NULL, 'G30', '连霍高速', '高速公路', NULL, '620503', '["62","6205","620503"]', '甘肃公航旅高速公路运营管理天水分公司', 42, 1, 1, NOW(), 1, NOW(), 0),
    (9542, 8542, 'G30连霍高速宝天段K1310+835', '桥梁', NULL, 'G30', '连霍高速', '高速公路', NULL, '620503', '["62","6205","620503"]', '甘肃公航旅高速公路运营管理天水分公司', 43, 1, 1, NOW(), 1, NOW(), 0),
    (9543, 8543, 'G30连霍高速宝天段K1324+528', '桥梁', NULL, 'G30', '连霍高速', '高速公路', NULL, '620503', '["62","6205","620503"]', '甘肃公航旅高速公路运营管理天水分公司', 44, 1, 1, NOW(), 1, NOW(), 0),
    (9544, 8544, 'G30连霍高速宝天段K1289+900', '桥梁', NULL, 'G30', '连霍高速', '高速公路', NULL, '620503', '["62","6205","620503"]', '甘肃公航旅高速公路运营管理天水分公司', 45, 1, 1, NOW(), 1, NOW(), 0),
    (9545, 8545, 'G8513平绵高速成武段XK368+300-XK368+350', '隧道', NULL, 'G8513', '平绵高速', '高速公路', NULL, '621202', '["62","6212","621202"]', '成武所', 46, 1, 1, NOW(), 1, NOW(), 0),
    (9546, 8546, 'G8513平绵高速成武段XK385+260-XK385+360', '隧道', NULL, 'G8513', '平绵高速', '高速公路', NULL, '621202', '["62","6212","621202"]', '成武所', 47, 1, 1, NOW(), 1, NOW(), 0),
    (9547, 8547, 'G8513平绵高速成武段K396+935', '桥梁', NULL, 'G8513', '平绵高速', '高速公路', NULL, '621202', '["62","6212","621202"]', '成武所', 48, 1, 1, NOW(), 1, NOW(), 0),
    (9548, 8548, 'G75兰海高速兰临段K22+200-K22+400滑坡', '边坡', NULL, 'G75', '兰海高速', '高速公路', NULL, '620102', '["62","6201","620102"]', '甘肃公航旅高速公路运营管理兰州分公司', 49, 1, 1, NOW(), 1, NOW(), 0),
    (9549, 8549, 'G8513平绵高速成武段K374+960水毁', '边坡', NULL, 'G8513', '平绵高速', '高速公路', NULL, '621202', '["62","6212","621202"]', '成武所', 50, 1, 1, NOW(), 1, NOW(), 0),
    (9550, 8550, 'G7011十天高速K653+900', '边坡', NULL, 'G7011', '十天高速', '高速公路', NULL, '621225', '["62","6212","621225"]', '成县所', 51, 1, 1, NOW(), 1, NOW(), 0),
    (9551, 8551, 'G568兰永一级K78+965-K79+140', '边坡', NULL, 'G568', '兰永一级', '一级公路', NULL, '620102', '["62","6201","620102"]', '甘肃公航旅高速公路运营管理兰州分公司', 52, 1, 1, NOW(), 1, NOW(), 0),
    (9552, 8552, 'G7011十天高速K646+945', '边坡', NULL, 'G7011', '十天高速', '高速公路', NULL, '621225', '["62","6212","621225"]', '成县所', 53, 1, 1, NOW(), 1, NOW(), 0),
    (9553, 8553, 'G7011十天高速K583+445滑坡', '边坡', NULL, 'G7011', '十天高速', '高速公路', NULL, '621221', '["62","6212","621221"]', '成县所', 54, 1, 1, NOW(), 1, NOW(), 0),
    (9554, 8554, 'G75 兰海高速武罐段K522+510水毁', '桥梁', NULL, 'G75', '兰海高速', '高速公路', NULL, '621222', '["62","6212","621222"]', '武都所', 55, 1, 1, NOW(), 1, NOW(), 0),
    (9555, 8555, 'G6京藏高速兰海段K1652+225崩塌', '边坡', NULL, 'G6', '京藏高速', '一级公路', NULL, '620104', '["62","6201","620104"]', '甘肃公航旅高速公路运营管理兰州分公司', 56, 1, 1, NOW(), 1, NOW(), 0),
    (9556, 8556, 'G1816 乌玛高速康临段K745+950-K745+990隧道', '边坡', NULL, 'G1816', '乌玛高速', '高速公路', NULL, '622925', '["62","6229","622925"]', '甘肃公航旅高速公路运营管理临夏分公司', 57, 1, 1, NOW(), 1, NOW(), 0),
    (9557, 8557, 'G1816乌玛高速康临段K753+240水毁', '边坡', NULL, 'G1816', '乌玛高速', '高速公路', NULL, '622925', '["62","6229","622925"]', '甘肃公航旅高速公路运营管理临夏分公司', 58, 1, 1, NOW(), 1, NOW(), 0),
    (9558, 8558, 'G22青兰高速平定段K1652+855水毁', '边坡', NULL, 'G22', '青兰高速', '高速公路', NULL, '621102', '["62","6211","621102"]', '甘肃公航旅高速公路运营管理定西分公司', 59, 1, 1, NOW(), 1, NOW(), 0),
    (9559, 8559, 'G8513平绵高速成武段K333+520', '边坡', NULL, 'G8513', '平绵高速', '高速公路', NULL, '621202', '["62","6212","621202"]', '成武所', 60, 1, 1, NOW(), 1, NOW(), 0),
    (9560, 8560, 'G8513平绵高速成武段K401+694', '桥梁', NULL, 'G8513', '平绵高速', '高速公路', NULL, '621202', '["62","6212","621202"]', '成武所', 61, 1, 1, NOW(), 1, NOW(), 0),
    (9561, 8561, 'G30连霍高速宝天段K1323+880泥石流', '边坡', 'G30 连霍高速宝鸡至天水段下行线 K1323+880 处，位于甘肃省天水市麦积区党川镇。下行线左侧与泥石流沟口基本呈正交关系，采用 2×2m 涵洞导排泥石流。', 'G30', '连霍高速', '高速公路', NULL, '620503', '["62","6205","620503"]', '甘肃公航旅高速公路运营管理天水分公司', 62, 1, 1, NOW(), 1, NOW(), 0),
    (9562, 8562, 'G6京藏高速K1623+400滑坡', '边坡', 'G6 京藏高速公路兰海段 K1623+300～K1623+400 段右侧边坡滑塌，位于甘肃省兰州市西固区河口乡。项目区地处陇西黄土高原向青藏高原过度带，为黄土峁梁沟壑相间地貌，山高坡陡，沟壑纵横，呈沟谷地形，沟谷深且多呈“V”字“W”字型断面，总体地破碎。坡体植被极为稀少，山顶多为黄土覆盖，下部多红泥岩出露。', 'G6', '京藏高速', '高速公路', NULL, '620111', '["62","6201","620111"]', '甘肃公航旅高速公路运营管理兰州分公司', 63, 1, 1, NOW(), 1, NOW(), 0),
    (9563, 8563, 'G30连霍高速 K1731+077', '桥梁', 'G30连霍高速K1731+077 匝道桥，位于甘肃省兰州市永登县树屏镇。 项目区地处陇西黄土高原向青藏高原过度带，为黄土峁梁沟壑相间地貌，沟壑纵横，呈沟谷地形，沟谷深且多呈“V”字“W”字型断面，总体地形破碎。坡体植被极为稀少，山顶多为黄土覆盖，下部多红泥岩出露。', 'G30', '连霍高速', '高速公路', NULL, '620121', '["62","6201","620121"]', '甘肃公航旅高速公路运营管理兰州分公司', 64, 1, 1, NOW(), 1, NOW(), 0),
    (9564, 8564, 'G30连霍高速K1328+500', '边坡', 'G30 连霍高速公路宝天段 SK1328+800~SK1328+950 段右侧临河路堤水毁,位于甘肃省天水市麦积区党川乡。 路段位于两中山间的永宁河(党川河)河谷区，河床较开阔，为山区常年性河流，区内地形起伏不大，地面标高介于 1488.40~1595.10m，党川河两岸山体基岩露头较多， 山体坡度约 48°微地貌单元为山间河谷冲洪积及山间洪坡积裙地貌。', 'G30', '连霍高速', '高速公路', NULL, '620503', '["62","6205","620503"]', '甘肃公航旅高速公路运营管理天水分公司', 65, 1, 1, NOW(), 1, NOW(), 0),
    (9565, 8565, 'G75 兰海高速K134+738边坡', '边坡', 'G75 兰海高速K134+738 滑坡，位于甘肃省定西市渭源县清源镇，东北侧果园村申家山。 山脊走向近东西向，地貌属于低山丘陵地貌，斜坡高程在 2180~2330 之间，坡体下部平均坡度约 30°，中部平均坡度约 8°。', 'G75', '兰海高速', '高速公路', NULL, '621123', '["62","6211","621123"]', '甘肃公航旅高速公路运营管理定西分公司', 66, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    project_name = VALUES(project_name),
    project_type = VALUES(project_type),
    project_summary = VALUES(project_summary),
    route_code = VALUES(route_code),
    route_name = VALUES(route_name),
    road_level = VALUES(road_level),
    project_risk_level = VALUES(project_risk_level),
    admin_region_code = VALUES(admin_region_code),
    admin_region_path_json = VALUES(admin_region_path_json),
    maintenance_org_name = VALUES(maintenance_org_name),
    source_row_no = VALUES(source_row_no),
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
    (8201, '锅炉温度超限', 'temperature', '温度', 'value > 80', 60, 'red', 'email,webhook', 1, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8202, '设备振动超限', 'vibration', '振动', 'value > 10', 120, 'orange', 'webhook', 1, 0, 1, 1, NOW(), 1, NOW(), 0)
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
    id, plan_name, alarm_level, risk_level, description, response_steps, contact_list, status, tenant_id,
    create_by, create_time, update_by, update_time, deleted
) VALUES
    (8401, '锅炉超温应急预案', 'red', 'red', '锅炉区域出现超温告警时执行',
     JSON_ARRAY('确认现场状态', '远程降载', '派发现场工单', '复盘关闭事件'),
     JSON_ARRAY(JSON_OBJECT('name', '值班长', 'phone', '13800000000'), JSON_OBJECT('name', '安全员', 'phone', '13800000001')),
     0, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    plan_name = VALUES(plan_name),
    alarm_level = VALUES(alarm_level),
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
    (8501, 'ALARM-20260317001', '锅炉温度超限', 'threshold', 'red',
     7002, '黄浦厂区', 8001, '锅炉温压监测点',
     2001, 'accept-http-device-01', '验收设备-HTTP-01', 'temperature', '92.4', '80',
     0, DATE_SUB(NOW(), INTERVAL 30 MINUTE), NULL, NULL, NULL, NULL, NULL, NULL,
     8201, '锅炉温度超限', 1, '待确认告警', 1, NOW(), 1, NOW(), 0),
    (8502, 'ALARM-20260317002', '设备振动异常', 'threshold', 'orange',
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
    (8601, 'EVENT-20260317001', '锅炉超温处置事件', 8501, 'ALARM-20260317001', 'red', 'red',
     7002, '黄浦厂区', 8001, '锅炉温压监测点',
     2001, 'accept-http-device-01', '验收设备-HTTP-01', 'temperature', '92.4',
     2, 1, 'high', 15, 120,
     DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 25 MINUTE), 1, DATE_SUB(NOW(), INTERVAL 20 MINUTE), NULL, NULL, NULL, NULL, '处理中',
     1, '进行中事件', 1, NOW(), 1, NOW(), 0),
    (8602, 'EVENT-20260317002', '振动异常复盘事件', 8502, 'ALARM-20260317002', 'orange', 'orange',
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


INSERT INTO `iot_product` (
    `id`,
    `tenant_id`,
    `product_key`,
    `product_name`,
    `protocol_code`,
    `node_type`,
    `data_format`,
    `manufacturer`,
    `description`,
    `status`,
    `remark`,
    `create_by`,
    `update_by`,
    `deleted`
) VALUES
      (202603192100560271, 1, 'zhd-warning-sound-light-alarm-v1', '中海达 预警型 声光报警器', 'mqtt-json', 1, 'JSON', '中海达', '预警型声光报警设备，协议 mqtt-json，直连接入', 1, '原始 productKey: hitarget_sound_light_alarm', NULL, NULL, 0),
      (202603192100560270, 1, 'zjhy-warning-sound-light-alarm-v1', '浙江华源 预警型 声光报警器', 'mqtt-json', 1, 'JSON', '浙江华源', '预警型声光报警设备，协议 mqtt-json，直连接入', 1, '原始 productKey: zjhy_sound_light_alarm', NULL, NULL, 0),
      (202603192100560259, 1, 'nf-collect-rtu-v1', '南方测绘 采集型 遥测终端', 'mqtt-json', 1, 'JSON', '南方测绘', '采集型遥测终端设备，协议 mqtt-json，直连接入', 1, '原始 productKey: south_rtu', NULL, NULL, 0),
      (202603192100560258, 1, 'nf-monitor-laser-rangefinder-v1', '南方测绘 监测型 激光测距仪', 'mqtt-json', 1, 'JSON', '南方测绘', '监测型激光测距设备，协议 mqtt-json，直连接入', 1, '原始 productKey: south_laser_rangefinder', NULL, NULL, 0),
      (202603192100560257, 1, 'zhd-monitor-tiltmeter-v1', '中海达 监测型 倾角仪', 'mqtt-json', 1, 'JSON', '中海达', '监测型倾角设备，协议 mqtt-json，直连接入', 1, '原始 productKey: hitarget_tiltmeter', NULL, NULL, 0),
      (202603192100560255, 1, 'nf-monitor-crack-meter-v1', '南方测绘 监测型 裂缝计', 'mqtt-json', 1, 'JSON', '南方测绘', '监测型裂缝监测设备，协议 mqtt-json，直连接入', 1, '原始 productKey: south_crack_meter', NULL, NULL, 0),
      (202603192100560254, 1, 'nf-monitor-mud-level-meter-v1', '南方测绘 监测型 泥位计', 'mqtt-json', 1, 'JSON', '南方测绘', '监测型泥位监测设备，协议 mqtt-json，直连接入', 1, '原始 productKey: south_mud_level_meter', NULL, NULL, 0),
      (202603192100560253, 1, 'nf-monitor-tipping-bucket-rain-gauge-v1', '南方测绘 监测型 翻斗式雨量计', 'mqtt-json', 1, 'JSON', '南方测绘', '监测型翻斗式雨量监测设备，协议 mqtt-json，直连接入', 1, '原始 productKey: south_rain_gauge', NULL, NULL, 0),
      (202603192100560252, 1, 'zhd-monitor-multi-displacement-v1', '中海达 监测型 多维位移监测仪', 'mqtt-json', 1, 'JSON', '中海达', '监测型多维位移监测设备，协议 mqtt-json，直连接入', 1, '原始 productKey: hitarget_multi_displacement', NULL, NULL, 0),
      (202603192100560251, 1, 'nf-monitor-multi-displacement-v1', '南方测绘 监测型 多维位移监测仪', 'mqtt-json', 1, 'JSON', '南方测绘', '监测型多维位移监测设备，协议 mqtt-json，直连接入', 1, '原始 productKey: south_multi_displacement', NULL, NULL, 0),
      (202603192100560250, 1, 'nf-monitor-deep-displacement-v1', '南方测绘 监测型 深部位移监测仪', 'mqtt-json', 1, 'JSON', '南方测绘', '监测型深部位移监测设备，协议 mqtt-json，直连接入', 1, '原始 productKey: south_deep_displacement', NULL, NULL, 0),
      (202603192100560249, 1, 'zhd-monitor-gnss-base-station-v1', '中海达 监测型 GNSS基准站', 'mqtt-json', 1, 'JSON', '中海达', '监测型 GNSS 基准站设备，协议 mqtt-json，直连接入', 1, '原始 productKey: hitarget_gnss_base_station', NULL, NULL, 0),
      (202603192100560248, 1, 'nf-monitor-gnss-base-station-v1', '南方测绘 监测型 GNSS基准站', 'mqtt-json', 1, 'JSON', '南方测绘', '监测型 GNSS 基准站设备，协议 mqtt-json，直连接入', 1, '原始 productKey: south_gnss_base_station', NULL, NULL, 0),
      (202603192100560247, 1, 'zhd-monitor-gnss-monitor-v1', '中海达 监测型 GNSS位移监测仪', 'mqtt-json', 1, 'JSON', '中海达', '监测型 GNSS 位移监测设备，协议 mqtt-json，直连接入', 1, '原始 productKey: hitarget_gnss_monitor', NULL, NULL, 0),
      (202603192100560246, 1, 'nf-monitor-gnss-monitor-v1', '南方测绘 监测型 GNSS位移监测仪', 'mqtt-json', 1, 'JSON', '南方测绘', '监测型 GNSS 位移监测设备，协议 mqtt-json，直连接入', 1, '原始 productKey: south_gnss_monitor', NULL, NULL, 0);

-- collector-child 共享 dev 基线：采集器 / 激光测距 / 深部位移 / 单台深部位移
INSERT INTO iot_product_model (
    id, tenant_id, product_id, model_type, identifier, model_name, data_type, specs_json,
    sort_no, required_flag, description, create_time, update_time, deleted
) VALUES
    (202604110200001, 1, 202603192100560259, 'property', 'temp', '温度', 'double', JSON_OBJECT('unit', '℃', 'category', 'collector_status'), 1, 0, '采集器运行温度', NOW(), NOW(), 0),
    (202604110200002, 1, 202603192100560259, 'property', 'humidity', '湿度', 'double', JSON_OBJECT('unit', '%', 'category', 'collector_status'), 2, 0, '采集器运行湿度', NOW(), NOW(), 0),
    (202604110200003, 1, 202603192100560259, 'property', 'signal_4g', '4G信号强度', 'int', JSON_OBJECT('unit', 'dBm', 'category', 'collector_status'), 3, 0, '采集器 4G 信号强度', NOW(), NOW(), 0),
    (202604110200011, 1, 202603192100560258, 'property', 'value', '激光测距值', 'double', JSON_OBJECT('unit', 'mm', 'precision', 4), 1, 0, '激光测距监测值', NOW(), NOW(), 0),
    (202604110200012, 1, 202603192100560258, 'property', 'sensor_state', '传感器状态', 'int', JSON_OBJECT('category', 'state'), 2, 0, '激光测距传感器状态', NOW(), NOW(), 0),
    (202604110200021, 1, 202603192100560250, 'property', 'dispsX', '顺滑动方向累计变形量', 'double', JSON_OBJECT('unit', 'mm', 'precision', 4), 1, 0, '深部位移顺滑动方向累计变形量', NOW(), NOW(), 0),
    (202604110200022, 1, 202603192100560250, 'property', 'dispsY', '垂直坡面方向累计变形量', 'double', JSON_OBJECT('unit', 'mm', 'precision', 4), 2, 0, '深部位移垂直坡面方向累计变形量', NOW(), NOW(), 0),
    (202604110200023, 1, 202603192100560250, 'property', 'sensor_state', '传感器状态', 'int', JSON_OBJECT('category', 'state'), 3, 0, '深部位移传感器状态', NOW(), NOW(), 0),
    (202604110200031, 1, 202603192100560253, 'property', 'value', '当前雨量', 'double', JSON_OBJECT('unit', 'mm', 'precision', 2), 1, 0, '翻斗式雨量计当前雨量', NOW(), NOW(), 0),
    (202604110200032, 1, 202603192100560253, 'property', 'totalValue', '累计雨量', 'double', JSON_OBJECT('unit', 'mm', 'precision', 2), 2, 0, '翻斗式雨量计累计雨量', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    model_name = VALUES(model_name),
    data_type = VALUES(data_type),
    specs_json = VALUES(specs_json),
    sort_no = VALUES(sort_no),
    required_flag = VALUES(required_flag),
    description = VALUES(description),
    update_time = NOW(),
    deleted = 0;

UPDATE iot_product
SET metadata_json = JSON_SET(
        COALESCE(metadata_json, JSON_OBJECT()),
        '$.objectInsight',
        JSON_OBJECT(
            'customMetrics',
            JSON_ARRAY(
                JSON_OBJECT('identifier', 'temp', 'displayName', '温度', 'enabled', TRUE, 'includeInTrend', TRUE, 'includeInExtension', TRUE, 'sortNo', 10),
                JSON_OBJECT('identifier', 'humidity', 'displayName', '湿度', 'enabled', TRUE, 'includeInTrend', TRUE, 'includeInExtension', TRUE, 'sortNo', 20),
                JSON_OBJECT('identifier', 'signal_4g', 'displayName', '4G信号强度', 'enabled', TRUE, 'includeInTrend', FALSE, 'includeInExtension', TRUE, 'sortNo', 30)
            )
        )
    ),
    update_by = 1,
    update_time = NOW(),
    deleted = 0
WHERE tenant_id = 1
  AND product_key = 'nf-collect-rtu-v1';

UPDATE iot_product
SET metadata_json = JSON_SET(
        COALESCE(metadata_json, JSON_OBJECT()),
        '$.objectInsight',
        JSON_OBJECT(
            'customMetrics',
            JSON_ARRAY(
                JSON_OBJECT('identifier', 'value', 'displayName', '激光测距值', 'enabled', TRUE, 'includeInTrend', TRUE, 'includeInExtension', TRUE, 'sortNo', 10),
                JSON_OBJECT('identifier', 'sensor_state', 'displayName', '传感器状态', 'enabled', TRUE, 'includeInTrend', TRUE, 'includeInExtension', TRUE, 'sortNo', 20)
            )
        )
    ),
    update_by = 1,
    update_time = NOW(),
    deleted = 0
WHERE tenant_id = 1
  AND product_key = 'nf-monitor-laser-rangefinder-v1';

UPDATE iot_product
SET metadata_json = JSON_SET(
        COALESCE(metadata_json, JSON_OBJECT()),
        '$.objectInsight',
        JSON_OBJECT(
            'customMetrics',
            JSON_ARRAY(
                JSON_OBJECT('identifier', 'dispsX', 'displayName', '顺滑动方向累计变形量', 'enabled', TRUE, 'includeInTrend', TRUE, 'includeInExtension', TRUE, 'sortNo', 10),
                JSON_OBJECT('identifier', 'dispsY', 'displayName', '垂直坡面方向累计变形量', 'enabled', TRUE, 'includeInTrend', TRUE, 'includeInExtension', TRUE, 'sortNo', 20),
                JSON_OBJECT('identifier', 'sensor_state', 'displayName', '传感器状态', 'enabled', TRUE, 'includeInTrend', TRUE, 'includeInExtension', TRUE, 'sortNo', 30)
            )
        )
    ),
    update_by = 1,
    update_time = NOW(),
    deleted = 0
WHERE tenant_id = 1
  AND product_key = 'nf-monitor-deep-displacement-v1';

UPDATE iot_product
SET metadata_json = JSON_SET(
        COALESCE(metadata_json, JSON_OBJECT()),
        '$.objectInsight',
        JSON_OBJECT(
            'customMetrics',
            JSON_ARRAY(
                JSON_OBJECT('identifier', 'value', 'displayName', '当前雨量', 'enabled', TRUE, 'includeInTrend', TRUE, 'includeInExtension', TRUE, 'sortNo', 10),
                JSON_OBJECT('identifier', 'totalValue', 'displayName', '累计雨量', 'enabled', TRUE, 'includeInTrend', TRUE, 'includeInExtension', TRUE, 'sortNo', 20)
            )
        )
    ),
    update_by = 1,
    update_time = NOW(),
    deleted = 0
WHERE tenant_id = 1
  AND product_key = 'nf-monitor-tipping-bucket-rain-gauge-v1';

INSERT INTO iot_vendor_metric_mapping_rule (
    id, tenant_id, scope_type, product_id, protocol_code, scenario_code, device_family,
    raw_identifier, logical_channel_code, relation_condition_json, normalization_rule_json,
    target_normative_identifier, status, version_no, approval_order_id,
    create_by, create_time, update_by, update_time, deleted
) VALUES
    (202604110800001, 1, 'PRODUCT', 202603192100560253, 'mqtt-json', 'phase4-rain-gauge', 'RAIN_GAUGE',
     'L3_YL_1.value', 'L3_YL_1', NULL, NULL, 'value', 'ACTIVE', 1, NULL, 1, NOW(), 1, NOW(), 0),
    (202604110800002, 1, 'PRODUCT', 202603192100560253, 'mqtt-json', 'phase4-rain-gauge', 'RAIN_GAUGE',
     'L3_YL_1.totalValue', 'L3_YL_1', NULL, NULL, 'totalValue', 'ACTIVE', 1, NULL, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    product_id = VALUES(product_id),
    protocol_code = VALUES(protocol_code),
    scenario_code = VALUES(scenario_code),
    device_family = VALUES(device_family),
    raw_identifier = VALUES(raw_identifier),
    logical_channel_code = VALUES(logical_channel_code),
    relation_condition_json = VALUES(relation_condition_json),
    normalization_rule_json = VALUES(normalization_rule_json),
    target_normative_identifier = VALUES(target_normative_identifier),
    status = VALUES(status),
    version_no = VALUES(version_no),
    approval_order_id = VALUES(approval_order_id),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO iot_device (
    id, tenant_id, org_id, org_name, product_id, device_name, device_code, device_secret, client_id, username, password,
    protocol_code, node_type, online_status, activate_status, device_status, firmware_version,
    ip_address, last_online_time, last_report_time, longitude, latitude, address, metadata_json,
    remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (202604110300001, 1, 7101, '平台运维中心', 202603192100560259, 'NF-COLLECTOR-SK00EA0D1307988', 'SK00EA0D1307988', '123456', 'SK00EA0D1307988', 'SK00EA0D1307988', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser', JSON_OBJECT('seedFamily', 'laser-collector'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0),
    (202604110300002, 1, 7101, '平台运维中心', 202603192100560259, 'NF-COLLECTOR-SK00EA0D1307967', 'SK00EA0D1307967', '123456', 'SK00EA0D1307967', 'SK00EA0D1307967', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser', JSON_OBJECT('seedFamily', 'laser-collector'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0),
    (202604110300003, 1, 7101, '平台运维中心', 202603192100560259, 'NF-COLLECTOR-SK00EA0D1307986', 'SK00EA0D1307986', '123456', 'SK00EA0D1307986', 'SK00EA0D1307986', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser', JSON_OBJECT('seedFamily', 'laser-collector'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0),
    (202604110300004, 1, 7101, '平台运维中心', 202603192100560259, 'NF-COLLECTOR-SK00EA0D1307987', 'SK00EA0D1307987', '123456', 'SK00EA0D1307987', 'SK00EA0D1307987', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser', JSON_OBJECT('seedFamily', 'laser-collector'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0),
    (202604110300005, 1, 7101, '平台运维中心', 202603192100560259, 'NF-COLLECTOR-SK00EA0D1308009', 'SK00EA0D1308009', '123456', 'SK00EA0D1308009', 'SK00EA0D1308009', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser', JSON_OBJECT('seedFamily', 'laser-collector'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0),
    (202604110300006, 1, 7101, '平台运维中心', 202603192100560259, 'NF-COLLECTOR-SK00EA0D1307984', 'SK00EA0D1307984', '123456', 'SK00EA0D1307984', 'SK00EA0D1307984', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser', JSON_OBJECT('seedFamily', 'laser-collector'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0),
    (202604110300007, 1, 7101, '平台运维中心', 202603192100560259, 'NF-COLLECTOR-SK00EA0D1308006', 'SK00EA0D1308006', '123456', 'SK00EA0D1308006', 'SK00EA0D1308006', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser', JSON_OBJECT('seedFamily', 'laser-collector'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0),
    (202604110300008, 1, 7101, '平台运维中心', 202603192100560259, 'NF-COLLECTOR-SK00EA0D1307968', 'SK00EA0D1307968', '123456', 'SK00EA0D1307968', 'SK00EA0D1307968', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser', JSON_OBJECT('seedFamily', 'laser-collector'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0),
    (202604110300009, 1, 7101, '平台运维中心', 202603192100560259, 'NF-COLLECTOR-SK00E90D1307874', 'SK00E90D1307874', '123456', 'SK00E90D1307874', 'SK00E90D1307874', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser', JSON_OBJECT('seedFamily', 'laser-collector'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0),
    (202604110300010, 1, 7101, '平台运维中心', 202603192100560259, 'NF-COLLECTOR-SK00EA0D1307992', 'SK00EA0D1307992', '123456', 'SK00EA0D1307992', 'SK00EA0D1307992', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser', JSON_OBJECT('seedFamily', 'laser-collector'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0),
    (202604110300101, 1, 7101, '平台运维中心', 202603192100560259, 'NF-COLLECTOR-SK00FB0D1310195', 'SK00FB0D1310195', '123456', 'SK00FB0D1310195', 'SK00FB0D1310195', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep', JSON_OBJECT('seedFamily', 'deep-collector'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0),
    (202604110300102, 1, 7101, '平台运维中心', 202603192100560259, 'NF-COLLECTOR-SK00FB0D1310216', 'SK00FB0D1310216', '123456', 'SK00FB0D1310216', 'SK00FB0D1310216', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep', JSON_OBJECT('seedFamily', 'deep-collector'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0),
    (202604110300103, 1, 7101, '平台运维中心', 202603192100560259, 'NF-COLLECTOR-SK00FB0D1310215', 'SK00FB0D1310215', '123456', 'SK00FB0D1310215', 'SK00FB0D1310215', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep', JSON_OBJECT('seedFamily', 'deep-collector'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0),
    (202604110300104, 1, 7101, '平台运维中心', 202603192100560259, 'NF-COLLECTOR-SK00FB0D1310000', 'SK00FB0D1310000', '123456', 'SK00FB0D1310000', 'SK00FB0D1310000', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep', JSON_OBJECT('seedFamily', 'deep-collector'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0),
    (202604110300201, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-SINGLE-SK00EB0D1308310', 'SK00EB0D1308310', '123456', 'SK00EB0D1308310', 'SK00EB0D1308310', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-single', JSON_OBJECT('seedFamily', 'deep-single'), 'collector child shared dev baseline', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    org_id = VALUES(org_id),
    org_name = VALUES(org_name),
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

INSERT INTO iot_device (
    id, tenant_id, org_id, org_name, product_id, device_name, device_code, device_secret, client_id, username, password,
    protocol_code, node_type, online_status, activate_status, device_status, firmware_version,
    ip_address, last_online_time, last_report_time, longitude, latitude, address, metadata_json,
    remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (202018108, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018108', '202018108', '123456', '202018108', '202018108', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018109, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018109', '202018109', '123456', '202018109', '202018109', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018110, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018110', '202018110', '123456', '202018110', '202018110', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018111, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018111', '202018111', '123456', '202018111', '202018111', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018112, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018112', '202018112', '123456', '202018112', '202018112', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018113, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018113', '202018113', '123456', '202018113', '202018113', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018114, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018114', '202018114', '123456', '202018114', '202018114', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018115, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018115', '202018115', '123456', '202018115', '202018115', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018116, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018116', '202018116', '123456', '202018116', '202018116', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018117, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018117', '202018117', '123456', '202018117', '202018117', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018118, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018118', '202018118', '123456', '202018118', '202018118', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018119, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018119', '202018119', '123456', '202018119', '202018119', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018120, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018120', '202018120', '123456', '202018120', '202018120', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018121, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018121', '202018121', '123456', '202018121', '202018121', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018122, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018122', '202018122', '123456', '202018122', '202018122', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018123, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018123', '202018123', '123456', '202018123', '202018123', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018124, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018124', '202018124', '123456', '202018124', '202018124', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018125, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018125', '202018125', '123456', '202018125', '202018125', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018126, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018126', '202018126', '123456', '202018126', '202018126', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018127, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018127', '202018127', '123456', '202018127', '202018127', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018128, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018128', '202018128', '123456', '202018128', '202018128', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018129, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018129', '202018129', '123456', '202018129', '202018129', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018130, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018130', '202018130', '123456', '202018130', '202018130', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018131, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018131', '202018131', '123456', '202018131', '202018131', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018132, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018132', '202018132', '123456', '202018132', '202018132', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018133, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018133', '202018133', '123456', '202018133', '202018133', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018134, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018134', '202018134', '123456', '202018134', '202018134', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018135, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018135', '202018135', '123456', '202018135', '202018135', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018136, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018136', '202018136', '123456', '202018136', '202018136', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018137, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018137', '202018137', '123456', '202018137', '202018137', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018138, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018138', '202018138', '123456', '202018138', '202018138', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018139, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018139', '202018139', '123456', '202018139', '202018139', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018140, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018140', '202018140', '123456', '202018140', '202018140', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018141, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018141', '202018141', '123456', '202018141', '202018141', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018142, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018142', '202018142', '123456', '202018142', '202018142', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018143, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018143', '202018143', '123456', '202018143', '202018143', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018144, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018144', '202018144', '123456', '202018144', '202018144', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018145, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018145', '202018145', '123456', '202018145', '202018145', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    org_id = VALUES(org_id),
    org_name = VALUES(org_name),
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

INSERT INTO iot_device (
    id, tenant_id, org_id, org_name, product_id, device_name, device_code, device_secret, client_id, username, password,
    protocol_code, node_type, online_status, activate_status, device_status, firmware_version,
    ip_address, last_online_time, last_report_time, longitude, latitude, address, metadata_json,
    remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (202018186, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018186', '202018186', '123456', '202018186', '202018186', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018187, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018187', '202018187', '123456', '202018187', '202018187', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018188, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018188', '202018188', '123456', '202018188', '202018188', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018189, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018189', '202018189', '123456', '202018189', '202018189', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018190, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018190', '202018190', '123456', '202018190', '202018190', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018191, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018191', '202018191', '123456', '202018191', '202018191', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018192, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018192', '202018192', '123456', '202018192', '202018192', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018193, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018193', '202018193', '123456', '202018193', '202018193', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018194, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018194', '202018194', '123456', '202018194', '202018194', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018195, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018195', '202018195', '123456', '202018195', '202018195', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018196, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018196', '202018196', '123456', '202018196', '202018196', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018197, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018197', '202018197', '123456', '202018197', '202018197', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018198, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018198', '202018198', '123456', '202018198', '202018198', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018200, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018200', '202018200', '123456', '202018200', '202018200', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018201, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018201', '202018201', '123456', '202018201', '202018201', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0),
    (202018203, 1, 7101, '平台运维中心', 202603192100560258, 'NF-LASER-202018203', '202018203', '123456', '202018203', '202018203', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-laser-child', JSON_OBJECT('seedFamily', 'laser-child'), 'laser child baseline', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    org_id = VALUES(org_id),
    org_name = VALUES(org_name),
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

INSERT INTO iot_device (
    id, tenant_id, org_id, org_name, product_id, device_name, device_code, device_secret, client_id, username, password,
    protocol_code, node_type, online_status, activate_status, device_status, firmware_version,
    ip_address, last_online_time, last_report_time, longitude, latitude, address, metadata_json,
    remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (84330619, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330619', '84330619', '123456', '84330619', '84330619', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330627, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330627', '84330627', '123456', '84330627', '84330627', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330630, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330630', '84330630', '123456', '84330630', '84330630', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330634, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330634', '84330634', '123456', '84330634', '84330634', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330635, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330635', '84330635', '123456', '84330635', '84330635', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330637, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330637', '84330637', '123456', '84330637', '84330637', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330640, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330640', '84330640', '123456', '84330640', '84330640', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330641, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330641', '84330641', '123456', '84330641', '84330641', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330643, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330643', '84330643', '123456', '84330643', '84330643', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330671, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330671', '84330671', '123456', '84330671', '84330671', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330672, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330672', '84330672', '123456', '84330672', '84330672', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330673, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330673', '84330673', '123456', '84330673', '84330673', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330674, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330674', '84330674', '123456', '84330674', '84330674', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330675, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330675', '84330675', '123456', '84330675', '84330675', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330676, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330676', '84330676', '123456', '84330676', '84330676', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330677, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330677', '84330677', '123456', '84330677', '84330677', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330686, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330686', '84330686', '123456', '84330686', '84330686', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330687, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330687', '84330687', '123456', '84330687', '84330687', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330691, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330691', '84330691', '123456', '84330691', '84330691', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330693, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330693', '84330693', '123456', '84330693', '84330693', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330695, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330695', '84330695', '123456', '84330695', '84330695', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330696, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330696', '84330696', '123456', '84330696', '84330696', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330697, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330697', '84330697', '123456', '84330697', '84330697', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330699, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330699', '84330699', '123456', '84330699', '84330699', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330701, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330701', '84330701', '123456', '84330701', '84330701', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330702, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330702', '84330702', '123456', '84330702', '84330702', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330705, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330705', '84330705', '123456', '84330705', '84330705', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330706, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330706', '84330706', '123456', '84330706', '84330706', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330707, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330707', '84330707', '123456', '84330707', '84330707', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330708, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330708', '84330708', '123456', '84330708', '84330708', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (84330709, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-84330709', '84330709', '123456', '84330709', '84330709', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0),
    (89908808, 1, 7101, '平台运维中心', 202603192100560250, 'NF-DEEP-89908808', '89908808', '123456', '89908808', '89908808', '123456', 'mqtt-json', 1, 1, 1, 1, '1.0.0', NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL, 'shared-dev-deep-child', JSON_OBJECT('seedFamily', 'deep-child'), 'deep child baseline', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    org_id = VALUES(org_id),
    org_name = VALUES(org_name),
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

INSERT INTO iot_device_relation (
    id, tenant_id, parent_device_id, parent_device_code, logical_channel_code, child_device_id,
    child_device_code, child_product_id, child_product_key, relation_type,
    canonicalization_strategy, status_mirror_strategy, enabled, remark, create_by, create_time,
    update_by, update_time, deleted
) VALUES
    (202604110500001, 1, 202604110300001, 'SK00EA0D1307988', 'L1_LF_1', 202018108, '202018108', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500002, 1, 202604110300001, 'SK00EA0D1307988', 'L1_LF_2', 202018109, '202018109', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500003, 1, 202604110300001, 'SK00EA0D1307988', 'L1_LF_3', 202018110, '202018110', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500004, 1, 202604110300001, 'SK00EA0D1307988', 'L1_LF_4', 202018111, '202018111', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500005, 1, 202604110300001, 'SK00EA0D1307988', 'L1_LF_5', 202018112, '202018112', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500006, 1, 202604110300001, 'SK00EA0D1307988', 'L1_LF_6', 202018113, '202018113', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500007, 1, 202604110300001, 'SK00EA0D1307988', 'L1_LF_7', 202018114, '202018114', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500008, 1, 202604110300001, 'SK00EA0D1307988', 'L1_LF_8', 202018115, '202018115', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500009, 1, 202604110300001, 'SK00EA0D1307988', 'L1_LF_9', 202018116, '202018116', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500010, 1, 202604110300002, 'SK00EA0D1307967', 'L1_LF_1', 202018134, '202018134', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500011, 1, 202604110300002, 'SK00EA0D1307967', 'L1_LF_2', 202018123, '202018123', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500012, 1, 202604110300002, 'SK00EA0D1307967', 'L1_LF_3', 202018129, '202018129', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500013, 1, 202604110300002, 'SK00EA0D1307967', 'L1_LF_4', 202018122, '202018122', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500014, 1, 202604110300002, 'SK00EA0D1307967', 'L1_LF_5', 202018120, '202018120', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500015, 1, 202604110300002, 'SK00EA0D1307967', 'L1_LF_6', 202018138, '202018138', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500016, 1, 202604110300002, 'SK00EA0D1307967', 'L1_LF_7', 202018140, '202018140', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500017, 1, 202604110300002, 'SK00EA0D1307967', 'L1_LF_8', 202018125, '202018125', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500018, 1, 202604110300002, 'SK00EA0D1307967', 'L1_LF_9', 202018132, '202018132', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500019, 1, 202604110300003, 'SK00EA0D1307986', 'L1_LF_1', 202018143, '202018143', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500020, 1, 202604110300003, 'SK00EA0D1307986', 'L1_LF_2', 202018135, '202018135', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500021, 1, 202604110300003, 'SK00EA0D1307986', 'L1_LF_3', 202018121, '202018121', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500022, 1, 202604110300003, 'SK00EA0D1307986', 'L1_LF_4', 202018137, '202018137', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500023, 1, 202604110300003, 'SK00EA0D1307986', 'L1_LF_5', 202018142, '202018142', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500024, 1, 202604110300003, 'SK00EA0D1307986', 'L1_LF_6', 202018130, '202018130', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500025, 1, 202604110300003, 'SK00EA0D1307986', 'L1_LF_7', 202018127, '202018127', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500026, 1, 202604110300003, 'SK00EA0D1307986', 'L1_LF_8', 202018118, '202018118', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500027, 1, 202604110300003, 'SK00EA0D1307986', 'L1_LF_9', 202018139, '202018139', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    parent_device_id = VALUES(parent_device_id),
    parent_device_code = VALUES(parent_device_code),
    child_device_id = VALUES(child_device_id),
    child_device_code = VALUES(child_device_code),
    child_product_id = VALUES(child_product_id),
    child_product_key = VALUES(child_product_key),
    relation_type = VALUES(relation_type),
    canonicalization_strategy = VALUES(canonicalization_strategy),
    status_mirror_strategy = VALUES(status_mirror_strategy),
    enabled = VALUES(enabled),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO iot_device_relation (
    id, tenant_id, parent_device_id, parent_device_code, logical_channel_code, child_device_id,
    child_device_code, child_product_id, child_product_key, relation_type,
    canonicalization_strategy, status_mirror_strategy, enabled, remark, create_by, create_time,
    update_by, update_time, deleted
) VALUES
    (202604110500028, 1, 202604110300004, 'SK00EA0D1307987', 'L1_LF_1', 202018124, '202018124', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500029, 1, 202604110300004, 'SK00EA0D1307987', 'L1_LF_2', 202018131, '202018131', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500030, 1, 202604110300004, 'SK00EA0D1307987', 'L1_LF_3', 202018128, '202018128', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500031, 1, 202604110300004, 'SK00EA0D1307987', 'L1_LF_4', 202018117, '202018117', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500032, 1, 202604110300004, 'SK00EA0D1307987', 'L1_LF_5', 202018141, '202018141', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500033, 1, 202604110300004, 'SK00EA0D1307987', 'L1_LF_6', 202018133, '202018133', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500034, 1, 202604110300004, 'SK00EA0D1307987', 'L1_LF_7', 202018119, '202018119', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500035, 1, 202604110300004, 'SK00EA0D1307987', 'L1_LF_8', 202018126, '202018126', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500036, 1, 202604110300004, 'SK00EA0D1307987', 'L1_LF_9', 202018136, '202018136', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500037, 1, 202604110300005, 'SK00EA0D1308009', 'L1_LF_1', 202018189, '202018189', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500038, 1, 202604110300005, 'SK00EA0D1308009', 'L1_LF_2', 202018144, '202018144', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500039, 1, 202604110300005, 'SK00EA0D1308009', 'L1_LF_3', 202018145, '202018145', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500040, 1, 202604110300006, 'SK00EA0D1307984', 'L1_LF_1', 202018197, '202018197', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500041, 1, 202604110300006, 'SK00EA0D1307984', 'L1_LF_2', 202018193, '202018193', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500042, 1, 202604110300006, 'SK00EA0D1307984', 'L1_LF_3', 202018198, '202018198', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500043, 1, 202604110300007, 'SK00EA0D1308006', 'L1_LF_1', 202018186, '202018186', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500044, 1, 202604110300007, 'SK00EA0D1308006', 'L1_LF_2', 202018196, '202018196', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500045, 1, 202604110300007, 'SK00EA0D1308006', 'L1_LF_3', 202018194, '202018194', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500046, 1, 202604110300008, 'SK00EA0D1307968', 'L1_LF_1', 202018195, '202018195', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500047, 1, 202604110300008, 'SK00EA0D1307968', 'L1_LF_2', 202018192, '202018192', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500048, 1, 202604110300008, 'SK00EA0D1307968', 'L1_LF_3', 202018190, '202018190', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500049, 1, 202604110300009, 'SK00E90D1307874', 'L1_LF_1', 202018188, '202018188', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500050, 1, 202604110300009, 'SK00E90D1307874', 'L1_LF_2', 202018200, '202018200', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500051, 1, 202604110300009, 'SK00E90D1307874', 'L1_LF_3', 202018191, '202018191', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500052, 1, 202604110300010, 'SK00EA0D1307992', 'L1_LF_1', 202018187, '202018187', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500053, 1, 202604110300010, 'SK00EA0D1307992', 'L1_LF_2', 202018203, '202018203', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500054, 1, 202604110300010, 'SK00EA0D1307992', 'L1_LF_3', 202018201, '202018201', 202603192100560258, 'nf-monitor-laser-rangefinder-v1', 'collector_child', 'LF_VALUE', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    parent_device_id = VALUES(parent_device_id),
    parent_device_code = VALUES(parent_device_code),
    child_device_id = VALUES(child_device_id),
    child_device_code = VALUES(child_device_code),
    child_product_id = VALUES(child_product_id),
    child_product_key = VALUES(child_product_key),
    relation_type = VALUES(relation_type),
    canonicalization_strategy = VALUES(canonicalization_strategy),
    status_mirror_strategy = VALUES(status_mirror_strategy),
    enabled = VALUES(enabled),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

INSERT INTO iot_device_relation (
    id, tenant_id, parent_device_id, parent_device_code, logical_channel_code, child_device_id,
    child_device_code, child_product_id, child_product_key, relation_type,
    canonicalization_strategy, status_mirror_strategy, enabled, remark, create_by, create_time,
    update_by, update_time, deleted
) VALUES
    (202604110500055, 1, 202604110300101, 'SK00FB0D1310195', 'L1_SW_1', 84330701, '84330701', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500056, 1, 202604110300101, 'SK00FB0D1310195', 'L1_SW_2', 84330695, '84330695', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500057, 1, 202604110300101, 'SK00FB0D1310195', 'L1_SW_3', 84330697, '84330697', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500058, 1, 202604110300101, 'SK00FB0D1310195', 'L1_SW_4', 84330699, '84330699', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500059, 1, 202604110300101, 'SK00FB0D1310195', 'L1_SW_5', 84330686, '84330686', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500060, 1, 202604110300101, 'SK00FB0D1310195', 'L1_SW_6', 84330687, '84330687', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500061, 1, 202604110300101, 'SK00FB0D1310195', 'L1_SW_7', 84330691, '84330691', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500062, 1, 202604110300101, 'SK00FB0D1310195', 'L1_SW_8', 84330696, '84330696', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500063, 1, 202604110300102, 'SK00FB0D1310216', 'L1_SW_1', 84330643, '84330643', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500064, 1, 202604110300102, 'SK00FB0D1310216', 'L1_SW_2', 84330640, '84330640', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500065, 1, 202604110300102, 'SK00FB0D1310216', 'L1_SW_3', 84330637, '84330637', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500066, 1, 202604110300102, 'SK00FB0D1310216', 'L1_SW_4', 84330673, '84330673', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500067, 1, 202604110300102, 'SK00FB0D1310216', 'L1_SW_5', 84330674, '84330674', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500068, 1, 202604110300102, 'SK00FB0D1310216', 'L1_SW_6', 84330675, '84330675', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500069, 1, 202604110300102, 'SK00FB0D1310216', 'L1_SW_7', 84330672, '84330672', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500070, 1, 202604110300102, 'SK00FB0D1310216', 'L1_SW_8', 84330677, '84330677', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500071, 1, 202604110300103, 'SK00FB0D1310215', 'L1_SW_1', 84330671, '84330671', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500072, 1, 202604110300103, 'SK00FB0D1310215', 'L1_SW_2', 84330641, '84330641', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500073, 1, 202604110300103, 'SK00FB0D1310215', 'L1_SW_3', 84330635, '84330635', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500074, 1, 202604110300103, 'SK00FB0D1310215', 'L1_SW_4', 84330630, '84330630', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500075, 1, 202604110300103, 'SK00FB0D1310215', 'L1_SW_5', 84330627, '84330627', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500076, 1, 202604110300103, 'SK00FB0D1310215', 'L1_SW_6', 84330619, '84330619', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500077, 1, 202604110300103, 'SK00FB0D1310215', 'L1_SW_7', 84330676, '84330676', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500078, 1, 202604110300103, 'SK00FB0D1310215', 'L1_SW_8', 84330634, '84330634', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500079, 1, 202604110300104, 'SK00FB0D1310000', 'L1_SW_1', 84330693, '84330693', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500080, 1, 202604110300104, 'SK00FB0D1310000', 'L1_SW_2', 84330707, '84330707', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500081, 1, 202604110300104, 'SK00FB0D1310000', 'L1_SW_3', 84330705, '84330705', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500082, 1, 202604110300104, 'SK00FB0D1310000', 'L1_SW_4', 84330708, '84330708', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500083, 1, 202604110300104, 'SK00FB0D1310000', 'L1_SW_5', 84330706, '84330706', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500084, 1, 202604110300104, 'SK00FB0D1310000', 'L1_SW_6', 84330702, '84330702', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500085, 1, 202604110300104, 'SK00FB0D1310000', 'L1_SW_7', 84330709, '84330709', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0),
    (202604110500086, 1, 202604110300104, 'SK00FB0D1310000', 'L1_SW_8', 89908808, '89908808', 202603192100560250, 'nf-monitor-deep-displacement-v1', 'collector_child', 'LEGACY', 'SENSOR_STATE', 1, 'shared dev collector child baseline', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    parent_device_id = VALUES(parent_device_id),
    parent_device_code = VALUES(parent_device_code),
    child_device_id = VALUES(child_device_id),
    child_device_code = VALUES(child_device_code),
    child_product_id = VALUES(child_product_id),
    child_product_key = VALUES(child_product_key),
    relation_type = VALUES(relation_type),
    canonicalization_strategy = VALUES(canonicalization_strategy),
    status_mirror_strategy = VALUES(status_mirror_strategy),
    enabled = VALUES(enabled),
    remark = VALUES(remark),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

-- 共享环境重复执行时，按父设备编码 + 逻辑通道只保留最新一条关系，避免历史假 ID 继续产出重复映射。
DELETE rel
FROM iot_device_relation rel
INNER JOIN iot_device_relation newer
    ON newer.tenant_id = rel.tenant_id
   AND newer.parent_device_code = rel.parent_device_code
   AND newer.logical_channel_code = rel.logical_channel_code
   AND newer.deleted = rel.deleted
   AND rel.deleted = 0
   AND (
       COALESCE(rel.update_time, '1970-01-01 00:00:00') < COALESCE(newer.update_time, '1970-01-01 00:00:00')
       OR (
           COALESCE(rel.update_time, '1970-01-01 00:00:00') = COALESCE(newer.update_time, '1970-01-01 00:00:00')
           AND COALESCE(rel.create_time, '1970-01-01 00:00:00') < COALESCE(newer.create_time, '1970-01-01 00:00:00')
       )
       OR (
           COALESCE(rel.update_time, '1970-01-01 00:00:00') = COALESCE(newer.update_time, '1970-01-01 00:00:00')
           AND COALESCE(rel.create_time, '1970-01-01 00:00:00') = COALESCE(newer.create_time, '1970-01-01 00:00:00')
           AND rel.id < newer.id
       )
   );

-- 再按设备编码回填真实设备 ID / 子产品信息，避免 ON DUPLICATE 命中旧设备后关系表继续保留脏引用。
UPDATE iot_device_relation rel
INNER JOIN iot_device parent_device
    ON parent_device.tenant_id = rel.tenant_id
   AND parent_device.device_code = rel.parent_device_code
   AND parent_device.deleted = 0
INNER JOIN iot_device child_device
    ON child_device.tenant_id = rel.tenant_id
   AND child_device.device_code = rel.child_device_code
   AND child_device.deleted = 0
LEFT JOIN iot_product child_product
    ON child_product.id = child_device.product_id
   AND child_product.deleted = 0
SET rel.parent_device_id = parent_device.id,
    rel.child_device_id = child_device.id,
    rel.child_product_id = child_device.product_id,
    rel.child_product_key = child_product.product_key,
    rel.update_by = 1,
    rel.update_time = NOW()
WHERE rel.deleted = 0;

INSERT INTO iot_device_property (
    id, tenant_id, device_id, identifier, property_name, property_value, value_type, report_time, create_time, update_time
) VALUES
    (202604110700001, 1, 202604110300003, 'temp', '温度', '20.31', 'double', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (202604110700002, 1, 202604110300003, 'humidity', '湿度', '89.04', 'double', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (202604110700003, 1, 202604110300003, 'signal_4g', '4G信号强度', '-71', 'int', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (202604110700004, 1, 202604110300101, 'temp', '温度', '19.82', 'double', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (202604110700005, 1, 202604110300101, 'humidity', '湿度', '71.55', 'double', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (202604110700006, 1, 202604110300101, 'signal_4g', '4G信号强度', '-69', 'int', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (202604110700007, 1, 202604110300201, 'dispsX', '顺滑动方向累计变形量', '-0.0166', 'double', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (202604110700008, 1, 202604110300201, 'dispsY', '垂直坡面方向累计变形量', '-0.0368', 'double', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (202604110700009, 1, 202604110300201, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (202604110700010, 1, 202018143, 'value', '激光测距值', '10.86', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700011, 1, 202018143, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700012, 1, 202018135, 'value', '激光测距值', '6.95', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700013, 1, 202018135, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700014, 1, 202018121, 'value', '激光测距值', '2473.72', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700015, 1, 202018121, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700016, 1, 202018137, 'value', '激光测距值', '2473.72', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700017, 1, 202018137, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700018, 1, 202018142, 'value', '激光测距值', '6.73', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700019, 1, 202018142, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700020, 1, 202018130, 'value', '激光测距值', '2473.72', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700021, 1, 202018130, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700022, 1, 202018127, 'value', '激光测距值', '2473.72', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700023, 1, 202018127, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700024, 1, 202018118, 'value', '激光测距值', '6.82', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700025, 1, 202018118, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700026, 1, 202018139, 'value', '激光测距值', '10.80', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700027, 1, 202018139, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700028, 1, 84330701, 'dispsX', '顺滑动方向累计变形量', '-0.0446', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700029, 1, 84330701, 'dispsY', '垂直坡面方向累计变形量', '0.0293', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700030, 1, 84330701, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700031, 1, 84330695, 'dispsX', '顺滑动方向累计变形量', '-0.0295', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700032, 1, 84330695, 'dispsY', '垂直坡面方向累计变形量', '0.0328', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700033, 1, 84330695, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700034, 1, 84330697, 'dispsX', '顺滑动方向累计变形量', '-0.0255', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700035, 1, 84330697, 'dispsY', '垂直坡面方向累计变形量', '0.0403', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700036, 1, 84330697, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700037, 1, 84330699, 'dispsX', '顺滑动方向累计变形量', '-0.0173', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700038, 1, 84330699, 'dispsY', '垂直坡面方向累计变形量', '0.0422', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700039, 1, 84330699, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700040, 1, 84330686, 'dispsX', '顺滑动方向累计变形量', '-0.0249', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700041, 1, 84330686, 'dispsY', '垂直坡面方向累计变形量', '0.0272', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700042, 1, 84330686, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700043, 1, 84330687, 'dispsX', '顺滑动方向累计变形量', '-0.0235', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700044, 1, 84330687, 'dispsY', '垂直坡面方向累计变形量', '0.0108', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700045, 1, 84330687, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700046, 1, 84330691, 'dispsX', '顺滑动方向累计变形量', '-0.0365', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700047, 1, 84330691, 'dispsY', '垂直坡面方向累计变形量', '0.0009', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700048, 1, 84330691, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700049, 1, 84330696, 'dispsX', '顺滑动方向累计变形量', '-0.0453', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700050, 1, 84330696, 'dispsY', '垂直坡面方向累计变形量', '-0.0164', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (202604110700051, 1, 84330696, 'sensor_state', '传感器状态', '0', 'int', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW())
ON DUPLICATE KEY UPDATE
    property_name = VALUES(property_name),
    property_value = VALUES(property_value),
    value_type = VALUES(value_type),
    report_time = VALUES(report_time),
    update_time = NOW();

-- 数据就绪说明
-- 1. IoT 主链路：产品/设备/物模型/属性/消息日志
-- 2. 风险平台：风险点、绑定、规则、联动、预案、告警、事件、工单
-- 3. 系统管理：组织、区域、字典、通知渠道、审计日志
