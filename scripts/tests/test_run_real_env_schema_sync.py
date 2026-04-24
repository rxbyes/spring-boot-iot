import importlib.util
import pathlib
import unittest
from unittest import mock

import scripts.schema_contract_test_support as schema_contract_support


SCRIPT_PATH = pathlib.Path(__file__).resolve().parents[1] / "run-real-env-schema-sync.py"
SPEC = importlib.util.spec_from_file_location("schema_sync", SCRIPT_PATH)
schema_sync = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(schema_sync)


class NormalizeColorCaseTest(unittest.TestCase):
    def test_normalize_color_case_maps_legacy_and_runtime_values(self):
        sql = schema_sync.normalize_color_case("alarm_level")
        self.assertIn("WHEN 'critical' THEN 'red'", sql)
        self.assertIn("WHEN 'warning' THEN 'orange'", sql)
        self.assertIn("WHEN 'medium' THEN 'yellow'", sql)
        self.assertIn("WHEN 'low' THEN 'blue'", sql)


class DictTargetDefinitionTest(unittest.TestCase):
    def test_alarm_level_targets_only_keep_four_color_values(self):
        targets = schema_sync.level_dict_targets()["alarm_level"]["target_items"]
        self.assertEqual([item[0] for item in targets], ["red", "orange", "yellow", "blue"])
        self.assertEqual(targets[0][4], ["critical"])
        self.assertEqual(targets[1][4], ["warning", "high"])
        self.assertEqual(targets[2][4], ["medium"])
        self.assertEqual(targets[3][4], ["info", "low"])

    def test_risk_level_targets_do_not_keep_legacy_values_as_visible_items(self):
        targets = schema_sync.level_dict_targets()["risk_level"]["target_items"]
        self.assertEqual([item[0] for item in targets], ["red", "orange", "yellow", "blue"])
        flattened_legacy = [legacy for _, _, _, _, legacy_values, _ in targets for legacy in legacy_values]
        self.assertCountEqual(flattened_legacy, ["critical", "warning", "info"])

    def test_system_governance_dict_targets_are_authoritative(self):
        targets = schema_sync.system_governance_dict_targets()
        self.assertEqual(
            [item[0] for item in targets["help_doc_category"]["target_items"]],
            ["business", "technical", "faq"],
        )
        self.assertEqual(
            [item[0] for item in targets["notification_channel_type"]["target_items"]],
            ["email", "sms", "webhook", "wechat", "feishu", "dingtalk"],
        )


class GeneratedSchemaManifestTest(unittest.TestCase):
    def test_schema_sync_reads_generated_manifest_for_structural_objects(self):
        manifest = schema_sync.load_schema_sync_manifest()
        self.assertIn("sys_governance_approval_policy", manifest["createTableSql"])
        self.assertIn("iot_device_relation", manifest["createTableSql"])
        self.assertNotIn("risk_point_highway_detail", manifest["createTableSql"])
        self.assertIn("iot_message_log", manifest["viewSql"])
        self.assertEqual(
            manifest["tableLifecycle"]["risk_point_highway_detail"],
            "archived",
        )

    def test_module_level_maps_are_derived_from_generated_manifest(self):
        manifest = schema_sync.load_schema_sync_manifest()
        self.assertEqual(schema_sync.CREATE_TABLE_SQL, manifest["createTableSql"])
        self.assertEqual(schema_sync.COLUMNS_TO_ADD, manifest["columnsToAdd"])
        self.assertEqual(schema_sync.INDEXES_TO_ADD, manifest["indexesToAdd"])
        self.assertEqual(schema_sync.VIEW_SQL, manifest["viewSql"])


