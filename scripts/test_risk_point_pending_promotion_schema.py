import pathlib
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[1]
INIT_SQL = ROOT / "sql" / "init.sql"
SCHEMA_SYNC = ROOT / "scripts" / "run-real-env-schema-sync.py"


class RiskPointPendingPromotionSchemaTest(unittest.TestCase):
    def assert_contains_all(self, content: str, snippets: list[str]) -> None:
        for snippet in snippets:
            self.assertIn(snippet, content)

    def test_init_sql_contains_pending_promotion_table(self) -> None:
        content = INIT_SQL.read_text(encoding="utf-8")
        self.assert_contains_all(
            content,
            [
                "CREATE TABLE risk_point_device_pending_promotion",
                "risk_point_device_id BIGINT",
                "evidence_snapshot_json JSON",
                "KEY idx_pending_promotion_pending_id",
                "KEY idx_pending_promotion_binding_id",
                "KEY idx_pending_promotion_status",
            ],
        )

    def test_schema_sync_contains_pending_promotion_table(self) -> None:
        content = SCHEMA_SYNC.read_text(encoding="utf-8")
        self.assert_contains_all(
            content,
            [
                '"risk_point_device_pending_promotion": """',
                "CREATE TABLE IF NOT EXISTS risk_point_device_pending_promotion",
                "risk_point_device_id BIGINT",
                "evidence_snapshot_json JSON",
                "KEY idx_pending_promotion_pending_id",
                "KEY idx_pending_promotion_binding_id",
                "KEY idx_pending_promotion_status",
            ],
        )

    def test_init_sql_contains_pending_binding_table(self) -> None:
        content = INIT_SQL.read_text(encoding="utf-8")
        self.assert_contains_all(
            content,
            [
                "CREATE TABLE risk_point_device_pending_binding",
                "KEY uk_pending_binding_batch_row",
                "KEY idx_pending_binding_status",
                "KEY idx_pending_binding_risk_device",
                "KEY idx_pending_binding_device_code",
            ],
        )

    def test_schema_sync_contains_pending_binding_table(self) -> None:
        content = SCHEMA_SYNC.read_text(encoding="utf-8")
        self.assert_contains_all(
            content,
            [
                '"risk_point_device_pending_binding": """',
                "CREATE TABLE IF NOT EXISTS risk_point_device_pending_binding",
                "KEY uk_pending_binding_batch_row",
                "KEY idx_pending_binding_status",
                "KEY idx_pending_binding_risk_device",
                "KEY idx_pending_binding_device_code",
            ],
        )


if __name__ == "__main__":
    unittest.main()
