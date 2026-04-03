#!/usr/bin/env python3
"""Generate pending risk-point-device import SQL from an OOXML workbook."""

from __future__ import annotations

import argparse
import os
import re
import sys
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Dict, Iterable, List, Sequence
from urllib.parse import urlparse
from xml.etree import ElementTree as ET
from zipfile import BadZipFile, ZipFile

import pymysql


REPO_ROOT = Path(__file__).resolve().parent.parent
APP_DEV_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application-dev.yml"
XML_NS = {"a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
TABLE_NAME = "risk_point_device_pending_binding"
EXPECTED_HEADERS = ("风险点名称", "设备编号")
READY_STATUS = "PENDING_METRIC_GOVERNANCE"
LOOKUP_SKIPPED_STATUS = "LOOKUP_SKIPPED"


@dataclass(frozen=True)
class ExcelBindingRow:
    risk_point_name: str
    device_code: str
    source_row_no: int


@dataclass(frozen=True)
class PendingBindingRecord:
    batch_no: str
    source_file_name: str
    source_row_no: int
    risk_point_name: str
    risk_point_id: int | None
    risk_point_code: str | None
    device_code: str
    device_id: int | None
    device_name: str | None
    resolution_status: str
    resolution_note: str | None
    tenant_id: int
    create_by: int | None


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate pending governance SQL for risk-point/device imports when metric identifiers are not ready yet."
    )
    parser.add_argument("--excel", required=True, help="Path to the OOXML workbook. The file may still use a .xls suffix.")
    parser.add_argument("--sql-output", help="Write generated SQL to this file instead of stdout.")
    parser.add_argument("--batch-no", help="Explicit import batch number. Defaults to a timestamp-based batch.")
    parser.add_argument("--tenant-id", type=int, default=1, help="Tenant id. Defaults to 1.")
    parser.add_argument("--operator-id", type=int, default=1, help="Operator id for create_by/update_by. Defaults to 1.")
    parser.add_argument("--skip-db-lookup", action="store_true", help="Skip real-environment risk point/device lookup.")
    parser.add_argument("--jdbc-url", help="Override JDBC url. Defaults to application-dev.yml / env.")
    parser.add_argument("--user", help="Override mysql username. Defaults to application-dev.yml / env.")
    parser.add_argument("--password", help="Override mysql password. Defaults to application-dev.yml / env.")
    return parser.parse_args()


def extract_default(text: str, env_name: str) -> str:
    match = re.search(rf"{env_name}:([^}}]+)}}", text)
    if not match:
        raise RuntimeError(f"Unable to resolve {env_name} from {APP_DEV_PATH}")
    return match.group(1).strip()


def load_dev_defaults() -> Dict[str, str]:
    text = APP_DEV_PATH.read_text(encoding="utf-8")
    return {
        "jdbc_url": extract_default(text, "IOT_MYSQL_URL"),
        "user": extract_default(text, "IOT_MYSQL_USERNAME"),
        "password": extract_default(text, "IOT_MYSQL_PASSWORD"),
    }


def resolve_connection_args(args: argparse.Namespace) -> Dict[str, object]:
    defaults = load_dev_defaults()
    jdbc_url = args.jdbc_url or os.getenv("IOT_MYSQL_URL") or defaults["jdbc_url"]
    user = args.user or os.getenv("IOT_MYSQL_USERNAME") or defaults["user"]
    password = args.password or os.getenv("IOT_MYSQL_PASSWORD") or defaults["password"]

    parsed = urlparse(jdbc_url.replace("jdbc:mysql://", "mysql://", 1))
    database = parsed.path.lstrip("/")
    if not parsed.hostname or not database:
        raise RuntimeError(f"Invalid jdbc url: {jdbc_url}")
    return {
        "host": parsed.hostname,
        "port": parsed.port or 3306,
        "database": database,
        "user": user,
        "password": password,
    }


def normalize_text(value: object) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def build_batch_no(args: argparse.Namespace) -> str:
    if args.batch_no and args.batch_no.strip():
        return args.batch_no.strip()
    return f"rpd-pending-{datetime.now().strftime('%Y%m%d%H%M%S')}"


def chunked(values: Sequence[str], size: int = 200) -> Iterable[Sequence[str]]:
    for index in range(0, len(values), size):
        yield values[index:index + size]


