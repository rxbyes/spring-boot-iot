# Governance Task View Selector Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the governance-task category button strip with a single `任务视角` dropdown while keeping existing query semantics, deep links, and control-plane behavior unchanged.

**Architecture:** Keep `/governance-task` as a control-plane entry that still derives scope from route query. The front end will map a new `filters.taskView` selector to the existing `view / executionStatus / workItemCode` query contract, and route-to-filter hydration will infer the selector from the same query so refresh and deep links continue to replay correctly.

**Tech Stack:** Vue 3, TypeScript, Vitest

---

## File Map

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\GovernanceTaskView.vue`
  Responsibility: replace the category strip UI, add task-view selector state, keep query mapping and empty-state continuation behavior intact.
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`
  Responsibility: lock selector-driven route writes and route-query-to-selector hydration with failing tests before implementation.
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
  Responsibility: document the new governance task filter expression and control-plane positioning.
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
  Responsibility: record the shipped selector change and targeted verification.
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`
  Responsibility: update acceptance wording for the governance-task filter area.
- Inspect only: `E:\idea\ghatg\spring-boot-iot\README.md`
- Inspect only: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

### Task 1: Lock selector behavior with failing tests

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`

- [ ] **Step 1: Replace the old preset-button test with a selector-driven query mapping test**

Add a test that mounts `GovernanceTaskView`, selects `待审批`, and asserts the router keeps the existing query contract:

```ts
const wrapper = mountWithStubs(GovernanceTaskView)
await flushPromises()

const taskViewSelect = wrapper.get('select[data-testid="governance-task-view-select"]')
await taskViewSelect.setValue('pending-approval')

expect(mockRouter.replace).toHaveBeenCalledWith({
  query: expect.objectContaining({
    executionStatus: 'PENDING_APPROVAL',
    workStatus: 'OPEN'
  })
})
```

- [ ] **Step 2: Add a route-hydration regression for query to selector**

Mount with:

```ts
mockRoute.query = {
  workItemCode: 'PENDING_LINKAGE_PLAN',
  workStatus: 'OPEN'
}
```

Then assert:

```ts
const taskViewSelect = wrapper.get('select[data-testid="governance-task-view-select"]')
expect((taskViewSelect.element as HTMLSelectElement).value).toBe('linkage-plan')
expect(wrapper.text()).toContain('待补联动/预案')
```

- [ ] **Step 3: Run the targeted view test file and verify RED**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts
```

Expected: FAIL because the current page still renders preset buttons and has no `governance-task-view-select`.

### Task 2: Implement the selector-driven governance-task filter

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\GovernanceTaskView.vue`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`

- [ ] **Step 1: Add task-view state to the filter model and hydrate it from route query**

Update the filter state from:

```ts
const filters = reactive({
  keyword: '',
  workStatus: 'OPEN'
})
```

to:

```ts
const filters = reactive({
  keyword: '',
  taskView: 'all',
  workStatus: 'OPEN'
})
```

and add a helper that maps the current route query to one of:

```ts
'all' | 'recommended' | 'pending-approval' | 'contract-release' | 'risk-binding' | 'threshold-policy' | 'linkage-plan' | 'replay'
```

- [ ] **Step 2: Replace the category button strip with the new selector**

Inside `StandardListFilterHeader`, render:

```vue
<label class="governance-task-filter-field">
  <span>任务视角</span>
  <select
    v-model="filters.taskView"
    data-testid="governance-task-view-select"
    class="governance-task-filter-input"
    @change="handleTaskViewChange"
  >
    <option value="all">全部</option>
    <option value="recommended">推荐优先处理</option>
    <option value="pending-approval">待审批</option>
    <option value="contract-release">待发布合同</option>
    <option value="risk-binding">待绑定风险点</option>
    <option value="threshold-policy">待补阈值</option>
    <option value="linkage-plan">待补联动/预案</option>
    <option value="replay">待运营复盘</option>
  </select>
</label>
```

Delete the `governance-task-category-strip` block entirely.

- [ ] **Step 3: Reuse the existing query contract instead of inventing a new one**

Keep `buildPresetQuery(...)` semantics, but drive it from the selector value:

```ts
function handleTaskViewChange() {
  setPageNum(1)
  void replaceTaskRouteQuery({
    ...persistentContextQuery(),
    ...buildPresetQuery(filters.taskView),
    keyword: normalizeText(filters.keyword),
    workStatus: filters.workStatus || 'OPEN',
    pageNum: '1',
    pageSize: String(pagination.pageSize)
  })
}
```

Also update `syncFiltersFromRoute()` so `filters.taskView` is inferred from `view`, `executionStatus`, and `workItemCode`.

- [ ] **Step 4: Remove the obsolete strip styling and keep shared filter styling intact**

Delete:

```css
.governance-task-category-strip {
  grid-template-columns: repeat(auto-fit, minmax(8rem, max-content));
  gap: 0.5rem;
  margin-top: 0.65rem;
}
```

Keep `.governance-task-filter-field` and `.governance-task-filter-input` unchanged unless the new inline selector needs a minimal adjustment.

- [ ] **Step 5: Re-run the targeted test file and verify GREEN**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts
```

Expected: PASS, including the new selector tests and the earlier empty-state continuation regression.

### Task 3: Update shipped docs in place

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`

- [ ] **Step 1: Update the business-flow document**

Document that `/governance-task` now exposes:

```text
快速搜索 + 任务视角 + 工作状态 + 查询/重置
```

and that `任务视角` is only a UI expression layer over the existing `view / executionStatus / workItemCode` contract.

- [ ] **Step 2: Update the acceptance baseline**

Add acceptance wording that the governance task filter area must provide the `任务视角` dropdown with the seven named backlog views plus `全部`, and that switching the dropdown must preserve the current query semantics.

- [ ] **Step 3: Record the release note**

Add one dated note in `docs/08-变更记录与技术债清单.md` including the verification command:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts
```

- [ ] **Step 4: Re-check entry docs**

Inspect `README.md` and `AGENTS.md`. Only edit them if the product positioning or workflow rules changed. Expected result for this change: no update needed.

### Task 4: Final verification

**Files:**
- Test only: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\GovernanceControlPlaneViews.test.ts`

- [ ] **Step 1: Run the targeted governance control-plane test file**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts
```

Expected: PASS

- [ ] **Step 2: Run diff sanity check**

Run:

```bash
git diff --check
```

Expected: no whitespace errors
