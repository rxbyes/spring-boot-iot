import importlib.util
import json
import pathlib
import re
import sys
import unittest

import scripts.schema_contract_test_support as schema_contract_support


REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
RENDERER_PATH = REPO_ROOT / "scripts" / "schema" / "render_artifacts.py"


def _load_renderer_module():
    if not RENDERER_PATH.exists():
        raise AssertionError(f"Missing renderer script: {RENDERER_PATH}")
    spec = importlib.util.spec_from_file_location("schema_artifact_renderer", RENDERER_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load renderer module from {RENDERER_PATH}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def _as_json(payload):
    if isinstance(payload, dict):
        return payload
    if isinstance(payload, str):
        return json.loads(payload)
    raise TypeError(f"Unsupported payload type for JSON conversion: {type(payload)}")


class SchemaArtifactGenerationTest(unittest.TestCase):
    def test_render_artifacts_bundle_matches_task2_contract(self):
        renderer = _load_renderer_module()
        bundle = renderer.render_artifacts(REPO_ROOT / "schema")
        self.assertIsInstance(bundle, dict)
        self.assertEqual(
            {
                "catalog_markdown",
                "lineage_markdown",
                "mysql_init_sql",
                "mysql_runtime_manifest",
                "mysql_schema_sync_manifest",
                "tdengine_init_sql",
                "tdengine_runtime_manifest",
            },
            set(bundle),
        )

        mysql_init_sql = bundle["mysql_init_sql"]
        tdengine_init_sql = bundle["tdengine_init_sql"]
        catalog_markdown = bundle["catalog_markdown"]
        lineage_markdown = bundle["lineage_markdown"]
        mysql_runtime_manifest = _as_json(bundle["mysql_runtime_manifest"])
        tdengine_runtime_manifest = _as_json(bundle["tdengine_runtime_manifest"])
        mysql_schema_sync_manifest = _as_json(bundle["mysql_schema_sync_manifest"])

        message_log_table_sql = schema_contract_support.extract_create_table_statement(
            mysql_init_sql,
            "iot_message_log",
        )
        self.assertIn("设备消息日志表", message_log_table_sql)
        business_event_table_sql = schema_contract_support.extract_create_table_statement(
            mysql_init_sql,
            "sys_business_event_log",
        )
        span_table_sql = schema_contract_support.extract_create_table_statement(
            mysql_init_sql,
            "sys_observability_span_log",
        )
        self.assertIn("业务事件日志表", business_event_table_sql)
        self.assertIn("可观测调用片段日志表", span_table_sql)
        self.assertRegex(mysql_init_sql, re.compile(r"[\u4e00-\u9fff]"))
        self.assertNotIn("risk_point_highway_detail", mysql_init_sql)
        self.assertNotIn("CREATE OR REPLACE VIEW iot_message_log", mysql_init_sql)
        self.assertNotIn("CREATE OR REPLACE VIEW iot_device_message_log", mysql_init_sql)

        for object_name in (
            "iot_device_telemetry_point",
            "iot_raw_measure_point",
            "iot_raw_status_point",
            "iot_raw_event_point",
            "iot_agg_measure_hour",
        ):
            statement = schema_contract_support.extract_tdengine_create_statement(
                tdengine_init_sql,
                object_name,
            )
            self.assertIn(object_name, statement)

        catalog_rows = schema_contract_support.parse_markdown_table(
            catalog_markdown,
            "| Name | Storage | Lifecycle | In Init | In Schema Sync | Runtime Bootstrap | Owner Module | Comment |",
        )
        archived_catalog_row = schema_contract_support.get_markdown_table_row(
            catalog_rows,
            "Name",
            "risk_point_highway_detail",
        )
        agg_catalog_row = schema_contract_support.get_markdown_table_row(
            catalog_rows,
            "Name",
            "iot_agg_measure_hour",
        )
        self.assertEqual("mysql_table", archived_catalog_row["Storage"])
        self.assertEqual("archived", archived_catalog_row["Lifecycle"])
        self.assertEqual("no", archived_catalog_row["In Init"])
        self.assertEqual("no", archived_catalog_row["In Schema Sync"])
        self.assertEqual("disabled", archived_catalog_row["Runtime Bootstrap"])
        self.assertEqual("tdengine_stable", agg_catalog_row["Storage"])
        self.assertEqual("active", agg_catalog_row["Lifecycle"])
        self.assertEqual("yes", agg_catalog_row["In Init"])
        self.assertEqual("no", agg_catalog_row["In Schema Sync"])
        self.assertEqual("manual_bootstrap_required", agg_catalog_row["Runtime Bootstrap"])

        lineage_summary_rows = schema_contract_support.parse_markdown_table(
            lineage_markdown,
            "| Domain | Objects | Relations | Roles |",
        )
        alarm_domain_summary = schema_contract_support.get_markdown_table_row(
            lineage_summary_rows,
            "Domain",
            "alarm",
        )
        self.assertIn("domain_master_data", alarm_domain_summary["Roles"])

        alarm_section = schema_contract_support.extract_markdown_section(
            lineage_markdown,
            "## Domain alarm",
        )
        device_section = schema_contract_support.extract_markdown_section(
            lineage_markdown,
            "## Domain device",
        )
        alarm_rows = schema_contract_support.parse_markdown_table(
            alarm_section,
            "| Object | Lineage Role | Relations | Business Boundary |",
        )
        device_rows = schema_contract_support.parse_markdown_table(
            device_section,
            "| Object | Lineage Role | Relations | Business Boundary |",
        )
        archived_lineage_row = schema_contract_support.get_markdown_table_row(
            alarm_rows,
            "Object",
            "risk_point_highway_detail",
        )
        product_model_row = schema_contract_support.get_markdown_table_row(
            device_rows,
            "Object",
            "iot_product_model",
        )
        self.assertEqual("domain_master_data", archived_lineage_row["Lineage Role"])
        self.assertIn("risk_point", archived_lineage_row["Relations"])
        self.assertIn("高速公路风险点扩展表的数据持久化与查询", archived_lineage_row["Business Boundary"])
        self.assertEqual("domain_master_data", product_model_row["Lineage Role"])

        alarm_mermaid = schema_contract_support.extract_markdown_mermaid_block(alarm_section)
        self.assertTrue(
            schema_contract_support.mermaid_has_edge(
                alarm_mermaid,
                "risk_point_highway_detail",
                "risk_point",
                "belongs_to via risk_point_id",
            )
        )

        self.assertIn("tables", mysql_runtime_manifest)
        self.assertIn("views", mysql_runtime_manifest)
        mysql_runtime_table_names = {obj["name"] for obj in mysql_runtime_manifest["tables"]}
        mysql_runtime_view_names = {obj["name"] for obj in mysql_runtime_manifest["views"]}
        self.assertIn("iot_message_log", mysql_runtime_table_names)
        self.assertIn("sys_business_event_log", mysql_runtime_table_names)
        self.assertIn("sys_observability_span_log", mysql_runtime_table_names)
        self.assertNotIn("iot_device_message_log", mysql_runtime_view_names)
        self.assertNotIn("risk_point_highway_detail", mysql_runtime_table_names)
        relation_entry = schema_contract_support.get_named_entry(
            mysql_runtime_manifest["tables"],
            "iot_device_relation",
            "mysql runtime table",
        )
        self.assertEqual("schema_sync_managed", relation_entry["runtimeBootstrapMode"])
        self.assertEqual("mysql_table", relation_entry["storageType"])
        self.assertTrue(any(item["name"] == "idx_relation_child_code" for item in relation_entry["indexes"]))

        tdengine_objects = tdengine_runtime_manifest["objects"]
        self.assertEqual(5, len(tdengine_objects))
        agg_entry = schema_contract_support.get_named_entry(
            tdengine_objects,
            "iot_agg_measure_hour",
            "tdengine runtime object",
        )
        self.assertEqual("manual_bootstrap_required", agg_entry["runtimeBootstrapMode"])
        self.assertEqual("tdengine_stable", agg_entry["storageType"])
        self.assertEqual(20, len(agg_entry["fieldDictionary"]))

        self.assertEqual(
            {
                "columnsToAdd",
                "createTableSql",
                "indexesToAdd",
                "tableLifecycle",
                "viewSql",
            },
            set(mysql_schema_sync_manifest),
        )
        self.assertFalse(mysql_schema_sync_manifest["viewSql"])
        self.assertTrue(
            any(entry["name"] == "iot_message_log" for entry in mysql_schema_sync_manifest["createTableSql"])
        )
        self.assertTrue(
            any(entry["name"] == "sys_business_event_log" for entry in mysql_schema_sync_manifest["createTableSql"])
        )
        self.assertTrue(
            any(entry["name"] == "sys_observability_span_log" for entry in mysql_schema_sync_manifest["createTableSql"])
        )
        self.assertFalse(
            any(entry["name"] == "risk_point_highway_detail" for entry in mysql_schema_sync_manifest["createTableSql"])
        )
        self.assertTrue(
            all("name" in entry and "columns" in entry for entry in mysql_schema_sync_manifest["columnsToAdd"])
        )
        self.assertTrue(
            all("name" in entry and "indexes" in entry for entry in mysql_schema_sync_manifest["indexesToAdd"])
        )
        self.assertEqual(
            "archived",
            mysql_schema_sync_manifest["tableLifecycle"]["risk_point_highway_detail"],
        )


if __name__ == "__main__":
    unittest.main()
