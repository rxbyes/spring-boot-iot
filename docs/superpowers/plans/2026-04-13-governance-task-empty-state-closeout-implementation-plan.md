# Governance Task Empty-State Closeout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let `/governance-task` keep a usable “continue processing” action when a query-scoped governance entry lands on an empty work-item list.

**Architecture:** Keep the current control-plane design and async work-item refresh model intact. Add a front-end-only empty-state continuation card in `GovernanceTaskView.vue` that derives a minimal dispatchable context from route query, reuses `buildGovernanceTaskDispatchLocation(...)`, and only appears when the empty result still carries actionable governance context.

**Tech Stack:** Vue 3, TypeScript, Vitest

---

## File Map

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\GovernanceTaskView.vue`
  Responsibility: add context-aware empty-state continuation card and route-to-domain dispatch handler.
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`
  Responsibility: cover the failing empty-state deep-link case and preserve plain empty-state behavior.
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
  Responsibility: document the closeout behavior for query-scoped empty governance task pages.
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
  Responsibility: record the shipped fix and verification.
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`
  Responsibility: document the new empty-state continuation behavior in the governance-task acceptance baseline.
- Inspect only: `E:\idea\ghatg\spring-boot-iot\README.md`
- Inspect only: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

### Task 1: Lock the bug with a failing governance-task empty-state test

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`

- [ ] **Step 1: Write the failing test for query-scoped empty-state dispatch**

Add a test that mounts `GovernanceTaskView` with:

```ts
mockRoute.query = {
  productId: '9223372036854775807',
  workItemCode: 'PENDING_CONTRACT_RELEASE',
  workStatus: 'OPEN'
}
```

and with:

```ts
mockPageWorkItems.mockResolvedValue({
  code: 200,
  msg: 'success',
  data: {
    total: 0,
    pageNum: 1,
    pageSize: 10,
    records: []
  }
})
```

Then assert:

```ts
expect(wrapper.text()).toContain('当前没有匹配的治理任务')
const continueButton = wrapper.findAll('button').find((button) => button.text() === '继续处理')
expect(continueButton).toBeTruthy()
await continueButton!.trigger('click')
expect(mockRouter.push).toHaveBeenCalledWith({
  path: '/products',
  query: {
    openProductId: '9223372036854775807',
    workbenchView: 'models',
    governanceSource: 'task',
    workItemCode: 'PENDING_CONTRACT_RELEASE'
  }
})
```

- [ ] **Step 2: Run the targeted view test and verify it fails**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts
```

Expected: FAIL because the current empty state only renders passive copy and has no `继续处理` button.

### Task 2: Implement the minimal empty-state continuation card

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\GovernanceTaskView.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`

- [ ] **Step 1: Add a minimal empty dispatch context helper**

Inside `GovernanceTaskView.vue`, derive a minimal dispatchable work-item-like object from route query:

- read `workItemCode`, `productId`, `subjectId`, `riskMetricId`
- keep IDs as strings
- only return a value when `buildGovernanceTaskDispatchLocation(...)` produces a route

- [ ] **Step 2: Replace the passive empty state with a conditional continuation card**

Render:

- existing empty-state title
- current explanation text
- one extra line explaining that the formal work item may still be syncing
- `继续处理` button only when the derived empty dispatch context exists

The click handler must reuse `router.push(buildGovernanceTaskDispatchLocation(...))`.

- [ ] **Step 3: Re-run the targeted test**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts
```

Expected: PASS, including the new empty-state dispatch test.

### Task 3: Update shipped behavior docs

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`

- [ ] **Step 1: Update the business-flow doc**

Document that `/governance-task` now keeps a continuation action in query-scoped empty states, instead of leaving users on a passive empty page.

- [ ] **Step 2: Update the acceptance matrix**

Document that `GovernanceTaskView` empty states with actionable query context must provide `继续处理` and dispatch into the domain workbench.

- [ ] **Step 3: Record the fix in change log**

Add one dated entry in `docs/08` with the targeted test command used for this fix.

- [ ] **Step 4: Re-check repo entry docs**

Inspect `README.md` and `AGENTS.md`; only edit them if the user-facing product positioning or coding rules changed. Expected result for this fix: no update needed.

### Task 4: Verification

**Files:**
- Test only: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`

- [ ] **Step 1: Run the targeted governance control-plane test file**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts
```

Expected: PASS

- [ ] **Step 2: Run a focused diff sanity check**

Run:

```bash
git diff --check
```

Expected: no whitespace errors
