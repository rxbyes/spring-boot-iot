#!/usr/bin/env python3
"""Read-only real-environment verification for layered threshold policies."""

from __future__ import annotations

import argparse
import json
import os
from datetime import datetime
from pathlib import Path
from typing import Any, Sequence

import pymysql


REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OUTPUT_DIR = REPO_ROOT / "logs" / "acceptance"

EXPECTED_COLUMNS = {
    "rule_scope": {"nullable": "NO"},
    "product_type": {"nullable": "YES"},
    "product_id": {"nullable": "YES"},
    "device_id": {"nullable": "YES"},
    "risk_point_device_id": {"nullable": "YES"},
}

EXPECTED_INDEXES = {
    "idx_rule_definition_product_type_metric": "product_type,risk_metric_id,metric_identifier",
    "idx_rule_definition_product_metric": "product_id,risk_metric_id,metric_identifier",
    "idx_rule_definition_device_metric": "device_id,risk_metric_id,metric_identifier",
    "idx_rule_definition_binding_metric": "risk_point_device_id,risk_metric_id,metric_identifier",
}

POLICY_PRECEDENCE = [
    "BINDING",
    "DEVICE",
    "PRODUCT",
    "PRODUCT_TYPE",
    "METRIC",
    "YAML_AUTO_CLOSURE",
]


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Verify layered threshold policy readiness in the real environment.")
    parser.add_argument("--host", default=os.getenv("IOT_MYSQL_HOST", "8.130.107.120"))
    parser.add_argument("--port", type=int, default=int(os.getenv("IOT_MYSQL_PORT", "3306")))
    parser.add_argument("--db", default=os.getenv("IOT_MYSQL_DB", "rm_iot"))
    parser.add_argument("--user", default=os.getenv("IOT_MYSQL_USERNAME", "root"))
    parser.add_argument("--password", default=os.getenv("IOT_MYSQL_PASSWORD", "mI8%pB1*gD"))
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT_DIR))
    parser.add_argument("--fail-on-breaches", action="store_true")
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
    )


def fetch_schema_summary(cur) -> dict[str, Any]:
    cur.execute(
        """
        SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'rule_definition'
          AND COLUMN_NAME IN ('rule_scope','product_type','product_id','device_id','risk_point_device_id')
        """
    )
    columns_by_name = {row["COLUMN_NAME"]: row for row in cur.fetchall()}

    cur.execute(
        """
        SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'rule_definition'
          AND INDEX_NAME IN (
            'idx_rule_definition_product_type_metric',
            'idx_rule_definition_product_metric',
            'idx_rule_definition_device_metric',
            'idx_rule_definition_binding_metric'
          )
        GROUP BY INDEX_NAME
        """
    )
    indexes_by_name = {row["INDEX_NAME"]: row["index_columns"] for row in cur.fetchall()}

    columns = []
    for name, expected in EXPECTED_COLUMNS.items():
        actual = columns_by_name.get(name)
        columns.append(
            {
                "name": name,
                "exists": actual is not None,
                "columnType": actual["COLUMN_TYPE"] if actual else None,
                "nullable": actual["IS_NULLABLE"] if actual else None,
                "expectedNullable": expected["nullable"],
                "matched": actual is not None and actual["IS_NULLABLE"] == expected["nullable"],
            }
        )

    indexes = []
    for name, expected_columns in EXPECTED_INDEXES.items():
        actual_columns = indexes_by_name.get(name)
        indexes.append(
            {
                "name": name,
                "exists": actual_columns is not None,
                "columns": actual_columns,
                "expectedColumns": expected_columns,
                "matched": actual_columns == expected_columns,
            }
        )

    return {"columns": columns, "indexes": indexes}


def fetch_policy_distribution(cur) -> dict[str, Any]:
    cur.execute("SELECT COUNT(*) AS total FROM rule_definition WHERE deleted = 0")
    active_total = int(cur.fetchone()["total"] or 0)
    cur.execute(
        """
        SELECT COALESCE(rule_scope, 'METRIC') AS ruleScope,
               COALESCE(product_type, '') AS productType,
               COUNT(*) AS ruleCount
        FROM rule_definition
        WHERE deleted = 0
        GROUP BY COALESCE(rule_scope, 'METRIC'), COALESCE(product_type, '')
        ORDER BY ruleScope, productType
        """
    )
    return {"activeRuleCount": active_total, "scopeCounts": list(cur.fetchall())}


