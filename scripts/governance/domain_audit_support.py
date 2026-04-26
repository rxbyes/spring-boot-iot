#!/usr/bin/env python3
"""Shared helpers for governance registry real-environment audits and exports."""

from __future__ import annotations

import csv
import json
import os
import re
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, Sequence
from urllib.parse import urlparse

import pymysql

from scripts.schema.load_registry import RegistryObject


REPO_ROOT = Path(__file__).resolve().parents[2]
APP_DEV_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application-dev.yml"
OBSERVABILITY_LOG_POLICY_PATH = REPO_ROOT / "config" / "automation" / "observability-log-governance-policy.json"
RELATED_RISK_POINT_TABLE = "risk_point"
RISK_POINT_DEVICE_TABLE = "risk_point_device"
RISK_POINT_CAPABILITY_TABLE = "risk_point_device_capability_binding"


def extract_default(text: str, env_name: str) -> str:
    match = re.search(rf"{env_name}:([^}}]+)}}", text)
    if not match:
        raise RuntimeError(f"Unable to resolve {env_name} from {APP_DEV_PATH}")
    return match.group(1).strip()


def load_dev_defaults() -> Dict[str, str]:
    text = APP_DEV_PATH.read_text(encoding="utf-8")
    return {
        "jdbc_url": extract_default(text, "IOT_MYSQL_URL"),
        "user": extract_default(text, "IOT_MYSQL_USERNAME"),
        "password": extract_default(text, "IOT_MYSQL_PASSWORD"),
    }


def resolve_connection_args(args) -> Dict[str, object]:
    defaults = load_dev_defaults()
    jdbc_url = getattr(args, "jdbc_url", None) or os.getenv("IOT_MYSQL_URL") or defaults["jdbc_url"]
    user = getattr(args, "user", None) or os.getenv("IOT_MYSQL_USERNAME") or defaults["user"]
    password = getattr(args, "password", None) or os.getenv("IOT_MYSQL_PASSWORD") or defaults["password"]

    parsed = urlparse(jdbc_url.replace("jdbc:mysql://", "mysql://", 1))
    database = parsed.path.lstrip("/")
    if not parsed.hostname or not database:
        raise RuntimeError(f"Invalid jdbc url: {jdbc_url}")
    return {
        "host": parsed.hostname,
        "port": parsed.port or 3306,
        "database": database,
        "user": user,
        "password": password,
    }


def normalize_row(row: Dict[str, object] | None) -> Dict[str, object] | None:
    if row is None:
        return None
    return {str(key).lower(): value for key, value in row.items()}


def query_all(cursor: pymysql.cursors.Cursor, sql: str, params: Sequence[object] | None = None) -> list[dict[str, object]]:
    cursor.execute(sql, list(params or ()))
    return [normalize_row(row) for row in cursor.fetchall()]


def query_one(cursor: pymysql.cursors.Cursor, sql: str, params: Sequence[object] | None = None) -> dict[str, object]:
    cursor.execute(sql, list(params or ()))
    row = normalize_row(cursor.fetchone())
    if row is None:
        raise RuntimeError(f"Query returned no rows: {sql}")
    return row


def table_exists(cursor: pymysql.cursors.Cursor, database: str, table_name: str) -> bool:
    row = query_one(
        cursor,
        """
        SELECT COUNT(*) AS c
        FROM information_schema.tables
        WHERE table_schema = %s AND table_name = %s
        """,
        (database, table_name),
    )
    return int(row["c"]) > 0


def get_table_comment(cursor: pymysql.cursors.Cursor, database: str, table_name: str) -> str:
    row = query_one(
        cursor,
        """
        SELECT table_comment
        FROM information_schema.tables
        WHERE table_schema = %s AND table_name = %s
        """,
        (database, table_name),
    )
    return str(row.get("table_comment") or "")


def get_table_type(cursor: pymysql.cursors.Cursor, database: str, table_name: str) -> str:
    row = query_one(
        cursor,
        """
        SELECT table_type
        FROM information_schema.tables
        WHERE table_schema = %s AND table_name = %s
        """,
        (database, table_name),
    )
    return str(row.get("table_type") or "")


def get_columns(cursor: pymysql.cursors.Cursor, database: str, table_name: str) -> list[dict[str, object]]:
    return query_all(
        cursor,
        """
        SELECT column_name, column_type, is_nullable, column_default, extra, column_comment, ordinal_position
        FROM information_schema.columns
        WHERE table_schema = %s AND table_name = %s
        ORDER BY ordinal_position
        """,
        (database, table_name),
    )


