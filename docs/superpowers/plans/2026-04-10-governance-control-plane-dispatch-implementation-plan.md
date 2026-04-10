# Governance Control Plane Dispatch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `/governance-task` dispatch users into the correct product or risk-point execution workspace instead of stopping at passive task viewing.

**Architecture:** Keep control-plane pages responsible for task orchestration and context routing only. Product and risk-point views continue to own domain execution and are enhanced to accept explicit route context that auto-opens the correct workbench.

**Tech Stack:** Vue 3, TypeScript, Vue Router, Vitest, existing shared workbench components

---

### Task 1: Add task-to-domain route resolution in governance control plane

**Files:**
- Create: `spring-boot-iot-ui/src/utils/governanceTaskDispatch.ts`
- Modify: `spring-boot-iot-ui/src/views/GovernanceTaskView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts`

- [ ] Step 1: Write failing tests for governance-task dispatch buttons and route generation.
- [ ] Step 2: Add a dedicated dispatch utility that maps `PENDING_CONTRACT_RELEASE` to `/products` workbench context and `PENDING_RISK_BINDING` to `/risk-point` binding context.
- [ ] Step 3: Update `GovernanceTaskView.vue` to render a dynamic `去处理` button when a work item can be dispatched.
- [ ] Step 4: Run the targeted governance control-plane test file and confirm the new dispatch assertions pass.

### Task 2: Add route-driven workbench opening in product definition center

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] Step 1: Write failing tests proving `/products?openProductId=<id>&workbenchView=models` auto-opens the product business workbench on `契约字段`.
- [ ] Step 2: Extend route parsing and list/detail loading flow so product pages can resolve `openProductId` even when the row is not already selected.
- [ ] Step 3: Preserve existing list filter behavior and avoid breaking manual open/edit/device-list actions.
- [ ] Step 4: Run the targeted product workbench tests and confirm route-driven opening passes.

### Task 3: Add route-driven pending-promotion opening in risk-point center

**Files:**
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

- [ ] Step 1: Write failing tests proving `/risk-point?openRiskPointId=<id>&bindingAction=pending-promotion` auto-opens the pending-promotion drawer.
- [ ] Step 2: Add route-context parsing that resolves the target risk point after list load and opens the correct binding action.
- [ ] Step 3: Keep existing keyword filter synchronization intact and ensure explicit route context does not duplicate local drawer state.
- [ ] Step 4: Run the targeted risk-point tests and confirm auto-open behavior passes.

### Task 4: Update docs for control-plane dispatch boundary

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] Step 1: Update business-flow documentation to explicitly state that governance control plane dispatches and domain workbenches execute.
- [ ] Step 2: Record the new route-context contract for `/governance-task -> /products` and `/governance-task -> /risk-point`.
- [ ] Step 3: Verify whether `README.md` and `AGENTS.md` need wording changes; only modify if the new behavior changes project-level positioning or operating rules.
- [ ] Step 4: Re-read changed docs for wording consistency with existing “控制面” and “工作台” terminology.