def load_sheet_target(archive: ZipFile) -> str:
    workbook_root = ET.fromstring(archive.read("xl/workbook.xml"))
    sheets = workbook_root.findall("a:sheets/a:sheet", XML_NS)
    if not sheets:
        raise RuntimeError("Excel 工作簿中没有任何 sheet。")
    first_sheet = sheets[0]
    relationship_id = first_sheet.attrib.get("{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id")
    if not relationship_id:
        raise RuntimeError("首个 sheet 缺少关系定义。")

    relationships_root = ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
    target = None
    for relationship in relationships_root:
        if relationship.attrib.get("Id") == relationship_id:
            target = relationship.attrib.get("Target")
            break
    if not target:
        raise RuntimeError(f"未找到 sheet 关系 {relationship_id}。")
    if target.startswith("/"):
        return target.lstrip("/")
    if target.startswith("xl/"):
        return target
    return f"xl/{target}"


def load_shared_strings(archive: ZipFile) -> List[str]:
    if "xl/sharedStrings.xml" not in archive.namelist():
        return []
    shared_root = ET.fromstring(archive.read("xl/sharedStrings.xml"))
    strings: List[str] = []
    for item in shared_root.findall("a:si", XML_NS):
        strings.append("".join((text.text or "") for text in item.iterfind(".//a:t", XML_NS)))
    return strings


def read_cell_value(cell: ET.Element, shared_strings: Sequence[str]) -> str:
    cell_type = cell.attrib.get("t")
    if cell_type == "inlineStr":
        return "".join((text.text or "") for text in cell.iterfind(".//a:t", XML_NS))
    value_node = cell.find("a:v", XML_NS)
    if value_node is None or value_node.text is None:
        return ""
    value = value_node.text
    if cell_type == "s":
        index = int(value)
        return shared_strings[index]
    return value


def read_excel_rows(excel_path: Path) -> List[ExcelBindingRow]:
    try:
        with ZipFile(excel_path) as archive:
            shared_strings = load_shared_strings(archive)
            sheet_target = load_sheet_target(archive)
            sheet_root = ET.fromstring(archive.read(sheet_target))
    except BadZipFile as exc:
        raise RuntimeError(
            f"{excel_path} 不是可解析的 OOXML 工作簿。当前脚本只支持 Excel 2007+ 压缩包格式。"
        ) from exc

    row_nodes = sheet_root.findall(".//a:sheetData/a:row", XML_NS)
    if not row_nodes:
        raise RuntimeError("Excel 工作表没有任何数据行。")

    header_row = row_nodes[0]
    header_map: Dict[str, str] = {}
    for cell in header_row.findall("a:c", XML_NS):
        column = "".join(ch for ch in cell.attrib.get("r", "") if ch.isalpha())
        header_map[column] = normalize_text(read_cell_value(cell, shared_strings)) or ""
    actual_headers = (header_map.get("A", ""), header_map.get("B", ""))
    if actual_headers != EXPECTED_HEADERS:
        raise RuntimeError(f"Excel 表头不符合预期，期望 {EXPECTED_HEADERS}，实际 {actual_headers}")

    rows: List[ExcelBindingRow] = []
    for index, row_node in enumerate(row_nodes[1:], start=2):
        value_map: Dict[str, str] = {}
        for cell in row_node.findall("a:c", XML_NS):
            column = "".join(ch for ch in cell.attrib.get("r", "") if ch.isalpha())
            value_map[column] = normalize_text(read_cell_value(cell, shared_strings)) or ""
        risk_point_name = value_map.get("A", "")
        device_code = value_map.get("B", "")
        if not risk_point_name and not device_code:
            continue
        source_row_no = int(row_node.attrib.get("r") or index)
        rows.append(ExcelBindingRow(risk_point_name=risk_point_name, device_code=device_code, source_row_no=source_row_no))
    return rows


def fetch_risk_points_by_name(
    cursor: pymysql.cursors.Cursor,
    tenant_id: int,
    risk_point_names: Sequence[str],
) -> Dict[str, List[Dict[str, object]]]:
    if not risk_point_names:
        return {}
    result: Dict[str, List[Dict[str, object]]] = {}
    unique_names = sorted(set(risk_point_names))
    for group in chunked(unique_names):
        placeholders = ", ".join(["%s"] * len(group))
        cursor.execute(
            f"""
            SELECT id, risk_point_name, risk_point_code
            FROM risk_point
            WHERE tenant_id = %s
              AND deleted = 0
              AND risk_point_name IN ({placeholders})
            ORDER BY id
            """,
            [tenant_id, *group],
        )
        for row in cursor.fetchall():
            name = normalize_text(row.get("risk_point_name")) or ""
            result.setdefault(name, []).append(dict(row))
    return result