class SchemaSyncCoverageTest(unittest.TestCase):
    def test_create_table_sql_covers_device_relation_table(self):
        self.assertIn("iot_device_relation", schema_sync.CREATE_TABLE_SQL)
        entry = schema_contract_support.get_schema_sync_create_entry("iot_device_relation")
        indexes = schema_contract_support.get_schema_sync_indexes("iot_device_relation")
        self.assertEqual("active", entry["lifecycle"])
        self.assertEqual(
            ("tenant_id", "parent_device_code", "logical_channel_code", "deleted"),
            indexes["uk_relation_parent_code_channel"]["columns"],
        )
        self.assertEqual("UNIQUE", indexes["uk_relation_parent_code_channel"]["kind"])
        self.assertEqual(
            ("tenant_id", "parent_device_code", "enabled", "deleted"),
            indexes["idx_relation_parent_code"]["columns"],
        )
        self.assertEqual(
            ("tenant_id", "child_device_code", "enabled", "deleted"),
            indexes["idx_relation_child_code"]["columns"],
        )

    def test_create_table_sql_covers_governance_approval_policy_table(self):
        self.assertIn("sys_governance_approval_policy", schema_sync.CREATE_TABLE_SQL)
        entry = schema_contract_support.get_schema_sync_create_entry("sys_governance_approval_policy")
        indexes = schema_contract_support.get_schema_sync_indexes("sys_governance_approval_policy")
        self.assertEqual("active", entry["lifecycle"])
        self.assertEqual(
            ("scope_type", "tenant_id", "action_code", "deleted"),
            indexes["uk_governance_approval_policy_scope_action"]["columns"],
        )
        self.assertEqual("UNIQUE", indexes["uk_governance_approval_policy_scope_action"]["kind"])

    def test_create_table_sql_covers_governance_approval_tables(self):
        order_entry = schema_contract_support.get_schema_sync_create_entry("sys_governance_approval_order")
        transition_entry = schema_contract_support.get_schema_sync_create_entry("sys_governance_approval_transition")
        replay_feedback_entry = schema_contract_support.get_schema_sync_create_entry("sys_governance_replay_feedback")
        order_columns = schema_contract_support.get_schema_sync_columns("sys_governance_approval_order")
        order_indexes = schema_contract_support.get_schema_sync_indexes("sys_governance_approval_order")
        transition_indexes = schema_contract_support.get_schema_sync_indexes("sys_governance_approval_transition")
        replay_feedback_indexes = schema_contract_support.get_schema_sync_indexes("sys_governance_replay_feedback")
        self.assertEqual("active", order_entry["lifecycle"])
        self.assertEqual("active", transition_entry["lifecycle"])
        self.assertEqual("active", replay_feedback_entry["lifecycle"])
        self.assertIn("work_item_id", order_columns)
        self.assertEqual(
            ("subject_type", "subject_id", "deleted"),
            order_indexes["idx_governance_approval_order_subject"]["columns"],
        )
        self.assertEqual(
            ("order_id", "create_time", "deleted"),
            transition_indexes["idx_governance_approval_transition_order"]["columns"],
        )
        self.assertEqual(
            ("work_item_id", "create_time", "deleted"),
            replay_feedback_indexes["idx_governance_replay_feedback_work_item"]["columns"],
        )
        self.assertEqual(
            ("release_batch_id", "create_time", "deleted"),
            replay_feedback_indexes["idx_governance_replay_feedback_release_batch"]["columns"],
        )

    def test_columns_to_add_cover_governance_approval_order_work_item_link(self):
        self.assertIn("sys_governance_approval_order", schema_sync.COLUMNS_TO_ADD)
        work_item_column = schema_contract_support.get_schema_sync_columns("sys_governance_approval_order")[
            "work_item_id"
        ]
        self.assertEqual("BIGINT DEFAULT NULL", work_item_column["definition"])
        self.assertEqual("工作项ID", work_item_column["commentZh"])

    def test_product_metadata_json_column_is_declared_for_schema_sync(self):
        self.assertIn("iot_product", schema_sync.COLUMNS_TO_ADD)
        metadata_json_column = schema_contract_support.get_schema_sync_columns("iot_product")["metadata_json"]
        self.assertEqual("JSON DEFAULT NULL", metadata_json_column["definition"])
        self.assertEqual("产品扩展元数据", metadata_json_column["commentZh"])

    def test_create_table_sql_covers_risk_metric_binding_tables(self):
        self.assertIn("risk_metric_linkage_binding", schema_sync.CREATE_TABLE_SQL)
        self.assertIn("risk_metric_emergency_plan_binding", schema_sync.CREATE_TABLE_SQL)
        linkage_indexes = schema_contract_support.get_schema_sync_indexes("risk_metric_linkage_binding")
        plan_indexes = schema_contract_support.get_schema_sync_indexes("risk_metric_emergency_plan_binding")
        self.assertEqual(
            ("tenant_id", "risk_metric_id", "linkage_rule_id", "deleted"),
            linkage_indexes["uk_risk_metric_linkage_active"]["columns"],
        )
        self.assertEqual(
            ("risk_metric_id", "binding_status", "deleted"),
            linkage_indexes["idx_risk_metric_linkage_metric"]["columns"],
        )
        self.assertEqual(
            ("tenant_id", "risk_metric_id", "emergency_plan_id", "deleted"),
            plan_indexes["uk_risk_metric_plan_active"]["columns"],
        )
        self.assertEqual(
            ("risk_metric_id", "binding_status", "deleted"),
            plan_indexes["idx_risk_metric_plan_metric"]["columns"],
        )

    def test_indexes_to_add_covers_risk_metric_binding_tables(self):
        self.assertIn("risk_metric_linkage_binding", schema_sync.INDEXES_TO_ADD)
        self.assertIn("risk_metric_emergency_plan_binding", schema_sync.INDEXES_TO_ADD)
        self.assertEqual(
            (True, ("tenant_id", "risk_metric_id", "linkage_rule_id", "deleted")),
            schema_sync.EXPECTED_INDEX_SHAPES[("risk_metric_linkage_binding", "uk_risk_metric_linkage_active")],
        )
        self.assertEqual(
            (False, ("linkage_rule_id", "binding_status", "deleted")),
            schema_sync.EXPECTED_INDEX_SHAPES[("risk_metric_linkage_binding", "idx_risk_metric_linkage_rule")],
        )
        self.assertEqual(
            (False, ("risk_metric_id", "binding_status", "deleted")),
            schema_sync.EXPECTED_INDEX_SHAPES[("risk_metric_linkage_binding", "idx_risk_metric_linkage_metric")],
        )
        self.assertEqual(
            (True, ("tenant_id", "risk_metric_id", "emergency_plan_id", "deleted")),
            schema_sync.EXPECTED_INDEX_SHAPES[("risk_metric_emergency_plan_binding", "uk_risk_metric_plan_active")],
        )
        self.assertEqual(
            (False, ("emergency_plan_id", "binding_status", "deleted")),
            schema_sync.EXPECTED_INDEX_SHAPES[("risk_metric_emergency_plan_binding", "idx_risk_metric_plan_rule")],
        )
        self.assertEqual(
            (False, ("risk_metric_id", "binding_status", "deleted")),
            schema_sync.EXPECTED_INDEX_SHAPES[("risk_metric_emergency_plan_binding", "idx_risk_metric_plan_metric")],
        )

    def test_create_table_sql_covers_governance_control_plane_tables(self):
        self.assertIn("iot_governance_work_item", schema_sync.CREATE_TABLE_SQL)
        self.assertIn("iot_governance_ops_alert", schema_sync.CREATE_TABLE_SQL)
        work_item_columns = schema_contract_support.get_schema_sync_columns("iot_governance_work_item")
        ops_alert_columns = schema_contract_support.get_schema_sync_columns("iot_governance_ops_alert")
        self.assertTrue({"work_status", "task_category", "execution_status"}.issubset(work_item_columns.keys()))
        self.assertIn("alert_status", ops_alert_columns)

    def test_create_table_sql_covers_product_metric_resolver_snapshot_table(self):
        self.assertIn("iot_product_metric_resolver_snapshot", schema_sync.CREATE_TABLE_SQL)
        snapshot_columns = schema_contract_support.get_schema_sync_columns("iot_product_metric_resolver_snapshot")
        snapshot_indexes = schema_contract_support.get_schema_sync_indexes("iot_product_metric_resolver_snapshot")
        self.assertIn("release_batch_id", snapshot_columns)
        self.assertEqual(
            ("product_id", "release_batch_id", "deleted"),
            snapshot_indexes["uk_metric_resolver_snapshot_batch"]["columns"],
        )
        self.assertEqual("UNIQUE", snapshot_indexes["uk_metric_resolver_snapshot_batch"]["kind"])

    def test_columns_to_add_covers_governance_work_item_lifecycle_hub_fields(self):
        self.assertIn("iot_governance_work_item", schema_sync.COLUMNS_TO_ADD)
        work_item_columns = dict(schema_sync.COLUMNS_TO_ADD["iot_governance_work_item"])
        self.assertTrue(
            {
                "task_category",
                "domain_code",
                "action_code",
                "execution_status",
                "recommendation_snapshot_json",
                "evidence_snapshot_json",
                "impact_snapshot_json",
                "rollback_snapshot_json",
            }.issubset(work_item_columns.keys())
        )
        self.assertEqual(
            work_item_columns["execution_status"],
            "VARCHAR(64) DEFAULT NULL COMMENT '生命周期执行状态'",
        )

    def test_indexes_to_add_covers_governance_control_plane_tables(self):
        self.assertIn("iot_governance_work_item", schema_sync.INDEXES_TO_ADD)
        self.assertIn("iot_governance_ops_alert", schema_sync.INDEXES_TO_ADD)
        self.assertEqual(
            (False, ("subject_type", "subject_id", "work_status", "deleted")),
            schema_sync.EXPECTED_INDEX_SHAPES[("iot_governance_work_item", "idx_governance_work_item_subject")],
        )
        self.assertEqual(
            (True, ("tenant_id", "alert_type", "alert_code", "deleted")),
            schema_sync.EXPECTED_INDEX_SHAPES[("iot_governance_ops_alert", "uk_governance_ops_alert_code")],
        )

    def test_indexes_to_add_covers_device_relation_parent_code_uniqueness(self):
        self.assertIn("iot_device_relation", schema_sync.INDEXES_TO_ADD)
        self.assertEqual(
            (True, ("tenant_id", "parent_device_code", "logical_channel_code", "deleted")),
            schema_sync.EXPECTED_INDEX_SHAPES[("iot_device_relation", "uk_relation_parent_code_channel")],
        )

    def test_indexes_to_add_covers_metric_resolver_snapshot_batch_uniqueness(self):
        self.assertIn("iot_product_metric_resolver_snapshot", schema_sync.INDEXES_TO_ADD)
        self.assertEqual(
            (True, ("product_id", "release_batch_id", "deleted")),
            schema_sync.EXPECTED_INDEX_SHAPES[("iot_product_metric_resolver_snapshot", "uk_metric_resolver_snapshot_batch")],
        )

    def test_governance_approval_policy_seeds_cover_fixed_reviewer_defaults(self):
        self.assertEqual(schema_sync.GOVERNANCE_REVIEWER_USERNAME, "governance_reviewer")
        self.assertEqual(schema_sync.GOVERNANCE_REVIEWER_ROLE_CODE, "SUPER_ADMIN")
        self.assertEqual(
            [seed["action_code"] for seed in schema_sync.GOVERNANCE_APPROVAL_POLICY_SEEDS],
            [
                "PRODUCT_CONTRACT_RELEASE_APPLY",
                "PRODUCT_CONTRACT_ROLLBACK",
                "VENDOR_MAPPING_RULE_PUBLISH",
                "VENDOR_MAPPING_RULE_ROLLBACK",
            ],
        )


