from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


if str(Path(__file__).resolve().parents[2]) not in sys.path:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from scripts.schema.load_registry import RegistryField, RegistryIndex, RegistryObject, SchemaRegistry, load_registry


REPO_ROOT = Path(__file__).resolve().parents[2]
OUTPUT_FILES = {
    "mysql_schema_sync_manifest": REPO_ROOT / "schema" / "generated" / "mysql-schema-sync.json",
    "catalog_markdown": REPO_ROOT / "docs" / "appendix" / "database-schema-object-catalog.generated.md",
    "mysql_runtime_manifest": REPO_ROOT
    / "spring-boot-iot-framework"
    / "src"
    / "main"
    / "resources"
    / "schema"
    / "runtime-bootstrap"
    / "mysql-active-schema.json",
    "tdengine_runtime_manifest": REPO_ROOT
    / "spring-boot-iot-telemetry"
    / "src"
    / "main"
    / "resources"
    / "schema"
    / "runtime-bootstrap"
    / "tdengine-active-schema.json",
    "mysql_init_sql": REPO_ROOT / "sql" / "init.sql",
    "tdengine_init_sql": REPO_ROOT / "sql" / "init-tdengine.sql",
}


def render_artifacts(schema_root: str | Path) -> dict[str, object]:
    registry = load_registry(schema_root)
    mysql_tables_for_sync = _sorted_objects(
        obj for obj in registry.mysql.values() if obj.included_in_schema_sync
    )
    mysql_views_for_sync = _sorted_objects(
        obj for obj in registry.mysql_views.values() if obj.included_in_schema_sync
    )
    mysql_runtime_objects = _sorted_objects(
        list(_active_mysql_tables(registry)) + list(_active_mysql_views(registry))
    )
    tdengine_runtime_objects = _sorted_objects(
        obj for obj in registry.tdengine.values() if obj.lifecycle == "active"
    )

    return {
        "mysql_init_sql": _render_mysql_init_sql(registry),
        "tdengine_init_sql": _render_tdengine_init_sql(registry),
        "mysql_schema_sync_manifest": {
            "createTableSql": [
                {"name": obj.name, "sql": _render_mysql_table_sql(obj)}
                for obj in mysql_tables_for_sync
            ],
            "columnsToAdd": [
                {"name": obj.name, "columns": _manifest_columns(obj.fields)}
                for obj in mysql_tables_for_sync
            ],
            "indexesToAdd": [
                {"name": obj.name, "indexes": _manifest_indexes(obj.indexes)}
                for obj in mysql_tables_for_sync
            ],
            "viewSql": [
                {"name": obj.name, "sql": _render_mysql_view_sql(obj)}
                for obj in mysql_views_for_sync
            ],
            "tableLifecycle": {
                obj.name: obj.lifecycle for obj in _sorted_objects(list(registry.mysql.values()))
            },
        },
        "mysql_runtime_manifest": _render_mysql_runtime_manifest(mysql_runtime_objects),
        "tdengine_runtime_manifest": _render_tdengine_runtime_manifest(tdengine_runtime_objects),
        "catalog_markdown": _render_catalog_markdown(registry),
    }


def materialize_artifacts(schema_root: str | Path) -> dict[Path, str]:
    bundle = render_artifacts(schema_root)
    rendered: dict[Path, str] = {}
    for key, path in OUTPUT_FILES.items():
        rendered[path] = _serialize_bundle_value(bundle[key])
    return rendered


def write_artifacts(schema_root: str | Path) -> list[Path]:
    rendered = materialize_artifacts(schema_root)
    written: list[Path] = []
    for path, content in rendered.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        current = path.read_text(encoding="utf-8") if path.exists() else None
        if current != content:
            path.write_text(content, encoding="utf-8", newline="\n")
            written.append(path)
    return written


def check_artifacts(schema_root: str | Path) -> list[Path]:
    rendered = materialize_artifacts(schema_root)
    mismatches: list[Path] = []
    for path, expected in rendered.items():
        if not path.exists() or path.read_text(encoding="utf-8") != expected:
            mismatches.append(path)
    return mismatches


def _active_mysql_tables(registry: SchemaRegistry) -> tuple[RegistryObject, ...]:
    return tuple(
        obj
        for obj in registry.mysql.values()
        if obj.lifecycle == "active" and obj.included_in_init
    )


def _active_mysql_views(registry: SchemaRegistry) -> tuple[RegistryObject, ...]:
    return tuple(
        obj
        for obj in registry.mysql_views.values()
        if obj.lifecycle == "active" and obj.included_in_init
    )


