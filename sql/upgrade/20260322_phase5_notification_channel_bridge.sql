USE rm_iot;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_in_app_message_bridge_log (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    message_id BIGINT NOT NULL COMMENT '消息ID',
    channel_code VARCHAR(64) NOT NULL COMMENT '渠道编码',
    bridge_scene VARCHAR(64) NOT NULL COMMENT '桥接场景',
    unread_count INT NOT NULL DEFAULT 0 COMMENT '最近一次桥接时的未读人数',
    recipient_snapshot VARCHAR(500) DEFAULT NULL COMMENT '未读对象摘要',
    bridge_status TINYINT NOT NULL DEFAULT 0 COMMENT '桥接状态 0失败/待重试 1成功',
    response_status_code INT DEFAULT NULL COMMENT '最近一次响应状态码',
    response_body VARCHAR(1000) DEFAULT NULL COMMENT '最近一次响应摘要',
    last_attempt_time DATETIME DEFAULT NULL COMMENT '最近一次尝试时间',
    success_time DATETIME DEFAULT NULL COMMENT '成功时间',
    attempt_count INT NOT NULL DEFAULT 0 COMMENT '尝试次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_in_app_message_bridge_message_channel (tenant_id, message_id, channel_code, bridge_scene),
    KEY idx_in_app_message_bridge_status_time (bridge_status, last_attempt_time),
    KEY idx_in_app_message_bridge_message (message_id, channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息未读桥接日志表';
