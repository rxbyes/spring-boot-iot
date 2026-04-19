#!/usr/bin/env python3
"""Scaffold for resetting product governance derived data."""

from __future__ import annotations

import argparse
import json
import re
from datetime import datetime
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parent.parent
APP_DEV_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application-dev.yml"

DELETE_TABLES = [
    "risk_metric_linkage_binding",
    "risk_metric_emergency_plan_binding",
    "risk_point_device_pending_promotion",
    "risk_point_device_pending_binding",
    "risk_point_device_capability_binding",
    "risk_point_device",
    "risk_metric_catalog",
    "iot_product_metric_resolver_snapshot",
    "iot_product_contract_release_snapshot",
    "iot_product_contract_release_batch",
    "iot_vendor_metric_mapping_rule_snapshot",
    "iot_vendor_metric_mapping_rule",
    "iot_vendor_metric_evidence",
    "iot_product_model",
]

CONDITIONAL_DELETE_TABLES = [
    "rule_definition",
    "iot_governance_work_item",
    "sys_governance_approval_transition",
    "sys_governance_approval_order",
]

KEEP_TABLES = [
    "iot_product",
    "iot_device",
    "iot_device_relation",
]


def build_cleanup_targets() -> dict[str, list[str]]:
    """Return independent copies of the cleanup target lists."""
    return {
        "delete_tables": DELETE_TABLES[:],
        "conditional_delete_tables": CONDITIONAL_DELETE_TABLES[:],
        "keep_tables": KEEP_TABLES[:],
    }


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    """Parse the initial CLI arguments for the reset workflow."""
    parser = argparse.ArgumentParser(description="Reset product governance derived data")
    parser.add_argument("--mode", choices=("dry-run", "backup", "execute"), default="dry-run")
    parser.add_argument("--tenant-id")
    parser.add_argument("--product-ids", nargs="*", default=[])
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--confirm", action="store_true")
    return parser.parse_args(argv)


def extract_default(text: str, env_name: str) -> str:
    """Extract the default value from a Spring placeholder."""
    match = re.search(rf"{env_name}:([^}}]+)}}", text)
    if not match:
        raise RuntimeError(f"Unable to resolve {env_name} from {APP_DEV_PATH}")
    return match.group(1).strip()


def load_dev_defaults() -> dict[str, str]:
    """Read the default MySQL connection settings from application-dev.yml."""
    text = APP_DEV_PATH.read_text(encoding="utf-8")
    return {
        "jdbc_url": extract_default(text, "IOT_MYSQL_URL"),
        "user": extract_default(text, "IOT_MYSQL_USERNAME"),
        "password": extract_default(text, "IOT_MYSQL_PASSWORD"),
    }


def build_cleanup_plan(
    tenant_id: str | None,
    product_ids: list[str],
    mode: str,
) -> dict[str, object]:
    """Build the dry-run cleanup manifest for later execution stages."""
    normalized_product_ids = [item for item in product_ids if str(item).strip()]
    operations = [{"table": table, "action": "delete"} for table in DELETE_TABLES]
    operations.extend({"table": table, "action": "conditional-delete"} for table in CONDITIONAL_DELETE_TABLES)
    operations.append({"table": "iot_product", "action": "reset-metadata"})
    operations.append({"table": "iot_device_onboarding_case", "action": "reset-release-batch"})
    return {
        "mode": mode,
        "scope": {
            "tenantId": tenant_id,
            "productIds": normalized_product_ids,
            "scopeType": "product_ids" if normalized_product_ids else ("tenant" if tenant_id else "all"),
        },
        "operations": operations,
        "metadata_reset": {
            "table": "iot_product",
            "paths": ["objectInsight.customMetrics"],
        },
    }


def write_backup_manifest(output_dir: Path, plan: dict[str, object]) -> Path:
    """Persist the current cleanup plan as a backup manifest."""
    output_dir.mkdir(parents=True, exist_ok=True)
    manifest_path = output_dir / f"product-governance-reset-{datetime.now():%Y%m%d%H%M%S}.json"
    manifest_path.write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding="utf-8")
    return manifest_path


def operation_sql(operation: dict[str, str]) -> str:
    """Build the SQL statement for a single cleanup operation."""
    if operation["action"] in {"delete", "conditional-delete"}:
        return f"DELETE FROM {operation['table']} WHERE deleted = 0"
    if operation["table"] == "iot_product":
        return "UPDATE iot_product SET metadata_json = NULL WHERE deleted = 0"
    if operation["table"] == "iot_device_onboarding_case":
        return "UPDATE iot_device_onboarding_case SET release_batch_id = NULL WHERE deleted = 0"
    raise ValueError(f"Unsupported operation: {operation}")


def execute_cleanup_plan(connection, plan: dict[str, object]) -> None:
    """Execute the current cleanup plan against an open DB connection."""
    with connection:
        cursor = connection.cursor()
        for operation in plan["operations"]:
            cursor.execute(operation_sql(operation))
        connection.commit()


def assert_execute_allowed(args: argparse.Namespace) -> None:
    """Prevent execute mode from running without explicit confirmation."""
    if args.mode == "execute" and (not args.execute or not args.confirm):
        raise RuntimeError("Execute mode requires both --execute and --confirm")


def main(argv: list[str] | None = None) -> int:
    """Print the current cleanup plan in JSON form."""
    args = parse_args(argv)
    assert_execute_allowed(args)
    plan = build_cleanup_plan(args.tenant_id, args.product_ids, args.mode)
    if args.mode == "backup":
        manifest_path = write_backup_manifest(REPO_ROOT / "tmp" / "product-governance-reset", plan)
        print(json.dumps({"backupManifest": str(manifest_path)}, ensure_ascii=False))
        return 0
    print(json.dumps(plan, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
