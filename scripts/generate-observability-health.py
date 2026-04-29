#!/usr/bin/env python3
"""Generate an observability health report from the real environment."""

from __future__ import annotations

import argparse
import json
import os
import re
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any, Dict

import pymysql


REPO_ROOT = Path(__file__).resolve().parent.parent
APP_DEV_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application-dev.yml"
DEFAULT_POLICY_PATH = REPO_ROOT / "config" / "automation" / "observability-health-policy.json"

RULE_OPERATORS = {
    ">=": lambda value, threshold: value >= threshold,
    "<=": lambda value, threshold: value <= threshold,
    ">": lambda value, threshold: value > threshold,
    "<": lambda value, threshold: value < threshold,
    "==": lambda value, threshold: value == threshold,
}


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate an observability health report via the real environment.")
    parser.add_argument("--hours", type=int, default=24, help="Lookback window in hours. Defaults to 24.")
    parser.add_argument("--jdbc-url", help="Override JDBC url. Defaults to application-dev.yml / env.")
    parser.add_argument("--user", help="Override mysql username. Defaults to application-dev.yml / env.")
    parser.add_argument("--password", help="Override mysql password. Defaults to application-dev.yml / env.")
    parser.add_argument("--json-out", help="Optional JSON output path.")
    parser.add_argument("--md-out", help="Optional Markdown output path.")
    parser.add_argument(
        "--policy-path",
        help="Optional policy JSON path. Defaults to config/automation/observability-health-policy.json when present.",
    )
    parser.add_argument(
        "--fail-on-breaches",
        action="store_true",
        help="Return exit code 1 when the policy evaluation has error-level breaches.",
    )
    return parser.parse_args(argv)


def extract_default(text: str, env_name: str) -> str:
    match = re.search(rf"{env_name}:([^}}]+)}}", text)
    if not match:
        raise RuntimeError(f"Unable to resolve {env_name} from configuration")
    return match.group(1).strip()


def load_dev_defaults() -> Dict[str, str]:
    dev_text = APP_DEV_PATH.read_text(encoding="utf-8")
    return {
        "jdbc_url": extract_default(dev_text, "IOT_MYSQL_URL"),
        "user": extract_default(dev_text, "IOT_MYSQL_USERNAME"),
        "password": extract_default(dev_text, "IOT_MYSQL_PASSWORD"),
    }


def resolve_runtime_args(args: argparse.Namespace) -> Dict[str, str]:
    defaults = load_dev_defaults()
    jdbc_url = args.jdbc_url or os.getenv("IOT_MYSQL_URL") or defaults["jdbc_url"]
    user = args.user or os.getenv("IOT_MYSQL_USERNAME") or defaults["user"]
    password = args.password or os.getenv("IOT_MYSQL_PASSWORD") or defaults["password"]

    parsed = re.match(r"jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)", jdbc_url or "")
    if not parsed:
        raise RuntimeError(f"Invalid jdbc url: {jdbc_url}")
    host, port, database = parsed.group(1), parsed.group(2) or "3306", parsed.group(3)
    return {
        "db_host": host,
        "db_port": port,
        "db_name": database,
        "db_user": user,
        "db_password": password,
    }


def open_db(runtime: Dict[str, str]) -> pymysql.connections.Connection:
    return pymysql.connect(
        host=runtime["db_host"],
        port=int(runtime["db_port"]),
        user=runtime["db_user"],
        password=runtime["db_password"],
        database=runtime["db_name"],
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        connect_timeout=10,
        read_timeout=30,
        write_timeout=30,
    )


def fetch_row(cur: pymysql.cursors.Cursor, sql: str, params: tuple[Any, ...]) -> Dict[str, Any]:
    cur.execute(sql, params)
    return cur.fetchone() or {}


def count_distinct_join(
    cur: pymysql.cursors.Cursor,
    left_sql: str,
    right_sql: str,
    left_params: tuple[Any, ...],
    right_params: tuple[Any, ...],
    join_type: str = "INNER JOIN",
) -> int:
    sql = f"""
        SELECT COUNT(*) AS total
        FROM ({left_sql}) left_trace
        {join_type} ({right_sql}) right_trace
          ON right_trace.trace_id = left_trace.trace_id
    """
    row = fetch_row(cur, sql, left_params + right_params)
    return int(row.get("total") or 0)


