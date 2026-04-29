# System Log V3 Archive Governance Declutter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework `/system-log` 的 `归档治理` 页签 so the first screen prioritizes three compact summary metrics and a batch master table, while pushing compare/report detail density back into the existing drawer.

**Architecture:** Keep the current message-archive API orchestration and drawer-loading flow in `AuditLogView.vue`, but slim the archive panel contract down to three overview metrics plus an optional latest-abnormal focus pill. The table stays row-selectable, yet each row now surfaces only status, compare conclusion, progress summary, and risk signal instead of the full治理数值组合.

**Tech Stack:** Vue 3 `script setup`, TypeScript, shared `StandardButton`, Element Plus loading states, Vitest + Vue Test Utils, Vite build.

---

### Task 1: Simplify the archive panel surface and remove redundant summary blocks

**Files:**
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogArchiveTabPanel.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts`

- [ ] **Step 1: Write the failing component test**

Add a component test that captures the new reduced surface: only three overview cards render, `最近异常批次` is no longer a fourth card, and the focus strip text is gone from the first screen.

```ts
it('renders three summary metrics and moves the latest abnormal batch into a lightweight focus entry', () => {
  const wrapper = mount(AuditLogArchiveTabPanel, {
    props: {
      ...archivePropsFactory(),
      overviewCards: [
        { key: 'abnormal', label: '异常批次', value: '2', meta: '总批次 4', clickable: true, active: false, testId: 'archive-batch-overview-abnormal' },
        { key: 'drifted', label: '执行偏差总量', value: '308', meta: '已对齐 1', clickable: true, active: false, testId: 'archive-batch-overview-drifted' },
        { key: 'remaining', label: '剩余过期总量', value: '308', meta: '部分可比 1', clickable: true, active: false, testId: 'archive-batch-overview-remaining' }
      ],
      latestAbnormalFocus: {
        batchNo: 'iot_message_log-20260426090100',
        occurredAt: '2026-04-26 09:01:00',
        active: true
      }
    },
    global: {
      stubs: { StandardButton: StandardButtonStub }
    }
  });

  expect(wrapper.findAll('.audit-log-archive-batch-ledger__overview-card')).toHaveLength(3);
  expect(wrapper.text()).toContain('最近异常批次');
  expect(wrapper.find('[data-testid="archive-batch-latest-focus"]').exists()).toBe(true);
  expect(wrapper.find('[data-testid="archive-governance-focus-strip"]').exists()).toBe(false);
});
```

- [ ] **Step 2: Run the focused component test and verify it fails**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts
```

Expected: FAIL because the panel still renders the old focus strip, still expects the fourth overview card, and does not know `latestAbnormalFocus`.

- [ ] **Step 3: Implement the reduced archive surface**

Update `AuditLogArchiveTabPanel.vue` to remove the focus strip and focus hint from the first screen, keep only three metric cards, and add a lightweight latest-abnormal entry above the table.

```vue
<div class="audit-log-archive-batch-ledger__overview">
  <article
    v-for="item in overviewCards"
    :key="item.key"
    class="audit-log-archive-batch-ledger__overview-card"
    :data-testid="item.testId"
    @click="item.clickable && emit('select-overview-card', item.key)"
  >
    <span>{{ item.label }}</span>
    <strong>{{ item.value }}</strong>
    <p>{{ item.meta }}</p>
  </article>
</div>

<button
  v-if="latestAbnormalFocus"
  data-testid="archive-batch-latest-focus"
  type="button"
  class="audit-log-archive-batch-ledger__latest-focus"
  @click="emit('select-latest-abnormal')"
>
  <span>最近异常批次</span>
  <strong>{{ latestAbnormalFocus.batchNo }}</strong>
  <small>{{ latestAbnormalFocus.occurredAt }}</small>
</button>
```

Change the prop contract accordingly:

```ts
interface ArchiveLatestAbnormalFocus {
  batchNo: string;
  occurredAt: string;
  active: boolean;
}

const props = defineProps<{
  overviewCards: ArchiveOverviewCard[];
  latestAbnormalFocus: ArchiveLatestAbnormalFocus | null;
  rows: ArchiveBatchRow[];
  total: number;
  loading: boolean;
  errorMessage: string;
}>();

const emit = defineEmits<{
  (event: 'select-latest-abnormal'): void;
  (event: 'select-overview-card', key: string): void;
  (event: 'select-row', row: ArchiveBatchRow): void;
  (event: 'open-detail', row: ArchiveBatchRow): void;
}>();
```

