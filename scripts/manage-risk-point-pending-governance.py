#!/usr/bin/env python3
"""Build and execute batch governance plans for risk point pending bindings."""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Dict, Iterable, List, Sequence
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse
from urllib.request import Request, urlopen

import pymysql


REPO_ROOT = Path(__file__).resolve().parent.parent
APP_DEV_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application-dev.yml"
DEFAULT_BASE_URL = "http://127.0.0.1:10099"
DEFAULT_USERNAME = "admin"
DEFAULT_PASSWORD = "123456"
PENDING_STATUSES = ("PENDING_METRIC_GOVERNANCE", "PARTIALLY_PROMOTED")
EXCLUDE_DEVICE_KEYWORDS = ("声光", "爆闪", "广播", "摄像", "监控", "情报板", "音柱", "联动", "控制器")


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


def resolve_connection_args(jdbc_url: str | None, user: str | None, password: str | None) -> Dict[str, object]:
    defaults = load_dev_defaults()
    resolved_jdbc_url = jdbc_url or os.getenv("IOT_MYSQL_URL") or defaults["jdbc_url"]
    resolved_user = user or os.getenv("IOT_MYSQL_USERNAME") or defaults["user"]
    resolved_password = password or os.getenv("IOT_MYSQL_PASSWORD") or defaults["password"]

    parsed = urlparse(resolved_jdbc_url.replace("jdbc:mysql://", "mysql://", 1))
    database = parsed.path.lstrip("/")
    if not parsed.hostname or not database:
        raise RuntimeError(f"Invalid jdbc url: {resolved_jdbc_url}")
    return {
        "host": parsed.hostname,
        "port": parsed.port or 3306,
        "database": database,
        "user": resolved_user,
        "password": resolved_password,
        "charset": "utf8mb4",
        "cursorclass": pymysql.cursors.DictCursor,
    }


def normalize_text(value: object) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def current_timestamp() -> str:
    return datetime.now().strftime("%Y%m%d%H%M%S")


def default_output_path(prefix: str) -> Path:
    return Path.cwd() / f"{prefix}-{current_timestamp()}.json"


def api_request(
    base_url: str,
    path: str,
    *,
    method: str = "GET",
    token: str | None = None,
    body: Dict[str, object] | None = None,
    timeout: int = 30,
) -> Dict[str, object]:
    url = f"{base_url.rstrip('/')}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    payload = None if body is None else json.dumps(body, ensure_ascii=False).encode("utf-8")
    request = Request(url, data=payload, headers=headers, method=method)
    try:
        with urlopen(request, timeout=timeout) as response:
            text = response.read().decode("utf-8")
            return json.loads(text) if text else {}
    except HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {path} failed with HTTP {exc.code}: {detail}") from exc
    except URLError as exc:
        raise RuntimeError(f"{method} {path} failed: {exc}") from exc


def login(base_url: str, username: str, password: str) -> str:
    payload = api_request(
        base_url,
        "/api/auth/login",
        method="POST",
        body={"username": username, "password": password},
        timeout=20,
    )
    token = ((payload.get("data") or {}).get("token"))
    if not token:
        raise RuntimeError("Login succeeded without token payload")
    return token


def fetch_pending_rows(connection_args: Dict[str, object]) -> List[Dict[str, object]]:
    sql = """
        select p.id,
               p.risk_point_name,
               p.device_name,
               p.device_code,
               p.resolution_status,
               pr.product_name
        from risk_point_device_pending_binding p
        left join iot_device d on d.id = p.device_id and d.deleted = 0
        left join iot_product pr on pr.id = d.product_id and pr.deleted = 0
        where p.deleted = 0
          and p.resolution_status in (%s, %s)
        order by p.id desc
    """
    with pymysql.connect(**connection_args) as connection:
        with connection.cursor() as cursor:
            cursor.execute(sql, PENDING_STATUSES)
            return list(cursor.fetchall())


def fetch_candidate_entries(base_url: str, token: str, pending_id: int) -> List[Dict[str, str]]:
    payload = api_request(
        base_url,
        f"/api/risk-point/pending-bindings/{pending_id}/candidates",
        method="GET",
        token=token,
        timeout=20,
    )
    candidates = (payload.get("data") or {}).get("candidates") or []
    normalized: List[Dict[str, str]] = []
    for item in candidates:
        metric_identifier = normalize_text(item.get("metricIdentifier")) if isinstance(item, dict) else None
        if not metric_identifier:
            continue
        normalized.append(
            {
                "metricIdentifier": metric_identifier,
                "metricName": normalize_text(item.get("metricName")) if isinstance(item, dict) else None,
            }
        )
    return normalized


