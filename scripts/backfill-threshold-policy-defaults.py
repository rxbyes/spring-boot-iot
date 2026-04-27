#!/usr/bin/env python3
"""Plan or apply default threshold policies for real-environment risk bindings."""

from __future__ import annotations

import argparse
import csv
import copy
import json
import os
import re
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Iterable, Sequence

import pymysql


REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_CONFIG_PATH = REPO_ROOT / "config" / "automation" / "threshold-policy-defaults.json"
DEFAULT_OUTPUT_DIR = REPO_ROOT / "logs" / "acceptance"
SIMPLE_EXPRESSION = re.compile(r"^\s*(?:value\s*)?(>=|<=|>|<|==|=)\s*(-?\d+(?:\.\d+)?)\s*$", re.IGNORECASE)
NUMERIC_VALUE = re.compile(r"^\s*(-?\d+(?:\.\d+)?)\s*$")
TEMPLATE_CONFIRMATION_CSV_FIELDS = [
    "index",
    "semanticTemplateKey",
    "productType",
    "metricIdentifier",
    "metricName",
    "observedRange",
    "bindingCount",
    "productCount",
    "productKeys",
    "rawIdentifiers",
    "metricAliases",
    "expression",
    "confirmationStatus",
    "requiredActions",
]
REQUIRED_TEMPLATE_CONFIRMATION_CSV_FIELDS = [
    "semanticTemplateKey",
    "productType",
    "metricIdentifier",
    "metricName",
    "expression",
    "confirmationStatus",
]


@dataclass(frozen=True)
class PolicyCandidate:
    rule_scope: str
    tenant_id: int
    metric_identifier: str
    metric_name: str | None
    expression: str
    alarm_level: str
    duration: int
    notification_methods: str
    convert_to_event: int
    risk_metric_id: int | None = None
    product_type: str | None = None
    product_id: int | None = None
    device_id: int | None = None
    risk_point_device_id: int | None = None
    source: str = "UNKNOWN"
    covered_binding_count: int = 1

    def key(self) -> str:
        identity = {
            "PRODUCT_TYPE": self.product_type or "",
            "PRODUCT": str(self.product_id or 0),
            "DEVICE": str(self.device_id or 0),
            "BINDING": str(self.risk_point_device_id or 0),
        }.get(self.rule_scope, "")
        return "|".join([self.rule_scope, str(self.tenant_id), self.metric_identifier, identity])


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Backfill default threshold policies from confirmed templates.")
    parser.add_argument("--host", default=os.getenv("IOT_MYSQL_HOST", "8.130.107.120"))
    parser.add_argument("--port", type=int, default=int(os.getenv("IOT_MYSQL_PORT", "3306")))
    parser.add_argument("--db", default=os.getenv("IOT_MYSQL_DB", "rm_iot"))
    parser.add_argument("--user", default=os.getenv("IOT_MYSQL_USERNAME", "root"))
    parser.add_argument("--password", default=os.getenv("IOT_MYSQL_PASSWORD", "mI8%pB1*gD"))
    parser.add_argument("--config-path", default=str(DEFAULT_CONFIG_PATH))
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT_DIR))
    parser.add_argument("--apply", action="store_true", help="Insert planned candidates. Omit for dry-run.")
    parser.add_argument("--limit", type=int, default=0, help="Maximum candidates to apply. 0 means no limit.")
    parser.add_argument("--operator-id", type=int, default=0)
    parser.add_argument("--confirm-report", default="", help="Recent dry-run JSON report required for apply.")
    parser.add_argument("--confirm-candidate-count", type=int, default=None)
    parser.add_argument("--confirm-ready-template-count", type=int, default=None)
    parser.add_argument("--max-confirm-age-hours", type=int, default=24)
    parser.add_argument("--validate-config-only", action="store_true")
    parser.add_argument("--fail-on-template-breaches", action="store_true")
    parser.add_argument("--min-ready-template-count", type=int, default=0)
    parser.add_argument("--export-template-config-path", default="")
    parser.add_argument("--merge-template-confirmation-csv", default="")
    parser.add_argument("--merge-template-output-path", default="")
    return parser.parse_args(argv)


def connect(args: argparse.Namespace):
    return pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.db,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=False,
    )


def load_config(path: str | Path) -> dict[str, Any]:
    config_path = Path(path)
    if not config_path.exists():
        return {
            "defaultDuration": 0,
            "defaultAlarmLevel": "orange",
            "defaultNotificationMethods": "",
            "defaultConvertToEvent": True,
            "manualSemanticTemplates": [],
            "productTypeTemplates": [],
        }
    return json.loads(config_path.read_text(encoding="utf-8"))


def normalize_alarm_level(value: str | None, fallback: str = "orange") -> str:
    if not value or not value.strip():
        value = fallback
    normalized = value.strip().lower()
    return {
        "critical": "red",
        "warning": "orange",
        "warn": "orange",
        "high": "orange",
        "medium": "yellow",
        "info": "blue",
        "low": "blue",
    }.get(normalized, normalized)


def normalize_expression(value: str | None, direction: str = ">=") -> str | None:
    if value is None or not str(value).strip():
        return None
    raw = str(value).strip()
    match = SIMPLE_EXPRESSION.match(raw)
    if match:
        return f"value {match.group(1)} {match.group(2)}"
    numeric = NUMERIC_VALUE.match(raw)
    if numeric:
        return f"value {direction.strip() or '>='} {numeric.group(1)}"
    return None


def parse_numeric_value(value: Any) -> float | None:
    if value is None:
        return None
    match = NUMERIC_VALUE.match(str(value))
    if not match:
        return None
    return float(match.group(1))


def is_executable_expression(value: str | None) -> bool:
    return normalize_expression(value) is not None


def resolve_product_type(product_key: str | None, product_name: str | None) -> str:
    merged = " ".join(part.strip().lower() for part in [product_key, product_name] if part and part.strip())
    if not merged:
        return "UNKNOWN"
    if any(keyword in merged for keyword in ["video", "camera", "ipc", "视频", "摄像"]):
        return "VIDEO"
    if any(keyword in merged for keyword in ["warning", "warn", "预警", "声光", "报警"]):
        return "WARNING"
    if any(keyword in merged for keyword in [
        "monitor",
        "monitoring",
        "监测",
        "gnss",
        "位移",
        "倾角",
        "裂缝",
        "雨量",
        "水位",
        "激光",
        "泥位",
        "测距",
    ]):
        return "MONITORING"
    return "UNKNOWN"


def normalize_scope(scope: str | None) -> str:
    return scope.strip().upper() if scope and scope.strip() else "METRIC"


def normalize_metric_identifier(value: str | None) -> str:
    return value.strip() if value and value.strip() else ""


def normalize_metric_name(value: str | None) -> str:
    return value.strip() if value and value.strip() else ""


