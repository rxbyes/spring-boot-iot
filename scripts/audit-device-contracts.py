#!/usr/bin/env python3
"""Audit and repair device access contracts in the real environment."""

from __future__ import annotations

import argparse
import csv
import json
import os
import re
import sys
from pathlib import Path
from typing import Dict, Iterable, List
from urllib.parse import urlparse

import pymysql


REPO_ROOT = Path(__file__).resolve().parent.parent
APP_DEV_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application-dev.yml"
REPAIR_HEADERS = ["device_code", "product_key", "protocol_code", "reviewer", "remark"]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Audit or repair iot_device product/protocol contracts.")
    parser.add_argument("--device-prefix", help="Audit devices by code prefix, for example SK11")
    parser.add_argument("--device-code", action="append", default=[], help="Audit a specific device code. Repeatable.")
    parser.add_argument("--limit", type=int, default=100, help="Max rows to inspect. Defaults to 100.")
    parser.add_argument("--jdbc-url", help="Override JDBC url. Defaults to application-dev.yml / env.")
    parser.add_argument("--user", help="Override mysql username. Defaults to application-dev.yml / env.")
    parser.add_argument("--password", help="Override mysql password. Defaults to application-dev.yml / env.")
    parser.add_argument("--export-repair-csv", help="Write a repair checklist CSV for non-OK audit rows.")
    parser.add_argument("--product-key", help="Optional default productKey to prefill export CSV rows.")
    parser.add_argument("--protocol-code", help="Optional default protocolCode to prefill export CSV rows.")
    parser.add_argument("--repair-file", help="Explicit repair checklist CSV. Required for repair mode.")
    parser.add_argument("--apply", action="store_true", help="Apply repair rows from --repair-file.")
    return parser.parse_args()


def load_dev_defaults() -> Dict[str, str]:
    text = APP_DEV_PATH.read_text(encoding="utf-8")
    return {
        "jdbc_url": extract_default(text, "IOT_MYSQL_URL"),
        "user": extract_default(text, "IOT_MYSQL_USERNAME"),
        "password": extract_default(text, "IOT_MYSQL_PASSWORD"),
    }


def extract_default(text: str, env_name: str) -> str:
    match = re.search(rf"{env_name}:([^}}]+)}}", text)
    if not match:
        raise RuntimeError(f"Unable to resolve {env_name} from {APP_DEV_PATH}")
    return match.group(1).strip()


def resolve_connection_args(args: argparse.Namespace) -> Dict[str, str]:
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


def normalize_device_codes(device_codes: Iterable[str]) -> List[str]:
    normalized: List[str] = []
    for device_code in device_codes:
        if device_code and device_code.strip():
            normalized.append(device_code.strip())
    return normalized


def build_where_clause(device_codes: List[str], device_prefix: str | None, params: List[object]) -> str:
    clauses = ["d.deleted = 0"]
    if device_codes:
        placeholders = ", ".join(["%s"] * len(device_codes))
        clauses.append(f"d.device_code IN ({placeholders})")
        params.extend(device_codes)
    if device_prefix:
        clauses.append("d.device_code LIKE %s")
        params.append(f"{device_prefix}%")
    if len(clauses) == 1:
        raise RuntimeError("Please provide --device-prefix or at least one --device-code.")
    return " WHERE " + " AND ".join(clauses)


def fetch_devices(cur: pymysql.cursors.Cursor,
                  device_codes: List[str],
                  device_prefix: str | None,
                  limit: int) -> List[Dict[str, object]]:
    params: List[object] = []
    where_clause = build_where_clause(device_codes, device_prefix, params)
    sql = f"""
        SELECT d.id,
               d.device_code,
               d.device_name,
               d.protocol_code AS device_protocol_code,
               d.product_id,
               d.node_type AS device_node_type,
               d.activate_status,
               d.device_status,
               d.last_report_time,
               p.id AS resolved_product_id,
               p.product_key,
               p.product_name,
               p.protocol_code AS product_protocol_code,
               p.node_type AS product_node_type,
               p.status AS product_status
        FROM iot_device d
        LEFT JOIN iot_product p
               ON p.id = d.product_id
              AND p.deleted = 0
        {where_clause}
        ORDER BY d.id DESC
        LIMIT %s
    """
    params.append(max(limit, 1))
    cur.execute(sql, params)
    rows = cur.fetchall()
    return [enrich_issue_summary(row) for row in rows]