def should_exclude_by_device_name(device_name: str | None) -> bool:
    text = device_name or ""
    return any(keyword in text for keyword in EXCLUDE_DEVICE_KEYWORDS)


def infer_family(device_name: str | None, product_name: str | None) -> str:
    normalized_device_name = device_name or ""
    normalized_product_name = product_name or ""
    text = f"{normalized_device_name} {normalized_product_name}".lower()
    if "gnss" in text and ("基准站" in normalized_device_name or "基准站" in normalized_product_name):
        return "GNSS基准站"
    if "gnss" in text:
        return "GNSS位移监测仪"
    if "多维" in normalized_device_name or "多维" in normalized_product_name:
        return "多维位移监测仪"
    if "测斜" in text or "深部位移" in text:
        return "深部位移/固定测斜仪"
    if "激光测距" in text:
        return "激光测距仪"
    if "倾角" in text:
        return normalized_product_name or "倾角仪"
    if "雨量" in text:
        return "雨量计"
    if "裂缝" in text:
        return "裂缝计"
    if "雷达" in text:
        return "雷达"
    if "水位" in text:
        return "水位计"
    if "孔压" in text or "渗压" in text:
        return "孔压/渗压"
    return normalized_product_name or normalized_device_name or "未识别家族"


def candidate_identifiers(candidate_entries: Sequence[Dict[str, str]]) -> List[str]:
    return [entry["metricIdentifier"] for entry in candidate_entries if normalize_text(entry.get("metricIdentifier"))]


def candidate_name_map(candidate_entries: Sequence[Dict[str, str]]) -> Dict[str, str]:
    result: Dict[str, str] = {}
    for entry in candidate_entries:
        identifier = normalize_text(entry.get("metricIdentifier"))
        if not identifier or identifier in result:
            continue
        result[identifier] = normalize_text(entry.get("metricName")) or identifier
    return result


def pick_recommended_metrics(
    candidate_entries: Sequence[Dict[str, str]],
    preferred_identifiers: Sequence[str],
) -> List[Dict[str, str]]:
    name_map = candidate_name_map(candidate_entries)
    selected: List[Dict[str, str]] = []
    for identifier in preferred_identifiers:
        if identifier in name_map:
            selected.append({"metricIdentifier": identifier, "metricName": name_map[identifier]})
    return selected


def build_base_item(
    row: Dict[str, object],
    family: str,
    candidate_entries: Sequence[Dict[str, str]],
) -> Dict[str, object]:
    return {
        "pendingId": row["id"],
        "riskPointName": normalize_text(row.get("risk_point_name")),
        "deviceName": normalize_text(row.get("device_name")),
        "deviceCode": normalize_text(row.get("device_code")),
        "productName": normalize_text(row.get("product_name")),
        "resolutionStatus": normalize_text(row.get("resolution_status")),
        "family": family,
        "candidateMetrics": list(candidate_entries),
    }


