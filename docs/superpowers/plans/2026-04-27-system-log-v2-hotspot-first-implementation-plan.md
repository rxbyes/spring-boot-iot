# System Log V2 Hotspot-First Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade `/system-log`'s `观测热点` tab into a linked hotspot workbench with a focus strip, a compact hotspot master table, synchronized drilldown panels, and a subordinate scheduled-task ledger.

**Architecture:** Keep `/system-log` as a single route with the existing three tabs, but reshape the `观测热点` tab around one primary object: the selected slow-span hotspot summary row. Push presentation and interaction details into `AuditLogHotspotTabPanel.vue`, keep data fetching and shared evidence drawer wiring in `AuditLogView.vue`, and validate the new behavior with view and component tests before changing production code.

**Tech Stack:** Vue 3 `<script setup>`, Element Plus, Vitest, existing `Standard*` workbench components, local `/system-log` state in `AuditLogView.vue`

---

### Task 1: Lock the new hotspot workbench behavior with failing tests

**Files:**
- Create: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: Write the failing component test for the hotspot focus strip and master/detail layout**

```ts
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import AuditLogHotspotTabPanel from '@/components/auditLog/AuditLogHotspotTabPanel.vue';

const summaryRows = [
  {
    spanType: 'SLOW_SQL',
    domainCode: 'system',
    eventCode: 'system.error.archive',
    objectType: 'sql',
    objectId: 'iot_message_log',
    latestStartedAt: '2026-04-27 12:30:00',
    latestTraceId: 'trace-hot-1',
    maxDurationMs: 2400,
    avgDurationMs: 1200,
    totalCount: 6
  },
  {
    spanType: 'HTTP',
    domainCode: 'device',
    eventCode: 'device.contract.publish',
    objectType: 'api',
    objectId: '/api/device/product/release',
    latestStartedAt: '2026-04-27 12:10:00',
    latestTraceId: 'trace-hot-2',
    maxDurationMs: 900,
    avgDurationMs: 520,
    totalCount: 3
  }
];

describe('AuditLogHotspotTabPanel', () => {
  it('renders the active hotspot in a focus strip and highlights the selected row', () => {
    const wrapper = mount(AuditLogHotspotTabPanel, {
      props: {
        slowSummaryLoading: false,
        slowSummaryRows: summaryRows,
        slowSummaryErrorMessage: '',
        formatSlowSummaryTitle: (row: any) => `${row.spanType} ${row.eventCode}`,
        formatSlowSummaryTarget: (row: any) => String(row.objectId || ''),
        formatValue: (value: any) => String(value ?? '--'),
        formatDuration: (value: any) => `${value} ms`,
        formatCount: (value: any) => String(value ?? 0),
        activeSlowSummary: summaryRows[0],
        activeSlowTrendSummary: summaryRows[0],
        selectedSlowSummaryKey: 'SLOW_SQL-system-system.error.archive-sql-iot_message_log',
        slowSpanLoading: false,
        slowSpanTotal: 1,
        slowSpanRows: [],
        slowSpanErrorMessage: '',
        slowTrendLoading: false,
        slowTrendRows: [],
        slowTrendErrorMessage: '',
        slowTrendWindow: 'LAST_24_HOURS',
        slowTrendWindowOptions: [{ label: '24小时', value: 'LAST_24_HOURS' }],
        defaultSlowTrendWindow: 'LAST_24_HOURS',
        formatSlowTrendBucketLabel: (row: any) => String(row.bucket || ''),
        formatPercentage: (value: any) => String(value ?? 0),
        scheduledTaskLoading: false,
        scheduledTaskRows: [],
        scheduledTaskTotal: 0,
        scheduledTaskErrorMessage: '',
        formatScheduledTaskName: (row: any) => String(row.taskCode || ''),
        formatScheduledTaskTrigger: (row: any) => String(row.triggerExpression || '')
      }
    });

    expect(wrapper.find('[data-testid="hotspot-focus-strip"]').text()).toContain('trace-hot-1');
    expect(wrapper.find('[data-testid="hotspot-master-table"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="hotspot-master-row"]').classes()).toContain('is-selected');
  });
});
```

