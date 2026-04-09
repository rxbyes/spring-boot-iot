# Access List Workbench Batch 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把接入侧第一批高频台账页统一到同一套列表工作台模板，先稳定共享层与首批来源页/目标页，再为后续风险域与平台治理批次复用。

**Architecture:** 这个总范围 spec 实际覆盖了 4 个相对独立的子系统，因此本计划只执行第一批“接入侧高频台账”。先把共享样式和共享列表节奏收口到 `styles + Standard*` 组件，再迁移 `ProductWorkbenchView`、`DeviceWorkbenchView`、`MessageTraceView`、`AccessErrorArchivePanel`、`AuditLogView`。详情抽屉、表单抽屉、导入/替换流程和后端接口全部保持不动。

**Tech Stack:** Vue 3、TypeScript、Element Plus、Vitest、Vite、共享 `Standard*` 组件、CSS variables、前端 guard 脚本

---

## Scope Note

全站总 spec 已确认要分 4 批推进。本计划只覆盖：

- 第一批接入侧高频台账
- 共享层改造
- 第一批文档和守卫收口

第二批风险域、第三批平台治理、第四批特殊工作台需要在第一批通过后分别产出独立实施计划，不能把四批内容混进同一轮编码。

## File Structure

### Existing files to modify

- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\styles\tokens.css`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\styles\global.css`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\StandardWorkbenchPanel.vue`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\StandardListFilterHeader.vue`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\ProductWorkbenchView.vue`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceWorkbenchView.vue`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\MessageTraceView.vue`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\AuditLogView.vue`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\AccessErrorArchivePanel.vue`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\scripts\list-page-guard.mjs`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\components\StandardWorkbenchPanel.test.ts`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\components\StandardListFilterHeader.test.ts`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\ProductWorkbenchView.test.ts`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\DeviceWorkbenchView.test.ts`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\MessageTraceView.test.ts`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\AuditLogView.test.ts`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\components\AccessErrorArchivePanel.test.ts`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\OperationsWorkbenchRefinement.test.ts`
- `E:\idea\ghatg\spring-boot-iot\docs\06-前端开发与CSS规范.md`
- `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- `E:\idea\ghatg\spring-boot-iot\docs\15-前端优化与治理计划.md`

### New files to create

- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\styles\workbench-list.css`

### Review-only sync targets

- `E:\idea\ghatg\spring-boot-iot\README.md`
- `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

说明：

- 第一批不改 `ReportWorkbenchView.vue` 与 `FilePayloadDebugView.vue` 的正文主舞台；它们只在后续批次按“列表子区”再拆小计划。
- 本计划不新增新的共享大组件，统一优先通过 `styles + Standard*` 组件收口。
- 当前工作区是脏的，每次提交只能 `git add` 本计划列出的文件，不要 `git add .`。

## Task 1: 建立第一批共享列表模板底座

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\styles\workbench-list.css`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\styles\tokens.css`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\styles\global.css`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\StandardWorkbenchPanel.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\StandardListFilterHeader.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\components\StandardWorkbenchPanel.test.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\components\StandardListFilterHeader.test.ts`

- [ ] **Step 1: 先写共享底座失败测试，锁定样式入口和组件钩子**

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\components\StandardWorkbenchPanel.test.ts` 追加：

```ts
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

it('uses shared workbench spacing tokens instead of page-private gaps', () => {
  const source = readFileSync(
    resolve(import.meta.dirname, '../../components/StandardWorkbenchPanel.vue'),
    'utf8'
  )

  expect(source).toContain('--ops-workbench-gap')
  expect(source).toContain('var(--ops-workbench-gap')
  expect(source).toContain('standard-workbench-panel__body')
})
```

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\components\StandardListFilterHeader.test.ts` 追加：

```ts
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

