USE rm_iot;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_in_app_message (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    message_type VARCHAR(32) NOT NULL COMMENT '消息类型 system/business/error',
    priority VARCHAR(32) NOT NULL DEFAULT 'medium' COMMENT '优先级 critical/high/medium/low',
    title VARCHAR(128) NOT NULL COMMENT '消息标题',
    summary VARCHAR(500) DEFAULT NULL COMMENT '消息摘要',
    content LONGTEXT DEFAULT NULL COMMENT '消息正文',
    target_type VARCHAR(16) NOT NULL DEFAULT 'all' COMMENT '推送范围 all/role/user',
    target_role_codes VARCHAR(500) DEFAULT NULL COMMENT '目标角色编码，逗号分隔',
    target_user_ids VARCHAR(500) DEFAULT NULL COMMENT '目标用户ID，逗号分隔',
    related_path VARCHAR(255) DEFAULT NULL COMMENT '关联页面路径',
    source_type VARCHAR(64) DEFAULT NULL COMMENT '来源类型',
    source_id VARCHAR(64) DEFAULT NULL COMMENT '来源业务ID',
    publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1发布中 0停用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_in_app_message_deleted_status_time (deleted, status, publish_time, id),
    KEY idx_in_app_message_deleted_type_time (deleted, message_type, publish_time, id),
    KEY idx_in_app_message_deleted_target_sort (deleted, target_type, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

CREATE TABLE IF NOT EXISTS sys_in_app_message_read (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    message_id BIGINT NOT NULL COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    read_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_in_app_message_read_user (tenant_id, message_id, user_id),
    KEY idx_in_app_message_read_user_time (user_id, read_time, message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息已读表';

CREATE TABLE IF NOT EXISTS sys_help_document (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_category VARCHAR(32) NOT NULL COMMENT '文档分类 business/technical/faq',
    title VARCHAR(128) NOT NULL COMMENT '文档标题',
    summary VARCHAR(500) DEFAULT NULL COMMENT '文档摘要',
    content LONGTEXT NOT NULL COMMENT '文档正文',
    keywords VARCHAR(500) DEFAULT NULL COMMENT '关键词，逗号分隔',
    related_paths VARCHAR(500) DEFAULT NULL COMMENT '关联页面路径，逗号分隔',
    visible_role_codes VARCHAR(500) DEFAULT NULL COMMENT '可见角色编码，逗号分隔',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_help_document_deleted_category_sort (deleted, doc_category, sort_no, id),
    KEY idx_help_document_deleted_status_sort (deleted, status, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帮助文档表';

INSERT INTO sys_in_app_message (
    id, tenant_id, message_type, priority, title, summary, content, target_type, target_role_codes, target_user_ids,
    related_path, source_type, source_id, publish_time, expire_time, status, sort_no, create_by, create_time, update_by, update_time, deleted
) VALUES
    (760101, 1, 'system', 'critical', '系统维护窗口提醒', '今晚 23:00 将执行日志链路维护，请提前完成排障导出。', '平台计划在今晚 23:00 至 23:30 执行日志链路维护。维护期间通知中心、审计中心与异常观测台可能存在短时延迟，请运维与研发同事提前导出必要信息。', 'all', NULL, NULL,
     '/system-log', 'system_maintenance', 'maintenance-20260321', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, 1, 1, NOW(), 1, NOW(), 0),
    (760102, 1, 'business', 'high', '风险运营日报待确认', '请业务与管理角色在 18:00 前确认今日告警闭环情况。', '风险运营日报已生成，请优先核对今日告警确认率、事件派工状态和待关闭事项。如存在跨班次未闭环问题，请在事件协同台补充反馈。', 'role', 'BUSINESS_STAFF,MANAGEMENT_STAFF', NULL,
     '/alarm-center', 'daily_report', 'risk-report-20260321', DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_ADD(NOW(), INTERVAL 2 DAY), 1, 2, 1, NOW(), 1, NOW(), 0),
    (760103, 1, 'error', 'high', '接入链路异常排查提示', '检测到最近 30 分钟内存在 MQTT 分发失败，请研发和运维优先复核 TraceId。', '系统在最近 30 分钟内检测到 MQTT 分发失败与设备不存在异常。请先在异常观测台定位 system_error，再到链路追踪台按 TraceId 复核上下游日志。', 'role', 'OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN', NULL,
     '/message-trace', 'system_error', 'mqtt-dispatch-failed', DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_ADD(NOW(), INTERVAL 3 DAY), 1, 3, 1, NOW(), 1, NOW(), 0),
    (760104, 1, 'business', 'medium', '通知编排配置复核', '管理员请复核默认 webhook 渠道是否仍指向有效地址。', '近期已补齐系统异常自动通知能力，请管理员复核默认 webhook 地址、超时时间与 system_error 场景配置，避免真实环境异常无人接收。', 'user', NULL, '1',
     '/channel', 'governance_task', 'channel-review-admin', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 5 DAY), 1, 4, 1, NOW(), 1, NOW(), 0)
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
    publish_time = VALUES(publish_time),
    expire_time = VALUES(expire_time),
    status = VALUES(status),
    sort_no = VALUES(sort_no),
    update_by = 1,
    update_time = NOW(),
    deleted = 0;

SET @user_admin_id = (SELECT id FROM sys_user WHERE tenant_id = 1 AND username = 'admin' AND deleted = 0 ORDER BY id LIMIT 1);
SET @user_business_demo_id = (SELECT id FROM sys_user WHERE tenant_id = 1 AND username = 'biz_demo' AND deleted = 0 ORDER BY id LIMIT 1);

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
    (760301, 1, 'business', '告警运营与事件协同业务手册', '面向业务与管理角色的日常闭环指引。', '本手册覆盖告警确认、抑制、关闭，以及事件派工、接收、开始、完成和关闭的基础流程。建议业务与管理角色优先按“告警运营台 -> 事件协同台 -> 运营分析中心”顺序复核每日闭环进度。', '告警,事件,闭环,运营日报', '/alarm-center,/event-disposal,/report-analysis', 'BUSINESS_STAFF,MANAGEMENT_STAFF',
     1, 1, 1, NOW(), 1, NOW(), 0),
    (760302, 1, 'technical', '接入异常排查与 TraceId 使用说明', '面向运维与研发角色的链路排障资料。', '本资料汇总链路验证中心、异常观测台、链路追踪台的排障路径。遇到 MQTT 订阅失败、消息分发失败、设备不存在等问题时，优先按 TraceId 串联 system_error 审计与设备消息日志。', 'TraceId,排障,MQTT,system_error', '/reporting,/system-log,/message-trace', 'OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN',
     1, 2, 1, NOW(), 1, NOW(), 0),
    (760303, 1, 'faq', '常见问题：产品与设备建档有什么区别', '统一解释产品、设备、父子拓扑和建档顺序。', '产品是接入模板，设备是现场资产实例。新增设备前必须先确定产品身份与协议契约；若存在父设备或网关关系，应在新增、编辑、批量导入或设备更换场景同步维护。', '产品,设备,建档,拓扑,FAQ', '/products,/devices', '',
     1, 3, 1, NOW(), 1, NOW(), 0),
    (760304, 1, 'faq', '通知中心与帮助中心如何使用', '说明右上角壳层入口的分类规则与使用方式。', '通知中心按系统事件、业务事件、错误事件聚合站内消息；帮助中心按业务类、技术类、FAQ 汇总资料。后续若接入真实 API，请继续保留当前分类与权限过滤口径。', '通知中心,帮助中心,FAQ,壳层', NULL, '',
     1, 4, 1, NOW(), 1, NOW(), 0)
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
