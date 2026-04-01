# Product Workbench Director Journal A Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the `/products` unified business workbench into the approved A-direction “director journal” layout while keeping the four-view architecture unchanged.

**Architecture:** Keep `ProductBusinessWorkbenchDrawer` as the single shell and refactor only presentation structure inside the five product-workbench components. The new UI uses a journal masthead, thin-line report columns, system color tokens, and compressed content instead of exhibition framing.

**Tech Stack:** Vue 3, TypeScript, scoped CSS, Vitest, Vite, existing shared `Standard*` components.

---

### Task 1: Lock the new journal contract in tests

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts`

- [ ] **Step 1: Write failing tests for the journal masthead shell**
- [ ] **Step 2: Run the five product component tests and verify they fail for missing journal selectors**
- [ ] **Step 3: Keep assertions focused on structure, duplicate-removal rules, and system-color-era hierarchy rather than literal CSS values**

### Task 2: Refactor the unified drawer masthead and overview

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`

- [ ] **Step 1: Replace exhibition header with a journal masthead**
- [ ] **Step 2: Rebuild overview into a report spread with lead sheet, contract line table, and archive notes**
- [ ] **Step 3: Rewrite scoped CSS to thin-line report grammar using only existing system tokens**
- [ ] **Step 4: Run drawer + overview tests and verify green**

### Task 3: Refactor the three sub-workspaces into journal sections

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`

- [ ] **Step 1: Turn model governance into a journal governance section**
- [ ] **Step 2: Turn device view into a ledger ruler + registry sheet**
- [ ] **Step 3: Turn edit view into a revision journal section**
- [ ] **Step 4: Run the three workspace tests and verify green**

### Task 4: Sync docs and run regression verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Replace the current “展陈型高级扁平” current-state wording with the approved A-direction journal wording**
- [ ] **Step 2: Run `/products` page regression tests**
- [ ] **Step 3: Run `component:guard`, `list:guard`, `style:guard`, and `build`**
- [ ] **Step 4: Re-run browser verification against the live `/products` workbench**
