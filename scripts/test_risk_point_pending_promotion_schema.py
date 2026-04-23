import unittest

from scripts.schema_contract_test_support import (
    extract_create_table_statement,
    get_schema_sync_create_entry,
    read_init_sql,
)


class RiskPointPendingPromotionSchemaTest(unittest.TestCase):
    def assert_contains_all(self, content: str, snippets: list[str]) -> None:
        for snippet in snippets:
            self.assertIn(snippet, content)

    def extract_schema_sync_create_sql(self, table_name: str) -> str:
        entry = get_schema_sync_create_entry(table_name)
        self.assertEqual("active", entry["lifecycle"])
        return entry["sql"]

    def assert_pending_binding_baseline(self, block: str, create_snippet: str) -> None:
        self.assert_contains_all(
            block,
            [
                create_snippet,
                "resolution_status VARCHAR(64) NOT NULL DEFAULT 'PENDING_METRIC_GOVERNANCE'",
                "metric_identifier VARCHAR(64) DEFAULT NULL",
                "UNIQUE KEY uk_pending_binding_batch_row (tenant_id, batch_no, source_row_no)",
                "KEY idx_pending_binding_status (tenant_id, resolution_status, deleted)",
                "KEY idx_pending_binding_risk_device (risk_point_id, device_id, deleted)",
                "KEY idx_pending_binding_device_code (tenant_id, device_code, deleted)",
            ],
        )

    def assert_pending_promotion_baseline(self, block: str, create_snippet: str) -> None:
        self.assert_contains_all(
            block,
            [
                create_snippet,
                "pending_binding_id BIGINT NOT NULL",
                "risk_point_device_id BIGINT DEFAULT NULL",
                "risk_point_id BIGINT NOT NULL",
                "device_id BIGINT NOT NULL",
                "metric_identifier VARCHAR(64) NOT NULL",
                "promotion_status VARCHAR(32) NOT NULL",
                "tenant_id BIGINT NOT NULL DEFAULT 1",
                "deleted TINYINT NOT NULL DEFAULT 0",
                "evidence_snapshot_json JSON DEFAULT NULL",
                "KEY idx_pending_promotion_pending_id (pending_binding_id)",
                "KEY idx_pending_promotion_binding_id (risk_point_device_id)",
                "KEY idx_pending_promotion_status (tenant_id, promotion_status, deleted)",
            ],
        )

    def test_init_sql_contains_pending_promotion_table(self) -> None:
        block = extract_create_table_statement(
            read_init_sql(),
            "risk_point_device_pending_promotion",
        )
        self.assert_pending_promotion_baseline(
            block,
            "CREATE TABLE risk_point_device_pending_promotion",
        )

    def test_schema_sync_contains_pending_promotion_table(self) -> None:
        block = self.extract_schema_sync_create_sql("risk_point_device_pending_promotion")
        self.assert_pending_promotion_baseline(
            block,
            "CREATE TABLE risk_point_device_pending_promotion",
        )

    def test_init_sql_contains_pending_binding_table(self) -> None:
        block = extract_create_table_statement(
            read_init_sql(),
            "risk_point_device_pending_binding",
        )
        self.assert_pending_binding_baseline(
            block,
            "CREATE TABLE risk_point_device_pending_binding",
        )

    def test_schema_sync_contains_pending_binding_table(self) -> None:
        block = self.extract_schema_sync_create_sql("risk_point_device_pending_binding")
        self.assert_pending_binding_baseline(
            block,
            "CREATE TABLE risk_point_device_pending_binding",
        )


if __name__ == "__main__":
    unittest.main()
