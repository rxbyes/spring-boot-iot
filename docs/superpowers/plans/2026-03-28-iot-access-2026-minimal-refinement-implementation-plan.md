# IoT Access 2026 Minimal Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变当前系统主配色、两层导航结构和业务布局的前提下，为 `接入智维` 六个核心页面实现“静稳控制台基线 + 少量现代 SaaS 留白”的全站视觉精修。

**Architecture:** 先收口共享组件视觉基线，再让单主列表页与真页签诊断页批量跟随，避免页面私有样式继续分叉。实现继续复用现有 `StandardWorkbenchPanel`、`StandardListFilterHeader`、`StandardTableToolbar`、`IotAccessTabWorkspace` 和现有 token 体系，只做同色系层次微调与节奏统一。

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vitest, Vite, CSS variables, shared Standard* components

---

## File Structure

### Existing files to modify

- `spring-boot-iot-ui/src/components/StandardWorkbenchPanel.vue`
- `spring-boot-iot-ui/src/components/StandardListFilterHeader.vue`
- `spring-boot-iot-ui/src/components/StandardTableToolbar.vue`
- `spring-boot-iot-ui/src/components/iotAccess/IotAccessTabWorkspace.vue`
- `spring-boot-iot-ui/src/styles/tokens.css`
- `spring-boot-iot-ui/src/styles/element-overrides.css`
- `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/AuditLogView.vue`
- `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue`
- `spring-boot-iot-ui/src/__tests__/components/StandardListFilterHeader.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts`
- `docs/06-前端开发与CSS规范.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`

### New files to create

- `spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchPanel.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/StandardTableToolbar.test.ts`

### Review-only sync targets

- `README.md`
- `AGENTS.md`

## Implementation Notes

- 当前工作区已存在其他未提交改动，本计划执行时只能 `git add` 本计划涉及的文件，禁止 `git add .`。
- 本轮是视觉精修，不得回流三层导航、页内假 Tab、右上角功能菜单或大块说明卡。
- 颜色只能继续使用现有 `tokens.css` 中的品牌橙、蓝灰正文、浅灰控制台背景体系，不新增页面私有主色。
- `设备台账` 与 `产品定义中心` 标题层级必须对齐；`链路追踪台`、`数据校验台` 继续保留真实页签，但页签更轻、更线性。
- 所有生产代码改动必须先由失败测试驱动；若某个视觉点无法通过行为测试直接表达，至少要先写 DOM/class 合约测试，再写实现。

### Task 1: 收口共享工作台基线与全局 token 轻量化

**Files:**
- Create: `spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchPanel.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/components/StandardTableToolbar.test.ts`
- Modify: `spring-boot-iot-ui/src/components/StandardWorkbenchPanel.vue`
- Modify: `spring-boot-iot-ui/src/components/StandardTableToolbar.vue`
- Modify: `spring-boot-iot-ui/src/styles/tokens.css`
- Modify: `spring-boot-iot-ui/src/styles/element-overrides.css`

- [ ] **Step 1: 先写共享工作台失败测试**

创建 `spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchPanel.test.ts`：

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'

const PanelCardStub = {
  name: 'PanelCard',
  template: '<section class="panel-card-stub"><slot name="header" /><slot /></section>'
}

describe('StandardWorkbenchPanel', () => {
  it('applies the refined minimal-shell classes by default', () => {
    const wrapper = mount(StandardWorkbenchPanel, {
      props: {
        title: '产品定义中心',
        titleVariant: 'section',
        showFilters: true,
        showToolbar: true
      },
      slots: {
        filters: '<div class="filters-slot">filters</div>',
        toolbar: '<div class="toolbar-slot">toolbar</div>',
        default: '<div class="body-slot">body</div>'
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub
        }
      }
    })

    expect(wrapper.classes()).toContain('standard-workbench-panel--minimal')
    expect(wrapper.find('.standard-workbench-panel__header').classes()).toContain('standard-workbench-panel__header--minimal')
    expect(wrapper.find('.standard-workbench-panel__body').classes()).toContain('standard-workbench-panel__body--minimal')
  })
})
```

创建 `spring-boot-iot-ui/src/__tests__/components/StandardTableToolbar.test.ts`：

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StandardTableToolbar from '@/components/StandardTableToolbar.vue'

describe('StandardTableToolbar', () => {
  it('uses the refined toolbar shell and keeps meta items in the quiet-info rail', () => {
    const wrapper = mount(StandardTableToolbar, {
      props: {
        metaItems: ['已选 2 项', '启用 18 个']
      },
      slots: {
        right: '<button type="button" class="toolbar-action">刷新列表</button>'
      }
    })

    expect(wrapper.classes()).toContain('standard-table-toolbar--minimal')
    expect(wrapper.find('.table-action-bar__left').classes()).toContain('standard-table-toolbar__meta-rail')
    expect(wrapper.findAll('.table-action-bar__meta')).toHaveLength(2)
    expect(wrapper.find('.toolbar-action').exists()).toBe(true)
  })
})
```

