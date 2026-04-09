# IoT Access Page Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按已批准的 A 方案，把 `接入智维` 页面统一收口为“面包屑/标题 + 真正有业务意义的页签 + 筛选区 + 单主内容区”，删除重复状态条、说明卡、假工作区和假入口。

**Architecture:** 继续复用现有 `StandardWorkbenchPanel`、`StandardListFilterHeader`、`StandardTableToolbar`、`StandardDetailDrawer`、`StandardFormDrawer` 等标准件，不新起第二套页面系统。`IotAccessPageShell` 只保留轻量页头，`IotAccessTabWorkspace` 只服务真实业务切换；`产品定义中心`、`设备资产中心`、`异常观测台` 改回单主列表页，`接入智维总览`、`链路验证中心`、`链路追踪台`、`数据校验台` 只保留真实页签。

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vitest, Vite, CSS variables, shared Standard* components

---

## File Structure

### Existing files to modify

- `spring-boot-iot-ui/src/components/iotAccess/IotAccessPageShell.vue`
- `spring-boot-iot-ui/src/components/iotAccess/IotAccessTabWorkspace.vue`
- `spring-boot-iot-ui/src/components/AccessErrorArchivePanel.vue`
- `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- `spring-boot-iot-ui/src/views/SectionLandingView.vue`
- `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- `spring-boot-iot-ui/src/views/AuditLogView.vue`
- `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue`
- `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessPageShell.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/SectionLandingView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`

### Existing files to delete

- `spring-boot-iot-ui/src/components/iotAccess/IotAccessResultSection.vue`
- `spring-boot-iot-ui/src/components/iotAccess/IotAccessFilterBar.vue`
- `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessResultSection.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessFilterBar.test.ts`

### New files to create

- `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessCleanup.test.ts`

### Review-only sync targets

- `README.md`
- `AGENTS.md`

## Implementation Notes

- 当前工作区已经有其他未提交改动；每次提交都只 `git add` 本计划列出的文件，禁止 `git add .`。
- `IotAccessPageShell` 需要继续保留面包屑、标题和右上角动作，但移除状态条和 `status` 槽位。
- `IotAccessTabWorkspace` 继续保留 query 同步能力，但视觉和语义要改成“业务页签”，不再表现为说明型 pill。
- `IotAccessResultSection` 与 `IotAccessFilterBar` 来自被否定的旧方案，实现时要一起清理，防止回流。
- `/audit-log` 的业务审计模式不能被误伤；本次只改 `/system-log` 对应的异常观测模式。
- `/message-trace` 仍然使用同一路由承接“链路追踪 / 失败归档”，不新建失败归档路由。
- `README.md` 与 `AGENTS.md` 需要在最后复核；若现有表述已足够，则明确记录“已检查，无需修改”。

### Task 1: 收紧 IoT Access 共享壳层并清理已废弃辅助组件

**Files:**
- Modify: `spring-boot-iot-ui/src/components/iotAccess/IotAccessPageShell.vue`
- Modify: `spring-boot-iot-ui/src/components/iotAccess/IotAccessTabWorkspace.vue`
- Create: `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessCleanup.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessPageShell.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts`
- Delete: `spring-boot-iot-ui/src/components/iotAccess/IotAccessResultSection.vue`
- Delete: `spring-boot-iot-ui/src/components/iotAccess/IotAccessFilterBar.vue`
- Delete: `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessResultSection.test.ts`
- Delete: `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessFilterBar.test.ts`

- [ ] **Step 1: 先写共享壳层和清理动作的失败测试**

在 `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessPageShell.test.ts` 把首个用例改成下面的版本：

```ts
it('renders breadcrumbs, title and actions without the deprecated summary strip', () => {
  const wrapper = mount(IotAccessPageShell, {
    props: {
      title: '产品定义中心',
      breadcrumbs: [
        { label: '接入智维', to: '/device-access' },
        { label: '产品定义中心' }
      ]
    },
    slots: {
      actions: '<button type="button" class="shell-action">新增产品</button>',
      status: '<span class="deprecated-status">来自异常观测台</span>'
    },
    global: {
      stubs: {
        RouterLink: RouterLinkStub
      }
    }
  });

  expect(wrapper.find('.iot-access-page-shell').exists()).toBe(true);
  expect(wrapper.find('.iot-access-page-shell__title').text()).toBe('产品定义中心');
  expect(wrapper.find('.shell-action').text()).toBe('新增产品');
  expect(wrapper.find('.iot-access-page-shell__status').exists()).toBe(false);
  expect(wrapper.text()).not.toContain('来自异常观测台');
});
```

在 `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts` 追加：

