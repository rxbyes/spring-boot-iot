#!/usr/bin/env python3
"""Cleanup legacy object-insight identifiers with a safe dry-run/apply workflow."""

from __future__ import annotations

import argparse
import json
import re
from datetime import datetime
from pathlib import Path
from urllib.parse import parse_qs, urlparse


REPO_ROOT = Path(__file__).resolve().parent.parent
APP_DEV_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application-dev.yml"


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Cleanup legacy short identifiers in iot_product.metadata_json.objectInsight.customMetrics"
    )
    parser.add_argument("--mode", choices=("dry-run", "apply"), default="dry-run")
    parser.add_argument("--all-products", action="store_true", help="Scan all products with object-insight metadata")
    parser.add_argument("--product-key", action="append", default=[], help="Target product key (repeatable)")
    parser.add_argument("--product-id", action="append", default=[], help="Target product id (repeatable)")
    parser.add_argument("--host")
    parser.add_argument("--port", type=int)
    parser.add_argument("--user")
    parser.add_argument("--password")
    parser.add_argument("--database")
    parser.add_argument("--report-path", help="Optional JSON report output path")
    parser.add_argument("--confirm", action="store_true", help="Required when --mode apply")
    return parser.parse_args(argv)


def extract_default(text: str, env_name: str) -> str:
    match = re.search(rf"{env_name}:([^}}]+)}}", text)
    if not match:
        raise RuntimeError(f"Unable to resolve {env_name} from {APP_DEV_PATH}")
    return match.group(1).strip()


def load_dev_defaults() -> dict[str, str]:
    text = APP_DEV_PATH.read_text(encoding="utf-8")
    return {
        "jdbc_url": extract_default(text, "IOT_MYSQL_URL"),
        "user": extract_default(text, "IOT_MYSQL_USERNAME"),
        "password": extract_default(text, "IOT_MYSQL_PASSWORD"),
    }


def parse_jdbc_url(jdbc_url: str) -> dict[str, object]:
    if not jdbc_url.startswith("jdbc:mysql://"):
        raise RuntimeError(f"Unsupported JDBC URL: {jdbc_url}")
    parsed = urlparse(jdbc_url[len("jdbc:"):])
    database = parsed.path.lstrip("/")
    if not database:
        raise RuntimeError(f"JDBC URL missing database name: {jdbc_url}")
    return {
        "host": parsed.hostname or "127.0.0.1",
        "port": parsed.port or 3306,
        "database": database,
        "query": parse_qs(parsed.query, keep_blank_values=True),
    }


def resolve_connection_params(args: argparse.Namespace) -> dict[str, object]:
    defaults = load_dev_defaults()
    parsed = parse_jdbc_url(defaults["jdbc_url"])
    query = parsed["query"]
    return {
        "host": args.host or parsed["host"],
        "port": int(args.port or parsed["port"]),
        "user": args.user or defaults["user"],
        "password": args.password or defaults["password"],
        "database": args.database or parsed["database"],
        "charset": query.get("characterEncoding", ["utf8mb4"])[0] or "utf8mb4",
    }


def open_connection(conn_params: dict[str, object]):
    try:
        import pymysql
    except ImportError as exc:
        raise RuntimeError("pymysql 未安装，无法执行清理脚本") from exc
    return pymysql.connect(
        host=conn_params["host"],
        port=conn_params["port"],
        user=conn_params["user"],
        password=conn_params["password"],
        database=conn_params["database"],
        charset=conn_params["charset"],
        autocommit=False,
    )


def normalize_text(value: object) -> str:
    if isinstance(value, str):
        return value.strip()
    return ""


def normalize_key(value: object) -> str:
    text = normalize_text(value)
    return text.lower() if text else ""


def identifier_tail(identifier: str) -> str:
    normalized = normalize_text(identifier)
    if not normalized:
        return ""
    separator = normalized.rfind(".")
    if separator < 0 or separator >= len(normalized) - 1:
        return normalized
    return normalize_text(normalized[separator + 1 :])


