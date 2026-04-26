#!/usr/bin/env python3
"""Audit monitoring-product risk metric catalog coverage in the shared dev database."""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from collections import Counter
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Dict, Iterable, List, Sequence
from urllib.parse import urlparse

import pymysql


REPO_ROOT = Path(__file__).resolve().parent.parent
APP_DEV_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application-dev.yml"
DEFAULT_OUTPUT_DIR = REPO_ROOT / "logs" / "acceptance"

GNSS_TOTAL_IDENTIFIERS = {"gpsTotalX", "gpsTotalY", "gpsTotalZ"}
DEEP_DISPLACEMENT_IDENTIFIERS = {"dispsX", "dispsY"}
LOGICAL_CHANNEL_PATTERN = re.compile(r"(?i)^(L\d+)_([A-Z]+)_(\d+)$")

STATUS_PUBLISHED_OK = "PUBLISHED_OK"
STATUS_EXPECTED_EMPTY = "EXPECTED_EMPTY"
STATUS_NEEDS_BACKFILL = "MISSING_CATALOG_NEEDS_BACKFILL"
STATUS_SELF_HEAL_ONLY = "MISSING_CATALOG_READ_SIDE_SELF_HEAL_ONLY"
STATUS_RULE_DRIFT = "CATALOG_RULE_DRIFT"
FAIL_STATUSES = {STATUS_NEEDS_BACKFILL, STATUS_RULE_DRIFT}


@dataclass(frozen=True)
class ParsedIdentifier:
    original: str
    level: str | None
    monitor_type: str | None
    channel_no: str | None
    leaf: str | None


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Audit monitoring-product risk metric catalog coverage")
    parser.add_argument("--product-key", action="append", default=[], help="Audit only the specified productKey. Repeatable.")
    parser.add_argument("--limit", type=int, default=200, help="Max products to inspect when no productKey is specified.")
    parser.add_argument("--jdbc-url", help="Override JDBC url. Defaults to application-dev.yml / env.")
    parser.add_argument("--user", help="Override mysql username. Defaults to application-dev.yml / env.")
    parser.add_argument("--password", help="Override mysql password. Defaults to application-dev.yml / env.")
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT_DIR), help="Directory for json/md reports.")
    parser.add_argument(
        "--write-repair-plan",
        action="store_true",
        help="Write a repair manifest for products that should backfill or retire risk metric catalog rows.",
    )
    parser.add_argument("--fail-on-gaps", action="store_true", help="Exit non-zero when backfill gaps or rule drift are detected.")
    return parser.parse_args(argv)


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


def normalize_product_keys(product_keys: Iterable[str]) -> List[str]:
    normalized = []
    for product_key in product_keys:
        text = normalize_text(product_key)
        if text:
            normalized.append(text)
    return normalized


def normalize_text(value: object) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def fetch_products(cur: pymysql.cursors.Cursor, product_keys: List[str], limit: int) -> List[Dict[str, object]]:
    params: List[object] = []
    where_clauses = ["p.deleted = 0"]
    if product_keys:
        placeholders = ", ".join(["%s"] * len(product_keys))
        where_clauses.append(f"p.product_key IN ({placeholders})")
        params.extend(product_keys)
    else:
        where_clauses.append("p.product_name LIKE %s")
        params.append("%监测型%")
    params.append(max(limit, 1))
    cur.execute(
        f"""
        SELECT p.id,
               p.tenant_id,
               p.product_key,
               p.product_name,
               p.manufacturer,
               p.description,
               COUNT(DISTINCT b.id) AS release_batch_count,
               MAX(b.id) AS latest_release_batch_id,
               MAX(b.create_time) AS latest_release_time
        FROM iot_product p
        LEFT JOIN iot_product_contract_release_batch b
               ON b.product_id = p.id
              AND b.deleted = 0
        WHERE {' AND '.join(where_clauses)}
        GROUP BY p.id, p.product_key, p.product_name
        ORDER BY p.id
        LIMIT %s
        """,
        params,
    )
    return list(cur.fetchall())


def fetch_property_models(cur: pymysql.cursors.Cursor, product_ids: List[int]) -> Dict[int, List[Dict[str, object]]]:
    if not product_ids:
        return {}
    placeholders = ", ".join(["%s"] * len(product_ids))
    cur.execute(
        f"""
        SELECT id, product_id, identifier, model_name, description, data_type, specs_json
        FROM iot_product_model
        WHERE deleted = 0
          AND model_type = 'property'
          AND product_id IN ({placeholders})
        ORDER BY product_id, identifier
        """,
        product_ids,
    )
    models_by_product: Dict[int, List[Dict[str, object]]] = {}
    for row in cur.fetchall():
        models_by_product.setdefault(int(row["product_id"]), []).append(row)
    return models_by_product