def count_union_distinct(
    cur: pymysql.cursors.Cursor,
    first_sql: str,
    second_sql: str,
    first_params: tuple[Any, ...],
    second_params: tuple[Any, ...],
) -> int:
    sql = f"""
        SELECT COUNT(*) AS total
        FROM (
            {first_sql}
            UNION
            {second_sql}
        ) union_trace
    """
    row = fetch_row(cur, sql, first_params + second_params)
    return int(row.get("total") or 0)


def normalize_json_text(value: Any) -> str | None:
    text = "" if value is None else str(value).strip()
    return text or None


def safe_ratio(numerator: int, denominator: int) -> float | None:
    if denominator <= 0:
        return None
    return round(numerator / denominator, 6)


def make_ratio_metric(metric_id: str, label: str, numerator: int, denominator: int, note: str | None = None) -> Dict[str, Any]:
    value = safe_ratio(numerator, denominator)
    return {
        "metricId": metric_id,
        "label": label,
        "kind": "ratio",
        "numerator": numerator,
        "denominator": denominator,
        "value": value,
        "percentage": None if value is None else round(value * 100, 2),
        "formatted": "n/a" if value is None else f"{value * 100:.2f}%",
        "note": note,
    }


def collect_observability_snapshot(conn: pymysql.connections.Connection, hours: int) -> Dict[str, Any]:
    window_end = datetime.now()
    window_start = window_end - timedelta(hours=max(hours, 1))
    params = (window_start,)

    with conn.cursor() as cur:
        span_row = fetch_row(
            cur,
            """
            SELECT COUNT(*) AS total,
                   SUM(CASE WHEN COALESCE(trace_id, '') <> '' THEN 1 ELSE 0 END) AS traced,
                   SUM(CASE WHEN tenant_id IS NULL THEN 1 ELSE 0 END) AS missing_tenant
            FROM sys_observability_span_log
            WHERE deleted = 0 AND started_at >= %s
            """,
            params,
        )
        event_row = fetch_row(
            cur,
            """
            SELECT COUNT(*) AS total,
                   SUM(CASE WHEN COALESCE(trace_id, '') <> '' THEN 1 ELSE 0 END) AS traced,
                   SUM(CASE WHEN tenant_id IS NULL THEN 1 ELSE 0 END) AS missing_tenant
            FROM sys_business_event_log
            WHERE deleted = 0 AND occurred_at >= %s
            """,
            params,
        )
        slow_sql_row = fetch_row(
            cur,
            """
            SELECT COUNT(*) AS total,
                   SUM(CASE WHEN COALESCE(trace_id, '') <> '' THEN 1 ELSE 0 END) AS traced
            FROM sys_observability_span_log
            WHERE deleted = 0 AND started_at >= %s AND span_type = 'SLOW_SQL'
            """,
            params,
        )
        message_flow_tag_row = fetch_row(
            cur,
            """
            SELECT COUNT(*) AS total,
                   SUM(
                       CASE
                           WHEN COALESCE(JSON_UNQUOTE(JSON_EXTRACT(tags_json, '$.deviceCode')), '') <> '' THEN 1
                           ELSE 0
                       END
                   ) AS with_device_code,
                   SUM(
                       CASE
                           WHEN COALESCE(JSON_UNQUOTE(JSON_EXTRACT(tags_json, '$.productKey')), '') <> '' THEN 1
                           ELSE 0
                       END
                   ) AS with_product_key
            FROM sys_observability_span_log
            WHERE deleted = 0 AND started_at >= %s AND span_type = 'MESSAGE_FLOW'
            """,
            params,
        )

        trace_from_spans_sql = """
            SELECT DISTINCT trace_id
            FROM sys_observability_span_log
            WHERE deleted = 0 AND started_at >= %s AND COALESCE(trace_id, '') <> ''
        """
        trace_from_events_sql = """
            SELECT DISTINCT trace_id
            FROM sys_business_event_log
            WHERE deleted = 0 AND occurred_at >= %s AND COALESCE(trace_id, '') <> ''
        """
        audit_trace_sql = """
            SELECT DISTINCT trace_id
            FROM sys_audit_log
            WHERE deleted = 0
              AND operation_time >= %s
              AND request_url LIKE '/api/%'
              AND COALESCE(trace_id, '') <> ''
        """
        http_span_trace_sql = """
            SELECT DISTINCT trace_id
            FROM sys_observability_span_log
            WHERE deleted = 0
              AND started_at >= %s
              AND span_type = 'HTTP_REQUEST'
              AND COALESCE(trace_id, '') <> ''
        """
        message_log_trace_sql = """
            SELECT DISTINCT trace_id
            FROM iot_message_log
            WHERE report_time >= %s AND COALESCE(trace_id, '') <> ''
        """
        message_flow_trace_sql = """
            SELECT DISTINCT trace_id
            FROM sys_observability_span_log
            WHERE deleted = 0
              AND started_at >= %s
              AND span_type = 'MESSAGE_FLOW'
              AND COALESCE(trace_id, '') <> ''
        """
        scheduled_event_trace_sql = """
            SELECT DISTINCT trace_id
            FROM sys_business_event_log
            WHERE deleted = 0
              AND occurred_at >= %s
              AND event_code = 'platform.scheduled.failure'
              AND COALESCE(trace_id, '') <> ''
        """
        scheduled_span_trace_sql = """
            SELECT DISTINCT trace_id
            FROM sys_observability_span_log
            WHERE deleted = 0
              AND started_at >= %s
              AND span_type = 'SCHEDULED_TASK'
              AND event_code = 'platform.scheduled.failure'
              AND COALESCE(trace_id, '') <> ''
        """

        span_trace_total = count_distinct_join(
            cur,
            trace_from_spans_sql,
            trace_from_spans_sql,
            params,
            params,
        )
        event_trace_total = count_distinct_join(
            cur,
            trace_from_events_sql,
            trace_from_events_sql,
            params,
            params,
        )
        orphan_span_traces = count_distinct_join(
            cur,
            trace_from_spans_sql,
            trace_from_events_sql,
            params,
            params,
            join_type="LEFT JOIN",
        ) - count_distinct_join(
            cur,
            trace_from_spans_sql,
            trace_from_events_sql,
            params,
            params,
        )
        timeline_ready_traces = count_distinct_join(
            cur,
            trace_from_spans_sql,
            trace_from_events_sql,
            params,
            params,
        )
        timeline_union_traces = count_union_distinct(
            cur,
            trace_from_spans_sql,
            trace_from_events_sql,
            params,
            params,
        )
        audit_trace_total = count_distinct_join(cur, audit_trace_sql, audit_trace_sql, params, params)
        audit_linked_total = count_distinct_join(cur, audit_trace_sql, http_span_trace_sql, params, params)
        message_trace_total = count_distinct_join(cur, message_log_trace_sql, message_log_trace_sql, params, params)
        message_linked_total = count_distinct_join(cur, message_log_trace_sql, message_flow_trace_sql, params, params)
        scheduled_trace_total = count_distinct_join(cur, scheduled_event_trace_sql, scheduled_event_trace_sql, params, params)
        scheduled_linked_total = count_distinct_join(cur, scheduled_event_trace_sql, scheduled_span_trace_sql, params, params)

    span_total = int(span_row.get("total") or 0)
    span_traced = int(span_row.get("traced") or 0)
    event_total = int(event_row.get("total") or 0)
    event_traced = int(event_row.get("traced") or 0)
    span_missing_tenant = int(span_row.get("missing_tenant") or 0)
    event_missing_tenant = int(event_row.get("missing_tenant") or 0)
    slow_sql_total = int(slow_sql_row.get("total") or 0)
    slow_sql_traced = int(slow_sql_row.get("traced") or 0)
    message_flow_total = int(message_flow_tag_row.get("total") or 0)
    message_flow_with_device = int(message_flow_tag_row.get("with_device_code") or 0)
    message_flow_with_product = int(message_flow_tag_row.get("with_product_key") or 0)

    counters = {
        "spanRows": span_total,
        "businessEventRows": event_total,
        "spanTraceRows": span_traced,
        "businessEventTraceRows": event_traced,
        "spanMissingTenantRows": span_missing_tenant,
        "businessEventMissingTenantRows": event_missing_tenant,
        "spanTraceCount": span_trace_total,
        "businessEventTraceCount": event_trace_total,
        "orphanSpanTraceCount": max(orphan_span_traces, 0),
        "timelineReadyTraceCount": timeline_ready_traces,
        "timelineUnionTraceCount": timeline_union_traces,
        "auditTraceCount": audit_trace_total,
        "auditLinkedTraceCount": audit_linked_total,
        "messageTraceCount": message_trace_total,
        "messageLinkedTraceCount": message_linked_total,
        "scheduledFailureTraceCount": scheduled_trace_total,
        "scheduledLinkedTraceCount": scheduled_linked_total,
        "slowSqlSpanCount": slow_sql_total,
        "slowSqlTracedSpanCount": slow_sql_traced,
        "messageFlowSpanCount": message_flow_total,
        "messageFlowWithDeviceCodeCount": message_flow_with_device,
        "messageFlowWithProductKeyCount": message_flow_with_product,
    }

    metrics = {
        "spanTraceCoverageRate": make_ratio_metric(
            "spanTraceCoverageRate",
            "Span TraceId 覆盖率",
            span_traced,
            span_total,
            "窗口内所有 span 行中具备 TraceId 的比例。",
        ),
        "businessEventTraceCoverageRate": make_ratio_metric(
            "businessEventTraceCoverageRate",
            "业务事件 TraceId 覆盖率",
            event_traced,
            event_total,
            "窗口内业务事件行中具备 TraceId 的比例。",
        ),
        "spanTenantCoverageRate": make_ratio_metric(
            "spanTenantCoverageRate",
            "Span tenantId 覆盖率",
            max(span_total - span_missing_tenant, 0),
            span_total,
            "窗口内 span 行 tenantId 不缺失的比例。",
        ),
        "businessEventTenantCoverageRate": make_ratio_metric(
            "businessEventTenantCoverageRate",
            "业务事件 tenantId 覆盖率",
            max(event_total - event_missing_tenant, 0),
            event_total,
            "窗口内业务事件行 tenantId 不缺失的比例。",
        ),
        "traceWithoutBusinessEventRate": make_ratio_metric(
            "traceWithoutBusinessEventRate",
            "无业务事件 Trace 占比",
            max(orphan_span_traces, 0),
            span_trace_total,
            "窗口内 span Trace 中无法关联任何业务事件的比例。",
        ),
        "evidencePackageReadyRate": make_ratio_metric(
            "evidencePackageReadyRate",
            "证据包双轨就绪率",
            timeline_ready_traces,
            timeline_union_traces,
            "窗口内 Trace 同时具备业务事件与调用片段的比例。",
        ),
        "httpTraceLinkRate": make_ratio_metric(
            "httpTraceLinkRate",
            "HTTP 留痕关联率",
            audit_linked_total,
            audit_trace_total,
            "窗口内 API 审计 Trace 可关联到 HTTP_REQUEST span 的比例。",
        ),
        "messageFlowTraceLinkRate": make_ratio_metric(
            "messageFlowTraceLinkRate",
            "消息链路留痕关联率",
            message_linked_total,
            message_trace_total,
            "窗口内消息日志 Trace 可关联到 MESSAGE_FLOW span 的比例。",
        ),
        "scheduledFailureLinkRate": make_ratio_metric(
            "scheduledFailureLinkRate",
            "调度异常双轨关联率",
            scheduled_linked_total,
            scheduled_trace_total,
            "窗口内调度异常业务事件可关联到 SCHEDULED_TASK span 的比例。",
        ),
        "slowSqlTraceCoverageRate": make_ratio_metric(
            "slowSqlTraceCoverageRate",
            "慢 SQL TraceId 覆盖率",
            slow_sql_traced,
            slow_sql_total,
            "窗口内 SLOW_SQL span 具备 TraceId 的比例。",
        ),
        "messageFlowDeviceCodeCoverageRate": make_ratio_metric(
            "messageFlowDeviceCodeCoverageRate",
            "消息链路 deviceCode 覆盖率",
            message_flow_with_device,
            message_flow_total,
            "窗口内 MESSAGE_FLOW span tags_json 中具备 deviceCode 的比例。",
        ),
        "messageFlowProductKeyCoverageRate": make_ratio_metric(
            "messageFlowProductKeyCoverageRate",
            "消息链路 productKey 覆盖率",
            message_flow_with_product,
            message_flow_total,
            "窗口内 MESSAGE_FLOW span tags_json 中具备 productKey 的比例。",
        ),
    }

    return {
        "generatedAt": window_end.isoformat(timespec="seconds"),
        "window": {
            "hours": max(hours, 1),
            "startedAt": window_start.isoformat(timespec="seconds"),
            "endedAt": window_end.isoformat(timespec="seconds"),
        },
        "summary": {
            "status": "unrated",
            "errors": 0,
            "warnings": 0,
            "counters": counters,
        },
        "metrics": metrics,
    }