def get_column_names(columns: Sequence[dict[str, object]]) -> list[str]:
    return [str(column["column_name"]) for column in columns]


def get_indexes(cursor: pymysql.cursors.Cursor, database: str, table_name: str) -> dict[str, dict[str, object]]:
    rows = query_all(
        cursor,
        """
        SELECT index_name, non_unique, seq_in_index, column_name
        FROM information_schema.statistics
        WHERE table_schema = %s AND table_name = %s
        ORDER BY index_name, seq_in_index
        """,
        (database, table_name),
    )
    indexes: dict[str, dict[str, object]] = {}
    for row in rows:
        name = str(row["index_name"])
        item = indexes.setdefault(name, {"non_unique": int(row["non_unique"]), "columns": []})
        item["columns"].append(row["column_name"])
    return indexes


def resolve_create_table_sql(show_create_row: dict[str, object]) -> str:
    for key, value in show_create_row.items():
        if key.startswith("create table") or key.startswith("create view"):
            return str(value)
    raise RuntimeError("Unable to resolve SHOW CREATE TABLE output.")


def pick_order_column(column_names: Sequence[str]) -> str:
    for name in ("update_time", "create_time", "id"):
        if name in column_names:
            return name
    return column_names[0]


def build_time_range(cursor: pymysql.cursors.Cursor, table_name: str, column_names: Sequence[str]) -> dict[str, object] | None:
    expressions: list[str] = []
    if "create_time" in column_names:
        expressions.extend(["MIN(create_time) AS min_create_time", "MAX(create_time) AS max_create_time"])
    if "update_time" in column_names:
        expressions.extend(["MIN(update_time) AS min_update_time", "MAX(update_time) AS max_update_time"])
    if not expressions:
        return None
    return query_one(cursor, f"SELECT {', '.join(expressions)} FROM `{table_name}`")


def fetch_all_table_rows(cursor: pymysql.cursors.Cursor, table_name: str, column_names: Sequence[str]) -> list[dict[str, object]]:
    return query_all(cursor, f"SELECT * FROM `{table_name}` ORDER BY `{pick_order_column(column_names)}` ASC")


def load_observability_log_policy() -> dict[str, object]:
    return json.loads(OBSERVABILITY_LOG_POLICY_PATH.read_text(encoding="utf-8"))


def resolve_message_log_retention_days() -> int:
    policy = load_observability_log_policy()
    return int(
        ((policy.get("tables") or {}).get("iot_message_log") or {}).get("retentionDays")
        or 30
    )


def collect_mysql_table_audit(
    cursor: pymysql.cursors.Cursor,
    database: str,
    table_name: str,
    sample_limit: int,
    include_export_rows: bool = True,
) -> dict[str, object]:
    audit: dict[str, object] = {
        "table_name": table_name,
        "table_exists": table_exists(cursor, database, table_name),
    }
    if not audit["table_exists"]:
        return audit

    columns = get_columns(cursor, database, table_name)
    column_names = get_column_names(columns)
    show_create_row = query_one(cursor, f"SHOW CREATE TABLE `{table_name}`")

    audit["columns"] = columns
    audit["table_type"] = get_table_type(cursor, database, table_name)
    audit["table_comment"] = get_table_comment(cursor, database, table_name)
    audit["indexes"] = get_indexes(cursor, database, table_name)
    audit["create_table_sql"] = resolve_create_table_sql(show_create_row)
    audit["row_count_total"] = int(query_one(cursor, f"SELECT COUNT(*) AS c FROM `{table_name}`")["c"])
    if "deleted" in column_names:
        audit["row_count_deleted_0"] = int(query_one(cursor, f"SELECT COUNT(*) AS c FROM `{table_name}` WHERE deleted = 0")["c"])
        audit["row_count_deleted_1"] = int(query_one(cursor, f"SELECT COUNT(*) AS c FROM `{table_name}` WHERE deleted = 1")["c"])
    if "tenant_id" in column_names:
        audit["tenant_distribution"] = query_all(
            cursor,
            f"SELECT tenant_id, COUNT(*) AS cnt FROM `{table_name}` GROUP BY tenant_id ORDER BY cnt DESC, tenant_id ASC",
        )
    time_range = build_time_range(cursor, table_name, column_names)
    if time_range is not None:
        audit["time_range"] = time_range
    audit["sample_rows"] = query_all(
        cursor,
        f"SELECT * FROM `{table_name}` ORDER BY `{pick_order_column(column_names)}` DESC LIMIT %s",
        (max(sample_limit, 1),),
    )
    audit["export_columns"] = column_names
    if include_export_rows:
        audit["export_rows"] = fetch_all_table_rows(cursor, table_name, column_names)
    else:
        audit["export_rows"] = []
        audit["export_rows_omitted"] = True
    return audit


