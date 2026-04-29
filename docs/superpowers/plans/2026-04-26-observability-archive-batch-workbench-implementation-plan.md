# Observability Archive Batch Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` or `superpowers:subagent-driven-development` if this plan is delegated later. In this session we implement directly in one isolated worktree.

**Goal:** Extend `/system-log` so operators can inspect `iot_message_log_archive_batch` results from the existing anomaly workbench instead of relying on scripts or direct SQL.

**Architecture:** Reuse the existing `GET /api/system/observability/message-archive-batches/page` backend and the typed client in `spring-boot-iot-ui/src/api/observability.ts`. Add a new archive-batch ledger panel and detail drawer inside `AuditLogView.vue`, then update tests and docs.

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vitest, Markdown docs

---

### Task 1: Lock the design and document the phase boundary

**Files:**
- Create: `docs/superpowers/specs/2026-04-26-observability-archive-batch-workbench-design.md`
- Create: `docs/superpowers/plans/2026-04-26-observability-archive-batch-workbench-implementation-plan.md`

- [ ] Record why G1 lands in `/system-log`, not `/automation-governance`.
- [ ] Fix the scope to “reuse existing API + same-page drawer,” with no new backend endpoint.

### Task 2: Add archive batch ledger and drawer to `AuditLogView`

**Files:**
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`

- [ ] Import `pageObservabilityMessageArchiveBatches` and archive-batch types.
- [ ] Add system-mode ledger state, loading, error, total, and active-detail refs.
- [ ] Load the latest `iot_message_log` archive batches on mount, refresh, and system-mode state changes.
- [ ] Render a `归档批次台账` panel alongside slow hotspots and scheduled-task ledger.
- [ ] Add a same-page detail drawer that shows batch summary, confirm-report info, row counts, and parsed `artifactsJson`.

### Task 3: Extend the UI regression test

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] Mock `pageObservabilityMessageArchiveBatches`.
- [ ] Assert the default request params.
- [ ] Assert the new ledger renders recent batch data.
- [ ] Assert clicking `详情` opens the batch drawer with confirm-report and artifact details.

### Task 4: Sync documentation

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`

- [ ] Update the observability capability summary to include the archive-batch workbench panel.
- [ ] Update `/system-log` behavior descriptions and changelog entries.

### Task 5: Verify and integrate

**Files:**
- No functional code changes expected beyond Task 2/3/4

- [ ] Run `spring-boot-iot-ui/node_modules/.bin/vitest --run src/__tests__/views/AuditLogView.test.ts`
- [ ] Review `git diff` for scope control.
- [ ] Commit in the isolated worktree and fast-forward merge back to `codex/dev`.

### Verification checklist

- [ ] `spring-boot-iot-ui/node_modules/.bin/vitest --run src/__tests__/views/AuditLogView.test.ts`

### Notes

- Keep all changes out of the dirty main workspace.
- Do not add a new route or governance page in G1.
- Do not widen this phase into archive-message search or report-file preview.
