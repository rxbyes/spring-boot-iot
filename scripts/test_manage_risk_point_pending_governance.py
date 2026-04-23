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

    def build_manual_review_manifest(self) -> dict:
        return {
            "summary": {
                "totalPending": 3,
                "promoteCandidateCount": 0,
                "manualReviewCount": 3,
                "needRuntimeEvidenceCount": 0,
                "excludeCandidateCount": 0,
            },
            "promote_candidates": [],
            "manual_review": [
                {
                    "pendingId": 418,
                    "riskPointName": "G30连霍高速 K1731+077",
                    "deviceName": "倾角仪",
                    "deviceCode": "15522566",
                    "productName": "中海达 监测型 倾角仪",
                    "resolutionStatus": "PENDING_METRIC_GOVERNANCE",
                    "family": "中海达 监测型 倾角仪",
                    "candidateMetrics": [
                        {"metricIdentifier": "gX", "metricName": "X轴加速度"},
                        {"metricIdentifier": "gY", "metricName": "Y轴加速度"},
                        {"metricIdentifier": "gZ", "metricName": "Z轴加速度"},
                    ],
                    "reason": "倾角仪出现 gX/gY/gZ，需确认是否接受为重点测点，或继续收口到 X/Y/Z/angle/AZI",
                },
                {
                    "pendingId": 370,
                    "riskPointName": "G22青兰高速平定段K1652+855水毁",
                    "deviceName": "GNSS",
                    "deviceCode": "SJ11F1148730978A",
                    "productName": "南方测绘 监测型 GNSS位移监测仪",
                    "resolutionStatus": "PENDING_METRIC_GOVERNANCE",
                    "family": "GNSS位移监测仪",
                    "candidateMetrics": [
                        {"metricIdentifier": "gX", "metricName": "X轴加速度"},
                        {"metricIdentifier": "gY", "metricName": "Y轴加速度"},
                        {"metricIdentifier": "gZ", "metricName": "Z轴加速度"},
                    ],
                    "reason": "GNSS家族仅出现 gX/gY/gZ，疑似设备归属或字段语义不清，需人工复核后再定",
                },
                {
                    "pendingId": 12,
                    "riskPointName": "G7011十天高速K595",
                    "deviceName": "DB6-基准点（GNSS）",
                    "deviceCode": "SJ11F2148734260A",
                    "productName": "南方测绘 监测型 GNSS基准站",
                    "resolutionStatus": "PENDING_METRIC_GOVERNANCE",
                    "family": "GNSS基准站",
                    "candidateMetrics": [
                        {"metricIdentifier": "gX", "metricName": "X轴加速度"},
                        {"metricIdentifier": "gY", "metricName": "Y轴加速度"},
                        {"metricIdentifier": "gZ", "metricName": "Z轴加速度"},
                    ],
                    "reason": "GNSS基准站仅出现 gX/gY/gZ，不符合当前重点 GNSS 位移口径，需人工复核",
                },
            ],
            "need_runtime_evidence": [],
            "exclude_candidates": [],
        }

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

    def test_build_manual_review_export_records(self) -> None:
        manifest = self.build_manual_review_manifest()

        records = self.module.build_manual_review_export_records(manifest["manual_review"])
        record_by_id = {record["pending_id"]: record for record in records}

        self.assertEqual(3, len(records))
        self.assertEqual("倾角仪 gX/gY/gZ 复核组", record_by_id[418]["group"])
        self.assertEqual("保留 pending，优先补字段映射", record_by_id[418]["suggested_action"])
        self.assertIn("AZI/X/Y/Z/angle", record_by_id[418]["todo"])
        self.assertEqual("GNSS 位移监测仪 gX/gY/gZ 复核组", record_by_id[370]["group"])
        self.assertIn("gpsTotalX/gpsTotalY/gpsTotalZ", record_by_id[370]["todo"])
        self.assertEqual("GNSS 基准站 gX/gY/gZ 复核组", record_by_id[12]["group"])
        self.assertIn("姿态/健康参数", record_by_id[12]["todo"])

    def test_build_manual_review_annotation_rows(self) -> None:
        manifest = self.build_manual_review_manifest()

        rows = self.module.build_manual_review_annotation_rows(manifest["manual_review"])
        note_by_id = {row["pendingId"]: row["resolutionNote"] for row in rows}

        self.assertEqual(3, len(rows))
        self.assertIn("AZI/X/Y/Z/angle", note_by_id[418])
        self.assertIn("gpsTotalX/gpsTotalY/gpsTotalZ", note_by_id[370])
        self.assertIn("姿态/健康参数", note_by_id[12])

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

    def test_cli_export_manual_review_writes_json(self) -> None:
        manifest = self.build_manual_review_manifest()
        with tempfile.TemporaryDirectory(prefix="pending-manual-review-export-") as temp_dir:
            manifest_path = Path(temp_dir) / "manifest.json"
            output_prefix = Path(temp_dir) / "manual-review"
            manifest_path.write_text(json.dumps(manifest, ensure_ascii=False), encoding="utf-8")
            result = subprocess.run(
                [
                    "python3",
                    str(SCRIPT_PATH),
                    "export-manual-review",
                    "--manifest",
                    str(manifest_path),
                    "--output-prefix",
                    str(output_prefix),
                ],
                capture_output=True,
                text=True,
                check=False,
            )
            json_path = Path(f"{output_prefix}.json")
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertTrue(json_path.exists())
            exported = json.loads(json_path.read_text(encoding="utf-8"))
            self.assertEqual(3, len(exported))

    def test_cli_annotate_manual_review_dry_run(self) -> None:
        manifest = self.build_manual_review_manifest()
        with tempfile.TemporaryDirectory(prefix="pending-manual-review-annotate-") as temp_dir:
            manifest_path = Path(temp_dir) / "manifest.json"
            manifest_path.write_text(json.dumps(manifest, ensure_ascii=False), encoding="utf-8")
            result = subprocess.run(
                [
                    "python3",
                    str(SCRIPT_PATH),
                    "annotate-manual-review",
                    "--manifest",
                    str(manifest_path),
                ],
                capture_output=True,
                text=True,
                check=False,
            )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("dryRun=True", result.stdout)
        self.assertIn("pendingId=418", result.stdout)
        self.assertIn("pendingId=370", result.stdout)
        self.assertIn("pendingId=12", result.stdout)

    def test_read_manual_review_decision_csv(self) -> None:
        with tempfile.TemporaryDirectory(prefix="pending-manual-review-decision-read-") as temp_dir:
            csv_path = Path(temp_dir) / "decisions.csv"
            csv_path.write_text(
                "\n".join(
                    [
                        "pending_id,business_decision,canonical_metrics,notes",
                        "418,PROMOTE,\"gpsTotalX,gpsTotalY,gpsTotalZ\",按规范转正",
                        "370,IGNORE,,确认排除",
                        "12,KEEP_PENDING,,继续等待基准站口径",
                    ]
                ),
                encoding="utf-8",
            )

            rows = self.module.read_manual_review_decision_csv(csv_path)

        self.assertEqual(3, len(rows))
        self.assertEqual(418, rows[0]["pendingId"])
        self.assertEqual("PROMOTE", rows[0]["businessDecision"])
        self.assertEqual(["gpsTotalX", "gpsTotalY", "gpsTotalZ"], rows[0]["canonicalMetrics"])
        self.assertEqual("确认排除", rows[1]["notes"])
        self.assertEqual("KEEP_PENDING", rows[2]["businessDecision"])

    def test_validate_manual_review_decision_rows(self) -> None:
        valid_rows = [
            {"pendingId": 418, "businessDecision": "PROMOTE", "canonicalMetrics": ["gpsTotalX"], "notes": "按规范转正"},
            {"pendingId": 370, "businessDecision": "IGNORE", "canonicalMetrics": [], "notes": "确认排除"},
            {"pendingId": 12, "businessDecision": "KEEP_PENDING", "canonicalMetrics": [], "notes": "继续等待"},
        ]
        self.module.validate_manual_review_decision_rows(valid_rows)

        with self.assertRaises(ValueError):
            self.module.validate_manual_review_decision_rows(
                [{"pendingId": 1, "businessDecision": "PROMOTE", "canonicalMetrics": [], "notes": ""}]
            )
        with self.assertRaises(ValueError):
            self.module.validate_manual_review_decision_rows(
                [{"pendingId": 2, "businessDecision": "IGNORE", "canonicalMetrics": ["gpsTotalX"], "notes": ""}]
            )
        with self.assertRaises(ValueError):
            self.module.validate_manual_review_decision_rows(
                [{"pendingId": 3, "businessDecision": "UNKNOWN", "canonicalMetrics": [], "notes": ""}]
            )

    def test_build_manual_review_decision_actions(self) -> None:
        rows = [
            {"pendingId": 418, "businessDecision": "PROMOTE", "canonicalMetrics": ["gpsTotalX", "gpsTotalY"], "notes": "按规范转正"},
            {"pendingId": 370, "businessDecision": "IGNORE", "canonicalMetrics": [], "notes": "确认排除"},
            {"pendingId": 12, "businessDecision": "KEEP_PENDING", "canonicalMetrics": [], "notes": "继续等待"},
        ]

        actions = self.module.build_manual_review_decision_actions(rows)
        action_by_id = {action["pendingId"]: action for action in actions}

        self.assertEqual("PROMOTE", action_by_id[418]["businessDecision"])
        self.assertEqual(
            [
                {"metricIdentifier": "gpsTotalX", "metricName": "gpsTotalX"},
                {"metricIdentifier": "gpsTotalY", "metricName": "gpsTotalY"},
            ],
            action_by_id[418]["requestBody"]["metrics"],
        )
        self.assertEqual("IGNORE", action_by_id[370]["businessDecision"])
        self.assertEqual("确认排除", action_by_id[370]["requestBody"]["ignoreNote"])
        self.assertEqual("KEEP_PENDING", action_by_id[12]["businessDecision"])
        self.assertIn("继续等待", action_by_id[12]["resolutionNote"])

    def test_cli_apply_manual_review_decisions_dry_run(self) -> None:
        with tempfile.TemporaryDirectory(prefix="pending-manual-review-decision-cli-") as temp_dir:
            csv_path = Path(temp_dir) / "decisions.csv"
            csv_path.write_text(
                "\n".join(
                    [
                        "pending_id,business_decision,canonical_metrics,notes",
                        "418,PROMOTE,\"gpsTotalX,gpsTotalY,gpsTotalZ\",按规范转正",
                        "370,IGNORE,,确认排除",
                        "12,KEEP_PENDING,,继续等待基准站口径",
                    ]
                ),
                encoding="utf-8",
            )
            result = subprocess.run(
                [
                    "python3",
                    str(SCRIPT_PATH),
                    "apply-manual-review-decisions",
                    "--csv",
                    str(csv_path),
                ],
                capture_output=True,
                text=True,
                check=False,
            )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("dryRun=True", result.stdout)
        self.assertIn("PROMOTE", result.stdout)
        self.assertIn("IGNORE", result.stdout)
        self.assertIn("KEEP_PENDING", result.stdout)


if __name__ == "__main__":
    unittest.main()
