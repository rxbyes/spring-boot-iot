#!/usr/bin/env python3
"""Scaffold for resetting product governance derived data."""

from __future__ import annotations

import argparse
import json
import re
from datetime import datetime
from pathlib import Path
from urllib.parse import parse_qs, urlparse


REPO_ROOT = Path(__file__).resolve().parent.parent
APP_DEV_PATH = REPO_ROOT / "spring-boot-iot-admin" / "src" / "main" / "resources" / "application-dev.yml"

DELETE_TABLES = [
    "risk_metric_linkage_binding",
    "risk_metric_emergency_plan_binding",
    "risk_point_device_pending_promotion",
    "risk_point_device_pending_binding",
    "risk_point_device_capability_binding",
    "risk_point_device",
    "risk_metric_catalog",
    "iot_product_metric_resolver_snapshot",
    "iot_product_contract_release_snapshot",
    "iot_product_contract_release_batch",
    "iot_vendor_metric_mapping_rule_snapshot",
    "iot_vendor_metric_mapping_rule",
    "iot_vendor_metric_evidence",
    "iot_product_model",
]

CONDITIONAL_DELETE_TABLES = [
    "rule_definition",
    "sys_governance_approval_transition",
    "sys_governance_approval_order",
    "iot_governance_work_item",
]

KEEP_TABLES = [
    "iot_product",
    "iot_device",
    "iot_device_relation",
]

PRODUCT_CONTRACT_RELEASE_APPLY = "PRODUCT_CONTRACT_RELEASE_APPLY"
PRODUCT_CONTRACT_ROLLBACK = "PRODUCT_CONTRACT_ROLLBACK"
VENDOR_MAPPING_RULE_PUBLISH = "VENDOR_MAPPING_RULE_PUBLISH"
VENDOR_MAPPING_RULE_ROLLBACK = "VENDOR_MAPPING_RULE_ROLLBACK"

GOVERNANCE_ACTION_CODES = (
    PRODUCT_CONTRACT_RELEASE_APPLY,
    PRODUCT_CONTRACT_ROLLBACK,
    VENDOR_MAPPING_RULE_PUBLISH,
    VENDOR_MAPPING_RULE_ROLLBACK,
)

GOVERNANCE_WORK_ITEM_CODES = (
    "PENDING_CONTRACT_RELEASE",
    "PENDING_RISK_BINDING",
)


def build_cleanup_targets() -> dict[str, list[str]]:
    """Return independent copies of the cleanup target lists."""
    return {
        "delete_tables": DELETE_TABLES[:],
        "conditional_delete_tables": CONDITIONAL_DELETE_TABLES[:],
        "keep_tables": KEEP_TABLES[:],
    }


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    """Parse the initial CLI arguments for the reset workflow."""
    parser = argparse.ArgumentParser(description="Reset product governance derived data")
    parser.add_argument("--mode", choices=("dry-run", "backup", "execute"), default="dry-run")
    parser.add_argument("--tenant-id")
    parser.add_argument("--product-ids", nargs="*", default=[])
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--confirm", action="store_true")
    return parser.parse_args(argv)


def extract_default(text: str, env_name: str) -> str:
    """Extract the default value from a Spring placeholder."""
    match = re.search(rf"{env_name}:([^}}]+)}}", text)
    if not match:
        raise RuntimeError(f"Unable to resolve {env_name} from {APP_DEV_PATH}")
    return match.group(1).strip()


def load_dev_defaults() -> dict[str, str]:
    """Read the default MySQL connection settings from application-dev.yml."""
    text = APP_DEV_PATH.read_text(encoding="utf-8")
    return {
        "jdbc_url": extract_default(text, "IOT_MYSQL_URL"),
        "user": extract_default(text, "IOT_MYSQL_USERNAME"),
        "password": extract_default(text, "IOT_MYSQL_PASSWORD"),
    }


def normalize_product_ids(product_ids: list[str]) -> list[str]:
    """Return normalized non-empty product identifiers."""
    return [str(item).strip() for item in product_ids if str(item).strip()]


