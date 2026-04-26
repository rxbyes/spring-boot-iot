#!/usr/bin/env python3
"""Tests for monitoring risk metric catalog audit script."""

from __future__ import annotations

import importlib.util
import json
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


def make_product(
    *,
    product_id: int,
    product_key: str,
    product_name: str,
    release_batch_count: int,
    metadata_json: dict | None = None,
    latest_release_batch_id: int | None = None,
    **extra: object,
) -> dict[str, object]:
    return {
        "id": product_id,
        "product_key": product_key,
        "product_name": product_name,
        "release_batch_count": release_batch_count,
        "latest_release_batch_id": latest_release_batch_id,
        "latest_release_time": None,
        "metadata_json": json.dumps(metadata_json, ensure_ascii=False) if metadata_json is not None else None,
        **extra,
    }


def make_contract(
    identifier: str,
    *,
    contract_id: int | None = None,
    model_name: str | None = None,
    description: str | None = None,
    data_type: str | None = None,
    specs_json: str | None = None,
) -> dict[str, object]:
    row: dict[str, object] = {
        "identifier": identifier,
        "model_name": model_name,
        "description": description,
    }
    if contract_id is not None:
        row["id"] = contract_id
    if data_type is not None:
        row["data_type"] = data_type
    if specs_json is not None:
        row["specs_json"] = specs_json
    return row


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

    def test_resolve_risk_enabled_identifiers_uses_measure_truth_from_product_metadata(self) -> None:
        module = load_module()
        product = make_product(
            product_id=2001,
            product_key="generic-monitor-v1",
            product_name="通用监测产品",
            release_batch_count=2,
            metadata_json={
                "objectInsight": {
                    "customMetrics": [
                        {"identifier": "L1_QJ_1.angle", "group": "measure", "enabled": True, "includeInTrend": True},
                        {"identifier": "L1_GP_1.gpsTotalX", "group": "measure"},
                        {"identifier": "S1_ZT_1.sensor_state", "group": "status", "enabled": True, "includeInTrend": True},
                        {"identifier": "L1_JS_1.gX", "group": "measure", "enabled": False, "includeInTrend": True},
                        {"identifier": "L1_LF_1.value", "group": "measure", "enabled": True, "includeInTrend": False},
                        {"identifier": "missing.contract", "group": "measure", "enabled": True, "includeInTrend": True},
                    ]
                }
            },
        )
        property_models = [
            make_contract("L1_QJ_1.angle"),
            make_contract("L1_GP_1.gpsTotalX"),
            make_contract("S1_ZT_1.sensor_state"),
            make_contract("L1_JS_1.gX"),
            make_contract("L1_LF_1.value"),
        ]

        identifiers = module.resolve_risk_enabled_identifiers(product, property_models)

        self.assertEqual(["L1_QJ_1.angle", "L1_GP_1.gpsTotalX"], identifiers)

    def test_classify_product_audit_marks_measure_truth_gap_as_needs_backfill(self) -> None:
        module = load_module()
        product = make_product(
            product_id=2002,
            product_key="generic-monitor-v1",
            product_name="通用监测产品",
            release_batch_count=4,
            metadata_json={
                "objectInsight": {
                    "customMetrics": [
                        {"identifier": "L1_QJ_1.angle", "group": "measure", "enabled": True, "includeInTrend": True}
                    ]
                }
            },
        )
        property_models = [make_contract("L1_QJ_1.angle", model_name="水平面夹角")]

        result = module.classify_product_audit(product, property_models, [])

        self.assertEqual(module.STATUS_NEEDS_BACKFILL, result["auditStatus"])
        self.assertEqual(["L1_QJ_1.angle"], result["expectedRiskIdentifiers"])
        self.assertEqual(["L1_QJ_1.angle"], result["missingCatalogIdentifiers"])
        self.assertIn("设为监测数据", result["notes"][0])

    def test_classify_product_audit_treats_non_measure_or_cancelled_trend_as_expected_empty(self) -> None:
        module = load_module()
        product = make_product(
            product_id=2003,
            product_key="generic-monitor-v1",
            product_name="通用监测产品",
            release_batch_count=2,
            metadata_json={
                "objectInsight": {
                    "customMetrics": [
                        {"identifier": "L1_LF_1.value", "group": "measure", "enabled": True, "includeInTrend": False},
                        {"identifier": "S1_ZT_1.sensor_state", "group": "status", "enabled": True, "includeInTrend": True},
                        {"identifier": "R1_CPU.temp", "group": "runtime", "enabled": True, "includeInTrend": True},
                    ]
                }
            },
        )
        result = module.classify_product_audit(
            product,
            [
                make_contract("L1_LF_1.value", model_name="裂缝量"),
                make_contract("S1_ZT_1.sensor_state", model_name="传感器状态"),
                make_contract("R1_CPU.temp", model_name="CPU 温度"),
            ],
            [],
        )

        self.assertEqual(module.STATUS_EXPECTED_EMPTY, result["auditStatus"])
        self.assertEqual([], result["expectedRiskIdentifiers"])

    def test_classify_product_audit_marks_lingering_catalog_rows_after_cancel_trend_as_rule_drift(self) -> None:
        module = load_module()
        product = make_product(
            product_id=2004,
            product_key="generic-monitor-v1",
            product_name="通用监测产品",
            release_batch_count=3,
            metadata_json={
                "objectInsight": {
                    "customMetrics": [
                        {"identifier": "L1_LF_1.value", "group": "measure", "enabled": True, "includeInTrend": False}
                    ]
                }
            },
        )
        catalog_rows = [
            {
                "contract_identifier": "L1_LF_1.value",
                "enabled": 1,
                "lifecycle_status": "ACTIVE",
            }
        ]

        result = module.classify_product_audit(product, [make_contract("L1_LF_1.value")], catalog_rows)

        self.assertEqual(module.STATUS_RULE_DRIFT, result["auditStatus"])
        self.assertEqual(["L1_LF_1.value"], result["unexpectedCatalogIdentifiers"])
        self.assertIn("取消趋势展示", result["notes"][0])

    def test_build_repair_plan_generates_upserts_and_retires_from_measure_truth_audit_results(self) -> None:
        module = load_module()
        product = make_product(
            product_id=2005,
            product_key="nf-monitor-gnss-monitor-v1",
            product_name="南方测绘 监测型 GNSS位移监测仪",
            release_batch_count=4,
            latest_release_batch_id=7001,
            metadata_json={
                "objectInsight": {
                    "customMetrics": [
                        {"identifier": "L1_GP_1.gpsTotalX", "group": "measure", "enabled": True, "includeInTrend": True},
                        {"identifier": "L1_GP_1.gpsTotalY", "group": "measure", "enabled": True, "includeInTrend": True},
                        {"identifier": "S1_ZT_1.sensor_state", "group": "status", "enabled": True, "includeInTrend": True},
                    ]
                }
            },
            tenant_id=1,
            manufacturer="南方测绘",
            description=None,
        )
        property_models_by_product = {
            2005: [
                make_contract("L1_GP_1.gpsTotalX", contract_id=4101, model_name="X方向累计变形量", data_type="double"),
                make_contract("L1_GP_1.gpsTotalY", contract_id=4102, model_name="Y方向累计变形量", data_type="double"),
                make_contract("S1_ZT_1.sensor_state", contract_id=4103, model_name="传感器状态", data_type="string"),
            ]
        }
        catalog_rows_by_product = {
            2005: [
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
                    "productId": 2005,
                    "productKey": "nf-monitor-gnss-monitor-v1",
                    "productName": "南方测绘 监测型 GNSS位移监测仪",
                    "auditStatus": module.STATUS_RULE_DRIFT,
                    "missingCatalogIdentifiers": ["L1_GP_1.gpsTotalX", "L1_GP_1.gpsTotalY"],
                    "unexpectedCatalogIdentifiers": ["L1_GP_1.gpsLegacy"],
                    "notes": ["当前启用目录缺少已明确“设为监测数据”且仍保留趋势展示的正式字段。"],
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
        self.assertEqual("RM_2005_L1_GP_1_GPSTOTALX", first_upsert["riskMetricCode"])
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
                    "notes": ["当前无正式发布批次；即使已设为监测数据，formal-metrics 读侧也只能等正式批次出现后再自愈。"],
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
                    "notes": ["已有正式发布批次，但当前启用目录缺少已明确“设为监测数据”且仍保留趋势展示的正式字段。"],
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
        self.assertIn("Measure Truth", md_content)
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
        self.assertIn("Measure Truth", md_content)
        self.assertIn("upsertCount: 2", md_content)


if __name__ == "__main__":
    unittest.main()
