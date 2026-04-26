#!/usr/bin/env python3
"""Govern observability logs from the real environment."""

from __future__ import annotations

import argparse
import json
import os
import re
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any

import pymysql


REPO_ROOT = Path(__file__).resolve().parent.parent
APP_DEV_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application-dev.yml"
DEFAULT_POLICY_PATH = REPO_ROOT / "config" / "automation" / "observability-log-governance-policy.json"


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Govern observability logs via the real environment.")
    parser.add_argument("--jdbc-url", help="Override JDBC url. Defaults to application-dev.yml / env.")
    parser.add_argument("--user", help="Override mysql username. Defaults to application-dev.yml / env.")
    parser.add_argument("--password", help="Override mysql password. Defaults to application-dev.yml / env.")
    parser.add_argument("--policy-path", help="Optional policy JSON path.")
    parser.add_argument("--json-out", help="Optional JSON output path.")
    parser.add_argument("--md-out", help="Optional Markdown output path.")
    parser.add_argument("--apply", action="store_true", help="Delete expired rows according to the policy.")
    return parser.parse_args(argv)


def extract_default(text: str, env_name: str) -> str:
    match = re.search(rf"{env_name}:([^}}]+)}}", text)
    if not match:
        raise RuntimeError(f"Unable to resolve {env_name} from configuration")
    return match.group(1).strip()


def load_dev_defaults() -> dict[str, str]:
    dev_text = APP_DEV_PATH.read_text(encoding="utf-8")
    return {
        "jdbc_url": extract_default(dev_text, "IOT_MYSQL_URL"),
        "user": extract_default(dev_text, "IOT_MYSQL_USERNAME"),
        "password": extract_default(dev_text, "IOT_MYSQL_PASSWORD"),
    }


def resolve_runtime_args(args: argparse.Namespace) -> dict[str, str]:
    defaults = load_dev_defaults()
    jdbc_url = args.jdbc_url or os.getenv("IOT_MYSQL_URL") or defaults["jdbc_url"]
    user = args.user or os.getenv("IOT_MYSQL_USERNAME") or defaults["user"]
    password = args.password or os.getenv("IOT_MYSQL_PASSWORD") or defaults["password"]

    parsed = re.match(r"jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)", jdbc_url or "")
    if not parsed:
        raise RuntimeError(f"Invalid jdbc url: {jdbc_url}")
    host, port, database = parsed.group(1), parsed.group(2) or "3306", parsed.group(3)
    return {
        "db_host": host,
        "db_port": port,
        "db_name": database,
        "db_user": user,
        "db_password": password,
    }


def open_db(runtime: dict[str, str]) -> pymysql.connections.Connection:
    return pymysql.connect(
        host=runtime["db_host"],
        port=int(runtime["db_port"]),
        user=runtime["db_user"],
        password=runtime["db_password"],
        database=runtime["db_name"],
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        connect_timeout=10,
        read_timeout=30,
        write_timeout=30,
        autocommit=False,
    )


def load_policy(policy_path: str | Path | None = None) -> dict[str, Any]:
    path = Path(policy_path) if policy_path else DEFAULT_POLICY_PATH
    return json.loads(path.read_text(encoding="utf-8"))


def fetch_row(cur: pymysql.cursors.Cursor, sql: str, params: tuple[Any, ...]) -> dict[str, Any]:
    cur.execute(sql, params)
    return cur.fetchone() or {}


def build_where_clause(base_where: str | None, time_field: str) -> str:
    base = (base_where or "").strip()
    if not base:
        return f"{time_field} < %s"
    return f"({base}) AND {time_field} < %s"


def count_rows(cur: pymysql.cursors.Cursor, table_name: str, base_where: str | None = None) -> int:
    sql = f"SELECT COUNT(*) AS total FROM {table_name}"
    params: tuple[Any, ...] = ()
    if base_where:
        sql += f" WHERE {base_where}"
    row = fetch_row(cur, sql, params)
    return int(row.get("total") or 0)


def count_expired_rows(cur: pymysql.cursors.Cursor, table_name: str, time_field: str, cutoff_at: datetime, base_where: str | None = None) -> int:
    sql = f"SELECT COUNT(*) AS total FROM {table_name} WHERE {build_where_clause(base_where, time_field)}"
    row = fetch_row(cur, sql, (cutoff_at,))
    return int(row.get("total") or 0)


