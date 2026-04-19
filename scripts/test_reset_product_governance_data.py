#!/usr/bin/env python3
"""Tests for the product governance reset script scaffold."""

from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).resolve().parent / "reset-product-governance-data.py"


def load_module():
    spec = importlib.util.spec_from_file_location("reset_product_governance_data", SCRIPT_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load module from {SCRIPT_PATH}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class ProductGovernanceResetScriptTest(unittest.TestCase):
    def setUp(self) -> None:
        self.module = load_module()

    def test_build_cleanup_targets_keeps_master_tables(self) -> None:
        targets = self.module.build_cleanup_targets()

        self.assertEqual("risk_metric_linkage_binding", targets["delete_tables"][0])
        self.assertIn("iot_product_model", targets["delete_tables"])
        self.assertIn("rule_definition", targets["conditional_delete_tables"])
        self.assertIn("iot_product", targets["keep_tables"])
        self.assertIn("iot_device", targets["keep_tables"])
        self.assertIn("iot_device_relation", targets["keep_tables"])
        self.assertNotIn("iot_product", targets["delete_tables"])

    def test_parse_args_defaults_to_dry_run(self) -> None:
        args = self.module.parse_args([])

        self.assertEqual("dry-run", args.mode)
        self.assertFalse(args.execute)
        self.assertFalse(args.confirm)
        self.assertIsNone(args.tenant_id)
        self.assertEqual([], args.product_ids)


if __name__ == "__main__":
    unittest.main()