Delete the old `archive-governance-focus-strip` and `focusHint` blocks from the template.

- [ ] **Step 4: Run the focused component test again**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts
```

Expected: PASS. The panel should now expose the lighter summary surface and no longer render the legacy focus strip.

- [ ] **Step 5: Commit the component surface cleanup**

```powershell
git add spring-boot-iot-ui/src/components/auditLog/AuditLogArchiveTabPanel.vue spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts
git commit -m "refactor: declutter system log archive panel"
```

### Task 2: Compact the archive master table and preserve overview interactions in the view layer

**Files:**
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogArchiveTabPanel.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: Write the failing view test**

Add a view-level regression that proves the latest abnormal batch is now driven by the lightweight focus entry, while the three metric cards still filter the ledger.

```ts
it('opens the latest abnormal archive batch from the lightweight focus entry and keeps metric cards separate', async () => {
  const wrapper = mountView();
  await flushPromises();
  await nextTick();
  await triggerSystemLogTab(wrapper, 'archives');

  expect(wrapper.findAll('.audit-log-archive-batch-ledger__overview-card')).toHaveLength(3);
  expect(wrapper.find('[data-testid="archive-batch-latest-focus"]').exists()).toBe(true);

  await wrapper.get('[data-testid="archive-batch-latest-focus"]').trigger('click');
  await flushPromises();
  await nextTick();

  expect(wrapper.find('[data-testid="archive-batch-master-table"]').text()).toContain('iot_message_log');
  expect(wrapper.findAll('.audit-log-archive-batch-ledger__overview-card.is-active').length).toBe(0);
});
```

- [ ] **Step 2: Run the focused view test and verify it fails**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/views/AuditLogView.test.ts -t "opens the latest abnormal archive batch from the lightweight focus entry and keeps metric cards separate"
```

Expected: FAIL because `AuditLogView.vue` still prepares a fourth `latest` overview card and the component does not emit `select-latest-abnormal`.

- [ ] **Step 3: Implement the compact archive view contract**

In `AuditLogView.vue`, split the old overview model into three metric cards plus one dedicated latest-abnormal focus object.

```ts
const messageArchiveBatchOverviewCards = computed<ArchiveBatchOverviewCard[]>(() => {
  const overview = messageArchiveBatchOverview.value;
  return [
    {
      key: 'abnormal',
      label: '异常批次',
      value: formatOptionalCount(overview?.abnormalBatches),
      meta: `总批次 ${formatOptionalCount(overview?.totalBatches)}`,
      testId: 'archive-batch-overview-abnormal',
      clickable: true,
      active: activeMessageArchiveBatchOverviewSelection.value === 'abnormal'
    },
    {
      key: 'drifted',
      label: '执行偏差总量',
      value: formatSignedCount(overview?.totalDeltaConfirmedVsDeleted),
      meta: `已对齐 ${formatOptionalCount(overview?.matchedBatches)}`,
      testId: 'archive-batch-overview-drifted',
      clickable: true,
      active: activeMessageArchiveBatchOverviewSelection.value === 'drifted'
    },
    {
      key: 'remaining',
      label: '剩余过期总量',
      value: formatOptionalCount(overview?.totalRemainingExpiredRows),
      meta: `部分可比 ${formatOptionalCount(overview?.partialBatches)}`,
      testId: 'archive-batch-overview-remaining',
      clickable: true,
      active: activeMessageArchiveBatchOverviewSelection.value === 'remaining'
    }
  ];
});

const messageArchiveBatchLatestFocus = computed(() =>
  messageArchiveBatchOverview.value?.latestAbnormalBatch
    ? {
        batchNo: formatValue(messageArchiveBatchOverview.value.latestAbnormalBatch),
        occurredAt: formatValue(messageArchiveBatchOverview.value.latestAbnormalOccurredAt),
        active: activeMessageArchiveBatchOverviewSelection.value === 'latest'
      }
    : null
);

const handleMessageArchiveBatchLatestFocus = async () => {
  const latestBatchNo = String(messageArchiveBatchOverview.value?.latestAbnormalBatch || '').trim();
  if (!latestBatchNo) {
    return;
  }
  activeMessageArchiveBatchOverviewSelection.value = 'latest';
  messageArchiveBatchFocusedBatchNo.value = latestBatchNo;
  await refreshMessageArchiveBatchLedger();
};
```

