# Product Workbench Minimal Copy And Brand Unification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 继续收敛产品经营工作台，把详情页解释文案压到极简级别，并把关联设备页的指标区统一回主色体系。

**Architecture:** 仅修改 `ProductDetailWorkbench.vue` 和 `ProductDeviceListWorkspace.vue` 及其组件测试，不改变工作台容器、路由或数据来源。先通过测试锁定极简文案和主色统一的目标，再做样式与文案实现，最后同步治理文档。

**Tech Stack:** Vue 3、TypeScript、Vitest、scoped CSS、Element Plus

---

## File Structure

### Existing files to modify

- `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
- `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`
- `docs/06-前端开发与CSS规范.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`

## Task 1: 锁定详情页极简文案

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`

- [ ] **Step 1: 写失败测试**

在 `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts` 中调整断言：

```ts
expect(wrapper.text()).not.toContain('接入契约摘要')
expect(wrapper.text()).not.toContain('治理摘要')
expect(wrapper.get('[data-testid="product-detail-judgement-summary"]').text()).not.toContain('最近上报')
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts --run
```

Expected:

- FAIL，因为当前组件仍保留 `接入契约摘要 / 治理摘要` 和多层解释文案。

- [ ] **Step 3: 写最小实现**

在 `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue` 中：

- 删除 `接入契约摘要 / 治理摘要` 解释块
- 压缩 `趋势摘要 / 接入提示 / 当前建议` 的副文
- 让区块更接近“标题 + 一条结论”

- [ ] **Step 4: 重跑测试确认通过**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts --run
```

Expected:

- PASS

## Task 2: 锁定关联设备页主色统一

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`

- [ ] **Step 1: 写失败测试**

在 `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts` 中补充断言：

```ts
expect(wrapper.find('.device-workspace__metric[data-tone=\"brand\"]').exists()).toBe(true)
expect(wrapper.find('.device-workspace__metric[data-tone=\"success\"]').exists()).toBe(false)
expect(wrapper.find('.device-workspace__metric[data-tone=\"danger\"]').exists()).toBe(false)
expect(wrapper.find('.device-workspace__metric[data-tone=\"accent\"]').exists()).toBe(false)
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDeviceListWorkspace.test.ts --run
```

Expected:

- FAIL，因为当前指标卡仍在使用多套 tone。

- [ ] **Step 3: 写最小实现**

在 `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue` 中：

- 把 `summaryMetrics` 里的 `tone` 统一改为 `brand`
- 删除 `success / danger / accent` 的卡片级配色覆写
- 保留表格中的语义色 tag

- [ ] **Step 4: 重跑测试确认通过**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDeviceListWorkspace.test.ts --run
```

Expected:

- PASS

## Task 3: 文档同步与回归验证

**Files:**
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: 更新文档**

记录：

- 详情页继续压缩说明文案的规则
- 关联设备页指标区统一回主色的规则

- [ ] **Step 2: 运行相关测试**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts src/__tests__/components/product/ProductDeviceListWorkspace.test.ts src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected:

- PASS

- [ ] **Step 3: 运行守卫与构建**

Run:

```bash
cd spring-boot-iot-ui
npm run component:guard
npm run list:guard
npm run style:guard
npm run build
```

Expected:

- 全部 PASS
