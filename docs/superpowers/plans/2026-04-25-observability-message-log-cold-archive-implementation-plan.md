# Observability Message Log Cold Archive Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a cold-archive-first governance flow for `iot_message_log`, including schema truth, governance registry coverage, archive batch evidence, and script-level archive-then-delete behavior.

**Architecture:** Keep `iot_message_log` as the active hot table and add two new MySQL truth objects: `iot_message_log_archive` for archived rows and `iot_message_log_archive_batch` for apply-time evidence. Extend the existing Python governance script to archive expired message rows before deleting them, and extend the governance registry/audit pipeline so the device domain can audit this hot-table-with-cold-archive pattern without affecting the current span/business-event delete-only path.

**Tech Stack:** Python 3, Node.js ESM, schema registry JSON, schema-governance JSON, MySQL/PyMySQL, unittest, node:test

---

### Task 1: Add schema and governance truth sources

**Files:**
- Modify: `schema/mysql/device-domain.json`
- Create: `schema-governance/device-domain.json`
- Modify: `scripts/tests/test_governance_registry.py`
- Modify: `scripts/tests/test_governance_tools.py`
- Test: `python3 scripts/tests/test_governance_registry.py`
- Test: `python3 scripts/tests/test_governance_tools.py`

- [ ] **Step 1: Write the failing governance-registry tests**

```python
def test_device_governance_domain_should_register_message_log_freeze_candidate(self):
    registry = governance_registry_loader.load_governance_registry(
        REPO_ROOT / "schema-governance",
        REPO_ROOT / "schema",
    )

    self.assertIn("device", registry.domains)
    device_domain = registry.domains["device"]
    self.assertIn("iot_message_log", device_domain.objects)

    object_entry = device_domain.objects["iot_message_log"]
    self.assertEqual("freeze_candidate", object_entry.governance_stage)
    self.assertEqual("mysql_hot_table_with_cold_archive", object_entry.real_env_audit_profile)

def test_render_domain_governance_ledger_should_include_device_message_log_governance_object(self):
    registry = load_registry()
    markdown = render_tools.render_domain_governance_ledger(registry)
    device_section = schema_contract_support.extract_markdown_section(markdown, "## Domain device")
    governance_rows = schema_contract_support.parse_markdown_table(
        device_section,
        "| Governance Object | Stage | Seed Packages | Audit Profile | Deletion Prerequisites | Notes |",
    )
    row = schema_contract_support.get_markdown_table_row(
        governance_rows,
        "Governance Object",
        "iot_message_log",
    )

    self.assertEqual("freeze_candidate", row["Stage"])
    self.assertEqual("mysql_hot_table_with_cold_archive", row["Audit Profile"])
```

- [ ] **Step 2: Run registry tests to verify they fail**

Run: `python3 scripts/tests/test_governance_registry.py`
Expected: FAIL because `schema-governance/device-domain.json` does not exist and `device` governance domain is missing.

- [ ] **Step 3: Run governance-tools tests to verify they fail**

Run: `python3 scripts/tests/test_governance_tools.py`
Expected: FAIL because device-domain governance rows do not contain `iot_message_log`.

- [ ] **Step 4: Add the new archive tables to the device schema registry**

```json
{
  "name": "iot_message_log_archive",
  "storageType": "mysql_table",
  "ownerModule": "spring-boot-iot-device",
  "lifecycle": "active",
  "includedInInit": true,
  "includedInSchemaSync": true,
  "runtimeBootstrapMode": "schema_sync_managed",
  "tableCommentZh": "设备消息日志冷归档表",
  "commentZh": "设备消息日志冷归档表",
  "lineageRole": "device_domain_archive",
  "businessBoundary": "用于保存超过热表保留期的设备消息原始证据，不参与默认业务读链路，只服务治理审计和人工导出。",
  "fields": [
    { "name": "id", "type": "BIGINT NOT NULL", "commentZh": "主键" },
    { "name": "original_log_id", "type": "BIGINT NOT NULL", "commentZh": "原热表日志ID" },
    { "name": "archive_batch_id", "type": "BIGINT NOT NULL", "commentZh": "归档批次ID" }
  ]
}
```

- [ ] **Step 5: Add the device governance registry file**

