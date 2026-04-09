#!/usr/bin/env python3
"""Build and execute batch governance plans for risk point pending bindings."""

from __future__ import annotations

import argparse
import csv
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


def default_output_prefix(prefix: str) -> Path:
    return Path.cwd() / f"{prefix}-{current_timestamp()}"


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


def write_json(path: str | Path, payload: object) -> None:
    target = Path(path)
    target.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def determine_manual_review_policy(item: Dict[str, object]) -> Dict[str, str]:
    family = normalize_text(item.get("family")) or ""
    product_name = normalize_text(item.get("productName")) or ""
    device_name = normalize_text(item.get("deviceName")) or ""
    family_text = f"{family} {product_name} {device_name}"
    if family == "GNSS基准站" or "GNSS基准站" in family_text:
        return {
            "group": "GNSS 基准站 gX/gY/gZ 复核组",
            "suggestedAction": "保留 pending，确认是否排除",
            "systemRecommendation": "暂不转正，待确认是否单独建基准站规则",
            "todo": "确认该基准站 gX/gY/gZ 是否仅为姿态/健康参数；若是，则不纳入正式风险测点",
            "resolutionNote": (
                "风险点与设备已匹配。2026-04-04 批量治理复核：当前候选仅见 gX/gY/gZ（加速度），"
                "暂不转正；请先确认该 GNSS 基准站字段是否仅为姿态/健康参数，未确认前继续保留 pending。"
            ),
        }
    if family == "GNSS位移监测仪" or "GNSS位移监测仪" in family_text:
        return {
            "group": "GNSS 位移监测仪 gX/gY/gZ 复核组",
            "suggestedAction": "先核对产品归属，再决定是否继续等待",
            "systemRecommendation": "暂不转正，不把 gX/gY/gZ 直接写入正式绑定",
            "todo": "核对设备命名、产品归属与字段语义；若仍属 GNSS 位移监测，则等待 gpsTotalX/gpsTotalY/gpsTotalZ 证据",
            "resolutionNote": (
                "风险点与设备已匹配。2026-04-04 批量治理复核：当前候选仅见 gX/gY/gZ（加速度），"
                "不符合当前 GNSS 位移重点测点口径，暂不转正；若仍属 GNSS 位移监测，请等待 "
                "gpsTotalX/gpsTotalY/gpsTotalZ 运行证据或先纠正产品归属/字段语义。"
            ),
        }
    if "倾角" in family_text:
        return {
            "group": "倾角仪 gX/gY/gZ 复核组",
            "suggestedAction": "保留 pending，优先补字段映射",
            "systemRecommendation": "暂不转正，等待规范字段或补映射",
            "todo": "确认该设备是否应继续收口到 AZI/X/Y/Z/angle；若是，则不要把 gX/gY/gZ 直接转正",
            "resolutionNote": (
                "风险点与设备已匹配。2026-04-04 批量治理复核：当前候选仅见 gX/gY/gZ（加速度），"
                "暂不转正；请先确认该设备是否应收口到 AZI/X/Y/Z/angle 等规范倾角测点，未确认前继续保留 pending。"
            ),
        }
    normalized_family = family or product_name or device_name or "未识别设备"
    return {
        "group": f"{normalized_family} 人工复核组",
        "suggestedAction": "保留 pending，等待业务确认",
        "systemRecommendation": "暂不转正，等待人工复核",
        "todo": "确认字段语义与业务归属后，再决定是否转正、继续等待或排除",
        "resolutionNote": (
            "风险点与设备已匹配。2026-04-04 批量治理复核：当前候选仍需人工确认字段语义与业务归属，"
            "暂不转正，继续保留 pending。"
        ),
    }


