#!/usr/bin/env python3
"""Read-only API acceptance for threshold policy recommendations."""

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
SUMMARY_PATH = "/api/risk-governance/missing-policies/product-metric-summaries"
RECOMMENDATION_FIELDS = {
    "recommendationWindowDays",
    "recommendationSampleCount",
    "recommendationMinValue",
    "recommendationMaxValue",
    "recommendationAvgValue",
    "recommendedExpression",
    "recommendedLowerExpression",
    "recommendedUpperExpression",
    "recommendationStatus",
    "recommendationDirection",
    "recommendationReason",
}
MANUAL_REVIEW_STATUSES = {
    "FLAT_ZERO_REVIEW",
    "INSUFFICIENT_SAMPLE",
    "NO_NUMERIC_SAMPLE",
    "REQUIRES_MANUAL_REVIEW",
    "UNSUPPORTED_PRODUCT_TYPE",
    "UNAVAILABLE",
}


class AcceptanceError(RuntimeError):
    """Acceptance assertion error."""


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Verify threshold policy recommendation fields through the real API."
    )
    parser.add_argument("--base-url", help="Override backend base url. Defaults to application.yml port/context path.")
    parser.add_argument("--username", default="admin")
    parser.add_argument("--password", default="123456")
    parser.add_argument("--page-num", type=int, default=1)
    parser.add_argument("--page-size", type=int, default=20)
    parser.add_argument("--timeout-seconds", type=int, default=15)
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT_DIR))
    parser.add_argument("--require-adoptable", action="store_true")
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


def has_recommendation_fields(record: dict[str, Any]) -> bool:
    return any(field in record for field in RECOMMENDATION_FIELDS)


def recommended_expression(record: dict[str, Any]) -> str:
    return str(
        record.get("recommendedExpression")
        or record.get("recommendedUpperExpression")
        or record.get("recommendedLowerExpression")
        or ""
    ).strip()


def summarize_records(records: list[dict[str, Any]]) -> dict[str, Any]:
    recommendation_field_rows = [record for record in records if has_recommendation_fields(record)]
    adoptable_records = [record for record in records if recommended_expression(record)]
    manual_review_records = [
        record
        for record in records
        if str(record.get("recommendationStatus") or "").strip().upper() in MANUAL_REVIEW_STATUSES
        and not recommended_expression(record)
    ]
    return {
        "recordCount": len(records),
        "recommendationFieldRowCount": len(recommendation_field_rows),
        "adoptableRecommendationCount": len(adoptable_records),
        "manualReviewCount": len(manual_review_records),
        "sampleAdoptable": adoptable_records[:5],
        "sampleManualReview": manual_review_records[:5],
    }


def evaluate_report(report: dict[str, Any], require_adoptable: bool) -> dict[str, Any]:
    summary = report["summary"]
    breaches = []
    if summary["recordCount"] > 0 and summary["recommendationFieldRowCount"] == 0:
        breaches.append({
            "type": "RECOMMENDATION_FIELDS_MISSING",
            "message": "missing policy summary rows did not expose threshold recommendation fields",
        })
    if require_adoptable and summary["adoptableRecommendationCount"] == 0:
        breaches.append({
            "type": "NO_ADOPTABLE_RECOMMENDATION",
            "message": "no row exposed a recommended threshold expression",
        })
    return {
        "status": "PASSED" if not breaches else "FAILED",
        "breachCount": len(breaches),
        "breaches": breaches,
    }


def fetch_summary_records(args: argparse.Namespace, base_url: str, token: str) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    query = urlencode({"pageNum": args.page_num, "pageSize": args.page_size})
    path = f"{SUMMARY_PATH}?{query}"
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


def build_report(args: argparse.Namespace) -> dict[str, Any]:
    base_url = args.base_url or load_default_base_url()
    token = login(base_url, args.username, args.password, args.timeout_seconds)
    page_data, records = fetch_summary_records(args, base_url, token)
    report = {
        "checkedAt": datetime.now().isoformat(timespec="seconds"),
        "target": {
            "baseUrl": base_url,
            "path": SUMMARY_PATH,
        },
        "query": {
            "pageNum": args.page_num,
            "pageSize": args.page_size,
        },
        "page": {
            "total": page_data.get("total"),
            "size": page_data.get("size"),
            "current": page_data.get("current"),
        },
        "summary": summarize_records(records),
        "records": records[:10],
    }
    report["evaluation"] = evaluate_report(report, args.require_adoptable)
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
        "# Threshold Policy Recommendation API Verification",
        "",
        f"- Status: `{evaluation['status']}`",
        f"- Checked At: `{report['checkedAt']}`",
        f"- Target: `{report['target']['baseUrl']}{report['target']['path']}`",
        f"- Records: `{summary['recordCount']}`",
        f"- Rows With Recommendation Fields: `{summary['recommendationFieldRowCount']}`",
        f"- Adoptable Recommendations: `{summary['adoptableRecommendationCount']}`",
        f"- Manual Review Rows: `{summary['manualReviewCount']}`",
        "",
        "## Adoptable Samples",
        markdown_table(
            ["Product", "Metric", "Expression", "Status", "Samples", "Window"],
            [
                [
                    item.get("productName") or item.get("productKey") or "--",
                    item.get("metricIdentifier") or "--",
                    recommended_expression(item) or "--",
                    item.get("recommendationStatus") or "--",
                    item.get("recommendationSampleCount") or 0,
                    item.get("recommendationWindowDays") or "--",
                ]
                for item in summary["sampleAdoptable"]
            ] or [["--", "--", "--", "--", 0, "--"]],
        ),
        "",
        "## Manual Review Samples",
        markdown_table(
            ["Product", "Metric", "Status", "Samples", "Window", "Reason"],
            [
                [
                    item.get("productName") or item.get("productKey") or "--",
                    item.get("metricIdentifier") or "--",
                    item.get("recommendationStatus") or "--",
                    item.get("recommendationSampleCount") or 0,
                    item.get("recommendationWindowDays") or "--",
                    item.get("recommendationReason") or "--",
                ]
                for item in summary["sampleManualReview"]
            ] or [["--", "--", "--", 0, "--", "--"]],
        ),
        "",
    ]
    if evaluation["breaches"]:
        lines.extend([
            "## Breaches",
            markdown_table(
                ["Type", "Message"],
                [[item["type"], item.get("message") or "--"] for item in evaluation["breaches"]],
            ),
            "",
        ])
    return "\n".join(lines)


def write_reports(report: dict[str, Any], output_dir: str | Path) -> tuple[Path, Path]:
    target_dir = Path(output_dir)
    target_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    json_path = target_dir / f"threshold-policy-recommendation-api-{timestamp}.json"
    md_path = target_dir / f"threshold-policy-recommendation-api-{timestamp}.md"
    json_content = json.dumps(report, ensure_ascii=False, indent=2)
    markdown_content = render_markdown(report)
    json_path.write_text(json_content, encoding="utf-8")
    md_path.write_text(markdown_content, encoding="utf-8")
    (target_dir / "threshold-policy-recommendation-api-latest.json").write_text(json_content, encoding="utf-8")
    (target_dir / "threshold-policy-recommendation-api-latest.md").write_text(markdown_content, encoding="utf-8")
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
        f"threshold recommendation api {report['evaluation']['status'].lower()}, "
        f"records={report['summary']['recordCount']}, "
        f"adoptable={report['summary']['adoptableRecommendationCount']}"
    )
    print(f"[status] {report['evaluation']['status']}")
    if args.fail_on_breaches and report["evaluation"]["status"] != "PASSED":
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
