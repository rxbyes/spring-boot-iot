# Product Detail Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `/products` 产品定义详情从当前 `42rem` 常规详情抽屉升级为 `60rem` 的经营简报式详情工作台，同时保留现有 `StandardDetailDrawer`、秒开补数和本地缓存基线。

**Architecture:** 让 `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue` 继续只负责列表、详情拉取、缓存和抽屉壳层控制，把详情展示本身抽到产品私有组件 `ProductDetailWorkbench.vue`。新组件内部按 `Hero Stage + Trend Stage + Contract & Archive Stage + Governance Stage` 四段结构组织内容，并在组件内承接详情专用格式化、趋势占位文案和 scoped CSS，避免 `ProductWorkbenchView.vue` 继续堆叠详情模板与样式。

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vitest, Vite, scoped CSS, existing `StandardDetailDrawer` / `StandardDrawerFooter`

---

## File Structure

### Existing files to modify

- `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- `docs/06-前端开发与CSS规范.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`
- `docs/archive/23-frontend-detail-optimization.md`

### New files to create

- `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`

### Review-only sync targets

- `README.md`
- `AGENTS.md`

## Implementation Notes

- 本计划假定实现发生在专用 worktree；如果必须在当前共享工作区执行，当前工作区已存在不相关改动，禁止使用 `git add .`。
- 不新增一级路由，不新增共享 `Standard*` 组件，不把 `物模型设计器` 放回详情头部动作。
- `ProductWorkbenchView.vue` 在实现完成后只保留详情抽屉壳层、请求/缓存、标题/副标题和 footer action，不再持有详情专用的巨型模板、格式化函数和大块 scoped CSS。
- 趋势区固定保留；如果活跃度字段为空，显示安静的占位文案，而不是整块消失。
- 头部动作固定只保留 `编辑`，底部动作固定保留 `关闭 / 查看设备`。

### Task 1: 创建产品详情工作台私有组件骨架

**Files:**
- Create: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
- Create: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`

- [ ] **Step 1: 先写组件失败测试，锁定四段结构和首屏主焦点**

创建 `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`：

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ProductDetailWorkbench from '@/components/product/ProductDetailWorkbench.vue'
import type { Product } from '@/types/api'

const baseProduct: Product = {
  id: 1001,
  productKey: 'north-monitor-gnss-v1',
  productName: '北斗监测终端',
  protocolCode: 'mqtt-json',
  nodeType: 1,
  dataFormat: 'JSON',
  manufacturer: 'GHLZM',
  description: '用于边坡监测的 GNSS 终端',
  status: 1,
  deviceCount: 2486,
  onlineDeviceCount: 1842,
  lastReportTime: '2026-03-29T09:15:00',
  createTime: '2026-03-01T10:00:00',
  updateTime: '2026-03-29T10:00:00',
  todayActiveCount: 826,
  sevenDaysActiveCount: 1634,
  thirtyDaysActiveCount: 2117,
  avgOnlineDuration: 128,
  maxOnlineDuration: 540
}

describe('ProductDetailWorkbench', () => {
  it('renders the executive-brief hierarchy with the device-scale hero first', () => {
    const wrapper = mount(ProductDetailWorkbench, {
      props: {
        product: baseProduct
      }
    })

    expect(
      wrapper.findAll('[data-testid="product-detail-stage-title"]').map((node) => node.text())
    ).toEqual([
      '设备规模与经营判断',
      '活跃趋势与状态判断',
      '接入契约与产品档案',
      '维护与治理'
    ])

    expect(wrapper.get('[data-testid="product-detail-hero-total"]').text()).toBe('2486')
    expect(wrapper.get('[data-testid="product-detail-hero-secondary-onlineDeviceCount"]').text()).toContain('1842')
    expect(wrapper.get('[data-testid="product-detail-hero-secondary-thirtyDaysActiveCount"]').text()).toContain('2117')
  })
})
```

- [ ] **Step 2: 运行测试，确认当前红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts --run
```

Expected:

- 测试失败，报错 `Failed to resolve import "@/components/product/ProductDetailWorkbench.vue"`。