def build_manual_review_export_records(items: Sequence[Dict[str, object]]) -> List[Dict[str, object]]:
    records: List[Dict[str, object]] = []
    for item in items:
        policy = determine_manual_review_policy(item)
        candidate_entries = item.get("candidateMetrics") or []
        metric_ids = [entry["metricIdentifier"] for entry in candidate_entries if normalize_text(entry.get("metricIdentifier"))]
        metric_names = [entry["metricName"] for entry in candidate_entries if normalize_text(entry.get("metricName"))]
        records.append(
            {
                "pending_id": int(item["pendingId"]),
                "group": policy["group"],
                "risk_point_name": normalize_text(item.get("riskPointName")),
                "device_name": normalize_text(item.get("deviceName")),
                "device_code": normalize_text(item.get("deviceCode")),
                "product_name": normalize_text(item.get("productName")),
                "resolution_status": normalize_text(item.get("resolutionStatus")),
                "candidate_metric_ids": ", ".join(metric_ids),
                "candidate_metric_names": ", ".join(metric_names),
                "reason": normalize_text(item.get("reason")),
                "suggested_action": policy["suggestedAction"],
                "system_recommendation": policy["systemRecommendation"],
                "todo": policy["todo"],
                "business_decision": "",
                "canonical_metrics": "",
                "owner": "",
                "due_date": "",
                "notes": "",
            }
        )
    return records


def build_manual_review_markdown(records: Sequence[Dict[str, object]]) -> str:
    lines = [
        "# 风险点 Pending 人工复核导出",
        "",
        f"生成时间：{datetime.now().isoformat(timespec='seconds')}",
        "",
        "## 分组概览",
        "",
    ]
    for group, count in Counter(record["group"] for record in records).items():
        lines.append(f"- {group}：{count} 条")
    lines.extend(
        [
            "",
            "## 明细",
            "",
            "| pendingId | 分组 | 风险点 | 设备 | 产品 | 候选测点 | 建议动作 | 需核对事项 |",
            "|---|---|---|---|---|---|---|---|",
        ]
    )
    for record in records:
        lines.append(
            f"| {record['pending_id']} | {record['group']} | {record['risk_point_name'] or ''} | "
            f"{record['device_name'] or ''} (`{record['device_code'] or ''}`) | {record['product_name'] or ''} | "
            f"{record['candidate_metric_ids'] or ''} | {record['suggested_action'] or ''} | {record['todo'] or ''} |"
        )
    lines.append("")
    return "\n".join(lines)


def write_manual_review_exports(output_prefix: str | Path, records: Sequence[Dict[str, object]]) -> Dict[str, Path]:
    prefix = Path(output_prefix)
    json_path = Path(f"{prefix}.json")
    csv_path = Path(f"{prefix}.csv")
    md_path = Path(f"{prefix}.md")
    write_json(json_path, list(records))
    fieldnames = [
        "pending_id",
        "group",
        "risk_point_name",
        "device_name",
        "device_code",
        "product_name",
        "resolution_status",
        "candidate_metric_ids",
        "candidate_metric_names",
        "reason",
        "suggested_action",
        "system_recommendation",
        "todo",
        "business_decision",
        "canonical_metrics",
        "owner",
        "due_date",
        "notes",
    ]
    with csv_path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(records)
    md_path.write_text(build_manual_review_markdown(records), encoding="utf-8")
    return {"json": json_path, "csv": csv_path, "md": md_path}


def build_manual_review_annotation_rows(items: Sequence[Dict[str, object]]) -> List[Dict[str, object]]:
    rows: List[Dict[str, object]] = []
    for item in items:
        policy = determine_manual_review_policy(item)
        rows.append(
            {
                "pendingId": int(item["pendingId"]),
                "deviceName": normalize_text(item.get("deviceName")),
                "group": policy["group"],
                "resolutionNote": policy["resolutionNote"],
            }
        )
    return rows