def classify_pending_row(row: Dict[str, object], candidate_entries: Sequence[Dict[str, str]]) -> tuple[str, Dict[str, object]]:
    family = infer_family(normalize_text(row.get("device_name")), normalize_text(row.get("product_name")))
    item = build_base_item(row, family, candidate_entries)
    metric_ids = candidate_identifiers(candidate_entries)
    metric_set = set(metric_ids)

    if should_exclude_by_device_name(item["deviceName"]):
        item["reason"] = "设备名称命中非监测类关键词，建议进入 ignore/排除批次"
        item["ignoreNote"] = "批量治理脚本判定为非重点监测设备，按排除批次忽略"
        return "exclude_candidates", item

    if family == "多维位移监测仪" and metric_set == {"gX", "gY", "gZ"} and len(metric_ids) == 3:
        item["recommendedMetrics"] = pick_recommended_metrics(candidate_entries, ["gX", "gY", "gZ"])
        item["completePending"] = True
        item["promotionNote"] = "批量治理脚本：多维位移重点测点转正"
        item["reason"] = "多维位移监测仪候选稳定命中 gX/gY/gZ"
        return "promote_candidates", item

    if family == "GNSS位移监测仪":
        if {"gpsTotalX", "gpsTotalY", "gpsTotalZ"}.issubset(metric_set):
            item["recommendedMetrics"] = pick_recommended_metrics(candidate_entries, ["gpsTotalX", "gpsTotalY", "gpsTotalZ"])
            item["completePending"] = True
            item["promotionNote"] = "批量治理脚本：GNSS 重点位移测点转正"
            item["reason"] = "GNSS家族候选已出现规范位移测点，建议只转正 gpsTotalX/Y/Z"
            return "promote_candidates", item
        if metric_set == {"gX", "gY", "gZ"} and len(metric_ids) == 3:
            item["reason"] = "GNSS家族仅出现 gX/gY/gZ，疑似设备归属或字段语义不清，需人工复核后再定"
            return "manual_review", item
        if not metric_ids:
            item["reason"] = "当前候选为空或未命中重点规范测点，需要继续等待运行态证据或人工补录规则"
            return "need_runtime_evidence", item
        item["reason"] = "GNSS家族候选存在非标准组合，需人工复核后再决定是否转正"
        return "manual_review", item

    if family == "GNSS基准站":
        if metric_set == {"gX", "gY", "gZ"} and len(metric_ids) == 3:
            item["reason"] = "GNSS基准站仅出现 gX/gY/gZ，不符合当前重点 GNSS 位移口径，需人工复核"
            return "manual_review", item
        if not metric_ids:
            item["reason"] = "当前候选为空或未命中重点规范测点，需要继续等待运行态证据或人工补录规则"
            return "need_runtime_evidence", item
        item["reason"] = "GNSS基准站候选组合需要人工复核"
        return "manual_review", item

    if "倾角" in family:
        if metric_set == {"AZI", "X", "Y", "Z", "angle"} and len(metric_ids) == 5:
            item["recommendedMetrics"] = pick_recommended_metrics(candidate_entries, ["AZI", "X", "Y", "Z", "angle"])
            item["completePending"] = True
            item["promotionNote"] = "批量治理脚本：倾角仪重点测点转正"
            item["reason"] = "倾角仪候选命中规范倾角测点集合"
            return "promote_candidates", item
        if metric_set == {"gX", "gY", "gZ"} and len(metric_ids) == 3:
            item["reason"] = "倾角仪出现 gX/gY/gZ，需确认是否接受为重点测点，或继续收口到 X/Y/Z/angle/AZI"
            return "manual_review", item
        if not metric_ids:
            item["reason"] = "当前候选为空或未命中重点规范测点，需要继续等待运行态证据或人工补录规则"
            return "need_runtime_evidence", item
        item["reason"] = "倾角仪候选组合需要人工复核"
        return "manual_review", item

    if family == "雨量计":
        selected = pick_recommended_metrics(candidate_entries, ["value", "totalValue"])
        if selected:
            item["recommendedMetrics"] = selected
            item["completePending"] = True
            item["promotionNote"] = "批量治理脚本：雨量计重点测点转正"
            item["reason"] = "雨量计已出现降雨核心测点，建议优先转正 value/totalValue，暂不把 temp 作为重点测点"
            return "promote_candidates", item
        if not metric_ids:
            item["reason"] = "当前候选为空或未命中重点规范测点，需要继续等待运行态证据或人工补录规则"
            return "need_runtime_evidence", item
        item["reason"] = "雨量计候选组合需要人工复核"
        return "manual_review", item

    if family in {"激光测距仪", "深部位移/固定测斜仪"}:
        if not metric_ids:
            item["reason"] = "当前候选为空或未命中重点规范测点，需要继续等待运行态证据或人工补录规则"
            return "need_runtime_evidence", item
        item["reason"] = f"{family} 当前候选组合需要人工复核"
        return "manual_review", item

    if not metric_ids:
        item["reason"] = "当前候选为空或未命中重点规范测点，需要继续等待运行态证据或人工补录规则"
        return "need_runtime_evidence", item

    item["reason"] = "当前候选存在但尚未匹配自动批量口径，建议先人工复核"
    return "manual_review", item


def build_summary(manifest: Dict[str, object]) -> Dict[str, int]:
    return {
        "totalPending": sum(len(manifest[key]) for key in bucket_keys()),
        "promoteCandidateCount": len(manifest["promote_candidates"]),
        "manualReviewCount": len(manifest["manual_review"]),
        "needRuntimeEvidenceCount": len(manifest["need_runtime_evidence"]),
        "excludeCandidateCount": len(manifest["exclude_candidates"]),
    }


def bucket_keys() -> Sequence[str]:
    return ("promote_candidates", "manual_review", "need_runtime_evidence", "exclude_candidates")


