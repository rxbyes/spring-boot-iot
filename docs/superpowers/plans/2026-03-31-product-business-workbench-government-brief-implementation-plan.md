# Product Business Workbench Government Brief Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把产品经营工作台从“已扁平化”继续升级为“首长简报式”的政企经营工作台，提升标题气场、章节秩序和整体观感，同时保持现有交互与功能不回退。

**Architecture:** 继续以 `ProductBusinessWorkbenchDrawer.vue` 为统一壳层，不改变 `/products` 页面工作台入口、状态机或后端接口。围绕五个产品工作台组件调整 DOM 结构、标题层级、信息分区和样式节奏，并先通过组件测试锁定新的结构锚点，再落地实现和文档同步。

**Tech Stack:** Vue 3、TypeScript、Element Plus、Vitest、scoped CSS、共享 `Standard*` 组件

**Execution Note:** 仓库 `AGENTS.md` 明确要求编码、验证与交付准备默认只能在 `codex/dev` 上进行。本计划执行时不新开功能分支 worktree，而是在当前 `codex/dev` 工作区只修改本计划列出的目标文件，并显式避开现有未提交的其他改动。

---

## File Structure

### Existing files to modify

- `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
- `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
- `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`
- `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts`
- `docs/06-前端开发与CSS规范.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`

### Existing files to review-only

- `README.md`
- `AGENTS.md`

## Task 1: 锁定统一工作台头部的首长简报式锚点

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`

- [ ] **Step 1: 先写失败测试，锁定新头部锚点**

在 `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts` 中补充断言：

```ts
expect(wrapper.find('.product-business-workbench__header-kicker').text()).toContain('产品经营工作台')
expect(wrapper.find('.product-business-workbench__headline').text()).toContain('演示产品')
expect(wrapper.find('.product-business-workbench__brief').text()).toContain('当前产品')
expect(wrapper.find('.product-business-workbench__tab-rail').exists()).toBe(true)
expect(wrapper.find('.product-business-workbench__meta-strip').exists()).toBe(true)
```

- [ ] **Step 2: 运行测试确认红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts --run
```

Expected:

- FAIL，当前组件还没有新的 `header-kicker / headline / brief / tab-rail / meta-strip`。

- [ ] **Step 3: 写最小实现让测试转绿**

在 `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue` 中：

- 把头部文案层级调整为 `header-kicker + headline + brief`
- 把 chips 区升级为更稳定的 `meta-strip`
- 把 tabs 区升级为更完整的 `tab-rail`
- 强化留白、边框、标题大小和状态 badge 的简报感

- [ ] **Step 4: 重跑测试确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts --run
```

Expected:

- PASS

## Task 2: 锁定经营总览页的“首长简报式”章节结构

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`

- [ ] **Step 1: 先写失败测试，锁定章节锚点**

在 `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts` 中补充断言：

```ts
expect(wrapper.find('[data-testid="product-detail-hero-stage-kicker"]').text()).toContain('经营主判断')
expect(wrapper.find('[data-testid="product-detail-primary-statement"]').exists()).toBe(true)
expect(wrapper.find('[data-testid="product-detail-judgement-summary"]').exists()).toBe(true)
expect(wrapper.find('[data-testid="product-detail-contract-summary"]').exists()).toBe(true)
expect(wrapper.find('[data-testid="product-detail-governance-summary"]').exists()).toBe(true)
```

- [ ] **Step 2: 运行测试确认红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts --run
```

Expected:

- FAIL，当前总览页尚未暴露新的简报式章节锚点。

- [ ] **Step 3: 写最小实现让测试转绿**

在 `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue` 中：

- 为主舞台增加章节 kicker 和更强主结论
- 把判断板块、契约档案板块、治理板块做成明确章节段落
- 强化主舞台与辅助指标的主次压差
- 统一章节头与节内内容的节奏

- [ ] **Step 4: 重跑测试确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts --run
```

Expected:

- PASS

## Task 3: 锁定物模型页的简报式治理骨架

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`

- [ ] **Step 1: 先写失败测试，锁定新锚点**

在 `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts` 中补充断言：

```ts
expect(wrapper.find('.product-model-designer__header-kicker').text()).toContain('物模型治理')
expect(wrapper.find('.product-model-designer__headline').exists()).toBe(true)
expect(wrapper.find('.product-model-designer__summary-lead').exists()).toBe(true)
expect(wrapper.find('.product-model-designer__workspace-rail').exists()).toBe(true)
expect(wrapper.find('.product-model-designer__workspace-main').exists()).toBe(true)
```

- [ ] **Step 2: 运行测试确认红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts --run
```