Then pass the new prop and event:

```vue
<AuditLogArchiveTabPanel
  :overview-cards="messageArchiveBatchOverviewCards"
  :latest-abnormal-focus="messageArchiveBatchLatestFocus"
  @select-latest-abnormal="handleMessageArchiveBatchLatestFocus"
  @select-overview-card="handleMessageArchiveBatchOverviewClick"
  @select-row="handleMessageArchiveBatchRowSelect"
  @open-detail="openMessageArchiveBatchDetail"
/>
```

Also compact the table cells in `AuditLogArchiveTabPanel.vue` so each row shows:

```vue
<header class="audit-log-archive-master-table__header">
  <span>批次</span>
  <span>执行状态</span>
  <span>对比结论</span>
  <span>进度摘要</span>
  <span>风险信号</span>
  <span>操作</span>
</header>
```

Use small formatter helpers in `AuditLogView.vue` for the compact copy:

```ts
const formatArchiveBatchProgressSummary = (row: ObservabilityMessageArchiveBatch) =>
  `确认 ${formatCount(row.confirmedExpiredRows)} / 归档 ${formatCount(row.archivedRows)} / 删除 ${formatCount(row.deletedRows)}`;

const formatArchiveBatchRiskSignal = (row: ObservabilityMessageArchiveBatch) =>
  `偏差 ${formatSignedCount(row.deltaDryRunVsDeleted)} · 剩余 ${formatOptionalCount(row.remainingExpiredRows)} · 报告 ${formatArchiveBatchPreviewAvailability(row)}`;
```

- [ ] **Step 4: Run the archive-related view tests**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/views/AuditLogView.test.ts -t "archive|latest abnormal|summary"
```

Expected: PASS for the new latest-focus interaction test and the existing archive regressions around summary selection clearing, stale refresh protection, and detail-drawer loading.

- [ ] **Step 5: Commit the archive view contract changes**

```powershell
git add spring-boot-iot-ui/src/views/AuditLogView.vue spring-boot-iot-ui/src/components/auditLog/AuditLogArchiveTabPanel.vue spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "feat: streamline system log archive governance"
```

### Task 3: Final archive regression pass and build verification

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogArchiveTabPanel.vue`

- [ ] **Step 1: Add final compact-surface assertions**

Make the tests assert the first-screen declutter explicitly:

```ts
expect(wrapper.text()).toContain('异常批次');
expect(wrapper.text()).toContain('执行偏差总量');
expect(wrapper.text()).toContain('剩余过期总量');
expect(wrapper.text()).not.toContain('当前治理对象');
expect(wrapper.text()).not.toContain('最近异常批次不在当前结果中，请调整时间范围后重试');
```

Also assert the compact table structure:

```ts
expect(wrapper.find('[data-testid="archive-batch-master-table"]').text()).toContain('进度摘要');
expect(wrapper.find('[data-testid="archive-batch-master-table"]').text()).toContain('风险信号');
```

- [ ] **Step 2: Run the component + view archive tests**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts src/__tests__/views/AuditLogView.test.ts
```

Expected: PASS after the wording and structure settle. If the old focus hint or latest-card assumptions still remain anywhere, this run should expose them.

- [ ] **Step 3: Finish the final spacing and hierarchy polish**

Update the archive panel classes so the lighter summary and compact table read as one workbench.

```css
.audit-log-archive-batch-ledger__overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.audit-log-archive-batch-ledger__latest-focus {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
```

Keep the drawer integration unchanged; only the first-screen density should change.

- [ ] **Step 4: Run the final archive verification suite**

Run:

```powershell
node node_modules\vitest\vitest.mjs --run src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts src/__tests__/views/AuditLogView.test.ts
npm run build
```

Expected:
- Vitest PASS for archive component + view tests
- Vite build PASS with no prop-contract or template regressions

- [ ] **Step 5: Commit the final archive polish**

```powershell
git add spring-boot-iot-ui/src/components/auditLog/AuditLogArchiveTabPanel.vue spring-boot-iot-ui/src/views/AuditLogView.vue spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "style: reduce system log archive surface density"
```
