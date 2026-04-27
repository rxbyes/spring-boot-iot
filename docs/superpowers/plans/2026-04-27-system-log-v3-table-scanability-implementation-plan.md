# System-Log V3 Table Scanability Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the `/system-log` hotspot and archive tables easier to scan by unifying their row grammar, visual hierarchy, and action density without changing backend contracts.

**Architecture:** Keep the existing panel split (`AuditLogHotspotTabPanel.vue` and `AuditLogArchiveTabPanel.vue`) and refine each panel in place rather than introducing a new shared shell. Use tests first to lock the new table grammar, then update template structure and scoped styles so both tabs share the same reading rhythm: identity first, state early, compressed signal groups, and lightweight right-aligned actions.

**Tech Stack:** Vue 3 SFCs, TypeScript in `<script setup>`, scoped CSS, Vitest, Vue Test Utils, Vite build.

---

## File Structure

### UI files

- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogHotspotTabPanel.vue`
  - Rebuild the hotspot master table into a tighter five-column scan layout.
  - Add local formatting helpers for risk state and condensed performance signals using existing summary data.
  - Harmonize row height, secondary text, and action layout with the archive ledger.
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogArchiveTabPanel.vue`
  - Rebuild the archive batch master table into a clearer identity/status/conclusion/risk/time/action grammar.
  - Move timestamp emphasis into a dedicated “recent time” column and compress risk copy.
  - Align card, row, and action styling with the hotspot workbench.
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
  - Only if needed for prop compatibility or helper labels exposed to the two panels.
  - Keep business logic and backend query flow unchanged.

### Tests

- Modify: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts`
  - Assert the new five-column reading model and condensed row copy.
- Modify: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts`
  - Assert the new archive row grammar and lightweight focus treatment.
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
  - Update integration expectations so the system-log page verifies the new scan-first wording and row surfaces.

### Execution note

- This plan is executed in the current workspace instead of a fresh worktree because the active `system-log` UI changes already live in this working tree and would be missing from a clean fork.

## Task 1: Tighten the hotspot master table into a scan-first workbench

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogHotspotTabPanel.vue`

- [ ] **Step 1: Write the failing hotspot tests**

Update the hotspot component test so it locks the new row grammar instead of the old “target / latest / max / avg / count” spread. The test should assert:

```ts
expect(wrapper.find('[data-testid="hotspot-master-header"]').text()).toContain('热点对象')
expect(wrapper.find('[data-testid="hotspot-master-header"]').text()).toContain('风险状态')
expect(wrapper.find('[data-testid="hotspot-master-header"]').text()).toContain('性能信号')
expect(wrapper.find('[data-testid="hotspot-master-header"]').text()).toContain('最近情况')
expect(wrapper.find('[data-testid="hotspot-master-header"]').text()).toContain('操作')

const firstRow = wrapper.findAll('[data-testid="hotspot-master-row"]')[0]
expect(firstRow.text()).toContain('设备接入查询')
expect(firstRow.text()).toContain('GET /api/system/observability/spans/page')
expect(firstRow.text()).toContain('trace-hotspot-001')
expect(firstRow.text()).toContain('P峰 1632 ms')
expect(firstRow.text()).toContain('均值 982 ms')
expect(firstRow.text()).toContain('5 次')
```

Update the view test so it checks the user-facing row surface after switching to `观测热点`:

```ts
const hotspotTable = wrapper.find('[data-testid="hotspot-master-table"]')
expect(hotspotTable.text()).toContain('热点对象')
expect(hotspotTable.text()).toContain('风险状态')
expect(hotspotTable.text()).toContain('性能信号')
expect(hotspotTable.text()).toContain('最近情况')
```

- [ ] **Step 2: Run the hotspot tests to verify they fail**

Run:

```bash
node node_modules\vitest\vitest.mjs --run src\__tests__\components\auditLog\AuditLogHotspotTabPanel.test.ts src\__tests__\views\AuditLogView.test.ts -t "hotspot"
```

Expected: FAIL because the current template still renders the older seven-column wording and row layout.

- [ ] **Step 3: Implement the hotspot scanability layout**

Update `AuditLogHotspotTabPanel.vue` so the master table uses a five-column grammar and dual-layer identity cell. Keep all behavior, events, and drilldown wiring intact.

Use this structure for each row:

```vue
<header data-testid="hotspot-master-header" class="audit-log-hotspot-master-table__header">
  <span>热点对象</span>
  <span>风险状态</span>
  <span>性能信号</span>
  <span>最近情况</span>
  <span>操作</span>
</header>

