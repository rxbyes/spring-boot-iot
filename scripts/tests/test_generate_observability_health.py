import importlib.util
import json
import pathlib
import tempfile
import unittest
from unittest import mock


SCRIPT_PATH = pathlib.Path(__file__).resolve().parents[1] / "generate-observability-health.py"
SPEC = importlib.util.spec_from_file_location("observability_health", SCRIPT_PATH)
observability_health = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(observability_health)


def create_report_fixture():
    report = {
        "generatedAt": "2026-04-25T20:30:00",
        "window": {
            "hours": 24,
            "startedAt": "2026-04-24T20:30:00",
            "endedAt": "2026-04-25T20:30:00",
        },
        "summary": {
            "status": "unrated",
            "errors": 0,
            "warnings": 0,
            "counters": {
                "spanRows": 100,
                "businessEventRows": 80,
                "spanTraceRows": 98,
                "businessEventTraceRows": 76,
            },
        },
        "metrics": {
            "spanTraceCoverageRate": observability_health.make_ratio_metric(
                "spanTraceCoverageRate", "Span TraceId 覆盖率", 98, 100
            ),
            "traceWithoutBusinessEventRate": observability_health.make_ratio_metric(
                "traceWithoutBusinessEventRate", "无业务事件 Trace 占比", 8, 40
            ),
            "messageFlowTraceLinkRate": observability_health.make_ratio_metric(
                "messageFlowTraceLinkRate", "消息链路留痕关联率", 36, 40
            ),
        },
    }
    return report


class EvaluatePolicyTest(unittest.TestCase):
    def test_evaluate_policy_counts_errors_and_warnings(self):
        report = create_report_fixture()
        policy = {
            "rules": [
                {
                    "id": "spanTraceCoverage",
                    "metric": "spanTraceCoverageRate",
                    "operator": ">=",
                    "threshold": 0.99,
                    "severity": "error",
                    "message": "Span TraceId 覆盖率必须不低于 99%",
                },
                {
                    "id": "messageFlowLink",
                    "metric": "messageFlowTraceLinkRate",
                    "operator": ">=",
                    "threshold": 0.95,
                    "severity": "warning",
                    "message": "消息链路留痕关联率建议不低于 95%",
                },
                {
                    "id": "orphanTrace",
                    "metric": "traceWithoutBusinessEventRate",
                    "operator": "<=",
                    "threshold": 0.25,
                    "severity": "warning",
                    "message": "无业务事件 Trace 占比建议不高于 25%",
                },
            ]
        }

        evaluation = observability_health.evaluate_policy(report, policy)

        self.assertEqual("failed", evaluation["status"])
        self.assertEqual(1, evaluation["summary"]["errors"])
        self.assertEqual(1, evaluation["summary"]["warnings"])
        self.assertEqual(0, evaluation["summary"]["skipped"])

    def test_evaluate_policy_skips_missing_metric_values(self):
        report = create_report_fixture()
        report["metrics"]["spanTraceCoverageRate"]["value"] = None
        policy = {
            "rules": [
                {
                    "id": "spanTraceCoverage",
                    "metric": "spanTraceCoverageRate",
                    "operator": ">=",
                    "threshold": 0.99,
                    "severity": "error",
                }
            ]
        }

        evaluation = observability_health.evaluate_policy(report, policy)

        self.assertEqual("passed", evaluation["status"])
        self.assertEqual(1, evaluation["summary"]["skipped"])
        self.assertEqual("skipped", evaluation["results"][0]["status"])


class RenderMarkdownTest(unittest.TestCase):
    def test_render_markdown_includes_policy_section(self):
        report = create_report_fixture()
        report["policyEvaluation"] = {
            "status": "warning",
            "summary": {"errors": 0, "warnings": 1, "skipped": 0},
            "results": [
                {
                    "id": "messageFlowLink",
                    "metric": "messageFlowTraceLinkRate",
                    "severity": "warning",
                    "status": "failed",
                    "message": "消息链路留痕关联率建议不低于 95%",
                    "threshold": 0.95,
                    "operator": ">=",
                    "actual": 0.9,
                }
            ],
        }
        report["summary"]["status"] = "warning"
        report["summary"]["warnings"] = 1

        markdown = observability_health.render_markdown(report)

        self.assertIn("# 可观测健康报告", markdown)
        self.assertIn("## 门禁评估", markdown)
        self.assertIn("消息链路留痕关联率建议不低于 95%", markdown)


class RunCliTest(unittest.TestCase):
    def test_run_cli_writes_artifacts_and_respects_policy_exit_code(self):
        report = create_report_fixture()
        policy = {
            "rules": [
                {
                    "id": "spanTraceCoverage",
                    "metric": "spanTraceCoverageRate",
                    "operator": ">=",
                    "threshold": 0.99,
                    "severity": "error",
                    "message": "Span TraceId 覆盖率必须不低于 99%",
                }
            ]
        }

        class FakeConnection:
            def close(self):
                return None

        with tempfile.TemporaryDirectory(prefix="observability-health-") as temp_dir:
            workspace_root = pathlib.Path(temp_dir)
            policy_path = workspace_root / "observability-policy.json"
            policy_path.write_text(json.dumps(policy, ensure_ascii=False), encoding="utf-8")

            with mock.patch.object(
                observability_health,
                "resolve_runtime_args",
                return_value={"db_host": "127.0.0.1", "db_port": "3306", "db_name": "rm_iot", "db_user": "demo", "db_password": "demo"},
            ), mock.patch.object(
                observability_health,
                "open_db",
                return_value=FakeConnection(),
            ), mock.patch.object(
                observability_health,
                "collect_observability_snapshot",
                return_value=report,
            ):
                result = observability_health.run_health_cli(
                    argv=[
                        "--hours=12",
                        f"--policy-path={policy_path}",
                        "--fail-on-breaches",
                    ],
                    workspace_root=workspace_root,
                )

            self.assertEqual(1, result["exitCode"])
            self.assertTrue(pathlib.Path(result["jsonPath"]).exists())
            self.assertTrue(pathlib.Path(result["markdownPath"]).exists())

            written = json.loads(pathlib.Path(result["jsonPath"]).read_text(encoding="utf-8"))
            self.assertEqual("failed", written["summary"]["status"])
            self.assertEqual(1, written["summary"]["errors"])


if __name__ == "__main__":
    unittest.main()
