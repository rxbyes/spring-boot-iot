#!/usr/bin/env python3
"""Export backup artifacts for a governance object."""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from scripts.governance.domain_audit_support import (
    attach_schema_comment_drift,
    audit_mysql_archived_object,
    export_rows_to_csv,
    render_backup_sql,
    resolve_connection_args,
    write_json,
)
from scripts.governance.load_governance_registry import load_governance_registry


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Export governance object backup artifacts.")
    parser.add_argument("--domain", required=True, help="Governance domain, for example alarm.")
    parser.add_argument("--object", dest="object_name", required=True, help="Governance object name.")
    parser.add_argument("--sample-limit", type=int, default=10, help="Sample row limit. Defaults to 10.")
    parser.add_argument("--output-dir", help="Optional output directory. Defaults to logs/governance-exports/<timestamp>.")
    parser.add_argument("--jdbc-url", help="Override JDBC url. Defaults to application-dev.yml / env.")
    parser.add_argument("--user", help="Override mysql username. Defaults to application-dev.yml / env.")
    parser.add_argument("--password", help="Override mysql password. Defaults to application-dev.yml / env.")
    return parser.parse_args()


def run_export(args: argparse.Namespace) -> dict[str, object]:
    governance_registry = load_governance_registry(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")
    if args.domain not in governance_registry.domains:
        raise RuntimeError(f"Governance domain not found: {args.domain}")
    domain = governance_registry.domains[args.domain]
    if args.object_name not in domain.objects:
        raise RuntimeError(f"Governance object not found in domain {args.domain!r}: {args.object_name}")

    object_entry = domain.objects[args.object_name]
    expected_schema_object = governance_registry.structure_registry.mysql.get(object_entry.object_name)
    connection_args = resolve_connection_args(args)
    if object_entry.real_env_audit_profile != "mysql_archived_object_with_seed":
        if object_entry.real_env_audit_profile == "mysql_hot_table_with_cold_archive":
            raise RuntimeError(
                "hot-table-with-cold-archive objects must use archive batch evidence instead of export_object_backup"
            )
        raise RuntimeError(f"unsupported profile: {object_entry.real_env_audit_profile}")

    audit = audit_mysql_archived_object(connection_args, object_entry.object_name, args.sample_limit)
    attach_schema_comment_drift(audit, expected_schema_object)
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    output_dir = (
        Path(args.output_dir).expanduser().resolve()
        if args.output_dir
        else (REPO_ROOT / "logs" / "governance-exports" / f"{args.domain}-{args.object_name}-{timestamp}")
    )
    output_dir.mkdir(parents=True, exist_ok=True)

    written_files: dict[str, str] = {}
    full_audit_path = output_dir / "audit.json"
    write_json(full_audit_path, audit)
    written_files["json"] = str(full_audit_path)

    if "csv" in object_entry.backup_requirements.export_formats:
        csv_path = output_dir / f"{args.object_name}.csv"
        export_rows_to_csv(csv_path, audit.get("export_rows", []), audit.get("export_columns", []))
        written_files["csv"] = str(csv_path)
    if "sql" in object_entry.backup_requirements.export_formats:
        sql_path = output_dir / f"{args.object_name}.sql"
        sql_path.write_text(
            render_backup_sql(args.object_name, audit.get("export_columns", []), audit.get("export_rows", [])),
            encoding="utf-8",
        )
        written_files["sql"] = str(sql_path)

    return {
        "domain": args.domain,
        "objectName": args.object_name,
        "outputDir": str(output_dir),
        "rowCountTotal": audit.get("row_count_total", 0),
        "writtenFiles": written_files,
    }


def main() -> int:
    try:
        payload = run_export(parse_args())
        print(json.dumps(payload, ensure_ascii=False, indent=2, default=str))
        return 0
    except Exception as exc:  # pragma: no cover - CLI failure path.
        print(f"[export-object-backup] {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