<div class="audit-log-hotspot-master-table__cell audit-log-hotspot-master-table__cell--identity">
  <strong>{{ formatSlowSummaryTitle(row) }}</strong>
  <span>{{ formatSlowSummaryTarget(row) }}</span>
  <small>{{ formatValue(row.latestTraceId) }}</small>
</div>
```

Add local helpers that compress summary values without requiring API changes:

```ts
const resolveHotspotRiskLabel = (row: SlowSummaryRow) => {
  if (Number(row.maxDurationMs || 0) >= 2000) return '高耗时'
  if (Number(row.totalCount || 0) >= 8) return '高频次'
  return '持续关注'
}

const resolveHotspotRiskTone = (row: SlowSummaryRow) => {
  if (Number(row.maxDurationMs || 0) >= 2000) return 'is-danger'
  if (Number(row.totalCount || 0) >= 8) return 'is-warning'
  return 'is-neutral'
}
```

Render performance and recency as compressed groups:

```vue
<div class="audit-log-hotspot-master-table__signal-group">
  <span>P峰 {{ formatDuration(row.maxDurationMs) }}</span>
  <span>均值 {{ formatDuration(row.avgDurationMs) }}</span>
  <span>{{ formatCount(row.totalCount) }} 次</span>
</div>
```

Refresh scoped CSS so the row height, identity cell, chips, and action rail match the new scan-first rhythm:

```css
.audit-log-hotspot-master-table__header,
.audit-log-hotspot-master-table__row {
  grid-template-columns: minmax(18rem, 1.8fr) minmax(8rem, 0.8fr) minmax(13rem, 1fr) minmax(10rem, 0.9fr) minmax(9rem, 0.85fr);
}

.audit-log-hotspot-master-table__cell--identity {
  gap: 0.24rem;
}

.audit-log-hotspot-master-table__risk-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 0.7rem;
  border-radius: 999px;
}
```

- [ ] **Step 4: Run the hotspot tests to verify they pass**

Run:

```bash
node node_modules\vitest\vitest.mjs --run src\__tests__\components\auditLog\AuditLogHotspotTabPanel.test.ts src\__tests__\views\AuditLogView.test.ts -t "hotspot"
```

Expected: PASS for the hotspot-specific assertions.

- [ ] **Step 5: Commit the hotspot scanability slice**

```bash
git add spring-boot-iot-ui/src/components/auditLog/AuditLogHotspotTabPanel.vue spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "feat: tighten system-log hotspot table scanability"
```

## Task 2: Rebuild the archive ledger rows around identity, status, risk, and time

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Modify: `spring-boot-iot-ui/src/components/auditLog/AuditLogArchiveTabPanel.vue`
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue` (only if helper text or prop names need adjustment)

- [ ] **Step 1: Write the failing archive tests**

Update the archive component test so it asserts the new six-column reading model:

```ts
const header = wrapper.find('[data-testid="archive-batch-master-header"]')
expect(header.text()).toContain('归档批次')
expect(header.text()).toContain('执行状态')
expect(header.text()).toContain('对比结论')
expect(header.text()).toContain('风险信号')
expect(header.text()).toContain('最近时间')
expect(header.text()).toContain('操作')

const selectedRow = wrapper.findAll('[data-testid="archive-batch-master-row"]')[1]
expect(selectedRow.text()).toContain('iot_message_log-20260426090100')
expect(selectedRow.text()).toContain('iot_message_log')
expect(selectedRow.text()).toContain('30 天')
expect(selectedRow.text()).toContain('偏差 308')
expect(selectedRow.text()).toContain('剩余 308')
expect(selectedRow.text()).toContain('截止 2026-03-27 09:00:00')
```

Update the view test so it validates the revised archive table surface after switching to `归档治理`:

```ts
const archiveTable = wrapper.find('[data-testid="archive-batch-master-table"]')
expect(archiveTable.text()).toContain('归档批次')
expect(archiveTable.text()).toContain('执行状态')
expect(archiveTable.text()).toContain('对比结论')
expect(archiveTable.text()).toContain('风险信号')
expect(archiveTable.text()).toContain('最近时间')
```

- [ ] **Step 2: Run the archive tests to verify they fail**

Run:

```bash
node node_modules\vitest\vitest.mjs --run src\__tests__\components\auditLog\AuditLogArchiveTabPanel.test.ts src\__tests__\views\AuditLogView.test.ts -t "archive|归档|summary"
```

Expected: FAIL because the existing ledger still renders `进度摘要` and places timestamps inside the first or last column.

- [ ] **Step 3: Implement the archive scanability layout**

Update `AuditLogArchiveTabPanel.vue` so the master table uses the six-column scan grammar and a lighter action rail.

