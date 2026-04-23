#!/usr/bin/env python3
"""Tests for pending risk-point-device import SQL generation."""

from __future__ import annotations

import importlib.util
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

from scripts.schema_contract_test_support import (
    extract_create_table_statement,
    get_schema_sync_create_entry,
    read_init_sql,
)


SCRIPT_PATH = Path(__file__).resolve().parent / "export-risk-point-device-pending-bindings.py"


def load_module():
    spec = importlib.util.spec_from_file_location("risk_point_pending_import", SCRIPT_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load module from {SCRIPT_PATH}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def workbook_bytes(rows: list[tuple[str, str]]) -> bytes:
    shared_strings: list[str] = []
    shared_index: dict[str, int] = {}

    def shared(value: str) -> int:
        if value not in shared_index:
            shared_index[value] = len(shared_strings)
            shared_strings.append(value)
        return shared_index[value]

    def cell(column: str, row_no: int, value: str) -> str:
        return f'<c r="{column}{row_no}" t="s"><v>{shared(value)}</v></c>'

    sheet_rows = []
    for row_no, (risk_point_name, device_code) in enumerate(rows, start=1):
        sheet_rows.append(
            f'<row r="{row_no}">{cell("A", row_no, risk_point_name)}{cell("B", row_no, device_code)}</row>'
        )

    shared_xml = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" '
        f'count="{len(shared_strings)}" uniqueCount="{len(shared_strings)}">'
        + "".join(f"<si><t>{text}</t></si>" for text in shared_strings)
        + "</sst>"
    )
    sheet_xml = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">'
        f'<sheetData>{"".join(sheet_rows)}</sheetData>'
        "</worksheet>"
    )
    workbook_xml = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" '
        'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
        '<sheets><sheet name="数据" sheetId="1" r:id="rId1"/></sheets>'
        "</workbook>"
    )
    workbook_rels_xml = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        '<Relationship Id="rId1" '
        'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" '
        'Target="worksheets/sheet1.xml"/>'
        '<Relationship Id="rId2" '
        'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" '
        'Target="sharedStrings.xml"/>'
        "</Relationships>"
    )
    root_rels_xml = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        '<Relationship Id="rId1" '
        'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" '
        'Target="xl/workbook.xml"/>'
        "</Relationships>"
    )
    content_types_xml = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
        '<Default Extension="xml" ContentType="application/xml"/>'
        '<Override PartName="/xl/workbook.xml" '
        'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
        '<Override PartName="/xl/worksheets/sheet1.xml" '
        'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
        '<Override PartName="/xl/sharedStrings.xml" '
        'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>'
        "</Types>"
    )

    temp_dir = Path(tempfile.mkdtemp(prefix="risk-point-pending-xlsx-"))
    workbook_path = temp_dir / "fixture.xlsx"
    with ZipFile(workbook_path, "w") as archive:
        archive.writestr("[Content_Types].xml", content_types_xml)
        archive.writestr("_rels/.rels", root_rels_xml)
        archive.writestr("xl/workbook.xml", workbook_xml)
        archive.writestr("xl/_rels/workbook.xml.rels", workbook_rels_xml)
        archive.writestr("xl/sharedStrings.xml", shared_xml)
        archive.writestr("xl/worksheets/sheet1.xml", sheet_xml)
    content = workbook_path.read_bytes()
    return content


class PendingImportScriptTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory(prefix="risk-point-pending-test-")
        self.work_dir = Path(self.temp_dir.name)
        self.workbook_path = self.work_dir / "设备信息.xls"
        self.output_path = self.work_dir / "pending.sql"
        self.workbook_path.write_bytes(
            workbook_bytes(
                [
                    ("风险点名称", "设备编号"),
                    ("北坡风险点", "DEV-001"),
                    ("缺失风险点", "DEV-404"),
                ]
            )
        )

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def test_reads_header_and_rows_from_ooxml_workbook(self) -> None:
        module = load_module()
        rows = module.read_excel_rows(self.workbook_path)
        self.assertEqual(
            [("北坡风险点", "DEV-001", 2), ("缺失风险点", "DEV-404", 3)],
            [(row.risk_point_name, row.device_code, row.source_row_no) for row in rows],
        )

    def test_render_insert_sql_marks_pending_metric_governance_and_unresolved_rows(self) -> None:
        module = load_module()
        rows = [
            module.ExcelBindingRow("北坡风险点", "DEV-001", 2),
            module.ExcelBindingRow("缺失风险点", "DEV-404", 3),
        ]
        risk_points = {"北坡风险点": [{"id": 11, "risk_point_code": "RP-011"}]}
        devices = {"DEV-001": {"id": 21, "device_name": "北坡位移仪"}}
        records = module.resolve_pending_rows(
            rows=rows,
            risk_points_by_name=risk_points,
            devices_by_code=devices,
            tenant_id=1,
            batch_no="batch-001",
            source_file_name="设备信息.xls",
            operator_id=1,
        )
        sql = module.render_insert_sql(records)
        self.assertIn("PENDING_METRIC_GOVERNANCE", sql)
        self.assertIn("RISK_POINT_NOT_FOUND;DEVICE_NOT_FOUND", sql)
        self.assertIn("DEV-001", sql)

    def test_schema_truth_sources_contain_pending_binding_table(self) -> None:
        init_sql = extract_create_table_statement(
            read_init_sql(),
            "risk_point_device_pending_binding",
        )
        schema_sync_entry = get_schema_sync_create_entry("risk_point_device_pending_binding")
        expected_snippets = [
            "CREATE TABLE risk_point_device_pending_binding",
            "resolution_status VARCHAR(64) NOT NULL DEFAULT 'PENDING_METRIC_GOVERNANCE'",
            "metric_identifier VARCHAR(64) DEFAULT NULL",
            "UNIQUE KEY uk_pending_binding_batch_row (tenant_id, batch_no, source_row_no)",
            "KEY idx_pending_binding_status (tenant_id, resolution_status, deleted)",
        ]

        self.assertEqual("active", schema_sync_entry["lifecycle"])
        for snippet in expected_snippets:
            self.assertIn(snippet, init_sql)
            self.assertIn(snippet, schema_sync_entry["sql"])

    def test_cli_writes_sql_file_with_summary(self) -> None:
        result = subprocess.run(
            [
                "python3",
                str(SCRIPT_PATH),
                "--excel",
                str(self.workbook_path),
                "--sql-output",
                str(self.output_path),
                "--batch-no",
                "batch-002",
                "--skip-db-lookup",
            ],
            capture_output=True,
            text=True,
            check=False,
        )
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("rows=2", result.stdout)
        self.assertTrue(self.output_path.exists())
        self.assertIn("INSERT INTO risk_point_device_pending_binding", self.output_path.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
