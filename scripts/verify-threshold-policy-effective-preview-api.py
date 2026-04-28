#!/usr/bin/env python3
"""Read-only API acceptance for threshold policy effective preview."""

from __future__ import annotations

import argparse
import json
import re
from datetime import datetime
from pathlib import Path
from typing import Any, Sequence
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse
from urllib.request import ProxyHandler, Request, build_opener, urlopen


REPO_ROOT = Path(__file__).resolve().parent.parent
APP_BASE_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application.yml"
DEFAULT_OUTPUT_DIR = REPO_ROOT / "logs" / "acceptance"
RULE_PAGE_PATH = "/api/rule-definition/page"
PREVIEW_PATH = "/api/rule-definition/effective-preview"
PRECEDENCE = ["BINDING", "DEVICE", "PRODUCT", "PRODUCT_TYPE", "METRIC"]


class AcceptanceError(RuntimeError):
    """Acceptance assertion error."""


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Verify threshold policy effective preview through the real API."
    )
    parser.add_argument("--base-url", help="Override backend base url. Defaults to application.yml port/context path.")
    parser.add_argument("--username", default="admin")
    parser.add_argument("--password", default="123456")
    parser.add_argument("--page-num", type=int, default=1)
    parser.add_argument("--page-size", type=int, default=20)
    parser.add_argument("--sample-size", type=int, default=10)
    parser.add_argument("--timeout-seconds", type=int, default=15)
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT_DIR))
    parser.add_argument("--fail-on-empty", action="store_true")
    parser.add_argument("--fail-on-breaches", action="store_true")
    return parser.parse_args(argv)


def load_default_base_url() -> str:
    app_text = APP_BASE_PATH.read_text(encoding="utf-8")
    port_match = re.search(r"server:\s*\n\s+port:\s*(\d+)", app_text)
    context_match = re.search(r"context-path:\s*(.+)", app_text)
    port = port_match.group(1) if port_match else "9999"
    context_path = context_match.group(1).strip() if context_match else "/"
    if context_path == "/":
        context_path = ""
    return f"http://127.0.0.1:{port}{context_path}"


def request_json(
    base_url: str,
    path: str,
    method: str = "GET",
    payload: dict[str, Any] | None = None,
    headers: dict[str, str] | None = None,
    timeout_seconds: int = 15,
) -> dict[str, Any]:
    url = base_url.rstrip("/") + path
    encoded_body = None
    request_headers = {"Accept": "application/json"}
    if headers:
        request_headers.update(headers)
    if payload is not None:
        encoded_body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        request_headers["Content-Type"] = "application/json; charset=utf-8"
    req = Request(url, data=encoded_body, method=method.upper(), headers=request_headers)
    try:
        parsed_base_url = urlparse(base_url)
        local_target = parsed_base_url.hostname in {"127.0.0.1", "localhost"}
        opener = build_opener(ProxyHandler({})) if local_target else None
        open_fn = opener.open if opener is not None else urlopen
        with open_fn(req, timeout=timeout_seconds) as resp:
            body = resp.read().decode("utf-8")
    except HTTPError as ex:
        body = ex.read().decode("utf-8", errors="replace")
        raise AcceptanceError(f"{method.upper()} {path} failed: HTTP {ex.code}, body={body}") from ex
    except URLError as ex:
        raise AcceptanceError(f"{method.upper()} {path} failed: {ex.reason}") from ex
    try:
        return json.loads(body)
    except json.JSONDecodeError as ex:
        raise AcceptanceError(f"{method.upper()} {path} returned non-JSON body: {body}") from ex


def ensure_envelope_ok(response: dict[str, Any], path: str) -> dict[str, Any]:
    if response.get("code") != 200:
        raise AcceptanceError(f"{path} returned code={response.get('code')}, msg={response.get('msg')}")
    return response


def login(base_url: str, username: str, password: str, timeout_seconds: int) -> str:
    response = ensure_envelope_ok(
        request_json(
            base_url,
            "/api/auth/login",
            method="POST",
            payload={"username": username, "password": password},
            timeout_seconds=timeout_seconds,
        ),
        "/api/auth/login",
    )
    token = (response.get("data") or {}).get("token")
    if not token:
        raise AcceptanceError("/api/auth/login returned success but token is missing")
    return str(token)


def normalize_scope(rule: dict[str, Any]) -> str:
    scope = str(rule.get("ruleScope") or "METRIC").strip().upper()
    return scope if scope else "METRIC"


def compact_dict(values: dict[str, Any]) -> dict[str, Any]:
    return {key: value for key, value in values.items() if value is not None and value != ""}


