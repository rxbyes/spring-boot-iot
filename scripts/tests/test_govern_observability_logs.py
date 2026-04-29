import importlib.util
import json
import pathlib
import tempfile
import unittest
from unittest import mock


SCRIPT_PATH = pathlib.Path(__file__).resolve().parents[1] / "govern-observability-logs.py"
SPEC = importlib.util.spec_from_file_location("observability_log_governance", SCRIPT_PATH)
observability_log_governance = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(observability_log_governance)


def create_snapshot_fixture():
    return {
        "generatedAt": "2026-04-25T22:30:00",
        "mode": "DRY_RUN",
        "tables": {
            "sys_observability_span_log": {
                "retentionDays": 30,
                "expiredRows": 12,
                "deletedRows": 0,
                "remainingExpiredRows": 12,
                "samples": [{"traceId": "trace-span-1", "spanType": "HTTP_REQUEST"}],
            },
            "sys_business_event_log": {
                "retentionDays": 90,
                "expiredRows": 4,
                "deletedRows": 0,
                "remainingExpiredRows": 4,
                "samples": [{"traceId": "trace-event-1", "eventCode": "governance.publish"}],
            },
            "iot_message_log": {
                "retentionDays": 30,
                "expiredRows": 20,
                "deletedRows": 0,
                "remainingExpiredRows": 20,
                "samples": [{"traceId": "trace-msg-1", "topic": "/sys/demo/up"}],
            },
        },
        "summary": {"expiredRows": 36, "deletedRows": 0, "tablesWithExpiredRows": 3},
    }


class LoadPolicyTest(unittest.TestCase):
    def test_load_policy_exposes_expected_defaults(self):
        policy = observability_log_governance.load_policy(
            pathlib.Path("config/automation/observability-log-governance-policy.json")
        )

        self.assertIn("tables", policy)
        self.assertEqual(30, policy["tables"]["sys_observability_span_log"]["retentionDays"])
        self.assertEqual(90, policy["tables"]["sys_business_event_log"]["retentionDays"])
        self.assertEqual(30, policy["tables"]["iot_message_log"]["retentionDays"])
        self.assertTrue(policy["tables"]["iot_message_log"]["archiveEnabled"])
        self.assertEqual("iot_message_log_archive", policy["tables"]["iot_message_log"]["archiveTable"])
        self.assertEqual("iot_message_log_archive_batch", policy["tables"]["iot_message_log"]["archiveBatchTable"])


class GovernanceIdTest(unittest.TestCase):
    def test_build_governance_id_keeps_large_sequences_monotonic_across_ticks(self):
        base = observability_log_governance.datetime(2026, 4, 25, 23, 59, 59, 123000)
        current_tick_high = observability_log_governance.build_governance_id(base, 1200)
        next_tick = observability_log_governance.build_governance_id(
            observability_log_governance.datetime(2026, 4, 25, 23, 59, 59, 124000),
            1,
        )

        self.assertLess(current_tick_high, next_tick)


class RenderMarkdownTest(unittest.TestCase):
    def test_render_markdown_includes_table_summary_and_mode(self):
        markdown = observability_log_governance.render_markdown(create_snapshot_fixture())

        self.assertIn("# 可观测日志治理报告", markdown)
        self.assertIn("DRY_RUN", markdown)
        self.assertIn("sys_observability_span_log", markdown)
        self.assertIn("iot_message_log", markdown)