def fetch_catalog_rows(cur: pymysql.cursors.Cursor, product_ids: List[int]) -> Dict[int, List[Dict[str, object]]]:
    if not product_ids:
        return {}
    placeholders = ", ".join(["%s"] * len(product_ids))
    cur.execute(
        f"""
        SELECT id,
               product_id,
               tenant_id,
               release_batch_id,
               product_model_id,
               contract_identifier,
               normative_identifier,
               risk_metric_code,
               risk_metric_name,
               risk_category,
               metric_role,
               source_scenario_code,
               metric_unit,
               metric_dimension,
               threshold_type,
               semantic_direction,
               threshold_direction,
               trend_enabled,
               gis_enabled,
               insight_enabled,
               analytics_enabled,
               enabled,
               lifecycle_status
        FROM risk_metric_catalog
        WHERE deleted = 0
          AND product_id IN ({placeholders})
        ORDER BY product_id, contract_identifier
        """,
        product_ids,
    )
    rows_by_product: Dict[int, List[Dict[str, object]]] = {}
    for row in cur.fetchall():
        rows_by_product.setdefault(int(row["product_id"]), []).append(row)
    return rows_by_product


def fetch_normative_definitions(
    cur: pymysql.cursors.Cursor,
    scenario_codes: Iterable[str],
) -> Dict[str, Dict[str, Dict[str, object]]]:
    normalized_scenarios = sorted({code for code in (normalize_text(item) for item in scenario_codes) if code})
    if not normalized_scenarios:
        return {}
    placeholders = ", ".join(["%s"] * len(normalized_scenarios))
    cur.execute(
        f"""
        SELECT scenario_code,
               identifier,
               unit,
               monitor_content_code,
               monitor_type_code,
               trend_enabled,
               metric_dimension,
               threshold_type,
               semantic_direction,
               gis_enabled,
               insight_enabled,
               analytics_enabled,
               status,
               metadata_json
        FROM iot_normative_metric_definition
        WHERE deleted = 0
          AND scenario_code IN ({placeholders})
        ORDER BY scenario_code, identifier
        """,
        normalized_scenarios,
    )
    definitions_by_scenario: Dict[str, Dict[str, Dict[str, object]]] = {}
    for row in cur.fetchall():
        scenario_code = normalize_text(row.get("scenario_code"))
        identifier = normalize_identifier(row.get("identifier"))
        if not scenario_code or not identifier:
            continue
        definitions_by_scenario.setdefault(scenario_code, {}).setdefault(identifier, row)
    return definitions_by_scenario


def normalize_identifier(value: object) -> str | None:
    return normalize_text(value)


def parse_identifier(identifier: str | None) -> ParsedIdentifier | None:
    normalized = normalize_identifier(identifier)
    if not normalized:
        return None
    prefix = None
    leaf = normalized
    dot_index = normalized.rfind(".")
    if 0 < dot_index < len(normalized) - 1:
        prefix = normalized[:dot_index]
        leaf = normalized[dot_index + 1 :]
    elif LOGICAL_CHANNEL_PATTERN.match(normalized):
        prefix = normalized
        leaf = None
    if not prefix:
        return ParsedIdentifier(normalized, None, None, None, leaf)
    matcher = LOGICAL_CHANNEL_PATTERN.match(prefix)
    if not matcher:
        return ParsedIdentifier(normalized, None, None, None, leaf)
    return ParsedIdentifier(
        original=normalized,
        level=matcher.group(1).upper(),
        monitor_type=matcher.group(2).upper(),
        channel_no=matcher.group(3),
        leaf=leaf,
    )


def safe_lower(value: object) -> str:
    text = normalize_text(value)
    return text.lower() if text else ""


def build_context_blob(product: Dict[str, object], contract: Dict[str, object]) -> str:
    return " ".join(
        [
            safe_lower(product.get("product_key")),
            safe_lower(product.get("product_name")),
            safe_lower(contract.get("model_name")),
            safe_lower(contract.get("description")),
        ]
    )


