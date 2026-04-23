from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

from scripts.schema.load_registry import SchemaRegistry, load_registry


VALID_GOVERNANCE_STAGES = {
    "active",
    "freeze_candidate",
    "archived",
    "pending_delete",
    "dropped",
}
VALID_SEED_USAGES = {"共享环境基线", "演示数据", "历史归档"}
REQUIRED_OBJECT_KEYS = {
    "objectName",
    "storageType",
    "governanceStage",
    "ownerModule",
    "businessDomain",
    "seedPackages",
    "realEnvAuditProfile",
    "backupRequirements",
    "deletionPrerequisites",
    "manualChecklist",
    "evidenceRefs",
    "notes",
}
REQUIRED_SEED_KEYS = {
    "name",
    "sourceFiles",
    "usage",
    "allowedInInitData",
    "boundObjects",
}


@dataclass(frozen=True)
class GovernanceBackupRequirements:
    export_formats: tuple[str, ...]
    must_capture_row_count: bool


@dataclass(frozen=True)
class GovernanceSeedPackage:
    name: str
    source_files: tuple[str, ...]
    usage: str
    allowed_in_init_data: bool
    bound_objects: tuple[str, ...]


@dataclass(frozen=True)
class GovernanceObject:
    object_name: str
    storage_type: str
    governance_stage: str
    owner_module: str
    business_domain: str
    seed_packages: tuple[str, ...]
    real_env_audit_profile: str
    backup_requirements: GovernanceBackupRequirements
    deletion_prerequisites: tuple[str, ...]
    manual_checklist: tuple[str, ...]
    evidence_refs: tuple[str, ...]
    notes: str


@dataclass(frozen=True)
class GovernanceDomain:
    domain: str
    objects: dict[str, GovernanceObject]
    seed_packages: dict[str, GovernanceSeedPackage]


@dataclass(frozen=True)
class GovernanceRegistry:
    domains: dict[str, GovernanceDomain]
    structure_registry: SchemaRegistry


def load_governance_registry(governance_root: str | Path, structure_root: str | Path) -> GovernanceRegistry:
    governance_dir = Path(governance_root).resolve()
    structure_registry = load_registry(structure_root)
    structure_objects = {obj.name: obj for obj in structure_registry.all_objects}

    domains: dict[str, GovernanceDomain] = {}
    for path in sorted(governance_dir.glob("*.json")):
        payload = json.loads(path.read_text(encoding="utf-8"))
        domain_name = _required_non_blank_string(payload, "domain", path, "file")
        if domain_name in domains:
            raise ValueError(f"Duplicate governance domain {domain_name!r} in {path}")

        raw_seed_packages = payload.get("seedPackages", [])
        if not isinstance(raw_seed_packages, list):
            raise ValueError(f"Governance file {path} seedPackages must be a list")
        seed_packages = {
            package.name: package
            for package in (
                _parse_seed_package(raw_seed_package, path, domain_name, structure_objects)
                for raw_seed_package in raw_seed_packages
            )
        }
        if len(seed_packages) != len(raw_seed_packages):
            raise ValueError(f"Governance file {path} contains duplicate seed package names")

        raw_objects = payload.get("objects")
        if not isinstance(raw_objects, list) or not raw_objects:
            raise ValueError(f"Governance file {path} must contain a non-empty objects list")
        objects = {
            item.object_name: item
            for item in (
                _parse_object(raw_object, path, domain_name, structure_objects, seed_packages)
                for raw_object in raw_objects
            )
        }
        if len(objects) != len(raw_objects):
            raise ValueError(f"Governance file {path} contains duplicate object names")

        domains[domain_name] = GovernanceDomain(
            domain=domain_name,
            objects=objects,
            seed_packages=seed_packages,
        )

    if not domains:
        raise ValueError(f"No governance registry files found under {governance_dir}")

    return GovernanceRegistry(domains=domains, structure_registry=structure_registry)


