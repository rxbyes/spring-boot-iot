-- 阈值规则配置表
CREATE TABLE IF NOT EXISTS `rule_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `metric_identifier` VARCHAR(64) NOT NULL COMMENT '测点标识符',
  `metric_name` VARCHAR(64) NOT NULL COMMENT '测点名称',
  `expression` VARCHAR(256) NOT NULL COMMENT '表达式（如：value > 100）',
  `duration` INT NOT NULL DEFAULT 0 COMMENT '持续时间（秒）',
  `alarm_level` VARCHAR(20) NOT NULL COMMENT '告警等级：critical-严重, warning-警告, info-提醒',
  `notification_methods` VARCHAR(64) DEFAULT NULL COMMENT '通知方式：email,sms,wechat',
  `convert_to_event` TINYINT NOT NULL DEFAULT 0 COMMENT '是否转事件：0-否，1-是',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-启用，1-停用',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_metric_identifier` (`metric_identifier`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阈值规则配置表';

-- 联动规则表
CREATE TABLE IF NOT EXISTS `linkage_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `trigger_condition` VARCHAR(512) NOT NULL COMMENT '触发条件（JSON格式）',
  `action_list` VARCHAR(1024) NOT NULL COMMENT '动作列表（JSON格式）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-启用，1-停用',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联动规则表';

-- 应急预案表
CREATE TABLE IF NOT EXISTS `emergency_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_name` VARCHAR(128) NOT NULL COMMENT '预案名称',
  `risk_level` VARCHAR(20) NOT NULL COMMENT '风险等级：critical-严重, warning-警告, info-提醒',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `response_steps` TEXT DEFAULT NULL COMMENT '响应步骤（JSON格式）',
  `contact_list` VARCHAR(512) DEFAULT NULL COMMENT '联系人列表（JSON格式）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-启用，1-停用',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_risk_level` (`risk_level`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应急预案表';
