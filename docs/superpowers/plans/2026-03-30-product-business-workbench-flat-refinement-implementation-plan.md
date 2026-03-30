# Product Business Workbench Flat Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `/products` 的统一产品经营工作台四个内页升级为扁平化、统一语法、符合验收标准的工作台体验，并保持现有行为与回写逻辑不回退。

**Architecture:** 继续以 `ProductBusinessWorkbenchDrawer.vue` 作为统一容器，不改变 `/products` 的路由和对象级状态机。重点改动统一工作台壳层、`经营总览 / 物模型治理 / 关联设备 / 编辑治理` 四个嵌入式 workspace 的结构与样式，同时用组件测试锁定新的扁平化 DOM 结构、信息层级和关键交互。

**Tech Stack:** Vue 3、TypeScript、Element Plus、Vitest、共享 `Standard*` 组件、scoped CSS

---

## File Structure

### Existing files to modify

- `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
- `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
- `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`
- `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts`
- `docs/06-前端开发与CSS规范.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`

### Review-only sync targets

- `README.md`
- `AGENTS.md`

## Task 1: 锁定统一工作台壳层的扁平化结构

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`

- [ ] **Step 1: 先写失败测试，锁定新壳层结构**

在 `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts` 中把断言改为新的扁平化骨架：

```ts
expect(wrapper.find('.product-business-workbench__hero').exists()).toBe(false)
expect(wrapper.find('.product-business-workbench__header').exists()).toBe(true)
expect(wrapper.find('.product-business-workbench__identity').text()).toContain('演示产品')
expect(wrapper.find('.product-business-workbench__identity').text()).toContain('demo-product')
expect(wrapper.find('.product-business-workbench__status-badge').text()).toContain('稳定使用中')
expect(wrapper.find('.product-business-workbench__meta').text()).toContain('mqtt-json')
expect(wrapper.find('.product-business-workbench__tabs').exists()).toBe(true)
expect(wrapper.find('.product-business-workbench__view-shell').exists()).toBe(true)
```

- [ ] **Step 2: 运行单测确认红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts --run
```

Expected:

- 失败，原因是当前组件仍使用旧的 `hero + metrics` 结构，没有新的 `header / identity / status-badge / view-shell`。

- [ ] **Step 3: 写最小实现让壳层测试转绿**

在 `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue` 中：

- 把旧 `.product-business-workbench__hero*` 结构改为：
  - `.product-business-workbench__header`
  - `.product-business-workbench__identity`
  - `.product-business-workbench__status-badge`
  - `.product-business-workbench__meta`
  - `.product-business-workbench__tabs`
  - `.product-business-workbench__view-shell`
- 移除厚重 `hero` 渐变语法，改用轻描边白底
- 保留现有 `activeView` 和 slot 机制，不改数据接口

- [ ] **Step 4: 重跑单测确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts --run
```

Expected:

- PASS

## Task 2: 锁定经营总览的扁平化主舞台

**Files:**
- Create/Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`（若已有则修改；若无则新增）
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`

- [ ] **Step 1: 先写失败测试，锁定“一个主舞台 + 两组二级板块”的新结构**

测试至少断言：

```ts
expect(wrapper.find('[data-testid="product-detail-hero-stage"]').exists()).toBe(true)
expect(wrapper.find('[data-testid="product-detail-primary-metric"]').text()).toContain('关联设备总量')
expect(wrapper.find('[data-testid="product-detail-secondary-metrics"]').exists()).toBe(true)
expect(wrapper.find('[data-testid="product-detail-judgement-stage"]').exists()).toBe(true)
expect(wrapper.find('[data-testid="product-detail-contract-archive-stage"]').exists()).toBe(true)
expect(wrapper.findAll('[data-testid="product-detail-stage-title"]')).toHaveLength(4)
```

- [ ] **Step 2: 运行单测确认红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts --run
```

Expected:

- 失败，原因是当前 DOM 仍保留旧的 `detail-summary-grid + brief-grid + notice` 语法，没有新的测试锚点。

- [ ] **Step 3: 写最小实现让总览页结构通过**

在 `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue` 中：

- 重新组织为四层：
  - `product-detail-hero-stage`
  - `product-detail-judgement-stage`
  - `product-detail-contract-archive-stage`
  - `product-detail-governance-stage`
- 主舞台只保留一个主指标焦点
- 把 `当前判断 / 趋势判断 / 接入提示` 收为更短的判断板块
- 把契约与档案合并为同一层
- 压平背景、边框、说明文案长度

- [ ] **Step 4: 重跑单测确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts --run
```

Expected:

- PASS

## Task 3: 锁定物模型治理页的扁平目录结构

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`

- [ ] **Step 1: 先写失败测试，锁定新结构**

在 `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts` 中新增断言：

