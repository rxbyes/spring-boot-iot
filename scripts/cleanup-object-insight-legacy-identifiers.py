#!/usr/bin/env python3
"""Cleanup legacy object-insight identifiers with governance suggestion reporting."""

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
    parser.add_argument(
        "--no-governance-suggestions",
        action="store_true",
        help="Skip runtime evidence scan and recommendation classification",
    )
    parser.add_argument(
        "--runtime-sample-limit",
        type=int,
        default=20,
        help="Max runtime identifier samples per unresolved metric",
    )
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


def assert_runtime_sample_limit(args: argparse.Namespace) -> None:
    if args.runtime_sample_limit <= 0:
        raise RuntimeError("--runtime-sample-limit 必须大于 0")


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


def deduplicate_texts(values: list[str]) -> list[str]:
    dedup: dict[str, str] = {}
    for value in values:
        text = normalize_text(value)
        key = normalize_key(text)
        if key and key not in dedup:
            dedup[key] = text
    return list(dedup.values())


def resolve_identifier_detail(raw_identifier: str, published_identifiers: list[str]) -> dict[str, object]:
    normalized = normalize_text(raw_identifier)
    if not normalized:
        return {"resolvedIdentifier": None, "source": "empty", "tailMatches": []}
    by_key = {normalize_key(item): item for item in published_identifiers if normalize_key(item)}
    exact = by_key.get(normalize_key(normalized))
    input_tail = normalize_key(identifier_tail(normalized))
    tail_matches = deduplicate_texts(
        [item for item in published_identifiers if normalize_key(identifier_tail(item)) == input_tail]
    )
    if exact:
        return {"resolvedIdentifier": exact, "source": "exact", "tailMatches": tail_matches}
    if "." in normalized:
        return {"resolvedIdentifier": None, "source": "unresolved", "tailMatches": tail_matches}
    if not input_tail:
        return {"resolvedIdentifier": None, "source": "unresolved", "tailMatches": tail_matches}
    if len(tail_matches) == 1:
        return {"resolvedIdentifier": tail_matches[0], "source": "tail", "tailMatches": tail_matches}
    if len(tail_matches) > 1:
        return {"resolvedIdentifier": None, "source": "ambiguous", "tailMatches": tail_matches}
    return {"resolvedIdentifier": None, "source": "unresolved", "tailMatches": []}


def resolve_identifier(raw_identifier: str, published_identifiers: list[str]) -> tuple[str | None, str]:
    detail = resolve_identifier_detail(raw_identifier, published_identifiers)
    return detail["resolvedIdentifier"], detail["source"]


def deduplicate_metric_issues(issues: list[dict[str, object]]) -> list[dict[str, object]]:
    dedup: dict[tuple[str, str], dict[str, object]] = {}
    for issue in issues:
        identifier = normalize_text(issue.get("identifier"))
        source = normalize_text(issue.get("source"))
        key = (normalize_key(identifier), source)
        if not key[0] or not source:
            continue
        if key not in dedup:
            dedup[key] = {
                "identifier": identifier,
                "source": source,
                "publishedTailMatches": deduplicate_texts(list(issue.get("publishedTailMatches") or [])),
            }
            continue
        merged_matches = list(dedup[key].get("publishedTailMatches") or [])
        merged_matches.extend(list(issue.get("publishedTailMatches") or []))
        dedup[key]["publishedTailMatches"] = deduplicate_texts(merged_matches)
    return list(dedup.values())


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
        "issues": [],
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
            detail = resolve_identifier_detail(raw_identifier, published_identifiers)
            resolved_identifier = detail["resolvedIdentifier"]
            source = detail["source"]
            if resolved_identifier:
                metric_copy["identifier"] = resolved_identifier
                if normalize_key(resolved_identifier) != normalize_key(raw_identifier):
                    result["changed"] = True
            elif source == "ambiguous":
                result["ambiguous"].append(raw_identifier)
                result["issues"].append(
                    {
                        "identifier": raw_identifier,
                        "source": source,
                        "publishedTailMatches": detail["tailMatches"],
                    }
                )
            elif source == "unresolved":
                result["unresolved"].append(raw_identifier)
                result["issues"].append(
                    {
                        "identifier": raw_identifier,
                        "source": source,
                        "publishedTailMatches": detail["tailMatches"],
                    }
                )
        transformed_metrics.append(metric_copy)
    deduplicated_metrics = deduplicate_metrics(transformed_metrics)
    if len(deduplicated_metrics) != len(transformed_metrics):
        result["changed"] = True
    result["afterMetricIdentifiers"] = [
        normalize_text(item.get("identifier"))
        for item in deduplicated_metrics
        if isinstance(item, dict) and normalize_text(item.get("identifier"))
    ]
    result["unresolved"] = deduplicate_texts(result["unresolved"])
    result["ambiguous"] = deduplicate_texts(result["ambiguous"])
    result["issues"] = deduplicate_metric_issues(result["issues"])
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