def fetch_devices_by_code(
    cursor: pymysql.cursors.Cursor,
    tenant_id: int,
    device_codes: Sequence[str],
) -> Dict[str, Dict[str, object]]:
    if not device_codes:
        return {}
    result: Dict[str, Dict[str, object]] = {}
    unique_codes = sorted(set(device_codes))
    for group in chunked(unique_codes):
        placeholders = ", ".join(["%s"] * len(group))
        cursor.execute(
            f"""
            SELECT id, device_code, device_name
            FROM iot_device
            WHERE tenant_id = %s
              AND deleted = 0
              AND device_code IN ({placeholders})
            ORDER BY id
            """,
            [tenant_id, *group],
        )
        for row in cursor.fetchall():
            device_code = normalize_text(row.get("device_code")) or ""
            result[device_code] = dict(row)
    return result


def build_resolution_note(status_codes: Sequence[str]) -> str:
    note_map = {
        "RISK_POINT_NAME_EMPTY": "来源行缺少风险点名称",
        "RISK_POINT_NOT_FOUND": "风险点名称未命中 risk_point",
        "RISK_POINT_NOT_UNIQUE": "风险点名称命中多条 risk_point，需要人工裁决",
        "DEVICE_CODE_EMPTY": "来源行缺少设备编号",
        "DEVICE_NOT_FOUND": "设备编号未命中 iot_device",
        LOOKUP_SKIPPED_STATUS: "本次跳过数据库匹配，请在真实环境重新生成或人工补齐映射",
    }
    return "；".join(note_map.get(code, code) for code in status_codes)


def resolve_pending_rows(
    rows: Sequence[ExcelBindingRow],
    risk_points_by_name: Dict[str, List[Dict[str, object]]],
    devices_by_code: Dict[str, Dict[str, object]],
    tenant_id: int,
    batch_no: str,
    source_file_name: str,
    operator_id: int | None,
    lookup_skipped: bool = False,
) -> List[PendingBindingRecord]:
    records: List[PendingBindingRecord] = []
    for row in rows:
        if lookup_skipped:
            records.append(
                PendingBindingRecord(
                    batch_no=batch_no,
                    source_file_name=source_file_name,
                    source_row_no=row.source_row_no,
                    risk_point_name=row.risk_point_name,
                    risk_point_id=None,
                    risk_point_code=None,
                    device_code=row.device_code,
                    device_id=None,
                    device_name=None,
                    resolution_status=LOOKUP_SKIPPED_STATUS,
                    resolution_note=build_resolution_note([LOOKUP_SKIPPED_STATUS]),
                    tenant_id=tenant_id,
                    create_by=operator_id,
                )
            )
            continue

        risk_point_id = None
        risk_point_code = None
        device_id = None
        device_name = None
        status_codes: List[str] = []

        if not row.risk_point_name:
            status_codes.append("RISK_POINT_NAME_EMPTY")
        else:
            risk_point_matches = risk_points_by_name.get(row.risk_point_name, [])
            if not risk_point_matches:
                status_codes.append("RISK_POINT_NOT_FOUND")
            elif len(risk_point_matches) > 1:
                status_codes.append("RISK_POINT_NOT_UNIQUE")
            else:
                risk_point_id = int(risk_point_matches[0]["id"])
                risk_point_code = normalize_text(risk_point_matches[0].get("risk_point_code"))

        if not row.device_code:
            status_codes.append("DEVICE_CODE_EMPTY")
        else:
            device = devices_by_code.get(row.device_code)
            if device is None:
                status_codes.append("DEVICE_NOT_FOUND")
            else:
                device_id = int(device["id"])
                device_name = normalize_text(device.get("device_name"))

        if status_codes:
            resolution_status = ";".join(status_codes)
            resolution_note = build_resolution_note(status_codes)
        else:
            resolution_status = READY_STATUS
            resolution_note = "风险点与设备已匹配，待结合真实上报补录 metric_identifier / metric_name 后转正。"

        records.append(
            PendingBindingRecord(
                batch_no=batch_no,
                source_file_name=source_file_name,
                source_row_no=row.source_row_no,
                risk_point_name=row.risk_point_name,
                risk_point_id=risk_point_id,
                risk_point_code=risk_point_code,
                device_code=row.device_code,
                device_id=device_id,
                device_name=device_name,
                resolution_status=resolution_status,
                resolution_note=resolution_note,
                tenant_id=tenant_id,
                create_by=operator_id,
            )
        )
    return records


def sql_literal(value: object) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, datetime):
        return f"'{value.strftime('%Y-%m-%d %H:%M:%S')}'"
    text = str(value).replace("\\", "\\\\").replace("'", "''")
    return f"'{text}'"