def assert_scope(args: argparse.Namespace) -> None:
    if args.all_products:
        return
    if args.product_key or args.product_id:
        return
    raise RuntimeError("请至少指定 --all-products 或 --product-key/--product-id")


def assert_apply_allowed(args: argparse.Namespace) -> None:
    if args.mode == "apply" and not args.confirm:
        raise RuntimeError("apply 模式需要显式传入 --confirm")


def build_product_query(args: argparse.Namespace) -> tuple[str, list[object]]:
    sql = [
        "SELECT id, product_key, product_name, metadata_json",
        "FROM iot_product",
        "WHERE deleted = 0",
        "AND metadata_json IS NOT NULL",
        "AND metadata_json <> ''",
    ]
    params: list[object] = []
    if args.product_id:
        placeholders = ",".join(["%s"] * len(args.product_id))
        sql.append(f"AND id IN ({placeholders})")
        params.extend(args.product_id)
    if args.product_key:
        placeholders = ",".join(["%s"] * len(args.product_key))
        sql.append(f"AND product_key IN ({placeholders})")
        params.extend(args.product_key)
    sql.append("ORDER BY id ASC")
    return "\n".join(sql), params


def load_products(cursor, args: argparse.Namespace) -> list[dict[str, object]]:
    sql, params = build_product_query(args)
    cursor.execute(sql, tuple(params))
    rows = cursor.fetchall()
    products: list[dict[str, object]] = []
    for row in rows:
        products.append(
            {
                "id": int(row[0]),
                "productKey": row[1],
                "productName": row[2],
                "metadataJson": row[3],
            }
        )
    return products


def load_latest_released_batch_id(cursor, product_id: int) -> int | None:
    cursor.execute(
        """
        SELECT id
        FROM iot_product_contract_release_batch
        WHERE deleted = 0
          AND product_id = %s
          AND release_status = 'RELEASED'
          AND rollback_time IS NULL
        ORDER BY create_time DESC, id DESC
        LIMIT 1
        """,
        (product_id,),
    )
    row = cursor.fetchone()
    return int(row[0]) if row else None


def load_published_identifiers_from_snapshot(cursor, product_id: int, release_batch_id: int) -> list[str]:
    cursor.execute(
        """
        SELECT snapshot_json
        FROM iot_product_metric_resolver_snapshot
        WHERE deleted = 0
          AND product_id = %s
          AND release_batch_id = %s
        ORDER BY create_time DESC, id DESC
        LIMIT 1
        """,
        (product_id, release_batch_id),
    )
    row = cursor.fetchone()
    if not row or not row[0]:
        return []
    try:
        payload = json.loads(row[0])
    except json.JSONDecodeError:
        return []
    identifiers = payload.get("publishedIdentifiers")
    if not isinstance(identifiers, list):
        return []
    dedup: dict[str, str] = {}
    for identifier in identifiers:
        text = normalize_text(identifier)
        key = normalize_key(text)
        if key and key not in dedup:
            dedup[key] = text
    return list(dedup.values())


def load_published_identifiers_from_product_model(cursor, product_id: int) -> list[str]:
    cursor.execute(
        """
        SELECT identifier
        FROM iot_product_model
        WHERE deleted = 0
          AND product_id = %s
          AND model_type = 'property'
        ORDER BY sort_no ASC, identifier ASC
        """,
        (product_id,),
    )
    rows = cursor.fetchall()
    dedup: dict[str, str] = {}
    for row in rows:
        text = normalize_text(row[0] if row else "")
        key = normalize_key(text)
        if key and key not in dedup:
            dedup[key] = text
    return list(dedup.values())


def load_published_identifiers(cursor, product_id: int) -> list[str]:
    release_batch_id = load_latest_released_batch_id(cursor, product_id)
    if release_batch_id is not None:
        identifiers = load_published_identifiers_from_snapshot(cursor, product_id, release_batch_id)
        if identifiers:
            return identifiers
    return load_published_identifiers_from_product_model(cursor, product_id)