def matches_scope_only(product: Dict[str, object], *tokens: str) -> bool:
    blob = " ".join([safe_lower(product.get("product_key")), safe_lower(product.get("product_name"))])
    return any(token and token.lower() in blob for token in tokens)


def matches_any(product: Dict[str, object], contract: Dict[str, object], *tokens: str) -> bool:
    blob = build_context_blob(product, contract)
    return any(token and token.lower() in blob for token in tokens)


def excludes_risk_catalog(product: Dict[str, object]) -> bool:
    return matches_scope_only(product, "phase5-mud-level", "mud-level", "mud_level", "泥位") or matches_scope_only(
        product, "base-station", "base_station", "基准站"
    )


def matches_single_value_risk(product: Dict[str, object], contract: Dict[str, object]) -> bool:
    return (
        matches_any(product, contract, "phase1-crack", "crack", "裂缝")
        or matches_any(product, contract, "laser-rangefinder", "laser_rangefinder", "laser", "激光", "测距")
        or matches_any(product, contract, "phase4-rain-gauge", "rain-gauge", "rain_gauge", "rain", "雨量")
    )


def matches_gnss(product: Dict[str, object], contract: Dict[str, object]) -> bool:
    return matches_any(product, contract, "phase2-gnss", "gnss", "北斗")


def matches_deep_displacement(product: Dict[str, object], contract: Dict[str, object]) -> bool:
    return matches_any(product, contract, "phase3-deep-displacement", "deep-displacement", "deep_displacement", "深部位移")


def is_risk_ready_identifier(product: Dict[str, object], contract: Dict[str, object]) -> bool:
    identifier = normalize_identifier(contract.get("identifier"))
    parsed = parse_identifier(identifier)
    if parsed is None:
        return False
    if excludes_risk_catalog(product):
        return False
    if parsed.level == "L1" and parsed.monitor_type == "LF":
        return parsed.leaf == "value"
    if parsed.level == "L1" and parsed.monitor_type == "GP":
        return parsed.leaf in GNSS_TOTAL_IDENTIFIERS
    if parsed.original in DEEP_DISPLACEMENT_IDENTIFIERS:
        return matches_deep_displacement(product, contract)
    if parsed.original in GNSS_TOTAL_IDENTIFIERS:
        return matches_gnss(product, contract)
    if parsed.original == "value":
        return matches_single_value_risk(product, contract)
    return False


def resolve_risk_enabled_identifiers(product: Dict[str, object], property_models: List[Dict[str, object]]) -> List[str]:
    identifiers = []
    seen = set()
    for contract in property_models:
        identifier = normalize_identifier(contract.get("identifier"))
        if not identifier or identifier in seen:
            continue
        seen.add(identifier)
        if is_risk_ready_identifier(product, contract):
            identifiers.append(identifier)
    return identifiers


def resolve_scope_scenario_code(product: Dict[str, object]) -> str | None:
    if matches_scope_only(product, "phase2-gnss", "gnss", "北斗", "卫星"):
        return "phase2-gnss"
    if matches_scope_only(product, "phase3-deep-displacement", "deep-displacement", "deep_displacement", "深部位移"):
        return "phase3-deep-displacement"
    if matches_scope_only(product, "phase4-rain-gauge", "rain-gauge", "rain_gauge", "rain", "雨量"):
        return "phase4-rain-gauge"
    if matches_scope_only(
        product,
        "phase1-crack",
        "crack",
        "裂缝",
        "laser-rangefinder",
        "laser_rangefinder",
        "laser",
        "激光",
        "测距",
    ):
        return "phase1-crack"
    return None


def resolve_semantic_reference(product: Dict[str, object], identifier: str | None) -> tuple[str | None, str | None]:
    normalized = normalize_identifier(identifier)
    if not normalized:
        return None, None
    parsed = parse_identifier(normalized)
    if parsed is None:
        return None, normalized
    fallback_scenario = resolve_scope_scenario_code(product)
    if parsed.level == "L1" and parsed.monitor_type == "LF" and parsed.leaf == "value":
        return "phase1-crack", "value"
    if parsed.level == "L1" and parsed.monitor_type == "GP" and parsed.leaf in GNSS_TOTAL_IDENTIFIERS:
        return "phase2-gnss", parsed.leaf
    if normalized in GNSS_TOTAL_IDENTIFIERS:
        return "phase2-gnss", normalized
    if normalized in DEEP_DISPLACEMENT_IDENTIFIERS:
        return "phase3-deep-displacement", normalized
    if normalized == "value":
        return fallback_scenario, "value"
    return fallback_scenario, normalized