def load_policy(policy_path: Path | None) -> Dict[str, Any] | None:
    if policy_path is None or not policy_path.exists():
        return None
    return json.loads(policy_path.read_text(encoding="utf-8"))


def evaluate_policy(report: Dict[str, Any], policy: Dict[str, Any] | None) -> Dict[str, Any] | None:
    if not policy:
        return None
    rules = policy.get("rules")
    if not isinstance(rules, list):
        raise RuntimeError("Observability health policy must contain a rules array.")

    results = []
    error_count = 0
    warning_count = 0
    skipped_count = 0
    metrics = report.get("metrics", {})

    for rule in rules:
        rule_id = str(rule.get("id") or "").strip()
        metric_id = str(rule.get("metric") or "").strip()
        operator = str(rule.get("operator") or "").strip()
        severity = str(rule.get("severity") or "").strip().lower()
        threshold = rule.get("threshold")
        metric = metrics.get(metric_id)
        metric_value = None if not metric else metric.get("value")
        note = str(rule.get("message") or "").strip() or None

        if operator not in RULE_OPERATORS:
            raise RuntimeError(f"Unsupported observability health operator: {operator}")
        if severity not in {"error", "warning"}:
            raise RuntimeError(f"Unsupported observability health severity: {severity}")

        if metric_value is None:
            skipped_count += 1
            results.append(
                {
                    "id": rule_id or metric_id,
                    "metric": metric_id,
                    "severity": severity,
                    "status": "skipped",
                    "message": note or "Metric has no denominator in the current window.",
                    "threshold": threshold,
                    "operator": operator,
                    "actual": None,
                }
            )
            continue

        passed = RULE_OPERATORS[operator](metric_value, threshold)
        if not passed and severity == "error":
            error_count += 1
        if not passed and severity == "warning":
            warning_count += 1
        results.append(
            {
                "id": rule_id or metric_id,
                "metric": metric_id,
                "severity": severity,
                "status": "passed" if passed else "failed",
                "message": note,
                "threshold": threshold,
                "operator": operator,
                "actual": metric_value,
            }
        )

    status = "failed" if error_count > 0 else "warning" if warning_count > 0 else "passed"
    return {
        "status": status,
        "summary": {
            "errors": error_count,
            "warnings": warning_count,
            "skipped": skipped_count,
        },
        "results": results,
    }