it('keeps the filter grid and actions row on shared spacing variables', () => {
  const source = readFileSync(
    resolve(import.meta.dirname, '../../components/StandardListFilterHeader.vue'),
    'utf8'
  )

  expect(source).toContain('--ops-filter-grid-gap')
  expect(source).toContain('--ops-filter-actions-gap')
  expect(source).toContain('StandardActionGroup gap="sm"')
})
```

- [ ] **Step 2: 运行测试，确认共享底座尚未接入这些变量**

Run:

```bash
cd E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui
npm run test -- src/__tests__/components/StandardWorkbenchPanel.test.ts src/__tests__/components/StandardListFilterHeader.test.ts --run
```

Expected: FAIL，原因是当前组件源码还没有 `--ops-workbench-gap`、`--ops-filter-grid-gap`、`--ops-filter-actions-gap`。

- [ ] **Step 3: 增加共享 token、共享列表样式文件，并让组件改用共享变量**

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\styles\tokens.css` 新增：

```css
  --ops-workbench-gap: 0.72rem;
  --ops-filter-grid-gap: 8px;
  --ops-filter-actions-gap: 14px;
  --ops-list-surface-radius: calc(var(--radius-lg) + 2px);
  --ops-list-surface-bg: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(247, 250, 255, 0.76));
  --ops-list-surface-bg-strong: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  --ops-list-surface-mask: rgba(248, 250, 255, 0.78);
  --ops-mobile-card-padding: 0.92rem 0.96rem;
  --ops-mobile-card-gap: 0.8rem;
  --ops-meta-chip-font-size: 11.5px;
  --ops-field-label-font-size: 11.5px;
  --ops-field-value-font-size: 13px;
```

创建 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\styles\workbench-list.css`：

```css
.standard-list-surface {
  position: relative;
  isolation: isolate;
  min-height: 14rem;
  border-radius: var(--ops-list-surface-radius);
  background: var(--ops-list-surface-bg);
}

.standard-list-surface .el-loading-mask {
  border-radius: inherit;
  background: var(--ops-list-surface-mask) !important;
  backdrop-filter: blur(5px);
}

.standard-mobile-record-grid {
  display: grid;
  gap: 12px;
}

.standard-mobile-record-card {
  display: grid;
  gap: var(--ops-mobile-card-gap);
  padding: var(--ops-mobile-card-padding);
  border: 1px solid var(--panel-border);
  border-radius: var(--ops-list-surface-radius);
  background: var(--ops-list-surface-bg-strong);
  box-shadow: var(--shadow-inset-highlight-76);
}

.standard-mobile-record-card__meta-item {
  display: inline-flex;
  align-items: center;
  min-height: 1.6rem;
  padding: 0.2rem 0.58rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--text-tertiary) 10%, transparent);
  color: var(--text-caption);
  font-size: var(--ops-meta-chip-font-size);
  line-height: 1.4;
}

.standard-mobile-record-card__field-label {
  color: var(--text-caption-2);
  font-size: var(--ops-field-label-font-size);
  line-height: 1.4;
}

.standard-mobile-record-card__field-value {
  color: var(--text-heading);
  font-size: var(--ops-field-value-font-size);
  font-weight: 600;
  line-height: 1.52;
}
```

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\styles\global.css` 顶部字体导入之后追加：

```css
@import "./workbench-list.css";
```

把 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\StandardWorkbenchPanel.vue` 的样式改成：

```css
.standard-workbench-panel {
  --ops-workbench-gap: var(--ops-workbench-gap);
  min-width: 0;
}

.standard-workbench-panel__filters,
.standard-workbench-panel__applied-filters,
.standard-workbench-panel__notices,
.standard-workbench-panel__toolbar,
.standard-workbench-panel__inline-state {
  margin-bottom: var(--ops-workbench-gap);
}

.standard-workbench-panel__pagination {
  margin-top: var(--ops-workbench-gap);
}
```

把 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\StandardListFilterHeader.vue` 的样式改成：

```css
.standard-list-filter-header__row {
  display: grid;
  grid-template-columns: var(--slfh-primary-columns);
  gap: var(--ops-filter-grid-gap, 8px);
  align-items: end;
}

.standard-list-filter-header__advanced-grid {
  display: grid;
  grid-template-columns: var(--slfh-advanced-columns);
  gap: var(--ops-filter-grid-gap, 8px);
}

.standard-list-filter-header__actions-row--minimal {
  margin-top: 12px;
  gap: 8px var(--ops-filter-actions-gap, 14px);
}
```

- [ ] **Step 4: 重新运行共享组件测试，确认转绿**

