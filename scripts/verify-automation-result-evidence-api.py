#!/usr/bin/env python3
"""Read-only API acceptance for automation result evidence lookup."""

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
RESULT_PAGE_PATH = "/api/report/automation-results/page"
RESULT_DETAIL_PATH_TEMPLATE = "/api/report/automation-results/{run_id}"
RESULT_EVIDENCE_PATH_TEMPLATE = "/api/report/automation-results/{run_id}/evidence"
RESULT_EVIDENCE_CONTENT_PATH_TEMPLATE = "/api/report/automation-results/{run_id}/evidence/content"


class AcceptanceError(RuntimeError):
    """Acceptance assertion error."""


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Verify automation result evidence is readable through the real API."
    )
    parser.add_argument("--base-url", help="Override backend base url. Defaults to application.yml port/context path.")
    parser.add_argument("--username", default="admin")
    parser.add_argument("--password", default="123456")
    parser.add_argument("--package-code", default="risk-ops-closure")
    parser.add_argument("--run-id", help="Specific registry run id to verify. Defaults to the latest matching run.")
    parser.add_argument("--page-num", type=int, default=1)
    parser.add_argument("--page-size", type=int, default=10)
    parser.add_argument("--expect-status", default="passed")
    parser.add_argument("--required-evidence-pattern", default="threshold-policy-effective-preview-api")
    parser.add_argument("--required-content", action="append")
    parser.add_argument("--timeout-seconds", type=int, default=15)
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT_DIR))
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


def fetch_result_page(args: argparse.Namespace, base_url: str, token: str) -> dict[str, Any]:
    query = {
        "pageNum": args.page_num,
        "pageSize": args.page_size,
    }
    if args.package_code:
        query["packageCode"] = args.package_code
    if args.run_id:
        query["keyword"] = args.run_id
    path = f"{RESULT_PAGE_PATH}?{urlencode(query)}"
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
        raise AcceptanceError(f"{path} returned page data that is not an object")
    return data


def fetch_result_detail(args: argparse.Namespace, base_url: str, token: str, run_id: str) -> dict[str, Any]:
    path = RESULT_DETAIL_PATH_TEMPLATE.format(run_id=run_id)
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
        raise AcceptanceError(f"{path} returned detail data that is not an object")
    return data


def fetch_result_evidence(args: argparse.Namespace, base_url: str, token: str, run_id: str) -> list[dict[str, Any]]:
    path = RESULT_EVIDENCE_PATH_TEMPLATE.format(run_id=run_id)
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
    if not isinstance(data, list):
        raise AcceptanceError(f"{path} returned evidence data that is not a list")
    return [item for item in data if isinstance(item, dict)]


def fetch_evidence_content(
    args: argparse.Namespace,
    base_url: str,
    token: str,
    run_id: str,
    evidence_path: str,
) -> dict[str, Any]:
    query = urlencode({"path": evidence_path})
    path = f"{RESULT_EVIDENCE_CONTENT_PATH_TEMPLATE.format(run_id=run_id)}?{query}"
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
        raise AcceptanceError(f"{path} returned evidence content that is not an object")
    return data


def select_run(page_data: dict[str, Any], requested_run_id: str | None) -> dict[str, Any] | None:
    records = page_data.get("records") or []
    if not isinstance(records, list):
        return None
    candidates = [item for item in records if isinstance(item, dict)]
    if requested_run_id:
        return next((item for item in candidates if str(item.get("runId")) == str(requested_run_id)), None)
    return candidates[0] if candidates else None


def find_required_evidence(
    evidence_items: list[dict[str, Any]],
    required_pattern: str | None,
) -> dict[str, Any] | None:
    if not required_pattern:
        return evidence_items[0] if evidence_items else None
    normalized_pattern = required_pattern.lower()
    matched = [
        item
        for item in evidence_items
        if normalized_pattern in str(item.get("path") or item.get("fileName") or "").lower()
    ]
    if not matched:
        return None
    return next(
        (
            item
            for item in matched
            if str(item.get("path") or "").lower().endswith(".md")
        ),
        next(
            (
                item
                for item in matched
                if str(item.get("category") or "").lower() == "markdown"
            ),
            matched[0],
        ),
    )


def evaluate_report(report: dict[str, Any]) -> dict[str, Any]:
    breaches = []
    selected_run = report.get("selectedRun")
    required_evidence = report.get("requiredEvidence")
    content_preview = report.get("contentPreview")
    expected_status = str(report.get("expectStatus") or "").strip().lower()
    if not selected_run:
        breaches.append({"type": "RUN_NOT_FOUND", "message": "no matching automation result run was found"})
    elif expected_status and str(selected_run.get("status") or "").strip().lower() != expected_status:
        breaches.append({
            "type": "RUN_STATUS_MISMATCH",
            "runId": selected_run.get("runId"),
            "expected": expected_status,
            "actual": selected_run.get("status"),
        })
    if not required_evidence:
        breaches.append({
            "type": "REQUIRED_EVIDENCE_MISSING",
            "runId": selected_run.get("runId") if selected_run else None,
            "message": "required evidence pattern was not found in evidence list",
        })
    if not content_preview:
        breaches.append({
            "type": "EVIDENCE_CONTENT_MISSING",
            "runId": selected_run.get("runId") if selected_run else None,
            "message": "required evidence content was not loaded",
        })
    else:
        content = str(content_preview.get("content") or "")
        for expected_text in report.get("requiredContent") or []:
            if expected_text and expected_text not in content:
                breaches.append({
                    "type": "EVIDENCE_CONTENT_TEXT_MISSING",
                    "runId": selected_run.get("runId") if selected_run else None,
                    "expected": expected_text,
                })
    return {
        "status": "PASSED" if not breaches else "FAILED",
        "breachCount": len(breaches),
        "breaches": breaches,
    }


