-- 风险点管理表
CREATE TABLE IF NOT EXISTS `risk_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `risk_point_code` VARCHAR(64) NOT NULL COMMENT '风险点编号',
  `risk_point_name` VARCHAR(128) NOT NULL COMMENT '风险点名称',
  `region_id` BIGINT NOT NULL COMMENT '区域ID',
  `region_name` VARCHAR(64) NOT NULL COMMENT '区域名称',
  `responsible_user` BIGINT NOT NULL COMMENT '负责人ID',
  `responsible_phone` VARCHAR(20) DEFAULT NULL COMMENT '负责人电话',
  `risk_level` VARCHAR(20) NOT NULL COMMENT '风险等级：critical-严重, warning-警告, info-提醒',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-启用，1-停用',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_risk_point_code` (`risk_point_code`),
  KEY `idx_region_id` (`region_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险点表';

-- 风险点与设备绑定表
CREATE TABLE IF NOT EXISTS `risk_point_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `risk_point_id` BIGINT NOT NULL COMMENT '风险点ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(64) NOT NULL COMMENT '设备编码',
  `device_name` VARCHAR(128) NOT NULL COMMENT '设备名称',
  `metric_identifier` VARCHAR(64) NOT NULL COMMENT '测点标识符',
  `metric_name` VARCHAR(64) NOT NULL COMMENT '测点名称',
  `default_threshold` VARCHAR(64) DEFAULT NULL COMMENT '默认阈值',
  `threshold_unit` VARCHAR(20) DEFAULT NULL COMMENT '阈值单位',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_risk_point_id` (`risk_point_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险点与设备绑定表';
