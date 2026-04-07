import importlib.util
import pathlib
import unittest


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


if __name__ == "__main__":
    unittest.main()