def scope_params(tenant_id: str | None, product_ids: list[str]) -> tuple[object, ...]:
    """Build the parameter tuple for direct table scope clauses."""
    normalized_product_ids = normalize_product_ids(product_ids)
    if normalized_product_ids:
        return tuple(normalized_product_ids)
    if tenant_id:
        return (tenant_id,)
    return ()


def build_cleanup_plan(
    tenant_id: str | None,
    product_ids: list[str],
    mode: str,
) -> dict[str, object]:
    """Build the dry-run cleanup manifest for later execution stages."""
    normalized_product_ids = normalize_product_ids(product_ids)
    ordered_delete_tables = [
        "risk_metric_linkage_binding",
        "risk_metric_emergency_plan_binding",
        "risk_point_device_pending_promotion",
        "risk_point_device_pending_binding",
        "risk_point_device_capability_binding",
        "risk_point_device",
        "rule_definition",
        "sys_governance_approval_transition",
        "sys_governance_approval_order",
        "iot_governance_work_item",
        "risk_metric_catalog",
        "iot_product_metric_resolver_snapshot",
        "iot_product_contract_release_snapshot",
        "iot_product_contract_release_batch",
        "iot_vendor_metric_mapping_rule_snapshot",
        "iot_vendor_metric_mapping_rule",
        "iot_vendor_metric_evidence",
        "iot_product_model",
    ]
    operations = [
        {
            "table": table,
            "action": "conditional-delete" if table in CONDITIONAL_DELETE_TABLES else "delete",
        }
        for table in ordered_delete_tables
    ]
    operations.append({"table": "iot_product", "action": "reset-metadata"})
    operations.append({"table": "iot_device_onboarding_case", "action": "reset-release-batch"})
    scope = {
        "tenantId": tenant_id,
        "productIds": normalized_product_ids,
        "scopeType": "product_ids" if normalized_product_ids else ("tenant" if tenant_id else "all"),
    }
    operation_previews = []
    for operation in operations:
        sql, params = operation_sql(operation, scope)
        operation_previews.append({
            **operation,
            "sql": sql,
            "params": list(params),
        })
    return {
        "mode": mode,
        "scope": scope,
        "operations": operation_previews,
        "metadata_reset": {
            "table": "iot_product",
            "paths": ["objectInsight.customMetrics", "governance.productCapabilityType"],
        },
    }


def write_backup_manifest(output_dir: Path, plan: dict[str, object]) -> Path:
    """Persist the current cleanup plan as a backup manifest."""
    output_dir.mkdir(parents=True, exist_ok=True)
    manifest_path = output_dir / f"product-governance-reset-{datetime.now():%Y%m%d%H%M%S}.json"
    manifest_path.write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding="utf-8")
    return manifest_path


def render_scope_clause(tenant_id: str | None, product_ids: list[str], column_name: str = "product_id") -> str:
    """Render the SQL clause for all/tenant/product scoped cleanup."""
    if product_ids:
        placeholders = ",".join(["%s"] * len(product_ids))
        return f" AND {column_name} IN ({placeholders})"
    if tenant_id:
        return " AND tenant_id = %s"
    return ""


def catalog_scope_subquery(tenant_id: str | None,
                           product_ids: list[str],
                           select_column: str = "id",
                           extra_clause: str = "") -> tuple[str, tuple[object, ...]]:
    """Return the scoped risk metric catalog subquery and parameters."""
    sql = f"SELECT {select_column} FROM risk_metric_catalog WHERE deleted = 0"
    sql += render_scope_clause(tenant_id, product_ids)
    sql += extra_clause
    return sql, scope_params(tenant_id, product_ids)


def release_batch_scope_subquery(tenant_id: str | None,
                                 product_ids: list[str],
                                 select_column: str = "id") -> tuple[str, tuple[object, ...]]:
    """Return the scoped release batch subquery and parameters."""
    sql = f"SELECT {select_column} FROM iot_product_contract_release_batch WHERE deleted = 0"
    sql += render_scope_clause(tenant_id, product_ids)
    return sql, scope_params(tenant_id, product_ids)


def vendor_rule_scope_subquery(tenant_id: str | None,
                               product_ids: list[str],
                               select_column: str = "id") -> tuple[str, tuple[object, ...]]:
    """Return the scoped vendor rule subquery and parameters."""
    sql = f"SELECT {select_column} FROM iot_vendor_metric_mapping_rule WHERE deleted = 0"
    sql += render_scope_clause(tenant_id, product_ids)
    return sql, scope_params(tenant_id, product_ids)