class BuildTableSnapshotTest(unittest.TestCase):
    def test_build_table_snapshot_uses_archive_first_apply_for_message_log(self):
        with mock.patch.object(
            observability_log_governance,
            "count_rows",
            return_value=50,
        ), mock.patch.object(
            observability_log_governance,
            "count_expired_rows",
            side_effect=[20, 0],
        ), mock.patch.object(
            observability_log_governance,
            "fetch_time_bounds",
            return_value={"oldest": "2026-03-01T00:00:00", "newest": "2026-04-25T23:00:00"},
        ), mock.patch.object(
            observability_log_governance,
            "fetch_samples",
            return_value=[{"traceId": "trace-msg-1", "topic": "/sys/demo/up"}],
        ), mock.patch.object(
            observability_log_governance,
            "archive_expired_message_rows",
            return_value={
                "archivedRows": 20,
                "deletedRows": 20,
                "archiveBatch": {
                    "status": "SUCCEEDED",
                    "batchNo": "iot_message_log-20260425235959",
                },
            },
        ):
            snapshot = observability_log_governance.build_table_snapshot(
                cur=mock.Mock(),
                table_name="iot_message_log",
                config={
                    "timeField": "report_time",
                    "retentionDays": 30,
                    "deleteBatchSize": 100,
                    "archiveEnabled": True,
                    "archiveTable": "iot_message_log_archive",
                    "archiveBatchTable": "iot_message_log_archive_batch",
                    "archiveChunkSize": 100,
                },
                sample_limit=5,
                apply_mode=True,
            )

        self.assertEqual(20, snapshot["archivedRows"])
        self.assertEqual(20, snapshot["deletedRows"])
        self.assertEqual("SUCCEEDED", snapshot["archiveBatch"]["status"])
        self.assertEqual("iot_message_log-20260425235959", snapshot["archiveBatch"]["batchNo"])

    def test_build_table_snapshot_keeps_failed_archive_batch_evidence(self):
        with mock.patch.object(
            observability_log_governance,
            "count_rows",
            return_value=50,
        ), mock.patch.object(
            observability_log_governance,
            "count_expired_rows",
            side_effect=[20, 12],
        ), mock.patch.object(
            observability_log_governance,
            "fetch_time_bounds",
            return_value={"oldest": "2026-03-01T00:00:00", "newest": "2026-04-25T23:00:00"},
        ), mock.patch.object(
            observability_log_governance,
            "fetch_samples",
            return_value=[{"traceId": "trace-msg-1", "topic": "/sys/demo/up"}],
        ), mock.patch.object(
            observability_log_governance,
            "archive_expired_message_rows",
            return_value={
                "archivedRows": 8,
                "deletedRows": 8,
                "archiveBatch": {
                    "status": "FAILED",
                    "batchNo": "iot_message_log-20260426000101",
                    "failedReason": "archive chunk failed",
                },
            },
        ):
            snapshot = observability_log_governance.build_table_snapshot(
                cur=mock.Mock(),
                table_name="iot_message_log",
                config={
                    "timeField": "report_time",
                    "retentionDays": 30,
                    "deleteBatchSize": 100,
                    "archiveEnabled": True,
                    "archiveTable": "iot_message_log_archive",
                    "archiveBatchTable": "iot_message_log_archive_batch",
                    "archiveChunkSize": 100,
                },
                sample_limit=5,
                apply_mode=True,
            )

        self.assertEqual(8, snapshot["archivedRows"])
        self.assertEqual(8, snapshot["deletedRows"])
        self.assertEqual("FAILED", snapshot["archiveBatch"]["status"])
        self.assertEqual(12, snapshot["remainingExpiredRows"])


