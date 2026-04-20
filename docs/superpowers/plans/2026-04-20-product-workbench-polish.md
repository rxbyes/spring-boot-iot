# Product Workbench Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore direct in-place editing on `/products`, switch `/products/:productId/devices` to standard server-side pagination, and tone down product overview typography and tab spacing so the detail workbench matches the rest of the access console.

**Architecture:** Keep the existing route split and the existing edit drawer truth source. The list page continues to own edit entry and edit session state; the detail page owns device paging state and delegates only table + pagination rendering to the device workspace component. Visual polish stays local to the product detail view and overview component without inventing new shared tokens.

**Tech Stack:** Vue 3 `<script setup>`, Vitest, Vue Test Utils, Element Plus, shared `StandardPagination`, shared `useServerPagination`, existing product/device API clients.

---

## File Map

- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
  - Restore visible direct `编辑` action in row actions while keeping `openEditWorkbench()` as the only edit entry.
- Modify: `spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue`
  - Add device pagination state, request devices by page, and wire page change events.
- Modify: `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`
  - Render shared pagination under the device table and emit page/size changes upward.
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
  - Keep the same information architecture but reduce emphasis and add stable class hooks for browser verification.
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
  - Lock direct edit visibility and in-place edit behavior.
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts`
  - Lock server pagination request parameters and pagination event flow.
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`
  - Lock pagination rendering and emitted events.
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
  - Lock compact hierarchy hooks used by the overview cards.
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

### Task 1: Restore Direct Edit On `/products`

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`

- [ ] **Step 1: Write the failing row-action test**

```ts
it('shows direct edit on product rows and keeps edit in the in-place drawer flow', async () => {
  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  ;(wrapper.vm as any).tableData = [
    {
      id: 1001,
      productKey: 'demo-product',
      productName: '演示产品',
      protocolCode: 'mqtt-json',
      nodeType: 1,
      dataFormat: 'JSON',
      manufacturer: 'GHLZM',
      status: 1,
      deviceCount: 0,
      onlineDeviceCount: 0,
      createTime: '2026-03-24T09:00:00',
      updateTime: '2026-03-24T09:00:00'
    }
  ]
  await nextTick()

  const directButtons = wrapper.findAll('.product-workbench-row-actions-stub__direct')
  expect(directButtons.some((node) => node.text() === '编辑')).toBe(true)

  await directButtons.find((node) => node.text() === '编辑')!.trigger('click')
  await flushPromises()
  await nextTick()

  expect(mockRouter.push).not.toHaveBeenCalled()
  expect(wrapper.get('[data-testid="product-business-workbench-active-view"]').text()).toBe('edit')
  expect(wrapper.find('.product-edit-workspace-stub').exists()).toBe(true)
})
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/ProductWorkbenchView.test.ts
```

Expected: the new assertion fails because the direct row actions only render `进入工作台` today.

- [ ] **Step 3: Implement the minimal row-action change**

```ts
function getProductDirectActions(variant: 'table' | 'card'): ProductDirectAction[] {
  const actions: ProductDirectAction[] = [
    {
      key: 'detail',
      command: 'detail',
      label: '进入工作台',
      title: '进入产品经营工作台',
      dataTestid: 'open-product-business-workbench'
    }
  ]

  if (permissionStore.hasPermission('iot:products:update')) {
    actions.push({
      key: 'edit',
      command: 'edit',
      label: '编辑',
      dataTestid: 'open-product-edit-workbench'
    })
  }

  if (permissionStore.hasPermission('iot:products:delete')) {
    actions.push({
      key: 'delete',
      command: 'delete',
      label: '删除'
    })
  }

  return actions
}
```

- [ ] **Step 4: Re-run the focused test and verify GREEN**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/ProductWorkbenchView.test.ts
```

Expected: `ProductWorkbenchView.test.ts` passes and still proves `edit` keeps the unified edit drawer path.

### Task 2: Convert Detail-Page Devices To Server Pagination

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`

- [ ] **Step 1: Write the failing detail-view pagination test**

```ts
const mockPageDevices = vi.hoisted(() => vi.fn())

vi.mock('@/api/device', () => ({
  deviceApi: {
    pageDevices: mockPageDevices
  }
}))

const ProductDeviceListWorkspaceStub = defineComponent({
  name: 'ProductDeviceListWorkspace',
  props: ['pagination'],
  emits: ['viewDevice', 'page-change', 'page-size-change'],
  template: `
    <section>
      <span class="device-page-num">{{ pagination?.pageNum }}</span>
      <span class="device-page-size">{{ pagination?.pageSize }}</span>
      <button class="emit-page-2" @click="$emit('page-change', 2)">page2</button>
      <button class="emit-size-20" @click="$emit('page-size-change', 20)">size20</button>
    </section>
  `
})

