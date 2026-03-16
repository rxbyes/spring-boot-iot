USE rm_iot;

-- Align shared Phase 4 dev databases that were partially upgraded before
-- risk monitoring was split into its own task script.

ALTER TABLE risk_point
    ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT '创建人';

ALTER TABLE risk_point
    ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT '更新人';

CREATE TABLE IF NOT EXISTS risk_point_device (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    risk_point_id BIGINT NOT NULL COMMENT '风险点ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    device_code VARCHAR(64) NOT NULL COMMENT '设备编码',
    device_name VARCHAR(128) NOT NULL COMMENT '设备名称',
    metric_identifier VARCHAR(64) NOT NULL COMMENT '测点标识符',
    metric_name VARCHAR(64) NOT NULL COMMENT '测点名称',
    default_threshold VARCHAR(64) DEFAULT NULL COMMENT '默认阈值',
    threshold_unit VARCHAR(20) DEFAULT NULL COMMENT '阈值单位',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_risk_point_id (risk_point_id),
    KEY idx_device_id (device_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险点与设备绑定表';

ALTER TABLE risk_point_device
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';

ALTER TABLE risk_point_device
    ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT NULL COMMENT '创建人';

ALTER TABLE risk_point_device
    ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT NULL COMMENT '更新人';

ALTER TABLE risk_point_device
    ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除';