def parse_json_object(value: object) -> Dict[str, object]:
    text = normalize_text(value)
    if not text:
        return {}
    try:
        parsed = json.loads(text)
    except json.JSONDecodeError:
        return {}
    return parsed if isinstance(parsed, dict) else {}


def read_string(source: Dict[str, object], *keys: str) -> str | None:
    if not source:
        return None
    for key in keys:
        if key not in source:
            continue
        normalized = normalize_text(source.get(key))
        if normalized:
            return normalized
    return None


def to_boolean_int(value: object) -> int | None:
    if value is None:
        return None
    if isinstance(value, bool):
        return 1 if value else 0
    if isinstance(value, (int, float)):
        return 1 if int(value) > 0 else 0
    normalized = normalize_text(value)
    if not normalized:
        return None
    if normalized.lower() in {"1", "true", "yes"}:
        return 1
    if normalized.lower() in {"0", "false", "no"}:
        return 0
    return None


def read_boolean_as_int(source: Dict[str, object], *keys: str) -> int | None:
    if not source:
        return None
    for key in keys:
        if key not in source:
            continue
        parsed = to_boolean_int(source.get(key))
        if parsed is not None:
            return parsed
    return None


def normalize_flag(value: object) -> int | None:
    parsed = to_boolean_int(value)
    return parsed


def first_non_blank(*values: object) -> str | None:
    for value in values:
        normalized = normalize_text(value)
        if normalized:
            return normalized
    return None


def first_non_null(*values: object) -> object | None:
    for value in values:
        if value is not None:
            return value
    return None


def is_numeric_data_type(value: object) -> bool:
    normalized = safe_lower(value)
    return normalized in {"double", "float", "int", "integer", "long", "short", "decimal"}


def default_metric_role(contract: Dict[str, object]) -> str:
    return "PRIMARY" if is_numeric_data_type(contract.get("data_type")) else "STATE"


def default_trend_enabled(contract: Dict[str, object]) -> int:
    return 1 if is_numeric_data_type(contract.get("data_type")) else 0


def default_analytics_enabled(contract: Dict[str, object]) -> int:
    return 1 if is_numeric_data_type(contract.get("data_type")) else 0


def resolve_default_threshold_direction(contract: Dict[str, object]) -> str | None:
    return "HIGHER_IS_RISKIER" if is_numeric_data_type(contract.get("data_type")) else None


def build_risk_metric_code(product_id: object, identifier: str | None) -> str | None:
    normalized = normalize_identifier(identifier)
    if not normalized:
        return None
    safe_identifier = re.sub(r"[^A-Z0-9]+", "_", normalized.upper()).strip("_")
    return f"RM_{product_id or 'GLOBAL'}_{safe_identifier}"


