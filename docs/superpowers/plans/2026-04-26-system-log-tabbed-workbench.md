# System Log Tabbed Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild `/system-log` system mode into a lightweight overview strip plus three equal-priority tabs (`异常排查 / 观测热点 / 归档治理`) without changing `/audit-log`, while preserving the existing drawers and observability evidence workflows.

**Architecture:** Keep `/system-log` on the existing `AuditLogView.vue` route, but split the system-mode body into three focused panel components coordinated by a page-level tab key and shared drawers. Reuse existing observability APIs, keep per-tab local state buckets inside the page-level controller, and use `IotAccessTabWorkspace` for tab chrome so the page gains real structure without becoming a new route tree.

**Tech Stack:** Vue 3 `<script setup>`, TypeScript, Element Plus, existing `IotAccessTabWorkspace` / `Standard*` shared UI components, Vitest + Vue Test Utils, Vite build.

## Status Snapshot (2026-04-26)

- `codex/dev` has already merged the main `/system-log` tabbed-workbench implementation.
- The lightweight overview strip, `异常排查 / 观测热点 / 归档治理` three-tab structure, shared drawers, archive filter restoration, and related docs are all in place.
- Focused frontend verification already passed on the merged branch:
  - `AuditLogView.test.ts`
  - `MessageTraceView.test.ts`
  - `AuditLogSystemOverviewStrip.test.ts`
  - `AuditLogHotspotTabPanel.test.ts`
  - `AuditLogArchiveTabPanel.test.ts`
  - `IotAccessTabWorkspace.test.ts`
- `npm --prefix spring-boot-iot-ui run build` also passed on the merged branch.

## Deferred Follow-Ups For Next Session

- [ ] **Browser acceptance after login**
  - Re-open local `/system-log` in the browser after completing the local login step.
  - Verify the real rendered workbench, not just unit-test behavior:
    - default tab is `异常排查`
    - top overview strip stays lightweight
    - `观测热点` and `归档治理` switch cleanly
    - shared evidence / archive drawers still open correctly
    - archive filters render and behave correctly in the live page

- [ ] **Add view-level regression for restored archive filters**
  - Extend `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
  - Cover end-to-end page behavior for restored archive controls:
    - `dateFrom`
    - `dateTo`
    - `onlyAbnormal`
    - `search`
    - `reset`
  - Assert these controls affect the actual archive query payload, not only the component-level emits.

---

## File Structure

### New files

- Create: `spring-boot-iot-ui/src/components/auditLog/AuditLogSystemOverviewStrip.vue`
  - Responsibility: render the lightweight system-mode overview strip and emit the target tab key when a summary item is clicked.
- Create: `spring-boot-iot-ui/src/components/auditLog/AuditLogErrorTabPanel.vue`
  - Responsibility: render only the system error filter area, inline state, anomaly list surfaces, and row actions for the `异常排查` tab.
- Create: `spring-boot-iot-ui/src/components/auditLog/AuditLogHotspotTabPanel.vue`
  - Responsibility: render only the slow-summary, slow-drilldown, slow-trend, and scheduled-task sections for the `观测热点` tab.
- Create: `spring-boot-iot-ui/src/components/auditLog/AuditLogArchiveTabPanel.vue`
  - Responsibility: render only the archive overview cards, filters, focus hint, and archive batch ledger for the `归档治理` tab.
- Create: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogSystemOverviewStrip.test.ts`
  - Responsibility: verify overview-strip labels, active state, and tab-change emits.
- Create: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts`
  - Responsibility: verify the hotspot panel isolates slow-summary and scheduled-task UI, loading, and error states.
- Create: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts`
  - Responsibility: verify the archive-governance panel keeps overview cards, filters, focus hint, abnormal row styling, and row action emit behavior.

### Modified files

- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
  - Responsibility: keep `/audit-log` business mode intact, introduce system-mode tab workspace, own the three per-tab state buckets, wire panel props/events, and keep shared drawers page-level.
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
  - Responsibility: add regression coverage for default tab, tab switching, per-tab state persistence, current-tab-only refresh, and `/audit-log` non-regression.
- Modify: `README.md`
  - Responsibility: document that `/system-log` now uses a lightweight overview strip plus three equal-priority tabs.
- Modify: `AGENTS.md`
  - Responsibility: update the current `/system-log` front-end shape and tab semantics in project memory.
- Modify: `docs/02-业务功能与流程说明.md`
  - Responsibility: update business-facing IA for the diagnostic page.