def enrich_highway_archive_audit(cursor: pymysql.cursors.Cursor, audit: dict[str, object], sample_limit: int) -> None:
    column_names = list(audit.get("export_columns", []))
    if "risk_point_id" not in column_names:
        return

    table_name = str(audit["table_name"])
    audit["risk_point_id_distinct_count"] = int(
        query_one(cursor, f"SELECT COUNT(DISTINCT risk_point_id) AS c FROM `{table_name}`")["c"]
    )
    audit["risk_point_join"] = {
        "missing_parent_rows": int(
            query_one(
                cursor,
                f"""
                SELECT COUNT(*) AS c
                FROM `{table_name}` d
                LEFT JOIN `{RELATED_RISK_POINT_TABLE}` r ON r.id = d.risk_point_id
                WHERE r.id IS NULL
                """,
            )["c"]
        ),
        "parent_deleted_distribution": query_all(
            cursor,
            f"""
            SELECT r.deleted, COUNT(*) AS cnt
            FROM `{table_name}` d
            JOIN `{RELATED_RISK_POINT_TABLE}` r ON r.id = d.risk_point_id
            GROUP BY r.deleted
            ORDER BY r.deleted
            """,
        ),
    }
    audit["risk_point_device_active_binding_count"] = int(
        query_one(
            cursor,
            f"""
            SELECT COUNT(*) AS c
            FROM `{RISK_POINT_DEVICE_TABLE}` rpd
            JOIN `{table_name}` d ON d.risk_point_id = rpd.risk_point_id
            WHERE rpd.deleted = 0
            """,
        )["c"]
    )
    audit["capability_binding_count"] = int(
        query_one(
            cursor,
            f"""
            SELECT COUNT(*) AS c
            FROM `{RISK_POINT_CAPABILITY_TABLE}` rpcb
            JOIN `{table_name}` d ON d.risk_point_id = rpcb.risk_point_id
            WHERE rpcb.deleted = 0
            """,
        )["c"]
    )
    audit["capability_binding_samples"] = query_all(
        cursor,
        f"""
        SELECT rpcb.id,
               rpcb.risk_point_id,
               rpcb.device_id,
               rpcb.device_code,
               rpcb.device_name,
               rpcb.device_capability_type,
               rpcb.extension_status,
               rpcb.tenant_id,
               rpcb.create_time,
               rpcb.update_time,
               rp.risk_point_code,
               rp.risk_point_name,
               rp.risk_type,
               rp.status AS risk_point_status
        FROM `{RISK_POINT_CAPABILITY_TABLE}` rpcb
        JOIN `{table_name}` d ON d.risk_point_id = rpcb.risk_point_id
        JOIN `{RELATED_RISK_POINT_TABLE}` rp ON rp.id = rpcb.risk_point_id
        WHERE rpcb.deleted = 0
        ORDER BY rpcb.update_time DESC, rpcb.id DESC
        LIMIT %s
        """,
        (max(sample_limit, 1),),
    )


def audit_mysql_archived_object(connection_args: dict[str, object], object_name: str, sample_limit: int) -> dict[str, object]:
    with pymysql.connect(
        host=str(connection_args["host"]),
        port=int(connection_args["port"]),
        user=str(connection_args["user"]),
        password=str(connection_args["password"]),
        database=str(connection_args["database"]),
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        connect_timeout=10,
        read_timeout=30,
        write_timeout=30,
        autocommit=True,
    ) as connection:
        with connection.cursor() as cursor:
            audit = collect_mysql_table_audit(cursor, str(connection_args["database"]), object_name, sample_limit)
            if object_name == "risk_point_highway_detail" and audit.get("table_exists"):
                enrich_highway_archive_audit(cursor, audit, sample_limit)
            return audit