def build_semantic_profile(
    product: Dict[str, object],
    contract: Dict[str, object],
    normative_definitions_by_scenario: Dict[str, Dict[str, Dict[str, object]]],
) -> Dict[str, object]:
    identifier = normalize_identifier(contract.get("identifier"))
    scenario_code, canonical_identifier = resolve_semantic_reference(product, identifier)
    normative_definition = (
        normative_definitions_by_scenario.get(scenario_code or "", {}).get(canonical_identifier or "")
        if scenario_code and canonical_identifier
        else None
    )
    specs = parse_json_object(contract.get("specs_json"))
    metadata = parse_json_object((normative_definition or {}).get("metadata_json"))

    resolved_scenario_code = first_non_blank((normative_definition or {}).get("scenario_code"), scenario_code)
    normative_identifier = first_non_blank((normative_definition or {}).get("identifier"), canonical_identifier, identifier)
    metric_unit = first_non_blank(
        read_string(specs, "unit"),
        (normative_definition or {}).get("unit"),
        read_string(metadata, "unit"),
    )
    metric_dimension = first_non_blank(
        read_string(specs, "dimension"),
        (normative_definition or {}).get("metric_dimension"),
        read_string(metadata, "dimension"),
        (normative_definition or {}).get("monitor_type_code"),
    )
    threshold_type = first_non_blank(
        read_string(specs, "thresholdType"),
        (normative_definition or {}).get("threshold_type"),
        read_string(metadata, "thresholdType"),
        read_string(metadata, "thresholdKind"),
    )
    semantic_direction = first_non_blank(
        read_string(specs, "semanticDirection"),
        (normative_definition or {}).get("semantic_direction"),
        read_string(metadata, "semanticDirection"),
    )
    threshold_direction = first_non_blank(
        read_string(specs, "thresholdDirection"),
        read_string(metadata, "thresholdDirection"),
        (normative_definition or {}).get("semantic_direction"),
        semantic_direction,
        resolve_default_threshold_direction(contract),
    )
    normalized_semantic_direction = first_non_blank(semantic_direction, threshold_direction)
    risk_category = first_non_blank(
        read_string(specs, "riskCategory"),
        read_string(metadata, "riskCategory"),
        (normative_definition or {}).get("monitor_content_code"),
        resolved_scenario_code,
    )
    metric_role = first_non_blank(
        read_string(specs, "metricRole"),
        read_string(metadata, "metricRole"),
        default_metric_role(contract),
    )
    lifecycle_status = first_non_blank(
        read_string(specs, "lifecycleStatus"),
        read_string(metadata, "lifecycleStatus"),
        (normative_definition or {}).get("status"),
        "ACTIVE",
    )
    trend_enabled = first_non_null(
        read_boolean_as_int(specs, "trendEnabled"),
        normalize_flag((normative_definition or {}).get("trend_enabled")),
        read_boolean_as_int(metadata, "trendEnabled"),
        default_trend_enabled(contract),
    )
    gis_enabled = first_non_null(
        read_boolean_as_int(specs, "gisEnabled", "fitGis"),
        normalize_flag((normative_definition or {}).get("gis_enabled")),
        read_boolean_as_int(metadata, "gisEnabled", "fitGis"),
        0,
    )
    insight_enabled = first_non_null(
        read_boolean_as_int(specs, "insightEnabled", "fitInsight"),
        normalize_flag((normative_definition or {}).get("insight_enabled")),
        read_boolean_as_int(metadata, "insightEnabled", "fitInsight"),
        1,
    )
    analytics_enabled = first_non_null(
        read_boolean_as_int(specs, "analyticsEnabled", "fitAnalytics"),
        normalize_flag((normative_definition or {}).get("analytics_enabled")),
        read_boolean_as_int(metadata, "analyticsEnabled", "fitAnalytics"),
        default_analytics_enabled(contract),
    )
    return {
        "sourceScenarioCode": resolved_scenario_code,
        "normativeIdentifier": normative_identifier,
        "riskCategory": risk_category,
        "metricRole": metric_role,
        "lifecycleStatus": lifecycle_status,
        "metricUnit": metric_unit,
        "metricDimension": metric_dimension,
        "thresholdType": threshold_type,
        "semanticDirection": normalized_semantic_direction,
        "thresholdDirection": threshold_direction,
        "trendEnabled": trend_enabled,
        "gisEnabled": gis_enabled,
        "insightEnabled": insight_enabled,
        "analyticsEnabled": analytics_enabled,
    }


def classify_product_audit(
    product: Dict[str, object],
    property_models: List[Dict[str, object]],
    catalog_rows: List[Dict[str, object]],
) -> Dict[str, object]:
    release_batch_count = int(product.get("release_batch_count") or 0)
    expected_identifiers = resolve_risk_enabled_identifiers(product, property_models)
    enabled_catalog_identifiers = sorted(
        {
            normalize_identifier(row.get("contract_identifier"))
            for row in catalog_rows
            if int(row.get("enabled") or 0) == 1 and normalize_identifier(row.get("contract_identifier"))
        }
    )
    expected_set = set(expected_identifiers)
    catalog_set = set(enabled_catalog_identifiers)
    missing_identifiers = sorted(expected_set - catalog_set)
    unexpected_identifiers = sorted(catalog_set - expected_set)

    if not expected_set and not catalog_set:
        audit_status = STATUS_EXPECTED_EMPTY
    elif expected_set and not catalog_set:
        audit_status = STATUS_NEEDS_BACKFILL if release_batch_count > 0 else STATUS_SELF_HEAL_ONLY
    elif not expected_set and catalog_set:
        audit_status = STATUS_RULE_DRIFT
    elif missing_identifiers or unexpected_identifiers:
        audit_status = STATUS_RULE_DRIFT
    else:
        audit_status = STATUS_PUBLISHED_OK

    notes: List[str] = []
    if audit_status == STATUS_NEEDS_BACKFILL:
        notes.append("已有正式发布批次，但当前启用目录为空，建议补一次目录回填。")
    if audit_status == STATUS_SELF_HEAL_ONLY:
        notes.append("当前无正式发布批次，风险绑定依赖 formal-metrics 读侧自愈。")
    if audit_status == STATUS_EXPECTED_EMPTY and expected_set == set():
        notes.append("按当前风险目录发布规则，空目录是预期结果。")
    if audit_status == STATUS_RULE_DRIFT:
        if missing_identifiers:
            notes.append("当前启用目录缺少按规则应发布的正式字段。")
        if unexpected_identifiers:
            notes.append("当前启用目录存在按规则不应发布的正式字段。")

    return {
        "productId": product.get("id"),
        "productKey": product.get("product_key"),
        "productName": product.get("product_name"),
        "releaseBatchCount": release_batch_count,
        "latestReleaseTime": product.get("latest_release_time").isoformat() if product.get("latest_release_time") else None,
        "propertyIdentifiers": [normalize_identifier(row.get("identifier")) for row in property_models if normalize_identifier(row.get("identifier"))],
        "expectedRiskIdentifiers": expected_identifiers,
        "currentCatalogIdentifiers": enabled_catalog_identifiers,
        "missingCatalogIdentifiers": missing_identifiers,
        "unexpectedCatalogIdentifiers": unexpected_identifiers,
        "auditStatus": audit_status,
        "notes": notes,
    }


