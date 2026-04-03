import pathlib
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[1]
INIT_SQL = ROOT / "sql" / "init.sql"
SCHEMA_SYNC = ROOT / "scripts" / "run-real-env-schema-sync.py"


class RiskPointPendingPromotionSchemaTest(unittest.TestCase):
    def assert_contains_all(self, content: str, snippets: list[str]) -> None:
        for snippet in snippets:
            self.assertIn(snippet, content)

    def assert_pending_binding_baseline(self, content: str) -> None:
        self.assert_contains_all(
            content,
            [
                "risk_point_device_pending_binding",
                "resolution_status VARCHAR(64) NOT NULL DEFAULT 'PENDING_METRIC_GOVERNANCE'",
                "metric_identifier VARCHAR(64) DEFAULT NULL",
                "UNIQUE KEY uk_pending_binding_batch_row (tenant_id, batch_no, source_row_no)",
                "KEY idx_pending_binding_status (tenant_id, resolution_status, deleted)",
                "KEY idx_pending_binding_risk_device (risk_point_id, device_id, deleted)",
                "KEY idx_pending_binding_device_code (tenant_id, device_code, deleted)",
            ],
        )

    def assert_pending_promotion_baseline(self, content: str) -> None:
        self.assert_contains_all(
            content,
            [
                "risk_point_device_pending_promotion",
                "risk_point_device_id BIGINT",
                "evidence_snapshot_json JSON",
                "KEY idx_pending_promotion_binding_id",
                "KEY idx_pending_promotion_status",
            ],
        )

    def test_init_sql_contains_pending_promotion_table(self) -> None:
        content = INIT_SQL.read_text(encoding="utf-8")
        self.assertIn("CREATE TABLE risk_point_device_pending_promotion", content)
        self.assert_pending_promotion_baseline(content)

    def test_schema_sync_contains_pending_promotion_table(self) -> None:
        content = SCHEMA_SYNC.read_text(encoding="utf-8")
        self.assertIn('"risk_point_device_pending_promotion": """', content)
        self.assertIn(
            "CREATE TABLE IF NOT EXISTS risk_point_device_pending_promotion",
            content,
        )
        self.assert_pending_promotion_baseline(content)

    def test_init_sql_contains_pending_binding_table(self) -> None:
        content = INIT_SQL.read_text(encoding="utf-8")
        self.assertIn("CREATE TABLE risk_point_device_pending_binding", content)
        self.assert_pending_binding_baseline(content)

    def test_schema_sync_contains_pending_binding_table(self) -> None:
        content = SCHEMA_SYNC.read_text(encoding="utf-8")
        self.assertIn('"risk_point_device_pending_binding": """', content)
        self.assertIn(
            "CREATE TABLE IF NOT EXISTS risk_point_device_pending_binding",
            content,
        )
        self.assert_pending_binding_baseline(content)


if __name__ == "__main__":
    unittest.main()