- [ ] **Step 3: 写最小组件实现，让四段结构先站起来**

创建 `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`：

```vue
<template>
  <div class="product-detail-workbench">
    <section class="product-detail-stage product-detail-stage--hero">
      <header class="product-detail-stage__header">
        <span class="product-detail-stage__eyebrow">Hero Stage</span>
        <h3 data-testid="product-detail-stage-title">设备规模与经营判断</h3>
      </header>
      <div class="product-detail-hero">
        <article class="product-detail-hero__main">
          <span class="product-detail-hero__label">关联设备总量</span>
          <strong data-testid="product-detail-hero-total">{{ heroTotal }}</strong>
          <p class="product-detail-hero__headline">{{ heroHeadline }}</p>
          <p class="product-detail-hero__summary">{{ heroSummary }}</p>
        </article>
        <div class="product-detail-hero__secondary">
          <article
            v-for="metric in heroSecondaryMetrics"
            :key="metric.key"
            class="product-detail-hero-metric"
            :data-testid="`product-detail-hero-secondary-${metric.key}`"
          >
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
            <p>{{ metric.hint }}</p>
          </article>
        </div>
      </div>
    </section>

    <section class="product-detail-stage">
      <header class="product-detail-stage__header">
        <span class="product-detail-stage__eyebrow">Trend Stage</span>
        <h3 data-testid="product-detail-stage-title">活跃趋势与状态判断</h3>
      </header>
    </section>

    <section class="product-detail-stage">
      <header class="product-detail-stage__header">
        <span class="product-detail-stage__eyebrow">Contract & Archive</span>
        <h3 data-testid="product-detail-stage-title">接入契约与产品档案</h3>
      </header>
    </section>

    <section class="product-detail-stage">
      <header class="product-detail-stage__header">
        <span class="product-detail-stage__eyebrow">Governance Stage</span>
        <h3 data-testid="product-detail-stage-title">维护与治理</h3>
      </header>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { Product } from '@/types/api'

const props = defineProps<{
  product: Product
}>()

function toDisplayCount(value?: number | null) {
  const count = Number(value)
  return Number.isFinite(count) ? String(count) : '--'
}

const heroTotal = computed(() => toDisplayCount(props.product.deviceCount))

const heroHeadline = computed(() => {
  if ((props.product.status ?? 1) === 0) {
    return '当前产品已停用，建议先确认是否还需要继续保留库存设备。'
  }
  if ((props.product.deviceCount ?? 0) === 0) {
    return '当前产品还没有关联设备，可继续完成首批建档。'
  }
  return '当前产品已形成稳定设备规模，可直接查看活跃表现与后续治理动作。'
})

const heroSummary = computed(() => {
  return `最近设备上报：${props.product.lastReportTime || '--'}`
})

const heroSecondaryMetrics = computed(() => [
  {
    key: 'onlineDeviceCount',
    label: '在线设备',
    value: toDisplayCount(props.product.onlineDeviceCount),
    hint: '当前在线覆盖'
  },
  {
    key: 'thirtyDaysActiveCount',
    label: '30 日活跃',
    value: toDisplayCount(props.product.thirtyDaysActiveCount),
    hint: '近 30 天有上报的设备'
  },
  {
    key: 'avgOnlineDuration',
    label: '平均在线时长',
    value: props.product.avgOnlineDuration != null ? `${props.product.avgOnlineDuration} 分钟` : '--',
    hint: '近 30 天会话平均时长'
  }
])
</script>

<style scoped>
.product-detail-workbench {
  display: grid;
  gap: 18px;
}

.product-detail-stage {
  padding: 16px;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: #fff;
}

.product-detail-stage--hero {
  background: linear-gradient(135deg, rgba(232, 128, 56, 0.08), rgba(255, 255, 255, 0.98));
}

.product-detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  gap: 16px;
}
</style>
```

- [ ] **Step 4: 重新运行测试，确认组件骨架转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts --run
```

Expected:

- `ProductDetailWorkbench.test.ts` 通过。

- [ ] **Step 5: 提交组件骨架**

```bash
git add \
  spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue \
  spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts
git commit -m "feat: add product detail workbench component shell"
```

### Task 2: 把详情抽屉壳层接回新工作台组件

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 1: 先写页面失败测试，锁定抽屉宽度和壳层接线**

在 `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts` 中更新 `StandardDetailDrawerStub`，并新增 `ProductDetailWorkbenchStub`：

```ts
const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['size'],
  template: `
    <section class="product-detail-drawer-stub" :data-size="size">
      <div class="product-detail-drawer-stub__header-actions"><slot name="header-actions" /></div>
      <div class="product-detail-drawer-stub__body"><slot /></div>
      <div class="product-detail-drawer-stub__footer"><slot name="footer" /></div>
    </section>
  `
})

const ProductDetailWorkbenchStub = defineComponent({
  name: 'ProductDetailWorkbench',
  props: ['product'],
  template: '<section class="product-detail-workbench-stub">{{ product?.productName }}</section>'
})
```

把它加入 `mountView()` 的 `stubs`：

```ts
ProductDetailWorkbench: ProductDetailWorkbenchStub
```

再追加测试：

```ts
it('renders the widened detail drawer shell through ProductDetailWorkbench', async () => {
  const wrapper = mountView()

  ;(wrapper.vm as any).detailVisible = true
  ;(wrapper.vm as any).detailData = {
    id: 1001,
    productKey: 'demo-product',
    productName: '演示产品',
    protocolCode: 'mqtt-json',
    nodeType: 1,
    dataFormat: 'JSON',
    status: 1,
    deviceCount: 12,
    onlineDeviceCount: 8
  }

  await nextTick()

  expect(wrapper.get('.product-detail-drawer-stub').attributes('data-size')).toBe('60rem')
  expect(wrapper.get('.product-detail-workbench-stub').text()).toContain('演示产品')
  expect(wrapper.text()).toContain('编辑')
  expect(wrapper.text()).toContain('查看设备')
})
```

- [ ] **Step 2: 运行页面测试，确认当前红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected:

- 新增测试失败，因为当前抽屉仍是 `42rem`，详情 body 也还没有接 `ProductDetailWorkbench`。

- [ ] **Step 3: 接回抽屉壳层，固定宽度和动作语义**

在 `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue` 中引入新组件：

```ts
import ProductDetailWorkbench from '@/components/product/ProductDetailWorkbench.vue'
```

把详情抽屉壳层改成：

```vue
<StandardDetailDrawer
  v-model="detailVisible"
  class="product-detail-drawer"
  size="60rem"
  eyebrow="产品定义详情"
  :title="detailTitle"
  :subtitle="detailSubtitle"
  :loading="detailLoading"
  :error-message="detailErrorMessage"
  :empty="!detailData"
>
  <template #header-actions>
    <StandardButton
      v-permission="'iot:products:update'"
      action="confirm"
      size="small"
      @click="handleEditFromDetail"
    >
      编辑
    </StandardButton>
  </template>

  <ProductDetailWorkbench v-if="detailData" :product="detailData" />

  <template #footer>
    <StandardDrawerFooter @cancel="detailVisible = false">
      <StandardButton action="cancel" class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="detailVisible = false">
        关闭
      </StandardButton>
      <StandardButton
        action="confirm"
        class="standard-drawer-footer__button standard-drawer-footer__button--primary"
        :disabled="!detailData?.productKey"
        @click="detailData && handleOpenDeviceListDrawer(detailData)"
      >
        查看设备
      </StandardButton>
    </StandardDrawerFooter>
  </template>
</StandardDetailDrawer>
```

把副标题更新为新结构口径：

```ts
const detailSubtitle = computed(() => '按设备规模、活跃趋势、接入契约与产品档案、维护治理四段结构查看。')
```

此步先只替换抽屉 body，不在这一提交里做样式细抛光和旧 detail 计算属性清理。

- [ ] **Step 4: 重新运行页面测试，确认接线转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected:

- `ProductWorkbenchView.test.ts` 通过，新增测试能读到 `60rem` 和 `ProductDetailWorkbench` stub。

- [ ] **Step 5: 提交抽屉壳层接线**

```bash
git add \
  spring-boot-iot-ui/src/views/ProductWorkbenchView.vue \
  spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts
git commit -m "feat: wire product detail workbench drawer shell"
```

### Task 3: 完成工作台细节、占位态和旧详情代码清理

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`

- [ ] **Step 1: 扩展组件失败测试，锁定趋势占位态和中文区块语义**

在 `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts` 追加：

```ts
it('keeps the trend stage visible with a quiet placeholder when no activity metrics exist', () => {
  const wrapper = mount(ProductDetailWorkbench, {
    props: {
      product: {
        ...baseProduct,
        todayActiveCount: null,
        sevenDaysActiveCount: null,
        thirtyDaysActiveCount: null,
        avgOnlineDuration: null,
        maxOnlineDuration: null
      }
    }
  })

  expect(wrapper.get('[data-testid="product-detail-stage-trend"]').text()).toContain('当前还没有足够的活跃度样本')
  expect(wrapper.find('[data-testid="product-detail-trend-metrics"]').exists()).toBe(false)
  expect(wrapper.text()).toContain('接入契约')
  expect(wrapper.text()).toContain('产品档案')
  expect(wrapper.text()).toContain('维护与治理')
})
```

- [ ] **Step 2: 运行测试，确认当前红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts --run
```

Expected:

- 新增测试失败，因为当前趋势区没有稳定的占位态，也没有 `product-detail-stage-trend` / `product-detail-trend-metrics` 合约节点。

- [ ] **Step 3: 在组件内补齐趋势占位、四段内容和响应式样式，并清理 view 中的旧 detail 代码**

在 `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue` 中补齐趋势区、契约档案区和治理区：

```vue
<section class="product-detail-stage product-detail-stage--trend" data-testid="product-detail-stage-trend">
  <header class="product-detail-stage__header">
    <span class="product-detail-stage__eyebrow">Trend Stage</span>
    <h3 data-testid="product-detail-stage-title">活跃趋势与状态判断</h3>
    <p class="product-detail-stage__intro">把设备活跃度从并列数字卡升级成趋势判断区。</p>
  </header>

  <div v-if="hasTrendMetrics" class="product-detail-trend" data-testid="product-detail-trend-metrics">
    <article class="product-detail-trend__main">
      <strong>{{ trendHeadline }}</strong>
      <p>{{ trendSummary }}</p>
    </article>
    <div class="product-detail-trend__rail">
      <article v-for="metric in trendMetrics" :key="metric.key" class="product-detail-trend-metric">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <p>{{ metric.hint }}</p>
      </article>
    </div>
  </div>

  <div v-else class="product-detail-trend__empty">
    当前还没有足够的活跃度样本，请先结合最近上报和在线覆盖继续观察。
  </div>
</section>

<section class="product-detail-stage">
  <header class="product-detail-stage__header">
    <span class="product-detail-stage__eyebrow">Contract & Archive Stage</span>
    <h3 data-testid="product-detail-stage-title">接入契约与产品档案</h3>
    <p class="product-detail-stage__intro">把关键核对信息下沉到安静的信息层，避免和首屏主舞台争抢注意力。</p>
  </header>

  <div class="product-detail-contract-archive">
    <article class="product-detail-info-card">
      <span class="product-detail-info-card__title">接入契约</span>
      <dl class="product-detail-kv-list">
        <div v-for="item in contractItems" :key="item.key" class="product-detail-kv-item">
          <dt>{{ item.label }}</dt>
          <dd>{{ item.value }}</dd>
          <p>{{ item.hint }}</p>
        </div>
      </dl>
    </article>

    <article class="product-detail-info-card">
      <span class="product-detail-info-card__title">产品档案</span>
      <dl class="product-detail-kv-list">
        <div v-for="item in archiveItems" :key="item.key" class="product-detail-kv-item">
          <dt>{{ item.label }}</dt>
          <dd>{{ item.value }}</dd>
        </div>
      </dl>
    </article>
  </div>