def render_insert_sql(records: Sequence[PendingBindingRecord]) -> str:
    if not records:
        return "-- No pending binding rows were generated.\n"

    columns = [
        "batch_no",
        "source_file_name",
        "source_row_no",
        "risk_point_name",
        "risk_point_id",
        "risk_point_code",
        "device_code",
        "device_id",
        "device_name",
        "resolution_status",
        "resolution_note",
        "metric_identifier",
        "metric_name",
        "promoted_binding_id",
        "promoted_time",
        "tenant_id",
        "create_by",
        "update_by",
        "deleted",
    ]
    values_sql = []
    for record in records:
        values = [
            record.batch_no,
            record.source_file_name,
            record.source_row_no,
            record.risk_point_name,
            record.risk_point_id,
            record.risk_point_code,
            record.device_code,
            record.device_id,
            record.device_name,
            record.resolution_status,
            record.resolution_note,
            None,
            None,
            None,
            None,
            record.tenant_id,
            record.create_by,
            record.create_by,
            0,
        ]
        values_sql.append("    (" + ", ".join(sql_literal(value) for value in values) + ")")

    return "\n".join(
        [
            f"-- Generated by scripts/export-risk-point-device-pending-bindings.py at {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
            f"-- Rows: {len(records)}",
            f"INSERT INTO {TABLE_NAME} (",
            "    " + ", ".join(columns),
            ") VALUES",
            ",\n".join(values_sql),
            "ON DUPLICATE KEY UPDATE",
            "    risk_point_name = VALUES(risk_point_name),",
            "    risk_point_id = VALUES(risk_point_id),",
            "    risk_point_code = VALUES(risk_point_code),",
            "    device_code = VALUES(device_code),",
            "    device_id = VALUES(device_id),",
            "    device_name = VALUES(device_name),",
            "    resolution_status = VALUES(resolution_status),",
            "    resolution_note = VALUES(resolution_note),",
            "    update_by = VALUES(update_by),",
            "    update_time = CURRENT_TIMESTAMP,",
            "    deleted = 0;",
            "",
        ]
    )


def build_summary(records: Sequence[PendingBindingRecord]) -> Dict[str, object]:
    status_counts: Dict[str, int] = {}
    for record in records:
        status_counts[record.resolution_status] = status_counts.get(record.resolution_status, 0) + 1
    ready = status_counts.get(READY_STATUS, 0)
    unresolved = len(records) - ready
    return {
        "rows": len(records),
        "ready": ready,
        "unresolved": unresolved,
        "status_counts": status_counts,
    }


def format_summary(batch_no: str, sql_output: Path | None, summary: Dict[str, object]) -> str:
    status_counts = summary["status_counts"]
    status_text = ", ".join(f"{status}:{count}" for status, count in sorted(status_counts.items()))
    output_text = str(sql_output) if sql_output is not None else "stdout"
    return (
        f"Generated pending binding SQL: batch={batch_no} rows={summary['rows']} "
        f"ready={summary['ready']} unresolved={summary['unresolved']} output={output_text} statuses={status_text}"
    )


def main() -> int:
    try:
        args = parse_args()
        excel_path = Path(args.excel).expanduser().resolve()
        rows = read_excel_rows(excel_path)
        batch_no = build_batch_no(args)

        if args.skip_db_lookup:
            risk_points_by_name: Dict[str, List[Dict[str, object]]] = {}
            devices_by_code: Dict[str, Dict[str, object]] = {}
        else:
            connection_args = resolve_connection_args(args)
            with pymysql.connect(
                host=connection_args["host"],
                port=int(connection_args["port"]),
                user=str(connection_args["user"]),
                password=str(connection_args["password"]),
                database=str(connection_args["database"]),
                charset="utf8mb4",
                cursorclass=pymysql.cursors.DictCursor,
            ) as connection:
                with connection.cursor() as cursor:
                    risk_points_by_name = fetch_risk_points_by_name(
                        cursor,
                        args.tenant_id,
                        [row.risk_point_name for row in rows if row.risk_point_name],
                    )
                    devices_by_code = fetch_devices_by_code(
                        cursor,
                        args.tenant_id,
                        [row.device_code for row in rows if row.device_code],
                    )

        records = resolve_pending_rows(
            rows=rows,
            risk_points_by_name=risk_points_by_name,
            devices_by_code=devices_by_code,
            tenant_id=args.tenant_id,
            batch_no=batch_no,
            source_file_name=excel_path.name,
            operator_id=args.operator_id,
            lookup_skipped=args.skip_db_lookup,
        )
        sql_text = render_insert_sql(records)
        sql_output = Path(args.sql_output).expanduser().resolve() if args.sql_output else None
        if sql_output is not None:
            sql_output.parent.mkdir(parents=True, exist_ok=True)
            sql_output.write_text(sql_text, encoding="utf-8")
        else:
            sys.stdout.write(sql_text)
        summary = build_summary(records)
        print(format_summary(batch_no, sql_output, summary))
        return 0
    except Exception as exc:  # pragma: no cover - exercised through CLI failure path.
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