Run:

```bash
cd E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui
npm run test -- src/__tests__/components/StandardWorkbenchPanel.test.ts src/__tests__/components/StandardListFilterHeader.test.ts --run
```

Expected: PASS。

- [ ] **Step 5: 提交共享底座**

```bash
git add spring-boot-iot-ui/src/styles/tokens.css spring-boot-iot-ui/src/styles/global.css spring-boot-iot-ui/src/styles/workbench-list.css spring-boot-iot-ui/src/components/StandardWorkbenchPanel.vue spring-boot-iot-ui/src/components/StandardListFilterHeader.vue spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchPanel.test.ts spring-boot-iot-ui/src/__tests__/components/StandardListFilterHeader.test.ts
git commit -m "feat: add shared access list workbench foundation"
```

## Task 2: 让产品定义中心和设备资产中心接入共享列表模板

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\ProductWorkbenchView.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceWorkbenchView.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\ProductWorkbenchView.test.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\DeviceWorkbenchView.test.ts`

- [ ] **Step 1: 先写来源页/目标页失败测试，锁定共享 surface 与移动卡片语法**

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\ProductWorkbenchView.test.ts` 追加：

```ts
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

it('uses the shared list surface and mobile-card grammar', () => {
  const source = readFileSync(resolve(import.meta.dirname, '../../views/ProductWorkbenchView.vue'), 'utf8')

  expect(source).toContain('standard-list-surface')
  expect(source).toContain('standard-mobile-record-grid')
  expect(source).toContain('standard-mobile-record-card')
})
```

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\DeviceWorkbenchView.test.ts` 追加：

```ts
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

it('keeps the device workbench on the shared list surface and trims toolbar density', () => {
  const source = readFileSync(resolve(import.meta.dirname, '../../views/DeviceWorkbenchView.vue'), 'utf8')

  expect(source).toContain('standard-list-surface')
  expect(source).toContain('standard-mobile-record-grid')
  expect(source).toContain('compact')
  expect(source).toContain('已登记 ${registeredCount} 台')
  expect(source).not.toContain('`未登记 ${unregisteredCount} 台`')
})
```

- [ ] **Step 2: 运行来源页/目标页测试，确认当前为红灯**

Run:

```bash
cd E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/DeviceWorkbenchView.test.ts --run
```

Expected: FAIL，因为当前两个页面源码还没有共享 surface/mobile-card 类，设备页工具条也还保留更满的元信息。

- [ ] **Step 3: 把产品页和设备页的结果区、移动卡片和工具条接入共享模板**

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\ProductWorkbenchView.vue` 把结果区和移动卡片类名改成：

```vue
<div
  v-loading="loading && hasRecords"
  class="product-result-panel standard-list-surface"
  element-loading-text="正在刷新产品列表"
  element-loading-background="var(--loading-mask-bg)"
>
  <div class="product-mobile-list standard-mobile-record-list">
    <div class="product-mobile-list__grid standard-mobile-record-grid">
      <article v-for="row in tableData" :key="getProductRowKey(row)" class="product-mobile-card standard-mobile-record-card">
```

并把 meta / field 的类挂上共享命名：

```vue
<span class="product-mobile-card__meta-item standard-mobile-record-card__meta-item">
<span class="standard-mobile-record-card__field-label">厂商</span>
<strong class="standard-mobile-record-card__field-value">{{ formatTextValue(row.manufacturer) }}</strong>
```

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceWorkbenchView.vue` 进行同样处理，并把工具条改成：

```vue
<StandardTableToolbar
  compact
  :meta-items="[
    `已选 ${selectedRows.length} 项`,
    `已登记 ${registeredCount} 台`,
    `在线 ${onlineCount} 台`,
    `已激活 ${activatedCount} 台`,
    `停用 ${disabledCount} 台`
  ]"
