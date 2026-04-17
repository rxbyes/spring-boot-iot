import json
import re
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[1]
INIT_SQL_PATH = REPO_ROOT / "sql" / "init.sql"
SCHEMA_SYNC_MANIFEST_PATH = REPO_ROOT / "schema" / "generated" / "mysql-schema-sync.json"


def read_init_sql() -> str:
    return INIT_SQL_PATH.read_text(encoding="utf-8")


def extract_create_table_statement(content: str, table_name: str) -> str:
    markers = (
        f"CREATE TABLE {table_name} (",
        f"CREATE TABLE IF NOT EXISTS {table_name} (",
        f"CREATE TABLE `{table_name}` (",
        f"CREATE TABLE IF NOT EXISTS `{table_name}` (",
    )
    start = next((content.find(marker) for marker in markers if content.find(marker) != -1), -1)
    if start == -1:
        raise AssertionError(f"missing CREATE TABLE block for {table_name}")
    end = content.find(";", start)
    if end == -1:
        raise AssertionError(f"missing statement terminator for {table_name}")
    return content[start : end + 1]


def extract_create_view_statement(content: str, view_name: str) -> str:
    markers = (
        f"CREATE OR REPLACE VIEW {view_name} AS",
        f"CREATE VIEW {view_name} AS",
        f"CREATE OR REPLACE VIEW `{view_name}` AS",
        f"CREATE VIEW `{view_name}` AS",
    )
    start = next((content.find(marker) for marker in markers if content.find(marker) != -1), -1)
    if start == -1:
        raise AssertionError(f"missing CREATE VIEW block for {view_name}")
    end = content.find(";", start)
    if end == -1:
        raise AssertionError(f"missing statement terminator for {view_name}")
    return content[start : end + 1]


def extract_tdengine_create_statement(content: str, object_name: str) -> str:
    markers = (
        f"CREATE TABLE {object_name} (",
        f"CREATE TABLE IF NOT EXISTS {object_name} (",
        f"CREATE STABLE {object_name} (",
        f"CREATE STABLE IF NOT EXISTS {object_name} (",
        f"CREATE TABLE `{object_name}` (",
        f"CREATE TABLE IF NOT EXISTS `{object_name}` (",
        f"CREATE STABLE `{object_name}` (",
        f"CREATE STABLE IF NOT EXISTS `{object_name}` (",
    )
    start = next((content.find(marker) for marker in markers if content.find(marker) != -1), -1)
    if start == -1:
        raise AssertionError(f"missing TDengine CREATE block for {object_name}")
    end = content.find(";", start)
    if end == -1:
        raise AssertionError(f"missing statement terminator for {object_name}")
    return content[start : end + 1]


def load_raw_schema_sync_manifest() -> dict[str, Any]:
    return json.loads(SCHEMA_SYNC_MANIFEST_PATH.read_text(encoding="utf-8"))


def get_named_entry(entries: list[dict[str, Any]], name: str, label: str = "entry") -> dict[str, Any]:
    entry = next((item for item in entries if item.get("name") == name), None)
    if entry is None:
        raise AssertionError(f"missing {label} for {name}")
    return entry


def _split_markdown_row(line: str) -> list[str]:
    stripped = line.strip()
    if not stripped.startswith("|") or not stripped.endswith("|"):
        raise AssertionError(f"invalid markdown table row: {line}")
    return [cell.strip() for cell in stripped.strip("|").split("|")]


def parse_markdown_table(markdown: str, header_line: str) -> list[dict[str, str]]:
    lines = markdown.splitlines()
    try:
        header_index = lines.index(header_line)
    except ValueError as exc:
        raise AssertionError(f"missing markdown table header: {header_line}") from exc
    if header_index + 1 >= len(lines):
        raise AssertionError(f"missing markdown table separator after: {header_line}")
    headers = _split_markdown_row(lines[header_index])
    rows: list[dict[str, str]] = []
    for line in lines[header_index + 2 :]:
        if not line.strip():
            break
        if not line.lstrip().startswith("|"):
            break
        values = _split_markdown_row(line)
        if len(values) != len(headers):
            raise AssertionError(f"markdown table row shape mismatch: {line}")
        rows.append(dict(zip(headers, values)))
    return rows


def get_markdown_table_row(rows: list[dict[str, str]], column: str, value: str) -> dict[str, str]:
    row = next((item for item in rows if item.get(column) == value), None)
    if row is None:
        raise AssertionError(f"missing markdown table row where {column}={value}")
    return row


def extract_markdown_section(markdown: str, heading: str) -> str:
    lines = markdown.splitlines()
    try:
        start_index = lines.index(heading)
    except ValueError as exc:
        raise AssertionError(f"missing markdown section: {heading}") from exc
    heading_level = len(heading) - len(heading.lstrip("#"))
    collected = [lines[start_index]]
    for line in lines[start_index + 1 :]:
        stripped = line.lstrip()
        if stripped.startswith("#"):
            level = len(stripped) - len(stripped.lstrip("#"))
            if level <= heading_level:
                break
        collected.append(line)
    return "\n".join(collected)


def extract_markdown_mermaid_block(markdown: str) -> str:
    lines = markdown.splitlines()
    start_index = next((index for index, line in enumerate(lines) if line.strip() == "```mermaid"), -1)
    if start_index == -1:
        raise AssertionError("missing mermaid block")
    end_index = next(
        (index for index in range(start_index + 1, len(lines)) if lines[index].strip() == "```"),
        -1,
    )
    if end_index == -1:
        raise AssertionError("missing mermaid fence terminator")
    return "\n".join(lines[start_index + 1 : end_index])


def mermaid_has_edge(mermaid_block: str, source_name: str, target_name: str, edge_label: str) -> bool:
    edge_pattern = re.compile(
        r'^(?P<source_id>[^\[]+)\["(?P<source_name>[^"]+)"\]\s+-->\|"(?P<label>[^"]+)"\|\s+'
        r'(?P<target_id>[^\[]+)\["(?P<target_name>[^"]+)"\]$'
    )
    for line in mermaid_block.splitlines():
        match = edge_pattern.match(line.strip())
        if not match:
            continue
        if (
            match.group("source_name") == source_name
            and match.group("target_name") == target_name
            and match.group("label") == edge_label
        ):
            return True
    return False


def get_schema_sync_create_entry(table_name: str) -> dict[str, Any]:
    manifest = load_raw_schema_sync_manifest()
    entry = get_named_entry(
        manifest.get("createTableSql", []),
        table_name,
        "createTableSql entry",
    )
    return {
        "name": entry["name"],
        "sql": entry["sql"],
        "lifecycle": manifest.get("tableLifecycle", {}).get(table_name),
    }


def get_schema_sync_columns(table_name: str) -> dict[str, dict[str, Any]]:
    manifest = load_raw_schema_sync_manifest()
    entry = get_named_entry(
        manifest.get("columnsToAdd", []),
        table_name,
        "columnsToAdd entry",
    )
    return {
        column["name"]: {
            "name": column["name"],
            "definition": column["definition"],
            "commentZh": column["commentZh"],
            "isTag": column.get("isTag", False),
        }
        for column in entry.get("columns", [])
    }


def get_schema_sync_indexes(table_name: str) -> dict[str, dict[str, Any]]:
    manifest = load_raw_schema_sync_manifest()
    entry = get_named_entry(
        manifest.get("indexesToAdd", []),
        table_name,
        "indexesToAdd entry",
    )
    return {
        index["name"]: {
            "name": index["name"],
            "kind": str(index["kind"]).upper(),
            "columns": tuple(index["columns"]),
        }
        for index in entry.get("indexes", [])
    }
