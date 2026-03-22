USE rm_iot;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_in_app_message_bridge_attempt_log (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    bridge_log_id BIGINT NOT NULL COMMENT '桥接日志ID',
    message_id BIGINT NOT NULL COMMENT '消息ID',
    channel_code VARCHAR(64) NOT NULL COMMENT '渠道编码',
    bridge_scene VARCHAR(64) NOT NULL COMMENT '桥接场景',
    attempt_no INT NOT NULL COMMENT '尝试序号',
    bridge_status TINYINT NOT NULL DEFAULT 0 COMMENT '桥接状态 0失败 1成功',
    unread_count INT NOT NULL DEFAULT 0 COMMENT '本次桥接时的未读人数',
    recipient_snapshot VARCHAR(500) DEFAULT NULL COMMENT '本次桥接时的未读对象摘要',
    response_status_code INT DEFAULT NULL COMMENT '本次响应状态码',
    response_body VARCHAR(1000) DEFAULT NULL COMMENT '本次响应摘要',
    attempt_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '尝试时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_in_app_message_bridge_attempt (bridge_log_id, attempt_no),
    KEY idx_in_app_message_bridge_attempt_log_time (bridge_log_id, attempt_time DESC),
    KEY idx_in_app_message_bridge_attempt_message (message_id, channel_code, attempt_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息桥接尝试明细表';