>
```

同时给设备页结果区和移动卡片接入同样的共享类：

```vue
<div class="device-result-panel standard-list-surface">
<div class="device-mobile-list__grid standard-mobile-record-grid">
<article class="device-mobile-card standard-mobile-record-card">
```

执行这一步时，删除两页 scoped CSS 里已经被 `workbench-list.css` 接管的重复规则：

- 重复的表面背景
- 重复的卡片 padding
- 重复的 meta 字号
- 重复的字段 label/value 字号

保留只属于产品/设备业务差异的样式：

- 产品/设备独有字段宽度与截断
- 设备状态色修饰
- 业务专属结构细节

- [ ] **Step 4: 重新运行来源页/目标页测试**

Run:

```bash
cd E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/DeviceWorkbenchView.test.ts --run
```

Expected: PASS。

- [ ] **Step 5: 提交来源页和目标页对齐**

```bash
git add spring-boot-iot-ui/src/views/ProductWorkbenchView.vue spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts
git commit -m "feat: align product and device workbenches to shared list template"
```

## Task 3: 把链路追踪台和失败归档台接入同一套台账模板

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\MessageTraceView.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\AccessErrorArchivePanel.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\MessageTraceView.test.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\components\AccessErrorArchivePanel.test.ts`

- [ ] **Step 1: 先写失败测试，锁定共享 surface、共享移动卡片和共享行内操作组件**

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\MessageTraceView.test.ts` 追加：

```ts
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

it('uses shared workbench row actions and shared list surface in trace mode', () => {
  const source = readFileSync(resolve(import.meta.dirname, '../../views/MessageTraceView.vue'), 'utf8')

  expect(source).toContain('class="message-trace-table-wrap standard-list-surface"')
  expect(source).toContain('<StandardWorkbenchRowActions')
  expect(source).toContain('standard-mobile-record-grid')
})
```

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\components\AccessErrorArchivePanel.test.ts` 追加：

```ts
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

it('collapses archive actions into direct actions plus menu and uses shared list surface', () => {
  const source = readFileSync(resolve(import.meta.dirname, '../../components/AccessErrorArchivePanel.vue'), 'utf8')

  expect(source).toContain('class="access-error-table-wrap standard-list-surface"')
  expect(source).toContain('<StandardWorkbenchRowActions')
  expect(source).toContain('standard-mobile-record-grid')
  expect(source).toContain("menuLabel=\"更多\"")
})
```

- [ ] **Step 2: 运行测试确认追踪台/归档台当前还没完成模板迁移**

Run:

```bash
cd E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui
npm run test -- src/__tests__/views/MessageTraceView.test.ts src/__tests__/components/AccessErrorArchivePanel.test.ts --run
```

Expected: FAIL，因为两个源码当前还在使用页面私有表格壳和 `StandardRowActions`。

- [ ] **Step 3: 为追踪台和归档台接入共享 surface、移动卡片和统一操作语法**

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\MessageTraceView.vue` 中：

1. 用共享 surface 包住表格：

```vue
<div class="message-trace-table-wrap standard-list-surface">
  <div class="message-trace-mobile-list standard-mobile-record-list">
    <div class="message-trace-mobile-list__grid standard-mobile-record-grid">
      <article v-for="row in tableData" :key="row.id" class="message-trace-mobile-card standard-mobile-record-card">
```

2. 新增移动卡片字段：

```vue
<span class="standard-mobile-record-card__field-label">TraceId</span>
<strong class="standard-mobile-record-card__field-value">{{ row.traceId || '--' }}</strong>
```

3. 把桌面操作列改成 `StandardWorkbenchRowActions`：

```vue
<StandardWorkbenchRowActions
  variant="table"
  gap="compact"
  :direct-items="getTraceDirectActions(row)"
  @command="(command) => handleTraceRowAction(command, row)"
/>
```

并新增最小 helper：

```ts
function getTraceDirectActions(row: MessageTraceLog) {
  return [
    { command: 'detail', label: '详情' },
    { command: 'observe', label: '观测', disabled: !canJumpWithRow(row) }
  ]
}
```

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\components\AccessErrorArchivePanel.vue` 中：

1. 同样包共享 surface 和移动卡片：

```vue
<div class="access-error-table-wrap standard-list-surface">
  <div class="access-error-mobile-list standard-mobile-record-list">
    <div class="access-error-mobile-list__grid standard-mobile-record-grid">
```

2. 桌面/移动操作都改成“2 个直出 + 1 个更多”：

