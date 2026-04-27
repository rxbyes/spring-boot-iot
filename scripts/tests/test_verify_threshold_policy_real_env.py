import importlib.util
import pathlib
import types
import unittest


SCRIPT_PATH = pathlib.Path(__file__).resolve().parents[1] / "verify-threshold-policy-real-env.py"
SPEC = importlib.util.spec_from_file_location("verify_threshold_policy_real_env", SCRIPT_PATH)
verify_threshold_policy = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(verify_threshold_policy)


def make_passing_report():
    return {
        "checkedAt": "2026-04-27T21:00:00",
        "database": {"host": "db", "port": 3306, "db": "rm_iot", "user": "root"},
        "policyPrecedence": verify_threshold_policy.POLICY_PRECEDENCE,
        "schema": {
            "columns": [
                {"name": name, "columnType": "varchar(32)", "nullable": expected["nullable"], "matched": True}
                for name, expected in verify_threshold_policy.EXPECTED_COLUMNS.items()
            ],
            "indexes": [
                {"name": name, "columns": columns, "matched": True}
                for name, columns in verify_threshold_policy.EXPECTED_INDEXES.items()
            ],
        },
        "policyDistribution": {
            "activeRuleCount": 1,
            "scopeCounts": [{"ruleScope": "PRODUCT_TYPE", "productType": "MONITORING", "ruleCount": 1}],
        },
        "duplicates": [],
    }


class ThresholdPolicyRealEnvVerificationTest(unittest.TestCase):
    def test_evaluate_report_passes_when_schema_matches_and_no_duplicates(self):
        report = make_passing_report()

        evaluation = verify_threshold_policy.evaluate_report(report)

        self.assertEqual("PASSED", evaluation["status"])
        self.assertEqual(0, evaluation["breachCount"])

    def test_evaluate_report_fails_on_schema_drift_and_duplicates(self):
        report = make_passing_report()
        report["schema"]["columns"][0]["matched"] = False
        report["schema"]["indexes"][0]["matched"] = False
        report["duplicates"] = [
            {
                "ruleScope": "PRODUCT_TYPE",
                "productType": "MONITORING",
                "productId": 0,
                "deviceId": 0,
                "riskPointDeviceId": 0,
                "metricIdentifier": "value",
                "duplicateCount": 2,
            }
        ]

        evaluation = verify_threshold_policy.evaluate_report(report)

        self.assertEqual("FAILED", evaluation["status"])
        self.assertEqual(
            ["COLUMN_DRIFT", "INDEX_DRIFT", "DUPLICATE_POLICY_GROUPS"],
            [item["type"] for item in evaluation["breaches"]],
        )

    def test_render_markdown_contains_operational_summary(self):
        report = make_passing_report()
        report["evaluation"] = verify_threshold_policy.evaluate_report(report)

        markdown = verify_threshold_policy.render_markdown(report)

        self.assertIn("Status: `PASSED`", markdown)
        self.assertIn("BINDING > DEVICE > PRODUCT > PRODUCT_TYPE > METRIC > YAML_AUTO_CLOSURE", markdown)
        self.assertIn("| PRODUCT_TYPE | MONITORING | 1 |", markdown)
        self.assertIn("No duplicate active policy groups found.", markdown)

    def test_parse_args_uses_shared_defaults_without_exposing_password_in_report(self):
        args = verify_threshold_policy.parse_args([])

        self.assertEqual("rm_iot", args.db)
        self.assertEqual(3306, args.port)
        self.assertTrue(args.password)

        report = make_passing_report()
        report["evaluation"] = verify_threshold_policy.evaluate_report(report)
        self.assertNotIn("password", report["database"])


if __name__ == "__main__":
    unittest.main()
