#!/usr/bin/env python3
"""Tests for the pending batch governance script."""

from __future__ import annotations

import importlib.util
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).resolve().parent / "manage-risk-point-pending-governance.py"


def load_module():
    spec = importlib.util.spec_from_file_location("risk_point_pending_batch_governance", SCRIPT_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load module from {SCRIPT_PATH}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class PendingBatchGovernanceScriptTest(unittest.TestCase):
    def setUp(self) -> None:
        self.module = load_module()

    def test_build_operational_manifest_classifies_expected_buckets(self) -> None:
        rows = [
            {
                "id": 101,
                "risk_point_name": "边坡A",
                "device_name": "DW1-多维检测仪",
                "device_code": "DW-001",
                "resolution_status": "PENDING_METRIC_GOVERNANCE",
                "product_name": "南方测绘 监测型 多维位移监测仪",
            },
            {
                "id": 102,
                "risk_point_name": "边坡B",
                "device_name": "GNSS-1",
                "device_code": "GNSS-001",
                "resolution_status": "PENDING_METRIC_GOVERNANCE",
                "product_name": "中海达 监测型 GNSS位移监测仪",
            },
            {
                "id": 103,
                "risk_point_name": "边坡C",
                "device_name": "SK1329+050-翻斗式雨量计",
                "device_code": "RAIN-001",
                "resolution_status": "PENDING_METRIC_GOVERNANCE",
                "product_name": "南方测绘 监测型 翻斗式雨量计",
            },
            {
                "id": 104,
                "risk_point_name": "边坡D",
                "device_name": "声光报警系统",
                "device_code": "ALARM-001",
                "resolution_status": "PENDING_METRIC_GOVERNANCE",
                "product_name": None,
            },
            {
                "id": 105,
                "risk_point_name": "边坡E",
                "device_name": "激光测距传感器1",
                "device_code": "LASER-001",
                "resolution_status": "PENDING_METRIC_GOVERNANCE",
                "product_name": "南方测绘 监测型 激光测距仪",
            },
            {
                "id": 106,
                "risk_point_name": "边坡F",
                "device_name": "GNSS-2",
                "device_code": "GNSS-002",
                "resolution_status": "PENDING_METRIC_GOVERNANCE",
                "product_name": "南方测绘 监测型 GNSS位移监测仪",
            },
        ]
        candidates_by_id = {
            101: [
                {"metricIdentifier": "gX", "metricName": "gX"},
                {"metricIdentifier": "gY", "metricName": "gY"},
                {"metricIdentifier": "gZ", "metricName": "gZ"},
            ],
            102: [
                {"metricIdentifier": "gpsTotalX", "metricName": "gpsTotalX"},
                {"metricIdentifier": "gpsTotalY", "metricName": "gpsTotalY"},
                {"metricIdentifier": "gpsTotalZ", "metricName": "gpsTotalZ"},
                {"metricIdentifier": "gX", "metricName": "gX"},
                {"metricIdentifier": "angle", "metricName": "angle"},
            ],
            103: [
                {"metricIdentifier": "temp", "metricName": "temp"},
                {"metricIdentifier": "totalValue", "metricName": "累计雨量"},
                {"metricIdentifier": "value", "metricName": "雨量"},
            ],
            104: [],
            105: [],
            106: [
                {"metricIdentifier": "gX", "metricName": "gX"},
                {"metricIdentifier": "gY", "metricName": "gY"},
                {"metricIdentifier": "gZ", "metricName": "gZ"},
            ],
        }

        manifest = self.module.build_operational_manifest(rows, candidates_by_id)

        self.assertEqual(6, manifest["summary"]["totalPending"])
        self.assertEqual(3, manifest["summary"]["promoteCandidateCount"])
        self.assertEqual(1, manifest["summary"]["manualReviewCount"])
        self.assertEqual(1, manifest["summary"]["needRuntimeEvidenceCount"])
        self.assertEqual(1, manifest["summary"]["excludeCandidateCount"])

        promote_by_id = {item["pendingId"]: item for item in manifest["promote_candidates"]}
        self.assertEqual(
            ["gX", "gY", "gZ"],
            [entry["metricIdentifier"] for entry in promote_by_id[101]["recommendedMetrics"]],
        )
        self.assertEqual(
            ["gpsTotalX", "gpsTotalY", "gpsTotalZ"],
            [entry["metricIdentifier"] for entry in promote_by_id[102]["recommendedMetrics"]],
        )
        self.assertEqual(
            ["value", "totalValue"],
            [entry["metricIdentifier"] for entry in promote_by_id[103]["recommendedMetrics"]],
        )
        self.assertEqual("声光报警系统", manifest["exclude_candidates"][0]["deviceName"])
        self.assertEqual("激光测距传感器1", manifest["need_runtime_evidence"][0]["deviceName"])
        self.assertEqual(106, manifest["manual_review"][0]["pendingId"])

    def test_build_request_helpers_respect_curated_metrics(self) -> None:
        promote_item = {
            "pendingId": 201,
            "recommendedMetrics": [
                {"metricIdentifier": "value", "metricName": "雨量"},
                {"metricIdentifier": "totalValue", "metricName": "累计雨量"},
            ],
            "completePending": True,
            "promotionNote": "批量治理脚本：雨量计重点测点转正",
            "reason": "雨量计已出现降雨核心测点",
        }
        ignore_item = {
            "pendingId": 202,
            "deviceName": "声光报警系统",
            "ignoreNote": "批量治理脚本判定为非重点监测设备，按排除批次忽略",
            "reason": "设备名称命中非监测类关键词",
        }

        promote_request = self.module.build_promote_request(promote_item)
        ignore_request = self.module.build_ignore_request(ignore_item)

        self.assertEqual(
            [
                {"metricIdentifier": "value", "metricName": "雨量"},
                {"metricIdentifier": "totalValue", "metricName": "累计雨量"},
            ],
            promote_request["metrics"],
        )
        self.assertTrue(promote_request["completePending"])
        self.assertEqual(
            "批量治理脚本判定为非重点监测设备，按排除批次忽略",
            ignore_request["ignoreNote"],
        )

    def test_cli_plan_prints_bucket_counts(self) -> None:
        manifest = {
            "summary": {
                "totalPending": 5,
                "promoteCandidateCount": 2,
                "manualReviewCount": 1,
                "needRuntimeEvidenceCount": 1,
                "excludeCandidateCount": 1,
            },
            "promote_candidates": [{"pendingId": 1}, {"pendingId": 2}],
            "manual_review": [{"pendingId": 3}],
            "need_runtime_evidence": [{"pendingId": 4}],
            "exclude_candidates": [{"pendingId": 5}],
        }
        with tempfile.TemporaryDirectory(prefix="pending-batch-plan-") as temp_dir:
            manifest_path = Path(temp_dir) / "manifest.json"
            manifest_path.write_text(json.dumps(manifest, ensure_ascii=False), encoding="utf-8")
            result = subprocess.run(
                ["python3", str(SCRIPT_PATH), "plan", "--manifest", str(manifest_path)],
                capture_output=True,
                text=True,
                check=False,
            )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("totalPending=5", result.stdout)
        self.assertIn("promoteCandidateCount=2", result.stdout)
        self.assertIn("excludeCandidateCount=1", result.stdout)


if __name__ == "__main__":
    unittest.main()