- Modify: `docs/15-前端优化与治理计划.md`
  - Responsibility: record the system-log layout convergence and confirm the overview strip stays lightweight.

## Task 1: Lock in the tabbed-system-mode regression tests

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: Add failing tests for the new system-log tab contract**

Add these new test blocks near the existing `/system-log` cases so the spec is executable before refactor:

```ts
it('defaults system-log to the 异常排查 tab and hides hotspot and archive sections until selected', async () => {
  const wrapper = mountView()

  await flushPromises()

  expect(wrapper.text()).toContain('异常观测台')
  expect(wrapper.find('[data-testid="system-log-tab-errors"]').attributes('data-active')).toBe('true')
  expect(wrapper.find('[data-testid="system-log-error-panel"]').exists()).toBe(true)
  expect(wrapper.find('[data-testid="system-log-hotspot-panel"]').exists()).toBe(false)
  expect(wrapper.find('[data-testid="system-log-archive-panel"]').exists()).toBe(false)
})

it('keeps filter state per tab when switching between errors, hotspots, and archives', async () => {
  const wrapper = mountView()

  await triggerSystemLogTab(wrapper, 'archives')
  await wrapper.get('[data-testid="archive-batch-filter-batch-no"]').setValue('batch-001')
  await triggerSystemLogTab(wrapper, 'errors')
  await wrapper.get('#quick-search').setValue('trace-001')
  await triggerSystemLogTab(wrapper, 'archives')

  expect((wrapper.get('[data-testid="archive-batch-filter-batch-no"]').element as HTMLInputElement).value).toBe('batch-001')
  await triggerSystemLogTab(wrapper, 'errors')
  expect((wrapper.get('#quick-search').element as HTMLInputElement).value).toBe('trace-001')
})

it('refreshes only the active system-log tab data source', async () => {
  const wrapper = mountView()

  await triggerSystemLogTab(wrapper, 'hotspots')
  await clickButtonByText(wrapper, '刷新列表')

  expect(listObservabilitySlowSpanSummaries).toHaveBeenCalled()
  expect(pageObservabilityScheduledTasks).toHaveBeenCalled()
  expect(pageObservabilityMessageArchiveBatches).not.toHaveBeenCalledTimes(2)
})

it('keeps /audit-log in the existing single-workbench layout without system tabs', async () => {
  mockRoute.path = '/audit-log'
  const wrapper = mountView()

  await flushPromises()

  expect(wrapper.find('[data-testid="system-log-tab-errors"]').exists()).toBe(false)
  expect(wrapper.text()).not.toContain('观测热点')
  expect(wrapper.text()).not.toContain('归档治理')
})
```

- [ ] **Step 2: Add or update minimal test helpers for tab interaction**

Add the small helpers the new tests need near the existing `findButtonByText()` helper:

```ts
async function triggerSystemLogTab(wrapper: ReturnType<typeof mountView>, key: 'errors' | 'hotspots' | 'archives') {
  const button = wrapper.get(`[data-testid="system-log-tab-${key}"]`)
  await button.trigger('click')
  await flushPromises()
}

async function clickButtonByText(wrapper: ReturnType<typeof mountView>, text: string) {
  const button = findButtonByText(wrapper, text)
  if (!button) {
    throw new Error(`Missing button: ${text}`)
  }
  await button.trigger('click')
  await flushPromises()
}
```

- [ ] **Step 3: Run the targeted view test file to verify the new cases fail first**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts
```

Expected:

```text
FAIL src/__tests__/views/AuditLogView.test.ts > AuditLogView > defaults system-log to the 异常排查 tab...
FAIL src/__tests__/views/AuditLogView.test.ts > AuditLogView > keeps filter state per tab...
```

- [ ] **Step 4: Commit the failing-test checkpoint**

Run:

```bash
git add spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "test: lock system-log tabbed workbench behavior"
```

## Task 2: Build the lightweight overview strip and hotspot/archive panels with component tests

**Files:**
- Create: `spring-boot-iot-ui/src/components/auditLog/AuditLogSystemOverviewStrip.vue`
- Create: `spring-boot-iot-ui/src/components/auditLog/AuditLogHotspotTabPanel.vue`
- Create: `spring-boot-iot-ui/src/components/auditLog/AuditLogArchiveTabPanel.vue`
- Create: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogSystemOverviewStrip.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogSystemOverviewStrip.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts`

- [ ] **Step 1: Write the failing component tests before introducing the new files**

Use these skeletons so each component contract is explicit:

```ts
// AuditLogSystemOverviewStrip.test.ts
it('renders lightweight overview chips and emits the requested tab key', async () => {
  const wrapper = mount(AuditLogSystemOverviewStrip, {
    props: {
      activeTab: 'errors',
      items: [
        { key: 'errors', label: '异常', value: '896019', targetTab: 'errors' },
        { key: 'hotspots', label: '慢点', value: '5', targetTab: 'hotspots' },
        { key: 'tasks', label: '调度', value: '9236', targetTab: 'hotspots' },
        { key: 'archives', label: '异常批次', value: '3', targetTab: 'archives' }
      ]
    }
  })

  await wrapper.get('[data-testid="system-log-overview-hotspots"]').trigger('click')
  expect(wrapper.emitted('change-tab')?.[0]).toEqual(['hotspots'])
})

// AuditLogHotspotTabPanel.test.ts
it('shows slow-summary and scheduled-task sections, but not the system error list', () => {
  const wrapper = mount(AuditLogHotspotTabPanel, { props: hotspotPropsFactory() })
  expect(wrapper.find('[data-testid="system-log-hotspot-panel"]').exists()).toBe(true)
  expect(wrapper.text()).toContain('性能慢点 Top')
  expect(wrapper.text()).toContain('调度任务台账')
  expect(wrapper.text()).not.toContain('异常摘要')
})

// AuditLogArchiveTabPanel.test.ts
it('renders archive overview cards and emits row actions without showing hotspot sections', async () => {
  const wrapper = mount(AuditLogArchiveTabPanel, { props: archivePropsFactory() })
  expect(wrapper.find('[data-testid="system-log-archive-panel"]').exists()).toBe(true)
  expect(wrapper.text()).toContain('归档批次台账')
  expect(wrapper.text()).not.toContain('性能慢点 Top')
  await wrapper.get('[data-testid="archive-batch-open-detail"]').trigger('click')
  expect(wrapper.emitted('open-detail')).toHaveLength(1)
})
```

- [ ] **Step 2: Run the new component tests and confirm they fail because the components do not exist yet**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run \
  src/__tests__/components/auditLog/AuditLogSystemOverviewStrip.test.ts \
  src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts \
  src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts
```

Expected:

```text
Error: Failed to resolve import "@/components/auditLog/AuditLogSystemOverviewStrip.vue"
Error: Failed to resolve import "@/components/auditLog/AuditLogHotspotTabPanel.vue"
Error: Failed to resolve import "@/components/auditLog/AuditLogArchiveTabPanel.vue"
```

- [ ] **Step 3: Implement the overview strip and the two non-error panels with minimal, focused templates**

Create `AuditLogSystemOverviewStrip.vue` with this shape:

```vue
<template>
  <section class="audit-log-system-overview-strip" aria-label="异常观测总览">
    <button
      v-for="item in items"
      :key="item.key"
      :data-testid="`system-log-overview-${item.key}`"
      type="button"
      class="audit-log-system-overview-strip__item"
      :class="{ 'is-active': item.targetTab === activeTab }"
      @click="$emit('change-tab', item.targetTab)"
    >
      <span>{{ item.label }}</span>
      <strong>{{ item.value }}</strong>
    </button>
  </section>
</template>
```

Create `AuditLogHotspotTabPanel.vue` as a focused extraction of the existing slow-summary + scheduled-task blocks:

```vue
<template>
  <div data-testid="system-log-hotspot-panel" class="audit-log-system-panel audit-log-system-panel--hotspots">
    <section v-loading="slowSummaryLoading" class="audit-log-slow-summary standard-list-surface">
      <!-- move the existing slow summary, drilldown, and trend markup here -->
    </section>
    <section v-loading="scheduledTaskLoading" class="audit-log-scheduled-task-ledger standard-list-surface">
      <!-- move the existing scheduled task ledger markup here -->
    </section>
  </div>
</template>
```

Create `AuditLogArchiveTabPanel.vue` as a focused extraction of the existing archive overview + ledger block:

```vue
<template>
  <div data-testid="system-log-archive-panel" class="audit-log-system-panel audit-log-system-panel--archives">
    <section v-loading="loading" class="audit-log-archive-batch-ledger standard-list-surface">
      <!-- move the overview cards, filters, focus hint, and ledger markup here -->
    </section>
  </div>
</template>
```

- [ ] **Step 4: Run the component tests again and make them pass**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run \
  src/__tests__/components/auditLog/AuditLogSystemOverviewStrip.test.ts \
  src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts \
  src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts
```

Expected:

```text
PASS src/__tests__/components/auditLog/AuditLogSystemOverviewStrip.test.ts
PASS src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts
PASS src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts
```