class FakeCursor:
    def __init__(self):
        self.executed = []
        self._last_sql = ""

    def execute(self, sql, params=None):
        self._last_sql = sql
        self.executed.append((sql, params))

    def fetchone(self):
        if "FROM information_schema.TABLES" in self._last_sql:
            return (1,)
        if "FROM information_schema.COLUMNS" in self._last_sql:
            return (1,)
        if "SELECT COUNT(1) FROM `sys_dict_item`" in self._last_sql:
            return (0,)
        if "SELECT COALESCE(MAX(id), 0) + 1 FROM `sys_dict_item`" in self._last_sql:
            return (8000,)
        if "FROM sys_dict" in self._last_sql:
            return (7202,)
        raise AssertionError(f"Unexpected fetchone for SQL: {self._last_sql}")

    def fetchall(self):
        if "FROM sys_dict_item" in self._last_sql:
            return [
                (7304, "critical"),
                (7305, "warning"),
                (7306, "medium"),
                (7307, "info"),
            ]
        raise AssertionError(f"Unexpected fetchall for SQL: {self._last_sql}")


class DictDuplicateCleanupTest(unittest.TestCase):
    def test_ensure_level_dict_marks_duplicate_dict_rows_deleted(self):
        cursor = FakeCursor()

        schema_sync.ensure_level_dict(
            cursor,
            "rm_iot",
            dict_code="alarm_level",
            **schema_sync.level_dict_targets()["alarm_level"],
        )

        duplicate_cleanup = [
            (sql, params)
            for sql, params in cursor.executed
            if "UPDATE sys_dict" in sql
            and "UPDATE sys_dict_item" not in sql
            and "dict_code = %s" in sql
            and "id <> %s" in sql
        ]
        self.assertEqual(len(duplicate_cleanup), 1)
        cleanup_sql, cleanup_params = duplicate_cleanup[0]
        self.assertIn("deleted = 1", cleanup_sql)
        self.assertEqual(cleanup_params, ("alarm_level", 7202))

    def test_ensure_system_governance_dicts_syncs_two_dict_codes(self):
        cursor = FakeCursor()

        schema_sync.ensure_system_governance_dicts(cursor, "rm_iot")

        dict_select_params = [
            params
            for sql, params in cursor.executed
            if "SELECT id" in sql and "FROM sys_dict" in sql and params is not None
        ]
        self.assertIn(("help_doc_category",), dict_select_params)
        self.assertIn(("notification_channel_type",), dict_select_params)


