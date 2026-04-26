# Observability Archive Batch Ops Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans or superpowers:subagent-driven-development if this plan is delegated later. In this session we implement directly in one isolated worktree.

**Goal:** Close the shared-env schema-sync blocker caused by `sys_dict.uk_dict_code_tenant` index uniqueness drift, and add a minimal observability read-side page API for `iot_message_log_archive_batch`.

**Architecture:** Keep the existing schema registry and F2 cold-archive pipeline unchanged. Extend `scripts/run-real-env-schema-sync.py` with a narrowly scoped auto-repair path for whitelisted repairable index drifts, and extend the current `/api/system/observability/**` stack with a tenant-scoped page query for archive batches.

**Tech Stack:** Python 3, Java 17, Spring Boot 4, JdbcTemplate, TypeScript, Vitest, JUnit 5

---

### Task 1: Lock the design and add failing tests

**Files:**
- Create: `docs/superpowers/specs/2026-04-26-observability-archive-batch-ops-design.md`
- Create: `docs/superpowers/plans/2026-04-26-observability-archive-batch-ops-implementation-plan.md`
- Modify: `scripts/tests/test_run_real_env_schema_sync.py`
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceControllerTest.java`
- Modify: `spring-boot-iot-system/src/test/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImplTest.java`
- Modify: `spring-boot-iot-ui/src/__tests__/api/observability.test.ts`

- [ ] Add Python tests for “same columns, uniqueness drift” repair eligibility and duplicate-guard behavior.
- [ ] Add backend controller/service tests for the new `message-archive-batches/page` endpoint.
- [ ] Add frontend API tests for the new observability batch page request builder.

### Task 2: Implement schema-sync repair for whitelisted uniqueness drift

**Files:**
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `scripts/tests/test_run_real_env_schema_sync.py`

- [ ] Introduce a whitelist for repairable index drifts, initially covering `("sys_dict", "uk_dict_code_tenant")`.
- [ ] Detect the exact drift shape: same column tuple, uniqueness differs.
- [ ] If expected target is unique, reuse duplicate-row guard before rebuilding.
- [ ] Rebuild the index with `DROP INDEX` + expected `ADD INDEX` DDL when safe.
- [ ] Preserve fatal behavior for all non-whitelisted or structurally different drifts.

### Task 3: Add archive batch read-side to the observability service

**Files:**
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/controller/ObservabilityEvidenceController.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/ObservabilityEvidenceQueryService.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/ObservabilityEvidenceQueryServiceImpl.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/model/ObservabilityMessageArchiveBatchPageQuery.java`
- Create: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/vo/ObservabilityMessageArchiveBatchVO.java`
- Modify: related tests

- [ ] Add a tenant-scoped page query for `iot_message_log_archive_batch`.
- [ ] Support filters `batchNo / sourceTable / status / dateFrom / dateTo`.
- [ ] Return the batch evidence fields required for operator reconciliation.
- [ ] Keep SQL style, paging behavior, and auth semantics aligned with existing observability endpoints.

### Task 4: Add frontend typed API support

**Files:**
- Modify: `spring-boot-iot-ui/src/api/observability.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/api/observability.test.ts`

- [ ] Add TypeScript interfaces for archive batch query/row types.
- [ ] Add `pageObservabilityMessageArchiveBatches(...)`.
- [ ] Verify query-string encoding for batch filters and date range params.

### Task 5: Update docs and verify end to end

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`

- [ ] Update docs with the new observability API and schema-sync repair behavior.
- [ ] Run local tests for Python, backend, and frontend slices.
- [ ] Re-run shared-env `python3 scripts/run-real-env-schema-sync.py`.
- [ ] If available, verify the archive batch page query against real data in shared env.

### Verification checklist

- [ ] `python3 -m unittest scripts.tests.test_run_real_env_schema_sync`
- [ ] `mvn -pl spring-boot-iot-system -Dtest=ObservabilityEvidenceControllerTest,ObservabilityEvidenceQueryServiceImplTest test`
- [ ] `cd spring-boot-iot-ui && npm test -- --run src/__tests__/api/observability.test.ts`
- [ ] `python3 scripts/run-real-env-schema-sync.py` against shared dev env

### Notes

- Do not modify unrelated dirty files in the main workspace.
- Do not widen the auto-repair scope beyond whitelisted uniqueness drift in this phase.
- Do not add a new UI page in this phase; typed API support is enough for F3.
