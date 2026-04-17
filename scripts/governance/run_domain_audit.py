#!/usr/bin/env python3
"""Run governance audits for a single business domain."""

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
    evaluate_deletion_readiness,
    resolve_connection_args,
    write_json,
)
from scripts.governance.load_governance_registry import load_governance_registry


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run schema-governance domain audits.")
    parser.add_argument("--domain", required=True, help="Governance domain to audit, for example alarm.")
    parser.add_argument("--sample-limit", type=int, default=10, help="Sample row limit. Defaults to 10.")
    parser.add_argument("--output", help="Optional JSON output path.")
    parser.add_argument("--jdbc-url", help="Override JDBC url. Defaults to application-dev.yml / env.")
    parser.add_argument("--user", help="Override mysql username. Defaults to application-dev.yml / env.")
    parser.add_argument("--password", help="Override mysql password. Defaults to application-dev.yml / env.")
    return parser.parse_args()


def run_audit(args: argparse.Namespace) -> dict[str, object]:
    governance_registry = load_governance_registry(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")
    if args.domain not in governance_registry.domains:
        raise RuntimeError(f"Governance domain not found: {args.domain}")

    connection_args = resolve_connection_args(args)
    domain = governance_registry.domains[args.domain]
    objects_payload: list[dict[str, object]] = []
    for object_entry in sorted(domain.objects.values(), key=lambda item: item.object_name):
        expected_schema_object = governance_registry.structure_registry.mysql.get(object_entry.object_name)
        payload: dict[str, object] = {
            "objectName": object_entry.object_name,
            "stage": object_entry.governance_stage,
            "seedPackages": list(object_entry.seed_packages),
            "auditProfile": object_entry.real_env_audit_profile,
            "ownerModule": object_entry.owner_module,
            "notes": object_entry.notes,
        }
        try:
            if object_entry.real_env_audit_profile != "mysql_archived_object_with_seed":
                raise RuntimeError(f"unsupported profile: {object_entry.real_env_audit_profile}")
            audit = audit_mysql_archived_object(connection_args, object_entry.object_name, args.sample_limit)
            attach_schema_comment_drift(audit, expected_schema_object)
            payload["audit"] = {key: value for key, value in audit.items() if key not in {"export_columns", "export_rows"}}
            payload["deletionDecision"] = evaluate_deletion_readiness(audit)
        except Exception as exc:  # noqa: BLE001
            payload["audit_error"] = str(exc)
            payload["deletionDecision"] = {
                "ready": False,
                "blocking_reasons": ["AUDIT_ERROR"],
                "checklist": list(object_entry.manual_checklist),
            }
        objects_payload.append(payload)

    return {
        "capturedAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "domain": args.domain,
        "connection": {
            "host": connection_args["host"],
            "port": connection_args["port"],
            "database": connection_args["database"],
        },
        "objects": objects_payload,
    }


def main() -> int:
    try:
        args = parse_args()
        payload = run_audit(args)
        print(json.dumps(payload, ensure_ascii=False, indent=2, default=str))
        if args.output:
            write_json(Path(args.output).expanduser().resolve(), payload)
        return 0
    except Exception as exc:  # pragma: no cover - CLI failure path.
        print(f"[run-domain-audit] {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