- [ ] **Step 2: 运行测试，确认当前红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/StandardWorkbenchPanel.test.ts src/__tests__/components/StandardTableToolbar.test.ts --run
```

Expected:

- `StandardWorkbenchPanel.test.ts` 失败，因为组件根节点和头部/body 还没有 `--minimal` 合约类。
- `StandardTableToolbar.test.ts` 失败，因为当前组件没有 `standard-table-toolbar--minimal` 与 `standard-table-toolbar__meta-rail`。

- [ ] **Step 3: 写最小共享实现与 token 调整**

在 `spring-boot-iot-ui/src/components/StandardWorkbenchPanel.vue` 把根节点和关键分区补成最小视觉基线：

```vue
<div class="standard-workbench-panel standard-workbench-panel--minimal ops-workbench standard-list-view">
  <PanelCard class="ops-hero-card ops-table-card standard-workbench-panel__card standard-workbench-panel__card--minimal">
```

```vue
<div v-if="hasHeaderContent" class="standard-workbench-panel__header standard-workbench-panel__header--minimal">
```

```vue
<div class="standard-workbench-panel__body standard-workbench-panel__body--minimal">
  <slot />
</div>
```

并补以下样式：

```css
.standard-workbench-panel--minimal .standard-workbench-panel__card {
  border-color: color-mix(in srgb, var(--panel-border) 92%, white);
  box-shadow: var(--shadow-card-soft);
}

.standard-workbench-panel__header--minimal {
  gap: 1.1rem;
}

.standard-workbench-panel__body--minimal {
  min-width: 0;
}
```

在 `spring-boot-iot-ui/src/components/StandardTableToolbar.vue` 把工具条根节点和左侧 meta rail 收口：

```vue
<div
  class="standard-table-toolbar standard-table-toolbar--minimal table-action-bar"
  :class="{
    'standard-table-toolbar--compact': compact
  }"
>
  <div v-if="hasLeft" class="table-action-bar__left standard-table-toolbar__meta-rail">
```

在 `spring-boot-iot-ui/src/styles/tokens.css` 把按钮和轻阴影调到更现代但不换色的密度：

```css
--button-height-md: 38px;
--shadow-card-soft: 0 4px 12px rgba(31, 50, 81, 0.05);
--shadow-card: 0 8px 22px rgba(31, 50, 81, 0.06);
```

在 `spring-boot-iot-ui/src/styles/element-overrides.css` 把输入框和表头轻量化：

```css
.el-input__wrapper,
.el-textarea__inner,
.el-select__wrapper {
  border-radius: 14px;
}

.el-table {
  --el-table-header-bg-color: color-mix(in srgb, var(--surface-soft) 72%, white);
}
```

- [ ] **Step 4: 重新运行测试，确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/StandardWorkbenchPanel.test.ts src/__tests__/components/StandardTableToolbar.test.ts --run
```

Expected:

- 2 个测试文件全部通过。

- [ ] **Step 5: 提交共享工作台基线**

```bash
git add \
  spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchPanel.test.ts \
  spring-boot-iot-ui/src/__tests__/components/StandardTableToolbar.test.ts \
  spring-boot-iot-ui/src/components/StandardWorkbenchPanel.vue \
  spring-boot-iot-ui/src/components/StandardTableToolbar.vue \
  spring-boot-iot-ui/src/styles/tokens.css \
  spring-boot-iot-ui/src/styles/element-overrides.css
git commit -m "feat: refine shared iot access workbench baseline"
```