def _parse_seed_package(
    raw_seed_package: object,
    path: Path,
    domain_name: str,
    structure_objects: dict[str, object],
) -> GovernanceSeedPackage:
    if not isinstance(raw_seed_package, dict):
        raise ValueError(f"Seed package in {path} must be a JSON object")
    missing = sorted(key for key in REQUIRED_SEED_KEYS if key not in raw_seed_package)
    if missing:
        raise ValueError(f"Seed package in {path} missing required keys: {', '.join(missing)}")

    name = _required_non_blank_string(raw_seed_package, "name", path, f"{domain_name}.seedPackage")
    usage = _required_non_blank_string(raw_seed_package, "usage", path, f"{domain_name}.{name}")
    if usage not in VALID_SEED_USAGES:
        raise ValueError(
            f"Seed package {name!r} in {path} has invalid usage {usage!r}; "
            f"valid values are {sorted(VALID_SEED_USAGES)}"
        )
    source_files = _required_non_empty_string_list(raw_seed_package, "sourceFiles", path, f"{domain_name}.{name}")
    bound_objects = _required_non_empty_string_list(raw_seed_package, "boundObjects", path, f"{domain_name}.{name}")
    for object_name in bound_objects:
        if object_name not in structure_objects:
            raise ValueError(
                f"Seed package {name!r} in {path} references missing structure object {object_name!r}"
            )
    allowed_in_init_data = _required_bool(raw_seed_package, "allowedInInitData", path, f"{domain_name}.{name}")
    return GovernanceSeedPackage(
        name=name,
        source_files=source_files,
        usage=usage,
        allowed_in_init_data=allowed_in_init_data,
        bound_objects=bound_objects,
    )


def _parse_object(
    raw_object: object,
    path: Path,
    domain_name: str,
    structure_objects: dict[str, object],
    seed_packages: dict[str, GovernanceSeedPackage],
) -> GovernanceObject:
    if not isinstance(raw_object, dict):
        raise ValueError(f"Governance object in {path} must be a JSON object")
    missing = sorted(key for key in REQUIRED_OBJECT_KEYS if key not in raw_object)
    if missing:
        raise ValueError(f"Governance object in {path} missing required keys: {', '.join(missing)}")

    object_name = _required_non_blank_string(raw_object, "objectName", path, f"{domain_name}.object")
    stage = _required_non_blank_string(raw_object, "governanceStage", path, object_name)
    if stage not in VALID_GOVERNANCE_STAGES:
        raise ValueError(
            f"Governance object {object_name!r} in {path} has invalid governanceStage {stage!r}; "
            f"valid values are {sorted(VALID_GOVERNANCE_STAGES)}"
        )

    structure_object = structure_objects.get(object_name)
    if stage != "dropped" and structure_object is None:
        raise ValueError(f"Governance object {object_name!r} in {path} not found in structure registry")
    if structure_object is not None:
        _validate_stage_against_structure(stage, structure_object.lifecycle, object_name, path)
        storage_type = _required_non_blank_string(raw_object, "storageType", path, object_name)
        if storage_type != structure_object.storage_type:
            raise ValueError(
                f"Governance object {object_name!r} in {path} storageType={storage_type!r} "
                f"does not match structure registry {structure_object.storage_type!r}"
            )
        owner_module = _required_non_blank_string(raw_object, "ownerModule", path, object_name)
        if owner_module != structure_object.owner_module:
            raise ValueError(
                f"Governance object {object_name!r} in {path} ownerModule={owner_module!r} "
                f"does not match structure registry {structure_object.owner_module!r}"
            )
    else:
        storage_type = _required_non_blank_string(raw_object, "storageType", path, object_name)
        owner_module = _required_non_blank_string(raw_object, "ownerModule", path, object_name)

    business_domain = _required_non_blank_string(raw_object, "businessDomain", path, object_name)
    seed_package_names = _optional_string_list(raw_object, "seedPackages", path, object_name)
    for seed_package_name in seed_package_names:
        if seed_package_name not in seed_packages:
            raise ValueError(
                f"Governance object {object_name!r} in {path} references missing seed package {seed_package_name!r}"
            )

    backup_requirements = _parse_backup_requirements(raw_object.get("backupRequirements"), path, object_name)
    deletion_prerequisites = _required_non_empty_string_list(
        raw_object,
        "deletionPrerequisites",
        path,
        object_name,
    )
    manual_checklist = _required_non_empty_string_list(raw_object, "manualChecklist", path, object_name)
    evidence_refs = _required_non_empty_string_list(raw_object, "evidenceRefs", path, object_name)
    notes = _required_non_blank_string(raw_object, "notes", path, object_name)
    real_env_audit_profile = _required_non_blank_string(raw_object, "realEnvAuditProfile", path, object_name)

    return GovernanceObject(
        object_name=object_name,
        storage_type=storage_type,
        governance_stage=stage,
        owner_module=owner_module,
        business_domain=business_domain,
        seed_packages=seed_package_names,
        real_env_audit_profile=real_env_audit_profile,
        backup_requirements=backup_requirements,
        deletion_prerequisites=deletion_prerequisites,
        manual_checklist=manual_checklist,
        evidence_refs=evidence_refs,
        notes=notes,
    )