```json
{
  "domain": "device",
  "objects": [
    {
      "objectName": "iot_message_log",
      "storageType": "mysql_table",
      "governanceStage": "freeze_candidate",
      "ownerModule": "spring-boot-iot-device",
      "businessDomain": "device",
      "seedPackages": [],
      "realEnvAuditProfile": "mysql_hot_table_with_cold_archive",
      "backupRequirements": {
        "exportFormats": ["json", "csv", "sql"],
        "mustCaptureRowCount": true
      },
      "deletionPrerequisites": [
        "archive_table_ready",
        "archive_batch_recorded",
        "confirmed_report_matches_apply",
        "docs_and_registry_updated"
      ],
      "manualChecklist": [
        "确认冷归档批次与热表删除统计一致"
      ],
      "evidenceRefs": [
        "docs/04-数据库设计与初始化数据.md",
        "docs/08-变更记录与技术债清单.md",
        "docs/11-可观测性、日志追踪与消息通知治理.md"
      ],
      "notes": "设备消息日志热表进入冷归档治理阶段，保留 active 热表定位，不直接退场。"
    }
  ],
  "seedPackages": []
}
```

- [ ] **Step 6: Extend the governance tests to assert the new device domain and profile**

```python
self.assertIn("device", registry.domains)
device_domain = registry.domains["device"]
self.assertIn("iot_message_log", device_domain.objects)
object_entry = device_domain.objects["iot_message_log"]
self.assertEqual("freeze_candidate", object_entry.governance_stage)
self.assertEqual("mysql_hot_table_with_cold_archive", object_entry.real_env_audit_profile)
```

- [ ] **Step 7: Run registry tests to verify they pass**

Run: `python3 scripts/tests/test_governance_registry.py`
Expected: PASS

- [ ] **Step 8: Run governance-tools tests to verify they pass**

Run: `python3 scripts/tests/test_governance_tools.py`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add schema/mysql/device-domain.json schema-governance/device-domain.json scripts/tests/test_governance_registry.py scripts/tests/test_governance_tools.py
git commit -m "feat: add message log cold archive registry"
```

### Task 2: Add governance audit support for hot-table-with-cold-archive

**Files:**
- Modify: `scripts/governance/load_governance_registry.py`
- Modify: `scripts/governance/domain_audit_support.py`
- Modify: `scripts/governance/run_domain_audit.py`
- Modify: `scripts/governance/export_object_backup.py`
- Modify: `scripts/tests/test_governance_tools.py`
- Test: `python3 scripts/tests/test_governance_tools.py`

- [ ] **Step 1: Write the failing audit-support test**

```python
def test_evaluate_hot_table_archive_health_should_flag_missing_archive_tables(self):
    audit = tools.evaluate_hot_table_archive_health(
        {
            "hot_table_exists": True,
            "archive_table_exists": False,
            "batch_table_exists": False,
            "expired_rows": 12,
            "latest_batch": None,
        }
    )

    self.assertFalse(audit["ready"])
    self.assertIn("ARCHIVE_TABLE_MISSING", audit["blocking_reasons"])
    self.assertIn("ARCHIVE_BATCH_TABLE_MISSING", audit["blocking_reasons"])
```

- [ ] **Step 2: Run governance-tools tests to verify they fail**

Run: `python3 scripts/tests/test_governance_tools.py`
Expected: FAIL because `evaluate_hot_table_archive_health` and the new audit profile handling do not exist.

- [ ] **Step 3: Add the new supported audit profile**

```python
VALID_GOVERNANCE_STAGES = {
    "active",
    "freeze_candidate",
    "archived",
    "pending_delete",
    "dropped",
}
```

```python
if object_entry.real_env_audit_profile == "mysql_hot_table_with_cold_archive":
    audit = audit_mysql_hot_table_with_cold_archive(
        connection_args,
        object_entry.object_name,
        f"{object_entry.object_name}_archive",
        f"{object_entry.object_name}_archive_batch",
        args.sample_limit,
    )
```

- [ ] **Step 4: Implement the archive health audit helper**

```python
def evaluate_hot_table_archive_health(audit: dict[str, object]) -> dict[str, object]:
    blocking_reasons: list[str] = []
    if not audit.get("hot_table_exists"):
        blocking_reasons.append("HOT_TABLE_MISSING")
    if not audit.get("archive_table_exists"):
        blocking_reasons.append("ARCHIVE_TABLE_MISSING")
    if not audit.get("batch_table_exists"):
        blocking_reasons.append("ARCHIVE_BATCH_TABLE_MISSING")
    if int(audit.get("expired_rows") or 0) > 0 and not audit.get("latest_batch"):
        blocking_reasons.append("NO_ARCHIVE_BATCH_EVIDENCE")
    return {
        "ready": not blocking_reasons,
        "blocking_reasons": blocking_reasons,
    }
