# Product Detail Tab Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove redundant tab subtitles from the product detail workbench, redesign the five section switches into friendlier single-line option tabs, and reduce the hero metrics to `关联设备 / 正式字段` without disturbing the routed detail architecture.

**Architecture:** Keep `/products/:productId/overview|devices|contracts|mapping-rules|releases` as the routing truth and continue using `RouterLink` for section navigation. Limit the behavior change to `ProductDetailWorkbenchView.vue`: the hero copy, metric cards, and tab row all stay in the existing page shell; tests lock the new labels and metric count before CSS/template changes land. Documentation updates only record the new visual contract and do not reopen the earlier pagination or edit-drawer work.

**Tech Stack:** Vue 3 `<script setup>`, Vitest, Vue Test Utils, Element Plus, existing `RouterLink`-based product workbench routes, Markdown docs under `docs/`.

---

## File Map

- Modify: `spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue`
  - Remove tab captions from the view model, cut hero metrics from three down to two, and restyle the hero/tab shell.
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts`
  - Lock the single-line tab labels and the two-card hero metric contract before changing the page.
- Modify: `docs/02-业务功能与流程说明.md`
  - Record that product detail pages now use single-line option tabs and a two-metric hero.
- Modify: `docs/08-变更记录与技术债清单.md`
  - Record the April 20 tab/hero visual cleanup.
- Modify: `docs/15-前端优化与治理计划.md`
  - Add long-term rules preventing repeated tab subtitles and low-frequency release metrics from returning to the hero.

### Task 1: Lock The New Product Detail Header Contract

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue`

- [ ] **Step 1: Write the failing hero/tab test**

```ts
it('renders single-line workbench tabs and keeps only device and field hero metrics', async () => {
  const wrapper = shallowMount(ProductDetailWorkbenchView, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
        StandardButton: true,
        ProductDetailWorkbench: true,
        ProductDeviceListWorkspace: ProductDeviceListWorkspaceStub,
        ProductModelDesignerWorkspace: true,
        StandardPageShell: StandardPageShellStub
      }
    }
  })

  await flushPromises()
  await nextTick()

  const tabTexts = wrapper
    .findAll('.product-detail-page__tab')
    .map((node) => node.text().replace(/\s+/g, ' ').trim())

  expect(tabTexts).toEqual(['产品总览', '关联设备', '契约字段', '映射规则', '版本台账'])
  expect(wrapper.find('.product-detail-page__tab small').exists()).toBe(false)

  const metricTexts = wrapper
    .findAll('.product-detail-page__metric-card')
    .map((node) => node.text().replace(/\s+/g, ' ').trim())

  expect(metricTexts).toHaveLength(2)
  expect(metricTexts[0]).toContain('关联设备')
  expect(metricTexts[1]).toContain('正式字段')
  expect(wrapper.text()).not.toContain('最新批次')
})
```

- [ ] **Step 2: Run the focused view test and verify RED**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/ProductDetailWorkbenchView.test.ts
```

Expected: FAIL because the current page still renders a `<small>` caption in every tab and still shows the `最新批次` hero metric card.

- [ ] **Step 3: Implement the minimal data/template change in the detail view**

```ts
const sectionLabels: Record<ProductWorkbenchSection, { label: string; description: string }> = {
  overview: {
    label: '产品总览',
    description: '查看产品档案、活跃度和最新发布状态。'
  },
  devices: {
    label: '关联设备',
    description: '查看当前产品下的设备清单与最近上报。'
  },
  contracts: {
    label: '契约字段',
    description: '只保留样本输入、识别结果、本次生效和当前已生效字段。'
  },
  'mapping-rules': {
    label: '映射规则',
    description: '集中维护厂商字段映射建议与映射规则台账。'
  },
  releases: {
    label: '版本台账',
    description: '查看发布批次、回滚试算和跨批次差异。'
  }
}

const tabItems = computed(() =>
  (Object.keys(sectionLabels) as ProductWorkbenchSection[]).map((key) => ({
    key,
    label: sectionLabels[key].label,
    to: buildProductWorkbenchSectionPath(productId.value, key)
  }))
)