### Task 2: 收口筛选头与真页签视觉语法

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardListFilterHeader.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/components/StandardListFilterHeader.vue`
- Modify: `spring-boot-iot-ui/src/components/iotAccess/IotAccessTabWorkspace.vue`

- [ ] **Step 1: 先扩展失败测试**

在 `spring-boot-iot-ui/src/__tests__/components/StandardListFilterHeader.test.ts` 追加：

```ts
it('marks the filter shell as the refined minimal header', async () => {
  const wrapper = mount(StandardListFilterHeader, {
    props: {
      model: {}
    },
    slots: {
      primary: createPrimaryFields(3),
      actions: '<button class="query-button" type="button">查询</button>'
    },
    global: {
      stubs: {
        ElForm: ElFormStub,
        ElCollapseTransition: ElCollapseTransitionStub,
        ElButton: ElButtonStub,
        StandardActionGroup: StandardActionGroupStub
      }
    }
  })

  await flushComponentTicks()

  expect(wrapper.classes()).toContain('standard-list-filter-header--minimal')
  expect(wrapper.find('.standard-list-filter-header__actions-row').classes()).toContain('standard-list-filter-header__actions-row--minimal')
})
```

把 `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts` 的第三个用例改成：

```ts
it('marks the tab rail as business-view navigation with the refined minimal shell', () => {
  const wrapper = mount(IotAccessTabWorkspace, {
    props: {
      items: [
        { key: 'asset', label: '资产底座' },
        { key: 'diagnostics', label: '诊断排障' }
      ],
      defaultKey: 'asset',
      syncQuery: false
    }
  })

  expect(wrapper.classes()).toContain('iot-access-tab-workspace--minimal')
  expect(wrapper.find('nav').attributes('aria-label')).toBe('业务视图切换')
  expect(wrapper.find('.iot-access-tab-workspace__tabs').classes()).toContain('iot-access-tab-workspace__tabs--minimal')
})
```

- [ ] **Step 2: 运行测试，确认当前红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/StandardListFilterHeader.test.ts src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts --run
```

Expected:

- `StandardListFilterHeader.test.ts` 失败，因为根节点和 actions row 还没有 `--minimal` 类。
- `IotAccessTabWorkspace.test.ts` 失败，因为组件根节点和 tabs 还没有最小视觉类。

- [ ] **Step 3: 写最小实现**

在 `spring-boot-iot-ui/src/components/StandardListFilterHeader.vue` 加 root 与 actions row 类：

```vue
<div class="standard-list-filter-header standard-list-filter-header--minimal">
```

```vue
<div class="standard-list-filter-header__actions-row standard-list-filter-header__actions-row--minimal">
```

并补样式：

```css
.standard-list-filter-header--minimal {
  gap: 0.1rem;
}

.standard-list-filter-header__row {
  gap: 12px 16px;
}

.standard-list-filter-header__actions-row--minimal {
  margin-top: 12px;
  gap: 8px 14px;
}
```

在 `spring-boot-iot-ui/src/components/iotAccess/IotAccessTabWorkspace.vue` 加 root 和 tabs 类：

```vue
<section class="iot-access-tab-workspace iot-access-tab-workspace--minimal">
  <nav class="iot-access-tab-workspace__tabs iot-access-tab-workspace__tabs--minimal" aria-label="业务视图切换">
```

并收紧页签样式：

```css
.iot-access-tab-workspace--minimal {
  gap: 1rem;
}

.iot-access-tab-workspace__tabs--minimal {
  border-bottom-color: color-mix(in srgb, var(--line-panel) 86%, white);
}

.iot-access-tab-workspace__tab {
  min-height: 2.8rem;
  padding: 0.75rem 1rem;
}
```

- [ ] **Step 4: 重新运行测试，确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/StandardListFilterHeader.test.ts src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts --run
```

Expected:

- 2 个测试文件全部通过。

- [ ] **Step 5: 提交筛选头与真页签收口**

```bash
git add \
  spring-boot-iot-ui/src/__tests__/components/StandardListFilterHeader.test.ts \
  spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts \
  spring-boot-iot-ui/src/components/StandardListFilterHeader.vue \
  spring-boot-iot-ui/src/components/iotAccess/IotAccessTabWorkspace.vue
git commit -m "feat: refine iot access filters and tab rails"
```

### Task 3: 对齐单主列表页的 2026 极简控制台基线

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`

- [ ] **Step 1: 先写页面合约失败测试**

在 `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts` 追加：

```ts
it('uses the refined minimal list-shell classes for the product center', async () => {
  mockPageProducts.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [sampleProductRow]
    }
  })

  const wrapper = shallowMount(ProductWorkbenchView, {
    global: {
      directives: {
        loading: () => undefined,
        permission: () => undefined
      },
      stubs: {
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardRowActions: StandardRowActionsStub,
        StandardActionLink: StandardActionLinkStub,
        StandardActionMenu: StandardActionMenuStub,
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardButton: StandardButtonStub,
        StandardInlineState: StandardInlineStateStub,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub
      }
    }
  })

  await flushPromises()
  await nextTick()

  expect(wrapper.classes()).toContain('product-asset-view--minimal')
})
```