</section>

<section class="product-detail-stage">
  <header class="product-detail-stage__header">
    <span class="product-detail-stage__eyebrow">Governance Stage</span>
    <h3 data-testid="product-detail-stage-title">维护与治理</h3>
    <p class="product-detail-stage__intro">把治理语义收束为可执行的短句和清单，作为详情尾段阅读。</p>
  </header>

  <div class="product-detail-governance">
    <article v-for="card in governanceCards" :key="card.key" class="product-detail-governance-card">
      <span class="product-detail-governance-card__title">{{ card.title }}</span>
      <strong>{{ card.headline }}</strong>
      <ul>
        <li v-for="line in card.lines" :key="line">{{ line }}</li>
      </ul>
    </article>
  </div>
</section>
```

```ts
const hasTrendMetrics = computed(() => {
  return props.product.todayActiveCount != null
    || props.product.sevenDaysActiveCount != null
    || props.product.thirtyDaysActiveCount != null
    || props.product.avgOnlineDuration != null
    || props.product.maxOnlineDuration != null
})

const trendHeadline = computed(() => {
  if (!hasTrendMetrics.value) {
    return '当前还没有足够的活跃度样本'
  }
  if ((props.product.todayActiveCount ?? 0) > 0 && (props.product.onlineDeviceCount ?? 0) > 0) {
    return '近期活跃表现稳定，可继续结合 7 日活跃和最长在线时长观察现场波动。'
  }
  return '活跃表现仍在爬坡，建议优先确认在线覆盖和最近上报节奏。'
})

const trendSummary = computed(() => {
  return `最近上报时间 ${props.product.lastReportTime || '--'}，用于辅助判断当前产品的接入稳定性。`
})

const trendMetrics = computed(() => {
  const metrics: Array<{ key: string; label: string; value: string; hint: string }> = []

  if (props.product.todayActiveCount != null) {
    metrics.push({
      key: 'today',
      label: '今日活跃',
      value: String(props.product.todayActiveCount),
      hint: '今天有上报的设备'
    })
  }

  if (props.product.sevenDaysActiveCount != null) {
    metrics.push({
      key: 'seven',
      label: '7 日活跃',
      value: String(props.product.sevenDaysActiveCount),
      hint: '最近 7 天有上报的设备'
    })
  }

  if (props.product.maxOnlineDuration != null) {
    metrics.push({
      key: 'maxOnlineDuration',
      label: '最长在线时长',
      value: `${props.product.maxOnlineDuration} 分钟`,
      hint: '近 30 天单次最长在线'
    })
  }

  return metrics
})

const contractItems = computed(() => [
  {
    key: 'protocolCode',
    label: '协议编码',
    value: props.product.protocolCode || '--',
    hint: '设备接入协议'
  },
  {
    key: 'nodeType',
    label: '节点类型',
    value: props.product.nodeType === 2 ? '网关子设备' : '直连设备',
    hint: '当前接入拓扑'
  },
  {
    key: 'dataFormat',
    label: '数据格式',
    value: props.product.dataFormat || '--',
    hint: '上报载荷格式'
  }
])

const archiveItems = computed(() => [
  {
    key: 'manufacturer',
    label: '厂商',
    value: props.product.manufacturer || '--'
  },
  {
    key: 'productId',
    label: '产品编号',
    value: props.product.id != null ? String(props.product.id) : '--'
  },
  {
    key: 'productKey',
    label: 'Product Key',
    value: props.product.productKey || '--'
  },
  {
    key: 'createTime',
    label: '创建时间',
    value: props.product.createTime || '--'
  },
  {
    key: 'description',
    label: '产品说明',
    value: props.product.description || '暂无产品说明'
  }
])