const heroMetrics = computed(() => [
  {
    key: 'deviceCount',
    label: '关联设备',
    value: String(overviewSummary.value?.deviceCount ?? product.value?.deviceCount ?? 0),
    hint: `在线 ${overviewSummary.value?.onlineDeviceCount ?? product.value?.onlineDeviceCount ?? 0}`
  },
  {
    key: 'formalFieldCount',
    label: '正式字段',
    value: String(overviewSummary.value?.formalFieldCount ?? 0),
    hint: '产品正式物模型'
  }
])
```

```vue
<nav class="product-detail-page__tabs" aria-label="产品工作区导航">
  <RouterLink
    v-for="item in tabItems"
    :key="item.key"
    :to="item.to"
    class="product-detail-page__tab"
    :class="{ 'product-detail-page__tab--active': activeSection === item.key }"
  >
    <span>{{ item.label }}</span>
  </RouterLink>
</nav>
```

- [ ] **Step 4: Re-run the focused view test and verify GREEN**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/ProductDetailWorkbenchView.test.ts
```

Expected: PASS with the new single-line tab contract and exactly two hero metric cards.

### Task 2: Restyle The Hero And Tabs Into Friendly Option Tabs

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue`

- [ ] **Step 1: Add the tab/hero CSS that matches the approved design**

```css
.product-detail-page {
  display: grid;
  gap: 1.08rem;
  min-width: 0;
}

.product-detail-page__hero {
  gap: 1.08rem;
  padding: 0.92rem 1.04rem 1rem;
}

.product-detail-page__hero-copy {
  gap: 0.22rem;
}

.product-detail-page__hero-copy h2 {
  font-size: clamp(1.18rem, 1.6vw, 1.42rem);
  line-height: 1.24;
}

.product-detail-page__hero-metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.78rem;
}

.product-detail-page__metric-card {
  gap: 0.24rem;
  padding: 0.76rem 0.84rem;
}

.product-detail-page__tabs {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.76rem;
  padding: 0.24rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-2xl) + 2px);
  background: color-mix(in srgb, var(--brand-light) 8%, white);
}

.product-detail-page__tab {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 2.9rem;
  padding: 0.72rem 0.86rem;
  border: 1px solid transparent;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.82);
  text-align: center;
}

.product-detail-page__tab span {
  font-weight: 600;
  line-height: 1.42;
}

.product-detail-page__tab:hover {
  border-color: color-mix(in srgb, var(--brand) 16%, var(--panel-border));
  background: rgba(255, 255, 255, 0.96);
  transform: translateY(-1px);
}

.product-detail-page__tab--active {
  border-color: color-mix(in srgb, var(--brand) 24%, var(--panel-border));
  background: color-mix(in srgb, var(--brand-light) 22%, white);
  color: color-mix(in srgb, var(--brand) 82%, var(--text-heading));
  box-shadow: 0 8px 18px -20px color-mix(in srgb, var(--brand) 42%, transparent);
}
```

- [ ] **Step 2: Add the responsive layout rules**

```css
@media (max-width: 1100px) {
  .product-detail-page__tabs {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .product-detail-page__hero-metrics,
  .product-detail-page__overview-grid,
  .product-detail-page__tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .product-detail-page__tabs,
  .product-detail-page__hero-metrics,
  .product-detail-page__overview-grid {
    grid-template-columns: 1fr;
  }
}
```

- [ ] **Step 3: Run the focused view test again to keep GREEN after styling**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/ProductDetailWorkbenchView.test.ts
```

Expected: PASS. The test should still lock behavior after the CSS-only refactor.

### Task 3: Update The Product Detail Docs And Verify The Frontend Build

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Update the product/detail behavior docs in place**

```md
- `/products/:productId/overview|devices|contracts|mapping-rules|releases` 当前已统一切换为单行真选项卡，不再在 tab 下方重复渲染 `概览 / 设备 / 契约 / 映射 / 版本` 一类缩写副标题。
- 产品详情页 hero 当前只保留 `关联设备 / 正式字段` 两项高频指标；`最新批次` 等发布信息继续下沉到总览内容区，不再挤占首屏工作台导航层级。
```

- [ ] **Step 2: Run the targeted regression suite**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/ProductDetailWorkbenchView.test.ts
```

Expected: PASS with `0` failures.

- [ ] **Step 3: Run the frontend build**

Run:

```bash
npm --prefix spring-boot-iot-ui run build
```

Expected: Vite build exits `0`.

- [ ] **Step 4: Stage only the task files and commit**

```bash
git add spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts docs/02-业务功能与流程说明.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md docs/superpowers/plans/2026-04-20-product-detail-tab-refresh.md
git commit -m "feat: refresh product detail workbench tabs"
```
