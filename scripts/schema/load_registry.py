from __future__ import annotations

import json
import re
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


VALID_LIFECYCLES = {"active", "archived", "pending_delete"}
CHINESE_RE = re.compile(r"[\u4e00-\u9fff]")
ENGLISH_ALPHA_RE = re.compile(r"[A-Za-z]")

REQUIRED_OBJECT_KEYS = {
    "name",
    "storageType",
    "ownerModule",
    "lifecycle",
    "includedInInit",
    "includedInSchemaSync",
    "runtimeBootstrapMode",
    "lineageRole",
    "businessBoundary",
    "fields",
    "indexes",
    "relations",
}


@dataclass(frozen=True)
class RegistryField:
    name: str
    data_type: str
    comment_zh: str
    is_tag: bool = False


@dataclass(frozen=True)
class RegistryIndex:
    name: str
    kind: str
    columns: tuple[str, ...]


@dataclass(frozen=True)
class RegistryRelation:
    relation_type: str
    target: str
    via_field: str


@dataclass(frozen=True)
class RegistryObject:
    name: str
    domain: str
    storage_type: str
    owner_module: str
    lifecycle: str
    included_in_init: bool
    included_in_schema_sync: bool
    runtime_bootstrap_mode: str
    table_comment_zh: str
    comment_zh: str
    fields: tuple[RegistryField, ...]
    indexes: tuple[RegistryIndex, ...]
    relations: tuple[RegistryRelation, ...]
    lineage_role: str
    business_boundary: str
    source_tables: tuple[str, ...] = ()
    definition_sql: str = ""


@dataclass(frozen=True)
class SchemaRegistry:
    mysql: dict[str, RegistryObject]
    mysql_views: dict[str, RegistryObject]
    tdengine: dict[str, RegistryObject]
    _invalid_relation_targets: tuple[str, ...]

    @property
    def mysql_objects(self) -> tuple[RegistryObject, ...]:
        return tuple(self.mysql.values())

    @property
    def all_objects(self) -> tuple[RegistryObject, ...]:
        return tuple(self.mysql.values()) + tuple(self.mysql_views.values()) + tuple(self.tdengine.values())

    def find_missing_comments(self) -> list[str]:
        missing: list[str] = []
        for obj in self.all_objects:
            if _is_blank(obj.comment_zh):
                missing.append(f"{obj.storage_type}:{obj.name}")
            for field in obj.fields:
                if _is_blank(field.comment_zh):
                    missing.append(f"{obj.storage_type}:{obj.name}.{field.name}")
        return missing

    def find_english_only_comments(self) -> list[str]:
        english_only: list[str] = []
        for obj in self.all_objects:
            if _is_english_only(obj.comment_zh):
                english_only.append(f"{obj.storage_type}:{obj.name}")
            for field in obj.fields:
                if _is_english_only(field.comment_zh):
                    english_only.append(f"{obj.storage_type}:{obj.name}.{field.name}")
        return english_only

    def find_invalid_relation_targets(self) -> list[str]:
        return list(self._invalid_relation_targets)


def load_registry(schema_root: str | Path) -> SchemaRegistry:
    root = Path(schema_root).resolve()
    mysql_objects = _load_objects(
        directory=root / "mysql",
        expected_file_storage_type="mysql",
        expected_object_storage_types={"mysql_table"},
    )
    mysql_view_objects = _load_objects(
        directory=root / "views",
        expected_file_storage_type="mysql_view",
        expected_object_storage_types={"mysql_view"},
    )
    tdengine_objects = _load_objects(
        directory=root / "tdengine",
        expected_file_storage_type="tdengine",
        expected_object_storage_types={"tdengine_table", "tdengine_stable"},
    )

    all_objects = mysql_objects + mysql_view_objects + tdengine_objects
    _validate_unique_names(all_objects)
    invalid_relations = _validate_relation_targets(all_objects)

    return SchemaRegistry(
        mysql={obj.name: obj for obj in mysql_objects},
        mysql_views={obj.name: obj for obj in mysql_view_objects},
        tdengine={obj.name: obj for obj in tdengine_objects},
        _invalid_relation_targets=tuple(invalid_relations),
    )