```

- [ ] **Step 5: Implement the hot-table audit collector**

```python
def audit_mysql_hot_table_with_cold_archive(
    connection_args: dict[str, object],
    hot_table_name: str,
    archive_table_name: str,
    batch_table_name: str,
    sample_limit: int,
) -> dict[str, object]:
    with pymysql.connect(..., cursorclass=pymysql.cursors.DictCursor) as connection:
        with connection.cursor() as cursor:
            hot_exists = table_exists(cursor, connection_args["database"], hot_table_name)
            archive_exists = table_exists(cursor, connection_args["database"], archive_table_name)
            batch_exists = table_exists(cursor, connection_args["database"], batch_table_name)
            ...
```

- [ ] **Step 6: Keep export behavior conservative**

```python
if object_entry.real_env_audit_profile == "mysql_hot_table_with_cold_archive":
    raise RuntimeError(
        "hot-table-with-cold-archive objects are audited in place; use archive batch evidence instead of export_object_backup."
    )
```

- [ ] **Step 7: Run governance-tools tests to verify they pass**

Run: `python3 scripts/tests/test_governance_tools.py`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add scripts/governance/load_governance_registry.py scripts/governance/domain_audit_support.py scripts/governance/run_domain_audit.py scripts/governance/export_object_backup.py scripts/tests/test_governance_tools.py
git commit -m "feat: audit message log cold archive governance"
```

### Task 3: Implement archive-first behavior in the observability governance script

**Files:**
- Modify: `config/automation/observability-log-governance-policy.json`
- Modify: `scripts/govern-observability-logs.py`
- Modify: `scripts/tests/test_govern_observability_logs.py`
- Test: `python3 scripts/tests/test_govern_observability_logs.py`

- [ ] **Step 1: Write the failing archive-first tests**

```python
def test_collect_governance_snapshot_applies_archive_before_delete(self):
    ...
    self.assertEqual(20, report["tables"]["iot_message_log"]["archivedRows"])
    self.assertEqual(20, report["tables"]["iot_message_log"]["deletedRows"])
    self.assertIsNotNone(report["tables"]["iot_message_log"]["archiveBatch"])

def test_collect_governance_snapshot_marks_failed_archive_batch_when_chunk_errors(self):
    ...
    self.assertEqual("FAILED", report["tables"]["iot_message_log"]["archiveBatch"]["status"])
```

- [ ] **Step 2: Run the Python governance tests to verify they fail**

Run: `python3 scripts/tests/test_govern_observability_logs.py`
Expected: FAIL because `archivedRows`, `archiveBatch`, and archive-first apply logic do not exist.

- [ ] **Step 3: Extend the policy with archive configuration**

```json
"iot_message_log": {
  "label": "设备消息日志",
  "timeField": "report_time",
  "retentionDays": 30,
  "deleteBatchSize": 500,
  "archiveEnabled": true,
  "archiveTable": "iot_message_log_archive",
  "archiveBatchTable": "iot_message_log_archive_batch",
  "archiveChunkSize": 500
}
```

- [ ] **Step 4: Add archive batch helpers and deterministic IDs**

```python
def build_batch_no(now: datetime, table_name: str) -> str:
    return f"{table_name}-{now.strftime('%Y%m%d%H%M%S')}"

def build_snowflake_like_id(now: datetime, sequence: int) -> int:
    return int(now.strftime("%Y%m%d%H%M%S%f")) * 100 + sequence
```

- [ ] **Step 5: Implement archive-then-delete for `iot_message_log`**

```python
if apply_mode and bool(config.get("archiveEnabled")):
    archive_result = archive_expired_message_rows(
        cur,
        table_name=table_name,
        archive_table=str(config["archiveTable"]),
        archive_batch_table=str(config["archiveBatchTable"]),
        cutoff_at=cutoff_at,
        batch_size=int(config.get("archiveChunkSize") or delete_batch_size),
        ...
    )
    deleted_rows = archive_result["deletedRows"]
    archived_rows = archive_result["archivedRows"]
```

- [ ] **Step 6: Record archive batch evidence in the report**

```python
return {
    ...
    "archivedRows": archived_rows,
    "archiveBatch": archive_batch,
}
```

- [ ] **Step 7: Run the Python governance tests to verify they pass**