class MigrateLevelValuesCursor:
    def __init__(self):
        self.executed = []
        self._last_sql = ""

    def execute(self, sql, params=None):
        self._last_sql = sql
        self.executed.append((sql, params))

    def fetchone(self):
        if "FROM information_schema.TABLES" in self._last_sql:
            return (1,)
        if "FROM information_schema.COLUMNS" in self._last_sql:
            return (1,)
        raise AssertionError(f"Unexpected fetchone for SQL: {self._last_sql}")

    def fetchall(self):
        raise AssertionError(f"Unexpected fetchall for SQL: {self._last_sql}")


class MigrateLevelValuesTest(unittest.TestCase):
    def test_migrate_level_values_normalizes_compatibility_risk_level_columns(self):
        cursor = MigrateLevelValuesCursor()

        schema_sync.migrate_level_values(cursor, "rm_iot")

        risk_point_risk_level_updates = [
            sql
            for sql, _ in cursor.executed
            if "UPDATE `risk_point`" in sql and "SET risk_level =" in sql
        ]
        emergency_plan_risk_level_updates = [
            sql
            for sql, _ in cursor.executed
            if "UPDATE `emergency_plan`" in sql and "SET risk_level =" in sql
        ]
        self.assertEqual(len(risk_point_risk_level_updates), 1)
        self.assertIn("WHEN 'warning' THEN 'orange'", risk_point_risk_level_updates[0])
        self.assertEqual(len(emergency_plan_risk_level_updates), 1)
        self.assertIn("WHEN 'critical' THEN 'red'", emergency_plan_risk_level_updates[0])