```ts
it('marks the tab rail as business-view navigation', () => {
  const wrapper = mount(IotAccessTabWorkspace, {
    props: {
      items: [
        { key: 'asset', label: '资产底座' },
        { key: 'diagnostics', label: '诊断排障' }
      ],
      defaultKey: 'asset',
      syncQuery: false
    }
  });

  expect(wrapper.find('nav').attributes('aria-label')).toBe('业务视图切换');
});
```

创建 `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessCleanup.test.ts`：

```ts
import { existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const root = resolve(import.meta.dirname, '../../..');

describe('iot access cleanup', () => {
  it('removes discarded result/filter helper components', () => {
    expect(existsSync(resolve(root, 'components/iotAccess/IotAccessResultSection.vue'))).toBe(false);
    expect(existsSync(resolve(root, 'components/iotAccess/IotAccessFilterBar.vue'))).toBe(false);
  });
});
```

- [ ] **Step 2: 运行测试确认当前为红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/iotAccess/IotAccessPageShell.test.ts src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts src/__tests__/components/iotAccess/IotAccessCleanup.test.ts --run
```

Expected:

- `IotAccessPageShell.test.ts` 失败，因为组件当前还会渲染 `status` 区。
- `IotAccessTabWorkspace.test.ts` 失败，因为 `aria-label` 还是 `工作区切换`。
- `IotAccessCleanup.test.ts` 失败，因为两个旧辅助组件仍然存在。

- [ ] **Step 3: 写最小共享实现并删除旧辅助组件**

在 `spring-boot-iot-ui/src/components/iotAccess/IotAccessPageShell.vue` 删除状态条块和 `status` prop，保留页头主体：

```vue
<template>
  <section class="iot-access-page-shell">
    <nav v-if="breadcrumbs.length" class="iot-access-page-shell__breadcrumbs" aria-label="页面层级">
      <template v-for="(item, index) in breadcrumbs" :key="`${item.label}-${index}`">
        <RouterLink
          v-if="item.to"
          :to="item.to"
          class="iot-access-page-shell__breadcrumb-item iot-access-page-shell__breadcrumb-item--link"
        >
          {{ item.label }}
        </RouterLink>
        <span
          v-else
          class="iot-access-page-shell__breadcrumb-item iot-access-page-shell__breadcrumb-item--current"
        >
          {{ item.label }}
        </span>
      </template>
    </nav>

    <div class="iot-access-page-shell__headline">
      <div class="iot-access-page-shell__copy">
        <h1 class="iot-access-page-shell__title">{{ title }}</h1>
      </div>
      <div v-if="$slots.actions" class="iot-access-page-shell__actions">
        <slot name="actions" />
      </div>
    </div>

    <div v-if="$slots.default" class="iot-access-page-shell__body">
      <slot />
    </div>
  </section>
</template>
```

把脚本改成：

```ts
withDefaults(
  defineProps<{
    title: string
    breadcrumbs?: IotAccessPageShellBreadcrumb[]
  }>(),
  {
    breadcrumbs: () => []
  }
)
```

在 `spring-boot-iot-ui/src/components/iotAccess/IotAccessTabWorkspace.vue` 把 `aria-label` 与 Tab 样式收紧到线性业务页签：

```vue
<nav class="iot-access-tab-workspace__tabs" aria-label="业务视图切换">
```

```css
.iot-access-tab-workspace__tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  border-bottom: 1px solid var(--shell-border);
}

.iot-access-tab-workspace__tab {
  border: none;
  border-bottom: 2px solid transparent;
  border-radius: 0;
  background: transparent;
  color: var(--text-secondary);
  min-height: 2.75rem;
  padding: 0.7rem 1rem;
  font-size: 0.92rem;
  font-weight: 600;
}

.iot-access-tab-workspace__tab--active {
  border-bottom-color: var(--brand);
  background: transparent;
  color: var(--brand);
  box-shadow: none;
}
```

然后删除这四个文件：

```bash
rm spring-boot-iot-ui/src/components/iotAccess/IotAccessResultSection.vue
rm spring-boot-iot-ui/src/components/iotAccess/IotAccessFilterBar.vue
rm spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessResultSection.test.ts
rm spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessFilterBar.test.ts
```

- [ ] **Step 4: 重新运行共享组件测试确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/iotAccess/IotAccessPageShell.test.ts src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts src/__tests__/components/iotAccess/IotAccessCleanup.test.ts --run
```

Expected:

- 3 个测试文件全部 PASS。

- [ ] **Step 5: 提交共享壳层收口**

Run:

```bash
git add \
  spring-boot-iot-ui/src/components/iotAccess/IotAccessPageShell.vue \
  spring-boot-iot-ui/src/components/iotAccess/IotAccessTabWorkspace.vue \
  spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessPageShell.test.ts \
  spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts \
  spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessCleanup.test.ts \
  spring-boot-iot-ui/src/components/iotAccess/IotAccessResultSection.vue \
  spring-boot-iot-ui/src/components/iotAccess/IotAccessFilterBar.vue \
  spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessResultSection.test.ts \
  spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessFilterBar.test.ts
git commit -m "refactor: tighten iot access chrome components"
```

### Task 2: 把接入智维总览改成“资产底座 / 诊断排障”单列表入口页

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- Modify: `spring-boot-iot-ui/src/views/SectionLandingView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/SectionLandingView.test.ts`

- [ ] **Step 1: 先写总览页失败测试**

在 `spring-boot-iot-ui/src/__tests__/views/SectionLandingView.test.ts` 把首个用例改成：

```ts
it('renders the iot access hub as two real business tabs with a single entry list', () => {
  const wrapper = mountView()

  expect(wrapper.find('.iot-access-page-shell').exists()).toBe(true)
  expect(wrapper.find('.iot-access-page-shell__status').exists()).toBe(false)
  expect(wrapper.find('.iot-access-tab-workspace').exists()).toBe(true)
  expect(wrapper.text()).toContain('接入智维')
  expect(wrapper.text()).toContain('资产底座')
  expect(wrapper.text()).toContain('诊断排障')
  expect(wrapper.text()).toContain('产品定义中心')
  expect(wrapper.text()).toContain('设备资产中心')
  expect(wrapper.text()).not.toContain('推荐处理')
  expect(wrapper.text()).not.toContain('最近使用')
  expect(wrapper.text()).not.toContain('全部能力')
})
```

- [ ] **Step 2: 运行总览测试确认失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/SectionLandingView.test.ts --run
```

Expected:

- 断言失败，因为当前总览仍然渲染 `推荐处理 / 最近使用 / 全部能力` 三个页签和顶部状态文案。

- [ ] **Step 3: 以最小改动重写总览结构**

在 `spring-boot-iot-ui/src/views/SectionLandingView.vue` 把页签、筛选和主内容区改成下面的形态：

```vue
<IotAccessPageShell :title="config?.title || '接入智维'">
  <template #actions>
    <div v-if="introActions.length" class="section-landing__intro-actions">
      <RouterLink
        v-for="action in introActions"
        :key="`${action.label}-${action.to}`"
        :to="action.to"
        class="section-landing__intro-action"
        :class="action.variant === 'primary' ? 'section-landing__intro-action--primary' : ''"
      >
        {{ action.label }}
      </RouterLink>
    </div>
  </template>
</IotAccessPageShell>

<IotAccessTabWorkspace :items="landingTabs" default-key="asset">
  <template #default="{ activeKey }">
    <StandardWorkbenchPanel title="页面入口" description="先筛入口，再进入对应业务页。" show-filters>
      <template #filters>
        <StandardListFilterHeader :model="{ keyword: landingKeyword }">
          <template #primary>
            <el-form-item>
              <el-input
                v-model="landingKeyword"
                placeholder="搜索页面名称或职责关键词"
                clearable
                prefix-icon="Search"
              />
            </el-form-item>
          </template>
        </StandardListFilterHeader>
      </template>

      <div class="section-landing__entry-list">
        <RouterLink
          v-for="card in groupedCards[activeKey]"
          :key="card.path"
          :to="card.path"
          class="section-landing__entry-item"
        >
          <strong>{{ card.label }}</strong>
          <span>{{ card.description }}</span>
        </RouterLink>
      </div>
    </StandardWorkbenchPanel>
  </template>
</IotAccessTabWorkspace>
```

把脚本中的页签和分组逻辑改成：

```ts
const landingTabs = [
  { key: 'asset', label: '资产底座' },
  { key: 'diagnostics', label: '诊断排障' }
]

const landingKeyword = ref('')
const assetPaths = new Set(['/products', '/devices'])

const filteredCards = computed(() => {
  const keyword = landingKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return accessibleCards.value
  }
  return accessibleCards.value.filter((card) =>
    [card.label, card.description, ...(card.keywords || [])]
      .join(' ')
      .toLowerCase()
      .includes(keyword)
  )
})

