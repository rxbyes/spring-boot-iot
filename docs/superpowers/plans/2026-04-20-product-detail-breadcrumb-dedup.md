# Product Detail Breadcrumb Dedup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the duplicated in-page breadcrumb and generic workbench eyebrow from product detail pages so the global shell breadcrumb remains the only navigation row.

**Architecture:** Keep the route and global shell breadcrumb as the single navigation source of truth. Simplify `ProductDetailWorkbenchView` so the page shell title describes the active subpage while the hero keeps the product identity. Update docs and tests together so the page structure contract stays explicit.

**Tech Stack:** Vue 3, Vue Router, Vitest, Vue Test Utils, Markdown docs.

---

### Task 1: Lock the breadcrumb regression and implement the page-shell cleanup

**Files:**
- Create: `spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue`

- [ ] **Step 1: Write the failing test**

```ts
import { defineComponent, nextTick } from 'vue'
import { shallowMount, flushPromises } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import ProductDetailWorkbenchView from '@/views/ProductDetailWorkbenchView.vue'

const mockRoute = vi.hoisted(() => ({
  path: '/products/42/overview',
  name: 'product-overview',
  params: { productId: '42' },
  query: {}
}))

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => ({
    push: vi.fn()
  })
}))

vi.mock('@/api/product', () => ({
  productApi: {
    getProductById: vi.fn().mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 42,
        productKey: 'nf-collect-rtu-v1',
        productName: '南方测绘 采集型 遥测终端',
        description: '采集型遥测终端设备，协议 mqtt-json，直接接入'
      }
    }),
    getProductOverviewSummary: vi.fn().mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    })
  }
}))

vi.mock('@/api/device', () => ({
  deviceApi: {
    pageDevices: vi.fn().mockResolvedValue({
      code: 200,
      msg: 'success',
      data: { total: 0, pageNum: 1, pageSize: 10, records: [] }
    })
  }
}))

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  props: ['title', 'description', 'eyebrow', 'breadcrumbs', 'showTitle'],
  template: `
    <section>
      <p class="shell-title">{{ title }}</p>
      <p v-if="eyebrow" class="shell-eyebrow">{{ eyebrow }}</p>
      <pre v-if="breadcrumbs" class="shell-breadcrumbs">{{ JSON.stringify(breadcrumbs) }}</pre>
      <slot name="actions" />
      <slot />
    </section>
  `
})

it.each([
  ['product-overview', '产品总览'],
  ['product-contracts', '契约字段'],
  ['product-mapping-rules', '映射规则']
])('titles the page by active section for %s', async (routeName, expectedTitle) => {
  mockRoute.name = routeName
  const wrapper = shallowMount(ProductDetailWorkbenchView, {
    global: {
      stubs: {
        RouterLink: true,
        StandardButton: true,
        ProductDetailWorkbench: true,
        ProductDeviceListWorkspace: true,
        ProductModelDesignerWorkspace: true,
        StandardPageShell: StandardPageShellStub
      }
    }
  })

  await flushPromises()
  await nextTick()

  expect(wrapper.find('.shell-title').text()).toBe(expectedTitle)
  expect(wrapper.find('.shell-eyebrow').exists()).toBe(false)
  expect(wrapper.find('.shell-breadcrumbs').exists()).toBe(false)
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `Push-Location spring-boot-iot-ui; npm test -- --run src/__tests__/views/ProductDetailWorkbenchView.test.ts; Pop-Location`
Expected: fail because the current view still passes breadcrumb and eyebrow props.

- [ ] **Step 3: Write minimal implementation**

Update `spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue` so `StandardPageShell` only receives the active subsection title and description:

```vue
<StandardPageShell
  class="product-detail-page"
  :title="sectionMeta.label"
  :description="sectionMeta.description"
>
```

Remove the local breadcrumb computation and remove `eyebrow="产品工作区"`. Leave the hero, tabs, loading state, error state, and section content unchanged.

- [ ] **Step 4: Run test to verify it passes**

Run: `Push-Location spring-boot-iot-ui; npm test -- --run src/__tests__/views/ProductDetailWorkbenchView.test.ts; Pop-Location`
Expected: pass with the shell title set to the active section and no in-page breadcrumb props.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue
git commit -m "fix: dedupe product detail breadcrumb"
```

### Task 2: Sync the living docs with the new page structure

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Write the failing test**

```text
Document check:
- 02 must say product detail pages keep only the global shell breadcrumb.
- 08 must record the dedup fix as a dated UI structure cleanup.
- 15 must add the rule that product detail pages must not re-render in-page breadcrumbs or generic "产品工作区" eyebrows.
```

- [ ] **Step 2: Run test to verify it fails**

Run: `Select-String -Path docs/02-业务功能与流程说明.md,docs/08-变更记录与技术债清单.md,docs/15-前端优化与治理计划.md -Pattern '全局壳层 breadcrumb|产品工作区' -Encoding UTF8`
Expected: the current docs do not yet explicitly capture the new contract.

- [ ] **Step 3: Write minimal implementation**

Add one short paragraph to each file:

```md
- `2026-04-20` 起，`/products/:productId/*` 详情页只保留全局壳层 breadcrumb，不再在页内重复渲染 `产品定义中心 / 产品名 / 当前子页`；页头只保留当前子页标题、说明和动作，产品身份继续由 hero 承接。
```

- [ ] **Step 4: Run test to verify it passes**

Run: `Select-String -Path docs/02-业务功能与流程说明.md,docs/08-变更记录与技术债清单.md,docs/15-前端优化与治理计划.md -Pattern '全局壳层 breadcrumb|产品工作区' -Encoding UTF8`
Expected: docs contain the new page-structure rule and no stale duplicate-navigation wording remains in the updated sections.

- [ ] **Step 5: Commit**

```bash
git add docs/02-业务功能与流程说明.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
git commit -m "docs: record product detail breadcrumb dedup"
```