- [ ] **Step 5: Commit the extracted overview/hotspot/archive components**

Run:

```bash
git add \
  spring-boot-iot-ui/src/components/auditLog/AuditLogSystemOverviewStrip.vue \
  spring-boot-iot-ui/src/components/auditLog/AuditLogHotspotTabPanel.vue \
  spring-boot-iot-ui/src/components/auditLog/AuditLogArchiveTabPanel.vue \
  spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogSystemOverviewStrip.test.ts \
  spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts \
  spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts
git commit -m "feat: extract system-log hotspot and archive panels"
```

## Task 3: Extract the error panel and refactor `AuditLogView.vue` to own tab state and shared drawers

**Files:**
- Create: `spring-boot-iot-ui/src/components/auditLog/AuditLogErrorTabPanel.vue`
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: Add a failing error-panel expectation into the existing page test file**

Append this case to make the extraction visible:

```ts
it('renders the anomaly list only inside the 异常排查 panel', async () => {
  const wrapper = mountView()
  await flushPromises()

  expect(wrapper.find('[data-testid="system-log-error-panel"]').exists()).toBe(true)
  expect(wrapper.find('[data-testid="system-log-hotspot-panel"]').exists()).toBe(false)

  await triggerSystemLogTab(wrapper, 'hotspots')
  expect(wrapper.find('[data-testid="system-log-error-panel"]').exists()).toBe(false)
  expect(wrapper.find('[data-testid="system-log-hotspot-panel"]').exists()).toBe(true)
})
```

- [ ] **Step 2: Run the `AuditLogView` test file again and confirm the new extraction case fails**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts
```

Expected:

```text
FAIL ... renders the anomaly list only inside the 异常排查 panel
```

- [ ] **Step 3: Implement the page-level tab controller and panel extraction in `AuditLogView.vue`**

Introduce a dedicated system-tab key, per-tab state buckets, and `IotAccessTabWorkspace` in the system branch:

```ts
type SystemLogTabKey = 'errors' | 'hotspots' | 'archives'

const systemLogTab = ref<SystemLogTabKey>('errors')
const errorTabState = reactive({ quickSearchKeyword: '', pageNum: 1, pageSize: 10 })
const hotspotTabState = reactive({ selectedSummaryKey: '', trendWindow: defaultSlowTrendWindow, scheduledPageNum: 1, scheduledPageSize: 5 })
const archiveTabState = reactive({ batchNo: '', status: '', compareStatus: '', onlyAbnormal: false, dateFrom: '', dateTo: '', pageNum: 1, pageSize: 5 })

const systemLogTabItems = [
  { key: 'errors', label: '异常排查' },
  { key: 'hotspots', label: '观测热点' },
  { key: 'archives', label: '归档治理' }
] as const

const systemOverviewItems = computed(() => [
  { key: 'errors', label: '异常', value: String(systemStats.value.total || 0), targetTab: 'errors' as const },
  { key: 'hotspots', label: '慢点', value: String(slowSummaryRows.value.length || 0), targetTab: 'hotspots' as const },
  { key: 'tasks', label: '调度', value: String(scheduledTaskTotal.value || 0), targetTab: 'hotspots' as const },
  { key: 'archives', label: '异常批次', value: String(messageArchiveBatchOverview.value?.abnormalBatches || 0), targetTab: 'archives' as const }
])
```

Replace the current `v-if="isSystemMode"` long-form body with:

```vue
<template v-if="isSystemMode">
  <AuditLogSystemOverviewStrip
    :active-tab="systemLogTab"
    :items="systemOverviewItems"
    @change-tab="handleSystemLogTabChange"
  />

  <IotAccessTabWorkspace
    v-model="systemLogTab"
    :items="systemLogTabItems"
    :sync-query="false"
    query-key="tab"
  >
    <template #default="{ activeKey }">
      <AuditLogErrorTabPanel
        v-if="activeKey === 'errors'"
        data-testid="system-log-error-panel"
        <!-- pass the existing error-table props and events -->
      />
      <AuditLogHotspotTabPanel
        v-else-if="activeKey === 'hotspots'"
        data-testid="system-log-hotspot-panel"
        <!-- pass slow-summary + scheduled-task props and events -->
      />
      <AuditLogArchiveTabPanel
        v-else
        data-testid="system-log-archive-panel"
        <!-- pass archive overview + ledger props and events -->
      />
    </template>
  </IotAccessTabWorkspace>