def device_scope_subquery(tenant_id: str | None,
                          product_ids: list[str],
                          select_column: str) -> tuple[str, tuple[object, ...]]:
    """Return the scoped device subquery and parameters."""
    sql = f"SELECT {select_column} FROM iot_device WHERE deleted = 0"
    sql += render_scope_clause(tenant_id, product_ids)
    return sql, scope_params(tenant_id, product_ids)


def approval_action_clause(column_name: str = "action_code") -> tuple[str, tuple[object, ...]]:
    """Render the product-governance approval action filter."""
    placeholders = ",".join(["%s"] * len(GOVERNANCE_ACTION_CODES))
    return f"{column_name} IN ({placeholders})", GOVERNANCE_ACTION_CODES


def work_item_scope_predicate(tenant_id: str | None,
                              product_ids: list[str]) -> tuple[str, tuple[object, ...]]:
    """Build the scoped predicate for governance work items."""
    work_item_placeholders = ",".join(["%s"] * len(GOVERNANCE_WORK_ITEM_CODES))
    action_clause, action_params = approval_action_clause()
    if product_ids:
        catalog_sql, catalog_params = catalog_scope_subquery(tenant_id, product_ids)
        batch_sql, batch_params = release_batch_scope_subquery(tenant_id, product_ids)
        product_placeholders = ",".join(["%s"] * len(product_ids))
        predicate = (
            "("
            f"(product_id IN ({product_placeholders})) OR "
            f"(release_batch_id IN ({batch_sql})) OR "
            f"(risk_metric_id IN ({catalog_sql}))"
            ")"
            f" AND (work_item_code IN ({work_item_placeholders}) OR {action_clause})"
        )
        params = (
            tuple(product_ids)
            + batch_params
            + catalog_params
            + GOVERNANCE_WORK_ITEM_CODES
            + action_params
        )
        return predicate, params
    predicate = f"(work_item_code IN ({work_item_placeholders}) OR {action_clause})"
    params: tuple[object, ...] = GOVERNANCE_WORK_ITEM_CODES + action_params
    if tenant_id:
        predicate = f"({predicate}) AND tenant_id = %s"
        params += (tenant_id,)
    return predicate, params


def work_item_scope_subquery(tenant_id: str | None,
                             product_ids: list[str],
                             select_column: str = "id") -> tuple[str, tuple[object, ...]]:
    """Return the scoped governance work item subquery and parameters."""
    predicate, params = work_item_scope_predicate(tenant_id, product_ids)
    return f"SELECT {select_column} FROM iot_governance_work_item WHERE deleted = 0 AND {predicate}", params


def approval_order_scope_predicate(tenant_id: str | None,
                                   product_ids: list[str]) -> tuple[str, tuple[object, ...]]:
    """Build the scoped predicate for governance approval orders."""
    if product_ids:
        release_batch_sql, release_batch_params = release_batch_scope_subquery(tenant_id, product_ids)
        rule_sql, rule_params = vendor_rule_scope_subquery(tenant_id, product_ids)
        work_item_sql, work_item_params = work_item_scope_subquery(tenant_id, product_ids)
        product_placeholders = ",".join(["%s"] * len(product_ids))
        vendor_action_placeholders = ",".join(["%s"] * 2)
        predicate = (
            "("
            f"(action_code = %s AND subject_type = 'PRODUCT' AND subject_id IN ({product_placeholders}))"
            " OR "
            f"(action_code = %s AND subject_type = 'RELEASE_BATCH' AND subject_id IN ({release_batch_sql}))"
            " OR "
            f"(action_code IN ({vendor_action_placeholders}) AND subject_type = 'VENDOR_MAPPING_RULE'"
            f" AND subject_id IN ({rule_sql}))"
            " OR "
            f"(work_item_id IN ({work_item_sql}))"
            ")"
        )
        params = (
            (PRODUCT_CONTRACT_RELEASE_APPLY,)
            + tuple(product_ids)
            + (PRODUCT_CONTRACT_ROLLBACK,)
            + release_batch_params
            + (VENDOR_MAPPING_RULE_PUBLISH, VENDOR_MAPPING_RULE_ROLLBACK)
            + rule_params
            + work_item_params
        )
        return predicate, params
    action_clause, action_params = approval_action_clause()
    predicate = action_clause
    params = action_params
    if tenant_id:
        predicate = f"({predicate}) AND tenant_id = %s"
        params += (tenant_id,)
    return predicate, params