const groupedCards = computed(() => ({
  asset: filteredCards.value.filter((card) => assetPaths.has(card.path)),
  diagnostics: filteredCards.value.filter((card) => !assetPaths.has(card.path))
}))
```

并把 `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts` 中 `iot-access` 文案收紧为：

```ts
description: '接入智维总览只负责入口分组和快速判断，不再重复子页说明墙。',
intro: '这里回答“先去哪处理”，子页只回答“在这个域里怎么做”。',
hubJudgement: '先进入资产底座，再切换到诊断排障。',
menuHint: '覆盖产品定义、设备资产、链路验证、异常观测、链路追踪与数据校验。'
```

- [ ] **Step 4: 重新运行总览测试确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/SectionLandingView.test.ts --run
```

Expected:

- `SectionLandingView.test.ts` PASS，且页面不再出现旧三页签与说明卡文案。

- [ ] **Step 5: 提交总览页收口**

Run:

```bash
git add \
  spring-boot-iot-ui/src/utils/sectionWorkspaces.ts \
  spring-boot-iot-ui/src/views/SectionLandingView.vue \
  spring-boot-iot-ui/src/__tests__/views/SectionLandingView.test.ts
git commit -m "refactor: simplify iot access hub layout"
```

### Task 3: 把产品定义中心收回单主列表页

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 1: 先写产品页失败测试**

在 `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts` 把“compact product workbench header”用例改成：

```ts
it('renders the product page as a single-ledger workbench without support tabs', async () => {
  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  expect(wrapper.find('.iot-access-page-shell').exists()).toBe(true)
  expect(wrapper.find('.iot-access-page-shell__status').exists()).toBe(false)
  expect(wrapper.find('.iot-access-tab-workspace').exists()).toBe(false)
  expect(wrapper.text()).toContain('产品定义中心')
  expect(wrapper.text()).toContain('新增产品')
  expect(wrapper.text()).not.toContain('物模型治理')
  expect(wrapper.text()).not.toContain('关联设备')
  expect(wrapper.text()).not.toContain('先补齐产品契约，再处理库存治理。')
})
```

- [ ] **Step 2: 运行产品页测试确认失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected:

- 断言失败，因为当前产品页仍有页签、顶部状态和两个支持区块。

- [ ] **Step 3: 移除页签/支持区，只保留筛选区和主列表**

把 `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue` 顶部结构替换成：

```vue
<div class="page-stack product-asset-view">
  <IotAccessPageShell title="产品定义中心" />

  <StandardWorkbenchPanel
    title="产品定义中心"
    description=""
    show-filters
    :show-applied-filters="hasAppliedFilters"
    show-toolbar
    :show-inline-state="showListInlineState"
    show-pagination
  >
```

删除模板顶部整段 `IotAccessTabWorkspace` 包裹块，也就是从：

```vue
<IotAccessTabWorkspace
```

开始，到：

```vue
</IotAccessTabWorkspace>
```

结束的整段支持区模板。

删除这些脚本定义和 import：

```ts
import IotAccessResultSection from '@/components/iotAccess/IotAccessResultSection.vue'
import IotAccessTabWorkspace from '@/components/iotAccess/IotAccessTabWorkspace.vue'

const productWorkspaceTabs = [
  { key: 'ledger', label: '产品台账' },
  { key: 'model', label: '物模型治理' },
  { key: 'devices', label: '关联设备' }
]

const productWorkspaceTab = ref('ledger')
```

同时删除这些只服务顶部支持区和状态条的计算字段：

- `productShellStatus`
- `currentProductLabel`
- `productModelWorkspaceHint`
- `currentProductAssociationSummary`
- `currentProductAssociationNextStep`

保留这些真实入口和台账能力：

- `workbenchInlineMessage`
- `workbenchInlineTone`
- `showListInlineState`
- `productRowActions`
- `handleOpenCurrentProductModelDesigner`
- `handleOpenCurrentProductDevices`

- [ ] **Step 4: 重新运行产品页测试确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected:

- `ProductWorkbenchView.test.ts` PASS。
- 现有物模型设计器和关联设备抽屉相关用例继续 PASS。

- [ ] **Step 5: 提交产品页收口**

Run:

```bash
git add \
  spring-boot-iot-ui/src/views/ProductWorkbenchView.vue \
  spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts
git commit -m "refactor: collapse product workbench to single ledger"
```

### Task 4: 把设备资产中心收回单主列表页

**Files:**
- Modify: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`

- [ ] **Step 1: 先写设备页失败测试**

在 `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts` 把“compact device workbench header”用例改成：

```ts
it('renders the device page as a single-ledger workbench without support tabs', async () => {
  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  expect(wrapper.find('.iot-access-page-shell').exists()).toBe(true)
  expect(wrapper.find('.iot-access-page-shell__status').exists()).toBe(false)
  expect(wrapper.find('.iot-access-tab-workspace').exists()).toBe(false)
  expect(wrapper.text()).toContain('设备资产中心')
  expect(wrapper.text()).toContain('新增设备')
  expect(wrapper.text()).toContain('批量导入')
  expect(wrapper.text()).not.toContain('未登记上报')
  expect(wrapper.text()).not.toContain('拓扑关系')
  expect(wrapper.text()).not.toContain('先判断在线、激活和拓扑异常，再进入设备治理。')
})
```

- [ ] **Step 2: 运行设备页测试确认失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/DeviceWorkbenchView.test.ts --run
```