</template>
```

Keep the existing shared drawers below the workbench panel, but make sure their state and handlers stay page-level.

- [ ] **Step 4: Add tab-specific refresh behavior instead of unconditional whole-page system refresh**

Update the refresh entrypoint so it switches on `systemLogTab.value`:

```ts
const refreshActiveSystemTab = async () => {
  switch (systemLogTab.value) {
    case 'errors':
      await Promise.all([getList(), getStats()])
      return
    case 'hotspots':
      await Promise.all([getSlowSpanSummaries(), getScheduledTaskLedger()])
      return
    case 'archives':
      await refreshMessageArchiveBatchLedger()
      return
  }
}

const handleRefresh = async () => {
  if (isSystemMode.value) {
    await refreshActiveSystemTab()
    return
  }
  await Promise.all([getList(), getStats()])
}
```

- [ ] **Step 5: Run the `AuditLogView` test file and make every system-log case pass**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/AuditLogView.test.ts
```

Expected:

```text
PASS src/__tests__/views/AuditLogView.test.ts
```

- [ ] **Step 6: Commit the page-level refactor**

Run:

```bash
git add \
  spring-boot-iot-ui/src/components/auditLog/AuditLogErrorTabPanel.vue \
  spring-boot-iot-ui/src/views/AuditLogView.vue \
  spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "feat: split system-log into tabbed workbench panels"
```

## Task 4: Regression verification, docs, and final polish

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogSystemOverviewStrip.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts`

- [ ] **Step 1: Update docs to describe the new tabbed system-log structure**

Use these exact content directions:

```md
- `/system-log` 系统模式当前采用“轻量总览带 + 异常排查 / 观测热点 / 归档治理 三张平级选项卡”的结构。
- `/audit-log` 审计中心模式保持原有主列表工作台，不共享系统模式选项卡。
- 顶部总览带只负责方向判断与快捷切换，不回到第二层概况卡或跨页功能墙。
```

Apply that wording in:

```text
README.md
AGENTS.md
docs/02-业务功能与流程说明.md
docs/15-前端优化与治理计划.md
```

- [ ] **Step 2: Run the focused frontend regression suite**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run \
  src/__tests__/views/AuditLogView.test.ts \
  src/__tests__/views/MessageTraceView.test.ts \
  src/__tests__/components/auditLog/AuditLogSystemOverviewStrip.test.ts \
  src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts \
  src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts
```

Expected:

```text
PASS src/__tests__/views/AuditLogView.test.ts
PASS src/__tests__/views/MessageTraceView.test.ts
PASS src/__tests__/components/auditLog/AuditLogSystemOverviewStrip.test.ts
PASS src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts
PASS src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts
```

- [ ] **Step 3: Run the production build to catch template/type regressions**

Run:

```bash
npm --prefix spring-boot-iot-ui run build
```

Expected:

```text
vite v7...
✓ built in ...
```

- [ ] **Step 4: Do a manual browser smoke check for both routes**

Open and verify:

```text
http://127.0.0.1:5174/system-log
http://127.0.0.1:5174/audit-log
```

Manual checklist:

```text
1. /system-log defaults to 异常排查
2. Overview strip switches tabs without reloading the route
3. Each tab shows only its own content
4. Per-tab filters persist after switching away and back
5. Shared drawers still open from the correct tab
6. /audit-log shows no system tabs and keeps the old single-workbench layout
```

- [ ] **Step 5: Commit docs and final verification**

Run:

```bash
git add \
  README.md \
  AGENTS.md \
  docs/02-业务功能与流程说明.md \
  docs/15-前端优化与治理计划.md
git commit -m "docs: describe tabbed system-log workbench"
```

## Self-Review

### Spec coverage

Every spec requirement maps to a task:

1. Lightweight overview strip: Task 2
2. Three equal-priority tabs: Task 3
3. Default to `异常排查`: Task 1 + Task 3
4. Per-tab state memory: Task 1 + Task 3
5. Preserve shared drawers: Task 3 + Task 4
6. `/audit-log` non-regression: Task 1 + Task 4
7. Docs update: Task 4

No spec gap remains.

### Placeholder scan

Checked for:

1. `TODO`
2. `TBD`
3. “write tests later”
4. “implement appropriate handling”

None remain in the plan.

### Type consistency

The plan consistently uses:

1. `SystemLogTabKey = 'errors' | 'hotspots' | 'archives'`
2. `AuditLogSystemOverviewStrip`
3. `AuditLogErrorTabPanel`
4. `AuditLogHotspotTabPanel`
5. `AuditLogArchiveTabPanel`

No conflicting names remain between tasks.