Use this structure:

```vue
<header data-testid="archive-batch-master-header" class="audit-log-archive-master-table__header">
  <span>归档批次</span>
  <span>执行状态</span>
  <span>对比结论</span>
  <span>风险信号</span>
  <span>最近时间</span>
  <span>操作</span>
</header>

<div class="audit-log-archive-master-table__cell audit-log-archive-master-table__cell--identity">
  <strong>{{ formatArchiveBatchName(row) }}</strong>
  <span>{{ formatValue(row.sourceTable) }} / {{ formatRetentionDays(row.retentionDays) }}</span>
</div>
```

Split risk and time instead of mixing them into progress/actions:

```vue
<div class="audit-log-archive-master-table__cell">
  <span>偏差 {{ formatSignedCount(row.deltaDryRunVsDeleted) }}</span>
  <span>剩余 {{ formatOptionalCount(row.remainingExpiredRows) }}</span>
  <span>报告 {{ formatArchiveBatchPreviewAvailability(row) }}</span>
</div>

<div class="audit-log-archive-master-table__cell">
  <strong>{{ formatValue(row.createTime || row.updateTime) }}</strong>
  <span>截止 {{ formatValue(row.cutoffAt) }}</span>
</div>
```

Move confirmed/archive/delete counts into a compact footer inside the identity cell or into a secondary status line only if they remain useful, but do not keep them as a dedicated primary column.

Refresh CSS so the archive rows align visually with the hotspot rows:

```css
.audit-log-archive-master-table__header,
.audit-log-archive-master-table__row {
  grid-template-columns: minmax(18rem, 1.7fr) minmax(8rem, 0.8fr) minmax(8rem, 0.8fr) minmax(12rem, 1fr) minmax(11rem, 0.95fr) minmax(8rem, 0.8fr);
}

.audit-log-archive-master-table__cell--identity {
  gap: 0.24rem;
}

.audit-log-archive-batch-ledger__latest-focus {
  min-height: 40px;
}
```

- [ ] **Step 4: Run the archive tests to verify they pass**

Run:

```bash
node node_modules\vitest\vitest.mjs --run src\__tests__\components\auditLog\AuditLogArchiveTabPanel.test.ts src\__tests__\views\AuditLogView.test.ts -t "archive|归档|summary"
```

Expected: PASS for the archive-focused assertions.

- [ ] **Step 5: Commit the archive scanability slice**

```bash
git add spring-boot-iot-ui/src/components/auditLog/AuditLogArchiveTabPanel.vue spring-boot-iot-ui/src/views/AuditLogView.vue spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "feat: streamline system-log archive table scanability"
```

## Task 3: Run combined verification for the full system-log surface

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Verify: `spring-boot-iot-ui`

- [ ] **Step 1: Run the combined system-log Vitest suite**

Run:

```bash
node node_modules\vitest\vitest.mjs --run src\__tests__\components\auditLog\AuditLogHotspotTabPanel.test.ts src\__tests__\components\auditLog\AuditLogArchiveTabPanel.test.ts src\__tests__\views\AuditLogView.test.ts
```

Expected: PASS with all hotspot, archive, and system-log integration tests green.

- [ ] **Step 2: Run the frontend production build**

Run:

```bash
npm run build
```

Expected: Vite build completes successfully with exit code `0`.

- [ ] **Step 3: Commit the verification-ready UI polish**

```bash
git add spring-boot-iot-ui/src/components/auditLog/AuditLogHotspotTabPanel.vue spring-boot-iot-ui/src/components/auditLog/AuditLogArchiveTabPanel.vue spring-boot-iot-ui/src/views/AuditLogView.vue spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogHotspotTabPanel.test.ts spring-boot-iot-ui/src/__tests__/components/auditLog/AuditLogArchiveTabPanel.test.ts spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "feat: polish system-log table scanability"
```

## Self-Review

### Spec coverage

1. Unified scan rules are covered by Task 1 and Task 2 column/header rewrites.
2. Hotspot row tightening and condensed signals are covered by Task 1.
3. Archive row tightening and separated time/risk emphasis are covered by Task 2.
4. Focus on tests plus build verification is covered by Task 3.

### Placeholder scan

1. No `TODO`/`TBD` markers remain.
2. Every task includes exact files and exact commands.
3. The code examples reference concrete component names and prop names that already exist in the codebase.

### Type consistency

1. `hotspotDrilldownView` and existing drilldown events remain unchanged.
2. Archive emit names (`select-row`, `open-detail`, `select-latest-abnormal`) remain unchanged.
3. The plan preserves the existing panel/view interface and only refines presentation semantics.
