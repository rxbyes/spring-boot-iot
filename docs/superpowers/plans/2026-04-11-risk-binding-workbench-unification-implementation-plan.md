# Risk Binding Workbench Unification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unify `/risk-point`'s `维护绑定` and `待治理转正` entry points into a single risk-binding workbench while keeping formal bindings and pending-governance truth sources separate.

**Architecture:** Keep `/risk-point` as the only domain route, and keep governance control-plane dispatch as context-only routing. Replace the two user-facing drawer entries with one `风险绑定工作台` container that can preselect either the formal-binding view or the pending-promotion view from explicit route context.

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vitest, existing shared workbench/drawer components

---

### Task 1: Reshape risk-point entry semantics around one workbench entry

**Files:**
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/components/riskPoint/RiskPointDetailDrawer.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/components/riskPoint/RiskPointDetailDrawer.test.ts`

- [ ] **Step 1: Write failing tests for the unified entry contract**
  Run: `npm run test -- --run src/__tests__/views/RiskPointView.test.ts src/__tests__/components/riskPoint/RiskPointDetailDrawer.test.ts`
  Expected: FAIL after assertions are updated to require a single `风险绑定` / `风险绑定工作台` entry instead of separate `维护绑定` and `待治理转正` buttons.

- [ ] **Step 2: Update `RiskPointDetailDrawer.vue` footer to emit one binding-workbench action**
  Replace the two footer actions with one `data-testid="detail-binding-workbench-action"` button that emits `binding-workbench`, while keeping `编辑风险点` unchanged.

- [ ] **Step 3: Update `RiskPointView.vue` row actions and detail handoff**
  Replace row-action command handling from `maintain-binding` + `pending-promotion` to one user-facing `binding-workbench` command.
  Keep route-context parsing for `bindingAction=pending-promotion|maintain-binding`, but map both values into the same workbench with different initial sub-views.

- [ ] **Step 4: Re-run the targeted detail and view tests**
  Run: `npm run test -- --run src/__tests__/views/RiskPointView.test.ts src/__tests__/components/riskPoint/RiskPointDetailDrawer.test.ts`
  Expected: PASS for the renamed action contract and route-driven entry assertions.

### Task 2: Build the unified risk-binding workbench container in `/risk-point`

**Files:**
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts`

- [ ] **Step 1: Write failing tests for one drawer with two sub-views**
  Add assertions that:
  - clicking `风险绑定` opens one `风险绑定工作台` drawer
  - governance route context with `bindingAction=maintain-binding` opens the workbench on the formal-binding view
  - governance route context with `bindingAction=pending-promotion` opens the same workbench on the pending view and still loads pending candidates
  Run: `npm run test -- --run src/__tests__/views/RiskPointView.test.ts src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts`
  Expected: FAIL before implementation.

- [ ] **Step 2: Make `RiskPointBindingMaintenanceDrawer.vue` embeddable**
  Add an `embedded`-style rendering path so the formal-binding content can be rendered inside another drawer without nesting a second `StandardFormDrawer`.
  Preserve all existing behavior for:
  - grouped formal bindings
  - add formal binding
  - whole-device unbind
  - single-binding remove
  - single-binding replace

- [ ] **Step 3: Replace the separate maintenance/pending drawers in `RiskPointView.vue`**
  Introduce one workbench drawer state:
  - active risk point
  - visible flag
  - initial/active sub-view (`formal` or `pending`)
  Render the embeddable formal-binding section and the existing pending-governance section behind a shared switcher.
  Keep pending-governance logic bound to `risk_point_device_pending_binding` and `risk_point_device_pending_promotion`; do not merge it into formal-binding writes.

- [ ] **Step 4: Re-run the targeted risk-point tests**
  Run: `npm run test -- --run src/__tests__/views/RiskPointView.test.ts src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts`
  Expected: PASS for unified drawer entry, route-preselected sub-view behavior, and unchanged formal-binding mutations.

### Task 3: Update risk-point and governance docs for the new workbench contract

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: Update business-flow wording**
  Replace the old “两个独立抽屉”表述 with “统一风险绑定工作台 + 子视图分域执行”.

- [ ] **Step 2: Update acceptance criteria**
  State that `/risk-point` now exposes one workbench entry, while control-plane route context still uses `bindingAction=maintain-binding|pending-promotion` to preselect the correct sub-view.

- [ ] **Step 3: Record the architectural boundary**
  Keep the wording explicit that:
  - the workbench is unified at the UI/workflow layer
  - formal bindings still write `risk_point_device`
  - pending governance still reads/writes `risk_point_device_pending_binding` and `risk_point_device_pending_promotion`

- [ ] **Step 4: Re-review project-level docs**
  Check `README.md` and `AGENTS.md`; only edit them if the new workbench changes repo-level positioning or operator rules.

### Task 4: Run fresh verification before closing the phase

**Files:**
- Modify if needed: same files as above

- [ ] **Step 1: Run focused UI tests**
  Run: `npm run test -- --run src/__tests__/views/RiskPointView.test.ts src/__tests__/components/riskPoint/RiskPointDetailDrawer.test.ts src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts`
  Expected: PASS

- [ ] **Step 2: Run build verification**
  Run: `npm run build`
  Expected: build succeeds with exit code `0`

- [ ] **Step 3: Run whitespace/conflict verification**
  Run: `git diff --check`
  Expected: no trailing-whitespace or conflict-marker errors