def dedupe_text(values: Iterable[Any]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for value in values:
        normalized = normalize_metric_identifier(str(value) if value is not None else None)
        if normalized and normalized not in seen:
            seen.add(normalized)
            result.append(normalized)
    return result


def metric_leaf_identifier(value: str | None) -> str:
    identifier = normalize_metric_identifier(value)
    if identifier in {"L1_LF_1", "L4_NW_1"}:
        return "value"
    if "." in identifier:
        return identifier.rsplit(".", 1)[-1]
    return identifier


def contains_any(value: str | None, keywords: Iterable[str]) -> bool:
    source = normalize_metric_name(value).lower()
    return bool(source) and any(keyword.lower() in source for keyword in keywords)


def manual_semantic_template_matches(template: dict[str, Any],
                                     product_type: str | None,
                                     product_key: str | None,
                                     metric_identifier: str | None,
                                     metric_name: str | None) -> bool:
    template_product_type = str(template.get("productType", "")).strip().upper()
    if template_product_type and template_product_type != str(product_type or "").strip().upper():
        return False
    match = template.get("match") if isinstance(template.get("match"), dict) else {}
    product_keys = dedupe_text(match.get("productKeys") or [])
    metric_names = dedupe_text(match.get("metricNames") or [])
    raw_identifiers = dedupe_text(match.get("rawIdentifiers") or [])
    identifiers = dedupe_text([
        template.get("metricIdentifier"),
        *(template.get("metricAliases") or []),
        *raw_identifiers,
    ])
    normalized_product_key = normalize_metric_identifier(product_key)
    normalized_metric_identifier = normalize_metric_identifier(metric_identifier)
    normalized_metric_name = normalize_metric_name(metric_name)
    if product_keys and normalized_product_key not in product_keys:
        return False
    if metric_names and normalized_metric_name not in metric_names:
        return False
    if raw_identifiers and normalized_metric_identifier not in raw_identifiers:
        return False
    return normalized_metric_identifier in identifiers


def manual_semantic_match(product_type: str | None,
                          product_key: str | None,
                          metric_identifier: str | None,
                          metric_name: str | None,
                          config: dict[str, Any] | None) -> dict[str, Any] | None:
    for template in (config or {}).get("manualSemanticTemplates") or []:
        if not manual_semantic_template_matches(template, product_type, product_key, metric_identifier, metric_name):
            continue
        metric_aliases = dedupe_text([template.get("metricIdentifier"), *(template.get("metricAliases") or [])])
        match = template.get("match") if isinstance(template.get("match"), dict) else {}
        return {
            "key": normalize_metric_identifier(template.get("semanticTemplateKey"))
            or f"manual:{normalize_metric_identifier(template.get('metricIdentifier'))}",
            "metricIdentifier": normalize_metric_identifier(template.get("metricIdentifier") or metric_identifier),
            "metricName": normalize_metric_name(template.get("metricName") or metric_name),
            "metricAliases": metric_aliases or dedupe_text([metric_identifier]),
            "match": {
                key: dedupe_text(match.get(key) or [])
                for key in ["productKeys", "metricNames", "rawIdentifiers"]
                if dedupe_text(match.get(key) or [])
            },
        }
    return None


def semantic_match(product_key: str | None,
                   metric_identifier: str | None,
                   metric_name: str | None,
                   product_type: str | None = None,
                   config: dict[str, Any] | None = None) -> dict[str, Any]:
    manual_match = manual_semantic_match(product_type, product_key, metric_identifier, metric_name, config)
    if manual_match is not None:
        return manual_match
    product_key = normalize_metric_identifier(product_key).lower()
    raw_identifier = normalize_metric_identifier(metric_identifier)
    leaf_identifier = metric_leaf_identifier(raw_identifier)
    name = normalize_metric_name(metric_name)
    raw_aliases = dedupe_text([raw_identifier, leaf_identifier])

    axis_names = {
        "X": ("X", "X\u8f74\u503e\u89d2", ["L1_QJ_1.X", "X"]),
        "Y": ("Y", "Y\u8f74\u503e\u89d2", ["L1_QJ_1.Y", "Y"]),
        "Z": ("Z", "Z\u8f74\u503e\u89d2", ["L1_QJ_1.Z", "Z"]),
    }
    acceleration_names = {
        "gX": ("gX", "X\u8f74\u52a0\u901f\u5ea6", ["L1_JS_1.gX", "gX"]),
        "gY": ("gY", "Y\u8f74\u52a0\u901f\u5ea6", ["L1_JS_1.gY", "gY"]),
        "gZ": ("gZ", "Z\u8f74\u52a0\u901f\u5ea6", ["L1_JS_1.gZ", "gZ"]),
    }
    gnss_names = {
        "gpsTotalX": ("gpsTotalX", "X\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf", ["L1_GP_1.gpsTotalX", "gpsTotalX"]),
        "gpsTotalY": ("gpsTotalY", "Y\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf", ["L1_GP_1.gpsTotalY", "gpsTotalY"]),
        "gpsTotalZ": ("gpsTotalZ", "Z\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf", ["L1_GP_1.gpsTotalZ", "gpsTotalZ"]),
    }
    deep_displacement_names = {
        "dispsX": ("dispsX", "\u987a\u6ed1\u52a8\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf", ["dispsX"]),
        "dispsY": ("dispsY", "\u5782\u76f4\u5761\u9762\u65b9\u5411\u7d2f\u8ba1\u53d8\u5f62\u91cf", ["dispsY"]),
    }

    if leaf_identifier in acceleration_names:
        canonical, canonical_name, aliases = acceleration_names[leaf_identifier]
        return {
            "key": f"axis-acceleration:{canonical}",
            "metricIdentifier": canonical,
            "metricName": canonical_name,
            "metricAliases": aliases,
            "match": {"rawIdentifiers": aliases},
        }
    if leaf_identifier in axis_names:
        canonical, canonical_name, aliases = axis_names[leaf_identifier]
        return {
            "key": f"axis-tilt:{canonical}",
            "metricIdentifier": canonical,
            "metricName": canonical_name,
            "metricAliases": aliases,
            "match": {"rawIdentifiers": aliases},
        }
    if leaf_identifier in gnss_names:
        canonical, canonical_name, aliases = gnss_names[leaf_identifier]
        return {
            "key": f"gnss-total:{canonical}",
            "metricIdentifier": canonical,
            "metricName": canonical_name,
            "metricAliases": aliases,
            "match": {"rawIdentifiers": aliases},
        }
    if leaf_identifier in deep_displacement_names:
        canonical, canonical_name, aliases = deep_displacement_names[leaf_identifier]
        return {
            "key": f"deep-displacement:{canonical}",
            "metricIdentifier": canonical,
            "metricName": canonical_name,
            "metricAliases": aliases,
            "match": {
                "productKeys": ["nf-monitor-deep-displacement-v1"],
                "rawIdentifiers": aliases,
            },
        }

    if raw_identifier.startswith("L1_LF_1") or contains_any(name, ["\u88c2\u7f1d"]) or "crack" in product_key:
        return {
            "key": "crack:value",
            "metricIdentifier": "value",
            "metricName": "\u88c2\u7f1d\u91cf",
            "metricAliases": dedupe_text(["L1_LF_1", "L1_LF_1.value", "value", *raw_aliases]),
            "match": {
                "metricNames": ["\u88c2\u7f1d\u503c", "\u88c2\u7f1d\u91cf"],
                "rawIdentifiers": ["L1_LF_1", "L1_LF_1.value", "value"],
            },
        }
    if contains_any(name, ["\u6fc0\u5149\u6d4b\u8ddd"]) or "laser-rangefinder" in product_key:
        return {
            "key": "laser-rangefinder:value",
            "metricIdentifier": "value",
            "metricName": "\u6fc0\u5149\u6d4b\u8ddd\u503c",
            "metricAliases": dedupe_text(["value", *raw_aliases]),
            "match": {
                "productKeys": ["nf-monitor-laser-rangefinder-v1"],
                "metricNames": ["\u6fc0\u5149\u6d4b\u8ddd\u503c"],
                "rawIdentifiers": ["value"],
            },
        }
    if contains_any(name, ["\u96e8\u91cf"]) or "rain-gauge" in product_key:
        return {
            "key": "rain-gauge:value",
            "metricIdentifier": "value",
            "metricName": "\u5f53\u524d\u96e8\u91cf",
            "metricAliases": dedupe_text(["value", *raw_aliases]),
            "match": {
                "productKeys": ["nf-monitor-tipping-bucket-rain-gauge-v1"],
                "metricNames": ["\u5f53\u524d\u96e8\u91cf"],
                "rawIdentifiers": ["value"],
            },
        }
    if raw_identifier == "L4_NW_1" or contains_any(name, ["\u6ce5\u4f4d"]) or "mud-level" in product_key:
        return {
            "key": "mud-level:value",
            "metricIdentifier": "value",
            "metricName": "\u6ce5\u4f4d\u9ad8\u7a0b\u503c",
            "metricAliases": dedupe_text(["L4_NW_1", "value", *raw_aliases]),
            "match": {
                "productKeys": ["nf-monitor-mud-level-meter-v1"],
                "metricNames": ["\u6ce5\u4f4d\u9ad8\u7a0b\u503c"],
                "rawIdentifiers": ["L4_NW_1", "value"],
            },
        }

    return {
        "key": f"raw:{raw_identifier}:{name}",
        "metricIdentifier": raw_identifier,
        "metricName": name,
        "metricAliases": raw_aliases,
        "match": {"rawIdentifiers": raw_aliases},
    }


def normalize_confirmation_status(template: dict[str, Any] | None) -> str:
    if not template:
        return "NEEDS_CONFIRMATION"
    value = template.get("confirmationStatus", template.get("status", "NEEDS_CONFIRMATION"))
    return str(value or "NEEDS_CONFIRMATION").strip().upper()


def is_template_ready(template: dict[str, Any] | None) -> bool:
    return normalize_confirmation_status(template) == "CONFIRMED" and is_executable_expression(
        None if template is None else template.get("expression")
    )


def template_match_values(template: dict[str, Any], key: str) -> list[str]:
    match = template.get("match") if isinstance(template.get("match"), dict) else {}
    values = match.get(key) if isinstance(match, dict) else None
    return dedupe_text(values or [])


def template_matches_binding(template: dict[str, Any], binding: dict[str, Any]) -> bool:
    metric_identifier = normalize_metric_identifier(binding.get("metric_identifier"))
    metric_name = normalize_metric_name(binding.get("metric_name"))
    product_key = normalize_metric_identifier(binding.get("product_key"))
    product_keys = template_match_values(template, "productKeys")
    metric_names = template_match_values(template, "metricNames")
    raw_identifiers = template_match_values(template, "rawIdentifiers")
    template_identifiers = dedupe_text([
        template.get("metricIdentifier"),
        *(template.get("metricAliases") or []),
        *raw_identifiers,
    ])
    if product_keys and product_key not in product_keys:
        return False
    if metric_names and metric_name not in metric_names:
        return False
    if raw_identifiers and metric_identifier not in raw_identifiers:
        return False
    return metric_identifier in template_identifiers


def format_observed_range(stats: dict[str, Any] | None) -> str:
    if not stats:
        return "--"
    numeric_count = int(stats.get("numericCount") or 0)
    return f"{stats.get('min')}..{stats.get('max')} (n={numeric_count})"


def build_template_confirmation_item(index: int, template: dict[str, Any]) -> dict[str, Any]:
    status = normalize_confirmation_status(template)
    required_actions: list[str] = []
    if status != "CONFIRMED":
        required_actions.append("CONFIRM_TEMPLATE")
    if not is_executable_expression(template.get("expression")):
        required_actions.append("FILL_EXPRESSION")
    evidence = template.get("evidence") if isinstance(template.get("evidence"), dict) else {}
    return {
        "index": index,
        "productType": str(template.get("productType", "")).strip().upper(),
        "semanticTemplateKey": normalize_metric_identifier(template.get("semanticTemplateKey")),
        "metricIdentifier": normalize_metric_identifier(template.get("metricIdentifier")),
        "metricName": normalize_metric_name(template.get("metricName")),
        "expression": template.get("expression"),
        "confirmationStatus": status,
        "observedRange": format_observed_range(template.get("observedValueStats")),
        "bindingCount": int(evidence.get("bindingCount") or 0),
        "productCount": int(evidence.get("productCount") or 0),
        "ready": not required_actions,
        "requiredActions": required_actions,
    }


def validate_template_config(config: dict[str, Any]) -> dict[str, Any]:
    templates = config.get("productTypeTemplates") or []
    breaches: list[dict[str, Any]] = []
    confirmation_checklist: list[dict[str, Any]] = []
    identity_groups: dict[str, list[int]] = {}
    identifier_groups: dict[str, list[dict[str, Any]]] = {}
    ready_count = 0
    confirmed_count = 0
    for index, template in enumerate(templates):
        confirmation_checklist.append(build_template_confirmation_item(index, template))
        product_type = str(template.get("productType", "")).strip().upper()
        metric_identifier = normalize_metric_identifier(template.get("metricIdentifier"))
        metric_name = normalize_metric_name(template.get("metricName"))
        status = normalize_confirmation_status(template)
        if status == "CONFIRMED":
            confirmed_count += 1
        if is_template_ready(template):
            ready_count += 1
        if not product_type:
            breaches.append({"type": "MISSING_PRODUCT_TYPE", "index": index})
        if template.get("riskMetricId") is None and not metric_identifier:
            breaches.append({"type": "MISSING_METRIC_IDENTITY", "index": index})
        if status != "CONFIRMED":
            breaches.append({"type": "TEMPLATE_NOT_CONFIRMED", "index": index, "status": status})
        if not is_executable_expression(template.get("expression")):
            breaches.append({"type": "INVALID_OR_MISSING_EXPRESSION", "index": index})
        identity_key = "|".join([
            product_type,
            str(template.get("riskMetricId") or ""),
            metric_identifier,
            metric_name,
        ])
        identity_groups.setdefault(identity_key, []).append(index)
        identifier_key = "|".join([product_type, metric_identifier])
        identifier_groups.setdefault(identifier_key, []).append({
            "index": index,
            "metricName": metric_name,
            "riskMetricId": template.get("riskMetricId"),
        })
    for key, indexes in identity_groups.items():
        if key.strip("|") and len(indexes) > 1:
            breaches.append({"type": "DUPLICATE_TEMPLATE_IDENTITY", "indexes": indexes})
    for key, items in identifier_groups.items():
        if key.endswith("|") or len(items) <= 1:
            continue
        has_unnamed = any(not item["metricName"] and item.get("riskMetricId") is None for item in items)
        if has_unnamed:
            breaches.append({
                "type": "AMBIGUOUS_IDENTIFIER_TEMPLATE",
                "key": key,
                "indexes": [item["index"] for item in items],
            })
    return {
        "templateCount": len(templates),
        "confirmedTemplateCount": confirmed_count,
        "readyTemplateCount": ready_count,
        "breachCount": len(breaches),
        "breaches": breaches,
        "confirmationChecklist": confirmation_checklist,
    }


def matches_metric(rule: dict[str, Any], binding: dict[str, Any]) -> bool:
    rule_metric_id = rule.get("risk_metric_id")
    binding_metric_id = binding.get("risk_metric_id")
    if rule_metric_id is not None and binding_metric_id is not None and int(rule_metric_id) == int(binding_metric_id):
        return True
    return normalize_metric_identifier(rule.get("metric_identifier")) == normalize_metric_identifier(binding.get("metric_identifier"))


def matches_scope(rule: dict[str, Any], binding: dict[str, Any]) -> bool:
    scope = normalize_scope(rule.get("rule_scope"))
    if scope == "BINDING":
        return binding.get("id") is not None and binding.get("id") == rule.get("risk_point_device_id")
    if scope == "DEVICE":
        return binding.get("device_id") is not None and binding.get("device_id") == rule.get("device_id")
    if scope == "PRODUCT":
        return binding.get("product_id") is not None and binding.get("product_id") == rule.get("product_id")
    if scope == "PRODUCT_TYPE":
        return bool(rule.get("product_type")) and binding.get("product_type") == str(rule.get("product_type")).strip().upper()
    return True


def is_binding_covered(binding: dict[str, Any], active_rules: Iterable[dict[str, Any]]) -> bool:
    return any(matches_metric(rule, binding) and matches_scope(rule, binding) for rule in active_rules)


def find_product_type_template(binding: dict[str, Any], config: dict[str, Any]) -> dict[str, Any] | None:
    product_type = binding.get("product_type")
    metric_identifier = normalize_metric_identifier(binding.get("metric_identifier"))
    metric_name = normalize_metric_name(binding.get("metric_name"))
    risk_metric_id = binding.get("risk_metric_id")
    templates = [
        template
        for template in config.get("productTypeTemplates") or []
        if str(template.get("productType", "")).strip().upper() == product_type
        and is_template_ready(template)
    ]
    for template in templates:
        if template.get("riskMetricId") is not None and risk_metric_id is not None:
            if int(template["riskMetricId"]) == int(risk_metric_id):
                return template
    identifier_matches = [
        template
        for template in templates
        if template_matches_binding(template, binding)
    ]
    named_matches = [
        template
        for template in identifier_matches
        if normalize_metric_name(template.get("metricName")) and normalize_metric_name(template.get("metricName")) == metric_name
    ]
    if len(named_matches) == 1:
        return named_matches[0]
    unnamed_matches = [
        template
        for template in identifier_matches
        if not normalize_metric_name(template.get("metricName"))
    ]
    if len(identifier_matches) == 1:
        return identifier_matches[0]
    if len(unnamed_matches) == 1 and not named_matches:
        return unnamed_matches[0]
    return None


def candidate_from_binding_default(binding: dict[str, Any], config: dict[str, Any]) -> PolicyCandidate | None:
    expression = normalize_expression(binding.get("default_threshold"), binding.get("thresholdDirection") or ">=")
    if not expression:
        return None
    return PolicyCandidate(
        rule_scope="BINDING",
        tenant_id=int(binding.get("tenant_id") or 1),
        metric_identifier=normalize_metric_identifier(binding.get("metric_identifier")),
        metric_name=binding.get("metric_name"),
        expression=expression,
        alarm_level=normalize_alarm_level(None, config.get("defaultAlarmLevel", "orange")),
        duration=int(config.get("defaultDuration") or 0),
        notification_methods=str(config.get("defaultNotificationMethods") or ""),
        convert_to_event=1 if config.get("defaultConvertToEvent", True) else 0,
        risk_metric_id=binding.get("risk_metric_id"),
        risk_point_device_id=binding.get("id"),
        source="BINDING_DEFAULT_THRESHOLD",
    )


def candidate_from_product_type_template(binding: dict[str, Any], template: dict[str, Any], config: dict[str, Any]) -> PolicyCandidate | None:
    expression = normalize_expression(template.get("expression"))
    if not expression:
        return None
    return PolicyCandidate(
        rule_scope="PRODUCT_TYPE",
        tenant_id=int(binding.get("tenant_id") or 1),
        metric_identifier=normalize_metric_identifier(template.get("metricIdentifier") or binding.get("metric_identifier")),
        metric_name=template.get("metricName") or binding.get("metric_name"),
        expression=expression,
        alarm_level=normalize_alarm_level(template.get("alarmLevel"), config.get("defaultAlarmLevel", "orange")),
        duration=int(template.get("duration", config.get("defaultDuration") or 0)),
        notification_methods=str(template.get("notificationMethods", config.get("defaultNotificationMethods") or "")),
        convert_to_event=1 if template.get("convertToEvent", config.get("defaultConvertToEvent", True)) else 0,
        product_type=binding.get("product_type"),
        source="PRODUCT_TYPE_TEMPLATE",
    )


def merge_candidate(existing: PolicyCandidate, incoming: PolicyCandidate) -> PolicyCandidate:
    return PolicyCandidate(
        rule_scope=existing.rule_scope,
        tenant_id=existing.tenant_id,
        metric_identifier=existing.metric_identifier,
        metric_name=existing.metric_name,
        expression=existing.expression,
        alarm_level=existing.alarm_level,
        duration=existing.duration,
        notification_methods=existing.notification_methods,
        convert_to_event=existing.convert_to_event,
        risk_metric_id=existing.risk_metric_id,
        product_type=existing.product_type,
        product_id=existing.product_id,
        device_id=existing.device_id,
        risk_point_device_id=existing.risk_point_device_id,
        source=existing.source,
        covered_binding_count=existing.covered_binding_count + incoming.covered_binding_count,
    )


def build_plan(bindings: list[dict[str, Any]], active_rules: list[dict[str, Any]], config: dict[str, Any]) -> dict[str, Any]:
    candidates: dict[str, PolicyCandidate] = {}
    skipped: list[dict[str, Any]] = []
    covered_count = 0
    for binding in bindings:
        binding["product_type"] = resolve_product_type(binding.get("product_key"), binding.get("product_name"))
        if is_binding_covered(binding, active_rules):
            covered_count += 1
            continue
        candidate = candidate_from_binding_default(binding, config)
        if candidate is None:
            template = find_product_type_template(binding, config)
            candidate = candidate_from_product_type_template(binding, template, config) if template else None
        if candidate is None:
            skipped.append({
                "reason": "NO_CONFIRMED_THRESHOLD_TEMPLATE",
                "bindingId": binding.get("id"),
                "deviceId": binding.get("device_id"),
                "productId": binding.get("product_id"),
                "productKey": binding.get("product_key"),
                "productName": binding.get("product_name"),
                "productType": binding.get("product_type"),
                "metricIdentifier": binding.get("metric_identifier"),
                "metricName": binding.get("metric_name"),
            })
            continue
        key = candidate.key()
        candidates[key] = merge_candidate(candidates[key], candidate) if key in candidates else candidate
    return {
        "coveredBindingCount": covered_count,
        "candidateCount": len(candidates),
        "skippedCount": len(skipped),
        "candidates": list(candidates.values()),
        "skipped": skipped,
    }


def build_template_gaps(skipped: list[dict[str, Any]], config: dict[str, Any] | None = None) -> list[dict[str, Any]]:
    gaps: dict[str, dict[str, Any]] = {}
    for item in skipped or []:
        if item.get("reason") != "NO_CONFIRMED_THRESHOLD_TEMPLATE":
            continue
        product_type = item.get("productType") or "UNKNOWN"
        semantic = semantic_match(
            item.get("productKey"),
            item.get("metricIdentifier"),
            item.get("metricName"),
            product_type,
            config,
        )
        metric_identifier = semantic["metricIdentifier"]
        metric_name = semantic["metricName"]
        key = "|".join([product_type, semantic["key"]])
        gap = gaps.setdefault(key, {
            "productType": product_type,
            "metricIdentifier": metric_identifier,
            "metricName": metric_name,
            "semanticTemplateKey": semantic["key"],
            "metricAliases": [],
            "match": {},
            "rawIdentifiers": [],
            "metricNames": [],
            "bindingCount": 0,
            "productCount": 0,
            "sampleProducts": [],
            "suggestedTemplate": {
                "productType": product_type,
                "metricIdentifier": metric_identifier,
                "metricName": metric_name,
                "semanticTemplateKey": semantic["key"],
                "metricAliases": [],
                "match": {},
                "expression": None,
                "alarmLevel": "orange",
                "duration": 0,
                "convertToEvent": True,
                "status": "NEEDS_CONFIRMATION",
            },
            "_productIds": set(),
            "_metricAliases": set(),
            "_rawIdentifiers": set(),
            "_metricNames": set(),
            "_matchProductKeys": set(),
            "_productKeys": set(),
        })
        gap["bindingCount"] += 1
        gap["_metricAliases"].update(semantic.get("metricAliases") or [])
        gap["_rawIdentifiers"].add(normalize_metric_identifier(item.get("metricIdentifier")))
        gap["_rawIdentifiers"].update((semantic.get("match") or {}).get("rawIdentifiers") or [])
        metric_name_value = normalize_metric_name(item.get("metricName"))
        if metric_name_value:
            gap["_metricNames"].add(metric_name_value)
        gap["_metricNames"].update((semantic.get("match") or {}).get("metricNames") or [])
        gap["_matchProductKeys"].update((semantic.get("match") or {}).get("productKeys") or [])
        product_key = normalize_metric_identifier(item.get("productKey"))
        if product_key:
            gap["_productKeys"].add(product_key)
        product_id = item.get("productId")
        if product_id is not None:
            gap["_productIds"].add(product_id)
        if len(gap["sampleProducts"]) < 5:
            sample = {
                "productId": product_id,
                "productKey": item.get("productKey"),
                "productName": item.get("productName"),
            }
            if sample not in gap["sampleProducts"]:
                gap["sampleProducts"].append(sample)
    result = []
    for gap in gaps.values():
        gap["productCount"] = len(gap.pop("_productIds"))
        match: dict[str, list[str]] = {}
        raw_identifiers = sorted(item for item in gap.pop("_rawIdentifiers") if item)
        metric_names = sorted(item for item in gap.pop("_metricNames") if item)
        observed_product_keys = sorted(item for item in gap.pop("_productKeys") if item)
        declared_product_keys = sorted(item for item in gap.pop("_matchProductKeys") if item)
        product_keys = sorted(set(observed_product_keys) | set(declared_product_keys)) if gap["metricIdentifier"] == "value" else declared_product_keys
        if raw_identifiers:
            match["rawIdentifiers"] = raw_identifiers
        if metric_names and gap["metricIdentifier"] == "value":
            match["metricNames"] = metric_names
        if product_keys:
            match["productKeys"] = product_keys
        gap["metricAliases"] = sorted(item for item in gap.pop("_metricAliases") if item)
        gap["rawIdentifiers"] = raw_identifiers
        gap["metricNames"] = metric_names
        gap["match"] = match
        gap["suggestedTemplate"]["metricAliases"] = gap["metricAliases"]
        gap["suggestedTemplate"]["match"] = match
        result.append(gap)
    return sorted(
        result,
        key=lambda value: (-value["bindingCount"], value["productType"], value["metricIdentifier"], value.get("metricName") or ""),
    )


def observed_row_matches_gap(gap: dict[str, Any], row: dict[str, Any]) -> bool:
    match = gap.get("match") if isinstance(gap.get("match"), dict) else {}
    raw_identifiers = set(dedupe_text(match.get("rawIdentifiers") or [gap.get("metricIdentifier")]))
    product_keys = set(dedupe_text(match.get("productKeys") or []))
    identifier = normalize_metric_identifier(row.get("identifier"))
    product_key = normalize_metric_identifier(row.get("productKey"))
    if raw_identifiers and identifier not in raw_identifiers:
        return False
    if product_keys and product_key not in product_keys:
        return False
    return True


def build_observed_value_stats(rows: list[dict[str, Any]]) -> dict[str, Any]:
    numeric_values = [
        numeric
        for numeric in (parse_numeric_value(row.get("propertyValue")) for row in rows)
        if numeric is not None
    ]
    product_keys = sorted({
        normalize_metric_identifier(row.get("productKey"))
        for row in rows
        if normalize_metric_identifier(row.get("productKey"))
    })
    latest_values = [
        str(row.get("reportTime"))
        for row in rows
        if row.get("reportTime") is not None
    ]
    stats: dict[str, Any] = {
        "sampleCount": len(rows),
        "numericCount": len(numeric_values),
        "productCount": len(product_keys),
        "productKeys": product_keys[:10],
        "latestAt": max(latest_values) if latest_values else None,
    }
    if numeric_values:
        stats.update({
            "min": round(min(numeric_values), 6),
            "max": round(max(numeric_values), 6),
            "avg": round(sum(numeric_values) / len(numeric_values), 6),
        })
    else:
        stats.update({"min": None, "max": None, "avg": None})
    return stats


def attach_observed_value_stats(gaps: list[dict[str, Any]], rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    for gap in gaps:
        matched_rows = [row for row in rows if observed_row_matches_gap(gap, row)]
        stats = build_observed_value_stats(matched_rows)
        gap["observedValueStats"] = stats
        gap.setdefault("suggestedTemplate", {})["observedValueStats"] = stats
    return gaps


def fetch_binding_totals(cur) -> dict[str, int]:
    cur.execute(
        """
        SELECT COUNT(*) AS riskBindingTotal,
               SUM(d.id IS NOT NULL AND d.deleted = 0 AND p.id IS NOT NULL AND p.deleted = 0) AS activeDeviceProductBindingCount,
               SUM(d.id IS NULL OR d.deleted <> 0 OR p.id IS NULL OR p.deleted <> 0) AS inactiveOrMissingDeviceProductCount
        FROM risk_point_device rpd
        LEFT JOIN iot_device d ON d.id = rpd.device_id
        LEFT JOIN iot_product p ON p.id = d.product_id
        WHERE rpd.deleted = 0
        """
    )
    row = cur.fetchone() or {}
    return {
        "riskBindingTotal": int(row.get("riskBindingTotal") or 0),
        "activeDeviceProductBindingCount": int(row.get("activeDeviceProductBindingCount") or 0),
        "inactiveOrMissingDeviceProductCount": int(row.get("inactiveOrMissingDeviceProductCount") or 0),
    }


def fetch_bindings(cur) -> list[dict[str, Any]]:
    cur.execute(
        """
        SELECT rpd.id,
               rpd.tenant_id,
               rpd.risk_point_id,
               rpd.device_id,
               rpd.device_code,
               rpd.device_name,
               rpd.risk_metric_id,
               rpd.metric_identifier,
               rpd.metric_name,
               rpd.default_threshold,
               rpd.threshold_unit,
               d.product_id,
               p.product_key,
               p.product_name
        FROM risk_point_device rpd
        JOIN iot_device d ON d.id = rpd.device_id AND d.deleted = 0
        JOIN iot_product p ON p.id = d.product_id AND p.deleted = 0
        WHERE rpd.deleted = 0
        ORDER BY rpd.id
        """
    )
    return list(cur.fetchall())


def fetch_active_rules(cur) -> list[dict[str, Any]]:
    cur.execute(
        """
        SELECT id,
               tenant_id,
               risk_metric_id,
               metric_identifier,
               rule_scope,
               product_type,
               product_id,
               device_id,
               risk_point_device_id
        FROM rule_definition
        WHERE deleted = 0
          AND status = 0
        """
    )
    return list(cur.fetchall())


def fetch_observed_value_rows(cur, gaps: list[dict[str, Any]]) -> list[dict[str, Any]]:
    identifiers = sorted({
        identifier
        for gap in gaps
        for identifier in dedupe_text((gap.get("match") or {}).get("rawIdentifiers") or [gap.get("metricIdentifier")])
    })
    if not identifiers:
        return []
    cur.execute(
        """
        SELECT p.product_key AS productKey,
               prop.identifier,
               prop.property_value AS propertyValue,
               prop.report_time AS reportTime
        FROM iot_device_property prop
        JOIN iot_device d ON d.id = prop.device_id AND d.deleted = 0
        JOIN iot_product p ON p.id = d.product_id AND p.deleted = 0
        WHERE prop.identifier IN %(identifiers)s
        """,
        {"identifiers": identifiers},
    )
    return list(cur.fetchall())


def insert_candidate(cur, candidate: PolicyCandidate, operator_id: int) -> int:
    rule_code = "threshold-backfill-" + datetime.now().strftime("%Y%m%d%H%M%S%f")
    rule_name = f"{candidate.rule_scope} default {candidate.metric_identifier}"
    cur.execute(
        """
        INSERT INTO rule_definition (
            rule_code,
            rule_name,
            metric_identifier,
            condition_expression,
            duration,
            alarm_level,
            convert_to_event,
            status,
            tenant_id,
            metric_name,
            expression,
            notification_methods,
            create_by,
            update_by,
            risk_metric_id,
            rule_scope,
            product_type,
            product_id,
            device_id,
            risk_point_device_id
        ) VALUES (
            %(rule_code)s,
            %(rule_name)s,
            %(metric_identifier)s,
            '',
            %(duration)s,
            %(alarm_level)s,
            %(convert_to_event)s,
            0,
            %(tenant_id)s,
            %(metric_name)s,
            %(expression)s,
            %(notification_methods)s,
            %(operator_id)s,
            %(operator_id)s,
            %(risk_metric_id)s,
            %(rule_scope)s,
            %(product_type)s,
            %(product_id)s,
            %(device_id)s,
            %(risk_point_device_id)s
        )
        """,
        {
            "rule_code": rule_code,
            "rule_name": rule_name[:128],
            "metric_identifier": candidate.metric_identifier,
            "duration": candidate.duration,
            "alarm_level": candidate.alarm_level,
            "convert_to_event": candidate.convert_to_event,
            "tenant_id": candidate.tenant_id,
            "metric_name": candidate.metric_name,
            "expression": candidate.expression,
            "notification_methods": candidate.notification_methods,
            "operator_id": operator_id,
            "risk_metric_id": candidate.risk_metric_id,
            "rule_scope": candidate.rule_scope,
            "product_type": candidate.product_type,
            "product_id": candidate.product_id,
            "device_id": candidate.device_id,
            "risk_point_device_id": candidate.risk_point_device_id,
        },
    )
    return int(cur.lastrowid)


def candidate_to_dict(candidate: PolicyCandidate) -> dict[str, Any]:
    return {
        "ruleScope": candidate.rule_scope,
        "tenantId": candidate.tenant_id,
        "metricIdentifier": candidate.metric_identifier,
        "metricName": candidate.metric_name,
        "expression": candidate.expression,
        "alarmLevel": candidate.alarm_level,
        "duration": candidate.duration,
        "notificationMethods": candidate.notification_methods,
        "convertToEvent": candidate.convert_to_event,
        "riskMetricId": candidate.risk_metric_id,
        "productType": candidate.product_type,
        "productId": candidate.product_id,
        "deviceId": candidate.device_id,
        "riskPointDeviceId": candidate.risk_point_device_id,
        "source": candidate.source,
        "coveredBindingCount": candidate.covered_binding_count,
    }


def candidate_signature(candidate: PolicyCandidate | dict[str, Any]) -> str:
    if isinstance(candidate, PolicyCandidate):
        source = candidate_to_dict(candidate)
    else:
        source = candidate
    fields = [
        source.get("ruleScope"),
        source.get("tenantId"),
        source.get("metricIdentifier"),
        source.get("metricName"),
        source.get("expression"),
        source.get("alarmLevel"),
        source.get("riskMetricId"),
        source.get("productType"),
        source.get("productId"),
        source.get("deviceId"),
        source.get("riskPointDeviceId"),
    ]
    return "|".join("" if value is None else str(value) for value in fields)


def load_confirm_report(path: str | Path) -> dict[str, Any]:
    return json.loads(Path(path).read_text(encoding="utf-8"))


def parse_report_time(value: str | None) -> datetime | None:
    if not value:
        return None
    try:
        return datetime.fromisoformat(value)
    except ValueError:
        return None


def validate_apply_confirmation(args: argparse.Namespace,
                                candidates: list[PolicyCandidate],
                                template_validation: dict[str, Any]) -> dict[str, Any]:
    breaches: list[dict[str, Any]] = []
    if not args.apply:
        return {"required": True, "status": "NOT_APPLICABLE", "breachCount": 0, "breaches": []}
    if args.operator_id <= 0:
        breaches.append({"type": "MISSING_OPERATOR_ID"})
    if args.confirm_candidate_count is None:
        breaches.append({"type": "MISSING_CONFIRM_CANDIDATE_COUNT"})
    elif args.confirm_candidate_count != len(candidates):
        breaches.append({
            "type": "CONFIRM_CANDIDATE_COUNT_MISMATCH",
            "expected": len(candidates),
            "actual": args.confirm_candidate_count,
        })
    ready_template_count = int(template_validation.get("readyTemplateCount") or 0)
    if args.confirm_ready_template_count is None:
        breaches.append({"type": "MISSING_CONFIRM_READY_TEMPLATE_COUNT"})
    elif args.confirm_ready_template_count != ready_template_count:
        breaches.append({
            "type": "CONFIRM_READY_TEMPLATE_COUNT_MISMATCH",
            "expected": ready_template_count,
            "actual": args.confirm_ready_template_count,
        })
    if not candidates:
        breaches.append({"type": "NO_CANDIDATES_TO_APPLY"})
    report = None
    if not str(args.confirm_report or "").strip():
        breaches.append({"type": "MISSING_CONFIRM_REPORT"})
    else:
        try:
            report = load_confirm_report(args.confirm_report)
        except OSError as error:
            breaches.append({"type": "CONFIRM_REPORT_UNREADABLE", "message": str(error)})
        except json.JSONDecodeError as error:
            breaches.append({"type": "CONFIRM_REPORT_INVALID_JSON", "message": str(error)})
    if report:
        if report.get("mode") != "dry-run":
            breaches.append({"type": "CONFIRM_REPORT_NOT_DRY_RUN", "mode": report.get("mode")})
        if int(report.get("candidateCount") or 0) != len(candidates):
            breaches.append({
                "type": "CONFIRM_REPORT_CANDIDATE_COUNT_MISMATCH",
                "expected": len(candidates),
                "actual": report.get("candidateCount"),
            })
        report_ready_count = int((report.get("templateValidation") or {}).get("readyTemplateCount") or 0)
        if report_ready_count != ready_template_count:
            breaches.append({
                "type": "CONFIRM_REPORT_READY_TEMPLATE_COUNT_MISMATCH",
                "expected": ready_template_count,
                "actual": report_ready_count,
            })
        checked_at = parse_report_time(report.get("checkedAt"))
        max_age_hours = max(args.max_confirm_age_hours or 24, 1)
        if checked_at is None:
            breaches.append({"type": "CONFIRM_REPORT_CHECKED_AT_INVALID"})
        else:
            age_seconds = (datetime.now() - checked_at).total_seconds()
            if age_seconds < 0 or age_seconds > max_age_hours * 3600:
                breaches.append({
                    "type": "CONFIRM_REPORT_EXPIRED",
                    "maxAgeHours": max_age_hours,
                    "ageSeconds": int(age_seconds),
                })
        current_signatures = sorted(candidate_signature(candidate) for candidate in candidates)
        report_signatures = sorted(candidate_signature(candidate) for candidate in report.get("candidates") or [])
        if report_signatures != current_signatures:
            breaches.append({"type": "CONFIRM_REPORT_CANDIDATES_DRIFTED"})
    return {
        "required": True,
        "status": "PASSED" if not breaches else "FAILED",
        "breachCount": len(breaches),
        "breaches": breaches,
        "confirmReport": str(args.confirm_report or ""),
        "confirmCandidateCount": args.confirm_candidate_count,
        "confirmReadyTemplateCount": args.confirm_ready_template_count,
    }


def render_markdown(report: dict[str, Any]) -> str:
    lines = [
        "# Threshold Policy Default Backfill",
        "",
        f"- Mode: `{report['mode']}`",
        f"- Status: `{report['status']}`",
        f"- Checked At: `{report['checkedAt']}`",
        f"- Risk Binding Total: `{report.get('riskBindingTotal', report['bindingTotal'])}`",
        f"- Active Device/Product Bindings: `{report.get('activeDeviceProductBindingCount', report['bindingTotal'])}`",
        f"- Inactive Or Missing Device/Product Bindings: `{report.get('inactiveOrMissingDeviceProductCount', 0)}`",
        f"- Already Covered Bindings: `{report['coveredBindingCount']}`",
        f"- Planned Candidates: `{report['candidateCount']}`",
        f"- Applied Candidates: `{report['appliedCount']}`",
        f"- Skipped Bindings: `{report['skippedCount']}`",
        f"- Template Gaps: `{report.get('templateGapCount', 0)}`",
        f"- Ready Templates: `{report.get('templateValidation', {}).get('readyTemplateCount', 0)}`",
        f"- Template Breaches: `{report.get('templateValidation', {}).get('breachCount', 0)}`",
        f"- Apply Confirmation: `{report.get('applyConfirmation', {}).get('status', 'NOT_APPLICABLE')}`",
        "",
        "## Candidates",
        "| Scope | Identity | Metric | Expression | Source | Covers |",
        "| --- | --- | --- | --- | --- | --- |",
    ]
    for item in report["candidates"][:50]:
        identity = item.get("productType") or item.get("productId") or item.get("deviceId") or item.get("riskPointDeviceId") or "--"
        lines.append(
            f"| {item['ruleScope']} | {identity} | {item['metricIdentifier']} | {item['expression']} | "
            f"{item['source']} | {item['coveredBindingCount']} |"
        )
    if not report["candidates"]:
        lines.append("| -- | -- | -- | -- | -- | 0 |")
    lines.extend([
        "",
        "## Skipped Summary",
        "| Reason | Count |",
        "| --- | --- |",
    ])
    reason_counts = report.get("skippedReasonCounts") or {}
    for reason, count in sorted(reason_counts.items()):
        lines.append(f"| {reason} | {count} |")
    if not reason_counts:
        lines.append("| -- | 0 |")
    lines.extend([
        "",
        "## Template Gaps",
        "| Product Type | Metric | Metric Name | Bindings | Products | Suggested Expression |",
        "| --- | --- | --- | --- | --- | --- |",
    ])
    for item in report.get("templateGaps", [])[:80]:
        suggested_expression = item.get("suggestedTemplate", {}).get("expression") or "NEEDS_CONFIRMATION"
        lines.append(
            f"| {item.get('productType') or '--'} | {item.get('metricIdentifier') or '--'} | "
            f"{item.get('metricName') or '--'} | {item.get('bindingCount') or 0} | "
            f"{item.get('productCount') or 0} | {suggested_expression} |"
        )
    if not report.get("templateGaps"):
        lines.append("| -- | -- | -- | 0 | 0 | -- |")
    lines.extend([
        "",
        "## Template Validation",
        "| Breach | Detail |",
        "| --- | --- |",
    ])
    breaches = report.get("templateValidation", {}).get("breaches") or []
    for item in breaches[:80]:
        detail = ", ".join(f"{key}={value}" for key, value in item.items() if key != "type")
        lines.append(f"| {item.get('type')} | {detail or '--'} |")
    if not breaches:
        lines.append("| -- | 0 |")
    lines.extend([
        "",
        "## Apply Confirmation",
        "| Breach | Detail |",
        "| --- | --- |",
    ])
    apply_breaches = report.get("applyConfirmation", {}).get("breaches") or []
    for item in apply_breaches[:80]:
        detail = ", ".join(f"{key}={value}" for key, value in item.items() if key != "type")
        lines.append(f"| {item.get('type')} | {detail or '--'} |")
    if not apply_breaches:
        lines.append("| -- | 0 |")
    lines.append("")
    return "\n".join(lines)


def build_template_draft(report: dict[str, Any]) -> dict[str, Any]:
    return {
        "description": "Draft threshold policy templates generated from real-environment dry-run gaps. Fill expression values before using this as --config-path for apply.",
        "generatedAt": report["checkedAt"],
        "sourceReport": report.get("reportPath"),
        "defaultDuration": 0,
        "defaultAlarmLevel": "orange",
        "defaultNotificationMethods": "",
        "defaultConvertToEvent": True,
        "productTypeTemplates": [
            {
                **(item.get("suggestedTemplate") or {}),
                "confirmationStatus": "NEEDS_CONFIRMATION",
                "evidence": {
                    "bindingCount": item.get("bindingCount") or 0,
                    "productCount": item.get("productCount") or 0,
                    "sampleProducts": item.get("sampleProducts") or [],
                },
            }
            for item in report.get("templateGaps", [])
        ],
    }


def render_template_draft_markdown(template_draft: dict[str, Any]) -> str:
    lines = [
        "# Threshold Policy Template Draft",
        "",
        f"- Generated At: `{template_draft.get('generatedAt')}`",
        f"- Template Count: `{len(template_draft.get('productTypeTemplates') or [])}`",
        "",
        "## Templates",
        "| Product Type | Metric | Metric Name | Expression | Observed Range | Bindings | Products | Status |",
        "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for item in template_draft.get("productTypeTemplates") or []:
        evidence = item.get("evidence") or {}
        stats = item.get("observedValueStats") or {}
        observed_range = "--"
        if stats:
            observed_range = f"{stats.get('min')}..{stats.get('max')} (n={stats.get('numericCount') or 0})"
        lines.append(
            f"| {item.get('productType') or '--'} | {item.get('metricIdentifier') or '--'} | "
            f"{item.get('metricName') or '--'} | {item.get('expression') or 'NEEDS_CONFIRMATION'} | "
            f"{observed_range} | {evidence.get('bindingCount') or 0} | {evidence.get('productCount') or 0} | "
            f"{item.get('confirmationStatus') or '--'} |"
        )
    if not template_draft.get("productTypeTemplates"):
        lines.append("| -- | -- | -- | -- | -- | 0 | 0 | -- |")
    lines.append("")
    return "\n".join(lines)


def render_template_confirmation_package_markdown(template_draft: dict[str, Any],
                                                  report: dict[str, Any],
                                                  output_dir: str | Path) -> str:
    target_dir = Path(output_dir)
    template_count = len(template_draft.get("productTypeTemplates") or [])
    min_ready = template_count
    lines = [
        "# Threshold Policy Template Confirmation Package",
        "",
        f"- Generated At: `{template_draft.get('generatedAt')}`",
        f"- Source Report: `{template_draft.get('sourceReport') or '--'}`",
        f"- Template Count: `{template_count}`",
        f"- Binding Total: `{report.get('bindingTotal')}`",
        f"- Risk Binding Total: `{report.get('riskBindingTotal')}`",
        f"- Candidate Count Before Confirmation: `{report.get('candidateCount')}`",
        f"- Skipped Count Before Confirmation: `{report.get('skippedCount')}`",
        "",
        "## Files",
        f"- Fillable CSV: `{target_dir / 'threshold-policy-template-confirmation-latest.csv'}`",
        f"- Merge Report: `{target_dir / 'threshold-policy-template-confirmation-merge-latest.md'}`",
        f"- Pending Config: `config/automation/threshold-policy-defaults.pending.json`",
        f"- Confirmed Config Target: `config/automation/threshold-policy-defaults.confirmed.json`",
        "",
        "## Workflow",
        "1. Fill `expression` for every row in the CSV.",
        "2. Change every accepted row `confirmationStatus` to `CONFIRMED`.",
        "3. Merge the CSV into a confirmed config:",
        "",
        "```powershell",
        "python scripts\\backfill-threshold-policy-defaults.py "
        "--merge-template-confirmation-csv=logs\\acceptance\\threshold-policy-template-confirmation-latest.csv "
        "--config-path=config\\automation\\threshold-policy-defaults.pending.json "
        "--merge-template-output-path=config\\automation\\threshold-policy-defaults.confirmed.json",
        "```",
        "",
        "4. Open the merge report. If `STATUS=FAILED`, the command will not write the confirmed config. "
        "Continue only after the report shows `targetWritten=true`.",
        "",
        "5. Validate the confirmed config:",
        "",
        "```powershell",
        "python scripts\\backfill-threshold-policy-defaults.py "
        "--validate-config-only "
        "--config-path=config\\automation\\threshold-policy-defaults.confirmed.json "
        f"--min-ready-template-count={min_ready} "
        "--fail-on-template-breaches",
        "```",
        "",
        "6. Run a dry-run with the confirmed config before any apply:",
        "",
        "```powershell",
        "python scripts\\backfill-threshold-policy-defaults.py "
        "--config-path=config\\automation\\threshold-policy-defaults.confirmed.json",
        "```",
        "",
        "7. Apply only with the latest dry-run report path and matching candidate/template counts.",
        "",
        "## Templates To Confirm",
        "| Index | Semantic Key | Product Type | Metric | Metric Name | Observed Range | Bindings | Products | Required Actions |",
        "| --- | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for index, template in enumerate(template_draft.get("productTypeTemplates") or []):
        row = template_confirmation_row(index, template)
        actions = str(row.get("requiredActions") or "").replace(";", ",") or "--"
        lines.append(
            f"| {row.get('index')} | {row.get('semanticTemplateKey') or '--'} | "
            f"{row.get('productType') or '--'} | {row.get('metricIdentifier') or '--'} | "
            f"{row.get('metricName') or '--'} | {row.get('observedRange') or '--'} | "
            f"{row.get('bindingCount') or 0} | {row.get('productCount') or 0} | {actions} |"
        )
    if not template_draft.get("productTypeTemplates"):
        lines.append("| -- | -- | -- | -- | -- | -- | 0 | 0 | -- |")
    return "\n".join(lines) + "\n"


def join_csv_values(values: Iterable[Any]) -> str:
    return ";".join(dedupe_text(values))


def template_confirmation_row(index: int, template: dict[str, Any]) -> dict[str, Any]:
    match = template.get("match") if isinstance(template.get("match"), dict) else {}
    evidence = template.get("evidence") if isinstance(template.get("evidence"), dict) else {}
    checklist_item = build_template_confirmation_item(index, template)
    return {
        "index": index,
        "semanticTemplateKey": template.get("semanticTemplateKey") or "",
        "productType": template.get("productType") or "",
        "metricIdentifier": template.get("metricIdentifier") or "",
        "metricName": template.get("metricName") or "",
        "observedRange": format_observed_range(template.get("observedValueStats")),
        "bindingCount": evidence.get("bindingCount") or 0,
        "productCount": evidence.get("productCount") or 0,
        "productKeys": join_csv_values(match.get("productKeys") or []),
        "rawIdentifiers": join_csv_values(match.get("rawIdentifiers") or []),
        "metricAliases": join_csv_values(template.get("metricAliases") or []),
        "expression": template.get("expression") or "",
        "confirmationStatus": template.get("confirmationStatus") or normalize_confirmation_status(template),
        "requiredActions": join_csv_values(checklist_item.get("requiredActions") or []),
    }


def write_template_confirmation_csv(template_draft: dict[str, Any], target_path: str | Path) -> Path:
    output_path = Path(target_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=TEMPLATE_CONFIRMATION_CSV_FIELDS)
        writer.writeheader()
        for index, template in enumerate(template_draft.get("productTypeTemplates") or []):
            writer.writerow(template_confirmation_row(index, template))
    return output_path


def template_confirmation_identity(template: dict[str, Any]) -> str:
    semantic_key = normalize_metric_identifier(template.get("semanticTemplateKey"))
    if semantic_key:
        return f"semantic:{semantic_key}"
    return "|".join([
        "metric",
        str(template.get("productType", "")).strip().upper(),
        normalize_metric_identifier(template.get("metricIdentifier")),
        normalize_metric_name(template.get("metricName")),
    ])


def merge_template_confirmation_csv(config: dict[str, Any], csv_path: str | Path) -> dict[str, Any]:
    merged, _summary = merge_template_confirmation_csv_with_summary(config, csv_path)
    return merged


def merge_template_confirmation_csv_with_summary(config: dict[str, Any],
                                                 csv_path: str | Path) -> tuple[dict[str, Any], dict[str, Any]]:
    merged = copy.deepcopy(config)
    templates = merged.get("productTypeTemplates") or []
    by_identity = {template_confirmation_identity(template): template for template in templates}
    allowed_confirmation_statuses = {"CONFIRMED", "NEEDS_CONFIRMATION"}
    seen_identities: dict[str, int] = {}
    summary: dict[str, Any] = {
        "csvPath": str(csv_path),
        "csvRowCount": 0,
        "matchedRowCount": 0,
        "unmatchedRowCount": 0,
        "missingHeaderCount": 0,
        "missingHeaders": [],
        "duplicateRowCount": 0,
        "invalidConfirmationStatusCount": 0,
        "invalidExpressionCount": 0,
        "updatedExpressionCount": 0,
        "updatedConfirmationStatusCount": 0,
        "unmatchedRows": [],
        "duplicateRows": [],
        "invalidConfirmationStatusRows": [],
        "invalidExpressionRows": [],
        "matchedRows": [],
    }
    with Path(csv_path).open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        actual_headers = set(reader.fieldnames or [])
        missing_headers = [
            field for field in REQUIRED_TEMPLATE_CONFIRMATION_CSV_FIELDS
            if field not in actual_headers
        ]
        if missing_headers:
            summary["missingHeaders"] = missing_headers
            summary["missingHeaderCount"] = len(missing_headers)
            return merged, summary
        for row in reader:
            summary["csvRowCount"] += 1
            identity = template_confirmation_identity(row)
            if identity in seen_identities:
                summary["duplicateRowCount"] += 1
                summary["duplicateRows"].append({
                    "row": summary["csvRowCount"],
                    "firstRow": seen_identities[identity],
                    "identity": identity,
                    "semanticTemplateKey": row.get("semanticTemplateKey") or "",
                    "productType": row.get("productType") or "",
                    "metricIdentifier": row.get("metricIdentifier") or "",
                    "metricName": row.get("metricName") or "",
                })
                continue
            seen_identities[identity] = summary["csvRowCount"]
            template = by_identity.get(identity)
            if template is None:
                summary["unmatchedRowCount"] += 1
                summary["unmatchedRows"].append({
                    "row": summary["csvRowCount"],
                    "identity": identity,
                    "semanticTemplateKey": row.get("semanticTemplateKey") or "",
                    "productType": row.get("productType") or "",
                    "metricIdentifier": row.get("metricIdentifier") or "",
                    "metricName": row.get("metricName") or "",
                })
                continue
            confirmation_status = str(row.get("confirmationStatus") or "").strip().upper()
            if confirmation_status and confirmation_status not in allowed_confirmation_statuses:
                summary["invalidConfirmationStatusCount"] += 1
                summary["invalidConfirmationStatusRows"].append({
                    "row": summary["csvRowCount"],
                    "identity": identity,
                    "confirmationStatus": confirmation_status,
                    "semanticTemplateKey": row.get("semanticTemplateKey") or "",
                    "productType": row.get("productType") or "",
                    "metricIdentifier": row.get("metricIdentifier") or "",
                    "metricName": row.get("metricName") or "",
                })
                continue
            summary["matchedRowCount"] += 1
            expression = str(row.get("expression") or "").strip()
            if confirmation_status == "CONFIRMED" and not is_executable_expression(expression):
                summary["invalidExpressionCount"] += 1
                summary["invalidExpressionRows"].append({
                    "row": summary["csvRowCount"],
                    "identity": identity,
                    "reason": "MISSING_EXPRESSION" if not expression else "UNSUPPORTED_EXPRESSION",
                    "expression": expression,
                    "semanticTemplateKey": row.get("semanticTemplateKey") or "",
                    "productType": row.get("productType") or "",
                    "metricIdentifier": row.get("metricIdentifier") or "",
                    "metricName": row.get("metricName") or "",
                })
            matched_row = {
                "row": summary["csvRowCount"],
                "identity": identity,
                "semanticTemplateKey": row.get("semanticTemplateKey") or template.get("semanticTemplateKey") or "",
                "productType": row.get("productType") or template.get("productType") or "",
                "metricIdentifier": row.get("metricIdentifier") or template.get("metricIdentifier") or "",
                "metricName": row.get("metricName") or template.get("metricName") or "",
            }
            if expression and expression != (template.get("expression") or ""):
                template["expression"] = expression
                summary["updatedExpressionCount"] += 1
                matched_row["expressionUpdated"] = True
            else:
                matched_row["expressionUpdated"] = False
            if confirmation_status and confirmation_status != (template.get("confirmationStatus") or ""):
                template["confirmationStatus"] = confirmation_status
                summary["updatedConfirmationStatusCount"] += 1
                matched_row["confirmationStatusUpdated"] = True
            else:
                matched_row["confirmationStatusUpdated"] = False
            summary["matchedRows"].append(matched_row)
    merged["productTypeTemplates"] = templates
    return merged, summary


def render_template_confirmation_merge_markdown(report: dict[str, Any]) -> str:
    lines = [
        "# Threshold Policy Template Confirmation Merge",
        "",
        f"- Status: `{report['status']}`",
        f"- Checked At: `{report['checkedAt']}`",
        f"- Config Path: `{report['configPath']}`",
        f"- CSV Path: `{report['csvPath']}`",
        f"- Output Path: `{report['outputPath']}`",
        f"- Target Written: `{report.get('targetWritten')}`",
        f"- CSV Rows: `{report['csvRowCount']}`",
        f"- Matched Rows: `{report['matchedRowCount']}`",
        f"- Unmatched Rows: `{report['unmatchedRowCount']}`",
        f"- Missing Required Headers: `{report['missingHeaderCount']}`",
        f"- Duplicate Rows: `{report['duplicateRowCount']}`",
        f"- Invalid Confirmation Status Rows: `{report['invalidConfirmationStatusCount']}`",
        f"- Invalid Expression Rows: `{report['invalidExpressionCount']}`",
        f"- Updated Expressions: `{report['updatedExpressionCount']}`",
        f"- Updated Confirmation Statuses: `{report['updatedConfirmationStatusCount']}`",
        f"- Ready Templates After Merge: `{report['templateValidation']['readyTemplateCount']}`",
        f"- Template Breaches After Merge: `{report['templateValidation']['breachCount']}`",
        "",
        "## Unmatched Rows",
        "| Row | Identity | Metric | Name |",
        "| --- | --- | --- | --- |",
    ]
    for item in report.get("unmatchedRows") or []:
        lines.append(
            f"| {item.get('row')} | {item.get('identity')} | "
            f"{item.get('metricIdentifier') or '--'} | {item.get('metricName') or '--'} |"
        )
    if not report.get("unmatchedRows"):
        lines.append("| -- | -- | -- | -- |")
    lines.extend([
        "",
        "## Missing Required Headers",
        "| Header |",
        "| --- |",
    ])
    for header in report.get("missingHeaders") or []:
        lines.append(f"| {header} |")
    if not report.get("missingHeaders"):
        lines.append("| -- |")
    lines.extend([
        "",
        "## Duplicate Rows",
        "| Row | First Row | Identity | Metric | Name |",
        "| --- | --- | --- | --- | --- |",
    ])
    for item in report.get("duplicateRows") or []:
        lines.append(
            f"| {item.get('row')} | {item.get('firstRow')} | {item.get('identity')} | "
            f"{item.get('metricIdentifier') or '--'} | {item.get('metricName') or '--'} |"
        )
    if not report.get("duplicateRows"):
        lines.append("| -- | -- | -- | -- | -- |")
    lines.extend([
        "",
        "## Invalid Confirmation Status Rows",
        "| Row | Identity | Status | Metric | Name |",
        "| --- | --- | --- | --- | --- |",
    ])
    for item in report.get("invalidConfirmationStatusRows") or []:
        lines.append(
            f"| {item.get('row')} | {item.get('identity')} | "
            f"{item.get('confirmationStatus') or '--'} | {item.get('metricIdentifier') or '--'} | "
            f"{item.get('metricName') or '--'} |"
        )
    if not report.get("invalidConfirmationStatusRows"):
        lines.append("| -- | -- | -- | -- | -- |")
    lines.extend([
        "",
        "## Invalid Expression Rows",
        "| Row | Identity | Reason | Expression | Metric | Name |",
        "| --- | --- | --- | --- | --- | --- |",
    ])
    for item in report.get("invalidExpressionRows") or []:
        lines.append(
            f"| {item.get('row')} | {item.get('identity')} | {item.get('reason') or '--'} | "
            f"{item.get('expression') or '--'} | {item.get('metricIdentifier') or '--'} | "
            f"{item.get('metricName') or '--'} |"
        )
    if not report.get("invalidExpressionRows"):
        lines.append("| -- | -- | -- | -- | -- | -- |")
    lines.extend([
        "",
        "## Matched Rows",
        "| Row | Identity | Metric | Expression Updated | Status Updated |",
        "| --- | --- | --- | --- | --- |",
    ])
    for item in report.get("matchedRows") or []:
        lines.append(
            f"| {item.get('row')} | {item.get('identity')} | "
            f"{item.get('metricIdentifier') or '--'} | {item.get('expressionUpdated')} | "
            f"{item.get('confirmationStatusUpdated')} |"
        )
    if not report.get("matchedRows"):
        lines.append("| -- | -- | -- | -- | -- |")
    return "\n".join(lines) + "\n"


def write_template_confirmation_merge_report(report: dict[str, Any], output_dir: str | Path) -> tuple[Path, Path]:
    target_dir = Path(output_dir)
    target_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    json_path = target_dir / f"threshold-policy-template-confirmation-merge-{timestamp}.json"
    md_path = target_dir / f"threshold-policy-template-confirmation-merge-{timestamp}.md"
    json_content = json.dumps(report, ensure_ascii=False, indent=2)
    markdown_content = render_template_confirmation_merge_markdown(report)
    json_path.write_text(json_content, encoding="utf-8")
    md_path.write_text(markdown_content, encoding="utf-8")
    (target_dir / "threshold-policy-template-confirmation-merge-latest.json").write_text(
        json_content,
        encoding="utf-8",
    )
    (target_dir / "threshold-policy-template-confirmation-merge-latest.md").write_text(
        markdown_content,
        encoding="utf-8",
    )
    return json_path, md_path


def write_merged_template_confirmation_config(config_path: str | Path,
                                              csv_path: str | Path,
                                              output_path: str | Path | None = None,
                                              output_dir: str | Path | None = None) -> Path:
    config = load_config(config_path)
    merged, summary = merge_template_confirmation_csv_with_summary(config, csv_path)
    target = Path(output_path) if output_path else Path(config_path)
    validation = validate_template_config(merged)
    if (
        validation["breachCount"] > 0
        or summary["duplicateRowCount"] > 0
        or summary["missingHeaderCount"] > 0
        or summary["invalidConfirmationStatusCount"] > 0
        or summary["invalidExpressionCount"] > 0
    ):
        status = "FAILED"
    elif summary["unmatchedRowCount"] > 0:
        status = "WARNING"
    else:
        status = "PASSED"
    target_written = status != "FAILED"
    if target_written:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(merged, ensure_ascii=False, indent=2), encoding="utf-8")
    report = {
        "checkedAt": datetime.now().isoformat(timespec="seconds"),
        "status": status,
        "configPath": str(config_path),
        "csvPath": str(csv_path),
        "outputPath": str(target),
        "targetWritten": target_written,
        "templateValidation": validation,
        **summary,
    }
    write_template_confirmation_merge_report(report, output_dir or target.parent)
    return target


def render_template_validation_markdown(report: dict[str, Any]) -> str:
    validation = report["templateValidation"]
    lines = [
        "# Threshold Policy Template Validation",
        "",
        f"- Status: `{report['status']}`",
        f"- Checked At: `{report['checkedAt']}`",
        f"- Config Path: `{report['configPath']}`",
        f"- Template Count: `{validation['templateCount']}`",
        f"- Confirmed Templates: `{validation['confirmedTemplateCount']}`",
        f"- Ready Templates: `{validation['readyTemplateCount']}`",
        f"- Breach Count: `{validation['breachCount']}`",
        f"- Min Ready Template Count: `{report['minReadyTemplateCount']}`",
        "",
        "## Breaches",
        "| Breach | Detail |",
        "| --- | --- |",
    ]
    for item in validation.get("breaches") or []:
        detail = ", ".join(f"{key}={value}" for key, value in item.items() if key != "type")
        lines.append(f"| {item.get('type')} | {detail or '--'} |")
    if not validation.get("breaches"):
        lines.append("| -- | 0 |")
    lines.extend([
        "",
        "## Manual Confirmation Checklist",
        "| Index | Product Type | Metric | Metric Name | Observed Range | Bindings | Required Actions |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ])
    for item in validation.get("confirmationChecklist") or []:
        actions = ",".join(item.get("requiredActions") or []) or "READY"
        lines.append(
            f"| {item.get('index')} | {item.get('productType') or '--'} | "
            f"{item.get('metricIdentifier') or '--'} | {item.get('metricName') or '--'} | "
            f"{item.get('observedRange') or '--'} | {item.get('bindingCount') or 0} | {actions} |"
        )
    if not validation.get("confirmationChecklist"):
        lines.append("| -- | -- | -- | -- | -- | 0 | -- |")
    lines.append("")
    return "\n".join(lines)


def build_template_validation_report(args: argparse.Namespace) -> dict[str, Any]:
    validation = validate_template_config(load_config(args.config_path))
    min_ready = max(args.min_ready_template_count or 0, 0)
    ready_breach = validation["readyTemplateCount"] < min_ready
    status = "FAILED" if validation["breachCount"] > 0 or ready_breach else "PASSED"
    breaches = list(validation["breaches"])
    if ready_breach:
        breaches.append({
            "type": "READY_TEMPLATE_COUNT_BELOW_MINIMUM",
            "expected": min_ready,
            "actual": validation["readyTemplateCount"],
        })
    validation = {**validation, "breachCount": len(breaches), "breaches": breaches}
    return {
        "checkedAt": datetime.now().isoformat(timespec="seconds"),
        "status": status,
        "configPath": str(args.config_path),
        "minReadyTemplateCount": min_ready,
        "templateValidation": validation,
    }


def write_template_validation_report(report: dict[str, Any], output_dir: str | Path) -> tuple[Path, Path]:
    target_dir = Path(output_dir)
    target_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    json_path = target_dir / f"threshold-policy-template-validation-{timestamp}.json"
    md_path = target_dir / f"threshold-policy-template-validation-{timestamp}.md"
    json_content = json.dumps(report, ensure_ascii=False, indent=2)
    markdown_content = render_template_validation_markdown(report)
    json_path.write_text(json_content, encoding="utf-8")
    md_path.write_text(markdown_content, encoding="utf-8")
    (target_dir / "threshold-policy-template-validation-latest.json").write_text(json_content, encoding="utf-8")
    (target_dir / "threshold-policy-template-validation-latest.md").write_text(markdown_content, encoding="utf-8")
    return json_path, md_path


def write_exported_template_config(report: dict[str, Any], target_path: str | Path) -> Path:
    output_path = Path(target_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    template_draft = build_template_draft(report)
    output_path.write_text(json.dumps(template_draft, ensure_ascii=False, indent=2), encoding="utf-8")
    return output_path


def write_reports(report: dict[str, Any], output_dir: str | Path) -> tuple[Path, Path]:
    target_dir = Path(output_dir)
    target_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    json_path = target_dir / f"threshold-policy-backfill-{timestamp}.json"
    md_path = target_dir / f"threshold-policy-backfill-{timestamp}.md"
    draft_json_path = target_dir / f"threshold-policy-template-draft-{timestamp}.json"
    draft_md_path = target_dir / f"threshold-policy-template-draft-{timestamp}.md"
    confirmation_csv_path = target_dir / f"threshold-policy-template-confirmation-{timestamp}.csv"
    confirmation_package_path = target_dir / f"threshold-policy-template-confirmation-package-{timestamp}.md"
    report["templateDraft"] = {
        "jsonPath": str(draft_json_path),
        "markdownPath": str(draft_md_path),
        "latestJsonPath": str(target_dir / "threshold-policy-template-draft-latest.json"),
        "latestMarkdownPath": str(target_dir / "threshold-policy-template-draft-latest.md"),
        "confirmationCsvPath": str(confirmation_csv_path),
        "latestConfirmationCsvPath": str(target_dir / "threshold-policy-template-confirmation-latest.csv"),
        "confirmationPackagePath": str(confirmation_package_path),
        "latestConfirmationPackagePath": str(
            target_dir / "threshold-policy-template-confirmation-package-latest.md"
        ),
    }
    json_content = json.dumps(report, ensure_ascii=False, indent=2)
    markdown_content = render_markdown(report)
    json_path.write_text(json_content, encoding="utf-8")
    md_path.write_text(markdown_content, encoding="utf-8")
    (target_dir / "threshold-policy-backfill-latest.json").write_text(json_content, encoding="utf-8")
    (target_dir / "threshold-policy-backfill-latest.md").write_text(markdown_content, encoding="utf-8")
    template_draft = build_template_draft({**report, "reportPath": str(json_path)})
    template_draft_json = json.dumps(template_draft, ensure_ascii=False, indent=2)
    template_draft_md = render_template_draft_markdown(template_draft)
    confirmation_package_md = render_template_confirmation_package_markdown(template_draft, report, target_dir)
    draft_json_path.write_text(template_draft_json, encoding="utf-8")
    draft_md_path.write_text(template_draft_md, encoding="utf-8")
    confirmation_package_path.write_text(confirmation_package_md, encoding="utf-8")
    write_template_confirmation_csv(template_draft, confirmation_csv_path)
    write_template_confirmation_csv(template_draft, target_dir / "threshold-policy-template-confirmation-latest.csv")
    (target_dir / "threshold-policy-template-draft-latest.json").write_text(template_draft_json, encoding="utf-8")
    (target_dir / "threshold-policy-template-draft-latest.md").write_text(template_draft_md, encoding="utf-8")
    (target_dir / "threshold-policy-template-confirmation-package-latest.md").write_text(
        confirmation_package_md,
        encoding="utf-8",
    )
    return json_path, md_path


def build_report(args: argparse.Namespace) -> dict[str, Any]:
    config = load_config(args.config_path)
    with connect(args) as conn:
        try:
            with conn.cursor() as cur:
                binding_totals = fetch_binding_totals(cur)
                bindings = fetch_bindings(cur)
                active_rules = fetch_active_rules(cur)
                template_validation = validate_template_config(config)
                plan = build_plan(bindings, active_rules, config)
                candidates = plan["candidates"]
                apply_confirmation = validate_apply_confirmation(args, candidates, template_validation)
                template_gaps = build_template_gaps(plan["skipped"], config)
                observed_value_rows = fetch_observed_value_rows(cur, template_gaps)
                attach_observed_value_stats(template_gaps, observed_value_rows)
                skipped_reason_counts: dict[str, int] = {}
                for item in plan["skipped"]:
                    skipped_reason_counts[item["reason"]] = skipped_reason_counts.get(item["reason"], 0) + 1
                applied_ids: list[int] = []
                if args.apply:
                    if template_validation["breachCount"] > 0:
                        raise RuntimeError("Template validation failed; refusing to apply threshold policies.")
                    if apply_confirmation["breachCount"] > 0:
                        raise RuntimeError("Apply confirmation failed; refusing to apply threshold policies.")
                    selected = candidates[: args.limit] if args.limit and args.limit > 0 else candidates
                    for candidate in selected:
                        applied_ids.append(insert_candidate(cur, candidate, args.operator_id))
                    conn.commit()
                else:
                    conn.rollback()
        except Exception:
            conn.rollback()
            raise
    report = {
        "checkedAt": datetime.now().isoformat(timespec="seconds"),
        "mode": "apply" if args.apply else "dry-run",
        "status": "PASSED",
        "database": {"host": args.host, "port": args.port, "db": args.db, "user": args.user},
        "configPath": str(args.config_path),
        "bindingTotal": len(bindings),
        "riskBindingTotal": binding_totals["riskBindingTotal"],
        "activeDeviceProductBindingCount": binding_totals["activeDeviceProductBindingCount"],
        "inactiveOrMissingDeviceProductCount": binding_totals["inactiveOrMissingDeviceProductCount"],
        "coveredBindingCount": plan["coveredBindingCount"],
        "candidateCount": len(candidates),
        "appliedCount": len(applied_ids),
        "appliedRuleIds": applied_ids,
        "skippedCount": plan["skippedCount"],
        "skippedReasonCounts": skipped_reason_counts,
        "templateGapCount": len(template_gaps),
        "templateGaps": template_gaps,
        "templateValidation": template_validation,
        "applyConfirmation": apply_confirmation,
        "candidates": [candidate_to_dict(candidate) for candidate in candidates],
        "skipped": plan["skipped"][:200],
    }
    return report


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv)
    if str(args.merge_template_confirmation_csv or "").strip():
        output_path = write_merged_template_confirmation_config(
            args.config_path,
            args.merge_template_confirmation_csv,
            args.merge_template_output_path or args.config_path,
            args.output_dir,
        )
        merge_report_path = Path(args.output_dir) / "threshold-policy-template-confirmation-merge-latest.json"
        merge_report = json.loads(merge_report_path.read_text(encoding="utf-8"))
        target_written = bool(merge_report.get("targetWritten"))
        if target_written:
            print(f"TEMPLATE_CONFIG_PATH={output_path}")
        else:
            print(f"TEMPLATE_CONFIG_PATH_NOT_WRITTEN={output_path}")
        print(f"MERGE_REPORT_JSON_PATH={merge_report_path}")
        print(f"MERGE_REPORT_MD_PATH={Path(args.output_dir) / 'threshold-policy-template-confirmation-merge-latest.md'}")
        print(f"STATUS={merge_report['status']}")
        print(f"TARGET_WRITTEN={str(target_written).lower()}")
        return 1 if merge_report["status"] == "FAILED" else 0
    if args.validate_config_only:
        report = build_template_validation_report(args)
        json_path, md_path = write_template_validation_report(report, args.output_dir)
        print(f"[report] {json_path}")
        print(f"[report] {md_path}")
        print(f"JSON_PATH={json_path}")
        print(f"MD_PATH={md_path}")
        print(f"STATUS={report['status']}")
        print(
            "SUMMARY="
            f"threshold policy template validation {report['status'].lower()}, "
            f"ready={report['templateValidation']['readyTemplateCount']}, "
            f"breaches={report['templateValidation']['breachCount']}"
        )
        if args.fail_on_template_breaches and report["status"] != "PASSED":
            return 1
        return 0
    report = build_report(args)
    json_path, md_path = write_reports(report, args.output_dir)
    exported_template_path = None
    if str(args.export_template_config_path or "").strip():
        exported_template_path = write_exported_template_config(
            {**report, "reportPath": str(json_path)},
            args.export_template_config_path,
        )
    print(f"[report] {json_path}")
    print(f"[report] {md_path}")
    if exported_template_path is not None:
        print(f"[template-config] {exported_template_path}")
        print(f"TEMPLATE_CONFIG_PATH={exported_template_path}")
    print(f"JSON_PATH={json_path}")
    print(f"MD_PATH={md_path}")
    print(f"STATUS={report['status']}")
    print(
        "SUMMARY="
        f"threshold policy backfill {report['mode']}, "
        f"candidates={report['candidateCount']}, applied={report['appliedCount']}, skipped={report['skippedCount']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
