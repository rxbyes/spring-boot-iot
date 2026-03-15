-- 联动规则与应急预案表结构

-- 联动规则表
CREATE TABLE IF NOT EXISTS `linkage_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `trigger_condition` TEXT DEFAULT NULL COMMENT '触发条件（JSON格式）',
  `action_list` TEXT DEFAULT NULL COMMENT '动作列表（JSON格式）',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-启用，1-停用',
  `tenant_id` BIGINT DEFAULT 0 COMMENT '租户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_rule_name` (`rule_name`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联动规则表';

-- 应急预案表
CREATE TABLE IF NOT EXISTS `emergency_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_name` VARCHAR(100) NOT NULL COMMENT '预案名称',
  `risk_level` VARCHAR(20) NOT NULL COMMENT '风险等级：critical-严重, warning-警告, info-提醒',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `response_steps` TEXT DEFAULT NULL COMMENT '响应步骤（JSON格式）',
  `contact_list` TEXT DEFAULT NULL COMMENT '联系人列表（JSON格式）',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-启用，1-停用',
  `tenant_id` BIGINT DEFAULT 0 COMMENT '租户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_plan_name` (`plan_name`),
  KEY `idx_risk_level` (`risk_level`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应急预案表';