def load_property_identifier_samples(
    cursor, product_id: int, raw_identifier: str, sample_limit: int
) -> list[dict[str, object]]:
    cursor.execute(
        """
        SELECT dp.identifier, COUNT(*) AS row_count
        FROM iot_device_property dp
        JOIN iot_device d ON d.id = dp.device_id
        WHERE d.deleted = 0
          AND d.product_id = %s
          AND (
            LOWER(dp.identifier) = LOWER(%s)
            OR LOWER(SUBSTRING_INDEX(dp.identifier, '.', -1)) = LOWER(%s)
          )
        GROUP BY dp.identifier
        ORDER BY row_count DESC, dp.identifier ASC
        LIMIT %s
        """,
        (product_id, raw_identifier, raw_identifier, sample_limit),
    )
    rows = cursor.fetchall()
    return [{"identifier": normalize_text(row[0]), "deviceRows": int(row[1])} for row in rows]


def load_evidence_identifier_samples(
    cursor, product_id: int, raw_identifier: str, sample_limit: int
) -> list[dict[str, object]]:
    cursor.execute(
        """
        SELECT raw_identifier, SUM(COALESCE(evidence_count, 0)) AS hit_count, COUNT(*) AS row_count
        FROM iot_vendor_metric_evidence
        WHERE deleted = 0
          AND product_id = %s
          AND (
            LOWER(raw_identifier) = LOWER(%s)
            OR LOWER(SUBSTRING_INDEX(raw_identifier, '.', -1)) = LOWER(%s)
          )
        GROUP BY raw_identifier
        ORDER BY hit_count DESC, row_count DESC, raw_identifier ASC
        LIMIT %s
        """,
        (product_id, raw_identifier, raw_identifier, sample_limit),
    )
    rows = cursor.fetchall()
    return [
        {
            "identifier": normalize_text(row[0]),
            "evidenceHits": int(row[1] or 0),
            "evidenceRows": int(row[2] or 0),
        }
        for row in rows
    ]


def load_evidence_canonical_suggestions(
    cursor, product_id: int, raw_identifier: str, sample_limit: int
) -> list[dict[str, object]]:
    cursor.execute(
        """
        SELECT canonical_identifier, SUM(COALESCE(evidence_count, 0)) AS hit_count
        FROM iot_vendor_metric_evidence
        WHERE deleted = 0
          AND product_id = %s
          AND canonical_identifier IS NOT NULL
          AND canonical_identifier <> ''
          AND (
            LOWER(raw_identifier) = LOWER(%s)
            OR LOWER(SUBSTRING_INDEX(raw_identifier, '.', -1)) = LOWER(%s)
          )
        GROUP BY canonical_identifier
        ORDER BY hit_count DESC, canonical_identifier ASC
        LIMIT %s
        """,
        (product_id, raw_identifier, raw_identifier, sample_limit),
    )
    rows = cursor.fetchall()
    return [{"identifier": normalize_text(row[0]), "evidenceHits": int(row[1] or 0)} for row in rows]


