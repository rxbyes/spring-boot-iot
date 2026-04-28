import importlib.util
import json
import pathlib
import tempfile
import unittest


SCRIPT_PATH = pathlib.Path(__file__).resolve().parents[1] / "verify-threshold-policy-recommendation-api.py"
SPEC = importlib.util.spec_from_file_location("threshold_policy_recommendation_api", SCRIPT_PATH)
threshold_api = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(threshold_api)


def make_report(records):
    return {
        "checkedAt": "2026-04-28T09:40:00",
        "target": {"baseUrl": "http://127.0.0.1:10099", "path": "/api/risk-governance/missing-policies/product-metric-summaries"},
        "query": {"pageNum": 1, "pageSize": 5},
        "summary": threshold_api.summarize_records(records),
        "records": records,
    }


class ThresholdPolicyRecommendationApiTest(unittest.TestCase):
    def test_summarize_records_counts_adoptable_recommendations(self):
        records = [
            {
                "productName": "Monitoring Crack",
                "metricIdentifier": "value",
                "recommendedExpression": "value >= 12",
                "recommendationStatus": "SUGGESTED",
            },
            {
                "productName": "Rain Gauge",
                "metricIdentifier": "value",
                "recommendationStatus": "FLAT_ZERO_REVIEW",
            },
        ]

        summary = threshold_api.summarize_records(records)

        self.assertEqual(2, summary["recordCount"])
        self.assertEqual(2, summary["recommendationFieldRowCount"])
        self.assertEqual(1, summary["adoptableRecommendationCount"])
        self.assertEqual(1, summary["manualReviewCount"])

    def test_evaluate_passes_when_rows_expose_recommendation_fields(self):
        report = make_report([
            {"metricIdentifier": "value", "recommendationStatus": "NO_NUMERIC_SAMPLE"}
        ])

        evaluation = threshold_api.evaluate_report(report, require_adoptable=False)

        self.assertEqual("PASSED", evaluation["status"])
        self.assertEqual([], evaluation["breaches"])

    def test_evaluate_fails_when_records_have_no_recommendation_fields(self):
        report = make_report([
            {"metricIdentifier": "value", "bindingCount": 3}
        ])

        evaluation = threshold_api.evaluate_report(report, require_adoptable=False)

        self.assertEqual("FAILED", evaluation["status"])
        self.assertEqual(["RECOMMENDATION_FIELDS_MISSING"], [item["type"] for item in evaluation["breaches"]])

    def test_evaluate_can_require_adoptable_recommendation(self):
        report = make_report([
            {"metricIdentifier": "value", "recommendationStatus": "INSUFFICIENT_SAMPLE"}
        ])

        evaluation = threshold_api.evaluate_report(report, require_adoptable=True)

        self.assertEqual("FAILED", evaluation["status"])
        self.assertEqual(["NO_ADOPTABLE_RECOMMENDATION"], [item["type"] for item in evaluation["breaches"]])

    def test_write_reports_updates_latest_aliases(self):
        report = make_report([
            {"metricIdentifier": "value", "recommendedExpression": "value >= 12"}
        ])
        report["evaluation"] = threshold_api.evaluate_report(report, require_adoptable=False)

        with tempfile.TemporaryDirectory() as temp_dir:
            json_path, md_path = threshold_api.write_reports(report, temp_dir)
            latest_json_path = pathlib.Path(temp_dir) / "threshold-policy-recommendation-api-latest.json"
            latest_md_path = pathlib.Path(temp_dir) / "threshold-policy-recommendation-api-latest.md"

            self.assertTrue(json_path.exists())
            self.assertTrue(md_path.exists())
            self.assertEqual(json.loads(json_path.read_text(encoding="utf-8")), json.loads(latest_json_path.read_text(encoding="utf-8")))
            self.assertEqual(md_path.read_text(encoding="utf-8"), latest_md_path.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