在 `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts` 追加：

```ts
it('uses the refined minimal list-shell classes for the device ledger', async () => {
  mockPageDevices.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 0,
      pageNum: 1,
      pageSize: 10,
      records: []
    }
  })

  const wrapper = mountView()

  await flushPromises()
  await nextTick()

  expect(wrapper.classes()).toContain('device-asset-view--minimal')
})
```

在 `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts` 追加：

```ts
it('uses the refined minimal list-shell classes for the observability ledger', async () => {
  const wrapper = mountView()

  await flushPromises()
  await nextTick()

  expect(wrapper.classes()).toContain('audit-log-view--minimal')
})
```

- [ ] **Step 2: 运行页面测试，确认当前红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/DeviceWorkbenchView.test.ts src/__tests__/views/AuditLogView.test.ts --run
```

Expected:

- 三个测试文件失败，因为页面根节点还没有 `--minimal` 对齐类。

- [ ] **Step 3: 写最小页面实现**

在三个页面根节点分别增加合约类：

`spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`

```vue
<div class="page-stack product-asset-view product-asset-view--compact product-asset-view--minimal">
```

`spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`

```vue
<div class="page-stack device-asset-view device-asset-view--minimal">
```

`spring-boot-iot-ui/src/views/AuditLogView.vue`

```vue
<div class="page-stack audit-log-view audit-log-view--minimal">
```

并补对应页面样式，统一到更轻的表头、工具条和标题节奏：

```css
.product-asset-view--minimal :deep(.standard-workbench-panel__title),
.device-asset-view--minimal :deep(.standard-workbench-panel__title),
.audit-log-view--minimal :deep(.standard-workbench-panel__title) {
  letter-spacing: -0.02em;
}

.product-asset-view--minimal :deep(.table-action-bar__meta),
.device-asset-view--minimal :deep(.table-action-bar__meta),
.audit-log-view--minimal :deep(.table-action-bar__meta) {
  color: var(--text-caption);
}
```

- [ ] **Step 4: 重新运行页面测试，确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/DeviceWorkbenchView.test.ts src/__tests__/views/AuditLogView.test.ts --run
```

Expected:

- 三个测试文件全部通过。

- [ ] **Step 5: 提交单主列表页精修**

```bash
git add \
  spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts \
  spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts \
  spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts \
  spring-boot-iot-ui/src/views/ProductWorkbenchView.vue \
  spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue \
  spring-boot-iot-ui/src/views/AuditLogView.vue
git commit -m "feat: align iot access list pages to minimal console style"
```

### Task 4: 对齐真页签诊断页的 2026 极简控制台基线

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts`
- Modify: `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- Modify: `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue`

- [ ] **Step 1: 先写诊断页失败测试**

在 `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts` 追加：

```ts
it('uses the refined minimal diagnostic-shell classes', async () => {
  vi.mocked(getDeviceByCode).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      id: 1,
      deviceCode: 'demo-device-01',
      productKey: 'demo-product',
      protocolCode: 'mqtt-json'
    }
  } as never)

  const wrapper = mountView()
  await flushPromises()

  expect(wrapper.find('.reporting-view').classes()).toContain('reporting-view--minimal')
})
```

在 `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts` 追加：

```ts
it('uses the refined minimal diagnostic-shell classes for message trace', async () => {
  vi.mocked(messageApi.pageMessageTraceLogs).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 0,
      pageNum: 1,
      pageSize: 10,
      records: []
    }
  } as never)
  vi.mocked(messageApi.pageMessageTraceStats).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      recentHourCount: 0,
      recent24HourCount: 0,
      dispatchFailureCount: 0
    }
  } as never)

  const wrapper = mountView()
  await flushPromises()

  expect(wrapper.find('.message-trace-view').classes()).toContain('message-trace-view--minimal')
})
```

在 `spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts` 追加：

```ts
it('uses the refined minimal diagnostic-shell classes for file validation', async () => {
  const wrapper = mountView()
  await flushPromises()

  expect(wrapper.find('.file-payload-debug-view').classes()).toContain('file-payload-debug-view--minimal')
})
```