Run: `python3 scripts/tests/test_govern_observability_logs.py`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add config/automation/observability-log-governance-policy.json scripts/govern-observability-logs.py scripts/tests/test_govern_observability_logs.py
git commit -m "feat: archive message logs before deletion"
```

### Task 4: Surface archive evidence in the Node wrapper and docs, then regenerate registries

**Files:**
- Modify: `scripts/auto/run-observability-log-governance.mjs`
- Modify: `scripts/auto/observability-log-governance.test.mjs`
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`
- Modify: `docs/appendix/database-schema-governance-catalog.generated.md`
- Modify: `docs/appendix/database-schema-domain-governance.generated.md`
- Modify: `schema/generated/mysql-schema-sync.json`
- Test: `node --test scripts/auto/observability-log-governance.test.mjs`
- Test: `python3 scripts/schema/check_schema_registry.py`
- Test: `python3 scripts/governance/check_governance_registry.py`

- [ ] **Step 1: Write the failing wrapper test**

```javascript
test('runObservabilityLogGovernanceCli returns archive batch summary when apply succeeds', async () => {
  const result = await runObservabilityLogGovernanceCli({
    ...,
    runGovernance: async () => ({
      exitCode: 0,
      jsonPath,
      markdownPath,
      report: {
        mode: 'APPLY',
        tables: {
          iot_message_log: {
            archiveBatch: {
              status: 'SUCCEEDED',
              batchNo: 'iot_message_log-20260425235959'
            }
          }
        }
      }
    })
  });

  assert.equal(result.archiveBatch.status, 'SUCCEEDED');
});
```

- [ ] **Step 2: Run the wrapper tests to verify they fail**

Run: `node --test scripts/auto/observability-log-governance.test.mjs`
Expected: FAIL because wrapper output does not expose archive batch summary.

- [ ] **Step 3: Extend the wrapper output**

```javascript
const archiveBatch =
  result.report?.tables?.iot_message_log?.archiveBatch || null;

return {
  ...,
  archiveBatch
};
```

- [ ] **Step 4: Regenerate schema and governance artifacts**

Run: `python scripts/schema/render_artifacts.py --write`
Expected: updates `schema/generated/mysql-schema-sync.json` and related generated outputs.

Run: `python scripts/governance/render_governance_docs.py --write`
Expected: updates governance appendices with the new `device` domain row.

- [ ] **Step 5: Update operator and schema docs**

```markdown
- `iot_message_log_archive`：设备消息日志冷归档表，保存超过热表保留期的历史原始消息证据。
- `iot_message_log_archive_batch`：冷归档 apply 批次台账，记录确认报告、归档行数、删除行数和失败原因。
- `node scripts/auto/run-observability-log-governance.mjs --mode=apply ...` 当前对 `iot_message_log` 固定执行“先 archive 后 delete”。
```

- [ ] **Step 6: Run wrapper tests to verify they pass**

Run: `node --test scripts/auto/observability-log-governance.test.mjs`
Expected: PASS

- [ ] **Step 7: Run schema and governance checkers**

Run: `python3 scripts/schema/check_schema_registry.py`
Expected: PASS

Run: `python3 scripts/governance/check_governance_registry.py`
Expected: PASS

- [ ] **Step 8: Run domain audit for the device domain**

Run: `python3 scripts/governance/run_domain_audit.py --domain device`
Expected: PASS and JSON includes `iot_message_log`, archive-table existence, batch-table existence, and archive health summary.

- [ ] **Step 9: Run final focused regression**

Run: `python3 scripts/tests/test_govern_observability_logs.py`
Expected: PASS

Run: `python3 scripts/tests/test_governance_registry.py`
Expected: PASS

Run: `python3 scripts/tests/test_governance_tools.py`
Expected: PASS

Run: `node --test scripts/auto/observability-log-governance.test.mjs`
Expected: PASS

- [ ] **Step 10: Commit**

```bash
git add scripts/auto/run-observability-log-governance.mjs scripts/auto/observability-log-governance.test.mjs README.md AGENTS.md docs/04-数据库设计与初始化数据.md docs/07-部署运行与配置说明.md docs/08-变更记录与技术债清单.md docs/11-可观测性、日志追踪与消息通知治理.md schema/generated/mysql-schema-sync.json docs/appendix/database-schema-governance-catalog.generated.md docs/appendix/database-schema-domain-governance.generated.md docs/superpowers/plans/2026-04-25-observability-message-log-cold-archive-implementation-plan.md
git commit -m "feat: add message log cold archive governance"
```
