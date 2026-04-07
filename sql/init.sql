CREATE DATABASE IF NOT EXISTS rm_iot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE rm_iot;

SET NAMES utf8mb4;

-- =========================
-- 0) Drop old tables
-- =========================
DROP VIEW IF EXISTS iot_message_log;

DROP TABLE IF EXISTS iot_event_work_order;
DROP TABLE IF EXISTS iot_event_record;
DROP TABLE IF EXISTS iot_alarm_record;
DROP TABLE IF EXISTS emergency_plan;
DROP TABLE IF EXISTS linkage_rule;
DROP TABLE IF EXISTS rule_definition;
DROP TABLE IF EXISTS risk_metric_catalog;
DROP TABLE IF EXISTS risk_point_highway_detail;
DROP TABLE IF EXISTS risk_point_device_pending_promotion;
DROP TABLE IF EXISTS risk_point_device_pending_binding;
DROP TABLE IF EXISTS risk_point_device;
DROP TABLE IF EXISTS risk_point;

DROP TABLE IF EXISTS iot_command_record;
DROP TABLE IF EXISTS iot_device_metric_latest;
DROP TABLE IF EXISTS iot_device_invalid_report_state;
DROP TABLE IF EXISTS iot_device_access_error_log;
DROP TABLE IF EXISTS iot_device_message_log;
DROP TABLE IF EXISTS iot_device_property;
DROP TABLE IF EXISTS iot_device_relation;
DROP TABLE IF EXISTS iot_device_online_session;
DROP TABLE IF EXISTS iot_device;
DROP TABLE IF EXISTS iot_product_contract_release_batch;
DROP TABLE IF EXISTS iot_vendor_metric_evidence;
DROP TABLE IF EXISTS iot_normative_metric_definition;
DROP TABLE IF EXISTS iot_product_model;
DROP TABLE IF EXISTS iot_product;

DROP TABLE IF EXISTS sys_governance_approval_transition;
DROP TABLE IF EXISTS sys_governance_approval_order;
DROP TABLE IF EXISTS sys_audit_log;
DROP TABLE IF EXISTS sys_help_document;
DROP TABLE IF EXISTS sys_in_app_message_bridge_attempt_log;
DROP TABLE IF EXISTS sys_in_app_message_bridge_log;
DROP TABLE IF EXISTS sys_in_app_message_read;
DROP TABLE IF EXISTS sys_in_app_message;
DROP TABLE IF EXISTS sys_notification_channel;
DROP TABLE IF EXISTS sys_dict_item;
DROP TABLE IF EXISTS sys_dict;
DROP TABLE IF EXISTS sys_region;
DROP TABLE IF EXISTS sys_organization;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_tenant;