def render_markdown(report: Dict[str, Any]) -> str:
    lines = [
        "# 可观测健康报告",
        "",
        f"- 生成时间：{report['generatedAt']}",
        f"- 观察窗口：最近 {report['window']['hours']} 小时",
        f"- 窗口开始：{report['window']['startedAt']}",
        f"- 窗口结束：{report['window']['endedAt']}",
        "",
        "## 概览",
        "",
        f"- 总体状态：{report['summary']['status']}",
        f"- error 数：{report['summary']['errors']}",
        f"- warning 数：{report['summary']['warnings']}",
        "",
        "## 关键计数",
        "",
        "| 指标 | 数值 |",
        "| --- | ---: |",
    ]
    for key, value in report["summary"]["counters"].items():
        lines.append(f"| `{key}` | {value} |")

    lines.extend(
        [
            "",
            "## 健康指标",
            "",
            "| 指标 | 当前值 | 分子 / 分母 | 说明 |",
            "| --- | ---: | --- | --- |",
        ]
    )
    for metric in report["metrics"].values():
        numerator = metric.get("numerator")
        denominator = metric.get("denominator")
        parts = "n/a" if denominator in (None, 0) else f"{numerator} / {denominator}"
        lines.append(
            f"| {metric['label']} | {metric['formatted']} | {parts} | {metric.get('note') or ''} |"
        )

    policy_evaluation = report.get("policyEvaluation")
    if policy_evaluation:
        lines.extend(
            [
                "",
                "## 门禁评估",
                "",
                f"- 状态：{policy_evaluation['status']}",
                f"- error 数：{policy_evaluation['summary']['errors']}",
                f"- warning 数：{policy_evaluation['summary']['warnings']}",
                f"- skipped 数：{policy_evaluation['summary']['skipped']}",
                "",
                "| 规则 | 状态 | severity | 当前值 | 条件 | 说明 |",
                "| --- | --- | --- | ---: | --- | --- |",
            ]
        )
        for result in policy_evaluation["results"]:
            actual = "n/a" if result["actual"] is None else f"{float(result['actual']) * 100:.2f}%"
            threshold = result["threshold"]
            threshold_text = f"{float(threshold) * 100:.2f}%" if threshold is not None else "n/a"
            lines.append(
                f"| `{result['id']}` | {result['status']} | {result['severity']} | {actual} | "
                f"`{result['operator']} {threshold_text}` | {result.get('message') or ''} |"
            )

    return "\n".join(lines) + "\n"