```ts
expect(wrapper.find('.product-model-designer__hero-stage').exists()).toBe(false)
expect(wrapper.find('.product-model-designer__header').exists()).toBe(true)
expect(wrapper.find('.product-model-designer__summary-strip').exists()).toBe(true)
expect(wrapper.find('.product-model-designer__workspace-shell').exists()).toBe(true)
expect(wrapper.find('.product-model-designer__candidate-nav').exists()).toBe(true)
```

- [ ] **Step 2: 运行单测确认红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts --run
```

Expected:

- 失败，原因是当前页面仍使用旧的 `hero-stage` 结构。

- [ ] **Step 3: 写最小实现让物模型页通过**

在 `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue` 中：

- 把旧 `hero-stage` 改为扁平 `header`
- 增加 `summary-strip`
- 用 `workspace-shell` 包住 `candidate-nav / candidate-body / candidate-rail`
- 保留候选提炼与正式模型两种模式
- 候选与正式视图共用更平的卡片和板式

- [ ] **Step 4: 重跑单测确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts --run
```

Expected:

- PASS

## Task 4: 锁定关联设备页的“指标带 + 台账主区”

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`

- [ ] **Step 1: 先写失败测试，锁定新结构**

在 `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts` 中新增断言：

```ts
expect(wrapper.find('.device-workspace__summary-band').exists()).toBe(true)
expect(wrapper.find('.device-workspace__table-stage').exists()).toBe(true)
expect(wrapper.find('.device-drawer__summary').exists()).toBe(false)
expect(wrapper.find('.device-workspace__section-copy').text()).toContain('关联设备台账')
```

- [ ] **Step 2: 运行单测确认红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDeviceListWorkspace.test.ts --run
```

Expected:

- 失败，原因是当前组件仍依赖旧 `device-drawer__summary` 结构。

- [ ] **Step 3: 写最小实现让设备页通过**

在 `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue` 中：

- 把首屏摘要改为紧凑 `summary-band`
- 保留 3 到 4 个指标，但显著压缩高度
- 表格区域改名并提升为 `table-stage`
- 操作列继续复用 `StandardRowActions` 与 `StandardActionLink`

- [ ] **Step 4: 重跑单测确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDeviceListWorkspace.test.ts --run
```

Expected:

- PASS

## Task 5: 锁定编辑治理页的“摘要条 + 内联提示 + 表单主区”

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`

- [ ] **Step 1: 先写失败测试，锁定新结构**

在 `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts` 中把断言改为：

```ts
expect(wrapper.find('.product-edit-workspace__hero').exists()).toBe(false)
expect(wrapper.find('.product-edit-workspace__summary-band').exists()).toBe(true)
expect(wrapper.find('.product-edit-workspace__notice').exists()).toBe(false)
expect(wrapper.find('.standard-inline-state-stub').exists()).toBe(true)
expect(wrapper.text()).toContain('基础档案')
expect(wrapper.text()).toContain('接入基线')
expect(wrapper.text()).toContain('补充说明')
```

- [ ] **Step 2: 运行单测确认红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductEditWorkspace.test.ts --run
```

Expected:

- 失败，原因是当前组件仍保留 `hero` 和 `编辑影响提示` 大卡结构。

- [ ] **Step 3: 写最小实现让编辑页通过**

在 `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue` 中：

- 用 `summary-band` 替换旧 hero
- 删除大块 `notice` 区，统一通过 `StandardInlineState` 表达治理提示
- 保留现有表单字段和底部动作
- 让表单更早进入主视觉层

- [ ] **Step 4: 重跑单测确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductEditWorkspace.test.ts --run
```

Expected:

- PASS

## Task 6: 跑组合验证并同步文档

**Files:**
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: 跑受影响组件测试**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- \
  src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts \
  src/__tests__/components/product/ProductDetailWorkbench.test.ts \
  src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  src/__tests__/components/product/ProductDeviceListWorkspace.test.ts \
  src/__tests__/components/product/ProductEditWorkspace.test.ts --run
```

Expected:

- 全部 PASS

- [ ] **Step 2: 跑页面回归与前端门禁**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
npm run component:guard
npm run list:guard
npm run style:guard
npm run build
```

Expected:

- 全部 PASS

- [ ] **Step 3: 同步文档**

文档至少补充：

- `docs/06-前端开发与CSS规范.md`
  - 补充产品经营工作台扁平化基线
- `docs/08-变更记录与技术债清单.md`
  - 记录 2026-03-30 的扁平化精修结果
- `docs/15-前端优化与治理计划.md`
  - 记录总骨架 A、设备/编辑页吸收 B 密度的治理规则

- [ ] **Step 4: 检查 `README.md` 和 `AGENTS.md` 是否需要同步**

预期：

- 本轮属于前端表现层精修，通常无需修改 `README.md` 与 `AGENTS.md`
- 若检查后无需修改，在最终交付说明中明确写出“已检查，无需更新”
