#!/usr/bin/env python3
"""Compatibility wrapper for archived highway risk-point detail audit/export."""

from __future__ import annotations

import argparse
import json
import os
import sys
from datetime import datetime
from pathlib import Path
from urllib.parse import urlparse

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from scripts.governance import domain_audit_support as support
from scripts.governance.domain_audit_support import (
    attach_schema_comment_drift,
    audit_mysql_archived_object,
    evaluate_deletion_readiness,
    export_rows_to_csv,
    render_backup_sql,
    write_json,
)
from scripts.schema.load_registry import load_registry


TABLE_NAME = "risk_point_highway_detail"


load_dev_defaults = support.load_dev_defaults


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Audit archived highway risk-point detail data and optionally export backup files."
    )
    parser.add_argument("--jdbc-url", help="Override JDBC url. Defaults to application-dev.yml / env.")
    parser.add_argument("--user", help="Override mysql username. Defaults to application-dev.yml / env.")
    parser.add_argument("--password", help="Override mysql password. Defaults to application-dev.yml / env.")
    parser.add_argument("--sample-limit", type=int, default=10, help="Sample row limit. Defaults to 10.")
    parser.add_argument("--export-json", help="Write the audit payload to a JSON file.")
    parser.add_argument("--export-csv", help="Write all archived detail rows to a UTF-8 CSV file.")
    parser.add_argument("--export-sql", help="Write a backup INSERT SQL file for all archived detail rows.")
    return parser.parse_args()


def resolve_connection_args(args) -> dict[str, object]:
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


def build_payload(connection_args: dict[str, object], audit: dict[str, object]) -> dict[str, object]:
    return {
        "captured_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "connection": {
            "host": connection_args["host"],
            "port": connection_args["port"],
            "database": connection_args["database"],
        },
        "audit": {key: value for key, value in audit.items() if key not in {"export_columns", "export_rows"}},
        "deletion_decision": evaluate_deletion_readiness(audit),
    }


def main() -> int:
    try:
        args = parse_args()
        connection_args = resolve_connection_args(args)
        audit = audit_mysql_archived_object(connection_args, TABLE_NAME, args.sample_limit)
        expected_schema_object = load_registry(REPO_ROOT / "schema").mysql.get(TABLE_NAME)
        attach_schema_comment_drift(audit, expected_schema_object)
        payload = build_payload(connection_args, audit)
        print(json.dumps(payload, ensure_ascii=False, indent=2, default=str))
        if args.export_json:
            write_json(Path(args.export_json).expanduser().resolve(), payload)
        if args.export_csv:
            export_rows_to_csv(
                Path(args.export_csv).expanduser().resolve(),
                audit.get("export_rows", []),
                audit.get("export_columns", []),
            )
        if args.export_sql:
            Path(args.export_sql).expanduser().resolve().write_text(
                render_backup_sql(TABLE_NAME, audit.get("export_columns", []), audit.get("export_rows", [])),
                encoding="utf-8",
            )
        return 0
    except Exception as exc:  # pragma: no cover - CLI failure path.
        print(f"[risk-point-highway-archive] {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