it('requests related devices with server pagination and reacts to pagination events', async () => {
  mockRoute.name = 'product-devices'
  mockPageDevices.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: { total: 25, pageNum: 1, pageSize: 10, records: [] }
  })

  const wrapper = shallowMount(ProductDetailWorkbenchView, {
    global: {
      stubs: {
        RouterLink: true,
        StandardButton: true,
        ProductDetailWorkbench: true,
        ProductDeviceListWorkspace: ProductDeviceListWorkspaceStub,
        ProductModelDesignerWorkspace: true,
        StandardPageShell: StandardPageShellStub
      }
    }
  })

  await flushPromises()
  expect(mockPageDevices).toHaveBeenLastCalledWith({
    productKey: 'nf-collect-rtu-v1',
    pageNum: 1,
    pageSize: 10
  })

  await wrapper.get('.emit-page-2').trigger('click')
  await flushPromises()
  expect(mockPageDevices).toHaveBeenLastCalledWith({
    productKey: 'nf-collect-rtu-v1',
    pageNum: 2,
    pageSize: 10
  })

  await wrapper.get('.emit-size-20').trigger('click')
  await flushPromises()
  expect(mockPageDevices).toHaveBeenLastCalledWith({
    productKey: 'nf-collect-rtu-v1',
    pageNum: 1,
    pageSize: 20
  })
})
```

- [ ] **Step 2: Write the failing workspace pagination rendering test**

```ts
const StandardPaginationStub = defineComponent({
  name: 'StandardPagination',
  props: ['currentPage', 'pageSize', 'total'],
  emits: ['update:current-page', 'update:page-size', 'current-change', 'size-change'],
  template: `
    <section class="device-pagination-stub">
      <span class="device-pagination-stub__page">{{ currentPage }}</span>
      <span class="device-pagination-stub__size">{{ pageSize }}</span>
      <span class="device-pagination-stub__total">{{ total }}</span>
      <button class="device-pagination-stub__next" @click="$emit('current-change', 2)">next</button>
      <button class="device-pagination-stub__resize" @click="$emit('size-change', 20)">resize</button>
    </section>
  `
})

it('renders shared pagination and emits page changes upward', async () => {
  const wrapper = mount(ProductDeviceListWorkspace, {
    props: {
      devices: [{ id: 2001, deviceName: '一号终端', deviceCode: 'device-001' }],
      pagination: { pageNum: 1, pageSize: 10, total: 25 }
    },
    global: {
      stubs: {
        ElTag: true,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub,
        StandardWorkbenchRowActions: StandardWorkbenchRowActionsStub,
        StandardPagination: StandardPaginationStub
      }
    }
  })

  expect(wrapper.find('.device-pagination-stub').exists()).toBe(true)

  await wrapper.get('.device-pagination-stub__next').trigger('click')
  expect(wrapper.emitted('page-change')).toEqual([[2]])

  await wrapper.get('.device-pagination-stub__resize').trigger('click')
  expect(wrapper.emitted('page-size-change')).toEqual([[20]])
})
```

- [ ] **Step 3: Run the focused tests and verify RED**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/ProductDetailWorkbenchView.test.ts src/__tests__/components/product/ProductDeviceListWorkspace.test.ts
```

Expected: failures show that the detail page still hardcodes `pageNum: 1, pageSize: 100`, and the workspace still has no pagination component or emitted events.

- [ ] **Step 4: Implement pagination state in the detail view**

```ts
const { pagination: devicePagination, setPageNum: setDevicePageNum, setPageSize: setDevicePageSize } = useServerPagination(10)

async function loadDevices() {
  if (!product.value?.productKey) {
    devices.value = []
    deviceErrorMessage.value = '产品 Key 缺失，无法加载关联设备。'
    return
  }

  devicesLoading.value = true
  deviceErrorMessage.value = ''
  try {
    const response = await deviceApi.pageDevices({
      productKey: product.value.productKey,
      pageNum: devicePagination.pageNum,
      pageSize: devicePagination.pageSize
    })
    devices.value = response.code === 200 ? response.data?.records || [] : []
    devicePagination.total = Number(response.data?.total || 0)
    devicePagination.pageNum = Number(response.data?.pageNum || devicePagination.pageNum)
    devicePagination.pageSize = Number(response.data?.pageSize || devicePagination.pageSize)
  } catch (error) {
    devices.value = []
    deviceErrorMessage.value = resolveRequestErrorMessage(error, '加载关联设备失败')
  } finally {
    devicesLoading.value = false
  }
}

function handleDevicePageChange(page: number) {
  setDevicePageNum(page)
  void loadDevices()
}

function handleDevicePageSizeChange(size: number) {
  setDevicePageSize(size)
  void loadDevices()
}
```

- [ ] **Step 5: Implement pagination UI in the workspace component**