def read_manual_review_decision_csv(path: str | Path) -> List[Dict[str, object]]:
    rows: List[Dict[str, object]] = []
    with Path(path).open("r", encoding="utf-8-sig", newline="") as file:
        reader = csv.DictReader(file)
        for raw in reader:
            pending_id_text = normalize_text((raw or {}).get("pending_id"))
            if not pending_id_text:
                continue
            business_decision = (normalize_text((raw or {}).get("business_decision")) or "").upper()
            canonical_metrics_text = normalize_text((raw or {}).get("canonical_metrics")) or ""
            canonical_metrics = [piece.strip() for piece in canonical_metrics_text.split(",") if piece.strip()]
            rows.append(
                {
                    "pendingId": int(pending_id_text),
                    "businessDecision": business_decision,
                    "canonicalMetrics": canonical_metrics,
                    "notes": normalize_text((raw or {}).get("notes")) or "",
                    "owner": normalize_text((raw or {}).get("owner")) or "",
                    "dueDate": normalize_text((raw or {}).get("due_date")) or "",
                    "raw": raw or {},
                }
            )
    return rows


def validate_manual_review_decision_rows(
    rows: Sequence[Dict[str, object]],
    *,
    allowed_pending_ids: set[int] | None = None,
) -> None:
    for row in rows:
        pending_id = int(row["pendingId"])
        business_decision = normalize_text(row.get("businessDecision")) or ""
        canonical_metrics = row.get("canonicalMetrics") or []
        if allowed_pending_ids is not None and pending_id not in allowed_pending_ids:
            raise ValueError(f"pendingId={pending_id} 不在当前 manual_review manifest 内")
        if not business_decision:
            continue
        if business_decision not in {"PROMOTE", "IGNORE", "KEEP_PENDING"}:
            raise ValueError(f"pendingId={pending_id} business_decision 非法: {business_decision}")
        if business_decision == "PROMOTE" and not canonical_metrics:
            raise ValueError(f"pendingId={pending_id} PROMOTE 必须填写 canonical_metrics")
        if business_decision in {"IGNORE", "KEEP_PENDING"} and canonical_metrics:
            raise ValueError(f"pendingId={pending_id} {business_decision} 不得填写 canonical_metrics")


def build_keep_pending_resolution_note(notes: str | None) -> str:
    normalized_notes = normalize_text(notes)
    if normalized_notes:
        return f"业务确认继续保留 pending。{normalized_notes}"
    return "业务确认继续保留 pending，待后续补充字段语义、产品归属或运行期证据。"


def build_manual_review_decision_actions(rows: Sequence[Dict[str, object]]) -> List[Dict[str, object]]:
    actions: List[Dict[str, object]] = []
    for row in rows:
        business_decision = normalize_text(row.get("businessDecision")) or ""
        if not business_decision:
            actions.append(
                {
                    "pendingId": int(row["pendingId"]),
                    "businessDecision": "",
                    "status": "SKIPPED",
                    "skipReason": "business_decision 为空",
                }
            )
            continue
        if business_decision == "PROMOTE":
            metrics = [
                {"metricIdentifier": metric_identifier, "metricName": metric_identifier}
                for metric_identifier in (row.get("canonicalMetrics") or [])
            ]
            note = normalize_text(row.get("notes")) or "业务确认 CSV 批量执行：按规范测点转正"
            actions.append(
                {
                    "pendingId": int(row["pendingId"]),
                    "businessDecision": business_decision,
                    "requestBody": {
                        "metrics": metrics,
                        "completePending": True,
                        "promotionNote": note,
                    },
                }
            )
            continue
        if business_decision == "IGNORE":
            note = normalize_text(row.get("notes")) or "业务确认 CSV 批量执行：确认排除"
            actions.append(
                {
                    "pendingId": int(row["pendingId"]),
                    "businessDecision": business_decision,
                    "requestBody": {"ignoreNote": note},
                }
            )
            continue
        actions.append(
            {
                "pendingId": int(row["pendingId"]),
                "businessDecision": business_decision,
                "resolutionNote": build_keep_pending_resolution_note(normalize_text(row.get("notes"))),
            }
        )
    return actions


