import importlib.util
import pathlib
import unittest
from unittest import mock


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


class SchemaSyncCoverageTest(unittest.TestCase):
    def test_create_table_sql_covers_device_relation_table(self):
        create_sql = schema_sync.CREATE_TABLE_SQL.get("iot_device_relation")
        self.assertIsNotNone(create_sql)
        self.assertIn("CREATE TABLE IF NOT EXISTS iot_device_relation", create_sql)
        self.assertIn("idx_relation_parent_code", create_sql)
        self.assertIn("idx_relation_child_code", create_sql)

    def test_create_table_sql_covers_governance_approval_tables(self):
        order_sql = schema_sync.CREATE_TABLE_SQL.get("sys_governance_approval_order")
        transition_sql = schema_sync.CREATE_TABLE_SQL.get("sys_governance_approval_transition")
        self.assertIsNotNone(order_sql)
        self.assertIsNotNone(transition_sql)
        self.assertIn("CREATE TABLE IF NOT EXISTS sys_governance_approval_order", order_sql)
        self.assertIn("idx_governance_approval_order_subject", order_sql)
        self.assertIn("CREATE TABLE IF NOT EXISTS sys_governance_approval_transition", transition_sql)
        self.assertIn("idx_governance_approval_transition_order", transition_sql)

    def test_product_metadata_json_column_is_declared_for_schema_sync(self):
        self.assertIn("iot_product", schema_sync.COLUMNS_TO_ADD)
        self.assertIn(
            ("metadata_json", "JSON DEFAULT NULL COMMENT '产品扩展元数据'"),
            schema_sync.COLUMNS_TO_ADD["iot_product"],
        )

    def test_create_table_sql_covers_risk_metric_binding_tables(self):
        linkage_sql = schema_sync.CREATE_TABLE_SQL.get("risk_metric_linkage_binding")
        plan_sql = schema_sync.CREATE_TABLE_SQL.get("risk_metric_emergency_plan_binding")
        self.assertIsNotNone(linkage_sql)
        self.assertIsNotNone(plan_sql)
        self.assertIn("CREATE TABLE IF NOT EXISTS risk_metric_linkage_binding", linkage_sql)
        self.assertIn("uk_risk_metric_linkage_active", linkage_sql)
        self.assertIn("idx_risk_metric_linkage_metric", linkage_sql)
        self.assertIn("CREATE TABLE IF NOT EXISTS risk_metric_emergency_plan_binding", plan_sql)
        self.assertIn("uk_risk_metric_plan_active", plan_sql)
        self.assertIn("idx_risk_metric_plan_metric", plan_sql)

    def test_indexes_to_add_covers_risk_metric_binding_tables(self):
        self.assertIn("risk_metric_linkage_binding", schema_sync.INDEXES_TO_ADD)
        self.assertIn("risk_metric_emergency_plan_binding", schema_sync.INDEXES_TO_ADD)
        linkage_index_sql = dict(schema_sync.INDEXES_TO_ADD["risk_metric_linkage_binding"])
        plan_index_sql = dict(schema_sync.INDEXES_TO_ADD["risk_metric_emergency_plan_binding"])
        self.assertEqual(
            linkage_index_sql["idx_risk_metric_linkage_rule"],
            "ALTER TABLE `risk_metric_linkage_binding` ADD INDEX `idx_risk_metric_linkage_rule` (`linkage_rule_id`, `binding_status`, `deleted`)",
        )
        self.assertEqual(
            linkage_index_sql["idx_risk_metric_linkage_metric"],
            "ALTER TABLE `risk_metric_linkage_binding` ADD INDEX `idx_risk_metric_linkage_metric` (`risk_metric_id`, `binding_status`, `deleted`)",
        )
        self.assertEqual(
            plan_index_sql["idx_risk_metric_plan_rule"],
            "ALTER TABLE `risk_metric_emergency_plan_binding` ADD INDEX `idx_risk_metric_plan_rule` (`emergency_plan_id`, `binding_status`, `deleted`)",
        )
        self.assertEqual(
            plan_index_sql["idx_risk_metric_plan_metric"],
            "ALTER TABLE `risk_metric_emergency_plan_binding` ADD INDEX `idx_risk_metric_plan_metric` (`risk_metric_id`, `binding_status`, `deleted`)",
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


if __name__ == "__main__":
    unittest.main()