def collect_runtime_signals(
    cursor, product_id: int, raw_identifier: str, sample_limit: int
) -> dict[str, object]:
    property_samples = load_property_identifier_samples(cursor, product_id, raw_identifier, sample_limit)
    evidence_samples = load_evidence_identifier_samples(cursor, product_id, raw_identifier, sample_limit)
    canonical_suggestions = load_evidence_canonical_suggestions(cursor, product_id, raw_identifier, sample_limit)

    raw_key = normalize_key(raw_identifier)
    raw_tail_key = normalize_key(identifier_tail(raw_identifier))
    property_exact_rows = 0
    property_tail_rows = 0
    for item in property_samples:
        identifier_key = normalize_key(item["identifier"])
        row_count = int(item["deviceRows"])
        if identifier_key == raw_key:
            property_exact_rows += row_count
        elif normalize_key(identifier_tail(item["identifier"])) == raw_tail_key:
            property_tail_rows += row_count

    evidence_exact_hits = 0
    evidence_tail_hits = 0
    for item in evidence_samples:
        identifier_key = normalize_key(item["identifier"])
        hit_count = int(item["evidenceHits"])
        if identifier_key == raw_key:
            evidence_exact_hits += hit_count
        elif normalize_key(identifier_tail(item["identifier"])) == raw_tail_key:
            evidence_tail_hits += hit_count

    candidate_identifiers = deduplicate_texts(
        [item["identifier"] for item in property_samples] + [item["identifier"] for item in evidence_samples]
    )

    return {
        "propertySamples": property_samples,
        "evidenceSamples": evidence_samples,
        "canonicalSuggestions": canonical_suggestions,
        "candidateIdentifiers": candidate_identifiers,
        "propertyTotalRows": sum(int(item["deviceRows"]) for item in property_samples),
        "propertyExactRows": property_exact_rows,
        "propertyTailRows": property_tail_rows,
        "evidenceTotalRows": sum(int(item["evidenceRows"]) for item in evidence_samples),
        "evidenceTotalHits": sum(int(item["evidenceHits"]) for item in evidence_samples),
        "evidenceExactHits": evidence_exact_hits,
        "evidenceTailHits": evidence_tail_hits,
    }


def classify_governance_action(issue: dict[str, object], runtime_signals: dict[str, object]) -> dict[str, object]:
    source = normalize_text(issue.get("source"))
    raw_identifier = normalize_text(issue.get("identifier"))
    candidate_identifiers = list(runtime_signals.get("candidateIdentifiers") or [])
    canonical_suggestions = [item["identifier"] for item in runtime_signals.get("canonicalSuggestions", [])]
    candidate_by_key = {normalize_key(item): item for item in candidate_identifiers if normalize_key(item)}
    raw_key = normalize_key(raw_identifier)

    if source == "ambiguous":
        return {
            "recommendedAction": "needs_manual_decision",
            "reasonCode": "ambiguous_published_tail_match",
            "targetIdentifier": None,
        }

    if raw_key and "." in raw_identifier and raw_key in candidate_by_key:
        return {
            "recommendedAction": "recommend_publish",
            "reasonCode": "runtime_exact_full_path",
            "targetIdentifier": candidate_by_key[raw_key],
        }

    if len(candidate_identifiers) == 1:
        return {
            "recommendedAction": "recommend_publish",
            "reasonCode": "single_runtime_identifier",
            "targetIdentifier": candidate_identifiers[0],
        }

    if not candidate_identifiers:
        if len(canonical_suggestions) == 1 and int(runtime_signals.get("evidenceTotalHits", 0)) > 0:
            return {
                "recommendedAction": "recommend_publish",
                "reasonCode": "single_canonical_suggestion",
                "targetIdentifier": canonical_suggestions[0],
            }
        return {
            "recommendedAction": "recommend_deprecate",
            "reasonCode": "no_runtime_evidence",
            "targetIdentifier": None,
        }

    return {
        "recommendedAction": "needs_manual_decision",
        "reasonCode": "multiple_runtime_identifiers",
        "targetIdentifier": None,
    }


