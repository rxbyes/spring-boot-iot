import importlib.util
import io
import json
import pathlib
import sys
import tempfile
import unittest
from contextlib import redirect_stdout


SCRIPT_PATH = pathlib.Path(__file__).resolve().parents[1] / "backfill-threshold-policy-defaults.py"
SPEC = importlib.util.spec_from_file_location("backfill_threshold_policy_defaults", SCRIPT_PATH)
backfill = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = backfill
SPEC.loader.exec_module(backfill)


class ThresholdPolicyDefaultBackfillTest(unittest.TestCase):
    def test_normalize_expression_accepts_numbers_and_simple_value_expressions(self):
        self.assertEqual("value >= 20", backfill.normalize_expression("20"))
        self.assertEqual("value > 3.5", backfill.normalize_expression("value > 3.5"))
        self.assertIsNone(backfill.normalize_expression("abs(value) > 20"))

    def test_resolve_product_type_matches_monitoring_product_keywords(self):
        self.assertEqual("MONITORING", backfill.resolve_product_type("nf-monitor-gnss-monitor-v1", "GNSS monitor"))
        self.assertEqual("VIDEO", backfill.resolve_product_type("camera-v1", "Video Camera"))
        self.assertEqual("UNKNOWN", backfill.resolve_product_type("generic-v1", "Generic"))

    def test_build_plan_prefers_existing_binding_default_threshold(self):
        config = {
            "defaultDuration": 0,
            "defaultAlarmLevel": "orange",
            "defaultNotificationMethods": "",
            "defaultConvertToEvent": True,
            "productTypeTemplates": [],
        }
        bindings = [
            {
                "id": 7001,
                "tenant_id": 1,
                "device_id": 8001,
                "risk_metric_id": 9101,
                "metric_identifier": "dispsY",
                "metric_name": "Y",
                "default_threshold": "20",
                "product_id": 1001,
                "product_key": "nf-monitor-multi-displacement-v1",
                "product_name": "Monitoring",
            }
        ]

        plan = backfill.build_plan(bindings, [], config)

        self.assertEqual(1, plan["candidateCount"])
        candidate = plan["candidates"][0]
        self.assertEqual("BINDING", candidate.rule_scope)
        self.assertEqual(7001, candidate.risk_point_device_id)
        self.assertEqual("value >= 20", candidate.expression)

    def test_build_plan_uses_product_type_template_once_for_many_bindings(self):
        config = {
            "defaultDuration": 0,
            "defaultAlarmLevel": "orange",
            "defaultNotificationMethods": "",
            "defaultConvertToEvent": True,
            "productTypeTemplates": [
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "value",
                    "expression": "value >= 10",
                    "confirmationStatus": "CONFIRMED",
                }
            ],
        }
        bindings = [
            {
                "id": 7001,
                "tenant_id": 1,
                "device_id": 8001,
                "risk_metric_id": 9101,
                "metric_identifier": "value",
                "metric_name": "value",
                "default_threshold": None,
                "product_id": 1001,
                "product_key": "nf-monitor-crack-meter-v1",
                "product_name": "Monitoring",
            },
            {
                "id": 7002,
                "tenant_id": 1,
                "device_id": 8002,
                "risk_metric_id": 9102,
                "metric_identifier": "value",
                "metric_name": "value",
                "default_threshold": None,
                "product_id": 1002,
                "product_key": "nf-monitor-laser-rangefinder-v1",
                "product_name": "Monitoring",
            },
        ]

        plan = backfill.build_plan(bindings, [], config)

        self.assertEqual(1, plan["candidateCount"])
        candidate = plan["candidates"][0]
        self.assertEqual("PRODUCT_TYPE", candidate.rule_scope)
        self.assertEqual("MONITORING", candidate.product_type)
        self.assertEqual(2, candidate.covered_binding_count)

    def test_product_type_template_uses_metric_name_to_disambiguate_same_identifier(self):
        config = {
            "productTypeTemplates": [
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "value",
                    "metricName": "裂缝值",
                    "expression": "value >= 10",
                    "confirmationStatus": "CONFIRMED",
                },
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "value",
                    "metricName": "激光测距值",
                    "expression": "value >= 20",
                    "confirmationStatus": "CONFIRMED",
                },
            ]
        }
        binding = {
            "product_type": "MONITORING",
            "metric_identifier": "value",
            "metric_name": "激光测距值",
            "risk_metric_id": None,
        }

        template = backfill.find_product_type_template(binding, config)

        self.assertEqual("value >= 20", template["expression"])

    def test_product_type_template_returns_none_when_same_identifier_is_ambiguous(self):
        config = {
            "productTypeTemplates": [
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "value",
                    "expression": "value >= 10",
                    "confirmationStatus": "CONFIRMED",
                },
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "value",
                    "expression": "value >= 20",
                    "confirmationStatus": "CONFIRMED",
                },
            ]
        }
        binding = {
            "product_type": "MONITORING",
            "metric_identifier": "value",
            "metric_name": "裂缝值",
            "risk_metric_id": None,
        }

        self.assertIsNone(backfill.find_product_type_template(binding, config))

    def test_build_plan_does_not_treat_product_type_rule_as_global(self):
        monitoring_binding = {
            "id": 7001,
            "tenant_id": 1,
            "device_id": 8001,
            "risk_metric_id": 9101,
            "metric_identifier": "value",
            "metric_name": "value",
            "default_threshold": None,
            "product_id": 1001,
            "product_key": "nf-monitor-crack-meter-v1",
            "product_name": "Monitoring",
        }
        video_binding = {
            "id": 7002,
            "tenant_id": 1,
            "device_id": 8002,
            "risk_metric_id": 9101,
            "metric_identifier": "value",
            "metric_name": "value",
            "default_threshold": None,
            "product_id": 1002,
            "product_key": "camera-v1",
            "product_name": "Video Camera",
        }
        active_rules = [
            {
                "tenant_id": 1,
                "risk_metric_id": None,
                "metric_identifier": "value",
                "rule_scope": "PRODUCT_TYPE",
                "product_type": "MONITORING",
            }
        ]

        self.assertTrue(backfill.is_binding_covered({**monitoring_binding, "product_type": "MONITORING"}, active_rules))
        self.assertFalse(backfill.is_binding_covered({**video_binding, "product_type": "VIDEO"}, active_rules))

    def test_render_markdown_uses_full_skipped_reason_counts(self):
        markdown = backfill.render_markdown({
            "mode": "dry-run",
            "status": "PASSED",
            "checkedAt": "2026-04-27T22:20:00",
            "bindingTotal": 1092,
            "riskBindingTotal": 1097,
            "activeDeviceProductBindingCount": 1092,
            "inactiveOrMissingDeviceProductCount": 5,
            "coveredBindingCount": 0,
            "candidateCount": 0,
            "appliedCount": 0,
            "skippedCount": 1092,
            "skippedReasonCounts": {"NO_CONFIRMED_THRESHOLD_TEMPLATE": 1092},
            "templateGapCount": 1,
            "templateValidation": {"readyTemplateCount": 0, "breachCount": 0, "breaches": []},
            "applyConfirmation": {"status": "NOT_APPLICABLE", "breachCount": 0, "breaches": []},
            "templateGaps": [
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "value",
                    "metricName": "裂缝值",
                    "bindingCount": 1092,
                    "productCount": 3,
                    "suggestedTemplate": {"expression": None},
                }
            ],
            "candidates": [],
            "skipped": [{"reason": "NO_CONFIRMED_THRESHOLD_TEMPLATE"}],
        })

        self.assertIn("Risk Binding Total: `1097`", markdown)
        self.assertIn("| NO_CONFIRMED_THRESHOLD_TEMPLATE | 1092 |", markdown)
        self.assertIn("| MONITORING | value | 裂缝值 | 1092 | 3 | NEEDS_CONFIRMATION |", markdown)

    def test_build_template_gaps_aggregates_skipped_items_by_product_type_and_metric(self):
        gaps = backfill.build_template_gaps([
            {
                "reason": "NO_CONFIRMED_THRESHOLD_TEMPLATE",
                "productType": "MONITORING",
                "metricIdentifier": "value",
                "metricName": "裂缝值",
                "productId": 1001,
                "productKey": "crack-v1",
                "productName": "裂缝计",
            },
            {
                "reason": "NO_CONFIRMED_THRESHOLD_TEMPLATE",
                "productType": "MONITORING",
                "metricIdentifier": "value",
                "metricName": "裂缝值",
                "productId": 1002,
                "productKey": "laser-v1",
                "productName": "激光测距仪",
            },
        ])

        self.assertEqual(1, len(gaps))
        self.assertEqual(2, gaps[0]["bindingCount"])
        self.assertEqual(2, gaps[0]["productCount"])
        self.assertEqual("NEEDS_CONFIRMATION", gaps[0]["suggestedTemplate"]["status"])

    def test_build_template_gaps_merges_full_path_and_leaf_identifiers_by_semantic_metric(self):
        gaps = backfill.build_template_gaps([
            {
                "reason": "NO_CONFIRMED_THRESHOLD_TEMPLATE",
                "productType": "MONITORING",
                "metricIdentifier": "L1_GP_1.gpsTotalX",
                "metricName": "GNSS \u7d2f\u8ba1\u4f4d\u79fb X",
                "productId": 1001,
                "productKey": "nf-monitor-gnss-monitor-v1",
                "productName": "GNSS",
            },
            {
                "reason": "NO_CONFIRMED_THRESHOLD_TEMPLATE",
                "productType": "MONITORING",
                "metricIdentifier": "gpsTotalX",
                "metricName": "X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                "productId": 1002,
                "productKey": "nf-monitor-gnss-monitor-v1",
                "productName": "GNSS",
            },
        ])

        self.assertEqual(1, len(gaps))
        self.assertEqual("gpsTotalX", gaps[0]["metricIdentifier"])
        self.assertEqual("X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf", gaps[0]["metricName"])
        self.assertEqual(["L1_GP_1.gpsTotalX", "gpsTotalX"], gaps[0]["metricAliases"])
        self.assertEqual(["L1_GP_1.gpsTotalX", "gpsTotalX"], gaps[0]["match"]["rawIdentifiers"])

    def test_build_template_gaps_keeps_value_metrics_separated_by_business_context(self):
        gaps = backfill.build_template_gaps([
            {
                "reason": "NO_CONFIRMED_THRESHOLD_TEMPLATE",
                "productType": "MONITORING",
                "metricIdentifier": "value",
                "metricName": "\u88c2\u7f1d\u503c",
                "productId": 1001,
                "productKey": "nf-monitor-crack-meter-v1",
                "productName": "Crack",
            },
            {
                "reason": "NO_CONFIRMED_THRESHOLD_TEMPLATE",
                "productType": "MONITORING",
                "metricIdentifier": "value",
                "metricName": "\u6fc0\u5149\u6d4b\u8ddd\u503c",
                "productId": 1002,
                "productKey": "nf-monitor-laser-rangefinder-v1",
                "productName": "Laser",
            },
        ])

        self.assertEqual(2, len(gaps))
        names = {gap["metricName"] for gap in gaps}
        self.assertEqual({"\u88c2\u7f1d\u91cf", "\u6fc0\u5149\u6d4b\u8ddd\u503c"}, names)
        for gap in gaps:
            self.assertIn("productKeys", gap["match"])

    def test_build_template_gaps_adds_deep_displacement_business_templates(self):
        gaps = backfill.build_template_gaps([
            {
                "reason": "NO_CONFIRMED_THRESHOLD_TEMPLATE",
                "productType": "MONITORING",
                "metricIdentifier": "dispsX",
                "metricName": "\u987a\u6ed1\u52a8\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                "productId": 1001,
                "productKey": "nf-monitor-deep-displacement-v1",
                "productName": "Deep displacement",
            },
            {
                "reason": "NO_CONFIRMED_THRESHOLD_TEMPLATE",
                "productType": "MONITORING",
                "metricIdentifier": "dispsY",
                "metricName": "\u5782\u76f4\u5761\u9762\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                "productId": 1001,
                "productKey": "nf-monitor-deep-displacement-v1",
                "productName": "Deep displacement",
            },
        ])

        by_identifier = {gap["metricIdentifier"]: gap for gap in gaps}
        self.assertEqual({"dispsX", "dispsY"}, set(by_identifier))
        self.assertEqual("deep-displacement:dispsX", by_identifier["dispsX"]["semanticTemplateKey"])
        self.assertEqual("deep-displacement:dispsY", by_identifier["dispsY"]["semanticTemplateKey"])
        self.assertEqual(
            ["nf-monitor-deep-displacement-v1"],
            by_identifier["dispsX"]["match"]["productKeys"],
        )
        self.assertEqual(
            ["nf-monitor-deep-displacement-v1"],
            by_identifier["dispsY"]["match"]["productKeys"],
        )

    def test_build_template_gaps_uses_manual_semantic_templates_from_config_for_new_sensors(self):
        config = {
            "manualSemanticTemplates": [
                {
                    "productType": "MONITORING",
                    "semanticTemplateKey": "manual-strain:strain",
                    "metricIdentifier": "strain",
                    "metricName": "\u5e94\u53d8\u503c",
                    "metricAliases": ["L5_YB_1.strain", "strain"],
                    "match": {
                        "productKeys": ["custom-monitor-strain-v1"],
                        "metricNames": ["\u5e94\u53d8\u503c"],
                        "rawIdentifiers": ["L5_YB_1.strain", "strain"],
                    },
                }
            ]
        }

        gaps = backfill.build_template_gaps([
            {
                "reason": "NO_CONFIRMED_THRESHOLD_TEMPLATE",
                "productType": "MONITORING",
                "metricIdentifier": "L5_YB_1.strain",
                "metricName": "\u5e94\u53d8\u503c",
                "productId": 1001,
                "productKey": "custom-monitor-strain-v1",
                "productName": "Custom strain sensor",
            },
            {
                "reason": "NO_CONFIRMED_THRESHOLD_TEMPLATE",
                "productType": "MONITORING",
                "metricIdentifier": "strain",
                "metricName": "\u5e94\u53d8\u503c",
                "productId": 1002,
                "productKey": "custom-monitor-strain-v1",
                "productName": "Custom strain sensor",
            },
        ], config)

        self.assertEqual(1, len(gaps))
        self.assertEqual("manual-strain:strain", gaps[0]["semanticTemplateKey"])
        self.assertEqual("strain", gaps[0]["metricIdentifier"])
        self.assertEqual("\u5e94\u53d8\u503c", gaps[0]["metricName"])
        self.assertEqual(["L5_YB_1.strain", "strain"], gaps[0]["metricAliases"])
        self.assertEqual(["custom-monitor-strain-v1"], gaps[0]["match"]["productKeys"])

    def test_manual_product_type_template_can_create_candidate_for_new_sensor(self):
        config = {
            "defaultDuration": 0,
            "defaultAlarmLevel": "orange",
            "defaultNotificationMethods": "",
            "defaultConvertToEvent": True,
            "productTypeTemplates": [
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "strain",
                    "metricName": "\u5e94\u53d8\u503c",
                    "metricAliases": ["L5_YB_1.strain", "strain"],
                    "match": {
                        "productKeys": ["custom-monitor-strain-v1"],
                        "metricNames": ["\u5e94\u53d8\u503c"],
                        "rawIdentifiers": ["L5_YB_1.strain", "strain"],
                    },
                    "expression": "value >= 50",
                    "confirmationStatus": "CONFIRMED",
                }
            ],
        }
        bindings = [
            {
                "id": 7001,
                "tenant_id": 1,
                "device_id": 8001,
                "risk_metric_id": 9101,
                "metric_identifier": "L5_YB_1.strain",
                "metric_name": "\u5e94\u53d8\u503c",
                "default_threshold": None,
                "product_id": 1001,
                "product_key": "custom-monitor-strain-v1",
                "product_name": "Custom strain sensor",
            }
        ]

        plan = backfill.build_plan(bindings, [], config)

        self.assertEqual(1, plan["candidateCount"])
        self.assertEqual("strain", plan["candidates"][0].metric_identifier)
        self.assertEqual("PRODUCT_TYPE_TEMPLATE", plan["candidates"][0].source)

    def test_attach_observed_value_stats_respects_template_match_conditions(self):
        gaps = [
            {
                "metricIdentifier": "value",
                "match": {
                    "rawIdentifiers": ["value"],
                    "productKeys": ["nf-monitor-laser-rangefinder-v1"],
                },
                "suggestedTemplate": {},
            },
            {
                "metricIdentifier": "value",
                "match": {
                    "rawIdentifiers": ["value"],
                    "productKeys": ["nf-monitor-crack-meter-v1"],
                },
                "suggestedTemplate": {},
            },
        ]
        rows = [
            {
                "productKey": "nf-monitor-laser-rangefinder-v1",
                "identifier": "value",
                "propertyValue": "10.5",
                "reportTime": "2026-04-27T10:00:00",
            },
            {
                "productKey": "nf-monitor-laser-rangefinder-v1",
                "identifier": "value",
                "propertyValue": "20.5",
                "reportTime": "2026-04-27T11:00:00",
            },
            {
                "productKey": "nf-monitor-crack-meter-v1",
                "identifier": "value",
                "propertyValue": "1.5",
                "reportTime": "2026-04-27T12:00:00",
            },
            {
                "productKey": "nf-monitor-crack-meter-v1",
                "identifier": "value",
                "propertyValue": "offline",
                "reportTime": "2026-04-27T13:00:00",
            },
        ]

        enriched = backfill.attach_observed_value_stats(gaps, rows)

        laser_stats = enriched[0]["observedValueStats"]
        crack_stats = enriched[1]["observedValueStats"]
        self.assertEqual(2, laser_stats["sampleCount"])
        self.assertEqual(2, laser_stats["numericCount"])
        self.assertEqual(10.5, laser_stats["min"])
        self.assertEqual(20.5, laser_stats["max"])
        self.assertEqual(15.5, laser_stats["avg"])
        self.assertEqual(2, crack_stats["sampleCount"])
        self.assertEqual(1, crack_stats["numericCount"])
        self.assertEqual(1.5, crack_stats["min"])
        self.assertEqual("2026-04-27T13:00:00", crack_stats["latestAt"])
        self.assertEqual(laser_stats, enriched[0]["suggestedTemplate"]["observedValueStats"])

    def test_product_type_template_match_conditions_prevent_value_cross_use(self):
        config = {
            "productTypeTemplates": [
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "value",
                    "metricName": "\u88c2\u7f1d\u91cf",
                    "metricAliases": ["L1_LF_1", "L1_LF_1.value", "value"],
                    "match": {
                        "productKeys": ["nf-monitor-crack-meter-v1"],
                        "metricNames": ["\u88c2\u7f1d\u503c", "\u88c2\u7f1d\u91cf"],
                        "rawIdentifiers": ["L1_LF_1", "L1_LF_1.value", "value"],
                    },
                    "expression": "value >= 10",
                    "confirmationStatus": "CONFIRMED",
                },
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "value",
                    "metricName": "\u6fc0\u5149\u6d4b\u8ddd\u503c",
                    "match": {
                        "productKeys": ["nf-monitor-laser-rangefinder-v1"],
                        "metricNames": ["\u6fc0\u5149\u6d4b\u8ddd\u503c"],
                        "rawIdentifiers": ["value"],
                    },
                    "expression": "value >= 20",
                    "confirmationStatus": "CONFIRMED",
                },
            ]
        }

        crack = backfill.find_product_type_template({
            "product_type": "MONITORING",
            "product_key": "nf-monitor-crack-meter-v1",
            "metric_identifier": "L1_LF_1.value",
            "metric_name": "\u88c2\u7f1d\u503c",
            "risk_metric_id": None,
        }, config)
        laser = backfill.find_product_type_template({
            "product_type": "MONITORING",
            "product_key": "nf-monitor-laser-rangefinder-v1",
            "metric_identifier": "value",
            "metric_name": "\u6fc0\u5149\u6d4b\u8ddd\u503c",
            "risk_metric_id": None,
        }, config)

        self.assertEqual("value >= 10", crack["expression"])
        self.assertEqual("value >= 20", laser["expression"])

    def test_write_reports_also_exports_template_draft_aliases(self):
        report = {
            "mode": "dry-run",
            "status": "PASSED",
            "checkedAt": "2026-04-27T22:30:00",
            "bindingTotal": 2,
            "riskBindingTotal": 2,
            "activeDeviceProductBindingCount": 2,
            "inactiveOrMissingDeviceProductCount": 0,
            "coveredBindingCount": 0,
            "candidateCount": 0,
            "appliedCount": 0,
            "skippedCount": 2,
            "skippedReasonCounts": {"NO_CONFIRMED_THRESHOLD_TEMPLATE": 2},
            "templateGapCount": 1,
            "templateValidation": {"readyTemplateCount": 0, "breachCount": 0, "breaches": []},
            "applyConfirmation": {"status": "NOT_APPLICABLE", "breachCount": 0, "breaches": []},
            "templateGaps": [
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "value",
                    "metricName": "裂缝值",
                    "bindingCount": 2,
                    "productCount": 1,
                    "sampleProducts": [{"productId": 1001, "productKey": "crack-v1", "productName": "裂缝计"}],
                    "suggestedTemplate": {
                        "productType": "MONITORING",
                        "metricIdentifier": "value",
                        "metricName": "裂缝值",
                        "expression": None,
                        "alarmLevel": "orange",
                        "duration": 0,
                        "convertToEvent": True,
                        "status": "NEEDS_CONFIRMATION",
                    },
                }
            ],
            "candidates": [],
            "skipped": [],
        }

        with tempfile.TemporaryDirectory() as temp_dir:
            backfill.write_reports(report, temp_dir)
            latest_draft_path = pathlib.Path(temp_dir) / "threshold-policy-template-draft-latest.json"
            latest_draft_md_path = pathlib.Path(temp_dir) / "threshold-policy-template-draft-latest.md"
            draft = json.loads(latest_draft_path.read_text(encoding="utf-8"))

            self.assertTrue(latest_draft_md_path.exists())
            self.assertEqual(1, len(draft["productTypeTemplates"]))
            self.assertEqual("NEEDS_CONFIRMATION", draft["productTypeTemplates"][0]["confirmationStatus"])
            self.assertEqual(2, draft["productTypeTemplates"][0]["evidence"]["bindingCount"])

    def test_template_validation_requires_confirmation_and_expression(self):
        validation = backfill.validate_template_config({
            "productTypeTemplates": [
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "value",
                    "metricName": "裂缝值",
                    "expression": None,
                    "confirmationStatus": "NEEDS_CONFIRMATION",
                }
            ]
        })

        self.assertEqual(0, validation["readyTemplateCount"])
        self.assertEqual(
            ["TEMPLATE_NOT_CONFIRMED", "INVALID_OR_MISSING_EXPRESSION"],
            [item["type"] for item in validation["breaches"]],
        )

    def test_template_validation_exposes_manual_confirmation_checklist(self):
        validation = backfill.validate_template_config({
            "productTypeTemplates": [
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "gpsTotalX",
                    "metricName": "X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                    "semanticTemplateKey": "gnss-total:gpsTotalX",
                    "expression": None,
                    "confirmationStatus": "NEEDS_CONFIRMATION",
                    "observedValueStats": {"min": -1.5, "max": 3.2, "numericCount": 10},
                    "evidence": {"bindingCount": 5, "productCount": 1},
                },
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "dispsX",
                    "metricName": "\u987a\u6ed1\u52a8\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                    "expression": "value >= 10",
                    "confirmationStatus": "CONFIRMED",
                    "observedValueStats": {"min": 0, "max": 8, "numericCount": 8},
                    "evidence": {"bindingCount": 2, "productCount": 1},
                },
            ]
        })

        checklist = validation["confirmationChecklist"]
        self.assertEqual(2, len(checklist))
        self.assertFalse(checklist[0]["ready"])
        self.assertEqual(["CONFIRM_TEMPLATE", "FILL_EXPRESSION"], checklist[0]["requiredActions"])
        self.assertEqual("-1.5..3.2 (n=10)", checklist[0]["observedRange"])
        self.assertEqual(5, checklist[0]["bindingCount"])
        self.assertTrue(checklist[1]["ready"])
        self.assertEqual([], checklist[1]["requiredActions"])

    def test_template_validation_markdown_lists_confirmation_checklist(self):
        markdown = backfill.render_template_validation_markdown({
            "status": "FAILED",
            "checkedAt": "2026-04-27T23:30:00",
            "configPath": "templates.json",
            "minReadyTemplateCount": 0,
            "templateValidation": {
                "templateCount": 1,
                "confirmedTemplateCount": 0,
                "readyTemplateCount": 0,
                "breachCount": 2,
                "breaches": [
                    {"type": "TEMPLATE_NOT_CONFIRMED", "index": 0},
                    {"type": "INVALID_OR_MISSING_EXPRESSION", "index": 0},
                ],
                "confirmationChecklist": [
                    {
                        "index": 0,
                        "productType": "MONITORING",
                        "metricIdentifier": "gpsTotalX",
                        "metricName": "X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                        "observedRange": "-1.5..3.2 (n=10)",
                        "bindingCount": 5,
                        "ready": False,
                        "requiredActions": ["CONFIRM_TEMPLATE", "FILL_EXPRESSION"],
                    }
                ],
            },
        })

        self.assertIn("## Manual Confirmation Checklist", markdown)
        self.assertIn("| 0 | MONITORING | gpsTotalX | X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf | -1.5..3.2 (n=10) | 5 | CONFIRM_TEMPLATE,FILL_EXPRESSION |", markdown)

    def test_unconfirmed_template_does_not_create_candidate_even_with_expression(self):
        config = {
            "productTypeTemplates": [
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "value",
                    "expression": "value >= 10",
                    "confirmationStatus": "NEEDS_CONFIRMATION",
                }
            ],
        }
        bindings = [
            {
                "id": 7001,
                "tenant_id": 1,
                "device_id": 8001,
                "risk_metric_id": 9101,
                "metric_identifier": "value",
                "metric_name": "value",
                "default_threshold": None,
                "product_id": 1001,
                "product_key": "nf-monitor-crack-meter-v1",
                "product_name": "Monitoring",
            }
        ]

        plan = backfill.build_plan(bindings, [], config)

        self.assertEqual(0, plan["candidateCount"])
        self.assertEqual(1, plan["skippedCount"])

    def test_apply_confirmation_requires_explicit_report_and_counts(self):
        args = backfill.parse_args(["--apply"])
        confirmation = backfill.validate_apply_confirmation(
            args,
            [],
            {"readyTemplateCount": 0, "breachCount": 0, "breaches": []},
        )

        self.assertEqual("FAILED", confirmation["status"])
        self.assertIn("MISSING_CONFIRM_REPORT", [item["type"] for item in confirmation["breaches"]])
        self.assertIn("MISSING_CONFIRM_CANDIDATE_COUNT", [item["type"] for item in confirmation["breaches"]])
        self.assertIn("NO_CANDIDATES_TO_APPLY", [item["type"] for item in confirmation["breaches"]])

    def test_apply_confirmation_accepts_matching_recent_dry_run_report(self):
        candidate = backfill.PolicyCandidate(
            rule_scope="PRODUCT_TYPE",
            tenant_id=1,
            metric_identifier="value",
            metric_name="裂缝值",
            expression="value >= 10",
            alarm_level="orange",
            duration=0,
            notification_methods="",
            convert_to_event=1,
            product_type="MONITORING",
            source="PRODUCT_TYPE_TEMPLATE",
        )
        with tempfile.TemporaryDirectory() as temp_dir:
            report_path = pathlib.Path(temp_dir) / "dry-run.json"
            report_path.write_text(json.dumps({
                "mode": "dry-run",
                "checkedAt": "2026-04-27T22:30:00",
                "candidateCount": 1,
                "templateValidation": {"readyTemplateCount": 1},
                "candidates": [backfill.candidate_to_dict(candidate)],
            }, ensure_ascii=False), encoding="utf-8")
            args = backfill.parse_args([
                "--apply",
                "--operator-id=1",
                f"--confirm-report={report_path}",
                "--confirm-candidate-count=1",
                "--confirm-ready-template-count=1",
                "--max-confirm-age-hours=99999",
            ])

            confirmation = backfill.validate_apply_confirmation(
                args,
                [candidate],
                {"readyTemplateCount": 1, "breachCount": 0, "breaches": []},
            )

        self.assertEqual("PASSED", confirmation["status"])
        self.assertEqual(0, confirmation["breachCount"])

    def test_template_validation_report_fails_when_ready_count_below_minimum(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            config_path = pathlib.Path(temp_dir) / "templates.json"
            config_path.write_text(json.dumps({
                "productTypeTemplates": [
                    {
                        "productType": "MONITORING",
                        "metricIdentifier": "value",
                        "expression": "value >= 10",
                        "confirmationStatus": "CONFIRMED",
                    }
                ]
            }), encoding="utf-8")
            args = backfill.parse_args([
                "--validate-config-only",
                f"--config-path={config_path}",
                "--min-ready-template-count=2",
            ])

            report = backfill.build_template_validation_report(args)

        self.assertEqual("FAILED", report["status"])
        self.assertIn(
            "READY_TEMPLATE_COUNT_BELOW_MINIMUM",
            [item["type"] for item in report["templateValidation"]["breaches"]],
        )

    def test_template_validation_report_passes_for_confirmed_templates(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            config_path = pathlib.Path(temp_dir) / "templates.json"
            config_path.write_text(json.dumps({
                "productTypeTemplates": [
                    {
                        "productType": "MONITORING",
                        "metricIdentifier": "value",
                        "expression": "value >= 10",
                        "confirmationStatus": "CONFIRMED",
                    }
                ]
            }), encoding="utf-8")
            args = backfill.parse_args([
                "--validate-config-only",
                f"--config-path={config_path}",
                "--min-ready-template-count=1",
            ])

            report = backfill.build_template_validation_report(args)

        self.assertEqual("PASSED", report["status"])
        self.assertEqual(1, report["templateValidation"]["readyTemplateCount"])

    def test_write_exported_template_config_creates_fillable_config(self):
        report = {
            "checkedAt": "2026-04-27T22:45:00",
            "reportPath": "logs/acceptance/example.json",
            "templateGaps": [
                {
                    "bindingCount": 1,
                    "productCount": 1,
                    "sampleProducts": [],
                    "suggestedTemplate": {
                        "productType": "MONITORING",
                        "metricIdentifier": "value",
                        "metricName": "裂缝值",
                        "expression": None,
                        "alarmLevel": "orange",
                        "duration": 0,
                        "convertToEvent": True,
                        "status": "NEEDS_CONFIRMATION",
                    },
                }
            ],
        }
        with tempfile.TemporaryDirectory() as temp_dir:
            target_path = pathlib.Path(temp_dir) / "threshold-policy-defaults.pending.json"

            backfill.write_exported_template_config(report, target_path)

            exported = json.loads(target_path.read_text(encoding="utf-8"))
        self.assertEqual(1, len(exported["productTypeTemplates"]))
        self.assertEqual("NEEDS_CONFIRMATION", exported["productTypeTemplates"][0]["confirmationStatus"])

    def test_write_template_confirmation_csv_exports_fillable_rows(self):
        template_draft = {
            "productTypeTemplates": [
                {
                    "productType": "MONITORING",
                    "semanticTemplateKey": "gnss-total:gpsTotalX",
                    "metricIdentifier": "gpsTotalX",
                    "metricName": "X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                    "metricAliases": ["L1_GP_1.gpsTotalX", "gpsTotalX"],
                    "match": {"rawIdentifiers": ["L1_GP_1.gpsTotalX", "gpsTotalX"]},
                    "expression": None,
                    "confirmationStatus": "NEEDS_CONFIRMATION",
                    "observedValueStats": {"min": -1.5, "max": 3.2, "numericCount": 10},
                    "evidence": {"bindingCount": 5, "productCount": 1},
                }
            ]
        }
        with tempfile.TemporaryDirectory() as temp_dir:
            path = pathlib.Path(temp_dir) / "confirmation.csv"
            backfill.write_template_confirmation_csv(template_draft, path)

            content = path.read_text(encoding="utf-8-sig")

        self.assertIn("expression", content)
        self.assertIn("confirmationStatus", content)
        self.assertIn("gnss-total:gpsTotalX", content)
        self.assertIn("-1.5..3.2 (n=10)", content)

    def test_write_reports_exports_confirmation_package_markdown(self):
        report = {
            "mode": "dry-run",
            "status": "PASSED",
            "checkedAt": "2026-04-27T22:30:00",
            "bindingTotal": 5,
            "riskBindingTotal": 5,
            "activeDeviceProductBindingCount": 5,
            "inactiveOrMissingDeviceProductCount": 0,
            "coveredBindingCount": 0,
            "candidateCount": 0,
            "appliedCount": 0,
            "skippedCount": 5,
            "skippedReasonCounts": {"NO_CONFIRMED_THRESHOLD_TEMPLATE": 5},
            "templateGapCount": 1,
            "templateValidation": {"readyTemplateCount": 0, "breachCount": 0, "breaches": []},
            "applyConfirmation": {"status": "NOT_APPLICABLE", "breachCount": 0, "breaches": []},
            "templateGaps": [
                {
                    "productType": "MONITORING",
                    "metricIdentifier": "gpsTotalX",
                    "metricName": "X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                    "semanticTemplateKey": "gnss-total:gpsTotalX",
                    "bindingCount": 5,
                    "productCount": 1,
                    "suggestedTemplate": {
                        "productType": "MONITORING",
                        "semanticTemplateKey": "gnss-total:gpsTotalX",
                        "metricIdentifier": "gpsTotalX",
                        "metricName": "X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                        "metricAliases": ["L1_GP_1.gpsTotalX", "gpsTotalX"],
                        "match": {"rawIdentifiers": ["L1_GP_1.gpsTotalX", "gpsTotalX"]},
                        "expression": None,
                        "confirmationStatus": "NEEDS_CONFIRMATION",
                        "observedValueStats": {"min": -1.5, "max": 3.2, "numericCount": 10},
                    },
                }
            ],
            "candidates": [],
            "skipped": [],
        }

        with tempfile.TemporaryDirectory() as temp_dir:
            backfill.write_reports(report, temp_dir)
            package_path = pathlib.Path(temp_dir) / "threshold-policy-template-confirmation-package-latest.md"
            content = package_path.read_text(encoding="utf-8")

        self.assertIn("Threshold Policy Template Confirmation Package", content)
        self.assertIn("threshold-policy-template-confirmation-latest.csv", content)
        self.assertIn("--merge-template-confirmation-csv", content)
        self.assertIn("threshold-policy-template-confirmation-merge-latest.md", content)
        self.assertIn("targetWritten=true", content)
        self.assertIn("will not write", content)
        self.assertIn("gnss-total:gpsTotalX", content)
        self.assertIn("CONFIRM_TEMPLATE,FILL_EXPRESSION", content)

    def test_merge_template_confirmation_csv_updates_only_confirmed_fields(self):
        config = {
            "productTypeTemplates": [
                {
                    "productType": "MONITORING",
                    "semanticTemplateKey": "gnss-total:gpsTotalX",
                    "metricIdentifier": "gpsTotalX",
                    "metricName": "X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                    "match": {"rawIdentifiers": ["gpsTotalX"]},
                    "expression": None,
                    "confirmationStatus": "NEEDS_CONFIRMATION",
                    "observedValueStats": {"min": -1.5, "max": 3.2, "numericCount": 10},
                }
            ]
        }
        with tempfile.TemporaryDirectory() as temp_dir:
            path = pathlib.Path(temp_dir) / "confirmation.csv"
            path.write_text(
                "semanticTemplateKey,productType,metricIdentifier,metricName,expression,confirmationStatus\n"
                "gnss-total:gpsTotalX,MONITORING,gpsTotalX,X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf,value >= 10,CONFIRMED\n",
                encoding="utf-8",
            )

            merged = backfill.merge_template_confirmation_csv(config, path)

        template = merged["productTypeTemplates"][0]
        self.assertEqual("value >= 10", template["expression"])
        self.assertEqual("CONFIRMED", template["confirmationStatus"])
        self.assertEqual({"rawIdentifiers": ["gpsTotalX"]}, template["match"])
        self.assertEqual({"min": -1.5, "max": 3.2, "numericCount": 10}, template["observedValueStats"])

    def test_write_merged_template_confirmation_config_writes_auditable_merge_report(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = pathlib.Path(temp_dir)
            config_path = root / "pending.json"
            csv_path = root / "confirmation.csv"
            output_path = root / "confirmed.json"
            config_path.write_text(json.dumps({
                "productTypeTemplates": [
                    {
                        "productType": "MONITORING",
                        "semanticTemplateKey": "gnss-total:gpsTotalX",
                        "metricIdentifier": "gpsTotalX",
                        "metricName": "X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                        "expression": None,
                        "confirmationStatus": "NEEDS_CONFIRMATION",
                    }
                ]
            }, ensure_ascii=False), encoding="utf-8")
            csv_path.write_text(
                "semanticTemplateKey,productType,metricIdentifier,metricName,expression,confirmationStatus\n"
                "gnss-total:gpsTotalX,MONITORING,gpsTotalX,X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf,value >= 10,CONFIRMED\n"
                "unknown-template,MONITORING,unknown,unknown,value >= 20,CONFIRMED\n",
                encoding="utf-8",
            )

            result_path = backfill.write_merged_template_confirmation_config(
                config_path,
                csv_path,
                output_path,
                root,
            )

            self.assertEqual(output_path, result_path)
            latest_report = root / "threshold-policy-template-confirmation-merge-latest.json"
            latest_markdown = root / "threshold-policy-template-confirmation-merge-latest.md"
            report = json.loads(latest_report.read_text(encoding="utf-8"))
            self.assertTrue(latest_markdown.exists())
            self.assertEqual("WARNING", report["status"])
            self.assertEqual(2, report["csvRowCount"])
            self.assertEqual(1, report["matchedRowCount"])
            self.assertEqual(1, report["unmatchedRowCount"])
            self.assertEqual(1, report["updatedExpressionCount"])
            self.assertEqual(1, report["updatedConfirmationStatusCount"])
            self.assertEqual(1, report["templateValidation"]["readyTemplateCount"])
            self.assertEqual("semantic:unknown-template", report["unmatchedRows"][0]["identity"])

    def test_merge_confirmation_report_fails_for_duplicate_and_invalid_csv_rows(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = pathlib.Path(temp_dir)
            config_path = root / "pending.json"
            csv_path = root / "confirmation.csv"
            output_path = root / "confirmed.json"
            config_path.write_text(json.dumps({
                "productTypeTemplates": [
                    {
                        "productType": "MONITORING",
                        "semanticTemplateKey": "gnss-total:gpsTotalX",
                        "metricIdentifier": "gpsTotalX",
                        "metricName": "X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                        "expression": None,
                        "confirmationStatus": "NEEDS_CONFIRMATION",
                    },
                    {
                        "productType": "MONITORING",
                        "semanticTemplateKey": "axis-acceleration:gY",
                        "metricIdentifier": "gY",
                        "metricName": "Y\u8f74\u52a0\u901f\u5ea6",
                        "expression": None,
                        "confirmationStatus": "NEEDS_CONFIRMATION",
                    }
                ]
            }, ensure_ascii=False), encoding="utf-8")
            csv_path.write_text(
                "semanticTemplateKey,productType,metricIdentifier,metricName,expression,confirmationStatus\n"
                "gnss-total:gpsTotalX,MONITORING,gpsTotalX,X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf,value >= 10,CONFIRMED\n"
                "gnss-total:gpsTotalX,MONITORING,gpsTotalX,X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf,value >= 20,CONFIRMED\n"
                "axis-acceleration:gY,MONITORING,gY,Y\u8f74\u52a0\u901f\u5ea6,value >= 30,CONFIRM\n",
                encoding="utf-8",
            )

            backfill.write_merged_template_confirmation_config(
                config_path,
                csv_path,
                output_path,
                root,
            )

            report = json.loads(
                (root / "threshold-policy-template-confirmation-merge-latest.json").read_text(encoding="utf-8")
            )
            markdown = (root / "threshold-policy-template-confirmation-merge-latest.md").read_text(encoding="utf-8")
            self.assertEqual("FAILED", report["status"])
            self.assertEqual(1, report["duplicateRowCount"])
            self.assertEqual(1, report["invalidConfirmationStatusCount"])
            self.assertEqual(2, report["duplicateRows"][0]["row"])
            self.assertEqual("CONFIRM", report["invalidConfirmationStatusRows"][0]["confirmationStatus"])
            self.assertIn("Duplicate Rows", markdown)
            self.assertIn("Invalid Confirmation Status Rows", markdown)

    def test_merge_confirmation_report_fails_for_confirmed_rows_with_invalid_expressions(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = pathlib.Path(temp_dir)
            config_path = root / "pending.json"
            csv_path = root / "confirmation.csv"
            output_path = root / "confirmed.json"
            config_path.write_text(json.dumps({
                "productTypeTemplates": [
                    {
                        "productType": "MONITORING",
                        "semanticTemplateKey": "gnss-total:gpsTotalX",
                        "metricIdentifier": "gpsTotalX",
                        "metricName": "X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                        "expression": None,
                        "confirmationStatus": "NEEDS_CONFIRMATION",
                    },
                    {
                        "productType": "MONITORING",
                        "semanticTemplateKey": "gnss-total:gpsTotalY",
                        "metricIdentifier": "gpsTotalY",
                        "metricName": "Y\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                        "expression": None,
                        "confirmationStatus": "NEEDS_CONFIRMATION",
                    },
                ]
            }, ensure_ascii=False), encoding="utf-8")
            csv_path.write_text(
                "semanticTemplateKey,productType,metricIdentifier,metricName,expression,confirmationStatus\n"
                "gnss-total:gpsTotalX,MONITORING,gpsTotalX,X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf,,CONFIRMED\n"
                "gnss-total:gpsTotalY,MONITORING,gpsTotalY,Y\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf,abs(value) > 10,CONFIRMED\n",
                encoding="utf-8",
            )

            backfill.write_merged_template_confirmation_config(
                config_path,
                csv_path,
                output_path,
                root,
            )

            report = json.loads(
                (root / "threshold-policy-template-confirmation-merge-latest.json").read_text(encoding="utf-8")
            )
            markdown = (root / "threshold-policy-template-confirmation-merge-latest.md").read_text(encoding="utf-8")
            self.assertEqual("FAILED", report["status"])
            self.assertEqual(2, report["invalidExpressionCount"])
            self.assertEqual("MISSING_EXPRESSION", report["invalidExpressionRows"][0]["reason"])
            self.assertEqual("UNSUPPORTED_EXPRESSION", report["invalidExpressionRows"][1]["reason"])
            self.assertIn("Invalid Expression Rows", markdown)
            self.assertIn("UNSUPPORTED_EXPRESSION", markdown)

    def test_merge_confirmation_report_fails_for_missing_required_csv_headers(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = pathlib.Path(temp_dir)
            config_path = root / "pending.json"
            csv_path = root / "confirmation.csv"
            output_path = root / "confirmed.json"
            config_path.write_text(json.dumps({
                "productTypeTemplates": [
                    {
                        "productType": "MONITORING",
                        "semanticTemplateKey": "gnss-total:gpsTotalX",
                        "metricIdentifier": "gpsTotalX",
                        "metricName": "X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                        "expression": None,
                        "confirmationStatus": "NEEDS_CONFIRMATION",
                    }
                ]
            }, ensure_ascii=False), encoding="utf-8")
            csv_path.write_text(
                "semanticTemplateKey,productType,metricIdentifier,metricName\n"
                "gnss-total:gpsTotalX,MONITORING,gpsTotalX,X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf\n",
                encoding="utf-8",
            )

            backfill.write_merged_template_confirmation_config(
                config_path,
                csv_path,
                output_path,
                root,
            )

            report = json.loads(
                (root / "threshold-policy-template-confirmation-merge-latest.json").read_text(encoding="utf-8")
            )
            markdown = (root / "threshold-policy-template-confirmation-merge-latest.md").read_text(encoding="utf-8")
            self.assertEqual("FAILED", report["status"])
            self.assertFalse(report["targetWritten"])
            self.assertFalse(output_path.exists())
            self.assertEqual(2, report["missingHeaderCount"])
            self.assertEqual(["expression", "confirmationStatus"], report["missingHeaders"])
            self.assertIn("Missing Required Headers", markdown)
            self.assertIn("confirmationStatus", markdown)

    def test_merge_cli_returns_nonzero_when_merge_report_failed(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = pathlib.Path(temp_dir)
            config_path = root / "pending.json"
            csv_path = root / "confirmation.csv"
            output_path = root / "confirmed.json"
            config_path.write_text(json.dumps({
                "productTypeTemplates": [
                    {
                        "productType": "MONITORING",
                        "semanticTemplateKey": "gnss-total:gpsTotalX",
                        "metricIdentifier": "gpsTotalX",
                        "metricName": "X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                        "expression": None,
                        "confirmationStatus": "NEEDS_CONFIRMATION",
                    }
                ]
            }, ensure_ascii=False), encoding="utf-8")
            csv_path.write_text(
                "semanticTemplateKey,productType,metricIdentifier,metricName,expression,confirmationStatus\n"
                "gnss-total:gpsTotalX,MONITORING,gpsTotalX,X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf,,CONFIRMED\n",
                encoding="utf-8",
            )

            output = io.StringIO()
            with redirect_stdout(output):
                exit_code = backfill.main([
                    f"--merge-template-confirmation-csv={csv_path}",
                    f"--config-path={config_path}",
                    f"--merge-template-output-path={output_path}",
                    f"--output-dir={root}",
                ])

            report = json.loads(
                (root / "threshold-policy-template-confirmation-merge-latest.json").read_text(encoding="utf-8")
            )
            self.assertEqual(1, exit_code)
            self.assertEqual("FAILED", report["status"])
            self.assertFalse(report["targetWritten"])
            self.assertFalse(output_path.exists())
            self.assertIn("TARGET_WRITTEN=false", output.getvalue())
            self.assertIn("TEMPLATE_CONFIG_PATH_NOT_WRITTEN=", output.getvalue())
            self.assertNotIn("TEMPLATE_CONFIG_PATH=", output.getvalue())

    def test_merge_cli_returns_zero_when_merge_report_passed(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = pathlib.Path(temp_dir)
            config_path = root / "pending.json"
            csv_path = root / "confirmation.csv"
            output_path = root / "confirmed.json"
            config_path.write_text(json.dumps({
                "productTypeTemplates": [
                    {
                        "productType": "MONITORING",
                        "semanticTemplateKey": "gnss-total:gpsTotalX",
                        "metricIdentifier": "gpsTotalX",
                        "metricName": "X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf",
                        "expression": None,
                        "confirmationStatus": "NEEDS_CONFIRMATION",
                    }
                ]
            }, ensure_ascii=False), encoding="utf-8")
            csv_path.write_text(
                "semanticTemplateKey,productType,metricIdentifier,metricName,expression,confirmationStatus\n"
                "gnss-total:gpsTotalX,MONITORING,gpsTotalX,X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf,value >= 10,CONFIRMED\n",
                encoding="utf-8",
            )

            output = io.StringIO()
            with redirect_stdout(output):
                exit_code = backfill.main([
                    f"--merge-template-confirmation-csv={csv_path}",
                    f"--config-path={config_path}",
                    f"--merge-template-output-path={output_path}",
                    f"--output-dir={root}",
                ])

            report = json.loads(
                (root / "threshold-policy-template-confirmation-merge-latest.json").read_text(encoding="utf-8")
            )
            self.assertEqual(0, exit_code)
            self.assertEqual("PASSED", report["status"])
            self.assertTrue(report["targetWritten"])
            self.assertTrue(output_path.exists())
            self.assertIn("TARGET_WRITTEN=true", output.getvalue())
            self.assertIn(f"TEMPLATE_CONFIG_PATH={output_path}", output.getvalue())
            self.assertEqual(1, report["templateValidation"]["readyTemplateCount"])


if __name__ == "__main__":
    unittest.main()
