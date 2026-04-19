#!/usr/bin/env python3
"""Scaffold for resetting product governance derived data."""

from __future__ import annotations

import argparse
import json
import re
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


def main(argv: list[str] | None = None) -> int:
    """Print the current cleanup plan in JSON form."""
    args = parse_args(argv)
    plan = build_cleanup_plan(args.tenant_id, args.product_ids, args.mode)
    print(json.dumps(plan, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
