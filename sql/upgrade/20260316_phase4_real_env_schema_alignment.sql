USE rm_iot;

-- Real-environment schema alignment for Phase 4 smoke verification.
-- This script is designed to be idempotent and non-destructive.
-- Date: 2026-03-16

CREATE TABLE IF NOT EXISTS iot_command_record (
    id BIGINT NOT NULL COMMENT 'pk',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    command_id VARCHAR(64) DEFAULT NULL COMMENT 'business command id',
    device_id BIGINT DEFAULT NULL COMMENT 'device id',
    device_code VARCHAR(64) DEFAULT NULL COMMENT 'device code',
    product_key VARCHAR(64) DEFAULT NULL COMMENT 'product key',
    gateway_device_code VARCHAR(64) DEFAULT NULL COMMENT 'gateway code',
    sub_device_code VARCHAR(64) DEFAULT NULL COMMENT 'sub device code',
    topic VARCHAR(255) DEFAULT NULL COMMENT 'downlink topic',
    command_type VARCHAR(32) DEFAULT NULL COMMENT 'command type',
    service_identifier VARCHAR(64) DEFAULT NULL COMMENT 'service id',
    request_payload LONGTEXT DEFAULT NULL COMMENT 'request payload',
    reply_payload LONGTEXT DEFAULT NULL COMMENT 'reply payload',
    qos TINYINT NOT NULL DEFAULT 0 COMMENT 'mqtt qos',
    retained TINYINT NOT NULL DEFAULT 0 COMMENT 'retained',
    status VARCHAR(32) DEFAULT NULL COMMENT 'status',
    send_time DATETIME DEFAULT NULL COMMENT 'send time',
    ack_time DATETIME DEFAULT NULL COMMENT 'ack time',
    timeout_time DATETIME DEFAULT NULL COMMENT 'timeout time',
    error_message VARCHAR(500) DEFAULT NULL COMMENT 'error',
    remark VARCHAR(500) DEFAULT NULL COMMENT 'remark',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_device_status (device_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='iot command record';

CREATE TABLE IF NOT EXISTS iot_alarm_record (
    id BIGINT NOT NULL COMMENT 'pk',
    alarm_code VARCHAR(64) DEFAULT NULL COMMENT 'alarm code',
    alarm_title VARCHAR(255) DEFAULT NULL COMMENT 'alarm title',
    alarm_type VARCHAR(32) DEFAULT NULL COMMENT 'alarm type',
    alarm_level VARCHAR(16) DEFAULT NULL COMMENT 'alarm level',
    region_id BIGINT DEFAULT NULL COMMENT 'region id',
    region_name VARCHAR(128) DEFAULT NULL COMMENT 'region name',
    risk_point_id BIGINT DEFAULT NULL COMMENT 'risk point id',
    risk_point_name VARCHAR(128) DEFAULT NULL COMMENT 'risk point name',
    device_id BIGINT DEFAULT NULL COMMENT 'device id',
    device_code VARCHAR(64) DEFAULT NULL COMMENT 'device code',
    device_name VARCHAR(128) DEFAULT NULL COMMENT 'device name',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT 'metric name',
    current_value VARCHAR(255) DEFAULT NULL COMMENT 'current value',
    threshold_value VARCHAR(255) DEFAULT NULL COMMENT 'threshold value',
    status TINYINT NOT NULL DEFAULT 0 COMMENT 'status',
    trigger_time DATETIME DEFAULT NULL COMMENT 'trigger time',
    confirm_time DATETIME DEFAULT NULL COMMENT 'confirm time',
    confirm_user BIGINT DEFAULT NULL COMMENT 'confirm user',
    suppress_time DATETIME DEFAULT NULL COMMENT 'suppress time',
    suppress_user BIGINT DEFAULT NULL COMMENT 'suppress user',
    close_time DATETIME DEFAULT NULL COMMENT 'close time',
    close_user BIGINT DEFAULT NULL COMMENT 'close user',
    rule_id BIGINT DEFAULT NULL COMMENT 'rule id',
    rule_name VARCHAR(128) DEFAULT NULL COMMENT 'rule name',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    remark VARCHAR(500) DEFAULT NULL COMMENT 'remark',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_device_status (device_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='iot alarm record';

CREATE TABLE IF NOT EXISTS iot_event_record (
    id BIGINT NOT NULL COMMENT 'pk',
    event_code VARCHAR(64) DEFAULT NULL COMMENT 'event code',
    event_title VARCHAR(255) DEFAULT NULL COMMENT 'event title',
    alarm_id BIGINT DEFAULT NULL COMMENT 'alarm id',
    alarm_code VARCHAR(64) DEFAULT NULL COMMENT 'alarm code',
    alarm_level VARCHAR(16) DEFAULT NULL COMMENT 'alarm level',
    risk_level VARCHAR(16) DEFAULT NULL COMMENT 'risk level',
    region_id BIGINT DEFAULT NULL COMMENT 'region id',
    region_name VARCHAR(128) DEFAULT NULL COMMENT 'region name',
    risk_point_id BIGINT DEFAULT NULL COMMENT 'risk point id',
    risk_point_name VARCHAR(128) DEFAULT NULL COMMENT 'risk point name',
    device_id BIGINT DEFAULT NULL COMMENT 'device id',
    device_code VARCHAR(64) DEFAULT NULL COMMENT 'device code',
    device_name VARCHAR(128) DEFAULT NULL COMMENT 'device name',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT 'metric name',
    current_value VARCHAR(255) DEFAULT NULL COMMENT 'current value',
    status TINYINT NOT NULL DEFAULT 0 COMMENT 'status',
    responsible_user BIGINT DEFAULT NULL COMMENT 'responsible user',
    urgency_level VARCHAR(16) DEFAULT NULL COMMENT 'urgency',
    arrival_time_limit INT DEFAULT NULL COMMENT 'arrival limit',
    completion_time_limit INT DEFAULT NULL COMMENT 'completion limit',
    trigger_time DATETIME DEFAULT NULL COMMENT 'trigger time',
    dispatch_time DATETIME DEFAULT NULL COMMENT 'dispatch time',
    dispatch_user BIGINT DEFAULT NULL COMMENT 'dispatch user',
    start_time DATETIME DEFAULT NULL COMMENT 'start time',
    complete_time DATETIME DEFAULT NULL COMMENT 'complete time',
    close_time DATETIME DEFAULT NULL COMMENT 'close time',
    close_user BIGINT DEFAULT NULL COMMENT 'close user',
    close_reason VARCHAR(500) DEFAULT NULL COMMENT 'close reason',
    review_notes TEXT DEFAULT NULL COMMENT 'review notes',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    remark VARCHAR(500) DEFAULT NULL COMMENT 'remark',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_device_status (device_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='iot event record';

CREATE TABLE IF NOT EXISTS iot_event_work_order (
    id BIGINT NOT NULL COMMENT 'pk',
    event_id BIGINT DEFAULT NULL COMMENT 'event id',
    event_code VARCHAR(64) DEFAULT NULL COMMENT 'event code',
    work_order_code VARCHAR(64) DEFAULT NULL COMMENT 'work order code',
    work_order_type VARCHAR(32) DEFAULT NULL COMMENT 'work order type',
    assign_user BIGINT DEFAULT NULL COMMENT 'assign user',
    receive_user BIGINT DEFAULT NULL COMMENT 'receive user',
    receive_time DATETIME DEFAULT NULL COMMENT 'receive time',
    start_time DATETIME DEFAULT NULL COMMENT 'start time',
    complete_time DATETIME DEFAULT NULL COMMENT 'complete time',
    status TINYINT NOT NULL DEFAULT 0 COMMENT 'status',
    feedback TEXT DEFAULT NULL COMMENT 'feedback',
    photos LONGTEXT DEFAULT NULL COMMENT 'photos',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    remark VARCHAR(500) DEFAULT NULL COMMENT 'remark',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_event_status (event_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='iot event work order';

CREATE TABLE IF NOT EXISTS risk_point (
    id BIGINT NOT NULL COMMENT 'pk',
    risk_point_code VARCHAR(64) DEFAULT NULL COMMENT 'risk point code',
    risk_point_name VARCHAR(128) DEFAULT NULL COMMENT 'risk point name',
    region_id BIGINT DEFAULT NULL COMMENT 'region id',
    region_name VARCHAR(128) DEFAULT NULL COMMENT 'region name',
    responsible_user BIGINT DEFAULT NULL COMMENT 'responsible user',
    responsible_phone VARCHAR(32) DEFAULT NULL COMMENT 'phone',
    risk_level VARCHAR(20) DEFAULT NULL COMMENT 'risk level',
    description VARCHAR(512) DEFAULT NULL COMMENT 'description',
    status TINYINT NOT NULL DEFAULT 0 COMMENT 'status',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_risk_point_code (risk_point_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='risk point';

CREATE TABLE IF NOT EXISTS risk_point_device (
    id BIGINT NOT NULL COMMENT 'pk',
    risk_point_id BIGINT DEFAULT NULL COMMENT 'risk point id',
    device_id BIGINT DEFAULT NULL COMMENT 'device id',
    device_code VARCHAR(64) DEFAULT NULL COMMENT 'device code',
    device_name VARCHAR(128) DEFAULT NULL COMMENT 'device name',
    metric_identifier VARCHAR(64) DEFAULT NULL COMMENT 'metric identifier',
    metric_name VARCHAR(64) DEFAULT NULL COMMENT 'metric name',
    default_threshold VARCHAR(64) DEFAULT NULL COMMENT 'default threshold',
    threshold_unit VARCHAR(20) DEFAULT NULL COMMENT 'threshold unit',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_risk_device (risk_point_id, device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='risk point device';

CREATE TABLE IF NOT EXISTS rule_definition (
    id BIGINT NOT NULL COMMENT 'pk',
    rule_name VARCHAR(128) DEFAULT NULL COMMENT 'rule name',
    metric_identifier VARCHAR(64) DEFAULT NULL COMMENT 'metric identifier',
    metric_name VARCHAR(64) DEFAULT NULL COMMENT 'metric name',
    expression VARCHAR(256) DEFAULT NULL COMMENT 'expression',
    duration INT DEFAULT 0 COMMENT 'duration',
    alarm_level VARCHAR(20) DEFAULT NULL COMMENT 'alarm level',
    notification_methods VARCHAR(64) DEFAULT NULL COMMENT 'notification methods',
    convert_to_event TINYINT NOT NULL DEFAULT 0 COMMENT 'convert to event',
    status TINYINT NOT NULL DEFAULT 0 COMMENT 'status',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_metric_identifier (metric_identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='rule definition';

CREATE TABLE IF NOT EXISTS linkage_rule (
    id BIGINT NOT NULL COMMENT 'pk',
    rule_name VARCHAR(128) DEFAULT NULL COMMENT 'rule name',
    description VARCHAR(512) DEFAULT NULL COMMENT 'description',
    trigger_condition LONGTEXT DEFAULT NULL COMMENT 'trigger condition',
    action_list LONGTEXT DEFAULT NULL COMMENT 'action list',
    status TINYINT NOT NULL DEFAULT 0 COMMENT 'status',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='linkage rule';

CREATE TABLE IF NOT EXISTS emergency_plan (
    id BIGINT NOT NULL COMMENT 'pk',
    plan_name VARCHAR(128) DEFAULT NULL COMMENT 'plan name',
    risk_level VARCHAR(20) DEFAULT NULL COMMENT 'risk level',
    description VARCHAR(512) DEFAULT NULL COMMENT 'description',
    response_steps LONGTEXT DEFAULT NULL COMMENT 'response steps',
    contact_list LONGTEXT DEFAULT NULL COMMENT 'contact list',
    status TINYINT NOT NULL DEFAULT 0 COMMENT 'status',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='emergency plan';

CREATE TABLE IF NOT EXISTS sys_organization (
    id BIGINT NOT NULL COMMENT 'pk',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT 'parent id',
    org_name VARCHAR(128) DEFAULT NULL COMMENT 'org name',
    org_code VARCHAR(64) DEFAULT NULL COMMENT 'org code',
    org_type VARCHAR(32) DEFAULT NULL COMMENT 'org type',
    leader_user_id BIGINT DEFAULT NULL COMMENT 'leader user id',
    leader_name VARCHAR(64) DEFAULT NULL COMMENT 'leader name',
    phone VARCHAR(32) DEFAULT NULL COMMENT 'phone',
    email VARCHAR(128) DEFAULT NULL COMMENT 'email',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'status',
    sort_no INT NOT NULL DEFAULT 0 COMMENT 'sort',
    remark VARCHAR(500) DEFAULT NULL COMMENT 'remark',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_org_code (org_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys organization';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL COMMENT 'pk',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    role_name VARCHAR(100) DEFAULT NULL COMMENT 'role name',
    role_code VARCHAR(100) DEFAULT NULL COMMENT 'role code',
    description VARCHAR(500) DEFAULT NULL COMMENT 'description',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'status',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys role';

CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGINT NOT NULL COMMENT 'pk',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    dict_name VARCHAR(128) DEFAULT NULL COMMENT 'dict name',
    dict_code VARCHAR(64) DEFAULT NULL COMMENT 'dict code',
    dict_type VARCHAR(32) DEFAULT NULL COMMENT 'dict type',
    dict_value VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'legacy dict value',
    dict_label VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'legacy dict label',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'status',
    sort_no INT NOT NULL DEFAULT 0 COMMENT 'sort',
    remark VARCHAR(500) DEFAULT NULL COMMENT 'remark',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_dict_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys dict';

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id BIGINT NOT NULL COMMENT 'pk',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    dict_id BIGINT DEFAULT NULL COMMENT 'dict id',
    item_name VARCHAR(128) DEFAULT NULL COMMENT 'item name',
    item_value VARCHAR(255) DEFAULT NULL COMMENT 'item value',
    item_type VARCHAR(32) DEFAULT NULL COMMENT 'item type',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'status',
    sort_no INT NOT NULL DEFAULT 0 COMMENT 'sort',
    remark VARCHAR(500) DEFAULT NULL COMMENT 'remark',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_dict_item (dict_id, item_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys dict item';

CREATE TABLE IF NOT EXISTS sys_notification_channel (
    id BIGINT NOT NULL COMMENT 'pk',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    channel_name VARCHAR(128) DEFAULT NULL COMMENT 'channel name',
    channel_code VARCHAR(64) DEFAULT NULL COMMENT 'channel code',
    channel_type VARCHAR(32) DEFAULT NULL COMMENT 'channel type',
    config LONGTEXT DEFAULT NULL COMMENT 'channel config',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'status',
    sort_no INT NOT NULL DEFAULT 0 COMMENT 'sort',
    remark VARCHAR(500) DEFAULT NULL COMMENT 'remark',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_channel_code (channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys notification channel';

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT NOT NULL COMMENT 'pk',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    user_id BIGINT DEFAULT NULL COMMENT 'user id',
    user_name VARCHAR(64) DEFAULT NULL COMMENT 'user name',
    operation_type VARCHAR(64) DEFAULT NULL COMMENT 'operation type',
    operation_module VARCHAR(128) DEFAULT NULL COMMENT 'operation module',
    operation_method VARCHAR(255) DEFAULT NULL COMMENT 'operation method',
    request_url VARCHAR(255) DEFAULT NULL COMMENT 'request url',
    request_method VARCHAR(16) DEFAULT NULL COMMENT 'request method',
    request_params LONGTEXT DEFAULT NULL COMMENT 'request params',
    response_result LONGTEXT DEFAULT NULL COMMENT 'response result',
    ip_address VARCHAR(64) DEFAULT NULL COMMENT 'ip address',
    location VARCHAR(128) DEFAULT NULL COMMENT 'location',
    operation_result TINYINT DEFAULT NULL COMMENT 'operation result',
    result_message VARCHAR(500) DEFAULT NULL COMMENT 'result message',
    operation_time DATETIME DEFAULT NULL COMMENT 'operation time',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_operation_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys audit log';

ALTER TABLE iot_command_record ADD COLUMN IF NOT EXISTS command_id VARCHAR(64) DEFAULT NULL COMMENT 'business command id';
ALTER TABLE iot_command_record ADD COLUMN IF NOT EXISTS device_code VARCHAR(64) DEFAULT NULL COMMENT 'device code';
ALTER TABLE iot_command_record ADD COLUMN IF NOT EXISTS product_key VARCHAR(64) DEFAULT NULL COMMENT 'product key';
ALTER TABLE iot_command_record ADD COLUMN IF NOT EXISTS topic VARCHAR(255) DEFAULT NULL COMMENT 'topic';
ALTER TABLE iot_command_record ADD COLUMN IF NOT EXISTS request_payload LONGTEXT DEFAULT NULL COMMENT 'request payload';
ALTER TABLE iot_command_record ADD COLUMN IF NOT EXISTS qos TINYINT NOT NULL DEFAULT 0 COMMENT 'mqtt qos';
ALTER TABLE iot_command_record ADD COLUMN IF NOT EXISTS retained TINYINT NOT NULL DEFAULT 0 COMMENT 'retained';
ALTER TABLE iot_command_record ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT NULL COMMENT 'status';
ALTER TABLE iot_command_record ADD COLUMN IF NOT EXISTS remark VARCHAR(500) DEFAULT NULL COMMENT 'remark';
ALTER TABLE iot_command_record ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT 'creator';
ALTER TABLE iot_command_record ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT 'updater';
ALTER TABLE iot_command_record ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';
ALTER TABLE iot_command_record MODIFY COLUMN message_id VARCHAR(64) NULL DEFAULT NULL COMMENT 'legacy message id';

ALTER TABLE iot_alarm_record ADD COLUMN IF NOT EXISTS metric_name VARCHAR(128) DEFAULT NULL COMMENT 'metric name';
ALTER TABLE iot_alarm_record ADD COLUMN IF NOT EXISTS threshold_value VARCHAR(255) DEFAULT NULL COMMENT 'threshold value';
ALTER TABLE iot_alarm_record ADD COLUMN IF NOT EXISTS remark VARCHAR(500) DEFAULT NULL COMMENT 'remark';
ALTER TABLE iot_alarm_record ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT 'creator';
ALTER TABLE iot_alarm_record ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT 'updater';
ALTER TABLE iot_alarm_record ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';

ALTER TABLE iot_event_record ADD COLUMN IF NOT EXISTS metric_name VARCHAR(128) DEFAULT NULL COMMENT 'metric name';
ALTER TABLE iot_event_record ADD COLUMN IF NOT EXISTS responsible_user BIGINT DEFAULT NULL COMMENT 'responsible user';
ALTER TABLE iot_event_record ADD COLUMN IF NOT EXISTS urgency_level VARCHAR(16) DEFAULT NULL COMMENT 'urgency';
ALTER TABLE iot_event_record ADD COLUMN IF NOT EXISTS arrival_time_limit INT DEFAULT NULL COMMENT 'arrival limit';
ALTER TABLE iot_event_record ADD COLUMN IF NOT EXISTS completion_time_limit INT DEFAULT NULL COMMENT 'completion limit';
ALTER TABLE iot_event_record ADD COLUMN IF NOT EXISTS remark VARCHAR(500) DEFAULT NULL COMMENT 'remark';
ALTER TABLE iot_event_record ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT 'creator';
ALTER TABLE iot_event_record ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT 'updater';
ALTER TABLE iot_event_record ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';

ALTER TABLE iot_event_work_order ADD COLUMN IF NOT EXISTS remark VARCHAR(500) DEFAULT NULL COMMENT 'remark';
ALTER TABLE iot_event_work_order ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT 'creator';
ALTER TABLE iot_event_work_order ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT 'updater';
ALTER TABLE iot_event_work_order ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';
ALTER TABLE iot_event_work_order MODIFY COLUMN work_order_type VARCHAR(32) NOT NULL DEFAULT 'event-dispatch' COMMENT 'work order type';

ALTER TABLE risk_point ADD COLUMN IF NOT EXISTS description VARCHAR(512) DEFAULT NULL COMMENT 'description';
ALTER TABLE risk_point ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT 'creator';
ALTER TABLE risk_point ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT 'updater';
ALTER TABLE risk_point ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';

ALTER TABLE risk_point_device ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id';
ALTER TABLE risk_point_device ADD COLUMN IF NOT EXISTS metric_name VARCHAR(64) DEFAULT NULL COMMENT 'metric name';
ALTER TABLE risk_point_device ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT 'creator';
ALTER TABLE risk_point_device ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT 'updater';
ALTER TABLE risk_point_device ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';
ALTER TABLE risk_point_device MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'pk';

ALTER TABLE rule_definition ADD COLUMN IF NOT EXISTS metric_name VARCHAR(64) DEFAULT NULL COMMENT 'metric name';
ALTER TABLE rule_definition ADD COLUMN IF NOT EXISTS expression VARCHAR(256) DEFAULT NULL COMMENT 'expression';
ALTER TABLE rule_definition ADD COLUMN IF NOT EXISTS notification_methods VARCHAR(64) DEFAULT NULL COMMENT 'notification methods';
ALTER TABLE rule_definition ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT 'creator';
ALTER TABLE rule_definition ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT 'updater';
ALTER TABLE rule_definition ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';
UPDATE rule_definition SET rule_code = NULL WHERE rule_code = '';
ALTER TABLE rule_definition MODIFY COLUMN rule_code VARCHAR(64) NULL DEFAULT NULL COMMENT 'legacy rule code';
ALTER TABLE rule_definition MODIFY COLUMN condition_expression VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'legacy condition expression';

ALTER TABLE linkage_rule ADD COLUMN IF NOT EXISTS description VARCHAR(512) DEFAULT NULL COMMENT 'description';
ALTER TABLE linkage_rule ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT 'creator';
ALTER TABLE linkage_rule ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT 'updater';
ALTER TABLE linkage_rule ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';
UPDATE linkage_rule SET rule_code = NULL WHERE rule_code = '';
ALTER TABLE linkage_rule MODIFY COLUMN rule_code VARCHAR(64) NULL DEFAULT NULL COMMENT 'legacy rule code';

ALTER TABLE emergency_plan ADD COLUMN IF NOT EXISTS description VARCHAR(512) DEFAULT NULL COMMENT 'description';
ALTER TABLE emergency_plan ADD COLUMN IF NOT EXISTS response_steps LONGTEXT DEFAULT NULL COMMENT 'response steps';
ALTER TABLE emergency_plan ADD COLUMN IF NOT EXISTS contact_list LONGTEXT DEFAULT NULL COMMENT 'contact list';
ALTER TABLE emergency_plan ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT 'creator';
ALTER TABLE emergency_plan ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT 'updater';
ALTER TABLE emergency_plan ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';
UPDATE emergency_plan SET plan_code = NULL WHERE plan_code = '';
ALTER TABLE emergency_plan MODIFY COLUMN plan_code VARCHAR(64) NULL DEFAULT NULL COMMENT 'legacy plan code';
ALTER TABLE emergency_plan MODIFY COLUMN applicable_scenario VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'legacy applicable scenario';
ALTER TABLE emergency_plan MODIFY COLUMN disposal_steps JSON NULL COMMENT 'legacy disposal steps';

ALTER TABLE sys_organization ADD COLUMN IF NOT EXISTS org_type VARCHAR(32) DEFAULT NULL COMMENT 'org type';
ALTER TABLE sys_organization ADD COLUMN IF NOT EXISTS leader_user_id BIGINT DEFAULT NULL COMMENT 'leader user id';
ALTER TABLE sys_organization ADD COLUMN IF NOT EXISTS leader_name VARCHAR(64) DEFAULT NULL COMMENT 'leader name';
ALTER TABLE sys_organization ADD COLUMN IF NOT EXISTS sort_no INT NOT NULL DEFAULT 0 COMMENT 'sort';
ALTER TABLE sys_organization ADD COLUMN IF NOT EXISTS remark VARCHAR(500) DEFAULT NULL COMMENT 'remark';
ALTER TABLE sys_organization ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT 'creator';
ALTER TABLE sys_organization ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT 'updater';
ALTER TABLE sys_organization ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';

ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS description VARCHAR(500) DEFAULT NULL COMMENT 'description';
ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT 'creator';
ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT 'updater';
ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';

ALTER TABLE sys_dict ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id';
ALTER TABLE sys_dict ADD COLUMN IF NOT EXISTS dict_type VARCHAR(32) DEFAULT NULL COMMENT 'dict type';
ALTER TABLE sys_dict ADD COLUMN IF NOT EXISTS status TINYINT NOT NULL DEFAULT 1 COMMENT 'status';
ALTER TABLE sys_dict ADD COLUMN IF NOT EXISTS sort_no INT NOT NULL DEFAULT 0 COMMENT 'sort';
ALTER TABLE sys_dict ADD COLUMN IF NOT EXISTS remark VARCHAR(500) DEFAULT NULL COMMENT 'remark';
ALTER TABLE sys_dict ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT 'creator';
ALTER TABLE sys_dict ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT 'updater';
ALTER TABLE sys_dict ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';
ALTER TABLE sys_dict ADD COLUMN IF NOT EXISTS dict_value VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'legacy dict value';
ALTER TABLE sys_dict ADD COLUMN IF NOT EXISTS dict_label VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'legacy dict label';
ALTER TABLE sys_dict MODIFY COLUMN dict_value VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'legacy dict value';
ALTER TABLE sys_dict MODIFY COLUMN dict_label VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'legacy dict label';
UPDATE sys_dict SET dict_value = '' WHERE dict_value IS NULL;
UPDATE sys_dict SET dict_label = '' WHERE dict_label IS NULL;

ALTER TABLE sys_dict_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id';
ALTER TABLE sys_dict_item ADD COLUMN IF NOT EXISTS item_type VARCHAR(32) DEFAULT NULL COMMENT 'item type';
ALTER TABLE sys_dict_item ADD COLUMN IF NOT EXISTS status TINYINT NOT NULL DEFAULT 1 COMMENT 'status';
ALTER TABLE sys_dict_item ADD COLUMN IF NOT EXISTS sort_no INT NOT NULL DEFAULT 0 COMMENT 'sort';
ALTER TABLE sys_dict_item ADD COLUMN IF NOT EXISTS remark VARCHAR(500) DEFAULT NULL COMMENT 'remark';
ALTER TABLE sys_dict_item ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT 'creator';
ALTER TABLE sys_dict_item ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT 'updater';
ALTER TABLE sys_dict_item ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';

ALTER TABLE sys_notification_channel ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id';
ALTER TABLE sys_notification_channel ADD COLUMN IF NOT EXISTS channel_type VARCHAR(32) DEFAULT NULL COMMENT 'channel type';
ALTER TABLE sys_notification_channel ADD COLUMN IF NOT EXISTS config LONGTEXT DEFAULT NULL COMMENT 'config';
ALTER TABLE sys_notification_channel ADD COLUMN IF NOT EXISTS status TINYINT NOT NULL DEFAULT 1 COMMENT 'status';
ALTER TABLE sys_notification_channel ADD COLUMN IF NOT EXISTS sort_no INT NOT NULL DEFAULT 0 COMMENT 'sort';
ALTER TABLE sys_notification_channel ADD COLUMN IF NOT EXISTS remark VARCHAR(500) DEFAULT NULL COMMENT 'remark';
ALTER TABLE sys_notification_channel ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT 'creator';
ALTER TABLE sys_notification_channel ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT 'updater';
ALTER TABLE sys_notification_channel ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';

ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS user_id BIGINT DEFAULT NULL COMMENT 'user id';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS user_name VARCHAR(64) DEFAULT NULL COMMENT 'user name';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS operation_type VARCHAR(64) DEFAULT NULL COMMENT 'operation type';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS operation_module VARCHAR(128) DEFAULT NULL COMMENT 'operation module';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS operation_method VARCHAR(255) DEFAULT NULL COMMENT 'operation method';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS request_url VARCHAR(255) DEFAULT NULL COMMENT 'request url';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS request_method VARCHAR(16) DEFAULT NULL COMMENT 'request method';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS request_params LONGTEXT DEFAULT NULL COMMENT 'request params';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS response_result LONGTEXT DEFAULT NULL COMMENT 'response result';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64) DEFAULT NULL COMMENT 'ip address';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS location VARCHAR(128) DEFAULT NULL COMMENT 'location';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS operation_result TINYINT DEFAULT NULL COMMENT 'operation result';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS result_message VARCHAR(500) DEFAULT NULL COMMENT 'result message';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS operation_time DATETIME DEFAULT NULL COMMENT 'operation time';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at';
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted';
ALTER TABLE sys_audit_log MODIFY COLUMN log_type VARCHAR(16) NOT NULL DEFAULT 'manual' COMMENT 'legacy required';
ALTER TABLE sys_audit_log MODIFY COLUMN operation_uri VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'legacy required';
ALTER TABLE sys_audit_log MODIFY COLUMN operation_method VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'method';
