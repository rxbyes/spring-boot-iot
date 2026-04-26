#!/usr/bin/env python3
"""Tests for monitoring risk metric catalog audit script."""

from __future__ import annotations

import importlib.util
import os
import sys
import tempfile
import types
import unittest
from pathlib import Path
from unittest import mock


SCRIPT_PATH = Path(__file__).resolve().parents[1] / "audit-monitoring-risk-metric-catalog.py"


def load_module():
    spec = importlib.util.spec_from_file_location("audit_monitoring_risk_metric_catalog", SCRIPT_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load module from {SCRIPT_PATH}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class MonitoringRiskMetricCatalogAuditScriptTest(unittest.TestCase):
    def test_resolve_connection_args_prefers_cli_then_env_then_defaults(self) -> None:
        module = load_module()
        args = types.SimpleNamespace(
            jdbc_url=None,
            user=None,
            password=None,
        )
        defaults = {
            "jdbc_url": "jdbc:mysql://default-host:3306/default_db",
            "user": "default-user",
            "password": "default-pass",
        }
        env = {
            "IOT_MYSQL_URL": "jdbc:mysql://env-host:3307/env_db",
            "IOT_MYSQL_USERNAME": "env-user",
            "IOT_MYSQL_PASSWORD": "env-pass",
        }
        with mock.patch.dict(os.environ, env, clear=False):
            with mock.patch.object(module, "load_dev_defaults", return_value=defaults):
                resolved = module.resolve_connection_args(args)
        self.assertEqual("env-host", resolved["host"])
        self.assertEqual(3307, resolved["port"])
        self.assertEqual("env_db", resolved["database"])
        self.assertEqual("env-user", resolved["user"])
        self.assertEqual("env-pass", resolved["password"])

        args.jdbc_url = "jdbc:mysql://cli-host:3308/cli_db"
        args.user = "cli-user"
        args.password = "cli-pass"
        with mock.patch.dict(os.environ, env, clear=False):
            with mock.patch.object(module, "load_dev_defaults", return_value=defaults):
                resolved = module.resolve_connection_args(args)
        self.assertEqual("cli-host", resolved["host"])
        self.assertEqual(3308, resolved["port"])
        self.assertEqual("cli_db", resolved["database"])
        self.assertEqual("cli-user", resolved["user"])
        self.assertEqual("cli-pass", resolved["password"])

    def test_classify_product_audit_marks_gnss_monitor_as_needs_backfill(self) -> None:
        module = load_module()
        product = {
            "id": 2004,
            "product_key": "nf-monitor-gnss-monitor-v1",
            "product_name": "南方测绘 监测型 GNSS位移监测仪",
            "release_batch_count": 4,
            "latest_release_time": None,
        }
        property_models = [
            {"identifier": "L1_GP_1.gpsTotalX", "model_name": "X方向累计变形量", "description": None},
            {"identifier": "L1_GP_1.gpsTotalY", "model_name": "Y方向累计变形量", "description": None},
            {"identifier": "L1_GP_1.gpsTotalZ", "model_name": "Z方向累计变形量", "description": None},
            {"identifier": "L1_JS_1.gX", "model_name": "X轴加速度", "description": None},
        ]

        result = module.classify_product_audit(product, property_models, [])

        self.assertEqual(module.STATUS_NEEDS_BACKFILL, result["auditStatus"])
        self.assertEqual(
            ["L1_GP_1.gpsTotalX", "L1_GP_1.gpsTotalY", "L1_GP_1.gpsTotalZ"],
            result["expectedRiskIdentifiers"],
        )
        self.assertEqual(
            ["L1_GP_1.gpsTotalX", "L1_GP_1.gpsTotalY", "L1_GP_1.gpsTotalZ"],
            result["missingCatalogIdentifiers"],
        )

    def test_classify_product_audit_marks_multi_displacement_without_batches_as_self_heal_only(self) -> None:
        module = load_module()
        product = {
            "id": 2002,
            "product_key": "zhd-monitor-multi-displacement-v1",
            "product_name": "中海达 监测型 多维位移监测仪",
            "release_batch_count": 0,
            "latest_release_time": None,
        }
        property_models = [
            {"identifier": "L1_LF_1.value", "model_name": "裂缝量", "description": None},
            {"identifier": "L1_QJ_1.angle", "model_name": "水平面夹角", "description": None},
            {"identifier": "L1_JS_1.gX", "model_name": "X轴加速度", "description": None},
        ]

        result = module.classify_product_audit(product, property_models, [])

        self.assertEqual(module.STATUS_SELF_HEAL_ONLY, result["auditStatus"])
        self.assertEqual(["L1_LF_1.value"], result["expectedRiskIdentifiers"])

    def test_classify_product_audit_treats_mud_level_and_bare_crack_placeholder_as_expected_empty(self) -> None:
        module = load_module()
        mud_product = {
            "id": 2006,
            "product_key": "nf-monitor-mud-level-meter-v1",
            "product_name": "南方测绘 监测型 泥位计",
            "release_batch_count": 1,
            "latest_release_time": None,
        }
        bare_crack_product = {
            "id": 2005,
            "product_key": "nf-monitor-crack-meter-v1",
            "product_name": "南方测绘 监测型 裂缝计",
            "release_batch_count": 2,
            "latest_release_time": None,
        }

        mud_result = module.classify_product_audit(
            mud_product,
            [{"identifier": "L4_NW_1", "model_name": "泥位高程值", "description": None}],
            [],
        )
        crack_result = module.classify_product_audit(
            bare_crack_product,
            [{"identifier": "L1_LF_1", "model_name": "裂缝值", "description": None}],
            [],
        )

        self.assertEqual(module.STATUS_EXPECTED_EMPTY, mud_result["auditStatus"])
        self.assertEqual([], mud_result["expectedRiskIdentifiers"])
        self.assertEqual(module.STATUS_EXPECTED_EMPTY, crack_result["auditStatus"])
        self.assertEqual([], crack_result["expectedRiskIdentifiers"])

    def test_classify_product_audit_marks_unexpected_catalog_rows_as_rule_drift(self) -> None:
        module = load_module()
        product = {
            "id": 2003,
            "product_key": "nf-monitor-gnss-base-station-v1",
            "product_name": "南方测绘 监测型 GNSS基准站",
            "release_batch_count": 0,
            "latest_release_time": None,
        }
        catalog_rows = [
            {
                "contract_identifier": "L1_GP_1.gpsTotalX",
                "normative_identifier": "gpsTotalX",
                "source_scenario_code": "phase2-gnss",
                "enabled": 1,
                "lifecycle_status": "PUBLISHED",
            }
        ]

        result = module.classify_product_audit(product, [], catalog_rows)

        self.assertEqual(module.STATUS_RULE_DRIFT, result["auditStatus"])
        self.assertEqual(["L1_GP_1.gpsTotalX"], result["unexpectedCatalogIdentifiers"])

    def test_build_repair_plan_generates_upserts_and_retires_from_audit_results(self) -> None:
        module = load_module()
        product = {
            "id": 2004,
            "tenant_id": 1,
            "product_key": "nf-monitor-gnss-monitor-v1",
            "product_name": "南方测绘 监测型 GNSS位移监测仪",
            "manufacturer": "南方测绘",
            "description": None,
            "release_batch_count": 4,
            "latest_release_batch_id": 7001,
            "latest_release_time": None,
        }
        property_models_by_product = {
            2004: [
                {
                    "id": 4101,
                    "identifier": "L1_GP_1.gpsTotalX",
                    "model_name": "X方向累计变形量",
                    "description": None,
                    "data_type": "double",
                    "specs_json": None,
                },
                {
                    "id": 4102,
                    "identifier": "L1_GP_1.gpsTotalY",
                    "model_name": "Y方向累计变形量",
                    "description": None,
                    "data_type": "double",
                    "specs_json": None,
                },
            ]
        }
        catalog_rows_by_product = {
            2004: [
                {
                    "id": 5101,
                    "contract_identifier": "L1_GP_1.gpsLegacy",
                    "enabled": 1,
                }
            ]
        }
        report = {
            "generatedAt": "2026-04-26T01:00:00",
            "scopeDescription": "nf-monitor-gnss-monitor-v1",
            "summary": {
                "TOTAL": 1,
                module.STATUS_RULE_DRIFT: 1,
            },
            "products": [
                {
                    "productId": 2004,
                    "productKey": "nf-monitor-gnss-monitor-v1",
                    "productName": "南方测绘 监测型 GNSS位移监测仪",
                    "auditStatus": module.STATUS_RULE_DRIFT,
                    "missingCatalogIdentifiers": ["L1_GP_1.gpsTotalX", "L1_GP_1.gpsTotalY"],
                    "unexpectedCatalogIdentifiers": ["L1_GP_1.gpsLegacy"],
                    "notes": ["当前启用目录缺少按规则应发布的正式字段。"],
                }
            ],
        }
        normative_definitions_by_scenario = {
            "phase2-gnss": {
                "gpsTotalX": {
                    "scenario_code": "phase2-gnss",
                    "identifier": "gpsTotalX",
                    "unit": "mm",
                    "monitor_content_code": "DISPLACEMENT",
                    "monitor_type_code": "GNSS",
                    "trend_enabled": 1,
                    "metric_dimension": "distance",
                    "threshold_type": "absolute",
                    "semantic_direction": "HIGHER_IS_RISKIER",
                    "gis_enabled": 0,
                    "insight_enabled": 1,
                    "analytics_enabled": 1,
                    "status": "ACTIVE",
                    "metadata_json": '{"riskCategory":"GNSS","metricRole":"PRIMARY"}',
                },
                "gpsTotalY": {
                    "scenario_code": "phase2-gnss",
                    "identifier": "gpsTotalY",
                    "unit": "mm",
                    "monitor_content_code": "DISPLACEMENT",
                    "monitor_type_code": "GNSS",
                    "trend_enabled": 1,
                    "metric_dimension": "distance",
                    "threshold_type": "absolute",
                    "semantic_direction": "HIGHER_IS_RISKIER",
                    "gis_enabled": 0,
                    "insight_enabled": 1,
                    "analytics_enabled": 1,
                    "status": "ACTIVE",
                    "metadata_json": '{"riskCategory":"GNSS","metricRole":"PRIMARY"}',
                },
            }
        }

        plan = module.build_repair_plan(
            report,
            [product],
            property_models_by_product,
            catalog_rows_by_product,
            normative_definitions_by_scenario,
        )

        self.assertEqual(1, plan["summary"]["eligibleProductCount"])
        self.assertEqual(2, plan["summary"]["upsertCount"])
        self.assertEqual(1, plan["summary"]["retireCount"])
        actions = plan["products"][0]["actions"]
        first_upsert = actions["upserts"][0]["row"]
        self.assertEqual(1, first_upsert["tenantId"])
        self.assertEqual(7001, first_upsert["releaseBatchId"])
        self.assertEqual("L1_GP_1.gpsTotalX", first_upsert["contractIdentifier"])
        self.assertEqual("gpsTotalX", first_upsert["normativeIdentifier"])
        self.assertEqual("phase2-gnss", first_upsert["sourceScenarioCode"])
        self.assertEqual("RM_2004_L1_GP_1_GPSTOTALX", first_upsert["riskMetricCode"])
        self.assertEqual("GNSS", first_upsert["riskCategory"])
        self.assertEqual("PRIMARY", first_upsert["metricRole"])
        self.assertEqual("L1_GP_1.gpsLegacy", actions["retires"][0]["contractIdentifier"])

    def test_build_repair_plan_skips_self_heal_only_products(self) -> None:
        module = load_module()
        report = {
            "generatedAt": "2026-04-26T01:05:00",
            "scopeDescription": "zhd-monitor-multi-displacement-v1",
            "summary": {
                "TOTAL": 1,
                module.STATUS_SELF_HEAL_ONLY: 1,
            },
            "products": [
                {
                    "productId": 2002,
                    "productKey": "zhd-monitor-multi-displacement-v1",
                    "productName": "中海达 监测型 多维位移监测仪",
                    "auditStatus": module.STATUS_SELF_HEAL_ONLY,
                    "missingCatalogIdentifiers": ["L1_LF_1.value"],
                    "unexpectedCatalogIdentifiers": [],
                    "notes": ["当前无正式发布批次，风险绑定依赖 formal-metrics 读侧自愈。"],
                }
            ],
        }

        plan = module.build_repair_plan(report, [], {}, {}, {})

        self.assertEqual(0, plan["summary"]["eligibleProductCount"])
        self.assertEqual(1, plan["summary"]["skippedSelfHealOnlyCount"])
        self.assertEqual("zhd-monitor-multi-displacement-v1", plan["skippedProducts"][0]["productKey"])

    def test_write_reports_persists_json_and_markdown(self) -> None:
        module = load_module()
        report = {
            "generatedAt": "2026-04-25T23:55:00",
            "scopeDescription": "all monitoring products",
            "summary": {
                "TOTAL": 2,
                module.STATUS_PUBLISHED_OK: 1,
                module.STATUS_NEEDS_BACKFILL: 1,
            },
            "products": [
                {
                    "productKey": "nf-monitor-gnss-monitor-v1",
                    "auditStatus": module.STATUS_NEEDS_BACKFILL,
                    "releaseBatchCount": 4,
                    "expectedRiskIdentifiers": ["L1_GP_1.gpsTotalX"],
                    "currentCatalogIdentifiers": [],
                    "notes": ["已有正式发布批次，但当前启用目录为空，建议补一次目录回填。"],
                },
                {
                    "productKey": "nf-monitor-deep-displacement-v1",
                    "auditStatus": module.STATUS_PUBLISHED_OK,
                    "releaseBatchCount": 3,
                    "expectedRiskIdentifiers": ["dispsX", "dispsY"],
                    "currentCatalogIdentifiers": ["dispsX", "dispsY"],
                    "notes": [],
                },
            ],
        }
        with tempfile.TemporaryDirectory(prefix="monitoring-risk-catalog-audit-") as temp_dir:
            outputs = module.write_reports(report, Path(temp_dir))
            json_content = Path(outputs["json"]).read_text(encoding="utf-8")
            md_content = Path(outputs["markdown"]).read_text(encoding="utf-8")

        self.assertIn("nf-monitor-gnss-monitor-v1", json_content)
        self.assertIn("Monitoring Risk Metric Catalog Audit", md_content)
        self.assertIn(module.STATUS_NEEDS_BACKFILL, md_content)

    def test_write_repair_plan_persists_json_and_markdown(self) -> None:
        module = load_module()
        plan = {
            "generatedAt": "2026-04-26T01:10:00",
            "scopeDescription": "nf-monitor-gnss-monitor-v1",
            "summary": {
                "eligibleProductCount": 1,
                "skippedSelfHealOnlyCount": 0,
                "upsertCount": 2,
                "retireCount": 1,
            },
            "products": [
                {
                    "productKey": "nf-monitor-gnss-monitor-v1",
                    "auditStatus": module.STATUS_RULE_DRIFT,
                    "latestReleaseBatchId": 7001,
                    "actions": {
                        "upserts": [{"row": {"contractIdentifier": "L1_GP_1.gpsTotalX"}}],
                        "retires": [{"contractIdentifier": "L1_GP_1.gpsLegacy"}],
                    },
                }
            ],
            "skippedProducts": [],
        }
        with tempfile.TemporaryDirectory(prefix="monitoring-risk-catalog-repair-") as temp_dir:
            outputs = module.write_repair_plan(plan, Path(temp_dir))
            json_content = Path(outputs["json"]).read_text(encoding="utf-8")
            md_content = Path(outputs["markdown"]).read_text(encoding="utf-8")

        self.assertIn("nf-monitor-gnss-monitor-v1", json_content)
        self.assertIn("Monitoring Risk Metric Catalog Repair Plan", md_content)
        self.assertIn("upsertCount: 2", md_content)


if __name__ == "__main__":
    unittest.main()