def summarize_results(results: List[Dict[str, object]]) -> Dict[str, int]:
    summary = Counter(result["auditStatus"] for result in results)
    summary["TOTAL"] = len(results)
    return dict(summary)


def render_markdown_report(report: Dict[str, object]) -> str:
    summary = report["summary"]
    lines = [
        "# Monitoring Risk Metric Catalog Audit",
        "",
        f"- Generated at: {report['generatedAt']}",
        f"- Scope: {report['scopeDescription']}",
        "",
        "## Summary",
        "",
        f"- TOTAL: {summary.get('TOTAL', 0)}",
        f"- {STATUS_PUBLISHED_OK}: {summary.get(STATUS_PUBLISHED_OK, 0)}",
        f"- {STATUS_EXPECTED_EMPTY}: {summary.get(STATUS_EXPECTED_EMPTY, 0)}",
        f"- {STATUS_NEEDS_BACKFILL}: {summary.get(STATUS_NEEDS_BACKFILL, 0)}",
        f"- {STATUS_SELF_HEAL_ONLY}: {summary.get(STATUS_SELF_HEAL_ONLY, 0)}",
        f"- {STATUS_RULE_DRIFT}: {summary.get(STATUS_RULE_DRIFT, 0)}",
        "",
        "## Product Results",
        "",
        "| productKey | status | releaseBatches | expectedRiskIdentifiers | currentCatalogIdentifiers |",
        "| --- | --- | ---: | --- | --- |",
    ]
    for result in report["products"]:
        lines.append(
            f"| {result['productKey']} | {result['auditStatus']} | {result['releaseBatchCount']} | "
            f"{', '.join(result['expectedRiskIdentifiers']) or '-'} | "
            f"{', '.join(result['currentCatalogIdentifiers']) or '-'} |"
        )
    lines.append("")
    attention_rows = [row for row in report["products"] if row["auditStatus"] in FAIL_STATUSES or row["auditStatus"] == STATUS_SELF_HEAL_ONLY]
    if attention_rows:
        lines.extend(["## Attention", ""])
        for row in attention_rows:
            reason = "；".join(row["notes"]) if row["notes"] else "需要进一步人工确认。"
            lines.append(
                f"- `{row['productKey']}`: {row['auditStatus']}，期望 `{', '.join(row['expectedRiskIdentifiers']) or '-'}`，"
                f"当前目录 `{', '.join(row['currentCatalogIdentifiers']) or '-'}`。{reason}"
            )
        lines.append("")
    return "\n".join(lines)


def write_reports(report: Dict[str, object], output_dir: Path) -> Dict[str, str]:
    output_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    json_path = output_dir / f"monitoring-risk-metric-catalog-audit-{timestamp}.json"
    md_path = output_dir / f"monitoring-risk-metric-catalog-audit-{timestamp}.md"
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    md_path.write_text(render_markdown_report(report), encoding="utf-8")
    return {
        "json": str(json_path),
        "markdown": str(md_path),
    }