def build_report(args: argparse.Namespace) -> dict[str, Any]:
    base_url = args.base_url or load_default_base_url()
    required_content = args.required_content or ["Status: `PASSED`"]
    token = login(base_url, args.username, args.password, args.timeout_seconds)
    page_data = fetch_result_page(args, base_url, token)
    selected_run = select_run(page_data, args.run_id)
    detail = None
    evidence_items: list[dict[str, Any]] = []
    required_evidence = None
    content_preview = None
    if selected_run:
        run_id = str(selected_run.get("runId"))
        detail = fetch_result_detail(args, base_url, token, run_id)
        evidence_items = fetch_result_evidence(args, base_url, token, run_id)
        required_evidence = find_required_evidence(evidence_items, args.required_evidence_pattern)
        if required_evidence:
            content_preview = fetch_evidence_content(
                args,
                base_url,
                token,
                run_id,
                str(required_evidence.get("path")),
            )
    report = {
        "checkedAt": datetime.now().isoformat(timespec="seconds"),
        "target": {
            "baseUrl": base_url,
            "pagePath": RESULT_PAGE_PATH,
            "packageCode": args.package_code,
            "requestedRunId": args.run_id,
            "requiredEvidencePattern": args.required_evidence_pattern,
        },
        "page": {
            "total": page_data.get("total"),
            "size": page_data.get("size"),
            "current": page_data.get("current"),
        },
        "selectedRun": selected_run,
        "detail": detail,
        "evidenceItems": evidence_items,
        "requiredEvidence": required_evidence,
        "contentPreview": content_preview,
        "expectStatus": args.expect_status,
        "requiredContent": required_content,
    }
    report["evaluation"] = evaluate_report(report)
    return report


def markdown_table(headers: list[str], rows: list[list[Any]]) -> str:
    lines = [
        "| " + " | ".join(headers) + " |",
        "| " + " | ".join(["---"] * len(headers)) + " |",
    ]
    for row in rows:
        lines.append("| " + " | ".join(str(value).replace("|", "\\|") for value in row) + " |")
    return "\n".join(lines)


def render_markdown(report: dict[str, Any]) -> str:
    evaluation = report["evaluation"]
    selected_run = report.get("selectedRun") or {}
    required_evidence = report.get("requiredEvidence") or {}
    evidence_items = report.get("evidenceItems") or []
    lines = [
        "# Automation Result Evidence API Verification",
        "",
        f"- Status: `{evaluation['status']}`",
        f"- Checked At: `{report['checkedAt']}`",
        f"- Target: `{report['target']['baseUrl']}{report['target'].get('pagePath', RESULT_PAGE_PATH)}`",
        f"- Package Code: `{report['target'].get('packageCode') or '--'}`",
        f"- Run ID: `{selected_run.get('runId') or '--'}`",
        f"- Run Status: `{selected_run.get('status') or '--'}`",
        f"- Evidence Count: `{len(evidence_items)}`",
        f"- Required Evidence: `{required_evidence.get('path') or '--'}`",
        "",
        "## Evidence",
        markdown_table(
            ["File", "Category", "Source"],
            [
                [
                    item.get("fileName") or item.get("path") or "--",
                    item.get("category") or "--",
                    item.get("source") or "--",
                ]
                for item in evidence_items
            ] or [["--", "--", "--"]],
        ),
        "",
    ]
    if evaluation["breaches"]:
        lines.extend([
            "## Breaches",
            markdown_table(
                ["Type", "Run", "Message"],
                [
                    [
                        item["type"],
                        item.get("runId") or selected_run.get("runId") or "--",
                        item.get("message") or item.get("expected") or item.get("actual") or "--",
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
    json_path = target_dir / f"automation-result-evidence-api-{timestamp}.json"
    md_path = target_dir / f"automation-result-evidence-api-{timestamp}.md"
    json_content = json.dumps(report, ensure_ascii=False, indent=2)
    markdown_content = render_markdown(report)
    json_path.write_text(json_content, encoding="utf-8")
    md_path.write_text(markdown_content, encoding="utf-8")
    (target_dir / "automation-result-evidence-api-latest.json").write_text(json_content, encoding="utf-8")
    (target_dir / "automation-result-evidence-api-latest.md").write_text(markdown_content, encoding="utf-8")
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
        f"automation result evidence {report['evaluation']['status'].lower()}, "
        f"runId={(report.get('selectedRun') or {}).get('runId') or '--'}, "
        f"evidence={len(report.get('evidenceItems') or [])}, "
        f"breaches={report['evaluation']['breachCount']}"
    )
    print(f"[status] {report['evaluation']['status']}")
    if args.fail_on_breaches and report["evaluation"]["status"] != "PASSED":
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
