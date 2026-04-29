# System Log V2 Archive Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade `/system-log`'s `归档治理` tab into a V2 workbench with a governance focus strip, a compact batch master table, persistent selected-row context, and the existing compare/report drawer flow.

**Architecture:** Keep the current archive data APIs, overview-card filtering, and detail drawer wiring in `AuditLogView.vue`, but reshape the tab surface inside `AuditLogArchiveTabPanel.vue` so the main object is the selected archive batch row. Track the selected batch key in the view, keep summary-card deep links working, and verify the new behavior with focused component/view tests before changing production code.

**Tech Stack:** Vue 3 `<script setup>`, Element Plus, Vitest, existing `StandardButton` workbench actions, local `/system-log` state in `AuditLogView.vue`

---

### Task 1: Lock the archive-governance workbench behavior with failing tests

**Files:**
- Create: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: Add a component test for the archive focus strip and selected master row**
- [ ] **Step 2: Add a view test that preserves the selected archive batch when switching away and back**
- [ ] **Step 3: Add a view test that keeps the latest-abnormal summary deep link selecting the matching row**
- [ ] **Step 4: Run the targeted Vitest slice and confirm the new assertions fail**

### Task 2: Rebuild the archive tab around a focus strip and compact master table

**Files:**
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogArchiveTabPanel.vue`
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`

- [ ] **Step 1: Add archive selected-row props and emits to the tab panel**
- [ ] **Step 2: Replace the archive card list with a compact master table and focus strip**
- [ ] **Step 3: Keep overview cards, filters, and the detail action, but visually subordinate them to the main ledger**
- [ ] **Step 4: Track `selectedMessageArchiveBatchKey` in `AuditLogView.vue` and sync it after refreshes**
- [ ] **Step 5: Make summary-card auto-focus and manual detail opens update the selected row**

### Task 3: Verify the V2 archive slice end to end

**Files:**
- Test: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: Run archive-focused Vitest coverage**

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts src/__tests__/views/AuditLogView.test.ts
```

- [ ] **Step 2: Run the UI production build**

```powershell
npm run build
```

- [ ] **Step 3: Sanity-check that only the `system-log` archive workbench files were touched for this slice**