class LegacyGovernancePermissionCleanupTest(unittest.TestCase):
    def test_cleanup_marks_legacy_governance_write_permissions_deleted(self):
        cursor = FakeCursor()

        schema_sync.ensure_legacy_governance_write_permissions(cursor, "rm_iot")

        update_sql = [sql for sql, _ in cursor.executed if sql.lstrip().startswith("UPDATE")]
        self.assertEqual(len(update_sql), 2)
        self.assertIn("UPDATE sys_role_menu rm", update_sql[0])
        self.assertIn("UPDATE sys_menu", update_sql[1])
        self.assertIn("risk:rule-definition:write", update_sql[0])
        self.assertIn("risk:linkage-rule:write", update_sql[0])
        self.assertIn("risk:emergency-plan:write", update_sql[0])
        self.assertIn("risk:rule-definition:write", update_sql[1])
        self.assertIn("risk:linkage-rule:write", update_sql[1])
        self.assertIn("risk:emergency-plan:write", update_sql[1])


class GovernanceFineGrainedPermissionSeedCursor:
    def __init__(self):
        self.executed = []
        self._last_sql = ""
        self._last_params = None
        self.next_role_menu_id = 97000000

    def execute(self, sql, params=None):
        self._last_sql = sql
        self._last_params = params
        self.executed.append((sql, params))

    def fetchone(self):
        if "SELECT id" in self._last_sql and "FROM sys_menu" in self._last_sql and "menu_code = %s" in self._last_sql:
            code = self._last_params[0]
            if code == "iot:normative-library:approve":
                return None
            if code == "risk:metric-catalog:approve":
                return (93002051,)
            return None
        if "SELECT COUNT(1) FROM `sys_menu` WHERE id = %s" in self._last_sql:
            return (0,)
        if "SELECT id" in self._last_sql and "FROM sys_role" in self._last_sql and "role_code = %s" in self._last_sql:
            role_code = self._last_params[0]
            role_map = {
                "MANAGEMENT_STAFF": (92000002,),
                "OPS_STAFF": (92000003,),
            }
            return role_map.get(role_code)
        if "SELECT id" in self._last_sql and "FROM sys_role_menu" in self._last_sql:
            return None
        if "SELECT COALESCE(MAX(id), 0) + 1 FROM `sys_role_menu`" in self._last_sql:
            self.next_role_menu_id += 1
            return (self.next_role_menu_id,)
        raise AssertionError(f"Unexpected fetchone for SQL: {self._last_sql}")

    def fetchall(self):
        raise AssertionError(f"Unexpected fetchall for SQL: {self._last_sql}")


class GovernanceFineGrainedPermissionSeedTest(unittest.TestCase):
    @mock.patch.object(schema_sync, "column_exists", return_value=True)
    @mock.patch.object(schema_sync, "table_exists", return_value=True)
    def test_seed_aligns_permission_menus_and_role_bindings(self, _mock_table_exists, _mock_column_exists):
        cursor = GovernanceFineGrainedPermissionSeedCursor()

        schema_sync.ensure_governance_fine_grained_permissions(cursor, "rm_iot")

        write_sql = [sql for sql, _ in cursor.executed if sql.lstrip().startswith(("INSERT", "UPDATE"))]
        combined_sql = "\n".join(write_sql)
        self.assertIn("INSERT INTO sys_menu", combined_sql)
        self.assertIn("UPDATE sys_menu", combined_sql)
        self.assertIn("INSERT INTO sys_role_menu", combined_sql)
        params_text = str([params for _, params in cursor.executed if params is not None])
        self.assertIn("iot:normative-library:approve", params_text)
        self.assertIn("risk:metric-catalog:approve", params_text)
        self.assertIn("MANAGEMENT_STAFF", params_text)
        self.assertIn("OPS_STAFF", params_text)


class GovernanceApprovalPolicySeedCursor:
    def __init__(self):
        self.executed = []
        self._last_sql = ""
        self._last_params = None
        self.next_user_role_id = 98000000

    def execute(self, sql, params=None):
        self._last_sql = sql
        self._last_params = params
        self.executed.append((sql, params))

    def fetchone(self):
        if "FROM sys_organization" in self._last_sql:
            return (7101,)
        if "SELECT id" in self._last_sql and "FROM sys_user" in self._last_sql and "username = %s" in self._last_sql:
            return None
        if "SELECT COUNT(1) FROM `sys_user` WHERE id = %s" in self._last_sql:
            return (0,)
        if "SELECT id" in self._last_sql and "FROM sys_role" in self._last_sql and "role_code = %s" in self._last_sql:
            return (92000001,)
        if "SELECT id" in self._last_sql and "FROM sys_user_role" in self._last_sql:
            return None
        if "SELECT COALESCE(MAX(id), 0) + 1 FROM `sys_user_role`" in self._last_sql:
            self.next_user_role_id += 1
            return (self.next_user_role_id,)
        if (
            "SELECT id" in self._last_sql
            and "FROM sys_governance_approval_policy" in self._last_sql
            and "action_code = %s" in self._last_sql
        ):
            return None
        if "SELECT COUNT(1) FROM `sys_governance_approval_policy` WHERE id = %s" in self._last_sql:
            return (0,)
        raise AssertionError(f"Unexpected fetchone for SQL: {self._last_sql}")

    def fetchall(self):
        raise AssertionError(f"Unexpected fetchall for SQL: {self._last_sql}")