Expected:

- 断言失败，因为当前设备页仍有页签、状态摘要和支持区块。

- [ ] **Step 3: 移除设备支持区和页签，保留登记状态/拓扑动作**

把 `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue` 顶部结构替换成：

```vue
<div class="page-stack device-asset-view">
  <IotAccessPageShell title="设备资产中心" />

  <StandardWorkbenchPanel
    title="设备资产中心"
    description=""
    show-filters
    :show-applied-filters="hasAppliedFilters"
    show-toolbar
    :show-inline-state="showListInlineState"
    show-pagination
  >
```

删除模板顶部整段 `IotAccessTabWorkspace` 包裹块，也就是从：

```vue
<IotAccessTabWorkspace
```

开始，到：

```vue
</IotAccessTabWorkspace>
```

结束的整段支持区模板。

删除这些脚本定义和 import：

```ts
import IotAccessResultSection from '@/components/iotAccess/IotAccessResultSection.vue'
import IotAccessTabWorkspace from '@/components/iotAccess/IotAccessTabWorkspace.vue'

const deviceWorkspaceTabs = [
  { key: 'ledger', label: '资产台账' },
  { key: 'unregistered', label: '未登记上报' },
  { key: 'topology', label: '拓扑关系' }
]

const deviceWorkspaceTab = ref('ledger')
```

保留这些真实台账能力：

- `searchForm.registrationStatus`
- `formatDeviceRelationValue`
- `openDetail`

并删除这些只服务顶部支持区和状态条的计算字段：

- `deviceShellStatus`
- `topologyLinkedCount`
- `gatewayLinkedCount`
- `unregisteredWorkspaceHint`
- `topologyWorkspaceHint`

这样未登记资产继续靠筛选承接，拓扑关系继续靠表格字段、详情抽屉和既有行操作承接。

- [ ] **Step 4: 重新运行设备页测试确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/DeviceWorkbenchView.test.ts --run
```

Expected:

- `DeviceWorkbenchView.test.ts` PASS。
- 诊断来源提示用例继续 PASS。

- [ ] **Step 5: 提交设备页收口**

Run:

```bash
git add \
  spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue \
  spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts
git commit -m "refactor: collapse device workbench to single ledger"
```

### Task 5: 保留链路验证中心和数据校验台的真页签，去掉状态条与伪页签

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts`

- [ ] **Step 1: 先写验证页和校验页失败测试**

在 `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts` 把首个用例改成：

```ts
it('keeps only real verification tabs and removes the top summary strip', () => {
  const wrapper = mountView();

  expect(wrapper.find('.iot-access-page-shell').exists()).toBe(true);
  expect(wrapper.find('.iot-access-page-shell__status').exists()).toBe(false);
  expect(wrapper.find('.iot-access-tab-workspace').exists()).toBe(true);
  expect(wrapper.text()).toContain('链路验证中心');
  expect(wrapper.text()).toContain('模拟验证');
  expect(wrapper.text()).toContain('结果复盘');
  expect(wrapper.text()).toContain('最近记录');
  expect(wrapper.text()).not.toContain('设备身份未校准');
});
```

在 `spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts` 把首个用例改成：

```ts
it('keeps validation and raw-response tabs only', () => {
  const wrapper = mount(FilePayloadDebugView, {
    global: {
      stubs: {
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: true,
        StandardInlineState: true,
        StandardInfoGrid: true,
        PanelCard: PanelCardStub,
        EmptyState: true,
        ResponsePanel: ResponsePanelStub,
        StandardButton: true,
        ElInput: true
      }
    }
  });

  expect(wrapper.find('.iot-access-page-shell').exists()).toBe(true);
  expect(wrapper.find('.iot-access-page-shell__status').exists()).toBe(false);
  expect(wrapper.find('.iot-access-tab-workspace').exists()).toBe(true);
  expect(wrapper.text()).toContain('设备校验');
  expect(wrapper.text()).toContain('原始响应');
  expect(wrapper.text()).not.toContain('历史快照');
});
```