const governanceCards = computed(() => [
  {
    key: 'advice',
    title: '当前建议',
    headline: (props.product.status ?? 1) === 0 ? '当前产品已停用，优先确认是否保留库存设备。' : '当前产品可继续围绕活跃表现和设备规模做经营判断。',
    lines: [
      `在线设备 ${toDisplayCount(props.product.onlineDeviceCount)} 台，可与总量一起判断接入覆盖。`,
      `30 日活跃 ${toDisplayCount(props.product.thirtyDaysActiveCount)} 台，适合用于识别沉默设备。`
    ]
  },
  {
    key: 'maintenance',
    title: '维护规则',
    headline: '保持契约字段和物模型定义同步更新。',
    lines: [
      '协议编码、节点类型、数据格式变更前先确认设备侧升级窗口。',
      '产品说明保持简洁，优先记录现场识别和接入边界。'
    ]
  },
  {
    key: 'checklist',
    title: '变更前确认',
    headline: '在编辑产品前先核对详情中的关键档案字段。',
    lines: [
      '确认 Product Key、厂商和创建时间无需人工回填。',
      '如需调整物模型，继续在列表行操作中进入设计器，不回流到详情头部。'
    ]
  }
])
```

补齐组件 scoped CSS，并固定 `<= 1024px` 时改为单列：

```css
.product-detail-workbench {
  display: grid;
  gap: 18px;
}

.product-detail-stage--hero {
  padding: 22px;
  border-radius: calc(var(--radius-xl) + 4px);
  background: linear-gradient(135deg, rgba(232, 128, 56, 0.12), rgba(255, 255, 255, 0.98) 42%, rgba(107, 142, 199, 0.08));
  box-shadow: 0 12px 28px rgba(31, 50, 81, 0.06);
}

.product-detail-hero,
.product-detail-trend,
.product-detail-contract-archive,
.product-detail-governance {
  display: grid;
  gap: 16px;
}

.product-detail-hero {
  grid-template-columns: minmax(0, 1.24fr) minmax(0, 0.76fr);
}

.product-detail-trend {
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  align-items: start;
}

.product-detail-contract-archive,
.product-detail-governance {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.product-detail-hero-metric,
.product-detail-trend-metric,
.product-detail-info-card,
.product-detail-governance-card {
  padding: 16px;
  border: 1px solid rgba(107, 142, 199, 0.16);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.96);
}

.product-detail-trend__rail,
.product-detail-kv-list,
.product-detail-governance-card ul {
  display: grid;
  gap: 12px;
}

.product-detail-kv-item {
  display: grid;
  gap: 4px;
}

@media (max-width: 1024px) {
  .product-detail-hero,
  .product-detail-trend,
  .product-detail-contract-archive,
  .product-detail-governance {
    grid-template-columns: 1fr;
  }
}
```

然后清理 `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue` 中已经失效的 detail 专用逻辑：

```ts
// 删除以下只服务旧内联详情模板的函数和 computed：
// - formatFullDateTime
// - parseCount
// - detailDescriptionText
// - detailOnlineRatioText
// - detailOnlineRatioPercent
// - detailLifecycleStage
// - detailOperationHeadline
// - detailGovernanceNotice
// - detailOperationSummary
// - detailSummaryMetrics
// - detailContractCards
// - detailArchiveIdText
// - detailArchiveProductKeyText
// - detailArchiveManufacturerText
// - detailArchiveCreateDateText
// - detailGovernanceHeadline
// - detailMaintenanceRules
// - detailChangeChecklist
// - hasActiveMetrics
// - detailActiveMetrics
```

同时删除旧内联详情样式块，只保留抽屉壳层样式：

```bash
rg -n "product-detail-zone|product-detail-overview|product-detail-ledger|product-detail-active|product-detail-governance" spring-boot-iot-ui/src/views/ProductWorkbenchView.vue
```

此命令在清理后应无输出。

- [ ] **Step 4: 重新运行组件测试和清理检查，确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts src/__tests__/views/ProductWorkbenchView.test.ts --run
rg -n "product-detail-zone|product-detail-overview|product-detail-ledger|product-detail-active|product-detail-governance|parseCount\\(|formatFullDateTime\\(" src/views/ProductWorkbenchView.vue
```

Expected:

- 两个测试文件全部通过。
- `rg` 无输出，说明旧 detail 专用模板类和 helper 已从 `ProductWorkbenchView.vue` 清理完毕。

- [ ] **Step 5: 提交工作台细抛光与旧代码清理**

```bash
git add \
  spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue \
  spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts \
  spring-boot-iot-ui/src/views/ProductWorkbenchView.vue
git commit -m "feat: polish product detail executive brief workbench"
```

### Task 4: 同步文档并做最终验证

**Files:**
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/archive/23-frontend-detail-optimization.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: 先写文档变更清单**

在 `docs/06-前端开发与CSS规范.md` 详情抽屉规则段落后补充：

```md
- `/products` 产品详情当前固定升级为 `60rem` 沉浸式详情工作台；仍复用 `StandardDetailDrawer`，但内部结构必须保持 `Hero Stage + Trend Stage + Contract & Archive Stage + Governance Stage` 四段，不得回退到多块同权重摘要卡平铺。
- `产品定义中心` 详情首屏只允许保留一个主焦点：关联设备规模主舞台；活跃趋势为第二阅读层，接入契约与产品档案下沉到第三阅读层，`物模型设计器` 不回流到详情头部动作。
```

在 `docs/15-前端优化与治理计划.md` 长期规则区补充：

```md
- `产品定义中心` 详情当前已升级为 `60rem` 经营简报式工作台：头部只保留 `编辑`，底部固定 `关闭 / 查看设备`，主舞台使用一处品牌橙强调，不得重新长回多处浅橙大板块或第二套详情头部动作。
```

在 `docs/08-变更记录与技术债清单.md` 前端摘要区记录：

```md
- 2026-03-29：`/products` 详情抽屉已升级为 `60rem` 沉浸式经营简报工作台，内部固定为 `Hero Stage + Trend Stage + Contract & Archive Stage + Governance Stage` 四段结构；设备规模与活跃表现上移为首屏主焦点，接入契约与产品档案下沉，继续保持“秒开 + 后台补数 + 本地详情缓存”基线。
```

在 `docs/archive/23-frontend-detail-optimization.md` 新增记录：

```md
## 2026-03-29 产品定义详情经营简报工作台升级

- 问题现象：当前详情虽然已经分区，但仍偏向普通字段卡集合，设备规模、活跃表现、契约档案和治理建议在首屏争抢注意力。
- 调整策略：把详情抽屉从 `42rem` 扩到 `60rem`，并重组为 `设备规模主舞台 -> 活跃趋势 -> 接入契约与产品档案 -> 维护与治理` 四段结构。
- 修改文件：
  - `src/components/product/ProductDetailWorkbench.vue`
  - `src/views/ProductWorkbenchView.vue`
- 防回退规则：
  - 详情抽屉宽度固定 `60rem`
  - 头部只保留 `编辑`
  - 底部只保留 `关闭 / 查看设备`
  - 首屏唯一主焦点为设备规模主舞台
  - 详情继续保持“秒开 + 后台静默补数”
```

- [ ] **Step 2: 检查 `README.md` 与 `AGENTS.md` 是否需要修改**

Run:

```bash
git diff -- README.md AGENTS.md
```

Expected:

- 本轮只改产品详情工作台结构与前端规范，无需修改 `README.md` 与 `AGENTS.md`。
- 如果命令无输出，最终交付说明中明确写出“已检查，无需修改”。

- [ ] **Step 3: 运行最终验证**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts src/__tests__/views/ProductWorkbenchView.test.ts --run
npm run component:guard
npm run list:guard
npm run style:guard
npm run build
```

Expected:

- 目标组件和页面测试通过。
- `component:guard`、`list:guard`、`style:guard` 全部通过。
- `npm run build` 通过。

- [ ] **Step 4: 提交文档同步**

```bash
git add \
  docs/06-前端开发与CSS规范.md \
  docs/08-变更记录与技术债清单.md \
  docs/15-前端优化与治理计划.md \
  docs/archive/23-frontend-detail-optimization.md
git commit -m "docs: record product detail workbench rules"
```
