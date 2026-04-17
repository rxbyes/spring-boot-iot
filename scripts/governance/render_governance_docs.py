#!/usr/bin/env python3
"""Render governance catalog appendix from schema-governance registry."""

from __future__ import annotations

import argparse
from collections import Counter
from pathlib import Path
import sys

REPO_ROOT = Path(__file__).resolve().parents[2]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from scripts.governance.load_governance_registry import GovernanceRegistry, load_governance_registry
from scripts.schema.load_registry import RegistryObject

OUTPUT_PATH = REPO_ROOT / "docs" / "appendix" / "database-schema-governance-catalog.generated.md"
DOMAIN_LEDGER_OUTPUT_PATH = REPO_ROOT / "docs" / "appendix" / "database-schema-domain-governance.generated.md"
OUTPUT_FILES = {
    "catalog_markdown": OUTPUT_PATH,
    "domain_ledger_markdown": DOMAIN_LEDGER_OUTPUT_PATH,
}


def render_governance_catalog(registry: GovernanceRegistry) -> str:
    lines = [
        "# Database Schema Governance Catalog",
        "",
        "Generated from the schema-governance registry. Do not edit by hand.",
        "",
        "| Domain | Object | Stage | Seed Packages | Audit Profile | Owner Module | Notes |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]
    for domain_name in sorted(registry.domains.keys()):
        domain = registry.domains[domain_name]
        for object_name in sorted(domain.objects.keys()):
            item = domain.objects[object_name]
            lines.append(
                "| {domain} | {object_name} | {stage} | {seed_packages} | {profile} | {owner} | {notes} |".format(
                    domain=domain_name,
                    object_name=item.object_name,
                    stage=item.governance_stage,
                    seed_packages=", ".join(item.seed_packages) or "-",
                    profile=item.real_env_audit_profile,
                    owner=item.owner_module,
                    notes=item.notes.replace("|", "\\|"),
                )
            )
    return "\n".join(lines).strip() + "\n"


def render_domain_governance_ledger(registry: GovernanceRegistry) -> str:
    lines = [
        "# Database Schema Domain Governance Ledger",
        "",
        "Generated from the schema and schema-governance registries. Do not edit by hand.",
    ]
    for domain_name in _all_domain_names(registry):
        structure_objects = _structure_objects_for_domain(registry, domain_name)
        governance_objects = _governance_objects_for_domain(registry, domain_name)
        lines.extend(
            [
                "",
                f"## Domain {domain_name}",
                "",
                "| Metric | Value |",
                "| --- | --- |",
                f"| Objects | {len(structure_objects)} |",
                f"| Relations | {sum(len(obj.relations) for obj in structure_objects)} |",
                f"| Owner Modules | {_format_counter(Counter(obj.owner_module for obj in structure_objects))} |",
                f"| Lineage Roles | {_format_counter(Counter(obj.lineage_role for obj in structure_objects))} |",
                "",
                "| Lifecycle | Count |",
                "| --- | --- |",
                *_render_lifecycle_rows(structure_objects),
                "",
                "| Object | Storage | Lifecycle | In Init | In Schema Sync | Runtime Bootstrap | Owner Module | Comment |",
                "| --- | --- | --- | --- | --- | --- | --- | --- |",
                *_render_object_rows(structure_objects),
                "",
                "| Governance Object | Stage | Seed Packages | Audit Profile | Deletion Prerequisites | Notes |",
                "| --- | --- | --- | --- | --- | --- |",
                *_render_governance_rows(governance_objects),
                "",
                "| Object | Relations |",
                "| --- | --- |",
                *_render_relation_rows(structure_objects),
                "",
                "```mermaid",
                "graph TD",
                *_render_domain_mermaid_lines(structure_objects),
                "```",
                "",
                "当前域如有真实库审计结论，请查看 `docs/04` 对应对象条目；带日期的最近审计事实与阶段性决策请查看 `docs/08`。",
            ]
        )
    return "\n".join(lines).strip() + "\n"


def _all_domain_names(registry: GovernanceRegistry) -> list[str]:
    names = {obj.domain for obj in registry.structure_registry.all_objects}
    names.update(registry.domains.keys())
    return sorted(names)


def _structure_objects_for_domain(registry: GovernanceRegistry, domain_name: str) -> list[RegistryObject]:
    return sorted(
        [obj for obj in registry.structure_registry.all_objects if obj.domain == domain_name],
        key=lambda item: (item.storage_type, item.name),
    )


def _governance_objects_for_domain(registry: GovernanceRegistry, domain_name: str):
    domain = registry.domains.get(domain_name)
    if domain is None:
        return []
    return [domain.objects[name] for name in sorted(domain.objects.keys())]


def _format_counter(counter: Counter[str]) -> str:
    if not counter:
        return "-"
    return ", ".join(f"{key}({counter[key]})" for key in sorted(counter.keys()))


def _render_lifecycle_rows(structure_objects: list[RegistryObject]) -> list[str]:
    counts = Counter(obj.lifecycle for obj in structure_objects)
    return [f"| {lifecycle} | {counts.get(lifecycle, 0)} |" for lifecycle in ("active", "archived", "pending_delete")]


def _render_object_rows(structure_objects: list[RegistryObject]) -> list[str]:
    return [
        "| {name} | {storage} | {lifecycle} | {in_init} | {in_sync} | {runtime} | {owner} | {comment} |".format(
            name=obj.name,
            storage=obj.storage_type,
            lifecycle=obj.lifecycle,
            in_init="yes" if obj.included_in_init else "no",
            in_sync="yes" if obj.included_in_schema_sync else "no",
            runtime=obj.runtime_bootstrap_mode,
            owner=obj.owner_module,
            comment=obj.comment_zh.replace("|", "\\|"),
        )
        for obj in structure_objects
    ]


def _render_governance_rows(governance_objects) -> list[str]:
    if not governance_objects:
        return ["| 当前无登记治理对象 | - | - | - | - | - |"]
    return [
        "| {name} | {stage} | {seed_packages} | {profile} | {prerequisites} | {notes} |".format(
            name=obj.object_name,
            stage=obj.governance_stage,
            seed_packages=", ".join(obj.seed_packages) or "-",
            profile=obj.real_env_audit_profile,
            prerequisites="<br>".join(obj.deletion_prerequisites),
            notes=obj.notes.replace("|", "\\|"),
        )
        for obj in governance_objects
    ]


def _render_relation_rows(structure_objects: list[RegistryObject]) -> list[str]:
    return [
        "| {name} | {relations} |".format(
            name=obj.name,
            relations=_relation_summary(obj),
        )
        for obj in structure_objects
    ]


def _relation_summary(obj: RegistryObject) -> str:
    if not obj.relations:
        return "-"
    return "<br>".join(
        f"{relation.target}（{relation.relation_type}:{relation.via_field}）"
        for relation in obj.relations
    )


def _render_domain_mermaid_lines(structure_objects: list[RegistryObject]) -> list[str]:
    lines: list[str] = []
    declared_nodes: set[str] = set()
    for obj in structure_objects:
        source_id = _mermaid_node_id(obj.name)
        if source_id not in declared_nodes:
            lines.append(f'  {source_id}["{obj.name}"]')
            declared_nodes.add(source_id)
        for relation in obj.relations:
            target_id = _mermaid_node_id(relation.target)
            if target_id not in declared_nodes:
                lines.append(f'  {target_id}["{relation.target}"]')
                declared_nodes.add(target_id)
            lines.append(
                f'  {source_id}["{obj.name}"] -->|"{relation.relation_type} via {relation.via_field}"| '
                f'{target_id}["{relation.target}"]'
            )
    return lines or ['  empty_domain["no relations"]']


def _mermaid_node_id(name: str) -> str:
    return "".join(ch if ch.isalnum() or ch == "_" else "_" for ch in name)


def render_governance_documents(registry: GovernanceRegistry) -> dict[str, str]:
    return {
        "catalog_markdown": render_governance_catalog(registry),
        "domain_ledger_markdown": render_domain_governance_ledger(registry),
    }


def materialize_governance_docs(
    governance_root: str | Path,
    structure_root: str | Path,
    output_paths: dict[str, str | Path] | None = None,
) -> dict[Path, str]:
    registry = load_governance_registry(governance_root, structure_root)
    bundle = render_governance_documents(registry)
    resolved_output_paths = output_paths or OUTPUT_FILES
    return {
        Path(resolved_output_paths[key]).resolve(): bundle[key]
        for key in ("catalog_markdown", "domain_ledger_markdown")
    }


def write_governance_docs(
    governance_root: str | Path,
    structure_root: str | Path,
    output_paths: dict[str, str | Path] | None = None,
) -> list[Path]:
    rendered = materialize_governance_docs(governance_root, structure_root, output_paths=output_paths)
    written: list[Path] = []
    for path, content in rendered.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        current = path.read_text(encoding="utf-8") if path.exists() else None
        if current != content:
            path.write_text(content, encoding="utf-8", newline="\n")
            written.append(path)
    return written


def check_governance_docs(
    governance_root: str | Path,
    structure_root: str | Path,
    output_paths: dict[str, str | Path] | None = None,
) -> list[Path]:
    rendered = materialize_governance_docs(governance_root, structure_root, output_paths=output_paths)
    mismatches: list[Path] = []
    for path, expected in rendered.items():
        if not path.exists() or path.read_text(encoding="utf-8") != expected:
            mismatches.append(path)
    return mismatches


def main() -> int:
    parser = argparse.ArgumentParser(description="Render governance appendix markdown from schema-governance registry.")
    parser.add_argument("--write", action="store_true", help="Write generated markdown to docs/appendix.")
    args = parser.parse_args()

    if args.write:
        write_governance_docs(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")
    else:
        registry = load_governance_registry(REPO_ROOT / "schema-governance", REPO_ROOT / "schema")
        markdown = render_governance_catalog(registry)
        domain_markdown = render_domain_governance_ledger(registry)
        print(markdown)
        print()
        print(domain_markdown)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
