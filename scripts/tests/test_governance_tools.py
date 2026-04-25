#!/usr/bin/env python3
"""Tests for governance audit/export helpers."""

from __future__ import annotations

import importlib.util
import pathlib
import sys
import tempfile
import unittest

import scripts.schema_contract_test_support as schema_contract_support


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


def load_registry():
    return governance_loader.load_governance_registry(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")


def structure_objects_for_domain(registry, domain_name: str):
    return sorted(
        [obj for obj in registry.structure_registry.all_objects if obj.domain == domain_name],
        key=lambda item: (item.storage_type, item.name),
    )


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

    def test_evaluate_hot_table_archive_health_should_block_when_archive_tables_missing(self):
        decision = tools.evaluate_hot_table_archive_health(
            {
                "hot_table_exists": True,
                "archive_table_exists": False,
                "batch_table_exists": False,
                "expired_rows": 12,
                "latest_batch": None,
            }
        )

        self.assertFalse(decision["ready"])
        self.assertIn("ARCHIVE_TABLE_MISSING", decision["blocking_reasons"])
        self.assertIn("ARCHIVE_BATCH_TABLE_MISSING", decision["blocking_reasons"])
        self.assertIn("NO_ARCHIVE_BATCH_EVIDENCE", decision["blocking_reasons"])

    def test_evaluate_hot_table_archive_health_should_pass_when_archive_chain_is_complete(self):
        decision = tools.evaluate_hot_table_archive_health(
            {
                "hot_table_exists": True,
                "archive_table_exists": True,
                "batch_table_exists": True,
                "expired_rows": 0,
                "latest_batch": {
                    "batch_no": "iot_message_log-20260426001010",
                    "status": "SUCCEEDED",
                },
            }
        )

        self.assertTrue(decision["ready"])
        self.assertEqual([], decision["blocking_reasons"])

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
        registry = load_registry()
        markdown = render_tools.render_governance_catalog(registry)
        archived_object = registry.domains["alarm"].objects["risk_point_highway_detail"]
        catalog_rows = schema_contract_support.parse_markdown_table(
            markdown,
            "| Domain | Object | Stage | Seed Packages | Audit Profile | Owner Module | Notes |",
        )
        archived_row = schema_contract_support.get_markdown_table_row(
            catalog_rows,
            "Object",
            archived_object.object_name,
        )

        self.assertEqual("alarm", archived_row["Domain"])
        self.assertEqual(archived_object.governance_stage, archived_row["Stage"])
        self.assertEqual(", ".join(archived_object.seed_packages), archived_row["Seed Packages"])
        self.assertEqual(archived_object.real_env_audit_profile, archived_row["Audit Profile"])
        self.assertEqual(archived_object.owner_module, archived_row["Owner Module"])
        self.assertEqual(archived_object.notes, archived_row["Notes"])

    def test_render_domain_governance_ledger_should_include_alarm_summary_and_governance_object(self):
        registry = load_registry()
        archived_object = registry.domains["alarm"].objects["risk_point_highway_detail"]
        alarm_structure_objects = structure_objects_for_domain(registry, "alarm")

        markdown = render_tools.render_domain_governance_ledger(registry)
        alarm_section = schema_contract_support.extract_markdown_section(markdown, "## Domain alarm")
        metric_rows = schema_contract_support.parse_markdown_table(
            alarm_section,
            "| Metric | Value |",
        )
        lifecycle_rows = schema_contract_support.parse_markdown_table(
            alarm_section,
            "| Lifecycle | Count |",
        )
        governance_rows = schema_contract_support.parse_markdown_table(
            alarm_section,
            "| Governance Object | Stage | Seed Packages | Audit Profile | Deletion Prerequisites | Notes |",
        )
        objects_row = schema_contract_support.get_markdown_table_row(metric_rows, "Metric", "Objects")
        relations_row = schema_contract_support.get_markdown_table_row(metric_rows, "Metric", "Relations")
        archived_lifecycle_row = schema_contract_support.get_markdown_table_row(
            lifecycle_rows,
            "Lifecycle",
            "archived",
        )
        archived_governance_row = schema_contract_support.get_markdown_table_row(
            governance_rows,
            "Governance Object",
            archived_object.object_name,
        )

        self.assertIn("# Database Schema Domain Governance Ledger", markdown)
        self.assertEqual(str(len(alarm_structure_objects)), objects_row["Value"])
        self.assertEqual(
            str(sum(len(obj.relations) for obj in alarm_structure_objects)),
            relations_row["Value"],
        )
        self.assertEqual(
            str(sum(1 for obj in alarm_structure_objects if obj.lifecycle == "archived")),
            archived_lifecycle_row["Count"],
        )
        self.assertEqual(archived_object.governance_stage, archived_governance_row["Stage"])
        self.assertEqual(", ".join(archived_object.seed_packages), archived_governance_row["Seed Packages"])
        self.assertEqual(archived_object.real_env_audit_profile, archived_governance_row["Audit Profile"])
        self.assertEqual(
            "<br>".join(archived_object.deletion_prerequisites),
            archived_governance_row["Deletion Prerequisites"],
        )
        self.assertEqual(archived_object.notes, archived_governance_row["Notes"])
        self.assertIn("`docs/04`", alarm_section)
        self.assertIn("`docs/08`", alarm_section)

    def test_render_domain_governance_ledger_should_include_device_message_log_governance_object(self):
        registry = load_registry()

        markdown = render_tools.render_domain_governance_ledger(registry)
        device_section = schema_contract_support.extract_markdown_section(markdown, "## Domain device")
        governance_rows = schema_contract_support.parse_markdown_table(
            device_section,
            "| Governance Object | Stage | Seed Packages | Audit Profile | Deletion Prerequisites | Notes |",
        )
        device_object_row = schema_contract_support.get_markdown_table_row(
            governance_rows,
            "Governance Object",
            "iot_message_log",
        )

        self.assertEqual("freeze_candidate", device_object_row["Stage"])
        self.assertEqual("-", device_object_row["Seed Packages"])
        self.assertEqual("mysql_hot_table_with_cold_archive", device_object_row["Audit Profile"])
        self.assertIn("archive_table_ready", device_object_row["Deletion Prerequisites"])
        self.assertIn("冷归档治理阶段", device_object_row["Notes"])

    def test_render_domain_governance_ledger_should_include_relation_summary_for_alarm_domain(self):
        registry = load_registry()

        markdown = render_tools.render_domain_governance_ledger(registry)
        alarm_section = schema_contract_support.extract_markdown_section(markdown, "## Domain alarm")
        relation_rows = schema_contract_support.parse_markdown_table(
            alarm_section,
            "| Object | Relations |",
        )
        highway_detail_row = schema_contract_support.get_markdown_table_row(
            relation_rows,
            "Object",
            "risk_point_highway_detail",
        )
        mermaid_block = schema_contract_support.extract_markdown_mermaid_block(alarm_section)

        self.assertIn("risk_point（belongs_to:risk_point_id）", highway_detail_row["Relations"])
        self.assertIn("sys_tenant（belongs_to:tenant_id）", highway_detail_row["Relations"])
        self.assertTrue(
            schema_contract_support.mermaid_has_edge(
                mermaid_block,
                "risk_point_highway_detail",
                "risk_point",
                "belongs_to via risk_point_id",
            )
        )
        self.assertTrue(
            schema_contract_support.mermaid_has_edge(
                mermaid_block,
                "risk_point_highway_detail",
                "sys_tenant",
                "belongs_to via tenant_id",
            )
        )

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
                output_paths["catalog_markdown"].resolve(),
                output_paths["domain_ledger_markdown"].resolve(),
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
            catalog_markdown = output_paths["catalog_markdown"].read_text(encoding="utf-8")
            domain_markdown = output_paths["domain_ledger_markdown"].read_text(encoding="utf-8")

        self.assertEqual(
            [
                output_paths["catalog_markdown"].resolve(),
                output_paths["domain_ledger_markdown"].resolve(),
            ],
            written,
        )
        self.assertEqual([], mismatches)
        catalog_rows = schema_contract_support.parse_markdown_table(
            catalog_markdown,
            "| Domain | Object | Stage | Seed Packages | Audit Profile | Owner Module | Notes |",
        )
        domain_section = schema_contract_support.extract_markdown_section(domain_markdown, "## Domain alarm")
        governance_rows = schema_contract_support.parse_markdown_table(
            domain_section,
            "| Governance Object | Stage | Seed Packages | Audit Profile | Deletion Prerequisites | Notes |",
        )
        schema_contract_support.get_markdown_table_row(
            catalog_rows,
            "Object",
            "risk_point_highway_detail",
        )
        schema_contract_support.get_markdown_table_row(
            governance_rows,
            "Governance Object",
            "risk_point_highway_detail",
        )

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