Expected:

- FAIL，当前组件没有新的 `header-kicker / headline / summary-lead / workspace-rail / workspace-main`。

- [ ] **Step 3: 写最小实现让测试转绿**

在 `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue` 中：

- 把头部改成更像正式章节页眉
- 让摘要条中的 lead 项更像“治理导语”
- 明确左右轨道和主正文区的角色
- 候选与正式模式继续共用同一套页面语法

- [ ] **Step 4: 重跑测试确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts --run
```

Expected:

- PASS

## Task 4: 锁定关联设备页的“先定调，再核账”

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`

- [ ] **Step 1: 先写失败测试，锁定新结构**

在 `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts` 中补充断言：

```ts
expect(wrapper.find('.device-workspace__section-kicker').text()).toContain('设备运行概览')
expect(wrapper.find('.device-workspace__metric-band').exists()).toBe(true)
expect(wrapper.find('.device-workspace__ledger-stage').exists()).toBe(true)
expect(wrapper.find('.device-workspace__ledger-heading').text()).toContain('关联设备台账')
```

- [ ] **Step 2: 运行测试确认红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDeviceListWorkspace.test.ts --run
```

Expected:

- FAIL，当前组件还没有新的 `section-kicker / metric-band / ledger-stage / ledger-heading`。

- [ ] **Step 3: 写最小实现让测试转绿**

在 `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue` 中：

- 把指标区域升级为统一横带
- 表格区升级为稳定的台账主区
- 让表格标题区成为真正章节头
- 强化简报式秩序，不增加装饰噪音

- [ ] **Step 4: 重跑测试确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDeviceListWorkspace.test.ts --run
```

Expected:

- PASS

## Task 5: 锁定编辑页的“变更申请页”气质

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`

- [ ] **Step 1: 先写失败测试，锁定新结构**

在 `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts` 中补充断言：

```ts
expect(wrapper.find('.product-edit-workspace__section-kicker').text()).toContain('编辑治理')
expect(wrapper.find('.product-edit-workspace__headline').exists()).toBe(true)
expect(wrapper.find('.product-edit-workspace__context-strip').exists()).toBe(true)
expect(wrapper.find('.product-edit-workspace__form-stage').exists()).toBe(true)
```

- [ ] **Step 2: 运行测试确认红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductEditWorkspace.test.ts --run
```

Expected:

- FAIL，当前编辑页还没有新的简报式章节锚点。

- [ ] **Step 3: 写最小实现让测试转绿**

在 `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue` 中：

- 把顶部摘要区升级为“章节头 + 变更上下文条”
- 给表单加上更明确的主区包裹层
- 保留 `StandardInlineState` 但让其成为章节中的治理提示，不漂浮

- [ ] **Step 4: 重跑测试确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductEditWorkspace.test.ts --run
```

Expected:

- PASS

## Task 6: 同步文档并执行完整验证

**Files:**
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: 更新文档中的设计规则**

在三份文档中补充：

- 产品经营工作台第二轮 `首长简报式` 规范
- 统一工作台头部与四个内页的章节主次规则
- 禁止回流成厚 hero、说明墙或普通卡片拼装页

- [ ] **Step 2: 运行聚合测试**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts src/__tests__/components/product/ProductDetailWorkbench.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductDeviceListWorkspace.test.ts src/__tests__/components/product/ProductEditWorkspace.test.ts src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected:

- 全部 PASS

- [ ] **Step 3: 运行共享守卫**

Run:

```bash
cd spring-boot-iot-ui
npm run component:guard
npm run list:guard
npm run style:guard
```

Expected:

- 三个守卫全部 PASS

- [ ] **Step 4: 运行正式构建**

Run:

```bash
cd spring-boot-iot-ui
npm run build
```

Expected:

- Build PASS

- [ ] **Step 5: 复核 README / AGENTS**

检查 `README.md` 与 `AGENTS.md` 是否需要同步。若无实际内容变化需求，则在交付说明中明确“已检查，无需更新”。