```vue
<StandardWorkbenchRowActions
  variant="table"
  gap="compact"
  :direct-items="getArchiveDirectActions(row)"
  :menu-items="accessErrorMenuItems"
  menu-label="更多"
  @command="(command) => handleArchiveRowAction(command, row)"
/>
```

对应 helper：

```ts
const accessErrorMenuItems = [
  { command: 'observe', label: '观测' }
]

function getArchiveDirectActions(row: AccessErrorRecord) {
  return [
    { command: 'detail', label: '详情' },
    { command: 'trace', label: '追踪', disabled: !canJumpToTrace(row) }
  ]
}
```

- [ ] **Step 4: 重新运行追踪台/归档台测试**

Run:

```bash
cd E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui
npm run test -- src/__tests__/views/MessageTraceView.test.ts src/__tests__/components/AccessErrorArchivePanel.test.ts --run
```

Expected: PASS。

- [ ] **Step 5: 提交追踪台和归档台模板迁移**

```bash
git add spring-boot-iot-ui/src/views/MessageTraceView.vue spring-boot-iot-ui/src/components/AccessErrorArchivePanel.vue spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts spring-boot-iot-ui/src/__tests__/components/AccessErrorArchivePanel.test.ts
git commit -m "feat: align trace and archive workbenches to shared list template"
```

## Task 4: 把异常观测台收口到同一套列表模板

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\AuditLogView.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\AuditLogView.test.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\OperationsWorkbenchRefinement.test.ts`

- [ ] **Step 1: 先写失败测试，锁定共享 row-actions、自适应宽度和移动列表**

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\AuditLogView.test.ts` 追加：

```ts
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

it('uses shared workbench row actions and mobile list grammar in system mode', () => {
  const source = readFileSync(resolve(import.meta.dirname, '../../views/AuditLogView.vue'), 'utf8')

  expect(source).toContain('<StandardWorkbenchRowActions')
  expect(source).toContain('standard-list-surface')
  expect(source).toContain('standard-mobile-record-grid')
})
```

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\OperationsWorkbenchRefinement.test.ts` 追加：

```ts
it('aligns audit-log action columns with adaptive shared row actions', () => {
  const source = readFileSync(resolve(sourceRoot, 'src/views/AuditLogView.vue'), 'utf8')

  expect(source).toContain('<StandardWorkbenchRowActions')
  expect(source).toContain('class-name="standard-row-actions-column"')
  expect(source).toContain('auditActionColumnWidth')
})
```

- [ ] **Step 2: 运行测试，确认异常观测台当前还没完全进入模板**

Run:

```bash
cd E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui
npm run test -- src/__tests__/views/AuditLogView.test.ts src/__tests__/views/OperationsWorkbenchRefinement.test.ts --run
```

Expected: FAIL。

- [ ] **Step 3: 给 AuditLogView 增加共享 surface、移动卡片和统一 action grammar**

在 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\AuditLogView.vue` 中：

1. 把表格区域包上共享 surface：

```vue
<div class="audit-log-table-wrap standard-list-surface">
  <div class="audit-log-mobile-list standard-mobile-record-list">
    <div class="audit-log-mobile-list__grid standard-mobile-record-grid">
```

2. 给系统模式和业务模式都补移动卡片字段：

```vue
<span class="standard-mobile-record-card__field-label">操作模块</span>
<strong class="standard-mobile-record-card__field-value">{{ row.operationModule || '--' }}</strong>
```

3. 把行内操作改成共享 workbench 组合：

```vue
<StandardWorkbenchRowActions
  variant="table"
  gap="compact"
  :direct-items="getAuditDirectActions(row)"
  :menu-items="getAuditMenuItems(row)"
  menu-label="更多"
  @command="(command) => handleAuditRowAction(command, row)"
/>
```

脚本里新增：

```ts
function getAuditDirectActions(row: AuditLogRecord) {
  if (isSystemMode.value) {
    return [
      { command: 'detail', label: '详情' },
      { command: 'trace', label: '追踪', disabled: !canJumpToMessageTrace(row) }
    ]
  }

  return [
    { command: 'detail', label: '详情' },
    { command: 'delete', label: '删除' }
  ]
}

function getAuditMenuItems(row: AuditLogRecord) {
  return isSystemMode.value ? [{ command: 'delete', label: '删除' }] : []
}
```

