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
    dedup_key VARCHAR(32) DEFAULT NULL COMMENT '去重键',
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
    KEY idx_in_app_message_deleted_target_sort (deleted, target_type, sort_no, id),
    KEY idx_in_app_message_source (source_type, source_id),
    KEY idx_in_app_message_tenant_dedup (tenant_id, dedup_key, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

SET @has_in_app_message_dedup_key = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_in_app_message'
      AND COLUMN_NAME = 'dedup_key'
);

SET @in_app_message_dedup_key_sql = IF(
    @has_in_app_message_dedup_key = 0,
    'ALTER TABLE sys_in_app_message ADD COLUMN dedup_key VARCHAR(32) DEFAULT NULL COMMENT ''去重键'' AFTER source_id',
    'SELECT 1'
);

PREPARE stmt_add_in_app_message_dedup_key FROM @in_app_message_dedup_key_sql;
EXECUTE stmt_add_in_app_message_dedup_key;
DEALLOCATE PREPARE stmt_add_in_app_message_dedup_key;

UPDATE sys_in_app_message
SET dedup_key = CASE
                    WHEN COALESCE(source_type, '') <> ''
                        AND COALESCE(source_id, '') <> ''
                        AND COALESCE(message_type, '') <> ''
                        AND target_type = 'all'
                        THEN MD5(CONCAT_WS('|', source_type, source_id, 'all', message_type))
                    WHEN COALESCE(source_type, '') <> ''
                        AND COALESCE(source_id, '') <> ''
                        AND COALESCE(message_type, '') <> ''
                        AND target_type = 'role'
                        AND COALESCE(target_role_codes, '') <> ''
                        THEN MD5(CONCAT_WS('|', source_type, source_id, CONCAT('role:', target_role_codes), message_type))
                    WHEN COALESCE(source_type, '') <> ''
                        AND COALESCE(source_id, '') <> ''
                        AND COALESCE(message_type, '') <> ''
                        AND target_type = 'user'
                        AND COALESCE(target_user_ids, '') <> ''
                        THEN MD5(CONCAT_WS('|', source_type, source_id, CONCAT('user:', target_user_ids), message_type))
                    ELSE dedup_key
                END
WHERE COALESCE(dedup_key, '') = '';

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
    related_path, source_type, source_id, dedup_key, publish_time, expire_time, status, sort_no, create_by, create_time, update_by, update_time, deleted
) VALUES
    (760101, 1, 'system', 'critical', '系统维护窗口提醒', '今晚 23:00 将执行日志链路维护，请提前完成排障导出。', '平台计划在今晚 23:00 至 23:30 执行日志链路维护。维护期间通知中心、审计中心与异常观测台可能存在短时延迟，请运维与研发同事提前导出必要信息。', 'all', NULL, NULL,
     '/system-log', 'manual', 'maintenance-20260321', MD5('manual|maintenance-20260321|all|system'), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, 1, 1, NOW(), 1, NOW(), 0),
    (760102, 1, 'business', 'high', '风险运营日报待确认', '请业务与管理角色在 18:00 前确认今日告警闭环情况。', '风险运营日报已生成，请优先核对今日告警确认率、事件派工状态和待关闭事项。如存在跨班次未闭环问题，请在事件协同台补充反馈。', 'role', 'BUSINESS_STAFF,MANAGEMENT_STAFF', NULL,
     '/alarm-center', 'manual', 'risk-report-20260321', MD5('manual|risk-report-20260321|role:BUSINESS_STAFF,MANAGEMENT_STAFF|business'), DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_ADD(NOW(), INTERVAL 2 DAY), 1, 2, 1, NOW(), 1, NOW(), 0),
    (760103, 1, 'error', 'high', '接入链路异常排查提示', '检测到最近 30 分钟内存在 MQTT 分发失败，请研发和运维优先复核 TraceId。', '系统在最近 30 分钟内检测到 MQTT 分发失败与设备不存在异常。请先在异常观测台定位 system_error，再到链路追踪台按 TraceId 复核上下游日志。', 'role', 'OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN', NULL,
     '/message-trace', 'system_error', 'mqtt-dispatch-failed', MD5('system_error|mqtt-dispatch-failed|role:OPS_STAFF,DEVELOPER_STAFF,SUPER_ADMIN|error'), DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_ADD(NOW(), INTERVAL 3 DAY), 1, 3, 1, NOW(), 1, NOW(), 0),
    (760104, 1, 'business', 'medium', '通知编排配置复核', '管理员请复核默认 webhook 渠道是否仍指向有效地址。', '近期已补齐系统异常自动通知能力，请管理员复核默认 webhook 地址、超时时间与 system_error 场景配置，避免真实环境异常无人接收。', 'user', NULL, '1',
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
