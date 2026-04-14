import importlib.util
import pathlib
import sys
import unittest


REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT_PATH = REPO_ROOT / "scripts" / "schema" / "load_registry.py"
SPEC = importlib.util.spec_from_file_location("schema_registry_loader", SCRIPT_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"Unable to load schema registry module from {SCRIPT_PATH}")
schema_registry_loader = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = schema_registry_loader
SPEC.loader.exec_module(schema_registry_loader)

PLACEHOLDER_PATTERNS = ("表说明", "字段说明", "业务边界说明", "对象业务边界说明", "???", "????")


class SchemaRegistryBaselineTest(unittest.TestCase):
    def test_registry_contract_and_comment_compliance(self):
        registry = schema_registry_loader.load_registry(REPO_ROOT / "schema")

        # Task 1 baseline contract
        self.assertEqual(len(registry.mysql_objects), 56)
        self.assertEqual(registry.mysql["risk_point_highway_detail"].lifecycle, "archived")
        self.assertEqual(
            registry.mysql["risk_point_device_capability_binding"].lifecycle,
            "active",
        )
        self.assertEqual(
            sorted(registry.tdengine.keys()),
            [
                "iot_agg_measure_hour",
                "iot_device_telemetry_point",
                "iot_raw_event_point",
                "iot_raw_measure_point",
                "iot_raw_status_point",
            ],
        )
        self.assertEqual(
            registry.tdengine["iot_agg_measure_hour"].runtime_bootstrap_mode,
            "manual_bootstrap_required",
        )
        self.assertEqual(registry.find_invalid_relation_targets(), [])
        self.assertEqual(registry.find_missing_comments(), [])
        self.assertEqual(registry.find_english_only_comments(), [])

    def test_representative_mysql_object_has_rich_metadata(self):
        registry = schema_registry_loader.load_registry(REPO_ROOT / "schema")
        message_log = registry.mysql["iot_device_message_log"]

        self.assertEqual(message_log.storage_type, "mysql_table")
        self.assertEqual(message_log.owner_module, "spring-boot-iot-device")
        self.assertTrue(message_log.included_in_init)
        self.assertTrue(message_log.included_in_schema_sync)
        self.assertEqual(message_log.runtime_bootstrap_mode, "schema_sync_managed")
        self.assertTrue(bool(message_log.lineage_role))
        self.assertTrue(bool(message_log.business_boundary))

        # Full baseline field coverage from sql/init.sql (not two-field stub)
        self.assertEqual(len(message_log.fields), 12)
        self.assertGreaterEqual(len(message_log.indexes), 5)
        self.assertGreaterEqual(len(message_log.relations), 2)
        self._assert_no_placeholder_text(message_log.table_comment_zh)
        self._assert_no_placeholder_text(message_log.business_boundary)
        for field in message_log.fields:
            self._assert_no_placeholder_text(field.comment_zh)

    def test_representative_governance_table_has_indexes_and_relations(self):
        registry = schema_registry_loader.load_registry(REPO_ROOT / "schema")
        approval_order = registry.mysql["sys_governance_approval_order"]

        self.assertEqual(approval_order.owner_module, "spring-boot-iot-system")
        self.assertEqual(approval_order.storage_type, "mysql_table")
        self.assertGreaterEqual(len(approval_order.fields), 18)
        self.assertGreaterEqual(len(approval_order.indexes), 4)
        self.assertGreaterEqual(len(approval_order.relations), 3)
        self.assertTrue(all(idx.columns for idx in approval_order.indexes))

    def test_tdengine_objects_have_full_field_depth(self):
        registry = schema_registry_loader.load_registry(REPO_ROOT / "schema")

        self.assertEqual(len(registry.tdengine["iot_device_telemetry_point"].fields), 18)
        self.assertEqual(len(registry.tdengine["iot_raw_measure_point"].fields), 19)
        self.assertEqual(len(registry.tdengine["iot_raw_status_point"].fields), 19)
        self.assertEqual(len(registry.tdengine["iot_raw_event_point"].fields), 19)
        self.assertEqual(len(registry.tdengine["iot_agg_measure_hour"].fields), 20)
        self.assertTrue(
            any(field.is_tag for field in registry.tdengine["iot_raw_measure_point"].fields)
        )

    def test_mysql_compatibility_view_has_definition_metadata(self):
        registry = schema_registry_loader.load_registry(REPO_ROOT / "schema")
        view_obj = registry.mysql_views["iot_message_log"]

        self.assertEqual(view_obj.storage_type, "mysql_view")
        self.assertEqual(view_obj.owner_module, "spring-boot-iot-device")
        self.assertEqual(len(view_obj.fields), 12)
        self.assertEqual(view_obj.source_tables, ("iot_device_message_log",))
        self.assertIn("SELECT", view_obj.definition_sql.upper())
        self.assertIn("FROM IOT_DEVICE_MESSAGE_LOG", view_obj.definition_sql.upper())
        self.assertGreaterEqual(len(view_obj.relations), 1)
        self._assert_no_placeholder_text(view_obj.table_comment_zh)
        self._assert_no_placeholder_text(view_obj.comment_zh)
        self._assert_no_placeholder_text(view_obj.business_boundary)
        for field in view_obj.fields:
            self._assert_no_placeholder_text(field.comment_zh)

    def _assert_no_placeholder_text(self, text: str):
        for pattern in PLACEHOLDER_PATTERNS:
            self.assertNotIn(pattern, text)


if __name__ == "__main__":
    unittest.main()
