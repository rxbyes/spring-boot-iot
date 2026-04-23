#!/usr/bin/env python3
"""Tests for archived highway risk-point detail audit/export tooling."""

from __future__ import annotations

import importlib.util
import os
import sys
import tempfile
import types
import unittest
from pathlib import Path
from unittest import mock


SCRIPT_PATH = Path(__file__).resolve().parents[1] / "audit-risk-point-highway-archive.py"


def load_module():
    spec = importlib.util.spec_from_file_location("risk_point_highway_archive_audit", SCRIPT_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load module from {SCRIPT_PATH}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class RiskPointHighwayArchiveAuditScriptTest(unittest.TestCase):
    def test_resolve_connection_args_prefers_cli_then_env_then_defaults(self) -> None:
        module = load_module()
        args = types.SimpleNamespace(
            jdbc_url=None,
            user=None,
            password=None,
        )
        defaults = {
            "jdbc_url": "jdbc:mysql://default-host:3306/default_db",
            "user": "default-user",
            "password": "default-pass",
        }
        env = {
            "IOT_MYSQL_URL": "jdbc:mysql://env-host:3307/env_db",
            "IOT_MYSQL_USERNAME": "env-user",
            "IOT_MYSQL_PASSWORD": "env-pass",
        }
        with mock.patch.dict(os.environ, env, clear=False):
            with mock.patch.object(module, "load_dev_defaults", return_value=defaults):
                resolved = module.resolve_connection_args(args)
        self.assertEqual("env-host", resolved["host"])
        self.assertEqual(3307, resolved["port"])
        self.assertEqual("env_db", resolved["database"])
        self.assertEqual("env-user", resolved["user"])
        self.assertEqual("env-pass", resolved["password"])

        args.jdbc_url = "jdbc:mysql://cli-host:3308/cli_db"
        args.user = "cli-user"
        args.password = "cli-pass"
        with mock.patch.dict(os.environ, env, clear=False):
            with mock.patch.object(module, "load_dev_defaults", return_value=defaults):
                resolved = module.resolve_connection_args(args)
        self.assertEqual("cli-host", resolved["host"])
        self.assertEqual(3308, resolved["port"])
        self.assertEqual("cli_db", resolved["database"])
        self.assertEqual("cli-user", resolved["user"])
        self.assertEqual("cli-pass", resolved["password"])

    def test_evaluate_deletion_readiness_blocks_when_rows_and_bindings_exist(self) -> None:
        module = load_module()
        audit = {
            "table_exists": True,
            "row_count_total": 65,
            "row_count_deleted_0": 65,
            "risk_point_id_distinct_count": 65,
            "risk_point_device_active_binding_count": 0,
            "capability_binding_count": 9,
            "risk_point_join": {
                "missing_parent_rows": 0,
            },
        }
        decision = module.evaluate_deletion_readiness(audit)
        self.assertFalse(decision["ready"])
        self.assertIn("ARCHIVE_ROWS_STILL_PRESENT", decision["blocking_reasons"])
        self.assertIn("CAPABILITY_BINDINGS_STILL_PRESENT", decision["blocking_reasons"])
        self.assertIn("请先导出并备份 archived 明细数据", decision["checklist"])

    def test_render_backup_sql_escapes_text_and_preserves_column_order(self) -> None:
        module = load_module()
        sql = module.render_backup_sql(
            table_name="risk_point_highway_detail",
            columns=["id", "project_name", "tenant_id"],
            rows=[
                {"id": 1, "project_name": "G30'边坡", "tenant_id": 1},
                {"id": 2, "project_name": "普通项目", "tenant_id": 1},
            ],
        )
        self.assertIn("INSERT INTO `risk_point_highway_detail` (`id`, `project_name`, `tenant_id`) VALUES", sql)
        self.assertIn("'G30''边坡'", sql)
        self.assertIn("(2, '普通项目', 1)", sql)

    def test_export_rows_to_csv_writes_utf8_header_and_values(self) -> None:
        module = load_module()
        with tempfile.TemporaryDirectory(prefix="risk-point-highway-audit-") as temp_dir:
            output_path = Path(temp_dir) / "highway-detail.csv"
            module.export_rows_to_csv(
                output_path,
                rows=[
                    {"risk_point_id": 71, "risk_point_code": "RP-HW-SLOPE-045", "project_type": "边坡"},
                    {"risk_point_id": 73, "risk_point_code": "RP-HW-SLOPE-046", "project_type": "边坡"},
                ],
                fieldnames=["risk_point_id", "risk_point_code", "project_type"],
            )
            content = output_path.read_text(encoding="utf-8-sig")
        self.assertIn("risk_point_id,risk_point_code,project_type", content)
        self.assertIn("RP-HW-SLOPE-045", content)


if __name__ == "__main__":
    unittest.main()