def summarize_governance_recommendations(items: list[dict[str, object]]) -> dict[str, int]:
    summary = {
        "total": len(items),
        "recommendPublish": 0,
        "recommendDeprecate": 0,
        "needsManualDecision": 0,
    }
    for item in items:
        action = normalize_text(item.get("recommendedAction"))
        if action == "recommend_publish":
            summary["recommendPublish"] += 1
        elif action == "recommend_deprecate":
            summary["recommendDeprecate"] += 1
        elif action == "needs_manual_decision":
            summary["needsManualDecision"] += 1
    return summary


def build_governance_recommendations(
    cursor, product_id: int, issues: list[dict[str, object]], sample_limit: int
) -> list[dict[str, object]]:
    recommendations: list[dict[str, object]] = []
    for issue in issues:
        runtime_signals = collect_runtime_signals(cursor, product_id, normalize_text(issue.get("identifier")), sample_limit)
        decision = classify_governance_action(issue, runtime_signals)
        recommendations.append(
            {
                "identifier": normalize_text(issue.get("identifier")),
                "issueType": normalize_text(issue.get("source")),
                "publishedTailMatches": deduplicate_texts(list(issue.get("publishedTailMatches") or [])),
                "recommendedAction": decision["recommendedAction"],
                "reasonCode": decision["reasonCode"],
                "targetIdentifier": decision["targetIdentifier"],
                "runtimeSignals": runtime_signals,
            }
        )
    return recommendations


def build_report(args: argparse.Namespace, conn_params: dict[str, object], product_reports: list[dict[str, object]]) -> dict[str, object]:
    changed = [item for item in product_reports if item.get("status") == "changed"]
    blocked = [item for item in product_reports if item.get("status") == "blocked"]
    skipped = [item for item in product_reports if item.get("status") == "skipped"]
    unchanged = [item for item in product_reports if item.get("status") == "unchanged"]
    applied = [item for item in product_reports if item.get("applied") is True]
    all_recommendations = [
        recommendation
        for item in product_reports
        for recommendation in item.get("governanceRecommendations", [])
    ]
    governance_summary = summarize_governance_recommendations(all_recommendations)
    products_with_recommendations = [item for item in product_reports if item.get("governanceRecommendations")]
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
            "productsWithRecommendations": len(products_with_recommendations),
            "recommendPublishItems": governance_summary["recommendPublish"],
            "recommendDeprecateItems": governance_summary["recommendDeprecate"],
            "needsManualDecisionItems": governance_summary["needsManualDecision"],
        },
        "governanceRecommendationSummary": governance_summary,
        "products": product_reports,
    }


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    assert_scope(args)
    assert_apply_allowed(args)
    assert_runtime_sample_limit(args)

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
                    "issues": transform["issues"],
                    "status": "unchanged",
                    "reason": transform["reason"],
                    "applied": False,
                    "governanceRecommendations": [],
                    "governanceRecommendationSummary": {
                        "total": 0,
                        "recommendPublish": 0,
                        "recommendDeprecate": 0,
                        "needsManualDecision": 0,
                    },
                }
                if transform["blocked"]:
                    report_item["status"] = "blocked"
                elif transform["changed"]:
                    report_item["status"] = "changed"
                    if args.mode == "apply":
                        update_product_metadata(cursor, product_id, transform["normalizedMetadataJson"])
                        report_item["applied"] = True
                if not args.no_governance_suggestions and transform["issues"]:
                    governance_recommendations = build_governance_recommendations(
                        cursor,
                        product_id,
                        transform["issues"],
                        args.runtime_sample_limit,
                    )
                    report_item["governanceRecommendations"] = governance_recommendations
                    report_item["governanceRecommendationSummary"] = summarize_governance_recommendations(
                        governance_recommendations
                    )
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