def build_preview_params(rule: dict[str, Any]) -> dict[str, Any]:
    scope = normalize_scope(rule)
    params = {
        "tenantId": rule.get("tenantId"),
        "riskMetricId": rule.get("riskMetricId"),
        "metricIdentifier": rule.get("metricIdentifier"),
    }
    if scope == "PRODUCT_TYPE":
        params["productType"] = rule.get("productType")
    elif scope == "PRODUCT":
        params["productId"] = rule.get("productId")
    elif scope == "DEVICE":
        params["deviceId"] = rule.get("deviceId")
    elif scope == "BINDING":
        params["riskPointDeviceId"] = rule.get("riskPointDeviceId")
    return compact_dict(params)


def same_id(left: Any, right: Any) -> bool:
    if left is None or right is None:
        return left is right
    return str(left) == str(right)


def fetch_enabled_rules(args: argparse.Namespace, base_url: str, token: str) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    query = urlencode(
        {
            "scopeView": "ALL",
            "status": 0,
            "pageNum": args.page_num,
            "pageSize": args.page_size,
        }
    )
    path = f"{RULE_PAGE_PATH}?{query}"
    response = ensure_envelope_ok(
        request_json(
            base_url,
            path,
            headers={"Authorization": f"Bearer {token}"},
            timeout_seconds=args.timeout_seconds,
        ),
        path,
    )
    page_data = response.get("data") or {}
    records = page_data.get("records") or []
    if not isinstance(records, list):
        raise AcceptanceError(f"{path} returned records that are not a list")
    return page_data, [record for record in records if isinstance(record, dict)]


def fetch_preview(args: argparse.Namespace, base_url: str, token: str, rule: dict[str, Any]) -> dict[str, Any]:
    query = urlencode(build_preview_params(rule))
    path = f"{PREVIEW_PATH}?{query}"
    response = ensure_envelope_ok(
        request_json(
            base_url,
            path,
            headers={"Authorization": f"Bearer {token}"},
            timeout_seconds=args.timeout_seconds,
        ),
        path,
    )
    data = response.get("data")
    if not isinstance(data, dict):
        raise AcceptanceError(f"{path} returned preview data that is not an object")
    return data


def evaluate_preview(rule: dict[str, Any], preview: dict[str, Any]) -> list[dict[str, Any]]:
    breaches = []
    if preview.get("hasMatchedRule") is not True:
        breaches.append({
            "type": "NO_MATCHED_RULE",
            "ruleId": rule.get("id"),
            "message": "effective preview did not return a matched rule",
        })
    matched_rule = preview.get("matchedRule") or {}
    if not same_id(matched_rule.get("id"), rule.get("id")):
        breaches.append({
            "type": "SELECTED_RULE_MISMATCH",
            "ruleId": rule.get("id"),
            "matchedRuleId": matched_rule.get("id"),
        })
    candidates = preview.get("candidates") or []
    if not isinstance(candidates, list) or not candidates:
        breaches.append({
            "type": "CANDIDATES_MISSING",
            "ruleId": rule.get("id"),
            "message": "effective preview did not return candidate explanations",
        })
    elif not any(same_id(candidate.get("ruleId"), rule.get("id")) and candidate.get("selected") is True for candidate in candidates if isinstance(candidate, dict)):
        breaches.append({
            "type": "SELECTED_CANDIDATE_MISSING",
            "ruleId": rule.get("id"),
            "message": "candidate list does not mark the expected rule as selected",
        })
    if normalize_scope(rule) not in PRECEDENCE:
        breaches.append({
            "type": "UNKNOWN_RULE_SCOPE",
            "ruleId": rule.get("id"),
            "scope": rule.get("ruleScope"),
        })
    return breaches


def summarize_preview_results(preview_results: list[dict[str, Any]]) -> dict[str, Any]:
    return {
        "sampledRuleCount": len(preview_results),
        "matchedPreviewCount": sum(1 for item in preview_results if (item.get("preview") or {}).get("hasMatchedRule") is True),
    }


def evaluate_report(report: dict[str, Any], fail_on_empty: bool) -> dict[str, Any]:
    breaches = []
    if fail_on_empty and not report["sampledRules"]:
        breaches.append({
            "type": "NO_ENABLED_RULE_SAMPLE",
            "message": "no enabled threshold policy was available for effective preview verification",
        })
    for item in report["previewResults"]:
        breaches.extend(evaluate_preview(item["rule"], item["preview"]))
    return {
        "status": "PASSED" if not breaches else "FAILED",
        "breachCount": len(breaches),
        "breaches": breaches,
    }