```vue
<StandardPagination
  v-if="pagination.total > 0"
  :current-page="pagination.pageNum"
  :page-size="pagination.pageSize"
  :total="pagination.total"
  :page-sizes="[10, 20, 50, 100]"
  layout="total, sizes, prev, pager, next, jumper"
  @update:current-page="emit('page-change', $event)"
  @update:page-size="emit('page-size-change', $event)"
  @current-change="emit('page-change', $event)"
  @size-change="emit('page-size-change', $event)"
/>
```

```ts
withDefaults(defineProps<{
  devices: Device[]
  pagination?: { pageNum: number; pageSize: number; total: number }
  loading?: boolean
  loadingText?: string
  errorMessage?: string
  empty?: boolean
  emptyText?: string
  devicesLoading?: boolean
}>(), {
  pagination: () => ({ pageNum: 1, pageSize: 10, total: 0 })
})

const emit = defineEmits<{
  (event: 'viewDevice', device: Device): void
  (event: 'page-change', page: number): void
  (event: 'page-size-change', size: number): void
}>()
```

- [ ] **Step 6: Re-run the focused tests and verify GREEN**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/ProductDetailWorkbenchView.test.ts src/__tests__/components/product/ProductDeviceListWorkspace.test.ts
```

Expected: both tests pass and prove the device tab now follows server pagination.

### Task 3: Tighten Overview Hierarchy And Tab Rhythm

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
- Modify: `spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue`

- [ ] **Step 1: Write the failing overview hook test**

```ts
it('keeps overview cards compact with explicit hierarchy hooks for browser verification', () => {
  const wrapper = mount(ProductDetailWorkbench, {
    props: {
      product: baseProduct
    }
  })

  expect(wrapper.find('.product-detail-workbench__copy-label--metric').exists()).toBe(true)
  expect(wrapper.find('.product-detail-workbench__copy-value--metric').exists()).toBe(true)
  expect(wrapper.find('.product-detail-workbench__copy-value--body').exists()).toBe(true)
})
```

- [ ] **Step 2: Run the focused component test and verify RED**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/components/product/ProductDetailWorkbench.test.ts
```

Expected: the new hierarchy hook classes do not exist yet.

- [ ] **Step 3: Apply the minimal overview + tab-spacing implementation**

```vue
<span class="product-detail-workbench__copy-label product-detail-workbench__copy-label--metric">
  {{ metric.label }}
</span>
<strong class="product-detail-workbench__copy-value product-detail-workbench__copy-value--metric">
  {{ metric.value }}
</strong>
```

```css
.product-detail-workbench {
  gap: 0.96rem;
}

.product-detail-workbench__copy-label {
  font-size: 0.76rem;
}

.product-detail-workbench__copy-label--metric {
  letter-spacing: 0.01em;
}

.product-detail-workbench__copy-value {
  font-size: 0.96rem;
}

.product-detail-workbench__copy-value--metric {
  font-size: 1rem;
  line-height: 1.28;
}
```

```css
.product-detail-page {
  gap: 1rem;
}

.product-detail-page__hero {
  gap: 1rem;
  padding: 1rem 1.1rem 1.08rem;
}

.product-detail-page__tabs {
  gap: 0.72rem;
  margin-top: 0.12rem;
}

.product-detail-page__tab {
  gap: 0.18rem;
  padding: 0.82rem 0.94rem;
}

.product-detail-page__content {
  gap: 1rem;
}
```

- [ ] **Step 4: Re-run the focused component test and verify GREEN**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/components/product/ProductDetailWorkbench.test.ts
```

Expected: the overview component test passes with the new compact hierarchy hooks.

### Task 4: Update Product Docs And Run Full Frontend Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Update the business and governance docs in place**

```md
- `/products` 主列表继续作为产品定义主入口，本轮恢复直达 `编辑`，并保持原地编辑抽屉闭环。
- `/products/:productId/devices` 当前已改为标准服务端分页，固定使用 `10 / 20 / 50 / 100` 四档。
- 产品详情页总览与页签节奏本轮已进一步收口，避免单页字号和页签密度偏离 `设备资产中心`。
```

- [ ] **Step 2: Run the targeted frontend regression**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/ProductDetailWorkbenchView.test.ts src/__tests__/components/product/ProductDeviceListWorkspace.test.ts src/__tests__/components/product/ProductDetailWorkbench.test.ts
```

Expected: all targeted tests pass with `0` failures.

- [ ] **Step 3: Run the frontend build**

Run:

```bash
npm --prefix spring-boot-iot-ui run build
```

Expected: Vite build exits `0`.

- [ ] **Step 4: Commit the feature work**

```bash
git add spring-boot-iot-ui/src/views/ProductWorkbenchView.vue spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts docs/02-业务功能与流程说明.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
git commit -m "feat: polish product workbench editing and paging"
```