class GovernanceApprovalPolicySeedTest(unittest.TestCase):
    @mock.patch.object(schema_sync, "column_exists", return_value=True)
    @mock.patch.object(schema_sync, "table_exists", return_value=True)
    def test_seed_aligns_fixed_reviewer_user_role_and_policies(self, _mock_table_exists, _mock_column_exists):
        cursor = GovernanceApprovalPolicySeedCursor()

        schema_sync.ensure_governance_approval_policy_defaults(cursor, "rm_iot")

        write_sql = [sql for sql, _ in cursor.executed if sql.lstrip().startswith(("INSERT", "UPDATE"))]
        combined_sql = "\n".join(write_sql)
        self.assertIn("INSERT INTO sys_user", combined_sql)
        self.assertIn("INSERT INTO sys_user_role", combined_sql)
        self.assertIn("INSERT INTO sys_governance_approval_policy", combined_sql)
        params_text = str([params for _, params in cursor.executed if params is not None])
        self.assertIn("governance_reviewer", params_text)
        self.assertIn("PRODUCT_CONTRACT_RELEASE_APPLY", params_text)
        self.assertIn("PRODUCT_CONTRACT_ROLLBACK", params_text)
        self.assertIn("SUPER_ADMIN", params_text)


class EnsureIndexesCursor:
    def __init__(self):
        self.executed = []
        self._last_sql = ""
        self._last_params = None

    def execute(self, sql, params=None):
        self._last_sql = sql
        self._last_params = params
        self.executed.append((sql, params))

    def fetchone(self):
        if "risk_metric_linkage_binding" in self._last_sql and "HAVING COUNT(1) > 1" in self._last_sql:
            return None
        if "risk_metric_emergency_plan_binding" in self._last_sql and "HAVING COUNT(1) > 1" in self._last_sql:
            return None
        raise AssertionError(f"Unexpected fetchone for SQL: {self._last_sql}")

    def fetchall(self):
        if "FROM information_schema.STATISTICS" in self._last_sql:
            db, table, index = self._last_params
            if db != "rm_iot":
                raise AssertionError(f"Unexpected db in params: {self._last_params}")
            expected = schema_sync.BINDING_INDEX_EXPECTED_SHAPES.get((table, index))
            if expected is not None:
                is_unique, columns = expected
                non_unique = 0 if is_unique else 1
                return [
                    (non_unique, column, seq)
                    for seq, column in enumerate(columns, start=1)
                ]
            return []
        raise AssertionError(f"Unexpected fetchall for SQL: {self._last_sql}")


