-- 字典配置表
CREATE TABLE `sys_dict` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户ID',
  `dict_name` varchar(100) NOT NULL COMMENT '字典名称',
  `dict_code` varchar(100) NOT NULL COMMENT '字典编码',
  `dict_type` varchar(20) DEFAULT 'text' COMMENT '字典类型 text/number/boolean/date',
  `status` tinyint DEFAULT '1' COMMENT '状态 1启用 0禁用',
  `sort_no` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标记 0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_code` (`dict_code`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典配置表';

-- 字典项表
CREATE TABLE `sys_dict_item` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户ID',
  `dict_id` bigint NOT NULL COMMENT '字典ID',
  `item_name` varchar(100) NOT NULL COMMENT '项名称',
  `item_value` varchar(200) NOT NULL COMMENT '项值',
  `item_type` varchar(20) DEFAULT 'string' COMMENT '项类型 string/number/boolean',
  `status` tinyint DEFAULT '1' COMMENT '状态 1启用 0禁用',
  `sort_no` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标记 0未删除 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_dict_id` (`dict_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项表';