def product_scoped_sql(table: str,
                       tenant_id: str | None,
                       product_ids: list[str],
                       action: str,
                       column_name: str = "product_id",
                       update_sql: str | None = None) -> tuple[str, tuple[object, ...]]:
    """Render direct product-scoped delete/update SQL."""
    clause = render_scope_clause(tenant_id, product_ids, column_name)
    params = scope_params(tenant_id, product_ids)
    if action in {"delete", "conditional-delete"}:
        return f"DELETE FROM {table} WHERE deleted = 0{clause}", params
    if update_sql is None:
        raise ValueError(f"Unsupported update operation for {table}")
    return f"{update_sql} WHERE deleted = 0{clause}", params


def operation_sql(operation: dict[str, str], scope: dict[str, object]) -> tuple[str, tuple[object, ...]]:
    """Build the SQL statement for a single cleanup operation."""
    tenant_id = scope.get("tenantId")
    product_ids = normalize_product_ids(list(scope.get("productIds") or []))
    table = operation["table"]
    action = operation["action"]
    if table in {
        "risk_metric_catalog",
        "iot_product_metric_resolver_snapshot",
        "iot_product_contract_release_snapshot",
        "iot_product_contract_release_batch",
        "iot_vendor_metric_mapping_rule_snapshot",
        "iot_vendor_metric_mapping_rule",
        "iot_vendor_metric_evidence",
        "iot_product_model",
    }:
        return product_scoped_sql(table, tenant_id, product_ids, action)
    if table == "iot_product":
        return product_scoped_sql(
            table,
            tenant_id,
            product_ids,
            action,
            "id",
            "UPDATE iot_product SET metadata_json = NULL",
        )
    if table == "risk_metric_linkage_binding":
        catalog_sql, catalog_params = catalog_scope_subquery(tenant_id, product_ids)
        return (
            f"DELETE FROM risk_metric_linkage_binding WHERE deleted = 0 AND risk_metric_id IN ({catalog_sql})",
            catalog_params,
        )
    if table == "risk_metric_emergency_plan_binding":
        catalog_sql, catalog_params = catalog_scope_subquery(tenant_id, product_ids)
        return (
            f"DELETE FROM risk_metric_emergency_plan_binding WHERE deleted = 0 AND risk_metric_id IN ({catalog_sql})",
            catalog_params,
        )
    if table == "risk_point_device":
        catalog_sql, catalog_params = catalog_scope_subquery(tenant_id, product_ids)
        contract_sql, contract_params = catalog_scope_subquery(tenant_id, product_ids, "contract_identifier")
        normative_sql, normative_params = catalog_scope_subquery(
            tenant_id,
            product_ids,
            "normative_identifier",
            " AND normative_identifier IS NOT NULL",
        )
        sql = (
            "DELETE FROM risk_point_device WHERE deleted = 0 AND ("
            f"risk_metric_id IN ({catalog_sql})"
            f" OR (risk_metric_id IS NULL AND metric_identifier IN ({contract_sql}))"
            f" OR (risk_metric_id IS NULL AND metric_identifier IN ({normative_sql}))"
            ")"
        )
        return sql, catalog_params + contract_params + normative_params
    if table == "risk_point_device_capability_binding":
        if product_ids:
            device_sql, device_params = device_scope_subquery(tenant_id, product_ids, "id")
            return (
                "DELETE FROM risk_point_device_capability_binding"
                f" WHERE deleted = 0 AND device_id IN ({device_sql})",
                device_params,
            )
        clause = " AND tenant_id = %s" if tenant_id else ""
        params = (tenant_id,) if tenant_id else ()
        return f"DELETE FROM risk_point_device_capability_binding WHERE deleted = 0{clause}", params
    if table == "risk_point_device_pending_binding":
        if product_ids:
            device_id_sql, device_id_params = device_scope_subquery(tenant_id, product_ids, "id")
            device_code_sql, device_code_params = device_scope_subquery(tenant_id, product_ids, "device_code")
            sql = (
                "DELETE FROM risk_point_device_pending_binding WHERE deleted = 0 AND ("
                f"(device_id IS NOT NULL AND device_id IN ({device_id_sql}))"
                f" OR (device_id IS NULL AND device_code IN ({device_code_sql}))"
                ")"
            )
            return sql, device_id_params + device_code_params
        clause = " AND tenant_id = %s" if tenant_id else ""
        params = (tenant_id,) if tenant_id else ()
        return f"DELETE FROM risk_point_device_pending_binding WHERE deleted = 0{clause}", params
    if table == "risk_point_device_pending_promotion":
        if product_ids:
            pending_sql, pending_params = operation_sql(
                {"table": "risk_point_device_pending_binding", "action": "delete"},
                scope,
            )
            risk_point_device_sql, risk_point_device_params = operation_sql(
                {"table": "risk_point_device", "action": "delete"},
                scope,
            )
            pending_subquery = pending_sql.replace(
                "DELETE FROM risk_point_device_pending_binding",
                "SELECT id FROM risk_point_device_pending_binding",
                1,
            )
            risk_point_device_subquery = risk_point_device_sql.replace(
                "DELETE FROM risk_point_device",
                "SELECT id FROM risk_point_device",
                1,
            )
            sql = (
                "DELETE FROM risk_point_device_pending_promotion WHERE deleted = 0 AND ("
                f"pending_binding_id IN ({pending_subquery})"
                f" OR risk_point_device_id IN ({risk_point_device_subquery})"
                ")"
            )
            return sql, pending_params + risk_point_device_params
        clause = " AND tenant_id = %s" if tenant_id else ""
        params = (tenant_id,) if tenant_id else ()
        return f"DELETE FROM risk_point_device_pending_promotion WHERE deleted = 0{clause}", params
    if table == "rule_definition":
        catalog_sql, catalog_params = catalog_scope_subquery(tenant_id, product_ids)
        contract_sql, contract_params = catalog_scope_subquery(tenant_id, product_ids, "contract_identifier")
        normative_sql, normative_params = catalog_scope_subquery(
            tenant_id,
            product_ids,
            "normative_identifier",
            " AND normative_identifier IS NOT NULL",
        )
        sql = (
            "DELETE FROM rule_definition WHERE deleted = 0 AND ("
            f"risk_metric_id IN ({catalog_sql})"
            f" OR metric_identifier IN ({contract_sql})"
            f" OR metric_identifier IN ({normative_sql})"
            ")"
        )
        return sql, catalog_params + contract_params + normative_params
    if table == "iot_governance_work_item":
        predicate, params = work_item_scope_predicate(tenant_id, product_ids)
        return f"DELETE FROM iot_governance_work_item WHERE deleted = 0 AND {predicate}", params
    if table == "sys_governance_approval_order":
        predicate, params = approval_order_scope_predicate(tenant_id, product_ids)
        return f"DELETE FROM sys_governance_approval_order WHERE deleted = 0 AND {predicate}", params
    if table == "sys_governance_approval_transition":
        predicate, params = approval_order_scope_predicate(tenant_id, product_ids)
        return (
            "DELETE FROM sys_governance_approval_transition WHERE deleted = 0 AND order_id IN ("
            f"SELECT id FROM sys_governance_approval_order WHERE deleted = 0 AND {predicate}"
            ")",
            params,
        )
    if table == "iot_device_onboarding_case":
        return product_scoped_sql(
            table,
            tenant_id,
            product_ids,
            action,
            "product_id",
            "UPDATE iot_device_onboarding_case"
            " SET release_batch_id = NULL, current_step = 'CONTRACT_RELEASE', status = 'IN_PROGRESS'",
        )
    raise ValueError(f"Unsupported operation: {operation}")