class RunCliTest(unittest.TestCase):
    def test_run_cli_apply_requires_confirmation_fields(self):
        with tempfile.TemporaryDirectory(prefix="observability-log-governance-") as temp_dir:
            workspace_root = pathlib.Path(temp_dir)
            policy_path = workspace_root / "observability-log-policy.json"
            policy_path.write_text(json.dumps({"tables": {}}, ensure_ascii=False), encoding="utf-8")

            with mock.patch.object(
                observability_log_governance,
                "resolve_runtime_args",
                return_value={"db_host": "127.0.0.1", "db_port": "3306", "db_name": "rm_iot", "db_user": "demo", "db_password": "demo"},
            ):
                with self.assertRaisesRegex(RuntimeError, "--apply requires --confirm-report-path"):
                    observability_log_governance.run_governance_cli(
                        argv=[f"--policy-path={policy_path}", "--apply"],
                        workspace_root=workspace_root,
                    )

    def test_run_cli_writes_artifacts_in_dry_run(self):
        class FakeConnection:
            def close(self):
                return None

        with tempfile.TemporaryDirectory(prefix="observability-log-governance-") as temp_dir:
            workspace_root = pathlib.Path(temp_dir)
            policy = {
                "reportDirectory": "logs/observability",
                "tables": {
                    "sys_observability_span_log": {"retentionDays": 30, "timeField": "started_at", "deleteBatchSize": 100},
                    "sys_business_event_log": {"retentionDays": 90, "timeField": "occurred_at", "deleteBatchSize": 100},
                    "iot_message_log": {"retentionDays": 30, "timeField": "report_time", "deleteBatchSize": 100},
                },
            }
            policy_path = workspace_root / "observability-log-policy.json"
            policy_path.write_text(json.dumps(policy, ensure_ascii=False), encoding="utf-8")

            with mock.patch.object(
                observability_log_governance,
                "resolve_runtime_args",
                return_value={"db_host": "127.0.0.1", "db_port": "3306", "db_name": "rm_iot", "db_user": "demo", "db_password": "demo"},
            ), mock.patch.object(
                observability_log_governance,
                "open_db",
                return_value=FakeConnection(),
            ), mock.patch.object(
                observability_log_governance,
                "collect_governance_snapshot",
                return_value=create_snapshot_fixture(),
            ):
                result = observability_log_governance.run_governance_cli(
                    argv=[f"--policy-path={policy_path}"],
                    workspace_root=workspace_root,
                )

            self.assertEqual(0, result["exitCode"])
            self.assertTrue(pathlib.Path(result["jsonPath"]).exists())
            self.assertTrue(pathlib.Path(result["markdownPath"]).exists())

    def test_run_cli_marks_apply_mode_and_delete_count(self):
        snapshot = create_snapshot_fixture()
        snapshot["mode"] = "APPLY"
        snapshot["summary"]["deletedRows"] = 16
        snapshot["tables"]["iot_message_log"]["deletedRows"] = 10

        class FakeConnection:
            def close(self):
                return None

        with tempfile.TemporaryDirectory(prefix="observability-log-governance-") as temp_dir:
            workspace_root = pathlib.Path(temp_dir)
            policy_path = workspace_root / "observability-log-policy.json"
            policy_path.write_text(json.dumps({"tables": {}}, ensure_ascii=False), encoding="utf-8")

            with mock.patch.object(
                observability_log_governance,
                "resolve_runtime_args",
                return_value={"db_host": "127.0.0.1", "db_port": "3306", "db_name": "rm_iot", "db_user": "demo", "db_password": "demo"},
            ), mock.patch.object(
                observability_log_governance,
                "open_db",
                return_value=FakeConnection(),
            ), mock.patch.object(
                observability_log_governance,
                "collect_governance_snapshot",
                return_value=snapshot,
            ):
                result = observability_log_governance.run_governance_cli(
                    argv=[
                        f"--policy-path={policy_path}",
                        "--apply",
                        "--confirm-report-path=logs/observability/report.json",
                        "--confirm-report-generated-at=2026-04-25T23:15:00",
                        "--confirmed-expired-rows=16",
                    ],
                    workspace_root=workspace_root,
                )

            written = json.loads(pathlib.Path(result["jsonPath"]).read_text(encoding="utf-8"))
            self.assertEqual("APPLY", written["mode"])
            self.assertEqual(16, written["summary"]["deletedRows"])

    def test_run_cli_apply_accepts_confirmation_fields(self):
        snapshot = create_snapshot_fixture()
        snapshot["mode"] = "APPLY"

        class FakeConnection:
            def close(self):
                return None

        with tempfile.TemporaryDirectory(prefix="observability-log-governance-") as temp_dir:
            workspace_root = pathlib.Path(temp_dir)
            policy_path = workspace_root / "observability-log-policy.json"
            policy_path.write_text(json.dumps({"tables": {}}, ensure_ascii=False), encoding="utf-8")

            with mock.patch.object(
                observability_log_governance,
                "resolve_runtime_args",
                return_value={"db_host": "127.0.0.1", "db_port": "3306", "db_name": "rm_iot", "db_user": "demo", "db_password": "demo"},
            ), mock.patch.object(
                observability_log_governance,
                "open_db",
                return_value=FakeConnection(),
            ), mock.patch.object(
                observability_log_governance,
                "collect_governance_snapshot",
                return_value=snapshot,
            ):
                result = observability_log_governance.run_governance_cli(
                    argv=[
                        f"--policy-path={policy_path}",
                        "--apply",
                        "--confirm-report-path=logs/observability/report.json",
                        "--confirm-report-generated-at=2026-04-25T23:15:00",
                        "--confirmed-expired-rows=8",
                    ],
                    workspace_root=workspace_root,
                )

            self.assertEqual(0, result["exitCode"])


if __name__ == "__main__":
    unittest.main()