def execute_manual_review_decision_actions(
    *,
    base_url: str,
    username: str,
    password: str,
    connection_args: Dict[str, object] | None,
    actions: Sequence[Dict[str, object]],
    apply_mode: bool,
    update_by: int,
) -> Dict[str, object]:
    actionable = [action for action in actions if normalize_text(action.get("businessDecision"))]
    token = None
    requires_api = any(action["businessDecision"] in {"PROMOTE", "IGNORE"} for action in actionable)
    if apply_mode and requires_api:
        token = login(base_url, username, password)

    connection = None
    if apply_mode and any(action["businessDecision"] == "KEEP_PENDING" for action in actionable):
        if connection_args is None:
            raise RuntimeError("KEEP_PENDING apply 需要数据库连接参数")
        connection = pymysql.connect(**connection_args)

    results: List[Dict[str, object]] = []
    try:
        cursor = connection.cursor() if connection is not None else None
        for action in actions:
            pending_id = int(action["pendingId"])
            business_decision = normalize_text(action.get("businessDecision")) or ""
            if not business_decision:
                result = {
                    "pendingId": pending_id,
                    "businessDecision": "",
                    "dryRun": not apply_mode,
                    "status": "SKIPPED",
                    "skipReason": action.get("skipReason") or "business_decision 为空",
                }
                results.append(result)
                print(
                    f"apply-manual-review-decisions pendingId={pending_id} businessDecision=EMPTY "
                    f"dryRun={not apply_mode} status=SKIPPED"
                )
                continue

            result = {
                "pendingId": pending_id,
                "businessDecision": business_decision,
                "dryRun": not apply_mode,
            }
            try:
                if not apply_mode:
                    if business_decision in {"PROMOTE", "IGNORE"}:
                        result["requestBody"] = action["requestBody"]
                    else:
                        result["resolutionNotePreview"] = action["resolutionNote"]
                    result["status"] = "DRY_RUN"
                elif business_decision == "PROMOTE":
                    response = api_request(
                        base_url,
                        f"/api/risk-point/pending-bindings/{pending_id}/promote",
                        method="POST",
                        token=token,
                        body=action["requestBody"],
                        timeout=30,
                    )
                    result["requestBody"] = action["requestBody"]
                    result["response"] = response
                    result["status"] = "APPLIED"
                elif business_decision == "IGNORE":
                    response = api_request(
                        base_url,
                        f"/api/risk-point/pending-bindings/{pending_id}/ignore",
                        method="POST",
                        token=token,
                        body=action["requestBody"],
                        timeout=30,
                    )
                    result["requestBody"] = action["requestBody"]
                    result["response"] = response
                    result["status"] = "APPLIED"
                else:
                    assert cursor is not None
                    cursor.execute(
                        """
                        update risk_point_device_pending_binding
                        set resolution_note = %s,
                            update_by = %s,
                            update_time = now()
                        where id = %s
                          and deleted = 0
                          and resolution_status in (%s, %s)
                        """,
                        (action["resolutionNote"], update_by, pending_id, *PENDING_STATUSES),
                    )
                    result["resolutionNotePreview"] = action["resolutionNote"]
                    result["status"] = "APPLIED" if cursor.rowcount == 1 else "SKIPPED"
                results.append(result)
                print(
                    f"apply-manual-review-decisions pendingId={pending_id} businessDecision={business_decision} "
                    f"dryRun={not apply_mode} status={result['status']}"
                )
            except Exception as exc:
                result["status"] = "ERROR"
                result["error"] = str(exc)
                results.append(result)
                print(
                    f"apply-manual-review-decisions pendingId={pending_id} businessDecision={business_decision} "
                    f"dryRun={not apply_mode} status=ERROR error={exc}"
                )
        if connection is not None:
            connection.commit()
    except Exception:
        if connection is not None:
            connection.rollback()
        raise
    finally:
        if connection is not None:
            connection.close()

    return {
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "mode": "apply-manual-review-decisions",
        "apply": apply_mode,
        "summary": dict(Counter(result["status"] for result in results)),
        "results": results,
    }