def build_operational_manifest(
    rows: Sequence[Dict[str, object]],
    candidates_by_pending_id: Dict[int, Sequence[Dict[str, str]]],
) -> Dict[str, object]:
    manifest = {
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "promote_candidates": [],
        "manual_review": [],
        "need_runtime_evidence": [],
        "exclude_candidates": [],
    }
    for row in rows:
        pending_id = int(row["id"])
        bucket, item = classify_pending_row(row, candidates_by_pending_id.get(pending_id, []))
        manifest[bucket].append(item)
    manifest["summary"] = build_summary(manifest)
    return manifest


def read_manifest(path: str) -> Dict[str, object]:
    with Path(path).open("r", encoding="utf-8") as file:
        manifest = json.load(file)
    if "summary" not in manifest:
        manifest["summary"] = build_summary(manifest)
    return manifest


def write_json(path: str | Path, payload: Dict[str, object]) -> None:
    target = Path(path)
    target.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def print_summary(manifest: Dict[str, object]) -> None:
    summary = manifest["summary"]
    print(
        "summary "
        f"totalPending={summary['totalPending']} "
        f"promoteCandidateCount={summary['promoteCandidateCount']} "
        f"manualReviewCount={summary['manualReviewCount']} "
        f"needRuntimeEvidenceCount={summary['needRuntimeEvidenceCount']} "
        f"excludeCandidateCount={summary['excludeCandidateCount']}"
    )
    for bucket in bucket_keys():
        items = manifest.get(bucket, [])
        preview_ids = [item["pendingId"] for item in items[:10]]
        print(f"{bucket} count={len(items)} previewPendingIds={preview_ids}")


def parse_pending_ids(raw: str | None) -> set[int] | None:
    if not raw:
        return None
    result: set[int] = set()
    for piece in raw.split(","):
        text = piece.strip()
        if text:
            result.add(int(text))
    return result


def select_items(
    items: Sequence[Dict[str, object]],
    *,
    pending_ids: set[int] | None,
    limit: int | None,
) -> List[Dict[str, object]]:
    filtered = [item for item in items if pending_ids is None or int(item["pendingId"]) in pending_ids]
    if limit is not None:
        filtered = filtered[:limit]
    return filtered


def build_promote_request(item: Dict[str, object]) -> Dict[str, object]:
    metrics = item.get("recommendedMetrics") or []
    return {
        "metrics": [
            {
                "metricIdentifier": entry["metricIdentifier"],
                "metricName": entry.get("metricName"),
            }
            for entry in metrics
        ],
        "completePending": bool(item.get("completePending", True)),
        "promotionNote": item.get("promotionNote") or item.get("reason"),
    }


def build_ignore_request(item: Dict[str, object]) -> Dict[str, object]:
    return {
        "ignoreNote": item.get("ignoreNote") or item.get("reason"),
    }


def execute_batch(
    *,
    mode: str,
    base_url: str,
    username: str,
    password: str,
    items: Sequence[Dict[str, object]],
    apply_mode: bool,
) -> Dict[str, object]:
    token = None
    if apply_mode:
        token = login(base_url, username, password)

    results: List[Dict[str, object]] = []
    for index, item in enumerate(items, start=1):
        pending_id = int(item["pendingId"])
        request_body = build_promote_request(item) if mode == "promote" else build_ignore_request(item)
        result = {
            "pendingId": pending_id,
            "deviceName": item.get("deviceName"),
            "mode": mode,
            "dryRun": not apply_mode,
            "requestBody": request_body,
        }
        try:
            if apply_mode:
                path = f"/api/risk-point/pending-bindings/{pending_id}/{'promote' if mode == 'promote' else 'ignore'}"
                response = api_request(base_url, path, method="POST", token=token, body=request_body, timeout=30)
                result["response"] = response
                result["status"] = "APPLIED"
            else:
                result["status"] = "DRY_RUN"
            results.append(result)
            print(f"{mode} processed {index}/{len(items)} pendingId={pending_id} status={result['status']}")
        except Exception as exc:
            result["status"] = "ERROR"
            result["error"] = str(exc)
            results.append(result)
            print(f"{mode} processed {index}/{len(items)} pendingId={pending_id} status=ERROR error={exc}")

    summary = Counter(result["status"] for result in results)
    return {
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "mode": mode,
        "apply": apply_mode,
        "summary": dict(summary),
        "results": results,
    }


def add_connection_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--jdbc-url", help="Override MySQL jdbc url. Defaults to application-dev.yml / env.")
    parser.add_argument("--user", help="Override MySQL username. Defaults to application-dev.yml / env.")
    parser.add_argument("--password", help="Override MySQL password. Defaults to application-dev.yml / env.")


