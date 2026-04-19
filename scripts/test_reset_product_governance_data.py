#!/usr/bin/env python3
"""Tests for the product governance reset script scaffold."""

from __future__ import annotations

import importlib.util
import tempfile
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

    def test_build_cleanup_plan_renders_scope_and_metadata_reset_targets(self) -> None:
        plan = self.module.build_cleanup_plan("2001", ["1001", "1002"], "dry-run")

        self.assertEqual("dry-run", plan["mode"])
        self.assertEqual("2001", plan["scope"]["tenantId"])
        self.assertEqual(["1001", "1002"], plan["scope"]["productIds"])
        self.assertEqual("product_ids", plan["scope"]["scopeType"])
        self.assertEqual("iot_product", plan["metadata_reset"]["table"])
        self.assertIn("objectInsight.customMetrics", plan["metadata_reset"]["paths"])
        self.assertEqual("risk_metric_linkage_binding", plan["operations"][0]["table"])
        self.assertEqual("delete", plan["operations"][0]["action"])

    def test_load_dev_defaults_extracts_mysql_connection_info(self) -> None:
        defaults = self.module.load_dev_defaults()

        self.assertIn("jdbc_url", defaults)
        self.assertIn("user", defaults)
        self.assertIn("password", defaults)
        self.assertTrue(defaults["jdbc_url"].startswith("jdbc:mysql://"))

    def test_write_backup_manifest_persists_json_summary(self) -> None:
        output_dir = Path(tempfile.mkdtemp())
        plan = self.module.build_cleanup_plan(None, [], "backup")

        manifest_path = self.module.write_backup_manifest(output_dir, plan)

        self.assertTrue(manifest_path.exists())
        self.assertEqual(".json", manifest_path.suffix)
        self.assertIn("product-governance-reset", manifest_path.name)
        self.assertIn('"mode": "backup"', manifest_path.read_text(encoding="utf-8"))

    def test_execute_cleanup_plan_runs_delete_then_update_in_order(self) -> None:
        statements: list[tuple[str, tuple[object, ...]]] = []

        class FakeCursor:
            def execute(self, sql, params=()):
                statements.append((str(sql).strip(), tuple(params)))

        class FakeConnection:
            def cursor(self):
                return FakeCursor()

            def commit(self):
                statements.append(("COMMIT", ()))

            def __enter__(self):
                return self

            def __exit__(self, exc_type, exc, tb):
                return False

        self.module.execute_cleanup_plan(
            FakeConnection(),
            self.module.build_cleanup_plan("2001", ["1001"], "execute"),
        )

        self.assertTrue(statements[0][0].startswith("DELETE FROM risk_metric_linkage_binding"))
        self.assertTrue(any(sql.startswith("UPDATE iot_product") for sql, _ in statements))
        self.assertTrue(any(sql.startswith("UPDATE iot_device_onboarding_case") for sql, _ in statements))
        self.assertEqual("COMMIT", statements[-1][0])

    def test_assert_execute_allowed_requires_execute_and_confirm(self) -> None:
        args = self.module.parse_args(["--mode", "execute"])

        with self.assertRaises(RuntimeError):
            self.module.assert_execute_allowed(args)

    def test_render_scope_clause_supports_all_tenant_and_product_filters(self) -> None:
        self.assertEqual("", self.module.render_scope_clause(None, []))
        self.assertIn("tenant_id = %s", self.module.render_scope_clause("2001", []))
        self.assertIn("product_id IN (%s,%s)", self.module.render_scope_clause(None, ["1001", "1002"]))

    def test_cross_domain_operations_include_onboarding_and_governance_cleanup(self) -> None:
        plan = self.module.build_cleanup_plan("2001", ["1001"], "dry-run")
        operation_keys = {(item["table"], item["action"]) for item in plan["operations"]}

        self.assertIn(("iot_device_onboarding_case", "reset-release-batch"), operation_keys)
        self.assertIn(("iot_governance_work_item", "conditional-delete"), operation_keys)
        self.assertIn(("sys_governance_approval_order", "conditional-delete"), operation_keys)

    def test_build_cleanup_plan_keeps_approval_scope_dependencies_before_release_deletes(self) -> None:
        plan = self.module.build_cleanup_plan("2001", ["1001"], "dry-run")
        ordered_tables = [item["table"] for item in plan["operations"]]

        self.assertLess(
            ordered_tables.index("sys_governance_approval_order"),
            ordered_tables.index("iot_product_contract_release_batch"),
        )
        self.assertLess(
            ordered_tables.index("sys_governance_approval_order"),
            ordered_tables.index("iot_vendor_metric_mapping_rule"),
        )

    def test_operation_sql_scopes_approval_orders_by_product_release_and_rule_subjects(self) -> None:
        sql, params = self.module.operation_sql(
            {"table": "sys_governance_approval_order", "action": "conditional-delete"},
            {"tenantId": None, "productIds": ["1001"], "scopeType": "product_ids"},
        )

        self.assertIn("subject_type = 'PRODUCT'", sql)
        self.assertIn("subject_type = 'RELEASE_BATCH'", sql)
        self.assertIn("subject_type = 'VENDOR_MAPPING_RULE'", sql)
        self.assertIn("work_item_id IN (SELECT id FROM iot_governance_work_item", sql)
        self.assertIn("1001", [str(item) for item in params])

    def test_operation_sql_resets_onboarding_case_to_contract_release_in_progress(self) -> None:
        sql, params = self.module.operation_sql(
            {"table": "iot_device_onboarding_case", "action": "reset-release-batch"},
            {"tenantId": "2001", "productIds": ["1001"], "scopeType": "product_ids"},
        )

        self.assertIn("release_batch_id = NULL", sql)
        self.assertIn("current_step = 'CONTRACT_RELEASE'", sql)
        self.assertIn("status = 'IN_PROGRESS'", sql)
        self.assertEqual(("1001",), params)

    def test_parse_jdbc_url_extracts_host_port_database_and_query(self) -> None:
        parsed = self.module.parse_jdbc_url(
            "jdbc:mysql://127.0.0.1:3306/rm_iot?useUnicode=true&characterEncoding=utf8mb4"
        )

        self.assertEqual("127.0.0.1", parsed["host"])
        self.assertEqual(3306, parsed["port"])
        self.assertEqual("rm_iot", parsed["database"])
        self.assertEqual(["utf8mb4"], parsed["query"]["characterEncoding"])


if __name__ == "__main__":
    unittest.main()
