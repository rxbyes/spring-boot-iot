USE rm_iot;

-- 鐪熷疄鍩虹鏁版嵁锛堝彲閲嶅鎵ц锛?
-- 鍓嶇疆锛氬凡鎵ц鏈€鏂?sql/init.sql銆?

SET NAMES utf8mb4;

-- =========================
-- 1) 绉熸埛/鐢ㄦ埛/瑙掕壊鍩虹鏁版嵁
-- =========================
INSERT INTO sys_tenant (
    id, tenant_name, tenant_code, contact_name, contact_phone, contact_email, status, remark, create_time, update_time, deleted
) VALUES (
    1, '榛樿绉熸埛', 'default', '骞冲彴绠＄悊鍛?, '13800000000', 'admin@ghlzm.com', 1, '鐪熷疄鐜鍩虹绉熸埛', NOW(), NOW(), 0
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
    (92000001, 1, '涓氬姟浜哄憳', 'BUSINESS_STAFF', '璐熻矗椋庨櫓鐩戞祴銆佸憡璀︾爺鍒ゃ€佷簨浠跺缃笌涓氬姟澶嶇洏銆?, 'SELF', 1, 1, NOW(), 1, NOW(), 0),
    (92000002, 1, '绠＄悊浜哄憳', 'MANAGEMENT_STAFF', '璐熻矗涓氬姟缁熺銆佽鍒欏鎵广€佺郴缁熸不鐞嗕笌杩愯惀绠＄悊銆?, 'ORG_AND_CHILDREN', 1, 1, NOW(), 1, NOW(), 0),
    (92000003, 1, '杩愮淮浜哄憳', 'OPS_STAFF', '璐熻矗璁惧鎺ュ叆銆佽仈璋冩帓闅溿€佽繍琛岀淮鎶や笌闂闂幆銆?, 'TENANT', 1, 1, NOW(), 1, NOW(), 0),
    (92000004, 1, '寮€鍙戜汉鍛?, 'DEVELOPER_STAFF', '璐熻矗鍗忚鑱旇皟銆佽鍒欏紑鍙戙€佺己闄峰畾浣嶄笌鍔熻兘楠岃瘉銆?, 'TENANT', 1, 1, NOW(), 1, NOW(), 0),
    (92000005, 1, '瓒呯骇绠＄悊鍛?, 'SUPER_ADMIN', '鎷ユ湁鍏ㄩ儴鑿滃崟涓庢搷浣滄潈闄愩€?, 'ALL', 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    description = VALUES(description),
    data_scope_type = VALUES(data_scope_type),
    status = VALUES(status),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

-- 鍒濆鍖栨紨绀鸿处鍙烽粯璁ゅ瘑鐮侊細123456锛圔Crypt锛?
INSERT INTO sys_user (
    id, tenant_id, org_id, username, password, nickname, real_name, phone, email, status, is_admin,
    remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (1, 1, 7101, 'admin', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '骞冲彴鎬绘帶瀹?, '瓒呯骇绠＄悊鍛?, '13800000000', 'admin@ghlzm.com', 1, 1,
     '瓒呯骇绠＄悊鍛樻紨绀鸿处鍙凤紝榛樿鏌ョ湅骞冲彴娌荤悊涓庡叏閲忚彍鍗曘€?, 1, NOW(), 1, NOW(), 0),
    (2, 1, 7102, 'biz_demo', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '椋庨櫓杩愯惀涓撳憳', '涓氬姟婕旂ず璐﹀彿', '13800000001', 'biz_demo@ghlzm.com', 1, 0,
     '涓氬姟浜哄憳婕旂ず璐﹀彿锛岄粯璁よ繘鍏ラ闄╄繍钀ュ伐浣滃彴銆?, 1, NOW(), 1, NOW(), 0),
    (3, 1, 7101, 'manager_demo', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '杩愯惀绠＄悊璐熻矗浜?, '绠＄悊婕旂ず璐﹀彿', '13800000002', 'manager_demo@ghlzm.com', 1, 0,
     '绠＄悊浜哄憳婕旂ず璐﹀彿锛岄粯璁よ繘鍏ラ闄╄繍钀ュ苟瑕嗙洊椋庨櫓绛栫暐銆佸钩鍙版不鐞嗐€?, 1, NOW(), 1, NOW(), 0),
    (4, 1, 7101, 'ops_demo', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '鎺ュ叆杩愮淮宸ョ▼甯?, '杩愮淮婕旂ず璐﹀彿', '13800000003', 'ops_demo@ghlzm.com', 1, 0,
     '杩愮淮浜哄憳婕旂ず璐﹀彿锛岄粯璁よ繘鍏ユ帴鍏ユ櫤缁村伐浣滃彴銆?, 1, NOW(), 1, NOW(), 0),
    (5, 1, 7101, 'dev_demo', '$2a$10$9Qvnnv2KdrBYP974N3bIGOkmbGCpIXHCXhuKvwBRJxdOEwv01R3eq',
     '骞冲彴寮€鍙戝伐绋嬪笀', '寮€鍙戞紨绀鸿处鍙?, '13800000004', 'dev_demo@ghlzm.com', 1, 0,
     '寮€鍙戜汉鍛樻紨绀鸿处鍙凤紝榛樿杩涘叆鎺ュ叆鏅虹淮骞跺紑鏀捐川閲忓伐鍦恒€?, 1, NOW(), 1, NOW(), 0)
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
    (93000001, 1, 0, '鎺ュ叆鏅虹淮', 'iot-access', '', 'Layout', 'connection', '{"description":"璧勪骇銆侀摼璺笌寮傚父瑙傛祴","menuTitle":"鎺ュ叆鏅虹淮","menuHint":"瑕嗙洊浜у搧瀹氫箟銆佽澶囪祫浜с€侀摼璺獙璇併€佸紓甯歌娴嬩笌鏁版嵁鏍￠獙銆?}', 10, 0, 0, '', 'iot-access', 10, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000002, 1, 0, '椋庨櫓杩愯惀', 'risk-ops', '', 'Layout', 'warning', '{"description":"鎬佸娍銆佸憡璀︿笌鍗忓悓闂幆","menuTitle":"椋庨櫓杩愯惀","menuHint":"瑕嗙洊瀹炴椂鐩戞祴銆佸憡璀﹁繍钀ャ€佷簨浠跺崗鍚屻€佸璞℃礊瀵熶笌杩愯惀澶嶇洏銆?}', 20, 0, 0, '', 'risk-ops', 20, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000004, 1, 0, '椋庨櫓绛栫暐', 'risk-config', '', 'Layout', 'operation', '{"description":"瀵硅薄銆侀槇鍊间笌鑱斿姩閰嶇疆","menuTitle":"椋庨櫓绛栫暐","menuHint":"瑕嗙洊椋庨櫓瀵硅薄銆侀槇鍊肩瓥鐣ャ€佽仈鍔ㄧ紪鎺掍笌搴旀€ラ妗堝簱銆?}', 30, 0, 0, '', 'risk-config', 30, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000003, 1, 0, '骞冲彴娌荤悊', 'system-governance', '', 'Layout', 'setting', '{"description":"缁勭粐銆佹潈闄愪笌瀹¤娌荤悊","menuTitle":"骞冲彴娌荤悊","menuHint":"瑕嗙洊缁勭粐銆佽处鍙枫€佽鑹层€佸鑸€佸尯鍩熴€佸瓧鍏搞€侀€氱煡銆佸府鍔╀笌瀹¤涓績銆?}', 40, 0, 0, '', 'system-governance', 40, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93000005, 1, 0, '璐ㄩ噺宸ュ満', 'quality-workbench', '', 'Layout', 'monitor', '{"description":"鐮斿彂宸ュ満銆佹墽琛岀粍缁囦笌缁撴灉鍩虹嚎","menuTitle":"璐ㄩ噺宸ュ満","menuHint":"瑕嗙洊鐮斿彂璧勪骇缂栨帓銆佹墽琛岀粍缁囦笌缁撴灉鍩虹嚎娌荤悊銆?}', 50, 0, 0, '', 'quality-workbench', 50, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93001001, 1, 93000001, '浜у搧瀹氫箟涓績', 'iot:products', '/products', 'ProductWorkbenchView', 'box', '{"caption":"浜у搧鍙拌处銆佸崗璁熀绾夸笌搴撳瓨褰掑睘"}', 11, 1, 1, '/products', 'iot:products', 11, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001002, 1, 93000001, '璁惧璧勪骇涓績', 'iot:devices', '/devices', 'DeviceWorkbenchView', 'cpu', '{"caption":"璁惧寤烘。銆佸湪绾跨姸鎬佷笌璧勪骇杩愮淮"}', 12, 1, 1, '/devices', 'iot:devices', 12, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001003, 1, 93000001, '閾捐矾楠岃瘉涓績', 'iot:reporting', '/reporting', 'ReportWorkbenchView', 'promotion', '{"caption":"HTTP 涓婃姤妯℃嫙銆乸ayload 鍥炴斁涓庤仈璋?}', 13, 1, 1, '/reporting', 'iot:reporting', 13, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001006, 1, 93000001, '寮傚父瑙傛祴鍙?, 'iot:system-log', '/system-log', 'AuditLogView', 'warning', '{"caption":"鐮斿彂娴嬭瘯瀹氫綅绯荤粺寮傚父涓庢帴鍏ラ棶棰?}', 14, 1, 1, '/system-log', 'iot:system-log', 14, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001007, 1, 93000001, '閾捐矾杩借釜鍙?, 'iot:message-trace', '/message-trace', 'MessageTraceView', 'tickets', '{"caption":"鎸?TraceId銆佽澶囩紪鐮佷笌 Topic 鎺掓煡璁惧鎺ュ叆閾捐矾"}', 15, 1, 1, '/message-trace', 'iot:message-trace', 15, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001005, 1, 93000001, '鏁版嵁鏍￠獙鍙?, 'iot:file-debug', '/file-debug', 'FilePayloadDebugView', 'document', '{"caption":"鏂囦欢蹇収涓庡浐浠惰仛鍚堢粨鏋滄牳楠?}', 16, 1, 1, '/file-debug', 'iot:file-debug', 16, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93002008, 1, 93000002, '瀹炴椂鐩戞祴鍙?, 'risk:monitoring', '/risk-monitoring', 'RealTimeMonitoringView', 'monitor', '{"caption":"椋庨櫓鐩戞祴鍒楄〃銆佺瓫閫変笌璇︽儏鎶藉眽"}', 21, 1, 1, '/risk-monitoring', 'risk:monitoring', 21, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002009, 1, 93000002, 'GIS鎬佸娍鍥?, 'risk:monitoring-gis', '/risk-monitoring-gis', 'RiskGisView', 'map-location', '{"caption":"鐐逛綅鎬佸娍銆佹湭瀹氫綅椋庨櫓鐐逛笌鍦板浘鑱斿姩"}', 22, 1, 1, '/risk-monitoring-gis', 'risk:monitoring-gis', 22, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002001, 1, 93000002, '鍛婅杩愯惀鍙?, 'risk:alarm', '/alarm-center', 'AlarmCenterView', 'bell', '{"caption":"鍛婅鍒楄〃銆佺‘璁ゃ€佹姂鍒朵笌鍏抽棴"}', 23, 1, 1, '/alarm-center', 'risk:alarm', 23, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002002, 1, 93000002, '浜嬩欢鍗忓悓鍙?, 'risk:event', '/event-disposal', 'EventDisposalView', 'flag', '{"caption":"宸ュ崟娲惧彂銆佸缃弽棣堜笌浜嬩欢闂幆"}', 24, 1, 1, '/event-disposal', 'risk:event', 24, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001004, 1, 93000002, '瀵硅薄娲炲療鍙?, 'iot:insight', '/insight', 'DeviceInsightView', 'data-analysis', '{"caption":"璁惧灞炴€с€佹秷鎭棩蹇椾笌椋庨櫓鐮斿垽绾跨储"}', 25, 1, 1, '/insight', 'iot:insight', 25, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002007, 1, 93000002, '杩愯惀鍒嗘瀽涓績', 'risk:report', '/report-analysis', 'ReportAnalysisView', 'trend-charts', '{"caption":"椋庨櫓瓒嬪娍銆佸憡璀︾粺璁′笌璁惧鍋ュ悍澶嶇洏"}', 26, 1, 1, '/report-analysis', 'risk:report', 26, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93002003, 1, 93000004, '椋庨櫓瀵硅薄涓績', 'risk:point', '/risk-point', 'RiskPointView', 'location', '{"caption":"椋庨櫓瀵硅薄寤烘。銆佽澶囩粦瀹氫笌绛夌骇娌荤悊"}', 31, 1, 1, '/risk-point', 'risk:point', 31, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002004, 1, 93000004, '闃堝€肩瓥鐣?, 'risk:rule-definition', '/rule-definition', 'RuleDefinitionView', 'set-up', '{"caption":"闃堝€艰鍒欑淮鎶や笌瑙﹀彂鏉′欢娌荤悊"}', 32, 1, 1, '/rule-definition', 'risk:rule-definition', 32, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002005, 1, 93000004, '鑱斿姩缂栨帓', 'risk:linkage-rule', '/linkage-rule', 'LinkageRuleView', 'operation', '{"caption":"瑙﹀彂鏉′欢涓庤仈鍔ㄥ姩浣滅紪鎺?}', 33, 1, 1, '/linkage-rule', 'risk:linkage-rule', 33, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002006, 1, 93000004, '搴旀€ラ妗堝簱', 'risk:emergency-plan', '/emergency-plan', 'EmergencyPlanView', 'tickets', '{"caption":"棰勬缁存姢銆佹楠ょ紪鎺掍笌鍝嶅簲鍗忓悓"}', 34, 1, 1, '/emergency-plan', 'risk:emergency-plan', 34, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002041, 1, 93002004, '缁存姢闃堝€肩瓥鐣?, 'risk:rule-definition:write', '', '', '', '{"caption":"闃堝€肩瓥鐣ユ柊澧炪€佺紪杈戙€佸垹闄ゆ寜閽潈闄?}', 3241, 2, 2, '', 'risk:rule-definition:write', 3241, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002042, 1, 93002005, '缁存姢鑱斿姩缂栨帓', 'risk:linkage-rule:write', '', '', '', '{"caption":"鑱斿姩缂栨帓鏂板銆佺紪杈戙€佸垹闄ゆ寜閽潈闄?}', 3341, 2, 2, '', 'risk:linkage-rule:write', 3341, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002043, 1, 93002006, '缁存姢搴旀€ラ妗?, 'risk:emergency-plan:write', '', '', '', '{"caption":"搴旀€ラ妗堟柊澧炪€佺紪杈戙€佸垹闄ゆ寜閽潈闄?}', 3441, 2, 2, '', 'risk:emergency-plan:write', 3441, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93003001, 1, 93000003, '缁勭粐鏋舵瀯', 'system:organization', '/organization', 'OrganizationView', 'office-building', '{"caption":"缁勭粐鏍戠淮鎶や笌璐ｄ换涓讳綋绠＄悊"}', 41, 1, 1, '/organization', 'system:organization', 41, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003002, 1, 93000003, '璐﹀彿涓績', 'system:user', '/user', 'UserView', 'user', '{"caption":"璐﹀彿缁存姢銆佺姸鎬佺鐞嗕笌瀵嗙爜閲嶇疆"}', 42, 1, 1, '/user', 'system:user', 42, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003003, 1, 93000003, '瑙掕壊鏉冮檺', 'system:role', '/role', 'RoleView', 'avatar', '{"caption":"瑙掕壊缁存姢涓庤彍鍗曟巿鏉冪鐞?}', 43, 1, 1, '/role', 'system:role', 43, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003008, 1, 93000003, '瀵艰埅缂栨帓', 'system:menu', '/menu', 'MenuView', 'menu', '{"caption":"鑿滃崟鏍戠粨鏋勪笌椤甸潰鏉冮檺椤圭淮鎶?}', 44, 1, 1, '/menu', 'system:menu', 44, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003004, 1, 93000003, '鍖哄煙鐗堝浘', 'system:region', '/region', 'RegionView', 'place', '{"caption":"鍖哄煙鏍戜笌涓氬姟鍖哄煙褰掑睘缁存姢"}', 45, 1, 1, '/region', 'system:region', 45, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003005, 1, 93000003, '鏁版嵁瀛楀吀', 'system:dict', '/dict', 'DictView', 'collection', '{"caption":"瀛楀吀绫诲瀷涓庡瓧鍏搁」閰嶇疆"}', 46, 1, 1, '/dict', 'system:dict', 46, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003006, 1, 93000003, '閫氱煡缂栨帓', 'system:channel', '/channel', 'ChannelView', 'chat-dot-round', '{"caption":"閫氱煡娓犻亾閰嶇疆銆佸惎鍋滀笌娴嬭瘯"}', 47, 1, 1, '/channel', 'system:channel', 47, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003010, 1, 93000003, '绔欏唴娑堟伅', 'system:in-app-message', '/in-app-message', 'InAppMessageView', 'bell', '{"caption":"閫氱煡涓績绔欏唴娑堟伅鐨勫垎绫汇€佽寖鍥翠笌鏃堕棿绐楀彛缂栨帓"}', 48, 1, 1, '/in-app-message', 'system:in-app-message', 48, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003011, 1, 93000003, '甯姪鏂囨。', 'system:help-doc', '/help-doc', 'HelpDocView', 'document-copy', '{"caption":"甯姪涓績涓氬姟绫汇€佹妧鏈被鍜?FAQ 璧勬枡缂栨帓"}', 49, 1, 1, '/help-doc', 'system:help-doc', 49, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003007, 1, 93000003, '瀹¤涓績', 'system:audit', '/audit-log', 'AuditLogView', 'document-checked', '{"caption":"瀹㈡埛涓庢不鐞嗕晶涓氬姟鎿嶄綔瀹¤"}', 50, 1, 1, '/audit-log', 'system:audit', 50, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93003020, 1, 93000005, '涓氬姟楠屾敹鍙?, 'system:business-acceptance', '/business-acceptance', 'BusinessAcceptanceWorkbenchView', 'finished', '{"caption":"鎸変氦浠樻竻鍗曡繍琛岄缃笟鍔￠獙鏀跺寘"}', 51, 1, 1, '/business-acceptance', 'system:business-acceptance', 51, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003015, 1, 93000005, '鐮斿彂宸ュ満', 'system:rd-workbench', '/rd-workbench', 'RdWorkbenchLandingView', 'edit-pen', '{"caption":"鐮斿彂鑷姩鍖栬祫浜х紪鎺掍富鍏ュ彛"}', 52, 1, 1, '/rd-workbench', 'system:rd-workbench', 52, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003016, 1, 93000005, '椤甸潰鐩樼偣鍙?, 'system:rd-automation-inventory', '/rd-automation-inventory', 'AutomationInventoryView', 'document', '{"caption":"椤甸潰娓呭崟銆佽鐩栫己鍙ｄ笌浜哄伐琛ュ綍"}', 53, 1, 1, '/rd-automation-inventory', 'system:rd-automation-inventory', 53, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003017, 1, 93000005, '鍦烘櫙妯℃澘鍙?, 'system:rd-automation-templates', '/rd-automation-templates', 'AutomationTemplatesView', 'files', '{"caption":"娌夋穩椤甸潰鍐掔儫銆佽〃鍗曟彁浜や笌鍒楄〃璇︽儏妯℃澘"}', 54, 1, 1, '/rd-automation-templates', 'system:rd-automation-templates', 54, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003018, 1, 93000005, '璁″垝缂栨帓鍙?, 'system:rd-automation-plans', '/rd-automation-plans', 'AutomationPlansView', 'edit', '{"caption":"缁存姢鍦烘櫙椤哄簭銆佹楠ゃ€佹柇瑷€涓庡鍏ュ鍑?}', 55, 1, 1, '/rd-automation-plans', 'system:rd-automation-plans', 55, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003019, 1, 93000005, '浜や粯鎵撳寘鍙?, 'system:rd-automation-handoff', '/rd-automation-handoff', 'AutomationHandoffView', 'promotion', '{"caption":"鏁寸悊鎵ц寤鸿銆佸熀绾胯鏄庝笌楠屾敹澶囨敞"}', 56, 1, 1, '/rd-automation-handoff', 'system:rd-automation-handoff', 56, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003013, 1, 93000005, '鎵ц涓績', 'system:automation-execution', '/automation-execution', 'AutomationExecutionView', 'operation', '{"caption":"鐩爣鐜銆佸懡浠ら瑙堜笌缁熶竴楠屾敹娉ㄥ唽琛?}', 57, 1, 1, '/automation-execution', 'system:automation-execution', 57, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003014, 1, 93000005, '缁撴灉涓庡熀绾夸腑蹇?, 'system:automation-results', '/automation-results', 'AutomationResultsView', 'data-analysis', '{"caption":"杩愯缁撴灉瀵煎叆銆佸け璐ュ鐩樹笌璐ㄩ噺寤鸿"}', 58, 1, 1, '/automation-results', 'system:automation-results', 58, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003012, 1, 93000005, '鑷姩鍖栬祫浜т腑蹇冿紙鍏煎鍏ュ彛锛?, 'system:automation-assets', '/automation-assets', 'AutomationAssetsView', 'document', '{"caption":"鍏煎鏃у叆鍙ｏ紝绗竴杞洿鎺ヨ惤鍒扮爺鍙戝伐鍦烘€昏"}', 59, 1, 1, '/automation-assets', 'system:automation-assets', 59, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003009, 1, 93000005, '鑷姩鍖栧伐鍦猴紙鍏煎鍏ュ彛锛?, 'system:automation-test', '/automation-test', 'AutomationTestCenterView', 'monitor', '{"caption":"鍏煎鏃у叆鍙ｏ紝绗竴杞洿鎺ヨ惤鍒扮爺鍙戝伐鍦烘€昏"}', 60, 1, 1, '/automation-test', 'system:automation-test', 60, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001011, 1, 93001001, '鏂板浜у搧', 'iot:products:add', '', '', '', '{"caption":"浜у搧瀹氫箟涓績鏂板浜у搧鎸夐挳鏉冮檺"}', 1101, 2, 2, '', 'iot:products:add', 1101, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001012, 1, 93001001, '缂栬緫浜у搧', 'iot:products:update', '', '', '', '{"caption":"浜у搧瀹氫箟涓績缂栬緫浜у搧鎸夐挳鏉冮檺"}', 1102, 2, 2, '', 'iot:products:update', 1102, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001013, 1, 93001001, '鍒犻櫎浜у搧', 'iot:products:delete', '', '', '', '{"caption":"浜у搧瀹氫箟涓績鍒犻櫎浜у搧鎸夐挳鏉冮檺"}', 1103, 2, 2, '', 'iot:products:delete', 1103, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001014, 1, 93001001, '瀵煎嚭浜у搧', 'iot:products:export', '', '', '', '{"caption":"浜у搧瀹氫箟涓績瀵煎嚭浜у搧鎸夐挳鏉冮檺"}', 1104, 2, 2, '', 'iot:products:export', 1104, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001101, 1, 93001002, '鏂板璁惧', 'iot:devices:add', '', '', '', '{"caption":"璁惧璧勪骇涓績鏂板璁惧鎸夐挳鏉冮檺"}', 1201, 2, 2, '', 'iot:devices:add', 1201, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001102, 1, 93001002, '缂栬緫璁惧', 'iot:devices:update', '', '', '', '{"caption":"璁惧璧勪骇涓績缂栬緫璁惧鎸夐挳鏉冮檺"}', 1202, 2, 2, '', 'iot:devices:update', 1202, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001103, 1, 93001002, '鍒犻櫎璁惧', 'iot:devices:delete', '', '', '', '{"caption":"璁惧璧勪骇涓績鍒犻櫎璁惧鎸夐挳鏉冮檺"}', 1203, 2, 2, '', 'iot:devices:delete', 1203, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001104, 1, 93001002, '瀵煎嚭璁惧', 'iot:devices:export', '', '', '', '{"caption":"璁惧璧勪骇涓績瀵煎嚭璁惧鎸夐挳鏉冮檺"}', 1204, 2, 2, '', 'iot:devices:export', 1204, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001105, 1, 93001002, '鎵归噺瀵煎叆璁惧', 'iot:devices:import', '', '', '', '{"caption":"璁惧璧勪骇涓績鎵归噺瀵煎叆璁惧鎸夐挳鏉冮檺"}', 1205, 2, 2, '', 'iot:devices:import', 1205, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001106, 1, 93001002, '鏇存崲璁惧', 'iot:devices:replace', '', '', '', '{"caption":"璁惧璧勪骇涓績鏇存崲璁惧鎸夐挳鏉冮檺"}', 1206, 2, 2, '', 'iot:devices:replace', 1206, 1, 1, 1, NOW(), 1, NOW(), 0),

    (93003101, 1, 93003002, '鏂板鐢ㄦ埛', 'system:user:add', '', '', '', '{"caption":"鏂板鐢ㄦ埛鎸夐挳鏉冮檺"}', 3201, 2, 2, '', 'system:user:add', 3201, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003102, 1, 93003002, '缂栬緫鐢ㄦ埛', 'system:user:update', '', '', '', '{"caption":"缂栬緫鐢ㄦ埛鎸夐挳鏉冮檺"}', 3202, 2, 2, '', 'system:user:update', 3202, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003103, 1, 93003002, '鍒犻櫎鐢ㄦ埛', 'system:user:delete', '', '', '', '{"caption":"鍒犻櫎鐢ㄦ埛鎸夐挳鏉冮檺"}', 3203, 2, 2, '', 'system:user:delete', 3203, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003104, 1, 93003002, '閲嶇疆瀵嗙爜', 'system:user:reset-password', '', '', '', '{"caption":"閲嶇疆瀵嗙爜鎸夐挳鏉冮檺"}', 3204, 2, 2, '', 'system:user:reset-password', 3204, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003201, 1, 93003003, '鏂板瑙掕壊', 'system:role:add', '', '', '', '{"caption":"鏂板瑙掕壊鎸夐挳鏉冮檺"}', 3301, 2, 2, '', 'system:role:add', 3301, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003202, 1, 93003003, '缂栬緫瑙掕壊', 'system:role:update', '', '', '', '{"caption":"缂栬緫瑙掕壊鎸夐挳鏉冮檺"}', 3302, 2, 2, '', 'system:role:update', 3302, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003203, 1, 93003003, '鍒犻櫎瑙掕壊', 'system:role:delete', '', '', '', '{"caption":"鍒犻櫎瑙掕壊鎸夐挳鏉冮檺"}', 3303, 2, 2, '', 'system:role:delete', 3303, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003301, 1, 93003008, '鏂板鑿滃崟', 'system:menu:add', '', '', '', '{"caption":"鏂板鑿滃崟鎸夐挳鏉冮檺"}', 3801, 2, 2, '', 'system:menu:add', 3801, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003302, 1, 93003008, '缂栬緫鑿滃崟', 'system:menu:update', '', '', '', '{"caption":"缂栬緫鑿滃崟鎸夐挳鏉冮檺"}', 3802, 2, 2, '', 'system:menu:update', 3802, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003303, 1, 93003008, '鍒犻櫎鑿滃崟', 'system:menu:delete', '', '', '', '{"caption":"鍒犻櫎鑿滃崟鎸夐挳鏉冮檺"}', 3803, 2, 2, '', 'system:menu:delete', 3803, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003401, 1, 93003010, '鏂板绔欏唴娑堟伅', 'system:in-app-message:add', '', '', '', '{"caption":"鏂板绔欏唴娑堟伅鎸夐挳鏉冮檺"}', 3901, 2, 2, '', 'system:in-app-message:add', 3901, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003402, 1, 93003010, '缂栬緫绔欏唴娑堟伅', 'system:in-app-message:update', '', '', '', '{"caption":"缂栬緫绔欏唴娑堟伅鎸夐挳鏉冮檺"}', 3902, 2, 2, '', 'system:in-app-message:update', 3902, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003403, 1, 93003010, '鍒犻櫎绔欏唴娑堟伅', 'system:in-app-message:delete', '', '', '', '{"caption":"鍒犻櫎绔欏唴娑堟伅鎸夐挳鏉冮檺"}', 3903, 2, 2, '', 'system:in-app-message:delete', 3903, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003501, 1, 93003011, '鏂板甯姪鏂囨。', 'system:help-doc:add', '', '', '', '{"caption":"鏂板甯姪鏂囨。鎸夐挳鏉冮檺"}', 4001, 2, 2, '', 'system:help-doc:add', 4001, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003502, 1, 93003011, '缂栬緫甯姪鏂囨。', 'system:help-doc:update', '', '', '', '{"caption":"缂栬緫甯姪鏂囨。鎸夐挳鏉冮檺"}', 4002, 2, 2, '', 'system:help-doc:update', 4002, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93003503, 1, 93003011, '鍒犻櫎甯姪鏂囨。', 'system:help-doc:delete', '', '', '', '{"caption":"鍒犻櫎甯姪鏂囨。鎸夐挳鏉冮檺"}', 4003, 2, 2, '', 'system:help-doc:delete', 4003, 1, 1, 1, NOW(), 1, NOW(), 0)
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
    (96010145, 1, @role_management_id, 93002041, 1, NOW(), 1, NOW(), 0),
    (96010146, 1, @role_management_id, 93002042, 1, NOW(), 1, NOW(), 0),
    (96010147, 1, @role_management_id, 93002043, 1, NOW(), 1, NOW(), 0),
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
    (96010142, 1, @role_management_id, 93000005, 1, NOW(), 1, NOW(), 0),
    (96010143, 1, @role_management_id, 93003020, 1, NOW(), 1, NOW(), 0),

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
    (96010148, 1, @role_ops_id, 93002041, 1, NOW(), 1, NOW(), 0),
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
    (96010149, 1, @role_developer_id, 93002041, 1, NOW(), 1, NOW(), 0),
    (96010150, 1, @role_developer_id, 93002042, 1, NOW(), 1, NOW(), 0),
    (96010151, 1, @role_developer_id, 93002043, 1, NOW(), 1, NOW(), 0),
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
    (96010118, 1, @role_developer_id, 93001014, 1, NOW(), 1, NOW(), 0);

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
    (93001026, 1, 93001001, '密钥托管查看', 'iot:secret-custody:view', '', '', '', '{"caption":"查看密钥托管与轮换记录"}', 1126, 2, 2, '', 'iot:secret-custody:view', 1126, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001027, 1, 93001001, '密钥轮换执行', 'iot:secret-custody:rotate', '', '', '', '{"caption":"执行设备密钥轮换"}', 1127, 2, 2, '', 'iot:secret-custody:rotate', 1127, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93001028, 1, 93001001, '密钥轮换复核', 'iot:secret-custody:approve', '', '', '', '{"caption":"关键密钥轮换动作双人复核"}', 1128, 2, 2, '', 'iot:secret-custody:approve', 1128, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002044, 1, 93002003, '风险指标标注', 'risk:metric-catalog:tag', '', '', '', '{"caption":"维护风险指标语义标签"}', 3144, 2, 2, '', 'risk:metric-catalog:tag', 3144, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002045, 1, 93002004, '阈值策略执行', 'risk:rule-definition:edit', '', '', '', '{"caption":"新增/更新/删除阈值策略"}', 3245, 2, 2, '', 'risk:rule-definition:edit', 3245, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002046, 1, 93002004, '阈值策略复核', 'risk:rule-definition:approve', '', '', '', '{"caption":"阈值策略关键写操作双人复核"}', 3246, 2, 2, '', 'risk:rule-definition:approve', 3246, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002047, 1, 93002005, '联动编排执行', 'risk:linkage-rule:edit', '', '', '', '{"caption":"新增/更新/删除联动规则"}', 3347, 2, 2, '', 'risk:linkage-rule:edit', 3347, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002048, 1, 93002005, '联动编排复核', 'risk:linkage-rule:approve', '', '', '', '{"caption":"联动规则关键写操作双人复核"}', 3348, 2, 2, '', 'risk:linkage-rule:approve', 3348, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002049, 1, 93002006, '预案执行', 'risk:emergency-plan:edit', '', '', '', '{"caption":"新增/更新/删除应急预案"}', 3449, 2, 2, '', 'risk:emergency-plan:edit', 3449, 1, 1, 1, NOW(), 1, NOW(), 0),
    (93002050, 1, 93002006, '预案复核', 'risk:emergency-plan:approve', '', '', '', '{"caption":"预案关键写操作双人复核"}', 3450, 2, 2, '', 'risk:emergency-plan:approve', 3450, 1, 1, 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
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
    UNION ALL SELECT @role_management_id, 93001022
    UNION ALL SELECT @role_management_id, 93001023
    UNION ALL SELECT @role_management_id, 93001024
    UNION ALL SELECT @role_management_id, 93001025
    UNION ALL SELECT @role_management_id, 93001026
    UNION ALL SELECT @role_management_id, 93001027
    UNION ALL SELECT @role_management_id, 93001028
    UNION ALL SELECT @role_management_id, 93002044
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
    UNION ALL SELECT @role_ops_id, 93002044
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

-- =========================
-- 2) IoT 浜у搧/璁惧/娑堟伅鍩虹嚎
-- =========================
INSERT INTO iot_product (
    id, tenant_id, product_key, product_name, protocol_code, node_type, data_format,
    manufacturer, description, status, remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (1001, 1, 'accept-http-product-01', '楠屾敹浜у搧-HTTP-01', 'mqtt-json', 1, 'JSON', 'GHLZM', 'HTTP 涓婚摼璺獙鏀朵骇鍝?, 1, '鐪熷疄鐜鍩虹嚎', 1, NOW(), 1, NOW(), 0),
    (1002, 1, 'accept-mqtt-product-01', '楠屾敹浜у搧-MQTT-01', 'mqtt-json', 1, 'JSON', 'GHLZM', 'MQTT 涓婚摼璺獙鏀朵骇鍝?, 1, '鐪熷疄鐜鍩虹嚎', 1, NOW(), 1, NOW(), 0)
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
    (3001, 1, 1001, 'property', 'temperature', '娓╁害', 'double', JSON_OBJECT('unit', '鈩?, 'min', -40, 'max', 200), 1, 0, '娓╁害娴嬬偣', NOW(), NOW(), 0),
    (3002, 1, 1001, 'property', 'humidity', '婀垮害', 'double', JSON_OBJECT('unit', '%', 'min', 0, 'max', 100), 2, 0, '婀垮害娴嬬偣', NOW(), NOW(), 0),
    (3003, 1, 1001, 'property', 'pressure', '鍘嬪姏', 'double', JSON_OBJECT('unit', 'kPa', 'min', 80, 'max', 140), 3, 0, '鍘嬪姏娴嬬偣', NOW(), NOW(), 0),
    (3004, 1, 1002, 'property', 'temperature', '娓╁害', 'double', JSON_OBJECT('unit', '鈩?, 'min', -40, 'max', 200), 1, 0, '娓╁害娴嬬偣', NOW(), NOW(), 0),
    (3005, 1, 1002, 'property', 'vibration', '鎸姩', 'double', JSON_OBJECT('unit', 'mm/s', 'min', 0, 'max', 30), 2, 0, '鎸姩娴嬬偣', NOW(), NOW(), 0)
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
    precision_digits, monitor_content_code, monitor_type_code, risk_enabled, trend_enabled, metadata_json
) VALUES
    (920001, 1, 'phase1-crack', 'CRACK', 'value', '瑁傜紳鐩戞祴鍊?, 'mm', 4, 'L1', 'LF', 1, 1, JSON_OBJECT('thresholdKind', 'absolute')),
    (920002, 1, 'phase1-crack', 'CRACK', 'sensor_state', '浼犳劅鍣ㄧ姸鎬?, NULL, 0, 'S1', 'ZT', 0, 0, JSON_OBJECT('usage', 'health_state')),
    (920011, 1, 'phase2-gnss', 'GNSS', 'gpsInitial', 'GNSS 鍘熷瑙傛祴鍩虹鏁版嵁', NULL, 0, 'L1', 'GP', 0, 0, JSON_OBJECT('usage', 'raw_observation')),
    (920012, 1, 'phase2-gnss', 'GNSS', 'gpsTotalX', 'GNSS 绱浣嶇Щ X', 'mm', 4, 'L1', 'GP', 1, 1, JSON_OBJECT('thresholdKind', 'absolute')),
    (920013, 1, 'phase2-gnss', 'GNSS', 'gpsTotalY', 'GNSS 绱浣嶇Щ Y', 'mm', 4, 'L1', 'GP', 1, 1, JSON_OBJECT('thresholdKind', 'absolute')),
    (920014, 1, 'phase2-gnss', 'GNSS', 'gpsTotalZ', 'GNSS 绱浣嶇Щ Z', 'mm', 4, 'L1', 'GP', 1, 1, JSON_OBJECT('thresholdKind', 'absolute')),
    (920015, 1, 'phase2-gnss', 'GNSS', 'sensor_state', '浼犳劅鍣ㄧ姸鎬?, NULL, 0, 'S1', 'ZT', 0, 0, JSON_OBJECT('usage', 'health_state'))
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    unit = VALUES(unit),
    precision_digits = VALUES(precision_digits),
    monitor_content_code = VALUES(monitor_content_code),
    monitor_type_code = VALUES(monitor_type_code),
    risk_enabled = VALUES(risk_enabled),
    trend_enabled = VALUES(trend_enabled),
    metadata_json = VALUES(metadata_json),
    deleted = 0;

INSERT INTO iot_device (
    id, tenant_id, org_id, org_name, product_id, device_name, device_code, device_secret, client_id, username, password,
    protocol_code, node_type, online_status, activate_status, device_status, firmware_version,
    ip_address, last_online_time, last_report_time, longitude, latitude, address, metadata_json,
    remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (2001, 1, 7101, '骞冲彴杩愮淮涓績', 1001, '楠屾敹璁惧-HTTP-01', 'accept-http-device-01', '123456', 'accept-http-device-01', 'accept-http-device-01', '123456',
     'mqtt-json', 1, 1, 1, 1, '1.0.0', '10.10.1.11', DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), 121.473700, 31.230400, '涓婃捣甯傞粍娴﹀尯涓北鍗楄矾', JSON_OBJECT('line', 'A', 'workshop', 'W1'),
     'HTTP 閾捐矾楠屾敹璁惧', 1, NOW(), 1, NOW(), 0),
    (2002, 1, 7102, '鍛婅澶勭疆缁?, 1002, '楠屾敹璁惧-MQTT-01', 'accept-mqtt-device-01', '123456', 'accept-mqtt-device-01', 'accept-mqtt-device-01', '123456',
     'mqtt-json', 1, 1, 1, 1, '1.2.3', '10.10.1.12', DATE_SUB(NOW(), INTERVAL 8 MINUTE), DATE_SUB(NOW(), INTERVAL 1 MINUTE), 121.478900, 31.226600, '涓婃捣甯傞粍娴﹀尯浜烘皯璺?, JSON_OBJECT('line', 'B', 'workshop', 'W2'),
     'MQTT 閾捐矾楠屾敹璁惧', 1, NOW(), 1, NOW(), 0)
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
    (4001, 1, 2001, 'temperature', '娓╁害', '26.5', 'double', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (4002, 1, 2001, 'humidity', '婀垮害', '68', 'double', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (4003, 1, 2001, 'pressure', '鍘嬪姏', '101.3', 'double', DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
    (4004, 1, 2002, 'temperature', '娓╁害', '31.2', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
    (4005, 1, 2002, 'vibration', '鎸姩', '5.6', 'double', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW())
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
     '楠屾敹涓嬭鎸囦护鏍蜂緥', 1, NOW(), 1, NOW(), 0)
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
-- 3) 绯荤粺绠＄悊鍩虹鏁版嵁
-- =========================
INSERT INTO sys_region (
    id, tenant_id, region_name, region_code, parent_id, region_type, longitude, latitude,
    status, sort_no, remark, create_by, create_time, update_by, update_time, deleted
) VALUES
    (7001, 1, '鍗庝笢绀鸿寖鍖?, 'EAST-DEMO', 0, 'province', 121.473700, 31.230400, 1, 1, '鐪熷疄鐜婕旂ず鍖哄煙', 1, NOW(), 1, NOW(), 0),
    (7002, 1, '榛勬郸鍘傚尯', 'HP-PLANT', 7001, 'district', 121.478900, 31.226600, 1, 1, '椋庨櫓鐐规墍灞炲尯鍩?, 1, NOW(), 1, NOW(), 0)
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
    (62, 1, '鐢樿們鐪?, '62', 0, 'province', NULL, NULL, 1, 1, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (6201, 1, '鍏板窞甯?, '6201', 62, 'city', NULL, NULL, 1, 11, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (6202, 1, '鍢夊唱鍏冲競', '6202', 62, 'city', NULL, NULL, 1, 12, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (6205, 1, '澶╂按甯?, '6205', 62, 'city', NULL, NULL, 1, 13, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (6206, 1, '姝﹀▉甯?, '6206', 62, 'city', NULL, NULL, 1, 14, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (6208, 1, '骞冲噳甯?, '6208', 62, 'city', NULL, NULL, 1, 15, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (6210, 1, '搴嗛槼甯?, '6210', 62, 'city', NULL, NULL, 1, 16, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (6211, 1, '瀹氳タ甯?, '6211', 62, 'city', NULL, NULL, 1, 17, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (6212, 1, '闄囧崡甯?, '6212', 62, 'city', NULL, NULL, 1, 18, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (6229, 1, '涓村鍥炴棌鑷不宸?, '6229', 62, 'city', NULL, NULL, 1, 19, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (620102, 1, '鍩庡叧鍖?, '620102', 6201, 'district', NULL, NULL, 1, 101, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (620103, 1, '涓冮噷娌冲尯', '620103', 6201, 'district', NULL, NULL, 1, 102, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (620104, 1, '瑗垮浐鍖?, '620104', 6201, 'district', NULL, NULL, 1, 103, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (620111, 1, '绾㈠彜鍖?, '620111', 6201, 'district', NULL, NULL, 1, 104, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (620121, 1, '姘哥櫥鍘?, '620121', 6201, 'district', NULL, NULL, 1, 105, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (620122, 1, '鐨嬪叞鍘?, '620122', 6201, 'district', NULL, NULL, 1, 106, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (620123, 1, '姒嗕腑鍘?, '620123', 6201, 'district', NULL, NULL, 1, 107, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (620201101, 1, '宄硥闀?, '620201101', 6202, 'street', NULL, NULL, 1, 108, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (620503, 1, '楹︾Н鍖?, '620503', 6205, 'district', NULL, NULL, 1, 109, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (620522, 1, '绉﹀畨鍘?, '620522', 6205, 'district', NULL, NULL, 1, 110, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (620602, 1, '鍑夊窞鍖?, '620602', 6206, 'district', NULL, NULL, 1, 111, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (620622, 1, '鍙ゆ氮鍘?, '620622', 6206, 'district', NULL, NULL, 1, 112, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (620881, 1, '鍗庝涵甯?, '620881', 6208, 'district', NULL, NULL, 1, 113, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (621021, 1, '搴嗗煄鍘?, '621021', 6210, 'district', NULL, NULL, 1, 114, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (621026, 1, '瀹佸幙', '621026', 6210, 'district', NULL, NULL, 1, 115, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (621102, 1, '瀹夊畾鍖?, '621102', 6211, 'district', NULL, NULL, 1, 116, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (621123, 1, '娓簮鍘?, '621123', 6211, 'district', NULL, NULL, 1, 117, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (621202, 1, '姝﹂兘鍖?, '621202', 6212, 'district', NULL, NULL, 1, 118, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (621221, 1, '鎴愬幙', '621221', 6212, 'district', NULL, NULL, 1, 119, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (621222, 1, '鏂囧幙', '621222', 6212, 'district', NULL, NULL, 1, 120, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (621225, 1, '瑗垮拰鍘?, '621225', 6212, 'district', NULL, NULL, 1, 121, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (622901, 1, '涓村甯?, '622901', 6229, 'district', NULL, NULL, 1, 122, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (622923, 1, '姘搁潠鍘?, '622923', 6229, 'district', NULL, NULL, 1, 123, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (622925, 1, '鍜屾斂鍘?, '622925', 6229, 'district', NULL, NULL, 1, 124, '楂橀€熷叕璺闄╃偣琛屾斂鍖哄垝鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0)
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
    (7101, 1, 0, '骞冲彴杩愮淮涓績', 'OPS-CENTER', 'dept', 1, '绯荤粺绠＄悊鍛?, '13800000000', 'ops@ghlzm.com', 1, 1, '杩愮淮涓績', 1, NOW(), 1, NOW(), 0),
    (7102, 1, 7101, '鍛婅澶勭疆缁?, 'ALARM-TEAM', 'team', 1, '绯荤粺绠＄悊鍛?, '13800000000', 'alarm@ghlzm.com', 1, 2, '鍛婅浜嬩欢澶勭疆鍥㈤槦', 1, NOW(), 1, NOW(), 0)
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
    (7111, 1, 0, '鎴愬幙鎵€', 'HW-ORG-CXS', 'dept', 1, '绯荤粺绠＄悊鍛?, '13800000000', NULL, 1, 101, '楂橀€熷叕璺闄╃偣绠″吇鍗曚綅鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (7112, 1, 0, '鎴愭鎵€', 'HW-ORG-CWS', 'dept', 1, '绯荤粺绠＄悊鍛?, '13800000000', NULL, 1, 102, '楂橀€熷叕璺闄╃偣绠″吇鍗曚綅鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (7113, 1, 0, '姝﹂兘鎵€', 'HW-ORG-WDS', 'dept', 1, '绯荤粺绠＄悊鍛?, '13800000000', NULL, 1, 103, '楂橀€熷叕璺闄╃偣绠″吇鍗曚綅鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (7114, 1, 0, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 'HW-ORG-LZFGS', 'dept', 1, '绯荤粺绠＄悊鍛?, '13800000000', NULL, 1, 104, '楂橀€熷叕璺闄╃偣绠″吇鍗曚綅鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (7115, 1, 0, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗕复澶忓垎鍏徃', 'HW-ORG-LXFGS', 'dept', 1, '绯荤粺绠＄悊鍛?, '13800000000', NULL, 1, 105, '楂橀€熷叕璺闄╃偣绠″吇鍗曚綅鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (7116, 1, 0, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗钩鍑夊垎鍏徃', 'HW-ORG-PLFGS', 'dept', 1, '绯荤粺绠＄悊鍛?, '13800000000', NULL, 1, 106, '楂橀€熷叕璺闄╃偣绠″吇鍗曚綅鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (7117, 1, 0, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 'HW-ORG-WWFGS', 'dept', 1, '绯荤粺绠＄悊鍛?, '13800000000', NULL, 1, 107, '楂橀€熷叕璺闄╃偣绠″吇鍗曚綅鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (7118, 1, 0, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 'HW-ORG-TSFGS', 'dept', 1, '绯荤粺绠＄悊鍛?, '13800000000', NULL, 1, 108, '楂橀€熷叕璺闄╃偣绠″吇鍗曚綅鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (7119, 1, 0, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗畾瑗垮垎鍏徃', 'HW-ORG-DXFGS', 'dept', 1, '绯荤粺绠＄悊鍛?, '13800000000', NULL, 1, 109, '楂橀€熷叕璺闄╃偣绠″吇鍗曚綅鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0),
    (7120, 1, 0, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘暒鐓屽垎鍏徃', 'HW-ORG-DHFGS', 'dept', 1, '绯荤粺绠＄悊鍛?, '13800000000', NULL, 1, 110, '楂橀€熷叕璺闄╃偣绠″吇鍗曚綅鏈€灏忓熀绾?, 1, NOW(), 1, NOW(), 0)
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
    (7201, 1, '椋庨櫓鐐圭瓑绾?, 'risk_point_level', 'text', 1, 1, '椋庨櫓鐐规。妗堢瓑绾у瓧鍏?, 1, NOW(), 1, NOW(), 0),
    (7202, 1, '鍛婅绛夌骇', 'alarm_level', 'text', 1, 2, '鍛婅绛夌骇鍥涜壊瀛楀吀', 1, NOW(), 1, NOW(), 0),
    (7203, 1, '椋庨櫓鎬佸娍绛夌骇', 'risk_level', 'text', 1, 3, '杩愯鎬侀闄╅鑹插瓧鍏?, 1, NOW(), 1, NOW(), 0),
    (7204, 1, '甯姪鏂囨。鍒嗙被', 'help_doc_category', 'text', 1, 4, '甯姪涓績鍒嗙被瀛楀吀', 1, NOW(), 1, NOW(), 0),
    (7205, 1, '閫氱煡娓犻亾绫诲瀷', 'notification_channel_type', 'text', 1, 5, '閫氱煡娓犻亾绫诲瀷瀛楀吀', 1, NOW(), 1, NOW(), 0)
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
    (7301, 1, 7201, '涓€绾ч闄╃偣', 'level_1', 'string', 1, 1, '椋庨櫓鐐圭瓑绾?涓€绾ч闄╃偣', 1, NOW(), 1, NOW(), 0),
    (7302, 1, 7201, '浜岀骇椋庨櫓鐐?, 'level_2', 'string', 1, 2, '椋庨櫓鐐圭瓑绾?浜岀骇椋庨櫓鐐?, 1, NOW(), 1, NOW(), 0),
    (7303, 1, 7201, '涓夌骇椋庨櫓鐐?, 'level_3', 'string', 1, 3, '椋庨櫓鐐圭瓑绾?涓夌骇椋庨櫓鐐?, 1, NOW(), 1, NOW(), 0),
    (7304, 1, 7202, '绾㈣壊', 'red', 'string', 1, 1, '鍛婅绛夌骇-绾㈣壊', 1, NOW(), 1, NOW(), 0),
    (7305, 1, 7202, '姗欒壊', 'orange', 'string', 1, 2, '鍛婅绛夌骇-姗欒壊', 1, NOW(), 1, NOW(), 0),
    (7306, 1, 7202, '榛勮壊', 'yellow', 'string', 1, 3, '鍛婅绛夌骇-榛勮壊', 1, NOW(), 1, NOW(), 0),
    (7307, 1, 7202, '钃濊壊', 'blue', 'string', 1, 4, '鍛婅绛夌骇-钃濊壊', 1, NOW(), 1, NOW(), 0),
    (7308, 1, 7203, '绾㈣壊', 'red', 'string', 1, 1, '椋庨櫓鎬佸娍绛夌骇-绾㈣壊', 1, NOW(), 1, NOW(), 0),
    (7309, 1, 7203, '姗欒壊', 'orange', 'string', 1, 2, '椋庨櫓鎬佸娍绛夌骇-姗欒壊', 1, NOW(), 1, NOW(), 0),
    (7310, 1, 7203, '榛勮壊', 'yellow', 'string', 1, 3, '椋庨櫓鎬佸娍绛夌骇-榛勮壊', 1, NOW(), 1, NOW(), 0),
    (7311, 1, 7203, '钃濊壊', 'blue', 'string', 1, 4, '椋庨櫓鎬佸娍绛夌骇-钃濊壊', 1, NOW(), 1, NOW(), 0),
    (7312, 1, 7204, '涓氬姟绫?, 'business', 'string', 1, 1, '甯姪鏂囨。鍒嗙被-涓氬姟绫?, 1, NOW(), 1, NOW(), 0),
    (7313, 1, 7204, '鎶€鏈被', 'technical', 'string', 1, 2, '甯姪鏂囨。鍒嗙被-鎶€鏈被', 1, NOW(), 1, NOW(), 0),
    (7314, 1, 7204, '甯歌闂', 'faq', 'string', 1, 3, '甯姪鏂囨。鍒嗙被-甯歌闂', 1, NOW(), 1, NOW(), 0),
    (7315, 1, 7205, '閭欢', 'email', 'string', 1, 1, '閫氱煡娓犻亾绫诲瀷-閭欢', 1, NOW(), 1, NOW(), 0),
    (7316, 1, 7205, '鐭俊', 'sms', 'string', 1, 2, '閫氱煡娓犻亾绫诲瀷-鐭俊', 1, NOW(), 1, NOW(), 0),
    (7317, 1, 7205, 'Webhook', 'webhook', 'string', 1, 3, '閫氱煡娓犻亾绫诲瀷-Webhook', 1, NOW(), 1, NOW(), 0),
    (7318, 1, 7205, '寰俊', 'wechat', 'string', 1, 4, '閫氱煡娓犻亾绫诲瀷-寰俊', 1, NOW(), 1, NOW(), 0),
    (7319, 1, 7205, '椋炰功', 'feishu', 'string', 1, 5, '閫氱煡娓犻亾绫诲瀷-椋炰功', 1, NOW(), 1, NOW(), 0),
    (7320, 1, 7205, '閽夐拤', 'dingtalk', 'string', 1, 6, '閫氱煡娓犻亾绫诲瀷-閽夐拤', 1, NOW(), 1, NOW(), 0)
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
    (7401, 1, '閭欢閫氱煡', 'email-default', 'email', JSON_OBJECT('host', 'smtp.example.com', 'port', 465, 'from', 'iot-alert@ghlzm.com'), 1, 1, '榛樿閭欢閫氱煡', 1, NOW(), 1, NOW(), 0),
    (7402, 1, 'Webhook閫氱煡', 'webhook-default', 'webhook', JSON_OBJECT('url', 'https://example.com/iot/webhook', 'headers', JSON_OBJECT('Authorization', 'Bearer demo-token'), 'scenes', JSON_ARRAY('system_error', 'observability_alert', 'in_app_unread_bridge'), 'timeoutMs', 3000, 'minIntervalSeconds', 300), 1, 2, '榛樿Webhook閫氱煡锛堝惈 system_error銆乷bservability_alert 涓庨珮浼樻湭璇绘ˉ鎺ュ満鏅級', 1, NOW(), 1, NOW(), 0)
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
    (760101, 1, 'system', 'critical', '绯荤粺缁存姢绐楀彛鎻愰啋', '浠婃櫄 23:00 灏嗘墽琛屾棩蹇楅摼璺淮鎶わ紝璇锋彁鍓嶅畬鎴愭帓闅滃鍑恒€?, '骞冲彴璁″垝鍦ㄤ粖鏅?23:00 鑷?23:30 鎵ц鏃ュ織閾捐矾缁存姢銆傜淮鎶ゆ湡闂撮€氱煡涓績銆佸璁′腑蹇冧笌寮傚父瑙傛祴鍙板彲鑳藉瓨鍦ㄧ煭鏃跺欢杩燂紝璇疯繍缁翠笌鐮斿彂鍚屼簨鎻愬墠瀵煎嚭蹇呰淇℃伅銆?, 'all', NULL, NULL,
     '/system-log', 'manual', 'maintenance-20260321', MD5('manual|maintenance-20260321|all|system'), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, 1, 1, NOW(), 1, NOW(), 0),
    (760102, 1, 'business', 'high', '椋庨櫓杩愯惀鏃ユ姤寰呯‘璁?, '璇蜂笟鍔′笌绠＄悊瑙掕壊鍦?18:00 鍓嶇‘璁や粖鏃ュ憡璀﹂棴鐜儏鍐点€?, '椋庨櫓杩愯惀鏃ユ姤宸茬敓鎴愶紝璇蜂紭鍏堟牳瀵逛粖鏃ュ憡璀︾‘璁ょ巼銆佷簨浠舵淳宸ョ姸鎬佸拰寰呭叧闂簨椤广€傚瀛樺湪璺ㄧ彮娆℃湭闂幆闂锛岃鍦ㄤ簨浠跺崗鍚屽彴琛ュ厖鍙嶉銆?, 'role', 'BUSINESS_STAFF,MANAGEMENT_STAFF', NULL,
     '/alarm-center', 'manual', 'risk-report-20260321', MD5('manual|risk-report-20260321|role:BUSINESS_STAFF,MANAGEMENT_STAFF|business'), DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_ADD(NOW(), INTERVAL 2 DAY), 1, 2, 1, NOW(), 1, NOW(), 0),
    (760103, 1, 'error', 'high', '鎺ュ叆閾捐矾寮傚父鎺掓煡鎻愮ず', '妫€娴嬪埌鏈€杩?30 鍒嗛挓鍐呭瓨鍦?MQTT 鍒嗗彂澶辫触锛岃鐮斿彂鍜岃繍缁翠紭鍏堝鏍?TraceId銆?, '绯荤粺鍦ㄦ渶杩?30 鍒嗛挓鍐呮娴嬪埌 MQTT 鍒嗗彂澶辫触涓庤澶囦笉瀛樺湪寮傚父銆傝鍏堝湪寮傚父瑙傛祴鍙板畾浣?system_error锛屽啀鍒伴摼璺拷韪彴鎸?TraceId 澶嶆牳涓婁笅娓告棩蹇椼€?, 'role', 'OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN', NULL,
     '/message-trace', 'system_error', 'mqtt-dispatch-failed', MD5('system_error|mqtt-dispatch-failed|role:OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN|error'), DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_ADD(NOW(), INTERVAL 3 DAY), 1, 3, 1, NOW(), 1, NOW(), 0),
    (760104, 1, 'business', 'medium', '閫氱煡缂栨帓閰嶇疆澶嶆牳', '绠＄悊鍛樿澶嶆牳榛樿 webhook 娓犻亾鏄惁浠嶆寚鍚戞湁鏁堝湴鍧€銆?, '杩戞湡宸茶ˉ榻愮郴缁熷紓甯镐笌瑙勫垯鍖栬繍缁村憡璀﹂€氱煡鑳藉姏锛岃绠＄悊鍛樺鏍搁粯璁?webhook 鍦板潃銆佽秴鏃舵椂闂翠笌 system_error / observability_alert 鍦烘櫙閰嶇疆锛岄伩鍏嶇湡瀹炵幆澧冨紓甯告棤浜烘帴鏀躲€?, 'user', NULL, '1',
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
    (760301, 1, 'business', '浜у搧涓庤澶囧缓妗ｆ寚鍗?, '闈㈠悜涓氬姟涓庣鐞嗚鑹茬殑浜у搧寤烘。銆佽澶囧缓妗ｅ拰鐖跺瓙鎷撴墤缁存姢鎸囧紩銆?, '閫傜敤瑙掕壊锛氫笟鍔′汉鍛樸€佺鐞嗕汉鍛樸€傞€傜敤椤甸潰锛氫骇鍝佸畾涔変腑蹇冦€佽澶囪祫浜т腑蹇冦€備娇鐢ㄥ満鏅細鏂拌澶囧叆搴撱€侀」鐩笂绾垮墠寤烘。銆佺埗瀛愯澶囧叧绯荤淮鎶ゃ€傛搷浣滄楠わ細1. 鍏堝湪浜у搧瀹氫箟涓績纭 productKey銆佸崗璁€佽妭鐐圭被鍨嬪拰鍘傚晢鍙ｅ緞銆?. 浜у搧鍚敤鍚庡啀鍦ㄨ澶囪祫浜т腑蹇冩柊澧炶澶囷紝骞堕€氳繃 productKey 缁ф壙鍗忚涓庤妭鐐圭被鍨嬨€?. 瀛樺湪缃戝叧鎴栫埗璁惧鏃讹紝鍚屾椂缁存姢 parentDeviceId 鎴?parentDeviceCode銆?. 闇€瑕佹壒閲忓鍏ユ椂锛屽厛鏍稿浜у搧鍚敤鐘舵€佸拰鐖惰澶囩紪鐮併€傜粨鏋滃垽鏂細浜у搧鍒楄〃鍙煡璇紝璁惧璇︽儏鑳界湅鍒颁骇鍝併€佺埗璁惧鍜屽叧鑱旂綉鍏充俊鎭紝寤烘。鍚庡彲缁х画涓婃姤涓庝笅鍙戙€傚父瑙侀棶棰橈細浜у搧鍋滅敤鍚庝笉鑳界户缁缓妗ｏ紱浜у搧鍒犻櫎鍓嶉渶鍏堟竻绌哄叧鑱斿簱瀛樿澶囥€傚欢浼搁槄璇伙細浜у搧涓庤澶囧瓧娈靛彛寰勩€佸缓妗ｉ『搴忋€佺埗瀛愭嫇鎵戠淮鎶よ鍒欍€?, '浜у搧,璁惧,寤烘。,鎷撴墤,productKey', '/products,/devices', 'BUSINESS_STAFF,MANAGEMENT_STAFF',
     1, 1, 1, NOW(), 1, NOW(), 0),
    (760302, 1, 'business', '鍛婅纭銆佹姂鍒朵笌鍏抽棴鎿嶄綔', '闈㈠悜涓氬姟涓庣鐞嗚鑹茬殑鍛婅闂幆鎿嶄綔璇存槑銆?, '閫傜敤瑙掕壊锛氫笟鍔′汉鍛樸€佺鐞嗕汉鍛樸€傞€傜敤椤甸潰锛氬憡璀﹁繍钀ュ彴銆備娇鐢ㄥ満鏅細鏃ュ父鍛婅鐮斿垽銆佸€肩彮浜ゆ帴銆侀棴鐜鏍搞€傛搷浣滄楠わ細1. 鍏堝湪鍛婅鍒楄〃鎸夌瓑绾с€佺姸鎬佸拰鏃堕棿鑼冨洿绛涢€夌洰鏍囧憡璀︺€?. 纭寮傚父宸茶璇嗗埆鏃舵墽琛岀‘璁わ紝骞惰ˉ鍏呭缃鏄庛€?. 鏄庣‘鏃犻渶缁х画鎵撴壈鐨勫憡璀﹀彲鎵ц鎶戝埗銆?. 椋庨櫓宸叉秷闄や笖澶勭疆瀹屾垚鍚庡啀鎵ц鍏抽棴銆傜粨鏋滃垽鏂細鍛婅璇︽儏涓兘鐪嬪埌纭銆佹姂鍒舵垨鍏抽棴鍚庣殑鏈€鏂扮姸鎬佸拰澶勭疆鐥曡抗銆傚父瑙侀棶棰橈細鍏抽棴鍓嶅簲鍏堢‘璁ょ幇鍦烘垨鑱斿姩缁撴灉锛岄伩鍏嶆妸鏈鐞嗗畬鎴愮殑寮傚父鎻愬墠缁撴潫銆傚欢浼搁槄璇伙細鍛婅绛夌骇銆佷簨浠惰仈鍔ㄥ叧绯汇€佽繍钀ュ垎鏋愪腑蹇冮棴鐜粺璁″彛寰勩€?, '鍛婅,纭,鎶戝埗,鍏抽棴,闂幆', '/alarm-center', 'BUSINESS_STAFF,MANAGEMENT_STAFF',
     1, 2, 1, NOW(), 1, NOW(), 0),
    (760303, 1, 'business', '浜嬩欢娲惧伐銆佹帴鏀躲€佸鐞嗐€佸畬缁撴祦绋?, '闈㈠悜涓氬姟涓庣鐞嗚鑹茬殑浜嬩欢鍗忓悓闂幆鎸囧紩銆?, '閫傜敤瑙掕壊锛氫笟鍔′汉鍛樸€佺鐞嗕汉鍛樸€傞€傜敤椤甸潰锛氫簨浠跺崗鍚屽彴銆備娇鐢ㄥ満鏅細鍛婅杞簨浠跺悗鐨勬淳宸ャ€佹帴鏀躲€佸鐞嗗拰瀹岀粨璺熻釜銆傛搷浣滄楠わ細1. 鍦ㄤ簨浠跺垪琛ㄧ‘璁や簨浠剁瓑绾с€佹潵婧愬拰寰呭姙鐘舵€併€?. 绠＄悊浜哄憳鍏堝畬鎴愭淳宸ワ紝鏄庣‘璐熻矗浜哄拰澶勭悊瑕佹眰銆?. 鎵ц浜哄憳渚濇瀹屾垚鎺ユ敹銆佸紑濮嬪鐞嗐€佸鐞嗗弽棣堝拰瀹屾垚銆?. 缁撴灉澶嶆牳閫氳繃鍚庡啀鍏抽棴浜嬩欢銆傜粨鏋滃垽鏂細浜嬩欢璇︽儏鍙湅鍒版淳宸ヨ褰曘€佸伐鍗曠姸鎬佸拰鍙嶉鐣欑棔锛岃繍钀ュ垎鏋愪腑蹇冨彲缁熻闂幆缁撴灉銆傚父瑙侀棶棰橈細鑻ュ鐞嗕腑鍙戠幇鏉′欢鍙樺寲锛屽簲鍏堣ˉ鍙嶉鍐嶈皟鏁存淳宸ワ紝涓嶈璺宠繃鐘舵€佹祦杞€傚欢浼搁槄璇伙細鍛婅鍒颁簨浠堕棴鐜€佸伐鍗曞鐞嗙姸鎬併€佷簨浠跺叧闂粺璁″彛寰勩€?, '浜嬩欢,娲惧伐,宸ュ崟,鎺ユ敹,瀹岀粨', '/event-disposal', 'BUSINESS_STAFF,MANAGEMENT_STAFF',
     1, 3, 1, NOW(), 1, NOW(), 0),
    (760304, 1, 'business', '椋庨櫓瀵硅薄銆侀槇鍊肩瓥鐣ャ€佽仈鍔ㄩ妗堥厤缃鏄?, '闈㈠悜涓氬姟涓庣鐞嗚鑹茬殑椋庨櫓绛栫暐缂栨帓璇存槑銆?, '閫傜敤瑙掕壊锛氫笟鍔′汉鍛樸€佺鐞嗕汉鍛樸€傞€傜敤椤甸潰锛氶闄╁璞′腑蹇冦€侀槇鍊肩瓥鐣ャ€佽仈鍔ㄧ紪鎺掋€佸簲鎬ラ妗堝簱銆備娇鐢ㄥ満鏅細鏂板椋庨櫓瀵硅薄銆侀厤缃憡璀﹂槇鍊笺€佸畾涔夎仈鍔ㄥ姩浣滃拰搴旀€ラ妗堛€傛搷浣滄楠わ細1. 鍏堝湪椋庨櫓瀵硅薄涓績瀹屾垚椋庨櫓鐐瑰拰璁惧娴嬬偣缁戝畾銆?. 鍐嶉厤缃槇鍊肩瓥鐣ワ紝鏄庣‘鎸囨爣銆佽〃杈惧紡鍜屽憡璀︾瓑绾с€?. 闇€瑕佽嚜鍔ㄥ缃椂锛岀户缁ˉ榻愯仈鍔ㄨ鍒欎笌搴旀€ラ妗堛€?. 涓婄嚎鍓嶇粨鍚堢湡瀹炶澶囨垨婕旂ず鏁版嵁楠岃瘉鍛戒腑鏁堟灉銆傜粨鏋滃垽鏂細椋庨櫓瀵硅薄銆佽鍒欍€佽仈鍔ㄥ拰棰勬閮藉彲鐙珛鏌ヨ锛屽懡涓粨鏋滀細鍦ㄥ憡璀︽垨浜嬩欢鐣欑棔涓綋鐜般€傚父瑙侀棶棰橈細鑻ヨ鑹叉殏鏃犲搴旇彍鍗曟潈闄愶紝甯姪涓績涓嶄細鎺ㄨ崘鏃犳潈璁块棶鐨勭瓥鐣ヨ祫鏂欍€傚欢浼搁槄璇伙細椋庨櫓鐐圭粦瀹氥€侀槇鍊艰鍒欍€佽仈鍔ㄨ鍒欍€佸簲鎬ラ妗堟帴鍙ｄ笌娴佺▼銆?, '椋庨櫓瀵硅薄,闃堝€?鑱斿姩,棰勬,绛栫暐', '/risk-point,/rule-definition,/linkage-rule,/emergency-plan', 'BUSINESS_STAFF,MANAGEMENT_STAFF',
     1, 4, 1, NOW(), 1, NOW(), 0),
    (760305, 1, 'business', '杩愯惀鍒嗘瀽涓績鎸囨爣鏌ョ湅璇存槑', '闈㈠悜涓氬姟涓庣鐞嗚鑹茬殑鎶ヨ〃鏌ョ湅鎸囧紩銆?, '閫傜敤瑙掕壊锛氫笟鍔′汉鍛樸€佺鐞嗕汉鍛樸€傞€傜敤椤甸潰锛氳繍钀ュ垎鏋愪腑蹇冦€備娇鐢ㄥ満鏅細鏃ユ姤銆佸懆鎶ャ€侀棴鐜鐩樺拰绠＄悊澶嶆牳銆傛搷浣滄楠わ細1. 鎸夋椂闂磋寖鍥存煡鐪嬮闄╄秼鍔裤€佸憡璀︾粺璁°€佷簨浠堕棴鐜拰璁惧鍋ュ悍銆?. 瀵规瘮寮傚父娉㈠嘲涓庨棴鐜晥鐜囷紝瀹氫綅闇€瑕佽窡杩涚殑椋庨櫓鐐规垨宸ュ崟銆?. 蹇呰鏃跺洖鍒板憡璀﹁繍钀ュ彴鎴栦簨浠跺崗鍚屽彴缁х画鏍稿疄鏄庣粏銆傜粨鏋滃垽鏂細鑳藉湪鍚屼竴椤甸潰瀹屾垚瓒嬪娍瑙傚療銆佸憡璀︾粺璁″拰闂幆缁撴灉澶嶆牳锛屽苟涓庝笟鍔″缃〉闈㈠舰鎴愬線杩斻€傚父瑙侀棶棰橈細鎶ヨ〃鐢ㄤ簬杩愯惀澶嶇洏锛屼笉鏇夸唬鍘熷鍛婅鎴栦簨浠惰鎯呫€傚欢浼搁槄璇伙細椋庨櫓瓒嬪娍銆佸憡璀︾粺璁°€佷簨浠堕棴鐜€佽澶囧仴搴峰洓绫诲垎鏋愭帴鍙ｃ€?, '鎶ヨ〃,杩愯惀鍒嗘瀽,椋庨櫓瓒嬪娍,鍛婅缁熻,浜嬩欢闂幆', '/report-analysis', 'BUSINESS_STAFF,MANAGEMENT_STAFF',
     1, 5, 1, NOW(), 1, NOW(), 0),
    (760306, 1, 'technical', 'HTTP 涓婃姤涓庨摼璺獙璇佷腑蹇冧娇鐢ㄨ鏄?, '闈㈠悜杩愮淮涓庣爺鍙戣鑹茬殑 HTTP 妯℃嫙涓婃姤涓庨摼璺獙璇佽祫鏂欍€?, '閫傜敤瑙掕壊锛氳繍缁翠汉鍛樸€佸紑鍙戜汉鍛樸€佽秴绾х鐞嗗憳銆傞€傜敤椤甸潰锛氶摼璺獙璇佷腑蹇冦€備娇鐢ㄥ満鏅細鑱旇皟 HTTP 涓婃姤銆侀獙璇佸崗璁В鐮併€佸鏍稿睘鎬у拰娑堟伅鏃ュ織鏄惁钀藉簱銆傛搷浣滄楠わ細1. 鍦ㄩ摼璺獙璇佷腑蹇冮€夋嫨鏄庢枃鎴栧瘑鏂囨ā寮忋€?. 鏄庢枃妯″紡鎸?C.1銆丆.2銆丆.3 缁勭粐姝ｆ枃锛屽瘑鏂囨ā寮忕洿鎺ヤ紶灏佸寘 JSON銆?. 鍙戦€佸悗绔嬪嵆鏌ョ湅灞炴€ф煡璇€佹秷鎭棩蹇楀拰璁惧鍦ㄧ嚎鐘舵€佹槸鍚﹀埛鏂般€?. 濡傚け璐ワ紝鍐嶇粨鍚?TraceId 鍒板紓甯歌娴嬪彴鍜岄摼璺拷韪彴缁х画鎺掓煡銆傜粨鏋滃垽鏂細鎺ュ彛杩斿洖鎴愬姛锛岃澶囧睘鎬с€佹秷鎭棩蹇楀拰鍦ㄧ嚎鐘舵€佸悓姝ユ洿鏂般€傚父瑙侀棶棰橈細鏄庢枃浜岃繘鍒舵ā鎷熻鎸夊崟瀛楄妭缂栫爜鍙戦€侊紝瀵嗘枃妯″紡鍒欑洿鎺ヤ紶鍘熷灏佸寘銆傚欢浼搁槄璇伙細HTTP 涓婃姤鍏ュ彛銆侀摼璺獙璇佷腑蹇冩槑鏂?瀵嗘枃妯″紡銆佸睘鎬т笌娑堟伅鏃ュ織鏌ヨ銆?, 'HTTP,閾捐矾楠岃瘉,涓婃姤,鏄庢枃,瀵嗘枃', '/reporting', 'OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN',
     1, 6, 1, NOW(), 1, NOW(), 0),
    (760307, 1, 'technical', 'MQTT Topic 瑙勮寖涓?mqtt-json / $dp 鍏煎璇存槑', '闈㈠悜杩愮淮涓庣爺鍙戣鑹茬殑 MQTT 鎺ュ叆瑙勮寖璇存槑銆?, '閫傜敤瑙掕壊锛氳繍缁翠汉鍛樸€佸紑鍙戜汉鍛樸€佽秴绾х鐞嗗憳銆傞€傜敤椤甸潰锛氶摼璺獙璇佷腑蹇冦€侀摼璺拷韪彴銆備娇鐢ㄥ満鏅細鑱旇皟鏍囧噯 Topic銆佸吋瀹瑰巻鍙?`$dp`銆佸畾浣嶄笂琛屾姤鏂囨牸寮忎笉鍖归厤闂銆傛搷浣滄楠わ細1. 浼樺厛纭璁惧浣跨敤鏍囧噯 `/sys/{productKey}/{deviceCode}/thing/.../post` Topic 杩樻槸鍘嗗彶 `$dp`銆?. 鏍囧噯 Topic 鍦烘櫙鎸変骇鍝佸崗璁€夋嫨 `mqtt-json` 绛夐€傞厤鍣ㄣ€?. 鍘嗗彶 `$dp` 鍦烘櫙閲嶇偣鏍稿瑙ｅ瘑銆佹祴鐐规槧灏勫拰瀛愯澶囨媶鍒嗐€?. 鑻ヤ笅琛岄獙璇佸け璐ワ紝鍐嶅鏍哥洰鏍囦骇鍝佺姸鎬佸拰涓嬪彂 Topic銆傜粨鏋滃垽鏂細娑堟伅鍙姝ｇ‘瑙ｇ爜锛屽睘鎬у拰鏃ュ織鎸夌洰鏍囪澶囪惤搴擄紝蹇呰鏃舵敮鎸佷笅琛屾渶灏忓彂甯冦€傚父瑙侀棶棰橈細鍗忚銆乀opic銆乸roductKey 鍜?deviceCode 浠讳竴涓嶄竴鑷达紝閮藉彲鑳藉鑷磋澶囦笉瀛樺湪鎴栧垎鍙戝け璐ャ€傚欢浼搁槄璇伙細MQTT Topic 瑙勮寖銆乣mqtt-json` 瑙ｇ爜銆乣$dp` 鍘嗗彶鍏煎閾捐矾銆?, 'MQTT,Topic,mqtt-json,$dp,鍗忚', '/reporting,/message-trace', 'OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN',
     1, 7, 1, NOW(), 1, NOW(), 0),
    (760308, 1, 'technical', 'TraceId銆侀摼璺拷韪彴銆佸紓甯歌娴嬪彴鎺掗殰璇存槑', '闈㈠悜杩愮淮涓庣爺鍙戣鑹茬殑缁熶竴鎺掗殰璺緞璇存槑銆?, '閫傜敤瑙掕壊锛氳繍缁翠汉鍛樸€佸紑鍙戜汉鍛樸€佽秴绾х鐞嗗憳銆傞€傜敤椤甸潰锛氶摼璺拷韪彴銆佸紓甯歌娴嬪彴銆備娇鐢ㄥ満鏅細鍑虹幇 MQTT 鍒嗗彂澶辫触銆佽澶囦笉瀛樺湪銆佸悗鍙板紓甯告垨鎺ュ彛鎶ラ敊鏃跺揩閫熶覆鑱斾笂涓嬫父閾捐矾銆傛搷浣滄楠わ細1. 鍏堣幏鍙栬姹傛垨鏃ュ織閲岀殑 TraceId銆?. 鍦ㄥ紓甯歌娴嬪彴鏌ョ湅鏄惁瀛樺湪 system_error 璁板綍銆?. 鍐嶅埌閾捐矾杩借釜鍙版寜 TraceId銆佽澶囩紪鐮併€佷骇鍝佹爣璇嗘垨 Topic 妫€绱㈡秷鎭棩蹇椼€?. 缁撳悎鎺ュ彛杩斿洖鍜岃澶囧睘鎬х粨鏋滅‘璁ゆ晠闅滆惤鐐广€傜粨鏋滃垽鏂細鑳藉鎶?HTTP銆丮QTT銆佺郴缁熷紓甯稿拰娑堟伅鏃ュ織涓叉垚鍚屼竴鏉℃帓闅滈摼璺€傚父瑙侀棶棰橈細娌℃湁 TraceId 鏃跺彲鍏堟寜璁惧缂栫爜鎴?Topic 缂╁皬鑼冨洿锛屽啀鍥炴函瀵瑰簲寮傚父璁板綍銆傚欢浼搁槄璇伙細X-Trace-Id銆乻ystem_error 瀹¤銆佹秷鎭拷韪笌娑堟伅鏃ュ織鏌ヨ銆?, 'TraceId,閾捐矾杩借釜,寮傚父瑙傛祴,system_error,鎺掗殰', '/message-trace,/system-log', 'OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN',
     1, 8, 1, NOW(), 1, NOW(), 0),
    (760309, 1, 'technical', '鐪熷疄鐜鍚姩銆佺幆澧冨彉閲忎笌渚濊禆妫€鏌ヨ鏄?, '闈㈠悜杩愮淮涓庣爺鍙戣鑹茬殑鐪熷疄鐜鍚姩鍓嶆鏌ユ竻鍗曘€?, '閫傜敤瑙掕壊锛氳繍缁翠汉鍛樸€佸紑鍙戜汉鍛樸€佽秴绾х鐞嗗憳銆傞€傜敤椤甸潰锛氶摼璺獙璇佷腑蹇冦€佸紓甯歌娴嬪彴銆備娇鐢ㄥ満鏅細鏈湴鑱旇皟銆佸叡浜幆澧冩帓闅溿€佸巻鍙插簱鍗囩骇鍚庡楠屻€傛搷浣滄楠わ細1. 鍏堢‘璁ゅ敮涓€鍚姩妯″潡浠嶄负 `spring-boot-iot-admin`銆?. 浠?`application-dev.yml` 涓哄熀绾挎鏌?MySQL銆丷edis銆乀Dengine銆丮QTT 绛変緷璧栨槸鍚﹀彲杈俱€?. 闇€瑕佽鐩栭粯璁ら厤缃椂锛屼紭鍏堜娇鐢ㄧ幆澧冨彉閲忋€?. 鍘嗗彶搴撹嫢缂哄皯绯荤粺鍐呭鑳藉姏鎴栨不鐞嗚彍鍗曪紝鍏堣ˉ榻愬搴斿垵濮嬪寲鎴栧崌绾ф暟鎹啀楠屾敹銆傜粨鏋滃垽鏂細鍚庣鑳芥寜 `dev` 閰嶇疆鍚姩锛屽墠绔彲姝ｅ父璁块棶 `/api/system/help-doc/**`銆乣/api/system/in-app-message/**` 鍜屼富瑕佷笟鍔℃帴鍙ｃ€傚父瑙侀棶棰橈細鐪熷疄鐜涓嶅彲鐢ㄦ椂鍙兘璁板綍涓虹幆澧冮樆濉烇紝涓嶈兘鍥為€€鍒版棫 H2 楠屾敹璺緞銆傚欢浼搁槄璇伙細杩愯鍩虹嚎銆佸叧閿幆澧冨彉閲忋€佸巻鍙插簱鍗囩骇涓庢帓闅滃叆鍙ｃ€?, '鍚姩,鐜鍙橀噺,渚濊禆妫€鏌?application-dev,楠屾敹', '/reporting,/system-log', 'OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN',
     1, 9, 1, NOW(), 1, NOW(), 0),
    (760310, 1, 'faq', 'FAQ锛氫骇鍝佸拰璁惧鏈変粈涔堝尯鍒?, '缁熶竴瑙ｉ噴浜у搧銆佽澶囥€佺埗瀛愭嫇鎵戝拰寤烘。椤哄簭銆?, '閫傜敤瑙掕壊锛氭墍鏈夌櫥褰曠敤鎴枫€傞€傜敤椤甸潰锛氫骇鍝佸畾涔変腑蹇冦€佽澶囪祫浜т腑蹇冦€備娇鐢ㄥ満鏅細棣栨鎺ヨЕ璁惧寤烘。銆佽В閲婁骇鍝佹ā鏉垮拰鐜板満璧勪骇瀹炰緥鐨勫樊寮傘€傛搷浣滄楠わ細1. 鍏堟妸浜у搧鐞嗚В涓烘帴鍏ユā鏉裤€?. 鍐嶆妸璁惧鐞嗚В涓虹幇鍦鸿祫浜у疄渚嬨€?. 鏈夌埗瀛愭嫇鎵戞椂锛屾妸鍏崇郴缁存姢鍦ㄨ澶囦晶鑰屼笉鏄骇鍝佷晶銆傜粨鏋滃垽鏂細鑳藉鍖哄垎鍝簺淇℃伅灞炰簬浜у搧涓绘暟鎹紝鍝簺淇℃伅灞炰簬璁惧瀹炰緥淇℃伅銆傚父瑙侀棶棰橈細鍚屼竴椤圭洰涓嬪鍙板悓绫昏澶囬€氬父鍏辩敤涓€涓骇鍝侊紝涓嶅洜瀹夎浣嶇疆涓嶅悓閲嶅寤轰骇鍝併€傚欢浼搁槄璇伙細浜у搧涓庤澶囬€昏緫鍏崇郴銆佸瓧娈靛彛寰勩€佸缓妗ｉ『搴忋€?, '浜у搧,璁惧,FAQ,寤烘。,鎷撴墤', '/products,/devices', '',
     1, 10, 1, NOW(), 1, NOW(), 0),
    (760311, 1, 'faq', 'FAQ锛氫负浠€涔堟垜鐪嬩笉鍒版煇涓〉闈㈡垨甯姪鏂囨。', '璇存槑鑿滃崟鏉冮檺銆佽鑹茶寖鍥村拰甯姪璧勬枡杩囨护瑙勫垯銆?, '閫傜敤瑙掕壊锛氭墍鏈夌櫥褰曠敤鎴枫€傞€傜敤椤甸潰锛氶€氱敤銆備娇鐢ㄥ満鏅細褰撳墠璐﹀彿鑳界櫥褰曪紝浣嗙湅涓嶅埌鐩爣椤甸潰銆佹寜閽垨甯姪璧勬枡鏃躲€傛搷浣滄楠わ細1. 鍏堢‘璁ゅ綋鍓嶈处鍙疯鑹叉槸鍚﹀叿澶囩洰鏍囪彍鍗曟巿鏉冦€?. 鍐嶇‘璁ゅ府鍔╂枃妗ｆ槸鍚﹂厤缃簡鍙瑙掕壊鎴栧叧鑱旈〉闈㈣矾寰勩€?. 濡傛灉椤甸潰鏈韩鏃犳潈璁块棶锛屽府鍔╀腑蹇冧篃涓嶄細缁х画鎺ㄨ崘瀵瑰簲璧勬枡銆傜粨鏋滃垽鏂細瑙掕壊鍜岃彍鍗曟巿鏉冭ˉ榻愬悗锛岄〉闈㈠叆鍙ｅ拰甯姪璧勬枡浼氫竴璧锋仮澶嶅彲瑙併€傚父瑙侀棶棰橈細甯姪涓績鎸夋潈闄愯繃婊ゅ唴瀹癸紝鏁呮剰涓嶅睍绀衡€滅湅寰楀埌浣嗙偣涓嶈繘鍘烩€濈殑浼叆鍙ｃ€傚欢浼搁槄璇伙細瑙掕壊榛樿鑼冨洿銆佽彍鍗曟巿鏉冦€佸府鍔╂枃妗ｅ彲瑙佹€ц鍒欍€?, '鏉冮檺,瑙掕壊,鑿滃崟,甯姪鏂囨。,鍙鎬?, NULL, '',
     1, 11, 1, NOW(), 1, NOW(), 0),
    (760312, 1, 'faq', 'FAQ锛氶€氱煡涓績涓庡府鍔╀腑蹇冩€庝箞鐢?, '璇存槑鍙充笂瑙掑３灞傚叆鍙ｇ殑鍒嗙被瑙勫垯鍜屼娇鐢ㄦ柟寮忋€?, '閫傜敤瑙掕壊锛氭墍鏈夌櫥褰曠敤鎴枫€傞€傜敤椤甸潰锛氶€氱敤銆備娇鐢ㄥ満鏅細棣栨浣跨敤澶撮儴澹冲眰鍏ュ彛鎴栭渶瑕佸揩閫熸煡鎵炬秷鎭€佸府鍔╄祫鏂欐椂銆傛搷浣滄楠わ細1. 鍦ㄥ彸涓婅鎵撳紑閫氱煡涓績鏌ョ湅绯荤粺浜嬩欢銆佷笟鍔′簨浠跺拰閿欒浜嬩欢銆?. 鍦ㄥ府鍔╀腑蹇冩煡鐪嬩笟鍔＄被銆佹妧鏈被鍜?FAQ 璧勬枡銆?. 闇€瑕佹洿澶氬唴瀹规椂锛岄€氳繃鈥滄煡鐪嬫洿澶氣€濊繘鍏ュ垪琛ㄦ娊灞夛紝鍐嶅仛鍒嗙被绛涢€夊拰鍏抽敭瀛楁悳绱€傜粨鏋滃垽鏂細鑳戒粠鎽樿闈㈡澘杩涘叆鍒楄〃鍜岃鎯咃紝骞惰烦杞埌鍏宠仈椤甸潰缁х画澶勭悊銆傚父瑙侀棶棰橈細甯姪涓績鍙睍绀哄綋鍓嶈处鍙风湡姝ｆ湁鏉冭闂殑璧勬枡锛岄€氱煡宸茶涔熼渶瑕佹樉寮忔搷浣溿€傚欢浼搁槄璇伙細澹冲眰鎽樿銆佸垪琛ㄦ娊灞夈€佽鎯呮娊灞変笌鏉冮檺杩囨护瑙勫垯銆?, '閫氱煡涓績,甯姪涓績,FAQ,澹冲眰,鎼滅储', NULL, '',
     1, 12, 1, NOW(), 1, NOW(), 0),
    (760313, 1, 'faq', 'FAQ锛氫负浠€涔堜細鍑虹幇 401銆佹棤鏉冮檺鎴栫郴缁熷唴瀹圭己琛ㄦ彁绀?, '璇存槑甯歌璁よ瘉銆佹巿鏉冨拰鐜鍒濆鍖栭棶棰樼殑鎺掓煡鏂瑰悜銆?, '閫傜敤瑙掕壊锛氭墍鏈夌櫥褰曠敤鎴枫€傞€傜敤椤甸潰锛氶€氱敤銆備娇鐢ㄥ満鏅細鎺ュ彛杩斿洖 `401`銆侀〉闈㈡彁绀烘棤鏉冮檺锛屾垨绯荤粺鍐呭鎺ュ彛鎻愮ず缂哄皯渚濊禆鏁版嵁鏃躲€傛搷浣滄楠わ細1. `401` 鍦烘櫙鍏堥噸鏂扮櫥褰曞苟纭鍓嶇宸叉惡甯?Bearer token銆?. 鏃犳潈闄愬満鏅厛妫€鏌ヨ鑹层€佽彍鍗曞拰鎸夐挳鎺堟潈銆?. 绯荤粺鍐呭渚濊禆缂哄け鏃讹紝璇风鐞嗗憳琛ラ綈甯姪鏂囨。鍜岀珯鍐呮秷鎭墍闇€鐨勬暟鎹〃涓庡垵濮嬪寲鏁版嵁銆傜粨鏋滃垽鏂細璁よ瘉鎭㈠鍚庢帴鍙ｅ彲璁块棶锛屾巿鏉冭ˉ榻愬悗椤甸潰鍏ュ彛涓庢寜閽仮澶嶏紝绯荤粺鍐呭鍒濆鍖栧畬鎴愬悗甯姪涓績鍜岄€氱煡涓績鍙甯歌繑鍥炴暟鎹€傚父瑙侀棶棰橈細鐜闃诲瑕佹槑纭褰曚负鐜闂锛屼笉鑳界敤鏃ч獙鏀惰矾寰勬浛浠ｇ湡瀹炵幆澧冦€傚欢浼搁槄璇伙細閴存潈瑙勮寖銆佽彍鍗曟巿鏉冦€佺湡瀹炵幆澧冨垵濮嬪寲涓庡崌绾ц鏄庛€?, '401,鏃犳潈闄?绯荤粺鍐呭,鍒濆鍖?FAQ', NULL, '',
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
-- 4) 椋庨櫓骞冲彴锛堝憡璀?浜嬩欢/椋庨櫓鐐癸級
-- =========================
INSERT INTO risk_point (
    id, risk_point_code, risk_point_name, org_id, org_name, region_id, region_name, responsible_user, responsible_phone,
    risk_level, risk_type, location_text, longitude, latitude, description, status, tenant_id, create_by, create_time, update_by, update_time, deleted
) VALUES
    (8001, 'RP-HP-001', '閿呯倝娓╁帇鐩戞祴鐐?, 7101, '骞冲彴杩愮淮涓績', 7002, '榛勬郸鍘傚尯', 1, '13800000000', 'red', 'GENERAL', NULL, NULL, NULL, '閿呯倝鍖洪珮娓╅珮鍘嬮闄╃洃娴?, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8002, 'RP-HP-002', '鎸姩鐩戞祴鐐?, 7102, '鍛婅澶勭疆缁?, 7002, '榛勬郸鍘傚尯', 1, '13800000000', 'orange', 'GENERAL', NULL, NULL, NULL, '鍏抽敭璁惧鎸姩椋庨櫓鐩戞祴', 0, 1, 1, NOW(), 1, NOW(), 0)
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
    (8501, 'RP-HW-SLOPE-001', 'G7011鍗佸ぉ楂橀€烱595', 7111, '鎴愬幙鎵€', 621221, '鎴愬幙', 1, '13800000000', 'blue', 'SLOPE', 'SXK595+818-SX595+960', 105.65682, 33.698381, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8502, 'RP-HW-SLOPE-002', 'G22闈掑叞楂橀€熷钩瀹氭K1458+75', 7116, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗钩鍑夊垎鍏徃', 621026, '瀹佸幙', 1, '13800000000', 'blue', 'SLOPE', 'G22', 107.75108, 35.33165, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8503, 'RP-HW-SLOPE-003', 'G2012瀹氭楂橀€焁K450+182', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 620622, '鍙ゆ氮鍘?, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8504, 'RP-HW-TUNNEL-001', 'G8513骞崇坏楂橀€熸垚姝︽SK384+870-SK384+920', 7112, '鎴愭鎵€', 621202, '姝﹂兘鍖?, 1, '13800000000', 'blue', 'TUNNEL', 'G8513', 105.09881, 33.469798, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8505, 'RP-HW-SLOPE-004', 'S38鐜嬪楂橀€烱14+650', 7115, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗕复澶忓垎鍏徃', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'S38', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8506, 'RP-HW-SLOPE-005', 'S32涓村ぇ楂橀€烱10+176-K10+560', 7115, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗕复澶忓垎鍏徃', 622901, '涓村甯?, 1, '13800000000', 'blue', 'SLOPE', 'S32', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8507, 'RP-HW-SLOPE-006', 'G30杩為湇楂橀€熸爲寰愭K1740+300', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G30', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8508, 'RP-HW-SLOPE-007', 'G30杩為湇楂橀€熸煶蹇犳K1689+900-K1690+300', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G30', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8509, 'RP-HW-SLOPE-008', 'G6浜棌楂橀€熷垬鐧芥K1414+770-K1415+550', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G6', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8510, 'RP-HW-SLOPE-009', 'G75鍏版捣楂橀€熷叞涓存K14+325', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 620103, '涓冮噷娌冲尯', 1, '13800000000', 'blue', 'SLOPE', 'G75', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8511, 'RP-HW-SLOPE-010', 'G75鍏版捣楂橀€熷叞涓存K11+325', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 620103, '涓冮噷娌冲尯', 1, '13800000000', 'blue', 'SLOPE', 'G75', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8512, 'RP-HW-SLOPE-011', 'G75鍏版捣楂橀€熷叞涓存K14+149.5', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 620103, '涓冮噷娌冲尯', 1, '13800000000', 'blue', 'SLOPE', 'G75', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8513, 'RP-HW-BRIDGE-001', 'G6 浜棌楂橀€熷叞娴锋K1661+350', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 620111, '绾㈠彜鍖?, 1, '13800000000', 'blue', 'BRIDGE', 'G6', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8514, 'RP-HW-SLOPE-012', 'G6浜棌楂橀€熺櫧鍏版K1548+810', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 620122, '鐨嬪叞鍘?, 1, '13800000000', 'blue', 'SLOPE', 'G6', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8515, 'RP-HW-SLOPE-013', 'G2201鍗楃粫鍩庨珮閫烱39+800', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 620104, '瑗垮浐鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G2201', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8516, 'RP-HW-SLOPE-014', 'G30杩為湇楂橀€熸煶蹇犳K1695+500锝濳1699+800', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 620102, '鍩庡叧鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G30', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8517, 'RP-HW-BRIDGE-002', 'G568鍏版案涓€绾75+253', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 622923, '姘搁潠鍘?, 1, '13800000000', 'blue', 'BRIDGE', 'G568', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8518, 'RP-HW-BRIDGE-003', 'G3011鏌虫牸楂橀€烱74+830', 7120, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘暒鐓屽垎鍏徃', 620201101, '宄硥闀?, 1, '13800000000', 'blue', 'BRIDGE', 'G3011', 95.896419, 40.517818, 'G3011鏌虫牸楂橀€烱74+830', 0, 1, 1, NOW(), 1, NOW(), 0),
    (8519, 'RP-HW-SLOPE-015', 'G2012瀹氭楂橀€烻K394+700锝濻K394+900', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 620602, '鍑夊窞鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8520, 'RP-HW-SLOPE-016', 'G2012瀹氭楂橀€烱393+170', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 620602, '鍑夊窞鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8521, 'RP-HW-SLOPE-017', 'G2012瀹氭楂橀€烱413+446', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 620602, '鍑夊窞鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8522, 'RP-HW-SLOPE-018', 'G2012瀹氭楂橀€烱400+315', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 620602, '鍑夊窞鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8523, 'RP-HW-SLOPE-019', 'G2012瀹氭楂橀€烱392+410', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 620602, '鍑夊窞鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8524, 'RP-HW-SLOPE-020', 'G2012瀹氭楂橀€烱392+790', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8525, 'RP-HW-SLOPE-021', 'G2012瀹氭楂橀€烱479+665', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8526, 'RP-HW-SLOPE-022', 'G2012瀹氭楂橀€烱465+175', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8527, 'RP-HW-SLOPE-023', 'G2012瀹氭楂橀€烱462+995', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8528, 'RP-HW-SLOPE-024', 'G2012瀹氭楂橀€烱420+891', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8529, 'RP-HW-SLOPE-025', 'G2012瀹氭楂橀€烱437+982', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8530, 'RP-HW-SLOPE-026', 'G2012瀹氭楂橀€烻K450+182', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G2012', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8531, 'RP-HW-SLOPE-027', 'G3017閲戞楂橀€烱67+050-K67+700', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G3017', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8532, 'RP-HW-SLOPE-028', 'G3017閲戞楂橀€烱68+860-K69+100', 7117, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', NULL, NULL, 1, '13800000000', 'blue', 'SLOPE', 'G3017', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8533, 'RP-HW-BRIDGE-004', 'G22闈掑叞楂橀€熷穳鏌虫SK1856+400-500妗ユ', 7119, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗畾瑗垮垎鍏徃', 620123, '姒嗕腑鍘?, 1, '13800000000', 'blue', 'BRIDGE', 'G22', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8534, 'RP-HW-TUNNEL-002', 'G22闈掑叞楂橀€熷穳鏌虫SK1839+928闅ч亾', 7119, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗畾瑗垮垎鍏徃', 620123, '姒嗕腑鍘?, 1, '13800000000', 'blue', 'TUNNEL', 'G22', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8535, 'RP-HW-SLOPE-029', 'G22闈掑叞楂橀€熷穳鏌虫K1858+250娌夐櫡', 7119, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗畾瑗垮垎鍏徃', 620123, '姒嗕腑鍘?, 1, '13800000000', 'blue', 'SLOPE', 'G22', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8536, 'RP-HW-SLOPE-030', 'G22闈掑叞楂橀€熷穳鏌虫K1855+575娌夐櫡', 7119, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗畾瑗垮垎鍏徃', 620123, '姒嗕腑鍘?, 1, '13800000000', 'blue', 'SLOPE', 'G22', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8537, 'RP-HW-TUNNEL-003', 'G22闈掑叞楂橀€熼浄瑗挎K1374+670-K1374+780', 7116, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗钩鍑夊垎鍏徃', 621021, '搴嗗煄鍘?, 1, '13800000000', 'blue', 'TUNNEL', 'G22', 107.70219, 35.904263, '妫氭礊璁捐鏍囧噯鍙楁帶浜庡簡鍩庨毀閬撹璁°€傞毀閬撹璁¤溅閫?0km/h锛屾寜鍒嗙寮忚璁★紝涓娿€佷笅琛岄棿璺濅负36m 宸﹀彸锛屾瘡骞呬负鍗曞悜鍙岃溅閬擄紝妫氭礊閲囩敤鍙樻埅闈㈡嫳闂ㄥ紡渚у锛屼晶澧欏唴杞粨鍗婂緞 5.43m锛屼晶澧欒窛鐢电紗妲介《闈㈤珮搴?3.1m锛岄噰鐢ㄩ挗绛嬫贩鍑濆湡鎵╁ぇ鍩虹锛屾í鍚戣 C25 閽㈢瓔娣峰嚌鍦熸敮鎾戞锛?m 闂磋窛璁句竴閬擄紝妯悜鏀拺姊佷笌渚у娣峰嚌鍦熶竴浣撴祰绛戝畬鎴愩€傛娲炴嫳椤堕噰鐢?I18 閽㈡嫳鏋朵负楠ㄦ灦锛岀旱鍚戦棿璺濅负 3.0m锛岄挗鎷?鏋朵笂娌跨旱鍚戦摵璁綶10 妲介挗锛屾Ы閽笌閽㈡嫳鏋剁剨鎺ワ紝椤堕儴閾鸿钃濊壊閬厜鏉匡紝閲囩敤铻烘爴涓庢Ы閽㈣繛鎺ャ€傛娲為珮濉柟璺熀涓嬭閽㈡尝绾圭娑碉紝灏嗘矡鍐呮眹姘存帓鑷充笅娓告矡璋枫€?         缁忚皟鏌ワ紝涓婁笅琛岀嚎妫氭礊鐥呭鎯呭喌鐩歌繎锛屼富瑕佺梾瀹充负妫氭礊鎷卞寮€瑁傘€佸眬閮ㄦ贩鍑濆湡鍓ヨ惤锛岃缂濇渶澶?瀹藉害杈?2.2cm锛屼釜鍒儴浣嶆嫳鍦堟暣浣撳墺钀斤紝鎷卞澶ч潰绉笚姘淬€佹硾纰变弗閲嶏紝娑傝鍓ヨ殌娉涚⒈涓ラ噸銆?, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8538, 'RP-HW-SLOPE-031', 'G8513骞崇坏楂橀€熷钩澶╂YK54+925', 7116, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗钩鍑夊垎鍏徃', 620881, '鍗庝涵甯?, 1, '13800000000', 'blue', 'SLOPE', 'G8513', 106.58477, 35.17848, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8539, 'RP-HW-BRIDGE-005', 'G8513骞崇坏楂橀€熷钩澶╂K176+953', 7118, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 620522, '绉﹀畨鍘?, 1, '13800000000', 'blue', 'BRIDGE', 'G8513', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8540, 'RP-HW-SLOPE-032', 'G30杩為湇楂橀€熷疂澶╂K1329+165', 7118, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 620503, '楹︾Н鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G30', 106.13355, 34.35039, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8541, 'RP-HW-BRIDGE-006', 'G30杩為湇楂橀€熷疂澶╂K1306+742', 7118, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 620503, '楹︾Н鍖?, 1, '13800000000', 'blue', 'BRIDGE', 'G30', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8542, 'RP-HW-BRIDGE-007', 'G30杩為湇楂橀€熷疂澶╂K1310+835', 7118, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 620503, '楹︾Н鍖?, 1, '13800000000', 'blue', 'BRIDGE', 'G30', 106.31153, 34.318538, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8543, 'RP-HW-BRIDGE-008', 'G30杩為湇楂橀€熷疂澶╂K1324+528', 7118, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 620503, '楹︾Н鍖?, 1, '13800000000', 'blue', 'BRIDGE', 'G30', 106.16548, 34.336527, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8544, 'RP-HW-BRIDGE-009', 'G30杩為湇楂橀€熷疂澶╂K1289+900', 7118, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 620503, '楹︾Н鍖?, 1, '13800000000', 'blue', 'BRIDGE', 'G30', 106.51702, 34.336102, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8545, 'RP-HW-TUNNEL-004', 'G8513骞崇坏楂橀€熸垚姝︽XK368+300-XK368+350', 7112, '鎴愭鎵€', 621202, '姝﹂兘鍖?, 1, '13800000000', 'blue', 'TUNNEL', 'G8513', 105.27762, 33.43379, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8546, 'RP-HW-TUNNEL-005', 'G8513骞崇坏楂橀€熸垚姝︽XK385+260-XK385+360', 7112, '鎴愭鎵€', 621202, '姝﹂兘鍖?, 1, '13800000000', 'blue', 'TUNNEL', 'G8513', 105.10659, 33.467689, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8547, 'RP-HW-BRIDGE-010', 'G8513骞崇坏楂橀€熸垚姝︽K396+935', 7112, '鎴愭鎵€', 621202, '姝﹂兘鍖?, 1, '13800000000', 'blue', 'BRIDGE', 'G8513', 105.01642, 33.488265, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8548, 'RP-HW-SLOPE-033', 'G75鍏版捣楂橀€熷叞涓存K22+200-K22+400婊戝潯', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 620102, '鍩庡叧鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G75', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8549, 'RP-HW-SLOPE-034', 'G8513骞崇坏楂橀€熸垚姝︽K374+960姘存瘉', 7112, '鎴愭鎵€', 621202, '姝﹂兘鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G8513', 105.21959, 33.436574, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8550, 'RP-HW-SLOPE-035', 'G7011鍗佸ぉ楂橀€烱653+900', 7111, '鎴愬幙鎵€', 621225, '瑗垮拰鍘?, 1, '13800000000', 'blue', 'SLOPE', 'G7011', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8551, 'RP-HW-SLOPE-036', 'G568鍏版案涓€绾78+965-K79+140', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 620102, '鍩庡叧鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G568', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8552, 'RP-HW-SLOPE-037', 'G7011鍗佸ぉ楂橀€烱646+945', 7111, '鎴愬幙鎵€', 621225, '瑗垮拰鍘?, 1, '13800000000', 'blue', 'SLOPE', 'G7011', NULL, NULL, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8553, 'RP-HW-SLOPE-038', 'G7011鍗佸ぉ楂橀€烱583+445婊戝潯', 7111, '鎴愬幙鎵€', 621221, '鎴愬幙', 1, '13800000000', 'blue', 'SLOPE', 'G7011', 105.77499, 33.741783, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8554, 'RP-HW-BRIDGE-011', 'G75 鍏版捣楂橀€熸缃愭K522+510姘存瘉', 7113, '姝﹂兘鎵€', 621222, '鏂囧幙', 1, '13800000000', 'blue', 'BRIDGE', 'G75', 105.41266, 32.762192, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8555, 'RP-HW-SLOPE-039', 'G6浜棌楂橀€熷叞娴锋K1652+225宕╁', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 620104, '瑗垮浐鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G6', 103.20301, 36.17702, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8556, 'RP-HW-SLOPE-040', 'G1816 涔岀帥楂橀€熷悍涓存K745+950-K745+990闅ч亾', 7115, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗕复澶忓垎鍏徃', 622925, '鍜屾斂鍘?, 1, '13800000000', 'blue', 'SLOPE', 'G1816', 103.32034, 35.452103, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8557, 'RP-HW-SLOPE-041', 'G1816涔岀帥楂橀€熷悍涓存K753+240姘存瘉', 7115, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗕复澶忓垎鍏徃', 622925, '鍜屾斂鍘?, 1, '13800000000', 'blue', 'SLOPE', 'G1816', 103.248, 35.481186, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8558, 'RP-HW-SLOPE-042', 'G22闈掑叞楂橀€熷钩瀹氭K1652+855姘存瘉', 7119, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗畾瑗垮垎鍏徃', 621102, '瀹夊畾鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G22', 105.78201, 35.546959, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8559, 'RP-HW-SLOPE-043', 'G8513骞崇坏楂橀€熸垚姝︽K333+520', 7112, '鎴愭鎵€', 621202, '姝﹂兘鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G8513', 105.46746, 33.639921, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8560, 'RP-HW-BRIDGE-012', 'G8513骞崇坏楂橀€熸垚姝︽K401+694', 7112, '鎴愭鎵€', 621202, '姝﹂兘鍖?, 1, '13800000000', 'blue', 'BRIDGE', 'G8513', 104.95312, 33.462721, NULL, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8561, 'RP-HW-SLOPE-044', 'G30杩為湇楂橀€熷疂澶╂K1323+880娉ョ煶娴?, 7118, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 620503, '楹︾Н鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G30', 106.16524, 34.336093, 'G30 杩為湇楂橀€熷疂楦¤嚦澶╂按娈典笅琛岀嚎 K1323+880 澶勶紝浣嶄簬鐢樿們鐪佸ぉ姘村競楹︾Н鍖哄厷宸濋晣銆備笅琛岀嚎宸︿晶涓庢偿鐭虫祦娌熷彛鍩烘湰鍛堟浜ゅ叧绯伙紝閲囩敤 2脳2m 娑垫礊瀵兼帓娉ョ煶娴併€?, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8562, 'RP-HW-SLOPE-045', 'G6浜棌楂橀€烱1623+400婊戝潯', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 620111, '绾㈠彜鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G6', 103.8313, 36.066116, 'G6 浜棌楂橀€熷叕璺叞娴锋 K1623+300锝濳1623+400 娈靛彸渚ц竟鍧℃粦濉岋紝浣嶄簬鐢樿們鐪佸叞宸炲競瑗垮浐鍖烘渤鍙ｄ埂銆傞」鐩尯鍦板闄囪タ榛勫湡楂樺師鍚戦潚钘忛珮鍘熻繃搴﹀甫锛屼负榛勫湡宄佹娌熷鐩搁棿鍦拌矊锛屽北楂樺潯闄★紝娌熷绾垫í锛屽憟娌熻胺鍦板舰锛屾矡璋锋繁涓斿鍛堚€淰鈥濆瓧鈥淲鈥濆瓧鍨嬫柇闈紝鎬讳綋鍦扮牬纰庛€傚潯浣撴琚瀬涓虹█灏戯紝灞遍《澶氫负榛勫湡瑕嗙洊锛屼笅閮ㄥ绾㈡偿宀╁嚭闇层€?, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8563, 'RP-HW-BRIDGE-013', 'G30杩為湇楂橀€?K1731+077', 7114, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 620121, '姘哥櫥鍘?, 1, '13800000000', 'blue', 'BRIDGE', 'G30', 103.59, 36.4, 'G30杩為湇楂橀€烱1731+077 鍖濋亾妗ワ紝浣嶄簬鐢樿們鐪佸叞宸炲競姘哥櫥鍘挎爲灞忛晣銆?椤圭洰鍖哄湴澶勯檱瑗块粍鍦熼珮鍘熷悜闈掕棌楂樺師杩囧害甯︼紝涓洪粍鍦熷硜姊佹矡澹戠浉闂村湴璨岋紝娌熷绾垫í锛屽憟娌熻胺鍦板舰锛屾矡璋锋繁涓斿鍛堚€淰鈥濆瓧鈥淲鈥濆瓧鍨嬫柇闈紝鎬讳綋鍦板舰鐮寸銆傚潯浣撴琚瀬涓虹█灏戯紝灞遍《澶氫负榛勫湡瑕嗙洊锛屼笅閮ㄥ绾㈡偿宀╁嚭闇层€?, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8564, 'RP-HW-SLOPE-046', 'G30杩為湇楂橀€烱1328+500', 7118, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 620503, '楹︾Н鍖?, 1, '13800000000', 'blue', 'SLOPE', 'G30', 106.13379, 34.344941, 'G30 杩為湇楂橀€熷叕璺疂澶╂ SK1328+800~SK1328+950 娈靛彸渚т复娌宠矾鍫ゆ按姣?浣嶄簬鐢樿們鐪佸ぉ姘村競楹︾Н鍖哄厷宸濅埂銆?璺浣嶄簬涓や腑灞遍棿鐨勬案瀹佹渤(鍏氬窛娌?娌宠胺鍖猴紝娌冲簥杈冨紑闃旓紝涓哄北鍖哄父骞存€ф渤娴侊紝鍖哄唴鍦板舰璧蜂紡涓嶅ぇ锛屽湴闈㈡爣楂樹粙浜?1488.40~1595.10m锛屽厷宸濇渤涓ゅ哺灞变綋鍩哄博闇插ご杈冨锛?灞变綋鍧″害绾?48掳寰湴璨屽崟鍏冧负灞遍棿娌宠胺鍐叉椽绉強灞遍棿娲潯绉鍦拌矊銆?, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8565, 'RP-HW-SLOPE-047', 'G75 鍏版捣楂橀€烱134+738杈瑰潯', 7119, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗畾瑗垮垎鍏徃', 621123, '娓簮鍘?, 1, '13800000000', 'blue', 'SLOPE', 'G75', 103.79258, 36.087916, 'G75 鍏版捣楂橀€烱134+738 婊戝潯锛屼綅浜庣敇鑲冪渷瀹氳タ甯傛腑婧愬幙娓呮簮闀囷紝涓滃寳渚ф灉鍥潙鐢冲灞便€?灞辫剨璧板悜杩戜笢瑗垮悜锛屽湴璨屽睘浜庝綆灞变笜闄靛湴璨岋紝鏂滃潯楂樼▼鍦?2180~2330 涔嬮棿锛屽潯浣撲笅閮ㄥ钩鍧囧潯搴︾害 30掳锛屼腑閮ㄥ钩鍧囧潯搴︾害 8掳銆?, 0, 1, 1, NOW(), 1, NOW(), 0)
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
    (9501, 8501, 'G7011鍗佸ぉ楂橀€烱595', '杈瑰潯', NULL, 'SXK595+818-SX595+960', '鍗佸ぉ', '楂橀€熷叕璺?, NULL, '621221', '["62","6212","621221"]', '鎴愬幙鎵€', 2, 1, 1, NOW(), 1, NOW(), 0),
    (9502, 8502, 'G22闈掑叞楂橀€熷钩瀹氭K1458+75', '杈瑰潯', NULL, 'G22', '闈掑叞楂橀€?, '楂橀€熷叕璺?, NULL, '621026', '["62","6210","621026"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗钩鍑夊垎鍏徃', 3, 1, 1, NOW(), 1, NOW(), 0),
    (9503, 8503, 'G2012瀹氭楂橀€焁K450+182', '杈瑰潯', NULL, 'G2012', '瀹氭楂橀€?, '楂橀€熷叕璺?, NULL, '620622', '["62","6206","620622"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 4, 1, 1, NOW(), 1, NOW(), 0),
    (9504, 8504, 'G8513骞崇坏楂橀€熸垚姝︽SK384+870-SK384+920', '闅ч亾', NULL, 'G8513', '骞崇坏楂橀€?, '楂橀€熷叕璺?, NULL, '621202', '["62","6212","621202"]', '鎴愭鎵€', 5, 1, 1, NOW(), 1, NOW(), 0),
    (9505, 8505, 'S38鐜嬪楂橀€烱14+650', '杈瑰潯', NULL, 'S38', '鐜嬪楂橀€?, '楂橀€熷叕璺?, NULL, NULL, NULL, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗕复澶忓垎鍏徃', 6, 1, 1, NOW(), 1, NOW(), 0),
    (9506, 8506, 'S32涓村ぇ楂橀€烱10+176-K10+560', '杈瑰潯', NULL, 'S32', '涓村ぇ楂橀€?, '楂橀€熷叕璺?, NULL, '622901', '["62","6229","622901"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗕复澶忓垎鍏徃', 7, 1, 1, NOW(), 1, NOW(), 0),
    (9507, 8507, 'G30杩為湇楂橀€熸爲寰愭K1740+300', '杈瑰潯', NULL, 'G30', '杩為湇楂橀€?, '楂橀€熷叕璺?, NULL, NULL, NULL, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 8, 1, 1, NOW(), 1, NOW(), 0),
    (9508, 8508, 'G30杩為湇楂橀€熸煶蹇犳K1689+900-K1690+300', '杈瑰潯', NULL, 'G30', '杩為湇楂橀€?, '楂橀€熷叕璺?, NULL, NULL, NULL, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 9, 1, 1, NOW(), 1, NOW(), 0),
    (9509, 8509, 'G6浜棌楂橀€熷垬鐧芥K1414+770-K1415+550', '杈瑰潯', NULL, 'G6', '浜棌楂橀€?, '楂橀€熷叕璺?, NULL, NULL, NULL, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 10, 1, 1, NOW(), 1, NOW(), 0),
    (9510, 8510, 'G75鍏版捣楂橀€熷叞涓存K14+325', '杈瑰潯', NULL, 'G75', '鍏版捣楂橀€?, '楂橀€熷叕璺?, NULL, '620103', '["62","6201","620103"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 11, 1, 1, NOW(), 1, NOW(), 0),
    (9511, 8511, 'G75鍏版捣楂橀€熷叞涓存K11+325', '杈瑰潯', NULL, 'G75', '鍏版捣楂橀€?, '楂橀€熷叕璺?, NULL, '620103', '["62","6201","620103"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 12, 1, 1, NOW(), 1, NOW(), 0),
    (9512, 8512, 'G75鍏版捣楂橀€熷叞涓存K14+149.5', '杈瑰潯', NULL, 'G75', '鍏版捣楂橀€?, '楂橀€熷叕璺?, NULL, '620103', '["62","6201","620103"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 13, 1, 1, NOW(), 1, NOW(), 0),
    (9513, 8513, 'G6 浜棌楂橀€熷叞娴锋K1661+350', '妗ユ', NULL, 'G6', '浜棌楂橀€?, '楂橀€熷叕璺?, NULL, '620111', '["62","6201","620111"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 14, 1, 1, NOW(), 1, NOW(), 0),
    (9514, 8514, 'G6浜棌楂橀€熺櫧鍏版K1548+810', '杈瑰潯', NULL, 'G6', '浜棌楂橀€?, '楂橀€熷叕璺?, NULL, '620122', '["62","6201","620122"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 15, 1, 1, NOW(), 1, NOW(), 0),
    (9515, 8515, 'G2201鍗楃粫鍩庨珮閫烱39+800', '杈瑰潯', NULL, 'G2201', '鍗楃粫鍩庨珮閫?, '楂橀€熷叕璺?, NULL, '620104', '["62","6201","620104"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 16, 1, 1, NOW(), 1, NOW(), 0),
    (9516, 8516, 'G30杩為湇楂橀€熸煶蹇犳K1695+500锝濳1699+800', '杈瑰潯', NULL, 'G30', '杩為湇楂橀€?, '楂橀€熷叕璺?, NULL, '620102', '["62","6201","620102"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 17, 1, 1, NOW(), 1, NOW(), 0),
    (9517, 8517, 'G568鍏版案涓€绾75+253', '妗ユ', NULL, 'G568', '鍏版案涓€绾?, '涓€绾у叕璺?, NULL, '622923', '["62","6229","622923"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 18, 1, 1, NOW(), 1, NOW(), 0),
    (9518, 8518, 'G3011鏌虫牸楂橀€烱74+830', '妗ユ', 'G3011鏌虫牸楂橀€烱74+830', 'G3011', '鏌虫牸楂橀€?, '楂橀€熷叕璺?, NULL, '620201101', '["62","6202","620201101"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘暒鐓屽垎鍏徃', 19, 1, 1, NOW(), 1, NOW(), 0),
    (9519, 8519, 'G2012瀹氭楂橀€烻K394+700锝濻K394+900', '杈瑰潯', NULL, 'G2012', '瀹氭楂橀€?, '楂橀€熷叕璺?, NULL, '620602', '["62","6206","620602"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 20, 1, 1, NOW(), 1, NOW(), 0),
    (9520, 8520, 'G2012瀹氭楂橀€烱393+170', '杈瑰潯', NULL, 'G2012', '瀹氭楂橀€?, '楂橀€熷叕璺?, NULL, '620602', '["62","6206","620602"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 21, 1, 1, NOW(), 1, NOW(), 0),
    (9521, 8521, 'G2012瀹氭楂橀€烱413+446', '杈瑰潯', NULL, 'G2012', '瀹氭楂橀€?, '楂橀€熷叕璺?, NULL, '620602', '["62","6206","620602"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 22, 1, 1, NOW(), 1, NOW(), 0),
    (9522, 8522, 'G2012瀹氭楂橀€烱400+315', '杈瑰潯', NULL, 'G2012', '瀹氭楂橀€?, '楂橀€熷叕璺?, NULL, '620602', '["62","6206","620602"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 23, 1, 1, NOW(), 1, NOW(), 0),
    (9523, 8523, 'G2012瀹氭楂橀€烱392+410', '杈瑰潯', NULL, 'G2012', '瀹氭楂橀€?, '楂橀€熷叕璺?, NULL, '620602', '["62","6206","620602"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 24, 1, 1, NOW(), 1, NOW(), 0),
    (9524, 8524, 'G2012瀹氭楂橀€烱392+790', '杈瑰潯', NULL, 'G2012', '瀹氭楂橀€?, '楂橀€熷叕璺?, NULL, NULL, NULL, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 25, 1, 1, NOW(), 1, NOW(), 0),
    (9525, 8525, 'G2012瀹氭楂橀€烱479+665', '杈瑰潯', NULL, 'G2012', '瀹氭楂橀€?, '楂橀€熷叕璺?, NULL, NULL, NULL, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 26, 1, 1, NOW(), 1, NOW(), 0),
    (9526, 8526, 'G2012瀹氭楂橀€烱465+175', '杈瑰潯', NULL, 'G2012', '瀹氭楂橀€?, '楂橀€熷叕璺?, NULL, NULL, NULL, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 27, 1, 1, NOW(), 1, NOW(), 0),
    (9527, 8527, 'G2012瀹氭楂橀€烱462+995', '杈瑰潯', NULL, 'G2012', '瀹氭楂橀€?, '楂橀€熷叕璺?, NULL, NULL, NULL, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 28, 1, 1, NOW(), 1, NOW(), 0),
    (9528, 8528, 'G2012瀹氭楂橀€烱420+891', '杈瑰潯', NULL, 'G2012', '瀹氭楂橀€?, '楂橀€熷叕璺?, NULL, NULL, NULL, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 29, 1, 1, NOW(), 1, NOW(), 0),
    (9529, 8529, 'G2012瀹氭楂橀€烱437+982', '杈瑰潯', NULL, 'G2012', '瀹氭楂橀€?, '楂橀€熷叕璺?, NULL, NULL, NULL, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 30, 1, 1, NOW(), 1, NOW(), 0),
    (9530, 8530, 'G2012瀹氭楂橀€烻K450+182', '杈瑰潯', NULL, 'G2012', '瀹氭楂橀€?, '楂橀€熷叕璺?, NULL, NULL, NULL, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 31, 1, 1, NOW(), 1, NOW(), 0),
    (9531, 8531, 'G3017閲戞楂橀€烱67+050-K67+700', '杈瑰潯', NULL, 'G3017', '閲戞楂橀€?, '楂橀€熷叕璺?, NULL, NULL, NULL, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 32, 1, 1, NOW(), 1, NOW(), 0),
    (9532, 8532, 'G3017閲戞楂橀€烱68+860-K69+100', '杈瑰潯', NULL, 'G3017', '閲戞楂橀€?, '楂橀€熷叕璺?, NULL, NULL, NULL, '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗘濞佸垎鍏徃', 33, 1, 1, NOW(), 1, NOW(), 0),
    (9533, 8533, 'G22闈掑叞楂橀€熷穳鏌虫SK1856+400-500妗ユ', '妗ユ', NULL, 'G22', '闈掑叞楂橀€?, '楂橀€熷叕璺?, NULL, '620123', '["62","6201","620123"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗畾瑗垮垎鍏徃', 34, 1, 1, NOW(), 1, NOW(), 0),
    (9534, 8534, 'G22闈掑叞楂橀€熷穳鏌虫SK1839+928闅ч亾', '闅ч亾', NULL, 'G22', '闈掑叞楂橀€?, '楂橀€熷叕璺?, NULL, '620123', '["62","6201","620123"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗畾瑗垮垎鍏徃', 35, 1, 1, NOW(), 1, NOW(), 0),
    (9535, 8535, 'G22闈掑叞楂橀€熷穳鏌虫K1858+250娌夐櫡', '杈瑰潯', NULL, 'G22', '闈掑叞楂橀€?, '楂橀€熷叕璺?, NULL, '620123', '["62","6201","620123"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗畾瑗垮垎鍏徃', 36, 1, 1, NOW(), 1, NOW(), 0),
    (9536, 8536, 'G22闈掑叞楂橀€熷穳鏌虫K1855+575娌夐櫡', '杈瑰潯', NULL, 'G22', '闈掑叞楂橀€?, '楂橀€熷叕璺?, NULL, '620123', '["62","6201","620123"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗畾瑗垮垎鍏徃', 37, 1, 1, NOW(), 1, NOW(), 0),
    (9537, 8537, 'G22闈掑叞楂橀€熼浄瑗挎K1374+670-K1374+780', '闅ч亾', '妫氭礊璁捐鏍囧噯鍙楁帶浜庡簡鍩庨毀閬撹璁°€傞毀閬撹璁¤溅閫?0km/h锛屾寜鍒嗙寮忚璁★紝涓娿€佷笅琛岄棿璺濅负36m 宸﹀彸锛屾瘡骞呬负鍗曞悜鍙岃溅閬擄紝妫氭礊閲囩敤鍙樻埅闈㈡嫳闂ㄥ紡渚у锛屼晶澧欏唴杞粨鍗婂緞 5.43m锛屼晶澧欒窛鐢电紗妲介《闈㈤珮搴?3.1m锛岄噰鐢ㄩ挗绛嬫贩鍑濆湡鎵╁ぇ鍩虹锛屾í鍚戣 C25 閽㈢瓔娣峰嚌鍦熸敮鎾戞锛?m 闂磋窛璁句竴閬擄紝妯悜鏀拺姊佷笌渚у娣峰嚌鍦熶竴浣撴祰绛戝畬鎴愩€傛娲炴嫳椤堕噰鐢?I18 閽㈡嫳鏋朵负楠ㄦ灦锛岀旱鍚戦棿璺濅负 3.0m锛岄挗鎷?鏋朵笂娌跨旱鍚戦摵璁綶10 妲介挗锛屾Ы閽笌閽㈡嫳鏋剁剨鎺ワ紝椤堕儴閾鸿钃濊壊閬厜鏉匡紝閲囩敤铻烘爴涓庢Ы閽㈣繛鎺ャ€傛娲為珮濉柟璺熀涓嬭閽㈡尝绾圭娑碉紝灏嗘矡鍐呮眹姘存帓鑷充笅娓告矡璋枫€?         缁忚皟鏌ワ紝涓婁笅琛岀嚎妫氭礊鐥呭鎯呭喌鐩歌繎锛屼富瑕佺梾瀹充负妫氭礊鎷卞寮€瑁傘€佸眬閮ㄦ贩鍑濆湡鍓ヨ惤锛岃缂濇渶澶?瀹藉害杈?2.2cm锛屼釜鍒儴浣嶆嫳鍦堟暣浣撳墺钀斤紝鎷卞澶ч潰绉笚姘淬€佹硾纰变弗閲嶏紝娑傝鍓ヨ殌娉涚⒈涓ラ噸銆?, 'G22', '闈掑叞楂橀€?, '楂橀€熷叕璺?, NULL, '621021', '["62","6210","621021"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗钩鍑夊垎鍏徃', 38, 1, 1, NOW(), 1, NOW(), 0),
    (9538, 8538, 'G8513骞崇坏楂橀€熷钩澶╂YK54+925', '杈瑰潯', NULL, 'G8513', '骞崇坏楂橀€?, '楂橀€熷叕璺?, NULL, '620881', '["62","6208","620881"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗钩鍑夊垎鍏徃', 39, 1, 1, NOW(), 1, NOW(), 0),
    (9539, 8539, 'G8513骞崇坏楂橀€熷钩澶╂K176+953', '妗ユ', NULL, 'G8513', '骞崇坏楂橀€?, '楂橀€熷叕璺?, NULL, '620522', '["62","6205","620522"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 40, 1, 1, NOW(), 1, NOW(), 0),
    (9540, 8540, 'G30杩為湇楂橀€熷疂澶╂K1329+165', '杈瑰潯', NULL, 'G30', '杩為湇楂橀€?, '楂橀€熷叕璺?, NULL, '620503', '["62","6205","620503"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 41, 1, 1, NOW(), 1, NOW(), 0),
    (9541, 8541, 'G30杩為湇楂橀€熷疂澶╂K1306+742', '妗ユ', NULL, 'G30', '杩為湇楂橀€?, '楂橀€熷叕璺?, NULL, '620503', '["62","6205","620503"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 42, 1, 1, NOW(), 1, NOW(), 0),
    (9542, 8542, 'G30杩為湇楂橀€熷疂澶╂K1310+835', '妗ユ', NULL, 'G30', '杩為湇楂橀€?, '楂橀€熷叕璺?, NULL, '620503', '["62","6205","620503"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 43, 1, 1, NOW(), 1, NOW(), 0),
    (9543, 8543, 'G30杩為湇楂橀€熷疂澶╂K1324+528', '妗ユ', NULL, 'G30', '杩為湇楂橀€?, '楂橀€熷叕璺?, NULL, '620503', '["62","6205","620503"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 44, 1, 1, NOW(), 1, NOW(), 0),
    (9544, 8544, 'G30杩為湇楂橀€熷疂澶╂K1289+900', '妗ユ', NULL, 'G30', '杩為湇楂橀€?, '楂橀€熷叕璺?, NULL, '620503', '["62","6205","620503"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 45, 1, 1, NOW(), 1, NOW(), 0),
    (9545, 8545, 'G8513骞崇坏楂橀€熸垚姝︽XK368+300-XK368+350', '闅ч亾', NULL, 'G8513', '骞崇坏楂橀€?, '楂橀€熷叕璺?, NULL, '621202', '["62","6212","621202"]', '鎴愭鎵€', 46, 1, 1, NOW(), 1, NOW(), 0),
    (9546, 8546, 'G8513骞崇坏楂橀€熸垚姝︽XK385+260-XK385+360', '闅ч亾', NULL, 'G8513', '骞崇坏楂橀€?, '楂橀€熷叕璺?, NULL, '621202', '["62","6212","621202"]', '鎴愭鎵€', 47, 1, 1, NOW(), 1, NOW(), 0),
    (9547, 8547, 'G8513骞崇坏楂橀€熸垚姝︽K396+935', '妗ユ', NULL, 'G8513', '骞崇坏楂橀€?, '楂橀€熷叕璺?, NULL, '621202', '["62","6212","621202"]', '鎴愭鎵€', 48, 1, 1, NOW(), 1, NOW(), 0),
    (9548, 8548, 'G75鍏版捣楂橀€熷叞涓存K22+200-K22+400婊戝潯', '杈瑰潯', NULL, 'G75', '鍏版捣楂橀€?, '楂橀€熷叕璺?, NULL, '620102', '["62","6201","620102"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 49, 1, 1, NOW(), 1, NOW(), 0),
    (9549, 8549, 'G8513骞崇坏楂橀€熸垚姝︽K374+960姘存瘉', '杈瑰潯', NULL, 'G8513', '骞崇坏楂橀€?, '楂橀€熷叕璺?, NULL, '621202', '["62","6212","621202"]', '鎴愭鎵€', 50, 1, 1, NOW(), 1, NOW(), 0),
    (9550, 8550, 'G7011鍗佸ぉ楂橀€烱653+900', '杈瑰潯', NULL, 'G7011', '鍗佸ぉ楂橀€?, '楂橀€熷叕璺?, NULL, '621225', '["62","6212","621225"]', '鎴愬幙鎵€', 51, 1, 1, NOW(), 1, NOW(), 0),
    (9551, 8551, 'G568鍏版案涓€绾78+965-K79+140', '杈瑰潯', NULL, 'G568', '鍏版案涓€绾?, '涓€绾у叕璺?, NULL, '620102', '["62","6201","620102"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 52, 1, 1, NOW(), 1, NOW(), 0),
    (9552, 8552, 'G7011鍗佸ぉ楂橀€烱646+945', '杈瑰潯', NULL, 'G7011', '鍗佸ぉ楂橀€?, '楂橀€熷叕璺?, NULL, '621225', '["62","6212","621225"]', '鎴愬幙鎵€', 53, 1, 1, NOW(), 1, NOW(), 0),
    (9553, 8553, 'G7011鍗佸ぉ楂橀€烱583+445婊戝潯', '杈瑰潯', NULL, 'G7011', '鍗佸ぉ楂橀€?, '楂橀€熷叕璺?, NULL, '621221', '["62","6212","621221"]', '鎴愬幙鎵€', 54, 1, 1, NOW(), 1, NOW(), 0),
    (9554, 8554, 'G75 鍏版捣楂橀€熸缃愭K522+510姘存瘉', '妗ユ', NULL, 'G75', '鍏版捣楂橀€?, '楂橀€熷叕璺?, NULL, '621222', '["62","6212","621222"]', '姝﹂兘鎵€', 55, 1, 1, NOW(), 1, NOW(), 0),
    (9555, 8555, 'G6浜棌楂橀€熷叞娴锋K1652+225宕╁', '杈瑰潯', NULL, 'G6', '浜棌楂橀€?, '涓€绾у叕璺?, NULL, '620104', '["62","6201","620104"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 56, 1, 1, NOW(), 1, NOW(), 0),
    (9556, 8556, 'G1816 涔岀帥楂橀€熷悍涓存K745+950-K745+990闅ч亾', '杈瑰潯', NULL, 'G1816', '涔岀帥楂橀€?, '楂橀€熷叕璺?, NULL, '622925', '["62","6229","622925"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗕复澶忓垎鍏徃', 57, 1, 1, NOW(), 1, NOW(), 0),
    (9557, 8557, 'G1816涔岀帥楂橀€熷悍涓存K753+240姘存瘉', '杈瑰潯', NULL, 'G1816', '涔岀帥楂橀€?, '楂橀€熷叕璺?, NULL, '622925', '["62","6229","622925"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗕复澶忓垎鍏徃', 58, 1, 1, NOW(), 1, NOW(), 0),
    (9558, 8558, 'G22闈掑叞楂橀€熷钩瀹氭K1652+855姘存瘉', '杈瑰潯', NULL, 'G22', '闈掑叞楂橀€?, '楂橀€熷叕璺?, NULL, '621102', '["62","6211","621102"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗畾瑗垮垎鍏徃', 59, 1, 1, NOW(), 1, NOW(), 0),
    (9559, 8559, 'G8513骞崇坏楂橀€熸垚姝︽K333+520', '杈瑰潯', NULL, 'G8513', '骞崇坏楂橀€?, '楂橀€熷叕璺?, NULL, '621202', '["62","6212","621202"]', '鎴愭鎵€', 60, 1, 1, NOW(), 1, NOW(), 0),
    (9560, 8560, 'G8513骞崇坏楂橀€熸垚姝︽K401+694', '妗ユ', NULL, 'G8513', '骞崇坏楂橀€?, '楂橀€熷叕璺?, NULL, '621202', '["62","6212","621202"]', '鎴愭鎵€', 61, 1, 1, NOW(), 1, NOW(), 0),
    (9561, 8561, 'G30杩為湇楂橀€熷疂澶╂K1323+880娉ョ煶娴?, '杈瑰潯', 'G30 杩為湇楂橀€熷疂楦¤嚦澶╂按娈典笅琛岀嚎 K1323+880 澶勶紝浣嶄簬鐢樿們鐪佸ぉ姘村競楹︾Н鍖哄厷宸濋晣銆備笅琛岀嚎宸︿晶涓庢偿鐭虫祦娌熷彛鍩烘湰鍛堟浜ゅ叧绯伙紝閲囩敤 2脳2m 娑垫礊瀵兼帓娉ョ煶娴併€?, 'G30', '杩為湇楂橀€?, '楂橀€熷叕璺?, NULL, '620503', '["62","6205","620503"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 62, 1, 1, NOW(), 1, NOW(), 0),
    (9562, 8562, 'G6浜棌楂橀€烱1623+400婊戝潯', '杈瑰潯', 'G6 浜棌楂橀€熷叕璺叞娴锋 K1623+300锝濳1623+400 娈靛彸渚ц竟鍧℃粦濉岋紝浣嶄簬鐢樿們鐪佸叞宸炲競瑗垮浐鍖烘渤鍙ｄ埂銆傞」鐩尯鍦板闄囪タ榛勫湡楂樺師鍚戦潚钘忛珮鍘熻繃搴﹀甫锛屼负榛勫湡宄佹娌熷鐩搁棿鍦拌矊锛屽北楂樺潯闄★紝娌熷绾垫í锛屽憟娌熻胺鍦板舰锛屾矡璋锋繁涓斿鍛堚€淰鈥濆瓧鈥淲鈥濆瓧鍨嬫柇闈紝鎬讳綋鍦扮牬纰庛€傚潯浣撴琚瀬涓虹█灏戯紝灞遍《澶氫负榛勫湡瑕嗙洊锛屼笅閮ㄥ绾㈡偿宀╁嚭闇层€?, 'G6', '浜棌楂橀€?, '楂橀€熷叕璺?, NULL, '620111', '["62","6201","620111"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 63, 1, 1, NOW(), 1, NOW(), 0),
    (9563, 8563, 'G30杩為湇楂橀€?K1731+077', '妗ユ', 'G30杩為湇楂橀€烱1731+077 鍖濋亾妗ワ紝浣嶄簬鐢樿們鐪佸叞宸炲競姘哥櫥鍘挎爲灞忛晣銆?椤圭洰鍖哄湴澶勯檱瑗块粍鍦熼珮鍘熷悜闈掕棌楂樺師杩囧害甯︼紝涓洪粍鍦熷硜姊佹矡澹戠浉闂村湴璨岋紝娌熷绾垫í锛屽憟娌熻胺鍦板舰锛屾矡璋锋繁涓斿鍛堚€淰鈥濆瓧鈥淲鈥濆瓧鍨嬫柇闈紝鎬讳綋鍦板舰鐮寸銆傚潯浣撴琚瀬涓虹█灏戯紝灞遍《澶氫负榛勫湡瑕嗙洊锛屼笅閮ㄥ绾㈡偿宀╁嚭闇层€?, 'G30', '杩為湇楂橀€?, '楂橀€熷叕璺?, NULL, '620121', '["62","6201","620121"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗叞宸炲垎鍏徃', 64, 1, 1, NOW(), 1, NOW(), 0),
    (9564, 8564, 'G30杩為湇楂橀€烱1328+500', '杈瑰潯', 'G30 杩為湇楂橀€熷叕璺疂澶╂ SK1328+800~SK1328+950 娈靛彸渚т复娌宠矾鍫ゆ按姣?浣嶄簬鐢樿們鐪佸ぉ姘村競楹︾Н鍖哄厷宸濅埂銆?璺浣嶄簬涓や腑灞遍棿鐨勬案瀹佹渤(鍏氬窛娌?娌宠胺鍖猴紝娌冲簥杈冨紑闃旓紝涓哄北鍖哄父骞存€ф渤娴侊紝鍖哄唴鍦板舰璧蜂紡涓嶅ぇ锛屽湴闈㈡爣楂樹粙浜?1488.40~1595.10m锛屽厷宸濇渤涓ゅ哺灞变綋鍩哄博闇插ご杈冨锛?灞变綋鍧″害绾?48掳寰湴璨屽崟鍏冧负灞遍棿娌宠胺鍐叉椽绉強灞遍棿娲潯绉鍦拌矊銆?, 'G30', '杩為湇楂橀€?, '楂橀€熷叕璺?, NULL, '620503', '["62","6205","620503"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗ぉ姘村垎鍏徃', 65, 1, 1, NOW(), 1, NOW(), 0),
    (9565, 8565, 'G75 鍏版捣楂橀€烱134+738杈瑰潯', '杈瑰潯', 'G75 鍏版捣楂橀€烱134+738 婊戝潯锛屼綅浜庣敇鑲冪渷瀹氳タ甯傛腑婧愬幙娓呮簮闀囷紝涓滃寳渚ф灉鍥潙鐢冲灞便€?灞辫剨璧板悜杩戜笢瑗垮悜锛屽湴璨屽睘浜庝綆灞变笜闄靛湴璨岋紝鏂滃潯楂樼▼鍦?2180~2330 涔嬮棿锛屽潯浣撲笅閮ㄥ钩鍧囧潯搴︾害 30掳锛屼腑閮ㄥ钩鍧囧潯搴︾害 8掳銆?, 'G75', '鍏版捣楂橀€?, '楂橀€熷叕璺?, NULL, '621123', '["62","6211","621123"]', '鐢樿們鍏埅鏃呴珮閫熷叕璺繍钀ョ鐞嗗畾瑗垮垎鍏徃', 66, 1, 1, NOW(), 1, NOW(), 0)
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
    (8101, 8001, 2001, 'accept-http-device-01', '楠屾敹璁惧-HTTP-01', 'temperature', '娓╁害', '80', '鈩?, 1, 1, NOW(), 1, NOW(), 0),
    (8102, 8001, 2001, 'accept-http-device-01', '楠屾敹璁惧-HTTP-01', 'pressure', '鍘嬪姏', '120', 'kPa', 1, 1, NOW(), 1, NOW(), 0),
    (8103, 8002, 2002, 'accept-mqtt-device-01', '楠屾敹璁惧-MQTT-01', 'vibration', '鎸姩', '10', 'mm/s', 1, 1, NOW(), 1, NOW(), 0)
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
    (8201, '閿呯倝娓╁害瓒呴檺', 'temperature', '娓╁害', 'value > 80', 60, 'red', 'email,webhook', 1, 0, 1, 1, NOW(), 1, NOW(), 0),
    (8202, '璁惧鎸姩瓒呴檺', 'vibration', '鎸姩', 'value > 10', 120, 'orange', 'webhook', 1, 0, 1, 1, NOW(), 1, NOW(), 0)
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
    (8301, '娓╁害瓒呴檺鑱斿姩瑙勫垯', '娓╁害瓒呴檺瑙﹀彂閫氱煡骞跺垱寤哄伐鍗?,
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
    (8401, '閿呯倝瓒呮俯搴旀€ラ妗?, 'red', 'red', '閿呯倝鍖哄煙鍑虹幇瓒呮俯鍛婅鏃舵墽琛?,
     JSON_ARRAY('纭鐜板満鐘舵€?, '杩滅▼闄嶈浇', '娲惧彂鐜板満宸ュ崟', '澶嶇洏鍏抽棴浜嬩欢'),
     JSON_ARRAY(JSON_OBJECT('name', '鍊肩彮闀?, 'phone', '13800000000'), JSON_OBJECT('name', '瀹夊叏鍛?, 'phone', '13800000001')),
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
    (8501, 'ALARM-20260317001', '閿呯倝娓╁害瓒呴檺', 'threshold', 'red',
     7002, '榛勬郸鍘傚尯', 8001, '閿呯倝娓╁帇鐩戞祴鐐?,
     2001, 'accept-http-device-01', '楠屾敹璁惧-HTTP-01', 'temperature', '92.4', '80',
     0, DATE_SUB(NOW(), INTERVAL 30 MINUTE), NULL, NULL, NULL, NULL, NULL, NULL,
     8201, '閿呯倝娓╁害瓒呴檺', 1, '寰呯‘璁ゅ憡璀?, 1, NOW(), 1, NOW(), 0),
    (8502, 'ALARM-20260317002', '璁惧鎸姩寮傚父', 'threshold', 'orange',
     7002, '榛勬郸鍘傚尯', 8002, '鎸姩鐩戞祴鐐?,
     2002, 'accept-mqtt-device-01', '楠屾敹璁惧-MQTT-01', 'vibration', '12.1', '10',
     3, DATE_SUB(NOW(), INTERVAL 50 MINUTE), DATE_SUB(NOW(), INTERVAL 45 MINUTE), 1, NULL, NULL, DATE_SUB(NOW(), INTERVAL 20 MINUTE), 1,
     8202, '璁惧鎸姩瓒呴檺', 1, '宸查棴鐜憡璀?, 1, NOW(), 1, NOW(), 0)
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
    (8601, 'EVENT-20260317001', '閿呯倝瓒呮俯澶勭疆浜嬩欢', 8501, 'ALARM-20260317001', 'red', 'red',
     7002, '榛勬郸鍘傚尯', 8001, '閿呯倝娓╁帇鐩戞祴鐐?,
     2001, 'accept-http-device-01', '楠屾敹璁惧-HTTP-01', 'temperature', '92.4',
     2, 1, 'high', 15, 120,
     DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 25 MINUTE), 1, DATE_SUB(NOW(), INTERVAL 20 MINUTE), NULL, NULL, NULL, NULL, '澶勭悊涓?,
     1, '杩涜涓簨浠?, 1, NOW(), 1, NOW(), 0),
    (8602, 'EVENT-20260317002', '鎸姩寮傚父澶嶇洏浜嬩欢', 8502, 'ALARM-20260317002', 'orange', 'orange',
     7002, '榛勬郸鍘傚尯', 8002, '鎸姩鐩戞祴鐐?,
     2002, 'accept-mqtt-device-01', '楠屾敹璁惧-MQTT-01', 'vibration', '12.1',
     4, 1, 'medium', 30, 240,
     DATE_SUB(NOW(), INTERVAL 50 MINUTE), DATE_SUB(NOW(), INTERVAL 45 MINUTE), 1, DATE_SUB(NOW(), INTERVAL 40 MINUTE), DATE_SUB(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE), 1, '澶勭疆瀹屾垚鍏抽棴', '宸插畬鎴愬鐩?,
     1, '宸查棴鐜簨浠?, 1, NOW(), 1, NOW(), 0)
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
     2, '宸插埌鍦烘帓鏌ワ紝鍑嗗闄嶈浇', JSON_ARRAY('https://example.com/img/wo-20260317001-1.jpg'), 1, '澶勭悊涓伐鍗?, 1, NOW(), 1, NOW(), 0),
    (8702, 8602, 'EVENT-20260317002', 'WO-20260317002', 'inspection',
     1, 1, DATE_SUB(NOW(), INTERVAL 44 MINUTE), DATE_SUB(NOW(), INTERVAL 40 MINUTE), DATE_SUB(NOW(), INTERVAL 26 MINUTE),
     3, '澶勭疆瀹屾垚锛岃澶囨仮澶嶇ǔ瀹?, JSON_ARRAY('https://example.com/img/wo-20260317002-1.jpg'), 1, '宸插畬鎴愬伐鍗?, 1, NOW(), 1, NOW(), 0)
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
      (202603192100560271, 1, 'zhd-warning-sound-light-alarm-v1', '涓捣杈?棰勮鍨?澹板厜鎶ヨ鍣?, 'mqtt-json', 1, 'JSON', '涓捣杈?, '棰勮鍨嬪０鍏夋姤璀﹁澶囷紝鍗忚 mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: hitarget_sound_light_alarm', NULL, NULL, 0),
      (202603192100560270, 1, 'zjhy-warning-sound-light-alarm-v1', '娴欐睙鍗庢簮 棰勮鍨?澹板厜鎶ヨ鍣?, 'mqtt-json', 1, 'JSON', '娴欐睙鍗庢簮', '棰勮鍨嬪０鍏夋姤璀﹁澶囷紝鍗忚 mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: zjhy_sound_light_alarm', NULL, NULL, 0),
      (202603192100560259, 1, 'nf-collect-rtu-v1', '鍗楁柟娴嬬粯 閲囬泦鍨?閬ユ祴缁堢', 'mqtt-json', 1, 'JSON', '鍗楁柟娴嬬粯', '閲囬泦鍨嬮仴娴嬬粓绔澶囷紝鍗忚 mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: south_rtu', NULL, NULL, 0),
      (202603192100560258, 1, 'nf-monitor-laser-rangefinder-v1', '鍗楁柟娴嬬粯 鐩戞祴鍨?婵€鍏夋祴璺濅华', 'mqtt-json', 1, 'JSON', '鍗楁柟娴嬬粯', '鐩戞祴鍨嬫縺鍏夋祴璺濊澶囷紝鍗忚 mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: south_laser_rangefinder', NULL, NULL, 0),
      (202603192100560257, 1, 'zhd-monitor-tiltmeter-v1', '涓捣杈?鐩戞祴鍨?鍊捐浠?, 'mqtt-json', 1, 'JSON', '涓捣杈?, '鐩戞祴鍨嬪€捐璁惧锛屽崗璁?mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: hitarget_tiltmeter', NULL, NULL, 0),
      (202603192100560255, 1, 'nf-monitor-crack-meter-v1', '鍗楁柟娴嬬粯 鐩戞祴鍨?瑁傜紳璁?, 'mqtt-json', 1, 'JSON', '鍗楁柟娴嬬粯', '鐩戞祴鍨嬭缂濈洃娴嬭澶囷紝鍗忚 mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: south_crack_meter', NULL, NULL, 0),
      (202603192100560254, 1, 'nf-monitor-mud-level-meter-v1', '鍗楁柟娴嬬粯 鐩戞祴鍨?娉ヤ綅璁?, 'mqtt-json', 1, 'JSON', '鍗楁柟娴嬬粯', '鐩戞祴鍨嬫偿浣嶇洃娴嬭澶囷紝鍗忚 mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: south_mud_level_meter', NULL, NULL, 0),
      (202603192100560253, 1, 'nf-monitor-tipping-bucket-rain-gauge-v1', '鍗楁柟娴嬬粯 鐩戞祴鍨?缈绘枟寮忛洦閲忚', 'mqtt-json', 1, 'JSON', '鍗楁柟娴嬬粯', '鐩戞祴鍨嬬炕鏂楀紡闆ㄩ噺鐩戞祴璁惧锛屽崗璁?mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: south_rain_gauge', NULL, NULL, 0),
      (202603192100560252, 1, 'zhd-monitor-multi-displacement-v1', '涓捣杈?鐩戞祴鍨?澶氱淮浣嶇Щ鐩戞祴浠?, 'mqtt-json', 1, 'JSON', '涓捣杈?, '鐩戞祴鍨嬪缁翠綅绉荤洃娴嬭澶囷紝鍗忚 mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: hitarget_multi_displacement', NULL, NULL, 0),
      (202603192100560251, 1, 'nf-monitor-multi-displacement-v1', '鍗楁柟娴嬬粯 鐩戞祴鍨?澶氱淮浣嶇Щ鐩戞祴浠?, 'mqtt-json', 1, 'JSON', '鍗楁柟娴嬬粯', '鐩戞祴鍨嬪缁翠綅绉荤洃娴嬭澶囷紝鍗忚 mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: south_multi_displacement', NULL, NULL, 0),
      (202603192100560250, 1, 'nf-monitor-deep-displacement-v1', '鍗楁柟娴嬬粯 鐩戞祴鍨?娣遍儴浣嶇Щ鐩戞祴浠?, 'mqtt-json', 1, 'JSON', '鍗楁柟娴嬬粯', '鐩戞祴鍨嬫繁閮ㄤ綅绉荤洃娴嬭澶囷紝鍗忚 mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: south_deep_displacement', NULL, NULL, 0),
      (202603192100560249, 1, 'zhd-monitor-gnss-base-station-v1', '涓捣杈?鐩戞祴鍨?GNSS鍩哄噯绔?, 'mqtt-json', 1, 'JSON', '涓捣杈?, '鐩戞祴鍨?GNSS 鍩哄噯绔欒澶囷紝鍗忚 mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: hitarget_gnss_base_station', NULL, NULL, 0),
      (202603192100560248, 1, 'nf-monitor-gnss-base-station-v1', '鍗楁柟娴嬬粯 鐩戞祴鍨?GNSS鍩哄噯绔?, 'mqtt-json', 1, 'JSON', '鍗楁柟娴嬬粯', '鐩戞祴鍨?GNSS 鍩哄噯绔欒澶囷紝鍗忚 mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: south_gnss_base_station', NULL, NULL, 0),
      (202603192100560247, 1, 'zhd-monitor-gnss-monitor-v1', '涓捣杈?鐩戞祴鍨?GNSS浣嶇Щ鐩戞祴浠?, 'mqtt-json', 1, 'JSON', '涓捣杈?, '鐩戞祴鍨?GNSS 浣嶇Щ鐩戞祴璁惧锛屽崗璁?mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: hitarget_gnss_monitor', NULL, NULL, 0),
      (202603192100560246, 1, 'nf-monitor-gnss-monitor-v1', '鍗楁柟娴嬬粯 鐩戞祴鍨?GNSS浣嶇Щ鐩戞祴浠?, 'mqtt-json', 1, 'JSON', '鍗楁柟娴嬬粯', '鐩戞祴鍨?GNSS 浣嶇Щ鐩戞祴璁惧锛屽崗璁?mqtt-json锛岀洿杩炴帴鍏?, 1, '鍘熷 productKey: south_gnss_monitor', NULL, NULL, 0);

-- 鏁版嵁灏辩华璇存槑
-- 1. IoT 涓婚摼璺細浜у搧/璁惧/鐗╂ā鍨?灞炴€?娑堟伅鏃ュ織
-- 2. 椋庨櫓骞冲彴锛氶闄╃偣銆佺粦瀹氥€佽鍒欍€佽仈鍔ㄣ€侀妗堛€佸憡璀︺€佷簨浠躲€佸伐鍗?
-- 3. 绯荤粺绠＄悊锛氱粍缁囥€佸尯鍩熴€佸瓧鍏搞€侀€氱煡娓犻亾銆佸璁℃棩蹇?