def fetch_device_by_code(cur: pymysql.cursors.Cursor, device_code: str) -> Dict[str, object] | None:
    rows = fetch_devices(cur, [device_code], None, 1)
    return rows[0] if rows else None


def enrich_issue_summary(row: Dict[str, object]) -> Dict[str, object]:
    issues: List[str] = []
    product_id = row.get("product_id")
    device_protocol = normalize_text(row.get("device_protocol_code"))
    product_protocol = normalize_text(row.get("product_protocol_code"))

    if product_id in (None, 0):
        issues.append("MISSING_PRODUCT_BINDING")
    elif row.get("resolved_product_id") is None:
        issues.append("PRODUCT_NOT_FOUND")

    if not device_protocol:
        issues.append("MISSING_DEVICE_PROTOCOL")
    if row.get("resolved_product_id") is not None and not product_protocol:
        issues.append("MISSING_PRODUCT_PROTOCOL")
    if device_protocol and product_protocol and device_protocol.lower() != product_protocol.lower():
        issues.append("DEVICE_PRODUCT_PROTOCOL_CONFLICT")
    if row.get("resolved_product_id") is not None and row.get("product_status") not in (None, 1):
        issues.append("PRODUCT_DISABLED")

    row = dict(row)
    row["issues"] = issues or ["OK"]
    return row


def normalize_text(value: object) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def summarize_audit(rows: List[Dict[str, object]]) -> Dict[str, int]:
    summary: Dict[str, int] = {"TOTAL": len(rows)}
    for row in rows:
        for issue in row["issues"]:
            summary[issue] = summary.get(issue, 0) + 1
    return summary


def lookup_product(cur: pymysql.cursors.Cursor, product_key: str) -> Dict[str, object]:
    cur.execute(
        """
        SELECT id, product_key, product_name, protocol_code, node_type, status
        FROM iot_product
        WHERE product_key = %s AND deleted = 0
        LIMIT 1
        """,
        (product_key,),
    )
    row = cur.fetchone()
    if not row:
        raise RuntimeError(f"Product not found: {product_key}")
    return row


def validate_mode(args: argparse.Namespace) -> str:
    if args.repair_file:
        if args.device_prefix or normalize_device_codes(args.device_code):
            raise RuntimeError("--repair-file mode does not accept --device-prefix or --device-code.")
        return "repair"
    if args.apply:
        raise RuntimeError("--apply only works with --repair-file. Direct query-based repair is disabled.")
    return "audit"


def export_repair_csv(path: str, rows: List[Dict[str, object]], args: argparse.Namespace) -> int:
    export_path = Path(path)
    export_path.parent.mkdir(parents=True, exist_ok=True)
    candidates = [row for row in rows if row.get("issues") != ["OK"]]
    default_product_key = normalize_text(args.product_key)
    default_protocol_code = normalize_text(args.protocol_code)

    with export_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=REPAIR_HEADERS)
        writer.writeheader()
        for row in candidates:
            writer.writerow({
                "device_code": row.get("device_code") or "",
                "product_key": default_product_key or row.get("product_key") or "",
                "protocol_code": default_protocol_code
                or row.get("product_protocol_code")
                or row.get("device_protocol_code")
                or "",
                "reviewer": "",
                "remark": ";".join(row.get("issues", [])),
            })
    return len(candidates)


def load_repair_rows(path: str) -> List[Dict[str, object]]:
    repair_path = Path(path)
    if not repair_path.exists():
        raise RuntimeError(f"Repair file not found: {repair_path}")
    with repair_path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        headers = reader.fieldnames or []
        missing_headers = [header for header in REPAIR_HEADERS if header not in headers]
        if missing_headers:
            raise RuntimeError(f"Repair file is missing headers: {', '.join(missing_headers)}")
        rows: List[Dict[str, object]] = []
        for row_no, row in enumerate(reader, start=2):
            rows.append({
                "row_no": row_no,
                "device_code": normalize_text(row.get("device_code")) or "",
                "product_key": normalize_text(row.get("product_key")) or "",
                "protocol_code": normalize_text(row.get("protocol_code")) or "",
                "reviewer": normalize_text(row.get("reviewer")) or "",
                "remark": normalize_text(row.get("remark")) or "",
            })
        return rows