def operation_count_sql(operation: dict[str, str], scope: dict[str, object]) -> tuple[str, tuple[object, ...]]:
    """Build the count statement for a single cleanup operation."""
    sql, params = operation_sql(operation, scope)
    table = operation["table"]
    if sql.startswith(f"DELETE FROM {table}"):
        return sql.replace(f"DELETE FROM {table}", f"SELECT COUNT(*) FROM {table}", 1), params
    where_index = sql.find(" WHERE ")
    if where_index < 0:
        raise ValueError(f"Unable to derive count SQL for {operation}")
    return f"SELECT COUNT(*) FROM {table}{sql[where_index:]}", params


def parse_jdbc_url(jdbc_url: str) -> dict[str, object]:
    """Parse the Spring JDBC URL into pymysql connection arguments."""
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


def open_mysql_connection(defaults: dict[str, str]):
    """Open a real MySQL connection only when backup/execute needs it."""
    try:
        import pymysql
    except ImportError as exc:
        raise RuntimeError("pymysql 未安装，无法执行 backup/execute 模式") from exc
    parsed = parse_jdbc_url(defaults["jdbc_url"])
    charset = parsed["query"].get("characterEncoding", ["utf8mb4"])[0] or "utf8mb4"
    return pymysql.connect(
        host=parsed["host"],
        port=int(parsed["port"]),
        user=defaults["user"],
        password=defaults["password"],
        database=parsed["database"],
        charset=charset,
        autocommit=False,
    )