- [ ] **Step 2: 运行两组测试确认失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/ReportWorkbenchView.test.ts src/__tests__/views/FilePayloadDebugView.test.ts --run
```

Expected:

- `ReportWorkbenchView.test.ts` 失败，因为页头还带状态条。
- `FilePayloadDebugView.test.ts` 失败，因为还有 `历史快照` 页签和状态条。

- [ ] **Step 3: 移除状态条并收紧真实页签**

在 `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue` 把页头改成：

```vue
<IotAccessPageShell title="链路验证中心">
  <template #actions>
    <StandardActionGroup gap="sm">
      <StandardButton v-if="canContinueTrace" action="refresh" plain @click="jumpToMessageTrace">
        继续链路追踪
      </StandardButton>
      <StandardButton v-if="canViewSystemLog" action="refresh" plain @click="jumpToSystemLog">
        查看异常观测
      </StandardButton>
      <StandardButton v-if="canOpenFileDebug" action="refresh" plain @click="jumpToFileDebug">
        打开数据校验
      </StandardButton>
    </StandardActionGroup>
  </template>
</IotAccessPageShell>
```

并删除 `:status="reportingStripStatus"` 与下面这个只服务顶部状态条的计算属性：

```ts
const reportingStripStatus = computed(() => {
  const deviceLabel = currentDiagnosticContext.value.deviceCode || '未查询';
  return `${currentDiagnosticFinding.value.title} · ${currentDiagnosticFinding.value.summary}（设备 ${deviceLabel}）`;
});
```

在 `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue` 把页头和页签改成：

```vue
<IotAccessPageShell title="数据校验台">
  <template #actions>
    <StandardButton action="refresh" plain @click="handleOpenTraceWorkbench">链路追踪台</StandardButton>
  </template>
</IotAccessPageShell>
```

```ts
const validationTabs = [
  { key: 'validate', label: '设备校验' },
  { key: 'raw-response', label: '原始响应' }
];
```

并删除 `validationStripStatus` 计算属性。

- [ ] **Step 4: 重新运行验证页/校验页测试确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/ReportWorkbenchView.test.ts src/__tests__/views/FilePayloadDebugView.test.ts --run
```

Expected:

- 两个测试文件 PASS。

- [ ] **Step 5: 提交验证页与校验页收口**

Run:

```bash
git add \
  spring-boot-iot-ui/src/views/ReportWorkbenchView.vue \
  spring-boot-iot-ui/src/views/FilePayloadDebugView.vue \
  spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts \
  spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts
git commit -m "refactor: keep only real tabs in iot diagnostics pages"
```

### Task 6: 收紧链路追踪台与异常观测台，只保留真实业务视图

**Files:**
- Modify: `spring-boot-iot-ui/src/components/AccessErrorArchivePanel.vue`
- Modify: `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: 先写追踪台和异常观测台失败测试**

在 `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts` 把首个结构用例改成：

```ts
it('keeps only trace and archive as real message-trace tabs', async () => {
  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  expect(wrapper.find('.iot-access-page-shell').exists()).toBe(true);
  expect(wrapper.find('.iot-access-page-shell__status').exists()).toBe(false);
  expect(wrapper.find('.iot-access-tab-workspace').exists()).toBe(true);
  expect(wrapper.text()).toContain('链路追踪台');
  expect(wrapper.text()).toContain('链路追踪');
  expect(wrapper.text()).toContain('失败归档');
  expect(wrapper.text()).not.toContain('时间线复盘');
  expect(wrapper.text()).not.toContain('运维看板');
  expect(wrapper.text()).not.toContain('最近会话');
});
```

在 `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts` 把系统模式结构用例改成：

```ts
it('keeps system mode list-first without summary tabs or strip copy', async () => {
  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  expect(wrapper.find('.iot-access-page-shell').exists()).toBe(true);
  expect(wrapper.find('.iot-access-page-shell__status').exists()).toBe(false);
  expect(wrapper.find('.iot-access-tab-workspace').exists()).toBe(false);
  expect(wrapper.text()).toContain('异常观测台');
  expect(wrapper.text()).toContain('链路追踪台');
  expect(wrapper.text()).toContain('失败归档');
  expect(wrapper.text()).not.toContain('聚合视图');
  expect(wrapper.text()).not.toContain('回跳治理');
});
```

- [ ] **Step 2: 运行两组测试确认失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/MessageTraceView.test.ts src/__tests__/views/AuditLogView.test.ts --run
```

Expected:

- `MessageTraceView.test.ts` 失败，因为页面还有状态条、工作区页签和支持区块。
- `AuditLogView.test.ts` 失败，因为系统模式还有状态条和三个伪页签。

- [ ] **Step 3: 把链路追踪台改成“顶部真页签 + 单主区”，把异常观测台改成单列表**