def build_report(products: List[Dict[str, object]],
                 property_models_by_product: Dict[int, List[Dict[str, object]]],
                 catalog_rows_by_product: Dict[int, List[Dict[str, object]]],
                 product_keys: List[str]) -> Dict[str, object]:
    results = []
    for product in products:
        product_id = int(product["id"])
        results.append(
            classify_product_audit(
                product,
                property_models_by_product.get(product_id, []),
                catalog_rows_by_product.get(product_id, []),
            )
        )
    return {
        "generatedAt": datetime.now().isoformat(),
        "scopeDescription": ", ".join(product_keys) if product_keys else "all monitoring products",
        "summary": summarize_results(results),
        "products": results,
    }


def build_repair_plan(
    report: Dict[str, object],
    products: List[Dict[str, object]],
    property_models_by_product: Dict[int, List[Dict[str, object]]],
    catalog_rows_by_product: Dict[int, List[Dict[str, object]]],
    normative_definitions_by_scenario: Dict[str, Dict[str, Dict[str, object]]],
) -> Dict[str, object]:
    product_by_id = {int(product["id"]): product for product in products}
    plan_products: List[Dict[str, object]] = []
    skipped_products: List[Dict[str, object]] = []
    upsert_count = 0
    retire_count = 0

    for result in report["products"]:
        product_id = int(result["productId"])
        audit_status = result["auditStatus"]
        if audit_status == STATUS_SELF_HEAL_ONLY:
            skipped_products.append(
                {
                    "productId": product_id,
                    "productKey": result["productKey"],
                    "auditStatus": audit_status,
                    "reason": "当前无正式发布批次，仅建议保留 formal-metrics 读侧自愈，不生成持久化目录回填动作。",
                }
            )
            continue
        product = product_by_id.get(product_id)
        if not product:
            continue
        if audit_status not in FAIL_STATUSES:
            continue

        property_models = property_models_by_product.get(product_id, [])
        contracts_by_identifier = {
            normalize_identifier(contract.get("identifier")): contract
            for contract in property_models
            if normalize_identifier(contract.get("identifier"))
        }
        catalog_rows = catalog_rows_by_product.get(product_id, [])
        catalog_by_identifier = {
            normalize_identifier(row.get("contract_identifier")): row
            for row in catalog_rows
            if normalize_identifier(row.get("contract_identifier"))
        }
        upserts: List[Dict[str, object]] = []
        retires: List[Dict[str, object]] = []

        for identifier in result["missingCatalogIdentifiers"]:
            contract = contracts_by_identifier.get(identifier)
            if not contract:
                continue
            existing_row = catalog_by_identifier.get(identifier)
            semantic_profile = build_semantic_profile(product, contract, normative_definitions_by_scenario)
            upserts.append(
                {
                    "operation": "UPSERT_CATALOG_ROW",
                    "existingCatalogId": existing_row.get("id") if existing_row else None,
                    "row": {
                        "tenantId": product.get("tenant_id"),
                        "productId": product_id,
                        "releaseBatchId": product.get("latest_release_batch_id"),
                        "productModelId": contract.get("id"),
                        "contractIdentifier": identifier,
                        "riskMetricCode": build_risk_metric_code(product_id, identifier),
                        "riskMetricName": first_non_blank(contract.get("model_name"), identifier),
                        "enabled": 1,
                        **semantic_profile,
                    },
                }
            )
        for identifier in result["unexpectedCatalogIdentifiers"]:
            row = catalog_by_identifier.get(identifier)
            if not row:
                continue
            retires.append(
                {
                    "operation": "RETIRE_CATALOG_ROW",
                    "catalogId": row.get("id"),
                    "contractIdentifier": identifier,
                    "enabled": 0,
                    "lifecycleStatus": "RETIRED",
                }
            )

        if not upserts and not retires:
            continue
        upsert_count += len(upserts)
        retire_count += len(retires)
        plan_products.append(
            {
                "productId": product_id,
                "productKey": result["productKey"],
                "productName": result["productName"],
                "auditStatus": audit_status,
                "latestReleaseBatchId": product.get("latest_release_batch_id"),
                "actions": {
                    "upserts": upserts,
                    "retires": retires,
                },
                "notes": result.get("notes") or [],
            }
        )

    return {
        "generatedAt": datetime.now().isoformat(),
        "scopeDescription": report["scopeDescription"],
        "summary": {
            "eligibleProductCount": len(plan_products),
            "skippedSelfHealOnlyCount": len(skipped_products),
            "upsertCount": upsert_count,
            "retireCount": retire_count,
        },
        "products": plan_products,
        "skippedProducts": skipped_products,
    }