def _render_mysql_init_sql(registry: SchemaRegistry) -> str:
    tables = _sorted_objects(_active_mysql_tables(registry))
    views = _sorted_objects(_active_mysql_views(registry))

    chunks = [
        "CREATE DATABASE IF NOT EXISTS rm_iot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;",
        "USE rm_iot;",
        "",
        "SET NAMES utf8mb4;",
        "",
        "-- 本文件由 scripts/schema/render_artifacts.py 生成，不要手工编辑。",
    ]
    if views:
        chunks.append("")
        chunks.append("-- 兼容视图")
        for obj in reversed(views):
            chunks.append(f"DROP VIEW IF EXISTS {obj.name};")
    if tables:
        chunks.append("")
        chunks.append("-- 活跃 MySQL 表")
        for obj in reversed(tables):
            chunks.append(f"DROP TABLE IF EXISTS {obj.name};")
        for obj in tables:
            chunks.extend(["", _render_mysql_table_sql(obj)])
    if views:
        for obj in views:
            chunks.extend(["", _render_mysql_view_sql(obj)])
    return "\n".join(chunks).strip() + "\n"


def _render_mysql_table_sql(obj: RegistryObject) -> str:
    column_lines = [
        f"  {field.name} {field.data_type} COMMENT '{_sql_escape(field.comment_zh)}'"
        for field in obj.fields
    ]
    index_lines = [f"  {_render_mysql_index(index)}" for index in obj.indexes]
    lines = [
        f"-- 表：{obj.name}",
        f"-- 说明：{obj.comment_zh}",
        f"CREATE TABLE {obj.name} (",
        ",\n".join(column_lines + index_lines),
        f") COMMENT='{_sql_escape(obj.table_comment_zh)}';",
    ]
    return "\n".join(lines)


def _render_mysql_index(index: RegistryIndex) -> str:
    columns = ", ".join(index.columns)
    if index.kind == "PRIMARY":
        return f"PRIMARY KEY ({columns})"
    if index.kind == "UNIQUE":
        return f"UNIQUE KEY {index.name} ({columns})"
    return f"KEY {index.name} ({columns})"


def _render_mysql_view_sql(obj: RegistryObject) -> str:
    return "\n".join(
        [
            f"-- 视图：{obj.name}",
            f"-- 说明：{obj.comment_zh}",
            f"CREATE OR REPLACE VIEW {obj.name} AS",
            obj.definition_sql.rstrip(" ;") + ";",
        ]
    )


def _render_tdengine_init_sql(registry: SchemaRegistry) -> str:
    objects = _sorted_objects(
        obj
        for obj in registry.tdengine.values()
        if obj.lifecycle == "active" and obj.included_in_init
    )
    chunks = [
        "CREATE DATABASE IF NOT EXISTS iot;",
        "USE iot;",
        "",
        "-- 本文件由 scripts/schema/render_artifacts.py 生成，不要手工编辑。",
    ]
    for obj in objects:
        chunks.extend(["", _render_tdengine_object_sql(obj)])
    return "\n".join(chunks).strip() + "\n"


def _render_tdengine_object_sql(obj: RegistryObject) -> str:
    dictionary_lines = [
        "-- 字段字典：",
        *[
            f"-- - {field.name}: {field.comment_zh}{'（TAG）' if field.is_tag else ''}"
            for field in obj.fields
        ],
    ]
    lines = [
        "-- ========================================",
        f"-- 对象：{obj.name}",
        f"-- 类型：{obj.storage_type}",
        f"-- 说明：{obj.comment_zh}",
        *dictionary_lines,
    ]
    if obj.storage_type == "tdengine_table":
        lines.append(_render_tdengine_table(obj))
    else:
        lines.append(_render_tdengine_stable(obj))
    return "\n".join(lines)


def _render_tdengine_table(obj: RegistryObject) -> str:
    fields = ",\n".join(f"  {field.name} {field.data_type}" for field in obj.fields)
    return f"CREATE TABLE IF NOT EXISTS {obj.name} (\n{fields}\n);"


def _render_tdengine_stable(obj: RegistryObject) -> str:
    normal_fields = [field for field in obj.fields if not field.is_tag]
    tag_fields = [field for field in obj.fields if field.is_tag]
    body = ",\n".join(f"  {field.name} {field.data_type}" for field in normal_fields)
    tags = ",\n".join(f"  {field.name} {field.data_type}" for field in tag_fields)
    return (
        f"CREATE STABLE IF NOT EXISTS {obj.name} (\n{body}\n)\n"
        f"TAGS (\n{tags}\n);"
    )