-- =========================
-- 1) System domain
-- =========================
CREATE TABLE sys_tenant (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_name VARCHAR(128) NOT NULL COMMENT '绉熸埛鍚嶇О',
    tenant_code VARCHAR(64) NOT NULL COMMENT '绉熸埛缂栫爜',
    contact_name VARCHAR(64) DEFAULT NULL COMMENT '鑱旂郴浜?,
    contact_phone VARCHAR(32) DEFAULT NULL COMMENT '鑱旂郴鐢佃瘽',
    contact_email VARCHAR(128) DEFAULT NULL COMMENT '鑱旂郴閭',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '鐘舵€?,
    expire_time DATETIME DEFAULT NULL COMMENT '鍒版湡鏃堕棿',
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (tenant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绉熸埛琛?;

CREATE TABLE sys_user (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    org_id BIGINT DEFAULT NULL COMMENT '涓绘満鏋処D',
    username VARCHAR(64) NOT NULL COMMENT '鐢ㄦ埛鍚?,
    password VARCHAR(255) NOT NULL COMMENT '瀵嗙爜',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '鏄电О',
    real_name VARCHAR(64) DEFAULT NULL COMMENT '鐪熷疄濮撳悕',
    phone VARCHAR(32) DEFAULT NULL COMMENT '鎵嬫満鍙?,
    email VARCHAR(128) DEFAULT NULL COMMENT '閭',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '澶村儚',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '鐘舵€?1鍚敤 0绂佺敤',
    is_admin TINYINT NOT NULL DEFAULT 0 COMMENT '鏄惁绠＄悊鍛?1鏄?0鍚?,
    last_login_ip VARCHAR(64) DEFAULT NULL COMMENT '鏈€鍚庣櫥褰旾P',
    last_login_time DATETIME DEFAULT NULL COMMENT '鏈€鍚庣櫥褰曟椂闂?,
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username_tenant (tenant_id, username),
    KEY idx_user_org_id (org_id),
    KEY idx_phone (phone),
    KEY idx_email (email),
    KEY idx_user_deleted_status_create_time (deleted, status, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绯荤粺鐢ㄦ埛琛?;

CREATE TABLE sys_role (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    role_name VARCHAR(100) NOT NULL COMMENT '瑙掕壊鍚嶇О',
    role_code VARCHAR(100) NOT NULL COMMENT '瑙掕壊缂栫爜',
    description VARCHAR(500) DEFAULT NULL COMMENT '瑙掕壊鎻忚堪',
    data_scope_type VARCHAR(32) NOT NULL DEFAULT 'TENANT' COMMENT '鏁版嵁鑼冨洿绫诲瀷',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '鐘舵€?,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code_tenant (tenant_id, role_code),
    KEY idx_role_code (role_code),
    KEY idx_role_deleted_status_create_time (deleted, status, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='瑙掕壊琛?;

CREATE TABLE sys_user_role (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    user_id BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
    role_id BIGINT NOT NULL COMMENT '瑙掕壊ID',
    create_by BIGINT DEFAULT NULL COMMENT '鍒涘缓浜?,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '鏇存柊浜?,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '鍒犻櫎鏍囪',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (tenant_id, user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鐢ㄦ埛瑙掕壊鍏宠仈琛?;

CREATE TABLE sys_menu (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '鐖惰彍鍗旾D',
    menu_name VARCHAR(100) NOT NULL COMMENT '鑿滃崟鍚嶇О',
    menu_code VARCHAR(100) DEFAULT NULL COMMENT '鑿滃崟缂栫爜',
    path VARCHAR(255) DEFAULT NULL COMMENT '璺敱璺緞',
    component VARCHAR(255) DEFAULT NULL COMMENT '缁勪欢璺緞',
    icon VARCHAR(100) DEFAULT NULL COMMENT '鍥炬爣',
    meta_json LONGTEXT DEFAULT NULL COMMENT 'UI 鍏冩暟鎹?,
    sort INT NOT NULL DEFAULT 0 COMMENT '鎺掑簭',
    type TINYINT NOT NULL DEFAULT 1 COMMENT '绫诲瀷 0鐩綍 1鑿滃崟 2鎸夐挳',
    menu_type TINYINT NOT NULL DEFAULT 1 COMMENT '鍏煎鍘嗗彶鑿滃崟绫诲瀷',
    route_path VARCHAR(255) DEFAULT NULL COMMENT '鍘嗗彶璺敱瀛楁',
    permission VARCHAR(128) DEFAULT NULL COMMENT '鍘嗗彶鏉冮檺鏍囪瘑瀛楁',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '鍘嗗彶鎺掑簭瀛楁',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '鏄惁鏄剧ず',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '鐘舵€?,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_menu_code_tenant (tenant_id, menu_code),
    KEY idx_parent_id (parent_id),
    KEY idx_status (status),
    KEY idx_menu_deleted_parent_sort (deleted, parent_id, sort, id),
    KEY idx_menu_deleted_status_sort (deleted, status, sort, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鑿滃崟琛?;

CREATE TABLE sys_role_menu (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    role_id BIGINT NOT NULL COMMENT '瑙掕壊ID',
    menu_id BIGINT NOT NULL COMMENT '鑿滃崟ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (tenant_id, role_id, menu_id),
    KEY idx_role_id (role_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='瑙掕壊鑿滃崟鍏宠仈琛?;

CREATE TABLE sys_organization (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '鐖禝D',
    org_name VARCHAR(128) NOT NULL COMMENT '缁勭粐鍚嶇О',
    org_code VARCHAR(64) NOT NULL COMMENT '缁勭粐缂栫爜',
    org_type VARCHAR(32) DEFAULT NULL COMMENT '缁勭粐绫诲瀷 dept/position/team',
    leader_user_id BIGINT DEFAULT NULL COMMENT '璐熻矗浜篒D',
    leader_name VARCHAR(64) DEFAULT NULL COMMENT '璐熻矗浜哄鍚?,
    phone VARCHAR(32) DEFAULT NULL COMMENT '鑱旂郴鐢佃瘽',
    email VARCHAR(128) DEFAULT NULL COMMENT '鑱旂郴閭',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '鐘舵€?1鍚敤 0绂佺敤',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '鎺掑簭',
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code_tenant (tenant_id, org_code),
    KEY idx_parent_id (parent_id),
    KEY idx_org_deleted_parent_sort (deleted, parent_id, sort_no, id),
    KEY idx_org_deleted_status_sort (deleted, status, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缁勭粐鏈烘瀯琛?;

CREATE TABLE sys_region (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    region_name VARCHAR(128) NOT NULL COMMENT '鍖哄煙鍚嶇О',
    region_code VARCHAR(64) NOT NULL COMMENT '鍖哄煙缂栫爜',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '鐖禝D',
    region_type VARCHAR(32) NOT NULL COMMENT '鍖哄煙绫诲瀷 province/city/district/street',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '缁忓害',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '绾害',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '鐘舵€?1鍚敤 0绂佺敤',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '鎺掑簭',
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_region_code_tenant (tenant_id, region_code),
    KEY idx_parent_id (parent_id),
    KEY idx_region_deleted_parent_sort (deleted, parent_id, sort_no, id),
    KEY idx_region_deleted_type_sort (deleted, region_type, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鍖哄煙琛?;

CREATE TABLE sys_dict (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    dict_name VARCHAR(128) NOT NULL COMMENT '瀛楀吀鍚嶇О',
    dict_code VARCHAR(64) NOT NULL COMMENT '瀛楀吀缂栫爜',
    dict_type VARCHAR(32) DEFAULT NULL COMMENT '瀛楀吀绫诲瀷',
    dict_value VARCHAR(255) NOT NULL DEFAULT '' COMMENT '鍘嗗彶鍏煎瀛楁',
    dict_label VARCHAR(128) NOT NULL DEFAULT '' COMMENT '鍘嗗彶鍏煎瀛楁',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '鐘舵€?1鍚敤 0绂佺敤',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '鎺掑簭',
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_code_tenant (tenant_id, dict_code),
    KEY idx_dict_deleted_sort (deleted, sort_no, id),
    KEY idx_dict_deleted_type_sort (deleted, dict_type, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='瀛楀吀琛?;

CREATE TABLE sys_dict_item (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    dict_id BIGINT NOT NULL COMMENT '瀛楀吀ID',
    item_name VARCHAR(128) NOT NULL COMMENT '椤瑰悕绉?,
    item_value VARCHAR(255) NOT NULL COMMENT '椤瑰€?,
    item_type VARCHAR(32) DEFAULT NULL COMMENT '椤圭被鍨?string/number/boolean',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '鐘舵€?1鍚敤 0绂佺敤',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '鎺掑簭',
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_value_tenant (tenant_id, dict_id, item_value),
    KEY idx_dict_id (dict_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='瀛楀吀椤硅〃';

CREATE TABLE sys_notification_channel (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    channel_name VARCHAR(128) NOT NULL COMMENT '娓犻亾鍚嶇О',
    channel_code VARCHAR(64) NOT NULL COMMENT '娓犻亾缂栫爜',
    channel_type VARCHAR(32) DEFAULT NULL COMMENT '娓犻亾绫诲瀷',
    config LONGTEXT DEFAULT NULL COMMENT '娓犻亾閰嶇疆(JSON)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '鐘舵€?1鍚敤 0绂佺敤',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '鎺掑簭',
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_channel_code_tenant (tenant_id, channel_code),
    KEY idx_channel_deleted_sort (deleted, sort_no, id),
    KEY idx_channel_deleted_type_sort (deleted, channel_type, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='閫氱煡娓犻亾琛?;

CREATE TABLE sys_in_app_message (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    message_type VARCHAR(32) NOT NULL COMMENT '娑堟伅绫诲瀷 system/business/error',
    priority VARCHAR(32) NOT NULL DEFAULT 'medium' COMMENT '浼樺厛绾?critical/high/medium/low',
    title VARCHAR(128) NOT NULL COMMENT '娑堟伅鏍囬',
    summary VARCHAR(500) DEFAULT NULL COMMENT '娑堟伅鎽樿',
    content LONGTEXT DEFAULT NULL COMMENT '娑堟伅姝ｆ枃',
    target_type VARCHAR(16) NOT NULL DEFAULT 'all' COMMENT '鎺ㄩ€佽寖鍥?all/role/user',
    target_role_codes VARCHAR(500) DEFAULT NULL COMMENT '鐩爣瑙掕壊缂栫爜锛岄€楀彿鍒嗛殧',
    target_user_ids VARCHAR(500) DEFAULT NULL COMMENT '鐩爣鐢ㄦ埛ID锛岄€楀彿鍒嗛殧',
    related_path VARCHAR(255) DEFAULT NULL COMMENT '鍏宠仈椤甸潰璺緞',
    source_type VARCHAR(64) DEFAULT NULL COMMENT '鏉ユ簮绫诲瀷',
    source_id VARCHAR(64) DEFAULT NULL COMMENT '鏉ユ簮涓氬姟ID',
    dedup_key VARCHAR(32) DEFAULT NULL COMMENT '鍘婚噸閿?,
    publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍙戝竷鏃堕棿',
    expire_time DATETIME DEFAULT NULL COMMENT '杩囨湡鏃堕棿',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '鐘舵€?1鍙戝竷涓?0鍋滅敤',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '鎺掑簭',
    create_by BIGINT DEFAULT NULL COMMENT '鍒涘缓浜?,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '鏇存柊浜?,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '鍒犻櫎鏍囪',
    PRIMARY KEY (id),
    KEY idx_in_app_message_deleted_status_time (deleted, status, publish_time, id),
    KEY idx_in_app_message_deleted_type_time (deleted, message_type, publish_time, id),
    KEY idx_in_app_message_deleted_target_sort (deleted, target_type, sort_no, id),
    KEY idx_in_app_message_source (source_type, source_id),
    KEY idx_in_app_message_tenant_dedup (tenant_id, dedup_key, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绔欏唴娑堟伅琛?;

CREATE TABLE sys_in_app_message_read (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    message_id BIGINT NOT NULL COMMENT '娑堟伅ID',
    user_id BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
    read_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '宸茶鏃堕棿',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_in_app_message_read_user (tenant_id, message_id, user_id),
    KEY idx_in_app_message_read_user_time (user_id, read_time, message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绔欏唴娑堟伅宸茶琛?;

CREATE TABLE sys_in_app_message_bridge_log (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    message_id BIGINT NOT NULL COMMENT '娑堟伅ID',
    channel_code VARCHAR(64) NOT NULL COMMENT '娓犻亾缂栫爜',
    bridge_scene VARCHAR(64) NOT NULL COMMENT '妗ユ帴鍦烘櫙',
    unread_count INT NOT NULL DEFAULT 0 COMMENT '鏈€杩戜竴娆℃ˉ鎺ユ椂鐨勬湭璇讳汉鏁?,
    recipient_snapshot VARCHAR(500) DEFAULT NULL COMMENT '鏈瀵硅薄鎽樿',
    bridge_status TINYINT NOT NULL DEFAULT 0 COMMENT '妗ユ帴鐘舵€?0澶辫触/寰呴噸璇?1鎴愬姛',
    response_status_code INT DEFAULT NULL COMMENT '鏈€杩戜竴娆″搷搴旂姸鎬佺爜',
    response_body VARCHAR(1000) DEFAULT NULL COMMENT '鏈€杩戜竴娆″搷搴旀憳瑕?,
    last_attempt_time DATETIME DEFAULT NULL COMMENT '鏈€杩戜竴娆″皾璇曟椂闂?,
    success_time DATETIME DEFAULT NULL COMMENT '鎴愬姛鏃堕棿',
    attempt_count INT NOT NULL DEFAULT 0 COMMENT '灏濊瘯娆℃暟',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_in_app_message_bridge_message_channel (tenant_id, message_id, channel_code, bridge_scene),
    KEY idx_in_app_message_bridge_status_time (bridge_status, last_attempt_time),
    KEY idx_in_app_message_bridge_message (message_id, channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绔欏唴娑堟伅鏈妗ユ帴鏃ュ織琛?;

CREATE TABLE sys_in_app_message_bridge_attempt_log (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    bridge_log_id BIGINT NOT NULL COMMENT '妗ユ帴鏃ュ織ID',
    message_id BIGINT NOT NULL COMMENT '娑堟伅ID',
    channel_code VARCHAR(64) NOT NULL COMMENT '娓犻亾缂栫爜',
    bridge_scene VARCHAR(64) NOT NULL COMMENT '妗ユ帴鍦烘櫙',
    attempt_no INT NOT NULL COMMENT '灏濊瘯搴忓彿',
    bridge_status TINYINT NOT NULL DEFAULT 0 COMMENT '妗ユ帴鐘舵€?0澶辫触 1鎴愬姛',
    unread_count INT NOT NULL DEFAULT 0 COMMENT '鏈妗ユ帴鏃剁殑鏈浜烘暟',
    recipient_snapshot VARCHAR(500) DEFAULT NULL COMMENT '鏈妗ユ帴鏃剁殑鏈瀵硅薄鎽樿',
    response_status_code INT DEFAULT NULL COMMENT '鏈鍝嶅簲鐘舵€佺爜',
    response_body VARCHAR(1000) DEFAULT NULL COMMENT '鏈鍝嶅簲鎽樿',
    attempt_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '灏濊瘯鏃堕棿',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_in_app_message_bridge_attempt (bridge_log_id, attempt_no),
    KEY idx_in_app_message_bridge_attempt_log_time (bridge_log_id, attempt_time DESC),
    KEY idx_in_app_message_bridge_attempt_message (message_id, channel_code, attempt_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绔欏唴娑堟伅妗ユ帴灏濊瘯鏄庣粏琛?;

CREATE TABLE sys_help_document (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    doc_category VARCHAR(32) NOT NULL COMMENT '鏂囨。鍒嗙被 business/technical/faq',
    title VARCHAR(128) NOT NULL COMMENT '鏂囨。鏍囬',
    summary VARCHAR(500) DEFAULT NULL COMMENT '鏂囨。鎽樿',
    content LONGTEXT NOT NULL COMMENT '鏂囨。姝ｆ枃',
    keywords VARCHAR(500) DEFAULT NULL COMMENT '鍏抽敭璇嶏紝閫楀彿鍒嗛殧',
    related_paths VARCHAR(500) DEFAULT NULL COMMENT '鍏宠仈椤甸潰璺緞锛岄€楀彿鍒嗛殧',
    visible_role_codes VARCHAR(500) DEFAULT NULL COMMENT '鍙瑙掕壊缂栫爜锛岄€楀彿鍒嗛殧',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '鐘舵€?1鍚敤 0鍋滅敤',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '鎺掑簭',
    create_by BIGINT DEFAULT NULL COMMENT '鍒涘缓浜?,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '鏇存柊浜?,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '鍒犻櫎鏍囪',
    PRIMARY KEY (id),
    KEY idx_help_document_deleted_category_sort (deleted, doc_category, sort_no, id),
    KEY idx_help_document_deleted_status_sort (deleted, status, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='甯姪鏂囨。琛?;

CREATE TABLE sys_audit_log (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    user_id BIGINT DEFAULT NULL COMMENT '鐢ㄦ埛ID',
    user_name VARCHAR(64) DEFAULT NULL COMMENT '鐢ㄦ埛鍚?,
    operation_type VARCHAR(64) DEFAULT NULL COMMENT '鎿嶄綔绫诲瀷',
    operation_module VARCHAR(128) DEFAULT NULL COMMENT '鎿嶄綔妯″潡',
    operation_method VARCHAR(255) NOT NULL DEFAULT '' COMMENT '鎿嶄綔鏂规硶',
    request_url VARCHAR(255) DEFAULT NULL COMMENT '璇锋眰URL',
    request_method VARCHAR(16) DEFAULT NULL COMMENT '璇锋眰鏂规硶',
    request_params LONGTEXT DEFAULT NULL COMMENT '璇锋眰鍙傛暟',
    response_result LONGTEXT DEFAULT NULL COMMENT '鍝嶅簲缁撴灉',
    ip_address VARCHAR(64) DEFAULT NULL COMMENT 'IP鍦板潃',
    location VARCHAR(128) DEFAULT NULL COMMENT '浣嶇疆',
    operation_result TINYINT DEFAULT NULL COMMENT '鎿嶄綔缁撴灉 1鎴愬姛 0澶辫触',
    result_message VARCHAR(500) DEFAULT NULL COMMENT '缁撴灉娑堟伅',
    operation_time DATETIME DEFAULT NULL COMMENT '鎿嶄綔鏃堕棿',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'trace id',
    device_code VARCHAR(64) DEFAULT NULL COMMENT 'device code',
    product_key VARCHAR(64) DEFAULT NULL COMMENT 'product key',
    error_code VARCHAR(64) DEFAULT NULL COMMENT 'error code',
    exception_class VARCHAR(255) DEFAULT NULL COMMENT 'exception class',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_operation_time (operation_time),
    KEY idx_trace_id (trace_id),
    KEY idx_device_code (device_code),
    KEY idx_audit_deleted_operation_time (deleted, operation_time, create_time, id),
    KEY idx_audit_deleted_type_time (deleted, operation_type, operation_time, create_time, id),
    KEY idx_audit_deleted_request_method_time (deleted, request_method, operation_time, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='瀹¤鏃ュ織琛?;

CREATE TABLE sys_governance_approval_order (
    id BIGINT NOT NULL COMMENT 'Approval order ID',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
    action_code VARCHAR(64) NOT NULL COMMENT 'Approval action code',
    action_name VARCHAR(128) DEFAULT NULL COMMENT 'Approval action name',
    subject_type VARCHAR(64) DEFAULT NULL COMMENT 'Approval subject type',
    subject_id BIGINT DEFAULT NULL COMMENT 'Approval subject id',
    status VARCHAR(32) NOT NULL COMMENT 'Approval status',
    operator_user_id BIGINT NOT NULL COMMENT 'Operator user id',
    approver_user_id BIGINT NOT NULL COMMENT 'Approver user id',
    payload_json LONGTEXT DEFAULT NULL COMMENT 'Approval payload',
    approval_comment VARCHAR(500) DEFAULT NULL COMMENT 'Approval comment',
    approved_time DATETIME DEFAULT NULL COMMENT 'Approval time',
    create_by BIGINT DEFAULT NULL COMMENT 'Creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    update_by BIGINT DEFAULT NULL COMMENT 'Updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Deleted flag',
    PRIMARY KEY (id),
    KEY idx_governance_approval_order_subject (subject_type, subject_id, deleted),
    KEY idx_governance_approval_order_status_time (status, create_time, deleted),
    KEY idx_governance_approval_order_operator (operator_user_id, approver_user_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance approval order';

CREATE TABLE sys_governance_approval_transition (
    id BIGINT NOT NULL COMMENT 'Approval transition id',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
    order_id BIGINT NOT NULL COMMENT 'Approval order id',
    from_status VARCHAR(32) DEFAULT NULL COMMENT 'From status',
    to_status VARCHAR(32) NOT NULL COMMENT 'To status',
    actor_user_id BIGINT NOT NULL COMMENT 'Transition actor user id',
    transition_comment VARCHAR(500) DEFAULT NULL COMMENT 'Transition comment',
    create_by BIGINT DEFAULT NULL COMMENT 'Creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Deleted flag',
    PRIMARY KEY (id),
    KEY idx_governance_approval_transition_order (order_id, create_time, deleted),
    KEY idx_governance_approval_transition_actor (actor_user_id, create_time, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Governance approval transition';

-- =========================
-- 2) IoT device domain
-- =========================
CREATE TABLE iot_product (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    product_key VARCHAR(64) NOT NULL COMMENT '浜у搧Key',
    product_name VARCHAR(128) NOT NULL COMMENT '浜у搧鍚嶇О',
    protocol_code VARCHAR(64) NOT NULL COMMENT '鍗忚缂栫爜',
    node_type TINYINT NOT NULL DEFAULT 1 COMMENT '鑺傜偣绫诲瀷 1鐩磋繛璁惧 2缃戝叧璁惧 3缃戝叧瀛愯澶?,
    data_format VARCHAR(32) NOT NULL DEFAULT 'JSON' COMMENT '鏁版嵁鏍煎紡',
    manufacturer VARCHAR(128) DEFAULT NULL COMMENT '鍘傚晢',
    description VARCHAR(500) DEFAULT NULL COMMENT '鎻忚堪',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '鐘舵€?,
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_key_tenant (tenant_id, product_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浜у搧琛?;

CREATE TABLE iot_product_model (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    product_id BIGINT NOT NULL COMMENT '浜у搧ID',
    model_type VARCHAR(32) NOT NULL COMMENT '妯″瀷绫诲瀷 property/event/service',
    identifier VARCHAR(64) NOT NULL COMMENT '鏍囪瘑绗?,
    model_name VARCHAR(128) NOT NULL COMMENT '鍚嶇О',
    data_type VARCHAR(32) NOT NULL COMMENT '鏁版嵁绫诲瀷',
    specs_json JSON DEFAULT NULL COMMENT '瑙勬牸JSON',
    event_type VARCHAR(32) DEFAULT NULL COMMENT '浜嬩欢绫诲瀷',
    service_input_json JSON DEFAULT NULL COMMENT '鏈嶅姟杈撳叆瀹氫箟',
    service_output_json JSON DEFAULT NULL COMMENT '鏈嶅姟杈撳嚭瀹氫箟',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '鎺掑簭',
    required_flag TINYINT NOT NULL DEFAULT 0 COMMENT '鏄惁蹇呭～',
    description VARCHAR(500) DEFAULT NULL COMMENT '鎻忚堪',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_identifier (product_id, model_type, identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浜у搧鐗╂ā鍨嬭〃';

CREATE TABLE iot_normative_metric_definition (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    scenario_code VARCHAR(64) NOT NULL COMMENT '娌荤悊鍦烘櫙缂栫爜',
    device_family VARCHAR(64) NOT NULL COMMENT '璁惧鏃忕紪鐮?,
    identifier VARCHAR(64) NOT NULL COMMENT '瑙勮寖瀛楁鏍囪瘑',
    display_name VARCHAR(128) NOT NULL COMMENT '瑙勮寖瀛楁鍚嶇О',
    unit VARCHAR(32) DEFAULT NULL COMMENT '鍗曚綅',
    precision_digits INT DEFAULT NULL COMMENT '绮惧害',
    monitor_content_code VARCHAR(32) DEFAULT NULL COMMENT '鐩戞祴鍐呭缂栫爜',
    monitor_type_code VARCHAR(32) DEFAULT NULL COMMENT '鐩戞祴绫诲瀷缂栫爜',
    risk_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '鏄惁鍏佽杩涘叆椋庨櫓闂幆',
    trend_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '鏄惁鍏佽瓒嬪娍鍒嗘瀽',
    metadata_json JSON DEFAULT NULL COMMENT '鎵╁睍鍏冩暟鎹?,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_normative_metric_scenario_identifier (scenario_code, identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='瑙勮寖瀛楁瀹氫箟琛?;

CREATE TABLE iot_vendor_metric_evidence (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    product_id BIGINT NOT NULL COMMENT '浜у搧ID',
    parent_device_code VARCHAR(64) DEFAULT NULL COMMENT '鐖惰澶囩紪鐮?,
    child_device_code VARCHAR(64) DEFAULT NULL COMMENT '瀛愯澶囩紪鐮?,
    raw_identifier VARCHAR(128) NOT NULL COMMENT '鍘熷瀛楁鏍囪瘑',
    canonical_identifier VARCHAR(64) DEFAULT NULL COMMENT '寤鸿瑙勮寖瀛楁鏍囪瘑',
    logical_channel_code VARCHAR(64) DEFAULT NULL COMMENT '閫昏緫閫氶亾缂栫爜',
    evidence_origin VARCHAR(32) NOT NULL COMMENT '璇佹嵁鏉ユ簮',
    sample_value VARCHAR(255) DEFAULT NULL COMMENT '鏍蜂緥鍊?,
    value_type VARCHAR(32) DEFAULT NULL COMMENT '鍊肩被鍨?,
    evidence_count INT NOT NULL DEFAULT 0 COMMENT '鍛戒腑娆℃暟',
    last_seen_time DATETIME DEFAULT NULL COMMENT '鏈€鍚庡嚭鐜版椂闂?,
    metadata_json JSON DEFAULT NULL COMMENT '鎵╁睍鍏冩暟鎹?,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vendor_metric_evidence (product_id, raw_identifier, logical_channel_code),
    KEY idx_vendor_metric_product_seen (product_id, last_seen_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鍘傚晢瀛楁璇佹嵁琛?;

CREATE TABLE iot_product_contract_release_batch (
    id BIGINT NOT NULL COMMENT 'Primary key',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
    product_id BIGINT NOT NULL COMMENT 'Product ID',
    scenario_code VARCHAR(64) NOT NULL COMMENT 'Scenario code',
    release_source VARCHAR(64) NOT NULL COMMENT 'Release source',
    released_field_count INT NOT NULL DEFAULT 0 COMMENT 'Released field count',
    create_by BIGINT DEFAULT NULL COMMENT 'Operator user ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rollback_by BIGINT DEFAULT NULL COMMENT 'Rollback operator user ID',
    rollback_time DATETIME DEFAULT NULL COMMENT 'Rollback time',
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_product_contract_release_product_time (product_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product contract release batch';

CREATE TABLE iot_product_contract_release_snapshot (
    id BIGINT NOT NULL COMMENT 'Primary key',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
    batch_id BIGINT NOT NULL COMMENT 'Release batch ID',
    product_id BIGINT NOT NULL COMMENT 'Product ID',
    snapshot_stage VARCHAR(32) NOT NULL COMMENT 'Snapshot stage',
    snapshot_json JSON NOT NULL COMMENT 'Snapshot payload JSON',
    create_by BIGINT DEFAULT NULL COMMENT 'Operator user ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_release_snapshot_batch_stage (batch_id, snapshot_stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product contract release snapshot';

CREATE TABLE iot_device (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    org_id BIGINT DEFAULT NULL COMMENT '鎵€灞炴満鏋処D',
    org_name VARCHAR(128) DEFAULT NULL COMMENT '鎵€灞炴満鏋勫悕绉?,
    product_id BIGINT NOT NULL COMMENT '浜у搧ID',
    gateway_id BIGINT DEFAULT NULL COMMENT '鎵€灞炵綉鍏矷D',
    parent_device_id BIGINT DEFAULT NULL COMMENT '鐖惰澶嘔D',
    device_name VARCHAR(128) NOT NULL COMMENT '璁惧鍚嶇О',
    device_code VARCHAR(64) NOT NULL COMMENT '璁惧缂栫爜',
    device_secret VARCHAR(128) DEFAULT NULL COMMENT '璁惧瀵嗛挜',
    client_id VARCHAR(128) DEFAULT NULL COMMENT '瀹㈡埛绔疘D',
    username VARCHAR(128) DEFAULT NULL COMMENT '鎺ュ叆鐢ㄦ埛鍚?,
    password VARCHAR(128) DEFAULT NULL COMMENT '鎺ュ叆瀵嗙爜',
    protocol_code VARCHAR(64) NOT NULL COMMENT '鍗忚缂栫爜',
    node_type TINYINT NOT NULL DEFAULT 1 COMMENT '鑺傜偣绫诲瀷 1鐩磋繛璁惧 2缃戝叧璁惧 3瀛愯澶?,
    online_status TINYINT NOT NULL DEFAULT 0 COMMENT '鍦ㄧ嚎鐘舵€?1鍦ㄧ嚎 0绂荤嚎',
    activate_status TINYINT NOT NULL DEFAULT 0 COMMENT '婵€娲荤姸鎬?1宸叉縺娲?0鏈縺娲?,
    device_status TINYINT NOT NULL DEFAULT 1 COMMENT '璁惧鐘舵€?1鍚敤 0绂佺敤',
    firmware_version VARCHAR(64) DEFAULT NULL COMMENT '鍥轰欢鐗堟湰',
    ip_address VARCHAR(64) DEFAULT NULL COMMENT '璁惧IP',
    last_online_time DATETIME DEFAULT NULL COMMENT '鏈€鍚庝笂绾挎椂闂?,
    last_offline_time DATETIME DEFAULT NULL COMMENT '鏈€鍚庣绾挎椂闂?,
    last_report_time DATETIME DEFAULT NULL COMMENT '鏈€鍚庝笂鎶ユ椂闂?,
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '缁忓害',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '绾害',
    address VARCHAR(255) DEFAULT NULL COMMENT '瀹夎鍦板潃',
    metadata_json JSON DEFAULT NULL COMMENT '璁惧鎵╁睍淇℃伅',
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_code_tenant (tenant_id, device_code),
    KEY idx_device_tenant_org_deleted (tenant_id, org_id, deleted, last_report_time, id),
    KEY idx_device_deleted_product_stats (deleted, product_id, last_report_time, online_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='璁惧琛?;

CREATE TABLE iot_device_relation (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    parent_device_id BIGINT NOT NULL COMMENT '鐖惰澶嘔D',
    parent_device_code VARCHAR(64) NOT NULL COMMENT '鐖惰澶囩紪鐮?,
    logical_channel_code VARCHAR(64) NOT NULL COMMENT '閫昏緫閫氶亾缂栫爜',
    child_device_id BIGINT NOT NULL COMMENT '瀛愯澶嘔D',
    child_device_code VARCHAR(64) NOT NULL COMMENT '瀛愯澶囩紪鐮?,
    child_product_id BIGINT DEFAULT NULL COMMENT '瀛愪骇鍝両D',
    child_product_key VARCHAR(64) DEFAULT NULL COMMENT '瀛愪骇鍝?productKey',
    relation_type VARCHAR(32) NOT NULL COMMENT '鍏崇郴绫诲瀷 collector_child/gateway_child',
    canonicalization_strategy VARCHAR(32) NOT NULL COMMENT '褰掍竴鍖栫瓥鐣?LEGACY/LF_VALUE',
    status_mirror_strategy VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '鐘舵€侀暅鍍忕瓥鐣?NONE/SENSOR_STATE',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '鏄惁鍚敤',
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_relation_parent_channel (tenant_id, parent_device_id, logical_channel_code, deleted),
    KEY idx_relation_parent_code (tenant_id, parent_device_code, enabled, deleted),
    KEY idx_relation_child_code (tenant_id, child_device_code, enabled, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='璁惧閫昏緫閫氶亾鍏崇郴琛?;

CREATE TABLE iot_device_online_session (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    product_id BIGINT NOT NULL COMMENT '浜у搧ID',
    device_id BIGINT NOT NULL COMMENT '璁惧ID',
    device_code VARCHAR(64) NOT NULL COMMENT '璁惧缂栫爜',
    online_time DATETIME NOT NULL COMMENT '浼氳瘽寮€濮嬫椂闂?,
    last_seen_time DATETIME DEFAULT NULL COMMENT '浼氳瘽鏈€鍚庢椿璺冩椂闂?,
    offline_time DATETIME DEFAULT NULL COMMENT '浼氳瘽缁撴潫鏃堕棿',
    duration_minutes BIGINT DEFAULT NULL COMMENT '鍦ㄧ嚎鏃堕暱锛堝垎閽燂級',
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_online_session_device_active (deleted, device_id, offline_time),
    KEY idx_online_session_product_time (deleted, product_id, online_time, offline_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='璁惧鍦ㄧ嚎浼氳瘽琛?;

CREATE TABLE iot_device_property (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    device_id BIGINT NOT NULL COMMENT '璁惧ID',
    identifier VARCHAR(64) NOT NULL COMMENT '灞炴€ф爣璇?,
    property_name VARCHAR(128) DEFAULT NULL COMMENT '灞炴€у悕绉?,
    property_value VARCHAR(1024) DEFAULT NULL COMMENT '灞炴€у€?,
    value_type VARCHAR(32) DEFAULT NULL COMMENT '鍊肩被鍨?,
    report_time DATETIME NOT NULL COMMENT '涓婃姤鏃堕棿',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_identifier (device_id, identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='璁惧鏈€鏂板睘鎬ц〃';

CREATE TABLE iot_device_metric_latest (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL COMMENT '绉熸埛ID',
    device_id BIGINT NOT NULL COMMENT '璁惧ID',
    product_id BIGINT NOT NULL COMMENT '浜у搧ID',
    metric_id VARCHAR(128) NOT NULL COMMENT '鎸囨爣鍞竴閿?,
    metric_code VARCHAR(128) NOT NULL COMMENT '鎸囨爣缂栫爜',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT '鎸囨爣鍚嶇О',
    value_type VARCHAR(32) DEFAULT NULL COMMENT '鍊肩被鍨?,
    value_double DOUBLE DEFAULT NULL COMMENT '娴偣鍊?,
    value_long BIGINT DEFAULT NULL COMMENT '鏁村瀷鍊?,
    value_bool TINYINT(1) DEFAULT NULL COMMENT '甯冨皵鍊?,
    value_text TEXT DEFAULT NULL COMMENT '鏂囨湰鍊?,
    quality_code VARCHAR(32) DEFAULT NULL COMMENT '璐ㄩ噺鐮?,
    alarm_flag TINYINT(1) DEFAULT NULL COMMENT '鍛婅鏍囪',
    reported_at DATETIME DEFAULT NULL COMMENT '瀹為檯涓婃姤鏃堕棿',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'trace id',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tel_latest_tenant_device_metric (tenant_id, device_id, metric_id),
    KEY idx_tel_latest_device_reported (device_id, reported_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='telemetry v2 latest鎶曞奖琛?;

CREATE TABLE iot_device_message_log (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    device_id BIGINT NOT NULL COMMENT '璁惧ID',
    product_id BIGINT DEFAULT NULL COMMENT '浜у搧ID',
    message_type VARCHAR(32) NOT NULL COMMENT '娑堟伅绫诲瀷 telemetry/event/property/reply',
    topic VARCHAR(255) DEFAULT NULL COMMENT '涓婚',
    payload JSON DEFAULT NULL COMMENT '鍘熷娑堟伅',
    report_time DATETIME NOT NULL COMMENT '涓婃姤鏃堕棿',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'trace id',
    device_code VARCHAR(64) DEFAULT NULL COMMENT 'device code',
    product_key VARCHAR(64) DEFAULT NULL COMMENT 'product key',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_device_time (device_id, report_time),
    KEY idx_message_type (message_type),
    KEY idx_trace_id (trace_id),
    KEY idx_device_code_time (device_code, report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='璁惧娑堟伅鏃ュ織琛?;

CREATE TABLE iot_device_access_error_log (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'trace id',
    protocol_code VARCHAR(64) DEFAULT NULL COMMENT '鍗忚缂栫爜',
    request_method VARCHAR(16) DEFAULT NULL COMMENT '璇锋眰鏂瑰紡',
    failure_stage VARCHAR(32) DEFAULT NULL COMMENT '澶辫触闃舵',
    device_code VARCHAR(64) DEFAULT NULL COMMENT '璁惧缂栫爜',
    product_key VARCHAR(64) DEFAULT NULL COMMENT '浜у搧Key',
    gateway_device_code VARCHAR(64) DEFAULT NULL COMMENT '缃戝叧璁惧缂栫爜',
    sub_device_code VARCHAR(64) DEFAULT NULL COMMENT '瀛愯澶囩紪鐮?,
    topic_route_type VARCHAR(32) DEFAULT NULL COMMENT 'topic 璺敱绫诲瀷',
    message_type VARCHAR(32) DEFAULT NULL COMMENT '娑堟伅绫诲瀷',
    topic VARCHAR(255) DEFAULT NULL COMMENT 'topic',
    client_id VARCHAR(128) DEFAULT NULL COMMENT '瀹㈡埛绔疘D',
    payload_size INT DEFAULT NULL COMMENT 'payload 澶у皬',
    payload_encoding VARCHAR(16) DEFAULT NULL COMMENT 'payload 缂栫爜',
    payload_truncated TINYINT NOT NULL DEFAULT 0 COMMENT 'payload 鏄惁鎴柇',
    raw_payload LONGTEXT DEFAULT NULL COMMENT '鍘熷 payload',
    error_code VARCHAR(64) DEFAULT NULL COMMENT '閿欒鐮?,
    exception_class VARCHAR(255) DEFAULT NULL COMMENT '寮傚父绫诲瀷',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '閿欒娑堟伅',
    contract_snapshot LONGTEXT DEFAULT NULL COMMENT '璁惧濂戠害蹇収',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_access_error_trace (trace_id),
    KEY idx_access_error_device_time (device_code, create_time),
    KEY idx_access_error_stage_time (failure_stage, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='璁惧鎺ュ叆澶辫触褰掓。琛?;

CREATE TABLE iot_device_invalid_report_state (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    governance_key VARCHAR(255) NOT NULL COMMENT '娌荤悊鍞竴閿?,
    reason_code VARCHAR(64) NOT NULL COMMENT '娌荤悊鍘熷洜缂栫爜',
    request_method VARCHAR(16) DEFAULT NULL COMMENT '璇锋眰鏂瑰紡',
    failure_stage VARCHAR(32) DEFAULT NULL COMMENT '澶辫触闃舵',
    device_code VARCHAR(64) DEFAULT NULL COMMENT '璁惧缂栫爜',
    product_key VARCHAR(64) DEFAULT NULL COMMENT '浜у搧Key',
    protocol_code VARCHAR(64) DEFAULT NULL COMMENT '鍗忚缂栫爜',
    topic_route_type VARCHAR(32) DEFAULT NULL COMMENT 'topic 璺敱绫诲瀷',
    topic VARCHAR(255) DEFAULT NULL COMMENT '鏈€杩?topic',
    client_id VARCHAR(128) DEFAULT NULL COMMENT '鏈€杩?clientId',
    payload_size INT DEFAULT NULL COMMENT '鏈€杩?payload 澶у皬',
    payload_encoding VARCHAR(16) DEFAULT NULL COMMENT '鏈€杩?payload 缂栫爜',
    last_payload LONGTEXT DEFAULT NULL COMMENT '鏈€杩?payload',
    last_trace_id VARCHAR(64) DEFAULT NULL COMMENT '鏈€杩?traceId',
    sample_error_message VARCHAR(500) DEFAULT NULL COMMENT '鏍锋湰閿欒娑堟伅',
    sample_exception_class VARCHAR(255) DEFAULT NULL COMMENT '鏍锋湰寮傚父绫?,
    first_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '棣栨鍛戒腑鏃堕棿',
    last_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鏈€杩戝懡涓椂闂?,
    hit_count BIGINT NOT NULL DEFAULT 0 COMMENT '鎬诲懡涓鏁?,
    sampled_count BIGINT NOT NULL DEFAULT 0 COMMENT '宸查噰鏍锋鏁?,
    suppressed_count BIGINT NOT NULL DEFAULT 0 COMMENT '琚姂鍒舵鏁?,
    suppressed_until DATETIME DEFAULT NULL COMMENT '鎶戝埗鎴鏃堕棿',
    resolved TINYINT NOT NULL DEFAULT 0 COMMENT '鏄惁宸茶В灏?,
    resolved_time DATETIME DEFAULT NULL COMMENT '瑙ｅ皝鏃堕棿',
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invalid_report_state_governance_key (governance_key),
    KEY idx_invalid_report_device_resolved (device_code, product_key, resolved, last_seen_time),
    KEY idx_invalid_report_reason_time (reason_code, last_seen_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鏃犳晥 MQTT 涓婃姤鏈€鏂版€佽〃';

CREATE TABLE iot_command_record (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    command_id VARCHAR(64) NOT NULL COMMENT '涓氬姟鍛戒护ID',
    device_id BIGINT NOT NULL COMMENT '璁惧ID',
    device_code VARCHAR(64) NOT NULL COMMENT '璁惧缂栫爜',
    product_key VARCHAR(64) NOT NULL COMMENT '浜у搧Key',
    gateway_device_code VARCHAR(64) DEFAULT NULL COMMENT '缃戝叧璁惧缂栫爜',
    sub_device_code VARCHAR(64) DEFAULT NULL COMMENT '瀛愯澶囩紪鐮?,
    topic VARCHAR(255) NOT NULL COMMENT '涓嬪彂Topic',
    command_type VARCHAR(32) NOT NULL COMMENT '鍛戒护绫诲瀷 property/service',
    service_identifier VARCHAR(64) DEFAULT NULL COMMENT '鏈嶅姟鏍囪瘑',
    request_payload LONGTEXT DEFAULT NULL COMMENT '涓嬪彂璇锋眰鎶ユ枃',
    reply_payload LONGTEXT DEFAULT NULL COMMENT '璁惧鍥炴墽鎶ユ枃',
    qos TINYINT NOT NULL DEFAULT 0 COMMENT 'MQTT QoS',
    retained TINYINT NOT NULL DEFAULT 0 COMMENT '鏄惁淇濈暀娑堟伅 1鏄?0鍚?,
    status VARCHAR(32) NOT NULL COMMENT '鍛戒护鐘舵€?CREATED/SENT/SUCCESS/FAILED/TIMEOUT',
    send_time DATETIME DEFAULT NULL COMMENT '鍙戦€佹椂闂?,
    ack_time DATETIME DEFAULT NULL COMMENT '鍥炴墽鏃堕棿',
    timeout_time DATETIME DEFAULT NULL COMMENT '瓒呮椂鏃堕棿',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '閿欒淇℃伅',
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_command_id (command_id),
    KEY idx_device_status (device_code, status),
    KEY idx_status_timeout (status, timeout_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='璁惧鍛戒护璁板綍琛?;

-- =========================
-- 3) Alarm / risk domain
-- =========================
CREATE TABLE iot_alarm_record (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    alarm_code VARCHAR(64) NOT NULL COMMENT '鍛婅缂栧彿',
    alarm_title VARCHAR(255) NOT NULL COMMENT '鍛婅鏍囬',
    alarm_type VARCHAR(32) NOT NULL COMMENT '鍛婅绫诲瀷',
    alarm_level VARCHAR(16) NOT NULL COMMENT '鍛婅绛夌骇',
    region_id BIGINT DEFAULT NULL COMMENT '鍖哄煙ID',
    region_name VARCHAR(128) DEFAULT NULL COMMENT '鍖哄煙鍚嶇О',
    risk_point_id BIGINT DEFAULT NULL COMMENT '椋庨櫓鐐笽D',
    risk_point_name VARCHAR(128) DEFAULT NULL COMMENT '椋庨櫓鐐瑰悕绉?,
    device_id BIGINT NOT NULL COMMENT '璁惧ID',
    device_code VARCHAR(64) NOT NULL COMMENT '璁惧缂栫爜',
    device_name VARCHAR(128) NOT NULL COMMENT '璁惧鍚嶇О',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT '娴嬬偣鍚嶇О',
    current_value VARCHAR(255) DEFAULT NULL COMMENT '褰撳墠鍊?,
    threshold_value VARCHAR(255) DEFAULT NULL COMMENT '闃堝€?,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '鐘舵€?0-鏈‘璁?1-宸茬‘璁?2-宸叉姂鍒?3-宸插叧闂?,
    trigger_time DATETIME NOT NULL COMMENT '瑙﹀彂鏃堕棿',
    confirm_time DATETIME DEFAULT NULL COMMENT '纭鏃堕棿',
    confirm_user BIGINT DEFAULT NULL COMMENT '纭鐢ㄦ埛',
    suppress_time DATETIME DEFAULT NULL COMMENT '鎶戝埗鏃堕棿',
    suppress_user BIGINT DEFAULT NULL COMMENT '鎶戝埗鐢ㄦ埛',
    close_time DATETIME DEFAULT NULL COMMENT '鍏抽棴鏃堕棿',
    close_user BIGINT DEFAULT NULL COMMENT '鍏抽棴鐢ㄦ埛',
    rule_id BIGINT DEFAULT NULL COMMENT '瑙勫垯ID',
    rule_name VARCHAR(128) DEFAULT NULL COMMENT '瑙勫垯鍚嶇О',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_alarm_code (alarm_code),
    KEY idx_device_status (device_code, status),
    KEY idx_trigger_time (trigger_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鍛婅璁板綍琛?;

CREATE TABLE iot_event_record (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    event_code VARCHAR(64) NOT NULL COMMENT '浜嬩欢缂栧彿',
    event_title VARCHAR(255) NOT NULL COMMENT '浜嬩欢鏍囬',
    alarm_id BIGINT DEFAULT NULL COMMENT '鍛婅ID',
    alarm_code VARCHAR(64) DEFAULT NULL COMMENT '鍛婅缂栧彿',
    alarm_level VARCHAR(16) DEFAULT NULL COMMENT '鍛婅绛夌骇',
    risk_level VARCHAR(16) DEFAULT NULL COMMENT '椋庨櫓绛夌骇',
    region_id BIGINT DEFAULT NULL COMMENT '鍖哄煙ID',
    region_name VARCHAR(128) DEFAULT NULL COMMENT '鍖哄煙鍚嶇О',
    risk_point_id BIGINT DEFAULT NULL COMMENT '椋庨櫓鐐笽D',
    risk_point_name VARCHAR(128) DEFAULT NULL COMMENT '椋庨櫓鐐瑰悕绉?,
    device_id BIGINT NOT NULL COMMENT '璁惧ID',
    device_code VARCHAR(64) NOT NULL COMMENT '璁惧缂栫爜',
    device_name VARCHAR(128) NOT NULL COMMENT '璁惧鍚嶇О',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT '娴嬬偣鍚嶇О',
    current_value VARCHAR(255) DEFAULT NULL COMMENT '褰撳墠鍊?,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '鐘舵€?0-寰呮淳鍙?1-宸叉淳鍙?2-澶勭悊涓?3-寰呴獙鏀?4-宸插叧闂?5-宸插彇娑?,
    responsible_user BIGINT DEFAULT NULL COMMENT '璐ｄ换浜?,
    urgency_level VARCHAR(16) DEFAULT NULL COMMENT '绱ф€ョ▼搴?,
    arrival_time_limit INT DEFAULT NULL COMMENT '鍒板満鏃堕檺锛堝垎閽燂級',
    completion_time_limit INT DEFAULT NULL COMMENT '瀹屾垚鏃堕檺锛堝垎閽燂級',
    trigger_time DATETIME NOT NULL COMMENT '瑙﹀彂鏃堕棿',
    dispatch_time DATETIME DEFAULT NULL COMMENT '娲惧彂鏃堕棿',
    dispatch_user BIGINT DEFAULT NULL COMMENT '娲惧彂鐢ㄦ埛',
    start_time DATETIME DEFAULT NULL COMMENT '澶勭悊寮€濮嬫椂闂?,
    complete_time DATETIME DEFAULT NULL COMMENT '澶勭悊瀹屾垚鏃堕棿',
    close_time DATETIME DEFAULT NULL COMMENT '鍏抽棴鏃堕棿',
    close_user BIGINT DEFAULT NULL COMMENT '鍏抽棴鐢ㄦ埛',
    close_reason VARCHAR(500) DEFAULT NULL COMMENT '鍏抽棴鍘熷洜',
    review_notes TEXT DEFAULT NULL COMMENT '澶嶇洏璁板綍',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_code (event_code),
    KEY idx_device_status (device_code, status),
    KEY idx_trigger_time (trigger_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浜嬩欢璁板綍琛?;

CREATE TABLE iot_event_work_order (
    id BIGINT NOT NULL COMMENT '涓婚敭',
    event_id BIGINT NOT NULL COMMENT '浜嬩欢ID',
    event_code VARCHAR(64) NOT NULL COMMENT '浜嬩欢缂栧彿',
    work_order_code VARCHAR(64) NOT NULL COMMENT '宸ュ崟缂栧彿',
    work_order_type VARCHAR(32) NOT NULL DEFAULT 'event-dispatch' COMMENT '宸ュ崟绫诲瀷',
    assign_user BIGINT NOT NULL COMMENT '娲惧彂鐢ㄦ埛',
    receive_user BIGINT DEFAULT NULL COMMENT '鎺ユ敹鐢ㄦ埛',
    receive_time DATETIME DEFAULT NULL COMMENT '鎺ユ敹鏃堕棿',
    start_time DATETIME DEFAULT NULL COMMENT '寮€濮嬫椂闂?,
    complete_time DATETIME DEFAULT NULL COMMENT '瀹屾垚鏃堕棿',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '鐘舵€?0-寰呮帴鏀?1-宸叉帴鏀?2-澶勭悊涓?3-宸插畬鎴?4-宸插彇娑?,
    feedback TEXT DEFAULT NULL COMMENT '鐜板満鍙嶉',
    photos LONGTEXT DEFAULT NULL COMMENT '鐓х墖URL锛圝SON鏁扮粍锛?,
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_order_code (work_order_code),
    KEY idx_event_status (event_id, status),
    KEY idx_receive_time (receive_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浜嬩欢宸ュ崟琛?;

CREATE TABLE risk_point (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
    risk_point_code VARCHAR(64) NOT NULL COMMENT '椋庨櫓鐐圭紪鍙?,
    risk_point_name VARCHAR(128) NOT NULL COMMENT '椋庨櫓鐐瑰悕绉?,
    org_id BIGINT DEFAULT NULL COMMENT '鎵€灞炵粍缁嘔D',
    org_name VARCHAR(128) DEFAULT NULL COMMENT '鎵€灞炵粍缁囧悕绉?,
    region_id BIGINT DEFAULT NULL COMMENT '鍖哄煙ID',
    region_name VARCHAR(128) DEFAULT NULL COMMENT '鍖哄煙鍚嶇О',
    responsible_user BIGINT DEFAULT NULL COMMENT '璐熻矗浜?,
    responsible_phone VARCHAR(32) DEFAULT NULL COMMENT '璐熻矗浜虹數璇?,
    risk_point_level VARCHAR(16) DEFAULT NULL COMMENT '椋庨櫓鐐规。妗堢瓑绾?level_1/level_2/level_3',
    current_risk_level VARCHAR(16) DEFAULT NULL COMMENT '褰撳墠椋庨櫓鎬佸娍绛夌骇 red/orange/yellow/blue',
    risk_level VARCHAR(20) DEFAULT NULL COMMENT '鍘嗗彶椋庨櫓绛夌骇鍏煎瀛楁',
    risk_type VARCHAR(32) NOT NULL DEFAULT 'GENERAL' COMMENT '椋庨櫓鐐圭被鍨?SLOPE/BRIDGE/TUNNEL/GENERAL',
    location_text VARCHAR(255) DEFAULT NULL COMMENT '浣嶇疆鎻忚堪/妗╁彿/鍖洪棿',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '椋庨櫓鐐圭粡搴?,
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '椋庨櫓鐐圭含搴?,
    description VARCHAR(1000) DEFAULT NULL COMMENT '鎻忚堪',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '鐘舵€?0-鍚敤 1-鍋滅敤',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_point_code_tenant (tenant_id, risk_point_code),
    KEY idx_region (region_id),
    KEY idx_status (status),
    KEY idx_risk_type_status (risk_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='椋庨櫓鐐硅〃';

CREATE TABLE risk_point_highway_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
    risk_point_id BIGINT NOT NULL COMMENT '椋庨櫓鐐笽D',
    project_name VARCHAR(255) NOT NULL COMMENT '椤圭洰鍚嶇О',
    project_type VARCHAR(32) NOT NULL COMMENT '椤圭洰绫诲瀷',
    project_summary TEXT DEFAULT NULL COMMENT '椤圭洰绠€浠?,
    route_code VARCHAR(64) NOT NULL COMMENT '璺嚎缂栧彿',
    route_name VARCHAR(128) DEFAULT NULL COMMENT '璺嚎鍚嶇О',
    road_level VARCHAR(64) DEFAULT NULL COMMENT '鍏矾绛夌骇',
    project_risk_level VARCHAR(32) DEFAULT NULL COMMENT '椤圭洰椋庨櫓绛夌骇鍘熷鍊?,
    admin_region_code VARCHAR(32) DEFAULT NULL COMMENT '琛屾斂鍖哄煙鏈骇缂栫爜',
    admin_region_path_json VARCHAR(255) DEFAULT NULL COMMENT '琛屾斂鍖哄煙璺緞JSON',
    maintenance_org_name VARCHAR(128) DEFAULT NULL COMMENT '绠″吇鍗曚綅鍚嶇О',
    source_row_no INT DEFAULT NULL COMMENT 'Excel鏉ユ簮琛屽彿',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_point_highway (risk_point_id),
    KEY idx_route_code (route_code),
    KEY idx_admin_region_code (admin_region_code),
    KEY idx_project_type (project_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楂橀€熷叕璺闄╃偣鎵╁睍琛?;

CREATE TABLE risk_metric_catalog (
    id BIGINT NOT NULL COMMENT 'Primary key',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
    product_id BIGINT NOT NULL COMMENT 'Product ID',
    product_model_id BIGINT DEFAULT NULL COMMENT 'Product model ID',
    contract_identifier VARCHAR(64) NOT NULL COMMENT 'Contract identifier',
    risk_metric_code VARCHAR(64) NOT NULL COMMENT 'Risk metric code',
    risk_metric_name VARCHAR(128) NOT NULL COMMENT 'Risk metric name',
    source_scenario_code VARCHAR(64) DEFAULT NULL COMMENT 'Source scenario code',
    metric_unit VARCHAR(32) DEFAULT NULL COMMENT 'Metric unit',
    metric_dimension VARCHAR(64) DEFAULT NULL COMMENT 'Metric dimension',
    threshold_type VARCHAR(32) DEFAULT NULL COMMENT 'Threshold type',
    semantic_direction VARCHAR(32) DEFAULT NULL COMMENT 'Semantic direction',
    threshold_direction VARCHAR(32) DEFAULT NULL COMMENT 'Threshold direction',
    trend_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'Trend enabled flag',
    gis_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'GIS enabled flag',
    insight_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'Insight enabled flag',
    analytics_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'Analytics enabled flag',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT 'Enable status',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_metric_catalog (product_id, contract_identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Risk metric catalog';

CREATE TABLE risk_point_device (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
    risk_point_id BIGINT NOT NULL COMMENT '椋庨櫓鐐笽D',
    device_id BIGINT NOT NULL COMMENT '璁惧ID',
    device_code VARCHAR(64) DEFAULT NULL COMMENT '璁惧缂栫爜',
    device_name VARCHAR(128) DEFAULT NULL COMMENT '璁惧鍚嶇О',
    risk_metric_id BIGINT DEFAULT NULL COMMENT '椋庨櫓鎸囨爣ID',
    metric_identifier VARCHAR(64) NOT NULL COMMENT '娴嬬偣鏍囪瘑绗?,
    metric_name VARCHAR(64) DEFAULT NULL COMMENT '娴嬬偣鍚嶇О',
    default_threshold VARCHAR(64) DEFAULT NULL COMMENT '榛樿闃堝€?,
    threshold_unit VARCHAR(20) DEFAULT NULL COMMENT '闃堝€煎崟浣?,
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_point_metric (risk_point_id, device_id, metric_identifier),
    KEY idx_risk_device (risk_point_id, device_id),
    KEY idx_risk_point_device_metric_catalog (risk_metric_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='椋庨櫓鐐硅澶囩粦瀹氳〃';

CREATE TABLE risk_point_device_pending_binding (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
    batch_no VARCHAR(64) NOT NULL COMMENT '瀵煎叆鎵规鍙?,
    source_file_name VARCHAR(255) DEFAULT NULL COMMENT '鏉ユ簮鏂囦欢鍚?,
    source_row_no INT NOT NULL COMMENT '鏉ユ簮琛屽彿',
    risk_point_name VARCHAR(128) NOT NULL COMMENT '鏉ユ簮椋庨櫓鐐瑰悕绉?,
    risk_point_id BIGINT DEFAULT NULL COMMENT '鍖归厤鍒扮殑椋庨櫓鐐笽D',
    risk_point_code VARCHAR(64) DEFAULT NULL COMMENT '鍖归厤鍒扮殑椋庨櫓鐐圭紪鍙?,
    device_code VARCHAR(64) NOT NULL COMMENT '鏉ユ簮璁惧缂栫爜',
    device_id BIGINT DEFAULT NULL COMMENT '鍖归厤鍒扮殑璁惧ID',
    device_name VARCHAR(128) DEFAULT NULL COMMENT '鍖归厤鍒扮殑璁惧鍚嶇О',
    resolution_status VARCHAR(64) NOT NULL DEFAULT 'PENDING_METRIC_GOVERNANCE' COMMENT '娌荤悊鐘舵€?,
    resolution_note VARCHAR(500) DEFAULT NULL COMMENT '娌荤悊璇存槑',
    metric_identifier VARCHAR(64) DEFAULT NULL COMMENT '鍚庣画琛ュ綍娴嬬偣鏍囪瘑',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT '鍚庣画琛ュ綍娴嬬偣鍚嶇О',
    promoted_binding_id BIGINT DEFAULT NULL COMMENT '杞鍚庣殑姝ｅ紡缁戝畾ID',
    promoted_time DATETIME DEFAULT NULL COMMENT '杞鏃堕棿',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pending_binding_batch_row (tenant_id, batch_no, source_row_no),
    KEY idx_pending_binding_status (tenant_id, resolution_status, deleted),
    KEY idx_pending_binding_risk_device (risk_point_id, device_id, deleted),
    KEY idx_pending_binding_device_code (tenant_id, device_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='椋庨櫓鐐硅澶囧緟娌荤悊瀵煎叆琛?;

CREATE TABLE risk_point_device_pending_promotion (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
    pending_binding_id BIGINT NOT NULL COMMENT '鏉ユ簮寰呮不鐞嗚褰旾D',
    risk_point_device_id BIGINT DEFAULT NULL COMMENT '姝ｅ紡缁戝畾ID',
    risk_point_id BIGINT NOT NULL COMMENT '椋庨櫓鐐笽D',
    device_id BIGINT NOT NULL COMMENT '璁惧ID',
    device_code VARCHAR(64) NOT NULL COMMENT '璁惧缂栫爜',
    device_name VARCHAR(128) DEFAULT NULL COMMENT '璁惧鍚嶇О',
    metric_identifier VARCHAR(64) NOT NULL COMMENT '娴嬬偣鏍囪瘑',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT '娴嬬偣鍚嶇О',
    promotion_status VARCHAR(32) NOT NULL COMMENT '杞缁撴灉',
    recommendation_level VARCHAR(16) DEFAULT NULL COMMENT '鎺ㄨ崘绛夌骇',
    recommendation_score INT DEFAULT NULL COMMENT '鎺ㄨ崘璇勫垎',
    evidence_snapshot_json JSON DEFAULT NULL COMMENT '璇佹嵁蹇収',
    promotion_note VARCHAR(500) DEFAULT NULL COMMENT '娌荤悊璇存槑',
    operator_id BIGINT DEFAULT NULL COMMENT '鎿嶄綔浜篒D',
    operator_name VARCHAR(128) DEFAULT NULL COMMENT '鎿嶄綔浜哄鍚?,
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_pending_promotion_pending_id (pending_binding_id),
    KEY idx_pending_promotion_binding_id (risk_point_device_id),
    KEY idx_pending_promotion_status (tenant_id, promotion_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='椋庨櫓鐐硅澶囧緟娌荤悊杞鏄庣粏琛?;

CREATE TABLE rule_definition (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
    rule_name VARCHAR(128) NOT NULL COMMENT '瑙勫垯鍚嶇О',
    risk_metric_id BIGINT DEFAULT NULL COMMENT '椋庨櫓鎸囨爣ID',
    metric_identifier VARCHAR(64) NOT NULL COMMENT '娴嬬偣鏍囪瘑绗?,
    metric_name VARCHAR(64) DEFAULT NULL COMMENT '娴嬬偣鍚嶇О',
    expression VARCHAR(256) DEFAULT NULL COMMENT '琛ㄨ揪寮?,
    duration INT NOT NULL DEFAULT 0 COMMENT '鎸佺画鏃堕棿(绉?',
    alarm_level VARCHAR(20) DEFAULT NULL COMMENT '鍛婅绛夌骇',
    notification_methods VARCHAR(64) DEFAULT NULL COMMENT '閫氱煡鏂瑰紡',
    convert_to_event TINYINT NOT NULL DEFAULT 0 COMMENT '鏄惁杞簨浠?,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '鐘舵€?0鍚敤 1鍋滅敤',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_metric_identifier (metric_identifier),
    KEY idx_rule_definition_metric_catalog (risk_metric_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闃堝€艰鍒欒〃';

CREATE TABLE linkage_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
    rule_name VARCHAR(128) NOT NULL COMMENT '瑙勫垯鍚嶇О',
    description VARCHAR(512) DEFAULT NULL COMMENT '鎻忚堪',
    trigger_condition LONGTEXT DEFAULT NULL COMMENT '瑙﹀彂鏉′欢(JSON)',
    action_list LONGTEXT DEFAULT NULL COMMENT '鍔ㄤ綔鍒楄〃(JSON)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '鐘舵€?0鍚敤 1鍋滅敤',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鑱斿姩瑙勫垯琛?;

CREATE TABLE emergency_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
    plan_name VARCHAR(128) NOT NULL COMMENT '棰勬鍚嶇О',
    alarm_level VARCHAR(16) DEFAULT NULL COMMENT '閫傜敤鍛婅绛夌骇 red/orange/yellow/blue',
    risk_level VARCHAR(20) DEFAULT NULL COMMENT '鍘嗗彶椋庨櫓绛夌骇鍏煎瀛楁',
    description VARCHAR(512) DEFAULT NULL COMMENT '鎻忚堪',
    response_steps LONGTEXT DEFAULT NULL COMMENT '鍝嶅簲姝ラ(JSON)',
    contact_list LONGTEXT DEFAULT NULL COMMENT '鑱旂郴浜哄垪琛?JSON)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '鐘舵€?0鍚敤 1鍋滅敤',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搴旀€ラ妗堣〃';

DROP TABLE IF EXISTS iot_device_secret_rotation_log;

CREATE TABLE iot_device_secret_rotation_log (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    device_code VARCHAR(64) NOT NULL COMMENT '设备编码',
    product_key VARCHAR(64) DEFAULT NULL COMMENT '产品key',
    rotation_batch_id VARCHAR(64) NOT NULL COMMENT '轮换批次ID',
    reason VARCHAR(500) DEFAULT NULL COMMENT '轮换原因',
    previous_secret_digest VARCHAR(128) DEFAULT NULL COMMENT '旧密钥摘要',
    current_secret_digest VARCHAR(128) DEFAULT NULL COMMENT '新密钥摘要',
    rotated_by BIGINT NOT NULL COMMENT '执行人',
    approved_by BIGINT NOT NULL COMMENT '复核人',
    rotate_time DATETIME NOT NULL COMMENT '轮换时间',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_device_rotation_device_time (device_id, rotate_time),
    KEY idx_device_rotation_batch (rotation_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备密钥轮换日志表';

-- 鏂囨。缁熶竴鍛藉悕鍏煎瑙嗗浘
CREATE OR REPLACE VIEW iot_message_log AS
SELECT
    id,
    tenant_id,
    device_id,
    product_id,
    trace_id,
    device_code,
    product_key,
    message_type,
    topic,
    payload,
    report_time,
    create_time
FROM iot_device_message_log;