- [ ] **Step 2: 运行诊断页测试，确认当前红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/ReportWorkbenchView.test.ts src/__tests__/views/MessageTraceView.test.ts src/__tests__/views/FilePayloadDebugView.test.ts --run
```

Expected:

- 三个测试文件失败，因为页面根节点还没有 `--minimal` 合约类。

- [ ] **Step 3: 写最小诊断页实现**

在三个页面根节点分别增加：

`spring-boot-iot-ui/src/views/ReportWorkbenchView.vue`

```vue
<div class="page-stack reporting-view reporting-view--minimal ops-workbench">
```

`spring-boot-iot-ui/src/views/MessageTraceView.vue`

```vue
<div class="page-stack message-trace-view message-trace-view--minimal">
```

`spring-boot-iot-ui/src/views/FilePayloadDebugView.vue`

```vue
<div class="page-stack file-payload-debug-view file-payload-debug-view--minimal">
```

并分别补最小样式，让真页签、标题和双栏工作面板更轻、更统一：

```css
.reporting-view--minimal :deep(.reporting-surface__title),
.message-trace-view--minimal :deep(.standard-workbench-panel__title),
.file-payload-debug-view--minimal :deep(.standard-workbench-panel__title) {
  letter-spacing: -0.02em;
}

.message-trace-view--minimal :deep(.table-action-bar__meta),
.file-payload-debug-view--minimal :deep(.table-action-bar__meta) {
  color: var(--text-caption);
}
```

- [ ] **Step 4: 重新运行诊断页测试，确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/ReportWorkbenchView.test.ts src/__tests__/views/MessageTraceView.test.ts src/__tests__/views/FilePayloadDebugView.test.ts --run
```

Expected:

- 三个测试文件全部通过。

- [ ] **Step 5: 提交诊断页精修**

```bash
git add \
  spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts \
  spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts \
  spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts \
  spring-boot-iot-ui/src/views/ReportWorkbenchView.vue \
  spring-boot-iot-ui/src/views/MessageTraceView.vue \
  spring-boot-iot-ui/src/views/FilePayloadDebugView.vue
git commit -m "feat: align iot access diagnostic pages to minimal console style"
```

### Task 5: 文档同步与全量验证

**Files:**
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: 先写文档变更清单**

在 `docs/06-前端开发与CSS规范.md` 增补规则：

```md
- `接入智维` 二轮精修继续保持当前系统主配色不变；页面现代化优先通过标题层级、筛选密度、工具条轻重、表头亮度和操作区节奏实现，不为单页新增私有主题色。
- 单主列表页与真页签诊断页都必须复用同一套“静稳控制台”共享组件语法，不得因页面类型不同再次漂移出另一组标题字号和工具条风格。
```

在 `docs/15-前端优化与治理计划.md` 增补长期规则：

```md
- `接入智维` 六个核心页的第二轮精修已确定为“静稳控制台基线 + 少量现代 SaaS 留白”，继续保持品牌橙、蓝灰正文与浅灰背景，不回流三层导航、右上角功能菜单和大块说明卡。
```

在 `docs/08-变更记录与技术债清单.md` 记录本轮摘要：

```md
- 2026-03-28：`接入智维` 六个核心页在既有两层导航结构上完成第二轮视觉精修，继续保持现有系统主配色不变，通过共享工作台、筛选头、工具条和真页签收口到“静稳控制台”语法。
```

- [ ] **Step 2: 检查 `README.md` 与 `AGENTS.md` 是否需要修改**

Run:

```bash
git diff -- README.md AGENTS.md
```

Expected:

- 若这轮仅为视觉治理与长期规则沉淀，则无需改动这两个文件。
- 若没有改动，最终交付说明中明确写出“已检查，无需修改”。

- [ ] **Step 3: 运行全量前端验证**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/StandardWorkbenchPanel.test.ts src/__tests__/components/StandardTableToolbar.test.ts src/__tests__/components/StandardListFilterHeader.test.ts src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/DeviceWorkbenchView.test.ts src/__tests__/views/AuditLogView.test.ts src/__tests__/components/ReportWorkbenchView.test.ts src/__tests__/views/MessageTraceView.test.ts src/__tests__/views/FilePayloadDebugView.test.ts --run
npm run component:guard
npm run list:guard
npm run style:guard
npm run build
```

Expected:

- 目标组件与页面测试全部通过。
- 三个 guard 全部通过。
- `npm run build` 通过。

- [ ] **Step 4: 提交文档与最终验证结果**

```bash
git add \
  docs/06-前端开发与CSS规范.md \
  docs/08-变更记录与技术债清单.md \
  docs/15-前端优化与治理计划.md
git commit -m "docs: record iot access minimal refinement rules"
```
