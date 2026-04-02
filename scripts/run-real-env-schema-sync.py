#!/usr/bin/env python3
"""Real-environment schema sync for Phase 4 smoke acceptance.

This script aligns known schema gaps in rm_iot without dropping tables.
"""

from __future__ import annotations

import argparse
import os
import sys
from typing import Dict, List, Tuple

import pymysql


CreateSqlMap = Dict[str, str]
ColumnSpecMap = Dict[str, List[Tuple[str, str]]]


CREATE_TABLE_SQL: CreateSqlMap = {
    "iot_device_online_session": """
CREATE TABLE IF NOT EXISTS iot_device_online_session (
    id BIGINT NOT NULL COMMENT 'pk',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    product_id BIGINT NOT NULL COMMENT 'product id',
    device_id BIGINT NOT NULL COMMENT 'device id',
    device_code VARCHAR(64) NOT NULL COMMENT 'device code',
    online_time DATETIME NOT NULL COMMENT 'online time',
    last_seen_time DATETIME DEFAULT NULL COMMENT 'last seen time',
    offline_time DATETIME DEFAULT NULL COMMENT 'offline time',
    duration_minutes BIGINT DEFAULT NULL COMMENT 'duration minutes',
    remark VARCHAR(500) DEFAULT NULL COMMENT 'remark',
    create_by BIGINT DEFAULT NULL COMMENT 'creator',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    update_by BIGINT DEFAULT NULL COMMENT 'updater',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_online_session_device_active (deleted, device_id, offline_time),
    KEY idx_online_session_product_time (deleted, product_id, online_time, offline_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='device online session'
""",
    "iot_device_metric_latest": """
CREATE TABLE IF NOT EXISTS iot_device_metric_latest (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'pk',
    tenant_id BIGINT NOT NULL COMMENT 'tenant id',
    device_id BIGINT NOT NULL COMMENT 'device id',
    product_id BIGINT NOT NULL COMMENT 'product id',
    metric_id VARCHAR(128) NOT NULL COMMENT 'metric id',
    metric_code VARCHAR(128) NOT NULL COMMENT 'metric code',
    metric_name VARCHAR(128) DEFAULT NULL COMMENT 'metric name',
    value_type VARCHAR(32) DEFAULT NULL COMMENT 'value type',
    value_double DOUBLE DEFAULT NULL COMMENT 'double value',
    value_long BIGINT DEFAULT NULL COMMENT 'long value',
    value_bool TINYINT(1) DEFAULT NULL COMMENT 'bool value',
    value_text TEXT DEFAULT NULL COMMENT 'text value',
    quality_code VARCHAR(32) DEFAULT NULL COMMENT 'quality code',
    alarm_flag TINYINT(1) DEFAULT NULL COMMENT 'alarm flag',
    reported_at DATETIME DEFAULT NULL COMMENT 'reported at',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'trace id',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tel_latest_tenant_device_metric (tenant_id, device_id, metric_id),
    KEY idx_tel_latest_device_reported (device_id, reported_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='telemetry v2 latest projection'
""",
    "iot_device_access_error_log": """
CREATE TABLE IF NOT EXISTS iot_device_access_error_log (
    id BIGINT NOT NULL COMMENT 'pk',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'trace id',
    protocol_code VARCHAR(64) DEFAULT NULL COMMENT 'protocol code',
    request_method VARCHAR(16) DEFAULT NULL COMMENT 'request method',
    failure_stage VARCHAR(32) DEFAULT NULL COMMENT 'failure stage',
    device_code VARCHAR(64) DEFAULT NULL COMMENT 'device code',
    product_key VARCHAR(64) DEFAULT NULL COMMENT 'product key',
    gateway_device_code VARCHAR(64) DEFAULT NULL COMMENT 'gateway device code',
    sub_device_code VARCHAR(64) DEFAULT NULL COMMENT 'sub device code',
    topic_route_type VARCHAR(32) DEFAULT NULL COMMENT 'topic route type',
    message_type VARCHAR(32) DEFAULT NULL COMMENT 'message type',
    topic VARCHAR(255) DEFAULT NULL COMMENT 'topic',
    client_id VARCHAR(128) DEFAULT NULL COMMENT 'client id',
    payload_size INT DEFAULT NULL COMMENT 'payload size',
    payload_encoding VARCHAR(16) DEFAULT NULL COMMENT 'payload encoding',
    payload_truncated TINYINT NOT NULL DEFAULT 0 COMMENT 'payload truncated',
    raw_payload LONGTEXT DEFAULT NULL COMMENT 'raw payload',
    error_code VARCHAR(64) DEFAULT NULL COMMENT 'error code',
    exception_class VARCHAR(255) DEFAULT NULL COMMENT 'exception class',
    error_message VARCHAR(500) DEFAULT NULL COMMENT 'error message',
    contract_snapshot LONGTEXT DEFAULT NULL COMMENT 'contract snapshot',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    KEY idx_access_error_trace (trace_id),
    KEY idx_access_error_device_time (device_code, create_time),
    KEY idx_access_error_stage_time (failure_stage, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='device access error archive'
""",
    "iot_device_invalid_report_state": """
CREATE TABLE IF NOT EXISTS iot_device_invalid_report_state (
    id BIGINT NOT NULL COMMENT 'pk',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',
    governance_key VARCHAR(255) NOT NULL COMMENT 'governance key',
    reason_code VARCHAR(64) NOT NULL COMMENT 'reason code',
    request_method VARCHAR(16) DEFAULT NULL COMMENT 'request method',
    failure_stage VARCHAR(32) DEFAULT NULL COMMENT 'failure stage',
    device_code VARCHAR(64) DEFAULT NULL COMMENT 'device code',
    product_key VARCHAR(64) DEFAULT NULL COMMENT 'product key',
    protocol_code VARCHAR(64) DEFAULT NULL COMMENT 'protocol code',
    topic_route_type VARCHAR(32) DEFAULT NULL COMMENT 'topic route type',
    topic VARCHAR(255) DEFAULT NULL COMMENT 'topic',
    client_id VARCHAR(128) DEFAULT NULL COMMENT 'client id',
    payload_size INT DEFAULT NULL COMMENT 'payload size',
    payload_encoding VARCHAR(16) DEFAULT NULL COMMENT 'payload encoding',
    last_payload LONGTEXT DEFAULT NULL COMMENT 'last payload',
    last_trace_id VARCHAR(64) DEFAULT NULL COMMENT 'last trace id',
    sample_error_message VARCHAR(500) DEFAULT NULL COMMENT 'sample error message',
    sample_exception_class VARCHAR(255) DEFAULT NULL COMMENT 'sample exception class',
    first_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'first seen time',
    last_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'last seen time',
    hit_count BIGINT NOT NULL DEFAULT 0 COMMENT 'hit count',
    sampled_count BIGINT NOT NULL DEFAULT 0 COMMENT 'sampled count',
    suppressed_count BIGINT NOT NULL DEFAULT 0 COMMENT 'suppressed count',
    suppressed_until DATETIME DEFAULT NULL COMMENT 'suppressed until',
    resolved TINYINT NOT NULL DEFAULT 0 COMMENT 'resolved',
    resolved_time DATETIME DEFAULT NULL COMMENT 'resolved time',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invalid_report_state_governance_key (governance_key),
    KEY idx_invalid_report_device_resolved (device_code, product_key, resolved, last_seen_time),
    KEY idx_invalid_report_reason_time (reason_code, last_seen_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='invalid mqtt report latest state'
""",
    "risk_point_device": """
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='risk point device'
""",
    "sys_notification_channel": """
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys notification channel'
""",
}

