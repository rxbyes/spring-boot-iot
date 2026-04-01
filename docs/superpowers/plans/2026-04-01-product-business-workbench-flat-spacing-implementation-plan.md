# Product Business Workbench Flat Spacing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `/products` 的 `产品经营工作台` 继续收口为更极简的扁平化详情页，去掉多余框感，重排页眉指标带，并把经营总览拆成两个独立章节。

**Architecture:** 仅调整 `ProductBusinessWorkbenchDrawer` 与 `ProductDetailWorkbench` 的 DOM 结构和 scoped CSS，让页眉从卡片式收口为横向经营带，让正文从“一个总大框 + 两组网格卡”收口为“两个独立章节 + 轻分隔字段表”。通过现有组件测试和 `/products` 页级测试锁住结构回归，再同步更新前端规范与治理文档。

**Tech Stack:** Vue 3、TypeScript、Vitest、Vite、scoped CSS、Element Plus、现有 `StandardDetailDrawer`

---

### Task 1: 更新测试以表达新的扁平化结构

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`

- [ ] **Step 1: 写页眉横向经营带的失败测试**

```ts
expect(wrapper.find('.product-business-workbench__header-band').exists()).toBe(true)
expect(wrapper.find('.product-business-workbench__summary-card').exists()).toBe(false)
expect(wrapper.find('.product-business-workbench__summary-divider').exists()).toBe(true)
expect(wrapper.find('.product-business-workbench__meta-inline').exists()).toBe(true)
expect(wrapper.find('.product-business-workbench__tab').classes()).toContain('product-business-workbench__tab')
```

- [ ] **Step 2: 运行组件测试并确认失败**

Run: `npm run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts --run`
Expected: FAIL，提示找不到 `product-business-workbench__header-band` 或仍然存在旧 `summary-card` 结构

- [ ] **Step 3: 写正文双章节去框结构的失败测试**

```ts
expect(wrapper.findAll('[data-testid="product-detail-stage-title"]').map((node) => node.text())).toEqual([
  '趋势摘要',
  '接入契约与产品档案'
])
expect(wrapper.find('.product-detail-workbench__ledger-board').exists()).toBe(false)
expect(wrapper.find('.product-detail-workbench__ledger-module').exists()).toBe(false)
expect(wrapper.find('.product-detail-workbench__trend-section').exists()).toBe(true)
expect(wrapper.find('.product-detail-workbench__archive-section').exists()).toBe(true)
```

- [ ] **Step 4: 运行详情组件测试并确认失败**

Run: `npm run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts --run`
Expected: FAIL，提示当前仍只有 `趋势摘要与契约档案` 总标题或仍存在 `ledger-board`

- [ ] **Step 5: 提交测试红灯基线**

```bash
git add spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts
git commit -m "test: define flatter product workbench layout"
```

### Task 2: 将页眉收口为横向经营带

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`

- [ ] **Step 1: 实现页眉 DOM 重排**

```vue
<section class="product-business-workbench__header">
  <div class="product-business-workbench__header-band">
    <div class="product-business-workbench__identity">
      <p class="product-business-workbench__kicker">产品经营工作台</p>
      <h3 class="product-business-workbench__headline">{{ productHeadline }}</h3>
      <div v-if="metaItems.length" class="product-business-workbench__meta-inline">
        <span v-for="item in metaItems" :key="item.key">{{ item.value }}</span>
      </div>
    </div>
    <div class="product-business-workbench__summary-band">
      <div
        v-for="(card, index) in headerSummaryCards"
        :key="card.key"
        class="product-business-workbench__summary-metric"
        :class="{ 'product-business-workbench__summary-metric--primary': card.primary }"
      >
        <span class="product-business-workbench__summary-label">{{ card.label }}</span>
        <span class="product-business-workbench__summary-value">{{ card.value }}</span>
        <span v-if="index < headerSummaryCards.length - 1" class="product-business-workbench__summary-divider" />
      </div>
    </div>
  </div>
</section>
```

- [ ] **Step 2: 用最小 CSS 实现横向经营带**

```css
.product-business-workbench__header-band {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(24rem, 28rem);
  gap: 2rem;
  align-items: end;
}

.product-business-workbench__summary-band {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr) minmax(0, 1fr);
  align-items: stretch;
  min-height: 6.6rem;
}

.product-business-workbench__summary-metric {
  position: relative;
  display: grid;
  align-content: end;
  gap: 0.18rem;
  padding: 0 1rem;
}
```