def _load_objects(
    directory: Path,
    expected_file_storage_type: str,
    expected_object_storage_types: set[str],
) -> list[RegistryObject]:
    if not directory.exists():
        return []

    objects: list[RegistryObject] = []
    for path in sorted(directory.glob("*.json")):
        payload = json.loads(path.read_text(encoding="utf-8"))
        file_storage_type = _required_non_blank_string(payload, "storageType", path=path, context="file")
        if file_storage_type != expected_file_storage_type:
            raise ValueError(
                f"Registry file {path} storageType={file_storage_type!r} "
                f"does not match expected {expected_file_storage_type!r}"
            )

        domain = _required_non_blank_string(payload, "domain", path=path, context="file")
        raw_objects = payload.get("objects")
        if not isinstance(raw_objects, list):
            raise ValueError(f"Registry file {path} must contain an objects list")
        if not raw_objects:
            raise ValueError(f"Registry file {path} has an empty objects list")

        for raw_obj in raw_objects:
            objects.append(
                _build_object(
                    raw_obj=raw_obj,
                    domain=domain,
                    path=path,
                    expected_object_storage_types=expected_object_storage_types,
                )
            )
    return objects


def _build_object(
    raw_obj: dict,
    domain: str,
    path: Path,
    expected_object_storage_types: set[str],
) -> RegistryObject:
    if not isinstance(raw_obj, dict):
        raise ValueError(f"Registry object in {path} must be a JSON object")
    _validate_required_object_keys(raw_obj, path=path)

    name = _required_non_blank_string(raw_obj, "name", path=path, context="object")
    storage_type = _required_non_blank_string(raw_obj, "storageType", path=path, context=name)
    if storage_type not in expected_object_storage_types:
        raise ValueError(
            f"Registry object {name!r} in {path} has storageType={storage_type!r}; "
            f"expected one of {sorted(expected_object_storage_types)}"
        )

    lifecycle = _required_non_blank_string(raw_obj, "lifecycle", path=path, context=name)
    if lifecycle not in VALID_LIFECYCLES:
        raise ValueError(
            f"Registry object {name!r} in {path} has invalid lifecycle {lifecycle!r}; "
            f"valid values are {sorted(VALID_LIFECYCLES)}"
        )

    owner_module = _required_non_blank_string(raw_obj, "ownerModule", path=path, context=name)
    runtime_bootstrap_mode = _required_non_blank_string(raw_obj, "runtimeBootstrapMode", path=path, context=name)
    lineage_role = _required_non_blank_string(raw_obj, "lineageRole", path=path, context=name)
    business_boundary = _required_non_blank_string(raw_obj, "businessBoundary", path=path, context=name)
    table_comment_zh = _required_non_blank_string(raw_obj, "tableCommentZh", path=path, context=name)
    comment_zh = _required_non_blank_string(raw_obj, "commentZh", path=path, context=name)

    included_in_init = _required_bool(raw_obj, "includedInInit", path=path, context=name)
    included_in_schema_sync = _required_bool(raw_obj, "includedInSchemaSync", path=path, context=name)

    fields = _parse_fields(raw_obj.get("fields"), object_name=name, path=path)
    indexes = _parse_indexes(raw_obj.get("indexes"), object_name=name, path=path)
    relations = _parse_relations(raw_obj.get("relations"), object_name=name, path=path)

    source_tables = tuple(str(v).strip() for v in raw_obj.get("sourceTables", []) if str(v).strip())
    definition_sql = str(raw_obj.get("definitionSql", "")).strip()
    if storage_type == "mysql_view":
        if not source_tables:
            raise ValueError(f"View object {name!r} in {path} must provide non-empty sourceTables")
        if not definition_sql:
            raise ValueError(f"View object {name!r} in {path} must provide non-empty definitionSql")

    return RegistryObject(
        name=name,
        domain=domain,
        storage_type=storage_type,
        owner_module=owner_module,
        lifecycle=lifecycle,
        included_in_init=included_in_init,
        included_in_schema_sync=included_in_schema_sync,
        runtime_bootstrap_mode=runtime_bootstrap_mode,
        table_comment_zh=table_comment_zh,
        comment_zh=comment_zh,
        fields=fields,
        indexes=indexes,
        relations=relations,
        lineage_role=lineage_role,
        business_boundary=business_boundary,
        source_tables=source_tables,
        definition_sql=definition_sql,
    )


def _validate_required_object_keys(raw_obj: dict, path: Path) -> None:
    missing = sorted(key for key in REQUIRED_OBJECT_KEYS if key not in raw_obj)
    if missing:
        raise ValueError(f"Registry object in {path} is missing required keys: {', '.join(missing)}")


def _parse_fields(raw_fields: object, object_name: str, path: Path) -> tuple[RegistryField, ...]:
    if not isinstance(raw_fields, list) or not raw_fields:
        raise ValueError(f"Registry object {object_name!r} in {path} must have a non-empty fields list")

    parsed: list[RegistryField] = []
    seen_names: set[str] = set()
    for item in raw_fields:
        if not isinstance(item, dict):
            raise ValueError(f"Field in object {object_name!r} of {path} must be a JSON object")
        name = _required_non_blank_string(item, "name", path=path, context=f"{object_name}.field")
        if name in seen_names:
            raise ValueError(f"Duplicate field {name!r} in object {object_name!r} of {path}")
        seen_names.add(name)
        data_type = _required_non_blank_string(item, "type", path=path, context=f"{object_name}.{name}")
        comment_zh = _required_non_blank_string(item, "commentZh", path=path, context=f"{object_name}.{name}")
        is_tag = bool(item.get("isTag", False))
        parsed.append(RegistryField(name=name, data_type=data_type, comment_zh=comment_zh, is_tag=is_tag))
    return tuple(parsed)