VIEW_SQL: CreateSqlMap = {
    "iot_message_log": """
CREATE OR REPLACE VIEW iot_message_log AS
SELECT
    id,
    tenant_id,
    device_id,
    product_id,
    trace_id,
    device_code,
    product_key,
    message_type,
    topic,
    payload,
    report_time,
    create_time
FROM iot_device_message_log
"""
}


COLUMNS_TO_ADD: ColumnSpecMap = {
    "iot_device_access_error_log": [
        ("contract_snapshot", "LONGTEXT DEFAULT NULL COMMENT 'contract snapshot'"),
    ],
    "iot_command_record": [
        ("device_code", "VARCHAR(64) DEFAULT NULL COMMENT 'device code'"),
        ("product_key", "VARCHAR(64) DEFAULT NULL COMMENT 'product key'"),
        ("topic", "VARCHAR(255) DEFAULT NULL COMMENT 'topic'"),
        ("request_payload", "LONGTEXT DEFAULT NULL COMMENT 'request payload'"),
        ("qos", "TINYINT NOT NULL DEFAULT 0 COMMENT 'mqtt qos'"),
        ("retained", "TINYINT NOT NULL DEFAULT 0 COMMENT 'retained'"),
        ("status", "VARCHAR(32) DEFAULT NULL COMMENT 'status'"),
        ("command_id", "VARCHAR(64) DEFAULT NULL COMMENT 'business command id'"),
        ("remark", "VARCHAR(500) DEFAULT NULL COMMENT 'remark'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
        ("deleted", "TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted'"),
    ],
    "iot_alarm_record": [
        ("remark", "VARCHAR(500) DEFAULT NULL COMMENT 'remark'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "iot_event_record": [
        ("remark", "VARCHAR(500) DEFAULT NULL COMMENT 'remark'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "iot_event_work_order": [
        ("remark", "VARCHAR(500) DEFAULT NULL COMMENT 'remark'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "risk_point": [
        ("org_id", "BIGINT DEFAULT NULL COMMENT 'organization id'"),
        ("org_name", "VARCHAR(128) DEFAULT NULL COMMENT 'organization name'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "rule_definition": [
        ("metric_name", "VARCHAR(64) DEFAULT NULL COMMENT 'metric name'"),
        ("expression", "VARCHAR(256) DEFAULT NULL COMMENT 'expression'"),
        ("notification_methods", "VARCHAR(64) DEFAULT NULL COMMENT 'notification methods'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "linkage_rule": [
        ("description", "VARCHAR(512) DEFAULT NULL COMMENT 'description'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "emergency_plan": [
        ("description", "VARCHAR(512) DEFAULT NULL COMMENT 'description'"),
        ("response_steps", "LONGTEXT DEFAULT NULL COMMENT 'response steps'"),
        ("contact_list", "LONGTEXT DEFAULT NULL COMMENT 'contact list'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "sys_organization": [
        ("org_type", "VARCHAR(32) DEFAULT NULL COMMENT 'org type'"),
        ("leader_user_id", "BIGINT DEFAULT NULL COMMENT 'leader user id'"),
        ("leader_name", "VARCHAR(64) DEFAULT NULL COMMENT 'leader name'"),
    ],
    "sys_role": [
        ("description", "VARCHAR(500) DEFAULT NULL COMMENT 'description'"),
    ],
    "sys_menu": [
        ("menu_code", "VARCHAR(100) DEFAULT NULL COMMENT 'menu code'"),
        ("path", "VARCHAR(255) DEFAULT NULL COMMENT 'route path'"),
        ("meta_json", "LONGTEXT DEFAULT NULL COMMENT 'meta json'"),
        ("sort", "INT DEFAULT 0 COMMENT 'sort'"),
        ("type", "TINYINT DEFAULT NULL COMMENT 'menu type'"),
        ("create_by", "BIGINT DEFAULT NULL COMMENT 'creator'"),
        ("update_by", "BIGINT DEFAULT NULL COMMENT 'updater'"),
    ],
    "sys_audit_log": [
        ("user_name", "VARCHAR(64) DEFAULT NULL COMMENT 'user name'"),
        ("trace_id", "VARCHAR(64) DEFAULT NULL COMMENT 'trace id'"),
        ("device_code", "VARCHAR(64) DEFAULT NULL COMMENT 'device code'"),
        ("product_key", "VARCHAR(64) DEFAULT NULL COMMENT 'product key'"),
        ("request_url", "VARCHAR(255) DEFAULT NULL COMMENT 'request url'"),
        ("location", "VARCHAR(128) DEFAULT NULL COMMENT 'location'"),
        ("operation_result", "TINYINT DEFAULT NULL COMMENT 'operation result'"),
        ("result_message", "VARCHAR(500) DEFAULT NULL COMMENT 'result message'"),
        ("error_code", "VARCHAR(64) DEFAULT NULL COMMENT 'error code'"),
        ("exception_class", "VARCHAR(255) DEFAULT NULL COMMENT 'exception class'"),
        ("operation_time", "DATETIME DEFAULT NULL COMMENT 'operation time'"),
        ("deleted", "TINYINT NOT NULL DEFAULT 0 COMMENT 'deleted'"),
    ],
    "iot_device_message_log": [
        ("trace_id", "VARCHAR(64) DEFAULT NULL COMMENT 'trace id'"),
        ("device_code", "VARCHAR(64) DEFAULT NULL COMMENT 'device code'"),
        ("product_key", "VARCHAR(64) DEFAULT NULL COMMENT 'product key'"),
    ],
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run rm_iot schema sync for real environment.")
    parser.add_argument("--host", default=os.getenv("IOT_MYSQL_HOST", "8.130.107.120"))
    parser.add_argument("--port", type=int, default=int(os.getenv("IOT_MYSQL_PORT", "3306")))
    parser.add_argument("--db", default=os.getenv("IOT_MYSQL_DB", "rm_iot"))
    parser.add_argument("--user", default=os.getenv("IOT_MYSQL_USERNAME", "root"))
    parser.add_argument("--password", default=os.getenv("IOT_MYSQL_PASSWORD", "mI8%pB1*gD"))
    return parser.parse_args()


def table_exists(cur: pymysql.cursors.Cursor, db: str, table: str) -> bool:
    cur.execute(
        """
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s
        LIMIT 1
        """,
        (db, table),
    )
    return cur.fetchone() is not None


def column_exists(cur: pymysql.cursors.Cursor, db: str, table: str, column: str) -> bool:
    cur.execute(
        """
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s AND COLUMN_NAME=%s
        LIMIT 1
        """,
        (db, table, column),
    )
    return cur.fetchone() is not None


def ensure_dict_defaults(cur: pymysql.cursors.Cursor) -> None:
    cur.execute(
        "ALTER TABLE sys_dict MODIFY COLUMN dict_value VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'legacy dict value'"
    )
    cur.execute(
        "ALTER TABLE sys_dict MODIFY COLUMN dict_label VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'legacy dict label'"
    )
    cur.execute("UPDATE sys_dict SET dict_value='' WHERE dict_value IS NULL")
    cur.execute("UPDATE sys_dict SET dict_label='' WHERE dict_label IS NULL")


def next_table_id(cur: pymysql.cursors.Cursor, table: str) -> int:
    cur.execute(f"SELECT COALESCE(MAX(id), 0) + 1 FROM `{table}`")
    return int(cur.fetchone()[0])


def ensure_risk_level_dict(cur: pymysql.cursors.Cursor, db: str) -> None:
    if not table_exists(cur, db, "sys_dict") or not table_exists(cur, db, "sys_dict_item"):
        return

    cur.execute(
        """
        SELECT id
        FROM sys_dict
        WHERE tenant_id = 1 AND dict_code = 'risk_level'
        ORDER BY deleted ASC, id ASC
        LIMIT 1
        """
    )
    existing_dict = cur.fetchone()
    dict_id = int(existing_dict[0]) if existing_dict else 7201
    dict_remark = "风险等级四色内码字典"

    if existing_dict:
        cur.execute(
            """
            UPDATE sys_dict
            SET dict_name=%s,
                dict_type='text',
                status=1,
                sort_no=1,
                remark=%s,
                update_by=1,
                update_time=NOW(),
                deleted=0
            WHERE id=%s
            """,
            ("风险等级", dict_remark, dict_id),
        )
    else:
        cur.execute(
            """
            INSERT INTO sys_dict (
                id, tenant_id, dict_name, dict_code, dict_type, status, sort_no, remark,
                create_by, create_time, update_by, update_time, deleted
            ) VALUES (%s, 1, %s, 'risk_level', 'text', 1, 1, %s, 1, NOW(), 1, NOW(), 0)
            """,
            (dict_id, "风险等级", dict_remark),
        )

    cur.execute(
        """
        SELECT id, item_value
        FROM sys_dict_item
        WHERE tenant_id = 1 AND dict_id = %s
        ORDER BY deleted ASC, sort_no ASC, id ASC
        """,
        (dict_id,),
    )
    existing_items = cur.fetchall()
    items_by_value: Dict[str, List[int]] = {}
    for item_id, item_value in existing_items:
        normalized_value = (item_value or "").strip().lower()
        items_by_value.setdefault(normalized_value, []).append(int(item_id))

    target_items = [
        ("red", "红色", 1, "风险等级-红色", "critical"),
        ("orange", "橙色", 2, "风险等级-橙色", "warning"),
        ("yellow", "黄色", 3, "风险等级-黄色", None),
        ("blue", "蓝色", 4, "风险等级-蓝色", "info"),
    ]
    retained_ids: List[int] = []

    for value, label, sort_no, remark, legacy_value in target_items:
        candidate_id = None
        for candidate_value in (value, legacy_value):
            if not candidate_value:
                continue
            candidate_ids = items_by_value.get(candidate_value, [])
            while candidate_ids:
                maybe_id = candidate_ids.pop(0)
                if maybe_id not in retained_ids:
                    candidate_id = maybe_id
                    break
            if candidate_id is not None:
                break

        if candidate_id is None:
            candidate_id = next_table_id(cur, "sys_dict_item")
            cur.execute(
                """
                INSERT INTO sys_dict_item (
                    id, tenant_id, dict_id, item_name, item_value, item_type, status, sort_no, remark,
                    create_by, create_time, update_by, update_time, deleted
                ) VALUES (%s, 1, %s, %s, %s, 'string', 1, %s, %s, 1, NOW(), 1, NOW(), 0)
                """,
                (candidate_id, dict_id, label, value, sort_no, remark),
            )
        else:
            cur.execute(
                """
                UPDATE sys_dict_item
                SET dict_id=%s,
                    item_name=%s,
                    item_value=%s,
                    item_type='string',
                    status=1,
                    sort_no=%s,
                    remark=%s,
                    update_by=1,
                    update_time=NOW(),
                    deleted=0
                WHERE id=%s
                """,
                (dict_id, label, value, sort_no, remark, candidate_id),
            )
        retained_ids.append(candidate_id)

    retained_placeholders = ", ".join(["%s"] * len(retained_ids))
    cur.execute(
        f"""
        UPDATE sys_dict_item
        SET status=0,
            deleted=1,
            update_by=1,
            update_time=NOW()
        WHERE tenant_id = 1
          AND dict_id = %s
          AND id NOT IN ({retained_placeholders})
        """,
        (dict_id, *retained_ids),
    )


def migrate_risk_level_values(cur: pymysql.cursors.Cursor, db: str) -> None:
    risk_level_tables = ("risk_point", "emergency_plan", "iot_event_record")
    for table in risk_level_tables:
        if not table_exists(cur, db, table) or not column_exists(cur, db, table, "risk_level"):
            continue
        cur.execute(
            f"""
            UPDATE `{table}`
            SET risk_level = CASE LOWER(TRIM(risk_level))
                WHEN 'critical' THEN 'red'
                WHEN 'warning' THEN 'orange'
                WHEN 'info' THEN 'blue'
                WHEN 'red' THEN 'red'
                WHEN 'orange' THEN 'orange'
                WHEN 'yellow' THEN 'yellow'
                WHEN 'blue' THEN 'blue'
                ELSE risk_level
            END
            WHERE risk_level IS NOT NULL
              AND TRIM(risk_level) <> ''
            """
        )


def ensure_audit_defaults(cur: pymysql.cursors.Cursor) -> None:
    cur.execute(
        "ALTER TABLE sys_audit_log MODIFY COLUMN log_type VARCHAR(16) NOT NULL DEFAULT 'manual'"
    )
    cur.execute(
        "ALTER TABLE sys_audit_log MODIFY COLUMN operation_uri VARCHAR(255) NOT NULL DEFAULT ''"
    )
    cur.execute(
        "ALTER TABLE sys_audit_log MODIFY COLUMN operation_method VARCHAR(255) NOT NULL DEFAULT ''"
    )


def ensure_legacy_phase4_defaults(cur: pymysql.cursors.Cursor, db: str) -> None:
    if table_exists(cur, db, "iot_command_record") and column_exists(cur, db, "iot_command_record", "message_id"):
        cur.execute("ALTER TABLE iot_command_record MODIFY COLUMN message_id VARCHAR(64) NULL DEFAULT NULL")

    if table_exists(cur, db, "iot_event_work_order") and column_exists(
        cur, db, "iot_event_work_order", "work_order_type"
    ):
        cur.execute(
            "ALTER TABLE iot_event_work_order MODIFY COLUMN work_order_type VARCHAR(32) NOT NULL DEFAULT 'event-dispatch'"
        )

    if table_exists(cur, db, "risk_point_device") and column_exists(cur, db, "risk_point_device", "id"):
        cur.execute("ALTER TABLE risk_point_device MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT")

    if table_exists(cur, db, "rule_definition") and column_exists(cur, db, "rule_definition", "rule_code"):
        cur.execute("ALTER TABLE rule_definition MODIFY COLUMN rule_code VARCHAR(64) NULL DEFAULT NULL")
    if table_exists(cur, db, "rule_definition") and column_exists(
        cur, db, "rule_definition", "condition_expression"
    ):
        cur.execute(
            "ALTER TABLE rule_definition MODIFY COLUMN condition_expression VARCHAR(255) NOT NULL DEFAULT ''"
        )

    if table_exists(cur, db, "linkage_rule") and column_exists(cur, db, "linkage_rule", "rule_code"):
        cur.execute("ALTER TABLE linkage_rule MODIFY COLUMN rule_code VARCHAR(64) NULL DEFAULT NULL")

    if table_exists(cur, db, "emergency_plan") and column_exists(cur, db, "emergency_plan", "plan_code"):
        cur.execute("ALTER TABLE emergency_plan MODIFY COLUMN plan_code VARCHAR(64) NULL DEFAULT NULL")
    if table_exists(cur, db, "emergency_plan") and column_exists(
        cur, db, "emergency_plan", "applicable_scenario"
    ):
        cur.execute(
            "ALTER TABLE emergency_plan MODIFY COLUMN applicable_scenario VARCHAR(255) NOT NULL DEFAULT ''"
        )
    if table_exists(cur, db, "emergency_plan") and column_exists(cur, db, "emergency_plan", "disposal_steps"):
        cur.execute("ALTER TABLE emergency_plan MODIFY COLUMN disposal_steps JSON NULL")


def ensure_menu_compat(cur: pymysql.cursors.Cursor, db: str) -> None:
    if not table_exists(cur, db, "sys_menu"):
        return

    # 兼容历史 sys_menu 字段命名，保证权限菜单查询可落在统一实体字段上。
    if column_exists(cur, db, "sys_menu", "menu_code") and column_exists(cur, db, "sys_menu", "permission"):
        cur.execute(
            "UPDATE sys_menu SET menu_code = permission WHERE (menu_code IS NULL OR menu_code = '') AND permission IS NOT NULL AND permission <> ''"
        )
    if column_exists(cur, db, "sys_menu", "menu_code"):
        cur.execute("UPDATE sys_menu SET menu_code = CONCAT('menu-', id) WHERE menu_code IS NULL OR menu_code = ''")

    if column_exists(cur, db, "sys_menu", "path") and column_exists(cur, db, "sys_menu", "route_path"):
        cur.execute("UPDATE sys_menu SET path = route_path WHERE (path IS NULL OR path = '') AND route_path IS NOT NULL")

    if column_exists(cur, db, "sys_menu", "sort") and column_exists(cur, db, "sys_menu", "sort_no"):
        cur.execute("UPDATE sys_menu SET sort = sort_no WHERE sort IS NULL")

    if column_exists(cur, db, "sys_menu", "type") and column_exists(cur, db, "sys_menu", "menu_type"):
        cur.execute("UPDATE sys_menu SET type = menu_type WHERE type IS NULL")


def main() -> int:
    args = parse_args()

    conn = pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        charset="utf8mb4",
        autocommit=True,
        connect_timeout=20,
        read_timeout=60,
        write_timeout=60,
    )

    try:
        with conn.cursor() as cur:
            cur.execute(f"USE `{args.db}`")

            for table, ddl in CREATE_TABLE_SQL.items():
                cur.execute(ddl)
                print(f"[table] ensured {table}")

            for table, specs in COLUMNS_TO_ADD.items():
                if not table_exists(cur, args.db, table):
                    print(f"[skip] table missing: {table}")
                    continue
                for column, definition in specs:
                    if column_exists(cur, args.db, table, column):
                        continue
                    cur.execute(f"ALTER TABLE `{table}` ADD COLUMN `{column}` {definition}")
                    print(f"[column] {table}.{column} added")

            if table_exists(cur, args.db, "sys_dict"):
                if column_exists(cur, args.db, "sys_dict", "dict_value") and column_exists(
                    cur, args.db, "sys_dict", "dict_label"
                ):
                    ensure_dict_defaults(cur)
                    print("[dict] sys_dict default constraints aligned")
                ensure_risk_level_dict(cur, args.db)
                print("[dict] risk_level four-color items aligned")

            if table_exists(cur, args.db, "sys_audit_log"):
                ensure_audit_defaults(cur)
                print("[audit] sys_audit_log legacy constraints aligned")

            ensure_legacy_phase4_defaults(cur, args.db)
            print("[phase4] legacy strict columns aligned")

            migrate_risk_level_values(cur, args.db)
            print("[phase4] risk level values migrated to four-color codes")

            ensure_menu_compat(cur, args.db)
            print("[menu] sys_menu legacy columns aligned")

            for view_name, ddl in VIEW_SQL.items():
                cur.execute(ddl)
                print(f"[view] ensured {view_name}")

        print("Schema sync completed.")
        return 0
    finally:
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