def _render_catalog_markdown(registry: SchemaRegistry) -> str:
    lines = [
        "# Database Schema Object Catalog",
        "",
        "Generated from the schema registry. Do not edit by hand.",
        "",
        "| Name | Storage | Lifecycle | In Init | In Schema Sync | Runtime Bootstrap | Owner Module | Comment |",
        "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for obj in _sorted_objects(registry.all_objects):
        lines.append(
            "| {name} | {storage} | {lifecycle} | {init} | {sync} | {runtime} | {owner} | {comment} |".format(
                name=obj.name,
                storage=obj.storage_type,
                lifecycle=obj.lifecycle,
                init=_bool_flag(obj.included_in_init),
                sync=_bool_flag(obj.included_in_schema_sync),
                runtime=obj.runtime_bootstrap_mode,
                owner=obj.owner_module,
                comment=_markdown_escape(obj.comment_zh),
            )
        )
    return "\n".join(lines).strip() + "\n"


def _manifest_columns(fields: tuple[RegistryField, ...]) -> list[dict[str, object]]:
    return [
        {
            "name": field.name,
            "definition": field.data_type,
            "commentZh": field.comment_zh,
            "isTag": field.is_tag,
        }
        for field in fields
    ]


def _manifest_indexes(indexes: tuple[RegistryIndex, ...]) -> list[dict[str, object]]:
    return [
        {
            "name": index.name,
            "kind": index.kind,
            "columns": list(index.columns),
        }
        for index in indexes
    ]


def _render_mysql_runtime_manifest(objects: list[RegistryObject]) -> dict[str, object]:
    tables = [obj for obj in objects if obj.storage_type == "mysql_table"]
    views = [obj for obj in objects if obj.storage_type == "mysql_view"]
    return {
        "tables": [_mysql_runtime_table_entry(obj) for obj in tables],
        "views": [_mysql_runtime_view_entry(obj) for obj in views],
    }


def _mysql_runtime_table_entry(obj: RegistryObject) -> dict[str, object]:
    return {
        "name": obj.name,
        "storageType": obj.storage_type,
        "ownerModule": obj.owner_module,
        "runtimeBootstrapMode": obj.runtime_bootstrap_mode,
        "createSql": _ensure_create_table_if_not_exists(_render_mysql_table_sql(obj)),
        "columns": [
            {
                "name": field.name,
                "addSql": _render_mysql_add_column_sql(obj.name, field),
            }
            for field in obj.fields
        ],
        "indexes": [
            {
                "name": index.name,
                "addSql": _render_mysql_add_index_sql(obj.name, index),
            }
            for index in obj.indexes
            if index.kind != "PRIMARY"
        ],
    }


def _mysql_runtime_view_entry(obj: RegistryObject) -> dict[str, object]:
    return {
        "name": obj.name,
        "storageType": obj.storage_type,
        "ownerModule": obj.owner_module,
        "runtimeBootstrapMode": obj.runtime_bootstrap_mode,
        "createOrReplaceSql": _render_mysql_view_sql(obj),
    }


def _render_tdengine_runtime_manifest(objects: list[RegistryObject]) -> dict[str, object]:
    return {
        "objects": [_tdengine_runtime_entry(obj) for obj in objects]
    }


def _tdengine_runtime_entry(obj: RegistryObject) -> dict[str, object]:
    return {
        "name": obj.name,
        "storageType": obj.storage_type,
        "ownerModule": obj.owner_module,
        "runtimeBootstrapMode": obj.runtime_bootstrap_mode,
        "createSql": _render_tdengine_table(obj) if obj.storage_type == "tdengine_table" else _render_tdengine_stable(obj),
        "fieldDictionary": [
            {
                "name": field.name,
                "commentZh": field.comment_zh,
                "isTag": field.is_tag,
            }
            for field in obj.fields
        ],
    }


def _ensure_create_table_if_not_exists(sql: str) -> str:
    return sql.replace("CREATE TABLE ", "CREATE TABLE IF NOT EXISTS ", 1)


def _render_mysql_add_column_sql(table_name: str, field: RegistryField) -> str:
    return (
        f"ALTER TABLE `{table_name}` ADD COLUMN `{field.name}` "
        f"{field.data_type} COMMENT '{_sql_escape(field.comment_zh)}'"
    )


def _render_mysql_add_index_sql(table_name: str, index: RegistryIndex) -> str:
    columns = ", ".join(f"`{column}`" for column in index.columns)
    if index.kind == "UNIQUE":
        return f"ALTER TABLE `{table_name}` ADD UNIQUE INDEX `{index.name}` ({columns})"
    return f"ALTER TABLE `{table_name}` ADD INDEX `{index.name}` ({columns})"


def _serialize_bundle_value(value: object) -> str:
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def _sorted_objects(objects) -> list[RegistryObject]:
    return sorted(objects, key=lambda obj: (obj.domain, obj.storage_type, obj.name))


def _sql_escape(text: str) -> str:
    return text.replace("'", "''")


def _markdown_escape(text: str) -> str:
    return text.replace("|", "\\|")


def _bool_flag(value: bool) -> str:
    return "yes" if value else "no"


def _parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Render schema registry artifacts.")
    parser.add_argument(
        "--schema-root",
        default=str(REPO_ROOT / "schema"),
        help="Schema registry root directory.",
    )
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--write", action="store_true", help="Write generated artifacts to disk.")
    mode.add_argument("--check", action="store_true", help="Check whether generated artifacts are up to date.")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = _parse_args(argv)
    schema_root = Path(args.schema_root).resolve()
    if args.write:
        written = write_artifacts(schema_root)
        for path in written:
            print(path.relative_to(REPO_ROOT))
        return 0

    mismatches = check_artifacts(schema_root)
    if mismatches:
        for path in mismatches:
            print(f"OUT_OF_DATE {path.relative_to(REPO_ROOT)}")
        return 1
    print("Schema artifacts are up to date.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