- [ ] **Step 2: Run the component test to verify it fails for the right reason**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts
```

Expected: FAIL because `AuditLogHotspotTabPanel.vue` does not yet render the new `hotspot-focus-strip`, `hotspot-master-table`, and selected-row semantics.

- [ ] **Step 3: Extend the view test with failing integration coverage for hotspot selection persistence and linked refresh**

```ts
it('keeps the selected hotspot active when switching away and back to the hotspots tab', async () => {
  const wrapper = mountView();
  await flushPromises();
  await nextTick();
  await triggerSystemLogTab(wrapper, 'hotspots');

  const rows = wrapper.findAll('[data-testid="hotspot-master-row"]');
  await rows[1]!.trigger('click');
  await flushPromises();
  await nextTick();

  expect(wrapper.find('[data-testid="hotspot-focus-strip"]').text()).toContain('trace-hot-2');

  await triggerSystemLogTab(wrapper, 'errors');
  await triggerSystemLogTab(wrapper, 'hotspots');

  const selectedRow = wrapper.findAll('[data-testid="hotspot-master-row"]').find((row) =>
    row.classes().includes('is-selected')
  );
  expect(selectedRow?.text()).toContain('/api/device/product/release');
});

it('keeps hotspot detail visible when scheduled task loading fails', async () => {
  vi.mocked(pageObservabilityScheduledTasks).mockRejectedValueOnce(new Error('task failed'));

  const wrapper = mountView();
  await flushPromises();
  await nextTick();
  await triggerSystemLogTab(wrapper, 'hotspots');

  expect(wrapper.find('[data-testid="hotspot-master-table"]').exists()).toBe(true);
  expect(wrapper.text()).toContain('task failed');
  expect(wrapper.find('[data-testid="hotspot-detail-section"]').exists()).toBe(true);
});
```

- [ ] **Step 4: Run the view test slice to verify the new expectations fail**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/views/AuditLogView.test.ts
```

Expected: FAIL on the new hotspot workbench assertions because the view does not yet manage a stable selected hotspot or expose the new test hooks.

- [ ] **Step 5: Commit the red tests checkpoint**

```powershell
git add spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "test: define hotspot workbench behavior"
```

### Task 2: Rebuild `观测热点` into a focus strip + master table + linked drilldown panel

**Files:**
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogHotspotTabPanel.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts`

- [ ] **Step 1: Add the new hotspot panel props and emits surface**

```ts
defineProps<{
  slowSummaryLoading: boolean;
  slowSummaryRows: SlowSummaryRow[];
  slowSummaryErrorMessage: string;
  formatSlowSummaryTitle: SlowSummaryFormatter;
  formatSlowSummaryTarget: SlowSummaryFormatter;
  formatValue: ValueFormatter;
  formatDuration: CountFormatter;
  formatCount: CountFormatter;
  activeSlowSummary: SlowSummaryRow | null;
  activeSlowTrendSummary: SlowSummaryRow | null;
  selectedSlowSummaryKey: string;
  slowSpanLoading: boolean;
  slowSpanTotal: number;
  slowSpanRows: SlowSpanRow[];
  slowSpanErrorMessage: string;
  slowTrendLoading: boolean;
  slowTrendRows: SlowTrendRow[];
  slowTrendErrorMessage: string;
  slowTrendWindow: string;
  slowTrendWindowOptions: SlowTrendWindowOption[];
  defaultSlowTrendWindow: string;
  formatSlowTrendBucketLabel: SlowTrendFormatter;
  formatPercentage: CountFormatter;
  scheduledTaskLoading: boolean;
  scheduledTaskRows: ScheduledTaskRow[];
  scheduledTaskTotal: number;
  scheduledTaskErrorMessage: string;
  formatScheduledTaskName: ScheduledTaskFormatter;
  formatScheduledTaskTrigger: ScheduledTaskFormatter;
}>();