def add_login_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help=f"Backend base url. Defaults to {DEFAULT_BASE_URL}.")
    parser.add_argument("--login-username", default=DEFAULT_USERNAME, help=f"Login username. Defaults to {DEFAULT_USERNAME}.")
    parser.add_argument("--login-password", default=DEFAULT_PASSWORD, help="Login password. Defaults to shared dev baseline.")


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Batch governance helper for risk point pending bindings.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    build_manifest = subparsers.add_parser("build-manifest", help="Scan pending rows and build an operational manifest.")
    add_connection_arguments(build_manifest)
    add_login_arguments(build_manifest)
    build_manifest.add_argument("--output", help="Write manifest JSON to this file.")

    plan = subparsers.add_parser("plan", help="Print a human-readable summary from a manifest.")
    plan.add_argument("--manifest", required=True, help="Path to the manifest JSON file.")

    promote = subparsers.add_parser("promote", help="Dry-run or apply promote actions from the manifest.")
    add_login_arguments(promote)
    promote.add_argument("--manifest", required=True, help="Path to the manifest JSON file.")
    promote.add_argument("--pending-ids", help="Comma-separated pending ids to execute.")
    promote.add_argument("--limit", type=int, help="Only execute the first N matching items.")
    promote.add_argument("--apply", action="store_true", help="Actually call the backend promote API.")
    promote.add_argument("--result-output", help="Write execution result JSON to this file.")

    ignore = subparsers.add_parser("ignore", help="Dry-run or apply ignore actions from the manifest.")
    add_login_arguments(ignore)
    ignore.add_argument("--manifest", required=True, help="Path to the manifest JSON file.")
    ignore.add_argument("--pending-ids", help="Comma-separated pending ids to execute.")
    ignore.add_argument("--limit", type=int, help="Only execute the first N matching items.")
    ignore.add_argument("--apply", action="store_true", help="Actually call the backend ignore API.")
    ignore.add_argument("--result-output", help="Write execution result JSON to this file.")

    return parser.parse_args(argv)


def run_build_manifest(args: argparse.Namespace) -> int:
    connection_args = resolve_connection_args(args.jdbc_url, args.user, args.password)
    rows = fetch_pending_rows(connection_args)
    token = login(args.base_url, args.login_username, args.login_password)
    candidates_by_pending_id: Dict[int, Sequence[Dict[str, str]]] = {}
    for index, row in enumerate(rows, start=1):
        pending_id = int(row["id"])
        if should_exclude_by_device_name(normalize_text(row.get("device_name"))):
            candidates_by_pending_id[pending_id] = []
        else:
            candidates_by_pending_id[pending_id] = fetch_candidate_entries(args.base_url, token, pending_id)
        if index % 25 == 0:
            print(f"build-manifest scanned {index}/{len(rows)} pending rows")

    manifest = build_operational_manifest(rows, candidates_by_pending_id)
    output_path = Path(args.output) if args.output else default_output_path("risk-point-pending-operational-manifest")
    write_json(output_path, manifest)
    print_summary(manifest)
    print(f"manifestFile={output_path}")
    return 0


def run_plan(args: argparse.Namespace) -> int:
    manifest = read_manifest(args.manifest)
    print_summary(manifest)
    return 0


def run_action(args: argparse.Namespace, mode: str) -> int:
    manifest = read_manifest(args.manifest)
    bucket = "promote_candidates" if mode == "promote" else "exclude_candidates"
    items = select_items(
        manifest.get(bucket, []),
        pending_ids=parse_pending_ids(args.pending_ids),
        limit=args.limit,
    )
    if not items:
        print(f"{mode} selectedCount=0")
        return 0
    result = execute_batch(
        mode=mode,
        base_url=args.base_url,
        username=args.login_username,
        password=args.login_password,
        items=items,
        apply_mode=bool(args.apply),
    )
    summary_text = " ".join(f"{key}={value}" for key, value in sorted(result["summary"].items()))
    print(f"{mode} summary {summary_text}")
    if args.result_output:
        write_json(args.result_output, result)
        print(f"resultFile={args.result_output}")
    return 0


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv)
    if args.command == "build-manifest":
        return run_build_manifest(args)
    if args.command == "plan":
        return run_plan(args)
    if args.command == "promote":
        return run_action(args, "promote")
    if args.command == "ignore":
        return run_action(args, "ignore")
    raise RuntimeError(f"Unsupported command: {args.command}")


if __name__ == "__main__":
    sys.exit(main())