def render_repair_markdown(plan: Dict[str, object]) -> str:
    summary = plan["summary"]
    lines = [
        "# Monitoring Risk Metric Catalog Repair Plan",
        "",
        f"- Generated at: {plan['generatedAt']}",
        f"- Scope: {plan['scopeDescription']}",
        "",
        "## Summary",
        "",
        f"- eligibleProductCount: {summary.get('eligibleProductCount', 0)}",
        f"- skippedSelfHealOnlyCount: {summary.get('skippedSelfHealOnlyCount', 0)}",
        f"- upsertCount: {summary.get('upsertCount', 0)}",
        f"- retireCount: {summary.get('retireCount', 0)}",
        "",
    ]
    if plan["products"]:
        lines.extend(
            [
                "## Planned Actions",
                "",
                "| productKey | auditStatus | latestReleaseBatchId | upserts | retires |",
                "| --- | --- | ---: | ---: | ---: |",
            ]
        )
        for item in plan["products"]:
            lines.append(
                f"| {item['productKey']} | {item['auditStatus']} | {item['latestReleaseBatchId'] or '-'} | "
                f"{len(item['actions']['upserts'])} | {len(item['actions']['retires'])} |"
            )
        lines.append("")
    if plan["skippedProducts"]:
        lines.extend(["## Skipped", ""])
        for item in plan["skippedProducts"]:
            lines.append(f"- `{item['productKey']}`: {item['reason']}")
        lines.append("")
    return "\n".join(lines)


def write_repair_plan(plan: Dict[str, object], output_dir: Path) -> Dict[str, str]:
    output_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    json_path = output_dir / f"monitoring-risk-metric-catalog-repair-{timestamp}.json"
    md_path = output_dir / f"monitoring-risk-metric-catalog-repair-{timestamp}.md"
    json_path.write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding="utf-8")
    md_path.write_text(render_repair_markdown(plan), encoding="utf-8")
    return {
        "json": str(json_path),
        "markdown": str(md_path),
    }


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv)
    product_keys = normalize_product_keys(args.product_key)
    connection_args = resolve_connection_args(args)
    conn = pymysql.connect(
        host=connection_args["host"],
        port=int(connection_args["port"]),
        user=str(connection_args["user"]),
        password=str(connection_args["password"]),
        database=str(connection_args["database"]),
        charset="utf8mb4",
        autocommit=True,
        cursorclass=pymysql.cursors.DictCursor,
    )
    try:
        with conn.cursor() as cur:
            products = fetch_products(cur, product_keys, args.limit)
            product_ids = [int(row["id"]) for row in products]
            property_models_by_product = fetch_property_models(cur, product_ids)
            catalog_rows_by_product = fetch_catalog_rows(cur, product_ids)
            scenario_codes = set()
            for product in products:
                scope_scenario = resolve_scope_scenario_code(product)
                if scope_scenario:
                    scenario_codes.add(scope_scenario)
                for contract in property_models_by_product.get(int(product["id"]), []):
                    scenario_code, _ = resolve_semantic_reference(product, contract.get("identifier"))
                    if scenario_code:
                        scenario_codes.add(scenario_code)
            normative_definitions_by_scenario = fetch_normative_definitions(
                cur,
                scenario_codes,
            )
        report = build_report(products, property_models_by_product, catalog_rows_by_product, product_keys)
        outputs = write_reports(report, Path(args.output_dir))
        payload = {
            "summary": report["summary"],
            "jsonReport": outputs["json"],
            "markdownReport": outputs["markdown"],
        }
        if args.write_repair_plan:
            repair_plan = build_repair_plan(
                report,
                products,
                property_models_by_product,
                catalog_rows_by_product,
                normative_definitions_by_scenario,
            )
            repair_outputs = write_repair_plan(repair_plan, Path(args.output_dir))
            payload["repairPlanSummary"] = repair_plan["summary"]
            payload["repairPlanJson"] = repair_outputs["json"]
            payload["repairPlanMarkdown"] = repair_outputs["markdown"]
        print(json.dumps(payload, ensure_ascii=False, indent=2))
        if args.fail_on_gaps and any(item["auditStatus"] in FAIL_STATUSES for item in report["products"]):
            return 1
        return 0
    finally:
        conn.close()


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
