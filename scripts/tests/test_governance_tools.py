#!/usr/bin/env python3
"""Tests for governance audit/export helpers."""

from __future__ import annotations

import importlib.util
import pathlib
import sys
import tempfile
import unittest


REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
SUPPORT_PATH = REPO_ROOT / "scripts" / "governance" / "domain_audit_support.py"
RENDER_PATH = REPO_ROOT / "scripts" / "governance" / "render_governance_docs.py"
LOADER_PATH = REPO_ROOT / "scripts" / "governance" / "load_governance_registry.py"


def load_module(path: pathlib.Path, name: str):
    spec = importlib.util.spec_from_file_location(name, path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load module from {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


tools = load_module(SUPPORT_PATH, "governance_tools")
render_tools = load_module(RENDER_PATH, "governance_render_tools")
governance_loader = load_module(LOADER_PATH, "governance_loader_for_render")


class GovernanceToolsTest(unittest.TestCase):
    def test_evaluate_deletion_readiness_should_block_when_capability_binding_exists(self):
        audit = {
            "table_exists": True,
            "row_count_total": 65,
            "capability_binding_count": 9,
            "risk_point_device_active_binding_count": 0,
            "risk_point_join": {"missing_parent_rows": 0},
        }

        decision = tools.evaluate_deletion_readiness(audit)

        self.assertFalse(decision["ready"])
        self.assertIn("ARCHIVE_ROWS_STILL_PRESENT", decision["blocking_reasons"])
        self.assertIn("CAPABILITY_BINDINGS_STILL_PRESENT", decision["blocking_reasons"])

    def test_render_backup_sql_should_escape_content(self):
        sql = tools.render_backup_sql(
            table_name="risk_point_highway_detail",
            columns=["id", "project_name", "tenant_id"],
            rows=[
                {"id": 1, "project_name": "G30'边坡", "tenant_id": 1},
            ],
        )

        self.assertIn("INSERT INTO `risk_point_highway_detail`", sql)
        self.assertIn("'G30''边坡'", sql)

    def test_export_rows_to_csv_should_write_utf8_sig(self):
        with tempfile.TemporaryDirectory(prefix="governance-csv-") as temp_dir:
            output_path = pathlib.Path(temp_dir) / "sample.csv"
            tools.export_rows_to_csv(
                output_path,
                rows=[
                    {"risk_point_id": 71, "risk_point_code": "RP-HW-SLOPE-045"},
                ],
                fieldnames=["risk_point_id", "risk_point_code"],
            )
            content = output_path.read_text(encoding="utf-8-sig")

        self.assertIn("risk_point_id,risk_point_code", content)
        self.assertIn("RP-HW-SLOPE-045", content)

    def test_render_governance_catalog_should_include_alarm_sample(self):
        registry = governance_loader.load_governance_registry(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")
        markdown = render_tools.render_governance_catalog(registry)

        self.assertIn("| alarm | risk_point_highway_detail | archived |", markdown)
        self.assertIn("highway_archived_risk_points_seed", markdown)

    def test_render_domain_governance_ledger_should_include_alarm_summary_and_governance_object(self):
        registry = governance_loader.load_governance_registry(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")

        markdown = render_tools.render_domain_governance_ledger(registry)

        self.assertIn("# Database Schema Domain Governance Ledger", markdown)
        self.assertIn("## Domain alarm", markdown)
        self.assertIn("| Lifecycle | Count |", markdown)
        self.assertIn("| archived | 1 |", markdown)
        self.assertIn("| risk_point_highway_detail | archived |", markdown)
        self.assertIn("当前域如有真实库审计结论，请查看 `docs/04`", markdown)

    def test_render_domain_governance_ledger_should_show_no_governance_objects_for_plain_domain(self):
        registry = governance_loader.load_governance_registry(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")

        markdown = render_tools.render_domain_governance_ledger(registry)

        self.assertIn("## Domain device", markdown)
        self.assertIn("| 当前无登记治理对象 | - | - | - | - | - |", markdown)

    def test_render_domain_governance_ledger_should_include_relation_summary_for_alarm_domain(self):
        registry = governance_loader.load_governance_registry(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")

        markdown = render_tools.render_domain_governance_ledger(registry)

        self.assertIn("risk_point（belongs_to:risk_point_id）", markdown)
        self.assertIn("```mermaid", markdown)

    def test_check_governance_docs_should_report_missing_generated_files(self):
        with tempfile.TemporaryDirectory(prefix="governance-doc-check-") as temp_dir:
            temp_root = pathlib.Path(temp_dir)
            output_paths = {
                "catalog_markdown": temp_root / "database-schema-governance-catalog.generated.md",
                "domain_ledger_markdown": temp_root / "database-schema-domain-governance.generated.md",
            }

            mismatches = render_tools.check_governance_docs(
                REPO_ROOT / "schema-governance",
                REPO_ROOT / "schema",
                output_paths=output_paths,
            )

        self.assertEqual(
            [
                output_paths["catalog_markdown"],
                output_paths["domain_ledger_markdown"],
            ],
            mismatches,
        )

    def test_write_governance_docs_should_materialize_generated_appendices(self):
        with tempfile.TemporaryDirectory(prefix="governance-doc-write-") as temp_dir:
            temp_root = pathlib.Path(temp_dir)
            output_paths = {
                "catalog_markdown": temp_root / "database-schema-governance-catalog.generated.md",
                "domain_ledger_markdown": temp_root / "database-schema-domain-governance.generated.md",
            }

            written = render_tools.write_governance_docs(
                REPO_ROOT / "schema-governance",
                REPO_ROOT / "schema",
                output_paths=output_paths,
            )
            mismatches = render_tools.check_governance_docs(
                REPO_ROOT / "schema-governance",
                REPO_ROOT / "schema",
                output_paths=output_paths,
            )
            domain_markdown = output_paths["domain_ledger_markdown"].read_text(encoding="utf-8")

        self.assertEqual(
            [
                output_paths["catalog_markdown"],
                output_paths["domain_ledger_markdown"],
            ],
            written,
        )
        self.assertEqual([], mismatches)
        self.assertIn("# Database Schema Domain Governance Ledger", domain_markdown)

    def test_build_schema_comment_drift_should_report_table_and_field_mismatches(self):
        registry = governance_loader.load_governance_registry(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")
        expected = registry.structure_registry.mysql["risk_point_highway_detail"]
        audit = {
            "table_exists": True,
            "table_comment": "高速公路风险点详情",
            "columns": [
                {"column_name": "id", "column_comment": "??"},
                {"column_name": "risk_point_id", "column_comment": "风险点ID"},
            ],
        }

        drift = tools.build_schema_comment_drift(audit, expected)

        self.assertTrue(drift["has_drift"])
        self.assertEqual("高速公路风险点扩展表", drift["table_comment_drift"]["expected"])
        self.assertEqual("高速公路风险点详情", drift["table_comment_drift"]["actual"])
        self.assertEqual("id", drift["field_comment_drifts"][0]["column_name"])
        self.assertIn("project_name", drift["missing_columns"])


if __name__ == "__main__":
    unittest.main()