同时把桌面操作列宽度继续交给现有 `auditActionColumnWidth`，不要改回写死数值。

- [ ] **Step 4: 重新运行异常观测台相关测试**

Run:

```bash
cd E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui
npm run test -- src/__tests__/views/AuditLogView.test.ts src/__tests__/views/OperationsWorkbenchRefinement.test.ts --run
```

Expected: PASS。

- [ ] **Step 5: 提交异常观测台模板迁移**

```bash
git add spring-boot-iot-ui/src/views/AuditLogView.vue spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts spring-boot-iot-ui/src/__tests__/views/OperationsWorkbenchRefinement.test.ts
git commit -m "feat: align audit workbench to shared list template"
```

## Task 5: 收紧第一批守卫并同步文档

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\scripts\list-page-guard.mjs`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\06-前端开发与CSS规范.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\15-前端优化与治理计划.md`
- Review: `E:\idea\ghatg\spring-boot-iot\README.md`
- Review: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

- [ ] **Step 1: 先写守卫失败场景，让第一批来源页/目标页进入治理名单**

把 `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\scripts\list-page-guard.mjs` 的 `governedViews` 改成：

```js
const governedViews = [
  "ProductWorkbenchView.vue",
  "DeviceWorkbenchView.vue",
  "AuditLogView.vue",
  "MessageTraceView.vue",
  "AlarmCenterView.vue",
  "EventDisposalView.vue",
  "RiskPointView.vue",
  "RuleDefinitionView.vue",
  "LinkageRuleView.vue",
  "EmergencyPlanView.vue",
  "OrganizationView.vue",
  "UserView.vue",
  "RoleView.vue",
  "RegionView.vue",
  "MenuView.vue",
  "DictView.vue",
  "ChannelView.vue",
  "InAppMessageView.vue",
  "HelpDocView.vue"
].map((fileName) => path.join(viewsRoot, fileName));
```

如果第一批页面尚未全部满足 `StandardListFilterHeader / StandardTableToolbar / StandardPagination`，这一步会让守卫先变红，这是预期行为。

- [ ] **Step 2: 原位同步文档规则**

在 `E:\idea\ghatg\spring-boot-iot\docs\06-前端开发与CSS规范.md` 追加：

```md
- `产品定义中心` 当前继续作为高频台账模板来源页之一；第一批接入侧列表页的 `设备资产中心`、`链路追踪台`、`异常观测台` 与 `接入失败归档台` 必须优先继承其列表工作台语法，不再各自维持第二套结果区、移动卡片和操作列样式。
- 第一批接入侧列表模板当前固定采用“共享 surface + 共享移动卡片 + 共享自适应操作列”的收口方式；页面 scoped CSS 只允许保留业务差异，不再重复声明底板、卡片 padding 和字段字号。
```

在 `E:\idea\ghatg\spring-boot-iot\docs\15-前端优化与治理计划.md` 追加：

```md
55. 第一批接入侧高频台账当前已按“来源页 + 目标页”方式推进：`/products` 为来源页，`/devices`、`/message-trace`、`/system-log` 与失败归档为首批目标页；后续批次必须继续沿该方式推进，而不是跨域混改。
56. 第一批接入侧列表模板当前固定通过 `workbench-list.css` 承接 surface、移动卡片和字段字号基线；如后续页面继续出现 scoped CSS 自建结果底板或移动卡片字阶，视为回退。
```

在 `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md` 新增：

```md
- 2026-03-31：接入侧第一批高频台账开始按“全站列表工作台模板”收口，当前来源页固定为 `/products`，首批目标页包括 `/devices`、`/message-trace`、`/system-log` 与失败归档列表组件；本轮统一共享列表底板、移动卡片语法、自适应操作列和文档治理规则，不触碰详情/表单流程。
```

- [ ] **Step 3: 复核 README 和 AGENTS，只在高层入口需要时补一句**

如果 `README.md` 与 `AGENTS.md` 已经能表达“全站列表模板按批次推进”的事实，则保持不改，并在执行记录中写：

