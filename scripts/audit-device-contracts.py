#!/usr/bin/env python3
"""Audit and optionally repair device access contracts in the real environment."""

from __future__ import annotations

import argparse
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


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Audit or repair iot_device product/protocol contracts.")
    parser.add_argument("--device-prefix", help="Audit devices by code prefix, for example SK11")
    parser.add_argument("--device-code", action="append", default=[], help="Audit a specific device code. Repeatable.")
    parser.add_argument("--limit", type=int, default=100, help="Max rows to inspect. Defaults to 100.")
    parser.add_argument("--jdbc-url", help="Override JDBC url. Defaults to application-dev.yml / env.")
    parser.add_argument("--user", help="Override mysql username. Defaults to application-dev.yml / env.")
    parser.add_argument("--password", help="Override mysql password. Defaults to application-dev.yml / env.")
    parser.add_argument("--product-key", help="Target productKey when applying a repair.")
    parser.add_argument("--protocol-code", default="mqtt-json", help="Target protocol when applying a repair.")
    parser.add_argument("--apply", action="store_true", help="Apply repair for explicit --device-code targets.")
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


def build_where_clause(args: argparse.Namespace, params: List[object]) -> str:
    clauses = ["d.deleted = 0"]
    device_codes = normalize_device_codes(args.device_code)
    if device_codes:
        placeholders = ", ".join(["%s"] * len(device_codes))
        clauses.append(f"d.device_code IN ({placeholders})")
        params.extend(device_codes)
    if args.device_prefix:
        clauses.append("d.device_code LIKE %s")
        params.append(f"{args.device_prefix}%")
    if len(clauses) == 1:
        raise RuntimeError("Please provide --device-prefix or at least one --device-code.")
    return " WHERE " + " AND ".join(clauses)


def normalize_device_codes(device_codes: Iterable[str]) -> List[str]:
    normalized: List[str] = []
    for device_code in device_codes:
        if device_code and device_code.strip():
            normalized.append(device_code.strip())
    return normalized


def fetch_devices(cur: pymysql.cursors.Cursor, args: argparse.Namespace) -> List[Dict[str, object]]:
    params: List[object] = []
    where_clause = build_where_clause(args, params)
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
    params.append(max(args.limit, 1))
    cur.execute(sql, params)
    rows = cur.fetchall()
    return [enrich_issue_summary(row) for row in rows]


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


def summarize(rows: List[Dict[str, object]]) -> Dict[str, int]:
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


def apply_repair(conn: pymysql.connections.Connection,
                 cur: pymysql.cursors.Cursor,
                 args: argparse.Namespace,
                 rows: List[Dict[str, object]]) -> List[Dict[str, object]]:
    device_codes = normalize_device_codes(args.device_code)
    if not args.apply:
        return rows
    if not device_codes:
        raise RuntimeError("--apply requires at least one explicit --device-code target.")
    if not args.product_key:
        raise RuntimeError("--apply requires --product-key.")

    product = lookup_product(cur, args.product_key.strip())
    target_protocol = normalize_text(args.protocol_code) or normalize_text(product.get("protocol_code"))
    if not target_protocol:
        raise RuntimeError("Target product has no protocol_code and --protocol-code is blank.")
    if normalize_text(product.get("protocol_code")) and normalize_text(product.get("protocol_code")).lower() != target_protocol.lower():
        raise RuntimeError(
            f"--protocol-code {target_protocol} does not match product {product['product_key']} protocol {product['protocol_code']}"
        )
    if product.get("status") != 1:
        raise RuntimeError(f"Target product is disabled: {product['product_key']}")

    found_codes = {row["device_code"] for row in rows}
    missing_codes = [code for code in device_codes if code not in found_codes]
    if missing_codes:
        raise RuntimeError(f"Target device codes not found in current query result: {', '.join(missing_codes)}")

    for row in rows:
        if row["device_code"] not in device_codes:
            continue
        cur.execute(
            """
            UPDATE iot_device
            SET product_id = %s,
                protocol_code = %s,
                node_type = %s,
                update_time = NOW()
            WHERE id = %s AND deleted = 0
            """,
            (product["id"], target_protocol, product.get("node_type"), row["id"]),
        )
    conn.commit()
    return fetch_devices(cur, args)


def print_report(rows: List[Dict[str, object]]) -> None:
    print(json.dumps({"summary": summarize(rows), "records": rows}, ensure_ascii=False, default=str, indent=2))


def main() -> int:
    args = parse_args()
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
            rows = fetch_devices(cur, args)
            if args.apply:
                rows = apply_repair(conn, cur, args, rows)
            print_report(rows)
        return 0
    except Exception as exc:
        conn.rollback()
        print(f"[device-contract] {exc}", file=sys.stderr)
        return 1
    finally:
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