def _parse_indexes(raw_indexes: object, object_name: str, path: Path) -> tuple[RegistryIndex, ...]:
    if not isinstance(raw_indexes, list):
        raise ValueError(f"Registry object {object_name!r} in {path} must provide indexes as a list")

    parsed: list[RegistryIndex] = []
    seen_index_names: set[str] = set()
    for item in raw_indexes:
        if not isinstance(item, dict):
            raise ValueError(f"Index in object {object_name!r} of {path} must be a JSON object")
        name = _required_non_blank_string(item, "name", path=path, context=f"{object_name}.index")
        if name in seen_index_names:
            raise ValueError(f"Duplicate index {name!r} in object {object_name!r} of {path}")
        seen_index_names.add(name)
        kind = _required_non_blank_string(item, "kind", path=path, context=f"{object_name}.{name}")
        raw_columns = item.get("columns")
        if not isinstance(raw_columns, list) or not raw_columns:
            raise ValueError(
                f"Index {name!r} in object {object_name!r} of {path} must define non-empty columns list"
            )
        columns = tuple(str(col).strip() for col in raw_columns if str(col).strip())
        if not columns:
            raise ValueError(
                f"Index {name!r} in object {object_name!r} of {path} must define non-blank columns"
            )
        parsed.append(RegistryIndex(name=name, kind=kind, columns=columns))
    return tuple(parsed)


def _parse_relations(raw_relations: object, object_name: str, path: Path) -> tuple[RegistryRelation, ...]:
    if not isinstance(raw_relations, list):
        raise ValueError(f"Registry object {object_name!r} in {path} must provide relations as a list")

    parsed: list[RegistryRelation] = []
    seen_relation_keys: set[tuple[str, str, str]] = set()
    for item in raw_relations:
        if not isinstance(item, dict):
            raise ValueError(f"Relation in object {object_name!r} of {path} must be a JSON object")
        relation_type = _required_non_blank_string(item, "type", path=path, context=f"{object_name}.relation")
        target = _required_non_blank_string(item, "target", path=path, context=f"{object_name}.relation")
        via_field = _required_non_blank_string(item, "viaField", path=path, context=f"{object_name}.relation")
        key = (relation_type, target, via_field)
        if key in seen_relation_keys:
            raise ValueError(f"Duplicate relation {key} in object {object_name!r} of {path}")
        seen_relation_keys.add(key)
        parsed.append(RegistryRelation(relation_type=relation_type, target=target, via_field=via_field))
    return tuple(parsed)


def _validate_unique_names(objects: list[RegistryObject]) -> None:
    duplicated = sorted(name for name, count in Counter(obj.name for obj in objects).items() if count > 1)
    if duplicated:
        raise ValueError(f"Duplicate registry object names detected: {', '.join(duplicated)}")


def _validate_relation_targets(objects: list[RegistryObject]) -> list[str]:
    names = {obj.name for obj in objects}
    invalid: list[str] = []
    for obj in objects:
        for relation in obj.relations:
            if relation.target not in names:
                invalid.append(f"{obj.name}:{relation.via_field}->{relation.target}")
    if invalid:
        raise ValueError(
            "Relation targets not found in registry objects: " + ", ".join(sorted(invalid))
        )
    return invalid


def _required_non_blank_string(container: dict, key: str, path: Path, context: str) -> str:
    value = container.get(key)
    if value is None:
        raise ValueError(f"Missing required key {key!r} in {context} ({path})")
    text = str(value).strip()
    if not text:
        raise ValueError(f"Blank value for key {key!r} in {context} ({path})")
    return text


def _required_bool(container: dict, key: str, path: Path, context: str) -> bool:
    value = container.get(key, None)
    if not isinstance(value, bool):
        raise ValueError(f"Key {key!r} in {context} ({path}) must be a boolean")
    return value


def _is_blank(text: str) -> bool:
    return not str(text).strip()


def _is_english_only(text: str) -> bool:
    normalized = str(text).strip()
    if not normalized:
        return False
    if CHINESE_RE.search(normalized):
        return False
    if not ENGLISH_ALPHA_RE.search(normalized):
        return False
    return all(ord(char) < 128 for char in normalized)