def execute_manual_review_annotations(
    *,
    connection_args: Dict[str, object] | None,
    items: Sequence[Dict[str, object]],
    apply_mode: bool,
    update_by: int,
) -> Dict[str, object]:
    annotation_rows = build_manual_review_annotation_rows(items)
    results: List[Dict[str, object]] = []
    if not apply_mode:
        for row in annotation_rows:
            result = {
                "pendingId": row["pendingId"],
                "deviceName": row.get("deviceName"),
                "group": row["group"],
                "dryRun": True,
                "status": "DRY_RUN",
                "resolutionNote": row["resolutionNote"],
            }
            results.append(result)
            print(
                f"annotate-manual-review pendingId={row['pendingId']} dryRun=True "
                f"status=DRY_RUN note={row['resolutionNote']}"
            )
        return {
            "generatedAt": datetime.now().isoformat(timespec="seconds"),
            "mode": "annotate-manual-review",
            "apply": False,
            "summary": {"DRY_RUN": len(results)},
            "results": results,
        }

    if connection_args is None:
        raise RuntimeError("Manual review annotation apply requires connection args")

    connection = pymysql.connect(**connection_args)
    try:
        with connection.cursor() as cursor:
            for row in annotation_rows:
                cursor.execute(
                    """
                    update risk_point_device_pending_binding
                    set resolution_note = %s,
                        update_by = %s,
                        update_time = now()
                    where id = %s
                      and deleted = 0
                      and resolution_status in (%s, %s)
                    """,
                    (row["resolutionNote"], update_by, row["pendingId"], *PENDING_STATUSES),
                )
                status = "APPLIED" if cursor.rowcount == 1 else "SKIPPED"
                result = {
                    "pendingId": row["pendingId"],
                    "deviceName": row.get("deviceName"),
                    "group": row["group"],
                    "dryRun": False,
                    "status": status,
                    "resolutionNote": row["resolutionNote"],
                }
                results.append(result)
                print(
                    f"annotate-manual-review pendingId={row['pendingId']} dryRun=False "
                    f"status={status} note={row['resolutionNote']}"
                )
        connection.commit()
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()

    return {
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "mode": "annotate-manual-review",
        "apply": True,
        "summary": dict(Counter(result["status"] for result in results)),
        "results": results,
    }


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

    export_manual_review = subparsers.add_parser(
        "export-manual-review",
        help="Export manual-review items to JSON/CSV/Markdown confirmation templates.",
    )
    export_manual_review.add_argument("--manifest", required=True, help="Path to the manifest JSON file.")
    export_manual_review.add_argument("--pending-ids", help="Comma-separated pending ids to export.")
    export_manual_review.add_argument("--limit", type=int, help="Only export the first N matching items.")
    export_manual_review.add_argument("--output-prefix", help="Output file prefix without extension.")

    annotate_manual_review = subparsers.add_parser(
        "annotate-manual-review",
        help="Dry-run or apply grouped resolution_note updates for manual-review items.",
    )
    annotate_manual_review.add_argument("--manifest", required=True, help="Path to the manifest JSON file.")
    annotate_manual_review.add_argument("--pending-ids", help="Comma-separated pending ids to annotate.")
    annotate_manual_review.add_argument("--limit", type=int, help="Only annotate the first N matching items.")
    annotate_manual_review.add_argument("--apply", action="store_true", help="Actually update resolution_note in MySQL.")
    annotate_manual_review.add_argument("--result-output", help="Write execution result JSON to this file.")
    annotate_manual_review.add_argument("--update-by", type=int, default=1, help="Value to write into update_by when applying.")
    add_connection_arguments(annotate_manual_review)

    apply_manual_review_decisions = subparsers.add_parser(
        "apply-manual-review-decisions",
        help="Dry-run or apply PROMOTE/IGNORE/KEEP_PENDING decisions from a filled manual-review CSV.",
    )
    apply_manual_review_decisions.add_argument("--csv", required=True, help="Path to the filled decision CSV.")
    apply_manual_review_decisions.add_argument("--manifest", help="Optional manifest JSON for pending id cross-check.")
    apply_manual_review_decisions.add_argument("--pending-ids", help="Comma-separated pending ids to execute.")
    apply_manual_review_decisions.add_argument("--limit", type=int, help="Only execute the first N matching rows.")
    apply_manual_review_decisions.add_argument("--apply", action="store_true", help="Actually execute decisions in the real environment.")
    apply_manual_review_decisions.add_argument("--result-output", help="Write execution result JSON to this file.")
    apply_manual_review_decisions.add_argument("--update-by", type=int, default=1, help="Value to write into update_by when applying KEEP_PENDING.")
    add_login_arguments(apply_manual_review_decisions)
    add_connection_arguments(apply_manual_review_decisions)

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