```text
README.md reviewed: no update needed
AGENTS.md reviewed: no update needed
```

只有当入口文档无法描述当前模板治理事实时，才追加一句：

```md
- 前端带列表模块当前按“全站列表工作台模板”分批推进，首批为接入侧高频台账。
```

- [ ] **Step 4: 提交守卫与文档**

```bash
git add spring-boot-iot-ui/scripts/list-page-guard.mjs docs/06-前端开发与CSS规范.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
git commit -m "docs: sync batch1 list workbench rollout guidance"
```

如果 `README.md` 或 `AGENTS.md` 在 Step 3 中确实变更，则把它们追加到同一条 `git add` 命令。

## Task 6: 执行第一批完整验证并交接到编码执行

**Files:**
- Modify only if验证暴露真实缺陷并且缺陷属于本计划文件。

- [ ] **Step 1: 运行第一批受影响测试**

Run:

```bash
cd E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui
npm run test -- src/__tests__/components/StandardWorkbenchPanel.test.ts src/__tests__/components/StandardListFilterHeader.test.ts src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/DeviceWorkbenchView.test.ts src/__tests__/views/MessageTraceView.test.ts src/__tests__/views/AuditLogView.test.ts src/__tests__/components/AccessErrorArchivePanel.test.ts src/__tests__/views/OperationsWorkbenchRefinement.test.ts --run
```

Expected: PASS。

- [ ] **Step 2: 运行第一批前端守卫**

Run:

```bash
cd E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui
npm run component:guard
npm run list:guard
npm run style:guard
```

Expected:

- `component:guard` PASS
- `list:guard` PASS
- `style:guard` PASS

- [ ] **Step 3: 运行第一批前端构建**

Run:

```bash
cd E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui
npm run build
```

Expected: Vite build PASS。

- [ ] **Step 4: 运行仓库级质量门禁**

Run:

```bash
cd E:\idea\ghatg\spring-boot-iot
node scripts/run-quality-gates.mjs
```

Expected:

- 前端构建、守卫和仓库级脚本通过
- 若受共享环境或本机 JDK/Node 版本阻塞，必须明确记录阻塞项，不得伪报通过

- [ ] **Step 5: 最终检查变更范围并提交最后一笔代码**

Run:

```bash
git diff --name-only
git status --short
```

Expected:

- 只出现本计划列出的第一批接入侧文件和文档
- 不把其他脏工作区文件一并带入

最终提交：

```bash
git add spring-boot-iot-ui/src/styles/tokens.css spring-boot-iot-ui/src/styles/global.css spring-boot-iot-ui/src/styles/workbench-list.css spring-boot-iot-ui/src/components/StandardWorkbenchPanel.vue spring-boot-iot-ui/src/components/StandardListFilterHeader.vue spring-boot-iot-ui/src/views/ProductWorkbenchView.vue spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue spring-boot-iot-ui/src/views/MessageTraceView.vue spring-boot-iot-ui/src/views/AuditLogView.vue spring-boot-iot-ui/src/components/AccessErrorArchivePanel.vue spring-boot-iot-ui/scripts/list-page-guard.mjs spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchPanel.test.ts spring-boot-iot-ui/src/__tests__/components/StandardListFilterHeader.test.ts spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts spring-boot-iot-ui/src/__tests__/views\MessageTraceView.test.ts spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts spring-boot-iot-ui/src/__tests__/components/AccessErrorArchivePanel.test.ts spring-boot-iot-ui/src/__tests__/views/OperationsWorkbenchRefinement.test.ts docs/06-前端开发与CSS规范.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
git commit -m "feat: roll out batch1 shared list workbench template"
```

## Self-Review Checklist

- 本计划只覆盖第一批接入侧，不混入第二批到第四批。
- 每个 spec 要求都有对应任务：
  - 共享模板边界 -> Task 1
  - 第一批来源页/目标页迁移 -> Task 2~4
  - 文档与守卫 -> Task 5
  - 完整验证 -> Task 6
- 没有使用 `TODO`、`TBD` 或“后续补齐细节”一类占位。
- 所有命令、文件路径和提交范围都写死，执行时不需要再猜。