class EnsureIndexesBehaviorTest(unittest.TestCase):
    @mock.patch.object(schema_sync, "table_exists", return_value=True)
    def test_ensure_indexes_executes_binding_index_ddl_when_missing(self, _mock_table_exists):
        cursor = EnsureIndexesCursor()

        missing_binding_indexes = {
            ("risk_metric_linkage_binding", "uk_risk_metric_linkage_active"),
            ("risk_metric_linkage_binding", "idx_risk_metric_linkage_rule"),
            ("risk_metric_linkage_binding", "idx_risk_metric_linkage_metric"),
            ("risk_metric_emergency_plan_binding", "uk_risk_metric_plan_active"),
            ("risk_metric_emergency_plan_binding", "idx_risk_metric_plan_rule"),
            ("risk_metric_emergency_plan_binding", "idx_risk_metric_plan_metric"),
        }

        def fake_index_exists(_cur, _db, table, index):
            return (table, index) not in missing_binding_indexes

        with mock.patch.object(schema_sync, "index_exists", side_effect=fake_index_exists):
            schema_sync.ensure_indexes(cursor, "rm_iot")

        executed_sql = {sql for sql, _ in cursor.executed}
        self.assertIn(
            "ALTER TABLE `risk_metric_linkage_binding` ADD UNIQUE INDEX `uk_risk_metric_linkage_active` (`tenant_id`, `risk_metric_id`, `linkage_rule_id`, `deleted`)",
            executed_sql,
        )
        self.assertIn(
            "ALTER TABLE `risk_metric_linkage_binding` ADD INDEX `idx_risk_metric_linkage_rule` (`linkage_rule_id`, `binding_status`, `deleted`)",
            executed_sql,
        )
        self.assertIn(
            "ALTER TABLE `risk_metric_linkage_binding` ADD INDEX `idx_risk_metric_linkage_metric` (`risk_metric_id`, `binding_status`, `deleted`)",
            executed_sql,
        )
        self.assertIn(
            "ALTER TABLE `risk_metric_emergency_plan_binding` ADD UNIQUE INDEX `uk_risk_metric_plan_active` (`tenant_id`, `risk_metric_id`, `emergency_plan_id`, `deleted`)",
            executed_sql,
        )
        self.assertIn(
            "ALTER TABLE `risk_metric_emergency_plan_binding` ADD INDEX `idx_risk_metric_plan_rule` (`emergency_plan_id`, `binding_status`, `deleted`)",
            executed_sql,
        )
        self.assertIn(
            "ALTER TABLE `risk_metric_emergency_plan_binding` ADD INDEX `idx_risk_metric_plan_metric` (`risk_metric_id`, `binding_status`, `deleted`)",
            executed_sql,
        )

    @mock.patch.object(schema_sync, "table_exists", return_value=True)
    def test_ensure_indexes_raises_when_unique_index_has_duplicate_rows(self, _mock_table_exists):
        cursor = EnsureIndexesCursor()

        def fake_index_exists(_cur, _db, table, index):
            return not (table == "risk_metric_linkage_binding" and index == "uk_risk_metric_linkage_active")

        original_fetchone = cursor.fetchone

        def fetchone_with_duplicate():
            if "risk_metric_linkage_binding" in cursor._last_sql and "HAVING COUNT(1) > 1" in cursor._last_sql:
                return (1,)
            return original_fetchone()

        cursor.fetchone = fetchone_with_duplicate

        with mock.patch.object(schema_sync, "index_exists", side_effect=fake_index_exists):
            with self.assertRaises(RuntimeError) as cm:
                schema_sync.ensure_indexes(cursor, "rm_iot")

        self.assertIn("risk_metric_linkage_binding.uk_risk_metric_linkage_active", str(cm.exception))
        self.assertIn("duplicate rows must be cleaned before schema sync can continue", str(cm.exception))
        executed_sql = {sql for sql, _ in cursor.executed}
        self.assertNotIn(
            "ALTER TABLE `risk_metric_linkage_binding` ADD UNIQUE INDEX `uk_risk_metric_linkage_active` (`tenant_id`, `risk_metric_id`, `linkage_rule_id`, `deleted`)",
            executed_sql,
        )

    @mock.patch.object(schema_sync, "table_exists", return_value=True)
    @mock.patch.object(schema_sync, "index_exists", return_value=True)
    def test_ensure_indexes_raises_when_existing_binding_index_shape_drifts(
        self, _mock_index_exists, _mock_table_exists
    ):
        cursor = EnsureIndexesCursor()
        original_fetchall = cursor.fetchall

        def fetchall_with_drift():
            if (
                "FROM information_schema.STATISTICS" in cursor._last_sql
                and cursor._last_params == ("rm_iot", "risk_metric_linkage_binding", "uk_risk_metric_linkage_active")
            ):
                return [
                    (0, "tenant_id", 1),
                    (0, "risk_metric_id", 2),
                    (0, "deleted", 3),
                ]
            return original_fetchall()

        cursor.fetchall = fetchall_with_drift

        with self.assertRaises(RuntimeError) as cm:
            schema_sync.ensure_indexes(cursor, "rm_iot")

        self.assertIn("risk_metric_linkage_binding.uk_risk_metric_linkage_active", str(cm.exception))
        self.assertIn("drifts from expected shape", str(cm.exception))
        executed_sql = {sql for sql, _ in cursor.executed}
        self.assertNotIn(
            "ALTER TABLE `risk_metric_linkage_binding` ADD UNIQUE INDEX `uk_risk_metric_linkage_active` (`tenant_id`, `risk_metric_id`, `linkage_rule_id`, `deleted`)",
            executed_sql,
        )


class CollectorChildBaselineSeedCursor:
    def __init__(self):
        self.executed = []

    def execute(self, sql, params=None):
        self.executed.append((sql, params))


class DeviceRelationIntegrityCursor:
    def __init__(self):
        self.executed = []
        self.rowcount = 0

    def execute(self, sql, params=None):
        self.executed.append((sql, params))
        self.rowcount = 1

    def fetchone(self):
        return (0,)