def build_report(args: argparse.Namespace) -> dict[str, Any]:
    base_url = args.base_url or load_default_base_url()
    token = login(base_url, args.username, args.password, args.timeout_seconds)
    page_data, records = fetch_enabled_rules(args, base_url, token)
    sampled_rules = records[: max(args.sample_size, 0)]
    preview_results = []
    for rule in sampled_rules:
        preview_results.append({"rule": rule, "preview": fetch_preview(args, base_url, token, rule)})
    report = {
        "checkedAt": datetime.now().isoformat(timespec="seconds"),
        "target": {
            "baseUrl": base_url,
            "rulePagePath": RULE_PAGE_PATH,
            "previewPath": PREVIEW_PATH,
        },
        "query": {
            "pageNum": args.page_num,
            "pageSize": args.page_size,
            "sampleSize": args.sample_size,
        },
        "page": {
            "total": page_data.get("total"),
            "size": page_data.get("size"),
            "current": page_data.get("current"),
        },
        "sampledRules": sampled_rules,
        "previewResults": preview_results,
    }
    report["summary"] = summarize_preview_results(preview_results)
    report["evaluation"] = evaluate_report(report, args.fail_on_empty)
    return report


def markdown_table(headers: list[str], rows: list[list[Any]]) -> str:
    lines = [
        "| " + " | ".join(headers) + " |",
        "| " + " | ".join(["---"] * len(headers)) + " |",
    ]
    for row in rows:
        lines.append("| " + " | ".join(str(value) for value in row) + " |")
    return "\n".join(lines)


def render_markdown(report: dict[str, Any]) -> str:
    evaluation = report["evaluation"]
    summary = report["summary"]
    lines = [
        "# Threshold Policy Effective Preview API Verification",
        "",
        f"- Status: `{evaluation['status']}`",
        f"- Checked At: `{report['checkedAt']}`",
        f"- Target: `{report['target']['baseUrl']}{report['target'].get('previewPath', PREVIEW_PATH)}`",
        f"- Sampled Rules: `{summary['sampledRuleCount']}`",
        f"- Matched Previews: `{summary['matchedPreviewCount']}`",
        f"- Policy Precedence: `{' > '.join(PRECEDENCE)}`",
        "",
        "## Preview Samples",
        markdown_table(
            ["Scope", "Metric", "Rule", "Matched Rule", "Matched"],
            [
                [
                    normalize_scope(item["rule"]),
                    item["rule"].get("metricIdentifier") or "--",
                    item["rule"].get("ruleName") or item["rule"].get("id") or "--",
                    (item["preview"].get("matchedRule") or {}).get("ruleName")
                    or (item["preview"].get("matchedRule") or {}).get("id")
                    or "--",
                    item["preview"].get("hasMatchedRule") is True,
                ]
                for item in report["previewResults"]
            ] or [["--", "--", "--", "--", False]],
        ),
        "",
    ]
    if evaluation["breaches"]:
        lines.extend([
            "## Breaches",
            markdown_table(
                ["Type", "Rule", "Message"],
                [
                    [
                        item["type"],
                        item.get("ruleId") or "--",
                        item.get("message") or item.get("matchedRuleId") or item.get("scope") or "--",
                    ]
                    for item in evaluation["breaches"]
                ],
            ),
            "",
        ])
    return "\n".join(lines)


def write_reports(report: dict[str, Any], output_dir: str | Path) -> tuple[Path, Path]:
    target_dir = Path(output_dir)
    target_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    json_path = target_dir / f"threshold-policy-effective-preview-api-{timestamp}.json"
    md_path = target_dir / f"threshold-policy-effective-preview-api-{timestamp}.md"
    json_content = json.dumps(report, ensure_ascii=False, indent=2)
    markdown_content = render_markdown(report)
    json_path.write_text(json_content, encoding="utf-8")
    md_path.write_text(markdown_content, encoding="utf-8")
    (target_dir / "threshold-policy-effective-preview-api-latest.json").write_text(json_content, encoding="utf-8")
    (target_dir / "threshold-policy-effective-preview-api-latest.md").write_text(markdown_content, encoding="utf-8")
    return json_path, md_path


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv)
    report = build_report(args)
    json_path, md_path = write_reports(report, args.output_dir)
    print(f"[report] {json_path}")
    print(f"[report] {md_path}")
    print(f"JSON_PATH={json_path}")
    print(f"MD_PATH={md_path}")
    print(f"STATUS={report['evaluation']['status']}")
    print(
        "SUMMARY="
        f"threshold policy effective preview {report['evaluation']['status'].lower()}, "
        f"samples={report['summary']['sampledRuleCount']}, "
        f"breaches={report['evaluation']['breachCount']}"
    )
    print(f"[status] {report['evaluation']['status']}")
    if args.fail_on_breaches and report["evaluation"]["status"] != "PASSED":
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