def run_export_manual_review(args: argparse.Namespace) -> int:
    manifest = read_manifest(args.manifest)
    items = select_items(
        manifest.get("manual_review", []),
        pending_ids=parse_pending_ids(args.pending_ids),
        limit=args.limit,
    )
    records = build_manual_review_export_records(items)
    output_prefix = Path(args.output_prefix) if args.output_prefix else default_output_prefix(
        "risk-point-pending-manual-review"
    )
    files = write_manual_review_exports(output_prefix, records)
    print(f"export-manual-review selectedCount={len(records)}")
    for format_name, path in files.items():
        print(f"{format_name}File={path}")
    return 0


def run_annotate_manual_review(args: argparse.Namespace) -> int:
    manifest = read_manifest(args.manifest)
    items = select_items(
        manifest.get("manual_review", []),
        pending_ids=parse_pending_ids(args.pending_ids),
        limit=args.limit,
    )
    if not items:
        print("annotate-manual-review selectedCount=0")
        return 0
    connection_args = None
    if args.apply:
        connection_args = resolve_connection_args(args.jdbc_url, args.user, args.password)
    result = execute_manual_review_annotations(
        connection_args=connection_args,
        items=items,
        apply_mode=bool(args.apply),
        update_by=int(args.update_by),
    )
    summary_text = " ".join(f"{key}={value}" for key, value in sorted(result["summary"].items()))
    print(f"annotate-manual-review summary dryRun={not bool(args.apply)} {summary_text}")
    if args.result_output:
        write_json(args.result_output, result)
        print(f"resultFile={args.result_output}")
    return 0


def run_apply_manual_review_decisions(args: argparse.Namespace) -> int:
    rows = read_manual_review_decision_csv(args.csv)
    rows = select_items(rows, pending_ids=parse_pending_ids(args.pending_ids), limit=args.limit)
    allowed_pending_ids = None
    if args.manifest:
        manifest = read_manifest(args.manifest)
        allowed_pending_ids = {int(item["pendingId"]) for item in manifest.get("manual_review", [])}
    validate_manual_review_decision_rows(rows, allowed_pending_ids=allowed_pending_ids)
    actions = build_manual_review_decision_actions(rows)
    connection_args = None
    if args.apply and any(action.get("businessDecision") == "KEEP_PENDING" for action in actions):
        connection_args = resolve_connection_args(args.jdbc_url, args.user, args.password)
    result = execute_manual_review_decision_actions(
        base_url=args.base_url,
        username=args.login_username,
        password=args.login_password,
        connection_args=connection_args,
        actions=actions,
        apply_mode=bool(args.apply),
        update_by=int(args.update_by),
    )
    summary_text = " ".join(f"{key}={value}" for key, value in sorted(result["summary"].items()))
    print(f"apply-manual-review-decisions summary dryRun={not bool(args.apply)} {summary_text}")
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
    if args.command == "export-manual-review":
        return run_export_manual_review(args)
    if args.command == "annotate-manual-review":
        return run_annotate_manual_review(args)
    if args.command == "apply-manual-review-decisions":
        return run_apply_manual_review_decisions(args)
    raise RuntimeError(f"Unsupported command: {args.command}")


if __name__ == "__main__":
    sys.exit(main())