def collect_operation_counts(connection, plan: dict[str, object]) -> list[dict[str, object]]:
    """Collect affected-row counts for backup mode."""
    counts = []
    cursor = connection.cursor()
    try:
        for operation in plan["operations"]:
            sql, params = operation_count_sql(operation, plan["scope"])
            cursor.execute(sql, params)
            row = cursor.fetchone()
            count = int(row[0] if isinstance(row, tuple) else row)
            counts.append({
                "table": operation["table"],
                "action": operation["action"],
                "affectedRows": count,
            })
    finally:
        close_cursor = getattr(cursor, "close", None)
        if callable(close_cursor):
            close_cursor()
    return counts


def execute_cleanup_plan(connection, plan: dict[str, object]) -> None:
    """Execute the current cleanup plan against an open DB connection."""
    cursor = connection.cursor()
    try:
        for operation in plan["operations"]:
            sql, params = operation_sql(operation, plan["scope"])
            cursor.execute(sql, params)
        connection.commit()
    except Exception:
        rollback = getattr(connection, "rollback", None)
        if callable(rollback):
            rollback()
        raise
    finally:
        close_cursor = getattr(cursor, "close", None)
        if callable(close_cursor):
            close_cursor()


def assert_execute_allowed(args: argparse.Namespace) -> None:
    """Prevent execute mode from running without explicit confirmation."""
    if args.mode == "execute" and (not args.execute or not args.confirm):
        raise RuntimeError("Execute mode requires both --execute and --confirm")


def main(argv: list[str] | None = None) -> int:
    """Print the current cleanup plan in JSON form."""
    args = parse_args(argv)
    assert_execute_allowed(args)
    plan = build_cleanup_plan(args.tenant_id, args.product_ids, args.mode)
    if args.mode == "backup":
        defaults = load_dev_defaults()
        connection = open_mysql_connection(defaults)
        try:
            plan["backupSummary"] = {
                "jdbcUrl": defaults["jdbc_url"],
                "generatedAt": datetime.now().isoformat(timespec="seconds"),
                "operations": collect_operation_counts(connection, plan),
            }
        finally:
            close_connection = getattr(connection, "close", None)
            if callable(close_connection):
                close_connection()
        manifest_path = write_backup_manifest(REPO_ROOT / "tmp" / "product-governance-reset", plan)
        print(json.dumps({"backupManifest": str(manifest_path)}, ensure_ascii=False, indent=2))
        return 0
    if args.mode == "execute":
        defaults = load_dev_defaults()
        connection = open_mysql_connection(defaults)
        try:
            execute_cleanup_plan(connection, plan)
        finally:
            close_connection = getattr(connection, "close", None)
            if callable(close_connection):
                close_connection()
        print(json.dumps({"mode": "execute", "status": "completed"}, ensure_ascii=False, indent=2))
        return 0
    print(json.dumps(plan, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
