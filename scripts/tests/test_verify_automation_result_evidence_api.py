import importlib.util
import json
import pathlib
import tempfile
import unittest


SCRIPT_PATH = pathlib.Path(__file__).resolve().parents[1] / "verify-automation-result-evidence-api.py"
SPEC = importlib.util.spec_from_file_location("automation_result_evidence_api", SCRIPT_PATH)
automation_result_api = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(automation_result_api)


def make_run(run_id="20260428125037", status="passed", package_code="risk-ops-closure"):
    return {
        "runId": run_id,
        "status": status,
        "packageCode": package_code,
        "summary": {"total": 3, "passed": 3, "failed": 0},
    }


def make_evidence(path="logs/acceptance/threshold-policy-effective-preview-api-20260428-125044.md"):
    return {
        "path": path,
        "fileName": pathlib.Path(path).name,
        "category": "markdown",
        "source": "scenario",
    }


class AutomationResultEvidenceApiTest(unittest.TestCase):
    def test_select_run_prefers_requested_run_id(self):
        page_data = {"records": [make_run("older"), make_run("target")]}

        selected = automation_result_api.select_run(page_data, requested_run_id="target")

        self.assertEqual("target", selected["runId"])

    def test_select_run_uses_latest_page_record_when_run_id_is_absent(self):
        page_data = {"records": [make_run("latest"), make_run("older")]}

        selected = automation_result_api.select_run(page_data, requested_run_id=None)

        self.assertEqual("latest", selected["runId"])

    def test_find_required_evidence_matches_case_insensitive_pattern(self):
        evidence = [
            make_evidence("logs/acceptance/registry-run-20260428125037.json"),
            make_evidence("logs/acceptance/threshold-policy-effective-preview-api-20260428-125044.md"),
        ]

        matched = automation_result_api.find_required_evidence(
            evidence,
            required_pattern="EFFECTIVE-preview-api",
        )

        self.assertEqual("threshold-policy-effective-preview-api-20260428-125044.md", matched["fileName"])

    def test_find_required_evidence_prefers_markdown_when_json_and_markdown_match(self):
        evidence = [
            make_evidence("logs/acceptance/threshold-policy-effective-preview-api-20260428-125044.json"),
            make_evidence("logs/acceptance/threshold-policy-effective-preview-api-20260428-125044.md"),
        ]

        matched = automation_result_api.find_required_evidence(
            evidence,
            required_pattern="threshold-policy-effective-preview-api",
        )

        self.assertEqual("markdown", matched["category"])
        self.assertTrue(matched["path"].endswith(".md"))

    def test_evaluate_report_passes_when_run_evidence_and_content_are_available(self):
        report = {
            "selectedRun": make_run(),
            "detail": make_run(),
            "evidenceItems": [make_evidence()],
            "requiredEvidence": make_evidence(),
            "contentPreview": {"content": "# Threshold Policy Effective Preview API\n\n- Status: `PASSED`"},
            "expectStatus": "passed",
            "requiredContent": ["Status: `PASSED`"],
        }

        evaluation = automation_result_api.evaluate_report(report)

        self.assertEqual("PASSED", evaluation["status"])
        self.assertEqual([], evaluation["breaches"])

    def test_evaluate_report_reports_missing_evidence_and_content(self):
        report = {
            "selectedRun": make_run(status="failed"),
            "detail": make_run(status="failed"),
            "evidenceItems": [],
            "requiredEvidence": None,
            "contentPreview": None,
            "expectStatus": "passed",
            "requiredContent": ["Status: `PASSED`"],
        }

        evaluation = automation_result_api.evaluate_report(report)

        self.assertEqual("FAILED", evaluation["status"])
        self.assertEqual(
            ["RUN_STATUS_MISMATCH", "REQUIRED_EVIDENCE_MISSING", "EVIDENCE_CONTENT_MISSING"],
            [item["type"] for item in evaluation["breaches"]],
        )

    def test_render_markdown_summarizes_evidence_link(self):
        report = {
            "checkedAt": "2026-04-28T13:00:00",
            "target": {"baseUrl": "http://127.0.0.1:9999", "packageCode": "risk-ops-closure"},
            "selectedRun": make_run(),
            "detail": make_run(),
            "evidenceItems": [make_evidence()],
            "requiredEvidence": make_evidence(),
            "contentPreview": {"content": "# Threshold Policy Effective Preview API\n\n- Status: `PASSED`"},
            "expectStatus": "passed",
            "requiredContent": ["Status: `PASSED`"],
        }
        report["evaluation"] = automation_result_api.evaluate_report(report)

        markdown = automation_result_api.render_markdown(report)

        self.assertIn("Status: `PASSED`", markdown)
        self.assertIn("Run ID: `20260428125037`", markdown)
        self.assertIn("threshold-policy-effective-preview-api-20260428-125044.md", markdown)

    def test_write_reports_updates_latest_aliases(self):
        report = {
            "checkedAt": "2026-04-28T13:00:00",
            "target": {"baseUrl": "http://127.0.0.1:9999", "packageCode": "risk-ops-closure"},
            "selectedRun": make_run(),
            "detail": make_run(),
            "evidenceItems": [make_evidence()],
            "requiredEvidence": make_evidence(),
            "contentPreview": {"content": "# Threshold Policy Effective Preview API\n\n- Status: `PASSED`"},
            "expectStatus": "passed",
            "requiredContent": ["Status: `PASSED`"],
        }
        report["evaluation"] = automation_result_api.evaluate_report(report)

        with tempfile.TemporaryDirectory() as temp_dir:
            json_path, md_path = automation_result_api.write_reports(report, temp_dir)
            latest_json_path = pathlib.Path(temp_dir) / "automation-result-evidence-api-latest.json"
            latest_md_path = pathlib.Path(temp_dir) / "automation-result-evidence-api-latest.md"

            self.assertTrue(json_path.exists())
            self.assertTrue(md_path.exists())
            self.assertEqual(
                json.loads(json_path.read_text(encoding="utf-8")),
                json.loads(latest_json_path.read_text(encoding="utf-8")),
            )
            self.assertEqual(md_path.read_text(encoding="utf-8"), latest_md_path.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
