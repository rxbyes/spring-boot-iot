# Protocol Governance Write-Side Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `/protocol-governance` 从“只读列表 + 审批入口”补齐为“草稿新增/编辑 + 解密试算”的最小可运营工作台。

**Architecture:** 保持现有后端协议治理接口不变，前端直接复用 `POST /api/governance/protocol/families`、`POST /api/governance/protocol/decrypt-profiles` 与 `POST /api/governance/protocol/decrypt-profiles/preview`。页面继续留在单一路由 `/protocol-governance`，在既有 `协议族定义`、`解密档案` 两个 `StandardWorkbenchPanel` 内补齐写侧表单与试算区，不新增第二层路由或独立抽屉。

**Tech Stack:** Vue 3、TypeScript、Vitest、现有 `StandardWorkbenchPanel` / `StandardButton`

---

### Task 1: Extend protocol governance API/types for write-side payloads

**Files:**
- Modify: `spring-boot-iot-ui/src/api/protocolGovernance.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`

- [ ] **Step 1: Add write-side payload types**
- [ ] **Step 2: Add family/decrypt profile save API helpers**
- [ ] **Step 3: Keep decrypt preview API typed for the workbench preview state**

### Task 2: Add failing protocol governance write-side tests

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProtocolGovernanceWorkbenchView.test.ts`

- [ ] **Step 1: Add a failing test for creating/updating a protocol family draft from the workbench**
- [ ] **Step 2: Run the targeted view test and verify RED**
- [ ] **Step 3: Add a failing test for creating a decrypt profile draft and running decrypt preview**
- [ ] **Step 4: Run the targeted view test and verify RED**

### Task 3: Implement the protocol governance write-side workbench

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ProtocolGovernanceWorkbenchView.vue`

- [ ] **Step 1: Add protocol family draft editor state, save action, and edit-entry loading**
- [ ] **Step 2: Run the targeted view test and verify GREEN for family draft editing**
- [ ] **Step 3: Add decrypt profile draft editor state, save action, and edit-entry loading**
- [ ] **Step 4: Add decrypt preview form/result state inside the decrypt profile panel**
- [ ] **Step 5: Run the targeted view test and verify GREEN for decrypt profile editing + preview**
- [ ] **Step 6: Refine copy and layout so `/protocol-governance` still reads as a workbench, not a second overview**

### Task 4: Update docs and verify the full slice

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: Update docs to state `/protocol-governance` now supports draft save/edit and decrypt preview**
- [ ] **Step 2: Run `npm --prefix spring-boot-iot-ui test -- --run ProtocolGovernanceWorkbenchView`**
- [ ] **Step 3: Run `node scripts/docs/check-topology.mjs`**
- [ ] **Step 4: Run `node scripts/run-governance-contract-gates.mjs`**
- [ ] **Step 5: Run `git diff --check`**