def resolve_output_path(workspace_root: Path, explicit_path: str | None, filename: str) -> Path:
    if explicit_path:
        candidate = Path(explicit_path)
        return candidate if candidate.is_absolute() else workspace_root / candidate
    return workspace_root / "logs" / "acceptance" / filename


def create_timestamp(now: datetime | None = None) -> str:
    target = now or datetime.now()
    return target.strftime("%Y%m%d%H%M%S")


def run_health_cli(argv: list[str] | None = None, workspace_root: Path | None = None) -> Dict[str, Any]:
    args = parse_args(argv)
    root = workspace_root or REPO_ROOT
    runtime = resolve_runtime_args(args)
    connection = open_db(runtime)
    try:
        report = collect_observability_snapshot(connection, args.hours)
    finally:
        connection.close()

    policy_path = Path(args.policy_path) if args.policy_path else DEFAULT_POLICY_PATH
    if not policy_path.is_absolute():
        policy_path = root / policy_path
    policy = load_policy(policy_path if policy_path.exists() else None)
    policy_evaluation = evaluate_policy(report, policy)
    if policy_evaluation:
        report["policyEvaluation"] = policy_evaluation
        report["summary"]["status"] = policy_evaluation["status"]
        report["summary"]["errors"] = policy_evaluation["summary"]["errors"]
        report["summary"]["warnings"] = policy_evaluation["summary"]["warnings"]

    timestamp = create_timestamp()
    json_path = resolve_output_path(root, args.json_out, f"observability-health-{timestamp}.json")
    markdown_path = resolve_output_path(root, args.md_out, f"observability-health-{timestamp}.md")
    json_path.parent.mkdir(parents=True, exist_ok=True)
    markdown_path.parent.mkdir(parents=True, exist_ok=True)
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    markdown_path.write_text(render_markdown(report), encoding="utf-8")

    exit_code = 0
    if args.fail_on_breaches and policy_evaluation and policy_evaluation["summary"]["errors"] > 0:
        exit_code = 1

    return {
        "exitCode": exit_code,
        "jsonPath": str(json_path),
        "markdownPath": str(markdown_path),
        "report": report,
    }


def main() -> None:
    result = run_health_cli()
    print(
        json.dumps(
            {
                "exitCode": result["exitCode"],
                "status": result["report"]["summary"]["status"],
                "errors": result["report"]["summary"]["errors"],
                "warnings": result["report"]["summary"]["warnings"],
                "jsonPath": result["jsonPath"],
                "markdownPath": result["markdownPath"],
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    raise SystemExit(result["exitCode"])


if __name__ == "__main__":
    main()