def repair_from_file(conn: pymysql.connections.Connection,
                     cur: pymysql.cursors.Cursor,
                     repair_rows: List[Dict[str, object]],
                     apply_changes: bool) -> Dict[str, object]:
    results: List[Dict[str, object]] = []
    summary = {"TOTAL": len(repair_rows), "VALIDATED": 0, "APPLIED": 0, "FAILED": 0, "SKIPPED": 0}

    for repair_row in repair_rows:
        device_code = repair_row["device_code"]
        product_key = repair_row["product_key"]
        protocol_code = repair_row["protocol_code"]
        result = dict(repair_row)
        try:
            if not device_code:
                raise RuntimeError("device_code is required")
            if not product_key:
                raise RuntimeError("product_key is required")
            if not protocol_code:
                raise RuntimeError("protocol_code is required")

            current = fetch_device_by_code(cur, device_code)
            if not current:
                raise RuntimeError(f"Device not found: {device_code}")

            product = lookup_product(cur, product_key)
            product_protocol = normalize_text(product.get("protocol_code"))
            if product.get("status") != 1:
                raise RuntimeError(f"Target product is disabled: {product_key}")
            if not product_protocol:
                raise RuntimeError(f"Target product has no protocol_code: {product_key}")
            if product_protocol.lower() != protocol_code.lower():
                raise RuntimeError(
                    f"protocol_code {protocol_code} does not match product {product_key} protocol {product_protocol}"
                )
            if product.get("node_type") is None:
                raise RuntimeError(f"Target product has no node_type: {product_key}")

            already_compliant = (
                current.get("resolved_product_id") == product.get("id")
                and normalize_text(current.get("device_protocol_code")) == protocol_code
                and current.get("device_node_type") == product.get("node_type")
                and current.get("issues") == ["OK"]
            )
            if already_compliant:
                summary["SKIPPED"] += 1
                result["status"] = "SKIPPED"
                result["message"] = "device already compliant"
                result["current"] = current
                results.append(result)
                continue

            if apply_changes:
                cur.execute(
                    """
                    UPDATE iot_device
                    SET product_id = %s,
                        protocol_code = %s,
                        node_type = %s,
                        update_time = NOW()
                    WHERE device_code = %s AND deleted = 0
                    """,
                    (product["id"], protocol_code, product.get("node_type"), device_code),
                )
                conn.commit()
                summary["APPLIED"] += 1
                result["status"] = "APPLIED"
                result["message"] = "device contract updated"
            else:
                summary["VALIDATED"] += 1
                result["status"] = "VALIDATED"
                result["message"] = "repair row validated"
            result["current"] = fetch_device_by_code(cur, device_code)
        except Exception as exc:  # noqa: BLE001 - per-row failure should not stop the batch
            conn.rollback()
            summary["FAILED"] += 1
            result["status"] = "FAILED"
            result["message"] = str(exc)
        results.append(result)
    return {"summary": summary, "records": results}


def run_audit(cur: pymysql.cursors.Cursor, args: argparse.Namespace) -> Dict[str, object]:
    rows = fetch_devices(cur, normalize_device_codes(args.device_code), args.device_prefix, args.limit)
    result = {"summary": summarize_audit(rows), "records": rows}
    if args.export_repair_csv:
        exported = export_repair_csv(args.export_repair_csv, rows, args)
        result["repairChecklistPath"] = str(Path(args.export_repair_csv))
        result["repairChecklistCount"] = exported
    return result


def print_report(payload: Dict[str, object]) -> None:
    print(json.dumps(payload, ensure_ascii=False, default=str, indent=2))


def main() -> int:
    args = parse_args()
    mode = validate_mode(args)
    conn_args = resolve_connection_args(args)
    conn = pymysql.connect(
        host=conn_args["host"],
        port=conn_args["port"],
        user=conn_args["user"],
        password=conn_args["password"],
        database=conn_args["database"],
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        connect_timeout=10,
        read_timeout=30,
        write_timeout=30,
        autocommit=False,
    )
    try:
        with conn.cursor() as cur:
            if mode == "repair":
                result = repair_from_file(conn, cur, load_repair_rows(args.repair_file), args.apply)
            else:
                result = run_audit(cur, args)
            print_report(result)
        if mode == "repair":
            return 0 if result["summary"]["FAILED"] == 0 else 1
        return 0
    except Exception as exc:
        conn.rollback()
        print(f"[device-contract] {exc}", file=sys.stderr)
        return 1
    finally:
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
