import importlib.util
import json
import pathlib
import re
import sys
import unittest


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
        mysql_runtime_manifest = _as_json(bundle["mysql_runtime_manifest"])
        tdengine_runtime_manifest = _as_json(bundle["tdengine_runtime_manifest"])
        mysql_schema_sync_manifest = _as_json(bundle["mysql_schema_sync_manifest"])

        self.assertIn("CREATE TABLE iot_device_message_log", mysql_init_sql)
        self.assertIn("设备消息日志表", mysql_init_sql)
        self.assertRegex(mysql_init_sql, re.compile(r"[\u4e00-\u9fff]"))
        self.assertNotIn("risk_point_highway_detail", mysql_init_sql)
        self.assertIn("CREATE OR REPLACE VIEW iot_message_log", mysql_init_sql)

        self.assertIn(
            "CREATE TABLE IF NOT EXISTS iot_device_telemetry_point",
            tdengine_init_sql,
        )
        self.assertIn("CREATE STABLE IF NOT EXISTS iot_raw_measure_point", tdengine_init_sql)
        self.assertIn("CREATE STABLE IF NOT EXISTS iot_raw_status_point", tdengine_init_sql)
        self.assertIn("CREATE STABLE IF NOT EXISTS iot_raw_event_point", tdengine_init_sql)
        self.assertIn("CREATE STABLE IF NOT EXISTS iot_agg_measure_hour", tdengine_init_sql)

        self.assertIn("risk_point_highway_detail", catalog_markdown)
        self.assertIn("archived", catalog_markdown)
        self.assertIn(
            "| risk_point_highway_detail | mysql_table | archived | no | no | disabled |",
            catalog_markdown,
        )
        self.assertIn(
            "| iot_agg_measure_hour | tdengine_stable | active | yes | no | manual_bootstrap_required |",
            catalog_markdown,
        )

        self.assertIn("tables", mysql_runtime_manifest)
        self.assertIn("views", mysql_runtime_manifest)
        mysql_runtime_table_names = {obj["name"] for obj in mysql_runtime_manifest["tables"]}
        mysql_runtime_view_names = {obj["name"] for obj in mysql_runtime_manifest["views"]}
        self.assertIn("iot_device_message_log", mysql_runtime_table_names)
        self.assertIn("iot_message_log", mysql_runtime_view_names)
        self.assertNotIn("risk_point_highway_detail", mysql_runtime_table_names)
        relation_entry = next(
            entry for entry in mysql_runtime_manifest["tables"] if entry["name"] == "iot_device_relation"
        )
        self.assertIn("CREATE TABLE IF NOT EXISTS iot_device_relation", relation_entry["createSql"])
        self.assertTrue(any(item["name"] == "idx_relation_child_code" for item in relation_entry["indexes"]))

        tdengine_objects = tdengine_runtime_manifest["objects"]
        self.assertEqual(5, len(tdengine_objects))
        self.assertTrue(
            any(
                obj["name"] == "iot_agg_measure_hour"
                and obj["runtimeBootstrapMode"] == "manual_bootstrap_required"
                and "CREATE STABLE IF NOT EXISTS iot_agg_measure_hour" in obj["createSql"]
                for obj in tdengine_objects
            )
        )

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
        self.assertTrue(
            any(
                entry["name"] == "iot_message_log"
                and "CREATE OR REPLACE VIEW iot_message_log" in entry["sql"]
                for entry in mysql_schema_sync_manifest["viewSql"]
            )
        )
        self.assertTrue(
            any(entry["name"] == "iot_device_message_log" for entry in mysql_schema_sync_manifest["createTableSql"])
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
