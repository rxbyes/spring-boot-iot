import pathlib
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[1]
INIT_SQL = ROOT / "sql" / "init.sql"
SCHEMA_SYNC = ROOT / "scripts" / "run-real-env-schema-sync.py"


class RiskPointPendingPromotionSchemaTest(unittest.TestCase):
    def test_init_sql_contains_pending_promotion_table(self) -> None:
        content = INIT_SQL.read_text(encoding="utf-8")
        self.assertIn("CREATE TABLE risk_point_device_pending_promotion", content)
        self.assertIn("KEY idx_pending_promotion_pending_id", content)

    def test_schema_sync_contains_pending_promotion_table(self) -> None:
        content = SCHEMA_SYNC.read_text(encoding="utf-8")
        self.assertIn('"risk_point_device_pending_promotion": """', content)
        self.assertIn(
            "CREATE TABLE IF NOT EXISTS risk_point_device_pending_promotion",
            content,
        )


if __name__ == "__main__":
    unittest.main()