def audit_mysql_hot_table_with_cold_archive(
    connection_args: dict[str, object],
    object_name: str,
    sample_limit: int,
    archive_table_name: str | None = None,
    batch_table_name: str | None = None,
) -> dict[str, object]:
    archive_table_name = archive_table_name or f"{object_name}_archive"
    batch_table_name = batch_table_name or f"{object_name}_archive_batch"
    retention_days = resolve_message_log_retention_days()
    cutoff_at = datetime.now() - timedelta(days=max(retention_days, 0))
    with pymysql.connect(
        host=str(connection_args["host"]),
        port=int(connection_args["port"]),
        user=str(connection_args["user"]),
        password=str(connection_args["password"]),
        database=str(connection_args["database"]),
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        connect_timeout=10,
        read_timeout=30,
        write_timeout=30,
        autocommit=True,
    ) as connection:
        with connection.cursor() as cursor:
            hot_audit = collect_mysql_table_audit(
                cursor,
                str(connection_args["database"]),
                object_name,
                sample_limit,
                include_export_rows=False,
            )
            hot_exists = bool(hot_audit.get("table_exists"))
            archive_exists = table_exists(cursor, str(connection_args["database"]), archive_table_name)
            batch_exists = table_exists(cursor, str(connection_args["database"]), batch_table_name)

            expired_rows = 0
            if hot_exists:
                expired_rows = int(
                    query_one(
                        cursor,
                        f"SELECT COUNT(*) AS c FROM `{object_name}` WHERE report_time < %s",
                        (cutoff_at,),
                    )["c"]
                )

            latest_batch = None
            recent_batches: list[dict[str, object]] = []
            if batch_exists:
                recent_batches = query_all(
                    cursor,
                    f"""
                    SELECT id, batch_no, governance_mode, status, retention_days, cutoff_at,
                           confirmed_expired_rows, candidate_rows, archived_rows, deleted_rows,
                           failed_reason, create_time, update_time
                    FROM `{batch_table_name}`
                    ORDER BY create_time DESC, id DESC
                    LIMIT %s
                    """,
                    (max(sample_limit, 1),),
                )
                latest_batch = recent_batches[0] if recent_batches else None

            hot_audit["hot_table_exists"] = hot_exists
            hot_audit["archive_table_name"] = archive_table_name
            hot_audit["archive_table_exists"] = archive_exists
            hot_audit["batch_table_name"] = batch_table_name
            hot_audit["batch_table_exists"] = batch_exists
            hot_audit["archive_row_count_total"] = (
                int(query_one(cursor, f"SELECT COUNT(*) AS c FROM `{archive_table_name}`")["c"])
                if archive_exists
                else 0
            )
            hot_audit["retention_days"] = retention_days
            hot_audit["cutoff_at"] = cutoff_at.strftime("%Y-%m-%d %H:%M:%S")
            hot_audit["expired_rows"] = expired_rows
            hot_audit["latest_batch"] = latest_batch
            hot_audit["recent_batches"] = recent_batches
            return hot_audit


def build_schema_comment_drift(audit: dict[str, object], expected_schema_object: RegistryObject) -> dict[str, object]:
    actual_table_comment = str(audit.get("table_comment") or "")
    expected_table_comment = expected_schema_object.table_comment_zh
    table_comment_drift = None
    if actual_table_comment != expected_table_comment:
        table_comment_drift = {
            "expected": expected_table_comment,
            "actual": actual_table_comment,
        }

    actual_columns = {
        str(column.get("column_name") or ""): str(column.get("column_comment") or "")
        for column in audit.get("columns", [])
    }
    field_comment_drifts: list[dict[str, object]] = []
    missing_columns: list[str] = []
    for field in expected_schema_object.fields:
        actual_comment = actual_columns.get(field.name)
        if actual_comment is None:
            missing_columns.append(field.name)
            continue
        if actual_comment != field.comment_zh:
            field_comment_drifts.append(
                {
                    "column_name": field.name,
                    "expected": field.comment_zh,
                    "actual": actual_comment,
                }
            )

    return {
        "has_drift": bool(table_comment_drift or field_comment_drifts or missing_columns),
        "table_comment_drift": table_comment_drift,
        "field_comment_drifts": field_comment_drifts,
        "missing_columns": missing_columns,
    }


def attach_schema_comment_drift(audit: dict[str, object], expected_schema_object: RegistryObject | None) -> dict[str, object]:
    if expected_schema_object is None or not audit.get("table_exists"):
        return audit
    audit["schema_comment_drift"] = build_schema_comment_drift(audit, expected_schema_object)
    return audit


