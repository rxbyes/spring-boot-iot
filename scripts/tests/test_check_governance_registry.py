#!/usr/bin/env python3
"""Tests for governance registry CLI checker contract."""

from __future__ import annotations

import contextlib
import importlib.util
import io
import pathlib
import sys
import unittest
from unittest import mock


REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT_PATH = REPO_ROOT / "scripts" / "governance" / "check_governance_registry.py"


def load_module():
    spec = importlib.util.spec_from_file_location("check_governance_registry_cli", SCRIPT_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load module from {SCRIPT_PATH}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class CheckGovernanceRegistryCliTest(unittest.TestCase):
    def test_main_should_report_success_when_generated_docs_are_current(self):
        module = load_module()
        stdout = io.StringIO()
        with mock.patch.object(module, "check_governance_docs", return_value=[]):
            with contextlib.redirect_stdout(stdout):
                exit_code = module.main()

        self.assertEqual(0, exit_code)
        self.assertEqual(
            "Governance registry is valid and generated docs are up to date.",
            stdout.getvalue().strip(),
        )

    def test_main_should_report_each_out_of_date_appendix_path(self):
        module = load_module()
        mismatches = [
            REPO_ROOT / "docs" / "appendix" / "database-schema-governance-catalog.generated.md",
            REPO_ROOT / "docs" / "appendix" / "database-schema-domain-governance.generated.md",
        ]
        stdout = io.StringIO()
        with mock.patch.object(module, "check_governance_docs", return_value=mismatches):
            with contextlib.redirect_stdout(stdout):
                exit_code = module.main()

        self.assertEqual(1, exit_code)
        self.assertEqual(
            [
                "OUT_OF_DATE docs/appendix/database-schema-governance-catalog.generated.md",
                "OUT_OF_DATE docs/appendix/database-schema-domain-governance.generated.md",
            ],
            stdout.getvalue().strip().splitlines(),
        )


if __name__ == "__main__":
    unittest.main()
