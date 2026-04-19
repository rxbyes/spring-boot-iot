#!/usr/bin/env python3
"""Scaffold for resetting product governance derived data."""

from __future__ import annotations

import argparse
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