def resolve_identifier(raw_identifier: str, published_identifiers: list[str]) -> tuple[str | None, str]:
    normalized = normalize_text(raw_identifier)
    if not normalized:
        return None, "empty"
    by_key = {normalize_key(item): item for item in published_identifiers if normalize_key(item)}
    exact = by_key.get(normalize_key(normalized))
    if exact:
        return exact, "exact"
    if "." in normalized:
        return None, "unresolved"
    input_tail = normalize_key(identifier_tail(normalized))
    if not input_tail:
        return None, "unresolved"
    matches = [item for item in published_identifiers if normalize_key(identifier_tail(item)) == input_tail]
    if len(matches) == 1:
        return matches[0], "tail"
    if len(matches) > 1:
        return None, "ambiguous"
    return None, "unresolved"


def deduplicate_metrics(metrics: list[dict[str, object]]) -> list[dict[str, object]]:
    by_identifier: dict[str, dict[str, object]] = {}
    fallback = 0
    for item in metrics:
        identifier = normalize_text(item.get("identifier"))
        key = normalize_key(identifier)
        if key:
            by_identifier[key] = item
        else:
            fallback += 1
            by_identifier[f"@@empty_{fallback}"] = item
    return list(by_identifier.values())


def transform_metadata(metadata_json: str, published_identifiers: list[str]) -> dict[str, object]:
    result = {
        "changed": False,
        "blocked": False,
        "unresolved": [],
        "ambiguous": [],
        "reason": None,
        "normalizedMetadataJson": metadata_json,
        "beforeMetricIdentifiers": [],
        "afterMetricIdentifiers": [],
    }
    text = normalize_text(metadata_json)
    if not text:
        result["reason"] = "metadata_empty"
        return result
    try:
        payload = json.loads(text)
    except json.JSONDecodeError:
        result["reason"] = "metadata_invalid_json"
        result["blocked"] = True
        return result
    if not isinstance(payload, dict):
        result["reason"] = "metadata_not_object"
        result["blocked"] = True
        return result
    object_insight = payload.get("objectInsight")
    if not isinstance(object_insight, dict):
        result["reason"] = "object_insight_missing"
        return result
    custom_metrics = object_insight.get("customMetrics")
    if not isinstance(custom_metrics, list):
        result["reason"] = "custom_metrics_missing"
        return result
    transformed_metrics: list[dict[str, object]] = []
    for metric in custom_metrics:
        if not isinstance(metric, dict):
            transformed_metrics.append(metric)
            continue
        metric_copy = dict(metric)
        raw_identifier = normalize_text(metric_copy.get("identifier"))
        if raw_identifier:
            result["beforeMetricIdentifiers"].append(raw_identifier)
            resolved_identifier, source = resolve_identifier(raw_identifier, published_identifiers)
            if resolved_identifier:
                metric_copy["identifier"] = resolved_identifier
                if normalize_key(resolved_identifier) != normalize_key(raw_identifier):
                    result["changed"] = True
            elif source == "ambiguous":
                result["ambiguous"].append(raw_identifier)
            elif source == "unresolved":
                result["unresolved"].append(raw_identifier)
        transformed_metrics.append(metric_copy)
    deduplicated_metrics = deduplicate_metrics(transformed_metrics)
    if len(deduplicated_metrics) != len(transformed_metrics):
        result["changed"] = True
    result["afterMetricIdentifiers"] = [
        normalize_text(item.get("identifier"))
        for item in deduplicated_metrics
        if isinstance(item, dict) and normalize_text(item.get("identifier"))
    ]
    if result["unresolved"] or result["ambiguous"]:
        result["blocked"] = True
        result["reason"] = "manual_required"
        return result
    if result["changed"]:
        object_insight["customMetrics"] = deduplicated_metrics
        payload["objectInsight"] = object_insight
        result["normalizedMetadataJson"] = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
    result["reason"] = "changed" if result["changed"] else "no_change"
    return result


