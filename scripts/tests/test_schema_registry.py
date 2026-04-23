import importlib.util
import json
import pathlib
import sys
import tempfile
import unittest


REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT_PATH = REPO_ROOT / "scripts" / "schema" / "load_registry.py"
SPEC = importlib.util.spec_from_file_location("schema_registry_loader", SCRIPT_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"Unable to load schema registry module from {SCRIPT_PATH}")
schema_registry_loader = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = schema_registry_loader
SPEC.loader.exec_module(schema_registry_loader)

PLACEHOLDER_PATTERNS = (
    "表说明",
    "字段说明",
    "业务边界说明",
    "对象业务边界说明",
    "???",
    "????",
    "TODO",
    "TBD",
    "待补充",
    "N/A",
)


class SchemaRegistryBaselineTest(unittest.TestCase):
    def test_registry_contract_and_comment_compliance(self):
        registry = schema_registry_loader.load_registry(REPO_ROOT / "schema")

        # Task 1 baseline contract
        self.assertEqual(len(registry.mysql_objects), 56)
        self.assertEqual(registry.mysql["risk_point_highway_detail"].lifecycle, "archived")
        self.assertFalse(registry.mysql["risk_point_highway_detail"].included_in_init)
        self.assertFalse(registry.mysql["risk_point_highway_detail"].included_in_schema_sync)
        self.assertEqual(registry.mysql["risk_point_highway_detail"].runtime_bootstrap_mode, "disabled")
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
        self.assertFalse(registry.tdengine["iot_agg_measure_hour"].included_in_schema_sync)
        self.assertEqual(registry.find_invalid_relation_targets(), [])
        self.assertEqual(registry.find_missing_comments(), [])
        self.assertEqual(registry.find_english_only_comments(), [])
        self.assertEqual(registry.find_suspicious_comment_fragments(), [])

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

    def test_load_registry_should_fail_on_duplicate_object_names(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = pathlib.Path(tmp_dir)
            self._prepare_registry_dirs(root)

            duplicate_name = "dup_object"
            mysql_payload = {
                "domain": "dup-domain",
                "storageType": "mysql",
                "objects": [self._build_mysql_object(name=duplicate_name)],
            }
            views_payload = {
                "domain": "dup-view-domain",
                "storageType": "mysql_view",
                "objects": [
                    self._build_view_object(
                        name=duplicate_name,
                        source_table="source_table_a",
                    )
                ],
            }
            tdengine_payload = {
                "domain": "tdengine-domain",
                "storageType": "tdengine",
                "objects": [self._build_tdengine_object(name="td_ok")],
            }
            self._write_json(root / "mysql" / "a.json", mysql_payload)
            self._write_json(root / "views" / "b.json", views_payload)
            self._write_json(root / "tdengine" / "c.json", tdengine_payload)

            with self.assertRaises(ValueError):
                schema_registry_loader.load_registry(root)

    def test_load_registry_should_fail_on_invalid_lifecycle(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = pathlib.Path(tmp_dir)
            self._prepare_registry_dirs(root)

            mysql_payload = {
                "domain": "invalid-lifecycle-domain",
                "storageType": "mysql",
                "objects": [
                    self._build_mysql_object(
                        name="invalid_lifecycle_obj",
                        lifecycle="retired",
                    )
                ],
            }
            views_payload = {
                "domain": "ok-view-domain",
                "storageType": "mysql_view",
                "objects": [
                    self._build_view_object(
                        name="view_ok",
                        source_table="invalid_lifecycle_obj",
                    )
                ],
            }
            tdengine_payload = {
                "domain": "ok-tdengine-domain",
                "storageType": "tdengine",
                "objects": [self._build_tdengine_object(name="td_ok")],
            }
            self._write_json(root / "mysql" / "a.json", mysql_payload)
            self._write_json(root / "views" / "b.json", views_payload)
            self._write_json(root / "tdengine" / "c.json", tdengine_payload)

            with self.assertRaises(ValueError):
                schema_registry_loader.load_registry(root)

    def test_load_registry_should_fail_when_relation_target_missing(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = pathlib.Path(tmp_dir)
            self._prepare_registry_dirs(root)

            mysql_payload = {
                "domain": "relation-domain",
                "storageType": "mysql",
                "objects": [
                    self._build_mysql_object(
                        name="relation_source",
                        relations=[
                            {
                                "type": "belongs_to",
                                "target": "non_existent_target",
                                "viaField": "tenant_id",
                            }
                        ],
                    )
                ],
            }
            views_payload = {
                "domain": "relation-view-domain",
                "storageType": "mysql_view",
                "objects": [
                    self._build_view_object(
                        name="view_ok",
                        source_table="relation_source",
                    )
                ],
            }
            tdengine_payload = {
                "domain": "relation-tdengine-domain",
                "storageType": "tdengine",
                "objects": [self._build_tdengine_object(name="td_ok")],
            }
            self._write_json(root / "mysql" / "a.json", mysql_payload)
            self._write_json(root / "views" / "b.json", views_payload)
            self._write_json(root / "tdengine" / "c.json", tdengine_payload)

            with self.assertRaises(ValueError):
                schema_registry_loader.load_registry(root)

    def test_load_registry_should_fail_when_relation_via_field_missing(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = pathlib.Path(tmp_dir)
            self._prepare_registry_dirs(root)

            mysql_payload = {
                "domain": "relation-via-field-domain",
                "storageType": "mysql",
                "objects": [
                    self._build_mysql_object(
                        name="relation_source",
                        relations=[
                            {
                                "type": "belongs_to",
                                "target": "relation_target",
                                "viaField": "missing_field",
                            }
                        ],
                    ),
                    self._build_mysql_object(name="relation_target"),
                ],
            }
            views_payload = {
                "domain": "ok-view-domain",
                "storageType": "mysql_view",
                "objects": [self._build_view_object(name="view_ok", source_table="relation_source")],
            }
            tdengine_payload = {
                "domain": "ok-tdengine-domain",
                "storageType": "tdengine",
                "objects": [self._build_tdengine_object(name="td_ok")],
            }
            self._write_json(root / "mysql" / "a.json", mysql_payload)
            self._write_json(root / "views" / "b.json", views_payload)
            self._write_json(root / "tdengine" / "c.json", tdengine_payload)

            with self.assertRaises(ValueError):
                schema_registry_loader.load_registry(root)

    def test_load_registry_should_fail_when_index_column_missing(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = pathlib.Path(tmp_dir)
            self._prepare_registry_dirs(root)

            broken = self._build_mysql_object(name="indexed_source")
            broken["indexes"] = [
                {
                    "name": "idx_missing_column",
                    "kind": "INDEX",
                    "columns": ["missing_field"],
                }
            ]
            mysql_payload = {
                "domain": "index-column-domain",
                "storageType": "mysql",
                "objects": [broken],
            }
            views_payload = {
                "domain": "ok-view-domain",
                "storageType": "mysql_view",
                "objects": [self._build_view_object(name="view_ok", source_table="indexed_source")],
            }
            tdengine_payload = {
                "domain": "ok-tdengine-domain",
                "storageType": "tdengine",
                "objects": [self._build_tdengine_object(name="td_ok")],
            }
            self._write_json(root / "mysql" / "a.json", mysql_payload)
            self._write_json(root / "views" / "b.json", views_payload)
            self._write_json(root / "tdengine" / "c.json", tdengine_payload)

            with self.assertRaises(ValueError):
                schema_registry_loader.load_registry(root)

    def test_load_registry_should_fail_on_malformed_source_tables_type(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = pathlib.Path(tmp_dir)
            self._prepare_registry_dirs(root)

            mysql_payload = {
                "domain": "source-table-domain",
                "storageType": "mysql",
                "objects": [self._build_mysql_object(name="source_table")],
            }
            broken_view = self._build_view_object(name="view_bad", source_table="source_table")
            broken_view["sourceTables"] = "source_table"
            views_payload = {
                "domain": "broken-view-domain",
                "storageType": "mysql_view",
                "objects": [broken_view],
            }
            tdengine_payload = {
                "domain": "ok-tdengine-domain",
                "storageType": "tdengine",
                "objects": [self._build_tdengine_object(name="td_ok")],
            }
            self._write_json(root / "mysql" / "a.json", mysql_payload)
            self._write_json(root / "views" / "b.json", views_payload)
            self._write_json(root / "tdengine" / "c.json", tdengine_payload)

            with self.assertRaises(ValueError):
                schema_registry_loader.load_registry(root)

    def test_load_registry_should_fail_on_placeholder_metadata(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = pathlib.Path(tmp_dir)
            self._prepare_registry_dirs(root)

            broken = self._build_mysql_object(name="placeholder_object")
            broken["tableCommentZh"] = "TODO"
            mysql_payload = {
                "domain": "placeholder-domain",
                "storageType": "mysql",
                "objects": [broken],
            }
            views_payload = {
                "domain": "ok-view-domain",
                "storageType": "mysql_view",
                "objects": [self._build_view_object(name="view_ok", source_table="placeholder_object")],
            }
            tdengine_payload = {
                "domain": "ok-tdengine-domain",
                "storageType": "tdengine",
                "objects": [self._build_tdengine_object(name="td_ok")],
            }
            self._write_json(root / "mysql" / "a.json", mysql_payload)
            self._write_json(root / "views" / "b.json", views_payload)
            self._write_json(root / "tdengine" / "c.json", tdengine_payload)

            with self.assertRaises(ValueError):
                schema_registry_loader.load_registry(root)

    def test_load_registry_should_fail_when_archived_object_keeps_active_flags(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = pathlib.Path(tmp_dir)
            self._prepare_registry_dirs(root)

            archived = self._build_mysql_object(name="archived_object", lifecycle="archived")
            mysql_payload = {
                "domain": "archived-domain",
                "storageType": "mysql",
                "objects": [archived],
            }
            views_payload = {
                "domain": "ok-view-domain",
                "storageType": "mysql_view",
                "objects": [self._build_view_object(name="view_ok", source_table="archived_object")],
            }
            tdengine_payload = {
                "domain": "ok-tdengine-domain",
                "storageType": "tdengine",
                "objects": [self._build_tdengine_object(name="td_ok")],
            }
            self._write_json(root / "mysql" / "a.json", mysql_payload)
            self._write_json(root / "views" / "b.json", views_payload)
            self._write_json(root / "tdengine" / "c.json", tdengine_payload)

            with self.assertRaises(ValueError):
                schema_registry_loader.load_registry(root)

    def _assert_no_placeholder_text(self, text: str):
        for pattern in PLACEHOLDER_PATTERNS:
            self.assertNotIn(pattern, text)

    def _prepare_registry_dirs(self, root: pathlib.Path):
        (root / "mysql").mkdir(parents=True, exist_ok=True)
        (root / "views").mkdir(parents=True, exist_ok=True)
        (root / "tdengine").mkdir(parents=True, exist_ok=True)

    def _write_json(self, path: pathlib.Path, payload: dict):
        path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")

    def _build_mysql_object(
        self,
        name: str,
        lifecycle: str = "active",
        relations: list[dict] | None = None,
    ) -> dict:
        if relations is None:
            relations = []
        return {
            "name": name,
            "storageType": "mysql_table",
            "ownerModule": "spring-boot-iot-device",
            "lifecycle": lifecycle,
            "includedInInit": True,
            "includedInSchemaSync": True,
            "runtimeBootstrapMode": "schema_sync_managed",
            "tableCommentZh": "示例数据表",
            "commentZh": "示例数据表",
            "fields": [
                {
                    "name": "tenant_id",
                    "type": "BIGINT NOT NULL",
                    "commentZh": "租户ID",
                }
            ],
            "indexes": [],
            "relations": relations,
            "lineageRole": "domain_master_data",
            "businessBoundary": "用于示例校验的数据边界。",
        }

    def _build_view_object(self, name: str, source_table: str) -> dict:
        return {
            "name": name,
            "storageType": "mysql_view",
            "ownerModule": "spring-boot-iot-device",
            "lifecycle": "active",
            "includedInInit": True,
            "includedInSchemaSync": True,
            "runtimeBootstrapMode": "view_only",
            "tableCommentZh": "示例兼容视图",
            "commentZh": "示例兼容视图",
            "fields": [
                {
                    "name": "tenant_id",
                    "type": "VIEW_COLUMN",
                    "commentZh": "租户ID",
                }
            ],
            "indexes": [],
            "relations": [
                {
                    "type": "derived_from",
                    "target": source_table,
                    "viaField": "view_select",
                }
            ],
            "lineageRole": "compatibility_projection",
            "businessBoundary": "用于示例视图兼容读取的边界。",
            "sourceTables": [source_table],
            "definitionSql": f"SELECT tenant_id FROM {source_table}",
        }

    def _build_tdengine_object(self, name: str) -> dict:
        return {
            "name": name,
            "storageType": "tdengine_table",
            "ownerModule": "spring-boot-iot-telemetry",
            "lifecycle": "active",
            "includedInInit": True,
            "includedInSchemaSync": True,
            "runtimeBootstrapMode": "auto_bootstrap",
            "tableCommentZh": "示例时序表",
            "commentZh": "示例时序表",
            "fields": [
                {
                    "name": "tenant_id",
                    "type": "BIGINT",
                    "commentZh": "租户ID",
                    "isTag": False,
                }
            ],
            "indexes": [],
            "relations": [],
            "lineageRole": "telemetry_compatibility_fallback",
            "businessBoundary": "用于示例时序写入边界。",
        }


if __name__ == "__main__":
    unittest.main()