def evaluate_deletion_readiness(audit: dict[str, object]) -> dict[str, object]:
    blocking_reasons: list[str] = []
    checklist = [
        "请先导出并备份 archived 明细数据",
        "确认 sql/init-data.sql 中的相关 seed 已完成迁移或移除",
        "确认 schema-governance/、docs/04、docs/08、README.md、AGENTS.md 已同步更新",
        "确认对象扩展字段已无人工核查需求或已迁入新的真相源",
    ]
    if not audit.get("table_exists"):
        blocking_reasons.append("ARCHIVED_TABLE_NOT_FOUND")
    if int(audit.get("row_count_total", 0) or 0) > 0:
        blocking_reasons.append("ARCHIVE_ROWS_STILL_PRESENT")
    if int(audit.get("capability_binding_count", 0) or 0) > 0:
        blocking_reasons.append("CAPABILITY_BINDINGS_STILL_PRESENT")
    if int(audit.get("risk_point_device_active_binding_count", 0) or 0) > 0:
        blocking_reasons.append("FORMAL_DEVICE_BINDINGS_STILL_PRESENT")
    risk_point_join = audit.get("risk_point_join") or {}
    if int(risk_point_join.get("missing_parent_rows", 0) or 0) > 0:
        blocking_reasons.append("MISSING_PARENT_RISK_POINTS")

    return {
        "ready": len(blocking_reasons) == 0,
        "blocking_reasons": blocking_reasons,
        "checklist": checklist,
    }


def evaluate_hot_table_archive_health(audit: dict[str, object]) -> dict[str, object]:
    blocking_reasons: list[str] = []
    if not audit.get("hot_table_exists"):
        blocking_reasons.append("HOT_TABLE_MISSING")
    table_type = str(audit.get("table_type") or "").upper()
    if table_type and table_type != "BASE TABLE":
        blocking_reasons.append("HOT_OBJECT_NOT_BASE_TABLE")
    if not audit.get("archive_table_exists"):
        blocking_reasons.append("ARCHIVE_TABLE_MISSING")
    if not audit.get("batch_table_exists"):
        blocking_reasons.append("ARCHIVE_BATCH_TABLE_MISSING")

    latest_batch = audit.get("latest_batch")
    if int(audit.get("expired_rows") or 0) > 0 and not latest_batch:
        blocking_reasons.append("NO_ARCHIVE_BATCH_EVIDENCE")
    if latest_batch and str(latest_batch.get("status") or "").upper() not in {"SUCCEEDED", "RUNNING"}:
        blocking_reasons.append("LATEST_BATCH_NOT_HEALTHY")
    if latest_batch and int(latest_batch.get("deleted_rows") or 0) > int(latest_batch.get("archived_rows") or 0):
        blocking_reasons.append("ARCHIVE_DELETE_COUNT_MISMATCH")

    return {
        "ready": len(blocking_reasons) == 0,
        "blocking_reasons": blocking_reasons,
        "checklist": [
            "确认冷归档表与批次表都已完成 schema sync",
            "确认最近一次 apply 批次状态正常且归档/删除统计一致",
            "确认 docs/04、docs/08、docs/11、README.md、AGENTS.md 与 schema-governance 已同步更新",
        ],
    }


def sql_literal(value: object) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, datetime):
        return f"'{value.strftime('%Y-%m-%d %H:%M:%S')}'"
    text = str(value).replace("\\", "\\\\").replace("'", "''")
    return f"'{text}'"


def render_backup_sql(table_name: str, columns: Sequence[str], rows: Sequence[dict[str, object]]) -> str:
    quoted_columns = ", ".join(f"`{column}`" for column in columns)
    if not rows:
        return f"-- No rows exported from `{table_name}`.\n"

    values_sql = [
        "(" + ", ".join(sql_literal(row.get(column)) for column in columns) + ")"
        for row in rows
    ]
    return "\n".join(
        [
            f"-- Backup export for `{table_name}` generated at {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
            f"INSERT INTO `{table_name}` ({quoted_columns}) VALUES",
            "  " + ",\n  ".join(values_sql) + ";",
            "",
        ]
    )


def export_rows_to_csv(path: Path, rows: Sequence[dict[str, object]], fieldnames: Sequence[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(fieldnames))
        writer.writeheader()
        for row in rows:
            writer.writerow({fieldname: row.get(fieldname) for fieldname in fieldnames})


def write_json(path: Path, payload: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2, default=str) + "\n", encoding="utf-8")