def update_product_metadata(cursor, product_id: int, metadata_json: str) -> None:
    cursor.execute(
        """
        UPDATE iot_product
        SET metadata_json = %s, update_time = NOW()
        WHERE id = %s AND deleted = 0
        """,
        (metadata_json, product_id),
    )


def default_report_path() -> Path:
    report_dir = REPO_ROOT / "tmp" / "object-insight-legacy-cleanup"
    report_dir.mkdir(parents=True, exist_ok=True)
    return report_dir / f"object-insight-legacy-cleanup-{datetime.now():%Y%m%d%H%M%S}.json"


def build_report(args: argparse.Namespace, conn_params: dict[str, object], product_reports: list[dict[str, object]]) -> dict[str, object]:
    changed = [item for item in product_reports if item.get("status") == "changed"]
    blocked = [item for item in product_reports if item.get("status") == "blocked"]
    skipped = [item for item in product_reports if item.get("status") == "skipped"]
    unchanged = [item for item in product_reports if item.get("status") == "unchanged"]
    applied = [item for item in product_reports if item.get("applied") is True]
    return {
        "mode": args.mode,
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "scope": {
            "allProducts": args.all_products,
            "productKeys": args.product_key,
            "productIds": args.product_id,
        },
        "connection": {
            "host": conn_params["host"],
            "port": conn_params["port"],
            "database": conn_params["database"],
            "user": conn_params["user"],
        },
        "summary": {
            "matchedProducts": len(product_reports),
            "changedProducts": len(changed),
            "blockedProducts": len(blocked),
            "unchangedProducts": len(unchanged),
            "skippedProducts": len(skipped),
            "appliedProducts": len(applied),
        },
        "products": product_reports,
    }


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    assert_scope(args)
    assert_apply_allowed(args)

    conn_params = resolve_connection_params(args)
    report_path = Path(args.report_path) if args.report_path else default_report_path()

    connection = open_connection(conn_params)
    product_reports: list[dict[str, object]] = []
    try:
        cursor = connection.cursor()
        try:
            products = load_products(cursor, args)
            for product in products:
                product_id = product["id"]
                product_key = product["productKey"]
                published_identifiers = load_published_identifiers(cursor, product_id)
                transform = transform_metadata(product["metadataJson"], published_identifiers)
                report_item = {
                    "productId": product_id,
                    "productKey": product_key,
                    "productName": product["productName"],
                    "publishedIdentifierCount": len(published_identifiers),
                    "publishedIdentifiers": published_identifiers,
                    "beforeMetricIdentifiers": transform["beforeMetricIdentifiers"],
                    "afterMetricIdentifiers": transform["afterMetricIdentifiers"],
                    "unresolved": transform["unresolved"],
                    "ambiguous": transform["ambiguous"],
                    "status": "unchanged",
                    "reason": transform["reason"],
                    "applied": False,
                }
                if transform["blocked"]:
                    report_item["status"] = "blocked"
                elif transform["changed"]:
                    report_item["status"] = "changed"
                    if args.mode == "apply":
                        update_product_metadata(cursor, product_id, transform["normalizedMetadataJson"])
                        report_item["applied"] = True
                product_reports.append(report_item)
            if args.mode == "apply":
                connection.commit()
            else:
                connection.rollback()
        finally:
            close_cursor = getattr(cursor, "close", None)
            if callable(close_cursor):
                close_cursor()
    except Exception:
        rollback = getattr(connection, "rollback", None)
        if callable(rollback):
            rollback()
        raise
    finally:
        close_connection = getattr(connection, "close", None)
        if callable(close_connection):
            close_connection()

    report = build_report(args, conn_params, product_reports)
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    output = {
        "mode": args.mode,
        "reportPath": str(report_path),
        "summary": report["summary"],
    }
    print(json.dumps(output, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
