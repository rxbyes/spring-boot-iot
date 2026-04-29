import importlib.util
import json
import pathlib
import tempfile
import unittest


SCRIPT_PATH = pathlib.Path(__file__).resolve().parents[1] / "verify-threshold-policy-effective-preview-api.py"
SPEC = importlib.util.spec_from_file_location("threshold_policy_effective_preview_api", SCRIPT_PATH)
threshold_preview_api = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(threshold_preview_api)


def make_rule(rule_id="9001", scope="PRODUCT", metric="value"):
    return {
        "id": rule_id,
        "tenantId": "1",
        "riskMetricId": "6102",
        "ruleScope": scope,
        "productType": "MONITORING" if scope == "PRODUCT_TYPE" else None,
        "productId": "1001" if scope == "PRODUCT" else None,
        "deviceId": "8001" if scope == "DEVICE" else None,
        "riskPointDeviceId": "7001" if scope == "BINDING" else None,
        "ruleName": f"{scope} threshold",
        "metricIdentifier": metric,
        "metricName": metric,
        "expression": "value >= 1",
        "status": 0,
    }


def make_preview(rule):
    return {
        "hasMatchedRule": True,
        "matchedScope": rule["ruleScope"],
        "matchedRule": rule,
        "decision": f"最终生效策略：{rule['ruleName']}",
        "candidates": [
            {
                "ruleId": rule["id"],
                "ruleScope": rule["ruleScope"],
                "matchedContext": True,
                "selected": True,
                "expression": rule["expression"],
            }
        ],
    }


class ThresholdPolicyEffectivePreviewApiTest(unittest.TestCase):
    def test_build_preview_params_preserves_scope_identity(self):
        binding_rule = make_rule(rule_id="202604280000000101", scope="BINDING")

        params = threshold_preview_api.build_preview_params(binding_rule)

        self.assertEqual("202604280000000101", binding_rule["id"])
        self.assertEqual("1", params["tenantId"])
        self.assertEqual("6102", params["riskMetricId"])
        self.assertEqual("value", params["metricIdentifier"])
        self.assertEqual("7001", params["riskPointDeviceId"])
        self.assertNotIn("productId", params)

    def test_evaluate_passes_when_selected_preview_matches_each_sampled_rule(self):
        product_rule = make_rule(scope="PRODUCT")
        binding_rule = make_rule(rule_id="9002", scope="BINDING")
        report = {
            "sampledRules": [product_rule, binding_rule],
            "previewResults": [
                {"rule": product_rule, "preview": make_preview(product_rule)},
                {"rule": binding_rule, "preview": make_preview(binding_rule)},
            ],
        }

        evaluation = threshold_preview_api.evaluate_report(report, fail_on_empty=False)

        self.assertEqual("PASSED", evaluation["status"])
        self.assertEqual([], evaluation["breaches"])

    def test_evaluate_fails_when_preview_selects_a_different_rule(self):
        source_rule = make_rule(rule_id="9001", scope="PRODUCT")
        other_rule = make_rule(rule_id="9002", scope="PRODUCT")
        report = {
            "sampledRules": [source_rule],
            "previewResults": [{"rule": source_rule, "preview": make_preview(other_rule)}],
        }

        evaluation = threshold_preview_api.evaluate_report(report, fail_on_empty=False)

        self.assertEqual("FAILED", evaluation["status"])
        self.assertIn("SELECTED_RULE_MISMATCH", [item["type"] for item in evaluation["breaches"]])

    def test_evaluate_can_fail_on_empty_samples(self):
        report = {"sampledRules": [], "previewResults": []}

        evaluation = threshold_preview_api.evaluate_report(report, fail_on_empty=True)

        self.assertEqual("FAILED", evaluation["status"])
        self.assertEqual(["NO_ENABLED_RULE_SAMPLE"], [item["type"] for item in evaluation["breaches"]])

    def test_render_markdown_contains_preview_summary(self):
        rule = make_rule(scope="PRODUCT")
        report = {
            "checkedAt": "2026-04-28T12:30:00",
            "target": {"baseUrl": "http://127.0.0.1:9999"},
            "summary": {"sampledRuleCount": 1, "matchedPreviewCount": 1},
            "sampledRules": [rule],
            "previewResults": [{"rule": rule, "preview": make_preview(rule)}],
        }
        report["evaluation"] = threshold_preview_api.evaluate_report(report, fail_on_empty=False)

        markdown = threshold_preview_api.render_markdown(report)

        self.assertIn("Status: `PASSED`", markdown)
        self.assertIn("| PRODUCT | value | PRODUCT threshold | PRODUCT threshold | True |", markdown)

    def test_write_reports_updates_latest_aliases(self):
        rule = make_rule(scope="PRODUCT")
        report = {
            "checkedAt": "2026-04-28T12:30:00",
            "target": {"baseUrl": "http://127.0.0.1:9999"},
            "summary": {"sampledRuleCount": 1, "matchedPreviewCount": 1},
            "sampledRules": [rule],
            "previewResults": [{"rule": rule, "preview": make_preview(rule)}],
        }
        report["evaluation"] = threshold_preview_api.evaluate_report(report, fail_on_empty=False)

        with tempfile.TemporaryDirectory() as temp_dir:
            json_path, md_path = threshold_preview_api.write_reports(report, temp_dir)
            latest_json_path = pathlib.Path(temp_dir) / "threshold-policy-effective-preview-api-latest.json"
            latest_md_path = pathlib.Path(temp_dir) / "threshold-policy-effective-preview-api-latest.md"

            self.assertTrue(json_path.exists())
            self.assertTrue(md_path.exists())
            self.assertEqual(
                json.loads(json_path.read_text(encoding="utf-8")),
                json.loads(latest_json_path.read_text(encoding="utf-8")),
            )
            self.assertEqual(md_path.read_text(encoding="utf-8"), latest_md_path.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
