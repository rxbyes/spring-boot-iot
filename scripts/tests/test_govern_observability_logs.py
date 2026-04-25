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


class RenderMarkdownTest(unittest.TestCase):
    def test_render_markdown_includes_table_summary_and_mode(self):
        markdown = observability_log_governance.render_markdown(create_snapshot_fixture())

        self.assertIn("# 可观测日志治理报告", markdown)
        self.assertIn("DRY_RUN", markdown)
        self.assertIn("sys_observability_span_log", markdown)
        self.assertIn("iot_message_log", markdown)


class RunCliTest(unittest.TestCase):
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
                    argv=[f"--policy-path={policy_path}", "--apply"],
                    workspace_root=workspace_root,
                )

            written = json.loads(pathlib.Path(result["jsonPath"]).read_text(encoding="utf-8"))
            self.assertEqual("APPLY", written["mode"])
            self.assertEqual(16, written["summary"]["deletedRows"])


if __name__ == "__main__":
    unittest.main()