def fetch_time_bounds(
    cur: pymysql.cursors.Cursor,
    table_name: str,
    time_field: str,
    base_where: str | None = None,
) -> dict[str, str | None]:
    sql = f"SELECT MIN({time_field}) AS oldest, MAX({time_field}) AS newest FROM {table_name}"
    params: tuple[Any, ...] = ()
    if base_where:
        sql += f" WHERE {base_where}"
    row = fetch_row(cur, sql, params)
    return {
        "oldest": format_datetime(row.get("oldest")),
        "newest": format_datetime(row.get("newest")),
    }


def format_datetime(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value.isoformat(timespec="seconds")
    return str(value)


def sample_fields_for(table_name: str, time_field: str) -> list[str]:
    if table_name == "sys_observability_span_log":
        return ["trace_id", "span_type", time_field]
    if table_name == "sys_business_event_log":
        return ["trace_id", "event_code", time_field]
    return ["trace_id", "topic", time_field]


def sample_key_name(column_name: str) -> str:
    mapping = {
        "trace_id": "traceId",
        "span_type": "spanType",
        "event_code": "eventCode",
        "report_time": "reportTime",
        "started_at": "startedAt",
        "occurred_at": "occurredAt",
    }
    return mapping.get(column_name, column_name)


def fetch_samples(
    cur: pymysql.cursors.Cursor,
    table_name: str,
    time_field: str,
    cutoff_at: datetime,
    sample_limit: int,
    base_where: str | None = None,
) -> list[dict[str, Any]]:
    if sample_limit <= 0:
        return []
    columns = sample_fields_for(table_name, time_field)
    sql = f"""
        SELECT {", ".join(columns)}
        FROM {table_name}
        WHERE {build_where_clause(base_where, time_field)}
        ORDER BY {time_field} ASC
        LIMIT {int(sample_limit)}
    """
    cur.execute(sql, (cutoff_at,))
    rows = cur.fetchall() or []
    samples: list[dict[str, Any]] = []
    for row in rows:
        normalized = {}
        for key, value in row.items():
            normalized[sample_key_name(key)] = format_datetime(value) if key == time_field else value
        samples.append(normalized)
    return samples


def delete_expired_rows(
    cur: pymysql.cursors.Cursor,
    table_name: str,
    time_field: str,
    cutoff_at: datetime,
    delete_batch_size: int,
    base_where: str | None = None,
) -> int:
    total_deleted = 0
    batch_size = max(int(delete_batch_size or 0), 1)
    sql = f"DELETE FROM {table_name} WHERE {build_where_clause(base_where, time_field)} LIMIT {batch_size}"
    while True:
        affected = cur.execute(sql, (cutoff_at,))
        total_deleted += int(affected or 0)
        if not affected or int(affected) < batch_size:
            break
    return total_deleted


def build_table_snapshot(
    cur: pymysql.cursors.Cursor,
    table_name: str,
    config: dict[str, Any],
    sample_limit: int,
    apply_mode: bool,
) -> dict[str, Any]:
    time_field = str(config["timeField"])
    retention_days = int(config["retentionDays"])
    delete_batch_size = int(config.get("deleteBatchSize") or 500)
    base_where = config.get("baseWhere")
    cutoff_at = datetime.now() - timedelta(days=max(retention_days, 0))

    total_rows = count_rows(cur, table_name, base_where)
    expired_rows = count_expired_rows(cur, table_name, time_field, cutoff_at, base_where)
    bounds = fetch_time_bounds(cur, table_name, time_field, base_where)
    samples = fetch_samples(cur, table_name, time_field, cutoff_at, sample_limit, base_where)
    deleted_rows = 0
    if apply_mode and expired_rows > 0:
        deleted_rows = delete_expired_rows(cur, table_name, time_field, cutoff_at, delete_batch_size, base_where)
    remaining_expired_rows = count_expired_rows(cur, table_name, time_field, cutoff_at, base_where)

    return {
        "label": config.get("label") or table_name,
        "timeField": time_field,
        "retentionDays": retention_days,
        "deleteBatchSize": delete_batch_size,
        "cutoffAt": cutoff_at.isoformat(timespec="seconds"),
        "totalRows": total_rows,
        "expiredRows": expired_rows,
        "deletedRows": deleted_rows,
        "remainingExpiredRows": remaining_expired_rows,
        "oldestRecordAt": bounds["oldest"],
        "latestRecordAt": bounds["newest"],
        "samples": samples,
    }


def collect_governance_snapshot(
    conn: pymysql.connections.Connection,
    policy: dict[str, Any],
    apply_mode: bool,
) -> dict[str, Any]:
    tables = policy.get("tables") or {}
    sample_limit = int(policy.get("sampleLimit") or 5)
    report_tables: dict[str, Any] = {}
    summary = {"expiredRows": 0, "deletedRows": 0, "tablesWithExpiredRows": 0}

    with conn.cursor() as cur:
        for table_name, config in tables.items():
            table_snapshot = build_table_snapshot(cur, table_name, config, sample_limit, apply_mode)
            report_tables[table_name] = table_snapshot
            summary["expiredRows"] += int(table_snapshot["expiredRows"])
            summary["deletedRows"] += int(table_snapshot["deletedRows"])
            if int(table_snapshot["expiredRows"]) > 0:
                summary["tablesWithExpiredRows"] += 1
        if apply_mode:
            conn.commit()
        else:
            conn.rollback()

    return {
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "mode": "APPLY" if apply_mode else "DRY_RUN",
        "tables": report_tables,
        "summary": summary,
    }


def render_markdown(report: dict[str, Any]) -> str:
    lines = [
        "# 可观测日志治理报告",
        "",
        f"- 生成时间：{report.get('generatedAt') or 'n/a'}",
        f"- 执行模式：{report.get('mode') or 'n/a'}",
        "",
        "## 汇总",
        "",
        "| 指标 | 数值 |",
        "| --- | ---: |",
        f"| 过期记录数 | {int((report.get('summary') or {}).get('expiredRows') or 0)} |",
        f"| 实际删除数 | {int((report.get('summary') or {}).get('deletedRows') or 0)} |",
        f"| 存在过期记录的表数 | {int((report.get('summary') or {}).get('tablesWithExpiredRows') or 0)} |",
        "",
        "## 分表明细",
        "",
    ]
    for table_name, table in (report.get("tables") or {}).items():
        lines.extend(
            [
                f"### {table_name}",
                "",
                f"- 标签：{table.get('label') or table_name}",
                f"- 保留天数：{table.get('retentionDays')}",
                f"- 时间字段：{table.get('timeField')}",
                f"- 过期阈值：{table.get('cutoffAt') or 'n/a'}",
                f"- 总量：{table.get('totalRows')}",
                f"- 过期量：{table.get('expiredRows')}",
                f"- 已删除：{table.get('deletedRows')}",
                f"- 剩余过期量：{table.get('remainingExpiredRows')}",
                f"- 最早记录：{table.get('oldestRecordAt') or 'n/a'}",
                f"- 最新记录：{table.get('latestRecordAt') or 'n/a'}",
                "",
                "样本：",
            ]
        )
        samples = table.get("samples") or []
        if not samples:
            lines.append("- 无")
        else:
            for sample in samples:
                lines.append(f"- `{json.dumps(sample, ensure_ascii=False)}`")
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def ensure_output_path(path_value: str | Path) -> Path:
    path = Path(path_value)
    path.parent.mkdir(parents=True, exist_ok=True)
    return path


def default_output_paths(workspace_root: Path, policy: dict[str, Any]) -> tuple[Path, Path]:
    report_dir = workspace_root / (policy.get("reportDirectory") or "logs/observability")
    report_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    base = report_dir / f"observability-log-governance-{stamp}"
    return base.with_suffix(".json"), base.with_suffix(".md")


def run_governance_cli(argv: list[str] | None = None, workspace_root: Path = REPO_ROOT) -> dict[str, Any]:
    args = parse_args(argv)
    runtime = resolve_runtime_args(args)
    policy = load_policy(args.policy_path)
    json_path, markdown_path = default_output_paths(Path(workspace_root), policy)
    if args.json_out:
        json_path = ensure_output_path(args.json_out)
    if args.md_out:
        markdown_path = ensure_output_path(args.md_out)

    connection = open_db(runtime)
    try:
        report = collect_governance_snapshot(connection, policy, args.apply)
    finally:
        connection.close()

    json_path = ensure_output_path(json_path)
    markdown_path = ensure_output_path(markdown_path)
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    markdown_path.write_text(render_markdown(report), encoding="utf-8")
    return {
        "exitCode": 0,
        "jsonPath": str(json_path),
        "markdownPath": str(markdown_path),
        "report": report,
    }


def main() -> int:
    result = run_governance_cli()
    return int(result["exitCode"])


if __name__ == "__main__":
    raise SystemExit(main())