在 `spring-boot-iot-ui/src/components/AccessErrorArchivePanel.vue` 给顶部模式切换加开关，避免嵌入追踪台后重复出现：

```vue
<StandardWorkbenchPanel
  title="接入失败归档台"
  description="查看 MQTT / $dp 接入失败归档、契约快照与原始报文，快速回放失败上下文。"
  :show-header-actions="showModeSwitch"
  show-filters
  :show-applied-filters="hasAppliedFilters"
  show-notices
  show-toolbar
  show-pagination
>
  <template v-if="showModeSwitch" #header-actions>
    <StandardChoiceGroup
      :model-value="viewMode"
      :options="viewModeOptions"
      responsive
      @update:modelValue="handleModeChange"
    />
  </template>
```

```ts
const props = withDefaults(
  defineProps<{
    viewMode: ObservabilityViewMode;
    viewModeOptions: ViewModeOption[];
    showModeSwitch?: boolean;
  }>(),
  {
    showModeSwitch: true
  }
);
```

在 `spring-boot-iot-ui/src/views/MessageTraceView.vue` 把顶层结构改成：

```vue
<IotAccessPageShell title="链路追踪台">
  <template #actions>
    <StandardButton action="refresh" plain :disabled="!canJumpWithSearch" @click="jumpToSystemLog()">
      异常观测台
    </StandardButton>
    <StandardButton action="refresh" plain :disabled="!canJumpToFileDebug" @click="jumpToFileDebug()">
      数据校验台
    </StandardButton>
  </template>
</IotAccessPageShell>

<IotAccessTabWorkspace
  v-model="pageModeValue"
  :items="messageTraceViewTabs"
  default-key="message-trace"
  query-key="mode"
>
  <template #default="{ activeKey }">
    <AccessErrorArchivePanel
      v-if="activeKey === 'access-error'"
      :view-mode="pageModeValue"
      :view-mode-options="pageModeOptions"
      :show-mode-switch="false"
      @change-view-mode="handlePageModeChange"
    />

    <div v-else class="message-trace-ledger-surface">
      <StandardWorkbenchPanel
        title="追踪台账"
        description="按 TraceId、设备编码、产品标识与 Topic 串联同一条接入链路。"
        show-filters
        :show-applied-filters="hasAppliedFilters"
        show-toolbar
        show-pagination
      >
```

脚本中把页签源改成：

```ts
const messageTraceViewTabs = [
  { key: 'message-trace', label: '链路追踪' },
  { key: 'access-error', label: '失败归档' }
];

const pageModeValue = computed<ObservabilityViewMode>({
  get: () => (route.query.mode === 'access-error' ? 'access-error' : 'message-trace'),
  set: (value) => {
    void handlePageModeChange(value);
  }
});
```

并删除原来的这些定义与 import：

- `import IotAccessResultSection from '@/components/iotAccess/IotAccessResultSection.vue'`
- `messageTraceWorkspaceTabs`
- `messageTraceWorkspaceTab`
- `messageTraceShellStatus`
- `currentArchiveTraceId`
- `currentArchiveDeviceCode`
- `currentArchiveProductKey`
- `timelineWorkspaceStatus`

同时删除结果区底部整段“运维看板 / 最近会话”支持块。

在 `spring-boot-iot-ui/src/views/AuditLogView.vue` 删掉系统模式下的 `IotAccessTabWorkspace` 和两个 `IotAccessResultSection`，保留：

```vue
<IotAccessPageShell v-if="isSystemMode" title="异常观测台">
  <template #actions>
    <StandardButton action="refresh" plain @click="handleJumpToMessageTrace()">链路追踪台</StandardButton>
    <StandardButton action="reset" plain @click="handleJumpToAccessError()">失败归档</StandardButton>
  </template>
</IotAccessPageShell>
```

同时删除 `:status="systemStripStatus"`、`systemWorkspaceTabs`、`systemWorkspaceTab` 和 `IotAccessResultSection` / `IotAccessTabWorkspace` imports，并删除下面两个只服务顶部状态条和支持区的计算字段：

- `systemFindingSummary`
- `systemStripStatus`

- [ ] **Step 4: 重新运行两组测试确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/MessageTraceView.test.ts src/__tests__/views/AuditLogView.test.ts --run
```

Expected:

- 两个测试文件 PASS。
- `MessageTraceView` 仍保留详情抽屉和失败归档跳转能力。
- `AuditLogView` 的 `/audit-log` 业务模式用例继续 PASS。

- [ ] **Step 5: 提交追踪台和异常观测台收口**

Run:

```bash
git add \
  spring-boot-iot-ui/src/components/AccessErrorArchivePanel.vue \
  spring-boot-iot-ui/src/views/MessageTraceView.vue \
  spring-boot-iot-ui/src/views/AuditLogView.vue \
  spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts \
  spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "refactor: simplify trace and anomaly workbenches"