def fetch_duplicate_groups(cur) -> list[dict[str, Any]]:
    cur.execute(
        """
        SELECT COALESCE(rule_scope, 'METRIC') AS ruleScope,
               COALESCE(product_type, '') AS productType,
               COALESCE(product_id, 0) AS productId,
               COALESCE(device_id, 0) AS deviceId,
               COALESCE(risk_point_device_id, 0) AS riskPointDeviceId,
               metric_identifier AS metricIdentifier,
               COUNT(*) AS duplicateCount
        FROM rule_definition
        WHERE deleted = 0
        GROUP BY COALESCE(rule_scope, 'METRIC'),
                 COALESCE(product_type, ''),
                 COALESCE(product_id, 0),
                 COALESCE(device_id, 0),
                 COALESCE(risk_point_device_id, 0),
                 metric_identifier
        HAVING COUNT(*) > 1
        ORDER BY duplicateCount DESC, ruleScope, metricIdentifier
        LIMIT 50
        """
    )
    return list(cur.fetchall())


def evaluate_report(report: dict[str, Any]) -> dict[str, Any]:
    breaches = []
    for column in report["schema"]["columns"]:
        if not column["matched"]:
            breaches.append({"type": "COLUMN_DRIFT", "target": column["name"]})
    for index in report["schema"]["indexes"]:
        if not index["matched"]:
            breaches.append({"type": "INDEX_DRIFT", "target": index["name"]})
    if report["duplicates"]:
        breaches.append({"type": "DUPLICATE_POLICY_GROUPS", "target": len(report["duplicates"])})
    return {
        "status": "PASSED" if not breaches else "FAILED",
        "breachCount": len(breaches),
        "breaches": breaches,
    }


def build_report(args: argparse.Namespace) -> dict[str, Any]:
    with connect(args) as conn:
        with conn.cursor() as cur:
            report = {
                "checkedAt": datetime.now().isoformat(timespec="seconds"),
                "database": {
                    "host": args.host,
                    "port": args.port,
                    "db": args.db,
                    "user": args.user,
                },
                "policyPrecedence": POLICY_PRECEDENCE,
                "schema": fetch_schema_summary(cur),
                "policyDistribution": fetch_policy_distribution(cur),
                "duplicates": fetch_duplicate_groups(cur),
            }
    report["evaluation"] = evaluate_report(report)
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
    schema = report["schema"]
    distribution = report["policyDistribution"]
    lines = [
        "# Threshold Policy Real Environment Verification",
        "",
        f"- Status: `{evaluation['status']}`",
        f"- Checked At: `{report['checkedAt']}`",
        f"- Database: `{report['database']['host']}:{report['database']['port']}/{report['database']['db']}`",
        f"- Active Rules: `{distribution['activeRuleCount']}`",
        f"- Duplicate Groups: `{len(report['duplicates'])}`",
        f"- Policy Precedence: `{' > '.join(report['policyPrecedence'])}`",
        "",
        "## Schema Columns",
        markdown_table(
            ["Column", "Type", "Nullable", "Matched"],
            [
                [item["name"], item.get("columnType") or "--", item.get("nullable") or "--", item["matched"]]
                for item in schema["columns"]
            ],
        ),
        "",
        "## Schema Indexes",
        markdown_table(
            ["Index", "Columns", "Matched"],
            [[item["name"], item.get("columns") or "--", item["matched"]] for item in schema["indexes"]],
        ),
        "",
        "## Policy Distribution",
        markdown_table(
            ["Scope", "Product Type", "Count"],
            [
                [item["ruleScope"], item["productType"] or "--", item["ruleCount"]]
                for item in distribution["scopeCounts"]
            ]
            or [["--", "--", 0]],
        ),
        "",
        "## Duplicate Groups",
    ]
    if report["duplicates"]:
        lines.append(
            markdown_table(
                ["Scope", "Product Type", "Product", "Device", "Binding", "Metric", "Count"],
                [
                    [
                        item["ruleScope"],
                        item["productType"] or "--",
                        item["productId"] or "--",
                        item["deviceId"] or "--",
                        item["riskPointDeviceId"] or "--",
                        item["metricIdentifier"],
                        item["duplicateCount"],
                    ]
                    for item in report["duplicates"]
                ],
            )
        )
    else:
        lines.append("No duplicate active policy groups found.")
    lines.append("")
    return "\n".join(lines)


def write_reports(report: dict[str, Any], output_dir: str | Path) -> tuple[Path, Path]:
    target_dir = Path(output_dir)
    target_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    json_path = target_dir / f"threshold-policy-real-env-{timestamp}.json"
    md_path = target_dir / f"threshold-policy-real-env-{timestamp}.md"
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    md_path.write_text(render_markdown(report), encoding="utf-8")
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
        f"threshold policy real env {report['evaluation']['status'].lower()}, "
        f"duplicates={len(report['duplicates'])}"
    )
    print(f"[status] {report['evaluation']['status']}")
    if args.fail_on_breaches and report["evaluation"]["status"] != "PASSED":
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