class CollectorChildBaselineSeedTest(unittest.TestCase):
    @mock.patch.object(schema_sync, "table_exists", return_value=True)
    def test_device_relation_integrity_cleanup_deduplicates_and_realigns_refs(self, _mock_table_exists):
        cursor = DeviceRelationIntegrityCursor()

        schema_sync.ensure_device_relation_integrity(cursor, "rm_iot")

        combined_sql = "\n".join(sql for sql, _ in cursor.executed)
        self.assertIn("DELETE rel", combined_sql)
        self.assertIn("UPDATE iot_device_relation rel", combined_sql)
        self.assertIn("parent_device.device_code = rel.parent_device_code", combined_sql)
        self.assertIn("child_device.device_code = rel.child_device_code", combined_sql)
        self.assertIn("SELECT COUNT(1)", combined_sql)

    @mock.patch.object(schema_sync, "column_exists", return_value=True)
    @mock.patch.object(schema_sync, "table_exists", return_value=True)
    def test_seed_aligns_collector_child_products_devices_relations_and_latest_properties(
        self, _mock_table_exists, _mock_column_exists
    ):
        cursor = CollectorChildBaselineSeedCursor()

        schema_sync.ensure_collector_child_dev_baseline(cursor, "rm_iot")

        write_sql = [sql for sql, _ in cursor.executed if sql.lstrip().startswith(("INSERT", "UPDATE"))]
        combined_sql = "\n".join(write_sql)
        self.assertIn("INSERT INTO iot_product", combined_sql)
        self.assertIn("INSERT INTO iot_product_model", combined_sql)
        self.assertIn("INSERT INTO iot_device", combined_sql)
        self.assertIn("INSERT INTO iot_device_relation", combined_sql)
        self.assertIn("INSERT INTO iot_device_property", combined_sql)
        self.assertIn("UPDATE iot_product", combined_sql)

        params_text = str([params for _, params in cursor.executed if params is not None])
        self.assertIn("nf-collect-rtu-v1", params_text)
        self.assertIn("nf-monitor-laser-rangefinder-v1", params_text)
        self.assertIn("nf-monitor-deep-displacement-v1", params_text)
        self.assertIn("SK00EA0D1307986", params_text)
        self.assertIn("SK00FB0D1310195", params_text)
        self.assertIn("202018143", params_text)
        self.assertIn("84330701", params_text)
        self.assertIn("collector_child", combined_sql)
        self.assertIn("LF_VALUE", params_text)
        self.assertIn("LEGACY", params_text)
        self.assertIn("SENSOR_STATE", params_text)
        self.assertIn("dispsX", params_text)
        self.assertIn("sensor_state", params_text)

    @mock.patch.object(schema_sync, "column_exists", return_value=True)
    @mock.patch.object(schema_sync, "table_exists", return_value=True)
    def test_seed_aligns_deep_displacement_normative_metrics(
        self, _mock_table_exists, _mock_column_exists
    ):
        cursor = CollectorChildBaselineSeedCursor()

        schema_sync.ensure_collector_child_dev_baseline(cursor, "rm_iot")

        write_sql = [sql for sql, _ in cursor.executed if sql.lstrip().startswith(("INSERT", "UPDATE"))]
        combined_sql = "\n".join(write_sql)
        self.assertIn("INSERT INTO iot_normative_metric_definition", combined_sql)

        params_text = str([params for _, params in cursor.executed if params is not None])
        self.assertIn("phase3-deep-displacement", params_text)
        self.assertIn("DEEP_DISPLACEMENT", params_text)
        self.assertIn("dispsX", params_text)
        self.assertIn("dispsY", params_text)
        self.assertIn("sensor_state", params_text)
        self.assertIn("HIGHER_IS_RISKIER", params_text)
        self.assertIn("STATE_IS_RISK", params_text)

    @mock.patch.object(schema_sync, "column_exists", return_value=True)
    @mock.patch.object(schema_sync, "table_exists", return_value=True)
    def test_seed_aligns_rain_gauge_contract_baseline_and_mapping_rules(
        self, _mock_table_exists, _mock_column_exists
    ):
        cursor = CollectorChildBaselineSeedCursor()

        schema_sync.ensure_collector_child_dev_baseline(cursor, "rm_iot")

        write_sql = [sql for sql, _ in cursor.executed if sql.lstrip().startswith(("INSERT", "UPDATE"))]
        combined_sql = "\n".join(write_sql)
        self.assertIn("INSERT INTO iot_vendor_metric_mapping_rule", combined_sql)

        params_text = str([params for _, params in cursor.executed if params is not None])
        self.assertIn("nf-monitor-tipping-bucket-rain-gauge-v1", params_text)
        self.assertIn("phase4-rain-gauge", params_text)
        self.assertIn("RAIN_GAUGE", params_text)
        self.assertIn("totalValue", params_text)
        self.assertIn("L3_YL_1.value", params_text)
        self.assertIn("L3_YL_1.totalValue", params_text)
        self.assertIn("当前雨量", params_text)
        self.assertIn("累计雨量", params_text)

    @mock.patch.object(schema_sync, "column_exists", return_value=True)
    @mock.patch.object(schema_sync, "table_exists", return_value=True)
    def test_seed_aligns_mud_level_normative_metrics(
        self, _mock_table_exists, _mock_column_exists
    ):
        cursor = CollectorChildBaselineSeedCursor()

        schema_sync.ensure_collector_child_dev_baseline(cursor, "rm_iot")

        write_sql = [sql for sql, _ in cursor.executed if sql.lstrip().startswith(("INSERT", "UPDATE"))]
        combined_sql = "\n".join(write_sql)
        self.assertIn("INSERT INTO iot_normative_metric_definition", combined_sql)

        params_text = str([params for _, params in cursor.executed if params is not None])
        self.assertIn("phase5-mud-level", params_text)
        self.assertIn("MUD_LEVEL", params_text)
        self.assertIn("value", params_text)
        self.assertIn("L4", params_text)
        self.assertIn("NW", params_text)
        self.assertIn("泥水位高程", params_text)
        self.assertIn("water_level", params_text)


if __name__ == "__main__":
    unittest.main()