```

### Task 7: 同步治理文档、复核 README/AGENTS，并跑完整验证

**Files:**
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: 先更新前端治理规则**

在 `docs/15-前端优化与治理计划.md` 中按下面方式原位更新规则，避免和旧方案并存：

```md
24. `链路追踪台`、`异常观测台`、`审计中心` 这类日志 / 诊断台账，统计摘要优先放在 `StandardTableToolbar.meta-items`、分页邻近区域或详情区；不要再回退到页面顶部紧凑状态条和私有概况卡。
30. `接入智维总览` 只保留 `资产底座 / 诊断排障` 两类真实入口，不再展示 `推荐处理 / 最近使用 / 全部能力` 三页签和说明卡。
31. `链路验证中心`、`异常观测台`、`链路追踪台`、`数据校验台` 的顶部统一只保留面包屑、标题和强相关跳转；如果状态已经在工具栏、分页或结果区表达，就不要再加顶部状态条。
34. `接入智维` 子页只在存在两个及以上真实业务视图时复用 `IotAccessTabWorkspace`；`IotAccessResultSection`、`IotAccessFilterBar` 不再作为正文结构组件回流。
36. `接入智维` 页面统一采用“面包屑/标题 + 真页签 + 筛选区 + 单主内容区”；`/products`、`/devices`、`/system-log` 这类单主业务页默认不保留页签。
```

- [ ] **Step 2: 更新变更记录并复核 README/AGENTS**

在 `docs/08-变更记录与技术债清单.md` 的“当前有效变更摘要”追加：

```md
- 2026-03-28：`接入智维` 页面结构已按“面包屑/标题 + 真页签 + 筛选区 + 单主内容区”统一收口。`/device-access` 改为 `资产底座 / 诊断排障` 双页签入口；`/products`、`/devices`、`/system-log` 去掉重复状态条和说明工作区；`/message-trace` 只保留 `链路追踪 / 失败归档`；`/file-debug` 删除 `历史快照` 页签。
```

然后人工检查：

```bash
sed -n '1,120p' README.md
sed -n '1,160p' AGENTS.md
```

Expected:

- 两份文件现有规则已覆盖“前端复用共享骨架、不要重复造列表与分页”的要求，无需新增文本。

- [ ] **Step 3: 跑接入智维收口后的完整验证**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- \
  src/__tests__/components/iotAccess/IotAccessPageShell.test.ts \
  src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts \
  src/__tests__/components/iotAccess/IotAccessCleanup.test.ts \
  src/__tests__/views/SectionLandingView.test.ts \
  src/__tests__/views/ProductWorkbenchView.test.ts \
  src/__tests__/views/DeviceWorkbenchView.test.ts \
  src/__tests__/components/ReportWorkbenchView.test.ts \
  src/__tests__/views/MessageTraceView.test.ts \
  src/__tests__/views/AuditLogView.test.ts \
  src/__tests__/views/FilePayloadDebugView.test.ts \
  --run
npm run build
npm run component:guard
npm run list:guard
npm run style:guard
cd ..
node scripts/docs/check-topology.mjs
```

Expected:

- 上述 10 个 Vitest 文件全部 PASS。
- `vite build` PASS。
- `component:guard`、`list:guard`、`style:guard` PASS。
- `docs` 拓扑校验 PASS。

- [ ] **Step 4: 提交文档和总验证结果**

Run:

```bash
git add \
  docs/08-变更记录与技术债清单.md \
  docs/15-前端优化与治理计划.md
git commit -m "docs: record iot access page consolidation rules"
```

## Self-Review Checklist

- 规格覆盖：
  - `接入智维总览` 双业务页签：Task 2
  - `产品定义中心` 单主列表页：Task 3
  - `设备资产中心` 单主列表页：Task 4
  - `链路验证中心` 真页签保留：Task 5
  - `链路追踪台` 仅保留追踪/归档真页签：Task 6
  - `异常观测台` 单主列表页：Task 6
  - `数据校验台` 删除 `历史快照` 页签：Task 5
  - 删除状态条、说明卡、假工作区：Task 1 至 Task 6
  - 文档同步：Task 7
- 占位词检查：本计划不含 `TODO`、`TBD`、`implement later`。
- 类型一致性：
  - `MessageTraceView` 使用 `message-trace` / `access-error`
  - `SectionLandingView` 使用 `asset` / `diagnostics`
  - `FilePayloadDebugView` 使用 `validate` / `raw-response`