const emit = defineEmits<{
  (event: 'select-slow-summary', row: SlowSummaryRow): void;
  (event: 'open-trace-evidence', traceId: string): void;
  (event: 'open-slow-span-detail', row: SlowSummaryRow): void;
  (event: 'open-slow-trend', row: SlowSummaryRow, window: string): void;
  (event: 'change-slow-trend-window', value: string): void;
}>();
```

- [ ] **Step 2: Replace the hotspot card grid with the new focus strip and compact master table**

```vue
<section class="audit-log-hotspot-workbench standard-list-surface">
  <header
    v-if="activeSlowSummary"
    data-testid="hotspot-focus-strip"
    class="audit-log-hotspot-workbench__focus"
  >
    <div class="audit-log-hotspot-workbench__focus-copy">
      <span>当前热点</span>
      <strong>{{ formatSlowSummaryTitle(activeSlowSummary) }}</strong>
      <p>{{ formatSlowSummaryTarget(activeSlowSummary) }}</p>
    </div>
    <div class="audit-log-hotspot-workbench__focus-meta">
      <span>{{ formatValue(activeSlowSummary.latestTraceId) }}</span>
      <span>{{ formatValue(activeSlowSummary.latestStartedAt) }}</span>
      <span>{{ slowTrendWindow === 'LAST_7_DAYS' ? '7天窗口' : '24小时窗口' }}</span>
    </div>
  </header>

  <div v-if="slowSummaryErrorMessage" class="audit-log-slow-summary__empty">
    {{ slowSummaryErrorMessage }}
  </div>
  <div v-else-if="slowSummaryRows.length === 0" class="audit-log-slow-summary__empty">
    暂无热点
  </div>
  <table v-else data-testid="hotspot-master-table" class="audit-log-hotspot-master-table">
    <tbody>
      <tr
        v-for="row in slowSummaryRows"
        :key="buildSlowSummaryKey(row)"
        data-testid="hotspot-master-row"
        :class="{ 'is-selected': buildSlowSummaryKey(row) === selectedSlowSummaryKey }"
        @click="emit('select-slow-summary', row)"
      >
        <td>{{ formatSlowSummaryTitle(row) }}</td>
        <td>{{ formatSlowSummaryTarget(row) }}</td>
        <td>{{ formatValue(row.latestStartedAt) }}</td>
        <td>{{ formatDuration(row.maxDurationMs) }}</td>
        <td>{{ formatDuration(row.avgDurationMs) }}</td>
        <td>{{ formatCount(row.totalCount) }}</td>
      </tr>
    </tbody>
  </table>
</section>
```

- [ ] **Step 3: Group the span detail and trend detail into one linked detail section and downgrade scheduled tasks**

```vue
<section v-if="activeSlowSummary || activeSlowTrendSummary" data-testid="hotspot-detail-section" class="audit-log-hotspot-detail standard-list-surface">
  <div class="audit-log-hotspot-detail__grid">
    <section v-loading="slowSpanLoading" class="audit-log-slow-span-drilldown">
      <!-- keep existing recent span rendering -->
    </section>
    <section v-loading="slowTrendLoading" class="audit-log-slow-trend-drilldown">
      <!-- keep existing trend rendering and choice group -->
    </section>
  </div>
</section>

<section v-loading="scheduledTaskLoading" class="audit-log-scheduled-task-ledger audit-log-scheduled-task-ledger--subdued standard-list-surface">
  <header class="audit-log-scheduled-task-ledger__header">
    <div>
      <h3>最近调度信号</h3>
    </div>
    <span>{{ scheduledTaskRows.length }} / {{ scheduledTaskTotal }}</span>
  </header>
</section>
```

- [ ] **Step 4: Add the minimal helpers and styles needed for selection, focus strip, and dense layout**

```ts
const buildSlowSummaryKey = (row: SlowSummaryRow) =>
  [
    row.spanType || 'span',
    row.domainCode || 'domain',
    row.eventCode || 'event',
    row.objectType || 'object',
    row.objectId || 'id'
  ].join('-');
```

```css
.audit-log-hotspot-workbench__focus {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.95rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 94%, white);
}

.audit-log-hotspot-master-table tr.is-selected {
  background: color-mix(in srgb, var(--el-color-primary-light-9) 70%, white);
}

.audit-log-hotspot-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.82rem;
}

.audit-log-scheduled-task-ledger--subdued {
  margin-top: 0;
  opacity: 0.96;
}
```

- [ ] **Step 5: Run the component test to verify the hotspot panel now passes**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts
```

Expected: PASS

- [ ] **Step 6: Commit the hotspot panel refactor**

```powershell
git add spring-boot-iot-ui/src/components/auditLog/AuditLogHotspotTabPanel.vue spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts
git commit -m "feat: redesign hotspot tab workbench"
```

### Task 3: Wire stable hotspot selection and linked drilldown behavior in `AuditLogView.vue`

**Files:**
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: Add a stable slow-summary key and selected hotspot state in the view**