def _parse_backup_requirements(raw_value: object, path: Path, object_name: str) -> GovernanceBackupRequirements:
    if not isinstance(raw_value, dict):
        raise ValueError(f"Governance object {object_name!r} in {path} backupRequirements must be an object")
    export_formats = _required_non_empty_string_list(raw_value, "exportFormats", path, object_name)
    must_capture_row_count = _required_bool(raw_value, "mustCaptureRowCount", path, object_name)
    return GovernanceBackupRequirements(
        export_formats=export_formats,
        must_capture_row_count=must_capture_row_count,
    )


def _validate_stage_against_structure(
    governance_stage: str,
    structure_lifecycle: str,
    object_name: str,
    path: Path,
) -> None:
    if governance_stage in {"active", "freeze_candidate"} and structure_lifecycle != "active":
        raise ValueError(
            f"Governance object {object_name!r} in {path} stage {governance_stage!r} "
            f"requires structure lifecycle 'active', found {structure_lifecycle!r}"
        )
    if governance_stage == "archived" and structure_lifecycle != "archived":
        raise ValueError(
            f"Governance object {object_name!r} in {path} stage 'archived' "
            f"requires structure lifecycle 'archived', found {structure_lifecycle!r}"
        )
    if governance_stage == "pending_delete" and structure_lifecycle != "pending_delete":
        raise ValueError(
            f"Governance object {object_name!r} in {path} stage 'pending_delete' "
            f"requires structure lifecycle 'pending_delete', found {structure_lifecycle!r}"
        )


def _required_non_blank_string(container: dict, key: str, path: Path, context: str) -> str:
    value = container.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"Key {key!r} in {context} ({path}) must be a non-blank string")
    return value.strip()


def _required_non_empty_string_list(container: dict, key: str, path: Path, context: str) -> tuple[str, ...]:
    values = _optional_string_list(container, key, path, context)
    if not values:
        raise ValueError(f"Key {key!r} in {context} ({path}) must be a non-empty string list")
    return values


def _optional_string_list(container: dict, key: str, path: Path, context: str) -> tuple[str, ...]:
    value = container.get(key, [])
    if not isinstance(value, list):
        raise ValueError(f"Key {key!r} in {context} ({path}) must be a list")
    items: list[str] = []
    for index, item in enumerate(value):
        if not isinstance(item, str) or not item.strip():
            raise ValueError(f"Key {key!r}[{index}] in {context} ({path}) must be a non-blank string")
        items.append(item.strip())
    return tuple(items)


def _required_bool(container: dict, key: str, path: Path, context: str) -> bool:
    value = container.get(key)
    if not isinstance(value, bool):
        raise ValueError(f"Key {key!r} in {context} ({path}) must be a boolean")
    return value
