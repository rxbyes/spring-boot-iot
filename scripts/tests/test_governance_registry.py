#!/usr/bin/env python3
"""Tests for schema-governance registry loading and validation."""

from __future__ import annotations

import importlib.util
import json
import pathlib
import sys
import tempfile
import unittest


REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT_PATH = REPO_ROOT / "scripts" / "governance" / "load_governance_registry.py"
SPEC = importlib.util.spec_from_file_location("governance_registry_loader", SCRIPT_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"Unable to load governance registry module from {SCRIPT_PATH}")
governance_registry_loader = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = governance_registry_loader
SPEC.loader.exec_module(governance_registry_loader)


class GovernanceRegistryTest(unittest.TestCase):
    def test_alarm_governance_domain_should_register_highway_archive_sample(self):
        registry = governance_registry_loader.load_governance_registry(
            REPO_ROOT / "schema-governance",
            REPO_ROOT / "schema",
        )

        self.assertIn("alarm", registry.domains)
        alarm_domain = registry.domains["alarm"]
        self.assertIn("risk_point_highway_detail", alarm_domain.objects)

        object_entry = alarm_domain.objects["risk_point_highway_detail"]
        self.assertEqual("archived", object_entry.governance_stage)
        self.assertEqual("mysql_archived_object_with_seed", object_entry.real_env_audit_profile)
        self.assertIn("highway_archived_risk_points_seed", object_entry.seed_packages)

        seed_package = alarm_domain.seed_packages["highway_archived_risk_points_seed"]
        self.assertEqual("历史归档", seed_package.usage)
        self.assertTrue(seed_package.allowed_in_init_data)
        self.assertIn("risk_point_highway_detail", seed_package.bound_objects)

    def test_load_governance_registry_should_fail_when_object_missing_in_structure_registry(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = pathlib.Path(temp_dir)
            payload = {
                "domain": "alarm",
                "objects": [
                    {
                        "objectName": "missing_object",
                        "storageType": "mysql_table",
                        "governanceStage": "archived",
                        "ownerModule": "spring-boot-iot-alarm",
                        "businessDomain": "alarm",
                        "seedPackages": [],
                        "realEnvAuditProfile": "mysql_archived_object_with_seed",
                        "backupRequirements": {
                            "exportFormats": ["json"],
                            "mustCaptureRowCount": True,
                        },
                        "deletionPrerequisites": ["docs_and_registry_updated"],
                        "manualChecklist": ["确认无运行时依赖"],
                        "evidenceRefs": ["docs/04-数据库设计与初始化数据.md"],
                        "notes": "测试对象",
                    }
                ],
                "seedPackages": [],
            }
            (root / "alarm-domain.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")

            with self.assertRaises(ValueError):
                governance_registry_loader.load_governance_registry(root, REPO_ROOT / "schema")

    def test_load_governance_registry_should_fail_on_invalid_stage(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = pathlib.Path(temp_dir)
            payload = {
                "domain": "alarm",
                "objects": [
                    {
                        "objectName": "risk_point_highway_detail",
                        "storageType": "mysql_table",
                        "governanceStage": "retired",
                        "ownerModule": "spring-boot-iot-alarm",
                        "businessDomain": "alarm",
                        "seedPackages": [],
                        "realEnvAuditProfile": "mysql_archived_object_with_seed",
                        "backupRequirements": {
                            "exportFormats": ["json"],
                            "mustCaptureRowCount": True,
                        },
                        "deletionPrerequisites": ["docs_and_registry_updated"],
                        "manualChecklist": ["确认无运行时依赖"],
                        "evidenceRefs": ["docs/04-数据库设计与初始化数据.md"],
                        "notes": "测试对象",
                    }
                ],
                "seedPackages": [],
            }
            (root / "alarm-domain.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")

            with self.assertRaises(ValueError):
                governance_registry_loader.load_governance_registry(root, REPO_ROOT / "schema")


if __name__ == "__main__":
    unittest.main()