```ts
const selectedSlowSummaryKey = ref('');

const buildSlowSummaryKey = (row: ObservabilitySlowSpanSummary) =>
  [
    row.spanType || 'span',
    row.domainCode || 'domain',
    row.eventCode || 'event',
    row.objectType || 'object',
    row.objectId || 'id'
  ].join('-');

const resolvedActiveSlowSummary = computed(() => {
  const matched = slowSummaryRows.value.find((row) => buildSlowSummaryKey(row) === selectedSlowSummaryKey.value);
  return matched || slowSummaryRows.value[0] || null;
});
```

- [ ] **Step 2: Sync the selected hotspot after summary loads and keep drilldowns aligned**

```ts
const syncSelectedSlowSummary = async () => {
  const nextRow = resolvedActiveSlowSummary.value;
  selectedSlowSummaryKey.value = nextRow ? buildSlowSummaryKey(nextRow) : '';
  if (!nextRow) {
    clearSlowSpanDrilldown();
    clearSlowTrendDrilldown();
    return;
  }
  if (!activeSlowSummary.value || buildSlowSummaryKey(activeSlowSummary.value) !== selectedSlowSummaryKey.value) {
    await loadSlowSpanDrilldown(nextRow);
  }
  if (!activeSlowTrendSummary.value || buildSlowSummaryKey(activeSlowTrendSummary.value) !== selectedSlowSummaryKey.value) {
    await loadSlowTrendDrilldown(nextRow, slowTrendWindow.value);
  }
};

const getSlowSpanSummaries = async () => {
  // existing request...
  if (res.code === 200) {
    slowSummaryRows.value = Array.isArray(res.data) ? res.data : [];
    await syncSelectedSlowSummary();
  }
};
```

- [ ] **Step 3: Handle panel selection events and pass the new selection prop down**

```ts
const handleSlowSummarySelect = (row: ObservabilitySlowSpanSummary) => {
  const nextKey = buildSlowSummaryKey(row);
  if (selectedSlowSummaryKey.value === nextKey) {
    return;
  }
  selectedSlowSummaryKey.value = nextKey;
  void loadSlowSpanDrilldown(row);
  void loadSlowTrendDrilldown(row, slowTrendWindow.value);
};
```

```vue
<AuditLogHotspotTabPanel
  :active-slow-summary="resolvedActiveSlowSummary"
  :active-slow-trend-summary="activeSlowTrendSummary"
  :selected-slow-summary-key="selectedSlowSummaryKey"
  @select-slow-summary="handleSlowSummarySelect"
  @open-slow-span-detail="loadSlowSpanDrilldown"
  @open-slow-trend="loadSlowTrendDrilldown"
  @change-slow-trend-window="handleSlowTrendWindowChange"
/>
```

- [ ] **Step 4: Update the view tests and run the view suite to verify green**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/views/AuditLogView.test.ts src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts
```

Expected: PASS with the new hotspot workbench assertions, including selection persistence and scheduled-task error isolation.

- [ ] **Step 5: Run a production build to verify the refactor compiles**

Run:

```powershell
npm --prefix spring-boot-iot-ui run build
```

Expected: build completes successfully with no Vue template or TypeScript errors.

- [ ] **Step 6: Commit the integrated hotspot-first workbench change**

```powershell
git add spring-boot-iot-ui/src/views/AuditLogView.vue spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "feat: upgrade system-log hotspots workbench"
```

### Task 4: Final verification and docs touchpoint

**Files:**
- Modify: `docs/superpowers/specs/2026-04-27-system-log-v2-hotspot-first-design.md` (only if implementation reality forces a wording correction)

- [ ] **Step 1: Re-run the focused verification commands in order**

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/views/AuditLogView.test.ts src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts
npm --prefix spring-boot-iot-ui run build
git diff --check
```

Expected:

1. All targeted Vitest cases PASS.
2. Frontend build PASS.
3. `git diff --check` returns clean output.

- [ ] **Step 2: Update the spec wording only if the shipped UI differs from the drafted copy**

```md
## 6. `观测热点` 首轮实现设计

已按“热点焦点条 + 热点主表 + 联动下钻区 + 最近调度信号”落地。
```

- [ ] **Step 3: Commit the verification/documentation checkpoint**

```powershell
git add docs/superpowers/specs/2026-04-27-system-log-v2-hotspot-first-design.md
git commit -m "docs: align hotspot-first design verification"
```
