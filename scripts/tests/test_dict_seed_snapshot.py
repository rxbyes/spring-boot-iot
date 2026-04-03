import pathlib
import unittest


INIT_DATA_SQL = pathlib.Path(__file__).resolve().parents[2] / "sql" / "init-data.sql"


class DictSeedSnapshotTest(unittest.TestCase):
    def test_seed_contains_current_level_dict_definitions(self):
        content = INIT_DATA_SQL.read_text(encoding="utf-8")
        self.assertIn("'risk_point_level'", content)
        self.assertIn("'level_1'", content)
        self.assertIn("'alarm_level'", content)
        self.assertIn("'red'", content)
        self.assertIn("'risk_level'", content)
        self.assertIn("'黄色'", content)

    def test_seed_soft_deletes_duplicate_level_dict_rows_and_non_target_items(self):
        content = INIT_DATA_SQL.read_text(encoding="utf-8")
        self.assertIn("UPDATE sys_dict", content)
        self.assertIn("dict_code IN ('risk_point_level', 'alarm_level', 'risk_level')", content)
        self.assertIn("UPDATE sys_dict_item", content)
        self.assertIn("item_value NOT IN ('level_1', 'level_2', 'level_3')", content)
        self.assertIn("item_value NOT IN ('red', 'orange', 'yellow', 'blue')", content)


if __name__ == "__main__":
    unittest.main()