- [ ] **Step 3: 运行页眉组件测试确认转绿**

Run: `npm run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts --run`
Expected: PASS

- [ ] **Step 4: 提交页眉结构调整**

```bash
git add spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts
git commit -m "feat: flatten product workbench header band"
```

### Task 3: 将经营总览拆为两个独立章节并去掉总大框

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`

- [ ] **Step 1: 实现双章节 DOM 结构**

```vue
<section class="detail-panel product-detail-workbench__trend-section" data-testid="product-detail-trend-stage">
  <div class="detail-section-header">
    <div><h3 data-testid="product-detail-stage-title">趋势摘要</h3></div>
  </div>
  <div v-if="hasTrendMetrics" class="product-detail-workbench__trend-grid" data-testid="product-detail-ledger-trend-rows">
    <!-- trend cells -->
  </div>
</section>

<section class="detail-panel product-detail-workbench__archive-section" data-testid="product-detail-archive-stage">
  <div class="detail-section-header">
    <div><h3 data-testid="product-detail-stage-title">接入契约与产品档案</h3></div>
  </div>
  <div class="product-detail-workbench__archive-grid" data-testid="product-detail-ledger-archive-rows">
    <!-- archive cells -->
  </div>
</section>
```

- [ ] **Step 2: 用最小 CSS 去掉总外框和多余单元格框感**

```css
.product-detail-workbench {
  display: grid;
  gap: 1.5rem;
}

.product-detail-workbench__trend-section,
.product-detail-workbench__archive-section {
  display: grid;
  gap: 0.88rem;
  padding: 0;
  border: none;
  background: transparent;
  box-shadow: none;
}

.product-detail-workbench__trend-grid,
.product-detail-workbench__archive-grid {
  border: none;
  border-radius: 0;
  background: transparent;
}
```

- [ ] **Step 3: 运行详情组件测试确认转绿**

Run: `npm run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts --run`
Expected: PASS

- [ ] **Step 4: 提交经营总览结构调整**

```bash
git add spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts
git commit -m "feat: split product overview into flat sections"
```

### Task 4: 回归 `/products` 页面与守卫

**Files:**
- Verify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Verify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 1: 运行产品工作台页级测试**

Run: `npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run`
Expected: PASS

- [ ] **Step 2: 运行与产品工作台直接相关的组件测试集合**

Run: `npm run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts src/__tests__/components/product/ProductDetailWorkbench.test.ts src/__tests__/components/product/ProductDeviceListWorkspace.test.ts src/__tests__/components/product/ProductEditWorkspace.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/views/ProductWorkbenchView.test.ts --run`
Expected: PASS

- [ ] **Step 3: 运行守卫与构建**

Run: `npm run component:guard`
Expected: `Component contract guard passed.`

Run: `npm run style:guard`
Expected: `Style guard passed.`

Run: `npm run build`
Expected: `built in ...`

- [ ] **Step 4: 提交回归验证相关调整**

```bash
git add spring-boot-iot-ui/src/views/ProductWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts
git commit -m "test: verify flat product workbench layout"
```

### Task 5: 同步前端规范与变更文档

**Files:**
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: 更新前端规范中的页面语法**

```md
- `产品经营工作台` 页眉当前固定为“标题区 + 横向经营数据带 + 串联式元信息 + 扁平 tab rail”。
- `经营总览` 当前固定拆为 `趋势摘要` 与 `接入契约与产品档案` 两个独立章节，不再保留一个总外框包住两段内容。
- `趋势摘要` 与 `接入契约与产品档案` 当前禁止回流为九宫格字段卡、厚指标卡和总大框包裹结构。
```

- [ ] **Step 2: 更新变更记录与治理计划**

```md
- 2026-04-01：`/products` 经营总览继续按极简扁平化收口，页眉右侧指标卡退化为横向经营带，总览从一个总外框拆为 `趋势摘要` 与 `接入契约与产品档案` 两个独立章节。
- 后续若再出现“总大框 + 小卡片阵列 + 普遍粗体”的详情页表达，视为视觉回退。
```

- [ ] **Step 3: 提交文档同步**

```bash
git add docs/06-前端开发与CSS规范.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
git commit -m "docs: document flatter product workbench spacing"
```
